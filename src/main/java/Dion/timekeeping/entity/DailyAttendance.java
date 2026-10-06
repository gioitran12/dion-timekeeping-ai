package Dion.timekeeping.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_attendances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate workDate; // Ngày làm việc (VD: 2026-10-06)

    @Column(length = 100, columnDefinition = "NVARCHAR(100)")
    private String shiftName; // Tên ca (VD: Ca làm việc DION 09:00 - 18:30)

    // CHECK-IN INFO
    private LocalDateTime checkInTime;
    private Double checkInLat;
    private Double checkInLon;
    private Boolean checkInLocationValid;
    private Double checkInConfidence;

    // CHECK-OUT INFO
    private LocalDateTime checkOutTime;
    private Double checkOutLat;
    private Double checkOutLon;
    private Boolean checkOutLocationValid;
    private Double checkOutConfidence;

    // TỔNG KẾT
    private Long totalMinutesWorked; // Tổng số phút đã làm trong ngày (tính từ checkIn đến checkOut)

    @Column(length = 50, columnDefinition = "NVARCHAR(50)")
    private String status; // "Hợp lệ", "Đi muộn", "Về sớm", "Thiếu thời gian làm việc", "Đang trong ca"

    @Column(length = 255, columnDefinition = "NVARCHAR(255)")
    private String note;
}
