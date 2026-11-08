# Implementação do Sistema de Reservas - Pousada Maré Alta

## Resumo Executivo

Sistema completo de gerenciamento de reservas para pousada, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL. O sistema cobre todo o fluxo de reservas, de criação até check-out, com notificações automáticas e relatórios.

## Arquitetura e Padrões

### Padrão Observer para Notificações

O sistema implementa o padrão **Observer** para o sistema de notificações, permitindo adicionar novos observadores sem modificar código existente:

- **ReservationObserver**: Interface que define o contrato de observadores
- **GuestNotificationObserver**: Notifica o hóspede quando reserva é confirmada
- **GovernanceNotificationObserver**: Notifica governança (entrada/saída) quando reserva é confirmada ou cancelada
- **FinanceNotificationObserver**: Notifica financeiro quando há pagamento ou reembolso
- **NotificationService**: Gerencia os observadores e dispara eventos

Essa arquitetura permite fácil adição de novos observadores (restaurante, transfer, etc.) sem modificação do código existente.

### Estrutura de Pacotes

```
com.pousada.marealta/
├── api/                     # API REST (Controllers, DTOs, Converters)
│   ├── RoomController.java
│   ├── ReservationController.java
│   ├── MessageController.java
│   ├── ReportController.java
│   ├── ReservationRequest.java
│   ├── ReservationResponse.java
│   ├── ReceiptResponse.java
│   ├── ReservationConverter.java
│   └── ErrorResponse.java
├── domain/                  # Modelos de domínio
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   ├── Receipt.java
│   ├── RatePlan.java (enum)
│   └── ReservationStatus.java (enum)
├── service/                 # Lógica de negócio
│   ├── ReservationService.java      # Fluxo de reservas
│   ├── PricingCalculator.java       # Cálculos de preço
│   ├── ReceiptFormatter.java        # Formatação de comprovantes
│   └── ReportService.java           # Geração de relatórios
├── repository/              # Acesso a dados (JDBC)
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── persistence/             # Configuração de BD
│   ├── Database.java                # Connection pool e inicialização
│   └── Migration.java               # Criação de tabelas
├── notification/            # Sistema de notificações (Observer)
│   ├── ReservationObserver.java
│   ├── GuestNotificationObserver.java
│   ├── GovernanceNotificationObserver.java
│   ├── FinanceNotificationObserver.java
│   └── NotificationService.java
└── App.java                 # Inicialização e roteamento
```

## Funcionalidades Implementadas

### 1. Gerenciamento de Quartos
- 5 quartos pré-configurados (101, 102, 201, 202, 301)
- Tipos: STANDARD, DELUXE, SUITE
- Capacidades: 2, 3, 4 pessoas
- Tarifas: R$ 249,90, R$ 389,90, R$ 579,90

### 2. Cálculos de Preço
- **Alta Temporada**: 15 dez a fim fev + julho inteiro → +20% na tarifa
- **Planos de Tarifa**: 
  - STANDARD: sem desconto
  - NON_REFUNDABLE: -15% desconto
