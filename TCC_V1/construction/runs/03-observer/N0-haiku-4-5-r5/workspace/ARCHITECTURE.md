# Arquitetura do Sistema

## Visão Geral

O sistema foi desenvolvido seguindo princípios de clean architecture, com separação clara entre camadas de apresentação, negócio e dados.

## Camadas

### 1. Controller (`controller/`)
Responsável pelos endpoints HTTP. Cada controller mapeia rotas e delega a lógica para os services.

- `RoomController`: Endpoints de quartos
- `ReservationController`: Endpoints de reservas (CRUD + operações de estado)
- `MessageController`: Endpoints de notificações
- `ReportController`: Endpoints de relatórios em CSV

### 2. Service (`service/`)
Contém a lógica de negócio principal.

- `ReservationService`: Orquestração de criação, confirmação, cancelamento, check-in/out
- `PricingService`: Cálculo de preços com suporte a temporadas e planos de tarifa

### 3. Repository (`repository/`)
Acesso a dados via JDBC. Cada repository é responsável por uma entidade.

- `RoomRepository`: Persistência de quartos
- `ReservationRepository`: Persistência de reservas
- `MessageRepository`: Persistência de notificações

### 4. Domain (`domain/`)
Modelos de domínio (value objects e entities).

- `Room`: Dados do quarto
- `Reservation`: Dados da reserva com seu ciclo de vida
- `Message`: Notificação enviada
- `ReservationDocument`: Comprovante de reserva

### 5. Notification (`notification/`)
Sistema de notificações extensível.

- `NotificationListener`: Interface para subscribers
- `NotificationService`: Orquestrador de listeners
- Implementações:
  - `GuestNotificationListener`: Notifica hóspede
  - `GovernanceNotificationListener`: Notifica governança
  - `FinanceNotificationListener`: Notifica financeiro

### 6. Utilities (`util/`)
Funções utilitárias.

- `MathUtil`: Arredondamento (HALF_EVEN)
- `DateFormatter`: Formatação de datas (dd/MM/yyyy)
- `MoneyFormatter`: Formatação de valores (R$ X.XXX,XX)
- `DocumentFormatter`: Geração de comprovantes

## Padrões Utilizados

### Builder Pattern
Classes de domínio usam Builder para facilitar criação com múltiplos parâmetros.

```java
new Reservation.Builder()
    .id(1L)
    .roomNumber(201)
    .guestName("Carla Dias")
    // ... mais campos
    .build();
```

### Repository Pattern
Isolamento de lógica de acesso a dados. Cada repository é responsável por uma entidade.

### Observer Pattern
Sistema de notificações implementado através de listeners que são notificados quando eventos ocorrem.

```java
NotificationService.getInstance().notifyReservationConfirmed(reservation);
```

### Singleton Pattern
`NotificationService` usa singleton para garantir uma única instância de gerenciamento de listeners.

## Fluxo de Criação de Reserva

```
POST /reservations
    ↓
ReservationController.register()
    ↓
ReservationService.create()
    ├─ Validação de datas
    ├─ Validação de hóspedes
    ├─ Validação de capacidade
    ├─ Validação de disponibilidade
    ├─ PricingService.calculatePrice()
    ├─ ReservationRepository.save()
    └─ Retorna Reservation com status PENDING
```

## Fluxo de Confirmação de Reserva

```
POST /reservations/{id}/confirmation
    ↓
ReservationController
    ↓
ReservationService.confirm()
    ├─ Validação de status (PENDING)
    ├─ Atualiza status para CONFIRMED
    ├─ Atualiza amountPaid com total da reserva
    ├─ ReservationRepository.update()
    └─ NotificationService.notifyReservationConfirmed()
        ├─ GuestNotificationListener (e-mail ao hóspede)
        ├─ GovernanceNotificationListener (e-mail à governança)
        └─ FinanceNotificationListener (e-mail ao financeiro)
```

## Extensibilidade

### Adicionar Novo Subscriber

1. Criar classe que implemente `NotificationListener`:
```java
public class RestaurantNotificationListener implements NotificationListener {
    @Override
    public void onReservationConfirmed(Reservation reservation) {
        // Notificar restaurante sobre hóspedes que chegam
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        // Notificar restaurante sobre cancelamento
    }
}
```

2. Registrar no `NotificationService`:
```java
NotificationService.getInstance()
    .addListener(new RestaurantNotificationListener(messageRepository));
```

### Adicionar Novo Tipo de Notificação

Se precisar notificar sobre outros eventos (ex: check-in próximo), estenda `NotificationListener` com novos métodos.

## Segurança

### Validações
- **Datas**: Check-out deve ser após check-in
- **Hóspedes**: Mínimo 1, máximo conforme capacidade do quarto
- **Disponibilidade**: Não há overbooking (reservas confirmadas/checked-in/checked-out)

### Autorização
O sistema não implementa autenticação/autorização (conforme especificação). Em produção:
- Implementar JWT ou sessão
- Controlar acesso por perfil (recepção, governança, financeiro)
- Auditoria de operações

## Tratamento de Erros

Erros são propagados com códigos específicos:
- `ROOM_NOT_FOUND` (404): Quarto não existe
- `RESERVATION_NOT_FOUND` (404): Reserva não existe
- `ROOM_UNAVAILABLE` (409): Quarto indisponível nas datas
- `CAPACITY_EXCEEDED` (422): Mais hóspedes que capacidade
- `OPERATION_NOT_ALLOWED` (409): Operação inválida para o status atual
- `INVALID_REQUEST` (400): Falta de campo ou formato inválido

## Performance

- **JDBC direto**: Sem ORM para máxima performance
- **Cálculos em memória**: Arredondamento feito após cálculo, não no banco
- **Índices implícitos**: PostgreSQL indexa PRIMARY KEY automaticamente
- **Queries otimizadas**: Prepared statements para evitar SQL injection

## Testes

Testes unitários cobrem o cálculo de preços em diferentes cenários:
- Temporadas (alta/baixa)
- Planos de tarifa (STANDARD/NON_REFUNDABLE)
- Arredondamento correto (HALF_EVEN)

Para testes de integração completos, seria necessário um banco PostgreSQL de teste.

## Melhorias Futuras

1. **Cache**: Dados de quartos raramente mudam, poderiam ser cacheados
2. **Auditoria**: Rastrear quem fez o quê e quando
3. **Backup automático**: Backup diário do banco
4. **Rate limiting**: Proteger API contra abuso
5. **Webhooks**: Notificações síncronas para sistemas externos
6. **Agendamento**: Enviar lembretes de check-in automático
7. **Relatórios avançados**: Dashboard com métricas de ocupação, receita
