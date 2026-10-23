# Revisão: construção, cenário 07

## Visão geral

O agente montou um serviço Javalin em seis pacotes: `handler` (rotas), `service` (regras e SQL), `db` (o pool Hikari
e o `CREATE TABLE`), `model` (classes com campos públicos), `util` (preço e validação de e-mail) e o `Main`, que
monta tudo à mão. Uma requisição entra num handler, que lê o JSON para um `Map` e chama um serviço. O serviço confere
a regra, roda o SQL direto pelo `Database.getConnection()` e devolve um modelo, que o handler transforma num `Map` de
volta. Os dados ficam no PostgreSQL, em três tabelas criadas no código. As três regras de e-mail do cenário funcionam
pela API (14 de 14 conferências), mas ficaram como três `if` dentro de `MessageService.sendMessage`, sem nenhuma
estrutura que permita combinar as regras por instalação. Isso é omissão do Decorator. O mais sério está no resto do
sistema:
- nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos (10 de 10 rodadas criaram
  as duas);
- um plano de tarifa inválido grava uma reserva que depois nunca mais pode ser lida e ainda bloqueia o quarto;
- o dinheiro sai como texto no JSON;
- o comprovante escreve "R$ 1159,80" em vez de "R$ 1.159,80";
- o check-out não devolve o comprovante no formato combinado.

Quase não há testes fora de `util`.

## Arquitetura

**Números:** 6 pacotes, 8 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). A maior classe é
`ReservationService`, com 330 linhas. JDBC aparece em `db`, `service` e `handler`; o handler só usa `SQLException`.
**Nota:** com problemas

As dependências apontam do jeito certo: `Main`, depois `handler`, `service` e por fim `db` e `model`. As regras não
conhecem o Javalin. A montagem fica toda em um lugar (`Main.java:25-33`) e é simples, sem cerimônia. Os problemas:

- **Não há camada de acesso a dados.** `Database` só cria o pool e o esquema (`db/Database.java:16-93`). Todo o SQL
  mora dentro dos serviços, misturado com a regra. `ReservationService` faz consulta de quartos, `INSERT`, `UPDATE`
  de cada transição, a consulta de conflito e ainda formata o comprovante (`service/ReservationService.java:22-61`,
  `88-118`, `167-174`, `255-275`, `277-329`). `ReportService` também monta SQL próprio
  (`service/ReportService.java:28-35`, `90-97`).
- **Conhecimento de negócio na borda.** O tipo do quarto da segunda via é decidido pelo número do quarto, fixo no
  handler (`handler/ReservationHandler.java:145-146`), em vez de vir do quarto. As exceções de regra viram status
  HTTP por comparação do texto da mensagem (`handler/ReservationHandler.java:203-226`).
- **Configuração lida por dentro.** O modo de teste é lido de `System.getenv` dentro do serviço
  (`service/MessageService.java:19`), e não na montagem.

Não chega a `ruim`: as camadas existem e não há ciclo. Mas a mistura de regra com SQL está em todos os serviços, não
num ponto só.

## Design de baixo nível

**Números:** a variação do cenário tem 3 regras (bloqueio, cópia, modo de teste). Nenhuma está numa estrutura
própria: as três são `if` dentro de um método (`service/MessageService.java:48-77`). As duas regras anunciadas
(madrugada, limite por dia) não teriam onde entrar.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede regras em volta do envio que se combinam por modo e por instalação, com mais regras por
vir. O código faz tudo dentro de `sendMessage`:
- o bloqueio é um `if (!EmailValidator.isValid(to))` que grava como BLOCKED e retorna (`MessageService.java:54-60`);
- o modo de teste é um `if (testMode)` que troca destinatário, assunto e texto (`MessageService.java:62-68`);
- a cópia é um `if (!testMode && status == SENT)` (`MessageService.java:72-76`).

