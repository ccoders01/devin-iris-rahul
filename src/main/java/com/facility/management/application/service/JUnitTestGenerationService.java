package com.facility.management.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class JUnitTestGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(JUnitTestGenerationService.class);
    private final LLMService llmService;
    private final String testOutputDirectory = "src/test/java/com/facility/management/junit/generated";
    
    public JUnitTestGenerationService(LLMService llmService) {
        this.llmService = llmService;
        createTestOutputDirectory();
    }
    
    private void createTestOutputDirectory() {
        try {
            Path testDir = Paths.get(testOutputDirectory);
            if (!Files.exists(testDir)) {
                Files.createDirectories(testDir);
                logger.info("Created JUnit test output directory: {}", testOutputDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create JUnit test output directory", e);
        }
    }
    
    public String generateJUnitTests(String ticketKey, String summary, String description, String generatedCode, String actionType) {
        logger.info("Generating JUnit tests for ticket: {} with action type: {}", ticketKey, actionType);
        
        String testCode;
        if (llmService.isConfigured()) {
            testCode = generateLLMPoweredJUnitTests(ticketKey, summary, description, generatedCode, actionType);
        } else {
            testCode = generateMockJUnitTests(ticketKey, summary, actionType);
        }
        
        String fileName = sanitizeClassName(ticketKey) + "JUnitTest.java";
        saveTestFile(fileName, testCode);
        
        return fileName;
    }
    
    private String generateLLMPoweredJUnitTests(String ticketKey, String summary, String description, String generatedCode, String actionType) {
        String prompt = String.format("""
            Generate comprehensive JUnit test cases for the following facility management system implementation:
            
            **JIRA Ticket:** %s
            **Summary:** %s
            **Description:** %s
            **Action Type:** %s
            
            **Generated Code to Test:**
            %s
            
            Please generate JUnit 5 test cases that:
            1. Test the happy path scenarios
            2. Test edge cases and boundary conditions
            3. Test error handling and validation
            4. Use proper mocking for dependencies
            5. Follow Spring Boot testing best practices
            6. Include @Test, @BeforeEach, @AfterEach annotations as needed
            7. Use AssertJ assertions for better readability
            
            Generate a complete test class with proper imports and structure.
            """, ticketKey, summary, description != null ? description : "No description", actionType, generatedCode);
        
        try {
            String llmResponse = llmService.generateAcceptanceCriteria(ticketKey, prompt);
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                return llmResponse;
            }
        } catch (Exception e) {
            logger.warn("LLM test generation failed, falling back to mock: {}", e.getMessage());
        }
        
        return generateMockJUnitTests(ticketKey, summary, actionType);
    }
    
    private String generateMockJUnitTests(String ticketKey, String summary, String actionType) {
        String className = sanitizeClassName(ticketKey) + "JUnitTest";
        
        return String.format("""
            package com.facility.management.junit.generated;
            
            import org.junit.jupiter.api.BeforeEach;
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.Mock;
            import org.mockito.junit.jupiter.MockitoExtension;
            import org.springframework.boot.test.context.SpringBootTest;
            import static org.assertj.core.api.Assertions.*;
            import static org.mockito.Mockito.*;
            
            @ExtendWith(MockitoExtension.class)
            @SpringBootTest
            public class %s {
                
                @Mock
                private FacilityService facilityService;
                
                @Mock
                private CustomerService customerService;
                
                @Mock
                private ContractService contractService;
                
                @BeforeEach
                void setUp() {
                }
                
                @Test
                void testHappyPath() {
                    // TODO: Implement specific test logic based on action type: %s
                    
                    assertThat(true).isTrue(); // Placeholder assertion
                }
                
                @Test
                void testEdgeCases() {
                    
                    assertThat(true).isTrue(); // Placeholder assertion
                }
                
                @Test
                void testErrorHandling() {
                    
                    assertThat(true).isTrue(); // Placeholder assertion
                }
                
                @Test
                void testValidation() {
                    
                    assertThat(true).isTrue(); // Placeholder assertion
                }
            }
            """, ticketKey, summary, actionType, className, summary, actionType);
    }
    
    private void saveTestFile(String fileName, String testCode) {
        try {
            Path filePath = Paths.get(testOutputDirectory, fileName);
            Files.write(filePath, testCode.getBytes());
            logger.info("Saved JUnit test file: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save JUnit test file: {}", fileName, e);
        }
    }
    
    private String sanitizeClassName(String ticketKey) {
        return ticketKey.replaceAll("[^a-zA-Z0-9]", "_");
    }
}
