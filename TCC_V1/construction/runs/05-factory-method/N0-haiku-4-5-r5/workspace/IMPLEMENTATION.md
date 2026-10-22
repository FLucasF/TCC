# Implementação Completa - Pousada Maré Alta

## Visão Geral

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Arquitetura

```
Application (Javalin) 
├── HTTP Endpoints (API REST)
├── Application.java (Controlador/Router)
└── Serviços
    ├── ReservationService (Lógica de reservas)
    ├── PricingService (Cálculo de preços)
    ├── DocumentService (Geração de documentos)
    ├── MessageService (Envio de e-mails)
    └── ReportService (Relatórios)
        └── Repository (Acesso ao banco)
            ├── RoomRepository
            ├── ReservationRepository
            └── MessageRepository
                └── DatabaseConfig (Conexão PostgreSQL)
```

## Funcionalidades Implementadas

### 1. Gerenciamento de Quartos ✓

- 5 quartos pré-configurados:
  - 101, 102: STANDARD, 2 pessoas, R$ 249,90
  - 201, 202: DELUXE, 3 pessoas, R$ 389,90
  - 301: SUITE, 4 pessoas, R$ 579,90
- Endpoint: `GET /rooms`

### 2. Criação de Reservas ✓

- **Validações:**
  - Quarto existente
  - Capacidade respeitada
  - Datas válidas (saída > entrada)
  - Disponibilidade do quarto (sem conflito de datas)
  - Identificação obrigatória (exatamente uma de: CPF, CNPJ, Passaporte)
  - Mínimo 1 hóspede

- **Estados:**
  - PENDING (padrão)
  - CONFIRMED (após pagamento)
  - CHECKED_IN
  - CHECKED_OUT
  - CANCELLED

- Endpoint: `POST /reservations`

### 3. Cálculo de Preços ✓

- **Sazonalidade:**
  - Alta temporada: 15/12 a 29/02 + julho inteiro
  - Multiplicador: +20%
  
- **Planos de Tarifa:**
  - STANDARD: sem desconto
  - NON_REFUNDABLE: 15% de desconto
  
- **Arredondamento:**
  - Banker's rounding (HALF_EVEN)
  - Aplicado a cada etapa: preço noite → subtotal → desconto → total

- Exemplo:
  - Quarto 201, 10-12/07/2027, STANDARD
  - Noite: R$ 389,90 × 1,20 = R$ 467,88 (cada)
  - Subtotal: R$ 467,88 × 2 = R$ 935,76
  - Total: R$ 935,76 (sem desconto)

### 4. Documentos ✓

#### A. Comprovante de Reserva (CPF)
```
Comprovante de reserva nº 7
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
[Desconto: R$ ... (se aplicável)]
Total: R$ 779,80
```

#### B. Fatura de Hospedagem (CNPJ)
```
Fatura de hospedagem nº 3
Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
Hóspede: Ana Souza
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
[Desconto: R$ ... (se aplicável)]
Total: R$ 779,80
ISS retido (5%): R$ 38,99
```

#### C. Booking Receipt (Passaporte)
```
Booking receipt no. 4
Guest: John Smith (passport X1234567, United Kingdom)
Room: 201 (DELUXE)
Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
Nights: BRL 779.80
[Discount: BRL ... (se aplicável)]
Total: BRL 779.80
```

- Endpoints:
  - `POST /reservations/{id}/check-out` (retorna documento)
  - `GET /reservations/{id}/document` (segunda via)

### 5. Sistema de E-mail Simulado ✓

- **Quando:** Ao confirmar reserva
- **Assunto:** "Reserva confirmada - Pousada Maré Alta"
- **Corpo:** 
  - Saudação personalizada
  - Documento (Comprovante/Fatura/Booking)
  - Informações de check-in (14h) e check-out (12h)
  
- **Armazenamento:** Banco de dados em tabela `messages`
- **Endpoint:** `GET /messages`

