package com.facility.management.presentation.controller;

import com.facility.management.application.service.FacilityService;
import com.facility.management.domain.facility.Facility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FacilityController {
    
    private final FacilityService facilityService;
    
    @GetMapping
    public ResponseEntity<List<Facility>> getAllFacilities() {
        List<Facility> facilities = facilityService.getAllFacilities();
        return ResponseEntity.ok(facilities);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Facility> getFacilityById(@PathVariable Long id) {
        return facilityService.getFacilityById(id)
                .map(facility -> ResponseEntity.ok(facility))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/gfrn/{gfrn}")
    public ResponseEntity<Facility> getFacilityByGfrn(@PathVariable String gfrn) {
        return facilityService.getFacilityByGfrn(gfrn)
                .map(facility -> ResponseEntity.ok(facility))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Facility> createFacility(@Valid @RequestBody Facility facility) {
        try {
            Facility createdFacility = facilityService.createFacility(facility);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdFacility);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Facility> updateFacility(@PathVariable Long id, @Valid @RequestBody Facility facilityDetails) {
        try {
            Facility updatedFacility = facilityService.updateFacility(id, facilityDetails);
            return ResponseEntity.ok(updatedFacility);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFacility(@PathVariable Long id) {
        try {
            facilityService.deleteFacility(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/country/{countryOfRisk}")
    public ResponseEntity<List<Facility>> getFacilitiesByCountryOfRisk(@PathVariable String countryOfRisk) {
        List<Facility> facilities = facilityService.getFacilitiesByCountryOfRisk(countryOfRisk);
        return ResponseEntity.ok(facilities);
    }
    
    @GetMapping("/period/{accountingPeriod}")
    public ResponseEntity<List<Facility>> getFacilitiesByAccountingPeriod(@PathVariable String accountingPeriod) {
        List<Facility> facilities = facilityService.getFacilitiesByAccountingPeriod(accountingPeriod);
        return ResponseEntity.ok(facilities);
    }
}
