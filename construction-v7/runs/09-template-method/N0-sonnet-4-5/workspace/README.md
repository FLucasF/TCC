# Pousada Maré Alta - Sistema de Reservas

Sistema de gestão de reservas para a Pousada Maré Alta.

## Compilação e Execução

### Compilar o projeto

```bash
mvn package
```

### Executar o servidor

```bash
java -jar target/reservations-1.0.0.jar
```

O servidor inicia na porta definida pela variável de ambiente `PORT`, ou 7070 caso não esteja definida.

### Rodar os testes

```bash
mvn test
```

### Verificação completa

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva (pagamento)
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna o comprovante
- `GET /reservations/{id}/document` - Obtém a segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todos os e-mails enviados (armazenados em memória)

### Relatórios (CSV)

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação para uma data
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro mensal
- `GET /reports/tourism-tax?month=YYYY-MM` - Relatório de taxa de turismo mensal
- `GET /reports/rate-plans?month=YYYY-MM` - Relatório de planos de tarifa mensal

## Quartos

- **101, 102**: STANDARD (2 pessoas) - R$ 249,90/noite
- **201, 202**: DELUXE (3 pessoas) - R$ 389,90/noite
- **301**: SUITE (4 pessoas) - R$ 579,90/noite

## Regras de Negócio

### Alta Temporada (+20%)

- 15 de dezembro a 28/29 de fevereiro
- Mês de julho inteiro

### Planos de Tarifa

- **STANDARD**: Preço normal, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, não reembolsável

### Status da Reserva

1. **PENDING**: Reserva criada, aguardando pagamento
2. **CONFIRMED**: Reserva confirmada (paga)
3. **CHECKED_IN**: Hóspede fez check-in
4. **CHECKED_OUT**: Hóspede fez check-out
5. **CANCELLED**: Reserva cancelada

### Taxa de Turismo

R$ 4,50 por hóspede por noite (aplicada nas reservas pagas).

## Exemplo de Uso

```bash
# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar a reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Obter relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"

# Obter relatório de taxa de turismo
curl "http://localhost:7070/reports/tourism-tax?month=2027-07"
```

## Tecnologias

- Java 21
- Javalin 6.7.0
- Maven
- JUnit 5

## Armazenamento

Os dados são armazenados apenas em memória. Ao reiniciar o servidor, todas as reservas são perdidas.
