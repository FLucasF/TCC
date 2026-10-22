# Implementação do Sistema de Reservas - Pousada Maré Alta

## Resumo Executivo

Sistema completo de gerenciamento de reservas foi desenvolvido em **Java 21** com **Javalin 6.7.0**, sem Spring. Usa **PostgreSQL 18** para persistência de dados e implementa o padrão **Strategy** para suportar múltiplos tipos de documentos (português com CPF, português com CNPJ, inglês com passaporte).

**Tudo foi construído de forma extensível**: adicionar um novo tipo de documento exige apenas criar uma nova classe e registrá-la - sem mexer em código que já funciona.

## Arquitetura

```
src/main/java/com/pousada/
├── api/
│   ├── Main.java                      # Ponto de entrada, inicializa BD e servidor
│   ├── JsonUtil.java                  # Utilitários para serialização JSON
│   └── handler/
│       ├── RoomHandler.java           # GET /rooms
│       ├── ReservationHandler.java    # CRUD de reservas
│       ├── MessageHandler.java        # GET /messages
│       └── ReportHandler.java         # GET /reports/*
├── domain/                             # Modelos de negócio
│   ├── Room.java                      # Quarto (record imutável)
│   ├── Reservation.java               # Reserva (mutable para estado)
│   ├── Message.java                   # E-mail enviado
│   ├── RoomType.java                  # Enum: STANDARD, DELUXE, SUITE
│   ├── RatePlan.java                  # Enum: STANDARD, NON_REFUNDABLE
│   └── ReservationStatus.java         # Enum: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
├── document/                           # Strategy Pattern para documentos
│   ├── DocumentGenerator.java         # Interface
│   ├── CpfDocumentGenerator.java      # Comprovante (português)
│   ├── CnpjDocumentGenerator.java     # Fatura (português com ISS)
│   ├── PassportDocumentGenerator.java # Booking receipt (inglês)
│   ├── DocumentGeneratorFactory.java  # Factory para criar gerador correto
│   └── Document.java                  # DTO para documento gerado
├── service/
│   ├── ReservationService.java        # Lógica de reservas (validação, transitions)
│   └── ReportService.java             # Geração de relatórios CSV
├── repository/
│   ├── RoomRepository.java            # Acesso a dados de quartos
│   ├── ReservationRepository.java     # Acesso a dados de reservas (com queries complexas)
│   └── MessageRepository.java         # Armazenamento de e-mails
├── db/
│   └── Database.java                  # Pool de conexão, migrations automáticas
└── util/
    ├── PriceCalculator.java           # Cálculos com arredondamento HALF_EVEN
    └── Formatter.java                 # Formatação de datas, valores, CPF/CNPJ
```

## Padrões de Projeto Utilizados

### 1. Strategy (para Documentos)
- Cada tipo de documento (CPF, CNPJ, Passaporte) tem sua própria estratégia
- Interface `DocumentGenerator` define o contrato
- `DocumentGeneratorFactory` seleciona a estratégia correta
- **Benefício**: Adicionar novo tipo de documento não mexe em código existente

### 2. Singleton (para Services e Repositories)
- Instância única garantida para toda a aplicação
- Acesso via `getInstance()`

### 3. Data Transfer Object (DTO)
- `Document` é um record imutável para transportar dados do documento gerado

## Características Implementadas

### Cálculo de Preços
- ✅ Alta temporada: 15/12-28/02 + julho = +20%
- ✅ Desconto NON_REFUNDABLE: 15% sobre subtotal
- ✅ Arredondamento HALF_EVEN em cada passo (nightly, subtotal, discount, total)
- ✅ ISS retido em faturas: 5% do total

### Validação de Regras de Negócio
- ✅ Capacidade de quarto: 1 a N hóspedes
- ✅ Datas: saída > entrada
- ✅ Ocupação: sem conflitos de datas por quarto
- ✅ Identificação: exatamente um tipo (CPF, CNPJ ou Passaporte)
- ✅ Campos obrigatórios: CNPJ e nome da empresa juntos; passaporte e país juntos

### Fluxo de Reserva
```
PENDING ──confirm──> CONFIRMED ──check-in──> CHECKED_IN ──check-out──> CHECKED_OUT
  ↓                      ↓
  └─────cancel─────────┘
```

- Cancelamento PENDING: sem reembolso (não pagou)
- Cancelamento CONFIRMED:
  - STANDARD: reembolso total
  - NON_REFUNDABLE: sem reembolso

### Documentos Gerados

#### Comprovante (CPF)
```
Comprovante de reserva nº 7
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
```

#### Fatura (CNPJ)
```
Fatura de hospedagem nº 3
Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
Hóspede: Ana Souza
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
ISS retido (5%): R$ 38,99
```

#### Booking Receipt (Passaporte)
```
Booking receipt no. 4
Guest: John Smith (passport X1234567, United Kingdom)
Room: 201 (DELUXE)
Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
Nights: BRL 779.80
Total: BRL 779.80
```

