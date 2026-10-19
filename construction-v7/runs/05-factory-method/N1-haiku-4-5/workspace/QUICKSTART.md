# Quick Start - Pousada Maré Alta

## Começar em 2 Minutos

### 1. Build (primeira vez)
```bash
mvn clean package
```

### 2. Rodar o servidor
```bash
java -jar target/marea-alta.jar
```
Servidor ativo em: http://localhost:7070

### 3. Testar um endpoint
```bash
curl http://localhost:7070/rooms
```

## Exemplo: Fazer uma Reserva Completa

### 1. Criar reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta: `{"id": 1, "total": 599.76, "status": "PENDING", ...}`

### 2. Confirmar (com pagamento)
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}'
```

Email enviado! Veja em:
```bash
curl http://localhost:7070/messages
```

### 3. Check-in na pousada
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 4. Check-out e comprovante
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Retorna o comprovante completo!

## Diferenças por Tipo de Hóspede

### Com CPF (Pessoa Física)
```json
{
  "guestCpf": "111.222.333-44"
}
→ Comprovante de reserva
```

### Com CNPJ (Empresa)
```json
{
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Empresa Ltda"
}
→ Fatura com ISS retido (5%)
```

### Com Passaporte (Estrangeiro)
```json
{
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom"
}
→ Booking Receipt (em inglês)
```

## Relatórios

### Ocupação de um dia
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```
Saída em CSV com quartos ocupados.

### Receita de um mês
```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```
Saída em CSV com reservas pagas.

## Erros Comuns

| Erro | Causa | Solução |
|------|-------|---------|
| ROOM_NOT_FOUND | Quarto não existe | Verifique números: 101-102, 201-202, 301 |
| ROOM_UNAVAILABLE | Quarto já reservado | Escolha outras datas |
| CAPACITY_EXCEEDED | Muitos hóspedes | Reduz guests ou escolhe quarto maior |
| OPERATION_NOT_ALLOWED | Operação inválida | Verifique estado: PENDING→CONFIRMED→CHECKED_IN→CHECKED_OUT |

## Temperaturas de Preço

| Período | Multiplicador |
|---------|---------------|
| 15/dez - 28/fev | +20% |
| julho | +20% |
| resto do ano | -0% |

Exemplo:
- STANDARD (R$ 249,90) em julho = R$ 299,88
- STANDARD (R$ 249,90) em setembro = R$ 249,90

## Próximos Passos

Veja `README.md` para documentação completa.
Veja `SUMMARY.md` para resumo técnico.
