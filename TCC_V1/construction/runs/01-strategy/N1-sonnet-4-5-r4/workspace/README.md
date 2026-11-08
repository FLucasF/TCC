# Sistema de Reservas - Pousada Maré Alta

Sistema de reservas para pousada com múltiplos planos de tarifa, gestão de ocupação e relatórios financeiros.

## Características

- **5 tipos de quartos**: STANDARD (101, 102), DELUXE (201, 202) e SUITE (301)
- **5 planos de tarifa**: 
  - STANDARD: preço normal com reembolso total
  - NON_REFUNDABLE: 15% de desconto sem reembolso
  - CORPORATE: descontos corporativos (12%, 18%, 25%)
  - WEEKEND_PACKAGE: pacote de fim de semana com preço fixo
  - LONG_STAY: 10% de desconto para 7+ noites
- **Alta temporada**: 15/dez a fim de fev + julho (20% adicional nas diárias)
- **Estados da reserva**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT ou CANCELLED
- **E-mails de confirmação** (armazenados para consulta)
- **Relatórios CSV**: ocupação por noite e receita por mês

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven
- HikariCP (connection pool)
- Testcontainers (testes de integração)

## Arquitetura

O projeto segue o princípio do Open-Closed: novos planos de tarifa podem ser adicionados sem modificar código existente.

```
src/main/java/com/pousada/
├── Main.java
├── domain/
│   ├── Room.java, RoomType.java
│   ├── Reservation.java, ReservationStatus.java
│   ├── Message.java
│   ├── RatePlanType.java (factory)
│   └── rateplan/
│       ├── RatePlan.java (interface)
│       ├── AbstractRatePlan.java
│       ├── StandardRatePlan.java
│       ├── NonRefundableRatePlan.java
│       ├── CorporateRatePlan.java
│       ├── WeekendPackageRatePlan.java
│       └── LongStayRatePlan.java
├── repository/
│   ├── Database.java
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── service/
│   ├── ReservationService.java
│   ├── MessageService.java
│   ├── ReportService.java
│   └── [exceções]
└── api/
    └── ApiController.java
```

## Como rodar

### Pré-requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.8+ (para compilar)

### Configuração

O sistema lê três variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070  # opcional, padrão: 7070
```

### Executar

```bash
# Com Maven
mvn clean package -DskipTests
java -jar target/mare-alta-1.0.0.jar

# Ou diretamente com Maven
mvn compile exec:java -Dexec.mainClass=com.pousada.Main
```

O sistema cria as tabelas automaticamente no banco.

### Testes

```bash
# Requer Docker rodando para Testcontainers
mvn verify
```

## API REST

### Quartos

**GET /rooms** - Lista os 5 quartos  
Resposta: `[{ number, type, capacity, nightlyRate }, ...]`

### Reservas

**POST /reservations** - Cria reserva  
Body: `{ roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, companyCode? }`  
Resposta 201: reserva completa

**GET /reservations/{id}** - Consulta reserva

**POST /reservations/{id}/confirmation** - Confirma e recebe pagamento  
**POST /reservations/{id}/cancellation** - Cancela e calcula reembolso  
**POST /reservations/{id}/check-in** - Faz check-in  
**POST /reservations/{id}/check-out** - Faz check-out e retorna comprovante

**GET /reservations/{id}/document** - Segunda via do comprovante (somente para reservas pagas)

### Mensagens

**GET /messages** - Lista e-mails enviados  
Resposta: `[{ id, channel, to, subject, body }, ...]`

### Relatórios

**GET /reports/occupancy?date=2027-07-10** - Ocupação em uma noite (CSV)  
**GET /reports/revenue?month=2027-07** - Receita de um mês (CSV)

### Códigos de erro

- `400 INVALID_REQUEST`: dados inválidos ou regra do plano violada
- `404 ROOM_NOT_FOUND`: quarto não existe
- `404 RESERVATION_NOT_FOUND`: reserva não existe
- `409 ROOM_UNAVAILABLE`: quarto já reservado nas datas
- `409 OPERATION_NOT_ALLOWED`: operação inválida para o estado da reserva
- `422 CAPACITY_EXCEEDED`: hóspedes excedem capacidade do quarto

## Exemplos de cálculos

### Convênio Tecnosul (12% desconto)
Deluxe 10/03 a 12/03 (fora de temporada):
- Diárias: R$ 779,80 (2 × R$ 389,90)
- Desconto: R$ 93,58
- Total: R$ 686,22

### Pacote de fim de semana
Deluxe sexta 12/03 a domingo 14/03:
- Preço fixo: R$ 820,00
- Cancelamento: reembolso de R$ 410,00 (50%)

### Estadia longa (7+ noites)
Standard 01/03 a 08/03:
- Diárias: R$ 1.749,30
- Desconto 10%: R$ 174,93
- Total: R$ 1.574,37

## Adicionando um novo plano

1. Criar classe em `domain/rateplan/` implementando `RatePlan`
2. Adicionar enum em `RatePlanType`
3. Pronto! O resto do código não precisa mudar.

## Licença

Uso interno - Pousada Maré Alta
