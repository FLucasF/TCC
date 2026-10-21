# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- Maven
- Jackson (JSON)

## Estrutura

- **5 quartos**:
  - 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
  - 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
  - 301: SUITE (4 pessoas, R$ 579,90/noite)

- **Alta temporada** (+20%):
  - 15 de dezembro a fim de fevereiro
  - Julho inteiro

- **Planos de tarifa**:
  - STANDARD: normal, reembolsável
  - NON_REFUNDABLE: 15% desconto, não reembolsável

## Compilar e Executar

```bash
# Compilar e testar
mvn clean verify

# Gerar JAR
mvn package

# Executar (porta padrão 7070, ou variável PORT)
java -jar target/pousada-mare-alta-1.0.0.jar

# Ou com porta customizada
PORT=8080 java -jar target/pousada-mare-alta-1.0.0.jar
```

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
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios (CSV)
- `GET /reports/occupancy?date=2027-07-10` - Ocupação de uma data
- `GET /reports/revenue?month=2027-07` - Receita de um mês
- `GET /reports/tourism-tax?month=2027-07` - Taxa de turismo de um mês
- `GET /reports/rate-plans?month=2027-07` - Comparação de planos de tarifa

## Exemplo de Uso

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Gerar relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

## Códigos de Erro

- `400 INVALID_REQUEST` - Requisição inválida
- `404 ROOM_NOT_FOUND` - Quarto não encontrado
- `404 RESERVATION_NOT_FOUND` - Reserva não encontrada
- `409 ROOM_UNAVAILABLE` - Quarto já reservado
- `409 OPERATION_NOT_ALLOWED` - Operação não permitida
- `422 CAPACITY_EXCEEDED` - Capacidade excedida

## Arquitetura

O projeto segue princípios de design orientado a objetos com foco em extensibilidade:

- **Domain**: Entidades e lógica de negócio
- **Repository**: Armazenamento em memória
- **Service**: Coordenação e regras de negócio
- **API**: Controllers e DTOs
- **Report**: Sistema extensível de relatórios

Os relatórios foram organizados para permitir fácil adição de novos tipos sem modificar os existentes, seguindo o princípio Open/Closed.
