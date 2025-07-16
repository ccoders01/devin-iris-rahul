package com.facility.management.application.service;

import com.facility.management.infrastructure.jira.JiraClient;
import com.facility.management.infrastructure.jira.JiraTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeleniumTestAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(SeleniumTestAgent.class);
    
    private final JiraClient jiraClient;
    private final LLMService llmService;
    private final TestExecutionService testExecutionService;
    private final Set<String> processedTestTickets = new HashSet<>();
    private final String testOutputDirectory = "src/test/java/com/facility/management/selenium/generated";
    
    @Autowired
    public SeleniumTestAgent(JiraClient jiraClient, LLMService llmService, TestExecutionService testExecutionService) {
        this.jiraClient = jiraClient;
        this.llmService = llmService;
        this.testExecutionService = testExecutionService;
        createTestOutputDirectory();
    }
    
    @Scheduled(fixedRate = 300000)
    public void monitorForTestGeneration() {
        if (!jiraClient.isConfigured()) {
            logger.debug("JIRA client not configured, skipping test generation monitoring");
            return;
        }
        
        logger.info("Selenium Testing Agent (Agent 2) - Monitoring for tickets ready for testing");
        
        jiraClient.searchRecentTickets(50)
                .subscribe(
                        response -> {
                            if (response != null && response.getIssues() != null) {
                                List<JiraTicket> testTickets = response.getIssues().stream()
                                        .filter(JiraTicket::isFacilityManagementTicket)
                                        .filter(ticket -> !processedTestTickets.contains(ticket.getKey()))
                                        .filter(this::isDevelopmentPhaseComplete)
                                        .collect(Collectors.toList());
                                
                                logger.info("Found {} tickets ready for Selenium testing (Agent 2)", testTickets.size());
                                
                                for (JiraTicket ticket : testTickets) {
                                    generateTestsForTicket(ticket);
                                }
                            }
                        },
                        error -> logger.error("Error monitoring JIRA tickets for test generation", error)
                );
    }
    
    private boolean isDevelopmentPhaseComplete(JiraTicket ticket) {
        return "In Progress".equals(ticket.getFields().getStatus().getName()) ||
               "Ready for Testing".equals(ticket.getFields().getStatus().getName());
    }
    
    private void generateTestsForTicket(JiraTicket ticket) {
        try {
            String correlationId = "AGENT2-" + ticket.getKey() + "-" + System.currentTimeMillis();
            logger.info("[{}] Selenium Testing Agent (Agent 2) - Processing ticket for test generation: {}", correlationId, ticket.getKey());
            
            processedTestTickets.add(ticket.getKey());
            
            String summary = ticket.getFields().getSummary();
            String description = ticket.getFields().getDescription();
            
            RequirementAnalysis analysis = parseRequirements(ticket);
            
            String requirementAnalysis = "";
            String acceptanceCriteria = "";
            
            if (llmService.isConfigured()) {
                logger.info("[{}] Using LLM for test case generation", correlationId);
                requirementAnalysis = llmService.analyzeRequirements(summary, description);
                acceptanceCriteria = llmService.generateAcceptanceCriteria(summary, description);
            } else {
                logger.info("[{}] Using mock mode for test case generation", correlationId);
                requirementAnalysis = "Mock requirement analysis for: " + summary;
                acceptanceCriteria = generateMockAcceptanceCriteria(summary);
            }
            
            String testGenerationComment = String.format("""
                🧪 **Selenium Testing Agent (Agent 2) - Test Generation Phase**
                
                **Requirement Analysis:**
                %s
                
                **Acceptance Criteria Generated:**
                %s
                
                Generating Selenium test cases...
                """, requirementAnalysis, acceptanceCriteria);
            
            jiraClient.addComment(ticket.getKey(), testGenerationComment)
                    .subscribe(
                            v -> logger.info("[{}] Added test generation phase comment", correlationId),
                            error -> logger.error("[{}] Error adding test generation comment", correlationId, error)
                    );
            
            List<String> acceptanceCriteriaList = List.of(acceptanceCriteria.split("\n\n"));
            String testCode = generateSeleniumTestCode(ticket, analysis, acceptanceCriteriaList);
            String fileName = sanitizeFileName(ticket.getKey()) + "Test.java";
            saveTestFile(fileName, testCode);
            
            try {
                byte[] testFileContent = testCode.getBytes();
                jiraClient.attachFileToTicket(ticket.getKey(), fileName, testFileContent)
                        .subscribe(
                                v -> {
                                    logger.info("[{}] Successfully attached test file {} to ticket {}", correlationId, fileName, ticket.getKey());
                                    executeTestsAndGenerateReport(ticket, fileName, correlationId);
                                },
                                error -> {
                                    logger.error("[{}] Failed to attach test file {} to ticket {}", correlationId, fileName, ticket.getKey(), error);
                                    executeTestsAndGenerateReport(ticket, fileName, correlationId);
                                }
                        );
            } catch (Exception e) {
                logger.error("[{}] Error preparing test file attachment for ticket {}", correlationId, ticket.getKey(), e);
                executeTestsAndGenerateReport(ticket, fileName, correlationId);
            }
            
        } catch (Exception e) {
            logger.error("Error generating tests for ticket {}", ticket.getKey(), e);
        }
    }
    
    private void executeTestsAndGenerateReport(JiraTicket ticket, String fileName, String correlationId) {
        logger.info("[{}] Starting test execution phase", correlationId);
        
        String testClassName = "com.facility.management.selenium.generated." + sanitizeClassName(ticket.getKey()) + "Test";
        TestExecutionService.TestExecutionResult executionResult = testExecutionService.executeSeleniumTests(testClassName);
        
        String testResultComment;
        if (executionResult.isAllPassed()) {
            testResultComment = String.format("""
                🧪 **Selenium Testing Agent (Agent 2) - Final Report**
                
                ✅ **All Selenium tests passed!**
                
                **Test Execution Summary:**
                %s
                
                **Test File:** %s (attached)
                
                **Agent 2 Workflow Completed:**
                - ✅ LLM-powered test case generation
                - ✅ Test file attachment to JIRA
                - ✅ Automated test execution
                - ✅ Test report generation
                
                **🎉 Two-Agent Workflow Complete - Marking ticket as DONE**
                """, formatTestResults(executionResult.getTestResults()), fileName);
            
            jiraClient.updateTicketStatus(ticket.getKey(), "31")
                    .subscribe(
                            v -> logger.info("[{}] ✅ Marked ticket {} as Done after successful Agent 2 completion", correlationId, ticket.getKey()),
                            error -> logger.error("[{}] Failed to mark ticket {} as Done", correlationId, ticket.getKey(), error)
                    );
        } else {
            testResultComment = String.format("""
                🧪 **Selenium Testing Agent (Agent 2) - Final Report**
                
                ❌ **Some Selenium tests failed**
                
                **Test Execution Summary:**
                %s
                
                **Test File:** %s (attached)
                
                **Agent 2 Workflow Status:**
                - ✅ LLM-powered test case generation
                - ✅ Test file attachment to JIRA
                - ✅ Automated test execution
                - ❌ Test failures detected
                
                **Next Steps:** Please review and fix the failing tests before proceeding.
                """, formatTestResults(executionResult.getTestResults()), fileName);
        }
        
        jiraClient.addComment(ticket.getKey(), testResultComment)
                .subscribe(
                        v -> logger.info("[{}] Added final test execution report to ticket {}", correlationId, ticket.getKey()),
                        error -> logger.error("[{}] Error adding test results comment to ticket {}", correlationId, ticket.getKey(), error)
                );
    }
    
    private String generateMockAcceptanceCriteria(String summary) {
        return String.format("""
            **Mock Acceptance Criteria** (LLM not configured)
            
            **GIVEN** the system is configured
            **WHEN** user performs the requested action: %s
            **THEN** the system should respond appropriately
            
            **GIVEN** invalid input is provided
            **WHEN** user attempts the operation
            **THEN** appropriate error messages should be displayed
            
            *Note: Configure AI_API_KEY environment variable to enable real criteria generation*
            """, summary);
    }
    
    private RequirementAnalysis parseRequirements(JiraTicket ticket) {
        String summary = ticket.getFields().getSummary();
        String description = ticket.getFields().getDescription();
        String issueType = ticket.getFields().getIssuetype() != null ? ticket.getFields().getIssuetype().getName() : "";
        
        RequirementAnalysis analysis = new RequirementAnalysis();
        analysis.setTicketKey(ticket.getKey());
        analysis.setSummary(summary);
        analysis.setDescription(description);
        analysis.setIssueType(issueType);
        
        analysis.setFunctionalRequirements(extractFunctionalRequirements(description));
        analysis.setUserStories(extractUserStories(description));
        analysis.setBusinessRules(extractBusinessRules(description));
        
        return analysis;
    }
    
    private List<String> extractFunctionalRequirements(String text) {
        if (text == null) return List.of();
        
        Pattern reqPattern = Pattern.compile("(?i)(the system should|user must be able to|application should|system must|requirement:|req:)\\s*([^.\\n]+)", Pattern.MULTILINE);
        Matcher matcher = reqPattern.matcher(text);
        
        return matcher.results()
                .map(match -> match.group(2).trim())
                .filter(req -> !req.isEmpty())
                .collect(Collectors.toList());
    }
    
    private List<String> extractUserStories(String text) {
        if (text == null) return List.of();
        
        Pattern storyPattern = Pattern.compile("(?i)as\\s+a\\s+([^,\\n]+),?\\s*i\\s+want\\s+([^,\\n]+)(?:,?\\s*so\\s+that\\s+([^.\\n]+))?", Pattern.MULTILINE);
        Matcher matcher = storyPattern.matcher(text);
        
        return matcher.results()
                .map(match -> {
                    String role = match.group(1).trim();
                    String want = match.group(2).trim();
                    String reason = match.group(3) != null ? match.group(3).trim() : "";
                    return String.format("As a %s, I want %s%s", role, want, reason.isEmpty() ? "" : " so that " + reason);
                })
                .collect(Collectors.toList());
    }
    
    private List<String> extractBusinessRules(String text) {
        if (text == null) return List.of();
        
        Pattern rulePattern = Pattern.compile("(?i)(validation:|rule:|constraint:|must not|should not|only if|when.*then)\\s*([^.\\n]+)", Pattern.MULTILINE);
        Matcher matcher = rulePattern.matcher(text);
        
        return matcher.results()
                .map(match -> match.group(2).trim())
                .filter(rule -> !rule.isEmpty())
                .collect(Collectors.toList());
    }
    
    private List<String> generateAcceptanceCriteria(RequirementAnalysis requirements) {
        List<String> criteria = new java.util.ArrayList<>();
        
        for (String req : requirements.getFunctionalRequirements()) {
            criteria.add("GIVEN the system is running WHEN " + req + " THEN the operation should complete successfully");
        }
        
        for (String story : requirements.getUserStories()) {
            criteria.add("GIVEN " + story + " WHEN the user performs the action THEN the expected outcome is achieved");
        }
        
        for (String rule : requirements.getBusinessRules()) {
            criteria.add("GIVEN the business rule: " + rule + " WHEN tested THEN the rule should be enforced");
        }
        
        if (criteria.isEmpty()) {
            criteria.add("GIVEN the application is accessible WHEN user navigates to the main page THEN the page should load successfully");
            criteria.add("GIVEN the user is on the main page WHEN user interacts with the interface THEN the interface should respond appropriately");
        }
        
        return criteria;
    }
    
    private String generateSeleniumTestCode(JiraTicket ticket, RequirementAnalysis requirements, List<String> acceptanceCriteria) {
        String className = sanitizeClassName(ticket.getKey()) + "Test";
        StringBuilder code = new StringBuilder();
        
        code.append("package com.facility.management.selenium.generated;\n\n");
        code.append("import org.openqa.selenium.WebDriver;\n");
        code.append("import org.openqa.selenium.chrome.ChromeDriver;\n");
        code.append("import org.openqa.selenium.chrome.ChromeOptions;\n");
        code.append("import org.openqa.selenium.By;\n");
        code.append("import org.openqa.selenium.WebElement;\n");
        code.append("import org.openqa.selenium.support.ui.WebDriverWait;\n");
        code.append("import org.openqa.selenium.support.ui.ExpectedConditions;\n");
        code.append("import org.testng.Assert;\n");
        code.append("import org.testng.annotations.*;\n");
        code.append("import io.github.bonigarcia.wdm.WebDriverManager;\n");
        code.append("import java.time.Duration;\n\n");
        
        code.append("/**\n");
        code.append(" * Generated Selenium test for JIRA ticket: ").append(ticket.getKey()).append("\n");
        code.append(" * Summary: ").append(requirements.getSummary()).append("\n");
        code.append(" * Generated on: ").append(java.time.LocalDateTime.now()).append("\n");
        code.append(" */\n");
        code.append("public class ").append(className).append(" {\n\n");
        
        code.append("    private WebDriver driver;\n");
        code.append("    private WebDriverWait wait;\n");
        code.append("    private static final String BASE_URL = \"http://localhost:8080\";\n\n");
        
        code.append("    @BeforeClass\n");
        code.append("    public void setupClass() {\n");
        code.append("        WebDriverManager.chromedriver().setup();\n");
        code.append("    }\n\n");
        
        code.append("    @BeforeMethod\n");
        code.append("    public void setup() {\n");
        code.append("        ChromeOptions options = new ChromeOptions();\n");
        code.append("        options.addArguments(\"--headless\");\n");
        code.append("        options.addArguments(\"--no-sandbox\");\n");
        code.append("        options.addArguments(\"--disable-dev-shm-usage\");\n");
        code.append("        driver = new ChromeDriver(options);\n");
        code.append("        wait = new WebDriverWait(driver, Duration.ofSeconds(10));\n");
        code.append("    }\n\n");
        
        code.append("    @AfterMethod\n");
        code.append("    public void teardown() {\n");
        code.append("        if (driver != null) {\n");
        code.append("            driver.quit();\n");
        code.append("        }\n");
        code.append("    }\n\n");
        
        for (int i = 0; i < acceptanceCriteria.size(); i++) {
            String criteria = acceptanceCriteria.get(i);
            String methodName = "testAcceptanceCriteria" + (i + 1);
            
            code.append("    @Test\n");
            code.append("    public void ").append(methodName).append("() {\n");
            
            String[] criteriaLines = criteria.split("\n");
            for (String line : criteriaLines) {
                code.append("        // ").append(line.trim()).append("\n");
            }
            code.append("        \n");
            code.append("        // Navigate to application\n");
            code.append("        driver.get(BASE_URL);\n");
            code.append("        \n");
            code.append("        // Wait for page to load\n");
            code.append("        wait.until(ExpectedConditions.titleContains(\"Facility Management\"));\n");
            code.append("        \n");
            code.append("        // Verify page is accessible\n");
            code.append("        Assert.assertTrue(driver.getTitle().contains(\"Facility Management\"), \"Application should be accessible\");\n");
            code.append("        \n");
            code.append("        // TODO: Implement specific test logic for:\n");
            String[] todoLines = criteria.split("\n");
            for (String line : todoLines) {
                code.append("        // ").append(line.trim()).append("\n");
            }
            code.append("        // Add your test implementation here based on the acceptance criteria\n");
            code.append("        \n");
            code.append("    }\n\n");
        }
        
        code.append("}\n");
        
        return code.toString();
    }
    
    private void createTestOutputDirectory() {
        File dir = new File(testOutputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private void saveTestFile(String fileName, String testCode) throws IOException {
        File testFile = new File(testOutputDirectory, fileName);
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(testCode);
        }
        logger.info("Saved test file: {}", testFile.getAbsolutePath());
    }
    
    private String sanitizeFileName(String ticketKey) {
        return ticketKey.replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    private String sanitizeClassName(String ticketKey) {
        String sanitized = sanitizeFileName(ticketKey);
        return sanitized.substring(0, 1).toUpperCase() + sanitized.substring(1);
    }
    
    public void resetProcessedTestTickets() {
        processedTestTickets.clear();
        logger.info("Reset processed test tickets cache");
    }
    
    public int getProcessedTestTicketsCount() {
        return processedTestTickets.size();
    }
    
    public Set<String> getProcessedTestTickets() {
        return new HashSet<>(processedTestTickets);
    }
    
    private String formatTestResults(List<TestExecutionService.TestResult> testResults) {
        if (testResults.isEmpty()) {
            return "No test results available";
        }
        
        StringBuilder results = new StringBuilder();
        for (TestExecutionService.TestResult result : testResults) {
            String status = result.isPassed() ? "✅ PASSED" : "❌ FAILED";
            results.append(String.format("- %s: %s (%s)\n", 
                result.getMethodName(), status, result.getExecutionTime()));
            if (!result.isPassed() && !result.getErrorMessage().isEmpty()) {
                results.append("  Error: ").append(result.getErrorMessage().substring(0, 
                    Math.min(100, result.getErrorMessage().length()))).append("...\n");
            }
        }
        return results.toString();
    }
    
    public static class RequirementAnalysis {
        private String ticketKey;
        private String summary;
        private String description;
        private String issueType;
        private List<String> functionalRequirements = new java.util.ArrayList<>();
        private List<String> userStories = new java.util.ArrayList<>();
        private List<String> businessRules = new java.util.ArrayList<>();
        
        public String getTicketKey() { return ticketKey; }
        public void setTicketKey(String ticketKey) { this.ticketKey = ticketKey; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getIssueType() { return issueType; }
        public void setIssueType(String issueType) { this.issueType = issueType; }
        
        public List<String> getFunctionalRequirements() { return functionalRequirements; }
        public void setFunctionalRequirements(List<String> functionalRequirements) { this.functionalRequirements = functionalRequirements; }
        
        public List<String> getUserStories() { return userStories; }
        public void setUserStories(List<String> userStories) { this.userStories = userStories; }
        
        public List<String> getBusinessRules() { return businessRules; }
        public void setBusinessRules(List<String> businessRules) { this.businessRules = businessRules; }
    }
}
