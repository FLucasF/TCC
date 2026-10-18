# Sistema de Gerenciamento de Pedidos - Loja Online

Sistema completo de gerenciamento de pedidos para loja online de roupas e acessórios.

## Requisitos

- Java 21
- PostgreSQL 18 (para produção)
- Maven 3.x

## Compilação

```bash
mvn package
```

Isso irá gerar o JAR standalone em `target/order-service-1.0.0.jar`.

## Execução

### Variáveis de Ambiente

- `PORT`: Porta do servidor (padrão: 8080)
- `DB_URL`: URL do banco PostgreSQL (padrão: jdbc:postgresql://localhost:5432/shop)
- `DB_USER`: Usuário do banco (padrão: postgres)
- `DB_PASSWORD`: Senha do banco (padrão: postgres)
- `TRANSLOG_URL`: URL da API da TransLog (padrão: http://localhost:8081)

### Iniciar o servidor

```bash
java -jar target/order-service-1.0.0.jar
```

Ou com variáveis customizadas:

```bash
PORT=9000 \
DB_URL=jdbc:postgresql://db.example.com:5432/shop \
DB_USER=myuser \
DB_PASSWORD=mypass \
TRANSLOG_URL=https://api.translog.com \
java -jar target/order-service-1.0.0.jar
```

## API Endpoints

### Checkout

- `POST /checkout/summary` - Calcula o resumo da compra sem criar pedido

### Pedidos

- `POST /orders` - Cria um novo pedido
- `GET /orders/{id}` - Consulta um pedido
- `POST /orders/{id}/payment` - Registra pagamento
- `POST /orders/{id}/shipment` - Registra envio
- `POST /orders/{id}/delivery` - Registra entrega
- `POST /orders/{id}/cancellation` - Cancela pedido

### Produtos

- `GET /products/{sku}` - Consulta produto

### Clientes

- `GET /customers/{cpf}` - Consulta saldo de crédito
- `GET /customers/{cpf}/notifications` - Lista notificações
- `POST /notifications/{id}/read` - Marca notificação como lida

## Funcionalidades Implementadas

### Cálculo de Preços

1. Subtotal dos produtos
2. Desconto do cupom (respeitando limite de 30%)
3. Frete (com integração TransLog)
4. Seguro por região
5. Ajuste de pagamento (desconto PIX, juros parcelamento, tarifa boleto)

### Cupons

- `WELCOME10`: 10% de desconto (primeira compra)
- `MINUS50`: R$ 50 de desconto (compras >= R$ 300)
- `FREESHIPPING`: Frete grátis
- `BUY3PAY2`: Compre 3, pague 2 (mesmo produto)

### Clube de Benefícios

- **BRONZE**: Sem benefícios
- **SILVER**: 2% de crédito para próxima compra
- **GOLD**: 5% de crédito, frete grátis (exceto motoboy), brinde em compras > R$ 500

### Formas de Entrega

- `ECONOMY`: Econômica (TransLog)
- `EXPRESS`: Expressa (TransLog)
- `STORE_PICKUP`: Retirada na loja (grátis, 1 dia)
- `COURIER`: Motoboy (R$ 18, mesmo dia, máx 5kg)

### Formas de Pagamento

- `PIX`: 5% desconto (se não usar cupom)
- `CREDIT_CARD`: 1-3x sem juros, 4-12x com 1,99% a.m.
- `BANK_SLIP`: Tarifa R$ 3,49, máximo R$ 1.000

### Controle de Estoque

- Estoque só baixa quando pagamento confirmado
- Controle de concorrência (locks pessimistas)
- Estoque nunca fica negativo
- Estoque volta ao cancelar pedido pago

### Ciclo de Vida do Pedido

- `AWAITING_PAYMENT` → `PAID` → `SHIPPED` → `DELIVERED`
- Cancelamento permitido antes do envio
- Pagamento expira em 48 horas
- Idempotência de avisos de pagamento (mesmo transactionId)

### Notificações

- Pagamento aprovado
- Pedido enviado
- Pedido cancelado

## Catálogo Inicial

O sistema já vem com produtos pré-cadastrados:

| SKU | Produto | Preço | Estoque |
|-----|---------|-------|---------|
| CAM-001 | Camiseta básica | R$ 79,90 | 50 |
| TEN-001 | Tênis corrida | R$ 249,90 | 10 |
| FON-001 | Fone sem fio | R$ 199,90 | 5 |
| MEI-001 | Meia esportiva | R$ 19,90 | 100 |
| JAQ-001 | Jaqueta corta-vento | R$ 389,90 | 3 |
| MOC-001 | Mochila urbana | R$ 159,90 | 1 |
| BON-001 | Boné aba reta | R$ 59,90 | 0 |
| REL-001 | Relógio digital | R$ 129,90 | 8 (descontinuado) |

## Testes

```bash
mvn verify
```

Os testes rodam sem necessidade de banco de dados ou Docker.

## Observações Técnicas

- Arredondamento HALF_EVEN (banker's rounding) em todas as etapas
- Valores monetários com 2 casas decimais
- Datas em formato ISO-8601 (UTC)
- Sistema preparado para deployment distribuído (2+ instâncias)
- Transações ACID no PostgreSQL
