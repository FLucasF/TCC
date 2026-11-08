# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Estrutura do Projeto

O projeto segue uma arquitetura em camadas organizada com design patterns:

- **Domain**: Modelos de domínio (Room, Reservation, Message, etc.)
- **Repository**: Acesso a dados com PostgreSQL
- **Service**: Lógica de negócio
- **Controller**: Endpoints REST
- **Report**: Relatórios CSV usando Template Method pattern

### Relatórios com Template Method

Os 4 relatórios CSV (ocupação, financeiro, taxa de turismo e planos de tarifa) foram implementados usando o **Template Method pattern**:

- `CsvReport`: Classe base abstrata que define a estrutura comum (título → cabeçalho → linhas → total)
- Cada relatório específico herda e implementa apenas suas partes únicas (nome, período, colunas, dados)
- Novos relatórios entram criando uma nova classe que estende `CsvReport`
- Mudanças estruturais (como adicionar data de geração) ocorrem em um único lugar

## Compilação e Execução

### Requisitos

- Java 21
- Maven 3.8+
- PostgreSQL 18

### Build

```bash
mvn clean package
```

Isso gera o JAR executável em `target/reservations.jar`.

### Execução

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha
export PORT=7070

java -jar target/reservations.jar
```

O sistema cria automaticamente as tabelas no banco de dados na primeira execução.

## API REST

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório de planos de tarifa

## Regras de Negócio

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Alta Temporada

A alta temporada (15/12 a 28/02 e todo mês de julho) aplica 20% de acréscimo na diária.

### Planos de Tarifa

- **STANDARD**: Tarifa normal, com reembolso em cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Arredondamento

Todos os valores monetários usam arredondamento "meio para o par" (RoundingMode.HALF_EVEN) com 2 casas decimais.

### Fluxo de Reserva

1. **PENDING**: Reserva criada
2. **CONFIRMED**: Reserva confirmada (pagamento efetuado)
3. **CHECKED_IN**: Hóspede fez check-in
4. **CHECKED_OUT**: Hóspede fez check-out
5. **CANCELLED**: Reserva cancelada (antes do check-in)

### Validações

- Quarto não pode ter duas reservas na mesma noite
- Número de hóspedes não pode exceder capacidade do quarto
- Número de hóspedes deve ser pelo menos 1
- Check-out deve ser depois do check-in

## Testes

### Testes Unitários

```bash
mvn test
```

Testa a lógica de preços, incluindo cálculo de alta temporada e arredondamento.

### Testes de Integração

Os testes de integração estão em `IntegrationTestManual.java` e requerem Docker (Testcontainers).

## Exemplo de Uso

### Criar uma reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Gerar relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

## Códigos de Erro

- `400 INVALID_REQUEST` - Requisição inválida
- `404 ROOM_NOT_FOUND` - Quarto não encontrado
- `404 RESERVATION_NOT_FOUND` - Reserva não encontrada
- `409 ROOM_UNAVAILABLE` - Quarto indisponível nas datas
- `409 OPERATION_NOT_ALLOWED` - Operação não permitida no status atual
- `422 CAPACITY_EXCEEDED` - Capacidade do quarto excedida
