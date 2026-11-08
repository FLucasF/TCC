# Pousada Maré Alta - Sistema de Reservas

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Requisitos

- Java 21+
- Maven 3.8+
- PostgreSQL 18+

## Compilação

```bash
mvn clean package
```

O comando gera um JAR executável em `target/reservas-1.0.0.jar`.

## Execução

### Configuração via variáveis de ambiente

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070

java -jar target/reservas-1.0.0.jar
```

### Valores padrão

Se as variáveis não forem definidas, o sistema usa:
- **DB_URL**: `jdbc:postgresql://localhost:5432/pousada`
- **DB_USER**: `postgres`
- **DB_PASSWORD**: `postgres`
- **PORT**: `7070`

## Estrutura da Pousada

### Quartos

| Número | Tipo     | Capacidade | Diária Base |
|--------|----------|------------|-------------|
| 101    | STANDARD | 2 pessoas  | R$ 249,90   |
| 102    | STANDARD | 2 pessoas  | R$ 249,90   |
| 201    | DELUXE   | 3 pessoas  | R$ 389,90   |
| 202    | DELUXE   | 3 pessoas  | R$ 389,90   |
| 301    | SUITE    | 4 pessoas  | R$ 579,90   |

### Alta Temporada

Acréscimo de 20% nas diárias durante:
- 15 de dezembro a fim de fevereiro
- Mês de julho completo

## Planos de Tarifa

### STANDARD (Normal)
- Sem desconto
- Reembolso total em caso de cancelamento

### NON_REFUNDABLE (Não Reembolsável)
- 15% de desconto sobre as diárias
- Sem reembolso em caso de cancelamento

### CORPORATE (Convênio Empresarial)
- Desconto por empresa:
  - **TECNOSUL**: 12%
  - **AGROVALE**: 18%
  - **FARMAVIDA**: 25%
- Requer código da empresa (`companyCode`)
- Reembolso total em caso de cancelamento

### WEEKEND_PACKAGE (Pacote de Fim de Semana)
- Entrada na sexta, saída no domingo (2 noites)
- Preço fixo independente de temporada:
  - STANDARD: R$ 520,00
  - DELUXE: R$ 820,00
  - SUITE: R$ 1.200,00
- Reembolso de 50% em caso de cancelamento

### LONG_STAY (Estadia Longa)
- Mínimo de 7 noites
- 10% de desconto sobre as diárias
- Alta temporada aplicada nas noites correspondentes
- Reembolso total em caso de cancelamento

## API Endpoints

### Quartos

**GET /rooms**
- Lista todos os quartos disponíveis

### Reservas

**POST /reservations**
- Cria uma nova reserva (status: PENDING)
- Body:
```json
{
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "companyCode": "TECNOSUL"
}
```

**GET /reservations/{id}**
- Consulta uma reserva

**POST /reservations/{id}/confirmation**
- Confirma uma reserva pendente (status: CONFIRMED)
- Envia e-mail de confirmação

**POST /reservations/{id}/cancellation**
- Cancela uma reserva (status: CANCELLED)
- Calcula reembolso conforme plano de tarifa

**POST /reservations/{id}/check-in**
- Faz check-in (status: CHECKED_IN)

**POST /reservations/{id}/check-out**
- Faz check-out (status: CHECKED_OUT)
- Retorna comprovante

**GET /reservations/{id}/document**
- Segunda via do comprovante (apenas para reservas pagas)

### Mensagens

**GET /messages**
- Lista todos os e-mails enviados

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
- Relatório de ocupação de uma data específica (CSV)

**GET /reports/revenue?month=2027-07**
- Relatório financeiro de um mês (CSV)

## Códigos de Erro

| Status | Código                  | Descrição                                    |
|--------|-------------------------|----------------------------------------------|
| 400    | INVALID_REQUEST         | Dados inválidos ou regras do plano violadas  |
| 404    | ROOM_NOT_FOUND          | Quarto não encontrado                        |
| 404    | RESERVATION_NOT_FOUND   | Reserva não encontrada                       |
| 409    | ROOM_UNAVAILABLE        | Quarto já reservado nas datas solicitadas    |
| 409    | OPERATION_NOT_ALLOWED   | Operação não permitida no status atual       |
| 422    | CAPACITY_EXCEEDED       | Mais hóspedes que a capacidade do quarto     |

## Testes

```bash
# Testes unitários
mvn test

# Build completo com verificação
mvn verify
```

## Arquitetura

```
src/main/java/com/pousadamarealta/
├── Application.java              # Classe principal
├── controller/                   # Controllers REST
│   ├── ReservationController.java
│   ├── RoomController.java
│   ├── MessageController.java
│   └── ReportController.java
├── dto/                          # Data Transfer Objects
├── exception/                    # Exceções customizadas
├── model/                        # Modelos de domínio
│   ├── Reservation.java
│   ├── Room.java
│   ├── Message.java
│   └── enums/
├── repository/                   # Camada de persistência
│   ├── Database.java
│   ├── ReservationRepository.java
│   ├── RoomRepository.java
│   └── MessageRepository.java
└── service/                      # Lógica de negócio
    ├── ReservationService.java
    ├── PricingService.java
    ├── NotificationService.java
    ├── ReportService.java
    └── rateplan/                 # Estratégias de planos de tarifa
        ├── RatePlanStrategy.java
        ├── StandardRatePlan.java
        ├── NonRefundableRatePlan.java
        ├── CorporateRatePlan.java
        ├── WeekendPackageRatePlan.java
        └── LongStayRatePlan.java
```

## Banco de Dados

O sistema cria automaticamente as tabelas necessárias na inicialização:

- **reservations**: Armazena todas as reservas
- **messages**: Armazena e-mails enviados

## Notas Importantes

- Todos os valores monetários usam arredondamento "meio para o par" (HALF_EVEN)
- Datas no formato ISO 8601 (yyyy-MM-dd)
- E-mails são armazenados no banco (não enviados de verdade)
- Um quarto não pode ter reservas conflitantes (apenas reservas não canceladas)
- CPF deve conter 11 dígitos numéricos
