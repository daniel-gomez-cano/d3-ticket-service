# ticket-service — VivaEventos

Genera boletas con QR único y valida ingreso en puerta.

## Endpoints

### Boletas individuales

| Método | URL | Rol | Descripción |
|--------|-----|-----|-------------|
| POST | `/api/tickets/generate` | ADMIN | Genera boleta con QR tras pago |
| POST | `/api/tickets/validate` | ORGANIZER, ADMIN | Valida QR en puerta |
| GET | `/api/tickets/{ticketId}` | CLIENT, ADMIN | Ver mi boleta |

# IMPORTANTE
Si tenía una versión antigua de hace 3 días de la base de datos PostgreSQL, deberá de borrar la tabla que sobrevivió a la refactorización del ticket-service (donde antes gestionaba los tipos de tiquetes y ahora no, pero al tener persistencia la BD siguió con una FK no debida).
### HACER:
```SQL
ALTER TABLE tickets
DROP CONSTRAINT IF EXISTS fkotik7mbbb14hu8n9og7o92k5h;

DROP TABLE IF EXISTS ticket_types;

/* TicketType fue migrado al event-service.
El ticket-service ya no mantiene tabla ticket_types ni FK hacia ella.*/
```

## Flujo para probar la App hasta ahora
```bash
1. Registrar ADMIN
2. Registrar ORGANIZER
3. Registrar CLIENT
4. Verificar organizer
5. Crear evento
6. Publicar evento
7. Crear carrito
8. Agregar item
9. Checkout
10. Obtener Payment URL
11. Pagar en Stripe
12. Generar ticket
13. Consultar ticket
14. Validar QR
15. Revalidar QR
```

