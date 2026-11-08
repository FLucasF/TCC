# Revisão: construção, cenário 01

## Visão geral

O projeto (`projeto/`) tem sete pacotes. `pousada.Main` monta tudo e registra as rotas (`Main.java:22-55`).
`pousada.api` tem quatro controllers que recebem o `Context` do Javalin. `pousada.domain` tem `Reservation`, com as
transições de situação, `Room`/`RoomType` e `Message`. `pousada.domain.rateplan` tem os cinco planos de tarifa. Em
`pousada.repository` ficam três repositórios JDBC sobre um pool Hikari, e em `pousada.service` só o
`NotificationService`, que monta o comprovante e o e-mail. Não há serviço de reservas: uma requisição
`POST /reservations` entra em `ReservationController.createReservation`, que lê o corpo como `Map`, confere a
capacidade e a disponibilidade, chama `Reservation.create` (que pede o preço ao plano) e grava pelo
`ReservationRepository`. Os dados ficam em três tabelas que os construtores dos repositórios criam com
`CREATE TABLE IF NOT EXISTS`.

A parte pedida pelo cenário (o Strategy dos planos) está bem resolvida, e as contas do texto conferem. Os problemas
sérios estão em outro lugar:
- a coluna `guest_cpf` é `VARCHAR(11)` (`ReservationRepository.java:29`). Com o CPF escrito como o próprio texto
  escreve (`111.222.333-44`), toda criação de reserva dá 500;
- nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. À mão, isso aconteceu em
  6 de 9 rodadas;
- os testes cobrem 8,8% das linhas.

## Arquitetura

**Números:** 7 pacotes, 12 dependências entre eles e 1 ciclo, `pousada.domain` ↔ `pousada.domain.rateplan`
(`measures/dependencies.md`). O Javalin só aparece em `pousada` e `pousada.api`, e o JDBC só em `pousada.config` e
`pousada.repository`. A maior classe é `ReservationController`, com 284 linhas.
**Nota:** com problemas

**O que está bem:**
- As regras não conhecem o Javalin nem o JDBC. O domínio e os planos não importam nada de HTTP ou SQL.
- O acesso ao banco fica isolado nos repositórios.
- A montagem é feita num lugar só (`Main.java:22-35`).

**O que está mal:**
- **Não há camada de regras para a reserva.** O controller faz a orquestração e guarda regras de negócio:
  - a capacidade, em `ReservationController.java:57`;
  - a disponibilidade do quarto, em `ReservationController.java:62-66`;
  - a regra de que a segunda via só existe para reserva paga, em `ReservationController.java:188`;
  - a confirmação, que grava e manda o e-mail em sequência, em `ReservationController.java:107-109`.
- **O relatório financeiro mora no handler.** A regra "o que fica com a pousada é o pago menos o devolvido" e a
  montagem do CSV estão em `ReportController.java:83-99`.
- **Há montagem estática fora do `main`:**
  - `RatePlanFactory` é um mapa estático, chamado de dentro da entidade (`Reservation.java:55` e `:110`). Isso cria o
    ciclo entre `domain` e `domain.rateplan`.
  - O tipo de cada quarto está fixo no código em `Room.typeFromNumber` (`Room.java:24-30`), repetindo o que já está na
    tabela `rooms`. `Reservation.roomType()` usa essa cópia (`Reservation.java:186-188`).
- **Os repositórios criam o esquema no próprio construtor** (`ReservationRepository.java:17-51`,
  `RoomRepository.java:16-61`).

O ciclo é pequeno, entre dois pacotes vizinhos. A mistura está localizada nos controllers de reserva e de relatório,
por isso `com problemas` e não `ruim`.

## Design de baixo nível

