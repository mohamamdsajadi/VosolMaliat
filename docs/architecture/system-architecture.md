# معماری سیستم

Last Updated: 2026-07-15

## هدف سند

توضیح نمای کلان سامانه، مرزهای مؤلفه‌ها، جریان داده، احراز هویت و topology منطقی.

## مخاطبان

معماران، توسعه‌دهندگان ارشد، DevOps و تیم امنیت.

## نمای کلان

```mermaid
flowchart LR
    U[کاربر] --> FE[Next.js Frontend]
    FE --> KC[Keycloak]
    FE --> API[Spring Boot API]
    API --> DB[(SQLite فعلی)]
    API --> CAM[Camunda 8]
    CAM --> W[Job Workers]
    W --> API
    API --> FEN[Fake Electronic Notification]
    API --> FS[Fake SANIM]
```

## مؤلفه‌ها

| مؤلفه | مسئولیت | شواهد |
|---|---|---|
| Frontend | UI فارسی، login، cartable، admin، reports | `src/main/frontend/src/app` |
| Backend API | REST، امنیت، domain logic، DTO | `presentation/api`, `application/service` |
| Domain/Persistence | entityها و repositoryها | `domain/model`, `domain/repository` |
| Camunda | workflow، user task، job worker | `bpmn`, `infrastructure/camunda` |
| Keycloak | احراز هویت و نقش پایه | `SecurityConfig`, `keycloak.js` |
| Fake integrations | شبیه‌سازی ابلاغ و سنیم | `FakeElectronicNotificationService`, `FakeSanimInformService` |

## جریان درخواست کارتابل

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant API as Spring API
    participant SEC as SecurityIdentityService
    participant CAM as Camunda
    FE->>API: GET /api/v1/cartable/my-tasks
    API->>SEC: currentUser()
    API->>CAM: search assigned/candidate tasks
    API->>CAM: fetch task variables
    API->>API: authorization + organization scope
    API-->>FE: CartableTaskResponse[]
```

## جریان صدور برگ اجرا

```mermaid
sequenceDiagram
    participant FE as UI/API Caller
    participant API as Spring API
    participant CAM as Camunda
    participant W as Worker
    participant DB as DB
    FE->>API: ارسال برگ بدهی
    API->>DB: ذخیره درخواست
    API->>CAM: start Process_0rtbdc5
    CAM->>W: check-issuance-possibility-v2
    W->>DB: خواندن قوانین
    CAM->>W: issue-execution-paper
    W->>DB: ایجاد ExecutionPaper
    W-->>CAM: executionPaperId + organization variables
```

## لایه‌ها

- Presentation: کنترلرهای REST.
- Application: سرویس‌ها، DTO، security context، rule engine.
- Domain: entity، enum، repository.
- Infrastructure: Camunda client, workers, message publisher.
- Frontend: Next.js app router، components، API clients.

## اسناد مرتبط

- [معماری بک‌اند](backend-architecture.md)
- [معماری فرانت‌اند](frontend-architecture.md)
- [معماری گردش‌کار](workflow-architecture.md)
- [معماری امنیت](security-architecture.md)

## سوالات باز

- topology تولیدی شامل reverse proxy، TLS، database و Camunda cluster در ریپازیتوری مشخص نیست.
