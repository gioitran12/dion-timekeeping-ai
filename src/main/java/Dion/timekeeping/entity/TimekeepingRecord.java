package Dion.timekeeping.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "timekeeping_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimekeepingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckType checkType; // CHECKIN hoặc CHECKOUT

    @Column(nullable = false)
    private LocalDateTime recordTime;

    private Double latitude; // GPS thiết bị lúc chấm công
    private Double longitude;

    @Builder.Default
    private Boolean locationValid = true; // Có nằm trong bán kính map công ty không

    private Double confidenceScore; // Điểm nhận diện khuôn mặt (VD: 0.92)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttendanceStatus status; // ON_TIME, LATE, EARLY_LEAVE

    @Column(length = 255)
    private String note;

    public enum CheckType {
        CHECKIN,
        CHECKOUT
    }

    public enum AttendanceStatus {
        ON_TIME,
        LATE,
        EARLY_LEAVE
    }
}
