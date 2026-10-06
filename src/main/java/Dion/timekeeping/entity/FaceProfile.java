package Dion.timekeeping.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "face_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    /**
     * Vector embedding 128 chiều biểu diễn đặc trưng khuôn mặt (lưu dạng chuỗi JSON float array, ví dụ: "[0.123, -0.456, ...]")
     */
    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String embeddingJson;

    @Column(nullable = false)
    @Builder.Default
    private Integer sampleCount = 1;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
