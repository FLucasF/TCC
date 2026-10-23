# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Arquitetura

O sistema implementa o padrão **Chain of Responsibility** para o envio de mensagens, permitindo adicionar novas regras de processamento sem modificar as existentes. Cada regra pode:
- Validar e bloquear mensagens
- Modificar destinatário, assunto ou corpo
- Gerar mensagens adicionais

### Regras de Mensagens Implementadas

1. **EmailValidationRule**: Valida endereços de e-mail (bloqueia inválidos)
2. **TestModeRule**: Redireciona mensagens para `testes@pousadamarealta.com.br` quando `HOTEL_TEST_MODE=true`
3. **ArchiveCopyRule**: Cria cópia para `arquivo@pousadamarealta.com.br` (apenas em modo produção)

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Build

```bash
mvn clean package
```

O comando gera um JAR executável em `target/reservas-1.0.0.jar`.

## Configuração

O sistema é configurado através de variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT="7070"                    # Opcional, padrão: 7070
export HOTEL_TEST_MODE="true"         # Opcional, padrão: false
```

## Execução

```bash
java -jar target/reservas-1.0.0.jar
```

O sistema:
1. Conecta ao banco de dados
2. Cria as tabelas se não existirem
3. Insere os 5 quartos padrão se o banco estiver vazio
4. Inicia o servidor HTTP na porta configurada

## API REST

### Quartos

**GET /rooms** - Lista todos os quartos

Resposta:
```json
[
  {
    "number": "101",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### Reservas

**POST /reservations** - Cria uma reserva

Request:
```json
{
  "roomNumber": "101",
  "guestName": "Ana Silva",
  "guestEmail": "ana@gmail.com",
  "guestCpf": "12345678900",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Resposta (201 Created):
```json
{
  "id": 1,
  "roomNumber": "101",
  "guestName": "Ana Silva",
  "guestEmail": "ana@gmail.com",
  "guestCpf": "12345678900",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

**GET /reservations/{id}** - Consulta uma reserva

**POST /reservations/{id}/confirmation** - Confirma a reserva (envia e-mail)

**POST /reservations/{id}/cancellation** - Cancela a reserva

**POST /reservations/{id}/check-in** - Faz o check-in

**POST /reservations/{id}/check-out** - Faz o check-out (retorna comprovante)

**GET /reservations/{id}/document** - Segunda via do comprovante (apenas reservas pagas)

### Mensagens

**GET /messages** - Lista todas as mensagens enviadas

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "ana@gmail.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "...",
    "status": "SENT"
  }
]
```

### Relatórios

**GET /reports/occupancy?date=2027-07-10** - Relatório de ocupação (CSV)

**GET /reports/revenue?month=2027-07** - Relatório financeiro (CSV)

## Regras de Negócio

### Preços

- **Quartos**:
  - 101, 102 (STANDARD): R$ 249,90/noite, até 2 pessoas
  - 201, 202 (DELUXE): R$ 389,90/noite, até 3 pessoas
  - 301 (SUITE): R$ 579,90/noite, até 4 pessoas

- **Alta temporada** (+20%): 15/12 a fim de fevereiro + julho inteiro

- **Plano não reembolsável** (-15% do total)

- **Arredondamento**: HALF_EVEN (meio para o par) em cada passo

### Ciclo de Vida da Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED
```

- Confirmação: apenas de PENDING
- Check-in: apenas de CONFIRMED
- Check-out: apenas de CHECKED_IN
- Cancelamento: apenas de PENDING ou CONFIRMED

### Validações

- E-mail inválido: bloqueia o envio (status = BLOCKED)
- Quarto já reservado: erro 409 ROOM_UNAVAILABLE
- Capacidade excedida: erro 422 CAPACITY_EXCEEDED
- Data saída ≤ entrada: erro 400 INVALID_REQUEST
- Menos de 1 hóspede: erro 400 INVALID_REQUEST

## Testes

```bash
# Testes unitários
mvn test

# Build completo
mvn verify
```

## Exemplo de Uso

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva (ID retornado na criação)
curl -X POST http://localhost:7070/reservations/1/confirmation

# Listar mensagens enviadas
curl http://localhost:7070/messages

# Relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── App.java                          # Aplicação principal
├── api/                              # Controladores REST
│   └── ApiController.java
├── domain/                           # Entidades de domínio
│   ├── Room.java
│   ├── Reservation.java
│   └── SentMessage.java
├── messaging/                        # Sistema de mensagens (Chain of Responsibility)
│   ├── MessageRule.java              # Interface das regras
│   ├── MessageDispatcher.java        # Orquestrador
│   ├── EmailValidationRule.java
│   ├── TestModeRule.java
│   └── ArchiveCopyRule.java
├── repository/                       # Acesso ao banco
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── service/                          # Lógica de negócio
│   ├── PricingService.java
│   ├── ReservationService.java
│   ├── MessageService.java
│   └── ReportService.java
└── infra/                            # Infraestrutura
    └── DatabaseSetup.java
```

## Códigos de Erro

- `INVALID_REQUEST` (400): Dados inválidos
- `ROOM_NOT_FOUND` (404): Quarto não existe
- `RESERVATION_NOT_FOUND` (404): Reserva não existe
- `ROOM_UNAVAILABLE` (409): Quarto já reservado
- `OPERATION_NOT_ALLOWED` (409): Operação não permitida no status atual
- `CAPACITY_EXCEEDED` (422): Número de hóspedes excede capacidade
