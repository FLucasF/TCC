# Pousada Maré Alta - Sistema de Reservas

Sistema de gestão de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Executar

### Com Docker Compose (recomendado)

```bash
docker-compose up -d
mvn package -DskipTests
java -jar target/mare-alta-1.0-SNAPSHOT.jar
```

### Manualmente

Configure as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070
```

Execute:

```bash
mvn package -DskipTests
java -jar target/mare-alta-1.0-SNAPSHOT.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna documento
- `GET /reservations/{id}/document` - Segunda via do documento

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Tipos de Documento

O sistema suporta três tipos de identificação do hóspede:

1. **CPF** - Pessoa física brasileira (comprovante em português)
2. **CNPJ** - Empresa (fatura de hospedagem com ISS)
3. **Passaporte** - Estrangeiro (booking receipt em inglês)
