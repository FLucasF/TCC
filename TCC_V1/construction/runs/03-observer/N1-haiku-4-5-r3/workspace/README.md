# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta.

## Compilação e Empacotamento

```bash
mvn clean verify
```

Isto gera um JAR executável em `target/reservations-system.jar`.

## Execução

```bash
java -jar target/reservations-system.jar
```

## Variáveis de Ambiente

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta para o servidor (padrão: 7070)

## Exemplo

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="password"
export PORT="8080"
java -jar target/reservations-system.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação em CSV
- `GET /reports/revenue?month=2027-07` - Relatório financeiro em CSV

## Características

- Cinco quartos com tarifas diferentes
- Sazonalidade (15 dez a fim de fev e julho com 20% acréscimo)
- Planos de tarifa (STANDARD e NON_REFUNDABLE com 15% desconto)
- Arredondamento "meio para o par" (HALF_EVEN)
- Comprovantes formatados
- Sistema de notificações extensível para múltiplos departamentos
- Relatórios em CSV
- Banco de dados PostgreSQL
