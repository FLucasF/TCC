# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Estrutura do Projeto

O projeto segue as orientações do CLAUDE.md, usando padrões de design onde apropriado:

- **Template Method**: Usado nos relatórios (`report/ReportGenerator.java`) para que mudanças estruturais (como adicionar data de geração) afetem todos os relatórios de uma vez. Cada novo tipo de relatório entra como uma subclasse, sem modificar os existentes.

## Compilar e Executar

### Compilar o projeto

```bash
mvn package
```

### Executar o sistema

#### Com Docker Compose (recomendado para testes)

1. Inicie o PostgreSQL:

```bash
docker-compose up -d
```

2. Execute o sistema:

```bash
java -jar target/reservas-1.0.0.jar
```

O sistema usa as configurações padrão (localhost:5432/pousada) se as variáveis de ambiente não estiverem definidas.

#### Com PostgreSQL existente

Configure as variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
export PORT=7070
```

Execute o JAR:

```bash
java -jar target/reservas-1.0.0.jar
```

O sistema cria automaticamente as tabelas no banco de dados e inicializa os 5 quartos da pousada.

## API

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna o comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista mensagens enviadas (emails)

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Ocupação de uma noite
- `GET /reports/revenue?month=2027-07` - Financeiro de um mês
- `GET /reports/tourism-tax?month=2027-07` - Taxa de turismo de um mês
- `GET /reports/rate-plans?month=2027-07` - Receita por plano de tarifa

## Exemplo de Uso

### Criar uma reserva

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

### Confirmar a reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Gerar relatório de ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

## Testes

```bash
mvn test
```

## Verificação Completa

```bash
mvn verify
```
