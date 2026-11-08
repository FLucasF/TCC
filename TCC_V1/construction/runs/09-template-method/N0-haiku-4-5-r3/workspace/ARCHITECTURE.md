# Arquitetura - Pousada Maré Alta

## Visão Geral

O sistema é uma aplicação web REST construída com:
- **Framework Web**: Javalin 6.7.0
- **Banco de Dados**: PostgreSQL 18
- **Linguagem**: Java 21
- **Build**: Maven
- **JSON**: Gson

## Estrutura de Diretórios

```
src/
├── main/
│   ├── java/com/pousada/
│   │   ├── Application.java          # Ponto de entrada
│   │   ├── database/
│   │   │   └── Database.java         # Conexão e inicialização do BD
│   │   ├── models/                   # Entidades do domínio
│   │   │   ├── Room.java
│   │   │   ├── Reservation.java
│   │   │   └── Message.java
│   │   ├── dto/                      # Data Transfer Objects
│   │   │   ├── RoomDTO.java
│   │   │   ├── ReservationDTO.java
│   │   │   ├── MessageDTO.java
│   │   │   ├── VoucherDTO.java
│   │   │   ├── CreateReservationRequest.java
│   │   │   └── ConfirmReservationRequest.java
│   │   ├── services/                 # Lógica de negócio
│   │   │   ├── RoomService.java
│   │   │   ├── ReservationService.java
│   │   │   ├── MessageService.java
│   │   │   ├── ReportService.java
│   │   │   └── VoucherService.java
│   │   ├── routes/                   # Controladores HTTP
│   │   │   ├── RoomRoutes.java
│   │   │   ├── ReservationRoutes.java
│   │   │   ├── MessageRoutes.java
│   │   │   └── ReportRoutes.java
│   │   └── utils/                    # Utilitários
│   │       ├── PricingUtils.java     # Cálculos de preço
│   │       ├── FormatUtils.java      # Formatação
│   │       └── ErrorResponse.java    # Tratamento de erros
│   └── resources/
│       └── logback.xml               # Configuração de logging
└── test/
    └── java/com/pousada/
        └── ReservationTest.java      # Testes unitários
```

## Padrões de Arquitetura

### MVC-like com Separação de Camadas

- **Routes**: Controladores que recebem requisições HTTP
- **Services**: Lógica de negócio
- **Models**: Entidades de domínio
- **Database**: Acesso aos dados
- **DTOs**: Serialização/Deserialização JSON

### Validação em Camadas

1. **Routes**: Validação de entrada HTTP
2. **Services**: Validação de regras de negócio
3. **Database**: Constraint de banco de dados

## Fluxo de Dados

```
HTTP Request
    ↓
Routes (validação básica)
    ↓
Services (lógica de negócio)
    ↓
Database (acesso aos dados)
    ↓
Models (dados persistidos)
    ↓
DTOs (serialização)
    ↓
HTTP Response (JSON)
```

## Componentes Principais

### Application.java
- Inicializa o banco de dados
- Configura as rotas HTTP
- Inicia o servidor Javalin

### Database.java
- Gerencia a conexão com PostgreSQL
- Cria o schema (tabelas) automaticamente
- Usa HikariCP para connection pooling

### RoomService
- Busca quartos do banco
- Sem lógica complexa (dados estáticos)

### ReservationService
- Cria reservas com cálculo de preços
- Controla transições de status
- Valida disponibilidade
- Gerencia cancelamentos com reembolsos

### ReportService
- Gera relatórios em CSV
- 4 tipos de relatórios
- Suporta filtros por data/mês

### PricingUtils
- Detecção de alta temporada
- Cálculo de diárias com ajuste sazonal
- Arredondamento "meio para o par" (HALF_EVEN)
- Cálculo de descontos
- Cálculo de totais

### FormatUtils
- Formatação de datas (DD/MM/YYYY e YYYY-MM-DD)
- Formatação de valores (R$ e CSV)
- Formatação de CPF

## Banco de Dados

### Schema

