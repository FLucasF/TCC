# Serviço de Pedidos - Loja Online

Sistema de gerenciamento de pedidos para loja de roupas e acessórios, implementado em Java 21 com Javalin e PostgreSQL.

## Padrões de Design Utilizados

Este projeto implementa os seguintes padrões GoF conforme as orientações do CLAUDE.md:

- **Strategy**: Implementado para:
  - Cupons de desconto (WELCOME10, MINUS50, FREESHIPPING, BUY3PAY2)
  - Formas de entrega (ECONOMY, EXPRESS, STORE_PICKUP, COURIER)
  - Níveis do clube (BRONZE, SILVER, GOLD)
  - Formas de pagamento (PIX, CREDIT_CARD, BANK_SLIP)

- **Observer**: Implementado para eventos de pagamento confirmado:
  - `StockObserver`: Baixa o estoque dos produtos
  - `CreditObserver`: Adiciona crédito no saldo do cliente
  - `NotificationObserver`: Cria notificações para o cliente
  - Facilita adicionar novos observadores (SMS, cashback, etc.) sem modificar código existente

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Variáveis de Ambiente

- `DB_URL`: URL de conexão do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/loja`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `TRANSLOG_URL`: URL da API da TransLog
- `PORT`: Porta do servidor (padrão: `8080`)

## Como Compilar e Executar

### Compilar o projeto

```bash
mvn verify
```

### Gerar o JAR executável

```bash
mvn package
```

### Executar o serviço

```bash
java -jar target/order-service-1.0.0.jar
```

## Estrutura do Projeto

```
src/main/java/com/loja/
├── Main.java                           # Ponto de entrada da aplicação
├── api/                                # Camada de API REST
│   ├── controllers/                    # Controllers Javalin
│   └── dto/                            # Data Transfer Objects
├── domain/                             # Lógica de domínio
│   ├── entities/                       # Entidades de domínio
│   ├── events/                         # Eventos de domínio
│   ├── observers/                      # Observadores (padrão Observer)
│   └── strategies/                     # Estratégias (padrão Strategy)
│       ├── club/                       # Estratégias de níveis de clube
│       ├── coupon/                     # Estratégias de cupons
│       ├── payment/                    # Estratégias de pagamento
│       └── shipping/                   # Estratégias de frete
├── infrastructure/                      # Infraestrutura
│   ├── Database.java                   # Gerenciador de banco de dados
│   └── TransLogClient.java             # Cliente da API TransLog
├── repositories/                        # Repositórios
│   └── impl/                           # Implementações PostgreSQL
└── services/                           # Serviços de aplicação
    ├── CheckoutService.java            # Cálculo de resumo
    ├── OrderService.java               # Gerenciamento de pedidos
    └── ValidationService.java          # Validações
```

## Endpoints da API

### Checkout
- `POST /checkout/summary` - Calcula resumo da compra sem registrar
- `POST /orders` - Registra novo pedido

### Pedidos
- `GET /orders/{id}` - Consulta um pedido
- `POST /orders/{id}/payment` - Confirma pagamento
- `POST /orders/{id}/shipment` - Registra envio
- `POST /orders/{id}/delivery` - Confirma entrega
- `POST /orders/{id}/cancellation` - Cancela pedido

### Produtos e Clientes
- `GET /products/{sku}` - Consulta produto
- `GET /customers/{cpf}` - Consulta cliente
- `GET /customers/{cpf}/notifications` - Lista notificações
- `POST /notifications/{id}/read` - Marca notificação como lida

## Inicialização do Banco

O serviço cria automaticamente as tabelas necessárias e inicializa o catálogo de produtos ao iniciar.

## Catálogo Inicial

| SKU      | Produto             | Preço   | Estoque |
|----------|---------------------|---------|---------|
| CAM-001  | Camiseta básica     | R$ 79,90 | 50      |
| TEN-001  | Tênis corrida       | R$ 249,90 | 10     |
| FON-001  | Fone sem fio        | R$ 199,90 | 5      |
| MEI-001  | Meia esportiva      | R$ 19,90 | 100     |
| JAQ-001  | Jaqueta corta-vento | R$ 389,90 | 3      |
| MOC-001  | Mochila urbana      | R$ 159,90 | 1      |
| BON-001  | Boné aba reta       | R$ 59,90 | 0       |
| REL-001  | Relógio digital     | R$ 129,90 | 8      |
