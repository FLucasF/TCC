# Arquitetura do Sistema

## Estrutura de Diretórios

```
pousada-mare-alta/
├── src/
│   ├── main/
│   │   ├── java/com/pousadamareal/
│   │   │   ├── Main.java                    # Inicialização da aplicação
│   │   │   ├── config/
│   │   │   │   └── DatabaseConfig.java      # Configuração e inicialização do BD
│   │   │   ├── model/
│   │   │   │   ├── Room.java               # Quarto
│   │   │   │   ├── Reservation.java        # Reserva
│   │   │   │   ├── Message.java            # Mensagem
│   │   │   │   ├── Document.java           # Documento (comprovante/fatura/receipt)
│   │   │   │   ├── RatePlan.java           # Enum: STANDARD, NON_REFUNDABLE
│   │   │   │   └── ReservationStatus.java  # Enum: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
│   │   │   ├── dao/
│   │   │   │   ├── RoomDAO.java            # Acesso a dados de quartos
│   │   │   │   ├── ReservationDAO.java     # Acesso a dados de reservas
│   │   │   │   └── MessageDAO.java         # Acesso a dados de mensagens
│   │   │   ├── handlers/
│   │   │   │   ├── RoomHandler.java        # Endpoints de quartos
│   │   │   │   ├── ReservationHandler.java # Endpoints de reservas
│   │   │   │   ├── MessageHandler.java     # Endpoints de mensagens
│   │   │   │   └── ReportHandler.java      # Endpoints de relatórios
│   │   │   └── util/
│   │   │       ├── PriceCalculator.java    # Cálculos de preço
│   │   │       └── DocumentFormatter.java  # Formatação de documentos
│   │   └── resources/
│   │       ├── schema.sql                  # Schema do banco de dados
│   │       └── logback.xml                 # Configuração de logs
│   └── test/
│       └── java/com/pousadamareal/
│           └── BasicTest.java              # Teste básico
├── pom.xml                                 # Configuração Maven
├── Dockerfile                              # Build da imagem Docker
├── docker-compose.yml                      # Orquestração Docker
├── README.md                               # Guia de início rápido
├── EXAMPLES.md                             # Exemplos de requisições
├── TESTING.md                              # Guia de testes
├── ARCHITECTURE.md                         # Este arquivo
└── .gitignore                              # Arquivos ignorados pelo Git
```

## Fluxo de Dados

### Criação de Reserva
```
POST /reservations
    ↓
ReservationHandler.createReservation()
    ↓
Validações:
  - Campos obrigatórios
  - Tipo de identificação (1 de: CPF, CNPJ, Passaporte)
  - Número de hóspedes (min 1)
  - Datas válidas (formato yyyy-MM-dd)
  - Quarto existe
  - Capacidade do quarto
  - Disponibilidade para as datas
    ↓
PriceCalculator:
  - Detecta alta temporada
  - Calcula preço da noite
  - Calcula subtotal
  - Calcula desconto (se NON_REFUNDABLE)
  - Calcula total
  - Usa banker's rounding (HALF_EVEN)
    ↓
ReservationDAO.createReservation()
    ↓
INSERT INTO reservations (status=PENDING, amount_paid=0)
    ↓
Resposta com Reservation
```

### Confirmação de Reserva
```
POST /reservations/{id}/confirmation
    ↓
ReservationHandler.confirmReservation()
    ↓
Validações:
  - Reserva existe
  - Status é PENDING
    ↓
ReservationDAO.updateReservationStatus()
    ↓
UPDATE reservations SET status=CONFIRMED, amount_paid=total
    ↓
DocumentFormatter:
  - Detecta tipo de documento (CPF/CNPJ/Passaporte)
  - Gera texto do comprovante/fatura/booking receipt
    ↓
MessageDAO.saveMessage()
    ↓
INSERT INTO messages (channel=EMAIL)
    ↓
Resposta com Reservation
```

### Cancelamento de Reserva
```
POST /reservations/{id}/cancellation
    ↓
ReservationHandler.cancelReservation()
    ↓
Validações:
  - Reserva existe
  - Status é PENDING ou CONFIRMED
    ↓
Cálculo de reembolso:
  - Se PENDING: refund = null
  - Se CONFIRMED + STANDARD: refund = amount_paid
  - Se CONFIRMED + NON_REFUNDABLE: refund = null
    ↓
ReservationDAO.updateReservationStatus()
    ↓
UPDATE reservations SET status=CANCELLED, refund=?
    ↓
Resposta com Reservation
```

### Check-in
```
POST /reservations/{id}/check-in
    ↓
ReservationHandler.checkIn()
    ↓
Validações:
  - Reserva existe
  - Status é CONFIRMED
    ↓
ReservationDAO.updateReservationStatus()
    ↓
UPDATE reservations SET status=CHECKED_IN
    ↓
Resposta com Reservation
```

### Check-out
```
POST /reservations/{id}/check-out
    ↓
ReservationHandler.checkOut()
    ↓
Validações:
  - Reserva existe
  - Status é CHECKED_IN
    ↓
ReservationDAO.updateReservationStatus()
    ↓
UPDATE reservations SET status=CHECKED_OUT
    ↓
DocumentFormatter (geração do comprovante)
    ↓
Resposta com Document
```

