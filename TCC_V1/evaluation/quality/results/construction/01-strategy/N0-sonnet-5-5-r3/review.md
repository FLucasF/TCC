# Revisão: construção, cenário 01

## Visão geral

O projeto tem dois pacotes: `com.marealta` (17 classes, 820 linhas), onde moram juntas a borda HTTP (`App`), as regras
(`ReservationService`, `Seasons`, `Money`, `Receipt`, `Reports`), o acesso ao banco (`Db`, `Store`) e os tipos do
domínio; e `com.marealta.plan` (10 classes, 175 linhas), com os planos de tarifa. Uma requisição entra numa rota de
`App.java:25-35`. A rota lê e valida o corpo (`App.java:146-170`) e chama um método de `ReservationService`. O serviço
abre uma transação (`Db.java:33-47`), pede o preço ao plano (`ReservationService.java:35-40`) e grava pelos métodos
estáticos de `Store`, que usam SQL com parâmetros. Os dados ficam no PostgreSQL, em três tabelas criadas por
`CREATE TABLE IF NOT EXISTS` em `Db.java:55-92`. O pedido do cenário (cinco planos com preço, regra de uso e devolução
próprios) foi resolvido como um Strategy limpo: uma interface, uma classe por plano e um mapa. As 72 conferências do
comportamento passaram. Os pontos fracos são estes:
- os testes cobrem só o pacote dos planos (21,9% das linhas no total);
- o esquema é criado no código, sem migração e sem CHECK;
- há um ciclo entre os dois pacotes, porque os planos lançam a `ApiException`, que carrega o status HTTP.

## Arquitetura

**Números:** 2 pacotes (`com.marealta` 17 classes/820 linhas, `com.marealta.plan` 10 classes/175 linhas); 2 dependências entre eles (`com.marealta -> com.marealta.plan` e `com.marealta.plan -> com.marealta`); 1 ciclo, entre esses dois pacotes. Só `com.marealta` usa Javalin, JDBC, Jackson e o pool. Maiores classes: `App` 187, `Store` 157, `ReservationService` 149 linhas.
**Nota:** com problemas

As camadas existem como classes e a borda é fina:
- **As rotas.** Em `App.java:25-35`, cada rota tem uma linha: lê, chama o serviço e escreve a resposta. A conversão
  do JSON fica em `App.java:146-186`, e o mapa de erros em `App.java:37-44`.
- **O serviço.** `ReservationService` não conhece o Javalin. Ele também não monta SQL: só repassa a `Connection`
  para `Store` (`ReservationService.java:38,45,49`).
- **O SQL.** Está todo em `Store.java` e `Db.java`.
- **A montagem.** É feita num lugar só, em `Main.java:10-13` (Db → ReservationService → App).

O que pesa:
- **Um ciclo novo.** `com.marealta.plan` depende de volta do pacote raiz por `Room`, `RoomType`, `Money`, `Seasons` e,
  principalmente, `ApiException` (`CorporatePlan.java:3,17`, `LongStayPlan.java:12`, `WeekendPackagePlan.java:21`). A
  `ApiException` carrega o status HTTP (`ApiException.java:5,8`), então a regra do plano decide o código da resposta
  HTTP. É o ciclo que a skill descreve como "um ciclo novo entre dois pacotes".
- **Um pacote raiz que mistura tudo.** HTTP, regras, SQL e domínio estão em `com.marealta`, sem separação entre eles.
  É por isso que o ciclo aparece.
- **`Store` é estático.** O serviço chama `Store.*` direto (`ReservationService.java:27,38,45,49,64`), em vez de
  receber um objeto de acesso a dados. Não há estado global (a conexão vem de fora), mas o serviço fica preso a essa
  implementação.

Num sistema deste tamanho isso é aceitável, mas são defeitos localizados que alguém deveria corrigir.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (a interface `RatePlan` e uma classe por plano, escolhida pelo mapa de `RatePlans`). Fora dela fica só uma regra pequena, guardar o código da empresa (`ReservationService.java:48`). O preço, a validação e a devolução não têm nenhum `switch` nem `if` sobre o plano.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `RatePlan` (`plan/RatePlan.java:9-18`) define `code()`, `quote(BookingTerms)` e
`refund(BigDecimal)`. Cada plano fica numa classe e é escolhido num único ponto, o mapa de `plan/RatePlans.java:10-17`:
- `StandardPlan`;
- `NonRefundablePlan`: 15% de desconto e devolução zero;
- `CorporatePlan`: o mapa das empresas e a recusa sem código ou com código desconhecido;
- `LongStayPlan`: o mínimo de 7 noites;
- `WeekendPackagePlan`: a regra da sexta ao domingo, o preço fechado por tipo e a devolução de metade.

