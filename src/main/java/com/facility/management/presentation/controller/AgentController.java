package com.facility.management.presentation.controller;

import com.facility.management.application.service.JiraMonitoringService;
import com.facility.management.application.service.SeleniumTestAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    
    private final JiraMonitoringService jiraMonitoringService;
    private final SeleniumTestAgent seleniumTestAgent;
    
    @Autowired
    public AgentController(JiraMonitoringService jiraMonitoringService, SeleniumTestAgent seleniumTestAgent) {
        this.jiraMonitoringService = jiraMonitoringService;
        this.seleniumTestAgent = seleniumTestAgent;
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus() {
        Map<String, Object> status = Map.of(
            "status", "running",
            "monitoring", "active",
            "lastCheck", System.currentTimeMillis(),
            "processedTicketsCount", jiraMonitoringService.getProcessedTicketsCount(),
            "processedTickets", jiraMonitoringService.getProcessedTickets(),
            "testAgentProcessedCount", seleniumTestAgent.getProcessedTestTicketsCount(),
            "testAgentProcessedTickets", seleniumTestAgent.getProcessedTestTickets(),
            "description", "AI Agents monitoring JIRA for facility management and test generation"
        );
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/trigger-monitoring")
    public ResponseEntity<Map<String, String>> triggerMonitoring() {
        jiraMonitoringService.monitorJiraTickets();
        return ResponseEntity.ok(Map.of("message", "JIRA monitoring triggered successfully"));
    }
    
    @PostMapping("/reset-cache")
    public ResponseEntity<Map<String, String>> resetCache() {
        jiraMonitoringService.resetProcessedTickets();
        seleniumTestAgent.resetProcessedTestTickets();
        return ResponseEntity.ok(Map.of("message", "Processed tickets cache reset successfully for both agents"));
    }
    
    @PostMapping("/trigger-test-generation")
    public ResponseEntity<Map<String, String>> triggerTestGeneration() {
        seleniumTestAgent.monitorJiraForTestGeneration();
        return ResponseEntity.ok(Map.of("message", "Selenium test generation triggered successfully"));
    }
}
