package com.facility.management.infrastructure.persistence;

import com.facility.management.domain.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCagId(String cagId);
    List<Customer> findByCountryOfRisk(String countryOfRisk);
    List<Customer> findByAccountingPeriod(String accountingPeriod);
    List<Customer> findByGfcid(String gfcid);
}
