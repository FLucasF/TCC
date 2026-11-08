# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, construído com Java 21, Javalin e PostgreSQL.

## Funcionalidades

### Gerenciamento de Quartos
- 5 quartos com diferentes tipos e tarifas
- STANDARD (101, 102): até 2 pessoas, R$ 249,90/noite
- DELUXE (201, 202): até 3 pessoas, R$ 389,90/noite
- SUITE (301): até 4 pessoas, R$ 579,90/noite

### Sistema de Reservas
- Criação de reservas pendentes
- Confirmação com envio de e-mail
- Check-in e check-out
- Cancelamento com reembolso conforme plano

### Planos de Tarifa
- **STANDARD**: Sem desconto
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em cancelamento

### Cálculo de Preços
- Detecção automática de alta temporada (15/12-29/02, mês de julho)
- Aumento de 20% na alta temporada
- Arredondamento "meio para o par" em cada etapa

### Relatórios
1. **Ocupação**: Quartos ocupados em uma data
2. **Financeiro**: Receita mensal por reserva
3. **Taxa de Turismo**: R$ 4,50 por hóspede/noite
4. **Análise de Planos**: Comparação STANDARD vs NON_REFUNDABLE

### Comprovantes
- Gerado automaticamente na confirmação
- Segunda via disponível a qualquer momento (reserva paga)
- Formatação padrão com número, dados do hóspede, valores

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.8+

## Variáveis de Ambiente

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="usuario"
export DB_PASSWORD="senha"
export PORT=7070  # Opcional, padrão é 7070
```

## Build e Execução

```bash
# Build
mvn clean package

# Executar
java -jar target/reserva-system.jar
```

O sistema irá:
1. Conectar ao PostgreSQL
2. Criar as tabelas necessárias
3. Popular os 5 quartos
4. Iniciar na porta configurada (ou 7070)

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva (status PENDING)
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar (status CONFIRMED, envia e-mail)
- `POST /reservations/{id}/cancellation` - Cancelar (reembolso conforme plano)
- `POST /reservations/{id}/check-in` - Check-in (status CHECKED_IN)
- `POST /reservations/{id}/check-out` - Check-out (status CHECKED_OUT, retorna comprovante)
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Lista e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação em uma data (CSV)
- `GET /reports/revenue?month=2027-07` - Receita mensal (CSV)
- `GET /reports/tourism-tax?month=2027-07` - Taxa de turismo (CSV)
- `GET /reports/rate-plans?month=2027-07` - Análise de planos (CSV)

## Exemplos de Uso

### Criar uma reserva
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

### Confirmar reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### Check-out e receber comprovante
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

### Obter relatório de ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10" \
  -H "Accept: text/csv"
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── Application.java              # Entrada e setup do Javalin
├── db/
│   └── Database.java            # Inicialização do PostgreSQL
├── model/
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── Document.java
├── repository/
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── service/
│   ├── PricingService.java       # Cálculos de preço
│   ├── ReservationService.java   # Lógica de negócio
│   ├── MessageService.java       # E-mails
│   └── ReportService.java        # Geração de relatórios
├── routes/
│   ├── RoomRoutes.java
│   ├── ReservationRoutes.java
│   ├── MessageRoutes.java
│   └── ReportRoutes.java
└── util/
    ├── PricingService.java
    ├── DateFormatter.java
    ├── CurrencyFormatter.java
    └── DocumentFormatter.java
```

## Testes

```bash
mvn test
```

Os testes validam:
- Detecção de alta temporada
- Cálculo de tarifas diárias
- Aplicação de descontos
- Arredondamento "meio para o par"
- Cálculo de noites

## Regras de Negócio

### Estados da Reserva
- **PENDING**: Recém criada, pode ser confirmada ou cancelada
- **CONFIRMED**: Paga, pode fazer check-in ou cancelar
- **CHECKED_IN**: Hóspede no quarto, pode fazer check-out
- **CHECKED_OUT**: Hóspede saiu, sem mais alterações
- **CANCELLED**: Cancelada, reembolso aplicado conforme plano

### Disponibilidade de Quartos
- Um quarto não pode ter duas reservas confirmadas/checked no mesmo período
- Quem sai em uma data não conflita com quem entra naquela data

### Cancelamento
- **STANDARD**: Reembolso total do valor pago
- **NON_REFUNDABLE**: Sem reembolso, mas continua contando para receita
- **PENDING**: Nada para reembolsar

### Alta Temporada
- 15 de dezembro a fim de fevereiro
- Mês de julho inteiro
- Aumenta 20% na tarifa diária

## Arredondamento Monetário

Usa "meio para o par" (RoundingMode.HALF_EVEN) em todas as operações:
- Tarifa da noite
- Subtotal de noites
- Desconto
- Total final

Exemplo: 249.90 × 1.20 = 299.88 (não 299.87)
