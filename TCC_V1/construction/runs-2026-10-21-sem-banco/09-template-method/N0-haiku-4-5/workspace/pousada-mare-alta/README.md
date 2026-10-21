# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, uma pequena pousada à beira-mar com 5 quartos.

## Como construir e executar

### Compilação
```bash
mvn clean package
```

### Execução
```bash
# Na porta padrão (7070)
java -jar target/app.jar

# Em uma porta específica
PORT=8080 java -jar target/app.jar
```

## API Endpoints

### Quartos
- **GET /rooms** - Lista todos os quartos disponíveis

### Reservas
- **POST /reservations** - Criar uma nova reserva
- **GET /reservations/{id}** - Obter detalhes de uma reserva
- **POST /reservations/{id}/confirmation** - Confirmar uma reserva (e enviar e-mail de confirmação)
- **POST /reservations/{id}/check-in** - Fazer check-in
- **POST /reservations/{id}/check-out** - Fazer check-out (retorna comprovante)
- **POST /reservations/{id}/cancellation** - Cancelar uma reserva
- **GET /reservations/{id}/document** - Obter segunda via do comprovante

### Mensagens
- **GET /messages** - Listar todos os e-mails enviados

### Relatórios (CSV)
- **GET /reports/occupancy?date=YYYY-MM-DD** - Relatório de ocupação
- **GET /reports/revenue?month=YYYY-MM** - Relatório financeiro
- **GET /reports/tourism-tax?month=YYYY-MM** - Relatório de taxa de turismo
- **GET /reports/rate-plans?month=YYYY-MM** - Relatório de planos de tarifa

## Quartos

### STANDARD (101, 102)
- Capacidade: 2 pessoas
- Diária: R$ 249,90

### DELUXE (201, 202)
- Capacidade: 3 pessoas
- Diária: R$ 389,90

### SUITE (301)
- Capacidade: 4 pessoas
- Diária: R$ 579,90

## Regras de negócio

### Alta Temporada
- 15 de dezembro até 28 de fevereiro
- Todo o mês de julho
- Aumento de 20% na diária

### Planos de Tarifa
- **STANDARD**: Sem desconto, reembolso integral em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

### Estados da Reserva
1. **PENDING**: Reserva criada mas não paga
2. **CONFIRMED**: Reserva paga
3. **CHECKED_IN**: Hóspede fez check-in
4. **CHECKED_OUT**: Hóspede fez check-out
5. **CANCELLED**: Reserva cancelada

### Regras de Cancelamento
- Pode cancelar antes do check-in (reservas PENDING ou CONFIRMED)
- Plano STANDARD: reembolsa 100%
- Plano NON_REFUNDABLE: sem reembolso (mantém os 15% de desconto)
- Reserva pendente não reembolsa nada (pois não tinha sido paga)

## Exemplo de requisição

```bash
# Criar uma reserva
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

# Confirmar uma reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Obter relatório de ocupação
curl http://localhost:7070/reports/occupancy?date=2027-07-10

# Obter relatório financeiro
curl http://localhost:7070/reports/revenue?month=2027-07
```

## Estrutura do projeto

```
src/main/java/com/pousada/
├── Application.java           # Aplicação principal (endpoints)
├── dto/                       # Data Transfer Objects
│   ├── ReservationDTO.java
│   ├── RoomDTO.java
│   ├── MessageDTO.java
│   ├── ComprovanteDTO.java
│   └── ErrorDTO.java
├── model/                     # Modelos de domínio
│   ├── Reservation.java
│   ├── Room.java
│   ├── Message.java
│   ├── RoomType.java
│   ├── RatePlan.java
│   └── ReservationStatus.java
├── repository/                # Acesso a dados
│   ├── ReservationRepository.java
│   ├── RoomRepository.java
│   └── MessageRepository.java
├── service/                   # Lógica de negócio
│   ├── ReservationService.java
│   ├── MessageService.java
│   ├── DocumentService.java
│   └── ReportService.java
└── util/                      # Utilitários
    ├── SeasonUtils.java
    └── PricingUtils.java
```

## Testes

```bash
# Rodar todos os testes
mvn test

# Rodar testes e verificação de qualidade
mvn verify
```

## Detalhes técnicos

- **Java**: 21
- **Framework**: Javalin 6.7.0
- **Build**: Maven
- **Dados**: Em memória (reinicia ao parar a aplicação)
- **Serialização**: Jackson com suporte a LocalDate
- **Logging**: SLF4J com SimpleLogger
