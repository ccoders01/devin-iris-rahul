package com.facility.management.presentation.controller;

import com.facility.management.application.service.CustomerService;
import com.facility.management.domain.customer.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerController {
    
    private final CustomerService customerService;
    
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id)
                .map(customer -> ResponseEntity.ok(customer))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/cag/{cagId}")
    public ResponseEntity<Customer> getCustomerByCagId(@PathVariable String cagId) {
        return customerService.getCustomerByCagId(cagId)
                .map(customer -> ResponseEntity.ok(customer))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        try {
            Customer createdCustomer = customerService.createCustomer(customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @Valid @RequestBody Customer customerDetails) {
        try {
            Customer updatedCustomer = customerService.updateCustomer(id, customerDetails);
            return ResponseEntity.ok(updatedCustomer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/country/{countryOfRisk}")
    public ResponseEntity<List<Customer>> getCustomersByCountryOfRisk(@PathVariable String countryOfRisk) {
        List<Customer> customers = customerService.getCustomersByCountryOfRisk(countryOfRisk);
        return ResponseEntity.ok(customers);
    }
    
    @GetMapping("/period/{accountingPeriod}")
    public ResponseEntity<List<Customer>> getCustomersByAccountingPeriod(@PathVariable String accountingPeriod) {
        List<Customer> customers = customerService.getCustomersByAccountingPeriod(accountingPeriod);
        return ResponseEntity.ok(customers);
    }
}
