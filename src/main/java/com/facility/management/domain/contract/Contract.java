package com.facility.management.domain.contract;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Transaction ID is required")
    @Column(unique = true)
    private String transactionId;
    
    @NotBlank(message = "GFRN is required")
    private String gfrn;
    
    @NotBlank(message = "GFCID is required")
    private String gfcid;
    
    @NotNull(message = "Direct amount is required")
    @DecimalMin(value = "0.0", message = "Direct amount must be positive")
    private BigDecimal directAmount;
    
    @NotNull(message = "Contingent amount is required")
    @DecimalMin(value = "0.0", message = "Contingent amount must be positive")
    private BigDecimal contingentAmount;
    
    @NotBlank(message = "Accounting period is required")
    private String accountingPeriod;
    
    public Contract(String transactionId, String gfrn, String gfcid, BigDecimal directAmount, 
                   BigDecimal contingentAmount, String accountingPeriod) {
        this.transactionId = transactionId;
        this.gfrn = gfrn;
        this.gfcid = gfcid;
        this.directAmount = directAmount;
        this.contingentAmount = contingentAmount;
        this.accountingPeriod = accountingPeriod;
    }
}
