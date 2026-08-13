# Employee Management & HR Dashboard — Application Documentation

**Version:** 1.0  
**Date:** August 13, 2026  
**Author:** BRICKREDSYS INDIA PRIVATE LIMITED

---

## 1. Overview

This project consists of **two Spring Boot applications** connected through **Azure Service Bus**:

| Application | Port | Purpose |
|-------------|------|---------|
| **Employee Management** (App 1) | 8080 | Create, update, delete employees; save to MySQL; publish events to Service Bus |
| **HR Dashboard** (App 2) | 8081 | Consume events from Service Bus; display employees on a web dashboard (in-memory) |

**Key design:** HR Dashboard shows data **only from Azure Service Bus messages** stored in memory. It does **not** read from the MySQL database.

---

## 2. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         EMPLOYEE MANAGEMENT (App 1)                          │
│                              http://localhost:8080                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  Browser / REST API                                                        │
│       │                                                                     │
│       ▼                                                                     │
│  EmployeeController  →  EmployeeService  →  EmployeeRepository               │
│       │                        │                    │                       │
│       │                        │                    ▼                       │
│       │                        │              MySQL (employeedb)            │
│       │                        │              Table: employees              │
│       │                        │                                            │
│       │                        ▼                                            │
│       │              ApplicationEventPublisher                              │
│       │                        │                                            │
│       │                        ▼ (after DB commit)                        │
│       │              EmployeeEventPublishListener                           │
│       │                        │                                            │
│       │                        ▼                                            │
│       │              EmployeeEventPublisher  ──────────────┐                │
└───────┼────────────────────────────────────────────────────┼────────────────┘
        │                                                    │
        │                                                    ▼
        │                              ┌─────────────────────────────────────┐
        │                              │      AZURE SERVICE BUS              │
        │                              │      Queue: employee-service        │
        │                              └─────────────────────────────────────┘
        │                                                    │
        │                                                    ▼
┌───────┼────────────────────────────────────────────────────────────────────┐
│       │                    HR DASHBOARD (App 2)                             │
│       │                    http://localhost:8081                            │
├───────┼────────────────────────────────────────────────────────────────────┤
│       │              EmployeeEventConsumer  (listens to queue)              │
│       │                        │                                            │
│       │                        ▼                                            │
│       │              EmployeeSyncService                                    │
│       │                        │                                            │
│       │                        ▼                                            │
│       │              EmployeeEventStore  (in-memory ConcurrentHashMap)      │
│       │                        │                                            │
│       │                        ▼                                            │
│       │              DashboardController  →  index.html (Thymeleaf)         │
└───────┴────────────────────────────────────────────────────────────────────┘
```

---

## 3. Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.2 |
| App 1 Database | MySQL 8 (`employeedb`) |
| App 1 UI | Thymeleaf |
| App 2 UI | Thymeleaf |
| Messaging | Azure Service Bus (`azure-messaging-servicebus` 7.17.4) |
| Build Tool | Maven |

---

## 4. Azure Service Bus Configuration

### 4.1 Required Azure Resources

1. **Service Bus Namespace** (e.g. `employee-servicebus-demo`)
2. **Queue** named `employee-service` (must match config in both apps)

### 4.2 Configuration Files

Both applications use the same configuration pattern:

**`application.properties`** (committed to project):
```properties
app.servicebus.connection-string=${AZURE_SERVICEBUS_CONNECTION_STRING:}
app.servicebus.queue-name=employee-service
spring.config.import=optional:classpath:application-local.properties
```

**`application-local.properties`** (NOT committed — contains secrets):
```properties
app.servicebus.connection-string=Endpoint=sb://YOUR-NAMESPACE.servicebus.windows.net/;SharedAccessKeyName=...;SharedAccessKey=...
```

Copy from `application-local.properties.example` and paste your connection string from:
**Azure Portal → Service Bus Namespace → Shared access policies → RootManageSharedAccessKey → Primary Connection String**

---

## 5. Application 1 — Employee Management

### 5.1 Project Location
```
Desktop\employee-management\
```

### 5.2 Main Features
- Web form to add employees
- REST API for CRUD operations
- Saves employees to MySQL
- Publishes events to Azure Service Bus after successful database commit

### 5.3 Package Structure
```
com.example.employeemanagement/
├── EmployeeManagementApplication.java
├── controller/
│   └── EmployeeController.java
├── service/
│   └── EmployeeService.java
├── repository/
│   └── EmployeeRepository.java
├── model/
│   └── Employee.java
├── dto/
│   └── EmployeeRequest.java
├── event/
│   ├── EmployeeEvent.java
│   ├── EmployeeChangedApplicationEvent.java
│   ├── EmployeeEventPublisher.java
│   └── EmployeeEventPublishListener.java
└── exception/
    └── GlobalExceptionHandler.java
