package Dion.timekeeping.service;

import Dion.timekeeping.dto.FaceRegisterRequest;
import Dion.timekeeping.dto.StreamTimekeepingRequest;
import Dion.timekeeping.dto.TimekeepingResponse;
import Dion.timekeeping.entity.*;
import Dion.timekeeping.repository.*;
import Dion.timekeeping.util.FaceVectorUtil;
import Dion.timekeeping.util.GeoLocationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimekeepingService {

    private final EmployeeRepository employeeRepository;
    private final FaceProfileRepository faceProfileRepository;
    private final TimekeepingRecordRepository timekeepingRecordRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final CompanyLocationRepository companyLocationRepository;
    private final WorkingHourConfigRepository workingHourConfigRepository;

    // Ngưỡng độ tin cậy tương đồng khuôn mặt (Cosine similarity >= 0.85)
    private static final double SIMILARITY_THRESHOLD = 0.85;

    // Khoảng thời gian tối thiểu giữa 2 lần nhận diện cùng 1 người (tránh quét liên tiếp, đơn vị: giây)
    private static final long COOLDOWN_SECONDS = 30;

    /**
     * Use Case: Register a face (Đăng ký khuôn mặt)
     */
    @Transactional
    public void registerFace(FaceRegisterRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + request.getEmployeeId()));

        if (request.getFaceDescriptor() == null || request.getFaceDescriptor().size() != 128) {
            throw new IllegalArgumentException("Dữ liệu Face Descriptor không hợp lệ (yêu cầu vector 128 chiều)!");
        }

        String jsonVector = FaceVectorUtil.toJson(request.getFaceDescriptor());

        FaceProfile faceProfile = faceProfileRepository.findByEmployeeId(employee.getId())
                .orElse(FaceProfile.builder().employee(employee).build());

        faceProfile.setEmbeddingJson(jsonVector);
        faceProfile.setSampleCount(faceProfile.getSampleCount() == null ? 1 : faceProfile.getSampleCount() + 1);

        faceProfileRepository.save(faceProfile);
        log.info("Đã cập nhật khuôn mặt thành công cho nhân viên: {} ({})", employee.getFullName(), employee.getEmployeeCode());
    }

    /**
     * Use Case: Timekeeping (Quét mặt từ Webcam Stream real-time)
     */
    @Transactional
    public TimekeepingResponse processStreamRecognition(StreamTimekeepingRequest request) {
        if (request.getFaceDescriptor() == null || request.getFaceDescriptor().size() != 128) {
            return TimekeepingResponse.builder()
                    .success(false)
                    .message("Không tìm thấy dữ liệu khuôn mặt hợp lệ trong khung hình.")
                    .build();
        }

        // 1. Lấy tất cả khuôn mặt nhân viên đang active
        List<FaceProfile> activeProfiles = faceProfileRepository.findAllActiveProfilesWithEmployee();
        if (activeProfiles.isEmpty()) {
            return TimekeepingResponse.builder()
                    .success(false)
                    .message("Hệ thống chưa có nhân viên nào đăng ký khuôn mặt.")
                    .build();
        }

        FaceProfile matchedProfile = null;
        double highestSimilarity = -1.0;

        // 2. So sánh vector gửi lên với các nhân viên trong DB
        for (FaceProfile profile : activeProfiles) {
            List<Double> savedVector = FaceVectorUtil.parseVector(profile.getEmbeddingJson());
            double sim = FaceVectorUtil.cosineSimilarity(request.getFaceDescriptor(), savedVector);

            if (sim > highestSimilarity) {
                highestSimilarity = sim;
                matchedProfile = profile;
            }
        }

        // 3. Kiểm tra ngưỡng tin cậy
        if (highestSimilarity < SIMILARITY_THRESHOLD || matchedProfile == null) {
            return TimekeepingResponse.builder()
                    .success(false)
                    .confidenceScore(highestSimilarity > 0 ? highestSimilarity : 0.0)
                    .message("Khuôn mặt chưa được đăng ký trong hệ thống.")
                    .build();
        }

        Employee employee = matchedProfile.getEmployee();
        LocalDateTime now = LocalDateTime.now();

        // 4. Kiểm tra Cooldown: Nhân viên này có vừa mới điểm danh trong 30 giây qua không?
        Optional<TimekeepingRecord> lastRecord = timekeepingRecordRepository.findFirstByEmployeeIdOrderByRecordTimeDesc(employee.getId());
        if (lastRecord.isPresent()) {
            long secondsSinceLastCheck = Duration.between(lastRecord.get().getRecordTime(), now).getSeconds();
            if (secondsSinceLastCheck < COOLDOWN_SECONDS) {
                return TimekeepingResponse.builder()
                        .success(false)
                        .message("Bạn vừa điểm danh cách đây " + secondsSinceLastCheck + " giây. Vui lòng chờ thêm một chút.")
                        .employeeName(employee.getFullName())
                        .employeeCode(employee.getEmployeeCode())
                        .confidenceScore(highestSimilarity)
                        .build();
            }
        }

        // 5. Kiểm tra vị trí địa lý GPS (Use Case: Configure company map location)
        boolean isLocationValid = true;
        if (request.getLatitude() != null && request.getLongitude() != null) {
            List<CompanyLocation> locations = companyLocationRepository.findByActiveTrue();
            if (!locations.isEmpty()) {
                isLocationValid = locations.stream().anyMatch(loc ->
                        GeoLocationUtil.isWithinAllowedRadius(
                                request.getLatitude(),
                                request.getLongitude(),
                                loc.getLatitude(),
                                loc.getLongitude(),
                                loc.getAllowedRadiusMeters()
                        )
                );
            }
        }

        // 6. Đánh giá trạng thái giờ làm (Use Case: Configure working hours)
        TimekeepingRecord.AttendanceStatus status = evaluateAttendanceStatus(request.getCheckType(), now.toLocalTime());

        // 7. Lưu bản ghi lịch sử thô (TimekeepingRecord)
        TimekeepingRecord record = TimekeepingRecord.builder()
                .employee(employee)
                .checkType(request.getCheckType() != null ? request.getCheckType() : TimekeepingRecord.CheckType.CHECKIN)
                .recordTime(now)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationValid(isLocationValid)
                .confidenceScore(highestSimilarity)
                .status(status)
                .note(isLocationValid ? "Hợp lệ" : "Vị trí nằm ngoài bán kính công ty")
                .build();

        timekeepingRecordRepository.save(record);

        // 8. Cập nhật Bảng Chấm công theo ngày (DailyAttendance)
        LocalDate today = now.toLocalDate();
        DailyAttendance daily = dailyAttendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElse(DailyAttendance.builder()
                        .employee(employee)
                        .workDate(today)
                        .shiftName("Ca làm việc DION 09:00–18:30")
                        .build());

        String displayStatus = "Hợp lệ";

        if (request.getCheckType() == TimekeepingRecord.CheckType.CHECKIN) {
            daily.setCheckInTime(now);
            daily.setCheckInLat(request.getLatitude());
            daily.setCheckInLon(request.getLongitude());
            daily.setCheckInLocationValid(isLocationValid);
            daily.setCheckInConfidence(highestSimilarity);

            // Kiểm tra đi muộn dựa theo ca làm
            WorkingHourConfig config = workingHourConfigRepository.findFirstByActiveTrue().orElse(null);
            if (config != null && now.toLocalTime().isAfter(config.getStartTime().plusMinutes(config.getLateGraceMinutes()))) {
                daily.setStatus("Đi muộn");
                displayStatus = "Đi muộn";
            } else {
                daily.setStatus("Hợp lệ");
            }
        } else if (request.getCheckType() == TimekeepingRecord.CheckType.CHECKOUT) {
            daily.setCheckOutTime(now);
            daily.setCheckOutLat(request.getLatitude());
            daily.setCheckOutLon(request.getLongitude());
            daily.setCheckOutLocationValid(isLocationValid);
            daily.setCheckOutConfidence(highestSimilarity);

            // Tính tổng thời lượng làm việc (phút)
            if (daily.getCheckInTime() != null) {
                long minutes = Duration.between(daily.getCheckInTime(), now).toMinutes();
                daily.setTotalMinutesWorked(minutes);

                // Quy tắc 8 giờ = 480 phút
                if (minutes < 480) {
                    daily.setStatus("Thiếu thời gian làm việc");
                    displayStatus = "Thiếu thời gian làm việc (" + (minutes / 60) + "h" + (minutes % 60) + "p < 8h)";
                } else if ("Đi muộn".equals(daily.getStatus())) {
                    displayStatus = "Đi muộn (Đã đủ 8h)";
                } else {
                    daily.setStatus("Hợp lệ");
                    displayStatus = "Hợp lệ (Đủ " + (minutes / 60) + "h" + (minutes % 60) + "p)";
                }
            } else {
                daily.setStatus("Không có giờ vào (Thiếu Check-in)");
                displayStatus = "Không có giờ vào";
            }
        }

        dailyAttendanceRepository.save(daily);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

        return TimekeepingResponse.builder()
                .success(true)
                .employeeName(employee.getFullName())
                .employeeCode(employee.getEmployeeCode())
                .checkType(record.getCheckType().name())
                .time(now.format(timeFormatter))
                .confidenceScore(Math.round(highestSimilarity * 100.0) / 100.0)
                .status(displayStatus)
                .locationValid(isLocationValid)
                .message("Điểm danh " + record.getCheckType() + " thành công!")
                .build();
    }

    private TimekeepingRecord.AttendanceStatus evaluateAttendanceStatus(TimekeepingRecord.CheckType checkType, LocalTime time) {
        Optional<WorkingHourConfig> configOpt = workingHourConfigRepository.findFirstByActiveTrue();
        if (configOpt.isEmpty()) {
            return TimekeepingRecord.AttendanceStatus.ON_TIME;
        }

        WorkingHourConfig config = configOpt.get();
        if (checkType == TimekeepingRecord.CheckType.CHECKIN) {
            LocalTime maxAllowedTime = config.getStartTime().plusMinutes(config.getLateGraceMinutes());
            return time.isAfter(maxAllowedTime) ? TimekeepingRecord.AttendanceStatus.LATE : TimekeepingRecord.AttendanceStatus.ON_TIME;
        } else if (checkType == TimekeepingRecord.CheckType.CHECKOUT) {
            LocalTime minAllowedTime = config.getEndTime().minusMinutes(config.getEarlyLeaveGraceMinutes());
            return time.isBefore(minAllowedTime) ? TimekeepingRecord.AttendanceStatus.EARLY_LEAVE : TimekeepingRecord.AttendanceStatus.ON_TIME;
        }

        return TimekeepingRecord.AttendanceStatus.ON_TIME;
    }
}
