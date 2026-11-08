# Pousada Maré Alta - Documentação Técnica

## Visão Geral

Sistema de gerenciamento de reservas para a Pousada Maré Alta. A aplicação gerencia:
- 5 quartos com tipos e capacidades diferentes
- Ciclo de vida de reservas com validações
- Cálculo de preços com high season e planos de tarifa
- Geração de comprovantes e relatórios
- Armazenamento de mensagens (emails)

## Arquitetura

```
API Layer (Api.java)
    ↓
Service Layer (ReservationService, ReceiptService, ReportService)
    ↓
Repository Layer (RoomRepository, ReservationRepository, MessageRepository)
    ↓
Database Layer (PostgreSQL via HikariCP)
```

## Componentes Principais

### 1. Database.java
- Inicializa pool de conexões HikariCP
- Cria schema automático na primeira execução
- Insere dados iniciais de quartos
- Índices para performance de conflitos de reserva

### 2. Models
- **Room**: Quarto (imutável após criação)
- **Reservation**: Reserva com estado e cálculos
- **Message**: Email armazenado
- **Receipt**: Comprovante formatado

### 3. Repositories
- **RoomRepository**: Leitura de quartos
- **ReservationRepository**: CRUD de reservas + queries específicas
- **MessageRepository**: Armazenamento de mensagens

### 4. Services

#### ReservationService
- Validação de entrada (capacidade, datas, conflitos)
- Criação de reservas com preços pré-calculados
- Transições de estado (confirm, check-in, check-out, cancel)
- Lógica de reembolso

#### ReceiptService
- Formatação de comprovantes em português
- Geração de emails (texto puro)
- Envio de mensagens (armazenado no banco)

#### ReportService
- Relatório de ocupação (por noite)
- Relatório de receita (por mês)
- Relatório de taxa turística (por mês)
- Relatório de análise de planos (por mês)

### 5. Api.java
- Registro de endpoints
- Tratamento centralizado de exceções
- Serialização JSON com Gson

### 6. Utils
- **MoneyRounder**: Arredondamento banker's (HALF_EVEN)
- **DateUtils**: Parsing/formatação de datas, detecção de high season
- **PriceCalculator**: Cálculo de preço total com descontos

## Fluxos Principais

### Criar Reserva
1. Validar quarto existe
2. Validar capacidade e datas
3. Validar sem conflitos
4. Calcular preço (alta temporada + plano)
5. Salvar no banco com status PENDING

### Confirmar Reserva
1. Validar status = PENDING
2. Marcar como CONFIRMED
3. Guardar amountPaid
4. Enviar email com comprovante
5. Retornar reserva atualizada

### Check-in
1. Validar status = CONFIRMED
2. Marcar como CHECKED_IN

### Check-out
1. Validar status = CHECKED_IN
2. Marcar como CHECKED_OUT
3. Retornar comprovante (mesmo formato do email)

### Cancelar
1. Validar status = PENDING ou CONFIRMED
2. Marcar como CANCELLED
3. Calcular reembolso (depende do plano):
   - STANDARD: refund = amountPaid
   - NON_REFUNDABLE: refund = null (sem reembolso)
4. Guardar refund no banco

## Detalhes de Implementação

### Arredondamento Monetário
Todos os cálculos de dinheiro usam `BigDecimal` com `RoundingMode.HALF_EVEN`:
1. Preço por noite (com high season)
2. Subtotal (preço × noites)
3. Desconto (subtotal × 15% para NON_REFUNDABLE)
4. Total (subtotal - desconto)

Exemplo:
```
SUITE NON_REFUNDABLE 10/07/2027 a 13/07/2027 (3 noites)
Base: R$ 579,90
High season: 579,90 × 1,20 = 695,88
Subtotal: 695,88 × 3 = 2.087,64
Desconto: 2.087,64 × 0,15 = 313,146 → arredonda para 313,15
Total: 2.087,64 - 313,15 = 1.774,49
```

### Alta Temporada
Detectada por noite individual:
- 15 de dezembro a 28 de fevereiro (verão)
- Mês de julho inteiro

Se qualquer noite cair em high season, aquela noite é cobrada com +20%.

### Ciclo de Vida de Reserva
```
PENDING
├─ (confirm) → CONFIRMED
│  ├─ (check-in) → CHECKED_IN
│  │  └─ (check-out) → CHECKED_OUT
│  └─ (cancel) → CANCELLED [refund = amountPaid se STANDARD]
└─ (cancel) → CANCELLED [refund = null, sem amountPaid]
```

### Refund
- `null`: Não foi cancelada (ou NON_REFUNDABLE)
- Valor decimal: Montante reembolsado

Cálculo em Revenue Report:
```
amount = amountPaid - (refund ?? 0)
```

### Relatórios CSV

#### Ocupação
- Querystring: `?date=2027-07-10`
- Filtro: reservas CONFIRMED/CHECKED_IN/CHECKED_OUT
- Condição: check_in <= date AND check_out > date
- Saída: room, type, reservation, check_in, check_out

