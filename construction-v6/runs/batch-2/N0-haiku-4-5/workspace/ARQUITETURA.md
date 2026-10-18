# Arquitetura do Serviço de Pedidos

## Visão Geral

O serviço de pedidos é uma aplicação Java 21 construída com Javalin 6.7.0 (web framework leve) e PostgreSQL 18 para persistência. A arquitetura segue princípios de separação de responsabilidades com camadas bem definidas.

## Camadas da Aplicação

### 1. Apresentação (API REST)

**Arquivo:** `api/Routes.java`

Define todos os endpoints HTTP da aplicação. Cada endpoint mapeia para um handler que:
1. Valida a entrada (JSON parsing automático via Gson)
2. Chama os serviços apropriados
3. Retorna resposta com código HTTP correto

**Características:**
- Usa Javalin para roteamento
- Gson para serialização/desserialização JSON
- Tratamento centralizado de erros com `ErrorResponse`

### 2. Lógica de Negócio (Services)

**Arquivos:**
- `services/CheckoutService.java` - Cálculos e validações
- `services/OrderService.java` - CRUD de pedidos
- `services/PaymentService.java` - Processamento de pagamentos
- `services/CustomerService.java` - Gerenciamento de clientes
- `services/NotificationService.java` - Sistema de notificações
- `services/ProductService.java` - Catálogo de produtos

**Características:**
- Lógica pura sem dependências de web framework
- Responsabilidade única por serviço
- Métodos estáticos para simplicidade (sem necessidade de IoC)

### 3. Acesso a Dados (Database)

**Arquivo:** `database/Database.java`

Gerencia:
- Pool de conexões com HikariCP
- Criação de schema (DDL)
- Inicialização do catálogo de produtos

**Design:**
- Singleton thread-safe
- Migrations automáticas na inicialização
- Prepared statements para segurança contra SQL injection

### 4. Modelos de Dados

**Arquivos:**
- `models/Order.java`, `models/Product.java`, etc.
- `models/Enums.java` - Enumerações de tipos
- `api/*Request.java` - DTOs para requisições

**Características:**
- POJOs simples (sem anotações JPA)
- Sem ORM - SQL manual com PreparedStatement
- Manutenção explícita de estado

## Fluxos Principais

### Fluxo de Checkout

1. **POST /checkout/summary**
   - Valida request (CheckoutService.validateCheckoutRequest)
   - Calcula resumo (CheckoutService.calculateSummary)
   - Retorna resumo sem salvar

2. **POST /orders**
   - Valida request
   - Calcula resumo
   - Cria pedido (OrderService.createOrder)
   - Insere itens do pedido
   - Retorna pedido com status AWAITING_PAYMENT

### Fluxo de Pagamento

1. **POST /orders/{id}/payment**
   - Valida existência do pedido
   - Verifica status (deve ser AWAITING_PAYMENT)
   - Valida timeout (48h)
   - Valida estoque suficiente
   - Atualiza estoque (decrementa)
   - Adiciona crédito ao cliente
   - Cria notificação PAYMENT_APPROVED
   - Retorna pedido com status PAID

### Fluxo de Envio

1. **POST /orders/{id}/shipment**
   - Valida existência do pedido
   - Verifica status (deve ser PAID)
   - Atualiza tracking code
   - Cria notificação ORDER_SHIPPED
   - Retorna pedido com status SHIPPED

### Fluxo de Cancelamento

1. **POST /orders/{id}/cancellation**
   - Valida existência do pedido
   - Verifica status (não pode ser SHIPPED/DELIVERED)
   - Se PAID: reverte estoque e crédito
   - Cria notificação ORDER_CANCELLED
   - Retorna pedido com status CANCELLED

## Decisões de Design

### 1. Sem Framework IoC (Spring)

**Rationale:**
- Javalin é mais leve que Spring
- Maior controle explícito
- Menos "magia" de dependência injeção
- Mais fácil de compreender e manter

**Trade-off:**
- Métodos estáticos em Services (menos testável em isolamento)
- Sem anotações declarativas

### 2. SQL Manual vs ORM

**Rationale:**
- Sem dependência de Hibernate/JPA
- Controle explícito das queries
- Melhor performance para casos simples
- Evita problemas de lazy loading

**Trade-off:**
- Mais código boilerplate de SQL
- Responsabilidade manual de mapeamento

### 3. Pool de Conexões com HikariCP

**Rationale:**
- Pool leve e eficiente
- Bem testado na indústria
- Simples de configurar
- Suporta múltiplas instâncias do serviço

### 4. Arredondamento HALF_EVEN

