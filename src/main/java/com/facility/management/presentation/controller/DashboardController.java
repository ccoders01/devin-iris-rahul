package com.facility.management.presentation.controller;

import com.facility.management.application.service.ContractService;
import com.facility.management.application.service.CustomerService;
import com.facility.management.application.service.FacilityService;
import com.facility.management.application.service.JiraMonitoringService;
import com.facility.management.application.service.SeleniumTestAgent;
import com.facility.management.domain.contract.Contract;
import com.facility.management.domain.customer.Customer;
import com.facility.management.domain.facility.Facility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    
    private final FacilityService facilityService;
    private final CustomerService customerService;
    private final ContractService contractService;
    private final JiraMonitoringService jiraMonitoringService;
    private final SeleniumTestAgent seleniumTestAgent;
    
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("facilitiesCount", facilityService.getAllFacilities().size());
        model.addAttribute("customersCount", customerService.getAllCustomers().size());
        model.addAttribute("contractsCount", contractService.getAllContracts().size());
        return "dashboard";
    }
    
    @GetMapping("/facilities")
    public String facilities(Model model) {
        model.addAttribute("facilities", facilityService.getAllFacilities());
        model.addAttribute("facility", new Facility());
        return "facilities";
    }
    
    @PostMapping("/facilities")
    public String createFacility(@ModelAttribute Facility facility) {
        facilityService.createFacility(facility);
        return "redirect:/facilities";
    }
    
    @GetMapping("/facilities/edit/{id}")
    public String editFacility(@PathVariable Long id, Model model) {
        facilityService.getFacilityById(id).ifPresent(facility -> model.addAttribute("facility", facility));
        return "edit-facility";
    }
    
    @PostMapping("/facilities/update/{id}")
    public String updateFacility(@PathVariable Long id, @ModelAttribute Facility facility) {
        facilityService.updateFacility(id, facility);
        return "redirect:/facilities";
    }
    
    @GetMapping("/facilities/delete/{id}")
    public String deleteFacility(@PathVariable Long id) {
        facilityService.deleteFacility(id);
        return "redirect:/facilities";
    }
    
    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("customer", new Customer());
        return "customers";
    }
    
    @PostMapping("/customers")
    public String createCustomer(@ModelAttribute Customer customer) {
        customerService.createCustomer(customer);
        return "redirect:/customers";
    }
    
    @GetMapping("/customers/edit/{id}")
    public String editCustomer(@PathVariable Long id, Model model) {
        customerService.getCustomerById(id).ifPresent(customer -> model.addAttribute("customer", customer));
        return "edit-customer";
    }
    
    @PostMapping("/customers/update/{id}")
    public String updateCustomer(@PathVariable Long id, @ModelAttribute Customer customer) {
        customerService.updateCustomer(id, customer);
        return "redirect:/customers";
    }
    
    @GetMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/customers";
    }
    
    @GetMapping("/contracts")
    public String contracts(Model model) {
        model.addAttribute("contracts", contractService.getAllContractsWithRelations());
        model.addAttribute("contract", new Contract());
        return "contracts";
    }
    
    @PostMapping("/contracts")
    public String createContract(@ModelAttribute Contract contract) {
        contractService.createContract(contract);
        return "redirect:/contracts";
    }
    
    @GetMapping("/contracts/edit/{id}")
    public String editContract(@PathVariable Long id, Model model) {
        contractService.getContractById(id).ifPresent(contract -> model.addAttribute("contract", contract));
        return "edit-contract";
    }
    
    @PostMapping("/contracts/update/{id}")
    public String updateContract(@PathVariable Long id, @ModelAttribute Contract contract) {
        contractService.updateContract(id, contract);
        return "redirect:/contracts";
    }
    
    @GetMapping("/contracts/delete/{id}")
    public String deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return "redirect:/contracts";
    }
    
    @GetMapping("/agent")
    public String agentMonitoring(Model model) {
        model.addAttribute("processedTicketsCount", jiraMonitoringService.getProcessedTicketsCount());
        model.addAttribute("processedTickets", jiraMonitoringService.getProcessedTickets());
        model.addAttribute("testProcessedTicketsCount", seleniumTestAgent.getProcessedTestTicketsCount());
        model.addAttribute("testProcessedTickets", seleniumTestAgent.getProcessedTestTickets());
        return "agent-monitoring";
    }
}
