# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0.

## Características

### Quartos
- **101, 102** - STANDARD: até 2 pessoas, R$ 249,90/noite
- **201, 202** - DELUXE: até 3 pessoas, R$ 389,90/noite  
- **301** - SUITE: até 4 pessoas, R$ 579,90/noite

### Planos de Tarifa

1. **STANDARD** - Plano normal sem desconto, reembolso total no cancelamento
2. **NON_REFUNDABLE** - 15% de desconto, sem reembolso no cancelamento
3. **CORPORATE** - Convênio com empresas:
   - TECNOSUL: 12% de desconto
   - AGROVALE: 18% de desconto
   - FARMAVIDA: 25% de desconto
   - Reembolso total no cancelamento
4. **WEEKEND_PACKAGE** - Pacote de fim de semana (sexta a domingo):
   - STANDARD: R$ 520,00
   - DELUXE: R$ 820,00
   - SUITE: R$ 1.200,00
   - Reembolso de 50% no cancelamento
5. **LONG_STAY** - Estadia longa (7+ noites):
   - 10% de desconto sobre as diárias
   - Reembolso total no cancelamento

### Alta Temporada
- 15 de dezembro a fim de fevereiro
- Mês de julho inteiro
- Acréscimo de 20% na diária

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven
- JUnit 5

## Build e Execução

### Compilar e testar
```bash
mvn clean test
```

### Gerar JAR executável
```bash
mvn package
```

### Executar
```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha
export PORT=7070  # Opcional, padrão é 7070

java -jar target/reservas-1.0.0.jar
```

## API REST

### Quartos

**GET /rooms**
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

**POST /reservations**
```json
{
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "companyCode": "TECNOSUL"  // Apenas para CORPORATE
}
```

**GET /reservations/{id}**

Retorna uma reserva com todos os seus dados:
- id, roomNumber, guestName, guestEmail, guestCpf
- checkIn, checkOut, guests, ratePlan
- nights, subtotal, discount, total
- status, amountPaid, refund

**POST /reservations/{id}/confirmation**

Confirma uma reserva pendente e envia e-mail de confirmação.

**POST /reservations/{id}/cancellation**

Cancela uma reserva (antes do check-in) e calcula o reembolso conforme o plano.

**POST /reservations/{id}/check-in**

Realiza o check-in de uma reserva confirmada.

**POST /reservations/{id}/check-out**

Realiza o check-out e retorna o comprovante.

**GET /reservations/{id}/document**

Gera segunda via do comprovante (apenas para reservas pagas).

### Mensagens

**GET /messages**

Lista todos os e-mails enviados pelo sistema.

### Relatórios

**GET /reports/occupancy?date=2027-07-10**

Relatório de ocupação em CSV para uma data específica.

**GET /reports/revenue?month=2027-07**

Relatório financeiro em CSV para um mês específico.

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── Main.java                          # Aplicação principal
├── config/
│   └── Database.java                  # Configuração e schema do banco
├── domain/
│   ├── Room.java                      # Modelo de quarto
│   ├── RoomType.java                  # Tipos de quarto (enum)
│   ├── Reservation.java               # Modelo de reserva
│   ├── ReservationStatus.java         # Status da reserva (enum)
│   ├── RatePlan.java                  # Planos de tarifa (enum)
│   ├── Message.java                   # Modelo de mensagem
│   └── Document.java                  # Modelo de comprovante
├── service/
│   ├── RoomService.java               # Serviço de quartos
│   ├── ReservationService.java        # Serviço de reservas
│   ├── MessageService.java            # Serviço de mensagens
│   ├── ReportService.java             # Serviço de relatórios
│   └── rateplan/                      # Estratégias de planos
│       ├── RatePlanStrategy.java      # Interface da estratégia
│       ├── StandardRatePlan.java      # Plano padrão
│       ├── NonRefundableRatePlan.java # Não reembolsável
│       ├── CorporateRatePlan.java     # Convênio corporativo
│       ├── WeekendPackageRatePlan.java # Pacote fim de semana
│       └── LongStayRatePlan.java      # Estadia longa
├── controller/
│   ├── RoomController.java            # Controller de quartos
│   ├── ReservationController.java     # Controller de reservas
│   ├── MessageController.java         # Controller de mensagens
│   └── ReportController.java          # Controller de relatórios
└── exception/
    └── ApiException.java              # Exceção customizada da API
```

## Padrões de Design

- **Strategy Pattern**: Implementação dos diferentes planos de tarifa
- **Repository Pattern**: Camada de serviços para acesso aos dados
- **MVC**: Separação clara entre controllers, services e domain

## Regras de Negócio

- Arredondamento: HALF_EVEN (arredondamento bancário) em todas as operações monetárias
- Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento: apenas antes do check-in (PENDING ou CONFIRMED)
- Disponibilidade: quartos não podem ter reservas sobrepostas
- Capacidade: número de hóspedes não pode exceder a capacidade do quarto
- E-mails: armazenados localmente para consulta da recepção

## Exemplos de Cálculo

### Convênio Tecnosul - DELUXE (10-12/03/2027)
- Diárias: R$ 779,80
- Desconto (12%): R$ 93,58
- Total: R$ 686,22

### Pacote Fim de Semana - DELUXE (12-14/03/2027)
- Total: R$ 820,00 (preço fixo)
- Se cancelar, reembolso: R$ 410,00

### Estadia Longa - STANDARD (01-08/03/2027, 7 noites)
- Diárias: R$ 1.749,30
- Desconto (10%): R$ 174,93
- Total: R$ 1.574,37
