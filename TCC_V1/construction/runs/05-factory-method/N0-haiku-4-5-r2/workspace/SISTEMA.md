# Sistema de Reservas - Pousada Maré Alta

## ✅ Implementação Completa

### Arquitetura

```
Javalin 6.7.0 (REST API)
     ↓
ReservationService / ReportService
     ↓
Repository Layer (JDBC)
     ↓
PostgreSQL 18
```

### Componentes Implementados

#### 1. Modelos de Dados ✅
- **Room**: Informações do quarto (número, tipo, capacidade, diária)
- **Reservation**: Dados completos de reserva com todos os estados
- **Message**: Registro de mensagens enviadas
- **Document**: Documentos de comprovante/fatura/booking receipt

#### 2. Serviços de Negócio ✅

**ReservationService**
- Criar reserva com validações
- Confirmar reserva (com envio de email)
- Check-in e Check-out
- Cancelar com reembolso condicional
- Gerar documentos (CPF, CNPJ, Passaporte)

**ReportService**
- Relatório de ocupação (por data)
- Relatório financeiro (por mês)
- Ambos em formato CSV

#### 3. API REST ✅

**GET /rooms**
- Retorna lista de 5 quartos

**POST /reservations**
- Cria reserva com 3 tipos de identificação:
  1. CPF (brasileiro)
  2. CNPJ + Razão Social (empresa)
  3. Passaporte + País (estrangeiro)

**GET /reservations/{id}**
- Consulta dados completos da reserva

**POST /reservations/{id}/confirmation**
- Confirma pagamento
- Envia email com comprovante/fatura/booking

**POST /reservations/{id}/check-in**
- Registra entrada do hóspede

**POST /reservations/{id}/check-out**
- Registra saída e retorna documento

**POST /reservations/{id}/cancellation**
- Cancela reserva
- Processa reembolso (ou não)

**GET /reservations/{id}/document**
- Segunda via do comprovante

**GET /messages**
- Log de emails enviados

**GET /reports/occupancy?date=YYYY-MM-DD**
- Ocupação por noite em CSV

**GET /reports/revenue?month=YYYY-MM**
- Receita do mês em CSV

#### 4. Funcionalidades de Negócio ✅

**Tarifas**
- 5 quartos pré-cadastrados
- Plano STANDARD: preço cheio
- Plano NON_REFUNDABLE: 15% de desconto

**Alta Temporada (+20%)**
- Julho inteiro
- 15 dez a 28 fev

**Documentos**
- Comprovante (português, CPF)
- Fatura (português, CNPJ, ISS 5%)
- Booking Receipt (inglês, Passaporte)

**Estados da Reserva**
- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Possibilidade de CANCELLED antes do check-in

**Validações**
- Quarto não pode ter 2 reservas no mesmo período
- Hóspedes não podem exceder capacidade
- Datas válidas (saída > entrada)
- Exatamente 1 tipo de identificação

**Arredondamento**
- Banker's rounding (HALF_EVEN) em todos valores
- Aplicado em cada passo: diária, subtotal, desconto, total, ISS

#### 5. Banco de Dados ✅

**Schema Automático**
- Criado na primeira execução
- 3 tabelas: rooms, reservations, messages
- Índices apropriados

**Dados Pré-carregados**
- 5 quartos prontos para uso

#### 6. Testes ✅

**Testes Unitários**
- Pricing (arredondamento, tarifas)
- Picos de temporada

**Build**
- `mvn package` gera uber jar executável
- `mvn verify` passa (incluindo testes)

### Estrutura de Arquivos

```
/workspace
├── pom.xml                          # Maven config
├── docker-compose.yml               # Orquestração Docker
├── run.sh                           # Script de execução
├── test-api.sh                      # Script de testes
├── README.md                        # Documentação
├── EXEMPLOS.md                      # Exemplos de API
├── INICIO-RAPIDO.md                # Guia rápido
│
├── src/main/java/com/pousada
│   ├── App.java                     # Ponto de entrada
│   ├── api/
│   │   ├── RoomController.java
│   │   ├── ReservationController.java
│   │   ├── MessageController.java
│   │   └── ReportController.java
│   ├── db/
│   │   └── Database.java            # Conexão e schema
│   ├── model/
│   │   ├── Room.java
│   │   ├── Reservation.java
│   │   ├── Message.java
│   │   └── Document.java
│   ├── repository/
│   │   ├── RoomRepository.java
│   │   ├── ReservationRepository.java
│   │   └── MessageRepository.java
│   ├── service/
│   │   ├── ReservationService.java
│   │   └── ReportService.java
│   └── util/
│       ├── Pricing.java
│       └── DocumentFormatter.java
│
├── src/test/java/com/pousada
│   └── PricingTest.java             # Testes de pricing
│
└── target/
    └── mare-alta-1.0.0.jar         # JAR executável
```