```

### 5.4 API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/` | Employee management web page |
| POST | `/employees/create` | Create employee via HTML form |
| POST | `/employees` | Create employee via REST API (JSON) |
| PUT | `/employees/{id}` | Update employee via REST API |
| DELETE | `/employees/{id}` | Delete employee via REST API |

**Create employee (REST) example:**
```http
POST http://localhost:8080/employees
Content-Type: application/json

{
  "name": "Ananya",
  "department": "IT",
  "email": "ananya@example.com"
}
```

**Update employee example:**
```http
PUT http://localhost:8080/employees/1
Content-Type: application/json

{
  "name": "Ananya Sharma",
  "department": "Engineering",
  "email": "ananya@example.com"
}
```

**Delete employee example:**
```http
DELETE http://localhost:8080/employees/1
```

### 5.5 Event Publishing Flow

1. User creates/updates/deletes an employee
2. `EmployeeService` saves/deletes in MySQL inside a `@Transactional` method
3. After commit, publishes `EmployeeChangedApplicationEvent` (Spring internal event)
4. `EmployeeEventPublishListener` listens with `@TransactionalEventListener(AFTER_COMMIT)`
5. `EmployeeEventPublisher` serializes `EmployeeEvent` to JSON and sends to Azure queue

**Why AFTER_COMMIT?** Ensures the message is only sent if the database operation succeeded.

### 5.6 Event Message Format (JSON)

```json
{
  "id": 1,
  "name": "Ananya",
  "department": "IT",
  "email": "ananya@example.com",
  "eventType": "EMPLOYEE_CREATED",
  "timestamp": "2026-08-13T10:30:00.123456Z"
}
```

**Event types:**
| eventType | When published |
|-----------|----------------|
| `EMPLOYEE_CREATED` | New employee saved |
| `EMPLOYEE_UPDATED` | Employee details changed |
| `EMPLOYEE_DELETED` | Employee removed |

### 5.7 Database Schema (MySQL)

**Database:** `employeedb`  
**Table:** `employees`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| name | VARCHAR | NOT NULL |
| department | VARCHAR | NOT NULL |
| email | VARCHAR | NOT NULL, UNIQUE |

---

## 6. Application 2 — HR Dashboard

### 6.1 Project Location
```
Desktop\HR Dashboard\
```

### 6.2 Main Features
- Listens to Azure Service Bus queue continuously
- Stores received employee data **in memory only**
- Displays employees on a read-only web dashboard
- Does **not** connect to MySQL

### 6.3 Package Structure
```
com.example.hrdashboard/
├── HrDashboardApplication.java
├── consumer/
│   └── EmployeeEventConsumer.java      ← Listens to Service Bus
├── service/
│   ├── EmployeeSyncService.java        ← Routes events to store
│   └── DashboardService.java           ← Serves data to UI
├── store/
│   └── EmployeeEventStore.java         ← In-memory storage
├── controller/
│   ├── DashboardController.java        ← Web page (/)
│   └── DashboardApiController.java     ← REST API (/api/dashboard)
├── dto/
│   ├── EmployeeEvent.java
│   └── DashboardResponse.java
└── model/
    └── EmployeeView.java
```

### 6.4 Web Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/` | HR Dashboard web page (employee table) |
| GET | `/api/dashboard` | JSON API (employees, count, last event) |

### 6.5 Event Consumption Flow

1. **`EmployeeEventConsumer`** starts on application boot (`@PostConstruct`)
2. Connects to Azure queue using `ServiceBusProcessorClient`
3. When a message arrives, calls `processMessage()`:
   - Reads JSON body from message
   - Parses to `EmployeeEvent` using Jackson `ObjectMapper`
   - Calls `EmployeeSyncService.syncFromEvent(event)`
   - On success: `context.complete()` (acknowledge — remove from queue)
   - On failure: `context.abandon()` (return to queue for retry)

4. **`EmployeeSyncService`** handles by event type:
   - `EMPLOYEE_CREATED` / `EMPLOYEE_UPDATED` → add/update in `EmployeeEventStore`
   - `EMPLOYEE_DELETED` → remove from `EmployeeEventStore`

5. **`DashboardController`** reads from store and renders `index.html`

### 6.6 In-Memory Storage

