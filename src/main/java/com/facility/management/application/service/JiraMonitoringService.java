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
            
            jiraClient.addComment(ticket.getKey(), "🤖 AI Agent: Ticket received and being processed...")
                    .subscribe(
                            v -> logger.info("Added processing comment to ticket {}", ticket.getKey()),
                            error -> logger.error("Error adding comment to ticket {}", ticket.getKey(), error)
                    );
            
            aiAgentService.processJiraTicket(ticket)
                    .subscribe(
                            result -> {
                                logger.info("AI processing completed for ticket {}: {}", ticket.getKey(), result.isSuccess());
                                
                                String comment = result.isSuccess() 
                                        ? "✅ AI Agent: Successfully processed and implemented changes. " + result.getMessage()
                                        : "❌ AI Agent: Processing failed. " + result.getMessage();
                                
                                jiraClient.addComment(ticket.getKey(), comment)
                                        .subscribe(
                                                v -> logger.info("Added result comment to ticket {}", ticket.getKey()),
                                                error -> logger.error("Error adding result comment to ticket {}", ticket.getKey(), error)
                                        );
                            },
                            error -> {
                                logger.error("Error processing ticket {} with AI agent", ticket.getKey(), error);
                                
                                jiraClient.addComment(ticket.getKey(), "❌ AI Agent: Processing error occurred. " + error.getMessage())
                                        .subscribe();
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
}
