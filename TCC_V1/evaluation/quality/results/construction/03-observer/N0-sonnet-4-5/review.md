# Revisão: construção, cenário 03

## Visão geral

O agente montou um projeto Javalin em 7 pacotes: `api` (os controllers), `service` (preço, reservas, relatórios),
`events` (o publicador e os cinco ouvintes), `repository` (JDBC puro com HikariCP), `database` (o pool e o
`CREATE TABLE IF NOT EXISTS`), `domain` (as entidades e os enums) e o `Main`, que liga as peças. Uma requisição vai
da rota em `Main.java:61-71` para o controller, que lê um `Map` cru, chama o `ReservationService`, e o serviço grava
pelo `ReservationRepository` e publica `ReservationConfirmedEvent`/`ReservationCancelledEvent` no `EventPublisher`.
Os ouvintes (hóspede, governança e financeiro) gravam os e-mails na tabela `messages`. Os dados ficam no PostgreSQL
e sobrevivem a um reinício. O Observer está bem feito. Os problemas mais sérios são estes: nada impede que o mesmo
quarto seja reservado duas vezes (em 9 de 10 rodadas simultâneas as duas reservas foram criadas); a coluna `type` do
relatório de ocupação mostra o plano de tarifa no lugar do tipo do quarto (`ReportService.java:33`); todo valor em
dinheiro dos e-mails sai com um espaço não separável (U+00A0) depois de "R$"; e os testes quase não existem (4
testes, 4,1% das linhas).

## Arquitetura

**Números:** 7 pacotes, 15 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece
em `api` e no pacote raiz; o JDBC só em `repository` e `database`. A maior classe é `ReservationRepository`, com 251
linhas.
**Nota:** com problemas

A direção das dependências está certa (api → service → repository → database) e as peças são montadas num lugar só,
`Main.java:25-51`, sem `static` global. As regras de preço e de transição (`PricingService`, `ReservationService`)
não conhecem nem o Javalin nem o SQL. A mistura está em pontos localizados:

- a regra "segunda via só para reserva paga" mora no handler, `ReservationController.java:121`, e está errada (ver
  Comportamento);
- o comprovante em JSON é montado no controller, que vai direto ao repositório buscar o tipo do quarto
  (`ReservationController.java:153-154`). O mesmo comprovante em texto é montado dentro do ouvinte do hóspede
  (`GuestNotificationListener.java:50-72`). São duas montagens do mesmo documento, em camadas diferentes;
- os controllers de quartos e de mensagens chamam o repositório direto (`RoomController.java:18`,
  `MessageController.java:18`). Para leituras simples num sistema deste tamanho, isso é aceitável;
- o pacote `events` junta o mecanismo (publicador, interfaces) com a formatação dos e-mails e depende de `repository`
  (`events -> repository` em `dependencies.md`).

## Design de baixo nível

**Números:** 3 interessados na confirmação e 2 no cancelamento, os 5 como ouvintes separados registrados no `Main`
(`Main.java:31-41`); nenhum e-mail montado fora da estrutura; o serviço publica 2 eventos
(`ReservationService.java:100`, `:127`) e não conhece nenhum destinatário.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

O pattern: `EventListener<T>` (`events/EventListener.java:3`) tem uma implementação por interessado, inclusive o
hóspede (`GuestNotificationListener.java:14`). O `EventPublisher` guarda a lista de ouvintes por tipo de evento e a
percorre (`EventPublisher.java:16-22`). O serviço recebe o publicador de fora (`ReservationService.java:23`), e o
registro acontece na montagem (`Main.java:32-41`). Para pôr o restaurante ou o transfer basta uma classe nova e uma
linha de `subscribe`, sem tocar no serviço. A regra "pendente cancelada não avisa ninguém" e "sem devolução não avisa
o financeiro" fica em cada ouvinte (`HousekeepingCancellationListener.java:22`,
`FinanceCancellationListener.java:24-31`), graças ao `wasConfirmed` que o evento carrega
(`ReservationCancelledEvent.java:5`). Não há estrutura sobrando: a interface-marcadora `Event` serve de limite do tipo
genérico do publicador, que é a própria peça do pattern.

O resto do código tem defeitos localizados:

