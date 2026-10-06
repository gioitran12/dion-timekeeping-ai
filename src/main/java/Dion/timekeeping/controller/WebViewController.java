package Dion.timekeeping.controller;

import Dion.timekeeping.entity.DailyAttendance;
import Dion.timekeeping.entity.Employee;
import Dion.timekeeping.entity.TimekeepingRecord;
import Dion.timekeeping.repository.DailyAttendanceRepository;
import Dion.timekeeping.repository.EmployeeRepository;
import Dion.timekeeping.repository.TimekeepingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebViewController {

    private final EmployeeRepository employeeRepository;
    private final TimekeepingRecordRepository timekeepingRecordRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * Dashboard tổng quan sau khi đăng nhập
     */
    @GetMapping({"/", "/home", "/dashboard"})
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "gioitd";
        Employee currentEmployee = employeeRepository.findByUsername(username)
                .orElseGet(() -> employeeRepository.findAll().stream().findFirst().orElse(null));

        List<TimekeepingRecord> recentRecords = List.of();
        TimekeepingRecord latestRecord = null;

        if (currentEmployee != null) {
            recentRecords = timekeepingRecordRepository.findByEmployeeIdOrderByRecordTimeDesc(currentEmployee.getId());
            latestRecord = recentRecords.isEmpty() ? null : recentRecords.get(0);
        }

        model.addAttribute("employee", currentEmployee);
        model.addAttribute("recentRecords", recentRecords);
        model.addAttribute("latestRecord", latestRecord);
        model.addAttribute("currentDateStr", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy")));
        model.addAttribute("currentTimeStr", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        return "dashboard";
    }

    /**
     * Màn hình AI Webcam Kiosk Chấm Công
     */
    @GetMapping("/timekeeping/scan")
    public String timekeepingScannerPage(Model model, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Employee currentEmployee = username != null ? employeeRepository.findByUsername(username).orElse(null) : null;
        model.addAttribute("employee", currentEmployee);
        model.addAttribute("currentDateStr", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy")));
        model.addAttribute("currentTimeStr", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        return "timekeeping/webcam";
    }

    /**
     * Màn hình Lịch làm việc & Lịch sử Chấm công cá nhân (DION Workplace Attendance History)
     */
    @GetMapping({"/timekeeping/history", "/attendance/history"})
    public String timekeepingHistory(Model model, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "gioitd";
        Employee employee = employeeRepository.findByUsername(username).orElseGet(() ->
                employeeRepository.findAll().stream().findFirst().orElse(null));

        List<DailyAttendance> dailyList = List.of();
        DailyAttendance todayAttendance = null;
        long totalMinutesMonth = 0;
        long lateCount = 0;
        long completedDays = 0;

        if (employee != null) {
            dailyList = dailyAttendanceRepository.findByEmployeeIdOrderByWorkDateDesc(employee.getId());
            todayAttendance = dailyAttendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), LocalDate.now()).orElse(null);

            for (DailyAttendance d : dailyList) {
                if (d.getTotalMinutesWorked() != null) {
                    totalMinutesMonth += d.getTotalMinutesWorked();
                }
                if ("Đi muộn".equals(d.getStatus())) {
                    lateCount++;
                }
                if (d.getCheckOutTime() != null) {
                    completedDays++;
                }
            }
        }

        double totalHours = Math.round((totalMinutesMonth / 60.0) * 10.0) / 10.0;
        int onTimePercentage = dailyList.isEmpty() ? 100 : (int) Math.round(((double) (dailyList.size() - lateCount) / dailyList.size()) * 100);

        model.addAttribute("employee", employee);
        model.addAttribute("dailyList", dailyList);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("presentDays", dailyList.size());
        model.addAttribute("completedCheckouts", completedDays);
        model.addAttribute("totalHours", totalHours);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("onTimePercentage", onTimePercentage);
        model.addAttribute("currentTimeStr", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        model.addAttribute("currentDateStr", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        return "timekeeping/history";
    }
}
