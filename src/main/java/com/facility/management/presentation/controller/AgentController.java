package com.facility.management.presentation.controller;

import com.facility.management.application.service.JiraMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    
    private final JiraMonitoringService jiraMonitoringService;
    
    @Autowired
    public AgentController(JiraMonitoringService jiraMonitoringService) {
        this.jiraMonitoringService = jiraMonitoringService;
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus() {
        Map<String, Object> status = Map.of(
            "status", "running",
            "monitoring", "active",
            "lastCheck", System.currentTimeMillis(),
            "description", "AI Agent monitoring JIRA for facility management tickets"
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
        return ResponseEntity.ok(Map.of("message", "Processed tickets cache reset successfully"));
    }
}
