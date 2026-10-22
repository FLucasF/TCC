# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, construído com Java 21 e Javalin.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Configuração

O sistema utiliza as seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão JDBC ao PostgreSQL (default: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (default: `postgres`)
- `DB_PASSWORD`: Senha do banco (default: `postgres`)
- `PORT`: Porta do servidor HTTP (default: `7070`)

## Build

```bash
mvn clean package
```

Isso gera um JAR executável em `target/pousada-reservas-1.0.0.jar`.

## Execução

```bash
java -jar target/pousada-reservas-1.0.0.jar
```

Ou com variáveis de ambiente customizadas:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=8080 \
java -jar target/pousada-reservas-1.0.0.jar
```

## Testes

```bash
mvn test
```

## Verificação completa

```bash
mvn verify
```

## Planos de Tarifa

O sistema suporta 5 planos de tarifa:

1. **STANDARD**: Plano normal, sem desconto, reembolso total em caso de cancelamento
2. **NON_REFUNDABLE**: 15% de desconto, sem reembolso
3. **CORPORATE**: Desconto por empresa conveniada (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%)
4. **WEEKEND_PACKAGE**: Pacote de fim de semana (sexta a domingo), preço fixo por tipo de quarto
5. **LONG_STAY**: Estadia longa (7+ noites), 10% de desconto

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas (emails)

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação por data
- `GET /reports/revenue?month=2027-07` - Relatório financeiro por mês

## Exemplos de Uso

### Criar uma reserva

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

### Confirmar uma reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Criar reserva com plano corporativo

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

### Listar quartos

```bash
curl http://localhost:7070/rooms
```

### Obter relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

## Desenvolvimento

Para iniciar o ambiente de desenvolvimento com PostgreSQL:

```bash
docker-compose up -d
mvn clean package
java -jar target/pousada-reservas-1.0.0.jar
```

Ou simplesmente:

```bash
./start.sh
```

## Arquitetura

O sistema segue os princípios do CLAUDE.md, organizando o código de forma que adicionar novos planos de tarifa seja questão de criar uma nova classe que implementa a interface `RatePlan`, sem modificar as classes existentes:

```java
public interface RatePlan {
    void validate(RoomType roomType, DateRange dateRange, int guests, String companyCode);
    Money calculateSubtotal(RoomType roomType, DateRange dateRange);
    Money calculateDiscount(Money subtotal);
    Money calculateRefund(Money amountPaid);
    String getType();
}
```

Cada plano implementa sua própria lógica de validação, cálculo de preço e reembolso, seguindo o padrão Strategy.
