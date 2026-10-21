# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- Maven
- Jackson (JSON)

## Compilar e Executar

### Compilar

```bash
mvn clean package
```

### Executar

```bash
java -jar target/mare-alta-1.0-SNAPSHOT.jar
```

Por padrão, o servidor inicia na porta 7070. Para usar outra porta:

```bash
PORT=8080 java -jar target/mare-alta-1.0-SNAPSHOT.jar
```

### Testes

```bash
mvn verify
```

## Funcionalidades

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Planos de Tarifa

1. **STANDARD**: Tarifa normal
2. **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento
3. **CORPORATE**: Desconto por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%)
4. **WEEKEND_PACKAGE**: Pacote sexta a domingo com preço fechado
5. **LONG_STAY**: Estadia de 7+ noites com 10% de desconto

### Alta Temporada

- 15 de dezembro a fim de fevereiro
- Julho inteiro
- Acréscimo de 20% na diária

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Exemplo de Uso

```bash
# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@email.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar a reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Consultar mensagens enviadas
curl http://localhost:7070/messages
```
