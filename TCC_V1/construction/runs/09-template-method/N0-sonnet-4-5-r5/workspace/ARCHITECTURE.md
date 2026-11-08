# Arquitetura do Sistema

## Visão Geral

O sistema foi construído seguindo uma arquitetura em camadas, com separação clara de responsabilidades:

```
┌─────────────────────────────────────────┐
│         API Layer (Javalin)             │
│    ReservationController                │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         Service Layer                   │
│  ReservationService  │  PricingService  │
│     ReportService                       │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Repository Layer (JDBC)            │
│  ReservationRepository │ RoomRepository │
│    MessageRepository                    │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        PostgreSQL Database              │
└─────────────────────────────────────────┘
```

## Camadas

### 1. Domain Layer (`domain/`)
Contém as entidades de negócio e enums:
- `Room`: Representa um quarto da pousada
- `Reservation`: Representa uma reserva
- `Message`: Representa uma mensagem enviada
- `RoomType`: Enum com tipos de quarto (STANDARD, DELUXE, SUITE)
- `RatePlan`: Enum com planos de tarifa (STANDARD, NON_REFUNDABLE)
- `ReservationStatus`: Enum com estados da reserva

### 2. Repository Layer (`repository/`)
Responsável pela persistência de dados usando JDBC puro:
- `DatabaseInitializer`: Cria as tabelas do banco de dados
- `JdbcRoomRepository`: Persistência de quartos
- `JdbcReservationRepository`: Persistência de reservas
- `JdbcMessageRepository`: Persistência de mensagens

**Decisão de design**: Uso de JDBC puro em vez de JPA/Hibernate para ter controle total sobre as queries e evitar a complexidade adicional de um ORM.

### 3. Service Layer (`service/`)
Contém a lógica de negócio:

**PricingService**: Cálculo de preços
- Identifica alta temporada (15/12 a fim de fev + julho inteiro)
- Aplica acréscimo de 20% na alta temporada
- Calcula desconto de 15% para plano não reembolsável
- Usa `RoundingMode.HALF_EVEN` (arredondamento bancário)

**ReservationService**: Gestão de reservas
- Criação de reservas com validações
- Transições de estado (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT)
- Cancelamento com cálculo de reembolso
- Geração de comprovantes
- Envio de e-mails (armazenados em memória)

**ReportService**: Geração de relatórios CSV
- Relatório de ocupação (por dia)
- Relatório financeiro (por mês)
- Relatório de taxa de turismo (R$ 4,50 por hóspede/noite)
- Relatório por plano de tarifa

**Extensibilidade dos Relatórios**: Todos os relatórios seguem o mesmo padrão (título + cabeçalho + linhas + total), facilitando a adição de novos relatórios e modificações futuras centralizadas.

### 4. API Layer (`api/`)
Exposição HTTP usando Javalin 6.7.0:

**ReservationController**: Manipula todas as requisições HTTP
- Validação de entrada
- Conversão entre DTOs e entidades de domínio
- Tratamento de exceções com códigos de erro apropriados

## Regras de Negócio Implementadas

### Cálculo de Preços
1. Cada noite é calculada individualmente com sua própria taxa
2. Alta temporada adiciona 20% à taxa base
3. Subtotal = soma das taxas de todas as noites
4. Desconto = 15% do subtotal (apenas NON_REFUNDABLE)
5. Total = subtotal - desconto
6. Arredondamento bancário (HALF_EVEN) em cada etapa

### Máquina de Estados da Reserva
```
PENDING ──────────────> CONFIRMED ──────> CHECKED_IN ──────> CHECKED_OUT
   │                                            
   └──────> CANCELLED <───────────┘
```

Restrições:
- Cancelamento só pode ocorrer antes do check-in
- Check-in só pode ocorrer após confirmação
- Check-out só pode ocorrer após check-in

### Validações
- Número de hóspedes >= 1 e <= capacidade do quarto
- Check-out > Check-in
- Quarto não pode ter reservas sobrepostas (exceto canceladas)
- Campos obrigatórios na criação de reserva

### Reembolso
- STANDARD: devolve valor pago
- NON_REFUNDABLE: não devolve nada
- PENDING: nada a devolver (não foi pago)

## Tecnologias

- **Java 21**: Linguagem principal
- **Javalin 6.7.0**: Framework web leve
- **PostgreSQL 18**: Banco de dados
- **HikariCP**: Pool de conexões
- **Jackson**: Serialização JSON
- **JUnit 5**: Testes unitários
- **Maven**: Build e gerenciamento de dependências

## Inicialização

1. O banco de dados é criado automaticamente na primeira execução
2. Os 5 quartos são inseridos (101, 102, 201, 202, 301)
3. A aplicação fica pronta para receber requisições

## Pontos de Extensão

### Adição de Novos Relatórios
Para adicionar um novo relatório:
1. Adicionar método em `ReportService` seguindo o padrão existente
2. Adicionar rota em `ReservationController`
3. Adicionar endpoint em `App.java`

### Modificação do Formato de Relatórios
Para adicionar uma linha de data no final de todos os relatórios:
1. Criar método helper em `ReportService` (ex: `appendGenerationDate()`)
2. Chamar esse método no final de cada gerador de relatório
3. Todas as mudanças ficam centralizadas

### Novos Estados de Reserva
Adicionar novo valor em `ReservationStatus` e implementar lógica de transição em `ReservationService`.

### Taxa de Turismo Variável
A taxa está como constante em `ReportService`. Para torná-la configurável:
1. Criar tabela de configuração
2. Criar repository para ler configurações
3. Injetar valor em `ReportService`
