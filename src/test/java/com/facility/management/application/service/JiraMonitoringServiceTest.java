package com.facility.management.application.service;

import com.facility.management.infrastructure.jira.JiraClient;
import com.facility.management.infrastructure.jira.JiraSearchResponse;
import com.facility.management.infrastructure.jira.JiraTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JiraMonitoringServiceTest {

    @Mock
    private JiraClient jiraClient;
    
    @Mock
    private AIAgentService aiAgentService;

    @InjectMocks
    private JiraMonitoringService jiraMonitoringService;

    private JiraTicket testTicket;
    private JiraSearchResponse searchResponse;

    @BeforeEach
    void setUp() {
        testTicket = new JiraTicket();
        testTicket.setKey("TEST-123");
        
        JiraTicket.Fields fields = new JiraTicket.Fields();
        fields.setSummary("Create new facility");
        fields.setDescription("Test facility creation");
        testTicket.setFields(fields);

        searchResponse = new JiraSearchResponse();
        searchResponse.setIssues(Arrays.asList(testTicket));
        searchResponse.setTotal(1);
    }

    @Test
    void testMonitorJiraTickets_NewTicketsFound() {
        when(jiraClient.isConfigured()).thenReturn(true);
        when(jiraClient.searchRecentTickets(anyInt())).thenReturn(Mono.just(searchResponse));
        when(jiraClient.addComment(any(), any())).thenReturn(Mono.empty());
        when(aiAgentService.processJiraTicket(any())).thenReturn(
            Mono.just(new AIAgentService.ProcessingResult(true, "Success")));

        jiraMonitoringService.monitorJiraTickets();

        verify(jiraClient, times(1)).searchRecentTickets(anyInt());
        verify(aiAgentService, times(1)).processJiraTicket(testTicket);
    }

    @Test
    void testMonitorJiraTickets_NoNewTickets() {
        searchResponse.setIssues(Collections.emptyList());
        searchResponse.setTotal(0);
        
        when(jiraClient.isConfigured()).thenReturn(true);
        when(jiraClient.searchRecentTickets(anyInt())).thenReturn(Mono.just(searchResponse));

        jiraMonitoringService.monitorJiraTickets();

        verify(jiraClient, times(1)).searchRecentTickets(anyInt());
        verify(aiAgentService, never()).processJiraTicket(any());
    }

    @Test
    void testMonitorJiraTickets_DuplicateTicketIgnored() {
        when(jiraClient.isConfigured()).thenReturn(true);
        when(jiraClient.searchRecentTickets(anyInt())).thenReturn(Mono.just(searchResponse));
        when(jiraClient.addComment(any(), any())).thenReturn(Mono.empty());
        when(aiAgentService.processJiraTicket(any())).thenReturn(
            Mono.just(new AIAgentService.ProcessingResult(true, "Success")));

        jiraMonitoringService.monitorJiraTickets();
        jiraMonitoringService.monitorJiraTickets();

        verify(jiraClient, times(2)).searchRecentTickets(anyInt());
        verify(aiAgentService, times(1)).processJiraTicket(testTicket);
    }

    @Test
    void testMonitorJiraTickets_JiraClientError() {
        when(jiraClient.isConfigured()).thenReturn(true);
        when(jiraClient.searchRecentTickets(anyInt())).thenReturn(Mono.error(new RuntimeException("JIRA connection failed")));

        jiraMonitoringService.monitorJiraTickets();

        verify(jiraClient, times(1)).searchRecentTickets(anyInt());
        verify(aiAgentService, never()).processJiraTicket(any());
    }

    @Test
    void testResetProcessedTickets() {
        when(jiraClient.isConfigured()).thenReturn(true);
        when(jiraClient.searchRecentTickets(anyInt())).thenReturn(Mono.just(searchResponse));
        when(jiraClient.addComment(any(), any())).thenReturn(Mono.empty());
        when(aiAgentService.processJiraTicket(any())).thenReturn(
            Mono.just(new AIAgentService.ProcessingResult(true, "Success")));

        jiraMonitoringService.monitorJiraTickets();
        jiraMonitoringService.resetProcessedTickets();
        jiraMonitoringService.monitorJiraTickets();

        verify(aiAgentService, times(2)).processJiraTicket(testTicket);
    }
}
