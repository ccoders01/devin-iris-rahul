package com.facility.management.application.service;

import com.facility.management.domain.facility.Facility;
import com.facility.management.infrastructure.persistence.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FacilityService {
    
    private final FacilityRepository facilityRepository;
    
    public List<Facility> getAllFacilities() {
        return facilityRepository.findAll();
    }
    
    public Optional<Facility> getFacilityById(Long id) {
        return facilityRepository.findById(id);
    }
    
    public Optional<Facility> getFacilityByGfrn(String gfrn) {
        return facilityRepository.findByGfrn(gfrn);
    }
    
    public Facility createFacility(Facility facility) {
        return facilityRepository.save(facility);
    }
    
    public Facility updateFacility(Long id, Facility facilityDetails) {
        return facilityRepository.findById(id)
                .map(facility -> {
                    facility.setGfrn(facilityDetails.getGfrn());
                    facility.setAccountingPeriod(facilityDetails.getAccountingPeriod());
                    facility.setName(facilityDetails.getName());
                    facility.setGfcid(facilityDetails.getGfcid());
                    facility.setCountryOfRisk(facilityDetails.getCountryOfRisk());
                    return facilityRepository.save(facility);
                })
                .orElseThrow(() -> new RuntimeException("Facility not found with id: " + id));
    }
    
    public void deleteFacility(Long id) {
        facilityRepository.deleteById(id);
    }
    
    public List<Facility> getFacilitiesByCountryOfRisk(String countryOfRisk) {
        return facilityRepository.findByCountryOfRisk(countryOfRisk);
    }
    
    public List<Facility> getFacilitiesByAccountingPeriod(String accountingPeriod) {
        return facilityRepository.findByAccountingPeriod(accountingPeriod);
    }
}
