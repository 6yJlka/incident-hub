# IncidentHub

[![CI](https://github.com/6yJlka/incident-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/6yJlka/incident-hub/actions/workflows/ci.yml)

IncidentHub — backend-сервис для регистрации и обработки внутренних технических инцидентов. Он связывает инциденты с каталогом бизнес-услуг и помогает определить влияние сбоя по графу зависимостей.

## Как запустить

Для обычного запуска нужны Docker и Docker Compose:

```bash
docker compose up --build
```

Для запуска с демонстрационными командами, пользователями, услугами, зависимостями и инцидентами:

```bash
SPRING_PROFILES_ACTIVE=demo docker compose up --build
```

В Windows PowerShell команда для демо-режима выглядит так:

```powershell
$env:SPRING_PROFILES_ACTIVE='demo'; docker compose up --build
```

После запуска приложение доступно на `http://localhost:8080`, Swagger UI — на [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). При первом старте Docker Compose соберёт приложение, запустит PostgreSQL и применит миграции Flyway. Остановить контейнеры можно командой `docker compose down`.

![Swagger UI](docs/screenshot-swagger.png)

## Что умеет IncidentHub

- Вести каталог бизнес-услуг, их владельцев, уровни критичности и направленные зависимости типов `SYNC`, `ASYNC` и `DATA`.
- Находить услуги, прямо или транзитивно затронутые сбоем, с ограничением глубины обхода и защитой от циклов.
- Регистрировать инциденты, назначать исполнителя и проводить инцидент по жизненному циклу `OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED`; поддерживаются повторное открытие и отмена.
- Хранить неизменяемую историю событий инцидента: создание, назначения и переходы между статусами.
- Предоставлять REST API для работы с услугами, командами, пользователями и инцидентами. Интерактивное описание контрактов и выполнение запросов доступны через [Swagger UI](http://localhost:8080/swagger-ui.html), спецификация OpenAPI — по адресу [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).

## Архитектура

Приложение построено как модульный монолит: оно разворачивается одним процессом и использует одну базу PostgreSQL, а код разделён по бизнес-областям. Такое устройство сохраняет простую эксплуатацию монолита и задаёт явные границы ответственности между частями системы.

| Домен | Ответственность |
|---|---|
| `catalog` | Каталог бизнес-услуг, граф зависимостей и расчёт затронутых услуг |
| `incident` | Регистрация инцидентов, поиск, назначение и жизненный цикл |
| `audit` | Неизменяемая история доменных событий инцидента |
| `team` | Команды-владельцы услуг и ответственные команды |
| `identity` | Пользователи, авторы и исполнители инцидентов |
| `common` | Общая конфигурация API и обработка ошибок |

Внутри доменов код разделён на доменную модель, прикладные сценарии, HTTP-слой и инфраструктуру хранения. Границы выбраны по бизнес-ответственности: каталог отвечает на вопрос о влиянии сбоя, модуль инцидентов управляет работой над ним, а аудит фиксирует произошедшие изменения.

## Стек

| Технология | Версия и назначение |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.7; Web MVC, Data JPA, Validation, Actuator |
| PostgreSQL | 16 Alpine |
| springdoc-openapi | 3.0.1; OpenAPI и Swagger UI |
| Gradle | 9.5.1 через Gradle Wrapper |
| Flyway, Hibernate, JUnit | Версии управляются BOM Spring Boot 4.0.7 |
| Testcontainers | Интеграционные тесты с `postgres:16-alpine` |
| Docker Compose | Локальный запуск приложения и базы данных |

## Доменная модель

```mermaid
erDiagram
    TEAM ||--o{ BUSINESS_SERVICE : owns
    TEAM o|--o{ INCIDENT : responsible_for
    USER ||--o{ INCIDENT : reports
    USER o|--o{ INCIDENT : assigned_to
    BUSINESS_SERVICE ||--o{ INCIDENT : affected_service
    BUSINESS_SERVICE ||--o{ SERVICE_DEPENDENCY : dependent
    BUSINESS_SERVICE ||--o{ SERVICE_DEPENDENCY : dependency
    INCIDENT ||--o{ INCIDENT_AUDIT_EVENT : records

    TEAM {
        bigint id
        string code
        string name
        boolean active
    }
    USER {
        bigint id
        string email
        string display_name
        boolean active
    }
    BUSINESS_SERVICE {
        bigint id
        string code
        string name
        enum tier
        boolean active
    }
    SERVICE_DEPENDENCY {
        bigint id
        enum type
    }
    INCIDENT {
        bigint id
        string title
        enum source
        enum priority
        enum severity
        enum status
    }
    INCIDENT_AUDIT_EVENT {
        bigint id
        enum event_type
        enum from_status
        enum to_status
        timestamp created_at
    }
```

Зависимость направлена от `dependent` к `dependency`: если недоступна `dependency`, затронутой считается услуга `dependent` и все зависящие от неё услуги. Каждый инцидент относится к одной затронутой услуге, имеет автора и может иметь ответственную команду и назначенного исполнителя.

## В планах

- Аутентификация, ролевая авторизация и получение автора запроса из учётной записи.
- Веб-интерфейс для операторов и ответственных команд.
- Бэклог продукта: публичный деплой, текстовая хроника инцидентов, приём событий мониторинга, группировка связанных инцидентов, подписки и эскалации, SLA, постмортемы и аналитика.
