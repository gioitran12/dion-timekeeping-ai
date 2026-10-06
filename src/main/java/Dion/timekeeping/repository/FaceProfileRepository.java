package Dion.timekeeping.repository;

import Dion.timekeeping.entity.FaceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaceProfileRepository extends JpaRepository<FaceProfile, Long> {
    Optional<FaceProfile> findByEmployeeId(Long employeeId);

    // Lấy tất cả face profile của các nhân viên đang active để so khớp nhanh
    @Query("SELECT fp FROM FaceProfile fp JOIN FETCH fp.employee e WHERE e.active = true")
    List<FaceProfile> findAllActiveProfilesWithEmployee();
}
