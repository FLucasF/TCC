# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- Maven 3.6+
- PostgreSQL 18

## Como rodar

### Com Docker (recomendado)

1. Inicie o PostgreSQL:
```bash
docker-compose up -d
```

2. Configure as variáveis de ambiente (ou use os valores padrão):
```bash
export DB_URL=jdbc:postgresql://localhost:5432/maralta
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070
```

3. Execute o JAR:
```bash
java -jar target/maralta-1.0.0.jar
```

### Build do projeto

```bash
mvn package
```

### Executar testes

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Obter segunda via do documento

### Mensagens

- `GET /messages` - Listar todas as mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Tipos de Identificação

O sistema suporta três tipos de identificação de hóspedes:

### CPF
```json
{
  "guestCpf": "111.222.333-44"
}
```

### CNPJ (empresa)
```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda"
}
```

### Passaporte (estrangeiro)
```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom"
}
```

## Exemplo de uso

### Criar uma reserva com CPF

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar uma reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

## Estrutura do projeto

O projeto segue as orientações do CLAUDE.md, organizando o código para que novos tipos de documento (como agências de turismo) possam ser adicionados facilmente sem modificar o código existente.

- `domain/` - Modelos de domínio e regras de negócio
- `service/` - Lógica de aplicação
- `repository/` - Acesso a dados
- `controller/` - API REST
- `infrastructure/` - Configuração de banco de dados

O padrão Visitor é usado para lidar com diferentes tipos de identificação e geração de documentos, permitindo adicionar novos tipos sem mexer nos existentes.
