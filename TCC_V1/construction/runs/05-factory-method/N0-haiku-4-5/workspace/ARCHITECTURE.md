# Arquitetura - Sistema de Reservas Pousada Maré Alta

## Visão Geral

Sistema de gerenciamento de reservas de hotel desenvolvido em Java 21 com Javalin 6.7.0, utilizando PostgreSQL 18 como banco de dados.

## Estrutura de Diretórios

```
/workspace
├── src/
│   ├── main/java/com/pousadamareaita/
│   │   ├── Application.java                 # Ponto de entrada
│   │   ├── api/                             # Handlers HTTP
│   │   │   ├── RoomHandler.java
│   │   │   ├── ReservationHandler.java
│   │   │   ├── MessageHandler.java
│   │   │   └── ReportHandler.java
│   │   ├── database/
│   │   │   └── Database.java                # Gerenciamento de conexões e schema
│   │   ├── model/                           # Modelos de dados
│   │   │   ├── Room.java
│   │   │   ├── Reservation.java
│   │   │   ├── Message.java
│   │   │   └── Document.java
│   │   ├── service/                         # Lógica de negócio
│   │   │   ├── RoomService.java
│   │   │   ├── ReservationService.java
│   │   │   ├── MessageService.java
│   │   │   └── DocumentService.java
│   │   ├── exception/
│   │   │   └── ApiException.java            # Exceções customizadas
│   │   └── util/
│   │       └── MoneyUtils.java              # Utilitários de cálculo monetário
│   └── test/java/com/pousadamareaita/
│       ├── MoneyUtilsTest.java
│       └── ReservationServiceTest.java
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── run.sh
├── README.md
├── TESTING.md
└── ARCHITECTURE.md (este arquivo)
```

## Componentes Principais

### 1. Database (database/Database.java)

Responsável por:
- Gerenciar conexões com PostgreSQL
- Criar e inicializar tabelas no banco
- Inserir dados iniciais (quartos)

**Tabelas:**
- `rooms` - Informações dos 5 quartos
- `reservations` - Registro de todas as reservas
- `messages` - Histórico de mensagens enviadas

### 2. Models (model/)

**Room**
- number, type, capacity, nightlyRate
- Imutável após criação

**Reservation**
- id, roomNumber, guestName, guestEmail
- Campos de identificação: guestCpf, companyCnpj, companyName, guestPassport, guestCountry
- Datas: checkIn, checkOut
- Cálculos: nights, subtotal, discount, total
- Status: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
- Valores: amountPaid, refund

**Message**
- id, channel, to, subject, body
- Armazenado para auditoria

**Document**
- Representa qualquer documento gerado (Comprovante, Fatura, Booking Receipt)
- Contém todos os dados da reserva + texto formatado

### 3. Services (service/)

**RoomService**
- Retorna lista de quartos
- Busca quarto por número

**ReservationService**
- Criar reserva (valida tudo)
- Obter reserva por ID
- Confirmar, cancelar, check-in, check-out
- Buscar reservas por data/quarto
- Buscar reservas por mês (para relatórios)
- Cálculos: subtotal (com alta temporada), desconto, total
- Arredondamento HALF_EVEN em cada passo

**MessageService**
- Enviar mensagem (armazenar em BD)
- Listar mensagens

**DocumentService**
- Gerar documento apropriado conforme tipo de identificação
- Formatar valores monetários em BRL ou BRL_EN
- Formatar datas conforme idioma

### 4. API Handlers (api/)

**RoomHandler**
- GET /rooms

**ReservationHandler**
- POST /reservations - criar
- GET /reservations/{id} - consultar
- POST /reservations/{id}/confirmation - confirmar com pagamento
- POST /reservations/{id}/cancellation - cancelar
- POST /reservations/{id}/check-in - entrada
- POST /reservations/{id}/check-out - saída (retorna documento)
- GET /reservations/{id}/document - segunda via

**MessageHandler**
- GET /messages

**ReportHandler**
- GET /reports/occupancy?date=YYYY-MM-DD - CSV com ocupação
- GET /reports/revenue?month=YYYY-MM - CSV com receita

### 5. Lógica de Negócio

#### Validações de Reserva
1. Identificação: exatamente um tipo (CPF, CNPJ, ou Passaporte)
2. Datas: check-out > check-in
3. Hóspedes: 1 a capacidade do quarto
4. Disponibilidade: quarto não pode ter 2 reservas não-canceladas sobrepostas

#### Cálculo de Preço
```
Para cada noite:
  preço = taxaNormal
  se em alta temporada:
    preço = preço * 1.20
  preço = arredondar(preço, HALF_EVEN)

subtotal = soma de todas as noites
subtotal = arredondar(subtotal, HALF_EVEN)

desconto = 0 (se STANDARD) ou 15% do subtotal (se NON_REFUNDABLE)
desconto = arredondar(desconto, HALF_EVEN)

total = subtotal - desconto
total = arredondar(total, HALF_EVEN)
```

#### Alta Temporada
- Julho (inteiro)
- 15 de dezembro a 28 de fevereiro

