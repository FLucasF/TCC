# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Compilação

```bash
mvn clean package
```

## Execução

O sistema precisa de um banco PostgreSQL rodando. Configure as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha
export PORT=7070  # opcional, padrão é 7070
```

Execute o JAR gerado:

```bash
java -jar target/pousada-reservas-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém documento (comprovante/fatura/receipt)

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Testes

```bash
mvn test
```

## Verificação completa

```bash
mvn verify
```