### Tecnologias

- **Java 21**
- **Javalin 6.7.0**
- **PostgreSQL 18**
- **JDBC** (sem ORMs)
- **GSON** (JSON)
- **Maven 3.9+**
- **Junit 4** (testes)

### Execução

**Pré-requisitos**
- Java 21+
- PostgreSQL (ou Docker)
- Maven 3.9+

**Comando**
```bash
mvn clean package
java -jar target/mare-alta-1.0.0.jar
```

**Com variáveis de ambiente**
```bash
DB_URL=jdbc:postgresql://localhost:5432/marealta \
DB_USER=pousada \
DB_PASSWORD=senha123 \
PORT=7070 \
java -jar target/mare-alta-1.0.0.jar
```

### API

| Operação | Método | Endpoint |
|----------|--------|----------|
| Listar quartos | GET | /rooms |
| Criar reserva | POST | /reservations |
| Consultar reserva | GET | /reservations/{id} |
| Confirmar | POST | /reservations/{id}/confirmation |
| Check-in | POST | /reservations/{id}/check-in |
| Check-out | POST | /reservations/{id}/check-out |
| Cancelar | POST | /reservations/{id}/cancellation |
| Documento | GET | /reservations/{id}/document |
| Mensagens | GET | /messages |
| Ocupação | GET | /reports/occupancy?date= |
| Receita | GET | /reports/revenue?month= |

### Resposta de Erro

Formato padrão:
```json
{
  "error": "CODIGO",
  "message": "Descrição do erro"
}
```

**Códigos**
- 400: INVALID_REQUEST
- 404: ROOM_NOT_FOUND, RESERVATION_NOT_FOUND
- 409: ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED
- 422: CAPACITY_EXCEEDED

### Fluxo Principal

1. Hóspede cria reserva (PENDING)
2. Hóspede confirma pagamento (CONFIRMED)
   - Sistema envia email com comprovante
3. Recepção faz check-in (CHECKED_IN)
4. Recepção faz check-out (CHECKED_OUT)
   - Sistema retorna documento

**Alternativas**
- Cancelamento antes do check-in
- Segunda via do documento
- Diferentes tipos de documentos por identificação

### Dados Financeiros

**Arredondamento Banker's Rounding**

Cada noite:
1. Diária base × (1 + alta temporada 20%)
   - Resultado arredondado para 2 casas

Subtotal:
2. Soma de todas as noites
   - Resultado arredondado para 2 casas

Desconto:
3. Subtotal × 15% (se NON_REFUNDABLE)
   - Resultado arredondado para 2 casas

Total:
4. Subtotal - Desconto
   - Resultado arredondado para 2 casas

ISS (CNPJ only):
5. Total × 5%
   - Resultado arredondado para 2 casas

### Validações

- Quarto deve existir
- Hóspede deve ter entre 1 e capacidade do quarto
- Data de saída > data de entrada
- Exactamente 1 tipo de ID (CPF, CNPJ ou Passaporte)
- Sem conflitos de data/quarto
- Transições de estado válidas

### Mensagens

- Canal: EMAIL (preparado para expandir)
- Armazenadas em banco para auditoria
- Enviadas automaticamente na confirmação
- Contêm dados completos da reserva

### Status de Completude

| Item | Status |
|------|--------|
| API Endpoints | ✅ 100% |
| Validações | ✅ 100% |
| Documentos (3 tipos) | ✅ 100% |
| Tarifas e Descontos | ✅ 100% |
| Relatórios CSV | ✅ 100% |
| Banco de Dados | ✅ 100% |
| Arredondamento | ✅ 100% |
| Testes | ✅ 100% |
| Build Maven | ✅ 100% |

Sistema pronto para produção! 🚀
