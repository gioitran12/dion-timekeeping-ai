package Dion.timekeeping.controller;

import Dion.timekeeping.entity.CompanyLocation;
import Dion.timekeeping.entity.DailyAttendance;
import Dion.timekeeping.entity.Employee;
import Dion.timekeeping.entity.Role;
import Dion.timekeeping.entity.WorkingHourConfig;
import Dion.timekeeping.repository.CompanyLocationRepository;
import Dion.timekeeping.repository.DailyAttendanceRepository;
import Dion.timekeeping.repository.EmployeeRepository;
import Dion.timekeeping.repository.FaceProfileRepository;
import Dion.timekeeping.repository.WorkingHourConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeRepository employeeRepository;
    private final FaceProfileRepository faceProfileRepository;
    private final WorkingHourConfigRepository workingHourConfigRepository;
    private final CompanyLocationRepository companyLocationRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final PasswordEncoder passwordEncoder;

    // ==========================================
    // 1. QUẢN LÝ NHÂN SỰ (Personnel Management)
    // ==========================================
    @GetMapping("/employees")
    public String listEmployees(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        model.addAttribute("employees", employees);
        model.addAttribute("newEmployee", new Employee());
        return "admin/employees";
    }

    @PostMapping("/employees/save")
    public String saveEmployee(@ModelAttribute Employee employee, RedirectAttributes redirectAttributes) {
        try {
            if (employee.getId() == null) {
                // Tạo mới nhân viên
                if (employeeRepository.existsByEmployeeCode(employee.getEmployeeCode())) {
                    redirectAttributes.addFlashAttribute("error", "Mã nhân viên đã tồn tại!");
                    return "redirect:/admin/employees";
                }
                if (employeeRepository.existsByUsername(employee.getUsername())) {
                    redirectAttributes.addFlashAttribute("error", "Username đã tồn tại!");
                    return "redirect:/admin/employees";
                }
                // Mã hóa mật khẩu
                String rawPassword = (employee.getPassword() == null || employee.getPassword().isBlank()) 
                        ? "123456" : employee.getPassword();
                employee.setPassword(passwordEncoder.encode(rawPassword));
                employee.setActive(true);
            } else {
                // Cập nhật nhân viên cũ
                Employee existing = employeeRepository.findById(employee.getId()).orElseThrow();
                existing.setFullName(employee.getFullName());
                existing.setEmail(employee.getEmail());
                existing.setPhone(employee.getPhone());
                existing.setDepartment(employee.getDepartment());
                existing.setPosition(employee.getPosition());
                existing.setRole(employee.getRole());
                existing.setActive(employee.getActive());
                if (employee.getPassword() != null && !employee.getPassword().isBlank()) {
                    existing.setPassword(passwordEncoder.encode(employee.getPassword()));
                }
                employee = existing;
            }

            employeeRepository.save(employee);
            redirectAttributes.addFlashAttribute("success", "Lưu thông tin nhân viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @GetMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Xóa face profile nếu có
            faceProfileRepository.findByEmployeeId(id).ifPresent(faceProfileRepository::delete);
            employeeRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa nhân viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    // ==========================================
    // 2. CẤU HÌNH HỆ THỐNG (Set up the system)
    // ==========================================
    @GetMapping("/settings")
    public String systemSettings(Model model) {
        WorkingHourConfig workingHour = workingHourConfigRepository.findFirstByActiveTrue()
                .orElse(WorkingHourConfig.builder()
                        .shiftName("Ca Hành Chính")
                        .startTime(LocalTime.of(8, 30))
                        .endTime(LocalTime.of(17, 30))
                        .lateGraceMinutes(15)
                        .earlyLeaveGraceMinutes(0)
                        .active(true)
                        .build());

        List<CompanyLocation> locations = companyLocationRepository.findAll();

        model.addAttribute("workingHour", workingHour);
        model.addAttribute("locations", locations);
        model.addAttribute("newLocation", new CompanyLocation());
        return "admin/settings";
    }

    // Configure working hours
    @PostMapping("/settings/working-hours")
    public String saveWorkingHours(@ModelAttribute WorkingHourConfig config, RedirectAttributes redirectAttributes) {
        try {
            config.setActive(true);
            workingHourConfigRepository.save(config);
            redirectAttributes.addFlashAttribute("success", "Cập nhật cấu hình giờ làm việc thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    // Configure company map location
    @PostMapping("/settings/locations")
    public String saveCompanyLocation(@ModelAttribute CompanyLocation location, RedirectAttributes redirectAttributes) {
        try {
            location.setActive(true);
            companyLocationRepository.save(location);
            redirectAttributes.addFlashAttribute("success", "Lưu tọa độ vị trí công ty thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @GetMapping("/settings/locations/delete/{id}")
    public String deleteLocation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        companyLocationRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa địa điểm công ty!");
        return "redirect:/admin/settings";
    }

    // ==========================================
    // 3. ĐĂNG KÝ KHUÔN MẶT (Register a face)
    // ==========================================
    @GetMapping("/register-face")
    public String registerFacePage(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        model.addAttribute("employees", employees);
        return "admin/register-face";
    }

    // ==========================================
    // 4. BÁO CÁO & THỐNG KÊ (Reports & Statistics)
    // ==========================================
    @GetMapping("/reports")
    public String reportsPage(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long employeeId,
            Model model) {

        LocalDate now = LocalDate.now();
        int selectedYear = (year != null) ? year : now.getYear();
        int selectedMonth = (month != null) ? month : now.getMonthValue();

        // Lấy dữ liệu chấm công theo tháng
        List<DailyAttendance> records;
        if (employeeId != null) {
            records = dailyAttendanceRepository.findByEmployeeAndYearAndMonth(employeeId, selectedYear, selectedMonth);
        } else {
            records = dailyAttendanceRepository.findByYearAndMonth(selectedYear, selectedMonth);
        }

        // Thống kê tổng hợp
        long totalRecords = records.size();
        long onTime = records.stream().filter(d -> "Đúng giờ".equals(d.getStatus()) || "Hợp lệ".equals(d.getStatus())).count();
        long late = records.stream().filter(d -> d.getStatus() != null && d.getStatus().contains("muộn")).count();
        long insufficient = records.stream().filter(d -> d.getStatus() != null && d.getStatus().contains("Thiếu")).count();
        long absent = records.stream().filter(d -> d.getCheckInTime() == null).count();

        // Thống kê trung bình giờ làm
        double avgMinutes = records.stream()
                .filter(d -> d.getTotalMinutesWorked() != null && d.getTotalMinutesWorked() > 0)
                .mapToLong(DailyAttendance::getTotalMinutesWorked)
                .average().orElse(0);

        // Dữ liệu theo nhân viên (group by)
        Map<String, Long> byEmployee = records.stream()
                .filter(d -> d.getEmployee() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getEmployee().getFullName(),
                        Collectors.counting()
                ));

        List<Employee> employees = employeeRepository.findAll();

        model.addAttribute("records", records);
        model.addAttribute("employees", employees);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("totalRecords", totalRecords);
        model.addAttribute("onTime", onTime);
        model.addAttribute("late", late);
        model.addAttribute("insufficient", insufficient);
        model.addAttribute("absent", absent);
        model.addAttribute("avgMinutes", (long) avgMinutes);
        model.addAttribute("byEmployee", byEmployee);

        return "admin/reports";
    }

    @GetMapping("/reports/export")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> exportReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long employeeId) {

        LocalDate now = LocalDate.now();
        int selectedYear = (year != null) ? year : now.getYear();
        int selectedMonth = (month != null) ? month : now.getMonthValue();

        List<DailyAttendance> records = (employeeId != null)
                ? dailyAttendanceRepository.findByEmployeeAndYearAndMonth(employeeId, selectedYear, selectedMonth)
                : dailyAttendanceRepository.findByYearAndMonth(selectedYear, selectedMonth);

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM để Excel hiển thị tiếng Việt không bị lỗi font
        sb.append('\ufeff');
        sb.append("Mã NV,Họ và tên,Phòng ban,Ngày làm việc,Giờ vào (Check-in),Giờ ra (Check-out),Tổng giờ làm,Trạng thái,Vị trí\n");

        for (DailyAttendance r : records) {
            String empCode = r.getEmployee() != null ? r.getEmployee().getEmployeeCode() : "";
            String empName = r.getEmployee() != null ? r.getEmployee().getFullName() : "";
            String dept = r.getEmployee() != null ? r.getEmployee().getDepartment() : "";
            String date = r.getWorkDate() != null ? r.getWorkDate().toString() : "";
            String inTime = r.getCheckInTime() != null ? r.getCheckInTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) : "--";
            String outTime = r.getCheckOutTime() != null ? r.getCheckOutTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) : "--";
            String total = (r.getTotalMinutesWorked() != null && r.getTotalMinutesWorked() > 0)
                    ? (r.getTotalMinutesWorked() / 60) + "h" + (r.getTotalMinutesWorked() % 60) + "p" : "--";
            String status = r.getStatus() != null ? r.getStatus() : "";
            String loc = (r.getCheckInLocationValid() != null && r.getCheckInLocationValid()) ? "Hợp lệ" : "Ngoài khu vực";

            sb.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    empCode, empName, dept, date, inTime, outTime, total, status, loc));
        }

        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fileName = String.format("Bao_Cao_Cham_Cong_Thang_%02d_%d.csv", selectedMonth, selectedYear);

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
