# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Compilação e Execução

### Build

```bash
mvn clean package
```

Isto gera o arquivo `target/reservas.jar`.

### Executar o servidor

```bash
java -jar target/reservas.jar
```

O servidor precisa das seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão PostgreSQL (ex: `jdbc:postgresql://localhost:5432/posada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta (padrão: 7070)

Exemplo:

```bash
DB_URL="jdbc:postgresql://localhost:5432/posada" \
DB_USER="postgres" \
DB_PASSWORD="password" \
PORT=8080 \
java -jar target/reservas.jar
```

## Testes

```bash
mvn test
```

Ou para executar o ciclo completo de verificação:

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva (com pagamento)
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém o comprovante/fatura/booking receipt

### Mensagens e Relatórios

- `GET /messages` - Lista todas as mensagens enviadas
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Estrutura do Projeto

```
src/main/java/com/posadamareaita/
├── Main.java                 # Ponto de entrada
├── api/                      # Endpoints REST
│   ├── Api.java
│   └── ErrorResponse.java
├── domain/                   # Modelos de domínio
│   ├── Message.java
│   ├── RatePlan.java
│   ├── Reservation.java
│   ├── ReservationStatus.java
│   ├── Room.java
│   └── RoomType.java
├── database/                 # Inicialização do BD
│   └── Database.java
├── repository/               # Acesso a dados
│   ├── MessageRepository.java
│   ├── ReservationRepository.java
│   └── RoomRepository.java
├── service/                  # Lógica de negócio
│   ├── DocumentService.java
│   ├── MessageService.java
│   ├── ReportService.java
│   └── ReservationService.java
└── util/                     # Utilitários
    ├── FormattingUtil.java
    ├── MoneyUtil.java
    ├── PriceCalculator.java
    └── SeasonUtil.java
```

## Características

### Quartos

A pousada tem 5 quartos:
- 101, 102: STANDARD (2 pessoas) - R$ 249,90/noite
- 201, 202: DELUXE (3 pessoas) - R$ 389,90/noite
- 301: SUITE (4 pessoas) - R$ 579,90/noite

### Alta Temporada

- 15 de dezembro até 28 de fevereiro: +20% no preço
- Julho inteiro: +20% no preço

### Planos de Tarifa

- **STANDARD**: preço normal, reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

### Identificação do Hóspede

Ao criar uma reserva, fornecer exatamente um dos seguintes:
- `guestCpf`: CPF do hóspede (recebe comprovante em português)
- `companyCnpj` + `companyName`: CNPJ da empresa (recebe fatura)
- `guestPassport` + `guestCountry`: Passaporte (recebe booking receipt em inglês)

### Arredondamento de Valores

Todos os valores monetários são arredondados usando a regra "meia para o par" (HALF_EVEN), aplicada em cada passo:
1. Preço por noite (com ajuste de temporada)
2. Subtotal (soma das noites)
3. Desconto (se aplicável)
4. Total (subtotal - desconto)

## Exemplo de Uso

### Criar uma reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD",
    "guestCpf": "123.456.789-00"
  }'
```

### Confirmar uma reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}'
```

### Fazer check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Fazer check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Obter relatório de ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

### Obter relatório financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-07
```
