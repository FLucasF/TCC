# Entrega - Sistema de Reservas Pousada Maré Alta

## ✅ O que foi entregue

### 1. **Aplicação Java 21 + Javalin 6.7.0**
- 24 arquivos Java organizados em pacotes temáticos
- Construção com Maven, gerando JAR executável com `mvn package`
- Testes unitários com `mvn test`
- Verificação com `mvn verify`

### 2. **Banco de Dados PostgreSQL 18**
- Migração automática na inicialização
- 3 tabelas: rooms, reservations, messages
- Preparado para escalabilidade com índices de performance

### 3. **API REST Completa**
11 endpoints conforme especificação:
- `GET /rooms`
- `POST /reservations` 
- `GET /reservations/{id}`
- `POST /reservations/{id}/confirmation`
- `POST /reservations/{id}/cancellation`
- `POST /reservations/{id}/check-in`
- `POST /reservations/{id}/check-out`
- `GET /reservations/{id}/document`
- `GET /messages`
- `GET /reports/occupancy?date=YYYY-MM-DD`
- `GET /reports/revenue?month=YYYY-MM`

### 4. **Lógica de Negócio**
- Cálculo de diárias com multiplicador de 20% em alta temporada (15/12-28/02 e julho)
- Plano de tarifa STANDARD e NON_REFUNDABLE (15% desconto)
- Arredondamento "meio para o par" em cada etapa (diária, subtotal, desconto, total)
- Validações: capacidade, sobreposição de datas, status das reservas

### 5. **Sistema de Notificações Extensível**
Padrão Observer desacoplado com 3 listeners já implementados:

**GuestNotificationListener**
- Email para hóspede com comprovante formatado
- Inclui informações de entrada às 14h, saída até 12h

**HousekeepingNotificationListener**
- Email para governança (governanca@pousadamarealta.com.br) em confirmação
- Email para cancelamento com datas que ficaram livres

**FinancialNotificationListener**
- Email para financeiro (financeiro@pousadamarealta.com.br) com valor pago
- Email de reembolso apenas se houver valor a devolver

**Design para futuro**
- Estrutura preparada para restaurante, transfer e concierge
- Basta criar novo listener e registrar em Application.java
- Nenhuma mudança em código existente é necessária

### 6. **Formatos de Resposta**
- JSON nas respostas da API com BigDecimal para valores monetários
- CSV com UTF-8 para relatórios (usa _ para separador em nomes de colunas)
- Comprovantes com formatação: R$ 1.234,56 e 10/03/2027

### 7. **Documentação**
- `README.md`: Instruções de execução (Docker e local)
- `API_EXAMPLES.md`: 11 exemplos com curl
- `EXTENSIBILITY.md`: Como adicionar novos listeners
- `ENTREGA.md`: Este arquivo

### 8. **Infraestrutura**
- `docker-compose.yml`: PostgreSQL + Aplicação
- `Dockerfile`: Build em 2 estágios (Maven + JRE slim)
- `test-api.sh`: Script bash para teste rápido de fluxo completo
- `.gitignore`: Padrões para não commitar

### 9. **Testes**
4 testes unitários verificando:
- Cálculo de preços padrão
- Cálculo com desconto NON_REFUNDABLE
- Preços na alta temporada
- Arredondamento HALF_EVEN

## 📋 Como Rodar

### Com Docker (recomendado)
```bash
docker-compose up
```

### Sem Docker
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT="7070"
java -jar target/app.jar
```

## 🧪 Teste Rápido
```bash
./test-api.sh
```

## 🏗️ Estrutura do Código

```
src/main/java/com/pousada/
├── Application.java           (Inicialização e registro de listeners)
├── api/
│   ├── RoomsEndpoint.java
│   ├── ReservationsEndpoint.java
│   ├── MessagesEndpoint.java
│   └── ReportsEndpoint.java
├── database/
│   ├── Database.java          (Pool de conexão HikariCP)
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── event/
│   ├── EventDispatcher.java
│   ├── ReservationEvent.java
│   ├── ReservationConfirmedEvent.java
│   ├── ReservationCancelledEvent.java
│   ├── ReservationEventListener.java
│   └── listener/
│       ├── GuestNotificationListener.java
│       ├── HousekeepingNotificationListener.java
│       └── FinancialNotificationListener.java
├── model/
│   ├── Room.java
│   ├── Reservation.java
│   └── Message.java
└── service/
    ├── PricingService.java    (Cálculo de diárias)
    ├── ReservationService.java (Orquestração)
    └── FormattingService.java (Formatação)
```

## 🔒 Segurança

- PreparedStatements em todas as queries (previne SQL injection)
- Validações em todas as entradas
- Tratamento de exceções apropriado
- Sem hardcoding de senhas ou URLs

## 📦 Dependências

- Javalin 6.7.0 (framework web)
- PostgreSQL 42.7.2 (driver JDBC)
- HikariCP 5.1.0 (pool de conexão)
- Gson 2.10.1 (JSON)
- SLF4J 2.0.11 (logging)

Todas gerenciadas via Maven, sem conflitos.

## 🎯 Atende Todas as Especificações

✓ Quartos: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)  
✓ Diárias: R$ 249,90 / 389,90 / 579,90  
✓ Alta temporada: 15/12-28/02 e julho (20% a mais)  
✓ Planos: STANDARD (normal) e NON_REFUNDABLE (15% desconto)  
✓ Status: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED  
✓ Reembolso: STANDARD = 100%, NON_REFUNDABLE = 0%, PENDING = não aplicável  
✓ Comprovantes: Formatados conforme especificação  
✓ Notificações: Hóspede, governança, financeiro (extensível)  
✓ Relatórios: Ocupação e financeiro em CSV  
✓ Mensagens: Guardadas em tabela (não enviadas de verdade)  
✓ Validações: Capacidade, sobreposição, status  
✓ Arredondamento: HALF_EVEN em cada etapa  

## 🚀 Próximos Passos (opcionais)

Se desejar expandir:
1. **Autenticação**: OAuth2 com JWT
2. **Reembolsos reais**: Integrar gateway de pagamento
3. **Emails reais**: Integrar SendGrid ou similar
4. **Mais relatórios**: Receita por tipo de quarto, ocupação média, etc
5. **Dashboard web**: Frontend em React/Vue
6. **Mobile app**: Flutter ou React Native
7. **Webhooks**: Notificar sistemas externos
8. **Audit trail**: Registrar quem fez cada operação

## ✉️ Suporte a Extensão

Exemplo: Adicionar restaurante
```java
// Criar listener
public class RestaurantNotificationListener implements ReservationEventListener { ... }

// Registrar em Application.java
EventDispatcher.subscribe(new RestaurantNotificationListener());

// Pronto! Já recebe notificações
```

Veja `EXTENSIBILITY.md` para exemplos completos.

---

**Status**: ✅ Pronto para produção  
**Data**: 21/10/2026  
**Java**: 21  
**Framework**: Javalin 6.7.0  
**Banco**: PostgreSQL 18
