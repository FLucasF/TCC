# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, uma pousada boutique no litoral com 5 quartos.

## Características

- **Gerenciamento de Reservas**: Criar, consultar, confirmar, cancelar e fazer check-in/check-out
- **Cálculo Automático de Preços**: Com suporte a alta temporada e diferentes planos de tarifa
- **Relatórios**: Ocupação, financeiro, taxa de turismo e rentabilidade por plano
- **E-mails**: Confirmação de reserva com comprovante
- **API REST**: Endpoints bem definidos para integração com site

## Build e Execução

### Build
```bash
mvn clean package
```

### Executar
```bash
java -jar target/reservas-1.0.0.jar
```

O servidor irá iniciar na porta 7070 (ou na porta definida pela variável de ambiente PORT).

### Testes
```bash
mvn test
mvn verify
```

## API Endpoints

### Quartos
- `GET /rooms` - Listar todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas (e-mails)

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação para uma data
- `GET /reports/revenue?month=YYYY-MM` - Receita para um mês
- `GET /reports/tourism-tax?month=YYYY-MM` - Taxa de turismo para um mês
- `GET /reports/rate-plans?month=YYYY-MM` - Rentabilidade por plano para um mês

## Quartos Disponíveis

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|---------|
| 101 | STANDARD | 2 | R$ 249,90 |
| 102 | STANDARD | 2 | R$ 249,90 |
| 201 | DELUXE | 3 | R$ 389,90 |
| 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Planos de Tarifa

- **STANDARD**: Sem desconto, reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

## Alta Temporada

- 15 de dezembro a 28 de fevereiro: +20% no valor das diárias
- Julho inteiro: +20% no valor das diárias

## Exemplo de Uso

### Criar uma reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar uma reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}'
```

### Obter relatório de receita
```bash
curl http://localhost:7070/reports/revenue?month=2027-07 \
  -H "Accept: text/csv"
```

## Estrutura do Projeto

```
src/
├── main/java/com/pousadamareal/reservas/
│   ├── Main.java                    # Aplicação principal com Javalin
│   ├── domain/                      # Modelos de domínio
│   │   ├── Room.java
│   │   ├── Reservation.java
│   │   ├── Message.java
│   │   └── ...enums
│   ├── dto/                         # Data Transfer Objects
│   │   ├── CreateReservationRequest.java
│   │   ├── ReservationResponse.java
│   │   └── ...
│   ├── service/
│   │   ├── ReservationService.java  # Lógica de negócio
│   │   └── ApiException.java
│   └── util/
│       └── PricingCalculator.java   # Cálculos de preço
└── test/
    └── java/com/pousadamareal/reservas/
        └── ... testes unitários
```

## Tecnologias

- Java 21
- Javalin 6.7.0 (framework web leve)
- Jackson (JSON)
- Maven
- JUnit 5 (testes)

## Persistência

Atualmente, os dados são armazenados em memória. As reservas e mensagens se perdem ao reiniciar a aplicação. Para produção, seria necessário integrar com um banco de dados.

## Taxa de Turismo

A prefeitura cobra R$ 4,50 por hóspede por noite. Este valor pode ser alterado no relatório de taxa de turismo.

## Status das Reservas

1. **PENDING**: Reserva criada, aguardando confirmação de pagamento
2. **CONFIRMED**: Pagamento recebido
3. **CHECKED_IN**: Hóspede fez check-in
4. **CHECKED_OUT**: Hóspede fez check-out
5. **CANCELLED**: Reserva cancelada (com ou sem reembolso conforme plano)
