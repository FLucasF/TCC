# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gestão de reservas para a Pousada Maré Alta, incluindo suporte para CPF, CNPJ e Passaporte desde o primeiro dia.

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.6+

## Compilação

```bash
mvn clean package
```

## Execução

### Variáveis de Ambiente

```bash
export DB_URL="jdbc:postgresql://localhost:5432/reservas"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT="7070"
```

### Iniciando a Aplicação

```bash
java -jar target/app.jar
```

O servidor iniciará na porta especificada (padrão: 7070).

## Funcionalidades

### Quartos
- 5 quartos com capacidades e tarifas pré-configuradas
- Tarifa dinâmica baseada na época do ano (alta temporada: +20%)

### Reservas
- Criação de reservas com validação de disponibilidade
- Suporte para três tipos de identificação:
  - **CPF**: Gera comprovante em português
  - **CNPJ + Razão Social**: Gera fatura de hospedagem com ISS
  - **Passaporte + País**: Gera booking receipt em inglês
- Ciclo de vida: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento com reembolso condicionado ao plano

### Planos de Tarifa
- **STANDARD**: Sem desconto, reembolsável em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, não reembolsável

### Documentos
- Comprovante de reserva (CPF)
- Fatura de hospedagem (CNPJ) com ISS retido (5%)
- Booking receipt em inglês (Passaporte)

### Relatórios
- **Ocupação**: Consulta de ocupação por data (CSV)
- **Financeiro**: Receita por mês (CSV)

### Mensagens
- Envio automático de e-mail de confirmação
- Armazenamento de mensagens enviadas para consulta

## API REST

### Quartos
```
GET /rooms
```

### Reservas
```
POST /reservations                      # Criar reserva
GET /reservations/{id}                  # Consultar reserva
POST /reservations/{id}/confirmation    # Confirmar pagamento
POST /reservations/{id}/cancellation    # Cancelar reserva
POST /reservations/{id}/check-in        # Check-in
POST /reservations/{id}/check-out       # Check-out
GET /reservations/{id}/document         # Segunda via do documento
```

### Mensagens
```
GET /messages
```

### Relatórios
```
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
```

## Exemplos de Uso

### Criar Reserva com CPF
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Criar Reserva com CNPJ
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@email.com",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

### Criar Reserva com Passaporte
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "John Smith",
    "guestEmail": "john@email.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar Reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### Check-out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

### Consultar Ocupação
```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

### Consultar Receita
```bash
curl http://localhost:7070/reports/revenue?month=2027-07
```

## Notas Técnicas

- Arredondamento usando HALF_EVEN ("meio para o par")
- Datas em formato ISO (YYYY-MM-DD) na API
- Valores monetários com 2 casas decimais
- Todas as operações usam transações do PostgreSQL
- Pool de conexões HikariCP para performance
