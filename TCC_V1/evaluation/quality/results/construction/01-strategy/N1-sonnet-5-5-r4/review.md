# Revisão: construção, cenário 01

## Visão geral

O projeto tem dois pacotes. `br.com.marealta` tem 21 classes: a borda HTTP (`Routes`), os serviços
(`ReservationService`, `ReportService`), o acesso ao banco (`ReservationRepository`, `MessageRepository`, `Database`) e
o domínio (`Reservation`, `Stay`, `Money`, `Season`, `Receipt`, os quartos). `br.com.marealta.plan` tem os planos de
tarifa. Uma requisição entra numa lambda de `Routes` (`Routes.java:29-41`). O corpo da criação é validado em
`NewReservation.parse` (`NewReservation.java:23-56`), que já resolve o plano pelo registro `RatePlans`. O serviço pede
o orçamento ao plano (`ReservationService.java:18`) e grava numa transação com trava por quarto
(`ReservationService.java:22-28`). Confirmar, cancelar, fazer check-in e check-out são métodos imutáveis do record
`Reservation` (`Reservation.java:49-68`), aplicados sobre uma linha travada com `FOR UPDATE`. Os dados ficam no
PostgreSQL, em duas tabelas que o próprio código cria (`Database.java:28-61`). Os quartos são constantes em código
(`Rooms.java:7-12`). Os cinco planos ficam numa hierarquia sob `RatePlan`, e o roteiro de comportamento passou inteiro
(72 de 72). Não há defeito sério. Os pontos fracos são um ciclo entre os dois pacotes num pacote principal sem
camadas, o esquema criado com `CREATE TABLE IF NOT EXISTS` sem migração nem restrições, e testes que deixam parte das
regras novas sem conferir.

## Arquitetura

**Números:** 2 pacotes (`br.com.marealta`, 21 classes e 923 linhas; `br.com.marealta.plan`, 8 classes e 203 linhas),
2 dependências entre eles e 1 ciclo (`br.com.marealta` ↔ `br.com.marealta.plan`). Javalin, JDBC, Jackson e o pool
estão todos no pacote principal. A maior classe é `ReservationRepository`, com 131 linhas.
**Nota:** com problemas

O que está bem:
- **A borda HTTP é fina.** Cada rota lê o parâmetro, chama o serviço e escreve a resposta, e os erros são traduzidos
  num ponto só (`Routes.java:28-48`).
- **Os serviços não conhecem o Javalin, e o SQL fica nos repositórios.** O serviço não tem SQL
  (`ReservationService.java:16-90`), e todo o SQL está em `ReservationRepository.java` e `MessageRepository.java`.
- **A montagem fica no `main`** (`App.java:9-12`).
- **Nenhuma classe domina o resto.** A maior tem 131 linhas.

Os defeitos:
- **Há um ciclo entre os dois pacotes.** O pacote `plan` depende do pacote principal: `ApiError`, `Money` e `Stay` em
  `CorporatePlan.java:3-5`, e `RoomType` em `WeekendPackagePlan.java:5`. O pacote principal depende de `plan` em
  `NewReservation.java:3-4`, `Reservation.java:3-4` e `ReservationService.java:3`. Os planos usam tipos do domínio
  (`Stay`, `Money`) que moram no mesmo pacote das rotas e do JDBC.
- **As camadas são só nomes de classe.** Rotas, serviços, repositórios e domínio dividem um único pacote, e o
  domínio conhece a borda:
  - o record `Reservation` carrega anotações do Jackson (`Reservation.java:5-6,28,33`);
  - `NewReservation` recebe um `ObjectMapper` e faz o parse do JSON (`NewReservation.java:23-29`);
  - as regras lançam `ApiError`, que carrega o status HTTP (`ApiError.java:22-43`), a partir do domínio e dos planos
    (`Reservation.java:72,82`, `CorporatePlan.java:23`).
- **Os serviços criam os repositórios por dentro** em vez de recebê-los na montagem (`ReservationService.java:9-10`,
  `ReportService.java:13`).

São misturas localizadas num sistema pequeno, e nenhuma regra mora no handler. O ciclo novo entre os dois pacotes é o
que a skill classifica como `com problemas`.

## Design de baixo nível

**Números:**
- Os 5 planos estão na estrutura e nenhum fica fora. Cada um é uma subclasse de `RatePlan`, com o seu preço, a sua
  validação e a sua devolução.
- Não há nenhum `switch` nem `if` sobre o plano fora das classes de plano.
- A escolha do plano acontece num único ponto, `RatePlans.find`, chamado em `NewReservation.java:43` e
  `Reservation.java:35`.

**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern pedido (Strategy).** `RatePlan` é uma classe abstrata (`RatePlan.java:11-44`):
- `quote` é `final` e roda validar, somar e descontar (`RatePlan.java:22-26`);
- os ganchos `validate`, `subtotal` e `discount` e o método `refund` têm padrão neutro (`RatePlan.java:29-43`).

