package com.facility.management.application.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class LLMService {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    
    private final OpenAiService openAiService;
    private final String model;
    private final boolean isConfigured;
    
    public LLMService(@Value("${ai.api-key:}") String apiKey,
                     @Value("${ai.model:gpt-3.5-turbo}") String model,
                     @Value("${ai.provider:mock}") String provider) {
        this.model = model;
        this.isConfigured = !"mock".equals(provider) && apiKey != null && !apiKey.trim().isEmpty();
        
        if (isConfigured) {
            this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
            logger.info("LLM Service initialized with provider: {} and model: {}", provider, model);
        } else {
            this.openAiService = null;
            logger.info("LLM Service running in mock mode - no API key provided");
        }
    }
    
    public boolean isConfigured() {
        return isConfigured;
    }
    
    public String analyzeRequirements(String ticketSummary, String ticketDescription) {
        if (!isConfigured) {
            return generateMockRequirementAnalysis(ticketSummary, ticketDescription);
        }
        
        try {
            String prompt = buildRequirementAnalysisPrompt(ticketSummary, ticketDescription);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                                "You are an expert business analyst specializing in facility management systems. " +
                                "Analyze JIRA tickets to extract requirements, identify action types, and suggest implementation approaches."),
                            new ChatMessage(ChatMessageRole.USER.value(), prompt)
                    ))
                    .maxTokens(1000)
                    .temperature(0.3)
                    .build();
            
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            return result.getChoices().get(0).getMessage().getContent();
            
        } catch (Exception e) {
            logger.error("Error calling LLM for requirement analysis", e);
            return "Error analyzing requirements: " + e.getMessage();
        }
    }
    
    public String generateImpactAnalysis(String ticketSummary, String ticketDescription, String requirementAnalysis) {
        if (!isConfigured) {
            return generateMockImpactAnalysis(ticketSummary, ticketDescription);
        }
        
        try {
            String prompt = buildImpactAnalysisPrompt(ticketSummary, ticketDescription, requirementAnalysis);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                                "You are a senior software architect with expertise in facility management systems. " +
                                "Analyze the impact of proposed changes on system architecture, dependencies, and implementation complexity."),
                            new ChatMessage(ChatMessageRole.USER.value(), prompt)
                    ))
                    .maxTokens(1500)
                    .temperature(0.2)
                    .build();
            
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            return result.getChoices().get(0).getMessage().getContent();
            
        } catch (Exception e) {
            logger.error("Error calling LLM for impact analysis", e);
            return "Error generating impact analysis: " + e.getMessage();
        }
    }
    
    public String generateAcceptanceCriteria(String ticketSummary, String ticketDescription) {
        if (!isConfigured) {
            return generateMockAcceptanceCriteria(ticketSummary, ticketDescription);
        }
        
        try {
            String prompt = buildAcceptanceCriteriaPrompt(ticketSummary, ticketDescription);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(
                            new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                                "You are a QA expert specializing in writing clear, testable acceptance criteria. " +
                                "Generate Given-When-Then format acceptance criteria for facility management features."),
                            new ChatMessage(ChatMessageRole.USER.value(), prompt)
                    ))
                    .maxTokens(800)
                    .temperature(0.3)
                    .build();
            
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            return result.getChoices().get(0).getMessage().getContent();
            
        } catch (Exception e) {
            logger.error("Error calling LLM for acceptance criteria generation", e);
            return "Error generating acceptance criteria: " + e.getMessage();
        }
    }
    
    private String buildRequirementAnalysisPrompt(String summary, String description) {
        return String.format("""
            Analyze this JIRA ticket and extract the key requirements:
            
            **Summary:** %s
            **Description:** %s
            
            Please provide:
            1. **Action Type**: What type of operation is requested (CREATE_FACILITY, CREATE_CUSTOMER, CREATE_CONTRACT, UPDATE_FACILITY, etc.)
            2. **Key Parameters**: Extract specific values mentioned (GFRN, GFCID, CAG ID, amounts, dates, etc.)
            3. **Business Context**: What business problem is being solved?
            4. **Implementation Approach**: Suggest the best way to implement this requirement
            
            Format your response as structured text that can be easily parsed.
            """, summary, description != null ? description : "No description provided");
    }
    
    private String buildImpactAnalysisPrompt(String summary, String description, String requirementAnalysis) {
        return String.format("""
            Perform an impact analysis for this facility management system change:
            
            **Ticket Summary:** %s
            **Description:** %s
            **Requirement Analysis:** %s
            
            Please analyze:
            1. **System Dependencies**: What other components will be affected?
            2. **Data Impact**: What database tables/entities will be modified?
            3. **Integration Points**: How will this affect JIRA, external APIs, or other systems?
            4. **Risk Assessment**: What are the potential risks and mitigation strategies?
            5. **Testing Considerations**: What areas need thorough testing?
            6. **Implementation Complexity**: Rate complexity (Low/Medium/High) and explain why
            
            Provide actionable insights for the development team.
            """, summary, description != null ? description : "No description provided", requirementAnalysis);
    }
    
    private String buildAcceptanceCriteriaPrompt(String summary, String description) {
        return String.format("""
            Generate comprehensive acceptance criteria for this facility management feature:
            
            **Summary:** %s
            **Description:** %s
            
            Create acceptance criteria in Given-When-Then format covering:
            1. **Happy Path Scenarios**: Normal successful operations
            2. **Edge Cases**: Boundary conditions and unusual inputs
            3. **Error Handling**: What happens when things go wrong
            4. **Validation Rules**: Data validation and business rule enforcement
            5. **Integration Testing**: External system interactions
            
            Each criterion should be specific, testable, and focused on user value.
            """, summary, description != null ? description : "No description provided");
    }
    
    private String generateMockRequirementAnalysis(String summary, String description) {
        return String.format("""
            **Mock Requirement Analysis** (LLM not configured)
            
            **Action Type**: Detected from summary: %s
            **Key Parameters**: Would extract specific values from description
            **Business Context**: Mock analysis of business requirements
            **Implementation Approach**: Suggested implementation strategy
            
            *Note: Configure AI_API_KEY environment variable to enable real LLM analysis*
            """, summary);
    }
    
    private String generateMockImpactAnalysis(String summary, String description) {
        return String.format("""
            **Mock Impact Analysis** (LLM not configured)
            
            **System Dependencies**: Mock dependency analysis
            **Data Impact**: Mock database impact assessment
            **Integration Points**: Mock integration analysis
            **Risk Assessment**: Mock risk evaluation
            **Testing Considerations**: Mock testing recommendations
            **Implementation Complexity**: Medium (mock assessment)
            
            *Note: Configure AI_API_KEY environment variable to enable real impact analysis*
            """);
    }
    
    private String generateMockAcceptanceCriteria(String summary, String description) {
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
}
