# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Funcionalidades

- Gestão de 5 quartos (STANDARD, DELUXE, SUITE)
- Cálculo automático de preços com alta temporada (+20% de dez a fev e julho)
- Planos de tarifa: STANDARD (sem desconto) e NON_REFUNDABLE (15% desconto)
- Fluxo de reserva: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT (ou CANCELLED)
- Notificações por e-mail para hóspede, governança e financeiro
- Relatórios de ocupação e receita em CSV

## Build e Execução

### Pré-requisitos

- Java 21
- Maven
- PostgreSQL 18

### Compilar

```bash
mvn clean package
```

Isso gera um jar executável em `target/marealta-1.0.0.jar`.

### Executar

```bash
java -jar target/marealta-1.0.0.jar
```

O servidor inicia na porta 7070 por padrão. Para usar outra porta:

```bash
PORT=8080 java -jar target/marealta-1.0.0.jar
```

### Variáveis de Ambiente

- `DB_URL`: URL de conexão PostgreSQL (ex: `jdbc:postgresql://localhost:5432/marealta`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta do servidor (padrão: 7070)

### Exemplo de Execução

```bash
DB_URL=jdbc:postgresql://localhost:5432/marealta \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=7070 \
java -jar target/marealta-1.0.0.jar
```

## API REST

### Quartos

- **GET /rooms** - Lista todos os quartos

### Reservas

- **POST /reservations** - Cria uma nova reserva
  - Retorna: 201 com dados da reserva
  - Campos obrigatórios: roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
  - Datas no formato: yyyy-MM-dd

- **GET /reservations/{id}** - Obtém dados de uma reserva

- **POST /reservations/{id}/confirmation** - Confirma a reserva (muda status para CONFIRMED)
  - Dispara notificações para hóspede, governança e financeiro

- **POST /reservations/{id}/check-in** - Faz check-in (muda status para CHECKED_IN)

- **POST /reservations/{id}/check-out** - Faz check-out (muda status para CHECKED_OUT)
  - Retorna: comprovante da reserva

- **POST /reservations/{id}/cancellation** - Cancela a reserva (muda status para CANCELLED)
  - Dispara notificações se a reserva estava confirmada

- **GET /reservations/{id}/document** - Obtém segunda via do comprovante
  - Disponível apenas para reservas confirmadas

### Mensagens

- **GET /messages** - Lista todas as mensagens de e-mail enviadas

### Relatórios

- **GET /reports/occupancy?date=yyyy-MM-dd** - Relatório de ocupação para uma data
  - Formato: CSV com colunas room, type, reservation, check_in, check_out

- **GET /reports/revenue?month=yyyy-MM** - Relatório de receita para um mês
  - Formato: CSV com colunas reservation, room, check_in, check_out, status, amount

## Códigos de Erro

- **400 INVALID_REQUEST** - Requisição inválida (campos faltando, formato incorreto)
- **404 ROOM_NOT_FOUND** - Quarto não existe
- **404 RESERVATION_NOT_FOUND** - Reserva não existe
- **409 ROOM_UNAVAILABLE** - Quarto indisponível para as datas
- **409 OPERATION_NOT_ALLOWED** - Operação não permitida para o status atual
- **422 CAPACITY_EXCEEDED** - Número de hóspedes maior que capacidade do quarto

## Testes

```bash
mvn test
```

Para rodar a verificação completa:

```bash
mvn verify
```

## Arquitetura

### Padrões de Design

- **Observer**: Sistema de notificações extensível (ReservationObserver)
  - GuestNotificationObserver: notifica o hóspede
  - GovernanceNotificationObserver: notifica a governança
  - FinanceNotificationObserver: notifica o financeiro

### Estrutura de Pacotes

- `domain/`: Entidades e enums do domínio
- `service/`: Lógica de negócio (PricingCalculator, ReservationService, etc)
- `repository/`: Acesso ao banco de dados
- `persistence/`: Configuração e migração do banco
- `notification/`: Sistema de notificações (Observer)
- `api/`: Controllers e DTOs REST

## Banco de Dados

O sistema cria automaticamente as tabelas na primeira execução:

- `rooms`: Informações dos quartos (pré-carregada com os 5 quartos da pousada)
- `reservations`: Dados das reservas
- `messages`: Mensagens de e-mail enviadas

Não é necessário nenhuma configuração manual do banco.
