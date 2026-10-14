# Arquitetura do Serviço de Pedidos

## Estrutura de Diretórios

```
src/main/
├── java/com/loja/pedidos/
│   ├── PedidosApplication.java          # Classe principal
│   ├── controller/                      # Controllers REST
│   │   ├── CheckoutController.java
│   │   ├── PedidoController.java
│   │   ├── ProdutoController.java
│   │   ├── ClienteController.java
│   │   ├── AvisoController.java
│   │   └── GlobalExceptionHandler.java
│   ├── service/                         # Lógica de negócio
│   │   ├── PedidoService.java
│   │   ├── ResumoPedidoService.java
│   │   ├── ValidacaoService.java
│   │   ├── TransLogService.java
│   │   ├── ClienteService.java
│   │   ├── AvisoService.java
│   │   └── MoneyUtil.java
│   ├── repository/                      # Acesso a dados
│   │   ├── PedidoRepository.java
│   │   ├── ClienteRepository.java
│   │   ├── ProdutoRepository.java
│   │   ├── AvisoRepository.java
│   │   └── PagamentoRepository.java
│   ├── domain/                          # Entidades JPA
│   │   ├── Pedido.java
│   │   ├── PedidoItem.java
│   │   ├── Cliente.java
│   │   ├── Aviso.java
│   │   ├── Pagamento.java
│   │   └── Produto.java
│   ├── dto/                             # Data Transfer Objects
│   │   ├── CheckoutRequestDTO.java
│   │   ├── ResumoDTO.java
│   │   ├── PedidoResponseDTO.java
│   │   ├── ItemDTO.java
│   │   ├── ClienteDTO.java
│   │   ├── ClienteSaldoDTO.java
│   │   ├── AvisoDTO.java
│   │   ├── ProdutoDTO.java
│   │   └── ErrorResponseDTO.java
│   └── exception/
│       └── PedidoException.java
└── resources/
    ├── application.properties
    ├── application-local.properties.example
    └── db/migration/
        ├── V1__init_schema.sql
        └── V2__insert_catalogo.sql
```

## Fluxo de Requisição

### 1. Checkout (Resumo)
```
POST /checkout/resumo
  → CheckoutController.calcularResumo()
    → ValidacaoService.validarCheckout()
    → ValidacaoService.validarDisponibilidadeEntrega()
    → ValidacaoService.validarCupomAplicavel()
    → ResumoPedidoService.calcularResumo()
    → ValidacaoService.validarFormaPagamentoDisponivel()
```

### 2. Criar Pedido
```
POST /pedidos
  → PedidoController.criarPedido()
    → PedidoService.criarPedido()
      → [validações igual ao checkout]
      → Salva Pedido no banco
      → Cria/Atualiza Cliente
```

### 3. Processar Pagamento
```
POST /pedidos/{id}/pagamento
  → PedidoController.registrarPagamento()
    → PedidoService.processarPagamento()
      → Valida se não passou 48h
      → Verifica estoque
      → Baixa estoque
      → Adiciona crédito do clube
      → Cria aviso PAGAMENTO_APROVADO
      → Marca como PAGO
```

### 4. Enviar Pedido
```
POST /pedidos/{id}/envio
  → PedidoController.enviarPedido()
    → PedidoService.enviarPedido()
      → Marca como ENVIADO
      → Cria aviso PEDIDO_ENVIADO
```

### 5. Entregar Pedido
```
POST /pedidos/{id}/entrega
  → PedidoController.entregarPedido()
    → PedidoService.entregarPedido()
      → Marca como ENTREGUE
```

### 6. Cancelar Pedido
```
POST /pedidos/{id}/cancelamento
  → PedidoController.cancelarPedido()
    → PedidoService.cancelarPedido()
      → Se PAGO: devolve estoque e crédito
      → Marca como CANCELADO
      → Cria aviso PEDIDO_CANCELADO
```

## Entidades e Relacionamentos

