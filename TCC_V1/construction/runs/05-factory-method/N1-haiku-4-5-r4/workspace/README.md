# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.8+

## Configuração

### Variáveis de Ambiente

Configure as seguintes variáveis antes de executar o sistema:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada_mare_alta"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT=7070  # Opcional, padrão é 7070
```

### Banco de Dados

Crie um banco de dados PostgreSQL:

```bash
psql -U postgres -c "CREATE DATABASE pousada_mare_alta;"
```

## Build

```bash
mvn clean package
```

Isto gera um jar executável em `target/reservation-system.jar`.

## Execução

```bash
java -jar target/reservation-system.jar
```

O sistema iniciará na porta especificada pela variável `PORT` (padrão: 7070).

## Testes

```bash
mvn test
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva pendente
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza o check-in
- `POST /reservations/{id}/check-out` - Realiza o check-out
- `GET /reservations/{id}/document` - Obtém a segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação para uma data (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro para um mês (CSV)

## Estrutura do Banco de Dados

O sistema cria automaticamente as tabelas necessárias:

- `messages` - Mensagens enviadas (e-mails)
- `reservations` - Dados das reservas

## Exemplo de Uso

### Criar uma reserva (CPF)

```json
POST /reservations
Content-Type: application/json

{
  "roomNumber": 201,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Criar uma reserva (CNPJ - Empresa)

```json
POST /reservations
Content-Type: application/json

{
  "roomNumber": 201,
  "guestName": "Ana Souza",
  "guestEmail": "ana@empresa.com",
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 1,
  "ratePlan": "STANDARD"
}
```

### Criar uma reserva (Passaporte - Estrangeiro)

```json
POST /reservations
Content-Type: application/json

{
  "roomNumber": 201,
  "guestName": "John Smith",
  "guestEmail": "john@example.com",
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Confirmar uma reserva

```json
POST /reservations/1/confirmation
Content-Type: application/json

{
  "amountPaid": 779.80
}
```

## Identificação de Hóspedes

O sistema suporta três tipos de identificação:

1. **CPF** - Pessoa física brasileira
   - Campo: `guestCpf`
   - Gera: Comprovante de reserva em português

2. **CNPJ** - Empresa/pessoa jurídica
   - Campos: `companyCnpj`, `companyName`
   - Gera: Fatura de hospedagem em português com ISS retido

3. **Passaporte** - Hóspede internacional
   - Campos: `guestPassport`, `guestCountry`
   - Gera: Booking receipt em inglês

## Cálculo de Preços

- **Diária base**: Varia conforme o tipo de quarto
- **Alta temporada**: +20% (15 de dezembro até fim de fevereiro, e julho inteiro)
- **Plano NON_REFUNDABLE**: 15% de desconto
- **Arredondamento**: Banker's rounding (HALF_EVEN) para centavos

## Status da Reserva

1. **PENDING** - Reserva criada, aguardando confirmação
2. **CONFIRMED** - Reserva confirmada após pagamento
3. **CHECKED_IN** - Hóspede realizou o check-in
4. **CHECKED_OUT** - Hóspede realizou o check-out
5. **CANCELLED** - Reserva cancelada

Transições permitidas:
- PENDING → CONFIRMED
- PENDING/CONFIRMED → CANCELLED
- CONFIRMED → CHECKED_IN
- CHECKED_IN → CHECKED_OUT