Os dois planos que o texto já trazia também estão na estrutura. O serviço pede o preço ao plano
(`ReservationService.java:40`) e a devolução também (`ReservationService.java:81`), sem nomear nenhum plano. A
classe abstrata `PerNightPlan` (`plan/PerNightPlan.java:8-26`) reúne o cálculo por diária com temporada, que quatro
planos usam, e o pacote a dispensa (`WeekendPackagePlan.java:11`). Assim, um plano novo é uma classe nova mais uma
linha no registro.

O que afasta um pouco é o `"CORPORATE".equals(plan.code())` em `ReservationService.java:48`. É exatamente a "regra de
plano isolada, pequena", que a referência manda contar nas dúvidas, e não muda a classe.

**O exagero.** Não há. `BookingTerms` e `Quote` são records com uso real. `Db.Work` (`Db.java:11-14`) existe porque o
lambda lança `SQLException`. Não há fábrica, builder nem camada que só repassa.

**O resto do código.** É curto e legível. O dinheiro é `BigDecimal` com `HALF_EVEN` (`Money.java:23-25`), as datas
são `LocalDate`, e há enums para a situação e o tipo de quarto. Defeitos localizados que um revisor exigente apontaria:
- **Percentuais como texto.** Ficam em `String` ("0.12", "0.15"; `CorporatePlan.java:7-10`,
  `PerNightPlan.java:10-12`, `Money.java:32-34`) e viram `BigDecimal` a cada uso. Um erro de digitação só aparece em
  tempo de execução.
- **O plano guardado como texto.** A reserva guarda o plano como `String` (`Reservation.java:8`), e o cancelamento
  resolve de novo pelo nome, com um `IllegalStateException` se não achar (`plan/RatePlans.java:25-27`).
- **O código HTTP na regra.** Os planos lançam a exceção com status HTTP (veja Arquitetura). A regra de negócio
  decide o 400.
- **Uma cópia campo a campo.** `withState` copia os 17 campos para mudar três (`ReservationService.java:143-148`).
- **Os dados dos quartos em dois lugares.** A capacidade e a diária estão no `enum` `RoomType`
  (`RoomType.java:6-8`) e na tabela `rooms`, que o código semeia a partir do `enum` (`Db.java:87-92`).
- **Uma busca que traz tudo.** `receipt()` carrega todos os quartos para achar um (`ReservationService.java:139`).

## Testes

**Números:** 8 testes rodaram, 0 falhas, 0 erros, 0 pulados. 8 métodos escritos, num arquivo só (`PricingTest`). Cobertura: 93 de 424 linhas (21,9%) e 30 de 128 ramos (23,4%). Por pacote: `com.marealta.plan` 48/49 linhas e 15/16 ramos, `com.marealta` 45/375 linhas e 15/112 ramos. Nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que os testes conferem bem, com valores:
- a parte nova do cenário, pelos exemplos do texto: Tecnosul 779,80 / 93,58 / 686,22 (`PricingTest.java:36-41`), o
  pacote deluxe 820,00 com devolução de 410,00 (`PricingTest.java:48-52`) e a estadia longa 1.749,30 / 174,93 /
  1.574,37 (`PricingTest.java:57-61`);
- as recusas: convênio sem código ou com código desconhecido (`:43-46`), pacote fora de sexta a domingo (`:53-54`) e
  estadia longa com 6 noites (`:62`);
- a temporada e as datas de borda (`:23-34`);
- o texto do comprovante e o formato do dinheiro (`:72-83`).

A pergunta da mutação:
- **Trocar 0.12 por 0.21 ou `<` por `<=` no mínimo de noites:** os testes pegam.
- **Mexer nos 18% da Agrovale ou nos 25% da Farmavida:** nenhum teste pega, porque só a Tecnosul é testada.
- **Mexer no preço do pacote standard ou da suíte:** nenhum teste pega.
- **Mexer na devolução integral do convênio e da estadia longa, ou do plano normal:** nenhum teste pega, porque só o
  pacote e o não reembolsável são testados (`:52,69`).
- **A estadia longa com noites de alta temporada:** fica sem teste.

Fora do pacote dos planos não há teste, e ficam sem nenhum teste:
- as transições de situação e as recusas 409 (`ReservationService.java:59-116`);
- o cancelamento pelo serviço;
- o conflito de noites;
- a capacidade;
- os dois relatórios CSV (`Reports.java`);
- o e-mail;
- toda a borda HTTP com os códigos de erro (`App.java`).

