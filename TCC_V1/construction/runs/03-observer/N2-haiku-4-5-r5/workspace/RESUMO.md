# Resumo do Sistema de Reservas - Pousada Maré Alta

## Visão Geral

Sistema completo de gerenciamento de reservas para uma pousada com 5 quartos, desenvolvido em **Java 21** com **Javalin 6.7.0** e **PostgreSQL 18**.

## Arquitetura

### Camadas

1. **Domain**: Modelos de negócio e enums
   - `Room`: Informações dos quartos (tipo, capacidade, taxa diária)
   - `Reservation`: Dados completos de uma reserva com cálculos
   - `Message`: Registro de mensagens enviadas
   - Enums: `RoomType`, `ReservationStatus`, `RatePlan`, `MessageChannel`

2. **Infrastructure**: Acesso a dados e cálculos
   - `Database`: Inicialização de schema e conexão
   - Repositórios: `RoomRepository`, `ReservationRepository`, `MessageRepository`
   - `PriceCalculator`: Cálculos de preço com arredondamento "meio para o par"

3. **Services**: Lógica de negócio
   - `ReservationService`: CRUD e transições de estado de reservas
   - `ReportService`: Geração de relatórios em CSV
   - `ReservationEventPublisher`: Pub/Sub para eventos
   - `ReservationEventListener`: Interface extensível para notificações
   - `HostelNotificationListener`: Implementação para e-mails

4. **API**: REST Controllers com Javalin
   - `RoomController`: Listagem de quartos
   - `ReservationController`: Gerenciamento de reservas
   - `MessageController`: Consulta de mensagens
   - `ReportController`: Geração de relatórios

### Padrões de Design

- **Observer/Pub-Sub**: Sistema de eventos para notificações extensível
- **Repository**: Abstração de acesso a dados
- **Service Layer**: Lógica de negócio centralizada
- **Value Objects**: Uso de enums e records para dados imutáveis

## Funcionalidades

### Gerenciamento de Reservas

- ✅ Criar reserva pendente
- ✅ Confirmar pagamento
- ✅ Cancelar com devolução (depende do plano e status)
- ✅ Check-in e check-out
- ✅ Gerar comprovantes (receipt format)
- ✅ Segunda via de comprovante

### Cálculos Financeiros

- ✅ Preços sazonais (20% a mais em alta temporada)
- ✅ Planos de tarifa (STANDARD normal, NON_REFUNDABLE com 15% desconto)
- ✅ Arredondamento "meio para o par" em cada passo
- ✅ Reembolsos inteligentes por plano

### Sistema de Notificações

- ✅ E-mail ao hóspede na confirmação
- ✅ E-mail à governança quando confirma (para preparar quarto)
- ✅ E-mail ao financeiro quando confirma (para registrar pagamento)
- ✅ E-mail à governança quando cancela (para liberar datas)
- ✅ E-mail ao financeiro quando cancela (se houver reembolso)
- ✅ Arquitetura extensível para novos departamentos

### Relatórios

- ✅ Ocupação por noite (CSV)
- ✅ Receita por mês (CSV)
- ✅ Cálculos corretos de money in/out

### Validações

- ✅ Capacidade de quartos vs. número de hóspedes
- ✅ Indisponibilidade de quartos (sem duplo-booking)
- ✅ Transições de estado corretas
- ✅ Datas válidas (check-out > check-in)
- ✅ Formatos de data e moeda

## Base de Dados

### Schema

```sql
-- Quartos (fixos)
rooms(number, type, capacity, nightly_rate)

-- Reservas (CRUD completo)
reservations(id, room_number, guest_*, check_in, check_out, guests, 
             rate_plan, nights, subtotal, discount, total, status, 
             amount_paid, refund)

-- Mensagens (log de notificações)
messages(id, channel, to_address, subject, body)
```

### Inicialização Automática

- Schema criado automaticamente na primeira execução
- Dados iniciais de quartos inseridos
- Índices para performance

## Performance

- **Conexão**: HikariCP com pool de 10 conexões
- **Indices**: Em `rooms.number`, `reservations.room_number+dates`, `messages.to`
- **Queries**: Otimizadas com JOINs mínimos (dados normalizados)

## Testes

- ✅ Testes unitários para `PriceCalculator` (sazonalidade, arredondamento)
- ✅ Compilação com `mvn clean verify`
- ✅ Tudo testado manualmente com curl

## Deployment

### Compilação
```bash
mvn clean package
```

Gera `target/pousada.jar` (fat JAR com todas as dependências)

### Execução
```bash
java -jar target/pousada.jar
```

Com variáveis de ambiente:
- `DB_URL`: JDBC connection string
- `DB_USER`: Usuário PostgreSQL
- `DB_PASSWORD`: Senha PostgreSQL  
- `PORT`: Porta HTTP (padrão 7070)

### Docker
```bash
docker-compose up -d  # Inicia PostgreSQL
java -jar target/pousada.jar
```

## Extensibilidade

### Adicionar Novo Listener

```java
public class RestauranteListener implements ReservationEventListener {
  @Override
  public void onReservationConfirmed(ReservationEvent event) {
    // Notificar restaurante sobre novo hóspede
  }
  // ... outros métodos
}

// Em Main.java
ReservationEventPublisher.subscribe(new RestauranteListener());
```

### Adicionar Novo Endpoint

```java
// Em novo controller
public static void novoEndpoint(Context ctx) { ... }

// Em Main.java
app.post("/novo", NovoController::novoEndpoint);
```

### Alterar Quartos

Modificar inserts na tabela `rooms` no `Database.createSchema()`

## Conformidade com Requisitos

- ✅ Java 21
- ✅ Javalin 6.7.0 (sem Spring)
- ✅ Maven
- ✅ PostgreSQL 18
- ✅ Variáveis de ambiente
- ✅ Schema automático
- ✅ JAR executável
- ✅ `mvn verify` passa
- ✅ Todos os endpoints da API
- ✅ Todos os cálculos financeiros
- ✅ Sistema de notificações extensível
- ✅ Relatórios em CSV
- ✅ Validações completas

## Próximas Melhorias (Opcionais)

- Autenticação/autorização
- Confirmação de e-mail
- Cancelamento online (link no e-mail)
- Dashboard de admin
- Integração com gateway de pagamento
- Backup automático do banco
- Logs estruturados
- Métricas (Prometheus)
- API de tarifa dinâmica
- Integração com SMS
