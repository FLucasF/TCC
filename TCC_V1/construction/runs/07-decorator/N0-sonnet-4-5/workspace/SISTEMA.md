# Sistema de Reservas - Pousada Maré Alta

## Visão Geral

Sistema completo de gestão de reservas desenvolvido em Java 21 com Javalin, PostgreSQL e arquitetura em camadas.

## Funcionalidades Implementadas

### ✅ Gestão de Quartos
- 5 quartos pré-configurados (101, 102: STANDARD | 201, 202: DELUXE | 301: SUITE)
- Capacidades e tarifas por tipo
- Validação de disponibilidade com bloqueio de conflitos

### ✅ Gestão de Reservas
- Criação de reservas com validação completa
- Ciclo de vida: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento com regras de reembolso
- Cálculo automático de preços com alta temporada

### ✅ Cálculo de Preços
- Alta temporada (15/dez a fev + julho): +20%
- Plano não reembolsável: -15% de desconto
- Arredondamento "half to even" em cada etapa
- Exemplo: 2 noites STANDARD em alta = R$ 599,76

### ✅ Sistema de Mensagens (Core Feature)

Sistema extensível baseado em Chain of Responsibility com três regras principais:

#### 1. Validação de E-mail
```java
EmailValidationRule
```
- Valida formato: algo@dominio.com
- Bloqueia mensagens inválidas (status: BLOCKED)
- Mantém endereço original para auditoria

#### 2. Cópia para Arquivo
```java
ArchiveCopyRule
```
- Gera cópia para arquivo@pousadamarealta.com.br
- Prefixo "[Cópia] " no assunto
- Não executa em modo de teste
- Não copia mensagens bloqueadas

#### 3. Redirecionamento em Teste
```java
TestModeRedirectRule
```
- Redireciona para testes@pousadamarealta.com.br
- Prefixo "[TESTE] " no assunto
- Adiciona linha "Destinatário original: ..."
- Preserva bloqueio de e-mails inválidos

**Extensibilidade**: Novas regras podem ser adicionadas implementando `MessageRule` e configurando em `MessagingService`.

### ✅ Relatórios CSV

#### Ocupação por Noite
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

