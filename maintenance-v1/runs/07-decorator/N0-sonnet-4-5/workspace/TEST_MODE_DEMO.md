# Como testar o Modo de Teste (HOTEL_TEST_MODE)

## Modo Normal (padrão)

Sem definir a variável de ambiente, o sistema funciona normalmente:

```bash
mvn verify
java -jar target/hotel.jar
```

Neste modo:
- E-mails válidos vão para o endereço real
- E-mails inválidos são bloqueados (status BLOCKED)
- Cada e-mail enviado gera uma cópia para `arquivo@pousadamarealta.com.br`

## Modo de Teste (HOTEL_TEST_MODE=true)

Para ativar o modo de teste:

```bash
export HOTEL_TEST_MODE=true
java -jar target/hotel.jar
```

Ou em uma única linha:

```bash
HOTEL_TEST_MODE=true java -jar target/hotel.jar
```

Neste modo:
- E-mails válidos vão para `testes@pousadamarealta.com.br`
- O assunto começa com `[TESTE] `
- A primeira linha do corpo é `Destinatário original: <endereço-original>`
- Não há cópia para arquivo
- E-mails inválidos continuam bloqueados (com endereço original)

## Exemplo de teste

1. Inicie o servidor em modo de teste:
```bash
HOTEL_TEST_MODE=true PORT=7070 java -jar target/hotel.jar
```

2. Crie e confirme uma reserva:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

curl -X POST http://localhost:7070/reservations/1/confirmation
```

3. Verifique as mensagens:
```bash
curl http://localhost:7070/messages
```

No modo de teste, você verá:
- `to`: `testes@pousadamarealta.com.br`
- `subject`: `[TESTE] Reserva confirmada - Pousada Maré Alta`
- `body`: começa com `Destinatário original: carla@example.com`
- `status`: `SENT`
- Apenas 1 mensagem (sem cópia para arquivo)

## Testando e-mails inválidos

Para testar bloqueio de e-mail inválido, crie uma reserva com e-mail malformado:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@gmail",
    "guestCpf": "222.333.444-55",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'

curl -X POST http://localhost:7070/reservations/2/confirmation
```

A mensagem aparecerá com:
- `to`: `joao@gmail` (endereço original mantido)
- `status`: `BLOCKED`
- Nenhuma cópia ou redirecionamento
