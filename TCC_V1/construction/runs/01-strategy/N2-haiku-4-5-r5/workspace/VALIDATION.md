# Checklist de Validação - Sistema de Reservas

## Funcionalidades Implementadas ✓

### Quartos
- [x] 5 quartos configurados (101, 102, 201, 202, 301)
- [x] Tipos de quarto (STANDARD, DELUXE, SUITE)
- [x] Capacidades corretas (2, 2, 3, 3, 4)
- [x] Diárias corretas (249.90, 249.90, 389.90, 389.90, 579.90)

### Planos de Tarifa
- [x] STANDARD: Sem desconto, com temporada alta (+20%)
- [x] NON_REFUNDABLE: 15% desconto, sem reembolso
- [x] CORPORATE: Desconto por empresa (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%)
- [x] WEEKEND_PACKAGE: Sexta-domingo, preço fixo, reembolso 50%
- [x] LONG_STAY: 7+ noites, 10% desconto
- [x] Extensível para novos planos (Strategy Pattern)

### Temporada Alta
- [x] Dezembro 15-31: +20%
- [x] Janeiro até 28: +20%
- [x] Fevereiro até 28: +20%
- [x] Julho inteiro: +20%

### Arredondamento
- [x] Banker's rounding (HALF_EVEN) em todas operações
- [x] Cada noite arredondada
- [x] Soma arredondada
- [x] Desconto arredondado
- [x] Total arredondado

### Estados de Reserva
- [x] PENDING: Reserva criada
- [x] CONFIRMED: Reserva paga
- [x] CHECKED_IN: Hóspede chegou
- [x] CHECKED_OUT: Hóspede saiu
- [x] CANCELLED: Cancelada
- [x] Transições de estado validadas

### Validações
- [x] Quarto deve existir (404 ROOM_NOT_FOUND)
- [x] Datas válidas: saída > entrada (400 INVALID_REQUEST)
- [x] Mínimo 1 hóspede (400 INVALID_REQUEST)
- [x] Máximo de hóspedes <= capacidade (422 CAPACITY_EXCEEDED)
- [x] Quarto não pode ter 2 reservas na mesma noite (409 ROOM_UNAVAILABLE)
- [x] Plano CORPORATE precisa de companyCode válido (400 INVALID_REQUEST)
- [x] Plano WEEKEND_PACKAGE só sexta-domingo, exatamente 2 noites (400 INVALID_REQUEST)
- [x] Plano LONG_STAY precisa de 7+ noites (400 INVALID_REQUEST)

### Operações
- [x] Criar reserva (POST /reservations) → 201
- [x] Obter reserva (GET /reservations/{id}) → 200
- [x] Confirmar reserva (POST /reservations/{id}/confirmation)
- [x] Cancelar reserva (POST /reservations/{id}/cancellation)
- [x] Check-in (POST /reservations/{id}/check-in)
- [x] Check-out (POST /reservations/{id}/check-out) → retorna comprovante
- [x] Segunda via (GET /reservations/{id}/document) → só para pagas

### Comprovante
- [x] Número da reserva
- [x] Nome e CPF do hóspede (formatado 111.222.333-44)
- [x] Número e tipo do quarto
- [x] Datas (formatadas DD/MM/YYYY)
- [x] Número de diárias (singular/plural)
- [x] Diárias (formatadas R$ 1.234,56)
- [x] Desconto (quando houver)
- [x] Total

### Confirmação por E-mail
- [x] Enviado ao confirmar reserva
- [x] Assunto: "Reserva confirmada - Pousada Maré Alta"
- [x] Corpo com comprovante
- [x] Horários (14h entrada, 12h saída)
- [x] E-mails armazenados (GET /messages)

### Reembolsos
- [x] STANDARD: 100% (se pago)
- [x] NON_REFUNDABLE: 0%
- [x] CORPORATE: 100%
- [x] WEEKEND_PACKAGE: 50%
- [x] LONG_STAY: 100%
- [x] Cancelamento anterior ao check-in: permite reembolso
- [x] PENDING cancelado: sem dinheiro para devolver

### Relatórios
- [x] Ocupação: Quartos ocupados em um dia específico
  - Formato: room, type, reservation, check_in, check_out
  - Linha final: total X/5
  - Arquivo CSV com charset UTF-8
- [x] Financeiro: Receita de um mês
  - Formato: reservation, room, check_in, check_out, status, amount
  - Apenas reservas pagas (amount_paid - refund)
  - Linha final: total
  - Arquivo CSV com charset UTF-8

### Casos de Teste
- [x] STANDARD 10-12 março: R$ 499.80
- [x] STANDARD 10-12 julho: R$ 599.76 (2 noites × R$ 249.90 × 1.20)
- [x] CORPORATE TECNOSUL 201 10-12 março: R$ 686.22 (R$ 779.80 - R$ 93.58)
- [x] WEEKEND_PACKAGE 201 sexta 12 a domingo 14 março: R$ 820.00
- [x] WEEKEND_PACKAGE reembolso: R$ 410.00 (50%)
- [x] LONG_STAY 101 01-08 março: R$ 1.574,37 (R$ 1.749.30 - R$ 174.93)

## Build e Testes ✓

- [x] Maven compila com `mvn clean package`
- [x] Gera JAR executável em target/reservation-system.jar
- [x] 9 testes unitários passando
  - [x] 6 testes de pricing
  - [x] 3 testes de API/utilidades
- [x] `mvn verify` passa sem erros
- [x] Jar criado: 7.2MB (com todas as dependências)

## Arquitetura ✓

- [x] Padrão Strategy para planos de tarifa
- [x] Fácil de estender para novos planos
- [x] Separação clara de responsabilidades
- [x] DAO pattern para acesso a dados
- [x] Service layer para lógica de negócio
- [x] REST API com Javalin
- [x] PostgreSQL para persistência

## Dados Iniciais ✓

- [x] Banco criado automaticamente na primeira execução
- [x] Quartos inseridos automaticamente
- [x] Schema de reservas, mensagens criado

## Documentação ✓

- [x] RUNNING.md: Como compilar e executar
- [x] CLAUDE.md: Instruções para próximos desenvolvimentos
- [x] Código bem estruturado e legível
- [x] Testes documentados

## Pronto para Produção ✓

- [x] Todas as validações implementadas
- [x] Tratamento de erros completo
- [x] Códigos HTTP corretos
- [x] Formatos de resposta corretos
- [x] Cálculos de preço validados
- [x] Banco de dados funcional
- [x] API funcionando conforme especificação
