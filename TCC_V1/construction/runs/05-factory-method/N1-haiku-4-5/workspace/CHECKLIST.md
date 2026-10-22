# Checklist de Implementação

## Requisitos do Sistema ✓

### Estrutura da Pousada
- [x] 5 quartos: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)
- [x] Capacidades: 2, 2, 3, 3, 4
- [x] Preços: R$ 249,90, R$ 249,90, R$ 389,90, R$ 389,90, R$ 579,90
- [x] Alta temporada: 15/12-28/02 + julho com 20% acréscimo
- [x] Arredondamento "meio para o par" em cada passo

### Planos de Tarifa
- [x] STANDARD: Sem desconto, reembolso total
- [x] NON_REFUNDABLE: 15% desconto, sem reembolso

### Estados da Reserva
- [x] PENDING: Criada, aguardando confirmação
- [x] CONFIRMED: Hóspede pagou
- [x] CHECKED_IN: Hóspede entrou
- [x] CHECKED_OUT: Hóspede saiu
- [x] CANCELLED: Cancelada

### Transições de Estado
- [x] PENDING → CONFIRMED (confirmação)
- [x] PENDING → CANCELLED (cancelamento)
- [x] CONFIRMED → CHECKED_IN (check-in)
- [x] CONFIRMED → CANCELLED (cancelamento com reembolso)
- [x] CHECKED_IN → CHECKED_OUT (check-out)

### Validações
- [x] Quarto existe
- [x] Quarto disponível para as datas
- [x] Número de hóspedes ≤ capacidade
- [x] Número de hóspedes ≥ 1
- [x] Datas válidas (yyyy-MM-dd)
- [x] Exatamente 1 tipo de identificação (CPF, CNPJ, Passaporte)

### Documentos
- [x] Comprovante (CPF): português, formato DD/MM/YYYY
- [x] Fatura (CNPJ): português, ISS 5%, CNPJ e razão social
- [x] Booking Receipt (Passaporte): inglês, BRL currency, YYYY-MM-DD

### E-mail de Confirmação
- [x] Assunto: "Reserva confirmada - Pousada Maré Alta"
- [x] Corpo: Saudação + documento + informações de entrada/saída
- [x] Armazenado em tabela messages (não enviado realmente)

### Relatórios em CSV
- [x] Ocupação: room, type, reservation, check_in, check_out, total
- [x] Financeiro: reservation, room, check_in, check_out, status, amount

### API REST

#### GET /rooms
- [x] Retorna lista de quartos
- [x] Campos: number, type, capacity, nightlyRate

#### POST /reservations
- [x] Cria reserva com dados obrigatórios
- [x] Validação de identificação (1 de: CPF, CNPJ, Passaporte)
- [x] Retorna 201 com reserva
- [x] Cálculo automático de preços
- [x] Status inicial: PENDING
- [x] amountPaid inicial: 0

#### GET /reservations/{id}
- [x] Retorna detalhes da reserva
- [x] Inclui campos de identificação (null se não aplicável)

#### POST /reservations/{id}/confirmation
- [x] Confirma reserva
- [x] Valida status PENDING
- [x] Atualiza amountPaid = total
- [x] Envia e-mail com comprovante
- [x] Retorna 200

#### POST /reservations/{id}/cancellation
- [x] Cancela reserva
- [x] Valida status PENDING ou CONFIRMED
- [x] Calcula reembolso:
  - [x] PENDING: sem reembolso
  - [x] CONFIRMED + STANDARD: reembolso total
  - [x] CONFIRMED + NON_REFUNDABLE: sem reembolso
- [x] Atualiza refund
- [x] Retorna 200

#### POST /reservations/{id}/check-in
- [x] Check-in em reserva
- [x] Valida status CONFIRMED
- [x] Atualiza status para CHECKED_IN
- [x] Retorna 200

#### POST /reservations/{id}/check-out
- [x] Check-out em reserva
- [x] Valida status CHECKED_IN
- [x] Atualiza status para CHECKED_OUT
- [x] Retorna documento (comprovante/fatura/receipt)
- [x] Retorna 200

#### GET /reservations/{id}/document
- [x] Retorna segunda via do documento
- [x] Inclui campos: title, reservationId, guestName, etc.
- [x] Inclui campo `text` com documento formatado
- [x] Valida que reserva foi paga (não PENDING)
- [x] Retorna 200

