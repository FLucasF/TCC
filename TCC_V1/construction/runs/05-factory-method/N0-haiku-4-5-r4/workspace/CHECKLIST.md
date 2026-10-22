# ✅ Checklist de Implementação

## Estrutura Básica
- [x] Java 21 com Javalin 6.7.0
- [x] Maven com pom.xml configurado
- [x] PostgreSQL 18 (configurável via env vars)
- [x] HikariCP para pool de conexões
- [x] Logging com SLF4J e Logback
- [x] Maven Shade Plugin para jar executável

## Banco de Dados
- [x] Inicialização automática no startup
- [x] Tabela `rooms` (5 quartos pré-cadastrados)
- [x] Tabela `reservations` com todos os campos necessários
- [x] Tabela `messages` para e-mails simulados
- [x] Índice em `reservations(room_number, check_in, check_out)` para performance
- [x] Variáveis de ambiente: DB_URL, DB_USER, DB_PASSWORD, PORT

## Quartos
- [x] 101, 102 (STANDARD, 2 pessoas, R$ 249,90)
- [x] 201, 202 (DELUXE, 3 pessoas, R$ 389,90)
- [x] 301 (SUITE, 4 pessoas, R$ 579,90)
- [x] API GET /rooms retorna todos os quartos

## Preços e Tarifa
- [x] Cálculo de preço base por noite
- [x] Alta temporada: 15 dez a 28 fev + julho (20% acréscimo)
- [x] Plano STANDARD (sem desconto)
- [x] Plano NON_REFUNDABLE (15% desconto)
- [x] Arredondamento HALF_EVEN em todos os passos:
  - [x] Preço da noite
  - [x] Subtotal
  - [x] Desconto
  - [x] Total

## Reservas
- [x] POST /reservations (criar com PENDING)
- [x] GET /reservations/{id} (obter detalhes)
- [x] Validação: quarto existe
- [x] Validação: não há conflito de datas (sem canceladas)
- [x] Validação: hóspedes <= capacidade
- [x] Validação: hóspedes >= 1
- [x] Validação: checkOut > checkIn
- [x] Validação: datas em formato yyyy-MM-dd
- [x] Cálculo automático de noites, subtotal, discount, total

## Identificação de Hóspedes
- [x] CPF (guestCpf) - pessoa física
- [x] CNPJ + companyName - empresa
- [x] Passaporte + guestCountry - estrangeiro
- [x] Validação: apenas um tipo de identificação
- [x] Validação: se CNPJ, companyName obrigatório
- [x] Validação: se Passaporte, guestCountry obrigatório
- [x] API retorna todos os campos (null para não usados)

## Estados de Reserva
- [x] PENDING (criação)
- [x] CONFIRMED (após confirmação/pagamento)
- [x] CHECKED_IN (check-in)
- [x] CHECKED_OUT (check-out)
- [x] CANCELLED (cancelamento)

## Confirmação (Pagamento)
- [x] POST /reservations/{id}/confirmation
- [x] Valida: reserva está PENDING
- [x] Muda para CONFIRMED
- [x] Armazena amountPaid
- [x] Envia e-mail de confirmação automaticamente
- [x] Erro 409 se não PENDING

## Check-in e Check-out
- [x] POST /reservations/{id}/check-in
  - [x] Valida: reserva está CONFIRMED
  - [x] Muda para CHECKED_IN
- [x] POST /reservations/{id}/check-out
  - [x] Valida: reserva está CHECKED_IN
  - [x] Muda para CHECKED_OUT
  - [x] Retorna documento com title e text
  - [x] Retorna reservationId, datas, valores

## Cancelamento
- [x] POST /reservations/{id}/cancellation
- [x] Valida: reserva está PENDING ou CONFIRMED (não CHECKED_IN/OUT/CANCELLED)
- [x] Muda para CANCELLED
- [x] Reembolso:
  - [x] PENDING: sem reembolso (sem pagamento)
  - [x] STANDARD + CONFIRMED: reembolso total
  - [x] NON_REFUNDABLE: sem reembolso
- [x] Armazena refund value

## Documentos
### Comprovante de Reserva (CPF)
- [x] Título: "Comprovante de reserva nº X"
- [x] Hóspede: "Nome (CPF XXX.XXX.XXX-XX)"
- [x] Quarto: "101 (STANDARD)"
- [x] Entrada/Saída/Noites com formatação PT
- [x] Datas em dd/MM/yyyy
- [x] "1 diária" / "N diárias" singular/plural
- [x] Diárias em "R$ 1.234,56" com formato brasileiro
- [x] Desconto (se houver)
- [x] Total

