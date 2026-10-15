# Serviço de Rastreamento de Pedidos

Sistema de gerenciamento de pedidos para loja online com controle de estados, reembolsos e estoque.

## Compilação e Execução

### Compilar
```bash
mvn clean compile
```

### Rodar testes
```bash
mvn test
```

### Verificar tudo (compilar + testes)
```bash
mvn verify
```

### Executar o serviço
```bash
mvn spring-boot:run
```

Ou:
```bash
java -jar target/pedidos-service-1.0.0.jar
```

O serviço estará disponível em `http://localhost:8080`

## API REST

### Criar um pedido
```bash
POST /pedidos
Content-Type: application/json

{
  "valorProdutos": 200.00,
  "frete": 20.00
}
```

Resposta (201):
```json
{
  "id": "uuid-string",
  "situacao": "AGUARDANDO_PAGAMENTO",
  "descricao": "Aguardando pagamento",
  "valorProdutos": 200.00,
  "frete": 20.00,
  "valorTotal": 220.00,
  "valorReembolsado": 0.00,
  "estoqueDevolvido": false,
  "coletaAgendada": false,
  "historico": ["AGUARDANDO_PAGAMENTO"]
}
```

### Executar uma ação
```bash
POST /pedidos/{id}/acoes
Content-Type: application/json

{
  "acao": "PAGAR"
}
```

Ações válidas: `PAGAR`, `SEPARAR`, `ENVIAR`, `ENTREGAR`, `CANCELAR`, `DEVOLVER`

### Consultar um pedido
```bash
GET /pedidos/{id}
```

## Fluxos de Pedido

### Fluxo Normal (Sem Devolução)
```
AGUARDANDO_PAGAMENTO → PAGAR → PAGO → SEPARAR → EM_SEPARACAO → ENVIAR → ENVIADO → ENTREGAR → ENTREGUE
```

### Com Devolução
```
ENTREGUE → DEVOLVER → DEVOLVIDO
```

### Com Cancelamento em Separação
```
EM_SEPARACAO → CANCELAR → CANCELADO (com devolução parcial e estoque recuperado)
```

## Regras de Reembolso

- **Cancelamento antes do pagamento**: Sem reembolso
- **Cancelamento após pagamento**: Reembolso total (produtos + frete)
- **Cancelamento em separação**: Reembolso total menos R$ 15,00 de taxa (mínimo R$ 0,00)
- **Devolução**: Reembolso dos produtos (sem frete)

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/loja/pedidos/
│   │   ├── PedidosApplication.java        # App principal
│   │   ├── controller/
│   │   │   └── PedidoController.java      # Endpoints REST
│   │   ├── model/
│   │   │   ├── Acao.java                  # Enum de ações
│   │   │   ├── Situacao.java              # Enum de situações
│   │   │   └── Pedido.java                # Entidade de pedido
│   │   ├── dto/
│   │   │   ├── CriarPedidoRequest.java
│   │   │   ├── AcaoRequest.java
│   │   │   ├── PedidoResponse.java
│   │   │   └── ErroResponse.java
│   │   └── service/
│   │       └── PedidoService.java         # Lógica de negócio
│   └── resources/
│       └── application.properties         # Config Spring Boot
└── test/
    └── java/com/loja/pedidos/
        ├── service/PedidoServiceTest.java
        └── controller/PedidoControllerTest.java
```

## Tecnologias

- Java 21
- Spring Boot 3.3.0
- Maven 3.x
- JUnit 5
- Spring Test
