package com.facility.management.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CodeGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeGenerationService.class);
    
    public String generateFacilityCreationCode(String gfrn, String name, String gfcid, String accountingPeriod, String countryOfRisk) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated facility creation code\n");
        code.append("Facility facility = new Facility(\n");
        code.append("    \"").append(gfrn).append("\",\n");
        code.append("    \"").append(accountingPeriod).append("\",\n");
        code.append("    \"").append(name).append("\",\n");
        code.append("    \"").append(gfcid).append("\",\n");
        code.append("    \"").append(countryOfRisk).append("\"\n");
        code.append(");\n");
        code.append("facilityService.createFacility(facility);\n");
        
        logger.info("Generated facility creation code for: {}", name);
        return code.toString();
    }
    
    public String generateCustomerCreationCode(String cagId, String gfcid, String accountingPeriod, String countryOfRisk) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated customer creation code\n");
        code.append("Customer customer = new Customer(\n");
        code.append("    \"").append(cagId).append("\",\n");
        code.append("    \"").append(gfcid).append("\",\n");
        code.append("    \"").append(accountingPeriod).append("\",\n");
        code.append("    \"").append(countryOfRisk).append("\"\n");
        code.append(");\n");
        code.append("customerService.createCustomer(customer);\n");
        
        logger.info("Generated customer creation code for: {}", cagId);
        return code.toString();
    }
    
    public String generateContractCreationCode(String transactionId, String gfrn, String gfcid, BigDecimal directAmount, BigDecimal contingentAmount, String accountingPeriod) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated contract creation code\n");
        code.append("Contract contract = new Contract(\n");
        code.append("    \"").append(transactionId).append("\",\n");
        code.append("    \"").append(gfrn).append("\",\n");
        code.append("    \"").append(gfcid).append("\",\n");
        code.append("    new BigDecimal(\"").append(directAmount).append("\"),\n");
        code.append("    new BigDecimal(\"").append(contingentAmount).append("\"),\n");
        code.append("    \"").append(accountingPeriod).append("\"\n");
        code.append(");\n");
        code.append("contractService.createContract(contract);\n");
        
        logger.info("Generated contract creation code for: {}", transactionId);
        return code.toString();
    }
    
    public String generateFacilityUpdateCode(Long facilityId, String gfrn) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated facility update code\n");
        code.append("Optional<Facility> facilityOpt = facilityService.getFacilityById(").append(facilityId).append("L);\n");
        code.append("if (facilityOpt.isPresent()) {\n");
        code.append("    Facility facility = facilityOpt.get();\n");
        if (gfrn != null) {
            code.append("    facility.setGfrn(\"").append(gfrn).append("\");\n");
        }
        code.append("    facilityService.updateFacility(").append(facilityId).append("L, facility);\n");
        code.append("}\n");
        
        logger.info("Generated facility update code for ID: {}", facilityId);
        return code.toString();
    }
    
    public String generateCustomerUpdateCode(Long customerId, String cagId) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated customer update code\n");
        code.append("Optional<Customer> customerOpt = customerService.getCustomerById(").append(customerId).append("L);\n");
        code.append("if (customerOpt.isPresent()) {\n");
        code.append("    Customer customer = customerOpt.get();\n");
        if (cagId != null) {
            code.append("    customer.setCagId(\"").append(cagId).append("\");\n");
        }
        code.append("    customerService.updateCustomer(").append(customerId).append("L, customer);\n");
        code.append("}\n");
        
        logger.info("Generated customer update code for ID: {}", customerId);
        return code.toString();
    }
    
    public String generateContractUpdateCode(Long contractId, String transactionId) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated contract update code\n");
        code.append("Optional<Contract> contractOpt = contractService.getContractById(").append(contractId).append("L);\n");
        code.append("if (contractOpt.isPresent()) {\n");
        code.append("    Contract contract = contractOpt.get();\n");
        if (transactionId != null) {
            code.append("    contract.setTransactionId(\"").append(transactionId).append("\");\n");
        }
        code.append("    contractService.updateContract(").append(contractId).append("L, contract);\n");
        code.append("}\n");
        
        logger.info("Generated contract update code for ID: {}", contractId);
        return code.toString();
    }
    
    public String generateReportCode(String reportType) {
        StringBuilder code = new StringBuilder();
        code.append("// Auto-generated report generation code\n");
        
        switch (reportType.toLowerCase()) {
            case "facility_summary":
            case "facilities":
                code.append("List<Facility> facilities = facilityService.getAllFacilities();\n");
                code.append("Map<String, Long> countryCounts = facilities.stream()\n");
                code.append("    .collect(Collectors.groupingBy(Facility::getCountryOfRisk, Collectors.counting()));\n");
                break;
            case "customer_summary":
            case "customers":
                code.append("List<Customer> customers = customerService.getAllCustomers();\n");
                code.append("Map<String, Long> countryCounts = customers.stream()\n");
                code.append("    .collect(Collectors.groupingBy(Customer::getCountryOfRisk, Collectors.counting()));\n");
                break;
            case "contract_summary":
            case "contracts":
                code.append("List<Contract> contracts = contractService.getAllContracts();\n");
                code.append("Double totalDirectAmount = contracts.stream()\n");
                code.append("    .mapToDouble(Contract::getDirectAmount).sum();\n");
                code.append("Double totalContingentAmount = contracts.stream()\n");
                code.append("    .mapToDouble(Contract::getContingentAmount).sum();\n");
                break;
            default:
                code.append("// Custom report generation for: ").append(reportType).append("\n");
                code.append("// Add specific report logic here\n");
        }
        
        logger.info("Generated report code for: {}", reportType);
        return code.toString();
    }
}
