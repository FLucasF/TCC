# Entrega - Sistema de Reservas Pousada Maré Alta

## ✅ O que foi desenvolvido

Um **sistema completo de gerenciamento de reservas** para a Pousada Maré Alta, pronto para produção, com:

- API REST em Javalin (Java 21)
- Banco de dados PostgreSQL 18
- Sistema de notificações extensível
- Relatórios em CSV
- Testes unitários
- Documentação completa

## 📦 Arquivos Entregues

### Documentação
- **README.md** - Guia de setup e uso rápido
- **ARCHITECTURE.md** - Documentação de arquitetura do sistema
- **NOTIFICATIONS.md** - Guia para estender o sistema de notificações
- **EXAMPLES.md** - Exemplos práticos de chamadas à API
- **DELIVERY.md** - Este arquivo

### Código-Fonte Principal (26 arquivos Java)

#### Modelos (`model/`)
- **RoomType.java** - Tipos de quarto (STANDARD, DELUXE, SUITE)
- **Room.java** - Entidade de quarto
- **ReservationStatus.java** - Estados de uma reserva
- **RatePlan.java** - Planos de tarifa (STANDARD, NON_REFUNDABLE)
- **Reservation.java** - Entidade de reserva com todos os campos
- **MessageChannel.java** - Canais de notificação (extensível)
- **Message.java** - Entidade de mensagem enviada

#### Banco de Dados (`db/`)
- **Database.java** - Inicialização e gerenciamento de conexões
- **RoomDAO.java** - Acesso aos dados de quartos
- **ReservationDAO.java** - Acesso aos dados de reservas
- **MessageDAO.java** - Acesso aos dados de mensagens

#### Serviços (`service/`)
- **ReservationService.java** - Lógica principal de reservas
- **RoomService.java** - Serviço de quartos
- **ReportService.java** - Geração de relatórios
- **NotificationService.java** - Orquestrador de notificações
- **NotificationHandler.java** - Interface para handlers
- **DefaultNotificationHandler.java** - Handler padrão (governança + financeiro)
- **Notification.java** - Classe de notificação

#### API REST (`api/`)
- **RoomController.java** - Endpoints de quartos
- **ReservationController.java** - Endpoints de reservas
- **MessageController.java** - Endpoints de mensagens
- **ReportController.java** - Endpoints de relatórios

#### Utilitários (`util/`)
- **MoneyRounder.java** - Arredondamento "meio para o par"
- **Formatters.java** - Formatação de datas e valores monetários
- **PricingCalculator.java** - Cálculo de preços com alta temporada

#### Principal
- **Main.java** - Entrada da aplicação, inicialização

### Testes
- **PricingCalculatorTest.java** - Testes unitários do cálculo de preços

### Configuração
- **pom.xml** - Configuração Maven com todas as dependências

## 🎯 Funcionalidades Implementadas

### Quartos
- 5 quartos pré-configurados (101, 102, 201, 202, 301)
- 3 tipos: STANDARD, DELUXE, SUITE
- Capacidades de 2-4 pessoas
- Diárias pré-definidas

### Reservas
- ✅ Criar reserva (status PENDING)
- ✅ Confirmar reserva com pagamento (status CONFIRMED)
- ✅ Check-in (status CHECKED_IN)
- ✅ Check-out com comprovante (status CHECKED_OUT)
- ✅ Cancelamento com reembolso conforme plano
- ✅ Segunda via de comprovante

### Cálculo de Preços
- ✅ Preço base por noite
- ✅ Aumento de 20% em alta temporada (15 dez - 29 fev e julho)
- ✅ Desconto de 15% para plano NON_REFUNDABLE
- ✅ Arredondamento correto em cada passo

### Notificações
- ✅ Email ao hóspede (confirmação)
- ✅ Email à governança (entrada, saída, cancelamento)
- ✅ Email ao financeiro (pagamento, reembolsos)
- ✅ Sistema extensível para novos destinatários
- ✅ Armazenamento de todas as mensagens enviadas

### Relatórios
- ✅ Ocupação por data (CSV)
- ✅ Receita por mês (CSV)

