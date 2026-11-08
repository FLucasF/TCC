# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, com suporte a múltiplos planos de tarifa, cálculo automático de preços com arredondamento banqueiro, relatórios em CSV e integração com email simulada.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Configuração

Defina as variáveis de ambiente antes de executar:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT="7070"
```

Se `PORT` não for definida, a porta padrão é 7070.

## Build

```bash
mvn clean verify
```

O comando acima:
- Compila o código
- Executa todos os testes
- Gera um JAR executável em `target/reservations-system-1.0.0.jar`

## Execução

```bash
java -jar target/reservations-system-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirma a reserva
- `POST /reservations/{id}/cancellation` - Cancela a reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação por noite (CSV)
- `GET /reports/revenue?month=2027-07` - Receita por mês (CSV)

## Estrutura dos Quartos

| Número | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101, 102 | STANDARD | 2 pessoas | R$ 249,90 |
| 201, 202 | DELUXE | 3 pessoas | R$ 389,90 |
| 301 | SUITE | 4 pessoas | R$ 579,90 |

## Planos de Tarifa

1. **STANDARD** - Preço normal, sem desconto
2. **NON_REFUNDABLE** - 15% de desconto, sem reembolso se cancelar
3. **CORPORATE** - Desconto corporativo (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), reembolso total se cancelar
4. **WEEKEND_PACKAGE** - Pacote fixo (sexta a domingo), reembolso de 50% se cancelar
5. **LONG_STAY** - 10% de desconto para 7+ noites, reembolso total se cancelar

## Alta Temporada

- Julho (inteiro)
- 15 de dezembro a 28 de fevereiro

Preço aumenta 20% durante alta temporada.

## Arredondamento

Todos os valores monetários são arredondados para centavos usando arredondamento banqueiro (half-to-even) em cada passo do cálculo.

## Exemplo de Requisição

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": "779.80"}'
```

## Testes

```bash
mvn test
```

## Banco de Dados

O sistema cria automaticamente todas as tabelas necessárias na primeira execução. Os quartos são inseridos automaticamente com os dados padrão.
