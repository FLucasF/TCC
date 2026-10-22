# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, construído em Java 21 com Javalin e PostgreSQL 18.

## Requisitos

- Java 21+
- Maven 3.6+
- PostgreSQL 18+

## Compilação e Empacotamento

```bash
mvn clean package
```

Gera um arquivo JAR executável: `target/mare-alta.jar`

## Execução

### Com Docker Compose (Banco de dados)

```bash
docker-compose up -d
```

### Rodando a aplicação

```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="marealta"
export DB_PASSWORD="password123"
export PORT="7070"
export HOTEL_TEST_MODE="false"  # ou "true" para modo de teste

java -jar target/mare-alta.jar
```

A aplicação estará disponível em `http://localhost:7070`

## Variáveis de Ambiente

- `DB_URL`: URL de conexão ao PostgreSQL (obrigatório)
- `DB_USER`: Usuário do banco de dados (obrigatório)
- `DB_PASSWORD`: Senha do banco de dados (obrigatório)
- `PORT`: Porta de execução (padrão: 7070)
- `HOTEL_TEST_MODE`: Ativa modo de teste - redireciona e-mails para `testes@pousadamarealta.com.br`

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos com preços e capacidades

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Obtém detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirma a reserva e envia comprovante por e-mail
- `POST /reservations/{id}/cancellation` - Cancela a reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out e retorna comprovante
- `GET /reservations/{id}/document` - Obtém segunda via do comprovante (apenas para reservas confirmadas)

### Mensagens

- `GET /messages` - Lista todas as mensagens enviadas/bloqueadas

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação em CSV
- `GET /reports/revenue?month=2027-07` - Relatório financeiro em CSV

## Exemplo de Criação de Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Regras de Negócio

### Temporada Alta

- 15 de dezembro até 28 de fevereiro
- Mês de julho inteiro
- Acréscimo de 20% no valor das diárias

### Planos de Tarifa

- **STANDARD**: Sem desconto. Cancelamento resulta em reembolso integral.
- **NON_REFUNDABLE**: 15% de desconto. Cancelamento não gera reembolso.

### Validação de E-mail

Endereços válidos devem ter:
- Um único `@`
- Algo antes do `@`
- Domínio com pelo menos um ponto (ex: gmail.com)

E-mails inválidos ficam com status BLOCKED na lista de mensagens.

### Modo de Teste

Quando `HOTEL_TEST_MODE=true`:
- Todas as mensagens são redirecionadas para `testes@pousadamarealta.com.br`
- O assunto recebe prefixo `[TESTE]`
- A primeira linha do corpo inclui o destinatário original
- Não é gerada cópia para arquivo

### Cópia para Arquivo

Quando `HOTEL_TEST_MODE=false`:
- Toda mensagem SENT gera uma cópia para `arquivo@pousadamarealta.com.br`
- O assunto recebe prefixo `[Cópia]`
- A cópia aparece como mensagem separada na lista

## Arredondamento Monetário

Todos os valores são arredondados para centavos usando o método "meio para par" (HALF_EVEN):
1. Preço de cada noite
2. Soma das noites
3. Valor do desconto
4. Total da reserva

## Estrutura de Dados

### Quartos Disponíveis

| Número | Tipo     | Capacidade | Diária    |
|--------|----------|------------|-----------|
| 101    | STANDARD | 2 pessoas  | R$ 249,90 |
| 102    | STANDARD | 2 pessoas  | R$ 249,90 |
| 201    | DELUXE   | 3 pessoas  | R$ 389,90 |
| 202    | DELUXE   | 3 pessoas  | R$ 389,90 |
| 301    | SUITE    | 4 pessoas  | R$ 579,90 |

## Estados da Reserva

- **PENDING**: Reserva criada, aguardando pagamento
- **CONFIRMED**: Pagamento confirmado, comprovante enviado
- **CHECKED_IN**: Hóspede realizou check-in
- **CHECKED_OUT**: Hóspede realizou check-out
- **CANCELLED**: Reserva cancelada (reembolso pode estar pendente)

## Testes

Execute os testes com:

```bash
mvn verify
```

## Arquitetura

O projeto segue uma arquitetura em camadas:

- **domain/**: Modelos de domínio (Room, Reservation, etc)
- **service/**: Lógica de negócio
- **infrastructure/persistence/**: Persistência em banco de dados
- **infrastructure/messaging/**: Sistema de envio de mensagens com políticas
- **api/**: Controllers/Endpoints
- **util/**: Utilitários (formatação, validação, cálculos)
