# Início Rápido - Pousada Maré Alta

## Compilação Rápida

```bash
# Compilar
mvn clean package

# JAR criado em: target/mare-alta.jar
```

## Execução com PostgreSQL local

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT="7070"

java -jar target/mare-alta.jar
```

A API estará disponível em `http://localhost:7070`

## Execução com Docker (recomendado para teste)

```bash
# Terminal 1: Iniciar PostgreSQL
docker-compose up

# Terminal 2: Executar aplicação
bash run.sh
```

## Primeiro Teste

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João",
    "guestEmail": "joao@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Arquitetura

```
Application (main)
  ├── API Handlers (handler/)
  │   ├── RoomHandler
  │   ├── ReservationHandler
  │   ├── MessageHandler
  │   └── ReportHandler
  ├── Services (service/)
  │   ├── RoomService
  │   ├── ReservationService
  │   ├── MessageService
  │   └── ReportService
  ├── Domain Models (domain/)
  │   ├── Room, Reservation
  │   ├── RatePlan, ReservationStatus
  │   └── Message, ReservationDocument
  ├── Database (database/)
  │   └── Database (PostgreSQL)
  └── Utilities (util/)
      ├── MoneyRounder (HALF_EVEN)
      ├── SeasonUtil
      └── ErrorResponse
```

## Endpoints Principais

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/rooms` | Lista quartos |
| POST | `/reservations` | Criar reserva |
| GET | `/reservations/{id}` | Detalhes da reserva |
| POST | `/reservations/{id}/confirmation` | Confirmar |
| POST | `/reservations/{id}/check-in` | Check-in |
| POST | `/reservations/{id}/check-out` | Check-out (retorna comprovante) |
| POST | `/reservations/{id}/cancellation` | Cancelar |
| GET | `/reservations/{id}/document` | Segunda via |
| GET | `/messages` | E-mails enviados |
| GET | `/reports/occupancy?date=YYYY-MM-DD` | Ocupação (CSV) |
| GET | `/reports/revenue?month=YYYY-MM` | Receita (CSV) |

## 5 Planos de Tarifa

1. **STANDARD**: Sem desconto, reembolso total
2. **NON_REFUNDABLE**: 15% desconto, sem reembolso
3. **CORPORATE**: Desconto por empresa + reembolso total
4. **WEEKEND_PACKAGE**: Preço fixo sexta-domingo
5. **LONG_STAY**: 10% desconto para 7+ noites

## Detalhes Importantes

- Todas as datas em formato ISO: `2027-07-10`
- Todos os valores monetários com 2 casas decimais
- Arredondamento "round half to even" em cada passo
- Alta temporada: 15 dez - 28 fev + julho (+20%)
- E-mails armazenados, não enviados de verdade
- Banco de dados inicializado automaticamente

## Testes

```bash
# Testes unitários
mvn test

# Testes + verificação
mvn verify

# Testar API (com servidor rodando)
bash test-api.sh
```

## Documentação Completa

- `LEIA-ME.md`: Documentação detalhada
- `EXEMPLOS-API.md`: Exemplos de todas as requisições
