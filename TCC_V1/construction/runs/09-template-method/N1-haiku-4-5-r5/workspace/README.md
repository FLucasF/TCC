# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, construído em Java 21 com Javalin e PostgreSQL 18.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven

## Compilação

```bash
mvn clean package
```

Isso vai gerar um JAR fat (`target/pousada-mare-alta-1.0.0.jar`) que pode ser executado independentemente.

## Execução

```bash
java -jar target/pousada-mare-alta-1.0.0.jar
```

O sistema precisa das seguintes variáveis de ambiente:

- `DB_URL`: URL de conexão PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do PostgreSQL
- `DB_PASSWORD`: Senha do PostgreSQL
- `PORT`: Porta para escutar (padrão: 7070)

Exemplo:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT="7070"
java -jar target/pousada-mare-alta-1.0.0.jar
```

## Estrutura

- **Quartos**: 5 quartos com diferentes tipos (STANDARD, DELUXE, SUITE)
- **Reservas**: Sistema completo de ciclo de vida (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT ou CANCELLED)
- **Relatórios**: Ocupação, Receita, Taxa de Turismo, Análise de Planos de Tarifa
- **Mensagens**: Sistema de notificações por email

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro
- `GET /reports/tourism-tax?month=YYYY-MM` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=YYYY-MM` - Análise de planos de tarifa

## Testes

```bash
mvn verify
```
