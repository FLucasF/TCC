# Guia de Testes Manuais

Roteiro para testar todas as funcionalidades do sistema manualmente.

## Preparação

1. Inicie o banco de dados:
```bash
docker-compose up -d
```

2. Compile o projeto:
```bash
mvn clean package
```

3. Execute o servidor (terminal 1):
```bash
./run.sh
```

4. Em outro terminal, prepare ferramentas:
```bash
# Instale jq para formatar JSON (opcional)
sudo apt-get install jq

# Ou use um cliente REST como Postman
```

## Testes

### 1. Listar Quartos

```bash
curl http://localhost:7070/rooms | jq
```

**Esperado**: Lista com 5 quartos (101, 102, 201, 202, 301) com tipos e capacidades.

### 2. Criar Reserva STANDARD (Baixa Temporada)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Silva",
    "guestEmail": "ana@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

**Esperado**:
- Status 201
- id: 1
- status: PENDING
- nights: 2
- subtotal: 499.80 (249.90 * 2)
- discount: 0.00
- total: 499.80
- amountPaid: 0.00

### 3. Criar Reserva NON_REFUNDABLE (Alta Temporada)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "João Santos",
    "guestEmail": "joao@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq
```

**Esperado**:
- Status 201
- id: 2
- nights: 2
- subtotal: 1439.76 (579.90 * 1.20 * 2, alta temporada em julho)
- discount: 215.96 (1439.76 * 15%)
- total: 1223.80
- status: PENDING

### 4. Confirmar Reserva 1

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

**Esperado**:
- status: CONFIRMED
- amountPaid: 499.80

**Verificar e-mail** (sem modo teste):
```bash
curl http://localhost:7070/messages | jq
```

Deve ter:
- Mensagem para ana@example.com (status SENT)
- Mensagem para arquivo@pousadamarealta.com.br com assunto "[Cópia] ..." (status SENT)

### 5. Tentar Confirmar de Novo (Deve Falhar)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**Esperado**: Status 409, erro OPERATION_NOT_ALLOWED

### 6. Fazer Check-in da Reserva 1

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

**Esperado**: status: CHECKED_IN

### 7. Fazer Check-out da Reserva 1

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

**Esperado**: Comprovante com:
- title: "Comprovante de reserva"
- reservationId: 1
- nights: 2
- subtotal: 499.80
- total: 499.80

### 8. Testar E-mail Inválido

Criar reserva com e-mail errado:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Teste Email Inválido",
    "guestEmail": "teste@gmail",
    "guestCpf": "99988877766",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq
```

**Esperado**: Status 201, reserva 3 criada (pendente)

Confirmar:
```bash
curl -X POST http://localhost:7070/reservations/3/confirmation | jq
```

**Esperado**: Status 200, confirmada

Verificar mensagens:
```bash
curl http://localhost:7070/messages | jq '.[] | {to, status, subject}'
```

**Esperado**: Mensagem com status BLOCKED para teste@gmail

### 9. Testar Capacidade Máxima

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Muita Gente",
    "guestEmail": "test@example.com",
    "guestCpf": "12312312312",
    "checkIn": "2027-03-20",
    "checkOut": "2027-03-21",
    "guests": 3,
    "ratePlan": "STANDARD"
  }'
```

**Esperado**: Status 422, CAPACITY_EXCEEDED (quarto STANDARD comporta 2)

### 10. Testar Quarto Indisponível

```bash
# Já temos reserva de 10 a 12 de março no quarto 101
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Conflito de Data",
    "guestEmail": "conflito@example.com",
    "guestCpf": "22233344455",
    "checkIn": "2027-03-11",
    "checkOut": "2027-03-13",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

**Esperado**: Status 409, ROOM_UNAVAILABLE

### 11. Testar Cancelamento com Reembolso

Confirmar reserva 2:
```bash
curl -X POST http://localhost:7070/reservations/2/confirmation | jq
```

Cancelar:
```bash
curl -X POST http://localhost:7070/reservations/2/cancellation | jq
```