**Números:** os cinco planos estão na estrutura e nenhum fica fora dela. Há uma interface, `RatePlan`, e cinco
implementações sobre uma classe abstrata, escolhidas por um mapa. Nenhum `switch` ou `if` sobre o plano aparece no
preço, na validação ou no cancelamento.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Strategy).**
- `RatePlan` (`RatePlan.java:7-15`) reúne as três coisas que variam: validar, calcular o preço e calcular a devolução.
- Cada plano, inclusive o normal e o não reembolsável, tem uma classe só sua:
  - `StandardRatePlan`;
  - `NonRefundableRatePlan`;
  - `CorporateRatePlan`, com o mapa das empresas em `CorporateRatePlan.java:11-15` e a validação em `:23-27`;
  - `WeekendPackageRatePlan`, com o preço fechado em `:30-33` e a metade devolvida em `:36-38`;
  - `LongStayRatePlan`, com o mínimo de 7 noites em `:19-23`.
- A escolha acontece num único ponto, o mapa de `RatePlanFactory.java:7-13`.
- A reserva só pede ao plano, sem nomeá-lo: a validação e o preço em `Reservation.java:55-58`, a devolução em
  `Reservation.java:110-111`.
- Um plano novo é uma classe a mais e uma linha no mapa.

Pequenos pontos que afastam um pouco, sem mudar a classe:
- O parâmetro `companyCode`, que só o convênio usa, entra na assinatura de todos os planos (`RatePlan.java:11` e
  `:13`).
- `isWeekendPackagePeriod`, que só o pacote usa, fica na classe abstrata (`AbstractRatePlan.java:62-66`).
- `RatePlan.name()` nunca é chamado.

**O exagero.** Não há. `AbstractRatePlan` guarda o que os cinco planos realmente compartilham: o arredondamento
HALF_EVEN, a temporada e a soma das noites. `PricingResult` é um record simples, e o mapa de seleção faz parte do
pattern.

**O resto do código (nota `com problemas`):**
- **Duplicação.**
  - Os seis handlers de `ReservationController` repetem o mesmo bloco: `parseLong`, `findById`, `orElseThrow` e três
    `catch` (`ReservationController.java:86-200`). Não há um tratador de exceções central.
  - As noites são contadas em dois lugares (`Reservation.java:59` e `AbstractRatePlan.java:58`).
- **Erros.**
  - Um `catch (IllegalArgumentException)` genérico vira 400 (`ReservationController.java:81-83`). Ele também
    esconderia um erro de programação.
  - O cast `(String) body.get("companyCode")` (`ReservationController.java:42`) dá `ClassCastException` e 500 quando o
    campo vem como número.
  - O corpo lido com `bodyAsClass(Map.class)` (`ReservationController.java:32`) dá 500 quando não é JSON. O texto pede
    400.
- **Tipos e escala do dinheiro.**
  - `amountPaid` nasce como `BigDecimal.ZERO`, sem escala (`Reservation.java:77`), e os descontos zerados também
    (`StandardRatePlan.java:22`, `WeekendPackageRatePlan.java:32`). Por isso o JSON sai `0`, e não `0.00`.
  - O plano é guardado como `String` e procurado de novo a cada uso (`Reservation.java:110`).
- **Formatação.** O dinheiro do comprovante sai de `NumberFormat.getCurrencyInstance(pt-BR)`
  (`NotificationService.java:15` e `:76`), que escreve `R$` seguido de um espaço não separável (U+00A0), e não do
  espaço comum que o texto mostra.
- **Código morto.** `Reservation.occupiesRoom` nunca é chamado.

## Testes

**Números:** 8 testes rodaram, 0 falharam e 0 foram pulados. Há 8 métodos escritos, numa classe só
(`RatePlanTest`). A cobertura é de 68 de 773 linhas (8,8%) e de 15 de 152 ramos (9,9%). Os pacotes `api`,
`repository`, `service` e `config` ficaram com 0%. Não há nenhum teste pela API nem contra o banco
(`measures/tests.md`).
**Nota:** ruim

**O que os testes conferem.** Só o cálculo de cada plano isolado, mas com valores certos:
- os três exemplos do texto: Tecnosul 779,80 / 93,58 / 686,22 (`RatePlanTest.java:49-53`), o pacote 820,00 com
  devolução de 410,00 (`:63-70`) e a estadia longa 1749,30 / 174,93 / 1574,37 (`:80-84`);
