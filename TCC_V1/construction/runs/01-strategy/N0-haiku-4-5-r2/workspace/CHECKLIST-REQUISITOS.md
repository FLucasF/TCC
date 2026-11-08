# Checklist de Requisitos - Sistema de Reservas Pousada Maré Alta

## ✅ Estrutura da Pousada

- [x] 5 quartos no total
- [x] Quartos 101-102: STANDARD, 2 pessoas, R$ 249,90
- [x] Quartos 201-202: DELUXE, 3 pessoas, R$ 389,90
- [x] Quarto 301: SUITE, 4 pessoas, R$ 579,90
- [x] Quartos nunca mudam

## ✅ Cálculo de Preços

- [x] Cada noite custa a diária do quarto
- [x] Alta temporada: 15 dez - 28 fev + julho inteiro (+20%)
- [x] Arredondamento "round half to even" a cada passo
- [x] Preço de cada noite, soma de noites, desconto e total todos arredondados

## ✅ Planos de Tarifa

### STANDARD
- [x] Sem desconto
- [x] Reembolso total se cancelado

### NON_REFUNDABLE
- [x] 15% de desconto sobre soma das noites
- [x] Sem reembolso

### CORPORATE (NOVO)
- [x] TECNOSUL: 12% desconto
- [x] AGROVALE: 18% desconto
- [x] FARMAVIDA: 25% desconto
- [x] Código da empresa obrigatório
- [x] Reembolso total se cancelado

### WEEKEND_PACKAGE (NOVO)
- [x] Entrada sexta, saída domingo (exatamente 2 noites)
- [x] Preço fixo: STD R$ 520, DLX R$ 820, SUITE R$ 1.200
- [x] Sem desconto
- [x] 50% reembolso se cancelado

### LONG_STAY (NOVO)
- [x] 7+ noites obrigatório
- [x] 10% desconto sobre diárias
- [x] Alta temporada continua aplicável
- [x] Reembolso total se cancelado

## ✅ Dados de Reserva

- [x] Nome, e-mail e CPF do hóspede
- [x] Quarto, datas (entrada e saída)
- [x] Quantidade de hóspedes
- [x] Plano de tarifa
- [x] Validação: mínimo 1 hóspede, máximo = capacidade do quarto
- [x] Validação: saída após entrada

## ✅ Estados de Reserva

- [x] PENDING: Reserva criada, sem pagamento
- [x] CONFIRMED: Hóspede pagou
- [x] CHECKED_IN: Hóspede fez check-in
- [x] CHECKED_OUT: Hóspede saiu
- [x] CANCELLED: Cancelada (antes de check-in)

## ✅ Transições de Estado

- [x] PENDING → CONFIRMED (com amountPaid)
- [x] CONFIRMED → CHECKED_IN
- [x] CHECKED_IN → CHECKED_OUT
- [x] PENDING ou CONFIRMED → CANCELLED (antes de check-in)
- [x] Cancelamento PENDING: sem reembolso, refund = null
- [x] Cancelamento CONFIRMED: reembolso conforme plano

## ✅ Disponibilidade

- [x] Quarto não pode ter 2 reservas na mesma noite (exceto CANCELLED)
- [x] Check-in é a partir das 14h, check-out até 12h
- [x] Quem entra naquela noite conta, quem sai naquele dia não

## ✅ Comprovantes

- [x] Formato correto com número, hóspede, quarto, datas, diárias, desconto (se houver), total
- [x] Formatação: datas em 10/03/2027, valores em R$ 779,80, CPF em 111.222.333-44
- [x] "1 diária" para 1 noite, "N diárias" para múltiplas
- [x] Enviado por e-mail na confirmação
- [x] Devolvido no check-out
- [x] Segunda via disponível em GET /reservations/{id}/document

## ✅ E-mails

- [x] Assunto: "Reserva confirmada - Pousada Maré Alta"
- [x] Corpo com saudação, comprovante e horários
- [x] Não são enviados de verdade, armazenados em lista
- [x] Recepção consulta lista para saber o que foi mandado

## ✅ Mensagens

- [x] Canal: EMAIL
- [x] Destinatário, assunto e texto
- [x] GET /messages retorna todas as mensagens

## ✅ Relatórios CSV