#### Estados da Reserva
```
PENDING (criada)
  ├─ confirmação → CONFIRMED (com pagamento)
  └─ cancelamento → CANCELLED (sem reembolso)

CONFIRMED (paga)
  ├─ check-in → CHECKED_IN
  └─ cancelamento → CANCELLED (com reembolso se STANDARD)

CHECKED_IN
  ├─ check-out → CHECKED_OUT
  └─ (não pode ser cancelada)

CHECKED_OUT (finalizada)

CANCELLED (cancelada)
```

#### Reembolso
- STANDARD + CONFIRMED + Cancelamento = reembolsa tudo
- NON_REFUNDABLE + qualquer estado + Cancelamento = sem reembolso
- PENDING + Cancelamento = sem reembolso (nunca pagou)

#### Documentos Gerados

**Comprovante (CPF)**
```
Comprovante de reserva nº X
Hóspede: Nome (CPF xxx.xxx.xxx-xx)
Quarto: 201 (DELUXE)
Entrada: DD/MM/YYYY | Saída: DD/MM/YYYY | N diárias
Diárias: R$ X.XXX,XX
[Desconto: R$ X.XXX,XX] (se houver)
Total: R$ X.XXX,XX
```

**Fatura (CNPJ)**
```
Fatura de hospedagem nº X
Empresa: Nome (CNPJ xx.xxx.xxx/xxxx-xx)
Hóspede: Nome
Quarto: 201 (DELUXE)
Entrada: DD/MM/YYYY | Saída: DD/MM/YYYY | N diárias
Diárias: R$ X.XXX,XX
[Desconto: R$ X.XXX,XX] (se houver)
Total: R$ X.XXX,XX
ISS retido (5%): R$ X.XXX,XX
```

**Booking Receipt (Passaporte)**
```
Booking receipt no. X
Guest: Name (passport XXXXXXX, Country)
Room: 201 (DELUXE)
Check-in: YYYY-MM-DD | Check-out: YYYY-MM-DD | N nights
Nights: BRL X,XXX.XX
[Discount: BRL X,XXX.XX] (if any)
Total: BRL X,XXX.XX
```

### 6. Relatórios (CSV)

**Ocupação (date=2027-07-10)**
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-07-10,2027-07-12
301,SUITE,2,2027-07-10,2027-07-15
total,2/5
```

Conta apenas quartos com reservas CONFIRMED, CHECKED_IN ou CHECKED_OUT naquela noite.
Entrada naquela noite = ocupado. Saída naquela noite = não ocupado.

**Financeiro (month=2027-07)**
```
reservation,room,check_in,check_out,status,amount
1,201,2027-07-10,2027-07-12,CHECKED_OUT,599.76
2,301,2027-07-10,2027-07-15,CONFIRMED,1159.80
total,,,,1759.56
```

Inclui todas as reservas que INICIARAM no mês e dejaram dinheiro:
- CONFIRMED, CHECKED_IN, CHECKED_OUT
- CANCELLED (se recebeu pagamento e não reembolsou tudo)

Amount = amountPaid - refund (se houver)

## Fluxo de Uso

### 1. Criar Reserva
- Cliente faz POST /reservations
- Sistema valida tudo
- Cria reserva em status PENDING (sem pagamento)

### 2. Confirmar Reserva
- Cliente faz POST /reservations/{id}/confirmation com amountPaid
- Sistema atualiza status para CONFIRMED
- Sistema gera documento apropriado
- Sistema envia email de confirmação

### 3. Check-in
- Recepção faz POST /reservations/{id}/check-in
- Sistema muda status para CHECKED_IN

### 4. Check-out
- Recepção faz POST /reservations/{id}/check-out
- Sistema retorna documento do check-out
- Status muda para CHECKED_OUT

### 5. Cancelamento
- Qualquer hora antes de check-in
- POST /reservations/{id}/cancellation
- Cálculo automático de reembolso conforme tipo

### 6. Relatórios
- Recepção consulta ocupação: GET /reports/occupancy?date=...
- Contador consulta financeiro: GET /reports/revenue?month=...
- Ambos em CSV para import em planilhas

## Tecnologias

- **Java 21** - Linguagem
- **Javalin 6.7.0** - Framework web
- **PostgreSQL 18** - Banco de dados
- **Jackson** - JSON marshalling
- **Maven** - Build tool
- **Junit 4** - Testing
- **Docker** - Containerização

## Segurança

- Validação de todos os inputs
- Prepared statements para SQL (previne SQL injection)
- Nenhum erro sensível é exposto na API
- Códigos de erro padronizados (INVALID_REQUEST, etc)

## Performance

- Índices automáticos em chaves primárias
- Consultas otimizadas (datas como parâmetros)
- Cálculos em memória (não há loops pesados)
- Mensagens e documentos gerados sob demanda

## Extensibilidade

Sistema foi desenhado para fácil extensão:
- Adicionar novo tipo de identificação: editar validateIdentification() e DocumentService
- Adicionar novo tipo de documento: editar generateDocument()
- Modificar temporada: editar isHighSeason()
- Novo endpoint: novo handler + rota na Application
