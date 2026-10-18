# Serviço de Pedidos - Loja Online

Serviço de backend para gerenciamento de pedidos, pagamentos, envios e estoque de uma loja online de roupas e acessórios.

## Requisitos

- Java 21+
- Maven 3.8+
- PostgreSQL 18 (para produção)

## Compilação e Build

```bash
# Compilar e testar
mvn clean verify

# Gerar JAR executável
mvn clean package
```

O JAR executável será gerado em `target/order-service.jar`.

## Execução

### Variáveis de Ambiente

```bash
# Porta do serviço (padrão: 8080)
PORT=8080

# Banco de dados PostgreSQL
DB_URL=jdbc:postgresql://localhost:5432/order_service
DB_USER=postgres
DB_PASSWORD=postgres

# URL da transportadora TransLog (opcional)
TRANSLOG_URL=https://api.translog.com.br
```

### Rodar localmente

```bash
java -jar target/order-service.jar
```

O serviço estará disponível em `http://localhost:8080`.

## API Endpoints

### Resumo de Compra
```
POST /checkout/summary
Content-Type: application/json

{
  "customer": {"cpf": "12345678909", "email": "ana@exemplo.com"},
  "items": [{"sku": "CAM-001", "quantity": 2}],
  "zipCode": "01310100",
  "region": "SOUTHEAST",
  "shippingMethod": "EXPRESS",
  "coupon": "WELCOME10",
  "paymentMethod": "PIX",
  "installments": 1,
  "clubTier": "BRONZE"
}
```

### Registrar Pedido
```
POST /orders
Content-Type: application/json
```
(mesma estrutura do resumo)

### Consultar Pedido
```
GET /orders/{id}
```

### Confirmar Pagamento
```
POST /orders/{id}/payment
Content-Type: application/json

{
  "transactionId": "TX-123456",
  "paidAt": "2026-10-14T16:00:00Z"
}
```

### Enviar Pedido
```
POST /orders/{id}/shipment
Content-Type: application/json

{
  "trackingCode": "BR123456789"
}
```

### Entregar Pedido
```
POST /orders/{id}/delivery
```

### Cancelar Pedido
```
POST /orders/{id}/cancellation
```

### Consultar Produto
```
GET /products/{sku}
```

### Saldo de Crédito do Cliente
```
GET /customers/{cpf}
```

### Listar Notificações
```
GET /customers/{cpf}/notifications
```

### Marcar Notificação como Lida
```
POST /notifications/{id}/read
```

## Cupons Disponíveis

- **WELCOME10**: 10% de desconto (apenas primeira compra)
- **MINUS50**: R$ 50,00 de desconto (compras acima de R$ 300)
- **FREESHIPPING**: Frete grátis
- **BUY3PAY2**: Compre 3, pague 2 (por produto)

## Níveis de Clube

- **BRONZE**: Sem benefícios
- **SILVER**: 2% de crédito em compras futuras
- **GOLD**: 5% de crédito, frete grátis, brinde em compras acima de R$ 500

## Regiões

- `SOUTHEAST` (Sudeste)
- `SOUTH` (Sul)
- `MIDWEST` (Centro-Oeste)
- `NORTH` (Norte)
- `NORTHEAST` (Nordeste)

## Formas de Entrega

- `ECONOMY`: Transportadora (via TransLog)
- `EXPRESS`: Transportadora Express (via TransLog)
- `STORE_PICKUP`: Retirada na loja (grátis, 1 dia)
- `COURIER`: Motoboy (R$ 18, mesmo dia, máx. 5kg)

## Formas de Pagamento

- `PIX`: 5% de desconto (sem cupom)
- `CREDIT_CARD`: Até 12 parcelas (juros a partir de 4x)
- `BANK_SLIP`: Tarifa de R$ 3,49 (não aceita boleto acima de R$ 1.000)

## Arquitetura

### Design Patterns Utilizados

- **Strategy**: Cupons, formas de pagamento, níveis de clube
- **Adapter**: Integração com TransLog
- **Factory**: Criação de estratégias

### Estrutura de Pacotes

```
com.loja/
├── api/              # Endpoints REST
├── model/            # Modelos de dados
├── repository/       # Acesso a dados
├── service/          # Lógica de negócio
├── strategy/         # Strategy patterns
├── shipping/         # Integração com transportadoras
└── util/             # Utilidades
```

## Banco de Dados

O serviço cria automaticamente o schema na primeira execução. Tabelas:

- `products`: Catálogo de produtos
- `customers`: Dados dos clientes e saldo de crédito
- `orders`: Pedidos registrados
- `notifications`: Notificações para clientes
- `payment_tracking`: Rastreamento de pagamentos (prevenção de duplicatas)

## Cálculo de Valores

Todos os valores são arredondados usando "meio para o par" (HALF_EVEN) em cada etapa:

1. Subtotal dos produtos
2. Desconto do cupom
3. Frete
4. Seguro
5. Ajuste da forma de pagamento
6. Total final

## Seguro de Entrega

O seguro é calculado como porcentagem do valor dos produtos, com limite por região:

- Sudeste/Sul: 1%, sem limite
- Centro-Oeste: 1,5%, sem limite
- Norte: 2,5%, máx. R$ 30
- Nordeste: 2%, máx. R$ 30

## Estados dos Pedidos

1. `AWAITING_PAYMENT`: Aguardando pagamento
2. `PAID`: Pagamento confirmado
3. `SHIPPED`: Pedido enviado
4. `DELIVERED`: Pedido entregue
5. `CANCELLED`: Pedido cancelado

Transições válidas:
- AWAITING_PAYMENT → PAID (via pagamento)
- PAID → SHIPPED (via envio)
- SHIPPED → DELIVERED (via entrega)
- AWAITING_PAYMENT → CANCELLED (antes do envio)
- PAID → CANCELLED (antes do envio)

## Tratamento de Erros

| Código | Descrição | Status HTTP |
|--------|-----------|-------------|
| INVALID_ORDER | Dados inválidos | 422 |
| UNKNOWN_PRODUCT | Produto não encontrado | 422 |
| PRODUCT_DISCONTINUED | Produto descontinuado | 422 |
| INSUFFICIENT_STOCK | Estoque insuficiente | 422 |
| SHIPPING_METHOD_UNAVAILABLE | Entrega não disponível | 422 |
| CARRIER_UNAVAILABLE | Transportadora fora do ar | 503 |
| ORDER_NOT_FOUND | Pedido não encontrado | 404 |
| ORDER_EXPIRED | Pedido expirou (>48h) | 409 |
| OPERATION_NOT_ALLOWED | Operação não permitida | 409 |

## Exemplos de Requisições

### Criar Pedido com PIX
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": {"cpf": "12345678909", "email": "ana@exemplo.com"},
    "items": [{"sku": "CAM-001", "quantity": 1}],
    "zipCode": "01310100",
    "region": "SOUTHEAST",
    "shippingMethod": "STORE_PICKUP",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

### Confirmar Pagamento
```bash
curl -X POST http://localhost:8080/orders/{orderId}/payment \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TX-123456",
    "paidAt": "2026-10-14T16:00:00Z"
  }'
```

## Desenvolvimento

### Rodando testes
```bash
mvn test
```

### IDE Configuration
O projeto está pronto para ser importado em qualquer IDE que suporte Maven (IntelliJ IDEA, VS Code, Eclipse, etc).
