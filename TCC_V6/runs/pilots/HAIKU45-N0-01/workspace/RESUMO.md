# Resumo da Implementação - Serviço de Pedidos

## O que foi Implementado

Um serviço completo de gerenciamento de pedidos para uma loja online de roupas e acessórios, desenvolvido em Java 21 com Spring Boot 3.3.0 e PostgreSQL 18.

## Estrutura do Projeto

### Configuração
- `pom.xml` - Configuração Maven com todas as dependências
- `src/main/resources/application.yml` - Configuração da aplicação
- `src/test/resources/application.yml` - Configuração de testes

### Entities (Modelos de Dados)
- `Produto` - Produtos do catálogo com SKU, preço, peso, estoque
- `Pedido` - Pedidos com status (AGUARDANDO_PAGAMENTO, PAGO, ENVIADO, ENTREGUE, CANCELADO)
- `ItemPedido` - Itens dentro de cada pedido
- `Cliente` - Clientes com CPF, saldo de crédito do clube
- `Aviso` - Avisos para clientes sobre seus pedidos

### DTOs (Objetos de Transferência de Dados)
- `DadosCompra` - Requisição com dados da compra (cliente, itens, entrega, pagamento)
- `Resumo` - Resposta com resumo detalhado da compra
- `RespostaPedido` - Detalhes completos do pedido
- `RespostaProduto` - Informações do produto
- `RespostaCliente` - Dados do cliente (CPF e saldo)
- `RespostaAviso` - Informações de avisos

### Services (Lógica de Negócio)
- `CalculoService` - Cálculos complexos:
  - Subtotal de produtos
  - Desconto de cupons (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2)
  - Frete (RETIRADA_LOJA, MOTOBOY, ECONOMICA, EXPRESSA via TransLog)
  - Seguro por região
  - Ajuste de pagamento (Pix 5%, Cartão com juros, Boleto com taxa)
  - Crédito do clube (BRONZE, PRATA, OURO)
  - Validação completa de dados

- `PedidoService` - Operações de pedidos:
  - Registrar novo pedido
  - Consultar pedido
  - Registrar pagamento com validação de estoque
  - Registrar envio
  - Registrar entrega
  - Cancelar pedido

- `ClienteService` - Gestão de clientes
- `AvisoService` - Gestão de avisos
- `ProdutoService` - Consulta de produtos

### Controllers REST
- `CheckoutController` - POST /checkout/resumo
- `PedidoController` - CRUD de pedidos e operações
- `ProdutoController` - Consulta de produtos
- `ClienteController` - Consulta de clientes e avisos
- `AvisoController` - Operações em avisos

### Utilitários e Configuração
- `ArredondamentoUtil` - Arredondamento "meio para o par" (HALF_EVEN)
- `TransLogClient` - Integração com transportadora
- `DataInitializer` - Inicialização automática do catálogo de produtos
- `WebConfig` - Configuração do RestTemplate
- `JacksonConfig` - Configuração do ObjectMapper

### Testes
- `CalculoServiceTest` - Testes unitários de cálculos
- `ArredondamentoUtilTest` - Testes de arredondamento
- `CheckoutControllerTest` - Testes de controller
- `PedidoServiceTest` - Testes de serviço de pedidos

## Funcionalidades Implementadas

### 1. Cálculo de Compra
✅ Subtotal de produtos
✅ Cupons de desconto com validações
✅ Frete dinâmico (TransLog)
✅ Seguro por região com limites
✅ Ajuste de forma de pagamento
✅ Crédito do clube
✅ Brinde para OURO acima de R$ 500
✅ Arredondamento correto "meio para o par"

### 2. Cupons
✅ BEMVINDO10: 10% desconto (primeira compra)
✅ MENOS50: R$ 50 desconto (mínimo R$ 300)
✅ FRETEGRATIS: Frete grátis
✅ LEVE3PAGUE2: 1 unidade grátis a cada 3

### 3. Formas de Entrega
✅ RETIRADA_LOJA: Grátis, 1 dia
✅ MOTOBOY: R$ 18, mesmo dia (até 5 kg)
✅ ECONOMICA: TransLog com dias extras para regiões
✅ EXPRESSA: TransLog com dias extras para regiões

