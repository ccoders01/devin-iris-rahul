package com.facility.management.presentation.controller;

import com.facility.management.application.service.ContractService;
import com.facility.management.domain.contract.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContractController {
    
    private final ContractService contractService;
    
    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts() {
        List<Contract> contracts = contractService.getAllContracts();
        return ResponseEntity.ok(contracts);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        return contractService.getContractById(id)
                .map(contract -> ResponseEntity.ok(contract))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<Contract> getContractByTransactionId(@PathVariable String transactionId) {
        return contractService.getContractByTransactionId(transactionId)
                .map(contract -> ResponseEntity.ok(contract))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Contract> createContract(@Valid @RequestBody Contract contract) {
        try {
            Contract createdContract = contractService.createContract(contract);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdContract);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(@PathVariable Long id, @Valid @RequestBody Contract contractDetails) {
        try {
            Contract updatedContract = contractService.updateContract(id, contractDetails);
            return ResponseEntity.ok(updatedContract);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        try {
            contractService.deleteContract(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/gfrn/{gfrn}")
    public ResponseEntity<List<Contract>> getContractsByGfrn(@PathVariable String gfrn) {
        List<Contract> contracts = contractService.getContractsByGfrn(gfrn);
        return ResponseEntity.ok(contracts);
    }
    
    @GetMapping("/period/{accountingPeriod}")
    public ResponseEntity<List<Contract>> getContractsByAccountingPeriod(@PathVariable String accountingPeriod) {
        List<Contract> contracts = contractService.getContractsByAccountingPeriod(accountingPeriod);
        return ResponseEntity.ok(contracts);
    }
}
