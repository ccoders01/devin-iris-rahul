package com.facility.management.domain.facility;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "facilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facility {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long facilityId;
    
    @NotBlank(message = "GFRN is required")
    @Column(unique = true)
    private String gfrn;
    
    @NotBlank(message = "Accounting period is required")
    private String accountingPeriod;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "GFCID is required")
    private String gfcid;
    
    @NotBlank(message = "Country of risk is required")
    private String countryOfRisk;
    
    public Facility(String gfrn, String accountingPeriod, String name, String gfcid, String countryOfRisk) {
        this.gfrn = gfrn;
        this.accountingPeriod = accountingPeriod;
        this.name = name;
        this.gfcid = gfcid;
        this.countryOfRisk = countryOfRisk;
    }
}
