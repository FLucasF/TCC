# Order Service

Sistema de gerenciamento de pedidos para loja online de roupas e acessórios.

## Funcionalidades

- Cálculo de resumo de compra com múltiplas etapas (produtos, desconto de cupom, frete, seguro, ajuste de pagamento)
- Registro de pedidos com controle de estado
- Múltiplas opções de entrega (ECONOMY, EXPRESS, STORE_PICKUP, COURIER)
- Integração com transportadora TransLog
- Sistema de cupons de desconto (WELCOME10, MINUS50, FREESHIPPING, BUY3PAY2)
- Níveis de clube da loja (BRONZE, SILVER, GOLD) com benefícios
- Múltiplas formas de pagamento (PIX, CREDIT_CARD, BANK_SLIP)
- Arredondamento "meio para o par" (HALF_EVEN) em cada etapa
- Controle de estoque com transações ACID
- Sistema de notificações para clientes
- Suporte a múltiplas regiões com seguro regional

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.9+

## Configuração

As seguintes variáveis de ambiente são obrigatórias:

- `DB_URL`: URL da conexão PostgreSQL (exemplo: `jdbc:postgresql://localhost:5432/orders`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `TRANSLOG_URL`: URL da API da TransLog (opcional para testes)

## Compilação e Testes

```bash
mvn clean verify
```

## Inicialização

```bash
mvn spring-boot:run
```

O serviço iniciará na porta 8080.

## Endpoints da API

### Cálculo de Resumo
```
POST /checkout/summary
```

### Registrar Pedido
```
POST /orders
```

### Consultar Pedido
```
GET /orders/{id}
```

### Processar Pagamento
```
POST /orders/{id}/payment
```

### Enviar Pedido
```
POST /orders/{id}/shipment
```

### Entregar Pedido
```
POST /orders/{id}/delivery
```

### Cancelar Pedido
```
POST /orders/{id}/cancellation
```

### Consultar Produto
```
GET /products/{sku}
```

### Consultar Saldo do Cliente
```
GET /customers/{cpf}
```

### Listar Notificações
```
GET /customers/{cpf}/notifications
```

### Marcar Notificação como Lida
```
POST /notifications/{id}/read
```

## Exemplos de Requisição

### Calcular Resumo
```json
{
  "customer": {
    "cpf": "12345678909",
    "email": "cliente@exemplo.com"
  },
  "items": [
    {"sku": "CAM-001", "quantity": 2}
  ],
  "zipCode": "01310100",
  "region": "SOUTHEAST",
  "shippingMethod": "EXPRESS",
  "coupon": null,
  "paymentMethod": "PIX",
  "installments": 1,
  "clubTier": "BRONZE"
}
```

## Estrutura do Projeto

```
src/
  main/
    java/com/loja/order/
      config/          - Configurações do Spring
      controller/      - Controllers REST
      domain/          - Enums e domínio de negócio
      dto/             - Data Transfer Objects
      model/           - Entidades JPA
      repository/      - Interfaces de repositório
      service/         - Lógica de negócio
      util/            - Utilitários
    resources/
      db/migration/    - Scripts Flyway de migração
  test/
    java/com/loja/order/
      service/         - Testes de serviço
      util/            - Testes de utilitários
```

## Catálogo Inicial

O sistema inicia com os seguintes produtos:

- CAM-001: Camiseta básica - R$ 79,90
- TEN-001: Tênis corrida - R$ 249,90
- FON-001: Fone sem fio - R$ 199,90
- MEI-001: Meia esportiva - R$ 19,90
- JAQ-001: Jaqueta corta-vento - R$ 389,90
- MOC-001: Mochila urbana - R$ 159,90
- BON-001: Boné aba reta - R$ 59,90
- REL-001: Relógio digital - R$ 129,90 (descontinuado)

## Notas

- O banco de dados é criado automaticamente na primeira execução
- As migrations Flyway são executadas automaticamente
- O arredondamento segue a regra "meio para o par" (HALF_EVEN) em cada etapa do cálculo
- O estoque é protegido contra race conditions com locks pessimistas
- As transações garantem consistência dos dados mesmo com múltiplas instâncias
