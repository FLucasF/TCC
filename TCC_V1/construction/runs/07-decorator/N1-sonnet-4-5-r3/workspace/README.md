# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para a Pousada Maré Alta, construído com Java 21 e Javalin.

## Tecnologias

- Java 21
- Javalin 6.7.0 (Web framework)
- PostgreSQL 18 (Banco de dados)
- HikariCP (Connection pool)
- Maven (Build tool)

## Configuração

O sistema utiliza variáveis de ambiente para configuração:

- `DB_URL`: URL JDBC do banco PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta HTTP onde o servidor irá rodar (padrão: 7070)
- `HOTEL_TEST_MODE`: Define se o sistema está em modo de teste (`true` ou `false`)

## Como executar

### 1. Compilar o projeto

```bash
mvn clean package
```

### 2. Executar o servidor

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT="7070"

java -jar target/reservations-1.0.0.jar
```

## Funcionalidades

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Preços

- **Alta temporada** (15/dez a fim/fev + julho): +20% na diária
- **Plano não reembolsável**: -15% de desconto sobre o total
- Arredondamento: "meio para o par" (banker's rounding) em cada passo

### Fluxo de reserva

1. **PENDING**: Reserva criada
2. **CONFIRMED**: Reserva confirmada e paga
3. **CHECKED_IN**: Hóspede fez check-in
4. **CHECKED_OUT**: Hóspede fez check-out
5. **CANCELLED**: Reserva cancelada (possível antes do check-in)

### Sistema de mensagens

O sistema possui um pipeline extensível de regras para processamento de mensagens:

#### Regras padrão (modo normal)

1. **Validação de e-mail**: Bloqueia mensagens para e-mails inválidos
2. **Cópia para arquivo**: Envia cópia para `arquivo@pousadamarealta.com.br` com prefixo `[Cópia]`

#### Regras em modo de teste

1. **Validação de e-mail**: Bloqueia mensagens para e-mails inválidos
2. **Redirecionamento**: Redireciona para `testes@pousadamarealta.com.br` com prefixo `[TESTE]` e linha adicional com destinatário original

#### Adicionando novas regras

Para adicionar uma nova regra de mensagem:

1. Crie uma classe que implemente `MessageRule`
2. Adicione a regra no `MessagePipeline` em `Configuration.java`

Exemplo:

```java
public class CustomRule implements MessageRule {
    @Override
    public MessageContext apply(MessageContext context) {
        // Implementar lógica
        return context;
    }
}
```

## API REST

### Quartos

- `GET /rooms`: Lista todos os quartos

### Reservas

- `POST /reservations`: Cria nova reserva
- `GET /reservations/{id}`: Busca reserva por ID
- `POST /reservations/{id}/confirmation`: Confirma reserva
- `POST /reservations/{id}/cancellation`: Cancela reserva
- `POST /reservations/{id}/check-in`: Faz check-in
- `POST /reservations/{id}/check-out`: Faz check-out e retorna comprovante
- `GET /reservations/{id}/document`: Busca segunda via do comprovante

### Mensagens

- `GET /messages`: Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10`: Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07`: Relatório financeiro (CSV)

## Códigos de erro

- `INVALID_REQUEST` (400): Requisição inválida
- `ROOM_NOT_FOUND` (404): Quarto não encontrado
- `RESERVATION_NOT_FOUND` (404): Reserva não encontrada
- `ROOM_UNAVAILABLE` (409): Quarto não disponível nas datas selecionadas
- `OPERATION_NOT_ALLOWED` (409): Operação não permitida no estado atual da reserva
- `CAPACITY_EXCEEDED` (422): Número de hóspedes excede capacidade do quarto

## Testes

```bash
mvn test         # Executa testes unitários
mvn verify       # Executa validação completa
```

### Testes de integração (opcional)

Os testes de integração estão disponíveis em `src/test/java/com/pousadamarealta/ReservationSystemIT.java.example`.

Para executá-los:

1. Inicie um banco PostgreSQL
2. Renomeie o arquivo removendo `.example`
3. Configure as variáveis de ambiente no teste
4. Execute `mvn verify`

## Arquitetura

O sistema foi organizado seguindo os princípios do CLAUDE.md:

- **Domain**: Entidades de negócio e lógica de domínio
- **Messaging**: Sistema extensível de regras para processamento de mensagens
- **Repository**: Acesso a dados
- **Service**: Orquestração de lógica de negócio
- **Web**: Camada HTTP (controllers e DTOs)
- **Config**: Configuração e inicialização

### Extensibilidade

O sistema foi projetado para ser facilmente extensível:

- Novas regras de mensagem podem ser adicionadas sem modificar as existentes
- Cada regra é uma classe independente que implementa `MessageRule`
- O pipeline processa as regras em ordem, permitindo composição
- Regras podem bloquear, modificar ou gerar mensagens adicionais
