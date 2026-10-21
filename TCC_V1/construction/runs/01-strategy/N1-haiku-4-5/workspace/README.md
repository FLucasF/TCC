# Pousada Maré Alta - Sistema de Reservas

Sistema completo de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Compilação e Execução

```bash
# Compilar e empacotar
mvn clean package

# Executar
java -jar target/mare-alta.jar

# Ou com porta customizada
PORT=8080 java -jar target/mare-alta.jar
```

## Características

### Quartos
- 2x STANDARD (101, 102) - 2 pessoas, R$ 249,90/noite
- 2x DELUXE (201, 202) - 3 pessoas, R$ 389,90/noite
- 1x SUITE (301) - 4 pessoas, R$ 579,90/noite

### Planos de Tarifa
1. **STANDARD**: tarifa normal com ajuste de alta temporada
2. **NON_REFUNDABLE**: 15% desconto, não reembolsável
3. **CORPORATE**: desconto por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%)
4. **WEEKEND_PACKAGE**: sexta a domingo, preço fixo (R$ 520/520/1200), 50% refund
5. **LONG_STAY**: 7+ noites, 10% desconto nas diárias

### Alta Temporada
- 15 de dezembro a 28 de fevereiro
- Mês de julho inteiro
- Acréscimo de 20% na diária

## API REST

### Quartos
- `GET /rooms` - Listar todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar pagamento
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação do dia (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Receita do mês (CSV)

## Exemplo de Uso

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{}'

# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}'

# Check-out
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}'
```

## Testes

```bash
# Executar testes
mvn test

# Verificação completa (testes + build)
mvn verify
```

## Arredondamento Financeiro

Todos os valores são arredondados para centavos usando o modo "round half to even" (banker's rounding), em cada etapa do cálculo:
1. Preço de cada noite
2. Subtotal (soma das noites)
3. Desconto
4. Total
5. Devolução
