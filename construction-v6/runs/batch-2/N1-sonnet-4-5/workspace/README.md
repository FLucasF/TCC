# Serviço de Pedidos - Loja Online

Sistema de gestão de pedidos para loja online de roupas e acessórios.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Configuração

O serviço é configurado via variáveis de ambiente:

- `DB_URL`: URL de conexão do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/loja`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `TRANSLOG_URL`: URL da API da TransLog (padrão: `http://localhost:8081`)
- `PORT`: Porta do serviço (padrão: `8080`)

## Compilação e Testes

```bash
mvn verify
```

## Execução

```bash
java -jar target/order-service-1.0.0.jar
```

## Endpoints

### Checkout
- `POST /checkout/summary` - Calcula o resumo da compra

### Pedidos
- `POST /orders` - Registra um novo pedido
- `GET /orders/{id}` - Consulta um pedido
- `POST /orders/{id}/payment` - Confirma o pagamento
- `POST /orders/{id}/shipment` - Registra o envio
- `POST /orders/{id}/delivery` - Registra a entrega
- `POST /orders/{id}/cancellation` - Cancela o pedido

### Produtos
- `GET /products/{sku}` - Consulta um produto

### Clientes
- `GET /customers/{cpf}` - Consulta o saldo de um cliente
- `GET /customers/{cpf}/notifications` - Lista as notificações de um cliente

### Notificações
- `POST /notifications/{id}/read` - Marca uma notificação como lida

## Estrutura do Projeto

- `domain/` - Modelos de domínio
- `repository/` - Acesso a dados
- `service/` - Lógica de negócio
  - `coupon/` - Estratégias de cupons
  - `shipping/` - Cálculo de frete
  - `insurance/` - Cálculo de seguro
  - `payment/` - Ajustes de pagamento
  - `club/` - Benefícios do clube
- `controller/` - Controladores REST
- `util/` - Utilitários

## Banco de Dados

O serviço cria automaticamente as tabelas necessárias ao iniciar e carrega o catálogo inicial de produtos.