### Validações
- ✅ Quarto existe
- ✅ Quarto disponível (não double-booking)
- ✅ Capacidade não excedida
- ✅ Datas válidas
- ✅ Operação permitida para o status

## 🚀 Instruções de Execução

### 1. Pré-requisitos
```bash
# Java 21
java --version

# Maven 3.6+
mvn --version

# PostgreSQL 18 (já rodando)
psql --version
```

### 2. Compilar
```bash
mvn clean package
```

Gera: `target/pousada-system.jar`

### 3. Configurar Banco de Dados
```bash
# Criar banco
createdb pousada_marealta

# Ou com Docker
docker run -d \
  -e POSTGRES_DB=pousada_marealta \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha123 \
  -p 5432:5432 \
  postgres:18
```

### 4. Executar
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada_marealta"
export DB_USER="pousada"
export DB_PASSWORD="senha123"
export PORT="7070"

java -jar target/pousada-system.jar
```

### 5. Testar
```bash
# Listar quartos
curl http://localhost:7070/rooms

# Ver exemplos em EXAMPLES.md
```

## 📋 Endpoints da API

### Quartos
- `GET /rooms` - Lista quartos

### Reservas
- `POST /reservations` - Criar
- `GET /reservations/{id}` - Obter
- `POST /reservations/{id}/confirmation` - Confirmar
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `POST /reservations/{id}/cancellation` - Cancelar
- `GET /reservations/{id}/document` - Segunda via

### Mensagens
- `GET /messages` - Listar emails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação
- `GET /reports/revenue?month=2027-07` - Receita

## 🔐 Segurança

- ✅ SQL Injection prevenido (Prepared Statements)
- ✅ Validação de entrada
- ✅ Tratamento de erros apropriado
- ✅ HTTP status codes corretos

Nota: Autenticação/Autorização não foram implementadas (fora do escopo).

## 📊 Testes

Executar testes:
```bash
mvn verify
```

Resultado esperado:
```
Tests run: 8, Failures: 0, Errors: 0
BUILD SUCCESS
```

## 🌍 Extensibilidade

### Adicionar Novo Handler de Notificação

1. Implementar `NotificationHandler`
2. Registrar em `Main.java`

Exemplo: Notificar restaurante em novas reservas, transfer do aeroporto, etc.

### Adicionar Novo Canal

Expandir `MessageChannel` enum além de EMAIL (SMS, Slack, WhatsApp, etc).

### Adicionar Novo Tipo de Quarto

1. Estender `RoomType`
2. Adicionar em `Database.initializeRooms()`

## 📝 Observações

1. **Emails não são enviados de verdade**: Ficam armazenados na tabela `messages` para consulta posterior.

2. **Arredondamento**: Usa "meio para o par" (banker's rounding) com `RoundingMode.HALF_EVEN`.

3. **Banco de dados**: Inicializado automaticamente na primeira execução.

4. **Quartos fixos**: Os 5 quartos não mudam (configurado em database).

5. **Zona horária**: Usa `LocalDate` (sem hora), compatível com qualquer timezone.

## 📚 Documentação Completa

- **README.md** - Setup rápido
- **ARCHITECTURE.md** - Arquitetura detalhada
- **NOTIFICATIONS.md** - Sistema de notificações
- **EXAMPLES.md** - Exemplos de curl
- **DELIVERY.md** - Este arquivo

## ✨ Destaques

1. **Sistema de notificações extensível desde o dia 1** - Novos destinatários podem ser adicionados sem modificar código existente.

2. **Lógica de preços precisa** - Arredondamento correto em cada etapa, cálculo de alta temporada.

3. **Relatórios em CSV** - Prontos para uso em Excel/Sheets.

4. **API RESTful completa** - Todos os endpoints especificados funcionando.

5. **Banco de dados normalizado** - Índices para performance.

6. **Código limpo e bem organizado** - Fácil de manter e estender.

## 🎓 Próximas Melhorias

- Dashboard web para gerentes
- Integração com gateway de pagamento
- SMS como canal de notificação
- Autenticação de usuários
- API de administração para quartos
- Cacheing distribuído

---

**Status**: ✅ PRONTO PARA PRODUÇÃO

Todos os requisitos foram atendidos. O sistema está compilando, testando e pronto para ser executado.
