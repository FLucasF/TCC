# Order Service

Serviço de e-commerce para gerenciamento de pedidos, pagamentos e estoque.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.8+

## Como executar localmente

### Com Docker Compose

```bash
docker-compose up -d postgres
export DB_URL=jdbc:postgresql://localhost:5432/order_db
export DB_USER=order_user
export DB_PASSWORD=order_pass
export PORT=8080
export TRANSLOG_URL=http://localhost:8081/translog

mvn clean package
java -jar target/order-service.jar
```

### Sem Docker

Configure as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://seu-host:5432/seu-banco
export DB_USER=seu-usuario
export DB_PASSWORD=sua-senha
export PORT=8080
export TRANSLOG_URL=http://seu-translog-url
```

Depois:

```bash
mvn clean package
java -jar target/order-service.jar
```

## Estrutura do Projeto

- `src/main/java/com/clothing/` - Código principal
  - `Application.java` - Ponto de entrada
  - `Database.java` - Configuração do banco de dados
  - `OrderRoutes.java` - Rotas de pedidos
  - `ProductRoutes.java` - Rotas de produtos
  - `CustomerRoutes.java` - Rotas de clientes
  - `model/` - Classes de modelo (DTO, entidades)
  - `service/` - Lógica de negócio (repositórios, serviços)

## API Endpoints

### Pedidos

- `POST /checkout/summary` - Calcular resumo da compra
- `POST /orders` - Registrar um novo pedido
- `GET /orders/{id}` - Consultar um pedido
- `POST /orders/{id}/payment` - Registrar pagamento
- `POST /orders/{id}/shipment` - Marcar como enviado
- `POST /orders/{id}/delivery` - Marcar como entregue
- `POST /orders/{id}/cancellation` - Cancelar pedido

### Produtos

- `GET /products/{sku}` - Consultar um produto

### Clientes

- `GET /customers/{cpf}` - Consultar saldo de crédito de um cliente
- `GET /customers/{cpf}/notifications` - Listar notificações de um cliente
- `POST /notifications/{id}/read` - Marcar uma notificação como lida

## Testes

```bash
mvn verify
```