```
Produto (sku PK)
  ├─ sku: String
  ├─ nome: String
  ├─ preco: BigDecimal
  ├─ pesoKg: BigDecimal
  ├─ estoque: int
  └─ foraDeLinha: boolean

Cliente (cpf PK)
  ├─ cpf: String
  ├─ email: String
  ├─ saldoCredito: BigDecimal
  ├─ nivelClube: String
  └─ criadoEm: Instant

Pedido (id PK)
  ├─ id: Long
  ├─ cpfCliente: String (FK)
  ├─ situacao: String
  ├─ criadoEm: Instant
  ├─ resumo: JsonNode
  ├─ codigoRastreio: String
  ├─ idTransacao: String
  └─ itens: List<PedidoItem>

PedidoItem
  ├─ id: Long
  ├─ pedidoId: Long (FK)
  ├─ sku: String (FK)
  └─ quantidade: int

Aviso (id PK)
  ├─ id: Long
  ├─ cpfCliente: String (FK)
  ├─ tipo: String (PAGAMENTO_APROVADO|PEDIDO_ENVIADO|PEDIDO_CANCELADO)
  ├─ pedidoId: Long (FK)
  ├─ situacao: String (NOVO|LIDO)
  └─ criadoEm: Instant

Pagamento (id PK)
  ├─ id: Long
  ├─ pedidoId: Long (FK)
  ├─ idTransacao: String (UNIQUE)
  ├─ pagoEm: Instant
  └─ registradoEm: Instant
```

## Transações

As operações críticas são executadas com `@Transactional`:

- `criarPedido()`: Atomic
- `processarPagamento()`: Atomic
- `enviarPedido()`: Atomic
- `entregarPedido()`: Atomic
- `cancelarPedido()`: Atomic

Isso garante que não há estados inconsistentes.

## Tratamento de Erros

Todas as exceções de negócio são `PedidoException`, que contêm:
- `errorCode`: Código do erro (ex: PEDIDO_INVALIDO)
- `statusCode`: HTTP status apropriado
- `message`: Mensagem para debug

O `GlobalExceptionHandler` converte para JSON:
```json
{
  "erro": "CODIGO_DO_ERRO"
}
```

## Migrações de Banco

### V1: Schema Inicial
- Cria todas as tabelas
- Define constraints e índices

### V2: Catálogo de Produtos
- Insere os 8 produtos iniciais

As migrações são executadas automaticamente pelo Flyway ao iniciar.

## Validações

O `ValidacaoService` valida em ordem:

1. Dados básicos (CPF, email, itens)
2. Existência de produtos
3. Produtos não estão fora de linha
4. Estoque disponível
5. Nível de clube válido
6. Região válida
7. CEP válido
8. Modalidade de entrega válida
9. Disponibilidade de entrega (peso, CEP)
10. Cupom existe
11. Cupom é aplicável
12. Forma de pagamento válida
13. Parcelas válidas para forma de pagamento
14. Forma de pagamento disponível (boleto < R$ 1.000)

## Cálculos de Preço

### MoneyUtil
Todos os valores são arredondados com `RoundingMode.HALF_EVEN`:
- 2.995 → 3.00 (arredonda para par)
- 2.985 → 2.98 (arredonda para par)

### ResumoPedidoService
Calcula na ordem correta:
1. Subtotal
2. Frete (consultando TransLog se necessário)
3. Desconto de cupom (máx. 30% do subtotal)
4. Seguro
5. Ajuste de pagamento (desconto PIX, taxa boleto, juros cartão)
6. Total final
7. Valor da parcela (se parcelado)
8. Crédito para próxima compra
9. Elegibilidade para brinde

### Integração TransLog
```java
GET {TRANSLOG_URL}/v2/quote?zip=CEP&weight_g=PESO&service=STD|EXP
```

Timeout: 2 segundos
Resposta esperada:
```json
{
  "price_cents": 3310,
  "eta_hours": 41
}
```

Se indisponível ou timeout → `TRANSPORTADORA_INDISPONIVEL`

## Configuração

Via variáveis de ambiente:
- `DB_URL`: URL do PostgreSQL
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do banco
- `TRANSLOG_URL`: URL da TransLog

Via properties:
- `server.port`: Porta do serviço (default 8080)
- `spring.jpa.show-sql`: Debug SQL (default false)
- `translog.timeout.seconds`: Timeout TransLog (default 2)

## Testes

### MoneyUtilTest
Testa arredondamento HALF_EVEN e operações monetárias.

### PedidoIntegrationTest
Testa fluxo completo com TestContainers (PostgreSQL).

Run: `mvn test`

(Requer Docker para TestContainers)

## Build e Deploy

```bash
# Build JAR executável
mvn clean verify

# Rodar com Spring Boot
java -jar target/pedidos-1.0.0.jar

# Ou via Maven
mvn spring-boot:run
```

Ambiente local:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/loja_pedidos"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export TRANSLOG_URL="https://api.translog.com.br"
```

O banco é criado automaticamente (schema + dados iniciais).
