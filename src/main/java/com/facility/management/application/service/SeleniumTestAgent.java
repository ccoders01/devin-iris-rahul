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
    private final Set<String> processedTestTickets = new HashSet<>();
    private final String testOutputDirectory = "src/test/java/com/facility/management/selenium/generated";
    
    @Autowired
    public SeleniumTestAgent(JiraClient jiraClient, LLMService llmService) {
        this.jiraClient = jiraClient;
        this.llmService = llmService;
        createTestOutputDirectory();
    }
    
    @Scheduled(fixedRate = 300000)
    public void monitorJiraForTestGeneration() {
        if (!jiraClient.isConfigured()) {
            logger.debug("JIRA client not configured, skipping test generation monitoring");
            return;
        }
        
        logger.info("Starting JIRA ticket monitoring for test generation");
        
        jiraClient.searchRecentTickets(50)
                .subscribe(
                        response -> {
                            if (response != null && response.getIssues() != null) {
                                List<JiraTicket> allTickets = response.getIssues().stream()
                                        .filter(ticket -> !processedTestTickets.contains(ticket.getKey()))
                                        .collect(Collectors.toList());
                                
                                logger.info("Found {} new tickets for test generation", allTickets.size());
                                
                                for (JiraTicket ticket : allTickets) {
                                    processTicketForTestGeneration(ticket);
                                }
                            }
                        },
                        error -> logger.error("Error monitoring JIRA tickets for test generation", error)
                );
    }
    
    private void processTicketForTestGeneration(JiraTicket ticket) {
        try {
            logger.info("Processing JIRA ticket for test generation: {}", ticket.getKey());
            
            processedTestTickets.add(ticket.getKey());
            
            String summary = ticket.getFields().getSummary();
            String description = ticket.getFields().getDescription();
            
            String requirementAnalysis = llmService.analyzeRequirements(summary, description);
            String acceptanceCriteria = llmService.generateAcceptanceCriteria(summary, description);
            
            jiraClient.addComment(ticket.getKey(), "🧪 Selenium Test Agent: Analyzing requirements with LLM and generating test cases...")
                    .subscribe(
                            v -> logger.info("Added test generation comment to ticket {}", ticket.getKey()),
                            error -> logger.error("Error adding test generation comment to ticket {}", ticket.getKey(), error)
                    );
            
            RequirementAnalysis requirements = parseRequirements(ticket);
            
            List<String> acceptanceCriteriaList = List.of(acceptanceCriteria.split("\n\n"));
            
            String testCode = generateSeleniumTestCode(ticket, requirements, acceptanceCriteriaList);
            
            String fileName = sanitizeFileName(ticket.getKey()) + "Test.java";
            saveTestFile(fileName, testCode);
            
            String successComment = String.format("""
                🧪 **Selenium Test Agent - LLM Enhanced**
                
                **Requirement Analysis:**
                %s
                
                **Acceptance Criteria Generated:**
                %s
                
                **Test File Created:** %s
                
                ✅ Selenium test cases have been generated using LLM analysis and saved.
                """, requirementAnalysis, acceptanceCriteria, fileName);
            
            jiraClient.addComment(ticket.getKey(), successComment)
                    .subscribe(
                            v -> logger.info("Added test generation success comment to ticket {}", ticket.getKey()),
                            error -> logger.error("Error adding success comment to ticket {}", ticket.getKey(), error)
                    );
            
        } catch (Exception e) {
            logger.error("Error processing ticket {} for test generation", ticket.getKey(), e);
            
            String errorComment = String.format("""
                🧪 **Selenium Test Agent - LLM Enhanced**
                
                ❌ Failed to generate test cases: %s
                
                Please check the system logs for more details.
                """, e.getMessage());
            
            jiraClient.addComment(ticket.getKey(), errorComment)
                    .subscribe();
        }
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
            code.append("        // ").append(criteria).append("\n");
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
            code.append("        // TODO: Implement specific test logic for: ").append(criteria).append("\n");
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
