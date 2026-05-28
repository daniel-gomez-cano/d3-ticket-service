# ticket-service — VivaEventos

Gestiona tipos de boleta, genera boletas con QR único y valida ingreso en puerta.

## Endpoints

### Tipos de boleta (SCRUM-30 a 34)

| Método | URL | Rol | Task |
|--------|-----|-----|------|
| POST | `/api/ticket-types` | ORGANIZER, ADMIN | SCRUM-31 |
| GET | `/api/ticket-types/event/{eventId}` | Autenticado | SCRUM-32 |
| PUT | `/api/ticket-types/{id}` | ORGANIZER, ADMIN | SCRUM-33 |
| DELETE | `/api/ticket-types/{id}` | ORGANIZER, ADMIN | SCRUM-34 |

### Boletas individuales

| Método | URL | Rol | Descripción |
|--------|-----|-----|-------------|
| POST | `/api/tickets/generate` | ADMIN | Genera boleta con QR tras pago |
| POST | `/api/tickets/validate` | ORGANIZER, ADMIN | Valida QR en puerta |
| GET | `/api/tickets/{ticketId}` | CLIENT, ADMIN | Ver mi boleta |

## Correr en local

```bash
mvn spring-boot:run
# H2 console: http://localhost:8082/h2-console  JDBC: jdbc:h2:mem:ticketdb  user: sa
```

## Agregar al docker-compose del proyecto

Pega el contenido de `docker-compose.snippet.yml` en el `docker-compose.yml` raíz.

```bash
docker-compose up --build ticket-service ticket-db
```

## Flujo de validación en puerta

```
Escaneo QR → POST /api/tickets/validate { "qrToken": "uuid" }
  → ACTIVE  : marca USED, responde { valid: true }
  → USED    : responde { valid: false, message: "ya utilizada el ..." }
  → No existe: responde { valid: false, message: "QR no reconocido" }
```
