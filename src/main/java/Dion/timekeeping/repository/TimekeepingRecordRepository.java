package Dion.timekeeping.repository;

import Dion.timekeeping.entity.TimekeepingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimekeepingRecordRepository extends JpaRepository<TimekeepingRecord, Long> {
    List<TimekeepingRecord> findByEmployeeIdOrderByRecordTimeDesc(Long employeeId);

    // Tìm lần chấm công gần nhất của nhân viên để tránh quét liên tiếp trong vài giây/phút
    Optional<TimekeepingRecord> findFirstByEmployeeIdOrderByRecordTimeDesc(Long employeeId);

    List<TimekeepingRecord> findByRecordTimeBetweenOrderByRecordTimeDesc(LocalDateTime start, LocalDateTime end);
}
