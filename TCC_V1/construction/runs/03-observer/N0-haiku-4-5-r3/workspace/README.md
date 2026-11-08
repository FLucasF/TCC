# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java com Javalin e PostgreSQL.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Setup Inicial

### 1. Banco de Dados

Crie um banco PostgreSQL:

```bash
createdb pousada_marealta
```

### 2. Variáveis de Ambiente

Configure as variáveis de ambiente necessárias:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada_marealta"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT="7070"  # Opcional, padrão é 7070
```

### 3. Compilar o Projeto

```bash
mvn clean package
```

### 4. Executar o Sistema

```bash
java -jar target/pousada-system.jar
```

O servidor estará disponível em `http://localhost:7070`

## API Endpoints

### Quartos

- **GET /rooms** - Lista todos os quartos

Resposta:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### Reservas

- **POST /reservations** - Criar uma reserva
- **GET /reservations/{id}** - Obter detalhes da reserva
- **POST /reservations/{id}/confirmation** - Confirmar uma reserva (com pagamento)
- **POST /reservations/{id}/check-in** - Fazer check-in
- **POST /reservations/{id}/check-out** - Fazer check-out
- **POST /reservations/{id}/cancellation** - Cancelar a reserva
- **GET /reservations/{id}/document** - Obter segunda via do comprovante

### Mensagens (Emails)

- **GET /messages** - Listar todas as mensagens enviadas

### Relatórios

- **GET /reports/occupancy?date=2027-07-10** - Relatório de ocupação (formato CSV)
- **GET /reports/revenue?month=2027-07** - Relatório de receita (formato CSV)

## Estrutura de Dados

### Quartos

A pousada tem 5 quartos:

- **101, 102** (STANDARD): 2 pessoas, R$ 249,90/noite
- **201, 202** (DELUXE): 3 pessoas, R$ 389,90/noite
- **301** (SUITE): 4 pessoas, R$ 579,90/noite

### Planos de Tarifa

- **STANDARD**: Reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Alta Temporada

- 15 de dezembro a 29 de fevereiro (aumento de 20%)
- Todo mês de julho (aumento de 20%)

## Processo de Reserva

1. **Criação (PENDING)**: Hospede preenche o formulário
2. **Confirmação (CONFIRMED)**: Hospede paga, recebe comprovante por email
3. **Check-in (CHECKED_IN)**: Recepcionista registra a entrada
4. **Check-out (CHECKED_OUT)**: Recepcionista registra a saída
5. **Cancelamento (CANCELLED)**: Possível antes do check-in, com reembolso conforme o plano

## Notificações

O sistema envia notificações automáticas para:

- **Hospede**: Comprovante de reserva confirmada
- **Governança**: Aviso de reserva confirmada e cancelada
- **Financeiro**: Aviso de pagamento recebido e reembolsos

## Exemplo de Uso

### 1. Criar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

### 2. Confirmar a Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}'
```

### 3. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 4. Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Testes

Executar os testes unitários:

```bash
mvn verify
```

## Estrutura do Projeto

```
src/
├── main/java/br/marealta/
│   ├── Main.java                    # Entrada da aplicação
│   ├── api/                         # Controllers REST
│   ├── db/                          # Camada de dados (DAO)
│   ├── model/                       # Modelos de dados
│   ├── service/                     # Lógica de negócio
│   └── util/                        # Utilitários
└── test/java/br/marealta/
    └── util/                        # Testes unitários
```

## Observações

- O sistema é extensível para novos canais de notificação (SMS, Slack, etc)
- Os emails não são realmente enviados, apenas armazenados na base de dados
- Arredondamento de valores usa "meio para o par" (banker's rounding)
- O banco de dados é inicializado automaticamente na primeira execução
