package com.facility.management.application.service;

import com.facility.management.infrastructure.jira.JiraClient;
import com.facility.management.infrastructure.jira.JiraTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JiraMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(JiraMonitoringService.class);
    
    private final JiraClient jiraClient;
    private final AIAgentService aiAgentService;
    private final LLMService llmService;
    private final Set<String> processedTickets = new HashSet<>();
    
    @Autowired
    public JiraMonitoringService(JiraClient jiraClient, AIAgentService aiAgentService, LLMService llmService) {
        this.jiraClient = jiraClient;
        this.aiAgentService = aiAgentService;
        this.llmService = llmService;
    }
    
    @Scheduled(fixedRate = 300000)
    public void monitorJiraTickets() {
        if (!jiraClient.isConfigured()) {
            logger.debug("JIRA client not configured, skipping monitoring");
            return;
        }
        
        logger.info("Starting JIRA ticket monitoring");
        
        jiraClient.searchRecentTickets(50)
                .subscribe(
                        response -> {
                            if (response != null && response.getIssues() != null) {
                                List<JiraTicket> facilityTickets = response.getIssues().stream()
                                        .filter(JiraTicket::isFacilityManagementTicket)
                                        .filter(ticket -> !processedTickets.contains(ticket.getKey()))
                                        .collect(Collectors.toList());
                                
                                logger.info("Found {} new facility management tickets", facilityTickets.size());
                                
                                for (JiraTicket ticket : facilityTickets) {
                                    processTicket(ticket);
                                }
                            }
                        },
                        error -> logger.error("Error monitoring JIRA tickets", error)
                );
    }
    
    private void processTicket(JiraTicket ticket) {
        try {
            String correlationId = "WORKFLOW-" + ticket.getKey() + "-" + System.currentTimeMillis();
            logger.info("[{}] Starting two-agent workflow for JIRA ticket: {}", correlationId, ticket.getKey());
            
            processedTickets.add(ticket.getKey());
            
            jiraClient.updateTicketStatus(ticket.getKey(), "21")
                    .subscribe(
                            v -> logger.info("[{}] Transitioned ticket {} to In Progress", correlationId, ticket.getKey()),
                            error -> logger.warn("[{}] Could not transition ticket {} to In Progress: {}", correlationId, ticket.getKey(), error.getMessage())
                    );
            
            startDevelopmentAgentWorkflow(ticket, correlationId);
            
        } catch (Exception e) {
            logger.error("Error processing ticket {}", ticket.getKey(), e);
        }
    }
    
    private void startDevelopmentAgentWorkflow(JiraTicket ticket, String correlationId) {
        logger.info("[{}] Starting Development Agent (Agent 1) workflow", correlationId);
        
        String summary = ticket.getFields().getSummary();
        String description = ticket.getFields().getDescription();
        
        String requirementAnalysis = llmService.analyzeRequirements(summary, description);
        String impactAnalysis = llmService.generateImpactAnalysis(summary, description, requirementAnalysis);
        String approachDesign = llmService.generateApproachDesign(summary, description, requirementAnalysis, impactAnalysis);
        
        String analysisComment = String.format("""
            🤖 **Development Agent (Agent 1) - LLM Analysis**
            
            **Requirement Analysis:**
            %s
            
            **Impact Analysis:**
            %s
            
            **Approach Design:**
            %s
            
            Starting code generation and JUnit test creation...
            """, requirementAnalysis, impactAnalysis, approachDesign);
        
        jiraClient.addComment(ticket.getKey(), analysisComment)
                .subscribe(
                        v -> logger.info("[{}] Added Development Agent analysis comment to ticket {}", correlationId, ticket.getKey()),
                        error -> logger.error("[{}] Failed to add analysis comment to ticket {}", correlationId, ticket.getKey(), error)
                );
        
        aiAgentService.processJiraTicket(ticket)
                .subscribe(
                        result -> {
                            logger.info("[{}] Development Agent processing completed for ticket {}: {}", correlationId, ticket.getKey(), result.isSuccess());
                            
                            if (result.isSuccess()) {
                                String devComment = "✅ **Development Agent (Agent 1) - Completed**\n\n" +
                                                  "- ✅ LLM Impact Analysis\n" +
                                                  "- ✅ Code Generation\n" +
                                                  "- ✅ JUnit Test Creation\n" +
                                                  "- ✅ Deployment\n\n" +
                                                  result.getMessage() + "\n\n" +
                                                  "🔄 **Ready for Selenium Testing Agent (Agent 2)**";
                                
                                jiraClient.addComment(ticket.getKey(), devComment)
                                        .subscribe(
                                                v -> logger.info("[{}] Added Development Agent completion comment - Agent 2 will pick up automatically", correlationId),
                                                error -> logger.error("[{}] Error adding dev completion comment", correlationId, error)
                                        );
                            } else {
                                String failComment = "❌ **Development Agent (Agent 1) - Failed**\n\n" + result.getMessage();
                                if (result.getMessage().contains("required") || result.getMessage().contains("not found")) {
                                    failComment += "\n\nPlease update the ticket with the required information and the agent will retry.";
                                }
                                
                                jiraClient.addComment(ticket.getKey(), failComment)
                                        .subscribe(
                                                v -> logger.info("[{}] Added Development Agent failure comment", correlationId),
                                                error -> logger.error("[{}] Error adding failure comment", correlationId, error)
                                        );
                                
                                if (!result.getMessage().contains("required") && !result.getMessage().contains("not found")) {
                                    jiraClient.updateTicketStatus(ticket.getKey(), "41")
                                            .subscribe(
                                                    v -> logger.info("[{}] Transitioned ticket {} to failed status", correlationId, ticket.getKey()),
                                                    error -> logger.warn("[{}] Could not transition ticket {} to failed status", correlationId, ticket.getKey())
                                            );
                                }
                            }
                        },
                        error -> {
                            logger.error("[{}] Error in Development Agent processing for ticket {}", correlationId, ticket.getKey(), error);
                            
                            String errorComment = "❌ **Development Agent (Agent 1) - Error**\n\nProcessing error occurred: " + error.getMessage();
                            jiraClient.addComment(ticket.getKey(), errorComment)
                                    .subscribe();
                            
                            jiraClient.updateTicketStatus(ticket.getKey(), "41")
                                    .subscribe(
                                            v -> logger.info("[{}] Transitioned ticket {} to failed status due to processing error", correlationId, ticket.getKey()),
                                            error2 -> logger.warn("[{}] Could not transition ticket {} to failed status", correlationId, ticket.getKey())
                                    );
                        }
                );
    }
    
    public void resetProcessedTickets() {
        processedTickets.clear();
        logger.info("Reset processed tickets cache");
    }
    
    public int getProcessedTicketsCount() {
        return processedTickets.size();
    }
    
    public Set<String> getProcessedTickets() {
        return new HashSet<>(processedTickets);
    }
}
