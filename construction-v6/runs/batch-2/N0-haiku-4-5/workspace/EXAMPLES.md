# Exemplos de Uso da API

## 1. Calcular Resumo de Compra

Calcula o resumo sem registrar o pedido.

**Request:**
```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "cpf": "12345678901",
      "email": "cliente@example.com"
    },
    "items": [
      {
        "sku": "CAM-001",
        "quantity": 2
      },
      {
        "sku": "TEN-001",
        "quantity": 1
      }
    ],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "EXPRESS",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

**Response:**
```json
{
  "productsSubtotal": 409.70,
  "couponDiscount": 0.00,
  "shippingCost": 33.10,
  "deliveryDays": 2,
  "insurance": 4.10,
  "paymentAdjustment": -20.45,
  "finalTotal": 426.35,
  "installments": 1,
  "installmentAmount": 426.35,
  "nextPurchaseCredit": 0.00,
  "gift": false
}
```

## 2. Registrar um Pedido

**Request:**
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "cpf": "12345678901",
      "email": "cliente@example.com"
    },
    "items": [
      {
        "sku": "CAM-001",
        "quantity": 2
      }
    ],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "AWAITING_PAYMENT",
  "createdAt": "2026-10-18T14:00:00Z",
  "summary": {
    "productsSubtotal": 159.80,
    "couponDiscount": 0.00,
    "shippingCost": 0.00,
    "deliveryDays": 1,
    "insurance": 1.60,
    "paymentAdjustment": -8.07,
    "finalTotal": 153.33,
    "installments": 1,
    "installmentAmount": 153.33,
    "nextPurchaseCredit": 0.00,
    "gift": false
  },
  "items": [
    {
      "sku": "CAM-001",
      "quantity": 2
    }
  ],
  "trackingCode": null
}
```

## 3. Confirmar Pagamento

**Request:**
```bash
curl -X POST http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000/payment \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX-2026101814000001",
    "paidAt": "2026-10-18T14:05:00Z"
  }'
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PAID",
  "createdAt": "2026-10-18T14:00:00Z",
  "summary": { ... },
  "items": [ ... ],
  "trackingCode": null
}
```

## 4. Registrar Envio

**Request:**
```bash
curl -X POST http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000/shipment \
  -H "Content-Type: application/json" \
  -d '{
    "trackingCode": "BR1234567890"
  }'
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SHIPPED",
  "createdAt": "2026-10-18T14:00:00Z",
  "summary": { ... },
  "items": [ ... ],
  "trackingCode": "BR1234567890"
}
```

## 5. Registrar Entrega

**Request:**
```bash
curl -X POST http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000/delivery
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "DELIVERED",
  "createdAt": "2026-10-18T14:00:00Z",
  "summary": { ... },
  "items": [ ... ],
  "trackingCode": "BR1234567890"
}
```

## 6. Cancelar Pedido

**Request:**
```bash
curl -X POST http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000/cancellation
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CANCELLED",
  "createdAt": "2026-10-18T14:00:00Z",
  "summary": { ... },
  "items": [ ... ],
  "trackingCode": null
}
```

## 7. Consultar Pedido

**Request:**
```bash
curl -X GET http://localhost:8080/orders/550e8400-e29b-41d4-a716-446655440000
```

**Response:** (mesmo formato do registro do pedido)

## 8. Consultar Produto

**Request:**
```bash
curl -X GET http://localhost:8080/products/CAM-001
```

**Response:**
```json
{
  "sku": "CAM-001",
  "name": "Camiseta básica",
  "price": 79.90,
  "weightKg": 0.30,
  "stock": 50,
  "discontinued": false
}
```

## 9. Consultar Saldo de Crédito do Cliente

**Request:**
```bash
curl -X GET http://localhost:8080/customers/12345678901
```

**Response:**
```json
{
  "cpf": "12345678901",
  "creditBalance": 10.50
}
```

## 10. Obter Notificações do Cliente

**Request:**
```bash
curl -X GET http://localhost:8080/customers/12345678901/notifications
```

**Response:**
```json
[
  {
    "id": "6f8b0c20-1234-5678-90ab-cdef12345678",
    "type": "PAYMENT_APPROVED",
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "NEW"
  },
  {
    "id": "7f9c1d31-2345-6789-01bc-def123456789",
    "type": "ORDER_SHIPPED",
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "NEW"
  }
]
```

## 11. Marcar Notificação como Lida

**Request:**
```bash
curl -X POST http://localhost:8080/notifications/6f8b0c20-1234-5678-90ab-cdef12345678/read
```

**Response:**
```json
{
  "id": "6f8b0c20-1234-5678-90ab-cdef12345678",
  "type": "PAYMENT_APPROVED",
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "READ"
}
```

## Exemplos Específicos do Escopo

### Exemplo 1: Compra com WELCOME10

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {"cpf": "12345678901", "email": "ana@exemplo.com"},
    "items": [
      {"sku": "CAM-001", "quantity": 2},
      {"sku": "TEN-001", "quantity": 1}
    ],
    "zipCode": "01310100",
    "region": "NORTH",
    "shippingMethod": "EXPRESS",
    "coupon": "WELCOME10",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

**Resultado esperado:**
- Subtotal: 409,70
- Desconto: 40,97
- Frete: 33,10
- Prazo: 5 dias
- Seguro: 10,24
- Ajuste: 0,00
- Total: 412,07

### Exemplo 2: Compra com parcelamento no cartão

```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {"cpf": "98765432101", "email": "bob@exemplo.com"},
    "items": [
      {"sku": "CAM-001", "quantity": 2},
      {"sku": "TEN-001", "quantity": 1}
    ],
    "zipCode": "01310100",
    "region": "MIDWEST",
    "shippingMethod": "ECONOMY",
    "paymentMethod": "CREDIT_CARD",
    "installments": 6,
    "clubTier": "SILVER"
  }'
```

**Resultado esperado:**
- 6 parcelas de 77,00
- Crédito próxima compra: 8,19

### Exemplo 3: Motoboy - Peso acima de 5kg (erro)

```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {"cpf": "11111111111", "email": "test@exemplo.com"},
    "items": [
      {"sku": "JAQ-001", "quantity": 3},
      {"sku": "TEN-001", "quantity": 1}
    ],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "COURIER",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

**Resultado esperado:** Erro `SHIPPING_METHOD_UNAVAILABLE` (peso total = 2.40 + 1.20 = 3.60kg, está ok)

Varie com mais itens para ultrapassar 5kg e receba o erro.

## Tratamento de Erros

### Produto não encontrado
```json
{
  "error": "UNKNOWN_PRODUCT"
}
```
HTTP 422

### Pedido não encontrado
```json
{
  "error": "ORDER_NOT_FOUND"
}
```
HTTP 404

### Operação não permitida
```json
{
  "error": "OPERATION_NOT_ALLOWED"
}
```
HTTP 409

### Estoque insuficiente
```json
{
  "error": "INSUFFICIENT_STOCK"
}
```
HTTP 422

### Transportadora indisponível
```json
{
  "error": "CARRIER_UNAVAILABLE"
}
```
HTTP 503
