# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Compilação

```bash
mvn clean package
```

## Execução

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL JDBC do PostgreSQL (formato: `jdbc:postgresql://host:port/database`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta HTTP (opcional, padrão: 7070)

Exemplo:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha
export PORT=7070

java -jar target/reservas-1.0-SNAPSHOT.jar
```

## Quartos

A pousada possui 5 quartos:

- **101, 102**: STANDARD (2 pessoas, R$ 249,90/noite)
- **201, 202**: DELUXE (3 pessoas, R$ 389,90/noite)
- **301**: SUITE (4 pessoas, R$ 579,90/noite)

## Planos de Tarifa

1. **STANDARD**: Tarifa normal, reembolso total em caso de cancelamento
2. **NON_REFUNDABLE**: 15% de desconto, sem reembolso
3. **CORPORATE**: Desconto corporativo (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), reembolso total
4. **WEEKEND_PACKAGE**: Pacote de fim de semana (sexta a domingo), preço fixo, reembolso de 50%
5. **LONG_STAY**: Estadia longa (7+ noites), 10% de desconto, reembolso total

## Alta Temporada

A alta temporada (acréscimo de 20% na diária) ocorre em:
- 15 de dezembro a fim de fevereiro
- Todo o mês de julho

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Retorna segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens de email enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Exemplos de Requisições

### Criar Reserva Standard
```json
POST /reservations
{
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Criar Reserva Corporativa
```json
POST /reservations
{
  "roomNumber": "202",
  "guestName": "Pedro Oliveira",
  "guestEmail": "pedro@email.com",
  "guestCpf": "11111111111",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "CORPORATE",
  "companyCode": "TECNOSUL"
}
```

### Criar Reserva de Pacote de Fim de Semana
```json
POST /reservations
{
  "roomNumber": "201",
  "guestName": "Ana Costa",
  "guestEmail": "ana@email.com",
  "guestCpf": "22222222222",
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-14",
  "guests": 2,
  "ratePlan": "WEEKEND_PACKAGE"
}
```

## Testes

Para executar os testes:

```bash
mvn test        # Testes unitários
mvn verify      # Testes unitários + integração
```

## Códigos de Erro

- `400 INVALID_REQUEST`: Requisição inválida
- `404 ROOM_NOT_FOUND`: Quarto não encontrado
- `404 RESERVATION_NOT_FOUND`: Reserva não encontrada
- `409 ROOM_UNAVAILABLE`: Quarto indisponível nas datas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida no estado atual
- `422 CAPACITY_EXCEEDED`: Capacidade do quarto excedida
