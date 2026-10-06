package Dion.timekeeping.repository;

import Dion.timekeeping.entity.CompanyLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, Long> {
    List<CompanyLocation> findByActiveTrue();
}