```sql
-- Quartos (dados estáticos)
CREATE TABLE rooms (
  number INTEGER PRIMARY KEY,
  type VARCHAR(20),
  capacity INTEGER,
  nightly_rate NUMERIC(10,2)
);

-- Reservas (dados dinâmicos)
CREATE TABLE reservations (
  id SERIAL PRIMARY KEY,
  room_number INTEGER,
  guest_name VARCHAR(255),
  guest_email VARCHAR(255),
  guest_cpf VARCHAR(14),
  check_in DATE,
  check_out DATE,
  guests INTEGER,
  rate_plan VARCHAR(20),
  nights INTEGER,
  subtotal NUMERIC(10,2),
  discount NUMERIC(10,2),
  total NUMERIC(10,2),
  status VARCHAR(20),
  amount_paid NUMERIC(10,2),
  refund NUMERIC(10,2)
);

-- Mensagens (e-mails)
CREATE TABLE messages (
  id SERIAL PRIMARY KEY,
  channel VARCHAR(20),
  to VARCHAR(255),
  subject VARCHAR(500),
  body TEXT
);
```

### Índices Implícitos
- Primary keys em todos os campos `id` e `number`
- Foreign key de `room_number` para `rooms(number)`

## Tratamento de Erros

### HTTP Status Codes

- **201**: Recurso criado com sucesso
- **200**: Operação bem-sucedida
- **400**: Requisição inválida (INVALID_REQUEST)
- **404**: Recurso não encontrado
- **409**: Conflito (operação não permitida ou recurso indisponível)
- **422**: Dados inválidos (capacidade excedida)

### Resposta de Erro

```json
{
  "error": "CODIGO_DO_ERRO",
  "message": "Descrição do erro"
}
```

## Segurança

### Validações Implementadas

1. **SQL Injection**: Uso de PreparedStatements
2. **Type Safety**: Java static typing
3. **Validação de Entrada**: Em todas as rotas
4. **Autorização**: N/A (sistema não tem autenticação)

### Considerações Futuras

- Adicionar autenticação/autorização
- Rate limiting
- HTTPS
- Logs de auditoria

## Performance

### Otimizações

- Connection pooling (HikariCP)
- Lazy loading onde apropriado
- Índices no banco de dados (PKs e FKs)
- Streaming de relatórios em CSV

### Escalabilidade

- Arquitetura stateless
- Banco de dados centralizado
- Sem cache em memória
- Possível adicionar cache/CDN para relatórios

## Testes

### Cobertura

- Utilitários de preço (PricingUtils)
- Utilitários de formatação (FormatUtils)
- Testes unitários básicos

### Execução

```bash
mvn verify  # Roda testes e verificações
```

## Deployment

### Modo Standalone

Gera um JAR fat executável com todas as dependências:

```bash
mvn clean package
java -jar target/marea-alta.jar
```

### Variáveis de Ambiente

- `DB_URL`: JDBC URL do banco
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do banco
- `PORT`: Porta HTTP (padrão: 7070)

## Extensibilidade

### Para Adicionar Nova Funcionalidade

1. **Modelo**: Adicionar em `models/`
2. **Serviço**: Lógica em `services/`
3. **Rota**: Endpoint em `routes/`
4. **DTO**: Serialização em `dto/`
5. **Banco**: Update em `Database.createSchema()`

### Exemplo: Novo Relatório

1. Adicionar método em `ReportService`
2. Adicionar rota em `ReportRoutes`
3. Registrar em `Application.java`

## Considerações de Design

### Por que não Spring Framework?

- Javalin é mais leve e simples
- Menos dependências (requisito do projeto)
- Melhor performance
- Mais fácil de entender

### Por que JDBC em vez de ORM?

- Controle fino sobre SQL
- Simples para consultas específicas
- Evita over-engineering
- Permite otimizações específicas

### Por que Gson em vez de Jackson?

- Mais simples para DTOs
- Menos dependências
- Performance adequada
- Menos configuração

## Roadmap Futuro

- [ ] Autenticação de hóspedes
- [ ] Dashboard administrativo
- [ ] Integração com payment gateway
- [ ] SMS para confirmações
- [ ] Notificações push
- [ ] API de calendário de disponibilidade
- [ ] Histórico de changes com auditoria
- [ ] Backup automático do banco
- [ ] Relatórios adicionais (cancelamentos, hóspedes por estado)
