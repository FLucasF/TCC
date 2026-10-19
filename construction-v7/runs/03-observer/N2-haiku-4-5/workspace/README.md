# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para uma pousada pequena no litoral, desenvolvido em Java 21 com Javalin 6.7.0.

## Características

- **5 Quartos**: 2 STANDARD (2 pessoas, R$ 249,90), 2 DELUXE (3 pessoas, R$ 389,90), 1 SUITE (4 pessoas, R$ 579,90)
- **Temporada Alta**: +20% nas diárias de 15/12 a 28/02 e julho inteiro
- **Planos de Tarifa**: STANDARD (reembolso total) e NON_REFUNDABLE (15% de desconto, sem reembolso)
- **Sistema de Notificações**: Emails automáticos para hóspede, governança e financeiro
- **Relatórios**: Ocupação diária e receita mensal em CSV
- **Validações**: Capacidade máxima, indisponibilidade de quartos, validação de datas

## Instalação

### Pré-requisitos
- Java 21+
- Maven 3.6+

### Build

```bash
mvn clean package
```

Gera um JAR executável em `target/reserva-system-1.0.0.jar`.

## Execução

```bash
java -jar target/reserva-system-1.0.0.jar
```

O servidor inicia na porta 7070 por padrão, ou na porta definida pela variável de ambiente `PORT`:

```bash
PORT=8080 java -jar target/reserva-system-1.0.0.jar
```

## API REST

### Quartos

**GET /rooms**
- Retorna lista de quartos disponíveis

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

**POST /reservations**
- Cria nova reserva (status: PENDING)

Request:
```json
{
  "roomNumber": 201,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

**GET /reservations/{id}**
- Obtém detalhes da reserva

**POST /reservations/{id}/confirmation**
- Confirma a reserva (PENDING → CONFIRMED)
- Envia 3 emails: hóspede, governança, financeiro

**POST /reservations/{id}/cancellation**
- Cancela a reserva (PENDING/CONFIRMED → CANCELLED)
- Processa reembolso conforme o plano

**POST /reservations/{id}/check-in**
- Realiza check-in (CONFIRMED → CHECKED_IN)

**POST /reservations/{id}/check-out**
- Realiza check-out (CHECKED_IN → CHECKED_OUT)
- Retorna comprovante

**GET /reservations/{id}/document**
- Obtém segunda via do comprovante (se pagamento realizado)

### Mensagens

**GET /messages**
- Lista todas as mensagens enviadas (emails)

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

**GET /reports/occupancy?date=2027-07-10**
- Relatório de ocupação em formato CSV

```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

**GET /reports/revenue?month=2027-07**
- Relatório de receita em formato CSV

```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,499.80
total,,,,499.80
```

## Estados da Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED (qualquer momento antes de CHECKED_IN)
```

## Cálculo de Preços

1. **Nightly Rate**: Diária base × 1.20 em alta temporada
2. **Subtotal**: Soma das nightly rates de cada dia
3. **Discount**: 15% do subtotal se plano NON_REFUNDABLE
4. **Total**: Subtotal - Discount

Arredondamento "meio para o par" em cada etapa.

## Notificações

### Confirmação de Reserva
- **Hóspede**: Email com comprovante completo
- **Governança**: Datas, número de hóspedes
- **Financeiro**: Valor pagamento

### Cancelamento
- **Governança**: Datas liberadas (se reserva confirmada)
- **Financeiro**: Valor de reembolso (se aplicável)

## Tratamento de Erros

| Código | Status | Descrição |
|--------|--------|-----------|
| INVALID_REQUEST | 400 | Falta campo, formato inválido |
| ROOM_NOT_FOUND | 404 | Quarto não existe |
| RESERVATION_NOT_FOUND | 404 | Reserva não existe |
| ROOM_UNAVAILABLE | 409 | Quarto ocupado nas datas |
| OPERATION_NOT_ALLOWED | 409 | Estado não permite operação |
| CAPACITY_EXCEEDED | 422 | Hóspedes > capacidade quarto |

## Estrutura do Projeto

```
src/
├── main/java/com/pousadamarealta/
│   ├── domain/          # Modelos (Room, Reservation, Message, etc)
│   ├── service/         # Lógica de negócio
│   ├── repository/      # Acesso aos dados (em memória)
│   ├── api/
│   │   ├── dto/         # Request/Response
│   │   └── ReservationApi.java
│   └── App.java         # Aplicação principal
└── test/java/...        # Testes unitários
```

## Testes

Execute os testes com:

```bash
mvn test
```

Todos os testes (28) devem passar:
- 9 testes de PricingService
- 8 testes de ReportService
- 11 testes de ReservationService

## Dados Persistidos

Por enquanto, os dados ficam apenas em memória. Se o sistema reiniciar, todas as reservas se perdem.

## Desenvolvido com

- Java 21
- Javalin 6.7.0
- Jetty 11.0.25
- Jackson 2.17.2
- Maven 3.x

## Autor

Desenvolvido como sistema de reservas para a Pousada Maré Alta.
