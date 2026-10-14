# Lista de Arquivos Criados

## Raiz do Projeto
- `pom.xml` - Configuração Maven
- `README.md` - Documentação principal
- `EXEMPLOS.md` - Exemplos de requisições HTTP
- `CONFIGURACAO.md` - Guia de configuração
- `RESUMO.md` - Resumo da implementação
- `ARQUIVOS.md` - Este arquivo
- `.gitignore` - Arquivo de ignore para Git

## Código Principal (src/main)

### Aplicação
- `src/main/java/com/loja/pedidos/LojaPedidosApplication.java` - Classe principal

### Configuração (src/main/java/com/loja/pedidos/config/)
- `TransLogConfig.java` - Configuração da TransLog
- `WebConfig.java` - Configuração do RestTemplate
- `JacksonConfig.java` - Configuração do Jackson ObjectMapper
- `DataInitializer.java` - Inicializador do catálogo de produtos

### Client HTTP (src/main/java/com/loja/pedidos/client/)
- `TransLogClient.java` - Cliente para integração com TransLog

### Controllers (src/main/java/com/loja/pedidos/controller/)
- `CheckoutController.java` - Endpoint de cálculo de resumo
- `PedidoController.java` - Endpoints de operações com pedidos
- `ProdutoController.java` - Endpoint de consulta de produtos
- `ClienteController.java` - Endpoints de dados do cliente e avisos
- `AvisoController.java` - Endpoint de operações em avisos

### DTOs (src/main/java/com/loja/pedidos/dto/)
- `DadosCompra.java` - Dados de entrada para cálculos
- `Resumo.java` - Resumo da compra
- `RespostaPedido.java` - Resposta com dados do pedido
- `RespostaProduto.java` - Resposta com dados do produto
- `RespostaCliente.java` - Resposta com dados do cliente
- `RespostaAviso.java` - Resposta com dados do aviso
- `RequisicaoPagamento.java` - Dados de pagamento
- `RequisicaoEnvio.java` - Dados de envio
- `RespostaErro.java` - Resposta padrão de erro

### Entities (src/main/java/com/loja/pedidos/entity/)
- `Produto.java` - Entidade de Produto
- `Pedido.java` - Entidade de Pedido
- `ItemPedido.java` - Entidade de Item do Pedido
- `Cliente.java` - Entidade de Cliente
- `Aviso.java` - Entidade de Aviso

### Exceções (src/main/java/com/loja/pedidos/exception/)
- `ErroNegocio.java` - Classe de exceções customizadas

### Repositórios (src/main/java/com/loja/pedidos/repository/)
- `ProdutoRepository.java` - Repositório JPA de Produtos
- `PedidoRepository.java` - Repositório JPA de Pedidos
- `ClienteRepository.java` - Repositório JPA de Clientes
- `AvisoRepository.java` - Repositório JPA de Avisos

### Serviços (src/main/java/com/loja/pedidos/service/)
- `CalculoService.java` - Lógica de cálculos de compra (800+ linhas)
- `PedidoService.java` - Lógica de gerenciamento de pedidos
- `ClienteService.java` - Lógica de gerenciamento de clientes
- `AvisoService.java` - Lógica de gerenciamento de avisos
- `ProdutoService.java` - Lógica de gerenciamento de produtos

### Utilitários (src/main/java/com/loja/pedidos/util/)
- `ArredondamentoUtil.java` - Utilitário de arredondamento

### Configuração de Recursos (src/main/resources/)
- `application.yml` - Configuração da aplicação

## Código de Testes (src/test)

### Testes Unitários
- `src/test/java/com/loja/pedidos/service/CalculoServiceTest.java` - Testes de cálculos
- `src/test/java/com/loja/pedidos/service/PedidoServiceTest.java` - Testes de pedidos
- `src/test/java/com/loja/pedidos/util/ArredondamentoUtilTest.java` - Testes de arredondamento
- `src/test/java/com/loja/pedidos/controller/CheckoutControllerTest.java` - Testes de controller

### Configuração de Testes
- `src/test/resources/application.yml` - Configuração específica para testes

## Estatísticas

### Arquivos Criados
- **Total**: ~40 arquivos
- **Código Java**: ~30 arquivos
- **Configuração**: 5 arquivos
- **Documentação**: 7 arquivos

### Linhas de Código
- **Serviços**: ~1200 linhas
- **Controllers**: ~200 linhas
- **DTOs**: ~300 linhas
- **Entities**: ~200 linhas
- **Repositórios**: ~100 linhas
- **Configuração**: ~100 linhas
- **Testes**: ~400 linhas
- **Total**: ~2500+ linhas de código Java

## Estrutura de Pastas

```
workspace/
├── src/
│   ├── main/
│   │   ├── java/com/loja/pedidos/
│   │   │   ├── client/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── util/
│   │   │   └── LojaPedidosApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── java/com/loja/pedidos/
│       │   ├── controller/
│       │   ├── service/
│       │   └── util/
│       └── resources/
│           └── application.yml
├── pom.xml
├── README.md
├── EXEMPLOS.md
├── CONFIGURACAO.md
├── RESUMO.md
├── ARQUIVOS.md
└── .gitignore
```

## Dependências Principais

- Spring Boot 3.3.0
- Spring Data JPA
- PostgreSQL Driver 42.7.3
- Lombok (para reduzir boilerplate)
- Jackson (para JSON)
- TestContainers (para testes de integração)
- JUnit 5 (para testes)
- Mockito (para mocks em testes)

## Build e Teste

- Maven 3.9+
- Java 21
- Surefire para execução de testes
- Spring Boot Maven Plugin para building
