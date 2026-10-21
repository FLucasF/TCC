# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.6+

## Como executar

### Build

```bash
mvn package
```

### Executar o sistema

```bash
java -jar target/reservations-1.0-SNAPSHOT.jar
```

O sistema inicia na porta 7070 por padrão. Para usar outra porta, defina a variável de ambiente `PORT`:

```bash
PORT=8080 java -jar target/reservations-1.0-SNAPSHOT.jar
```

### Executar os testes

```bash
mvn verify
```

## API REST

### Quartos

- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém comprovante (segunda via)

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro
- `GET /reports/tourism-tax?month=YYYY-MM` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=YYYY-MM` - Relatório de planos de tarifa

## Estrutura do Projeto

O projeto utiliza o padrão **Template Method** para os relatórios, permitindo que novos relatórios sejam facilmente adicionados e que mudanças na estrutura dos relatórios (como adicionar uma linha de data) sejam aplicadas automaticamente em todos eles.

### Pacotes principais

- `com.pousadamarealta.domain` - Entidades de domínio (Room, Reservation, etc.)
- `com.pousadamarealta.repository` - Repositórios em memória
- `com.pousadamarealta.service` - Serviços de negócio
- `com.pousadamarealta.report` - Relatórios CSV com Template Method
- `com.pousadamarealta.api.dto` - DTOs para API REST

## Regras de Negócio

### Quartos

- 101, 102: STANDARD (2 pessoas) - R$ 249,90/noite
- 201, 202: DELUXE (3 pessoas) - R$ 389,90/noite
- 301: SUITE (4 pessoas) - R$ 579,90/noite

### Alta Temporada

- 15 de dezembro a fim de fevereiro
- Mês de julho completo
- Acréscimo de 20% na diária

### Planos de Tarifa

- STANDARD: preço normal
- NON_REFUNDABLE: 15% de desconto, sem reembolso em caso de cancelamento

### Estados da Reserva

1. PENDING - Reserva criada, aguardando pagamento
2. CONFIRMED - Reserva confirmada e paga
3. CHECKED_IN - Hóspede realizou check-in
4. CHECKED_OUT - Hóspede realizou check-out
5. CANCELLED - Reserva cancelada

### Validações

- Não pode haver duas reservas ativas para o mesmo quarto na mesma noite
- Número de hóspedes não pode exceder a capacidade do quarto
- Mínimo de 1 hóspede por reserva
- Data de saída deve ser posterior à data de entrada

## Exemplo de Uso

```bash
# Criar reserva
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
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Gerar relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```
