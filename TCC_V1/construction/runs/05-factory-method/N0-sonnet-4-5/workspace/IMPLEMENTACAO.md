# Resumo da Implementação - Pousada Maré Alta

## Sistema Completo Implementado

✅ **Backend Java 21 com Javalin 6.7.0** (sem Spring)
✅ **Banco de dados PostgreSQL 18** com criação automática de schema
✅ **Três tipos de documentos** implementados desde o primeiro dia
✅ **Testes unitários** com cobertura das regras de negócio
✅ **Testes de integração** prontos (requerem Docker)
✅ **Build com Maven** gerando JAR executável autônomo

## Estrutura do Projeto

```
pousada-mare-alta/
├── src/main/java/br/com/marealte/
│   ├── Main.java                           # Ponto de entrada
│   ├── controller/                         # Controladores REST
│   │   ├── MessageController.java
│   │   ├── ReportController.java
│   │   ├── ReservationController.java
│   │   └── RoomController.java
│   ├── database/                           # Setup do banco
│   │   ├── Database.java
│   │   └── DatabaseSetup.java
│   ├── dto/                                # DTOs de request/response
│   │   ├── CreateReservationRequest.java
│   │   └── ErrorResponse.java
│   ├── model/                              # Modelos de domínio
│   │   ├── Document.java
│   │   ├── Message.java
│   │   ├── RatePlan.java
│   │   ├── Reservation.java
│   │   ├── ReservationStatus.java
│   │   ├── Room.java
│   │   └── RoomType.java
│   ├── repository/                         # Acesso a dados
│   │   ├── MessageRepository.java
│   │   ├── ReservationRepository.java
│   │   └── RoomRepository.java
│   ├── routes/                             # Registro de rotas
│   │   └── Routes.java
│   └── service/                            # Lógica de negócio
│       ├── DocumentService.java
│       ├── EmailService.java
│       ├── PricingService.java
│       └── ReportService.java
└── src/test/java/
    ├── PousadaIT.java                      # Testes de integração (20 casos)
    └── service/PricingServiceTest.java     # Testes unitários (10 casos)
```

## Funcionalidades Implementadas

### 1. Três Tipos de Identificação e Documentos

#### CPF (Pessoa Física)
- Comprovante em português
- Formato: "Comprovante de reserva nº X"
- Mostra: CPF, dados em português, valores em R$

#### CNPJ (Empresa)
- Fatura de hospedagem
- Formato: "Fatura de hospedagem nº X"
- Mostra: CNPJ, razão social, hóspede, ISS retido (5%)

#### Passaporte (Estrangeiro)
- Booking receipt em inglês
- Formato: "Booking receipt no. X"
- Mostra: passport, país, valores em BRL com formatação US, datas ISO

### 2. Cálculo de Preços

- **Alta temporada**: 15/12 a fim de fevereiro + julho inteiro (+20%)
- **Arredondamento**: HALF_EVEN (meio para o par) em cada etapa
- **Desconto NON_REFUNDABLE**: 15% sobre o subtotal
- **Cálculo**: diária por diária → soma → desconto → total

### 3. Fluxo de Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
         ↓
    CANCELLED (somente antes do check-in)
```

### 4. Regras de Negócio

✅ Validação de identificação: exatamente uma das três formas
✅ Disponibilidade de quarto: sem sobreposição de datas
✅ Capacidade: guests >= 1 e <= capacity
✅ Cancelamento: somente PENDING ou CONFIRMED
✅ Reembolso: STANDARD devolve, NON_REFUNDABLE não devolve
✅ Email de confirmação: enviado ao confirmar reserva

### 5. Relatórios CSV

**Ocupação por Data**
- Formato: room,type,reservation,check_in,check_out
- Total: "total,X/5"
- Considera: reservas pagas (CONFIRMED, CHECKED_IN, CHECKED_OUT)
- Regra: quem entra conta, quem sai não

**Receita por Mês**
- Formato: reservation,room,check_in,check_out,status,amount
- Total: "total,X.XX"
- Considera: reservas que começam no mês
- Valor: amountPaid - refund

## Testes

### Unitários (10 casos)
- ✅ Identificação de alta temporada
- ✅ Cálculo de preços com arredondamento
- ✅ Verificação de HALF_EVEN rounding

### Integração (20 casos)
- ✅ Listar quartos
- ✅ Criar reserva com CPF, CNPJ e Passaporte
- ✅ Validações de identificação
- ✅ Cálculo de alta temporada
- ✅ Validações de negócio (capacidade, disponibilidade)
- ✅ Fluxo completo (criação → confirmação → check-in → check-out)
- ✅ Documentos (comprovante, fatura, receipt)
- ✅ Cancelamento com/sem reembolso
- ✅ Relatórios CSV

## Como Usar

### 1. Subir Banco de Dados
```bash
docker-compose up -d postgres
```

### 2. Executar Aplicação
```bash
# Via Maven
mvn clean package
java -jar target/pousada-mare-alta-1.0.0.jar

# Ou direto
mvn exec:java -Dexec.mainClass="br.com.marealte.Main"
```

### 3. Testar API
```bash
./test-api.sh
```

### 4. Rodar Testes
```bash
# Unitários
mvn test

# Integração (requer Docker)
mvn verify
```

## Detalhes Técnicos

### Arredondamento Bancário (HALF_EVEN)
- 10.125 → 10.12 (arredonda para baixo, par mais próximo)
- 10.135 → 10.14 (arredonda para cima, par mais próximo)
- Usado em TODOS os cálculos monetários

### Alta Temporada
```java
// Julho inteiro
mês == JULY

// 15/12 até fim de fevereiro
(mês == DECEMBER && dia >= 15) || mês == JANUARY || mês == FEBRUARY
```

### Formatação de Valores

**Português (CPF/CNPJ)**
- R$ 1.234,56 (ponto como separador de milhar, vírgula decimal)
- Datas: dd/MM/yyyy

**Inglês (Passaporte)**
- BRL 1,234.56 (vírgula como separador de milhar, ponto decimal)
- Datas: yyyy-MM-dd

### Banco de Dados
- Criação automática de tabelas na inicialização
- População inicial dos 5 quartos
- SERIAL para IDs auto-incrementais
- DECIMAL(10,2) para valores monetários

## Endpoints Implementados

Todos os 11 endpoints especificados:
1. GET /rooms
2. POST /reservations
3. GET /reservations/{id}
4. POST /reservations/{id}/confirmation
5. POST /reservations/{id}/cancellation
6. POST /reservations/{id}/check-in
7. POST /reservations/{id}/check-out
8. GET /reservations/{id}/document
9. GET /messages
10. GET /reports/occupancy?date=YYYY-MM-DD
11. GET /reports/revenue?month=YYYY-MM

## Códigos de Erro Implementados

Todos os 6 códigos especificados:
- 400 INVALID_REQUEST
- 404 ROOM_NOT_FOUND
- 404 RESERVATION_NOT_FOUND
- 409 ROOM_UNAVAILABLE
- 409 OPERATION_NOT_ALLOWED
- 422 CAPACITY_EXCEEDED

## Pronto para Produção

✅ JAR executável autônomo (9.3 MB)
✅ Variáveis de ambiente configuráveis
✅ Logs apropriados
✅ Tratamento de erros completo
✅ Validações de entrada
✅ Testes automatizados
✅ Docker pronto
✅ Documentação completa