## Guía con curls
```bash
# ── SETUP ─────────────────────────────────────────────────────────────────────
# Port-forwards activos para todos los servicios
# Variables de entorno del mensaje anterior cargadas en Git Bash

# ── FASE 0: REGISTRO ──────────────────────────────────────────────────────────

# Registrar los 3 usuarios (ya no falla con 409 tras el fix)
curl -s -X POST "http://localhost:8081/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin.sistema","email":"admin@viva.com","firstName":"Admin","lastName":"Sistema","password":"Admin1234","role":"ADMIN"}'

curl -s -X POST "http://localhost:8081/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"organizer.test","email":"organizer@viva.com","firstName":"Organizer","lastName":"Test","password":"Organ1234","role":"ORGANIZER"}'

curl -s -X POST "http://localhost:8081/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"client.test","email":"client@viva.com","firstName":"Client","lastName":"Test","password":"Client1234","role":"CLIENT"}'

# Obtener tokens
export ADMIN_TOKEN=$(get_token "admin.sistema" "Admin1234")
export ORGANIZER_TOKEN=$(get_token "organizer.test" "Organ1234")
export CLIENT_TOKEN=$(get_token "client.test" "Client1234")

# ── FASE 1: ORGANIZER ─────────────────────────────────────────────────────────

# Ver perfil (confirma que el registro pobló la BD local)
curl -s http://localhost:8081/creator/me \
  -H "Authorization: Bearer $ORGANIZER_TOKEN" | python -m json.tool

# ADMIN obtiene lista de organizers y verifica al organizer
curl -s http://localhost:8081/admin/organizers \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python -m json.tool

export ORGANIZER_KC_ID="<id-del-organizer-en-la-respuesta>"

curl -s -X PUT "http://localhost:8081/admin/organizers/$ORGANIZER_KC_ID/verify" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Crear evento con ticket types embebidos
curl -s -X POST "http://localhost:8082/api/v1/events" \
  -H "Authorization: Bearer $ORGANIZER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Concierto Rock Cali 2026",
    "description": "El mejor festival del año",
    "eventDate": "2026-12-15T20:00:00",
    "location": "Centro de Eventos Valle del Pacífico, Cali",
    "imageUrl": "https://example.com/rock.jpg",
    "maxCapacity": 500,
    "ticketTypes": [
      {"name": "General", "price": 80000, "quantity": 400},
      {"name": "VIP",     "price": 200000, "quantity": 100}
    ]
  }' | python -m json.tool

export EVENT_ID=<id-del-evento>
export TICKET_TYPE_ID_GENERAL=<id-del-tipo-general>

# Verificar el nuevo endpoint interno (confirma el cambio de arquitectura)
curl -s "http://localhost:8082/api/internal/ticket-types/$TICKET_TYPE_ID_GENERAL" | python -m json.tool
# Esperado: {id, name, price, remainingCapacity, active, eventId}

# Publicar evento
curl -s -X PATCH "http://localhost:8082/api/v1/events/$EVENT_ID/publish" \
  -H "Authorization: Bearer $ORGANIZER_TOKEN" | python -m json.tool

# ── FASE 2: CLIENT ────────────────────────────────────────────────────────────

# Ver catálogo (público)
curl -s "http://localhost:8082/api/v1/events" | python -m json.tool

# Crear carrito
curl -s "http://localhost:8083/api/cart" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | python -m json.tool
export CART_ID="<id-del-carrito>"

# Agregar ítem — ahora llama a event-service internamente, no a ticket-service
curl -s -X POST "http://localhost:8083/api/cart/items" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"ticketTypeId\": \"$TICKET_TYPE_ID_GENERAL\", \"quantity\": 2}" | python -m json.tool

# Ver resumen
curl -s "http://localhost:8083/api/cart/summary" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | python -m json.tool

# Checkout (publica a RabbitMQ → payment-service crea preferencia MP)
curl -s -X POST "http://localhost:8083/api/cart/checkout" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "client@viva.com"}' | python -m json.tool

# Esperar ~2 segundos y consultar el pago creado por el async flow
sleep 2
curl -s "http://localhost:8084/api/payments/cart/$CART_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | python -m json.tool
# Esperado: {status: PENDING, paymentUrl: "https://checkout.stripe.com/c/pay/cs_test..."}

# ── FASE 3: PAGO EN SANDBOX ───────────────────────────────────────────────────
# Abre el paymentUrl en el navegador
# Paga con tarjeta 4242 4242 4242 4242 / CVV 123 / fecha valida

# ── FASE 4: TICKET ────────────────────────────────────────────────────────────
# (POR HACER: Conexión RabbitMQ para generación automática)
export CLIENT_KC_ID=$(decode_jwt $CLIENT_TOKEN | python -c "import sys,json; print(json.load(sys.stdin)['sub'])")

# (USAR ESTA POR AHORA: Si es manual — MVP)
curl -s -X POST "http://localhost:8085/api/tickets/generate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": \"$CART_ID\",
    \"ticketTypeId\": \"$TICKET_TYPE_ID_GENERAL\",
    \"ticketTypeName\": \"General\",
    \"eventId\": \"$EVENT_ID\",
    \"buyerId\": \"$CLIENT_KC_ID\"
  }" | python -m json.tool
export TICKET_ID="<id-del-ticket>"

# CLIENT consulta su ticket con el QR
curl -s "http://localhost:8085/api/tickets/$TICKET_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN" | python -m json.tool

# ORGANIZER valida el QR en puerta
curl -s -X POST "http://localhost:8085/api/tickets/validate" \
  -H "Authorization: Bearer $ORGANIZER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"qrToken": "<qrToken-del-ticket>"}' | python -m json.tool
# Esperado: {"valid": true, "message": "Acceso permitido"}

# Segundo intento de escaneo (debe fallar)
curl -s -X POST "http://localhost:8085/api/tickets/validate" \
  -H "Authorization: Bearer $ORGANIZER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"qrToken": "<qrToken-del-ticket>"}' | python -m json.tool
# Esperado: {"valid": false, "message": "Boleta ya utilizada el ..."}
```