# Sistema de Reservas - Pousada Maré Alta

## Como Rodar

### Pré-requisitos
- PostgreSQL 18 rodando
- Java 21+

### Compilar e Gerar JAR

```bash
mvn clean package
```

Isso gerará um JAR executável em `target/reservation-system.jar`.

### Rodar o Sistema

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT=7070  # Opcional, padrão é 7070
export HOTEL_TEST_MODE=true  # Opcional, para ativar modo teste

java -jar target/reservation-system.jar
```

## Variáveis de Ambiente

- **DB_URL** (obrigatório): String de conexão JDBC do PostgreSQL
- **DB_USER** (obrigatório): Usuário do banco de dados
- **DB_PASSWORD** (obrigatório): Senha do banco de dados
- **PORT** (opcional): Porta para o servidor (padrão: 7070)
- **HOTEL_TEST_MODE** (opcional): Se "true", ativa modo de teste para emails

## Modo Teste

Quando `HOTEL_TEST_MODE=true`:
- Todos os emails são redirecionados para `testes@pousadamarealta.com.br`
- Assuntos ganham prefixo `[TESTE]`
- O email original é informado no início do corpo da mensagem
- Não é gerada cópia para arquivo

## API REST

### Quartos

```http
GET /rooms
```

Retorna lista de quartos disponíveis.

### Reservas

```http
POST /reservations
Content-Type: application/json

{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Retorna `201` com a reserva criada.

```http
GET /reservations/{id}
```

Retorna detalhes da reserva.

```http
POST /reservations/{id}/confirmation
Content-Type: application/json

{
  "amountPaid": 599.76
}
```

Confirma a reserva (envia email de confirmação).

```http
POST /reservations/{id}/check-in
```

Faz check-in da reserva.

```http
POST /reservations/{id}/check-out
```

Faz check-out e retorna comprovante.

```http
GET /reservations/{id}/document
```

Retorna segunda via do comprovante (apenas para reservas pagas).

```http
POST /reservations/{id}/cancellation
```

Cancela a reserva (reembolso de acordo com plano de tarifa).

### Mensagens

```http
GET /messages
```

Retorna lista de todas as mensagens enviadas (incluindo bloqueadas e cópias para arquivo).

### Relatórios

```http
GET /reports/occupancy?date=2027-07-10
```

Retorna relatório de ocupação em CSV para a data especificada.

```http
GET /reports/revenue?month=2027-07
```

Retorna relatório financeiro em CSV para o mês especificado (formato YYYY-MM).

## Quartos

| Número | Tipo      | Capacidade | Diária (Normal) | Diária (Alta Temporada) |
|--------|-----------|------------|-----------------|-------------------------|
| 101    | STANDARD  | 2 pessoas  | R$ 249,90       | R$ 299,88               |
| 102    | STANDARD  | 2 pessoas  | R$ 249,90       | R$ 299,88               |
| 201    | DELUXE    | 3 pessoas  | R$ 389,90       | R$ 467,88               |
| 202    | DELUXE    | 3 pessoas  | R$ 389,90       | R$ 467,88               |
| 301    | SUITE     | 4 pessoas  | R$ 579,90       | R$ 695,88               |

## Planos de Tarifa

- **STANDARD**: Preço normal, reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

## Ciclo de Vida da Reserva

1. **PENDING**: Reserva criada, aguardando confirmação
2. **CONFIRMED**: Pagamento recebido, email de confirmação enviado
3. **CHECKED_IN**: Hóspede fez check-in
4. **CHECKED_OUT**: Hóspede fez check-out
5. **CANCELLED**: Reserva cancelada (pode acontecer de PENDING ou CONFIRMED)

## Alta Temporada

- 15 de dezembro até 28 de fevereiro
- Mês de julho (todo o mês)

Nessas datas, as diárias têm acréscimo de 20%.

## Validação de Emails

Um email é considerado válido quando:
- Contém exatamente um `@`
- Tem conteúdo antes do `@`
- Tem um domínio com pelo menos um ponto após o `@` (exemplo: `nome@empresa.com.br`)

Emails inválidos são bloqueados e aparecem na lista de mensagens com status "BLOCKED".

## Arredondamento

Todos os valores monetários são arredondados com arredondamento "meio para par" (banker's rounding) em cada passo:
- Preço de cada noite
- Subtotal (soma das noites)
- Desconto
- Total

## Códigos de Erro

| Status | Código                | Descrição                                    |
|--------|----------------------|----------------------------------------------|
| 400    | INVALID_REQUEST      | Dados inválidos ou fora do formato          |
| 404    | ROOM_NOT_FOUND       | Quarto não existe                            |
| 404    | RESERVATION_NOT_FOUND| Reserva não encontrada                       |
| 409    | ROOM_UNAVAILABLE     | Quarto já reservado para essas datas        |
| 409    | OPERATION_NOT_ALLOWED| Operação não permitida para este status      |
| 422    | CAPACITY_EXCEEDED    | Mais hóspedes do que o quarto comporta      |
