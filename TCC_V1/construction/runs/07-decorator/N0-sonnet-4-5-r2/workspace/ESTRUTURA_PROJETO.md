# Estrutura do Projeto

```
pousada-mare-alta/
│
├── pom.xml                         # Configuração Maven
├── README.md                       # Visão geral e setup
├── ARCHITECTURE.md                 # Design do sistema de mensagens
├── EXAMPLES.md                     # Exemplos de uso da API
├── GUIA_NOVAS_REGRAS.md           # Como adicionar novas regras
├── SISTEMA_IMPLEMENTADO.md        # Checklist completo
├── .env.example                   # Variáveis de ambiente de exemplo
├── .gitignore                     # Arquivos ignorados pelo git
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/pousadamarealta/
│   │           │
│   │           ├── Application.java                    # 🚀 Classe principal
│   │           │
│   │           ├── controller/                         # 🌐 API REST
│   │           │   ├── ApiController.java              # Controller principal
│   │           │   └── dto/
│   │           │       ├── CreateReservationRequest.java
│   │           │       └── ErrorResponse.java
│   │           │
│   │           ├── domain/                             # 📦 Modelo de Domínio
│   │           │   ├── model/
│   │           │   │   ├── Room.java                   # Quarto
│   │           │   │   ├── RoomType.java               # STANDARD/DELUXE/SUITE
│   │           │   │   ├── Reservation.java            # Reserva
│   │           │   │   ├── ReservationStatus.java      # Estados da reserva
│   │           │   │   ├── RatePlan.java               # Planos de tarifa
│   │           │   │   ├── Message.java                # Mensagem
│   │           │   │   ├── MessageChannel.java         # EMAIL
│   │           │   │   ├── MessageStatus.java          # SENT/BLOCKED
│   │           │   │   └── Document.java               # Comprovante
│   │           │   │
│   │           │   ├── repository/                     # Interfaces
│   │           │   │   ├── RoomRepository.java
│   │           │   │   ├── ReservationRepository.java
│   │           │   │   └── MessageRepository.java
│   │           │   │
│   │           │   └── exception/
│   │           │       └── ApiException.java
│   │           │
│   │           ├── infrastructure/                     # 💾 Persistência
│   │           │   ├── DatabaseInitializer.java        # Criação de tabelas
│   │           │   ├── InMemoryRoomRepository.java     # Quartos fixos
│   │           │   ├── PostgresReservationRepository.java
│   │           │   └── PostgresMessageRepository.java
│   │           │
│   │           ├── messaging/                          # ⭐ Sistema de Mensagens
│   │           │   ├── MessageProcessor.java           # Interface base
│   │           │   ├── MessageService.java             # Orquestrador
│   │           │   ├── ProcessingContext.java          # Contexto (test mode)
│   │           │   ├── ProcessingResult.java           # Resultado do processamento
│   │           │   │
│   │           │   └── rules/                          # Regras específicas
│   │           │       ├── EmailValidationRule.java    # Valida e-mail
│   │           │       ├── TestModeRule.java           # Modo de teste
│   │           │       └── ArchiveCopyRule.java        # Cópia para arquivo
│   │           │
│   │           └── service/                            # 💼 Lógica de Negócio
│   │               ├── PricingService.java             # Cálculo de preços
│   │               ├── DocumentService.java            # Geração de comprovantes
│   │               ├── ReservationService.java         # Operações de reserva
│   │               └── ReportService.java              # Relatórios CSV
│   │
│   └── test/
│       └── java/
│           └── com/pousadamarealta/
│               ├── ReservationSystemIT.java            # Testes principais
│               └── TestModeIT.java                     # Testes modo de teste
│
└── target/
    └── reservas-1.0.0.jar                             # 📦 JAR executável

```

## Descrição dos Componentes

### 🚀 Application
- Ponto de entrada do sistema
- Configura e inicializa todos os componentes
- Lê variáveis de ambiente
- Inicia servidor Javalin

### 🌐 Controller (API REST)
- **ApiController**: Rotas HTTP, validação de entrada, tratamento de erros
- **DTOs**: Objetos de transferência de dados

### 📦 Domain (Modelo de Domínio)
- **model**: Entidades e enums do domínio
- **repository**: Interfaces para acesso a dados
- **exception**: Exceções personalizadas

