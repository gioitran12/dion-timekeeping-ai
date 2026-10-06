package Dion.timekeeping.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String employeeCode; // Mã nhân viên ví dụ: NV001

    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 100, columnDefinition = "NVARCHAR(100)")
    private String department;

    @Column(length = 100, columnDefinition = "NVARCHAR(100)")
    private String position;

    @Column(unique = true, length = 50)
    private String username;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private Role role = Role.EMPLOYEE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
