# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.x

## Configuração

O sistema utiliza variáveis de ambiente para configuração:

- `DB_URL`: URL de conexão JDBC ao PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco de dados (padrão: `postgres`)
- `PORT`: Porta HTTP (padrão: `7070`)

## Compilação

```bash
mvn package
```

## Execução

```bash
java -jar target/pousada-sistema-1.0.0.jar
```

Com variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=8080 \
java -jar target/pousada-sistema-1.0.0.jar
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Obtém segunda via do documento

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Tipos de Identificação

O sistema suporta três tipos de hóspedes:

1. **Pessoa Física** (CPF): Recebe comprovante em português
2. **Empresa** (CNPJ + Razão Social): Recebe fatura com ISS retido
3. **Estrangeiro** (Passaporte + País): Recebe booking receipt em inglês

## Exemplo de Requisição

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Testes

```bash
mvn verify
```

## Estrutura de Dados

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90)
- 201, 202: DELUXE (3 pessoas, R$ 389,90)
- 301: SUITE (4 pessoas, R$ 579,90)

### Alta Temporada

20% de acréscimo:
- 15/12 a 28/02
- Julho inteiro

### Planos de Tarifa

- STANDARD: Preço normal, reembolsável
- NON_REFUNDABLE: 15% de desconto, não reembolsável
