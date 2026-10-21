# Sistema de Reservas - Pousada Maré Alta

Um sistema completo de reservas para a Pousada Maré Alta, implementado em Java 21 com Javalin.

## Características

- **5 quartos** configurados com preços e capacidades diferentes
- **5 planos de tarifa** com diferentes regras de cálculo e cancelamento
- **Fluxo completo de reservas**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT ou CANCELLED
- **Cálculos precisos** com arredondamento "meio para o par"
- **Alta temporada** automática (dezembro-fevereiro e julho)
- **Relatórios** de ocupação e financeiro em CSV
- **Emails simulados** com comprovantes de reserva

## Compilação e Execução

### Compilar e testar:
```bash
mvn clean verify
```

### Executar:
```bash
java -jar target/pousada-1.0.0.jar
```

A aplicação inicia na porta 7070 (ou via variável de ambiente `PORT`).

## Quartos

| Quarto | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101 | STANDARD | 2 | R$ 249,90 |
| 102 | STANDARD | 2 | R$ 249,90 |
| 201 | DELUXE | 3 | R$ 389,90 |
| 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Planos de Tarifa

### STANDARD
Preço normal das diárias. Reembolso integral em caso de cancelamento antes do check-in.

### NON_REFUNDABLE
Desconto de 15% sobre as diárias. Sem reembolso em caso de cancelamento.

### CORPORATE
Desconto por empresa conveniada (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%). Reembolso integral em caso de cancelamento antes do check-in.

Exemplo:
- DELUXE de 10/03 a 12/03: R$ 779,80 → R$ 93,58 desconto → R$ 686,22 total

### WEEKEND_PACKAGE
Preço fixo para sexta-domingo (exatamente 2 noites):
- STANDARD: R$ 520,00
- DELUXE: R$ 820,00
- SUITE: R$ 1.200,00

Reembolso de 50% em caso de cancelamento.

### LONG_STAY
7 ou mais noites: desconto de 10% sobre as diárias. Reembolso integral em caso de cancelamento.

Exemplo:
- STANDARD 7 noites: R$ 1.749,30 → R$ 174,93 desconto → R$ 1.574,37 total

## API

### Quartos
```
GET /rooms
```
Retorna lista de todos os quartos com detalhes.

### Reservas
```
POST /reservations
```
Cria uma nova reserva (status PENDING).

Corpo esperado:
```json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "companyCode": null
}
```

```
GET /reservations/{id}
```
Retorna detalhes de uma reserva.

```
POST /reservations/{id}/confirmation
```
Confirma a reserva (PENDING → CONFIRMED). Envia email com comprovante.

```
POST /reservations/{id}/check-in
```
Realiza o check-in (CONFIRMED → CHECKED_IN).

```
POST /reservations/{id}/check-out
```
Realiza o check-out (CHECKED_IN → CHECKED_OUT). Retorna comprovante.

```
POST /reservations/{id}/cancellation
```
Cancela a reserva (qualquer status anterior ao check-out → CANCELLED).

```
GET /reservations/{id}/document
```
Retorna segunda via do comprovante (apenas para reservas confirmadas ou posteriormente).

### Mensagens
```
GET /messages
```
Retorna histórico de emails enviados.

### Relatórios
```
GET /reports/occupancy?date=2027-03-12
```
Retorna ocupação do dia em formato CSV.

```
GET /reports/revenue?month=2027-03
```
Retorna receita do mês em formato CSV.

## Validações

- **400 INVALID_REQUEST**: Dados inválidos, datas fora do formato, menos de 1 hóspede, regras do plano não atendidas
- **404 ROOM_NOT_FOUND**: Quarto não existe
- **404 RESERVATION_NOT_FOUND**: Reserva não existe
- **409 ROOM_UNAVAILABLE**: Quarto já reservado para as datas
- **409 OPERATION_NOT_ALLOWED**: Operação não permitida para o status atual
- **422 CAPACITY_EXCEEDED**: Mais hóspedes que a capacidade do quarto

## Exemplo de Fluxo

```bash
# 1. Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# 2. Confirmar reserva (envia email)
curl -X POST http://localhost:7070/reservations/1/confirmation

# 3. Fazer check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# 4. Fazer check-out (retorna comprovante)
curl -X POST http://localhost:7070/reservations/1/check-out

# 5. Ver relatório de ocupação
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

## Estrutura do Código

- `Application.java` - API HTTP principal
- `service/ReservationService.java` - Lógica de reservas
- `service/PricingService.java` - Cálculo de preços e tarifas
- `service/MessageService.java` - Gerenciamento de emails
- `service/ReportService.java` - Geração de relatórios CSV
- `model/` - Classes de dados (Room, Reservation, Message, etc)
- `data/RoomData.java` - Dados iniciais dos quartos

## Dados em Memória

As reservas, mensagens e relatórios são armazenados em memória. Ao reiniciar a aplicação, todos os dados são perdidos. Para persistência, seria necessário adicionar um banco de dados.

## Testes

A cobertura de testes inclui:
- Cálculos de preços para todos os planos de tarifa
- Validações de datas e capacidade
- Fluxos de cancelamento e devoluções
- Arredondamento "meio para o par"

Execute `mvn test` para rodar os testes.
