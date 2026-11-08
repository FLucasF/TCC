# Sistema de Reservas - Pousada Maré Alta

Sistema de reservas para uma pousada com 5 quartos, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18
- Docker Compose (opcional, para testes)
- curl (para testar a API)

## Estrutura da Pousada

### Quartos

- **Quartos 101-102**: STANDARD, 2 pessoas, R$ 249,90/noite
- **Quartos 201-202**: DELUXE, 3 pessoas, R$ 389,90/noite
- **Quarto 301**: SUITE, 4 pessoas, R$ 579,90/noite

### Alta Temporada

- 15 de dezembro a 28 de fevereiro (aumento de 20%)
- Mês de julho (aumento de 20%)

## Planos de Tarifa

1. **STANDARD**: Sem desconto, reembolso total se cancelado
2. **NON_REFUNDABLE**: 15% de desconto, sem reembolso
3. **CORPORATE**: Desconto por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), reembolso total
4. **WEEKEND_PACKAGE**: Preço fixo (STD R$ 520, DLX R$ 820, SUITE R$ 1.200), apenas sexta a domingo, 50% de reembolso
5. **LONG_STAY**: 7+ noites com 10% de desconto, reembolso total

## Compilação

```bash
mvn clean compile
```

## Empacotamento

```bash
mvn package
```

Gera um JAR executável em `target/mare-alta.jar`.

## Execução

### Com PostgreSQL local

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT="7070"

java -jar target/mare-alta.jar
```

### Com PostgreSQL em Docker

```bash
# Iniciar banco de dados
docker-compose up -d

# Executar aplicação
bash run.sh
```

## API

### Quartos

- **GET /rooms** - Lista todos os quartos

### Reservas

- **POST /reservations** - Cria uma reserva
- **GET /reservations/{id}** - Obtém detalhes de uma reserva
- **POST /reservations/{id}/confirmation** - Confirma uma reserva (PENDING → CONFIRMED)
- **POST /reservations/{id}/check-in** - Registra entrada (CONFIRMED → CHECKED_IN)
- **POST /reservations/{id}/check-out** - Registra saída e retorna comprovante (CHECKED_IN → CHECKED_OUT)
- **POST /reservations/{id}/cancellation** - Cancela uma reserva
- **GET /reservations/{id}/document** - Obtém segunda via do comprovante

### Mensagens

- **GET /messages** - Lista e-mails enviados

### Relatórios

- **GET /reports/occupancy?date=YYYY-MM-DD** - Relatório de ocupação (CSV)
- **GET /reports/revenue?month=YYYY-MM** - Relatório financeiro (CSV)

## Exemplos de Requisições

### Criar Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 779.80
  }'
```

### Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

## Testes

Execute os testes com:

```bash
mvn test
mvn verify
```

Para testar manualmente a API:

```bash
bash test-api.sh
```

## Arredondamento Monetário

Todos os valores são arredondados para centavos usando "round half to even" (banker's rounding) a cada passo: diárias, subtotal, desconto e total.

## Ciclo de Vida de uma Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
            ↓
        CANCELLED
```

- PENDING: Reserva criada, sem pagamento
- CONFIRMED: Pagamento recebido
- CHECKED_IN: Hóspede fez check-in
- CHECKED_OUT: Hóspede saiu
- CANCELLED: Reserva cancelada (antes do check-in)

## Estrutura do Banco de Dados

- **rooms**: Informações dos quartos
- **reservations**: Dados das reservas
- **messages**: E-mails enviados

O banco de dados é inicializado automaticamente na primeira execução.

## Notas

- Os e-mails não são enviados de verdade, apenas armazenados no banco de dados
- Todos os valores monetários no JSON têm exatamente 2 casas decimais
- As datas usam formato ISO (YYYY-MM-DD)
- Comprovantes são enviados por e-mail na confirmação e retornados no check-out
