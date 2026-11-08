# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Configuração do Banco de Dados

O sistema cria automaticamente as tabelas necessárias ao iniciar. Configure as variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="sua-senha"
export PORT=7070  # opcional, padrão é 7070
```

Para criar o banco de dados no PostgreSQL:

```bash
createdb pousada
```

## Compilação

```bash
mvn clean package
```

## Execução

```bash
java -jar target/pousada-mare-alta-1.0.0.jar
```

Ou usando as variáveis de ambiente inline:

```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="postgres" \
DB_PASSWORD="senha" \
java -jar target/pousada-mare-alta-1.0.0.jar
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Busca uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Gera segunda via do comprovante

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Planos de Tarifa

### STANDARD
Plano normal sem desconto, com reembolso total em caso de cancelamento.

### NON_REFUNDABLE
15% de desconto, sem reembolso.

### CORPORATE
Desconto por convênio empresarial. Requer `companyCode`:
- TECNOSUL: 12%
- AGROVALE: 18%
- FARMAVIDA: 25%

### WEEKEND_PACKAGE
Pacote de fim de semana (sexta a domingo):
- STANDARD: R$ 520,00
- DELUXE: R$ 820,00
- SUITE: R$ 1.200,00
Reembolso de 50% em caso de cancelamento.

### LONG_STAY
Para estadias de 7+ noites, 10% de desconto, reembolso total.

## Exemplo de Uso

Criar uma reserva:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Confirmar reserva:

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

## Testes

```bash
mvn test
```

## Verificação Completa

```bash
mvn verify
```
