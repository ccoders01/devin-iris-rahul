package com.facility.management.application.service;

import com.facility.management.domain.facility.Facility;
import com.facility.management.domain.customer.Customer;
import com.facility.management.domain.contract.Contract;
import com.facility.management.infrastructure.jira.JiraTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIAgentService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIAgentService.class);
    
    private final FacilityService facilityService;
    private final CustomerService customerService;
    private final ContractService contractService;
    private final CodeGenerationService codeGenerationService;
    private final DeploymentService deploymentService;
    private final LLMService llmService;
    
    @Autowired
    public AIAgentService(FacilityService facilityService, 
                         CustomerService customerService,
                         ContractService contractService,
                         CodeGenerationService codeGenerationService,
                         DeploymentService deploymentService,
                         LLMService llmService) {
        this.facilityService = facilityService;
        this.customerService = customerService;
        this.contractService = contractService;
        this.codeGenerationService = codeGenerationService;
        this.deploymentService = deploymentService;
        this.llmService = llmService;
    }
    
    public Mono<ProcessingResult> processJiraTicket(JiraTicket ticket) {
        return Mono.fromCallable(() -> {
            logger.info("AI Agent processing ticket: {}", ticket.getKey());
            
            String summary = ticket.getFields().getSummary();
            String description = ticket.getFields().getDescription();
            String fullText = summary + " " + (description != null ? description : "");
            
            RequirementAnalysis analysis = analyzeRequirements(fullText);
            
            if (llmService.isConfigured()) {
                String llmAnalysis = llmService.analyzeRequirements(summary, description);
                logger.info("LLM Analysis for ticket {}: {}", ticket.getKey(), llmAnalysis);
                enhanceAnalysisWithLLM(analysis, llmAnalysis);
            }
            
            switch (analysis.getActionType()) {
                case CREATE_FACILITY:
                    return handleCreateFacility(analysis);
                case CREATE_CUSTOMER:
                    return handleCreateCustomer(analysis);
                case CREATE_CONTRACT:
                    return handleCreateContract(analysis);
                case UPDATE_FACILITY:
                    return handleUpdateFacility(analysis);
                case UPDATE_CUSTOMER:
                    return handleUpdateCustomer(analysis);
                case UPDATE_CONTRACT:
                    return handleUpdateContract(analysis);
                case GENERATE_REPORT:
                    return handleGenerateReport(analysis);
                default:
                    return new ProcessingResult(false, "Unable to determine action from ticket requirements");
            }
        });
    }
    
    private RequirementAnalysis analyzeRequirements(String text) {
        RequirementAnalysis analysis = new RequirementAnalysis();
        
        String lowerText = text.toLowerCase();
        
        if (containsPattern(lowerText, "create.*facility|add.*facility|new.*facility")) {
            analysis.setActionType(ActionType.CREATE_FACILITY);
            analysis.setGfrn(extractValue(text, "gfrn[:\\s]+([^\\n,\\s]+)"));
            analysis.setName(extractValue(text, "name[:\\s]+([^\\n,]+)"));
            analysis.setGfcid(extractValue(text, "gfcid[:\\s]+([^\\n,\\s]+)"));
            analysis.setAccountingPeriod(extractValue(text, "accounting.*period[:\\s]+([^\\n,]+)"));
            analysis.setCountryOfRisk(extractValue(text, "country.*risk[:\\s]+([^\\n,]+)"));
        } else if (containsPattern(lowerText, "create.*customer|add.*customer|new.*customer")) {
            analysis.setActionType(ActionType.CREATE_CUSTOMER);
            analysis.setCagId(extractValue(text, "cag.*id[:\\s]+([^\\n,\\s]+)"));
            analysis.setGfcid(extractValue(text, "gfcid[:\\s]+([^\\n,\\s]+)"));
            analysis.setAccountingPeriod(extractValue(text, "accounting.*period[:\\s]+([^\\n,]+)"));
            analysis.setCountryOfRisk(extractValue(text, "country.*risk[:\\s]+([^\\n,]+)"));
        } else if (containsPattern(lowerText, "create.*contract|add.*contract|new.*contract")) {
            analysis.setActionType(ActionType.CREATE_CONTRACT);
            analysis.setTransactionId(extractValue(text, "transaction.*id[:\\s]+([^\\n,\\s]+)"));
            analysis.setGfrn(extractValue(text, "gfrn[:\\s]+([^\\n,\\s]+)"));
            analysis.setGfcid(extractValue(text, "gfcid[:\\s]+([^\\n,\\s]+)"));
            analysis.setDirectAmount(extractBigDecimalValue(text, "direct.*amount[:\\s]+([\\d\\.]+)"));
            analysis.setContingentAmount(extractBigDecimalValue(text, "contingent.*amount[:\\s]+([\\d\\.]+)"));
            analysis.setAccountingPeriod(extractValue(text, "accounting.*period[:\\s]+([^\\n,]+)"));
        } else if (containsPattern(lowerText, "update.*facility|modify.*facility")) {
            analysis.setActionType(ActionType.UPDATE_FACILITY);
            analysis.setFacilityId(extractLongValue(text, "facility.*id[:\\s]+(\\d+)"));
            analysis.setGfrn(extractValue(text, "gfrn[:\\s]+([^\\n,\\s]+)"));
        } else if (containsPattern(lowerText, "update.*customer|modify.*customer")) {
            analysis.setActionType(ActionType.UPDATE_CUSTOMER);
            analysis.setCustomerId(extractLongValue(text, "customer.*id[:\\s]+(\\d+)"));
            analysis.setCagId(extractValue(text, "cag.*id[:\\s]+([^\\n,\\s]+)"));
        } else if (containsPattern(lowerText, "update.*contract|modify.*contract")) {
            analysis.setActionType(ActionType.UPDATE_CONTRACT);
            analysis.setContractId(extractLongValue(text, "contract.*id[:\\s]+(\\d+)"));
            analysis.setTransactionId(extractValue(text, "transaction.*id[:\\s]+([^\\n,\\s]+)"));
        } else if (containsPattern(lowerText, "generate.*report|create.*report|report.*on")) {
            analysis.setActionType(ActionType.GENERATE_REPORT);
            analysis.setReportType(extractValue(text, "report.*type[:\\s]+([^\\n,]+)"));
        } else {
            analysis.setActionType(ActionType.UNKNOWN);
        }
        
        return analysis;
    }
    
    private boolean containsPattern(String text, String pattern) {
        return Pattern.compile(pattern).matcher(text).find();
    }
    
    private String extractValue(String text, String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }
    
    private Long extractLongValue(String text, String pattern) {
        String value = extractValue(text, pattern);
        try {
            return value != null ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private BigDecimal extractBigDecimalValue(String text, String pattern) {
        String value = extractValue(text, pattern);
        try {
            return value != null ? new BigDecimal(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private ProcessingResult handleCreateFacility(RequirementAnalysis analysis) {
        try {
            String gfrn = analysis.getGfrn() != null ? analysis.getGfrn() : "AUTO-" + System.currentTimeMillis();
            String name = analysis.getName() != null ? analysis.getName() : "New Facility";
            String gfcid = analysis.getGfcid() != null ? analysis.getGfcid() : "AUTO-GFCID";
            String accountingPeriod = analysis.getAccountingPeriod() != null ? analysis.getAccountingPeriod() : "2024";
            String countryOfRisk = analysis.getCountryOfRisk() != null ? analysis.getCountryOfRisk() : "US";
            
            Facility facility = new Facility(gfrn, accountingPeriod, name, gfcid, countryOfRisk);
            facilityService.createFacility(facility);
            
            String code = codeGenerationService.generateFacilityCreationCode(gfrn, name, gfcid, accountingPeriod, countryOfRisk);
            logger.info("Generated code for facility creation: {}", code);
            
            boolean deployed = deploymentService.deployChanges(code, "Create facility: " + name);
            
            return new ProcessingResult(deployed, "Successfully created facility: " + name + (deployed ? " and deployed" : ""));
        } catch (Exception e) {
            logger.error("Error creating facility", e);
            return new ProcessingResult(false, "Error creating facility: " + e.getMessage());
        }
    }
    
    private ProcessingResult handleCreateCustomer(RequirementAnalysis analysis) {
        try {
            String cagId = analysis.getCagId() != null ? analysis.getCagId() : "AUTO-CAG-" + System.currentTimeMillis();
            String gfcid = analysis.getGfcid() != null ? analysis.getGfcid() : "AUTO-GFCID";
            String accountingPeriod = analysis.getAccountingPeriod() != null ? analysis.getAccountingPeriod() : "2024";
            String countryOfRisk = analysis.getCountryOfRisk() != null ? analysis.getCountryOfRisk() : "US";
            
            Customer customer = new Customer(cagId, gfcid, accountingPeriod, countryOfRisk);
            customerService.createCustomer(customer);
            
            String code = codeGenerationService.generateCustomerCreationCode(cagId, gfcid, accountingPeriod, countryOfRisk);
            logger.info("Generated code for customer creation: {}", code);
            
            boolean deployed = deploymentService.deployChanges(code, "Create customer: " + cagId);
            
            return new ProcessingResult(deployed, "Successfully created customer: " + cagId + (deployed ? " and deployed" : ""));
        } catch (Exception e) {
            logger.error("Error creating customer", e);
            return new ProcessingResult(false, "Error creating customer: " + e.getMessage());
        }
    }
    
    private ProcessingResult handleCreateContract(RequirementAnalysis analysis) {
        try {
            String transactionId = analysis.getTransactionId() != null ? analysis.getTransactionId() : "AUTO-TXN-" + System.currentTimeMillis();
            String gfrn = analysis.getGfrn();
            String gfcid = analysis.getGfcid();
            
            if (gfrn == null || gfrn.trim().isEmpty()) {
                return new ProcessingResult(false, "GFRN is required for contract creation. Please specify a valid facility GFRN.");
            }
            if (gfcid == null || gfcid.trim().isEmpty()) {
                gfcid = "AUTO-GFCID";
            }
            
            if (facilityService.getFacilityByGfrn(gfrn).isEmpty()) {
                return new ProcessingResult(false, "Facility with GFRN '" + gfrn + "' not found. Please create the facility first or use an existing GFRN.");
            }
            
            BigDecimal directAmount = analysis.getDirectAmount() != null ? analysis.getDirectAmount() : BigDecimal.ZERO;
            BigDecimal contingentAmount = analysis.getContingentAmount() != null ? analysis.getContingentAmount() : BigDecimal.ZERO;
            String accountingPeriod = analysis.getAccountingPeriod() != null ? analysis.getAccountingPeriod() : "2024";
            
            Contract contract = new Contract(transactionId, gfrn, gfcid, directAmount, contingentAmount, accountingPeriod);
            contractService.createContract(contract);
            
            String code = codeGenerationService.generateContractCreationCode(transactionId, gfrn, gfcid, directAmount, contingentAmount, accountingPeriod);
            logger.info("Generated code for contract creation: {}", code);
            
            boolean deployed = deploymentService.deployChanges(code, "Create contract: " + transactionId);
            
            return new ProcessingResult(deployed, "Successfully created contract: " + transactionId + (deployed ? " and deployed" : ""));
        } catch (Exception e) {
            logger.error("Error creating contract", e);
            return new ProcessingResult(false, "Error creating contract: " + e.getMessage());
        }
    }
    
    private ProcessingResult handleUpdateFacility(RequirementAnalysis analysis) {
        try {
            Long facilityId = analysis.getFacilityId();
            if (facilityId == null) {
                return new ProcessingResult(false, "Facility ID is required for update");
            }
            
            Facility existingFacility = facilityService.getFacilityById(facilityId).orElse(null);
            if (existingFacility == null) {
                return new ProcessingResult(false, "Facility not found with ID: " + facilityId);
            }
            
            if (analysis.getGfrn() != null) {
                existingFacility.setGfrn(analysis.getGfrn());
            }
            
            facilityService.updateFacility(facilityId, existingFacility);
            
            String code = codeGenerationService.generateFacilityUpdateCode(facilityId, analysis.getGfrn());
            boolean deployed = deploymentService.deployChanges(code, "Update facility: " + facilityId);
            
            return new ProcessingResult(deployed, "Successfully updated facility: " + facilityId + (deployed ? " and deployed" : ""));
        } catch (Exception e) {
            logger.error("Error updating facility", e);
            return new ProcessingResult(false, "Error updating facility: " + e.getMessage());
        }
    }
    
    private ProcessingResult handleUpdateCustomer(RequirementAnalysis analysis) {
        try {
            Long customerId = analysis.getCustomerId();
            if (customerId == null) {
                return new ProcessingResult(false, "Customer ID is required for update");
            }
            
            Customer existingCustomer = customerService.getCustomerById(customerId).orElse(null);
            if (existingCustomer == null) {
                return new ProcessingResult(false, "Customer not found with ID: " + customerId);
            }
            
            if (analysis.getCagId() != null) {
                existingCustomer.setCagId(analysis.getCagId());
            }
            
            customerService.updateCustomer(customerId, existingCustomer);
            
            String code = codeGenerationService.generateCustomerUpdateCode(customerId, analysis.getCagId());
            boolean deployed = deploymentService.deployChanges(code, "Update customer: " + customerId);
            
            return new ProcessingResult(deployed, "Successfully updated customer: " + customerId + (deployed ? " and deployed" : ""));
        } catch (Exception e) {
            logger.error("Error updating customer", e);
            return new ProcessingResult(false, "Error updating customer: " + e.getMessage());
        }
    }
    
    private ProcessingResult handleUpdateContract(RequirementAnalysis analysis) {
        try {
            Long contractId = analysis.getContractId();
            if (contractId == null) {
                return new ProcessingResult(false, "Contract ID is required for update");
            }
            
            Contract existingContract = contractService.getContractById(contractId).orElse(null);
            if (existingContract == null) {
                return new ProcessingResult(false, "Contract not found with ID: " + contractId);
            }
            
            if (analysis.getTransactionId() != null) {
                existingContract.setTransactionId(analysis.getTransactionId());
            }
            
            contractService.updateContract(contractId, existingContract);
            
            String code = codeGenerationService.generateContractUpdateCode(contractId, analysis.getTransactionId());
            boolean deployed = deploymentService.deployChanges(code, "Update contract: " + contractId);
            
            return new ProcessingResult(deployed, "Successfully updated contract: " + contractId + (deployed ? " and deployed" : ""));
        } catch (Exception e) {
            logger.error("Error updating contract", e);
            return new ProcessingResult(false, "Error updating contract: " + e.getMessage());
        }
    }
    
    private ProcessingResult handleGenerateReport(RequirementAnalysis analysis) {
        try {
            String reportType = analysis.getReportType() != null ? analysis.getReportType() : "summary";
            
            String code = codeGenerationService.generateReportCode(reportType);
            logger.info("Generated code for report generation: {}", code);
            
            boolean deployed = deploymentService.deployChanges(code, "Generate report: " + reportType);
            
            return new ProcessingResult(deployed, "Successfully generated " + reportType + " report" + (deployed ? " and deployed" : ""));
        } catch (Exception e) {
            logger.error("Error generating report", e);
            return new ProcessingResult(false, "Error generating report: " + e.getMessage());
        }
    }
    
    public static class RequirementAnalysis {
        private ActionType actionType;
        private String gfrn;
        private String name;
        private String gfcid;
        private String accountingPeriod;
        private String countryOfRisk;
        private String cagId;
        private String transactionId;
        private BigDecimal directAmount;
        private BigDecimal contingentAmount;
        private Long facilityId;
        private Long customerId;
        private Long contractId;
        private String reportType;
        
        public ActionType getActionType() { return actionType; }
        public void setActionType(ActionType actionType) { this.actionType = actionType; }
        
        public String getGfrn() { return gfrn; }
        public void setGfrn(String gfrn) { this.gfrn = gfrn; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getGfcid() { return gfcid; }
        public void setGfcid(String gfcid) { this.gfcid = gfcid; }
        
        public String getAccountingPeriod() { return accountingPeriod; }
        public void setAccountingPeriod(String accountingPeriod) { this.accountingPeriod = accountingPeriod; }
        
        public String getCountryOfRisk() { return countryOfRisk; }
        public void setCountryOfRisk(String countryOfRisk) { this.countryOfRisk = countryOfRisk; }
        
        public String getCagId() { return cagId; }
        public void setCagId(String cagId) { this.cagId = cagId; }
        
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public BigDecimal getDirectAmount() { return directAmount; }
        public void setDirectAmount(BigDecimal directAmount) { this.directAmount = directAmount; }
        
        public BigDecimal getContingentAmount() { return contingentAmount; }
        public void setContingentAmount(BigDecimal contingentAmount) { this.contingentAmount = contingentAmount; }
        
        public Long getFacilityId() { return facilityId; }
        public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }
        
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        
        public Long getContractId() { return contractId; }
        public void setContractId(Long contractId) { this.contractId = contractId; }
        
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
    }
    
    private void enhanceAnalysisWithLLM(RequirementAnalysis analysis, String llmAnalysis) {
        try {
            String lowerLLM = llmAnalysis.toLowerCase();
            
            if (lowerLLM.contains("create_facility") && analysis.getActionType() == ActionType.UNKNOWN) {
                analysis.setActionType(ActionType.CREATE_FACILITY);
            } else if (lowerLLM.contains("create_customer") && analysis.getActionType() == ActionType.UNKNOWN) {
                analysis.setActionType(ActionType.CREATE_CUSTOMER);
            } else if (lowerLLM.contains("create_contract") && analysis.getActionType() == ActionType.UNKNOWN) {
                analysis.setActionType(ActionType.CREATE_CONTRACT);
            } else if (lowerLLM.contains("update_facility") && analysis.getActionType() == ActionType.UNKNOWN) {
                analysis.setActionType(ActionType.UPDATE_FACILITY);
            } else if (lowerLLM.contains("update_customer") && analysis.getActionType() == ActionType.UNKNOWN) {
                analysis.setActionType(ActionType.UPDATE_CUSTOMER);
            } else if (lowerLLM.contains("update_contract") && analysis.getActionType() == ActionType.UNKNOWN) {
                analysis.setActionType(ActionType.UPDATE_CONTRACT);
            } else if (lowerLLM.contains("generate_report") && analysis.getActionType() == ActionType.UNKNOWN) {
                analysis.setActionType(ActionType.GENERATE_REPORT);
            }
            
            if (analysis.getGfrn() == null && lowerLLM.contains("gfrn")) {
                String gfrnMatch = extractFromLLMAnalysis(llmAnalysis, "gfrn[:\\s]*([A-Za-z0-9-]+)");
                if (gfrnMatch != null) analysis.setGfrn(gfrnMatch);
            }
            
            if (analysis.getGfcid() == null && lowerLLM.contains("gfcid")) {
                String gfcidMatch = extractFromLLMAnalysis(llmAnalysis, "gfcid[:\\s]*([A-Za-z0-9-]+)");
                if (gfcidMatch != null) analysis.setGfcid(gfcidMatch);
            }
            
            if (analysis.getCagId() == null && lowerLLM.contains("cag")) {
                String cagMatch = extractFromLLMAnalysis(llmAnalysis, "cag[\\s]*id[:\\s]*([A-Za-z0-9-]+)");
                if (cagMatch != null) analysis.setCagId(cagMatch);
            }
            
        } catch (Exception e) {
            logger.warn("Error enhancing analysis with LLM: {}", e.getMessage());
        }
    }
    
    private String extractFromLLMAnalysis(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            return m.find() ? m.group(1).trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    public enum ActionType {
        CREATE_FACILITY,
        CREATE_CUSTOMER,
        CREATE_CONTRACT,
        UPDATE_FACILITY,
        UPDATE_CUSTOMER,
        UPDATE_CONTRACT,
        GENERATE_REPORT,
        UNKNOWN
    }
    
    public static class ProcessingResult {
        private final boolean success;
        private final String message;
        
        public ProcessingResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
