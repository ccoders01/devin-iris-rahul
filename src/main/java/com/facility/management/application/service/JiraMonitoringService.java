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
    private final Set<String> processedTickets = new HashSet<>();
    
    @Autowired
    public JiraMonitoringService(JiraClient jiraClient, AIAgentService aiAgentService) {
        this.jiraClient = jiraClient;
        this.aiAgentService = aiAgentService;
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
            logger.info("Processing JIRA ticket: {}", ticket.getKey());
            
            processedTickets.add(ticket.getKey());
            
            jiraClient.updateTicketStatus(ticket.getKey(), "21")
                    .subscribe(
                            v -> logger.info("Transitioned ticket {} to In Progress", ticket.getKey()),
                            error -> logger.warn("Could not transition ticket {} to In Progress: {}", ticket.getKey(), error.getMessage())
                    );
            
            jiraClient.addComment(ticket.getKey(), "🤖 AI Agent: Ticket received and being processed...")
                    .subscribe(
                            v -> logger.info("Added processing comment to ticket {}", ticket.getKey()),
                            error -> logger.error("Error adding comment to ticket {}", ticket.getKey(), error)
                    );
            
            aiAgentService.processJiraTicket(ticket)
                    .subscribe(
                            result -> {
                                logger.info("AI processing completed for ticket {}: {}", ticket.getKey(), result.isSuccess());
                                
                                String comment;
                                String transitionId = null;
                                
                                if (result.isSuccess()) {
                                    comment = "✅ AI Agent: Successfully processed and implemented changes. " + result.getMessage();
                                    transitionId = "31";
                                } else {
                                    comment = "❌ AI Agent: Processing failed. " + result.getMessage();
                                    if (result.getMessage().contains("required") || result.getMessage().contains("not found")) {
                                        comment += " Please update the ticket with the required information and the agent will retry.";
                                    } else {
                                        transitionId = "41";
                                    }
                                }
                                
                                jiraClient.addComment(ticket.getKey(), comment)
                                        .subscribe(
                                                v -> logger.info("Added result comment to ticket {}", ticket.getKey()),
                                                error -> logger.error("Error adding result comment to ticket {}", ticket.getKey(), error)
                                        );
                                
                                if (transitionId != null) {
                                    jiraClient.updateTicketStatus(ticket.getKey(), transitionId)
                                            .subscribe(
                                                    v -> logger.info("Transitioned ticket {} to final status", ticket.getKey()),
                                                    error -> logger.warn("Could not transition ticket {} to final status: {}", ticket.getKey(), error.getMessage())
                                            );
                                } else {
                                    logger.info("Keeping ticket {} in In Progress status for user to address validation issues", ticket.getKey());
                                }
                            },
                            error -> {
                                logger.error("Error processing ticket {} with AI agent", ticket.getKey(), error);
                                
                                String errorComment = "❌ AI Agent: Processing error occurred. " + error.getMessage();
                                jiraClient.addComment(ticket.getKey(), errorComment)
                                        .subscribe();
                                
                                jiraClient.updateTicketStatus(ticket.getKey(), "41")
                                        .subscribe(
                                                v -> logger.info("Transitioned ticket {} to failed status due to processing error", ticket.getKey()),
                                                error2 -> logger.warn("Could not transition ticket {} to failed status: {}", ticket.getKey(), error2.getMessage())
                                        );
                            }
                    );
            
        } catch (Exception e) {
            logger.error("Error processing ticket {}", ticket.getKey(), e);
        }
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
