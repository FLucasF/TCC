# Início Rápido - Pousada Maré Alta

## TL;DR

```bash
# 1. Subir o banco de dados
docker-compose up -d

# 2. Executar a aplicação
java -jar target/reservas-1.0.0.jar

# 3. Testar
curl http://localhost:7070/rooms
```

## O que foi construído

Sistema completo de reservas com:

✅ **5 quartos** em 3 categorias (STANDARD, DELUXE, SUITE)  
✅ **API REST** completa (Javalin)  
✅ **Cálculo automático** de preços com sazonalidade e descontos  
✅ **Sistema de notificações extensível** via eventos  
✅ **3 destinatários** de e-mail: hóspede, governança, financeiro  
✅ **Relatórios CSV** (ocupação e receita)  
✅ **Persistência** em PostgreSQL  
✅ **Testes unitários** validando lógica de negócio  

## Arquitetura Principal

O sistema usa **Event-Driven Architecture** para notificações:

```
Confirmação de Reserva
        ↓
Publica ReservationConfirmedEvent
        ↓
┌───────┴────────┬─────────────┐
↓                ↓             ↓
Guest          Housekeeping  Finance
Listener       Listener      Listener
↓                ↓             ↓
E-mail         E-mail        E-mail
```

**Por quê?** Adicionar restaurante ou transfer é criar uma classe nova, sem mexer no resto:

```java
// Adicionar novo destinatário = 1 classe + 1 linha
eventPublisher.subscribe(new RestaurantNotificationListener(...));
```

## Estrutura de Arquivos

```
/workspace/
├── src/
│   ├── main/java/com/pousadamarealta/
│   │   ├── domain/          # Modelos, enums, eventos
│   │   ├── service/         # Lógica de negócio
│   │   ├── repository/      # Acesso a dados
│   │   ├── controller/      # REST API
│   │   └── App.java         # Main
│   └── test/java/           # Testes
├── target/
│   └── reservas-1.0.0.jar   # JAR executável (9.4MB)
├── pom.xml                  # Maven config
├── docker-compose.yml       # PostgreSQL local
├── README.md                # Documentação completa
├── ARCHITECTURE.md          # Decisões arquiteturais
├── EXAMPLES.md              # Exemplos de API
└── QUICKSTART.md            # Este arquivo
```

## Como Rodar

### Opção 1: Com Docker (recomendado)

```bash
# Terminal 1: Banco de dados
docker-compose up -d

# Terminal 2: Aplicação
java -jar target/reservas-1.0.0.jar
```

### Opção 2: PostgreSQL já instalado

```bash
# Criar banco
createdb pousada

# Executar com conexão customizada
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=seu_usuario \
DB_PASSWORD=sua_senha \
java -jar target/reservas-1.0.0.jar
```

### Opção 3: Desenvolvimento com rebuild

```bash
# Watch mode (requer ferramenta externa)
mvn clean package -DskipTests && java -jar target/reservas-1.0.0.jar
```

## Fluxo Completo de Teste

```bash
# 1. Listar quartos disponíveis
curl http://localhost:7070/rooms

# 2. Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
# Retorna: { "id": 1, "status": "PENDING", "total": 779.80, ... }

# 3. Confirmar reserva (simula pagamento recebido)
curl -X POST http://localhost:7070/reservations/1/confirmation
# Retorna: { "id": 1, "status": "CONFIRMED", "amountPaid": 779.80, ... }

# 4. Verificar e-mails enviados automaticamente
curl http://localhost:7070/messages
# Retorna: 3 mensagens (hóspede, governança, financeiro)

# 5. Fazer check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# 6. Fazer check-out (recebe comprovante)
curl -X POST http://localhost:7070/reservations/1/check-out
# Retorna: comprovante formatado

# 7. Relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"

# 8. Relatório financeiro do mês
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

## Endpoints Principais

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/rooms` | Lista quartos |
| POST | `/reservations` | Cria reserva (→ 201) |
| GET | `/reservations/{id}` | Consulta reserva |
| POST | `/reservations/{id}/confirmation` | Confirma reserva |
| POST | `/reservations/{id}/cancellation` | Cancela reserva |
| POST | `/reservations/{id}/check-in` | Check-in |
| POST | `/reservations/{id}/check-out` | Check-out (→ comprovante) |
| GET | `/reservations/{id}/document` | Segunda via |
| GET | `/messages` | E-mails enviados |
| GET | `/reports/occupancy?date=...` | Ocupação (CSV) |
| GET | `/reports/revenue?month=...` | Receita (CSV) |

## Regras de Negócio Implementadas

### Preços
- **Baixa temporada**: preço da tabela
- **Alta temporada** (+20%):
  - 15/dez a fim de fev
  - Julho inteiro
- **Plano não reembolsável**: -15% sobre total

### Estados da Reserva
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
   ↓
CANCELLED
```

### Notificações Automáticas

**Ao confirmar:**
- ✉️ Hóspede: comprovante completo
- ✉️ Governança: datas e número de hóspedes
- ✉️ Financeiro: valor recebido

**Ao cancelar (se estava confirmada):**
- ✉️ Governança: datas que ficaram livres
- ✉️ Financeiro: valor de reembolso (se houver)

### Validações
- ❌ Mais hóspedes que capacidade → 422 CAPACITY_EXCEEDED
- ❌ Quarto já reservado → 409 ROOM_UNAVAILABLE
- ❌ Quarto inexistente → 404 ROOM_NOT_FOUND
- ❌ Operação inválida → 409 OPERATION_NOT_ALLOWED

## Próximos Passos

### Para adicionar Restaurante
```java
// 1. Criar listener
public class RestaurantNotificationListener implements EventListener {
    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent e) {
            Message msg = Message.email(
                "restaurante@pousadamarealta.com.br",
                "Reserva confirmada - café da manhã",
                String.format("Check-in: %s\nHóspedes: %d",
                    formatDate(e.getReservation().getCheckIn()),
                    e.getReservation().getGuests())
            );
            messageRepository.save(msg);
        }
    }
}

// 2. Registrar no App.java
eventPublisher.subscribe(new RestaurantNotificationListener(messageRepository));
```

### Para adicionar Transfer
Mesma ideia! Crie `TransferNotificationListener` e registre.

## Problemas Comuns

### Porta 7070 já em uso
```bash
PORT=8080 java -jar target/reservas-1.0.0.jar
```

### PostgreSQL não conecta
```bash
# Verificar se está rodando
docker-compose ps

# Ver logs
docker-compose logs postgres

# Parar e reiniciar
docker-compose down
docker-compose up -d
```

### Recompilar depois de mudanças
```bash
mvn clean package -DskipTests
java -jar target/reservas-1.0.0.jar
```

## Testes

```bash
# Testes unitários (rápidos)
mvn test

# Testes de integração (requer Docker)
mvn verify
```

## Documentação Completa

- **README.md**: Documentação geral e referência da API
- **ARCHITECTURE.md**: Explicação detalhada da arquitetura de eventos
- **EXAMPLES.md**: Exemplos práticos de uso dos endpoints

## Suporte

O sistema está pronto para produção com:
- ✅ Persistência em banco relacional
- ✅ Schema criado automaticamente
- ✅ Tratamento de erros
- ✅ Validações de regras de negócio
- ✅ Logs configurados
- ✅ JAR executável standalone

Para deploy:
```bash
# Configurar variáveis de ambiente de produção
export DB_URL=jdbc:postgresql://prod-server:5432/pousada
export DB_USER=pousada_user
export DB_PASSWORD=senha_segura
export PORT=80

# Executar
java -jar reservas-1.0.0.jar
```
