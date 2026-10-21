# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.6+

## Compilação

```bash
mvn clean package
```

O comando gera um JAR executável em `target/mare-alta-1.0.0.jar`.

## Execução

```bash
java -jar target/mare-alta-1.0.0.jar
```

Por padrão, o servidor inicia na porta 7070. Para usar outra porta, configure a variável de ambiente `PORT`:

```bash
PORT=8080 java -jar target/mare-alta-1.0.0.jar
```

## Testes

```bash
mvn test
```

## Verificação completa

```bash
mvn verify
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos disponíveis

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out (retorna o documento)
- `GET /reservations/{id}/document` - Obtém segunda via do documento

### Mensagens
- `GET /messages` - Lista mensagens enviadas (e-mails)

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Tipos de Identificação

Ao criar uma reserva, é necessário informar exatamente um dos três tipos de identificação:

1. **CPF**: `guestCpf` - Gera comprovante em português
2. **CNPJ**: `companyCnpj` + `companyName` - Gera fatura com ISS
3. **Passaporte**: `guestPassport` + `guestCountry` - Gera booking receipt em inglês

## Exemplo de Requisição

```bash
curl -X POST http://localhost:7070/reservations \
  -H 'Content-Type: application/json' \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Regras de Negócio

- Alta temporada: 15/12 a fim de fevereiro + mês de julho inteiro (20% acréscimo)
- Plano NON_REFUNDABLE: 15% de desconto, sem reembolso
- Plano STANDARD: sem desconto, reembolso total em caso de cancelamento
- Arredondamento: HALF_EVEN (meio para o par)
