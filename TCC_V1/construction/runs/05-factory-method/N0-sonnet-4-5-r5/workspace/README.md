# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Funcionalidades

- **Gestão de Quartos**: 5 quartos (2 STANDARD, 2 DELUXE, 1 SUITE)
- **Reservas**: Criação, confirmação, check-in, check-out e cancelamento
- **Cálculo Automático**: Preços com alta temporada e planos tarifários
- **Documentos**: Comprovantes em português, faturas para empresas e booking receipts em inglês
- **E-mails**: Sistema de notificação por e-mail (armazenado em banco)
- **Relatórios**: Ocupação por noite e receita por mês em formato CSV

## Tipos de Identificação de Hóspedes

O sistema suporta três tipos de identificação:

1. **CPF** - Pessoa física brasileira (gera comprovante em português)
2. **CNPJ** - Empresa (gera fatura com ISS retido)
3. **Passaporte** - Estrangeiro (gera booking receipt em inglês)

## Temporadas e Preços

### Quartos

| Número | Tipo     | Capacidade | Diária Base |
|--------|----------|------------|-------------|
| 101    | STANDARD | 2 pessoas  | R$ 249,90   |
| 102    | STANDARD | 2 pessoas  | R$ 249,90   |
| 201    | DELUXE   | 3 pessoas  | R$ 389,90   |
| 202    | DELUXE   | 3 pessoas  | R$ 389,90   |
| 301    | SUITE    | 4 pessoas  | R$ 579,90   |

### Alta Temporada (+20%)

- 15 de dezembro a 28 de fevereiro
- Mês de julho inteiro

### Planos Tarifários

- **STANDARD**: Preço normal, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, não reembolsável

## Requisitos

- Java 21 ou superior
- PostgreSQL 18 ou superior
- Maven 3.9 ou superior

## Configuração

Configure as seguintes variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070  # Opcional, padrão é 7070
```

## Build

```bash
mvn clean package
```

Isso gera o arquivo `target/pousada-mare-alta-1.0.0.jar`.

## Execução

```bash
java -jar target/pousada-mare-alta-1.0.0.jar
```

O servidor inicia na porta 7070 (ou conforme definido em `PORT`).

## API REST

### Quartos

**GET /rooms**
- Lista todos os quartos disponíveis

### Reservas

**POST /reservations**
- Cria uma nova reserva (retorna 201)
- Body (exemplo com CPF):
```json
{
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@email.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

- Body (exemplo com CNPJ):
```json
{
  "roomNumber": "201",
  "guestName": "Ana Souza",
  "guestEmail": "ana@empresa.com",
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

- Body (exemplo com passaporte):
```json
{
  "roomNumber": "301",
  "guestName": "John Smith",
  "guestEmail": "john@email.com",
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "NON_REFUNDABLE"
}
```

**GET /reservations/{id}**
- Consulta uma reserva

**POST /reservations/{id}/confirmation**
- Confirma uma reserva (status PENDING → CONFIRMED)
- Envia e-mail de confirmação

**POST /reservations/{id}/check-in**
- Faz check-in (status CONFIRMED → CHECKED_IN)

**POST /reservations/{id}/check-out**
- Faz check-out (status CHECKED_IN → CHECKED_OUT)
- Retorna o documento (comprovante/fatura/receipt)

**POST /reservations/{id}/cancellation**
- Cancela uma reserva
- Calcula reembolso conforme plano tarifário

**GET /reservations/{id}/document**
- Segunda via do documento (apenas para reservas pagas)

### Mensagens

**GET /messages**
- Lista todos os e-mails enviados

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
- Relatório de ocupação para uma noite específica (CSV)

**GET /reports/revenue?month=2027-07**
- Relatório financeiro do mês (CSV)

## Códigos de Erro

| Código | HTTP | Descrição |
|--------|------|-----------|
| INVALID_REQUEST | 400 | Requisição inválida |
| ROOM_NOT_FOUND | 404 | Quarto não encontrado |
| RESERVATION_NOT_FOUND | 404 | Reserva não encontrada |
| ROOM_UNAVAILABLE | 409 | Quarto indisponível nas datas |
| OPERATION_NOT_ALLOWED | 409 | Operação não permitida no status atual |
| CAPACITY_EXCEEDED | 422 | Número de hóspedes excede capacidade |

## Estrutura do Banco de Dados

O sistema cria automaticamente as tabelas necessárias:

- **rooms**: Cadastro de quartos
- **reservations**: Todas as reservas
- **messages**: E-mails enviados

## Testes

Execute os testes unitários:

```bash
mvn test
```

Os testes de integração requerem Docker e podem ser executados com:

```bash
mvn verify -DskipITs=false
```

## Exemplo de Uso

1. Listar quartos disponíveis:
```bash
curl http://localhost:7070/rooms
```

2. Criar uma reserva:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Maria Silva",
    "guestEmail": "maria@email.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

3. Confirmar a reserva (assumindo ID 1):
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

4. Consultar e-mails enviados:
```bash
curl http://localhost:7070/messages
```

## Licença

Propriedade da Pousada Maré Alta © 2027
