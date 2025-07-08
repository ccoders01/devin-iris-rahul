package com.facility.management.testdata;

import com.facility.management.domain.contract.Contract;
import com.facility.management.domain.customer.Customer;
import com.facility.management.domain.facility.Facility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SyntheticDataTest {

    @Autowired
    private CsvDataLoader csvDataLoader;

    @Test
    public void testLoadFacilities() throws IOException {
        List<Facility> facilities = csvDataLoader.loadFacilities();
        
        assertEquals(20, facilities.size());
        
        Facility firstFacility = facilities.get(0);
        assertEquals("GFRN001", firstFacility.getGfrn());
        assertEquals("2024-Q1", firstFacility.getAccountingPeriod());
        assertEquals("Main Manufacturing Facility", firstFacility.getName());
        assertEquals("GFCID001", firstFacility.getGfcid());
        assertEquals("USA", firstFacility.getCountryOfRisk());
    }

    @Test
    public void testLoadCustomers() throws IOException {
        List<Customer> customers = csvDataLoader.loadCustomers();
        
        assertEquals(25, customers.size());
        
        Customer firstCustomer = customers.get(0);
        assertEquals("CAG001", firstCustomer.getCagId());
        assertEquals("GFCID001", firstCustomer.getGfcid());
        assertEquals("2024-Q1", firstCustomer.getAccountingPeriod());
        assertEquals("USA", firstCustomer.getCountryOfRisk());
    }

    @Test
    public void testLoadContracts() throws IOException {
        List<Contract> contracts = csvDataLoader.loadContracts();
        
        assertEquals(35, contracts.size());
        
        Contract firstContract = contracts.get(0);
        assertEquals("TXN001", firstContract.getTransactionId());
        assertEquals("GFRN001", firstContract.getGfrn());
        assertEquals("GFCID001", firstContract.getGfcid());
        assertEquals(new BigDecimal("2500000.00"), firstContract.getDirectAmount());
        assertEquals(new BigDecimal("1250000.00"), firstContract.getContingentAmount());
        assertEquals("2024-Q1", firstContract.getAccountingPeriod());
    }

    @Test
    public void testReferentialIntegrity() throws IOException {
        List<Facility> facilities = csvDataLoader.loadFacilities();
        List<Customer> customers = csvDataLoader.loadCustomers();
        List<Contract> contracts = csvDataLoader.loadContracts();
        
        for (Customer customer : customers) {
            boolean facilityExists = facilities.stream()
                .anyMatch(f -> f.getGfcid().equals(customer.getGfcid()));
            assertTrue(facilityExists, 
                "Customer " + customer.getCagId() + " references non-existent facility " + customer.getGfcid());
        }
        
        for (Contract contract : contracts) {
            boolean facilityExists = facilities.stream()
                .anyMatch(f -> f.getGfrn().equals(contract.getGfrn()));
            assertTrue(facilityExists, 
                "Contract " + contract.getTransactionId() + " references non-existent facility " + contract.getGfrn());
            
            boolean customerExists = customers.stream()
                .anyMatch(c -> c.getGfcid().equals(contract.getGfcid()));
            assertTrue(customerExists, 
                "Contract " + contract.getTransactionId() + " references non-existent customer facility " + contract.getGfcid());
        }
    }

    @Test
    public void testDataVariety() throws IOException {
        List<Facility> facilities = csvDataLoader.loadFacilities();
        List<Customer> customers = csvDataLoader.loadCustomers();
        List<Contract> contracts = csvDataLoader.loadContracts();
        
        assertTrue(facilities.stream().anyMatch(f -> f.getAccountingPeriod().equals("2023-Q1")));
        assertTrue(facilities.stream().anyMatch(f -> f.getAccountingPeriod().equals("2024-Q4")));
        
        assertTrue(facilities.stream().anyMatch(f -> f.getCountryOfRisk().equals("USA")));
        assertTrue(facilities.stream().anyMatch(f -> f.getCountryOfRisk().equals("Germany")));
        assertTrue(facilities.stream().anyMatch(f -> f.getCountryOfRisk().equals("Singapore")));
        
        assertTrue(contracts.stream().anyMatch(c -> c.getDirectAmount().compareTo(new BigDecimal("100000")) < 0));
        assertTrue(contracts.stream().anyMatch(c -> c.getDirectAmount().compareTo(new BigDecimal("5000000")) > 0));
        
        assertTrue(contracts.stream().anyMatch(c -> c.getContingentAmount().equals(BigDecimal.ZERO)));
    }
}