### Ocupação
- [x] Para uma noite específica
- [x] Colunas: room, type, reservation, check_in, check_out
- [x] Apenas reservas CONFIRMED, CHECKED_IN ou CHECKED_OUT
- [x] Ordenado por número do quarto
- [x] Linha "total" com quantos ocupados de 5
- [x] Formato de data: 2027-07-10
- [x] Charset: UTF-8

### Financeiro
- [x] Para um mês específico
- [x] Reservas que começam naquele mês
- [x] Apenas com amount_paid > 0 ou refund > 0
- [x] Colunas: reservation, room, check_in, check_out, status, amount
- [x] amount = amount_paid - refund
- [x] Ordenado por data de entrada, depois por ID
- [x] Linha "total" com soma
- [x] Formato de valor: 599.76 (com ponto decimal)
- [x] Formato de data: 2027-07-10
- [x] Charset: UTF-8

## ✅ API

### Rotas
- [x] GET /rooms: lista quartos com number, type, capacity, nightlyRate
- [x] POST /reservations: cria reserva (201), aceita roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, companyCode (opcional)
- [x] GET /reservations/{id}: detalhes
- [x] POST /reservations/{id}/confirmation: confirma com amountPaid
- [x] POST /reservations/{id}/cancellation: cancela
- [x] POST /reservations/{id}/check-in: check-in
- [x] POST /reservations/{id}/check-out: check-out com comprovante
- [x] GET /reservations/{id}/document: segunda via
- [x] GET /messages: e-mails enviados
- [x] GET /reports/occupancy?date=2027-07-10: CSV UTF-8
- [x] GET /reports/revenue?month=2027-07: CSV UTF-8

### Resposta de Reserva
- [x] id, roomNumber, guestName, guestEmail, guestCpf
- [x] checkIn, checkOut (formato ISO)
- [x] guests, ratePlan
- [x] nights, subtotal, discount, total (números com 2 casas)
- [x] status, amountPaid (números com 2 casas)
- [x] refund (número com 2 casas ou null)

### Códigos de Erro
- [x] 400 INVALID_REQUEST: falta campo, JSON inválido, data inválida, mês inválido, regra de plano violada
- [x] 404 ROOM_NOT_FOUND: quarto não existe
- [x] 404 RESERVATION_NOT_FOUND: reserva não existe
- [x] 409 ROOM_UNAVAILABLE: quarto reservado nas datas
- [x] 409 OPERATION_NOT_ALLOWED: operação incompatível com estado
- [x] 422 CAPACITY_EXCEEDED: mais hóspedes que capacidade

## ✅ Novos Planos (Requisitos de Dia 1)

- [x] CORPORATE com 3 empresas (TECNOSUL, AGROVALE, FARMAVIDA)
- [x] WEEKEND_PACKAGE com preço fixo sexta-domingo
- [x] LONG_STAY com 7+ noites e desconto 10%
- [x] Todos com suas próprias regras de desconto e reembolso
- [x] Campo companyCode na criação (opcional, ignorado em planos que não usam)
- [x] 400 INVALID_REQUEST quando plano não pode ser usado conforme regras

## ✅ Verificações de Cálculo

- [x] TECNOSUL DELUXE 10/03-12/03: R$ 779,80 diárias, R$ 93,58 desconto, R$ 686,22 total ✓
- [x] WEEKEND_PACKAGE DELUXE 12/03-14/03: R$ 820,00, 50% reembolso = R$ 410,00 ✓
- [x] LONG_STAY STANDARD 01/03-08/03: R$ 1.749,30 diárias, R$ 174,93 desconto, R$ 1.574,37 total ✓

## ✅ Tecnologia

- [x] Java 21
- [x] Javalin 6.7.0
- [x] Maven
- [x] PostgreSQL 18
- [x] Variáveis de ambiente: DB_URL, DB_USER, DB_PASSWORD, PORT
- [x] Banco começa vazio, sistema cria schema
- [x] mvn package gera JAR executável
- [x] java -jar funciona
- [x] mvn verify passa

## ✅ Documentação

- [x] LEIA-ME.md: documentação completa
- [x] COMECO-RAPIDO.md: início rápido
- [x] EXEMPLOS-API.md: exemplos de requisições
- [x] docker-compose.yml: para testes
- [x] run.sh: script de execução
- [x] test-api.sh: script de testes
- [x] Código bem estruturado e comentado

## Status Final

✅ **TODOS OS REQUISITOS IMPLEMENTADOS**

Sistema pronto para uso!
