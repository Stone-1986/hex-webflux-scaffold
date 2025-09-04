# ⚡ Manejo Global de Errores — Spring Boot 3.5.4 + WebFlux + Hexagonal

Este módulo implementa un **sistema centralizado de manejo de errores** para aplicaciones reactivas con **Spring WebFlux**, siguiendo los principios de **arquitectura hexagonal**.

El objetivo es ofrecer un formato consistente de errores basado en [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) (**application/problem+json**), desacoplar el **dominio** de los detalles de transporte (HTTP) y facilitar la **trazabilidad** mediante `X-Correlation-Id`.

---

## 🎯 Objetivos de diseño

- **Dominio agnóstico de HTTP** → solo define `Reason` (NOT_FOUND, CONFLICT, etc.).
- **Catálogo de errores del dominio** con códigos estables (`PRB-001`, `PRT-001`).
- **Infraestructura traduce** `Reason → HttpStatus` y serializa en Problem+JSON.
- **Uniformidad** en el formato de respuesta, tanto para errores de negocio, validaciones y fallos técnicos.
- **Observabilidad** mediante `X-Correlation-Id`.

---

## 🗂 Estructura de paquetes

### Dominio (`domain:model`)

```mermaid
com.co.lab.hex.exceptions
├── DomainException.java
├── WithErrors.java
├── Reason.java
│
├── error/
│ ├── ErrorMessage.java
│ └── Errors.java
│
├── business/
│ ├── BusinessErrorMessages.java
│ └── CustomBusinessException.java
│
└── technical/
├── TechnicalErrorMessages.java
└── CustomTechnicalException.java
```

### Infraestructura (`infrastructure:entry-points:reactive-web`)

```mermaid
com.co.lab.hex.errorHandler
├── CorrelationIdFilter.java
├── ProblemDetailsMapper.java
├── CustomErrorAttributes.java
└── GlobalErrorWebExceptionHandlerConfig.jav
```

---

## 🔧 Dependencias clave

```gradle
implementation 'org.springframework.boot:spring-boot-starter-webflux'
implementation 'org.springframework.boot:spring-boot-starter-validation'
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'io.projectreactor:reactor-test'
```

📋 Códigos de error soportados

| Reason                | HTTP | Caso típico                                                      |
| --------------------- | ---- | ---------------------------------------------------------------- |
| `NOT_FOUND`           | 404  | Recurso no existe (`GET /patients/123` y no existe el paciente)  |
| `INVALID_INPUT`       | 400  | Datos mal formados (`birthDate: "2025-30-99"`)                   |
| `CONFLICT`            | 409  | Estado conflictivo (DNI duplicado en creación)                   |
| `UNPROCESSABLE`       | 422  | Regla de negocio incumplida (póliza inactiva)                    |
| `SERVICE_UNAVAILABLE` | 503  | Dependencia caída (DB / microservicio externo / circuit breaker) |
| `INTERNAL_ERROR`      | 500  | Error inesperado (NullPointerException, fallo no controlado)     |

🔑 Diferencia entre 400 y 422

* 400 (INVALID_INPUT): error técnico de formato/entrada.
* 422 (UNPROCESSABLE): entrada correcta, pero regla de negocio inválida.

🔑 Flujo de manejo de errores

1. Handler/UseCase lanza una excepción (CustomBusinessException, CustomTechnicalException, validación, etc.).
2. La excepción burbujea por el pipeline de WebFlux.
3. CustomErrorAttributes captura el error y lo envía a ProblemDetailsMapper.
4. ProblemDetailsMapper traduce la excepción a Problem+JSON:
   * Decide el status (HTTP) en base a Reason o tipo de excepción.
   * Construye el cuerpo uniforme con title, detail, code, errors, correlationId.
   * Usa siempre "type": "about:blank". 
5. GlobalErrorWebExceptionHandlerConfig responde con application/problem+json.

🧪 Ejemplos de uso

1. Error de dominio (422)

```
throw new CustomBusinessException(
BusinessErrorMessages.INSURER_NOT_ACTIVE,
List.of(Errors.e("POLICY_EXPIRED", "Policy expired on 2025-05-01"))
);
```
Respuesta
```json
{
  "type": "about:blank",
  "title": "Business Rule Violation",
  "status": 422,
  "detail": "Insurer not active",
  "instance": "/patients",
  "timestamp": "2025-09-04T22:18:40Z",
  "correlationId": "abc-123",
  "code": "PRB-002",
  "errors": [
    { "code": "POLICY_EXPIRED", "message": "Policy expired on 2025-05-01" }
  ]
}
```

2. Validación (400)
```
POST /patients
Content-Type: application/json

{
"dni": "",
"birthDate": "2025-30-99"
}
```

Respuesta:
```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for argument",
  "instance": "/patients",
  "timestamp": "2025-09-04T22:19:12Z",
  "correlationId": "abc-123",
  "code": "VALIDATION_ERROR",
  "errors": [
      { "field": "dni", "message": "must not be blank" },
      { "field": "birthDate", "message": "invalid date format" }
  ]
}
```

3. Downstream caído (503)

Si un WebClient lanza WebClientResponseException con status=503, el mapper preserva el status.

```json
{
  "type": "about:blank",
  "title": "Service Unavailable",
  "status": 503,
  "detail": "503 Service Unavailable from downstream",
  "instance": "/insurers/ACME",
  "timestamp": "2025-09-04T22:22:05Z",
  "correlationId": "abc-123"
}
```
🧭 Diagrama de flujo (texto) — de throw a respuesta HTTP

```mermaid
flowchart TD
A[Handler / UseCase] -->|"(1) ocurre un error<br/>(dominio, validación, downstream, etc.)"| B[Pipeline WebFlux]
B -->|"(2) excepción burbujea<br/>(filters, handlers, codecs)"| C[GlobalErrorWebExceptionHandler]
C -->|"(3) delega en<br/>CustomErrorAttributes.getErrorAttributes(...)"| D[CustomErrorAttributes]

    D -->|"(4) obtiene Throwable real<br/>(BlockHound unwrap)"| D
    D -->|"(5) resuelve correlationId<br/>(X-Correlation-Id)"| D
    D -->|"(6) invoca<br/>ProblemDetailsMapper.toProblemBody(...)"| E[ProblemDetailsMapper]

    subgraph Status Decision (7)
      E --> F{Tipo de excepción}
      F -->|DomainException| G[Reason → HTTP<br/>(404/409/422/503/500)]
      F -->|Bind / Constraint / Input| H[400]
      F -->|WebClientResponseException| I[Status downstream]
      F -->|Timeout| J[504]
      F -->|Circuit breaker| K[503]
      F -->|Otro| L[500]
    end

    E -->|"(8) arma Problem+JSON<br/>(title, status, detail, cid...)"| D
    D -->|"(9) log WARN (4xx)<br/>o ERROR (5xx)"| D
    D --> M[GlobalErrorWebExceptionHandler]
    M -->|"(10) responde<br/>status + application/problem+json"| N[Cliente HTTP]
    N -->|"(11) recibe respuesta uniforme"| N
```
📎 Referencias

RFC 7807: Problem Details for HTTP APIs
Spring Boot WebFlux Error Handling
Resilience4j Circuit Breaker
