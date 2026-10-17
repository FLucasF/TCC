# Order Service

Sistema completo de gestão de pedidos para loja online de roupas e acessórios.

## Funcionalidades

- **Catálogo de Produtos**: Gerenciamento de produtos com preço, peso e estoque
- **Cálculo de Resumo**: Cálculo complexo de preços com:
  - Subtotal de produtos
  - Descontos de cupom (WELCOME10, MINUS50, FREESHIPPING, BUY3PAY2)
  - Frete via TransLog
  - Seguro por região
  - Ajustes de forma de pagamento (desconto PIX, tarifa boleto, juros cartão)
  - Crédito do clube para próxima compra
  - Validação de brinde (GOLD acima de R$ 500)

- **Gestão de Pedidos**: Criação, consulta e atualização de estado
  - Estados: AWAITING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED

- **Controle de Estoque**: Baixa de estoque apenas após confirmação de pagamento

- **Processamento de Pagamento**: Integração com notificações de pagamento
  - Idempotência garantida
  - Expiração de 48 horas
  - Verificação de estoque

- **Notificações**: Avisos ao cliente (pagamento aprovado, pedido enviado, cancelamento)

- **REST API**: Endpoints para todas as operações

## Requisitos

- Java 21
- Maven
- PostgreSQL 18 (para produção)
- H2 (para testes)

## Build e Testes

```bash
# Compilar e rodar testes
mvn clean verify

# Apenas compilar
mvn clean compile

# Apenas testes
mvn clean test

# Build da aplicação
mvn clean package
```

## Configuração

Variáveis de ambiente necessárias:

```bash
DB_URL=jdbc:postgresql://localhost:5432/orderdb
DB_USER=postgres
DB_PASSWORD=password
TRANSLOG_URL=http://translog.example.com
```

Se não configuradas, usam valores padrão (localhost).

## Executar a Aplicação

```bash
java -jar target/order-service-1.0.0.jar
```

Ou com variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://host:5432/db DB_USER=user DB_PASSWORD=pass TRANSLOG_URL=http://translog java -jar target/order-service-1.0.0.jar
```

## API Endpoints

### Resumo de Compra
```
POST /checkout/summary
Body: OrderRequestDTO
Returns: SummaryResponseDTO
```

### Criar Pedido
```
POST /orders
Body: OrderRequestDTO
Returns: OrderResponseDTO (201)
```

### Consultar Pedido
```
GET /orders/{id}
Returns: OrderResponseDTO
```

### Processar Pagamento
```
POST /orders/{id}/payment
Body: { "transactionId": "TX-123", "paidAt": "2026-10-14T16:00:00Z" }
Returns: OrderResponseDTO
```

### Enviar Pedido
```
POST /orders/{id}/shipment
Body: { "trackingCode": "BR123456789" }
Returns: OrderResponseDTO
```

### Entregar Pedido
```
POST /orders/{id}/delivery
Returns: OrderResponseDTO
```

### Cancelar Pedido
```
POST /orders/{id}/cancellation
Returns: OrderResponseDTO
```

### Consultar Produto
```
GET /products/{sku}
Returns: ProductResponseDTO
```

### Consultar Saldo do Cliente
```
GET /customers/{cpf}
Returns: { "cpf": "...", "creditBalance": 20.48 }
```

### Consultar Notificações
```
GET /customers/{cpf}/notifications
Returns: [ NotificationResponseDTO, ... ]
```

### Marcar Notificação como Lida
```
POST /notifications/{id}/read
Returns: NotificationResponseDTO
```

## Estrutura do Projeto

```
src/
├── main/java/com/loja/orderservice/
│   ├── Application.java
│   ├── client/                 # Cliente HTTP
│   ├── config/                 # Configurações
│   ├── controller/             # REST Controllers
│   ├── dto/                    # Data Transfer Objects
│   ├── entity/                 # Entidades JPA
│   ├── exception/              # Exceções customizadas
│   ├── listener/               # Event Listeners
│   ├── repository/             # JPA Repositories
│   ├── service/                # Lógica de negócio
│   └── util/                   # Utilitários
├── resources/
│   └── application.properties  # Configuração
└── test/                       # Testes
```

## Dados Iniciais

O catálogo é carregado automaticamente ao iniciar a aplicação:

- CAM-001: Camiseta básica (R$ 79,90)
- TEN-001: Tênis corrida (R$ 249,90)
- FON-001: Fone sem fio (R$ 199,90)
- MEI-001: Meia esportiva (R$ 19,90)
- JAQ-001: Jaqueta corta-vento (R$ 389,90)
- MOC-001: Mochila urbana (R$ 159,90)
- BON-001: Boné aba reta (R$ 59,90)
- REL-001: Relógio digital (R$ 129,90) [Descontinuado]

## Cupons Disponíveis

- **WELCOME10**: 10% desconto para primeira compra
- **MINUS50**: R$ 50 desconto para compras acima de R$ 300
- **FREESHIPPING**: Frete grátis
- **BUY3PAY2**: A cada 3 unidades do mesmo produto, 1 sai grátis

## Níveis do Clube

- **BRONZE**: Sem benefícios
- **SILVER**: 2% de crédito para próxima compra
- **GOLD**: 5% de crédito, frete grátis, brinde acima de R$ 500

## Regiões

- SOUTHEAST (Sudeste)
- SOUTH (Sul)
- MIDWEST (Centro-Oeste)
- NORTH (Norte)
- NORTHEAST (Nordeste)

## Formas de Entrega

- **ECONOMY**: Via TransLog (frete cobrado)
- **EXPRESS**: Via TransLog (frete cobrado)
- **STORE_PICKUP**: Retirada na loja (grátis, 1 dia)
- **COURIER**: Motoboy (R$ 18, máximo 5kg, mesmo dia)

## Formas de Pagamento

- **PIX**: 5% desconto, sem parcelamento
- **CREDIT_CARD**: Sem juros até 3x, juros a partir de 4x (1.99% a.m.)
- **BANK_SLIP**: R$ 3,49 de taxa, sem parcelamento, máximo R$ 1.000

## Tecnologias

- Spring Boot 4.1.1
- Spring Data JPA
- PostgreSQL 18
- H2 (testes)
- JUnit 5
- Mockito
- Jackson
