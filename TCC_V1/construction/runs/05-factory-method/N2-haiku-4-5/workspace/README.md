# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Compilação e Execução

### Compilação

```bash
mvn clean package
```

Gera um arquivo JAR executável em `target/pousada-mareal.jar`.

### Execução

```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="postgres" \
DB_PASSWORD="senha" \
PORT=7070 \
java -jar target/pousada-mareal.jar
```

Variáveis de ambiente:
- `DB_URL`: URL de conexão com PostgreSQL (obrigatório)
- `DB_USER`: Usuário do banco de dados (obrigatório)
- `DB_PASSWORD`: Senha do banco de dados (obrigatório)
- `PORT`: Porta HTTP (padrão: 7070)

## Testes

```bash
mvn test
mvn verify
```

## API

### Quartos

**GET /rooms** - Lista todos os quartos

Resposta:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### Reservas

**POST /reservations** - Cria uma nova reserva

Requisição com CPF:
```json
{
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-10",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Requisição com CNPJ:
```json
{
  "roomNumber": 201,
  "guestName": "Ana Souza",
  "guestEmail": "ana@example.com",
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 1,
  "ratePlan": "STANDARD"
}
```

Requisição com Passaporte:
```json
{
  "roomNumber": 201,
  "guestName": "John Smith",
  "guestEmail": "john@example.com",
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 1,
  "ratePlan": "NON_REFUNDABLE"
}
```

**GET /reservations/{id}** - Obtém detalhes de uma reserva

**POST /reservations/{id}/confirmation** - Confirma o pagamento da reserva

**POST /reservations/{id}/cancellation** - Cancela a reserva

**POST /reservations/{id}/check-in** - Realiza o check-in

**POST /reservations/{id}/check-out** - Realiza o check-out e devolve o comprovante

**GET /reservations/{id}/document** - Obtém a segunda via do documento

### Mensagens

**GET /messages** - Lista todas as mensagens enviadas

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Conteúdo do e-mail..."
  }
]
```

### Relatórios

**GET /reports/occupancy?date=2027-07-10** - Relatório de ocupação

Resposta (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

**GET /reports/revenue?month=2027-07** - Relatório financeiro

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
total,599.76
```

## Estrutura do Projeto

```
src/
├── main/java/br/com/pousadamareal/
│   ├── Main.java - Entrada da aplicação
│   ├── api/
│   │   ├── dto/ - Conversores JSON
│   │   └── handlers/ - Handlers dos endpoints
│   ├── db/ - Acesso ao banco de dados
│   ├── domain/ - Modelos de domínio
│   ├── repository/ - Acesso a dados
│   ├── service/ - Lógica de negócio
│   └── util/ - Utilidades
└── test/ - Testes
```

## Características

- Gestão de 5 quartos com diferentes tipos e capacidades
- Preços dinâmicos com alta temporada (dez 15-fev 28/29 e julho)
- Dois planos de tarifa: STANDARD e NON_REFUNDABLE (15% desconto)
- Arredondamento "meio para o par" em todos os cálculos
- Três tipos de identificação: CPF, CNPJ e Passaporte
- Diferentes documentos por tipo: Comprovante, Fatura e Booking Receipt
- E-mails de confirmação (armazenados no banco)
- Relatórios de ocupação e financeiro em CSV
- Validação completa de dados
- Tratamento de erros com códigos específicos

## Banco de Dados

O sistema cria automaticamente as tabelas necessárias na primeira execução:
- `rooms` - Informações dos quartos
- `reservations` - Dados das reservas
- `messages` - Mensagens enviadas

Não é necessário preparar o banco previamente.
