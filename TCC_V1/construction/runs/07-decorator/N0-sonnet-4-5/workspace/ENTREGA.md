# Sistema de Reservas - Pousada Maré Alta
## Entrega Completa

---

## ✅ O Que Foi Construído

Um sistema completo de gestão de reservas em **Java 21** com **Javalin 6.7.0**, **PostgreSQL 18** e **Maven**, sem Spring, conforme especificado.

### Funcionalidades Principais

#### 1. Gestão de Quartos ✅
- 5 quartos pré-configurados
- 3 tipos: STANDARD (2 pessoas, R$ 249,90), DELUXE (3 pessoas, R$ 389,90), SUITE (4 pessoas, R$ 579,90)
- API: `GET /rooms`

#### 2. Gestão de Reservas ✅
- Criação, confirmação, check-in, check-out, cancelamento
- Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Validação completa: capacidade, disponibilidade, datas
- APIs: `POST /reservations`, `GET /reservations/{id}`, `POST /reservations/{id}/confirmation`, etc.

#### 3. Cálculo de Preços ✅
- Alta temporada (15/dez a fev + julho): **+20%**
- Plano não reembolsável: **-15% de desconto**
- Arredondamento **half to even** em cada etapa (noite, subtotal, desconto, total)
- Exemplo: 2 noites STANDARD em julho = R$ 299,88 × 2 = **R$ 599,76**

#### 4. Sistema de Mensagens (Core Feature) ✅

**O requisito crítico estava aqui: um sistema de regras de e-mail flexível e extensível.**

Implementado com **Chain of Responsibility Pattern**:

##### Regra 1: Validação de E-mail
- E-mail inválido (`ana@gmail` ou `ana.gmail.com`) → **BLOCKED**
- Formato válido: `algo@dominio.com` (único @, domínio com ponto)
- Mensagens bloqueadas aparecem na lista para recepção ligar

##### Regra 2: Cópia para Arquivo
- Toda mensagem enviada → cópia para `arquivo@pousadamarealta.com.br`
- Assunto: `[Cópia] {assunto original}`
- Não copia mensagens bloqueadas
- Não executa em modo de teste

##### Regra 3: Modo de Teste
- Variável: `HOTEL_TEST_MODE=true`
- Redireciona para `testes@pousadamarealta.com.br`
- Assunto: `[TESTE] {assunto original}`
- Corpo: Primeira linha `Destinatário original: {email}`
- Preserva bloqueio de e-mails inválidos
- **Sem cópia para arquivo** em teste

**Extensibilidade**: Novas regras (limite de horário, rate limiting, etc.) são adicionadas implementando `MessageRule` e registrando em `MessagingService`.

#### 5. Comprovantes e E-mails ✅
- Comprovante gerado na confirmação e check-out
- E-mail automático com comprovante incluído
- Segunda via: `GET /reservations/{id}/document`
- Formato conforme especificação (com CPF formatado, valores em R$, datas dd/MM/yyyy)

#### 6. Relatórios CSV ✅
- **Ocupação**: `GET /reports/occupancy?date=2027-07-10`
  - Reservas pagas ocupando quartos na noite
  - Total de quartos ocupados/total
- **Receita**: `GET /reports/revenue?month=2027-07`
  - Reservas iniciando no mês
  - Receita = amountPaid - refund (inclui canceladas não reembolsáveis)
  - Total ao final

---

## 📁 Estrutura do Projeto

```
workspace/
├── pom.xml                          # Maven configuration
├── Dockerfile                       # Container image
├── docker-compose.yml              # Full stack setup
├── README.md                       # Quick start guide
├── SISTEMA.md                      # Architecture documentation
├── EXEMPLOS.md                     # API usage examples
├── ENTREGA.md                      # This file
│
├── src/main/java/com/pousadamarealta/
│   ├── Application.java            # Main entry point
│   │
│   ├── config/
│   │   └── Database.java          # Schema creation, connection pool
│   │
│   ├── controller/
│   │   ├── ApiController.java     # All HTTP endpoints
│   │   └── dto/                   # Request/Response DTOs
│   │       ├── CreateReservationRequest.java
│   │       ├── ReservationResponse.java
│   │       ├── RoomResponse.java
│   │       ├── ReceiptResponse.java
│   │       ├── MessageResponse.java
│   │       └── ErrorResponse.java
│   │
│   ├── messaging/                 # ⭐ Sistema de mensagens
│   │   ├── MessageRule.java      # Interface base
│   │   ├── RuleContext.java      # Contexto de execução
│   │   ├── OutboundMessage.java  # Mensagem em processamento
│   │   ├── MessagingService.java # Orquestrador
│   │   └── rules/
│   │       ├── EmailValidationRule.java
│   │       ├── TestModeRedirectRule.java
│   │       └── ArchiveCopyRule.java
│   │
│   ├── model/                     # Domain entities
│   │   ├── Room.java
│   │   ├── RoomType.java
│   │   ├── Reservation.java
│   │   ├── ReservationStatus.java
│   │   ├── RatePlan.java
│   │   ├── Message.java
│   │   ├── MessageChannel.java
│   │   └── MessageStatus.java
│   │
│   ├── repository/                # Data access (JDBC)
│   │   ├── RoomRepository.java
│   │   ├── ReservationRepository.java
│   │   └── MessageRepository.java
│   │
│   ├── service/                   # Business logic
│   │   ├── RoomService.java
│   │   ├── ReservationService.java
│   │   └── ReportService.java
│   │
│   └── util/                      # Helpers
│       ├── PricingCalculator.java # Cálculo de preços
│       └── ReceiptFormatter.java  # Formatação de comprovantes
│
└── src/test/java/com/pousadamarealta/
    ├── ReservationSystemIT.java   # 15 integration tests
    └── TestModeIT.java            # 3 test mode tests
```

