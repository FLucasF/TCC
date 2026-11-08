# Revisão: construção, cenário 03

## Visão geral

O projeto tem 8 pacotes: `App` (a montagem e as rotas do Javalin), `api.controller` (os handlers), `api.dto` (os
records de entrada e saída, serializados com Gson), `domain` (a `Reservation` com o preço, a máquina de estados e a
lista de ouvintes), `notification` (a interface `ReservationObserver` e um ouvinte por interessado), `repository`
(JDBC puro com HikariCP) e `util` (temporada e formatação). Uma requisição vai da rota em `App.java:72-85` ao
controller, que lê a reserva pelo repositório, chama o método da entidade (`confirm`, `cancel`...), que avisa os
ouvintes, e salva de volta; os dados ficam em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` em
`DatabaseInitializer.java`. O mais sério: a coluna `guest_cpf` é `VARCHAR(11)` (`DatabaseInitializer.java:33`) e nada
normaliza o CPF, então uma reserva com o CPF no formato que o texto mostra (`111.222.333-44`) estoura com 500. Pelo
roteiro, nenhuma reserva foi criada (17 de 66 conferências passaram). Além disso, nada impede que duas reservas
simultâneas fiquem com o mesmo quarto: testei à mão e as duas foram criadas em todas as 9 rodadas. E o dinheiro sai no
JSON como `double` (779.8, 0.0) e não com 2 casas.

## Arquitetura

**Números:** 8 pacotes, 18 dependências entre eles, 2 ciclos (`domain` ↔ `notification` e `repository` ↔
`notification`), em `measures/dependencies.md`. O Javalin só aparece em `App` e `api.controller`; o JDBC, em `App`
(o pool) e `repository`.
**Nota:** com problemas

O que está bem: o acesso ao banco fica isolado nos repositórios, as regras de preço e de situação moram na
`Reservation` sem Javalin e sem SQL (`Reservation.java:57-131`), e a montagem fica num lugar só (`App.java:29-53`).
Os problemas:

- Não há camada de serviço, e parte da regra fica no handler. A checagem de quarto livre (`ReservationController.java:231-237`),
  a de capacidade (`:220-222`) e a de quem pode tirar a segunda via (`:340-343`) estão no controller. Ele também faz a
  orquestração (buscar, mudar, salvar) repetida em cada handler (`:272-280`, `:288-296`, `:304-312`, `:320-328`).
- Os dois ciclos vêm de uma escolha só: os ouvintes ficam presos na entidade. A `domain.Reservation` importa
  `notification.ReservationObserver` (`Reservation.java:3,35`), e o `ReservationRepository` recebe a lista de ouvintes
  e a pendura em cada reserva que lê do banco (`ReservationRepository.java:143-148`, `:367-369`). O controller faz o
  mesmo ao criar (`ReservationController.java:251-253`). Então a ligação dos ouvintes acontece em dois lugares fora da
  montagem, e o repositório (camada de dados) precisa conhecer as notificações.
- O `NotificationService` (`notification`) depende do `MessageRepository` (`NotificationService.java:3,9`). Isso está
  certo na direção, mas fecha o ciclo com o repositório que depende de `notification`.

Não chega a `ruim` porque as camadas existem e o SQL não vaza para as rotas. Mas são dois ciclos e uma regra de
negócio (a disponibilidade) no handler.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro) × 2 eventos (confirmação, cancelamento). Os 6 casos
estão na estrutura, e nenhum fica fora dela. 1 interface de ouvinte e 3 implementações, com a lista montada em
`App.java:40-44`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `ReservationObserver` (`ReservationObserver.java:5-8`) tem `onReservationConfirmed` e
`onReservationCancelled`, e cada interessado é uma classe: `GuestNotifier`, `HousekeepingNotifier` e `FinanceNotifier`.
O e-mail do hóspede também é um ouvinte (`GuestNotifier.java:15-30`). Quem anuncia é a `Reservation`, que percorre a
lista sem saber quem está nela (`Reservation.java:133-143`), e a lista vem de fora, montada em `App.java:40-44`. A
regra de cada destino (não avisar quando a reserva não estava confirmada, não avisar o financeiro sem valor a devolver)
mora no próprio ouvinte (`HousekeepingNotifier.java:63-66`, `FinanceNotifier.java:33-40`), e não em quem anuncia. Para
pôr o restaurante ou o transfer, basta uma classe nova e uma linha em `App.java:40-44`. O que afasta um pouco do ideal:

- o anunciante é a entidade, e não um serviço. Por isso a lista tem de ser pendurada em cada instância, no repositório
  e no controller (ver Arquitetura);
- o evento de cancelamento leva um `boolean wasConfirmed` (`Reservation.java:120,141`), e não a situação anterior;
- os avisos saem antes de a reserva ser salva (`Reservation.java:98`, e o save só vem em
  `ReservationController.java:277`), fora de uma transação. Se o `UPDATE` falhar, os e-mails já foram gravados.

Mesmo assim, é a estrutura que a referência chama de acerto.

**O exagero.** Não há estrutura sem necessidade. O `NotificationService` (`NotificationService.java:12-15`) é fino, mas
monta a `Message` com o canal e evita repetir isso em cada ouvinte. Não há fábricas nem interfaces com uma
implementação só.

**O resto do código.**

- Dinheiro em `double` na saída: os DTOs convertem `BigDecimal` com `doubleValue()` (`ReservationResponse.java:253-258,272-277`,
  `DocumentResponse.java:187-189,202-204`, `RoomResponse.java:290,297`), e o CSV usa `%.2f` sobre `doubleValue()`
  (`ReportController.java:79-91`), que ainda depende do `Locale` padrão da máquina. O resultado é `779.8` e `0.0` no
  JSON.
- A leitura do banco recalcula subtotal, desconto e total a partir da diária atual do `enum` (`ReservationRepository.java:348-358`
  chama o construtor que recalcula, `Reservation.java:51-54`) e ignora o que foi gravado. Se a diária mudar, as
  reservas antigas mudam de valor.
- Setters públicos `setStatus`, `setAmountPaid` e `setRefund` (`Reservation.java:209-227`) deixam qualquer um pular a
  máquina de estados. `getObservers` expõe a lista mutável (`:229-231`). A variável `previousStatus` não é usada
  (`:94`).
- Duplicação: o mesmo `findById(...).orElseThrow(RESERVATION_NOT_FOUND)` em 6 handlers
  (`ReservationController.java:263,272,288,304,320,337`); a mesma lista de 17 colunas no `SELECT` em 4 consultas
  (`ReservationRepository.java:226,252,285,317`); `parseDate` copiado em dois controllers
  (`ReservationController.java:376-382`, `ReportController.java:97-103`).
- Os erros: o corpo que não é JSON cai no `catch (Exception)` genérico e vira 500 (`App.java:66-70`), em vez do 400
  pedido.
- `Formatter.formatMoney` usa o `NumberFormat` de moeda do pt-BR (`Formatter.java:311,315`), que põe um espaço
  não-separável entre "R$" e o valor, e não o espaço comum do texto.

São defeitos localizados, e o domínio usa `BigDecimal` com `HALF_EVEN` e `LocalDate` onde importa
(`Reservation.java:57-83`). Por isso a nota é `com problemas`.

## Testes

**Números:** 18 testes rodaram, 0 falharam e 0 foram pulados; 18 métodos escritos em 3 arquivos. Cobertura de 13,9%
das linhas (110 de 790) e 22,7% dos ramos (35 de 154). `domain` tem 101 de 181 linhas; `notification`,
`repository`, `api.controller` e `api.dto` têm 0 (`measures/tests.md`). Nenhum teste passa pela API nem pelo banco.
**Nota:** com problemas

O que está testado, com valores:

- o preço da alta temporada, 599,76 (`ReservationTest.java:116-135`);
- o NON_REFUNDABLE com desconto de 116,97 e total de 662,83 (`:137-154`);
- as transições permitidas e recusadas (`:156-253`, `:317-336`);
- as devoluções nos três casos de cancelamento (`:255-315`);
- os limites da temporada, inclusive 14 e 15/12 (`SeasonCalculatorTest.java:355-361`).

A parte nova do cenário só tem testes do mecanismo. `ReservationObserverTest.java:28-102` confere, com um ouvinte
falso, que a entidade chama os ouvintes na confirmação e no cancelamento. Os três ouvintes de verdade têm 0% de
cobertura. A pergunta da mutação:

- trocar `governanca@...` ou `financeiro@...` (`HousekeepingNotifier.java:7`, `FinanceNotifier.java:9`) não derruba
  nenhum teste;
- trocar o assunto "Reserva confirmada - quarto %s" (`HousekeepingNotifier.java:17`) não derruba nenhum teste;
- apagar o `return` que não avisa o financeiro quando não há o que devolver (`FinanceNotifier.java:38-40`) não derruba
  nenhum teste.

Também ficaram sem teste o comprovante e o e-mail do hóspede (`ReservationDocument.render`, `GuestNotifier`), os
códigos de erro, a disponibilidade, os dois relatórios e a persistência. É justamente onde estão os defeitos que o
roteiro achou (o CPF, o dinheiro no JSON, o JSON inválido). Os testes que existem são bons, mas cobrem só o domínio.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, ok. Há 0 chaves estrangeiras, 0 CHECK e 0 UNIQUE/EXCLUDE. O esquema é criado com `CREATE TABLE` no
Java, sem migrações. São 0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** ruim

- **Um tipo que quebra o uso normal.** `guest_cpf VARCHAR(11)` (`DatabaseInitializer.java:33`), sem nenhuma
  normalização na entrada (`ReservationController.java:244`). Um CPF com pontos e traço, como o texto escreve, dá
  `value too long for type character varying(11)`: 23 vezes no `measures/behavior-service.log`, e cada uma vira 500 ao
  criar a reserva.
- **Nada impede duas reservas no mesmo quarto.** A disponibilidade é um `SELECT` (`ReservationRepository.java:250-281`)
  seguido de um `INSERT` em outra conexão (`:158-182`), sem transação, sem trava e sem restrição de exclusão. Nem um
  `synchronized` protege o processo. Com o serviço de pé e um CPF de 11 dígitos, mandei dois pedidos simultâneos em 9
  rodadas, e as duas reservas foram criadas (201 e 201) em todas. É dado errado: o mesmo quarto vendido duas vezes.
- O esquema não evolui: `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:20,27,49`) não acrescenta uma coluna
  numa tabela que já existe.
- `reservations.room_number` não tem chave estrangeira para `rooms` (`DatabaseInitializer.java:29`), e `room_type` é
  copiado na reserva (`:30`). A capacidade e a diária não estão no banco, só no `enum` (`RoomType.java:4-6`).
- As colunas `status`, `rate_plan`, `room_type` e `channel` são `VARCHAR`, sem CHECK.
- Há um pool (`App.java:108-114`), o SQL usa `?` e as conexões são fechadas com try-with-resources. O `subtotal`,
  `discount` e `total` gravados não são lidos de volta (ver Design).

O tipo do CPF impede o uso que o texto descreve, e a duplicação de reserva fica totalmente sem proteção. Os dois juntos
dão `ruim`.

## Comportamento

**Números:** 17 de 66 conferências passaram (`measures/behavior.md`):

- a parte comum: 13 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 03: 2 de 15;
- duas reservas ao mesmo tempo: 1 de 2, mas as 10 rodadas deram 500 e 500.

**Nota:** ruim

A causa principal: o roteiro manda o CPF como `111.222.333-44`, o formato do próprio texto, e todo `POST /reservations`
válido responde 500 (a coluna de 11 caracteres, na coluna Banco). Daí em diante não existe reserva. Confirmação,
e-mails, check-in, check-out, cancelamentos, segunda via e relatórios com dados falham em cascata (404 ou CSV vazio).
Os 2 OK do cenário ("o financeiro não é avisado", "cancelar pendente não manda nada") passaram porque nenhuma mensagem
foi gerada, e não provam a regra. Para o dono da pousada, o site não consegue reservar nada.

O que passou é o que não depende de criar reserva: `GET /rooms` com cinco quartos, as recusas 422, 404 e 400 de
validação, os cabeçalhos dos CSV e o 400 do mês inválido.

Outras falhas, independentes do CPF:

- **Corpo que não é JSON:** responde 500 INTERNAL_ERROR, e não 400 (`App.java:66-70`).
- **Dinheiro no JSON:** à mão, com um CPF de 11 dígitos, o serviço devolveu `"subtotal": 779.8`, `"discount": 0.0` e
  `"nightlyRate": 249.9`. O texto pede 2 casas (599.76), e isso também falharia na conferência "amountPaid 0.00".
- **"o 201 comporta 3" e "a diária do 201":** o roteiro procura `number == 201` como número, e o serviço devolve
  `"201"` como texto. O texto não diz o tipo do número do quarto, então esse ponto estava em aberto e não pesa. Já a
  diária sairia `389.9` de qualquer jeito.

O que vi à mão, com um CPF de 11 dígitos, sobre a parte nova:

- Os avisos saem com os destinos e os assuntos certos: "Reserva confirmada - quarto 201", "Pagamento recebido -
  reserva 1", "Reserva cancelada - quarto 201" e "Reembolso - reserva 1". A devolução de um STANDARD é de R$ 779,80.
- O valor aparece como `R$ 779,80`, com o espaço não-separável, e não como "R$ 779,80". A busca literal do roteiro
  pelo comprovante e pelo valor pago provavelmente falharia mesmo assim.
- O relatório financeiro deixou de fora a cancelada com devolução, como pedido.

O defeito das reservas simultâneas aparece aqui como 500 e 500 por causa do CPF, mas pesa na coluna Banco.

## Dúvidas

- O texto não fixa em que formato o CPF chega no `POST /reservations`, só como ele sai no comprovante. Mesmo que se
  aceite o CPF só com dígitos, sobram o dinheiro sem 2 casas, o 500 para JSON inválido e as reservas duplicadas em
  pedidos simultâneos. Com isso, eu daria `com problemas` no comportamento, e não `bom`.
- Na arquitetura, fiquei entre `com problemas` e `ruim`, porque há dois ciclos. Fiquei com `com problemas` porque os
  dois vêm da mesma decisão (os ouvintes presos na entidade) e as camadas de HTTP e de dados estão separadas.
- Nos testes, os que existem conferem valores, mas a parte nova do cenário (os três ouvintes) não tem cobertura
  nenhuma. Pesei como `com problemas`, e não `ruim`.
