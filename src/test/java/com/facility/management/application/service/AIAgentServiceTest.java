package com.facility.management.application.service;

import com.facility.management.infrastructure.jira.JiraTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIAgentServiceTest {

    @Mock
    private FacilityService facilityService;
    
    @Mock
    private CustomerService customerService;
    
    @Mock
    private ContractService contractService;
    
    @Mock
    private CodeGenerationService codeGenerationService;
    
    @Mock
    private DeploymentService deploymentService;

    @InjectMocks
    private AIAgentService aiAgentService;

    private JiraTicket testTicket;

    @BeforeEach
    void setUp() {
        testTicket = new JiraTicket();
        testTicket.setKey("TEST-123");
        
        JiraTicket.Fields fields = new JiraTicket.Fields();
        fields.setSummary("Create new facility");
        fields.setDescription("Create facility with GFRN: TEST-GFRN, Name: Test Facility, GFCID: TEST-GFCID, Accounting Period: 2024, Country: US");
        testTicket.setFields(fields);
    }

    @Test
    void testProcessJiraTicket_CreateFacility() {
        when(codeGenerationService.generateFacilityCreationCode(any(), any(), any(), any(), any()))
            .thenReturn("// Generated facility code");
        when(deploymentService.deployChanges(any(), any())).thenReturn(true);

        aiAgentService.processJiraTicket(testTicket)
                .subscribe(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals("Facility creation completed successfully", result.getMessage());
                });
        verify(facilityService, times(1)).createFacility(any());
        verify(deploymentService, times(1)).deployChanges(any(), any());
    }

    @Test
    void testProcessJiraTicket_CreateCustomer() {
        testTicket.getFields().setSummary("Create new customer");
        testTicket.getFields().setDescription("Create customer with CAG ID: TEST-CAG, GFCID: TEST-GFCID, Accounting Period: 2024, Country: US");

        when(codeGenerationService.generateCustomerCreationCode(any(), any(), any(), any()))
            .thenReturn("// Generated customer code");
        when(deploymentService.deployChanges(any(), any())).thenReturn(true);

        aiAgentService.processJiraTicket(testTicket)
                .subscribe(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals("Customer creation completed successfully", result.getMessage());
                });
        verify(customerService, times(1)).createCustomer(any());
    }

    @Test
    void testProcessJiraTicket_CreateContract() {
        testTicket.getFields().setSummary("Create new contract");
        testTicket.getFields().setDescription("Create contract with Transaction ID: TXN-123, GFRN: TEST-GFRN, GFCID: TEST-GFCID, Direct Amount: 1000.00, Contingent Amount: 500.00, Accounting Period: 2024");

        when(codeGenerationService.generateContractCreationCode(any(), any(), any(), any(), any(), any()))
            .thenReturn("// Generated contract code");
        when(deploymentService.deployChanges(any(), any())).thenReturn(true);

        aiAgentService.processJiraTicket(testTicket)
                .subscribe(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals("Contract creation completed successfully", result.getMessage());
                });
        verify(contractService, times(1)).createContract(any());
    }

    @Test
    void testProcessJiraTicket_DeploymentFailure() {
        when(codeGenerationService.generateFacilityCreationCode(any(), any(), any(), any(), any()))
            .thenReturn("// Generated facility code");
        when(deploymentService.deployChanges(any(), any())).thenReturn(false);

        aiAgentService.processJiraTicket(testTicket)
                .subscribe(result -> {
                    assertFalse(result.isSuccess());
                    assertTrue(result.getMessage().contains("Deployment failed"));
                });
    }

    @Test
    void testProcessJiraTicket_UnknownRequirement() {
        testTicket.getFields().setSummary("Unknown request");
        testTicket.getFields().setDescription("This is not a recognized facility management request");

        aiAgentService.processJiraTicket(testTicket)
                .subscribe(result -> {
                    assertFalse(result.isSuccess());
                    assertEquals("Could not determine action from ticket requirements", result.getMessage());
                });
    }

}
