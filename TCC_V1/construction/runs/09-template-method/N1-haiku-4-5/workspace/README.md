# Pousada Maré Alta - Sistema de Reservas

Sistema completo de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Requisitos

- Java 21 ou superior
- PostgreSQL 18 ou superior
- Maven

## Compilação

```bash
mvn clean package
```

Isso gera um JAR executável em `target/mare-alta.jar`.

## Execução

```bash
java -jar target/mare-alta.jar
```

## Variáveis de Ambiente

- `DB_URL`: URL de conexão com o PostgreSQL (ex: `jdbc:postgresql://localhost:5432/mare_alta`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta para o servidor (padrão: 7070)

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes de uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório por plano de tarifa

## Arquitetura

- **Models**: Entidades do domínio (Room, Reservation, Message, etc.)
- **Database**: Camada de acesso ao banco de dados com repositories
- **Business**: Lógica de negócio (ReservationService)
- **API**: Endpoints REST com Javalin
- **Reports**: Geração de relatórios em CSV
- **Utils**: Utilitários para preços e formatação

## Testes

```bash
mvn verify
```
