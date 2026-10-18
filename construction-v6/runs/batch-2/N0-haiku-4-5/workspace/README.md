# Order Service - Serviço de Processamento de Pedidos

Sistema completo de processamento de pedidos para loja online de roupas e acessórios. Implementado em Java 21 com Javalin 6.7.0, usando PostgreSQL 18 como banco de dados.

## Funcionalidades

- **Cálculo de Resumo de Compra**: Calcula subtotal, descontos, frete, seguro, ajuste de pagamento e total final
- **Gerenciamento de Pedidos**: Registro, consulta, atualização de status de pedidos
- **Processamento de Pagamentos**: Suporta PIX, cartão de crédito com parcelamento e boleto
- **Cupons**: Suporta WELCOME10, MINUS50, FREESHIPPING e BUY3PAY2
- **Programa de Clube**: Níveis BRONZE, SILVER e GOLD com diferentes benefícios
- **Gerenciamento de Estoque**: Controle de estoque com atualização apenas quando pagamento é confirmado
- **Integração com Transportadora**: Consulta de frete com TransLog
- **Sistema de Notificações**: Registra eventos de pagamento, envio e cancelamento

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Compilação

```bash
mvn clean package
```

Isso gera um JAR executável em `/target/order-service-1.0.0.jar`.

## Execução

### Opção 1: Valores padrão (localhost)

```bash
java -jar target/order-service-1.0.0.jar
```

Conectará em:
- `jdbc:postgresql://localhost:5432/orderservice`
- Usuário: `postgres`
- Senha: `postgres`
- Porta do serviço: `8080`

### Opção 2: Variáveis de ambiente

```bash
DB_URL=jdbc:postgresql://db-host:5432/mydb \
DB_USER=myuser \
DB_PASSWORD=mypass \
PORT=3000 \
TRANSLOG_URL=https://translog.example.com \
java -jar target/order-service-1.0.0.jar
```

Variáveis disponíveis:
- `DB_URL`: URL de conexão do PostgreSQL
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta do serviço (padrão: 8080)
- `TRANSLOG_URL`: URL base da API da TransLog para consultas de frete

## Endpoints da API

### Checkout

- **POST /checkout/summary**: Calcula resumo da compra
- **POST /orders**: Registra novo pedido

### Consultas

- **GET /orders/{id}**: Obtém detalhes do pedido
- **GET /products/{sku}**: Obtém dados do produto
- **GET /customers/{cpf}**: Obtém saldo de crédito do cliente
- **GET /customers/{cpf}/notifications**: Obtém notificações do cliente

### Operações no Pedido

- **POST /orders/{id}/payment**: Registra pagamento aprovado
- **POST /orders/{id}/shipment**: Registra envio
- **POST /orders/{id}/delivery**: Registra entrega
- **POST /orders/{id}/cancellation**: Cancela pedido

### Notificações

- **POST /notifications/{id}/read**: Marca notificação como lida

## Exemplo de Requisição

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
    "shippingMethod": "EXPRESS",
    "paymentMethod": "PIX",
    "clubTier": "BRONZE"
  }'
```

## Testes

```bash
mvn verify
```

## Estrutura do Projeto

```
src/main/java/com/loja/
├── App.java                 # Aplicação principal
├── api/
│   ├── CheckoutRequest.java
│   ├── ErrorResponse.java
│   ├── PaymentRequest.java
│   ├── Routes.java
│   └── ShipmentRequest.java
├── database/
│   └── Database.java        # Gerenciamento de BD e migrações
├── models/
│   ├── Customer.java
│   ├── Enums.java
│   ├── Notification.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderSummary.java
│   └── Product.java
├── services/
│   ├── CheckoutService.java # Cálculos e validações
│   ├── CustomerService.java
│   ├── NotificationService.java
│   ├── OrderService.java
│   ├── PaymentService.java
│   └── ProductService.java
└── utils/
    └── MoneyUtils.java      # Arredondamento monetário
```

## Cálculo de Valores

### Arredondamento

Todos os valores em dinheiro são arredondados para centavos usando arredondamento "meio para o par" (HALF_EVEN):
- 2.995 → 3.00
- 2.985 → 2.98

### Cálculo do Total

1. Subtotal = Soma de (preço × quantidade) de todos os produtos
2. Desconto do cupom = Aplicado sobre o subtotal
3. Frete = De acordo com a opção de entrega
4. Seguro = Porcentagem sobre os produtos, com limite por região
5. Ajuste de pagamento = Desconto (PIX) ou tarifa (boleto) ou juros (cartão com parcelamento)
6. Total final = Subtotal - desconto + frete + seguro + ajuste

### Crédito do Clube

- **BRONZE**: Sem crédito
- **SILVER**: 2% do subtotal
- **GOLD**: 5% do subtotal

O crédito é aplicado na próxima compra quando o pagamento anterior é confirmado.

### Desconto Máximo

Nenhum cupom pode oferecer desconto maior que 30% do valor dos produtos.

## Observações Importantes

- O estoque só é decrementado quando o pagamento é confirmado
- Um mesmo pagamento (mesma transação) não pode ser processado duas vezes
- Pagamentos que chegam mais de 48h após o registro do pedido são recusados
- Clientes que ainda não realizaram uma compra com sucesso podem usar WELCOME10
- O motoboy (COURIER) está limitado a pedidos de até 5kg
- Boleto não é aceito para pedidos acima de R$ 1.000,00

## Banco de Dados

O banco de dados é inicializado automaticamente na primeira execução com:
- Tabela de produtos (catálogo inicial incluído)
- Tabela de pedidos
- Tabela de itens de pedido
- Tabela de clientes
- Tabela de notificações

## Performance e Escalabilidade

O serviço foi projetado para rodar em múltiplas instâncias atrás de um load balancer, compartilhando um mesmo banco de dados PostgreSQL.

- Conexões ao banco gerenciadas com HikariCP
- Pool de até 10 conexões
- Suporte a múltiplas requisições simultâneas
