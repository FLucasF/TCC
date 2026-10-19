# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido com Java 21 e Javalin.

## Características

- 5 quartos com diferentes tipos e capacidades
- 5 planos de tarifa com regras específicas:
  - **STANDARD**: plano normal com reembolso total
  - **NON_REFUNDABLE**: 15% de desconto, sem reembolso
  - **CORPORATE**: descontos corporativos (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%)
  - **WEEKEND_PACKAGE**: pacote de fim de semana (sexta a domingo) com preço fixo
  - **LONG_STAY**: 10% de desconto para estadias de 7+ noites
- Cálculo automático de alta temporada (15/dez a fim/fev e todo mês de julho)
- Sistema de estados para reservas (Pendente → Confirmada → Check-in → Check-out / Cancelada)
- Envio de e-mails de confirmação
- Relatórios em CSV de ocupação e financeiro

## Requisitos

- Java 21+
- Maven 3.6+

## Como compilar

```bash
mvn clean package
```

## Como executar

```bash
java -jar target/mare-alta-1.0.0.jar
```

O servidor inicia na porta 7070 por padrão. Para usar outra porta:

```bash
PORT=8080 java -jar target/mare-alta-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Exemplo de uso

Criar uma reserva corporativa:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

## Testes

```bash
mvn test
```

## Arquitetura

O sistema foi desenvolvido seguindo o padrão **Strategy** para os planos de tarifa, permitindo fácil adição de novos planos sem modificar código existente. Cada plano implementa a interface `RatePlan` com suas próprias regras de validação, cálculo de preço e política de reembolso.
