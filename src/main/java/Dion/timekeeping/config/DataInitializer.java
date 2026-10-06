package Dion.timekeeping.config;

import Dion.timekeeping.entity.CompanyLocation;
import Dion.timekeeping.entity.Employee;
import Dion.timekeeping.entity.Role;
import Dion.timekeeping.entity.WorkingHourConfig;
import Dion.timekeeping.repository.CompanyLocationRepository;
import Dion.timekeeping.repository.EmployeeRepository;
import Dion.timekeeping.repository.WorkingHourConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final WorkingHourConfigRepository workingHourConfigRepository;
    private final CompanyLocationRepository companyLocationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Khởi tạo hoặc sửa tên tiếng Việt tài khoản ADMIN
        Employee admin = employeeRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = Employee.builder()
                    .employeeCode("ADMIN001")
                    .fullName("Quản Trị Viên")
                    .department("Phòng Kỹ Thuật · Ban Quản Trị")
                    .position("System Administrator")
                    .email("admin@dion.vn")
                    .phone("0900000001")
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .active(true)
                    .build();
            employeeRepository.save(admin);
            log.info(">>> Đã khởi tạo tài khoản ADMIN mặc định: username=admin, password=admin123");
        } else {
            // Sửa đè lại tiếng Việt chuẩn
            admin.setFullName("Quản Trị Viên");
            admin.setDepartment("Phòng Kỹ Thuật · Ban Quản Trị");
            admin.setPosition("System Administrator");
            employeeRepository.save(admin);
            log.info(">>> Đã cập nhật lại tên tiếng Việt cho ADMIN: Quản Trị Viên");
        }

        // 2. Khởi tạo hoặc sửa tên tiếng Việt tài khoản Nhân viên Trần Giỏi
        Employee emp = employeeRepository.findByUsername("gioitd").orElse(null);
        if (emp == null) {
            emp = Employee.builder()
                    .employeeCode("DION001")
                    .fullName("Trần Giỏi")
                    .department("Nhân viên · AI")
                    .position("AI Engineer")
                    .email("gioi.tran@dion.vn")
                    .phone("0987654321")
                    .username("gioitd")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.EMPLOYEE)
                    .active(true)
                    .build();
            employeeRepository.save(emp);
            log.info(">>> Đã khởi tạo tài khoản Nhân viên: username=gioitd, password=123456");
        } else {
            emp.setFullName("Trần Giỏi");
            emp.setDepartment("Nhân viên · AI");
            emp.setPosition("AI Engineer");
            employeeRepository.save(emp);
            log.info(">>> Đã cập nhật lại tên tiếng Việt cho Employee: Trần Giỏi");
        }

        // 3. Khởi tạo cấu hình giờ làm việc mặc định
        if (workingHourConfigRepository.findAll().isEmpty()) {
            WorkingHourConfig shift = WorkingHourConfig.builder()
                    .shiftName("Ca Hành Chính")
                    .startTime(LocalTime.of(8, 30))
                    .endTime(LocalTime.of(17, 30))
                    .lateGraceMinutes(15)
                    .earlyLeaveGraceMinutes(0)
                    .active(true)
                    .build();
            workingHourConfigRepository.save(shift);
            log.info(">>> Đã khởi tạo cấu hình ca làm việc mặc định: 08:30 - 17:30");
        }

        // 4. Khởi tạo cấu hình tọa độ văn phòng công ty mặc định
        if (companyLocationRepository.findAll().isEmpty()) {
            CompanyLocation loc = CompanyLocation.builder()
                    .name("Văn phòng DION Digital Innovation")
                    .latitude(21.028511)
                    .longitude(105.854444)
                    .allowedRadiusMeters(200.0)
                    .active(true)
                    .build();
            companyLocationRepository.save(loc);
            log.info(">>> Đã khởi tạo tọa độ văn phòng mặc định với bán kính 200m");
        }
    }
}
