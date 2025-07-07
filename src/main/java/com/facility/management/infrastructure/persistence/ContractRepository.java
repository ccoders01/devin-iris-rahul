package com.facility.management.infrastructure.persistence;

import com.facility.management.domain.contract.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByTransactionId(String transactionId);
    List<Contract> findByGfrn(String gfrn);
    List<Contract> findByGfcid(String gfcid);
    List<Contract> findByAccountingPeriod(String accountingPeriod);
    
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.facility LEFT JOIN FETCH c.customer")
    List<Contract> findAllWithRelations();
}
