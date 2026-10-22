# Sistema de Reservas - Pousada Maré Alta

Um sistema completo de gerenciamento de reservas para a Pousada Maré Alta, construído com Java 21, Javalin e PostgreSQL.

## Features

- ✅ Gerenciamento de 5 quartos com tipos e capacidades diferentes
- ✅ Cálculo de preços com alta temporada (20% de desconto adicional)
- ✅ Planos de tarifa (STANDARD, NON_REFUNDABLE com 15% desconto)
- ✅ Suporte a três tipos de documentos de reserva:
  - Comprovante (CPF)
  - Fatura de hospedagem (CNPJ)
  - Booking receipt em inglês (Passaporte)
- ✅ Sistema de confirmação por e-mail
- ✅ Check-in e check-out
- ✅ Cancelamento com política de reembolso
- ✅ Relatórios de ocupação e financeiro em CSV
- ✅ Arredondamento banker's rounding em todos os cálculos

## Compilação

```bash
mvn clean package
```

O arquivo JAR será gerado em `target/pousada-mare-alta.jar`.

## Execução

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=7070 \
java -jar target/pousada-mare-alta.jar
```

As variáveis de ambiente padrão são:
- `PORT`: 7070 (se não especificada)
- `DB_URL`, `DB_USER`, `DB_PASSWORD`: Obrigatórias

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém os detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna documento
- `GET /reservations/{id}/document` - Obtém documento (segunda via)

### Mensagens e Relatórios
- `GET /messages` - Lista e-mails enviados
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Tipos de Identificação

A reserva pode ser criada com uma das três formas de identificação:

```json
{
  "guestCpf": "111.222.333-44"
}
```

```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda"
}
```

```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom"
}
```

## Testes

```bash
mvn test
```

## Verificação

```bash
mvn verify
```