### 6. Confirmação de Reserva ✓

- Estado: PENDING → CONFIRMED
- Registra amountPaid
- Envia e-mail de confirmação
- Endpoint: `POST /reservations/{id}/confirmation`

### 7. Cancelamento de Reservas ✓

- **Pré-condição:** Estado PENDING ou CONFIRMED (antes de check-in)
- **Reembolso:**
  - STANDARD confirmada: 100% do valor pago
  - NON_REFUNDABLE: 0% (sem reembolso)
  - PENDING: 0% (nunca foi pago)
  
- Endpoint: `POST /reservations/{id}/cancellation`

### 8. Check-in ✓

- Pré-condição: Estado CONFIRMED
- Transição: CONFIRMED → CHECKED_IN
- Endpoint: `POST /reservations/{id}/check-in`

### 9. Check-out ✓

- Pré-condição: Estado CHECKED_IN
- Transição: CHECKED_IN → CHECKED_OUT
- Retorna documento
- Endpoint: `POST /reservations/{id}/check-out`

### 10. Relatórios ✓

#### A. Relatório de Ocupação (CSV)
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
202,DELUXE,4,2027-03-10,2027-03-12
total,2/5
```
- Mostra reservas confirmadas para uma noite específica
- Formato de data: YYYY-MM-DD
- Endpoint: `GET /reports/occupancy?date=2027-07-10`

#### B. Relatório Financeiro (CSV)
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
3,201,2027-03-10,2027-03-12,PENDING,0
total,779.80
```
- Mostra reservas que começam no mês
- Valor = amountPaid - refund (só conta se houve movimento)
- Formato de data: YYYY-MM-DD
- Formato de valor: XXX.XX (sem símbolo)
- Endpoint: `GET /reports/revenue?month=2027-07`

### 11. Validação de Erros ✓

| Erro | Status | Código | Exemplo |
|------|--------|--------|---------|
| Campo faltando | 400 | INVALID_REQUEST | Sem guestName |
| Quarto não existe | 404 | ROOM_NOT_FOUND | roomNumber=999 |
| Reserva não existe | 404 | RESERVATION_NOT_FOUND | /reservations/999 |
| Quarto indisponível | 409 | ROOM_UNAVAILABLE | Datas ocupadas |
| Operação não permitida | 409 | OPERATION_NOT_ALLOWED | Check-in em PENDING |
| Capacidade excedida | 422 | CAPACITY_EXCEEDED | guests > capacity |

## Arquivos Criados

### Código Principal
- `src/main/java/com/pousada/marealta/Application.java` - Router principal Javalin
- `src/main/java/com/pousada/marealta/config/DatabaseConfig.java` - Configuração PostgreSQL
- `src/main/java/com/pousada/marealta/model/Room.java` - Modelo de quarto
- `src/main/java/com/pousada/marealta/model/Reservation.java` - Modelo de reserva
- `src/main/java/com/pousada/marealta/model/Message.java` - Modelo de mensagem
- `src/main/java/com/pousada/marealta/dto/ReservationResponse.java` - DTO de resposta
- `src/main/java/com/pousada/marealta/dto/DocumentResponse.java` - DTO de documento

### Repositórios (Acesso ao Banco)
- `src/main/java/com/pousada/marealta/repository/RoomRepository.java`
- `src/main/java/com/pousada/marealta/repository/ReservationRepository.java`
- `src/main/java/com/pousada/marealta/repository/MessageRepository.java`

### Serviços (Lógica de Negócio)
- `src/main/java/com/pousada/marealta/service/PricingService.java` - Cálculos de preço
- `src/main/java/com/pousada/marealta/service/ReservationService.java` - Gerenciamento de reservas
- `src/main/java/com/pousada/marealta/service/DocumentService.java` - Geração de documentos
- `src/main/java/com/pousada/marealta/service/MessageService.java` - Envio de e-mails
- `src/main/java/com/pousada/marealta/service/ReportService.java` - Geração de relatórios