**Esperado**:
- status: CANCELLED
- refund: 0.00 (NON_REFUNDABLE não tem reembolso)

### 12. Testar Cancelamento com Reembolso Total

Criar e confirmar nova reserva STANDARD:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Cancela",
    "guestEmail": "cancela@example.com",
    "guestCpf": "33344455566",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.id'
```

Confirmar (assume ID 4):
```bash
curl -X POST http://localhost:7070/reservations/4/confirmation | jq
```

Cancelar:
```bash
curl -X POST http://localhost:7070/reservations/4/cancellation | jq
```

**Esperado**: refund = amountPaid (100% de reembolso STANDARD)

### 13. Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

**Esperado**: CSV
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
total,1/5
```

### 14. Relatório Financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-03
```

**Esperado**: CSV com receita de março (reserva 1 check-out)

### 15. Testar Modo de Teste

1. Pare o servidor (Ctrl+C)

2. Inicie com modo teste:
```bash
HOTEL_TEST_MODE=true ./run.sh
```

3. Crie e confirme uma reserva:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Test",
    "guestEmail": "maria@example.com",
    "guestCpf": "44455566677",
    "checkIn": "2027-06-10",
    "checkOut": "2027-06-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.id'
```

4. Confirme:
```bash
curl -X POST http://localhost:7070/reservations/5/confirmation
```

5. Verifique mensagens:
```bash
curl http://localhost:7070/messages | jq '.[-1]'
```

**Esperado**:
- to: "testes@pousadamarealta.com.br"
- subject começa com "[TESTE]"
- body começa com "Destinatário original: maria@example.com"
- Apenas 1 mensagem (sem cópia para arquivo)

### 16. Testar Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document | jq
```

**Esperado**: Mesmo comprovante que foi enviado no check-out

### 17. Testar Erros de Validação

Data inválida:
```bash
curl http://localhost:7070/reports/occupancy?date=invalid
```
**Esperado**: Status 400, INVALID_REQUEST

Mês inválido:
```bash
curl http://localhost:7070/reports/revenue?month=invalid
```
**Esperado**: Status 400, INVALID_REQUEST

### 18. Testar Reserva Não Encontrada

```bash
curl http://localhost:7070/reservations/999
```

**Esperado**: Status 404, RESERVATION_NOT_FOUND

## Checklist de Validação

- [ ] Todos os 5 quartos listados corretamente
- [ ] Reserva STANDARD calculada sem desconto
- [ ] Reserva NON_REFUNDABLE com 15% desconto
- [ ] Alta temporada aplica +20% (julho)
- [ ] E-mail válido envia mensagem (SENT)
- [ ] E-mail inválido bloqueia (BLOCKED)
- [ ] Cópia para arquivo criada (modo normal)
- [ ] Modo teste redireciona para testes@
- [ ] Capacidade máxima validada
- [ ] Conflito de datas detectado
- [ ] Reembolso STANDARD = 100%
- [ ] Reembolso NON_REFUNDABLE = 0%
- [ ] Relatório ocupação correto
- [ ] Relatório financeiro correto
- [ ] Segunda via do comprovante disponível
- [ ] Erros de validação retornam código certo
- [ ] Transições de status funcionam

## Debugging

Se algo falhar:

1. **Verificar logs do servidor**: olhar stdout
2. **Conectar ao banco**: 
   ```bash
   psql -h localhost -U postgres -d pousada
   SELECT * FROM reservations;
   SELECT * FROM messages;
   ```
3. **Verificar variáveis de ambiente**: `echo $HOTEL_TEST_MODE`
4. **Verificar portas**:
   ```bash
   lsof -i :5432  # PostgreSQL
   lsof -i :7070  # Servidor
   ```

## Notas Importantes

- Reservas are sequenciais (id 1, 2, 3...)
- Mensagens também são sequenciais
- Arredondamento é "meio para o par" (HALF_EVEN)
- Datas em ISO: YYYY-MM-DD
- Valores em JSON com 2 casas decimais
- E-mail bloqueado: não impede criar/confirmar reserva
