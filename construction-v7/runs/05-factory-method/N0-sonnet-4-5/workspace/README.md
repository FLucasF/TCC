# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Funcionalidades

- ✅ Gerenciamento de quartos (5 quartos: 2 STANDARD, 2 DELUXE, 1 SUITE)
- ✅ Criação e gestão de reservas
- ✅ Cálculo automático de preços (temporada alta/baixa)
- ✅ Planos de tarifa (normal e não reembolsável)
- ✅ Fluxo completo: criação → confirmação → check-in → check-out
- ✅ Três tipos de identificação:
  - CPF (comprovante em português)
  - CNPJ (fatura de hospedagem com ISS)
  - Passaporte (booking receipt em inglês)
- ✅ Envio de e-mails de confirmação
- ✅ Relatórios de ocupação e financeiro (CSV)
- ✅ API REST completa

## Temporadas

- **Alta temporada** (+20%): 15/12 a 28/02 e todo mês de julho
- **Baixa temporada**: demais períodos

## Build e Execução

```bash
# Compilar e testar
mvn verify

# Executar
java -jar target/reservations-1.0.0.jar

# Porta customizada
PORT=8080 java -jar target/reservations-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva (pagamento)
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna documento
- `GET /reservations/{id}/document` - Segunda via do documento

### Mensagens
- `GET /messages` - Lista e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Exemplos de Uso

### Criar reserva com CPF
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Criar reserva com CNPJ
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Criar reserva com Passaporte
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-16",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

## Tecnologias

- Java 21
- Javalin 6.7.0
- Jackson (JSON)
- JUnit 5 + AssertJ (testes)
- Maven

## Arredondamento

O sistema utiliza arredondamento bancário (HALF_EVEN) para todos os valores monetários, conforme especificado.

## Observações

- Dados armazenados em memória (reiniciar = perder dados)
- E-mails não são enviados de verdade, ficam armazenados na lista de mensagens
- Todas as validações de negócio implementadas (capacidade, disponibilidade, status, etc.)
