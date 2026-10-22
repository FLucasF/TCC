# Sistema de Reservas - Pousada Maré Alta

Sistema completo de reservas para pousada com API REST, cálculo de tarifas sazonais, e notificações automáticas.

## Requisitos

- Java 21
- PostgreSQL 18+
- Maven 3.9.6+

## Build

```bash
mvn clean package
```

## Execução

### Variáveis de Ambiente

```bash
export DB_URL="jdbc:postgresql://localhost:5432/reservas"
export DB_USER="usuario"
export DB_PASSWORD="senha"
export PORT=7070  # Opcional, padrão é 7070
```

### Rodando a aplicação

```bash
java -jar target/reservas-1.0.0.jar
```

### Com Docker Compose

```bash
docker-compose up
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva pendente
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação por data
- `GET /reports/revenue?month=2027-07` - Relatório de faturamento por mês

## Quarteis

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101 | STANDARD | 2 | R$ 249,90 |
| 102 | STANDARD | 2 | R$ 249,90 |
| 201 | DELUXE | 3 | R$ 389,90 |
| 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Planos de Tarifa

- **STANDARD**: Sem desconto, reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em cancelamento

## Alta Temporada

- 15 de dezembro a 29 de fevereiro (ou 28/02 em ano não-bissexto)
- Mês de julho inteiro

Durante alta temporada, as diárias têm acréscimo de 20%.

## Sistema de Notificações

Quando uma reserva é confirmada:
- Hóspede recebe e-mail com comprovante
- Governança (governanca@pousadamarealta.com.br) recebe notificação com datas
- Financeiro (financeiro@pousadamarealta.com.br) recebe notificação com valor

Quando uma reserva é cancelada:
- Governança recebe notificação
- Financeiro recebe notificação se há reembolso

Mensagens são armazenadas no banco e consultáveis via `/messages`.

## Testes

```bash
mvn verify
```