- a formatação de dinheiro está copiada em três ouvintes (`FinanceCancellationListener.java:41-44`,
  `FinanceNotificationListener.java:33-36`, `GuestNotificationListener.java:78-81`), e as três usam
  `NumberFormat.getCurrencyInstance(pt-BR)`, que no Java 21 (CLDR) põe U+00A0 entre "R$" e o número. O
  `DATE_FORMATTER` também está copiado em vários arquivos. `ReservationController.java:20-21` declara constantes que
  nunca usa;
- o mesmo `try/catch` que traduz exceção em código HTTP está repetido em seis handlers
  (`ReservationController.java:47-114`), em vez de um `app.exception(...)`. Por isso o `NumberFormatException` de um
  id não numérico (`ReservationController.java:60`) e o `JsonParseException` de um corpo inválido
  (`ReservationController.java:30`) escapam como 500;
- `Reservation` é um saco de setters (`Reservation.java:29-155`), e as transições ficam soltas no serviço, que lista
  os estados proibidos à mão no cancelamento (`ReservationService.java:109-111`). `Reservation.java:26` começa com
  `BigDecimal.ZERO` (escala 0), e por isso a reserva pendente sai com `"amountPaid":0`;
- erro de digitação de tipo: `ReportService.java:33` escreve `getRatePlan()` na coluna `type`;
- o corpo do POST é lido como `Map` cru e convertido campo a campo (`ReservationController.java:30-39`, `:172-189`).

Os tipos de domínio estão certos (`BigDecimal` com `HALF_EVEN` em `PricingService.java:19`, `:35`, `:41`,
`LocalDate`, enums).

## Testes

**Números:** 4 testes rodaram, 0 falharam, 0 pulados; 4 métodos escritos, num arquivo só. Cobertura de 33 de 803
linhas (4,1%) e de 9 de 124 ramos (7,3%). Os pacotes `events`, `api`, `repository` e `database` têm 0% de cobertura
(`measures/tests.md`).
**Nota:** ruim

Os quatro testes (`PricingServiceTest.java:17-50`) conferem com valores a diária de baixa temporada, a de alta
(299.88) e o desconto do não reembolsável (89.96, com arredondamento). É pouco, mas certo. Ficou sem nenhum teste
quase tudo:

- a soma das noites e o total (`calculateSubtotal`);
- os limites da temporada (15/12, fim de fevereiro);
- as transições da reserva e as recusas (409, 422, 404, 400);
- o cancelamento com e sem devolução;
- os relatórios;
- toda a parte nova do cenário: nenhum ouvinte, nem o publicador, é executado por algum teste.

A pergunta da mutação na parte nova não tem resposta boa. Trocar o assunto "Pagamento recebido", apagar a condição
`refund == 0` em `FinanceCancellationListener.java:30` ou o `wasConfirmed` em
`HousekeepingCancellationListener.java:22`, ou tirar uma linha de `subscribe` em `Main.java:32-41`: nenhum teste
falharia. A troca de `getType()` por `getRatePlan()` no relatório também passou sem rede. Não há teste pela API nem
contra o banco.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em
`numeric(10,2)` nas 6 colunas e datas em `date`. Conferências que acusaram algo: a chave estrangeira
`reservations(room_number) -> rooms` sem índice; nenhum CHECK sobre `status`, `rate_plan`, `type` e `channel`; 0
restrições que protegem uma regra; 0 transações explícitas; 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

O básico está certo: tipos, `NOT NULL`, a chave estrangeira para o quarto (`Database.java:57`), SQL todo com `?` e
try-with-resources sobre um pool Hikari (`Database.java:15-22`), e os dados sobrevivem ao reinício. Os defeitos:

- o esquema é criado no código com `CREATE TABLE IF NOT EXISTS` (`Database.java:30-69`), sem migrações. O próximo
  pedido que acrescentar uma coluna não terá como chegar a um banco que já existe;
- a regra "um quarto não pode ter duas reservas na mesma noite" não tem proteção nenhuma. É um `SELECT` de conflito
  (`ReservationRepository.java:126-134`) seguido de um `INSERT` noutra conexão em autocommit
  (`ReservationService.java:55-84`), sem transação, sem trava, sem `EXCLUDE` e nem mesmo um `synchronized`. O roteiro
  mostrou o efeito: 9 de 10 rodadas simultâneas criaram as duas reservas. É o defeito mais sério do projeto;
