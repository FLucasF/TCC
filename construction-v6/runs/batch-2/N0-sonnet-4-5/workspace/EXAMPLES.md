# Exemplos de Uso da API

## Calcular Resumo (Exemplo 1)

```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "cpf": "12345678909",
      "email": "ana@exemplo.com"
    },
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

**Resposta esperada:**
```json
{
  "productsSubtotal": 409.70,
  "couponDiscount": 40.97,
  "shippingCost": 33.10,
  "deliveryDays": 5,
  "insurance": 10.24,
  "paymentAdjustment": 0.00,
  "finalTotal": 412.07,
  "installments": 1,
  "installmentAmount": 412.07,
  "nextPurchaseCredit": 0.00,
  "gift": false
}
```

## Criar Pedido

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "cpf": "12345678909",
      "email": "ana@exemplo.com"
    },
    "items": [
      {"sku": "CAM-001", "quantity": 2}
    ],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "SILVER"
  }'
```

**Resposta esperada (201):**
```json
{
  "id": 1,
  "status": "AWAITING_PAYMENT",
  "createdAt": "2026-10-18T13:00:00Z",
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
    "nextPurchaseCredit": 3.20,
    "gift": false
  },
  "items": [
    {"sku": "CAM-001", "quantity": 2}
  ],
  "trackingCode": null
}
```

## Registrar Pagamento

```bash
curl -X POST http://localhost:8080/orders/1/payment \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX-123456",
    "paidAt": "2026-10-18T13:30:00Z"
  }'
```

**Resposta esperada (200):**
```json
{
  "id": 1,
  "status": "PAID",
  "createdAt": "2026-10-18T13:00:00Z",
  "summary": { ... },
  "items": [ ... ],
  "trackingCode": null
}
```

## Registrar Envio

```bash
curl -X POST http://localhost:8080/orders/1/shipment \
  -H "Content-Type: application/json" \
  -d '{
    "trackingCode": "BR123456789"
  }'
```

## Registrar Entrega

```bash
curl -X POST http://localhost:8080/orders/1/delivery
```

## Cancelar Pedido

```bash
curl -X POST http://localhost:8080/orders/1/cancellation
```

## Consultar Pedido

```bash
curl http://localhost:8080/orders/1
```

## Consultar Produto

```bash
curl http://localhost:8080/products/CAM-001
```

**Resposta esperada (200):**
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

## Consultar Cliente

```bash
curl http://localhost:8080/customers/12345678909
```

**Resposta esperada (200):**
```json
{
  "cpf": "12345678909",
  "creditBalance": 3.20
}
```

## Consultar Notificações

```bash
curl http://localhost:8080/customers/12345678909/notifications
```

**Resposta esperada (200):**
```json
[
  {
    "id": 1,
    "type": "PAYMENT_APPROVED",
    "orderId": 1,
    "status": "NEW"
  },
  {
    "id": 2,
    "type": "ORDER_SHIPPED",
    "orderId": 1,
    "status": "NEW"
  }
]
```

## Marcar Notificação como Lida

```bash
curl -X POST http://localhost:8080/notifications/1/read
```

## Exemplo de Erro - Produto Sem Estoque

```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {"cpf": "12345678909", "email": "ana@exemplo.com"},
    "items": [{"sku": "BON-001", "quantity": 1}],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

**Resposta esperada (422):**
```json
{
  "error": "INSUFFICIENT_STOCK"
}
```

## Exemplo de Erro - Cupom Não Aplicável

```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {"cpf": "12345678909", "email": "ana@exemplo.com"},
    "items": [{"sku": "CAM-001", "quantity": 1}],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "coupon": "FREESHIPPING",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

**Resposta esperada (422):**
```json
{
  "error": "COUPON_NOT_APPLICABLE"
}
```
