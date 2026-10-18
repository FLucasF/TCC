# Implementação do Serviço de Pedidos

## Resumo

Serviço completo de pedidos desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18, implementando todos os requisitos especificados.

## Estrutura do Projeto

### Camadas

1. **API (REST)** - `src/main/java/com/loja/api/`
   - CheckoutEndpoint: Cálculo de resumo
   - OrderEndpoint: Operações de pedidos
   - ProductEndpoint: Consulta de produtos
   - CustomerEndpoint: Dados de clientes
   - NotificationEndpoint: Avisos aos clientes

2. **Serviços** - `src/main/java/com/loja/service/`
   - CheckoutService: Cálculo de resumos e validações complexas
   - OrderService: Ciclo de vida dos pedidos
   - Exceções: CheckoutException, OrderException

3. **Repositórios** - `src/main/java/com/loja/repository/`
   - ProductRepository
   - OrderRepository
   - CustomerRepository
   - NotificationRepository
   - Database: Gerenciador de conexões e schema

4. **Modelos** - `src/main/java/com/loja/model/`
   - Entidades: Product, Order, Customer, Notification
   - Enums: ClubTier, Region, ShippingMethod, PaymentMethod, OrderStatus, NotificationType
   - DTOs: CheckoutRequest, PaymentRequest, ShipmentRequest, CustomerRequest

5. **Estratégias (Strategy Pattern)** - `src/main/java/com/loja/strategy/`
   - CouponStrategy: WELCOME10, MINUS50, FREESHIPPING, BUY3PAY2
   - PaymentStrategy: PIX, CREDIT_CARD, BANK_SLIP
   - ClubBenefitsStrategy: BRONZE, SILVER, GOLD
   - Factories: CouponFactory, PaymentFactory, ClubFactory

6. **Integração com Transportadora** - `src/main/java/com/loja/shipping/`
   - TransLogAdapter: Implementação de ShippingCarrier
   - ShippingCarrier: Interface
   - ShippingQuote: Resposta da transportadora

7. **Utilitários** - `src/main/java/com/loja/util/`
   - Money: Arredondamento HALF_EVEN

## Funcionalidades Implementadas

### Cálculo de Resumo
- ✅ Soma dos produtos
- ✅ Desconto de cupons com validação de condições
- ✅ Cálculo de frete via TransLog
- ✅ Prazo de entrega com ajustes por região
- ✅ Seguro por região com limites
- ✅ Ajuste da forma de pagamento (desconto PIX, juros cartão, tarifa boleto)
- ✅ Crédito do clube
- ✅ Verificação de brinde
- ✅ Arredondamento "meio para o par" em cada etapa

### Cupons
- ✅ WELCOME10: 10% para primeira compra
- ✅ MINUS50: R$ 50 para compras ≥ R$ 300
- ✅ FREESHIPPING: Desconto igual ao frete
- ✅ BUY3PAY2: 1 grátis a cada 3 unidades
- ✅ Teto máximo de 30% de desconto
- ✅ Validação de aplicabilidade

### Formas de Pagamento
- ✅ PIX: 5% de desconto (sem cupom)
- ✅ Cartão: 1-12 parcelas, com juros de 1,99% a.m. após 3x
- ✅ Boleto: Tarifa de R$ 3,49, máx. R$ 1.000

### Entregas
- ✅ ECONOMY: Via TransLog (STD)
- ✅ EXPRESS: Via TransLog (EXP)
- ✅ STORE_PICKUP: Grátis, 1 dia
- ✅ COURIER: R$ 18, 0 dias, máx. 5kg
- ✅ Ajustes de prazo para Norte (+3) e Nordeste (+2)

### Clube
- ✅ BRONZE: Sem benefícios
- ✅ SILVER: 2% de crédito em futuras compras
- ✅ GOLD: 5% de crédito, frete grátis (exceto COURIER), brinde > R$ 500

### Ciclo de Vida dos Pedidos
- ✅ Criação: AWAITING_PAYMENT
- ✅ Pagamento: Verificação de estoque, deduçãode estoque, crédito do clube, notificação
- ✅ Envio: Mudança para SHIPPED com código de rastreio
- ✅ Entrega: Mudança para DELIVERED
- ✅ Cancelamento: Reversão de estoque e crédito se pago
- ✅ Expiração: Cancelamento automático após 48h sem pagamento
- ✅ Idempotência: Mesmo transactionId não processa pagamento duplicado

### Segurança e Validações
- ✅ Validação de entrada em ordem específica
- ✅ Verificação de produtos descontinuados
- ✅ Garantia de estoque suficiente
- ✅ Verificação de CEP com TransLog
- ✅ Limitação de boleto (máx. R$ 1.000)
- ✅ Limite de peso para COURIER (máx. 5kg)

