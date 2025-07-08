package com.facility.management.infrastructure;

import com.facility.management.domain.contract.Contract;
import com.facility.management.domain.customer.Customer;
import com.facility.management.domain.facility.Facility;
import com.facility.management.infrastructure.persistence.ContractRepository;
import com.facility.management.infrastructure.persistence.CustomerRepository;
import com.facility.management.infrastructure.persistence.FacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final FacilityRepository facilityRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    
    @Override
    public void run(String... args) throws Exception {
        if (facilityRepository.count() == 0) {
            initializeSampleData();
        }
    }
    
    private void initializeSampleData() {
        log.info("Initializing sample data...");
        
        Facility facility1 = new Facility("GFRN001", "2024-Q1", "Main Manufacturing Facility", "GFCID001", "USA");
        Facility facility2 = new Facility("GFRN002", "2024-Q1", "European Distribution Center", "GFCID002", "Germany");
        Facility facility3 = new Facility("GFRN003", "2024-Q2", "Asia Pacific Hub", "GFCID003", "Singapore");
        Facility facility4 = new Facility("GFRN004", "2024-Q2", "Research & Development Center", "GFCID004", "Canada");
        Facility facility5 = new Facility("GFRN005", "2024-Q3", "Latin America Operations", "GFCID005", "Brazil");
        
        facilityRepository.save(facility1);
        facilityRepository.save(facility2);
        facilityRepository.save(facility3);
        facilityRepository.save(facility4);
        facilityRepository.save(facility5);
        
        Customer customer1 = new Customer("CAG001", "GFCID001", "2024-Q1", "USA");
        Customer customer2 = new Customer("CAG002", "GFCID002", "2024-Q1", "Germany");
        Customer customer3 = new Customer("CAG003", "GFCID003", "2024-Q2", "Singapore");
        Customer customer4 = new Customer("CAG004", "GFCID004", "2024-Q2", "Canada");
        Customer customer5 = new Customer("CAG005", "GFCID005", "2024-Q3", "Brazil");
        Customer customer6 = new Customer("CAG006", "GFCID006", "2024-Q3", "Mexico");
        
        customerRepository.save(customer1);
        customerRepository.save(customer2);
        customerRepository.save(customer3);
        customerRepository.save(customer4);
        customerRepository.save(customer5);
        customerRepository.save(customer6);
        
        Contract contract1 = new Contract("TXN001", "GFRN001", "GFCID001", 
                new BigDecimal("1000000.00"), new BigDecimal("500000.00"), "2024-Q1");
        Contract contract2 = new Contract("TXN002", "GFRN002", "GFCID002", 
                new BigDecimal("750000.00"), new BigDecimal("250000.00"), "2024-Q1");
        Contract contract3 = new Contract("TXN003", "GFRN003", "GFCID003", 
                new BigDecimal("1200000.00"), new BigDecimal("600000.00"), "2024-Q2");
        Contract contract4 = new Contract("TXN004", "GFRN004", "GFCID004", 
                new BigDecimal("800000.00"), new BigDecimal("400000.00"), "2024-Q2");
        Contract contract5 = new Contract("TXN005", "GFRN005", "GFCID005", 
                new BigDecimal("950000.00"), new BigDecimal("475000.00"), "2024-Q3");
        Contract contract6 = new Contract("TXN006", "GFRN001", "GFCID006", 
                new BigDecimal("1100000.00"), new BigDecimal("550000.00"), "2024-Q3");
        
        contractRepository.save(contract1);
        contractRepository.save(contract2);
        contractRepository.save(contract3);
        contractRepository.save(contract4);
        contractRepository.save(contract5);
        contractRepository.save(contract6);
        
        log.info("Sample data initialization completed!");
        log.info("Created {} facilities", facilityRepository.count());
        log.info("Created {} customers", customerRepository.count());
        log.info("Created {} contracts", contractRepository.count());
    }
}
