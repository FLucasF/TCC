# Pousada Maré Alta - Sistema de Reservas

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Arquitetura

O sistema é composto por:

- **API REST** (Javalin 6.7.0): endpoints para gerenciar reservas, quartos, mensagens e relatórios
- **Banco de Dados** (PostgreSQL 18): armazena reservas, quartos, mensagens
- **Serviços**: lógica de negócio para reservas, mensagens, relatórios
- **Repositórios**: acesso aos dados do banco
- **Utilidades**: formatação monetária, validação de e-mail, cálculos de datas

## Compilação

```bash
mvn clean package
```

Gera um JAR executável: `target/mare-alta.jar`

## Execução

```bash
java -jar target/mare-alta.jar
```

### Variáveis de ambiente

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: usuário do banco de dados
- `DB_PASSWORD`: senha do banco de dados
- `PORT`: porta de execução (padrão: 7070)
- `HOTEL_TEST_MODE`: ativa modo de teste, redireciona e-mails para `testes@pousadamarealta.com.br`
- `HOTEL_ARCHIVE_DISABLED`: desabilita cópia automática para arquivo (padrão: habilitado)

Exemplo completo:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=8080 \
HOTEL_TEST_MODE=true \
java -jar target/mare-alta.jar
```

## Testes

```bash
mvn verify
```

Executa 20 testes unitários:
- Validação de e-mail
- Cálculos monetários com arredondamento HALF_EVEN
- Detecção de alta temporada
- Formatação de CPF

## API REST

### Quartos

**GET /rooms**
- Retorna lista de todos os quartos

### Reservas

**POST /reservations**
- Cria nova reserva pendente
- Corpo: `roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan`
- Datas em formato ISO (2027-07-10)

**GET /reservations/{id}**
- Retorna detalhes da reserva

**POST /reservations/{id}/confirmation**
- Confirma reserva pendente
- Envia e-mail de confirmação com comprovante

**POST /reservations/{id}/check-in**
- Faz check-in de reserva confirmada

**POST /reservations/{id}/check-out**
- Faz check-out e retorna comprovante

**POST /reservations/{id}/cancellation**
- Cancela reserva pendente ou confirmada
- Reembolso: STANDARD recebe 100%, NON_REFUNDABLE recebe 0%

**GET /reservations/{id}/document**
- Retorna comprovante da reserva (segunda via)

### Mensagens

**GET /messages**
- Retorna lista de mensagens enviadas e bloqueadas
- Campos: `id, channel, to, subject, body, status` (SENT ou BLOCKED)

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
- Ocupação do hotel em uma data
- Formato: CSV com encoding UTF-8
- Colunas: room, type, reservation, check_in, check_out
- Última linha: total,X/5

**GET /reports/revenue?month=2027-07**
- Receita do mês (reservas que começaram no mês)
- Formato: CSV com encoding UTF-8
- Colunas: reservation, room, check_in, check_out, status, amount
- Última linha: total,XXXX.XX

## Regras de Negócio

### Quartos
- 101, 102: STANDARD (2 pessoas) - R$ 249,90/noite
- 201, 202: DELUXE (3 pessoas) - R$ 389,90/noite
- 301: SUITE (4 pessoas) - R$ 579,90/noite

### Temporadas
- Alta temporada: 15 dez a 29 fev + mês de julho
- Preço 20% mais caro na alta temporada

### Planos de tarifa
- STANDARD: sem desconto, reembolso total se cancelado após confirmação
- NON_REFUNDABLE: 15% desconto, sem reembolso se cancelado

### Valores monetários
- Arredondamento "meio para o par" (HALF_EVEN) em cada passo
- Preço da noite > soma das noites > desconto > total final

### Validação de e-mail
- Deve ter exatamente 1 `@`
- Algo antes do `@`
- Domínio com pelo menos 1 ponto (ex: `ana@gmail.com`)
- E-mail inválido: mensagem bloqueada

### Modo de teste
- Com `HOTEL_TEST_MODE=true`:
  - Todos os e-mails vão para `testes@pousadamarealta.com.br`
  - Assunto começa com `[TESTE]`
  - Primeira linha do corpo: "Destinatário original: email@real.com"
  - Sem cópia automática para arquivo
  - E-mail bloqueado continua com endereço original

### Arquivo
- Com `HOTEL_ARCHIVE_DISABLED` não definido (padrão):
  - Cópia automática de cada e-mail para `arquivo@pousadamarealta.com.br`
  - Assunto começa com `[Cópia]`
  - Aparece como outra mensagem na lista (status SENT)
  - Não cria cópia de mensagens bloqueadas ou em modo teste

## Fluxo de Reserva

1. **PENDING**: Reserva criada, aguardando pagamento
2. **CONFIRMED**: Pagamento recebido, e-mail de confirmação enviado
3. **CHECKED_IN**: Hóspede fez check-in
4. **CHECKED_OUT**: Hóspede fez check-out
5. **CANCELLED**: Reserva cancelada (antes do check-in), com reembolso conforme plano

## Códigos de Erro

| Status | Código | Descrição |
|--------|--------|-----------|
| 400 | INVALID_REQUEST | Campo faltando, datas inválidas, formato errado |
| 404 | ROOM_NOT_FOUND | Quarto não existe |
| 404 | RESERVATION_NOT_FOUND | Reserva não existe |
| 409 | ROOM_UNAVAILABLE | Quarto já reservado nessas datas |
| 409 | OPERATION_NOT_ALLOWED | Operação não permitida para o status atual |
| 422 | CAPACITY_EXCEEDED | Mais hóspedes do que o quarto comporta |
