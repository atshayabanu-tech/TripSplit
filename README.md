# TripSplit — Group Travel Expense Settlement API

Professional Spring Boot REST API for shared travel expenses and settlement.

## This version does NOT require Docker

### Requirements
- Java 21 JDK
- Maven 3.9+
- MySQL 8.0+

## 1. Install MySQL

Install MySQL Server 8.x on Windows and make sure the MySQL service is running.

Then open **MySQL Command Line Client** (or MySQL Workbench) as an administrator and run the contents of:

```text
database/setup.sql
```

The setup creates:
- Database: `tripsplit`
- Username: `tripsplit`
- Password: `tripsplit`

If you prefer a different MySQL username/password, set these environment variables before starting the application:

```powershell
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
```

You can also change `DB_URL` if MySQL is not using port 3306.

## 2. Check Java and Maven

Open PowerShell in the project directory:

```powershell
java -version
mvn -version
```

Java 21 and Maven 3.9+ are recommended.

## 3. Run the application

From the project root:

```powershell
mvn spring-boot:run
```

You should eventually see a message similar to:

```text
Started TripSplitApplication
```

## 4. Open the API

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

Swagger UI is the easiest way to demonstrate the project.

## 5. Test the complete flow

1. `POST /api/v1/trips` — create a trip
2. `POST /api/v1/trips/{tripId}/participants` — add participants
3. `POST /api/v1/trips/{tripId}/expenses` — record expenses
4. `GET /api/v1/trips/{tripId}` — view the complete trip
5. `GET /api/v1/trips/{tripId}/settlement` — calculate who pays whom

Example trip:

```json
{"name":"Goa Weekend"}
```

Example participant:

```json
{"name":"Asha","email":"asha@example.com"}
```

Example expense:

```json
{"payerId":"<participant-id>","description":"Hotel","amount":6000,"currency":"INR"}
```

## Business rules implemented

- Participants must have valid names and email addresses.
- Participant emails are unique within a trip.
- An expense payer must belong to the same trip.
- Settlement requires at least one participant.
- Settlement supports one currency per trip settlement.
- Equal-split balances are calculated to two decimal places.
- A greedy settlement algorithm produces transfers between debtors and creditors.
- Validation and structured API error responses are included.

## Project structure

```text
tripsplit-project/
├── database/
│   └── setup.sql
├── src/
│   ├── main/
│   │   ├── java/com/tripsplit/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── error/
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   └── test/
├── pom.xml
└── README.md
```

## Stop the application

Press `Ctrl + C` in the PowerShell window running Maven.

## Notes

Hibernate is configured with `ddl-auto: update` for evaluation/development convenience. For a production deployment, use database migrations such as Flyway or Liquibase and external secret management.