### 4. Formas de Pagamento
✅ PIX: 5% desconto (sem cupom)
✅ CARTAO: 1-3x sem juros, 4-12x com 1.99% ao mês
✅ BOLETO: Taxa de R$ 3.49 (máximo R$ 1000)

### 5. Clube do Cliente
✅ BRONZE: Sem benefícios
✅ PRATA: 2% de crédito
✅ OURO: 5% de crédito + frete grátis (exceto motoboy) + brinde

### 6. Gerenciamento de Pedidos
✅ Registrar novo pedido
✅ Rastrear status do pedido
✅ Registrar pagamento
✅ Registrar envio com rastreio
✅ Registrar entrega
✅ Cancelar pedido com devolução de estoque/crédito

### 7. Controle de Estoque
✅ Validação de estoque antes de registrar pedido
✅ Baixa de estoque apenas após confirmação de pagamento
✅ Devolução de estoque ao cancelar pedido pago
✅ Prevenção de vendas duplas

### 8. Avisos para Cliente
✅ Pagamento aprovado
✅ Pedido enviado
✅ Pedido cancelado
✅ Marcação como lido

### 9. Validações
✅ CPF/email obrigatórios
✅ Carrinho não vazio
✅ SKU único no carrinho
✅ Quantidade positiva
✅ Produto existe e está disponível
✅ Estoque suficiente
✅ Cupom válido e aplicável
✅ CEP no formato correto
✅ Forma de entrega disponível
✅ Forma de pagamento disponível
✅ Parcelamento válido para forma de pagamento

## Banco de Dados

### Tabelas
- `produtos` - Catálogo de produtos
- `clientes` - Dados de clientes
- `pedidos` - Pedidos com resumo em JSONB
- `itens_pedido` - Itens dentro de cada pedido
- `avisos` - Avisos gerados durante operações

### Inicialização
- Schema criado automaticamente pelo Hibernate
- Catálogo inicial carregado pelo `DataInitializer`
- Dados persistem entre reinicializações

## Como Executar

### Desenvolvimento
```bash
mvn spring-boot:run
```

### Testes
```bash
mvn clean test
```

### Verificação Completa
```bash
mvn clean verify
```

### Build (JAR)
```bash
mvn clean package
java -jar target/pedidos-1.0.0.jar
```

## Variáveis de Ambiente

- `DB_URL` - URL do PostgreSQL
- `DB_USER` - Usuário do banco
- `DB_PASSWORD` - Senha do banco
- `TRANSLOG_URL` - URL da transportadora (padrão: http://localhost:9999)

## Exemplos de Uso

Ver arquivo `EXEMPLOS.md` para exemplos de requisições HTTP.

## Observações Técnicas

### Arredondamento
Todos os valores monetários usam `RoundingMode.HALF_EVEN` (meio para o par), conforme solicitado:
- 2.995 → 3.00
- 2.985 → 2.98

### Segurança
- Validações em múltiplos níveis
- Prevenção de race conditions no estoque com transações
- Tratamento de erros específicos
- IDs de transação para idempotência

### Performance
- Índices nas colunas de busca frequente
- Queries otimizadas com JPA
- Cache de configurações
- Timeout de 2 segundos para TransLog

### Escalabilidade
- Estrutura preparada para múltiplas instâncias
- Banco de dados centralizado
- APIs RESTful sem estado
- Fácil adição de novas transportadoras/cupons

## Arquivos Inclusos

- `pom.xml` - Configuração Maven
- `README.md` - Documentação principal
- `EXEMPLOS.md` - Exemplos de requisições
- `CONFIGURACAO.md` - Guia de configuração
- `RESUMO.md` - Este arquivo
- `.gitignore` - Arquivos a ignorar no Git
- Source code completo em `src/main/` e `src/test/`

## Status

✅ Projeto completo e funcional
✅ Todos os testes compilam
✅ Lógica de negócio implementada
✅ Integração com banco de dados
✅ APIs REST conforme especificação
✅ Tratamento de erros robusto