- confirmar e cancelar também fazem "ler, mudar, gravar" sem trava (`ReservationService.java:88-98`): dois pedidos
  ao mesmo tempo podem confirmar duas vezes e mandar os e-mails em dobro. A reserva é gravada antes dos e-mails, cada
  um na sua conexão, sem transação. Se um ouvinte falhar, a reserva fica confirmada e o cliente recebe 500;
- `findByNight` e `findByCheckInMonth` filtram no SQL, sem carregar a tabela inteira, mas `EXTRACT(YEAR/MONTH ...)`
  (`ReservationRepository.java:200-201`) não usa índice. Não há índice nenhum além das chaves.

## Comportamento

**Números:** 57 de 66 conferências passaram (`measures/behavior.md`):

- parte comum: 40 de 46;
- reinício: 3 de 3;
- cenário 03: 13 de 15;
- reservas simultâneas: 1 de 2.

**Nota:** com problemas

As falhas que pesam:

- **duas reservas ao mesmo tempo:** em 9 de 10 rodadas as duas foram criadas. Para o dono, isso quer dizer o mesmo
  quarto vendido duas vezes quando dois hóspedes reservam no mesmo instante. A causa está na coluna do banco;
- **o dinheiro nos e-mails:** "R$ 779,80" sai com um espaço não separável (bytes `302 240`, conferido em
  `GET /messages`). Por isso falharam o comprovante no e-mail do hóspede e os valores dos e-mails do financeiro (2
  das 2 falhas do cenário). Na tela parece certo, mas não é o texto que o dono deu, e qualquer busca ou comparação por
  "R$ 779,80" falha;
- **corpo que não é JSON:** 500 em vez de 400 INVALID_REQUEST (o `JsonParseException` escapa,
  `ReservationController.java:30`). O mesmo acontece com um id não numérico, como em `GET /reservations/abc`, que dá
  500 (conferido à mão);
- **segunda via de reserva pendente:** 404 RESERVATION_NOT_FOUND em vez de 409. A reserva existe, e o texto diz que
  uma operação que a situação não permite é 409 OPERATION_NOT_ALLOWED. Pior, o teste em `ReservationController.java:121`
  olha só se há valor pago: uma reserva confirmada e depois cancelada continua tirando a segunda via (conferido à mão:
  200 com o comprovante de uma reserva CANCELLED), e o texto diz que ela só existe para reserva paga (confirmada, com
  check-in ou com check-out);
- **`amountPaid` da pendente:** sai `0` e não `0.00` (`Reservation.java:26`). O texto pede número com 2 casas.

Duas falhas não pesam. "O 201 comporta 3" e "a diária do 201" falharam porque o roteiro procura `number` como
número, e o projeto devolve `"201"` como texto. Os valores estão certos (capacity 3, nightlyRate 389.90, conferidos à
mão), e o texto não fixa o tipo do número do quarto.

Fora do roteiro: o relatório de ocupação põe o plano de tarifa na coluna `type`. Conferido à mão, uma reserva
NON_REFUNDABLE no 301 sai como `301,NON_REFUNDABLE,2,...` e não `301,SUITE,...` (`ReportService.java:33`). O roteiro
só passou porque usou um STANDARD no quarto 101, que é STANDARD. O contador e a recepção recebem a informação errada
em toda reserva que não seja de plano normal num quarto STANDARD.

## Dúvidas

- O número do quarto como texto (`"201"`) ou como número: o texto não decide. Não pesei.
- O código para a segunda via de uma reserva não paga: o texto não diz qual é. 409 OPERATION_NOT_ALLOWED é o que mais
  se aproxima da regra geral, e por isso pesei pouco o 404. Pesei mais a segunda via de reserva cancelada, que o texto
  exclui.
- No banco, a falta total de proteção contra reservas duplicadas fica entre `com problemas` e `ruim`. Dei
  `com problemas` porque o esquema em si (tipos, chaves, `NOT NULL`) está certo, mas é um risco real de dado errado.
