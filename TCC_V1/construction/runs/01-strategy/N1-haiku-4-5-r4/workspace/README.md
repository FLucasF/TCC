# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas implementado em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Execução

### Pré-requisitos
- PostgreSQL 18 rodando
- Java 21
- Variáveis de ambiente:
  - `DB_URL`: JDBC URL do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
  - `DB_USER`: Usuário do banco
  - `DB_PASSWORD`: Senha do banco
  - `PORT`: Porta (padrão: 7070)

### Iniciar o servidor

```bash
java -jar target/mare-alta.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Listar e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Exemplos de Uso

### Criar Reserva STANDARD
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Criar Reserva CORPORATE
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432101",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
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

### Relatório de Ocupação
```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

### Relatório Financeiro
```bash
curl http://localhost:7070/reports/revenue?month=2027-07
```

## Planos de Tarifa

1. **STANDARD**: Preço normal da diária
2. **NON_REFUNDABLE**: 15% desconto, não reembolsável
3. **CORPORATE**: Desconto por empresa (TECNOSUL: 12%, AGROVALE: 18%, FARMAVIDA: 25%)
4. **WEEKEND_PACKAGE**: Sexta a domingo, preço fixo (R$ 520 STD, R$ 820 DLX, R$ 1.200 SUITE)
5. **LONG_STAY**: 7+ noites, 10% desconto

## Temporada Alta

- 15 de dezembro a 29 de fevereiro
- Mês de julho inteiro
- Acréscimo de 20% na diária

## Estados da Reserva

1. **PENDING**: Recém-criada, não paga
2. **CONFIRMED**: Paga, aguardando check-in
3. **CHECKED_IN**: Hóspede chegou
4. **CHECKED_OUT**: Hóspede saiu
5. **CANCELLED**: Cancelada

## Compilar

```bash
mvn clean package
```

## Testes

```bash
mvn verify
```