- o normal na alta temporada (`:21-25`);
- três recusas de validação (`:88-125`).

**O que ficou sem teste.** Fora o preço, nada do que o texto pede tem teste:
- as transições PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT e as que são proibidas;
- o cancelamento chamado pela reserva;
- os relatórios;
- o comprovante e o e-mail;
- os códigos de erro da API;
- a gravação no banco.

**A pergunta da mutação**, em regras da parte nova:
- Trocar 0.18 da Agrovale ou 0.25 da Farmavida por outro número não quebra nenhum teste. Só a Tecnosul é testada.
- Trocar `<` por `<=` em `LongStayRatePlan.java:20` também não quebra nada: o teste usa 2 noites
  (`RatePlanTest.java:103-104`), não 6 nem 7.
- A devolução do convênio, da estadia longa e do normal (tudo) e a do não reembolsável (nada) não são testadas.
- Não há caso positivo para a validação do pacote. Se `validate` passasse a recusar toda sexta-feira, nada falharia.

Com 0% em `api` e `repository`, a coluna `VARCHAR(11)` do CPF e o `bodyAsClass` que dá 500 passaram sem ser vistos.
Os testes do preço são bons, mas o sistema praticamente não tem teste, por isso `ruim`.

## Banco

**Números:** com o script do jeito que está na skill, o banco não foi medido. O script sobe o maior jar com
`Main-Class` em `target/`, e esse jar é o driver do PostgreSQL copiado para `target/lib`
(`measures/database-script-jar-errado.md`). Rodei uma cópia do script que ignora `target/lib`. Com ela, o serviço
subiu num banco vazio e criou 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária
(`measures/database.md`). O dinheiro está em `numeric(10,2)` e as datas em `date`. Não há nenhuma chave estrangeira,
nenhum CHECK nas colunas de situação e plano, e nenhuma restrição UNIQUE ou EXCLUDE. O código usa 0 transações
explícitas e 0 `FOR UPDATE`, e não há SQL montado com `+`.
**Nota:** ruim

**O que está bem:**
- tipos certos para dinheiro e datas;
- SQL sempre com parâmetros;
- conexões em try-with-resources;
- pool Hikari (`Database.java:19-25`);
- os dados sobrevivem a um reinício.

**Os defeitos sérios:**
- **O CPF não cabe na coluna.** `guest_cpf VARCHAR(11)` (`ReservationRepository.java:29`) só aceita o CPF sem
  pontuação. Com `111.222.333-44`, a forma que o próprio texto usa, o INSERT falha com
  `value too long for type character varying(11)` e a API devolve 500. Foi o que aconteceu quando chamei a API à mão.
  A recusa não é validada antes de chegar ao banco.
- **Nada impede duas reservas do mesmo quarto na mesma noite.** O controller consulta e depois insere
  (`ReservationController.java:62-73`), sem transação, trava ou restrição; nem um `synchronized` existe. À mão, em 9
  rodadas de dois pedidos simultâneos para o quarto 202, as duas reservas foram gravadas em 6. Isso é dado errado no
  banco.

**Os outros defeitos:**
- O esquema é criado com `CREATE TABLE IF NOT EXISTS` nos construtores dos repositórios, sem migrações. Não há como
  acrescentar uma coluna numa tabela que já existe.
- `reservations.room_number` não tem chave estrangeira para `rooms.number`.
- `status` e `rate_plan` não têm CHECK.
- Na confirmação, a mudança de situação e a mensagem são duas escritas independentes, sem transação
  (`ReservationController.java:108-109`).
- `findByMonth` filtra com `EXTRACT(...)` sobre `check_in` (`ReservationRepository.java:207-212`), e isso não usa
  índice. O peso é pequeno.

## Comportamento

**Números:**
- **O roteiro como está na skill:** 0 conferências. O serviço não subiu porque o script escolheu o jar do driver
  PostgreSQL (`measures/behavior-script-jar-errado.md`).
