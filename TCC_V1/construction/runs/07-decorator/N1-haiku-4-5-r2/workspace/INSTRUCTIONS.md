# Pousada Maré Alta - Sistema de Reservas

## Instruções de execução

### Pré-requisitos
- Java 21+
- PostgreSQL 18+

### Compilação

```bash
mvn clean package
```

Gera: `target/app.jar`

### Executar

```bash
java -jar target/app.jar
```

### Variáveis de ambiente

- `DB_URL`: URL JDBC do PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do banco
- `PORT`: Porta (padrão: 7070)
- `HOTEL_TEST_MODE`: Define modo de teste (`true`/`false`)

### Exemplo com Docker

```bash
docker run -d \
  --name postgres \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha \
  -p 5432:5432 \
  postgres:18

export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha"
export PORT="7070"
export HOTEL_TEST_MODE="false"

java -jar target/app.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista quartos disponíveis

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Obter detalhes de reserva
- `POST /reservations/{id}/confirmation` - Confirmar pagamento
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `POST /reservations/{id}/cancellation` - Cancelar
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Ocupação
- `GET /reports/revenue?month=2027-07` - Financeiro

## Testes

```bash
mvn verify
```

## Características

### Quartos
- 101, 102 (STANDARD, 2 pessoas, R$ 249,90/noite)
- 201, 202 (DELUXE, 3 pessoas, R$ 389,90/noite)
- 301 (SUITE, 4 pessoas, R$ 579,90/noite)

### Alta Temporada
- 15/12 a 28/02: +20%
- Julho inteiro: +20%

### Planos de Tarifa
- STANDARD: Reembolso total se cancelado
- NON_REFUNDABLE: 15% desconto, sem reembolso se cancelado

### Validação de E-mail
- Válido: um @ + algo antes + domínio com ponto (ex: user@example.com)
- Inválido: bloqueado na lista de mensagens

### Modo Teste
- Ativa com `HOTEL_TEST_MODE=true`
- Redireciona e-mails para testes@pousadamarealta.com.br
- Adiciona `[TESTE]` no assunto
- Sem cópia para arquivo

### Cópia para Arquivo
- Cria segunda mensagem para arquivo@pousadamarealta.com.br
- Assunto: `[Cópia] {original}`
- Desativada em modo teste