O texto não pedia testes. O que existe é bom e preciso, mas deixa sem rede a maior parte das regras do sistema e
parte da regra nova.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro é todo `numeric(12,2)` e as datas são `date`. Há 1 chave estrangeira (`reservations.room_number`), com índice. Nenhuma data em texto. Nenhuma restrição CHECK/UNIQUE/EXCLUDE: `status`, `rate_plan`, `type` e `channel` são `text` sem CHECK. Esquema criado por `CREATE TABLE` no Java, sem migrações. Há 1 transação explícita e 2 `FOR UPDATE`.
**Nota:** com problemas

O que está certo:
- **Os tipos e as chaves.** Os tipos estão certos e há `NOT NULL` no que é obrigatório (`Db.java:55-92`). A
  reserva é ligada ao quarto por chave estrangeira, com índice (`Db.java:63,79`).
- **O SQL.** Todo o SQL com dado de fora usa `?`. Os quatro candidatos a SQL montado com `+` que o script listou
  montam só constantes: `RES_COLS` e os `where`/`order` fixos em `Store.java:94,126`, e os números do `enum` na
  semente de `Db.java:90`.
- **As conexões e as transações.** As conexões vêm de um pool Hikari (`Db.java:22-30`) e são devolvidas com
  try-with-resources. Cada operação roda numa transação com rollback (`Db.java:33-47`).
- **As reservas no mesmo quarto.** O `SELECT ... FOR UPDATE` na linha do quarto (`Store.java:37`) serializa as
  reservas concorrentes do mesmo quarto no próprio banco, não só dentro do processo. O roteiro confirmou isso nas 10
  rodadas simultâneas.
- **A criação ao subir.** O `migrate()` usa um advisory lock (`Db.java:53`) para que dois processos não criem o
  esquema ao mesmo tempo.

Os defeitos:
- **O esquema não evolui.** É criado com `CREATE TABLE IF NOT EXISTS` no código (`Db.java:55,61,81`), sem migração
  versionada. O próximo pedido que precisar de uma coluna nova não tem como acrescentá-la num banco que já roda.
- **O banco não protege nenhuma regra.** Não há CHECK em `status`/`rate_plan`/`type`, nem
  `check_out > check_in`, nem `guests >= 1`. Também não há restrição contra noites sobrepostas: a regra depende só da
  trava no código (`Store.java:37,47`).

Nada perde ou corrompe dado.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. A parte comum passou em 46 de 46, os dados depois de reiniciar em 3 de 3, o cenário 01 em 21 de 21 e as reservas simultâneas em 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API:
- **A parte comum:** os quartos, o preço com a temporada e o meio para o par, as recusas com os códigos certos, a
  confirmação com o e-mail e o comprovante, o check-in e o check-out, as devoluções e os dois relatórios em CSV.
- **Os três planos novos,** com os valores do financeiro:
  - Tecnosul R$ 686,22, Agrovale R$ 639,44, Farmavida R$ 584,85;
  - o pacote a R$ 520,00, R$ 820,00 e R$ 1.200,00, inclusive em julho, com a devolução de R$ 410,00;
  - a estadia longa a R$ 1.574,37, com a temporada valendo, e a recusa com 6 noites.
- **O `companyCode` nos outros planos:** é ignorado, como o texto pede.
- **Depois de reiniciar:** os dados continuam lá.
- **Duas reservas ao mesmo tempo:** nunca geram duas reservas no mesmo quarto.

Fora do roteiro, uma escolha que o texto não fechou: as regras do plano são conferidas antes da capacidade e do
conflito de noites (`ReservationService.java:40-47`). Um pacote com data errada e hóspedes demais responde 400, e não
422. O texto não define essa ordem, então isso não pesa.

## Dúvidas

- **A nota do design de baixo nível** ficou entre `bom` e `com problemas`. Os defeitos apontados são todos pequenos e
  localizados: os percentuais em `String`, o plano guardado como `String`, a exceção com status HTTP lançada pelos
  planos e a cópia de 17 campos em `withState`. Nenhum deles torna o código difícil de mudar. Dei `com problemas`
  porque um revisor exigente pediria pelo menos os percentuais em `BigDecimal` e o plano tipado.
- **A regra fora do plano.** `ReservationService.java:48` (`"CORPORATE".equals(plan.code())`, para guardar o código
  da empresa só no convênio) é uma regra de plano fora da estrutura. A referência diz que ela, sozinha, não muda a
  classe. Por isso fica `acerto`.