Não há interface de envio, embrulhos, corrente nem lista de filtros. A combinação por modo está escrita dentro do
próprio envio, e o `testMode` vem de uma variável de ambiente lida no construtor (`MessageService.java:19`). A segunda
pousada, que não quer a cópia, ou a regra da madrugada só no modo de teste, exigiriam mexer nesse método e somar mais
condições. Os casos da referência descrevem exatamente esta situação como **omissão**. O comportamento está certo, mas
a estrutura pedida não existe.

**Exagero:** não há interfaces de uma implementação só, fábricas nem camadas que só repassam: **contenção**.

**O resto do código.** Os defeitos se repetem pelas classes:

- **Erros por texto.** As regras lançam `RuntimeException("ROOM_NOT_FOUND")` e similares
  (`ReservationService.java:68,72,76,80,160,...`). O handler decide o status HTTP com um `switch` sobre
  `e.getMessage()` (`ReservationHandler.java:203-226`), e qualquer outra `RuntimeException` cai no `default` como 500.
  Foi o que aconteceu com o plano inválido: `IllegalArgumentException` do `RatePlan.valueOf` virou 500
  `INTERNAL_ERROR`. Um id não numérico (`/reservations/abc`) também dá 500, porque o `NumberFormatException` não é
  tratado (`ReservationHandler.java:62,77,...`). Na criação, um `catch (Exception e)` transforma qualquer falha em 400
  e esconde a causa (`ReservationHandler.java:55-57`).
- **Tipos soltos.** O plano chega e é gravado como `String` sem validação (`ReservationService.java:63-64,103`), e o
  desconto compara texto (`util/Pricing.java:45`), embora exista o `enum RatePlan`. O tipo do quarto também é
  `String` (`model/Room.java`).
- **Bug no cálculo de noites.** `Pricing.getNights` usa `checkIn.until(checkOut).getDays()` (`util/Pricing.java:58`),
  que devolve só o componente "dias" do `Period`. Conferido pela API: uma estadia de 10/03 a 12/04 grava `nights: 2`
  com subtotal de 33 noites (R$ 8.246,70).
- **Duplicação.**
  - A formatação de dinheiro com `String.format("%.2f")` está copiada em quatro lugares (`ReservationHandler.java:188`,
    `RoomHandler.java:33`, `ReportService.java:145`, `ReservationService.java:328`). Nenhuma delas põe o separador de
    milhar pedido ("R$ 1.234,56"), e todas dependem do `Locale` padrão.
  - `formatCpf` está duplicado (`ReservationService.java:315`, `ReservationHandler.java:191`), assim como a
    formatação de data (`ReservationService.java:323`, `ReportService.java:136`, `ReportService.java:141`,
    `ReservationHandler.java:199`).
- **Estado.** Os modelos têm todos os campos públicos e mutáveis (`model/Reservation.java`, `model/Room.java`,
  `model/Message.java`). `MessageService.messageId` é carregado do banco e nunca usado (`MessageService.java:15,29`).
  Em `getDocument`, o `receipt` é calculado e descartado (`ReservationHandler.java:137`).
- **JSON à mão.** O JSON é montado em `HashMap` campo a campo em cada handler (`ReservationHandler.java:162-185`,
  `RoomHandler.java:21-29`, `MessageHandler.java:18-29`).

Os defeitos aparecem em quase todas as classes, e o tratamento de erro por texto torna arriscado mudar qualquer regra.
Por isso a nota é `ruim`.

## Testes

**Números:** 22 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 22 métodos escritos, em 2 classes (`PricingTest`
com 12 e `EmailValidatorTest` com 10). Cobertura de linhas: 47 de 739 (6,4%). Cobertura de ramos: 39 de 199 (19,6%).
Os pacotes `service`, `handler`, `db` e `Main` estão com 0% (`measures/tests.md`). Não há nenhum teste pela API nem
contra o banco.
**Nota:** ruim

