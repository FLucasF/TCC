# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, construído com Java 21 e Javalin 6.7.0.

## Características

- **Gerenciamento de Quartos**: 5 quartos com tipos (STANDARD, DELUXE, SUITE) e capacidades diferentes
- **Reservas Flexíveis**: Suporte a CPF, CNPJ e Passaporte de estrangeiros
- **Cálculo de Preços Inteligente**: 
  - Tarifa alta na temporada (15/12 - 28/02 e julho)
  - Planos de tarifa: STANDARD ou NÃO_REEMBOLSÁVEL (15% desconto)
  - Arredondamento "meio para par" em todos os cálculos
- **Documentos Personalizados**:
  - Comprovante (CPF) em português
  - Fatura (CNPJ) com ISS retido em português
  - Booking Receipt (Passaporte) em inglês
- **Fluxo de Reserva**: PENDENTE → CONFIRMADA → CHECK-IN → CHECK-OUT ou CANCELADA
- **Confirmação por Email**: Emails rastreados (apenas em memória)
- **Relatórios em CSV**: 
  - Ocupação por data
  - Receita por mês
- **API RESTful**: Endpoints completos para todas as operações

## Tecnologia

- **Java 21** com resources de módulos selados
- **Javalin 6.7.0** como framework web leve
- **Jackson** para serialização JSON
- **Maven** para build e empacotamento
- **Armazenamento em memória** (reinicialização limpa o sistema)

## Construção e Execução

### Build
```bash
mvn clean package
```

Gera `/workspace/target/marea-alta.jar` com todas as dependências incluídas.

### Execução
```bash
java -jar target/marea-alta.jar
```

Ou com porta customizada:
```bash
PORT=8080 java -jar target/marea-alta.jar
```

Padrão: porta 7070

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out (retorna documento)
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Listar emails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação de um dia (CSV)
- `GET /reports/revenue?month=2027-07` - Receita de um mês (CSV)

## Quartos Disponíveis

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101, 102 | STANDARD | 2 | R$ 249,90 |
| 201, 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

*Em alta temporada: +20%*

## Exemplo de Uso

### 1. Criar Reserva com CPF
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### 2. Confirmar Reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}'
```

### 3. Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 4. Check-out (retorna comprovante)
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Identificação do Hóspede

Escolha exatamente uma das três opções:

### CPF (Pessoa Física)
```json
{
  "guestCpf": "111.222.333-44"
}
```
→ Gera: Comprovante de reserva

### CNPJ (Pessoa Jurídica)
```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda"
}
```
→ Gera: Fatura com ISS retido (5%)

### Passaporte (Estrangeiro)
```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom"
}
```
→ Gera: Booking Receipt (em inglês)

## Fluxo de Estados da Reserva

```
PENDING (criada)
   ↓
   ├─→ CONFIRMED (pagamento recebido)
   │      ↓
   │      ├─→ CHECKED_IN (hóspede entrou)
   │      │      ↓
   │      │      └─→ CHECKED_OUT (hóspede saiu)
   │      └─→ CANCELLED (cancelada)
   │
   └─→ CANCELLED (cancelada sem pagar)
```

## Valores de Retorno Monetários

- JSON: números com 2 casas decimais (ex: 599.76)
- CSV: números com ponto decimal (ex: 599.76)
- Documentos: formatados com vírgula (ex: R$ 599,76)

## Arredondamento

Todos os cálculos usam arredondamento "meio para par" (RoundingMode.HALF_EVEN):
- Tarifa por noite
- Subtotal
- Desconto
- Total

Exemplo: 389.90 * 1.20 = 467.88

## Tratamento de Erros

| Código HTTP | Erro | Situação |
|-------------|------|----------|
| 400 | INVALID_REQUEST | Campo obrigatório faltando, formato inválido |
| 404 | ROOM_NOT_FOUND | Quarto não existe |
| 404 | RESERVATION_NOT_FOUND | Reserva não existe |
| 409 | ROOM_UNAVAILABLE | Quarto já reservado para as datas |
| 409 | OPERATION_NOT_ALLOWED | Operação não permitida no estado atual |
| 422 | CAPACITY_EXCEEDED | Mais hóspedes que a capacidade do quarto |

## Estrutura do Projeto

```
/workspace/
├── pom.xml
├── src/main/java/com/pousada/
│   ├── Application.java
│   ├── api/
│   │   ├── ApiHandler.java
│   │   └── dto/
│   │       ├── CreateReservationRequest.java
│   │       ├── ReservationResponse.java
│   │       ├── RoomResponse.java
│   │       ├── DocumentResponse.java
│   │       ├── MessageResponse.java
│   │       ├── ConfirmationRequest.java
│   │       └── ErrorResponse.java
│   ├── domain/
│   │   ├── Room.java
│   │   ├── Reservation.java
│   │   ├── GuestIdentification.java
│   │   ├── CpfIdentification.java
│   │   ├── CnpjIdentification.java
│   │   ├── PassportIdentification.java
│   │   ├── Document.java
│   │   └── Message.java
│   ├── repository/
│   │   └── InMemoryRepository.java
│   └── service/
│       ├── ReservationService.java
│       ├── PricingService.java
│       ├── DocumentGenerator.java
│       └── ReportService.java
└── target/
    └── marea-alta.jar
```

## Notas de Implementação

- Dados armazenados apenas em memória; reinicialização limpa tudo
- IDs de reserva começam em 1 e incrementam
- Emails são rastreados mas não são realmente enviados
- Relatórios CSV incluem linha de total
- Documentos gerados dinamicamente sem templates
- Sem banco de dados, sem persistência