Cada plano tem a sua classe, e os dois que já existiam também:
- `StandardPlan.java:4-8`;
- `NonRefundablePlan.java:14-22`: 15% de desconto, devolução zero;
- `CorporatePlan.java:11-30`: o mapa de empresas, a validação do código e o desconto;
- `WeekendPackagePlan.java:13-38`: sexta mais duas noites, o preço fechado por tipo de quarto e a devolução de metade;
- `LongStayPlan.java:9-26`: o mínimo de 7 noites e 10% de desconto.

O registro `RatePlans.java:8-13` é o único lugar que lista os planos. Os pontos que usam o plano só pedem a ele:
- o orçamento, em `ReservationService.java:18`;
- a devolução, em `Reservation.java:66`, cuja condição é sobre a situação da reserva, não sobre o plano.

Nenhum ponto pergunta qual é o plano. Um plano novo é uma classe nova mais uma linha no registro. A referência aceita
uma classe abstrata com uma implementação por plano, escolhida num único ponto. O `companyCode` só é lido pelo
convênio (`CorporatePlan.java:22,29`), e os outros planos o ignoram sem `if`.

**O exagero: não há.** Cada estrutura tem uso:
- `Quote` é um record de dois valores com o total derivado (`Quote.java:5-9`);
- `Stay` é o que o plano precisa saber da estadia (`Stay.java:8`);
- `Database.Work` é a interface funcional que deixa passar o trabalho para `read` e `inTransaction`
  (`Database.java:11-14`).

Não há fábricas, builders, interfaces com uma única implementação nem camadas que só repassam.

**O resto do código** está limpo:
- **Os tipos são os certos.** O dinheiro é `BigDecimal`, arredondado meio para o par num lugar só
  (`Money.java:16-27`), e as datas são `LocalDate`. A situação da reserva é um `enum`, `ReservationStatus.java:3-10`,
  com a regra de "paga" nele.
- **O estado não muda escondido.** As transições são imutáveis e conferem a situação de partida
  (`Reservation.java:49-88`).
- **As validações de formato ficam num lugar só** (`NewReservation.java:58-80`).

Detalhes que um revisor citaria, sem pedir mudança antes de aceitar:
- o número de noites é calculado em dois lugares (`Reservation.java:29-31` e `Stay.java:10-12`);
- `Reservation.plan()` e `room()` usam `orElseThrow()` sem mensagem (`Reservation.java:35,40`), e um código de plano
  desconhecido vindo do banco viraria um 500 genérico;
- o id chega ao serviço como `String`, e o serviço o converte (`ReservationService.java:84-90`).

## Testes

**Números:** 11 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 11 métodos de teste escritos, em 2 arquivos: 8
de unidade em `PricingTest` e 3 pela API contra o PostgreSQL em `ApiTest`. A cobertura de linhas é de 92,2% (367 de
398), e a de ramos, de 69,8% (67 de 96). O pacote `plan` tem 46 de 48 linhas e 10 de 10 ramos cobertos.
**Nota:** com problemas

Os testes conferem valores, não só o status.

As regras de preço têm os números do texto:
- a alta temporada dá 599,76 (`PricingTest.java:19-22`);
- o NON_REFUNDABLE tem desconto de 74,97 (`PricingTest.java:25-29`);
- a Tecnosul dá 779,80, 93,58 e 686,22 (`PricingTest.java:32-37`);
- o pacote sai 820,00, devolve 410,00, e a suíte em julho sai 1.200,00 (`PricingTest.java:46-51`);
- a estadia longa dá 1.749,30, 174,93 e 1.574,37 (`PricingTest.java:60-66`).

As recusas dos planos novos também estão lá (`PricingTest.java:40-43,54-57,65`), e o texto do comprovante é conferido
inteiro (`PricingTest.java:69-81`).

`ApiTest` percorre o fluxo inteiro num schema descartável do banco real (`ApiTest.java:116-128`):
- confirmação, segunda via, e-mail, ocupação, check-in, check-out e financeiro (`ApiTest.java:166-201`);
- a devolução do pacote (`ApiTest.java:204-213`);
- os códigos de erro (`ApiTest.java:216-229`).

Pela pergunta da mutação, parte das regras ficaria sem rede:
- **As taxas da Agrovale (0.18) e da Farmavida (0.25)** podem ser trocadas sem nenhum teste falhar. Só a Tecnosul é
  testada (`PricingTest.java:33`, `ApiTest.java:167`).
- **A devolução integral do CORPORATE e do LONG_STAY** não é conferida em nenhum teste. Trocar o `return paid` de
  `RatePlan.java:30` não seria pego, porque o único teste de devolução é o do pacote.
- **A devolução zero do NON_REFUNDABLE** (`NonRefundablePlan.java:20-22`) e **a devolução zero de uma reserva
  pendente** (`Reservation.java:66`) não são conferidas.