## Banco de Dados

### Tabelas

#### rooms
```
- number (INT, PRIMARY KEY)
- type (VARCHAR)
- capacity (INT)
- nightly_rate (NUMERIC(10,2))
```

#### reservations
```
- id (SERIAL, PRIMARY KEY)
- room_number (INT, FOREIGN KEY)
- guest_name (VARCHAR)
- guest_email (VARCHAR)
- guest_cpf (VARCHAR, nullable)
- company_cnpj (VARCHAR, nullable)
- company_name (VARCHAR, nullable)
- guest_passport (VARCHAR, nullable)
- guest_country (VARCHAR, nullable)
- check_in (DATE)
- check_out (DATE)
- guests (INT)
- rate_plan (VARCHAR)
- nights (INT)
- subtotal (NUMERIC(10,2))
- discount (NUMERIC(10,2))
- total (NUMERIC(10,2))
- status (VARCHAR)
- amount_paid (NUMERIC(10,2))
- refund (NUMERIC(10,2), nullable)
- created_at (TIMESTAMP)
```

#### messages
```
- id (SERIAL, PRIMARY KEY)
- channel (VARCHAR)
- recipient_email (VARCHAR)
- subject (VARCHAR)
- body (TEXT)
- created_at (TIMESTAMP)
```

## Camadas de Aplicação

### 1. Handler (API Layer)
Responsável por:
- Receber requisições HTTP
- Validar entrada
- Chamar lógica de negócio
- Retornar respostas JSON ou CSV

Arquivos:
- RoomHandler.java
- ReservationHandler.java
- MessageHandler.java
- ReportHandler.java

### 2. DAO (Data Access Layer)
Responsável por:
- Executar queries SQL
- Mapear resultados para objetos Java

Arquivos:
- RoomDAO.java
- ReservationDAO.java
- MessageDAO.java

### 3. Util (Business Logic Layer)
Responsável por:
- Cálculos de preço
- Detecção de alta temporada
- Arredondamento correto
- Formatação de documentos
- Tradução de datas e moedas

Arquivos:
- PriceCalculator.java
- DocumentFormatter.java

### 4. Model (Domain Layer)
Responsável por:
- Representar entidades do domínio
- Enums para valores fixos

Arquivos:
- Room.java
- Reservation.java
- Message.java
- Document.java
- RatePlan.java
- ReservationStatus.java

## Cálculo de Preços

### Detecção de Alta Temporada
```java
if (mês == julho) return true;
if (mês == dezembro && dia >= 15) return true;
if (mês == janeiro) return true;
if (mês == fevereiro) return true;
return false;
```

### Cálculo do Preço da Noite
```
preçoBase = room.nightlyRate
if (isHighSeason(checkIn)) {
    preçoNota = preçoBase × 1.20
} else {
    preçoNota = preçoBase
}
preçoNota = round(preçoNota)
```

### Cálculo do Total
```
noites = checkOut - checkIn
subtotal = noites × preçoNota
subtotal = round(subtotal)

if (ratePlan == NON_REFUNDABLE) {
    desconto = round(subtotal × 0.15)
} else {
    desconto = 0
}

total = round(subtotal - desconto)
```

### Arredondamento
Usa `BigDecimal.setScale(2, RoundingMode.HALF_EVEN)`:
- HALF_EVEN é "banker's rounding"
- .5 arredonda para o número par mais próximo
- Ex: 1.5 → 2, 2.5 → 2, 3.5 → 4

## Documentos

### Comprovante (CPF)
- Português
- Mostra CPF do hóspede
- Formato de datas: DD/MM/YYYY
- Formato de moeda: R$ X.XXX,XX

### Fatura (CNPJ)
- Português
- Mostra CNPJ e razão social da empresa
- ISS retido (5%)
- Formato de datas: DD/MM/YYYY
- Formato de moeda: R$ X.XXX,XX

### Booking Receipt (Passaporte)
- Inglês
- Mostra número do passaporte e país
- Formato de datas: YYYY-MM-DD
- Formato de moeda: BRL X,XXX.XX

## Fluxo de Estado da Reserva

```
PENDING
   ↓
   ├→ confirmation → CONFIRMED
   │                    ↓
   │                    ├→ check-in → CHECKED_IN
   │                    │                ↓
   │                    │                └→ check-out → CHECKED_OUT
   │                    │
   │                    └→ cancellation → CANCELLED
   │
   └→ cancellation → CANCELLED
```

## Relatórios

### Ocupação
- Data como parâmetro
- Mostra quartos ocupados naquela noite
- Incluem reservas CONFIRMED, CHECKED_IN, CHECKED_OUT
- Formato CSV

### Financeiro
- Mês como parâmetro
- Mostra reservas que começam naquele mês com dinheiro pago
- Incluem CONFIRMED, CHECKED_IN, CHECKED_OUT
- Incluem CANCELLED se NON_REFUNDABLE (pois não há reembolso)
- Valor = amount_paid - refund (ou 0 se ambos null)
- Formato CSV
