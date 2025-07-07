# Facility Management System

A Java application built using Domain-Driven Design (DDD) principles for managing Facilities, Customers, and Contracts.

## Features

- **Domain-Driven Design Architecture**: Clean separation of concerns with domain, application, infrastructure, and presentation layers
- **Three Core Domains**:
  - **Facility**: facilityId, GFRN, accountingPeriod, name, GFCID, countryOfRisk
  - **Customer**: CAGId, GFCID, accountingPeriod, countryOfRisk
  - **Contract**: transactionId, GFRN, GFCID, directAmount, contingentAmount, accountingPeriod
- **Full CRUD Operations**: Create, Read, Update, Delete for all domains
- **Web Dashboard**: Modern, responsive UI built with Bootstrap and Thymeleaf
- **Sample Data**: Pre-loaded test data for immediate use
- **REST API**: RESTful endpoints for all domains

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **H2 Database** (in-memory)
- **Thymeleaf** (templating engine)
- **Bootstrap 5.3.0** (UI framework)
- **Maven** (build tool)

## Project Structure

```
src/main/java/com/facility/management/
├── domain/                     # Domain entities
│   ├── facility/
│   ├── customer/
│   └── contract/
├── application/                # Application services
│   └── service/
├── infrastructure/             # Infrastructure layer
│   └── persistence/           # Repository interfaces
├── presentation/              # Presentation layer
│   ├── controller/           # REST and Web controllers
│   └── dto/                  # Data Transfer Objects
└── FacilityManagementApplication.java
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application:

```bash
mvn spring-boot:run
```

4. Open your browser and navigate to: `http://localhost:8080`

### Database Console

Access the H2 database console at: `http://localhost:8080/h2-console`

- **JDBC URL**: `jdbc:h2:mem:facilitydb`
- **Username**: `sa`
- **Password**: `password`

## API Endpoints

### Facilities
- `GET /api/facilities` - Get all facilities
- `GET /api/facilities/{id}` - Get facility by ID
- `GET /api/facilities/gfrn/{gfrn}` - Get facility by GFRN
- `POST /api/facilities` - Create new facility
- `PUT /api/facilities/{id}` - Update facility
- `DELETE /api/facilities/{id}` - Delete facility

### Customers
- `GET /api/customers` - Get all customers
- `GET /api/customers/{id}` - Get customer by ID
- `GET /api/customers/cag/{cagId}` - Get customer by CAG ID
- `POST /api/customers` - Create new customer
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

### Contracts
- `GET /api/contracts` - Get all contracts
- `GET /api/contracts/{id}` - Get contract by ID
- `GET /api/contracts/transaction/{transactionId}` - Get contract by transaction ID
- `POST /api/contracts` - Create new contract
- `PUT /api/contracts/{id}` - Update contract
- `DELETE /api/contracts/{id}` - Delete contract

## Sample Data

The application comes pre-loaded with sample data:
- 5 Facilities across different regions
- 6 Customers from various countries
- 6 Contracts with different amounts and periods

## Architecture

This application follows Domain-Driven Design principles:

- **Domain Layer**: Contains the core business entities and logic
- **Application Layer**: Orchestrates domain objects and contains application services
- **Infrastructure Layer**: Handles data persistence and external concerns
- **Presentation Layer**: Handles HTTP requests and responses, both for web UI and REST API

## License

This project is licensed under the MIT License.
