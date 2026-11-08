# Guia de Configuração e Uso

## Pré-requisitos

- Java 21
- PostgreSQL 18
- Maven 3.8+

## Compilação

```bash
mvn clean package
```

Isso gera um JAR executável em `target/app.jar`.

## Configuração do Banco de Dados

O sistema espera um PostgreSQL em execução e obtém as credenciais através de variáveis de ambiente:

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/reservas`)
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do usuário
- `PORT`: Porta do servidor (opcional, padrão 7070)

## Execução

```bash
DB_URL="jdbc:postgresql://localhost:5432/reservas" \
DB_USER="user" \
DB_PASSWORD="password" \
java -jar target/app.jar
```

O servidor iniciará na porta especificada em `PORT` ou 7070 por padrão.

## Arquitetura do Sistema

### Planos de Tarifa Extensíveis

O sistema foi projetado para permitir a adição de novos planos de tarifa sem modificação do código existente:

1. Implemente a interface `RatePlan` em uma nova classe
2. Registre o plano em `RatePlanFactory`

Cada plano define:
- Validação de aplicabilidade
- Cálculo de preço com subtotal e desconto
- Valor de reembolso em caso de cancelamento

### Estrutura de Dados

- **Rooms**: Quartos da pousada (imutáveis, configurados ao iniciar)
- **Reservations**: Registros de reservas com todos os estados
- **Messages**: Mensagens enviadas (atualmente emails)

### Validações

- Quartos ocupados não podem ter reservas conflitantes
- Capacidade de hóspedes não pode exceder a do quarto
- Operações respeitem os estados da reserva
- Planos validam suas próprias regras

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter detalhes
- `POST /reservations/{id}/confirmation` - Confirmar pagamento
- `POST /reservations/{id}/cancellation` - Cancelar
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out e gerar comprovante
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar emails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação por data (CSV)
- `GET /reports/revenue?month=2027-07` - Faturamento por mês (CSV)

## Testes

```bash
mvn test
```

Todos os testes de cálculo de preços dos planos passam validando os exemplos fornecidos.
