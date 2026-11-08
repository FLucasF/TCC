# Resumo da Implementação - Pousada Maré Alta

## O que foi desenvolvido

Um sistema completo de reservas para a Pousada Maré Alta com Java 21, Javalin 6.7.0, PostgreSQL 18 e Maven.

## Arquitetura

### Camadas
- **Database Layer**: Acesso ao PostgreSQL com criação automática de schema
- **Repository Layer**: ReservationRepository, RoomRepository, MessageRepository
- **Service Layer**: ReservationService (lógica de negócio), ReportService (relatórios)
- **Notification Layer**: Sistema extensível de notificações (GovernanceNotifier, FinanceNotifier)
- **Route Layer**: Endpoints REST usando Javalin
- **Utility Layer**: DateUtils, MoneyUtils (arredondamento "meio para o par")

### Componentes Principais

#### Models
- `Room`: Representação de quartos (101-102 STANDARD, 201-202 DELUXE, 301 SUITE)
- `Reservation`: Controla ciclo completo (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT → CANCELLED)
- `Message`: Armazena mensagens de notificação

#### Notificadores (Sistema Extensível)
```java
ReservationNotifier <- GovernanceNotifier, FinanceNotifier
```

Para adicionar novo departamento (ex: Restaurante):
1. Criar classe `RestaurantNotifier implements ReservationNotifier`
2. Registrar em `Main.java`: `notificationService.register(new RestaurantNotifier(...))`

## Funcionalidades Implementadas

### Reservas
- ✓ Criação (POST /reservations)
- ✓ Consulta (GET /reservations/{id})
- ✓ Confirmação com pagamento (POST /reservations/{id}/confirmation)
- ✓ Cancelamento com reembolso condicional (POST /reservations/{id}/cancellation)
- ✓ Check-in (POST /reservations/{id}/check-in)
- ✓ Check-out com comprovante (POST /reservations/{id}/check-out)
- ✓ Segunda via do comprovante (GET /reservations/{id}/document)

### Sazonalidade
- ✓ Alta temporada: 15 dez a fim de fev + julho inteiro (20% acréscimo)
- ✓ Cálculo por noite (verifica cada dia)

### Planos de Tarifa
- ✓ STANDARD: sem desconto
- ✓ NON_REFUNDABLE: 15% desconto, sem reembolso ao cancelar

### Arredondamento
- ✓ Banker's Rounding (HALF_EVEN): 10.495 → 10.50, 10.505 → 10.50
- ✓ Aplicado em: preço da noite, subtotal, desconto, total

### Comprovantes
- ✓ Formato textual com CPF formatado (111.222.333-44)
- ✓ Entregue ao confirmar
- ✓ Entregue ao fazer check-out
- ✓ Disponível como segunda via (GET /documents/{id})

### Notificações
- ✓ Ao confirmar: email para hóspede + governança + financeiro
- ✓ Ao cancelar confirmada: email para governança + financeiro (se houver reembolso)
- ✓ Mensagens armazenadas (GET /messages)

### Relatórios
- ✓ Ocupação (GET /reports/occupancy?date=2027-07-10): CSV com quartos ocupados
- ✓ Financeiro (GET /reports/revenue?month=2027-07): CSV com receita por mês

## Validações

- ✓ Quarto não pode ter duas reservas na mesma noite (não canceladas)
- ✓ Hóspedes não podem exceder capacidade do quarto
- ✓ Mínimo de 1 hóspede
- ✓ Data de saída posterior à de entrada
- ✓ Operações permitidas apenas em estados válidos
- ✓ Formato de datas e valores em dinheiro

## Códigos de Erro HTTP

| Status | Erro | Descrição |
|--------|------|-----------|
| 201 | - | Reserva criada com sucesso |
| 400 | INVALID_REQUEST | Falta campo, JSON inválido, datas fora do formato |
| 404 | ROOM_NOT_FOUND | Quarto não existe |
| 404 | RESERVATION_NOT_FOUND | Reserva não existe |
| 409 | ROOM_UNAVAILABLE | Quarto reservado nas datas |
| 409 | OPERATION_NOT_ALLOWED | Operação não permitida no estado atual |
| 422 | CAPACITY_EXCEEDED | Mais hóspedes que a capacidade |

## Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| DB_URL | JDBC URL do PostgreSQL | (obrigatório) |
| DB_USER | Usuário do banco | (obrigatório) |
| DB_PASSWORD | Senha do banco | (obrigatório) |
| PORT | Porta do servidor | 7070 |

## Build e Deploy

```bash
# Compilar
mvn clean verify

# Executar
java -jar target/reservations-system.jar
```

## Testes

- ✓ MoneyUtilsTest: Testes de arredondamento e cálculos financeiros
- ✓ DateUtilsTest: Testes de sazonalidade e formatação de datas
- ✓ Build completo passa (mvn verify)

## Detalhes Técnicos

### Padrões Utilizados
- **Observer Pattern**: ReservationNotifier + NotificationService
- **Repository Pattern**: Para acesso a dados
- **Service Layer**: Lógica de negócio isolada

### Segurança
- ✓ Validação de entrada em todos os endpoints
- ✓ Prepared statements contra SQL injection
- ✓ Tipagem forte em DTOs

### Performance
- ✓ Índices em reservations(room_number, check_in, check_out)
- ✓ Índice em reservations(check_in) para relatórios
- ✓ Queries otimizadas para buscas comuns

### Formato Brasileiro
- ✓ Datas: dd/mm/yyyy (comprovantes), yyyy-mm-dd (APIs/CSVs)
- ✓ Dinheiro: R$ 1.234,56 (comprovantes), 1234.56 (APIs/CSVs)
- ✓ CPF: 111.222.333-44
