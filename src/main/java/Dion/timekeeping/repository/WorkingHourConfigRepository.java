package Dion.timekeeping.repository;

import Dion.timekeeping.entity.WorkingHourConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkingHourConfigRepository extends JpaRepository<WorkingHourConfig, Long> {
    Optional<WorkingHourConfig> findFirstByActiveTrue();
}
