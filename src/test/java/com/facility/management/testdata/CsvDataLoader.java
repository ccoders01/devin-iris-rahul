package com.facility.management.testdata;

import com.facility.management.domain.contract.Contract;
import com.facility.management.domain.customer.Customer;
import com.facility.management.domain.facility.Facility;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvDataLoader {

    public List<Facility> loadFacilities() throws IOException {
        List<Facility> facilities = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("testdata/facilities.csv");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                facilities.add(new Facility(
                    fields[0], // GFRN
                    fields[1], // AccountingPeriod
                    fields[2], // Name
                    fields[3], // GFCID
                    fields[4]  // CountryOfRisk
                ));
            }
        }
        return facilities;
    }

    public List<Customer> loadCustomers() throws IOException {
        List<Customer> customers = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("testdata/customers.csv");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                customers.add(new Customer(
                    fields[0], // CAG_ID
                    fields[1], // GFCID
                    fields[2], // AccountingPeriod
                    fields[3]  // CountryOfRisk
                ));
            }
        }
        return customers;
    }

    public List<Contract> loadContracts() throws IOException {
        List<Contract> contracts = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("testdata/contracts.csv");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                contracts.add(new Contract(
                    fields[0], // TransactionID
                    fields[1], // GFRN
                    fields[2], // GFCID
                    new BigDecimal(fields[3]), // DirectAmount
                    new BigDecimal(fields[4]), // ContingentAmount
                    fields[5]  // AccountingPeriod
                ));
            }
        }
        return contracts;
    }
}
