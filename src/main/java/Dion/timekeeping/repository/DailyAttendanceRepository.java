package Dion.timekeeping.repository;

import Dion.timekeeping.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long> {
    Optional<DailyAttendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
    List<DailyAttendance> findByEmployeeIdOrderByWorkDateDesc(Long employeeId);
    List<DailyAttendance> findTop30ByEmployeeIdOrderByWorkDateDesc(Long employeeId);
}