### E-mails
Enviados automaticamente na confirmação com o comprovante/fatura/receipt dentro:
- Assunto: "Reserva confirmada - Pousada Maré Alta"
- Corpo em português, com o documento apropriado inserido

### Relatórios em CSV

#### Ocupação (por noite)
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

#### Financeiro (por mês)
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,,,,779.80
```

## APIs HTTP

### GET /rooms
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.9
  }
]
```

### POST /reservations
```json
{
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

Retorna 201 com a reserva criada (status PENDING).

### POST /reservations/{id}/confirmation
Confirma e envia e-mail. Retorna 409 se status inválido.

### POST /reservations/{id}/check-in
Retorna 409 se não está CONFIRMED.

### POST /reservations/{id}/check-out
Retorna o documento (record ou fatura ou booking receipt).

### POST /reservations/{id}/cancellation
Calcula reembolso e cancela. Retorna 409 se status inválido.

### GET /reservations/{id}/document
Segunda via do documento (só para reservas pagas).

### GET /messages
Retorna e-mails enviados.

### GET /reports/occupancy?date=2027-07-10
CSV com quartos ocupados naquela noite.

### GET /reports/revenue?month=2027-07
CSV com receita do mês (reservas que começaram naquele mês).

## Códigos de Erro HTTP

| Status | Código | Significado |
|--------|--------|-------------|
| 400 | INVALID_REQUEST | Campo faltando, JSON inválido, datas inválidas |
| 404 | ROOM_NOT_FOUND | Quarto não existe |
| 404 | RESERVATION_NOT_FOUND | Reserva não existe |
| 409 | ROOM_UNAVAILABLE | Quarto já reservado naquelas datas |
| 409 | OPERATION_NOT_ALLOWED | Transição de estado inválida |
| 422 | CAPACITY_EXCEEDED | Mais hóspedes que capacidade do quarto |

## Banco de Dados

### Tabelas

#### rooms
- number (INT PK)
- type (VARCHAR)
- capacity (INT)
- nightly_rate (NUMERIC)

#### reservations
- id (BIGSERIAL PK)
- room_number (INT FK)
- guest_name, guest_email (VARCHAR)
- guest_cpf, company_cnpj, company_name (VARCHAR nullable)
- guest_passport, guest_country (VARCHAR nullable)
- check_in, check_out (DATE)
- guests, nights (INT)
- rate_plan (VARCHAR)
- subtotal, discount, total (NUMERIC)
- status (VARCHAR)
- amount_paid, refund (NUMERIC)

#### messages
- id (BIGSERIAL PK)
- channel (VARCHAR = "EMAIL")
- to (VARCHAR)
- subject, body (VARCHAR/TEXT)

### Migrations
Criadas automaticamente na inicialização (schema criado se não existir).

## Testes

### Cobertura
- PriceCalculatorTest: Alta temporada, cálculo de noites, desconto, ISS
- FormatterTest: Formatação de datas (PT/EN), valores, CPF, CNPJ

### Executar
```bash
mvn test       # Testes unitários
mvn verify     # Testes + packaging
```

## Compilação e Deploy

### Build
```bash
mvn clean package
```

Gera: `/target/mare-alta-api-1.0.0.jar` (7.3 MB, com todas as dependências incluídas via maven-shade-plugin)

### Execução
```bash
export DB_URL="jdbc:postgresql://localhost:5432/mare_alta"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070

java -jar target/mare-alta-api-1.0.0.jar
```

### Inicialização de Dados
Ao iniciar:
1. Cria tabelas se não existirem
2. Popula 5 quartos se tabela estiver vazia

## Extensibilidade para Agências de Turismo

A estrutura está pronta para suportar um novo tipo de documento. Seria necessário:

1. Adicionar campos na Reservation (ex: `agencyCode`, `agencyName`)
2. Criar `TravelAgencyDocumentGenerator implements DocumentGenerator`
3. Registrar na `DocumentGeneratorFactory`
4. Validar os campos no ReservationHandler

**Zero alterações** em código existente - apenas adição!

## Conformidade com Requisitos

✅ Java 21  
✅ Javalin 6.7.0, sem Spring  
✅ PostgreSQL 18  
✅ Maven com shade plugin (jar auto-executável)  
✅ Variáveis de ambiente (DB_URL, DB_USER, DB_PASSWORD, PORT)  
✅ Migrations automáticas  
✅ mvn verify passa  
✅ Design orientado a extensão (próximos casos entram num lugar só)  
✅ Arredondamento HALF_EVEN  
✅ Tratamento de erros com status HTTP correto  
✅ Documentos em múltiplos formatos  
✅ Relatórios em CSV com charset UTF-8  
✅ Identificação flexível (CPF, CNPJ ou Passaporte)
