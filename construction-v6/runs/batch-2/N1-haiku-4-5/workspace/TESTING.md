# Testes Manuais

## Setup

1. Inicie o PostgreSQL:
```bash
docker-compose up -d postgres
sleep 5
```

2. Defina as variáveis de ambiente:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/order_db
export DB_USER=order_user
export DB_PASSWORD=order_pass
export PORT=8080
export TRANSLOG_URL=http://mock-translog:8081  # Mock para testes sem TransLog real
```

3. Inicie o serviço:
```bash
java -jar target/order-service.jar
```

O serviço estará disponível em `http://localhost:8080`

## Testes Básicos

### 1. Listar um produto
```bash
curl http://localhost:8080/products/CAM-001
```

Esperado: Status 200 com os dados do produto

### 2. Calcular resumo de compra
```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "cpf": "12345678901",
      "email": "test@example.com"
    },
    "items": [
      {"sku": "CAM-001", "quantity": 2},
      {"sku": "TEN-001", "quantity": 1}
    ],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

Esperado: Status 200 com resumo da compra

### 3. Registrar um pedido
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {
      "cpf": "12345678901",
      "email": "test@example.com"
    },
    "items": [
      {"sku": "CAM-001", "quantity": 2}
    ],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

Esperado: Status 201 com dados do pedido criado

### 4. Consultar um pedido
```bash
curl http://localhost:8080/orders/1
```

Esperado: Status 200 com dados do pedido

### 5. Registrar pagamento
```bash
curl -X POST http://localhost:8080/orders/1/payment \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX-001",
    "paidAt": "2026-10-18T10:00:00Z"
  }'
```

Esperado: Status 200 com pedido atualizado para PAID

### 6. Enviar pedido
```bash
curl -X POST http://localhost:8080/orders/1/shipment \
  -H "Content-Type: application/json" \
  -d '{
    "trackingCode": "BR123456789"
  }'
```

Esperado: Status 200 com pedido atualizado para SHIPPED

## Testes de Validação

### Erro: Carrinho vazio
```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {"cpf": "12345678901", "email": "test@example.com"},
    "items": [],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

Esperado: Status 422 com erro `INVALID_ORDER`

### Erro: Produto descontinuado
```bash
curl -X POST http://localhost:8080/checkout/summary \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {"cpf": "12345678901", "email": "test@example.com"},
    "items": [{"sku": "REL-001", "quantity": 1}],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

Esperado: Status 422 com erro `PRODUCT_DISCONTINUED`

## Notas Importantes

1. **Integração com TransLog**: O serviço tenta conectar ao TransLog para calcular frete. Sem a URL configurada corretamente, as opções ECONOMY e EXPRESS não funcionarão.

2. **Arredondamento**: Todos os valores monetários usam arredondamento "meio para o par" (HALF_EVEN).

3. **Estoque**: O estoque é decrementado apenas quando o pagamento é confirmado, não quando o pedido é registrado.

4. **Cupom WELCOME10**: Só funciona na primeira compra do cliente (quando não há pedidos PAID, SHIPPED ou DELIVERED).

## Limpeza

Para limpar os dados do PostgreSQL:
```bash
docker-compose down -v
```
