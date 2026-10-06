package Dion.timekeeping.controller;

import Dion.timekeeping.entity.CompanyLocation;
import Dion.timekeeping.entity.Employee;
import Dion.timekeeping.entity.Role;
import Dion.timekeeping.entity.WorkingHourConfig;
import Dion.timekeeping.repository.CompanyLocationRepository;
import Dion.timekeeping.repository.EmployeeRepository;
import Dion.timekeeping.repository.FaceProfileRepository;
import Dion.timekeeping.repository.WorkingHourConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EmployeeRepository employeeRepository;
    private final FaceProfileRepository faceProfileRepository;
    private final WorkingHourConfigRepository workingHourConfigRepository;
    private final CompanyLocationRepository companyLocationRepository;
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
}
