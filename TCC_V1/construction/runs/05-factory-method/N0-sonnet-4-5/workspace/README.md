# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Funcionalidades

- Gerenciamento de 5 quartos (STANDARD, DELUXE, SUITE)
- Criação e gestão de reservas com cálculo automático de preços
- Suporte a alta temporada (15/12 a fim de fevereiro + julho)
- Três tipos de identificação:
  - CPF (pessoa física) - gera comprovante em português
  - CNPJ (empresa) - gera fatura com ISS
  - Passaporte (estrangeiro) - gera booking receipt em inglês
- Planos de tarifa: STANDARD e NON_REFUNDABLE (15% desconto)
- Fluxo completo: Pendente → Confirmado → Check-in → Check-out
- Envio de emails de confirmação (armazenados localmente)
- Relatórios em CSV: ocupação por data e receita por mês

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Configuração

Configure as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/marealte
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070  # opcional, padrão 7070
```

## Execução

### Build

```bash
mvn clean package
```

### Executar

```bash
java -jar target/pousada-mare-alta-1.0.0.jar
```

Ou durante o desenvolvimento:

```bash
mvn exec:java -Dexec.mainClass="br.com.marealte.Main"
```

### Testes

```bash
# Testes unitários
mvn test

# Testes unitários + integração (requer Docker)
mvn verify
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Busca uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna documento
- `GET /reservations/{id}/document` - Retorna segunda via do documento

### Mensagens
- `GET /messages` - Lista emails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Exemplo de Criação de Reserva

### Com CPF
```json
{
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@email.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Com CNPJ
```json
{
  "roomNumber": "201",
  "guestName": "Ana Costa",
  "guestEmail": "ana@email.com",
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Empresa ABC Ltda",
  "checkIn": "2027-03-15",
  "checkOut": "2027-03-17",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

### Com Passaporte
```json
{
  "roomNumber": "301",
  "guestName": "John Smith",
  "guestEmail": "john@email.com",
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "checkIn": "2027-03-20",
  "checkOut": "2027-03-22",
  "guests": 3,
  "ratePlan": "NON_REFUNDABLE"
}
```

## Códigos de Erro

- `400 INVALID_REQUEST` - Requisição inválida
- `404 ROOM_NOT_FOUND` - Quarto não encontrado
- `404 RESERVATION_NOT_FOUND` - Reserva não encontrada
- `409 ROOM_UNAVAILABLE` - Quarto indisponível nas datas
- `409 OPERATION_NOT_ALLOWED` - Operação não permitida no status atual
- `422 CAPACITY_EXCEEDED` - Capacidade do quarto excedida
