# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Compilação e Empacotamento

Para compilar o projeto:

```bash
mvn clean package
```

O JAR será gerado em `target/mare-alta-system-1.0.0.jar`

Para rodar os testes:

```bash
mvn verify
```

## Execução

### Requisitos

- PostgreSQL 18 rodando
- Java 21 instalado

### Variáveis de Ambiente

```bash
export DB_URL="jdbc:postgresql://localhost:5432/mare_alta"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT=7070  # Opcional, padrão é 7070
```

### Iniciando o Servidor

```bash
java -jar target/mare-alta-system-1.0.0.jar
```

O servidor estará disponível em `http://localhost:7070`

## Estrutura do Projeto

```
src/main/java/com/pousada/
├── Application.java           # Ponto de entrada
├── api/
│   └── ApiRoutes.java        # Definição de rotas da API
├── database/
│   ├── Database.java         # Gerenciamento de conexão
│   ├── RoomRepository.java   # Acesso aos quartos
│   ├── ReservationRepository.java  # Acesso às reservas
│   └── MessageRepository.java      # Acesso às mensagens
├── model/
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   ├── Receipt.java
│   ├── RoomType.java
│   ├── RatePlan.java
│   └── ReservationStatus.java
├── service/
│   ├── ReservationService.java     # Lógica de negócio
│   ├── ReportService.java          # Geração de relatórios
│   └── pricing/                    # Estratégias de preço
│       ├── PricingStrategy.java
│       ├── StandardPricingStrategy.java
│       ├── NonRefundablePricingStrategy.java
│       ├── CorporatePricingStrategy.java
│       ├── WeekendPackagePricingStrategy.java
│       ├── LongStayPricingStrategy.java
│       ├── PricingStrategyFactory.java
│       ├── PricingResult.java
│       └── RefundPolicy.java
└── util/
    ├── PriceCalculator.java
    └── Formatter.java
```

## API Endpoints

### Quartos

**GET /rooms** - Lista todos os quartos

Resposta:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### Reservas

**POST /reservations** - Cria uma nova reserva

Corpo:
```json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "companyCode": null
}
```

Resposta: Código 201 com a reserva criada

**GET /reservations/{id}** - Obtém os detalhes de uma reserva

**POST /reservations/{id}/confirmation** - Confirma uma reserva pendente

Corpo:
```json
{
  "amountPaid": 599.76
}
```

**POST /reservations/{id}/cancellation** - Cancela uma reserva pendente ou confirmada

**POST /reservations/{id}/check-in** - Faz o check-in de uma reserva confirmada

**POST /reservations/{id}/check-out** - Faz o check-out de uma reserva com check-in

Resposta: Comprovante da reserva

**GET /reservations/{id}/document** - Obtém a segunda via do comprovante (para reserva paga)

### Mensagens

**GET /messages** - Lista todas as mensagens enviadas

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "..."
  }
]
```

### Relatórios

**GET /reports/occupancy?date=2027-07-10** - Relatório de ocupação para uma data específica

Retorna CSV com as colunas: room, type, reservation, check_in, check_out

**GET /reports/revenue?month=2027-07** - Relatório de receita para um mês

Retorna CSV com as colunas: reservation, room, check_in, check_out, status, amount

## Planos de Tarifa

### STANDARD
- Preço normal das diárias
- Alta temporada: 20% acréscimo
- Reembolso total em caso de cancelamento

### NON_REFUNDABLE
- 15% desconto sobre o total
- Sem reembolso em caso de cancelamento

### CORPORATE
- Desconto por empresa:
  - TECNOSUL: 12%
  - AGROVALE: 18%
  - FARMAVIDA: 25%
- Reembolso total em caso de cancelamento
- Requer `companyCode`

### WEEKEND_PACKAGE
- Entrada na sexta, saída no domingo (exatamente 2 noites)
- Preço fixo:
  - STANDARD: R$ 520,00
  - DELUXE: R$ 820,00
  - SUITE: R$ 1.200,00
- Reembolso de 50% em caso de cancelamento
- Não sofre influência da alta temporada

### LONG_STAY
- Mínimo 7 noites
- 10% desconto sobre as diárias
- Alta temporada continua valendo
- Reembolso total em caso de cancelamento

## Quartos da Pousada

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101 | STANDARD | 2 | R$ 249,90 |
| 102 | STANDARD | 2 | R$ 249,90 |
| 201 | DELUXE | 3 | R$ 389,90 |
| 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Alta Temporada

- 15 de dezembro até o final de fevereiro
- Julho inteiro

Durante a alta temporada, as diárias sofrem acréscimo de 20%.

## Códigos de Erro

| Código | Status | Descrição |
|--------|--------|-----------|
| INVALID_REQUEST | 400 | Requisição inválida (campos faltando, formato incorreto, etc) |
| ROOM_NOT_FOUND | 404 | Quarto não existe |
| RESERVATION_NOT_FOUND | 404 | Reserva não existe |
| ROOM_UNAVAILABLE | 409 | Quarto indisponível para as datas solicitadas |
| OPERATION_NOT_ALLOWED | 409 | Operação não permitida para o status atual da reserva |
| CAPACITY_EXCEEDED | 422 | Número de hóspedes exceeds a capacidade do quarto |

## Arredondamento

Todos os valores monetários são arredondados para centavos usando o arredondamento "meio para par" (HALF_EVEN), em cada etapa do cálculo:
- Preço de cada noite
- Soma das noites (subtotal)
- Desconto
- Total final

## Exemplo de Fluxo

1. **Criar Reserva**: POST /reservations com dados do hóspede
2. **Confirmar**: POST /reservations/{id}/confirmation com valor pago
3. **Email Enviado**: Sistema envia confirmação por email (simulado)
4. **Check-in**: POST /reservations/{id}/check-in quando hóspede chega
5. **Check-out**: POST /reservations/{id}/check-out quando hóspede parte
6. **Comprovante**: GET /reservations/{id}/document ou recebido no check-out

Ou:

1. **Criar Reserva**: POST /reservations
2. **Cancelar**: POST /reservations/{id}/cancellation (se cancelar antes de pagar, nada a devolver)
