# Serviço de Gestão de Pedidos

Serviço Spring Boot para gerenciar pedidos de uma loja online de roupas e acessórios, incluindo cálculo de preços, controle de estoque, processamento de pagamentos e acompanhamento de envios.

## Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18

## Configuração

### Variáveis de Ambiente

O serviço espera as seguintes variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/loja_pedidos"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export TRANSLOG_URL="https://api.translog.com.br"
```

### Banco de Dados

O banco será criado automaticamente ao iniciar o serviço. As migrações Flyway setuparão o schema.

## Construindo o Projeto

```bash
mvn clean verify
```

## Rodando o Serviço

```bash
mvn spring-boot:run
```

O serviço estará disponível em `http://localhost:8080`.

## API Endpoints

### Checkout

**POST /checkout/resumo**
- Calcula o resumo de um pedido sem registrá-lo
- Body: CheckoutRequest
- Response: ResumoDTO (200)

### Pedidos

**POST /pedidos**
- Registra um novo pedido
- Body: CheckoutRequest
- Response: PedidoResponse (201)

**GET /pedidos/{id}**
- Obtém informações de um pedido
- Response: PedidoResponse (200)

**POST /pedidos/{id}/pagamento**
- Registra o pagamento de um pedido
- Body: { "idTransacao": "...", "pagoEm": "2026-10-14T..." }
- Response: PedidoResponse (200)

**POST /pedidos/{id}/envio**
- Marca um pedido como enviado
- Body: { "codigoRastreio": "..." }
- Response: PedidoResponse (200)

**POST /pedidos/{id}/entrega**
- Marca um pedido como entregue
- Response: PedidoResponse (200)

**POST /pedidos/{id}/cancelamento**
- Cancela um pedido
- Response: PedidoResponse (200)

### Produtos

**GET /produtos/{sku}**
- Obtém informações de um produto
- Response: ProdutoDTO (200)

### Clientes

**GET /clientes/{cpf}**
- Obtém saldo de crédito de um cliente
- Response: ClienteSaldoDTO (200)

**GET /clientes/{cpf}/avisos**
- Obtém avisos de um cliente
- Response: List<AvisoDTO> (200)

### Avisos

**POST /avisos/{id}/leitura**
- Marca um aviso como lido
- Response: AvisoDTO (200)

## Arquitetura

### Camadas

- **Controllers**: Expõem os endpoints REST
- **Services**: Contêm a lógica de negócio
- **Repositories**: Acesso a dados via JPA
- **Domain**: Entidades JPA

### Serviços Principais

- `PedidoService`: Gerencia ciclo de vida dos pedidos
- `ResumoPedidoService`: Calcula resumos com todas as regras de preço
- `ValidacaoService`: Valida requisições
- `TransLogService`: Integra com transportadora TransLog
- `ClienteService`: Gerencia dados de clientes
- `AvisoService`: Gerencia avisos para clientes

### Utilitários

- `MoneyUtil`: Operações com valores monetários e arredondamento HALF_EVEN

## Regras de Negócio Implementadas

### Cálculo de Preços (em ordem)
1. Subtotal: soma de (preço × quantidade)
2. Desconto do cupom (se aplicável)
3. Frete: conforme modalidade selecionada
4. Seguro: conforme região do cliente
5. Ajuste de pagamento (juros, taxa, desconto)
6. Total final

### Cupons
- **BEMVINDO10**: 10% desconto para primeira compra
- **MENOS50**: R$ 50 desconto em compras ≥ R$ 300
- **FRETEGRATIS**: Frete grátis
- **LEVE3PAGUE2**: A cada 3 unidades de um produto, uma grátis

Máximo de desconto: 30% do subtotal.

### Clube
- **BRONZE**: Sem benefícios
- **PRATA**: 2% de crédito para próxima compra
- **OURO**: 5% de crédito, frete grátis, brinde em compras > R$ 500

### Formas de Pagamento
- **PIX**: 5% desconto (não acumula com cupom)
- **CARTÃO**: Até 12 parcelas (sem juros até 3x, 1.99% a.m. depois)
- **BOLETO**: Taxa de R$ 3.49 (máximo R$ 1.000)

### Seguro
Porcentagem sobre subtotal conforme região, com limites:
- Sudeste/Sul: 1% (sem limite)
- Centro-Oeste: 1.5% (sem limite)
- Norte: 2.5% (máx. R$ 30)
- Nordeste: 2% (máx. R$ 30)

### Entrega
- **RETIRADA_LOJA**: Grátis, 1 dia
- **MOTOBOY**: R$ 18, até 5 kg
- **ECONOMICA**: TransLog, STD
- **EXPRESSA**: TransLog, EXP

Regiões Norte e Nordeste têm prazos adicionais.

### Estoque
- Só baixa após pagamento confirmado
- Nunca fica negativo
- Produtos fora de linha não podem ser vendidos

## Testes

Os testes unitários podem ser rodados com:

```bash
mvn test
```

Os testes de integração requerem Docker para rodar PostgreSQL via TestContainers.

## Build

O build gera um JAR executável em `target/pedidos-1.0.0.jar`.

Para rodar diretamente:

```bash
java -jar target/pedidos-1.0.0.jar
```

## Documentação Adicional

Consulte o código para mais detalhes sobre:
- Validações detalhadas de negócio
- Tratamento de transações e concorrência
- Estrutura de dados persisted