- **A cópia do script que ignora `target/lib`:** 18 de 72 passaram (`measures/behavior.md`):

  | parte | passaram |
  |---|---|
  | parte comum | 11 de 46 |
  | dados depois de reiniciar | 1 de 3 |
  | cenário | 5 de 21 |
  | duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** ruim

**Por que tanta falha no roteiro.** Quase todas as falhas vêm de uma cascata: o roteiro manda `roomNumber` como
número (`201`), e o projeto só aceita texto. `getRequiredString` responde 400 `Missing or invalid field: roomNumber`
(`ReservationController.java:240-245`). O texto não diz o tipo, então isso sozinho seria uma escolha discutível. O
problema é que, mesmo com `roomNumber` como texto, a criação continua quebrada com o CPF do roteiro
(`111.222.333-44`), agora com 500, por causa do `VARCHAR(11)` (veja Banco). Um site que mande o CPF formatado não
consegue criar nenhuma reserva.

**Chamando a API à mão**, com `roomNumber` como texto e o CPF só com dígitos:

- **Funciona:**
  - 599,76 no normal em julho e 37,48 / 212,42 no não reembolsável;
  - 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED e 404 ROOM_NOT_FOUND / RESERVATION_NOT_FOUND;
  - a máquina de situações, com 409 OPERATION_NOT_ALLOWED;
  - a segunda via só para reserva paga;
  - as devoluções;
  - os dois relatórios em `text/csv; charset=utf-8`;
  - os dados depois de reiniciar;
  - todo o cenário:

    | plano | resultado |
    |---|---|
    | CORPORATE | Tecnosul 686,22, Agrovale 639,44, Farmavida 25% |
    | WEEKEND_PACKAGE | 520 / 820 / 1200, inclusive em julho; devolução de 410,00 |
    | LONG_STAY | 1574,37 e 1754,30 com temporada; recusa com 6 noites |
    | código ignorado no STANDARD | ok |
    | código inválido ou ausente | 400 |

- **Falha:**
  - O corpo que não é JSON dá 500 em vez de 400.
  - `month=2027-13` devolve 200 com `total,0` em vez de 400, e `2027-7` é aceito.
  - `companyCode` numérico dá 500.
  - O dinheiro sai sem 2 casas em vários pontos: `"amountPaid":0` e `"discount":0` na criação, `"refund":0` ao
    cancelar uma pendente, e `total,0` no CSV vazio.
  - O e-mail traz `R$` seguido de espaço não separável, então não bate com o texto pedido ao pé da letra.
  - Duas reservas simultâneas para o mesmo quarto e as mesmas noites foram ambas criadas em 6 de 9 rodadas.

**Para o dono da pousada:** as contas dos planos estão certas, mas o sistema, do jeito que foi entregue:
- não aceita reserva com o CPF escrito com pontos e traço (a resposta é um erro interno);
- não aceita o número do quarto mandado como número;
- pode vender o mesmo quarto duas vezes para a mesma noite quando dois pedidos chegam juntos.

## Dúvidas

- **O jar e o script.** O `mvn package` gera um jar "fino" (`pom.xml`: `addClasspath` com `lib/`) e copia as
  dependências para `target/lib`. `java -jar target/pousada-mare-alta-1.0.0.jar` sobe normalmente, então considerei
  atendido o "jar que sobe sozinho". Os scripts da skill pegam o maior jar com `Main-Class`, que é o driver
  PostgreSQL. Os números de Banco e Comportamento vêm de uma cópia dos scripts (em `/tmp`) que só exclui
  `target/lib`. As saídas originais estão guardadas em `measures/*-script-jar-errado.md`.
- **O tipo de `roomNumber`.** O texto não diz se é texto ou número. Exigir texto e recusar número com 400 pode ser
  defendido; pesou pouco na nota. O que pesou foi o 500 com o CPF formatado e a reserva duplicada.
- **A nota dos Testes, entre `com problemas` e `ruim`.** Os 8 testes conferem com valores exatamente as contas novas
  do cenário. Escolhi `ruim` porque todo o resto das regras pedidas (situações, cancelamento chamado pela reserva,
  relatórios, erros, banco) está sem nenhum teste, e a cobertura é de 8,8%.
