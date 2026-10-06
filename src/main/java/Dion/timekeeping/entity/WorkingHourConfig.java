package Dion.timekeeping.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "working_hour_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingHourConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String shiftName; // Ví dụ: "Ca Hành Chính", "Ca Sáng"

    @Column(nullable = false)
    private LocalTime startTime; // Ví dụ: 08:30:00

    @Column(nullable = false)
    private LocalTime endTime; // Ví dụ: 17:30:00

    @Builder.Default
    private Integer lateGraceMinutes = 15; // Cho phép trễ tối đa bao nhiêu phút không tính phạt

    @Builder.Default
    private Integer earlyLeaveGraceMinutes = 0; // Cho phép về sớm tối đa bao nhiêu phút

    @Builder.Default
    private Boolean active = true;
}
