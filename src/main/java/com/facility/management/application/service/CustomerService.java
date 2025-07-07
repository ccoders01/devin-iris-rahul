package com.facility.management.application.service;

import com.facility.management.domain.customer.Customer;
import com.facility.management.infrastructure.persistence.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }
    
    public Optional<Customer> getCustomerByCagId(String cagId) {
        return customerRepository.findByCagId(cagId);
    }
    
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
    
    public Customer updateCustomer(Long id, Customer customerDetails) {
        return customerRepository.findById(id)
                .map(customer -> {
                    customer.setCagId(customerDetails.getCagId());
                    customer.setGfcid(customerDetails.getGfcid());
                    customer.setAccountingPeriod(customerDetails.getAccountingPeriod());
                    customer.setCountryOfRisk(customerDetails.getCountryOfRisk());
                    return customerRepository.save(customer);
                })
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }
    
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }
    
    public List<Customer> getCustomersByCountryOfRisk(String countryOfRisk) {
        return customerRepository.findByCountryOfRisk(countryOfRisk);
    }
    
    public List<Customer> getCustomersByAccountingPeriod(String accountingPeriod) {
        return customerRepository.findByAccountingPeriod(accountingPeriod);
    }
}