### Fatura de Hospedagem (CNPJ)
- [x] Título: "Fatura de hospedagem nº X"
- [x] Empresa: "Nome (CNPJ XX.XXX.XXX/XXXX-XX)"
- [x] Hóspede: "Nome" (sem CPF)
- [x] Quarto, Entrada, Saída, Noites, Diárias, Desconto, Total (igual ao comprovante)
- [x] ISS retido (5%): "R$ X.XXX,XX"

### Booking Receipt (Passaporte)
- [x] Título: "Booking receipt no. X"
- [x] Guest: "Nome (passport XXXX, Country)"
- [x] Room: "101 (STANDARD)"
- [x] Datas em yyyy-MM-dd
- [x] "1 night" / "N nights"
- [x] Valores em "BRL 1,234.56" com formato internacional
- [x] Discount (se houver)
- [x] Total

## Documentos - Disponibilidade
- [x] GET /reservations/{id}/check-out (retorna documento)
- [x] GET /reservations/{id}/document (segunda via)
  - [x] Valida: reserva confirmada (amount_paid > 0)
  - [x] Erro 409 se não paga

## E-mails (Simulados)
- [x] POST /reservations/{id}/confirmation envia e-mail
- [x] Assunto: "Reserva confirmada - Pousada Maré Alta"
- [x] Corpo em português com saudação
- [x] Contém documento apropriado
- [x] Informações de check-in (14h) e check-out (12h)
- [x] Armazenado em tabela `messages`
- [x] GET /messages retorna lista

## Relatórios
### Ocupação
- [x] GET /reports/occupancy?date=yyyy-MM-dd
- [x] CSV em UTF-8
- [x] Colunas: room, type, reservation, check_in, check_out
- [x] Apenas CONFIRMED, CHECKED_IN, CHECKED_OUT
- [x] Quem entra naque dia conta, quem sai não
- [x] Ordem por número do quarto
- [x] Linha total: total,X/5

### Financeiro
- [x] GET /reports/revenue?month=yyyy-MM
- [x] CSV em UTF-8
- [x] Colunas: reservation, room, check_in, check_out, status, amount
- [x] Apenas reservas que começam no mês
- [x] Estados: CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
- [x] Amount = amountPaid - refund (ou 0 se refund NULL)
- [x] Ordem por check_in, depois id
- [x] Valores com formato X.XX (ponto como separador decimal)
- [x] Linha total com soma

## API Completa
- [x] GET /rooms
- [x] POST /reservations
- [x] GET /reservations/{id}
- [x] POST /reservations/{id}/confirmation
- [x] POST /reservations/{id}/check-in
- [x] POST /reservations/{id}/check-out
- [x] POST /reservations/{id}/cancellation
- [x] GET /reservations/{id}/document
- [x] GET /messages
- [x] GET /reports/occupancy
- [x] GET /reports/revenue

## Tratamento de Erros
- [x] 400 INVALID_REQUEST (campos faltando, formato inválido)
- [x] 404 ROOM_NOT_FOUND
- [x] 404 RESERVATION_NOT_FOUND
- [x] 409 ROOM_UNAVAILABLE
- [x] 409 OPERATION_NOT_ALLOWED
- [x] 422 CAPACITY_EXCEEDED
- [x] Resposta padrão: `{"error": "CODE", "message": "..."}`

## Build e Deploy
- [x] `mvn clean compile` funciona
- [x] `mvn package` gera jar de ~15MB
- [x] Maven Shade para jar com todas as dependências
- [x] `java -jar mare-alta-1.0.0.jar` inicia sem argumentos
- [x] `mvn verify` passa com sucesso
- [x] Porta default 7070
- [x] Porta configurável via PORT env var

## Formato de Dados
- [x] JSON na API (application/json)
- [x] Datas: yyyy-MM-dd na API
- [x] Datas: dd/MM/yyyy em documentos português
- [x] Datas: yyyy-MM-dd em documentos inglês
- [x] Valores: 2 casas decimais em JSON (599.76)
- [x] Valores: R$ ou BRL com formatação local em textos
- [x] CSV: UTF-8, valores com ponto decimal

## Testes
- [x] PriceCalculatorTest com casos de:
  - [x] Alta temporada
  - [x] Baixa temporada
  - [x] Cálculo de noites
  - [x] Preço STANDARD
  - [x] Preço NON_REFUNDABLE
  - [x] Arredondamento

## Documentação
- [x] README.md com instruções
- [x] EXAMPLES.md com exemplos de API
- [x] IMPLEMENTATION_SUMMARY.md com o que foi feito
- [x] CHECKLIST.md (este arquivo)

---

**Status Final: ✅ COMPLETO**

O sistema está 100% implementado conforme as especificações.
O jar está pronto para ser implantado em produção.