- **Arredondamento**: HALF_EVEN (banker's rounding) em cada passo
  - Tarifa por noite
  - Subtotal (soma das noites)
  - Desconto
  - Total

### 3. Fluxo de Reserva
Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT (ou CANCELLED)

- **PENDING**: Reserva criada, aguardando confirmação
- **CONFIRMED**: Reserva confirmada com pagamento, disparadas notificações
- **CHECKED_IN**: Hóspede fez check-in
- **CHECKED_OUT**: Hóspede fez check-out
- **CANCELLED**: Reserva cancelada (PENDING/CONFIRMED)
  - STANDARD: reembolso total
  - NON_REFUNDABLE: sem reembolso

### 4. Sistema de Notificações
**Observadores atuais:**
- Hóspede: recebe confirmação com comprovante por e-mail
- Governança: notificada de reservas confirmadas (datas, hóspedes) e cancelamentos
- Financeiro: notificado de pagamentos e reembolsos

**Extensibilidade:**
- Novo observador requer apenas implementação de ReservationObserver
- Registro em NotificationService
- Nenhuma modificação em código existente

### 5. Comprovantes
Formato padronizado com:
- Número da reserva
- Dados do hóspede (CPF formatado)
- Quarto (número e tipo)
- Datas e noites
- Diárias (com alta temporada)
- Desconto (se houver)
- Total

Disponível em:
- Email de confirmação (automático)
- GET /reservations/{id}/document (segunda via)

### 6. Relatórios em CSV

**Ocupação (GET /reports/occupancy?date=yyyy-MM-dd)**
- Mostra quartos ocupados em uma data
- Colunas: room, type, reservation, check_in, check_out
- Total de ocupação no final

**Receita (GET /reports/revenue?month=yyyy-MM)**
- Mostra reservas que começam no mês com dinheiro recebido
- Colunas: reservation, room, check_in, check_out, status, amount
- Calcula: amountPaid - refund
- Soma total no final

### 7. Validações
- Datas: checkIn < checkOut
- Hóspedes: >= 1 e <= capacidade do quarto
- Quartos: não pode ter 2 reservas na mesma noite (excluindo CANCELLED)
- Operações: validam estado atual da reserva

### 8. Persistência
- **PostgreSQL 18** com HikariCP connection pool
- **Migração automática**: cria tabelas na primeira execução
- **Tabelas:**
  - rooms: quartos (pré-carregada)
  - reservations: dados completos de cada reserva
  - messages: histórico de e-mails enviados

## API REST

### Endpoints Implementados

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | /rooms | Lista quartos |
| POST | /reservations | Cria reserva |
| GET | /reservations/{id} | Obtém reserva |
| POST | /reservations/{id}/confirmation | Confirma reserva |
| POST | /reservations/{id}/check-in | Faz check-in |
| POST | /reservations/{id}/check-out | Faz check-out |
| POST | /reservations/{id}/cancellation | Cancela reserva |
| GET | /reservations/{id}/document | Segunda via de comprovante |
| GET | /messages | Lista e-mails enviados |
| GET | /reports/occupancy | Relatório de ocupação |
| GET | /reports/revenue | Relatório de receita |

### Códigos de Erro Implementados
- 400: INVALID_REQUEST (campos, formato)
- 404: ROOM_NOT_FOUND, RESERVATION_NOT_FOUND
- 409: ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED
- 422: CAPACITY_EXCEEDED

## Tecnologias

- **Java 21**: Linguagem principal
- **Javalin 6.7.0**: Framework web leve sem Spring
- **Maven**: Build e dependency management
- **PostgreSQL 18**: Banco de dados
- **HikariCP**: Connection pooling
- **Jackson**: JSON processing
- **SLF4J + Logback**: Logging
- **JUnit 5**: Testes unitários

## Build e Deploy

### Compilar
```bash
mvn clean package
```
Gera: `target/marealta-1.0.0.jar`

### Executar
```bash
java -jar target/marealta-1.0.0.jar
```
Variáveis obrigatórias: DB_URL, DB_USER, DB_PASSWORD

### Testes
```bash
mvn test          # Testes unitários
mvn verify        # Verificação completa
```

## Decisões de Design

1. **Sem Spring**: Framework leve Javalin foi escolhido conforme requisito, resultando em código mais direto
2. **JDBC puro**: Evita abstrações desnecessárias, mantém código simples e compreensível
3. **Observer Pattern**: Preparado para crescimento do sistema (restaurante, transfer futuros)
4. **Imutabilidade de Modelos**: Objetos de domínio são imutáveis, mudanças via setters controlados
5. **Separação de Responsabilidades**: Cada pacote com responsabilidade clara
6. **Formatação Centralizada**: ReceiptFormatter centraliza regras de formatação

## Próximas Extensões Possíveis

1. Adicionar novo observador para restaurante (extend ReservationObserver)
2. Adicionar novo observador para transfer aeroporto
3. Integrar envio real de e-mails
4. Dashboard web com Angular/React
5. Histórico de preços
6. Políticas de reembolso personalizadas
7. Integração com sistema de pagamento

## Conclusão

Sistema completo, testado e pronto para produção. Arquitetura extensível usando padrões de design, permitindo crescimento sem reescrever código existente.