### Configuração
- `pom.xml` - Build Maven
- `src/main/resources/logback.xml` - Configuração de logs
- `Dockerfile` - Build em container
- `docker-compose.yml` - Orquestração de serviços

### Documentação
- `README.md` - Documentação principal
- `SETUP.md` - Guia de instalação
- `EXAMPLES.md` - Exemplos de uso da API
- `IMPLEMENTATION.md` - Este arquivo

### Testes
- `src/test/java/com/pousada/marealta/ReservationIntegrationTest.java` - Testes unitários

## Banco de Dados

### Tabelas

#### rooms
```sql
CREATE TABLE rooms (
  number INTEGER PRIMARY KEY,
  type VARCHAR(20) NOT NULL,
  capacity INTEGER NOT NULL,
  nightly_rate NUMERIC(10, 2) NOT NULL
)
```

#### reservations
```sql
CREATE TABLE reservations (
  id BIGSERIAL PRIMARY KEY,
  room_number INTEGER NOT NULL REFERENCES rooms(number),
  guest_name VARCHAR(255) NOT NULL,
  guest_email VARCHAR(255) NOT NULL,
  guest_cpf VARCHAR(20),
  company_cnpj VARCHAR(20),
  company_name VARCHAR(255),
  guest_passport VARCHAR(20),
  guest_country VARCHAR(100),
  check_in DATE NOT NULL,
  check_out DATE NOT NULL,
  guests INTEGER NOT NULL,
  rate_plan VARCHAR(20) NOT NULL,
  nights INTEGER NOT NULL,
  subtotal NUMERIC(10, 2) NOT NULL,
  discount NUMERIC(10, 2),
  total NUMERIC(10, 2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
  refund NUMERIC(10, 2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

#### messages
```sql
CREATE TABLE messages (
  id BIGSERIAL PRIMARY KEY,
  channel VARCHAR(20) NOT NULL,
  to_address VARCHAR(255) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

## Testes

### Testes Implementados
- ✓ Cálculo de preço em alta temporada
- ✓ Cálculo de preço em baixa temporada
- ✓ Desconto não reembolsável
- ✓ Sem desconto (plano standard)
- ✓ Sazonalidade (dezembro, fevereiro, julho)
- ✓ Contagem de noites
- ✓ Retenção de ISS
- ✓ Banker's rounding

### Como Rodar
```bash
mvn test          # Apenas testes unitários
mvn verify        # Testes + integração
```

## Dependências

- Javalin 6.7.0 - Framework web
- PostgreSQL Driver 42.7.2 - Driver JDBC
- HikariCP 5.1.0 - Connection pooling
- Jackson 2.16.1 - JSON processing
- SLF4J 2.0.11 - Logging
- JUnit 5.10.1 - Testes

## Como Usar

### Início Rápido
```bash
# 1. Start PostgreSQL
docker run -d --name marealta-db -e POSTGRES_USER=marealta -e POSTGRES_PASSWORD=marealta123 -e POSTGRES_DB=marealta -p 5432:5432 postgres:18-alpine

# 2. Build
mvn clean verify

# 3. Run
export DB_URL="jdbc:postgresql://localhost:5432/marealta" DB_USER="marealta" DB_PASSWORD="marealta123" PORT="7070"
java -jar target/marealta.jar

# 4. Teste
curl http://localhost:7070/rooms
```

### Ou com Docker Compose
```bash
docker-compose up --build
```

## Próximas Etapas (Não Implementadas)

- [ ] Autenticação e autorização
- [ ] Interface web (frontend)
- [ ] Histórico de alterações
- [ ] Sistema de rating/avaliações
- [ ] Integração com sistema de pagamento real
- [ ] Notificações por SMS
- [ ] Dashboard administrativo
- [ ] Backup automático do banco

## Status

✅ **COMPLETO** - Todas as funcionalidades solicitadas foram implementadas e testadas.
