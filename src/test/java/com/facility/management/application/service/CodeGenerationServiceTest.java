package com.facility.management.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenerationServiceTest {

    private CodeGenerationService codeGenerationService;

    @BeforeEach
    void setUp() {
        codeGenerationService = new CodeGenerationService();
    }

    @Test
    void testGenerateFacilityCreationCode() {
        String code = codeGenerationService.generateFacilityCreationCode(
            "TEST-GFRN", "Test Facility", "TEST-GFCID", "2024", "US");

        assertNotNull(code);
        assertTrue(code.contains("Facility facility = new Facility("));
        assertTrue(code.contains("\"TEST-GFRN\""));
        assertTrue(code.contains("\"Test Facility\""));
        assertTrue(code.contains("\"TEST-GFCID\""));
        assertTrue(code.contains("\"2024\""));
        assertTrue(code.contains("\"US\""));
        assertTrue(code.contains("facilityService.createFacility(facility)"));
    }

    @Test
    void testGenerateCustomerCreationCode() {
        String code = codeGenerationService.generateCustomerCreationCode(
            "TEST-CAG", "TEST-GFCID", "2024", "US");

        assertNotNull(code);
        assertTrue(code.contains("Customer customer = new Customer("));
        assertTrue(code.contains("\"TEST-CAG\""));
        assertTrue(code.contains("\"TEST-GFCID\""));
        assertTrue(code.contains("\"2024\""));
        assertTrue(code.contains("\"US\""));
        assertTrue(code.contains("customerService.createCustomer(customer)"));
    }

    @Test
    void testGenerateContractCreationCode() {
        BigDecimal directAmount = new BigDecimal("1000.00");
        BigDecimal contingentAmount = new BigDecimal("500.00");
        
        String code = codeGenerationService.generateContractCreationCode(
            "TXN-123", "TEST-GFRN", "TEST-GFCID", directAmount, contingentAmount, "2024");

        assertNotNull(code);
        assertTrue(code.contains("Contract contract = new Contract("));
        assertTrue(code.contains("\"TXN-123\""));
        assertTrue(code.contains("\"TEST-GFRN\""));
        assertTrue(code.contains("\"TEST-GFCID\""));
        assertTrue(code.contains("new BigDecimal(\"1000.00\")"));
        assertTrue(code.contains("new BigDecimal(\"500.00\")"));
        assertTrue(code.contains("\"2024\""));
        assertTrue(code.contains("contractService.createContract(contract)"));
    }

    @Test
    void testGenerateFacilityUpdateCode() {
        String code = codeGenerationService.generateFacilityUpdateCode(1L, "NEW-GFRN");

        assertNotNull(code);
        assertTrue(code.contains("facilityService.getFacilityById(1L)"));
        assertTrue(code.contains("facility.setGfrn(\"NEW-GFRN\")"));
        assertTrue(code.contains("facilityService.updateFacility(1L, facility)"));
    }

    @Test
    void testGenerateCustomerUpdateCode() {
        String code = codeGenerationService.generateCustomerUpdateCode(2L, "NEW-CAG");

        assertNotNull(code);
        assertTrue(code.contains("customerService.getCustomerById(2L)"));
        assertTrue(code.contains("customer.setCagId(\"NEW-CAG\")"));
        assertTrue(code.contains("customerService.updateCustomer(2L, customer)"));
    }

    @Test
    void testGenerateContractUpdateCode() {
        String code = codeGenerationService.generateContractUpdateCode(3L, "NEW-TXN");

        assertNotNull(code);
        assertTrue(code.contains("contractService.getContractById(3L)"));
        assertTrue(code.contains("contract.setTransactionId(\"NEW-TXN\")"));
        assertTrue(code.contains("contractService.updateContract(3L, contract)"));
    }

    @Test
    void testGenerateReportCode_FacilitySummary() {
        String code = codeGenerationService.generateReportCode("facility_summary");

        assertNotNull(code);
        assertTrue(code.contains("facilityService.getAllFacilities()"));
        assertTrue(code.contains("Collectors.groupingBy(Facility::getCountryOfRisk"));
    }

    @Test
    void testGenerateReportCode_CustomerSummary() {
        String code = codeGenerationService.generateReportCode("customer_summary");

        assertNotNull(code);
        assertTrue(code.contains("customerService.getAllCustomers()"));
        assertTrue(code.contains("Collectors.groupingBy(Customer::getCountryOfRisk"));
    }

    @Test
    void testGenerateReportCode_ContractSummary() {
        String code = codeGenerationService.generateReportCode("contract_summary");

        assertNotNull(code);
        assertTrue(code.contains("contractService.getAllContracts()"));
        assertTrue(code.contains("mapToDouble(Contract::getDirectAmount)"));
        assertTrue(code.contains("mapToDouble(Contract::getContingentAmount)"));
    }

    @Test
    void testGenerateReportCode_CustomReport() {
        String code = codeGenerationService.generateReportCode("custom_report");

        assertNotNull(code);
        assertTrue(code.contains("Custom report generation for: custom_report"));
    }
}
