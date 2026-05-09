# Plasmit Subscription Service

Spring Boot service for managing Plasmit subscription plans and assigning plans to hospitals from the super-admin workflow.

## Tech Stack

- Java 17
- Spring Boot 3.4.4
- Spring Web
- Spring Security
- Spring JDBC
- MySQL
- JJWT
- Maven Wrapper

## Features

- Create, update, list, and activate/deactivate subscription plans.
- Assign subscription plans to hospitals.
- Update hospital subscription mappings.
- Filter subscription plans and hospital mappings.
- Protect subscription APIs with JWT bearer authentication.

## Project Structure

```text
src/main/java/com/plasmit/subscription
├── common          # Shared API response wrapper
├── config          # Spring Security and JDBC configuration
├── controller      # REST endpoints
├── dto             # Request and response DTOs
├── repository      # JDBC database access
├── security        # JWT parsing and tenant context
└── service         # Business logic
```

## Requirements

- JDK 17 or later
- MySQL 8 or later
- Maven is optional because the project includes `mvnw` and `mvnw.cmd`

## Configuration

Default configuration is in `src/main/resources/application.properties`.

```properties
spring.application.name=subscription-service
server.port=8083

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/plasmit_auth?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:hospital_app}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:HospitalApp@123}

jwt.secret=${JWT_SECRET:<same-secret-used-by-auth-service>}
```

Before running locally, make sure:

- MySQL is running.
- The same database used by auth and dashboard exists. The default name is `plasmit_auth`.
- The database contains the `hospitals`, `subscription_plans`, and `hospital_subscriptions` tables.
- `JWT_SECRET` matches the service that issues login tokens.

## VPS Database Setup

Use the same MySQL database that auth and dashboard use. If the database is named `plasmit_auth`, the grant must also target `plasmit_auth.*`.

```sql
CREATE DATABASE IF NOT EXISTS plasmit_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'hospital_app'@'localhost' IDENTIFIED BY '<password>';
GRANT ALL PRIVILEGES ON plasmit_auth.* TO 'hospital_app'@'localhost';
FLUSH PRIVILEGES;
```

Create the subscription tables in that same database:

```bash
mysql -u hospital_app -p plasmit_auth < deploy/mysql-subscription-tables.sql
```

If auth and dashboard already use a different database name, replace `plasmit_auth` everywhere with that existing database name.

## Run Locally

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

The service starts on:

```text
http://localhost:8083
```

## Build

On Windows:

```powershell
.\mvnw.cmd clean package
```

On macOS/Linux:

```bash
./mvnw clean package
```

## VPS Deployment

Build the jar on the VPS or upload a built jar:

```bash
./mvnw clean package
```

The service can run under systemd like the auth and dashboard services. A sample unit is available at:

```text
deploy/plasmit-subscription-service.service.example
```

Copy it to `/etc/systemd/system/plasmit-subscription-service.service`, then update:

