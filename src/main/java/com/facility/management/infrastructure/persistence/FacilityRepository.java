package com.facility.management.infrastructure.persistence;

import com.facility.management.domain.facility.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {
    Optional<Facility> findByGfrn(String gfrn);
    List<Facility> findByCountryOfRisk(String countryOfRisk);
    List<Facility> findByAccountingPeriod(String accountingPeriod);
    List<Facility> findByGfcid(String gfcid);
}