#### Receita por Mês
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
5,301,2027-03-20,2027-03-25,CANCELLED,1969.62
total,2749.42
```
- Inclui reservas canceladas não reembolsáveis
- Valores = amountPaid - refund

### ✅ Comprovantes
- Gerados na confirmação e check-out
- Segunda via disponível para reservas pagas
- Formato texto e JSON
- E-mail de confirmação com comprovante incluído

## Arquitetura

```
┌─────────────────────────────────────────────┐
│           API Layer (Javalin)               │
│  Controllers + DTOs + Exception Handlers    │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         Service Layer                       │
│  ReservationService │ RoomService           │
│  ReportService │ MessagingService           │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│      Repository Layer (JDBC)                │
│  ReservationRepo │ RoomRepo │ MessageRepo   │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         PostgreSQL 18                       │
│  HikariCP Connection Pool                   │
└─────────────────────────────────────────────┘
```

### Padrões de Design

- **Repository Pattern**: Isolamento de acesso a dados
- **Service Layer**: Lógica de negócio centralizada
- **DTO Pattern**: Separação modelo/API
- **Chain of Responsibility**: Sistema de regras de mensagens
- **Strategy Pattern**: Cálculo de preços
- **Builder Pattern**: Formatação de comprovantes

## Estrutura de Dados

### Reservations
```sql
id (BIGSERIAL)
room_number (VARCHAR)
guest_name, guest_email, guest_cpf
check_in, check_out (DATE)
guests (INTEGER)
rate_plan (VARCHAR)
nights (INTEGER)
subtotal, discount, total (NUMERIC)
status (VARCHAR)
amount_paid, refund (NUMERIC)
```

### Messages
```sql
id (BIGSERIAL)
channel (VARCHAR)
recipient (VARCHAR)
subject, body (TEXT)
status (VARCHAR)
```

## Validações Implementadas

### Request
- ✅ Campos obrigatórios
- ✅ Formato de datas (yyyy-MM-dd)
- ✅ Número de hóspedes >= 1
- ✅ Check-out > check-in

### Business Rules
- ✅ Quarto existe
- ✅ Capacidade não excedida
- ✅ Sem conflito de datas
- ✅ Transições de estado válidas
- ✅ E-mail válido

## Tratamento de Erros

Todos os erros retornam JSON estruturado:
```json
{
  "error": "CODIGO",
  "message": "Descrição legível"
}
```

HTTP Status apropriados:
- 400: Dados inválidos
- 404: Recurso não encontrado
- 409: Conflito de negócio
- 422: Validação semântica

## Configuração por Ambiente

### Desenvolvimento
```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada_dev
HOTEL_TEST_MODE=true
```

### Staging
```bash
DB_URL=jdbc:postgresql://staging-db:5432/pousada
HOTEL_TEST_MODE=true
```

### Produção
```bash
DB_URL=jdbc:postgresql://prod-db:5432/pousada
HOTEL_TEST_MODE=false
```

## Testes

### Cobertura
- ✅ 15 cenários de teste em ReservationSystemIT
- ✅ 3 cenários de teste em TestModeIT
- ✅ Testes end-to-end com banco real (Testcontainers)

### Cenários Testados
1. Listar quartos
2. Criar reserva
3. Confirmar com envio de e-mail
4. Check-in e check-out
5. Obter comprovante
6. Cálculo de alta temporada
7. Desconto não reembolsável
8. Bloqueio de e-mail inválido
9. Conflito de disponibilidade
10. Excesso de capacidade
11. Cancelamento com reembolso
12. Cancelamento sem reembolso
13. Relatório de ocupação
14. Relatório de receita
15. Modo de teste (redirecionamento)
16. Modo de teste (sem cópia arquivo)
17. Modo de teste (bloqueio e-mail)

## Deploy

### Docker Compose
```bash
mvn clean package
docker-compose up
```

### JAR Standalone
```bash
java -jar target/reservation-system-1.0.0.jar
```

### Requisitos de Infraestrutura
- Java 21 Runtime
- PostgreSQL 18
- 512MB RAM mínimo
- Porta configurável (default 7070)

## Manutenção e Extensões Futuras

### Facilmente Extensível

1. **Novas Regras de Mensagem**
   - Implementar `MessageRule`
   - Adicionar em `MessagingService.rules`

2. **Novos Tipos de Quarto**
   - Adicionar enum em `RoomType`
   - Inserir em `Database.initializeRooms()`

3. **Novos Relatórios**
   - Adicionar método em `ReportService`
   - Registrar endpoint em `ApiController`

4. **Canais de Mensagem**
   - SMS, WhatsApp, etc.
   - Adicionar em `MessageChannel`
   - Regras específicas por canal

### Regras Futuras Mencionadas

- Limite de horário para envio (19h-8h bloqueado)
- Rate limiting por hóspede/dia
- Regras por instalação (config externa)
- Notificações de lembrete (24h antes)

## Monitoramento

### Logs
- SLF4J + Simple Logger
- Todas as operações críticas logadas
- Erros com stack trace completo

### Auditoria
- Todas as mensagens preservadas (inclusive bloqueadas)
- Histórico completo de reservas
- Rastreamento de cancelamentos e reembolsos

## Segurança

- ✅ Sem SQL Injection (PreparedStatement)
- ✅ Validação de entrada em todas as camadas
- ✅ Connection pooling (HikariCP)
- ✅ Modo de teste para desenvolvimento seguro
- ✅ Separação de configuração por ambiente

## Performance

- Connection pool configurado (10 conexões)
- Índices automáticos (PKs, FKs)
- Queries otimizadas (sem N+1)
- JSON serialization eficiente (Jackson)
- Stateless (horizontal scaling ready)

## Conformidade

- ✅ Todas as especificações atendidas
- ✅ Arredondamento exato (half to even)
- ✅ Formatos de data/moeda conforme requisitos
- ✅ Estados e transições conforme especificado
- ✅ Sistema de mensagens completo desde o primeiro dia
