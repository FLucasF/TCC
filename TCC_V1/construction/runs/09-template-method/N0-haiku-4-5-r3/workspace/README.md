# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Construir o Projeto

```bash
mvn clean package
```

Isso gera um JAR executável em `target/marea-alta.jar`.

## Executar o Sistema

O sistema requer as seguintes variáveis de ambiente:

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/marea_alta`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT` (opcional): Porta para o servidor HTTP (padrão: 7070)

Exemplo:

```bash
java -jar target/marea-alta.jar
```

Com variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/marea_alta
export DB_USER=postgres
export DB_PASSWORD=senha
export PORT=8080
java -jar target/marea-alta.jar
```

## API

### Quartos

**GET /rooms**
- Retorna todos os quartos disponíveis

### Reservas

**POST /reservations**
- Cria uma nova reserva
- Body: `{ roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan }`
- Datas no formato: `2027-07-10`
- Retorna: 201 com a reserva criada

**GET /reservations/{id}**
- Retorna os dados da reserva

**POST /reservations/{id}/confirmation**
- Confirma uma reserva pendente
- Body: `{ amountPaid }`
- Retorna: A reserva confirmada

**POST /reservations/{id}/cancellation**
- Cancela uma reserva
- Retorna: A reserva cancelada

**POST /reservations/{id}/check-in**
- Realiza o check-in de uma reserva
- Retorna: A reserva com check-in realizado

**POST /reservations/{id}/check-out**
- Realiza o check-out de uma reserva
- Retorna: O comprovante de check-out

**GET /reservations/{id}/document**
- Retorna a segunda via do comprovante

### Mensagens

**GET /messages**
- Retorna todas as mensagens enviadas (e-mails)

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
- Retorna relatório de ocupação em CSV

**GET /reports/revenue?month=2027-07**
- Retorna relatório financeiro em CSV

**GET /reports/tourism-tax?month=2027-07**
- Retorna relatório de taxa de turismo em CSV

**GET /reports/rate-plans?month=2027-07**
- Retorna relatório de planos de tarifa em CSV

## Testes

Execute os testes com:

```bash
mvn verify
```

## Estrutura do Banco de Dados

O sistema cria automaticamente as seguintes tabelas:

- `rooms`: Quartos da pousada
- `reservations`: Reservas
- `messages`: Mensagens enviadas (e-mails)

## Dados Padrão

A pousada possui 5 quartos:
- 101 e 102: STANDARD (até 2 pessoas, R$ 249,90/noite)
- 201 e 202: DELUXE (até 3 pessoas, R$ 389,90/noite)
- 301: SUITE (até 4 pessoas, R$ 579,90/noite)

## Período de Alta Temporada

- 15 de dezembro até 28 de fevereiro
- Mês inteiro de julho
- Neste período, as diárias têm aumento de 20%

## Planos de Tarifa

- STANDARD: Tarifa normal, 100% reembolsável em caso de cancelamento
- NON_REFUNDABLE: 15% de desconto, não reembolsável em caso de cancelamento

## Taxa de Turismo

R$ 4,50 por hóspede por noite, cobrado pela prefeitura.