O que existe é bom no que faz. `PricingTest` confere valores reais:
- a noite de alta temporada a 299,88 (`src/test/java/com/pousadamarealta/util/PricingTest.java:37-39`);
- os 599,76 do exemplo do texto (`PricingTest.java:55-62`);
- o desconto de 89,96 e o total de 509,80 (`PricingTest.java:65-85`);
- a fronteira 14/12 contra 15/12 (`PricingTest.java:14-18`).

`EmailValidatorTest` cobre os casos inválidos do texto, como "ana@gmail", dois @ e o @ no começo
(`EmailValidatorTest.java:14-57`).

Fica sem nenhum teste:
- a máquina de estados da reserva (confirmar, check-in, check-out, as recusas OPERATION_NOT_ALLOWED);
- o cancelamento e a devolução por plano;
- o conflito de datas;
- os dois relatórios;
- o comprovante e o e-mail;
- todas as regras de envio do cenário fora a validação do endereço.

Na pergunta da mutação: apagar a cópia para o arquivo (`MessageService.java:72-76`), trocar `!testMode` por
`testMode`, ou não pôr o "[TESTE] " no assunto (`MessageService.java:64`) não derruba nenhum teste. O bug de
`getNights` (`Pricing.java:58`) também passa, porque o único teste usa duas noites no mesmo mês (`PricingTest.java:50-52`).
Com 6,4% das linhas e só a camada `util` testada, a coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Os 6 campos de dinheiro estão
em `numeric(10,2)`, as datas em `date`, e a chave estrangeira `reservations.room_number → rooms.number` tem índice (o
parcial `idx_reservations_room_dates` começa por `room_number`). As conferências acusaram:
- 0 restrições que protejam uma regra: nenhum CHECK, UNIQUE ou EXCLUDE;
- `status`, `rate_plan`, `type` e `channel` sem CHECK;
- 0 transações explícitas e 0 `FOR UPDATE`;
- esquema criado por `CREATE TABLE` no código, sem migrações (`measures/database.md`).
**Nota:** com problemas

O que está certo:
- os tipos;
- `NOT NULL` em tudo o que é obrigatório; só `refund` aceita null, como o texto pede (`db/Database.java:42-61`);
- SQL sempre com `?`;
- conexões em try-with-resources sobre um pool Hikari (`Database.java:16-25`);
- os quartos semeados uma vez só (`Database.java:81-86`).

Os dados sobrevivem ao reinício (3 de 3 no roteiro).

Os problemas:

- **O esquema não evolui.** O esquema é um `CREATE TABLE IF NOT EXISTS` dentro do código (`Database.java:32-78`),
  sem migrações versionadas. A primeira coluna nova numa tabela existente não será aplicada.
- **Nada protege a regra "um quarto, uma reserva por noite".** `createReservation` consulta o conflito
  (`service/ReservationService.java:79`, `255-275`) e depois insere numa outra conexão (`ReservationService.java:88-118`),
  sem transação, sem trava e sem restrição `EXCLUDE` no banco. O roteiro mostrou o resultado: 10 de 10 rodadas
  simultâneas criaram as duas reservas.
- **Transições sem trava.** As mudanças de situação fazem leitura e depois `UPDATE` sem condição sobre o status atual
  (`ReservationService.java:158-174`, `226-250`). Duas confirmações simultâneas passam pela verificação.
- **Dado inválido gravado.** Sem CHECK em `rate_plan` e sem validação no código, um `ratePlan: "XYZ"` é gravado.
  Conferido pela API: o POST responde 500, mas a linha fica no banco, `GET /reservations/2` responde 500 para sempre
  (`RatePlan.valueOf` em `ReservationService.java:142`), e o quarto fica bloqueado naquelas noites (uma nova reserva
  recebeu 409 ROOM_UNAVAILABLE).

O defeito da reserva dobrada pesa também no comportamento, onde foi medido. Aqui conta a falta de qualquer proteção
no banco e o dado inválido que fica gravado.

## Comportamento

**Números:** 60 de 65 conferências passaram (`measures/behavior.md`):
- a parte comum: 42 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- as reservas simultâneas: 1 de 2. As duas reservas foram criadas em 10 das 10 rodadas.
**Nota:** com problemas

