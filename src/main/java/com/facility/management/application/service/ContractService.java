package com.facility.management.application.service;

import com.facility.management.domain.contract.Contract;
import com.facility.management.infrastructure.persistence.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {
    
    private final ContractRepository contractRepository;
    
    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }
    
    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }
    
    public Optional<Contract> getContractByTransactionId(String transactionId) {
        return contractRepository.findByTransactionId(transactionId);
    }
    
    public Contract createContract(Contract contract) {
        return contractRepository.save(contract);
    }
    
    public Contract updateContract(Long id, Contract contractDetails) {
        return contractRepository.findById(id)
                .map(contract -> {
                    contract.setTransactionId(contractDetails.getTransactionId());
                    contract.setGfrn(contractDetails.getGfrn());
                    contract.setGfcid(contractDetails.getGfcid());
                    contract.setDirectAmount(contractDetails.getDirectAmount());
                    contract.setContingentAmount(contractDetails.getContingentAmount());
                    contract.setAccountingPeriod(contractDetails.getAccountingPeriod());
                    return contractRepository.save(contract);
                })
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
    }
    
    public void deleteContract(Long id) {
        contractRepository.deleteById(id);
    }
    
    public List<Contract> getContractsByGfrn(String gfrn) {
        return contractRepository.findByGfrn(gfrn);
    }
    
    public List<Contract> getContractsByAccountingPeriod(String accountingPeriod) {
        return contractRepository.findByAccountingPeriod(accountingPeriod);
    }
}