- **A estadia longa com noites de alta temporada** não é testada.
- **No financeiro, a regra de que a NON_REFUNDABLE cancelada continua contando** não é testada.
- **O corpo do e-mail** não é conferido, só o assunto e o canal (`ApiTest.java:185-186`).

A iniciativa é boa e o que existe está certo. Mas parte das regras novas e das devoluções ficou sem conferência, e
70% de ramos confirma isso.

## Banco

**Números:** 2 tabelas, `reservations` (15 colunas, 14 `NOT NULL`) e `messages` (5 colunas). Ambas têm chave primária.
Os valores em dinheiro estão todos em `numeric(12,2)`, e as datas, em `date`. O script acusou:
- 0 chaves estrangeiras;
- 0 CHECK, UNIQUE ou EXCLUDE;
- `status`, `rate_plan` e `channel` em `text` sem CHECK;
- o esquema criado por `CREATE TABLE` no código, sem arquivo de migração.

O código usa um pool (Hikari), uma transação explícita e `FOR UPDATE`. Os 4 candidatos a SQL montado com `+`
concatenam só constantes.
**Nota:** com problemas

O que está certo:
- **Os tipos.** O dinheiro está em `numeric(12,2)` e as datas em `date` (`Database.java:39-48`). O `refund` é o único
  nulo, de propósito, porque o texto pede `null` antes do cancelamento.
- **O SQL tem parâmetros.** A concatenação só junta constantes: `COLUMNS` e as cláusulas fixas
  (`ReservationRepository.java:16-17,67,88-89,94-95,99`).
- **As conexões são devolvidas.** O acesso passa por try-with-resources sobre o pool (`Database.java:63-85`).
- **A duplicação de reservas é protegida no banco, não só no processo.** Uma trava `pg_advisory_xact_lock` pelo
  número do quarto, dentro da transação, serializa as reservas do mesmo quarto antes de procurar conflito
  (`ReservationRepository.java:20-25`, `ReservationService.java:22-28`). Ela vale entre processos, e o roteiro
  simultâneo passou (10 de 10).
- **As transições também ficam protegidas.** Elas travam a linha com `FOR UPDATE` (`ReservationRepository.java:67`,
  `ReservationService.java:80-82`).
- **A confirmação e o e-mail andam juntos.** Os dois são gravados na mesma transação (`ReservationService.java:37-43`).

Os defeitos:
- **O esquema não tem como evoluir.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no construtor de `Database`
  (`Database.java:28-61`). Uma coluna nova num banco que já roda não seria acrescentada. Por exemplo, guardar o
  `companyCode`, que hoje não é persistido (`ReservationRepository.java:43-44`).
- **Não há restrições.** Nenhuma regra do domínio está no esquema:
  - nenhum CHECK em `status`, `rate_plan`, `guests > 0` ou `check_out > check_in`;
  - nenhuma restrição de exclusão para as noites do quarto.

  Os quartos só existem em código (`Rooms.java:7-12`), então `room_number` não tem chave estrangeira. É uma escolha
  defensável, porque "os quartos quase nunca mudam", mas o banco aceita uma reserva para um quarto inexistente.

## Comportamento

**Números:** 72 de 72 conferências passaram, nenhuma falhou:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- duas reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o texto descreve funciona pela API:
- **Os valores do financeiro conferem:** R$ 686,22, R$ 820,00 e a devolução de R$ 410,00, R$ 1.574,37.
- **As outras empresas do convênio** dão os valores certos: Agrovale 639,44, Farmavida 584,85.
- **As recusas dos planos dão 400 INVALID_REQUEST:** convênio sem código ou com código desconhecido, pacote fora de
  sexta a domingo e estadia longa com menos de 7 noites.
- **O pacote ignora a temporada.** Em julho ele continua R$ 820,00.
- **A estadia longa aplica a temporada.** De 28/06 a 05/07, dá 1.754,30.
- **As devoluções seguem cada plano:** o convênio e a estadia longa devolvem tudo, e o pacote, metade.
- **O `companyCode` é ignorado fora do convênio.**
- **O normal e o não reembolsável** continuam como o texto diz.

Para o dono da pousada, isto quer dizer que:
- os preços, os cancelamentos e os relatórios saem como ele descreveu;
- os dados sobrevivem a um reinício;
- duas reservas do mesmo quarto feitas ao mesmo tempo nunca são aceitas juntas.

## Dúvidas

- Os quartos ficam só em código, sem tabela nem chave estrangeira (`Rooms.java:7-12`). O texto diz que "os quartos
  quase nunca mudam", e por isso pesei pouco. Um revisor mais rígido poderia pedir a tabela de quartos com a chave
  estrangeira.
- Na nota dos testes, a fronteira entre `bom` e `com problemas` é estreita. Os testes conferem com valores todos os
  exemplos do texto. Dei `com problemas` porque duas das três taxas do convênio e as devoluções do CORPORATE, do
  LONG_STAY e do NON_REFUNDABLE ficaram sem teste.
