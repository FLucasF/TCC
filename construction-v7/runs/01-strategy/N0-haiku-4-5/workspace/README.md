# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para uma pousada com 5 quartos, implementado em Java 21 com Javalin 6.7.0.

## Compilação

```bash
mvn clean package
```

Isso vai gerar um JAR executável em `target/mare-alta-1.0.0.jar`.

## Execução

```bash
java -jar target/mare-alta-1.0.0.jar
```

A aplicação iniciará na porta **7070** por padrão, ou em uma porta customizada via variável de ambiente:

```bash
PORT=8080 java -jar target/mare-alta-1.0.0.jar
```

## Verificação

```bash
mvn verify
```

Roda todos os testes unitários.

## Quartos

A pousada tem 5 quartos:

- **101, 102**: STANDARD - 2 pessoas - R$ 249,90/noite
- **201, 202**: DELUXE - 3 pessoas - R$ 389,90/noite
- **301**: SUITE - 4 pessoas - R$ 579,90/noite

Alta temporada (20% acréscimo): 15/12 a 28/02 e julho inteiro

## Planos de Tarifa

### STANDARD (Normal)
- Sem desconto
- 100% reembolso se cancelado antes do check-in

### NON_REFUNDABLE
- 15% desconto sobre as noites
- Sem reembolso se cancelado

### CORPORATE (Convênio com empresas)
Requer `companyCode` (maiúsculas):
- **TECNOSUL**: 12% desconto
- **AGROVALE**: 18% desconto
- **FARMAVIDA**: 25% desconto
- 100% reembolso se cancelado antes do check-in

### WEEKEND_PACKAGE
Sexta a domingo (exatamente 2 noites), preço fechado:
- Standard: R$ 520,00
- Deluxe: R$ 820,00
- Suite: R$ 1.200,00
- 50% reembolso se cancelado

### LONG_STAY
7+ noites:
- 10% desconto sobre as diárias (alta temporada continua valendo)
- 100% reembolso se cancelado

## Estados da Reserva

1. **PENDING**: Criada, aguardando confirmação
2. **CONFIRMED**: Paga
3. **CHECKED_IN**: Hóspede entrou
4. **CHECKED_OUT**: Hóspede saiu
5. **CANCELLED**: Cancelada

Transições válidas:
- PENDING → CONFIRMED
- CONFIRMED → CHECKED_IN
- CHECKED_IN → CHECKED_OUT
- PENDING/CONFIRMED → CANCELLED

## Endpoints

### Quartos
```
GET /rooms
```

### Reservas
```
POST   /reservations
GET    /reservations/{id}
POST   /reservations/{id}/confirmation
POST   /reservations/{id}/check-in
POST   /reservations/{id}/check-out
POST   /reservations/{id}/cancellation
GET    /reservations/{id}/document (segunda via do comprovante)
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

### Criar reserva STANDARD
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

### Criar reserva CORPORATE
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@tecnosul.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

### Criar reserva WEEKEND_PACKAGE
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

### Criar reserva LONG_STAY
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }'
```

### Confirmar reserva
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

### Cancelar
```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

### Obter segunda via do comprovante
```bash
curl http://localhost:7070/reservations/1/document
```

### Verificar mensagens enviadas
```bash
curl http://localhost:7070/messages
```

### Relatório de ocupação para uma noite
```bash
curl 'http://localhost:7070/reports/occupancy?date=2027-03-12'
```

### Relatório financeiro para um mês
```bash
curl 'http://localhost:7070/reports/revenue?month=2027-03'
```

## Codificação de Dinheiro

- Arredondamento "meio para o par" (HALF_EVEN) em todas as operações
- No JSON: com 2 casas decimais (ex: 499.80)
- No CSV: com 2 casas decimais, ponto como separador (ex: 499.80)

## Codificação de Datas

- No JSON e requisições: `yyyy-MM-dd` (ex: 2027-03-10)
- Em relatórios CSV: `yyyy-MM-dd`
- Em comprovantes/e-mails: `dd/MM/yyyy` (ex: 10/03/2027)

## Formato de CPF

- Entrada: 11 dígitos contínuos (ex: 12345678900)
- Saída em comprovantes: formatado (ex: 123.456.789-00)

## Persistência de Dados

Os dados são mantidos em memória durante a execução. Se a aplicação reiniciar, todas as reservas são perdidas.

## Estrutura do Projeto

```
src/main/java/com/pousada/
├── Application.java                 # Classe principal com rotas Javalin
├── domain/                          # Modelos de domínio
│   ├── Room.java
│   ├── Reservation.java
│   ├── ReservationStatus.java
│   ├── RatePlan.java
│   ├── Message.java
│   ├── MessageChannel.java
│   └── ReservationDocument.java
├── service/                         # Lógica de negócio
│   ├── RoomService.java
│   ├── ReservationService.java
│   ├── MessageService.java
│   ├── ReportService.java
│   └── pricing/                     # Estratégias de preço
│       ├── PricingStrategy.java     # Interface
│       ├── PricingStrategyFactory.java
│       ├── StandardPricingStrategy.java
│       ├── NonRefundablePricingStrategy.java
│       ├── CorporatePricingStrategy.java
│       ├── WeekendPackagePricingStrategy.java
│       └── LongStayPricingStrategy.java
├── api/                             # Controllers
│   ├── RoomController.java
│   ├── ReservationController.java
│   ├── MessageController.java
│   └── ReportController.java
├── util/                            # Utilitários
│   ├── MoneyRounder.java
│   └── DateHelper.java
└── exception/
    └── ApiException.java
```

## Testes

Os testes incluem validações de:
- Cálculo de preços em alta temporada
- Arredondamento de moeda (HALF_EVEN)
- Descontos por plano
- Validações de datas e regras de negócio

Execute com:
```bash
mvn test
```
