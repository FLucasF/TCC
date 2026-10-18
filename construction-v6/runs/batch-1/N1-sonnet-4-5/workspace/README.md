# Serviço de Pedidos - Loja Online

Sistema de gerenciamento de pedidos para loja online de roupas e acessórios.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18 (produção)

## Configuração

O serviço requer as seguintes variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/loja
DB_USER=postgres
DB_PASSWORD=senha
TRANSLOG_URL=http://api.translog.com
```

## Execução

### Compilar e testar

```bash
mvn verify
```

### Executar o serviço

```bash
mvn spring-boot:run
```

O serviço estará disponível em `http://localhost:8080`.

## Endpoints

### Checkout
- `POST /checkout/summary` - Calcular resumo da compra

### Pedidos
- `POST /orders` - Registrar pedido
- `GET /orders/{id}` - Consultar pedido
- `POST /orders/{id}/payment` - Processar pagamento
- `POST /orders/{id}/shipment` - Enviar pedido
- `POST /orders/{id}/delivery` - Entregar pedido
- `POST /orders/{id}/cancellation` - Cancelar pedido

### Produtos
- `GET /products/{sku}` - Consultar produto

### Clientes
- `GET /customers/{cpf}` - Consultar saldo do cliente
- `GET /customers/{cpf}/notifications` - Listar notificações

### Notificações
- `POST /notifications/{id}/read` - Marcar notificação como lida

## Catálogo Inicial

O catálogo é inicializado automaticamente na primeira execução:

| SKU | Produto | Preço | Peso (kg) | Estoque |
|-----|---------|-------|-----------|---------|
| CAM-001 | Camiseta básica | R$ 79,90 | 0,30 | 50 |
| TEN-001 | Tênis corrida | R$ 249,90 | 1,20 | 10 |
| FON-001 | Fone sem fio | R$ 199,90 | 0,25 | 5 |
| MEI-001 | Meia esportiva | R$ 19,90 | 0,10 | 100 |
| JAQ-001 | Jaqueta corta-vento | R$ 389,90 | 0,80 | 3 |
| MOC-001 | Mochila urbana | R$ 159,90 | 0,90 | 1 |
| BON-001 | Boné aba reta | R$ 59,90 | 0,15 | 0 |
| REL-001 | Relógio digital (descontinuado) | R$ 129,90 | 0,20 | 8 |

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/loja/
│   │   ├── controller/     # Endpoints REST
│   │   ├── domain/         # Entidades e Value Objects
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Tratamento de erros
│   │   ├── integration/    # Integrações externas
│   │   ├── repository/     # Acesso a dados
│   │   └── service/        # Lógica de negócio
│   │       ├── club/       # Estratégias de clube
│   │       ├── coupon/     # Estratégias de cupons
│   │       ├── payment/    # Estratégias de pagamento
│   │       └── shipping/   # Estratégias de entrega
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/loja/
        ├── domain/         # Testes de domínio
        └── service/        # Testes de serviço
```

## Arquitetura

O projeto segue os princípios do CLAUDE.md:

- **Estratégias**: Cupons, formas de entrega, formas de pagamento e níveis do clube são implementados como estratégias, permitindo fácil adição de novos casos similares.
- **Value Objects**: `Money` e `Weight` encapsulam lógica de arredondamento e conversão.
- **Proteção de Estoque**: Locks pessimistas garantem que o estoque nunca fique negativo.
- **Idempotência**: Pagamentos duplicados (mesmo `transactionId`) são tratados de forma idempotente.
- **Arredondamento**: Todos os valores monetários usam arredondamento "meio para o par" (banker's rounding).
