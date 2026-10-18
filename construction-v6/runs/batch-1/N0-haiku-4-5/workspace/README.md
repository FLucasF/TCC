# Order Service

Serviço de gerenciamento de pedidos para loja online de roupas e acessórios.

## Compilação

O projeto utiliza Maven 3.8+ e Java 21. Para compilar e rodar os testes:

```bash
mvn clean verify
```

## Estrutura do Projeto

- **src/main/java**: Código fonte da aplicação
  - **domain**: Entidades JPA e enums
  - **dto**: Data Transfer Objects para API
  - **repository**: Interfaces JPA Repository
  - **service**: Lógica de negócio (CheckoutService, OrderService, CatalogService, CustomerService)
  - **controller**: Controladores REST
  - **exception**: Exceções customizadas
  - **integration**: Cliente HTTP para TransLog
  - **util**: Utilitários (CurrencyUtils)
  - **config**: Configurações (DataInitializer)

- **src/test/java**: Testes unitários

## Funcionalidades Implementadas

### Checkout
- **POST /checkout/summary**: Calcula resumo do pedido sem registrá-lo
  - Validação completa de entrada
  - Cálculo de preços com arredondamento "meio para par"
  - Cálculo de cupons (WELCOME10, MINUS50, FREESHIPPING, BUY3PAY2)
  - Cálculo de frete via TransLog
  - Cálculo de seguro por região
  - Ajustes de pagamento (Pix, Cartão de Crédito, Boleto)

### Pedidos
- **POST /orders**: Registra novo pedido
- **GET /orders/{id}**: Consulta pedido
- **POST /orders/{id}/payment**: Processa notificação de pagamento
  - Valida timeout (48 horas)
  - Atualiza estoque
  - Adiciona crédito do clube
  - Cria notificação de pagamento
- **POST /orders/{id}/shipment**: Marca como enviado
- **POST /orders/{id}/delivery**: Marca como entregue
- **POST /orders/{id}/cancellation**: Cancela pedido
  - Devolve estoque se pago
  - Remove crédito do clube
  - Cria notificação de cancelamento

### Produtos
- **GET /products/{sku}**: Consulta produto

### Clientes
- **GET /customers/{cpf}**: Consulta saldo de crédito
- **GET /customers/{cpf}/notifications**: Lista notificações do cliente
- **POST /notifications/{id}/read**: Marca notificação como lida

## Configuração

### Variáveis de Ambiente

- `DB_URL`: URL do PostgreSQL (padrão: jdbc:postgresql://localhost:5432/order_service)
- `DB_USER`: Usuário do banco (padrão: postgres)
- `DB_PASSWORD`: Senha do banco (padrão: postgres)
- `TRANSLOG_URL`: URL da API da TransLog (padrão: http://localhost:8081)

### Banco de Dados

A aplicação cria as tabelas automaticamente no startup via Hibernate. Nenhuma configuração manual é necessária.

## Catálogo Inicial

Os 8 produtos iniciais são carregados automaticamente ao iniciar:
- CAM-001: Camiseta básica
- TEN-001: Tênis corrida
- FON-001: Fone sem fio
- MEI-001: Meia esportiva
- JAQ-001: Jaqueta corta-vento
- MOC-001: Mochila urbana
- BON-001: Boné aba reta
- REL-001: Relógio digital (descontinuado)

## Lógica de Negócio

### Cálculo de Preços
1. Subtotal dos produtos (preço × quantidade)
2. Desconto do cupom (se aplicável)
3. Frete (baseado na forma de entrega e região)
4. Seguro (percentual por região com limite)
5. Ajuste de pagamento (Pix -5%, Boleto +R$ 3,49, Cartão com juros de 1,99% a.m.)
6. Crédito para próxima compra (2% Silver, 5% Gold)

### Cupons
- **WELCOME10**: 10% desconto (apenas primeira compra)
- **MINUS50**: R$ 50,00 desconto (acima de R$ 300,00)
- **FREESHIPPING**: Frete grátis
- **BUY3PAY2**: A cada 3 unidades, 1 sai grátis

### Formas de Entrega
- **ECONOMY**: Via TransLog (padrão)
- **EXPRESS**: Via TransLog (mais rápido)
- **STORE_PICKUP**: Retirada na loja (R$ 0, 1 dia)
- **COURIER**: Motoboy (R$ 18, mesmo dia, máx 5 kg)

### Clube
- **BRONZE**: Sem benefícios
- **SILVER**: 2% de crédito, sem restrições
- **GOLD**: 5% de crédito, frete grátis, brinde acima de R$ 500

## Tratamento de Erros

Todos os erros retornam JSON com código do erro:
```json
{ "error": "CÓDIGO_DO_ERRO" }
```

Com HTTP status apropriado (422, 404, 409, 503).

## Testes

Os testes cobrem:
- Cálculo de resumos
- Validação de entrada
- Arredondamento de valores
- Tratamento de exceções

Execute com:
```bash
mvn test
```

## Notas Técnicas

- Spring Boot 3.3.0 com Java 21
- PostgreSQL 18 com Hibernate JPA
- Transações com @Transactional
- JSON via Jackson
- Logging via SLF4J
- Testes com JUnit 5 e Mockito
