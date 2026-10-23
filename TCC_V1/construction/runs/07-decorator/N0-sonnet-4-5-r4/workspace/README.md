# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Funcionalidades

- Gestão de quartos (5 quartos: 101, 102, 201, 202, 301)
- Criação e gestão de reservas
- Cálculo automático de preços com alta temporada (15/12 a fim de fevereiro e julho completo)
- Planos de tarifa: STANDARD e NON_REFUNDABLE (15% desconto)
- Fluxo de estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Sistema de mensagens com validação de e-mail
- Relatórios de ocupação e financeiro em CSV

## Sistema de Mensagens

O sistema implementa um mecanismo extensível de regras para envio de mensagens usando o padrão Chain of Responsibility. Três regras estão implementadas desde o primeiro dia:

### Regras Implementadas

1. **Validação de e-mail** (`EmailValidationRule`)
   - E-mails inválidos (sem @, sem domínio com ponto, etc.) são marcados como BLOCKED
   - Mensagens bloqueadas aparecem na lista com status BLOCKED para que a recepção possa entrar em contato
   - Um e-mail é considerado válido quando tem exatamente um @, algo antes dele e um domínio com pelo menos um ponto depois

2. **Cópia para arquivo** (`ArchiveCopyRule`)
   - Toda mensagem enviada (não bloqueada) gera automaticamente uma cópia para arquivo@pousadamarealta.com.br
   - O assunto da cópia começa com "[Cópia] "
   - A cópia aparece na lista de mensagens logo após a mensagem original
   - Mensagens bloqueadas não geram cópia

3. **Modo de teste** (`TestModeRule`)
   - Quando HOTEL_TEST_MODE=true, nenhuma mensagem vai para o destinatário real
   - Todas as mensagens são redirecionadas para testes@pousadamarealta.com.br
   - O assunto ganha o prefixo "[TESTE] "
   - A primeira linha do corpo informa o destinatário original
   - Validação de e-mail continua funcionando (mensagens com e-mail inválido continuam bloqueadas)
   - No modo de teste, a cópia para arquivo não é gerada

### Extensibilidade

O sistema foi projetado para permitir facilmente a adição de novas regras, como:
- Limite de mensagens por hóspede por dia
- Horário de envio (evitar madrugada)
- Filtros de conteúdo
- Diferentes comportamentos por tipo de instalação

Para adicionar uma nova regra, basta:
1. Criar uma classe que implemente `MessageRule`
2. Adicionar a regra na lista em `MessageService`
3. Configurar se a regra vale sempre, só no modo de teste ou só fora dele

## Compilação e Execução

```bash
# Compilar
mvn clean package

# Executar
java -jar target/reservas-1.0.0.jar

# Executar testes
mvn verify
```

## Variáveis de Ambiente

- `DB_URL`: URL JDBC do PostgreSQL (obrigatório)
- `DB_USER`: Usuário do banco (obrigatório)
- `DB_PASSWORD`: Senha do banco (obrigatório)
- `PORT`: Porta do servidor (padrão: 7070)
- `HOTEL_TEST_MODE`: Ativa modo de teste (true/false)

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Busca reserva por ID
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `GET /reservations/{id}/document` - Obtém comprovante (segunda via)

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

## Exemplo de Uso

```bash
# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```
