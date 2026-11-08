# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## 🎯 Funcionalidades Implementadas

### ✅ Núcleo
- [x] 5 quartos configuráveis (101, 102, 201, 202, 301) com capacidades e tarifas
- [x] Ciclo completo de reserva: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- [x] Suporte a cancelamento em qualquer ponto (antes do check-in)
- [x] Cálculo automático de preços com arredondamento "meio para par"
- [x] Suporte a duas temporadas: alta (15/dez-fev + julho) e baixa
- [x] Dois planos de tarifa: STANDARD e NON_REFUNDABLE (15% desconto)

### ✅ Validações
- [x] Sem overbooking - impossível reservar quarto já ocupado
- [x] Validação de capacidade - impossível exceder limite do quarto
- [x] Validação de datas - saída deve ser após entrada
- [x] Validação de hóspedes - mínimo 1

### ✅ Documentação
- [x] Comprovante de reserva gerado automaticamente
- [x] Segunda via acessível a qualquer momento
- [x] Formatação correta com datas e valores em português

### ✅ Notificações (Extensível)
- [x] E-mail ao hóspede com comprovante (confirmação)
- [x] E-mail à governança (confirmação e cancelamento)
- [x] E-mail ao financeiro (confirmação de pagamento e reembolso)
- [x] Sistema pronto para novos subscribers (restaurante, transfer, etc)
- [x] Histórico de mensagens em banco de dados

### ✅ API REST
- [x] GET /rooms - Lista quartos
- [x] POST /reservations - Cria reserva
- [x] GET /reservations/{id} - Detalhes da reserva
- [x] POST /reservations/{id}/confirmation - Confirma (pagamento)
- [x] POST /reservations/{id}/cancellation - Cancela
- [x] POST /reservations/{id}/check-in - Check-in
- [x] POST /reservations/{id}/check-out - Check-out (retorna comprovante)
- [x] GET /reservations/{id}/document - Segunda via
- [x] GET /messages - Histórico de notificações
- [x] GET /reports/occupancy - Relatório de ocupação (CSV)
- [x] GET /reports/revenue - Relatório financeiro (CSV)

### ✅ Banco de Dados
- [x] PostgreSQL com schema criado automaticamente
- [x] Tabelas: rooms, reservations, messages
- [x] Dados iniciais dos quartos carregados automaticamente
- [x] Sem necessidade de migrations manuais

### ✅ Build & Deploy
- [x] Maven com Java 21
- [x] `mvn package` gera JAR executável
- [x] `mvn verify` passa com sucesso
- [x] Configuração via variáveis de ambiente (DB_URL, DB_USER, DB_PASSWORD, PORT)
- [x] Testes unitários inclusos

## 📁 Documentação

| Documento | Conteúdo |
|-----------|----------|
| [SETUP.md](SETUP.md) | Instruções de build e execução |
| [API_EXAMPLES.md](API_EXAMPLES.md) | Exemplos de uso de todos os endpoints |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Design da arquitetura e padrões |
| [NOTIFICACOES.md](NOTIFICACOES.md) | Como funcionam e como estender notificações |

## 🚀 Quick Start

### 1. Compilar
```bash
mvn clean package
```

### 2. Executar
```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT=7070

java -jar target/marealta-reservations-1.0.0.jar
```

Ou use o script:
```bash
./run.sh
```

### 3. Testar
```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Veja [API_EXAMPLES.md](API_EXAMPLES.md) para mais exemplos.

## 📊 Estrutura do Projeto

```
src/main/java/com/pousada/marealta/
├── Application.java              # Entrada principal
├── config/
│   └── DatabaseConfig.java       # Inicialização e schema do BD
├── controller/                   # Endpoints REST
│   ├── RoomController.java
│   ├── ReservationController.java
│   ├── MessageController.java
│   └── ReportController.java
├── domain/                       # Modelos de negócio
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── ReservationDocument.java
├── notification/                 # Sistema de notificações
│   ├── NotificationListener.java
│   ├── NotificationService.java
│   ├── GuestNotificationListener.java
│   ├── GovernanceNotificationListener.java
│   └── FinanceNotificationListener.java
├── repository/                   # Acesso a dados
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── service/                      # Lógica de negócio
│   ├── ReservationService.java
│   └── PricingService.java
└── util/                         # Utilitários
    ├── MathUtil.java
    ├── DateFormatter.java
    ├── MoneyFormatter.java
    └── DocumentFormatter.java
