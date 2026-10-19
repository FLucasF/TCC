# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, construído com Java 21 e Javalin 6.7.0.

## Características

- **Gerenciamento de Quartos**: 5 quartos (101-102: STANDARD, 201-202: DELUXE, 301: SUITE)
- **Reservas com Tarifação**: Alta temporada (15/12 a 28/02 e julho) com 20% acréscimo
- **Planos de Tarifa**: STANDARD (sem desconto) e NON_REFUNDABLE (15% de desconto)
- **Estados de Reserva**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT, com possibilidade de CANCELLED
- **Notificações por Email**: Hóspede, Governança e Financeiro recebem notificações automáticas
- **Relatórios**: Ocupação por data e receita por mês em CSV
- **Armazenamento em Memória**: Dados perdidos ao reiniciar (conforme especificado)

## Compilação

```bash
mvn clean package
```

Gera um JAR executável em `target/app.jar`.

## Execução

```bash
# Na porta padrão (7070)
java -jar target/app.jar

# Em porta customizada
PORT=8080 java -jar target/app.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar uma nova reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar a reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `POST /reservations/{id}/cancellation` - Cancelar a reserva
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Ver todas as mensagens de email enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação de um dia (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Receita de um mês (CSV)

## Exemplo de Uso

### Criar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Ao confirmar, são automaticamente enviadas 3 emails:
- **Hóspede**: Confirmação com comprovante
- **Governança** (governanca@pousadamarealta.com.br): Datas e número de hóspedes
- **Financeiro** (financeiro@pousadamarealta.com.br): Valor pago

### Check-in e Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
curl -X POST http://localhost:7070/reservations/1/check-out
```

### Cancelamento

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Se a reserva estava confirmada, são enviados emails:
- **Governança**: Datas que ficaram livres
- **Financeiro**: Valor a devolver (apenas se reembolsável)

### Relatórios

```bash
# Ocupação em 10/07/2027
curl http://localhost:7070/reports/occupancy?date=2027-07-10

# Receita em julho/2027
curl http://localhost:7070/reports/revenue?month=2027-07
```

## Validações

O sistema valida:
- Datas de saída após entrada
- Número de hóspedes entre 1 e capacidade do quarto
- Disponibilidade do quarto nas datas solicitadas
- Existência do quarto
- Transições de estado válidas da reserva

## Testes

```bash
mvn test
mvn verify  # Executa testes e validações finais
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── App.java                    # Aplicação principal e endpoints
├── model/
│   ├── RoomType.java          # Enum: STANDARD, DELUXE, SUITE
│   ├── ReservationStatus.java  # Enum: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
│   ├── RatePlan.java          # Enum: STANDARD, NON_REFUNDABLE
│   ├── Room.java              # Modelo de quarto
│   ├── Reservation.java       # Modelo de reserva
│   ├── Message.java           # Modelo de mensagem de email
│   └── Receipt.java           # Modelo de comprovante
└── service/
    ├── ReservationService.java # Lógica de negócios
    ├── PricingService.java     # Cálculos de preço e tarifação
    └── FormatService.java      # Formatação de valores e datas
```

## Notas Técnicas

- **Arredondamento**: Utiliza "meio para o par" (banker's rounding) via BigDecimal.HALF_EVEN
- **Altas Temporadas**: 
  - Julho (todos os dias)
  - 15 de dezembro a 28 de fevereiro
- **Cálculo de Preço**: Noite × Taxa da Temporada = Preço da Noite → Soma das Noites → Desconto (se aplicável) = Total
- **Mensagens**: Armazenadas em memória, consultáveis via GET /messages
