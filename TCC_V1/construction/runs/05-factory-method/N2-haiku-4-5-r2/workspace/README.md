# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gerenciamento de reservas para pousada, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Pré-requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 18+

## Configuração

### Variáveis de Ambiente

- `DB_URL`: URL de conexão PostgreSQL (ex: `jdbc:postgresql://localhost:5432/mare_alta`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta para o servidor (padrão: 7070)

### Exemplo de Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/mare_alta"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT="7070"

java -jar target/mare-alta-1.0.jar
```

## Build

```bash
mvn clean package
```

Isso gera `/workspace/target/mare-alta-1.0.jar`.

## Testes

```bash
mvn verify
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter dados da reserva
- `POST /reservations/{id}/confirmation` - Confirmar pagamento
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out (retorna documento)
- `GET /reservations/{id}/document` - Obter segunda via do documento

### Mensagens
- `GET /messages` - Listar e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação por noite (CSV)
- `GET /reports/revenue?month=2027-07` - Receita por mês (CSV)

## Tipos de Identificação

As reservas suportam 3 tipos de identificação:

1. **CPF**: Gera "Comprovante de reserva" em português
   - Campos: `guestCpf`

2. **CNPJ**: Gera "Fatura de hospedagem" com ISS (5%) em português
   - Campos: `companyCnpj`, `companyName`

3. **Passaporte**: Gera "Booking receipt" em inglês com moeda BRL
   - Campos: `guestPassport`, `guestCountry`

## Exemplo de Uso

```bash
# Criar reserva com CPF
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar pagamento
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}'

# Fazer check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Fazer check-out (retorna documento)
curl -X POST http://localhost:7070/reservations/1/check-out

# Obter segunda via
curl http://localhost:7070/reservations/1/document
```

## Arquitetura

- **Model**: Entidades (Reservation, Room, Message, Document, etc)
- **Repository**: Acesso a dados via JDBC
- **Service**: Lógica de negócio
- **HTTP**: Endpoints Javalin
- **Document**: Strategy Pattern para formatação de documentos
- **Util**: Formatação de datas, moeda e validações

## Características

✅ Gerenciamento de 5 quartos (2 STANDARD, 2 DELUXE, 1 SUITE)  
✅ Cálculo automático de preços com sazonalidade  
✅ Arredondamento "meio para par" em todas operações monetárias  
✅ 3 tipos de documentos (Comprovante, Fatura, Booking Receipt)  
✅ Sistema de mensagens (e-mails em memória)  
✅ Relatórios de ocupação e receita em CSV  
✅ Validação de conflitos de datas  
✅ Gerenciamento de cancelamentos e reembolsos  
✅ PostgreSQL com criação automática de tabelas