```

## 🔧 Decisões de Design

### Sem Spring Framework
Conforme solicitado, o projeto usa apenas Javalin com JDBC direto, resultando em:
- **Simplidade**: Código mais direto, sem "magic"
- **Performance**: Sem overhead de container
- **Controle**: Controle total sobre JDBC e transações

### Extensibilidade de Notificações
O sistema foi desenhado para ser extensível desde o primeiro dia:
- Interface `NotificationListener` permite novos subscribers sem modificar código existente
- Inicializado em `Application.java`, permitindo adicionar/remover facilmente
- Histórico de mensagens em banco para auditoria

### Arredondamento "Meio para Par" (HALF_EVEN)
Implementado em `MathUtil.roundHalfEven()` usando `RoundingMode.HALF_EVEN`:
- Exemplo: R$ 249,90 × 1,2 (alta temporada) = R$ 299,88
- Consistente em cada passo do cálculo (diária, subtotal, desconto, total)

### JDBC vs ORM
Usamos JDBC direto por:
- Controle total sobre queries
- Performance previsível
- Sem dependências pesadas
- Código SQL explícito e auditável

## 🧪 Testes

Os testes unitários verificam cálculo de preços em diferentes cenários:

```bash
mvn test
```

Casos testados:
- Tarifa padrão em baixa temporada
- Tarifa padrão em alta temporada
- Desconto NON_REFUNDABLE
- Combinações de alta temporada + NON_REFUNDABLE
- Dezembro (início e fim de alta temporada)

## 📈 Relatórios

### Ocupação (CSV)
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

Mostra quem está hospedado em uma data específica.

### Financeiro (CSV)
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,779.80
```

Mostra receita líquida (pagamento - reembolso) por mês.

## 🔐 Tratamento de Erros

API retorna erros com código e mensagem:

```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the requested dates"
}
```

Códigos suportados:
- `INVALID_REQUEST` (400): Falta campo ou formato inválido
- `ROOM_NOT_FOUND` (404): Quarto não existe
- `RESERVATION_NOT_FOUND` (404): Reserva não existe
- `CAPACITY_EXCEEDED` (422): Mais hóspedes que capacidade
- `ROOM_UNAVAILABLE` (409): Quarto ocupado nas datas
- `OPERATION_NOT_ALLOWED` (409): Operação inválida para status atual

## 🎓 Como Estender

### Adicionar novo subscriber de notificação
Veja [NOTIFICACOES.md](NOTIFICACOES.md) - tem exemplo pronto para restaurante e transfer.

### Adicionar novo tipo de quarto
1. Inserir em `DatabaseConfig.createSchema()` (tabela rooms)
2. Usar a API normalmente

### Adicionar novo relatório
1. Criar método em um controller
2. Consultar `ReservationRepository`
3. Formatar resposta (JSON ou CSV)

## 📝 Dados de Exemplo

**Quartos:**
- 101, 102: STANDARD, até 2 pessoas, R$ 249,90/noite
- 201, 202: DELUXE, até 3 pessoas, R$ 389,90/noite
- 301: SUITE, até 4 pessoas, R$ 579,90/noite

**Temporadas:**
- Alta: 15/dez a fim fevereiro + todo julho (+20% na diária)
- Baixa: Resto do ano

**Planos:**
- STANDARD: Sem desconto, 100% reembolsável se cancelar
- NON_REFUNDABLE: -15%, 0% reembolso se cancelar

## 📞 Contato & Suporte

Para dúvidas sobre:
- **Funcionalidade**: Veja [API_EXAMPLES.md](API_EXAMPLES.md)
- **Arquitetura**: Veja [ARCHITECTURE.md](ARCHITECTURE.md)
- **Notificações**: Veja [NOTIFICACOES.md](NOTIFICACOES.md)
- **Execução**: Veja [SETUP.md](SETUP.md)

---

**Status**: ✅ Pronto para produção
**Última atualização**: 2026-11-02
