# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Características

- **5 quartos** com diferentes tipos e capacidades
- **5 planos de tarifa** com regras específicas de preço e reembolso
- **Alta temporada** (15/dez a fim/fev e julho inteiro) com 20% de acréscimo
- **Sistema de status** de reserva (pendente → confirmada → check-in → check-out)
- **E-mails** de confirmação (armazenados em memória)
- **Relatórios** CSV de ocupação e financeiro

## Quartos

| Número | Tipo     | Capacidade | Diária Base |
|--------|----------|------------|-------------|
| 101    | STANDARD | 2 pessoas  | R$ 249,90   |
| 102    | STANDARD | 2 pessoas  | R$ 249,90   |
| 201    | DELUXE   | 3 pessoas  | R$ 389,90   |
| 202    | DELUXE   | 3 pessoas  | R$ 389,90   |
| 301    | SUITE    | 4 pessoas  | R$ 579,90   |

## Planos de Tarifa

### 1. STANDARD (Normal)
- Sem desconto
- Reembolso total se cancelar

### 2. NON_REFUNDABLE (Não Reembolsável)
- 15% de desconto sobre as diárias
- Sem reembolso se cancelar

### 3. CORPORATE (Convênio Empresarial)
- Desconto por empresa:
  - **TECNOSUL**: 12%
  - **AGROVALE**: 18%
  - **FARMAVIDA**: 25%
- Reembolso total se cancelar
- Requer campo `companyCode` na criação

### 4. WEEKEND_PACKAGE (Pacote de Fim de Semana)
- Entrada na sexta, saída no domingo (exatamente 2 noites)
- Preço fechado (ignora temporada):
  - STANDARD: R$ 520,00
  - DELUXE: R$ 820,00
  - SUITE: R$ 1.200,00
- Reembolso de 50% se cancelar

### 5. LONG_STAY (Estadia Longa)
- Mínimo 7 noites
- 10% de desconto sobre as diárias
- Alta temporada se aplica
- Reembolso total se cancelar

## Build e Execução

```bash
# Compilar e gerar o JAR
mvn package

# Executar o sistema
java -jar target/reservas-1.0.0.jar

# Especificar porta via variável de ambiente
PORT=8080 java -jar target/reservas-1.0.0.jar

# Rodar testes
mvn test

# Build completo com verificação
mvn verify
```

## API Endpoints

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
  "ratePlan": "CORPORATE",
  "companyCode": "TECNOSUL"
}
```

**GET /reservations/{id}**

**POST /reservations/{id}/confirmation** - Confirma e envia e-mail

**POST /reservations/{id}/cancellation** - Cancela e calcula reembolso

**POST /reservations/{id}/check-in**

**POST /reservations/{id}/check-out** - Retorna o comprovante

**GET /reservations/{id}/document** - Segunda via do comprovante

### Mensagens

**GET /messages** - Lista e-mails enviados

### Relatórios

**GET /reports/occupancy?date=2027-07-10** - Ocupação por noite (CSV)

**GET /reports/revenue?month=2027-07** - Receita por mês (CSV)

## Códigos de Erro

- `400 INVALID_REQUEST` - Dados inválidos ou faltando
- `404 ROOM_NOT_FOUND` - Quarto não existe
- `404 RESERVATION_NOT_FOUND` - Reserva não existe
- `409 ROOM_UNAVAILABLE` - Quarto já reservado nas datas
- `409 OPERATION_NOT_ALLOWED` - Operação não permitida para o status atual
- `422 CAPACITY_EXCEEDED` - Número de hóspedes excede capacidade

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── Main.java                          # Aplicação principal e controllers
├── dto/                               # DTOs de request/response
├── exception/                         # Exceções customizadas
├── model/                             # Entidades de domínio
└── service/
    ├── RoomService.java              # Gerenciamento de quartos
    ├── ReservationService.java       # Lógica de reservas
    ├── MessageService.java           # Envio de mensagens
    ├── ReportService.java            # Geração de relatórios
    ├── RatePlanStrategy.java         # Interface para planos de tarifa
    ├── StandardRatePlan.java         # Implementação STANDARD
    ├── NonRefundableRatePlan.java    # Implementação NON_REFUNDABLE
    ├── CorporateRatePlan.java        # Implementação CORPORATE
    ├── WeekendPackageRatePlan.java   # Implementação WEEKEND_PACKAGE
    └── LongStayRatePlan.java         # Implementação LONG_STAY
```

## Exemplos de Cálculo

### Alta Temporada (STANDARD)
- 2 noites em julho (10-12/jul/2027)
- Diária: R$ 249,90 × 1,20 = R$ 299,88
- Subtotal: R$ 599,76
- Total: R$ 599,76

### Convênio Tecnosul (CORPORATE)
- 2 noites em março (10-12/mar/2027) no DELUXE
- Diária: R$ 389,90 × 2 = R$ 779,80
- Desconto (12%): R$ 93,58
- Total: R$ 686,22

### Pacote Fim de Semana (WEEKEND_PACKAGE)
- Sexta a domingo no DELUXE
- Total: R$ 820,00 (preço fixo)
- Se cancelar: reembolso de R$ 410,00

### Estadia Longa (LONG_STAY)
- 7 noites em março (01-08/mar/2027) no STANDARD
- Diária: R$ 249,90 × 7 = R$ 1.749,30
- Desconto (10%): R$ 174,93
- Total: R$ 1.574,37

## Tecnologias

- Java 21
- Javalin 6.7.0
- Jackson 2.18.0 (JSON)
- JUnit 5 + AssertJ (testes)
- Maven
- SLF4J (logging)
