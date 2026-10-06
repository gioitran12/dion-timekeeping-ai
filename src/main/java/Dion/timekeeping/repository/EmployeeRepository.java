package Dion.timekeeping.repository;

import Dion.timekeeping.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    Optional<Employee> findByUsername(String username);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByUsername(String username);
}
