package com.facility.management.domain.customer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "CAG ID is required")
    @Column(unique = true)
    private String cagId;
    
    @NotBlank(message = "GFCID is required")
    private String gfcid;
    
    @NotBlank(message = "Accounting period is required")
    private String accountingPeriod;
    
    @NotBlank(message = "Country of risk is required")
    private String countryOfRisk;
    
    public Customer(String cagId, String gfcid, String accountingPeriod, String countryOfRisk) {
        this.cagId = cagId;
        this.gfcid = gfcid;
        this.accountingPeriod = accountingPeriod;
        this.countryOfRisk = countryOfRisk;
    }
}
