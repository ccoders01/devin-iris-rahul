# Synthetic Test Data for Facility Management System

This directory contains comprehensive synthetic data in CSV format for testing the Java DDD Facility Management application.

## Files Overview

### 1. facilities.csv
- **Records**: 20 facilities
- **Columns**: GFRN, AccountingPeriod, Name, GFCID, CountryOfRisk
- **Coverage**: 
  - Accounting periods: 2023-Q1 through 2024-Q4
  - Countries: USA, Germany, Singapore, Canada, Brazil, UK, Japan, Australia, India, France, Mexico
  - Facility types: Manufacturing, Distribution, R&D, Operations, Headquarters, Offices

### 2. customers.csv
- **Records**: 25 customers
- **Columns**: CAG_ID, GFCID, AccountingPeriod, CountryOfRisk
- **Coverage**:
  - Primary customers (one per facility)
  - Secondary customers (multiple per facility for relationship testing)
  - Historical and current accounting periods
  - Geographic distribution matching facilities

### 3. contracts.csv
- **Records**: 35 contracts
- **Columns**: TransactionID, GFRN, GFCID, DirectAmount, ContingentAmount, AccountingPeriod
- **Coverage**:
  - Contract values: $50K to $10M
  - Multiple contracts per facility/customer
  - Edge cases: Zero contingent amounts, very large amounts
  - Historical and current periods

## Data Characteristics

### Temporal Coverage
- **2023**: Q1, Q2, Q3, Q4
- **2024**: Q1, Q2, Q3, Q4

### Geographic Distribution
- **Americas**: USA, Canada, Brazil, Mexico
- **Europe**: Germany, UK, France
- **Asia-Pacific**: Singapore, Japan, Australia, India

### Referential Integrity
- All contracts reference existing facilities (GFRN) and customers (GFCID)
- Customers are associated with valid facilities
- Multiple relationships supported (many customers per facility, many contracts per customer)

### Edge Cases Covered
- Zero contingent amounts (TXN013, TXN014)
- Small contracts ($50K-$75K)
- Large contracts ($8.5M-$10M)
- Multiple contracts per facility
- Historical data spanning 8 quarters

## Usage in Testing

### Unit Tests
Import CSV data into test fixtures for comprehensive unit testing scenarios.

### Integration Tests
Use as seed data for database integration tests to verify:
- CRUD operations across all domains
- Relationship constraints
- Business logic validation
- Reporting and analytics functions

### Load Testing
Scale up the data patterns for performance testing scenarios.

### Manual Testing
Import into local development environment for manual testing and UI verification.

## Data Generation Notes

The synthetic data maintains realistic business patterns while ensuring:
- Unique identifiers across all domains
- Proper foreign key relationships
- Diverse value ranges for comprehensive testing
- Edge cases for robust validation testing
