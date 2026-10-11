# EventPass 🎟️

> High-throughput Event Ticketing REST API built with Spring Boot 4 and Java 25. Engineered with atomic concurrency controls to guarantee zero overselling under parallel booking loads.

[![CI](https://github.com/harsh-rajjj/eventpass/actions/workflows/ci.yml/badge.svg)](https://github.com/harsh-rajjj/eventpass/actions/workflows/ci.yml)

## 📌 Key Engineering Highlights
- **Guaranteed Zero Overselling:** Uses database-level conditional updates (`WHERE sold_count + :qty <= capacity`) inside transactional boundaries to prevent race conditions during high-concurrency ticket drops.
- **Stateless Security:** JWT authentication with role-based access control (`ATTENDEE`, `ORGANIZER`, `ADMIN`).
- **Production-Ready Standards:** RFC 9457 `ProblemDetail` error handling, Flyway database migrations, and OpenAPI 3 documentation.

## 🛠️ Tech Stack
- **Runtime:** Java 25 (LTS), Spring Boot 4.1.1
- **Security:** Spring Security 7, JJWT (Stateless Bearer Tokens)
- **Data & Storage:** Spring Data JPA, Hibernate, MySQL 8.4, Flyway
- **Testing:** JUnit 5, MockMvc, Testcontainers

## 🚀 Quick Start

### Prerequisites
- JDK 25
- Docker & Docker Compose

### Running Locally
```powershell
# Clone the repository
git clone https://github.com/harsh-rajjj/eventpass.git

# Run via Maven Wrapper
./mvnw spring-boot:run```