- `WorkingDirectory`
- `ExecStart`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`

Then enable and start it:

```bash
sudo systemctl daemon-reload
sudo systemctl enable plasmit-subscription-service.service
sudo systemctl restart plasmit-subscription-service.service
sudo systemctl status plasmit-subscription-service.service --no-pager
```

The service listens on port `8083` by default.

To deploy the latest patch from this repository on the VPS:

```bash
cd /opt/plasmit/plasmit-subscription-service
bash deploy/update-subscription-service.sh
```

The update script pulls `origin/main`, builds the jar, restarts `plasmit-subscription-service.service`, checks port `8083`, and prints the deployed commit.

If `/opt/plasmit/plasmit-subscription-service` does not exist on the VPS, first check where systemd currently runs the service from:

```bash
sudo systemctl cat plasmit-subscription-service.service
sudo systemctl show plasmit-subscription-service.service -p WorkingDirectory -p ExecStart
```

If there is no subscription service project folder under `/opt/plasmit`, clone it:

```bash
cd /opt/plasmit
git clone git@github.com:monusinghbr/plasmit-subscription-service.git plasmit-subscription-service
cd plasmit-subscription-service
bash deploy/update-subscription-service.sh
```

To check auto-start for all Plasmit systemd services:

```bash
bash deploy/check-plasmit-services.sh
```

Equivalent direct command:

```bash
systemctl is-enabled \
plasmit-auth.service \
plasmit-super-admin.service \
plasmit-subscription-service.service \
plasmit-hospital-core.service \
plasmit-patient-hospital.service \
plasmit-appointment-hospital.service \
plasmit-lab-hospital.service \
plasmit-billing-hospital.service \
plasmit-prescription-hospital.service \
plasmit-pharmacy-hospital.service \
plasmit-pharmacy-sales-hospital.service
```

## Authentication

All subscription endpoints under `/api/v1/super-admin/subscriptions/**` require a JWT bearer token.

```http
Authorization: Bearer <token>
```

The token is expected to include:

- `sub`: user id
- `tenantId`: tenant id
- `role`: user role
- `userType`: user type
- `exp`: expiration time

## API Endpoints

Base path:

```text
/api/v1/super-admin/subscriptions
```

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/plans` | List subscription plans. Supports `search` and `status` query params. |
| POST | `/plans` | Create a subscription plan. |
| PUT | `/plans/{planId}` | Update a subscription plan. |
| PATCH | `/plans/{planId}/status` | Update plan status. |
| GET | `/hospital-mappings` | List hospital subscription mappings. Supports `hospitalId`, `planId`, and `status` query params. |
| POST | `/hospital-mappings` | Assign a subscription plan to a hospital. |
| PUT | `/hospital-mappings/{mappingId}` | Update a hospital subscription mapping. |

## Request Examples

Create a plan:

```json
{
  "name": "Starter",
  "billingCycle": "MONTHLY",
  "price": 999,
  "currency": "INR",
  "userLimit": 10,
  "branchLimit": 1,
  "storageLimitGb": 5,
  "includedFeatures": [
    "Patient management",
    "Appointments",
    "Basic reports"
  ]
}
```

Update plan status:

```json
{
  "status": "ACTIVE",
  "reason": "Plan is available for new hospitals"
}
```

Assign a plan to a hospital:

```json
{
  "hospitalId": 1,
  "planId": 1,
  "startDate": "2026-05-01",
  "endDate": "2027-05-01",
  "status": "ACTIVE",
  "paymentStatus": "PENDING",
  "invoiceNumber": "INV-2026-0001"
}
```

Update a hospital mapping:

```json
{
  "planId": 2,
  "status": "ACTIVE",
  "paymentStatus": "PAID",
  "reason": "Upgraded subscription"
}
```

## Response Format

Successful responses use the shared `ApiResponse` wrapper:

```json
{
  "success": true,
  "message": "Subscription plans fetched",
  "data": [],
  "timestamp": "2026-05-08T14:30:00"
}
```

## Validation Rules

- Plan price must be greater than or equal to `0`.
- User, branch, and storage limits must be at least `1`.
- Billing cycle must be `MONTHLY`, `YEARLY`, or `CUSTOM`.
- Plan status must be `ACTIVE` or `INACTIVE`.
- Hospital subscription end date cannot be before start date.

## Database Tables Used

The repository uses these tables:

- `subscription_plans`
- `hospital_subscriptions`
- `hospitals`

Important columns expected by the code include:

- `subscription_plans`: `id`, `name`, `billing_cycle`, `price`, `currency`, `user_limit`, `branch_limit`, `storage_limit_gb`, `included_features`, `status`, `created_by`, `updated_by`, `created_at`, `updated_at`, `is_deleted`
- `hospital_subscriptions`: `id`, `hospital_id`, `plan_id`, `start_date`, `end_date`, `status`, `payment_status`, `renewal_date`, `invoice_number`, `created_by`, `updated_by`, `created_at`, `updated_at`, `is_deleted`
- `hospitals`: `id`, `hospital_name`, `hospital_code`

## Development Notes

- The service uses `NamedParameterJdbcTemplate` instead of JPA.
- Tenant and user details are read from the JWT and stored in `TenantContext` for the current request.
- Subscription APIs are stateless and do not create HTTP sessions.