### 💾 Infrastructure (Persistência)
- **DatabaseInitializer**: Cria schema do banco
- **Repositories**: Implementações PostgreSQL e in-memory

### ⭐ Messaging (Sistema Extensível)
- **MessageProcessor**: Interface para regras
- **MessageService**: Pipeline de processamento
- **ProcessingContext**: Contexto compartilhado
- **ProcessingResult**: Resultado de cada regra
- **rules/**: Implementações específicas (validação, teste, cópia)

### 💼 Service (Lógica de Negócio)
- **PricingService**: Cálculos de preço, alta temporada, descontos
- **DocumentService**: Formatação de comprovantes e e-mails
- **ReservationService**: Orquestra operações de reserva
- **ReportService**: Gera relatórios CSV

### 🧪 Tests
- **ReservationSystemIT**: Testes de integração completos
- **TestModeIT**: Testes específicos do modo de teste

## Fluxo de Dados

### Criar Reserva
```
HTTP POST /reservations
    ↓
ApiController.createReservation()
    ↓
ReservationService.createReservation()
    ↓
├─ RoomRepository.findByNumber()
├─ PricingService.calculate*()
└─ ReservationRepository.save()
    ↓
HTTP 201 + Reservation JSON
```

### Confirmar Reserva (com E-mail)
```
HTTP POST /reservations/{id}/confirmation
    ↓
ApiController.confirmReservation()
    ↓
ReservationService.confirmReservation()
    ↓
├─ Atualiza status para CONFIRMED
├─ Define amountPaid = total
├─ DocumentService.generateDocument()
├─ DocumentService.formatDocument()
└─ MessageService.send()
    ↓
    MessageService Pipeline:
    ├─ EmailValidationRule
    ├─ TestModeRule
    └─ ArchiveCopyRule
        ↓
    MessageRepository.save() (1 ou 2 mensagens)
    ↓
HTTP 200 + Reservation JSON
```

### Relatório de Ocupação
```
HTTP GET /reports/occupancy?date=2027-07-10
    ↓
ApiController.getOccupancyReport()
    ↓
ReportService.generateOccupancyReport()
    ↓
├─ ReservationRepository.findPaidReservationsForNight()
└─ Formata como CSV
    ↓
HTTP 200 + CSV
```

## Variáveis de Ambiente

| Variável | Descrição | Default |
|----------|-----------|---------|
| `DB_URL` | JDBC URL do PostgreSQL | - |
| `DB_USER` | Usuário do banco | - |
| `DB_PASSWORD` | Senha do banco | - |
| `PORT` | Porta do servidor | 7070 |
| `HOTEL_TEST_MODE` | Modo de teste (true/false) | false |

## Comandos Úteis

```bash
# Compilar
mvn clean compile

# Empacotar
mvn clean package

# Executar testes
mvn test

# Testes de integração
mvn verify

# Executar aplicação
java -jar target/reservas-1.0.0.jar

# Build sem testes
mvn package -DskipTests
```

## Padrões de Projeto Utilizados

- **Repository Pattern**: Abstração de acesso a dados
- **Service Layer**: Lógica de negócio separada dos controllers
- **Chain of Responsibility**: Pipeline de processamento de mensagens
- **Strategy Pattern**: Diferentes regras de processamento
- **DTO Pattern**: Separação entre API e modelo de domínio
- **Dependency Injection**: Construção manual (sem framework)

## Princípios SOLID

- **Single Responsibility**: Cada classe tem uma responsabilidade clara
- **Open/Closed**: Sistema de mensagens aberto para extensão
- **Liskov Substitution**: Interfaces bem definidas
- **Interface Segregation**: Interfaces pequenas e específicas
- **Dependency Inversion**: Dependências de abstrações, não implementações

## Decisões de Design

1. **Sem Spring**: Conforme solicitado, usando Javalin puro
2. **Rooms em memória**: Quartos são fixos e nunca mudam
3. **Pipeline extensível**: Facilita adição de novas regras
4. **Arredondamento cuidadoso**: HALF_EVEN em cada etapa
5. **Validações no service**: Lógica de negócio centralizada
6. **Erros tipados**: Códigos de erro padronizados
7. **Testes de integração**: Validam comportamento end-to-end