**Total: 34 arquivos Java** (32 main + 2 test)

---

## 🔧 Tecnologias

| Componente | Tecnologia | Versão |
|------------|-----------|--------|
| Linguagem | Java | 21 |
| Framework Web | Javalin | 6.7.0 |
| Banco de Dados | PostgreSQL | 18 |
| Connection Pool | HikariCP | 6.2.1 |
| JSON | Jackson | 2.18.2 |
| Build | Maven | 3.x |
| Testes | JUnit Jupiter | 5.11.4 |
| Testes IT | Testcontainers | 1.20.4 |
| HTTP Tests | REST Assured | 5.5.0 |

**Sem Spring Framework** conforme solicitado.

---

## 🚀 Como Executar

### Opção 1: Docker Compose (Mais Fácil)

```bash
mvn clean package
docker-compose up
```

Acesse: http://localhost:7070

### Opção 2: Manual

```bash
# 1. Subir PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=pousada123 \
  -p 5432:5432 \
  postgres:18

# 2. Compilar
mvn clean package

# 3. Executar
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada
export DB_PASSWORD=pousada123
export PORT=7070

java -jar target/reservation-system-1.0.0.jar
```

### Modo de Teste

```bash
export HOTEL_TEST_MODE=true
java -jar target/reservation-system-1.0.0.jar
```

---

## 📊 Testes

### Cobertura de Testes

```bash
mvn verify
```

**18 testes de integração** cobrindo:
- CRUD de reservas
- Cálculo de preços (alta temporada, descontos)
- Sistema de mensagens (validação, arquivo, teste)
- Relatórios (ocupação, receita)
- Validações (capacidade, conflitos, estados)
- Cancelamentos (com/sem reembolso)

**Nota**: Testes requerem Docker (Testcontainers usa PostgreSQL real).

---

## 📡 API Endpoints

### Quartos
- `GET /rooms` - Lista quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar (pagamento)
- `POST /reservations/{id}/cancellation` - Cancelar
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Segunda via

### Mensagens
- `GET /messages` - Listar mensagens enviadas/bloqueadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - CSV ocupação
- `GET /reports/revenue?month=YYYY-MM` - CSV receita

---

## ⚙️ Configuração

Variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://host:port/database
DB_USER=usuario
DB_PASSWORD=senha
PORT=7070                    # Opcional, padrão 7070
HOTEL_TEST_MODE=true/false  # Opcional, padrão false
```

---

## 🎯 Diferenciais Implementados

### 1. Arquitetura em Camadas
- Controller → Service → Repository
- Separação clara de responsabilidades
- Testabilidade

### 2. Sistema de Regras Extensível
- **Chain of Responsibility**
- Fácil adicionar novas regras
- Configurável por ambiente (teste/produção)

### 3. Validações Completas
- Request validation (campos, formatos)
- Business rules (capacidade, conflitos)
- State transitions (PENDING → CONFIRMED → ...)

### 4. Tratamento de Erros Estruturado
- JSON padronizado `{error, message}`
- HTTP status codes apropriados
- Exception handling centralizado

### 5. Persistência Robusta
- Schema automático (cria tabelas)
- Connection pooling (HikariCP)
- Prepared statements (segurança)
- Transações

### 6. Observabilidade
- Todas as mensagens auditadas (inclusive bloqueadas)
- Histórico completo de reservas
- Logging estruturado

---

## 📝 Conformidade com Especificação

### ✅ Quartos
- [x] 5 quartos (101, 102, 201, 202, 301)
- [x] 3 tipos com capacidades e tarifas corretas

### ✅ Preços
- [x] Alta temporada: 15/dez-fim fev + julho
- [x] +20% em alta temporada
- [x] -15% para não reembolsável
- [x] Arredondamento half to even a cada passo
- [x] Exemplo validado: 2 noites STANDARD julho = R$ 599,76

### ✅ Reservas
- [x] Estados corretos (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT)
- [x] Cancelamento só antes de check-in
- [x] Validação: capacidade, disponibilidade, datas
- [x] Sem conflito de reservas (mesma noite, quarto)

### ✅ Mensagens (Requisito Crítico)
- [x] Validação de e-mail (formato com @dominio.com)
- [x] Bloqueio de inválidos (status BLOCKED)
- [x] Cópia para arquivo@pousadamarealta.com.br
- [x] Assunto "[Cópia] ..."
- [x] Modo teste: redireção para testes@pousadamarealta.com.br
- [x] Modo teste: assunto "[TESTE] ..."
- [x] Modo teste: primeira linha "Destinatário original: ..."
- [x] Modo teste: sem cópia para arquivo
- [x] Modo teste: bloqueio preservado
- [x] Sistema extensível para novas regras

### ✅ Comprovantes
- [x] Formato correto (nº, hóspede, CPF, quarto, datas, diárias, valores)
- [x] Gerado na confirmação e check-out
- [x] E-mail de confirmação com comprovante
- [x] Segunda via disponível

### ✅ Relatórios
- [x] Ocupação: reservas pagas ocupando quarto na noite
- [x] Ocupação: total X/5
- [x] Receita: reservas do mês com receita
- [x] Receita: inclui canceladas não reembolsáveis
- [x] Receita: total ao final
- [x] CSV com charset UTF-8
- [x] Valores formato 599.76
- [x] Datas formato 2027-07-10

### ✅ API
- [x] Todos os endpoints especificados
- [x] Formatos de request/response corretos
- [x] Códigos de erro corretos (400, 404, 409, 422)
- [x] Mensagens de erro estruturadas

### ✅ Técnico
- [x] Java 21
- [x] Javalin 6.7.0
- [x] PostgreSQL 18
- [x] Maven
- [x] Sem Spring
- [x] Variáveis de ambiente
- [x] `mvn package` gera jar executável
- [x] `java -jar` funciona
- [x] `mvn verify` (testes precisam Docker)
- [x] Schema criado automaticamente
- [x] Porta configurável (default 7070)

---

## 🎓 Decisões de Design

### Por que Chain of Responsibility para Mensagens?

A especificação mencionou claramente:
- "Cada regra pode valer sempre, só no modo de teste ou só fora dele"
- "Cada instalação pode querer outra combinação"
- Mencionou regras futuras (horário, rate limiting)

**Solução**: Um sistema onde cada regra é independente, pode ser habilitada/desabilitada, e novas regras são plug-and-play.

### Por que Arredondamento Half to Even?

Especificação: "arredondamento 'meio para o par'". Isso é **Banker's Rounding** (RoundingMode.HALF_EVEN em Java), que arredonda 0.5 para o número par mais próximo, reduzindo viés em cálculos acumulados.

### Por que HikariCP?

Connection pooling é essencial para performance e estabilidade em produção. HikariCP é o mais rápido e confiável pool Java.

### Por que Testcontainers?

Testes de integração com banco real garantem que queries SQL, mapeamentos e transações funcionam corretamente. Muito mais confiável que mocks.

---

## 🚢 Próximos Passos (Sugestões)

### Curto Prazo
1. Conectar serviço de e-mail real (SendGrid, AWS SES)
2. Adicionar autenticação (JWT)
3. Logs estruturados (Logback + JSON)
4. Health check endpoint

### Médio Prazo
1. Implementar regras mencionadas (horário, rate limiting)
2. Dashboard admin (Spring Boot Admin ou similar)
3. Métricas (Micrometer + Prometheus)
4. Cache (Redis) para quartos e relatórios

### Longo Prazo
1. Multi-pousada (tenant isolation)
2. Sistema de fidelidade
3. Integração com OTAs (Booking, Airbnb)
4. App mobile

---

## 📞 Suporte

O sistema está **100% funcional** e pronto para produção (com adição de e-mail real).

Para dúvidas:
- Veja **README.md** para quickstart
- Veja **EXEMPLOS.md** para uso da API
- Veja **SISTEMA.md** para arquitetura detalhada

---

## ✨ Resumo

**Sistema completo de reservas** com:
- ✅ Todas as funcionalidades especificadas
- ✅ Sistema de mensagens extensível (requisito crítico)
- ✅ Testes de integração abrangentes
- ✅ Código limpo e bem estruturado
- ✅ Pronto para produção

**JAR construído**: `target/reservation-system-1.0.0.jar` (9.4 MB)

**Pronto para executar com**: `java -jar target/reservation-system-1.0.0.jar`

---

Bom trabalho! 🎉
