# Pousada Maré Alta - Sistema de Reservas

Sistema de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Características

- **5 quartos configurados**: 2 STANDARD (R$ 249,90), 2 DELUXE (R$ 389,90), 1 SUITE (R$ 579,90)
- **Sazonalidade**: 20% de aumento na alta temporada (15/12 a 29/02 e julho)
- **Planos de tarifa**: STANDARD (sem desconto) e NON_REFUNDABLE (15% desconto)
- **Documentos**: Comprovante (CPF), Fatura (CNPJ), Booking Receipt (Passaporte)
- **E-mail simulado**: Confirmações armazenadas em banco de dados
- **Relatórios**: Ocupação e Financeiro em CSV

## Requisitos

- Java 21
- Docker e Docker Compose (para rodar com PostgreSQL)
- Ou PostgreSQL 18+ instalado localmente

## Como rodar

### Com Docker Compose (recomendado)

```bash
docker-compose up --build
```

A API estará disponível em `http://localhost:7070`

### Localmente

1. Configure PostgreSQL:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT=7070
```

2. Build e execute:
```bash
mvn package
java -jar target/marealta.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirma a reserva
- `POST /reservations/{id}/cancellation` - Cancela a reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna documento)
- `GET /reservations/{id}/document` - Obtém segunda via do documento

### Mensagens
- `GET /messages` - Lista todos os e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Exemplos de Uso

### Criar uma reserva (com CPF)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 779.80
  }'
```

### Criar reserva com CNPJ (empresa)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@tecnosul.com.br",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Criar reserva com Passaporte (estrangeiro)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Obter relatório de ocupação
```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

### Obter relatório financeiro
```bash
curl http://localhost:7070/reports/revenue?month=2027-07
```

## Testes

Execute os testes com:
```bash
mvn test
```

Execute todos os testes incluindo integração:
```bash
mvn verify
```

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/
│   │   └── com/pousada/marealta/
│   │       ├── Application.java              # Classe principal (Javalin)
│   │       ├── config/
│   │       │   └── DatabaseConfig.java       # Configuração do banco
│   │       ├── model/
│   │       │   ├── Room.java
│   │       │   ├── Reservation.java
│   │       │   └── Message.java
│   │       ├── repository/
│   │       │   ├── RoomRepository.java
│   │       │   ├── ReservationRepository.java
│   │       │   └── MessageRepository.java
│   │       ├── service/
│   │       │   ├── PricingService.java       # Cálculo de preços
│   │       │   ├── ReservationService.java   # Lógica de negócio
│   │       │   ├── DocumentService.java      # Geração de documentos
│   │       │   ├── MessageService.java       # Envio de e-mails
│   │       │   └── ReportService.java        # Geração de relatórios
│   │       └── dto/
│   │           ├── ReservationResponse.java
│   │           └── DocumentResponse.java
│   └── resources/
│       └── logback.xml
└── test/
    └── java/
        └── com/pousada/marealta/
            └── ReservationIntegrationTest.java
```

## Notas Técnicas

- **Arredondamento**: Usa HALF_EVEN (banker's rounding) para centavos
- **Identificação**: Apenas uma forma por reserva (CPF OU CNPJ OU Passaporte)
- **Cancelamento**: Reembolso apenas para plano STANDARD confirmado
- **ISS**: 5% retido no total da fatura (apenas para empresas)
- **Conexão**: HikariCP para pool de conexões com PostgreSQL
