# Arquitetura do Sistema

## Visão Geral

O Sistema de Reservas da Pousada Maré Alta foi construído com uma arquitetura em camadas, facilitando manutenção, testes e extensão.

```
┌─────────────────────────────────────┐
│       API REST (Javalin)            │  Controllers
├─────────────────────────────────────┤
│       Services (Lógica de Negócio)  │  ReservationService, RoomService, etc
├─────────────────────────────────────┤
│       DAOs (Persistência)            │  ReservationDAO, RoomDAO, etc
├─────────────────────────────────────┤
│       PostgreSQL Database            │
└─────────────────────────────────────┘
```

## Camadas

### 1. API REST (Controllers)

Localizado em `src/main/java/br/marealta/api/`

- **RoomController**: Gerencia listagem de quartos
- **ReservationController**: Gerencia criar, confirmar, cancelar, check-in/out de reservas
- **MessageController**: Lista mensagens (emails) enviadas
- **ReportController**: Gera relatórios de ocupação e receita

Características:
- Tratamento de erros com códigos HTTP apropriados
- Validação de entrada
- Serialização/desserialização JSON automática

### 2. Services (Lógica de Negócio)

Localizado em `src/main/java/br/marealta/service/`

#### ReservationService
- Criação de reservas com cálculo automático de preços
- Confirmação com envio de notificações
- Check-in/Check-out
- Cancelamento com reembolso conforme plano
- Geração de comprovantes

Regras de negócio implementadas:
- Validação de capacidade
- Bloqueio de datas ocupadas
- Cálculo de preços com alta temporada (20% de aumento)
- Aplicação de descontos (NON_REFUNDABLE)
- Arredondamento correto de valores

#### NotificationService
- Registro de handlers de notificação
- Envio de notificações
- Persistência de mensagens no banco

#### DefaultNotificationHandler
- Notificação do hóspede (confirmação)
- Notificação da governança (entrada, saída)
- Notificação do financeiro (pagamento, reembolsos)

#### RoomService
- Listagem de quartos com seus detalhes

#### ReportService
- Geração de relatório de ocupação por data
- Geração de relatório de receita por mês

### 3. DAOs (Persistência)

Localizado em `src/main/java/br/marealta/db/`

- **RoomDAO**: Acesso aos dados de quartos
- **ReservationDAO**: Acesso aos dados de reservas (insert, update, find por ID, por data, por período)
- **MessageDAO**: Acesso aos dados de mensagens (insert, select all)

Características:
- Prepared Statements para segurança
- Mapping automático de ResultSet para objetos Java
- Transações de banco de dados

### 4. Database

`Database.java` é responsável por:
- Inicialização da conexão
- Criação automática de schema na primeira execução
- Gerenciamento do pool de conexões
- Leitura de variáveis de ambiente

## Modelos de Dados

### Room
- number: int
- type: STANDARD, DELUXE, SUITE
- capacity: int
- nightlyRate: BigDecimal

### Reservation
- id: long
- roomNumber: int
- guestName, guestEmail, guestCpf: String
- checkIn, checkOut: LocalDate
- guests: int
- ratePlan: STANDARD ou NON_REFUNDABLE
- nights: int
- subtotal, discount, total: BigDecimal
- status: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
- amountPaid, refund: BigDecimal

### Message
- id: long
- channel: EMAIL (extensível)
- to: String (endereço do destinatário)
- subject, body: String

## Fluxo de Reserva

```
1. Criação (POST /reservations)
   └─ Validação e cálculo de preço
   └─ Status: PENDING

2. Confirmação (POST /reservations/{id}/confirmation)
   └─ Validação de status
   └─ Registro de pagamento
   └─ Notificações para hóspede, governança, financeiro
   └─ Status: CONFIRMED

3. Check-in (POST /reservations/{id}/check-in)
   └─ Validação de status
   └─ Status: CHECKED_IN

4. Check-out (POST /reservations/{id}/check-out)
   └─ Validação de status
   └─ Gera comprovante
   └─ Status: CHECKED_OUT

5. Cancelamento (POST /reservations/{id}/cancellation)
   └─ Validação de status (apenas antes de CHECKED_IN)
   └─ Cálculo de reembolso conforme plano
   └─ Notificações (se estava confirmada)
   └─ Status: CANCELLED
```

## Cálculo de Preços

O sistema calcula preços em 4 passos:

1. **Preço por noite**: nightlyRate × (1.20 se alta temporada, 1.0 caso contrário)
2. **Subtotal**: Soma dos preços noturnos (arredondado)
3. **Desconto**: 15% do subtotal se NON_REFUNDABLE, senão 0 (arredondado)
4. **Total**: subtotal - desconto (arredondado)

Arredondamento: "Meio para o par" (banker's rounding) usando `RoundingMode.HALF_EVEN`

### Exemplo

Quarto STANDARD (R$ 249,90) de 10 a 12 de julho:

```
Noite 1 (10 jul): 249,90 × 1,20 = 299,88
Noite 2 (11 jul): 249,90 × 1,20 = 299,88
Subtotal: 599,76

Se STANDARD:
  Desconto: 0,00
  Total: 599,76

Se NON_REFUNDABLE:
  Desconto: 599,76 × 0,15 = 89,96
  Total: 509,80
```

## Relatórios

### Ocupação (CSV)
Mostra quais quartos estão ocupados em uma data específica:

```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
201,DELUXE,2,2027-03-10,2027-03-13
total,2/5
```

### Receita (CSV)
Mostra receita de reservas que iniciaram em um mês específico:

```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.80
2,201,2027-03-10,2027-03-13,CONFIRMED,779.80
total,1279.60
```

A receita é: amountPaid - refund (se houver).

## Extensibilidade

### Adicionar Novo Handler de Notificação

1. Implementar `NotificationHandler`
2. Registrar em `Main.java` via `notificationService.registerHandler()`

Exemplo:
```java
public class RestaurantNotificationHandler implements NotificationHandler {
    // implementar onReservationConfirmed e onReservationCancelled
}
```

### Adicionar Novo Canal de Notificação

1. Estender enum `MessageChannel`
2. Usar em um novo handler

### Adicionar Novo Tipo de Quarto

1. Estender enum `RoomType`
2. Adicionar entrada na tabela `rooms` (via `Database.initializeRooms()`)

### Adicionar Novo Relatório

1. Criar método em `ReportService`
2. Adicionar endpoint em `ReportController`
3. Chamar o novo endpoint via GET `/reports/...`

## Segurança

- **SQL Injection**: Prevenido com Prepared Statements
- **Validação de entrada**: Todos os parâmetros são validados nos controllers
- **Autenticação**: Não implementada (fora do escopo)
- **Autorização**: Não implementada (fora do escopo)

## Performance

- **Índices**: Criados em `room_number`, `check_in`, `status`
- **Pré-carregamento**: Quartos são carregados uma vez ao iniciar
- **Pool de conexões**: Gerenciado pelo driver PostgreSQL

## Testes

- **Unitários**: `PricingCalculatorTest` - Testa lógica de cálculo
- **Integração**: Não implementados (requer Docker)

Para adicionar testes de integração:
1. Usar TestContainers com PostgreSQL
2. Testar fluxos completos de reserva

## Logs

O sistema usa SLF4J com Logback. Logs são impressos no console.

Para configurar nível de log, edite `src/main/resources/logback.xml` (não incluído).

## Deployment

1. Compilar: `mvn clean package`
2. JAR gerado: `target/pousada-system.jar`
3. Executar: `java -jar target/pousada-system.jar`

Variáveis de ambiente obrigatórias:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

Variáveis de ambiente opcionais:
- `PORT` (padrão: 7070)

## Estrutura de Diretórios

```
/workspace
├── pom.xml                          # Configuração Maven
├── README.md                        # Guia de uso
├── ARCHITECTURE.md                  # Este arquivo
├── NOTIFICATIONS.md                 # Guia de notificações
├── EXAMPLES.md                      # Exemplos de API
├── src/
│   ├── main/
│   │   └── java/br/marealta/
│   │       ├── Main.java
│   │       ├── api/
│   │       ├── db/
│   │       ├── model/
│   │       ├── service/
│   │       └── util/
│   └── test/
│       └── java/br/marealta/
│           └── util/
└── target/
    └── pousada-system.jar           # JAR executável
```

## Próximas Melhorias (Futuro)

1. Autenticação e autorização (OAuth2)
2. API de admin para gerenciar quartos
3. Sistema de cancelamento automático de reservas vencidas
4. Integração com gateway de pagamento
5. SMS como canal de notificação
6. Dashboard web para visualizar ocupação em tempo real
7. Cacheing de dados
8. Busca e filtro de reservas
9. Histórico de alterações em reservas
10. Relatórios mais detalhados
