# Serviço de Pedidos - Loja Online

Serviço REST para gerenciar pedidos de uma loja online de roupas e acessórios.

## Funcionalidades

- Cálculo de resumo de compra
- Registro de pedidos
- Gerenciamento de status de pedidos
- Controle de estoque
- Suporte a cupons de desconto
- Programa de clube de cliente
- Integração com transportadora
- Cálculo de frete e seguro
- Suporte a múltiplas formas de pagamento

## Dependências

- Java 21
- PostgreSQL 18
- Spring Boot 3.3.0

## Variáveis de Ambiente

- `DB_URL`: URL do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/loja_pedidos`)
- `DB_USER`: Usuário do PostgreSQL (padrão: `postgres`)
- `DB_PASSWORD`: Senha do PostgreSQL (padrão: `postgres`)
- `TRANSLOG_URL`: URL da API da TransLog (padrão: `http://localhost:9999`)

## Compilação

```bash
mvn clean compile
```

## Testes

```bash
mvn clean test
```

## Build e Verificação

```bash
mvn clean verify
```

## Endpoints

### Checkout
- `POST /checkout/resumo` - Calcular resumo da compra

### Pedidos
- `POST /pedidos` - Registrar novo pedido
- `GET /pedidos/{id}` - Consultar pedido
- `POST /pedidos/{id}/pagamento` - Registrar pagamento
- `POST /pedidos/{id}/envio` - Registrar envio
- `POST /pedidos/{id}/entrega` - Registrar entrega
- `POST /pedidos/{id}/cancelamento` - Cancelar pedido

### Produtos
- `GET /produtos/{sku}` - Consultar produto

### Clientes
- `GET /clientes/{cpf}` - Consultar dados do cliente
- `GET /clientes/{cpf}/avisos` - Consultar avisos do cliente

### Avisos
- `POST /avisos/{id}/leitura` - Marcar aviso como lido

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/loja/pedidos/
│   │   ├── LojaPedidosApplication.java
│   │   ├── client/          - Clientes HTTP
│   │   ├── config/          - Configurações
│   │   ├── controller/      - Controllers REST
│   │   ├── dto/             - Data Transfer Objects
│   │   ├── entity/          - Entidades JPA
│   │   ├── exception/       - Exceções customizadas
│   │   ├── repository/      - Repositórios JPA
│   │   ├── service/         - Serviços de negócio
│   │   └── util/            - Utilitários
│   └── resources/
│       └── application.yml  - Configuração da aplicação
└── test/
    └── java/com/loja/pedidos/
        ├── service/         - Testes de serviços
        ├── util/            - Testes de utilitários
        ├── controller/      - Testes de controllers
        └── integration/     - Testes de integração
```

## Notas de Implementação

### Arredondamento
Todos os cálculos de valor em dinheiro utilizam arredondamento "meio para o par" (HALF_EVEN) com 2 casas decimais.

### Cupons
- `BEMVINDO10`: 10% de desconto no subtotal (apenas primeira compra)
- `MENOS50`: R$ 50 de desconto (mínimo R$ 300 em produtos)
- `FRETEGRATIS`: Frete grátis
- `LEVE3PAGUE2`: A cada 3 unidades, 1 sai grátis

### Clube
- `BRONZE`: Sem benefícios
- `PRATA`: 2% de crédito em produtos
- `OURO`: 5% de crédito, frete grátis (exceto motoboy), brinde acima de R$ 500

### Pagamento
- `PIX`: 5% de desconto (sem cupom)
- `CARTAO`: Até 3x sem juros, 4-12x com 1.99% ao mês
- `BOLETO`: Taxa fixa de R$ 3.49 (máximo R$ 1000)