### Notificações
- ✅ PAYMENT_APPROVED: Quando pagamento confirmado
- ✅ ORDER_SHIPPED: Quando envio realizado
- ✅ ORDER_CANCELLED: Quando pedido cancelado
- ✅ Status NEW/READ
- ✅ Rastreabilidade por CPF

### Persistência
- ✅ PostgreSQL com schema automático
- ✅ Catálogo inicial com 8 produtos
- ✅ Histórico de pedidos
- ✅ Rastreamento de transações para evitar duplicatas
- ✅ Saldo de crédito dos clientes

## Design Patterns Usados

1. **Strategy**: Cupons, formas de pagamento, níveis de clube
   - Permite adicionar novos cupons/pagamentos/níveis sem modificar código existente

2. **Factory**: Criação de estratégias
   - CouponFactory, PaymentFactory, ClubFactory

3. **Adapter**: Integração com TransLog
   - TransLogAdapter encapsula a API da transportadora

4. **Repository**: Acesso a dados
   - Isolamento da lógica de banco de dados

## Exemplos de Cálculo

### Exemplo 1: Camiseta + PIX (primeira compra)
- Subtotal: 2 × 79,90 = 159,80
- Cupom WELCOME10: -16,00 (10%)
- Frete (STORE_PICKUP): 0,00
- Seguro (SOUTHEAST): 1,60
- PIX: -8,00 (5%)
- **Total: 137,40**

### Exemplo 2: Jaqueta × 3 + Cartão 3× (GOLD, > R$ 500)
- Subtotal: 3 × 389,90 = 1.169,70
- Cupom: 0,00
- Frete (EXPRESS): -0,00 (GOLD não paga)
- Seguro (NORTH): 30,00 (limite)
- Cartão (3×, sem juros): 0,00
- **Total: 1.199,70 | 3× de 399,90**
- Crédito: 70,98 (5%)
- Brinde: SIM

## Testes

- ✅ CheckoutServiceTest: Testes de resumo
- ✅ mvn verify: Compila, testa e empacota
- ✅ JAR executável gerado: `target/order-service.jar`

## Estrutura do Banco de Dados

```sql
CREATE TABLE products (
  sku VARCHAR(20) PRIMARY KEY,
  name VARCHAR(255),
  price DECIMAL(10,2),
  weight_kg DECIMAL(10,2),
  stock INT,
  discontinued BOOLEAN
)

CREATE TABLE customers (
  cpf VARCHAR(11) PRIMARY KEY,
  credit_balance DECIMAL(10,2)
)

CREATE TABLE orders (
  id VARCHAR(36) PRIMARY KEY,
  cpf VARCHAR(11),
  email VARCHAR(255),
  status VARCHAR(50),
  created_at TIMESTAMP,
  paid_at TIMESTAMP,
  tracking_code VARCHAR(50),
  summary_json TEXT,
  items_json TEXT
)

CREATE TABLE notifications (
  id VARCHAR(36) PRIMARY KEY,
  type VARCHAR(50),
  order_id VARCHAR(36),
  cpf VARCHAR(11),
  status VARCHAR(50)
)

CREATE TABLE payment_tracking (
  order_id VARCHAR(36) PRIMARY KEY,
  transaction_id VARCHAR(255)
)
```

## Como Usar

1. **Compilar**:
   ```bash
   mvn clean verify
   ```

2. **Executar**:
   ```bash
   PORT=8080 DB_URL=jdbc:postgresql://localhost:5432/order_service \
   DB_USER=postgres DB_PASSWORD=postgres \
   java -jar target/order-service.jar
   ```

3. **Testar (exemplo)**:
   ```bash
   curl -X POST http://localhost:8080/checkout/summary \
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

## Notas de Implementação

- Transações sincronizadas para idempotência de pagamentos
- Validações em ordem conforme especificado (para códigos de erro corretos)
- Arredondamento HALF_EVEN para centavos
- Prioridade de regras: GOLD + FREESHIPPING = frete zero (GOLD prevale)
- Cupom WELCOME10 valida status PAID/SHIPPED/DELIVERED, não cancelados
- Suporte para expansão: adicione novas estratégias sem refatorar existente
- Testável: mock repos para unitários, schema automático para integração

## Próximos Passos Opcionais

1. Autenticação/Autorização (JWT, OAuth)
2. Rate limiting
3. Cache de transações
4. Fila de eventos (pedidos, notificações)
5. Relatórios e analytics
6. Dashboard de admin
7. Integração com email/SMS para notificações
8. Mais testes automatizados (integration, E2E)