**Rationale:**
- Especificado no escopo
- Evita viés (diferente de ROUND_UP/DOWN)
- Implementado com `BigDecimal.setScale(2, RoundingMode.HALF_EVEN)`

### 5. Validação em Dois Níveis

**Nível 1: Estrutural**
- Campos obrigatórios
- Tipos válidos
- Formatos válidos (CPF, CEP, etc)

**Nível 2: Negocial**
- Produtos existem e não estão descontinuados
- Estoque suficiente
- Cupom é aplicável
- Transportadora atende
- Forma de pagamento é permitida

## Segurança

### SQL Injection Prevention
- Prepared statements em todas as queries
- Parametrização de inputs

### Validação de Entrada
- Desserialização JSON segura
- Validação de CPF (11 dígitos)
- Validação de CEP (8 dígitos)
- Validação de enumerações

### Integridade de Dados
- Transações por operação crítica (quando aplicável)
- Constraint único em CPF
- Foreign keys entre tabelas

## Performance

### Otimizações

1. **Pool de Conexões**
   - HikariCP com 10 conexões
   - Suporta múltiplas requisições simultâneas

2. **Preparação de Queries**
   - PreparedStatement reutilizável
   - Execução eficiente

3. **Índices no Banco**
   - Primary keys em todos os agregados
   - Foreign keys para relacionamentos

4. **Sem N+1 Queries**
   - Queries compiladas para cada acesso
   - Sem lazy loading problems

### Escalabilidade

O serviço foi projetado para:
- Rodar em múltiplas instâncias
- Compartilhar um único banco de dados
- Ser stateless (sem sessão em memória)
- Suportar load balancing transparente

## Monitoramento e Observabilidade

### Logging
- SLF4J com Simple backend
- Logs automáticos de operações de BD

### Rastreamento de Pedidos
- Tracking code armazenado
- Notificações registradas
- Histórico de pagamento

## Futuro: Melhorias Possíveis

1. **Cache**
   - Cache de produtos em memória
   - Cache de translog queries

2. **Async Processing**
   - Notificações assíncronas
   - Processamento de relatórios

3. **Observabilidade**
   - Métricas Prometheus
   - Distributed tracing
   - Health checks

4. **Testes**
   - Testes integrados com testcontainers
   - Testes de carga/performance
   - Testes de contrato com TransLog

5. **Validação Avançada**
   - CPF válido (algoritmo checksum)
   - CEP válido (consulta BD de CEP)
   - Email válido (validação regex/lib)

## Dependências Principais

```
javalin 6.7.0          - Web framework
postgresql 42.7.1      - JDBC driver
hikaricp 5.1.0         - Connection pooling
gson 2.10.1            - JSON serialization
slf4j 2.0.9            - Logging
```

Todas são bibliotecas maduras, bem mantidas e confiáveis.

## Estrutura do Banco de Dados

```sql
products (sku PRIMARY KEY)
├── name, price, weight_kg, stock, discontinued

customers (cpf PRIMARY KEY)
├── email, credit_balance

orders (id PRIMARY KEY)
├── cpf (FK customers)
├── email, status, created_at, paid_at, payment_transaction_id
├── summary fields (subtotal, discounts, totals)
├── shipping_method, region, payment_method, club_tier

order_items
├── order_id (FK orders)
├── sku (FK products)
├── quantity

notifications (id PRIMARY KEY)
├── cpf (FK customers)
├── type, order_id, status, created_at
```

## Tratamento de Concorrência

### Cenário: Dois Clientes Comprando Último Item

1. Cliente A: checkout - vê 1 unidade em estoque ✓
2. Cliente B: checkout - vê 1 unidade em estoque ✓
3. Cliente A: pagamento confirmado - estoque diminui para 0 ✓
4. Cliente B: pagamento confirmado - tenta diminuir estoque, quantidade < 0 ❌
   - Situação detectada, pedido cancelado automaticamente

**Proteção:** SELECT FOR UPDATE ou validação atomática de estoque antes de debitar.

Nota: A implementação atual não usa locks explícitos, confiando no processamento sequencial. Para produção com alta concorrência, considere:
- Locks pessimistas
- Versioning otimista
- Event sourcing

## Resilência

### Falhas de Transporte
- TransLog timeout: 2 segundos
- Retorna erro `CARRIER_UNAVAILABLE` se não responder
- Cliente precisa retentar checkout

### Falhas de Banco
- Pool reconecta automaticamente
- Exceções propagadas para cliente

### Duplicação de Transação
- Validação por `(orderId, transactionId)` único
- Mesma transação retorna resultado anterior sem side-effects duplos