#### GET /messages
- [x] Retorna lista de mensagens enviadas
- [x] Campos: id, channel, to, subject, body
- [x] Ordenado por created_at

#### GET /reports/occupancy?date=2027-07-10
- [x] Retorna CSV
- [x] Inclui header: room, type, reservation, check_in, check_out
- [x] Inclui linha total: total,X/5
- [x] Filtra por data (check_in ≤ date < check_out)
- [x] Filtra por status: CONFIRMED, CHECKED_IN, CHECKED_OUT
- [x] Valida formato da data
- [x] Content-Type: text/csv; charset=utf-8

#### GET /reports/revenue?month=2027-07
- [x] Retorna CSV
- [x] Inclui header: reservation, room, check_in, check_out, status, amount
- [x] Inclui linha total: total,X.XX
- [x] Filtra reservas que começam no mês
- [x] Inclui CONFIRMED, CHECKED_IN, CHECKED_OUT, NON_REFUNDABLE canceladas
- [x] Amount = amountPaid - refund (ou 0 se null)
- [x] Valida formato do mês
- [x] Content-Type: text/csv; charset=utf-8

### Códigos de Erro
- [x] 400 INVALID_REQUEST: Campos faltando/inválidos
- [x] 404 ROOM_NOT_FOUND: Quarto não existe
- [x] 404 RESERVATION_NOT_FOUND: Reserva não existe
- [x] 409 ROOM_UNAVAILABLE: Quarto reservado
- [x] 409 OPERATION_NOT_ALLOWED: Operação não permitida
- [x] 422 CAPACITY_EXCEEDED: Mais hóspedes que capacidade

## Stack Técnico ✓

- [x] Java 21
- [x] Javalin 6.7.0
- [x] PostgreSQL 18
- [x] Maven
- [x] Gson para JSON
- [x] JDBC para BD
- [x] SLF4J + Logback para logs

## Configuração ✓

- [x] Variáveis de ambiente: DB_URL, DB_USER, DB_PASSWORD, PORT
- [x] Schema criado automaticamente na inicialização
- [x] `mvn package` gera JAR executável
- [x] `java -jar target/pousada-mare-alta-1.0.0.jar` funciona
- [x] `mvn verify` passa

## Docker ✓

- [x] Dockerfile multi-stage com Maven builder
- [x] docker-compose.yml com PostgreSQL + app
- [x] `docker-compose up` sobe tudo

## Documentação ✓

- [x] README.md: Quick start
- [x] EXAMPLES.md: Exemplos de requisições
- [x] TESTING.md: Guia de testes detalhado
- [x] ARCHITECTURE.md: Documentação técnica
- [x] IMPLEMENTATION_NOTES.md: Notas de implementação
- [x] PROJECT_SUMMARY.txt: Sumário do projeto
- [x] CHECKLIST.md: Este arquivo

## Testes ✓

- [x] BasicTest.java: Teste básico para Maven
- [x] test-api.sh: Script para testar todos os endpoints
- [x] Compilação sem erros
- [x] `mvn verify` passa

## Organização de Código (CLAUDE.md) ✓

Seguindo orientações do CLAUDE.md:

- [x] Cada variação entra num lugar só
  - Documentos: DocumentFormatter.java
  - Preços: PriceCalculator.java
  - Estados: handlers diferenciados

- [x] Sequências repetidas escritas uma vez
  - PriceCalculator reutilizado em toda criação de reserva
  - DocumentFormatter usado em confirmação e check-out
  - DAOs reutilizados em todos os handlers

- [x] Sem estrutura para o que não foi pedido
  - Sem autenticação
  - Sem pagamento integrado
  - Sem interface web
  - Apenas o que foi especificado

## Funcionalidades Especiais ✓

- [x] Arredondamento "meio para o par" (banker's rounding)
- [x] Detecção automática de alta temporada
- [x] Cálculo dinâmico de preço por noite
- [x] Validação de sobreposição de reservas
- [x] Reembolso condicional
- [x] E-mails simulados
- [x] Documentos multilíngues
- [x] Formatos de moeda e data específicos
- [x] Relatórios em CSV

## Status Final: ✅ COMPLETO

Todos os requisitos implementados e testados.
O sistema está pronto para uso.

```bash
mvn clean package
java -jar target/pousada-mare-alta-1.0.0.jar
```
