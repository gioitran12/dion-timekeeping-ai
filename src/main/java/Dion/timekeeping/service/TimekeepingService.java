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
import java.util.*;

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
    private static final double SIMILARITY_THRESHOLD = 0.82;

    // Khoảng thời gian tối thiểu giữa 2 lần nhận diện cùng 1 người (đơn vị: giây)
    private static final long COOLDOWN_SECONDS = 30;

    /**
     * Lấy trạng thái chấm công hôm nay của nhân viên đang đăng nhập
     */
    public Map<String, Object> getTodayStatus(String username) {
        Map<String, Object> result = new LinkedHashMap<>();
        Employee employee = employeeRepository.findByUsername(username).orElse(null);
        if (employee == null) {
            result.put("hasCheckedIn", false);
            result.put("hasCheckedOut", false);
            result.put("error", "Không tìm thấy nhân viên");
            return result;
        }

        LocalDate today = LocalDate.now();
        Optional<DailyAttendance> dailyOpt = dailyAttendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today);

        boolean hasCheckedIn = dailyOpt.map(d -> d.getCheckInTime() != null).orElse(false);
        boolean hasCheckedOut = dailyOpt.map(d -> d.getCheckOutTime() != null).orElse(false);
        String checkInTime = dailyOpt.filter(d -> d.getCheckInTime() != null)
                .map(d -> d.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .orElse(null);
        String checkOutTime = dailyOpt.filter(d -> d.getCheckOutTime() != null)
                .map(d -> d.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .orElse(null);

        result.put("hasCheckedIn", hasCheckedIn);
        result.put("hasCheckedOut", hasCheckedOut);
        result.put("checkInTime", checkInTime);
        result.put("checkOutTime", checkOutTime);
        result.put("employeeName", employee.getFullName());
        result.put("hasFaceProfile", faceProfileRepository.findByEmployeeId(employee.getId()).isPresent());
        return result;
    }

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
     * Use Case: Timekeeping — Quét mặt từ Webcam Stream real-time
     * Yêu cầu: người dùng phải đã đăng nhập (loggedInUsername != null)
     */
    @Transactional
    public TimekeepingResponse processStreamRecognition(StreamTimekeepingRequest request) {
        if (request.getFaceDescriptor() == null || request.getFaceDescriptor().size() != 128) {
            return TimekeepingResponse.builder()
                    .success(false)
                    .message("Không tìm thấy dữ liệu khuôn mặt hợp lệ trong khung hình.")
                    .build();
        }

        // 1. Lấy nhân viên từ session đăng nhập (bắt buộc)
        if (request.getLoggedInUsername() == null || request.getLoggedInUsername().isBlank()) {
            return TimekeepingResponse.builder()
                    .success(false)
                    .message("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.")
                    .build();
        }

        Employee employee = employeeRepository.findByUsername(request.getLoggedInUsername()).orElse(null);
        if (employee == null || !employee.getActive()) {
            return TimekeepingResponse.builder()
                    .success(false)
                    .message("Tài khoản không tồn tại hoặc đã bị vô hiệu hóa.")
                    .build();
        }

        // 2. Kiểm tra nhân viên đã đăng ký khuôn mặt chưa
        FaceProfile myProfile = faceProfileRepository.findByEmployeeId(employee.getId()).orElse(null);
        if (myProfile == null || myProfile.getEmbeddingJson() == null) {
            return TimekeepingResponse.builder()
                    .success(false)
                    .employeeName(employee.getFullName())
                    .employeeCode(employee.getEmployeeCode())
                    .message("Bạn chưa đăng ký khuôn mặt. Vui lòng liên hệ Admin để đăng ký.")
                    .build();
        }

        // 3. Xác minh khuôn mặt khớp với tài khoản đang đăng nhập
        List<Double> savedVector = FaceVectorUtil.parseVector(myProfile.getEmbeddingJson());
        double similarity = FaceVectorUtil.cosineSimilarity(request.getFaceDescriptor(), savedVector);

        if (similarity < SIMILARITY_THRESHOLD) {
            return TimekeepingResponse.builder()
                    .success(false)
                    .employeeName(employee.getFullName())
                    .employeeCode(employee.getEmployeeCode())
                    .confidenceScore(Math.round(similarity * 100.0) / 100.0)
                    .message("Khuôn mặt không khớp với tài khoản đang đăng nhập (" +
                            String.format("%.0f%%", similarity * 100) + "). Vui lòng thử lại.")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 4. Kiểm tra Cooldown
        Optional<TimekeepingRecord> lastRecord = timekeepingRecordRepository
                .findFirstByEmployeeIdOrderByRecordTimeDesc(employee.getId());
        if (lastRecord.isPresent()) {
            long secondsSinceLastCheck = Duration.between(lastRecord.get().getRecordTime(), now).getSeconds();
            if (secondsSinceLastCheck < COOLDOWN_SECONDS) {
                return TimekeepingResponse.builder()
                        .success(false)
                        .message("Bạn vừa điểm danh cách đây " + secondsSinceLastCheck + " giây. Vui lòng chờ thêm.")
                        .employeeName(employee.getFullName())
                        .employeeCode(employee.getEmployeeCode())
                        .confidenceScore(similarity)
                        .build();
            }
        }

        // 5. Auto-detect CHECKIN vs CHECKOUT dựa trên trạng thái ngày hôm nay
        DailyAttendance daily = dailyAttendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElse(DailyAttendance.builder()
                        .employee(employee)
                        .workDate(today)
                        .shiftName("Ca làm việc DION")
                        .build());

        TimekeepingRecord.CheckType autoCheckType;
        if (daily.getCheckInTime() == null) {
            // Chưa check-in hôm nay → bắt buộc CHECKIN
            autoCheckType = TimekeepingRecord.CheckType.CHECKIN;
        } else if (daily.getCheckOutTime() == null) {
            // Đã check-in nhưng chưa check-out → CHECKOUT
            autoCheckType = TimekeepingRecord.CheckType.CHECKOUT;
        } else {
            // Đã check-in & check-out đủ rồi
            return TimekeepingResponse.builder()
                    .success(false)
                    .employeeName(employee.getFullName())
                    .employeeCode(employee.getEmployeeCode())
                    .message("Bạn đã hoàn thành chấm công hôm nay (Check-in & Check-out).")
                    .build();
        }

        // 6. Kiểm tra vị trí địa lý GPS
        boolean isLocationValid = true;
        if (request.getLatitude() != null && request.getLongitude() != null) {
            List<CompanyLocation> locations = companyLocationRepository.findByActiveTrue();
            if (!locations.isEmpty()) {
                isLocationValid = locations.stream().anyMatch(loc ->
                        GeoLocationUtil.isWithinAllowedRadius(
                                request.getLatitude(), request.getLongitude(),
                                loc.getLatitude(), loc.getLongitude(),
                                loc.getAllowedRadiusMeters()
                        )
                );
            }
        }

        // 7. Đánh giá trạng thái giờ làm
        TimekeepingRecord.AttendanceStatus attendanceStatus = evaluateAttendanceStatus(autoCheckType, now.toLocalTime());

        // 8. Lưu bản ghi thô
        TimekeepingRecord record = TimekeepingRecord.builder()
                .employee(employee)
                .checkType(autoCheckType)
                .recordTime(now)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationValid(isLocationValid)
                .confidenceScore(similarity)
                .status(attendanceStatus)
                .note(isLocationValid ? "Hợp lệ" : "Vị trí nằm ngoài bán kính công ty")
                .build();

        timekeepingRecordRepository.save(record);

        // 9. Cập nhật DailyAttendance
        String displayStatus = "Hợp lệ";

        if (autoCheckType == TimekeepingRecord.CheckType.CHECKIN) {
            daily.setCheckInTime(now);
            daily.setCheckInLat(request.getLatitude());
            daily.setCheckInLon(request.getLongitude());
            daily.setCheckInLocationValid(isLocationValid);
            daily.setCheckInConfidence(similarity);

            WorkingHourConfig config = workingHourConfigRepository.findFirstByActiveTrue().orElse(null);
            if (config != null && now.toLocalTime().isAfter(config.getStartTime().plusMinutes(config.getLateGraceMinutes()))) {
                daily.setStatus("Đi muộn");
                displayStatus = "Đi muộn";
            } else {
                daily.setStatus("Đúng giờ");
            }
        } else {
            // CHECKOUT
            daily.setCheckOutTime(now);
            daily.setCheckOutLat(request.getLatitude());
            daily.setCheckOutLon(request.getLongitude());
            daily.setCheckOutLocationValid(isLocationValid);
            daily.setCheckOutConfidence(similarity);

            if (daily.getCheckInTime() != null) {
                long minutes = Duration.between(daily.getCheckInTime(), now).toMinutes();
                daily.setTotalMinutesWorked(minutes);

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
                .checkType(autoCheckType.name())
                .time(now.format(timeFormatter))
                .confidenceScore(Math.round(similarity * 100.0) / 100.0)
                .status(displayStatus)
                .locationValid(isLocationValid)
                .message("Điểm danh " + (autoCheckType == TimekeepingRecord.CheckType.CHECKIN ? "Check-in" : "Check-out") + " thành công!")
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
