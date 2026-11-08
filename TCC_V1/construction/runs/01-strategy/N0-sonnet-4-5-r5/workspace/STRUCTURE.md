# Estrutura do Projeto

## Arquivos de Configuração

```
pom.xml                    # Configuração Maven
.env.example              # Exemplo de variáveis de ambiente
create_database.sql       # Script SQL para criar o banco
run.sh                    # Script para executar a aplicação
```

## Código Fonte (src/main/java/com/pousadamarealta/)

### Main
- `Main.java` - Inicialização da aplicação Javalin

### Controllers (REST API)
- `controller/RoomController.java` - Endpoints de quartos
- `controller/ReservationController.java` - Endpoints de reservas
- `controller/MessageController.java` - Endpoints de mensagens
- `controller/ReportController.java` - Endpoints de relatórios

### DTOs (Data Transfer Objects)
- `dto/ErrorResponse.java` - Resposta de erro padronizada
- `dto/ReceiptResponse.java` - Comprovante de reserva

### Models (Entidades de Domínio)
- `model/Room.java` - Entidade Quarto
- `model/RoomType.java` - Enum tipos de quarto
- `model/Reservation.java` - Entidade Reserva
- `model/ReservationStatus.java` - Enum estados da reserva
- `model/RatePlan.java` - Enum planos de tarifa
- `model/Message.java` - Entidade Mensagem

### Repositories (Acesso a Dados)
- `repository/DatabaseManager.java` - Gerenciamento de conexões e inicialização
- `repository/RoomRepository.java` - Acesso aos dados de quartos
- `repository/ReservationRepository.java` - Acesso aos dados de reservas
- `repository/MessageRepository.java` - Acesso aos dados de mensagens

### Services (Lógica de Negócio)
- `service/PricingService.java` - Cálculo de preços e planos de tarifa
- `service/ReservationService.java` - Lógica de reservas e e-mails
- `service/ReportService.java` - Geração de relatórios CSV

## Testes (src/test/java/com/pousadamarealta/)

- `PricingServiceTest.java` - Testes de cálculo de preços

## Documentação

```
README.md                 # Documentação principal
API_EXAMPLES.md          # Exemplos de requisições
CHECKLIST.md             # Checklist de implementação
STRUCTURE.md             # Este arquivo
```

## Arquitetura

```
┌─────────────────────────────────────────┐
│              Cliente HTTP               │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│           Controllers (REST)            │
│  - RoomController                       │
│  - ReservationController                │
│  - MessageController                    │
│  - ReportController                     │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│           Services (Lógica)             │
│  - PricingService                       │
│  - ReservationService                   │
│  - ReportService                        │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│        Repositories (Dados)             │
│  - RoomRepository                       │
│  - ReservationRepository                │
│  - MessageRepository                    │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│         PostgreSQL Database             │
│  - rooms                                │
│  - reservations                         │
│  - messages                             │
└─────────────────────────────────────────┘
```

## Fluxo de uma Reserva

```
1. POST /reservations
   ↓
2. ReservationController.createReservation()
   ↓
3. ReservationService.createReservation()
   ├─ Valida quarto (RoomRepository)
   ├─ Valida plano de tarifa (PricingService)
   ├─ Verifica disponibilidade (ReservationRepository)
   ├─ Calcula preços (PricingService)
   └─ Salva reserva (ReservationRepository)
   ↓
4. Retorna Reservation (status: PENDING)
   ↓
5. POST /reservations/{id}/confirmation
   ↓
6. ReservationService.confirmReservation()
   ├─ Atualiza status para CONFIRMED
   ├─ Define amountPaid = total
   ├─ Gera comprovante
   ├─ Envia e-mail (MessageRepository)
   └─ Atualiza reserva (ReservationRepository)
   ↓
7. Retorna Reservation (status: CONFIRMED)
```

## Principais Características

### 1. Cálculo de Preços (PricingService)

- Arredondamento banker's rounding (HALF_EVEN)
- Alta temporada automática por data
- 5 planos de tarifa implementados
- Extensível para novos planos

### 2. Validações (ReservationService)

- Capacidade do quarto
- Disponibilidade (sem sobreposição)
- Regras específicas por plano
- Transições de estado válidas

### 3. Persistência (Repositories)

- Pool de conexões HikariCP
- Inicialização automática do esquema
- População inicial dos quartos
- Queries otimizadas

### 4. API REST (Controllers)

- JSON com Jackson
- Códigos de erro padronizados
- CSV para relatórios
- Validação de entrada

## Dependências Principais

- **Javalin 6.7.0** - Framework web
- **PostgreSQL JDBC 42.7.4** - Driver do banco
- **HikariCP 5.1.0** - Pool de conexões
- **Jackson 2.18.0** - Serialização JSON
- **SLF4J 2.0.16** - Logging
- **JUnit 5.11.3** - Testes
