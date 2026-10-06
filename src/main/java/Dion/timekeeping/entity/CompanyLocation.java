package Dion.timekeeping.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String name;

    @Column(nullable = false)
    private Double latitude; // Vĩ độ (VD: 21.028511)

    @Column(nullable = false)
    private Double longitude; // Kinh độ (VD: 105.854444)

    @Column(nullable = false)
    @Builder.Default
    private Double allowedRadiusMeters = 100.0; // Bán kính cho phép chấm công (mét)

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