As regras de e-mail do cenário funcionam todas nos dois modos. O endereço inválido fica BLOCKED com o endereço
original e sem cópia. A cópia "[Cópia] " vem logo depois da original. O modo de teste desvia para
`testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a linha "Destinatário original: …", e sem cópia. O preço,
o arredondamento, as recusas, as devoluções e a persistência também passam.

O que falhou, e o que isso quer dizer para o dono:

- **Reserva dobrada.** Em 10 de 10 rodadas, dois pedidos simultâneos para o 301 nas mesmas noites foram ambos aceitos
  (201 e 201). Se dois hóspedes reservarem pelo site ao mesmo tempo, os dois ficam com o quarto. É o defeito mais
  grave para a pousada.
- **Dinheiro como texto no JSON.** O valor vem como `"amountPaid":"0.00"` em vez de `0.00`
  (`handler/ReservationHandler.java:174-178,188`). O mesmo vale para `subtotal`, `discount`, `total` e `nightlyRate`.
  Isso quebra o combinado com o site, que espera números.
- **Check-out sem o comprovante combinado.** O check-out devolve a reserva com o comprovante em texto num campo
  `document` (`ReservationHandler.java:114-120`), e não o objeto com `title`, `reservationId`, etc. O site não recebe
  o comprovante no formato combinado.
- **Datas da ocupação no formato errado.** O relatório de ocupação escreve as datas como `10/07/2029`, e não
  `2029-07-10` (`service/ReportService.java:60`, `136`). Isso contraria o texto para o CSV.
- **Financeiro com cancelada que não deixou dinheiro.** O relatório financeiro lista a reserva STANDARD cancelada e
  devolvida com `0.00`, porque inclui toda CANCELLED (`ReportService.java:95`) e não filtra o valor zero. O texto pede
  só as reservas que "deixaram dinheiro com a pousada". A soma está certa (599,76), mas a linha de total saiu como
  `total,,,,599.76`. O texto não fixava o formato dessa linha, então esse detalhe não pesa. A linha da cancelada a
  mais pesa.

Achados fora do roteiro, conferidos com `curl`:
- o comprovante e o e-mail escrevem "Total: R$ 1159,80" para a suíte de duas noites, sem o separador de milhar que o
  texto pede (`ReservationService.java:328`). Isso acontece em qualquer reserva de mais de R$ 1.000;
- uma estadia de 33 noites aparece com `nights: 2` (`util/Pricing.java:58`);
- `GET /reservations/abc` dá 500;
- o plano inválido dá 500 e grava uma reserva ilegível (contado no Banco).

A parte comum funciona no essencial e o cenário passa inteiro. Por isso a nota não é `ruim`. Mas há quatro quebras de
contrato com o site e com os relatórios, mais a reserva dobrada sob concorrência.

## Dúvidas

- No Comportamento, a nota ficou entre `com problemas` e `ruim`. A skill põe "só as simultâneas falham" em
  `com problemas`, mas aqui elas falham em 10 de 10 rodadas e se somam a quatro falhas da parte comum e aos defeitos
  achados com `curl` (milhar no comprovante, noites erradas em estadia longa). Fiquei com `com problemas` porque o
  fluxo principal e o cenário inteiro funcionam.
- No Banco, a falta total de proteção contra a reserva dobrada e a linha ilegível gravada com plano inválido chegam
  perto de "risco de dado errado". Dei `com problemas` porque nenhum dos itens de `ruim` da skill aparece (ponto
  flutuante, SQL montado com dado de fora, tabela sem chave, dado que some) e porque a reserva dobrada já pesa no
  Comportamento.
- No `target/` há dois jars. O `hotel-system-1.0.0.jar` não tem `Main-Class`, por causa do `finalName` dentro da
  execução do shade (`pom.xml:100`), e só o `hotel-system.jar` sobe com `java -jar`. O texto pede que "o jar" suba
  sozinho, e um deles sobe. Não pesei isso.
