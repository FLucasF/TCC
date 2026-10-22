# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Compilação

```bash
mvn clean package
```

O comando gera um JAR executável em `target/pousada-mareal.jar`.

## Execução

```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="postgres" \
DB_PASSWORD="password" \
PORT=7070 \
java -jar target/pousada-mareal.jar
```

## Variáveis de Ambiente

- `DB_URL`: URL de conexão PostgreSQL (obrigatório)
- `DB_USER`: Usuário do PostgreSQL (obrigatório)
- `DB_PASSWORD`: Senha do PostgreSQL (obrigatório)
- `PORT`: Porta do servidor (padrão: 7070)

## Endpoints da API

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva
- `GET /reservations/:id` - Obtém dados da reserva
- `POST /reservations/:id/confirmation` - Confirma a reserva
- `POST /reservations/:id/cancellation` - Cancela a reserva
- `POST /reservations/:id/check-in` - Faz check-in
- `POST /reservations/:id/check-out` - Faz check-out
- `GET /reservations/:id/document` - Obtém a segunda via do documento

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Recursos

- 5 quartos (2 STANDARD, 2 DELUXE, 1 SUITE)
- Suporte a 3 tipos de documentos: Comprovante (CPF), Fatura (CNPJ), Booking Receipt (Passaporte)
- Cálculo automático de tarifa com alta temporada (20% sobre 15/12-29/02 e julho)
- Arredondamento correto com "meio para o par"
- Cancelamento com reembolso variável por plano de tarifa
- Relatórios em CSV