#### Receita
- Querystring: `?month=2027-07`
- Filtro: reservas começadas naquele mês
- Condição: check_in >= firstDay AND check_in < nextMonth
- Saída: reservation, room, check_in, check_out, status, amount

#### Taxa Turística
- Querystring: `?month=2027-07`
- Filtro: idem receita, mas apenas CONFIRMED/CHECKED_IN/CHECKED_OUT
- Cálculo: R$ 4,50 × guests × nights
- Saída: reservation, room, check_in, check_out, guests, nights, tax

#### Planos de Tarifa
- Querystring: `?month=2027-07`
- Filtro: idem receita, agrupado por ratePlan
- Agrupa STANDARD e NON_REFUNDABLE
- Saída: rate_plan, reservations, amount

### Formatação de Comprovante

Template:
```
Comprovante de reserva nº {id}
Hóspede: {name} (CPF {cpf})
Quarto: {number} ({type})
Entrada: {checkIn} | Saída: {checkOut} | {nights} diária(s)
Diárias: R$ {subtotal}
[Desconto: R$ {discount}]  ← só se > 0
Total: R$ {total}
```

Locales:
- Datas no comprovante: `dd/MM/yyyy`
- Dinheiro: `#,##0.00` (pt_BR)
- Datas em CSV: `yyyy-MM-dd` (ISO)
- Dinheiro em CSV: `0.00` (US decimal)

## Decisões de Design

### 1. Preços Imutáveis
Uma vez criada, a reserva não muda seus preços. Se alta temporada muda, não afeta reservas antigas.

### 2. Refund Nullable
- `null` = não foi cancelada OU é NON_REFUNDABLE
- `0` não seria usado (usamos null para "não se aplica")
- Simplifica lógica de "reservas com dinheiro que ficou"

### 3. High Season por Noite
Ao invés de um período fixo, calcula por noite dentro do intervalo.
Mais flexível para mudanças futuras (p.ex., feriados específicos).

### 4. Emails em Banco
Para integração futura com:
- Sistema de fila de envio
- Reenvio de comprovantes
- Auditoria de comunicação

### 5. Sem Spring
Javalin é mais leve e atendia perfeitamente.
Sem overhead de auto-configuration.

### 6. BigDecimal para Dinheiro
Necessário para precisão em cálculos monetários.
Float/Double causariam erros de arredondamento.

## Extensibilidade

### Futuros Relatórios
O padrão está pronto para:
- Relatório de cancelamentos
- Relatório de hóspedes por estado
- Relatório de taxa ocupacional

Adicionar em:
1. ReportService: novo método
2. Api.java: novo endpoint GET /reports/{name}
3. Database indices se necessário

### Taxa Turística Dinâmica
Atualmente hardcoded em ReportService:
```java
private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");
```

Para parametrizar:
1. Adicionar tabela de histórico de taxas
2. Passar ano/mês como parâmetro
3. Buscar taxa no banco

### Timestamps em Relatórios
Conforme requisito futuro, adicionar:
```
[título e dados]

Gerado em: 2027-07-15 14:32:45
```

Adicionar em ReportService.generate*():
```java
LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
```

## Testes

Testes unitários cobrem:
- PriceCalculator (low/high season, planos)
- DateUtils (parsing, high season detection)
- Arredondamento (HALF_EVEN)

Testes de integração requerem:
- PostgreSQL rodando
- Variáveis de ambiente DB_* configuradas

Execute com: `mvn verify`

## Segurança

### Validações
- ✅ CPF/Email são strings (sem validação de formato)
- ✅ Datas são parseadas e validadas
- ✅ Números (guests, room) parseados com parseInt
- ✅ Capacidade vs guests verificado
- ✅ Sem SQL injection (PreparedStatement)

### Não Implementado
- Autenticação (todos podem criar/modificar)
- Autorização (sem roles)
- Rate limiting
- CORS (qualquer origem)

Para produção adicionar:
- JWT ou sessão
- Verificação de role (admin vs guest vs staff)
- Rate limiting por IP
- HTTPS + HSTS

## Deploy

### Docker
```bash
docker-compose up
```

### Manual (Linux)
```bash
mvn clean package
export DB_URL="jdbc:postgresql://db-host:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"
export PORT=7070
java -jar target/app.jar
```

### Requisitos
- Java 21+ (runtime ou JDK)
- PostgreSQL 18+
- 256MB RAM mínimo

## Troubleshooting

### "Connection refused"
- Verificar DB_URL, DB_USER, DB_PASSWORD
- Verificar se PostgreSQL está rodando
- Verificar firewall

### "Table does not exist"
- Schema é criado automaticamente
- Se erro persiste, deletar DB e recriar

### "Too many open connections"
- HikariCP tem máximo de 10 conexões
- Aumentar em Database.java: `config.setMaximumPoolSize(20)`

### Port já em uso
```bash
export PORT=8080
```

## Contato/Suporte

Sistema desenvolvido para Pousada Maré Alta.
Todas as especificações atendidas conforme requisitado.