`EmployeeEventStore` uses `ConcurrentHashMap<Long, EmployeeView>`:
- Thread-safe for concurrent message processing
- Data is **lost on application restart**
- Only employees received via Service Bus while the app is running are shown

---

## 7. How to Run the Applications

### 7.1 Prerequisites
- Java 17 installed
- Maven installed
- MySQL running on `localhost:3306` (for App 1 only)
- Azure Service Bus namespace and queue created
- Connection string configured in `application-local.properties` for **both** apps

### 7.2 Start Order (Important)

**Always start HR Dashboard (consumer) BEFORE Employee Management (publisher):**

```powershell
# Terminal 1 — HR Dashboard (start first)
cd "Desktop\HR Dashboard"
mvn spring-boot:run

# Terminal 2 — Employee Management (start second)
cd "Desktop\employee-management"
mvn spring-boot:run
```

### 7.3 Verify Startup

**App 1 logs should show:**
```
Azure Service Bus configured. Events will be published to queue: employee-service
```

**App 2 logs should show:**
```
Listening on Azure Service Bus queue: employee-service
```

### 7.4 Test the Integration

1. Open `http://localhost:8080` — create an employee
2. Open `http://localhost:8081` — refresh page; employee should appear
3. Check App 2 console: `Received EMPLOYEE_CREATED from Service Bus: Ananya (id=1)`

---

## 8. End-to-End Sequence

```
Step 1: User submits employee form on App 1 (localhost:8080)
           │
Step 2: EmployeeService validates and saves to MySQL
           │
Step 3: EmployeeChangedApplicationEvent published (after commit)
           │
Step 4: EmployeeEventPublisher sends JSON to Azure queue "employee-service"
           │
Step 5: EmployeeEventConsumer (App 2) receives message
           │
Step 6: EmployeeSyncService stores employee in EmployeeEventStore (memory)
           │
Step 7: User opens/refreshes http://localhost:8081 — employee visible in table
```

---

## 9. Important Behaviors & Limitations

| Topic | Behavior |
|-------|----------|
| **Data source for dashboard** | Service Bus messages only (in-memory) |
| **App restart** | HR Dashboard loses all displayed employees until new messages arrive |
| **App 2 down during create** | Messages wait in Azure queue; delivered when App 2 starts |
| **Delete Azure queue** | No new events; dashboard shows empty (no DB fallback) |
| **Duplicate messages** | Handled idempotently by employee ID in store |
| **MySQL on App 2** | Not used — removed intentionally |

---

## 10. Troubleshooting

| Problem | Solution |
|---------|----------|
| Dashboard empty after create | Ensure App 2 was running before create; refresh page; check queue name matches in both apps |
| "Service Bus NOT configured" warning | Add connection string to `application-local.properties` |
| "Failed to publish" in App 1 | Verify queue exists in Azure; check connection string |
| Employee not removed on delete | Send DELETE via App 1 API; refresh dashboard page |
| Port already in use | Stop other instance or change `server.port` in `application.properties` |

---

## 11. Security Notes

- Never commit `application-local.properties` (contains Azure keys)
- Regenerate SharedAccessKey if connection string was exposed
- Use separate keys for production with minimal permissions (Send vs Listen)

---

## 12. Project File Summary

### Employee Management (App 1)
| File | Purpose |
|------|---------|
| `EmployeeController.java` | Web + REST endpoints |
| `EmployeeService.java` | Business logic, triggers events |
| `EmployeeEventPublisher.java` | Sends messages to Azure queue |
| `EmployeeEventPublishListener.java` | Publishes after DB commit |
| `EmployeeEvent.java` | Event JSON structure |
| `application.properties` | MySQL + Service Bus config |
| `application-local.properties` | Azure connection string (secret) |

### HR Dashboard (App 2)
| File | Purpose |
|------|---------|
| `EmployeeEventConsumer.java` | Receives messages from Azure queue |
| `EmployeeSyncService.java` | Processes CREATE/UPDATE/DELETE events |
| `EmployeeEventStore.java` | In-memory employee storage |
| `DashboardController.java` | Serves web dashboard |
| `index.html` | Dashboard UI template |
| `application.properties` | Service Bus config |
| `application-local.properties` | Azure connection string (secret) |

---

## 13. Contact & Support

For questions about this integration, refer to:
- Azure Service Bus documentation: https://learn.microsoft.com/en-us/azure/service-bus-messaging/
- Spring Boot documentation: https://spring.io/projects/spring-boot

---

*End of Document*
