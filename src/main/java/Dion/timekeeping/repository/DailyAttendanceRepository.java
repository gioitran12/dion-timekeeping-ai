package Dion.timekeeping.repository;

import Dion.timekeeping.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long> {
    Optional<DailyAttendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
    List<DailyAttendance> findByEmployeeIdOrderByWorkDateDesc(Long employeeId);
    List<DailyAttendance> findTop30ByEmployeeIdOrderByWorkDateDesc(Long employeeId);

    @Query("SELECT d FROM DailyAttendance d WHERE YEAR(d.workDate) = :year AND MONTH(d.workDate) = :month ORDER BY d.workDate DESC, d.employee.fullName ASC")
    List<DailyAttendance> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT d FROM DailyAttendance d WHERE d.employee.id = :empId AND YEAR(d.workDate) = :year AND MONTH(d.workDate) = :month ORDER BY d.workDate DESC")
    List<DailyAttendance> findByEmployeeAndYearAndMonth(@Param("empId") Long empId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT d FROM DailyAttendance d WHERE d.workDate BETWEEN :from AND :to ORDER BY d.workDate DESC")
    List<DailyAttendance> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
