# Revisão: construção, cenário 01

## Visão geral

O projeto é um serviço Javalin com a borda HTTP em `api` (controllers, DTOs, `ErrorHandler`), as regras em
`domain.service` e `domain.rateplan` (uma interface `RatePlan` com uma classe por plano), o acesso ao banco em
`domain.repository` (JDBC puro, apesar do nome "domain") e o pool e o esquema em `infrastructure.database`.
Uma requisição entra no controller, que pega uma conexão do pool e, **a cada chamada**, monta à mão os repositórios e
o `ReservationService` (`api/controller/ReservationController.java:177-183`). O serviço valida, pede ao plano o preço
e grava. Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código. O problema
mais sério: **nenhuma reserva pode ser criada com o CPF escrito como no texto do dono** ("111.222.333-44"). A coluna
`guest_cpf` é `VARCHAR(11)` (`infrastructure/database/DatabaseInitializer.java:33`), não há validação, e o erro do banco
volta como 500 (`ReservationController.java:61`). Por isso 52 das 72 conferências do roteiro falharam. Com CPF só de
dígitos os valores dos cinco planos saem certos, mas sobram três defeitos: nada impede duas reservas do mesmo quarto
na mesma noite (sem restrição, sem trava e sem transação), a segunda via sai para reserva cancelada, e os testes são
quase só cinco contas de preço.

## Arquitetura

**Números:** 10 pacotes, 23 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O pacote
`api.controller` depende de 7 pacotes, inclusive `infrastructure.database` e `domain.repository`, e usa JDBC.
**Nota:** com problemas

O que está bem: as regras (`domain.service`, `domain.rateplan`) não importam Javalin nem JDBC. O SQL fica todo nos
três repositórios. O `ErrorHandler` traduz as exceções de domínio em códigos HTTP num lugar só
(`api/ErrorHandler.java:10-55`). Não há ciclo.

O que precisa mudar:
- **A montagem não fica num lugar só.** O `Main` cria os controllers passando o `DatabaseConfig`
  (`Main.java:34-37`), e são os controllers que criam repositórios e serviços a cada requisição:
  `ReservationController.java:177-183`, `ReportController.java:40-42` e `:64-66`, `RoomController.java:22-23`,
  `MessageController.java:22`. A borda HTTP conhece `java.sql.Connection` e controla a vida da conexão.
- **Há regra na borda.** A segunda via decide quem tem direito olhando `amountPaid` dentro do controller
  (`ReservationController.java:164-166`). A regra está errada, mas pesa no Comportamento. O check-out vai buscar o
  quarto direto num repositório (`ReservationController.java:133-135`) em vez de pedir ao serviço.
- `domain.repository` tem JDBC dentro de um pacote que se chama domínio. Isso não chega a ser um problema de
  dependência, mas confunde.

Um sistema deste tamanho cresce sem dor com essa estrutura. Mesmo assim, a montagem espalhada pelos controllers e a
conexão JDBC dentro da camada HTTP são misturas que um revisor pediria para corrigir.

## Design de baixo nível

**Números:** os 5 planos estão na estrutura e nenhum fica fora dela. São 5 implementações de `RatePlan`, cada uma com
`validate`, `calculatePrice` e `calculateRefund`, e um único ponto de seleção (o mapa de `RatePlanFactory`). Não há
nenhum `switch` nem `if` sobre o plano no serviço. O laço que soma as noites aparece copiado em 4 planos, e há
7 `catch (Exception)` iguais num só controller.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É o Strategy como a referência pede. A interface `domain/rateplan/RatePlan.java:9-12` tem o preço, a
validação e a devolução. Há uma classe por plano, incluindo os dois que já existiam (`StandardRatePlan`,
`NonRefundableRatePlan`), e a escolha acontece só em `RatePlanFactory.java:8-14` (um `Map` do enum para a
implementação). O serviço pede tudo ao plano sem nomeá-lo: a validação em `ReservationService.java:37-38`, o preço em
`:49` e a devolução no cancelamento em `:139-140`. As regras de quem pode usar cada plano também ficaram dentro dele:
o código da empresa em `CorporateRatePlan.java:21-29`, sexta a domingo e 2 noites em
`WeekendPackageRatePlan.java:22-36`, 7 noites em `LongStayRatePlan.java:18-24`. Um plano novo é uma classe e uma
linha no mapa. O que afasta um pouco: `ReservationService.java:60` grava o `companyCode` em qualquer plano. É uma
regra pequena fora do plano e não muda a classe (ver Dúvidas).

**O exagero.** Não há. O `RatePlanFactory` é o ponto de seleção que o pattern precisa. `PriceCalculation` e o record
`RatePlan.ReservationRequest` são simples, e não há interfaces de repositório nem camadas que só repassam.

**O resto do código.**
- **Duplicação:** o laço noite a noite com a temporada aparece copiado em `StandardRatePlan.java:19-31`,
  `NonRefundableRatePlan.java:20-32`, `CorporateRatePlan.java:33-45` e `LongStayRatePlan.java:28-40`. Uma mudança
  na temporada ou no arredondamento das noites tem de ser feita em quatro lugares. Um método comum ou uma classe base
  para "somar as diárias" resolveria.
- **Os erros:** cada handler de `ReservationController` repete o mesmo `try/catch` que relança uma lista de
  exceções de domínio e embrulha o resto em `RuntimeException` (`ReservationController.java:58-63`, `:96-100`,
  `:109-113`, `:122-126`, `:137-141`, `:150-154`, `:170-174`). Isso transforma qualquer erro inesperado (o do CPF,
  por exemplo) num 500 genérico, e o padrão se repete em `ReportController.java:45`, `RoomController.java:25` e
  `MessageController.java:25`.
- **Dinheiro:** o tipo é certo (`BigDecimal`, `HALF_EVEN`), mas a escala não é sempre 2. `BigDecimal.ZERO` em
  `StandardRatePlan.java:32`, `WeekendPackageRatePlan.java:41` e `Reservation.java:27` sai no JSON como `0`, e não
  `0.00`, na criação.
- **Validação:** o CPF não passa por nenhuma validação nem normalização. O controller só confere se o campo veio
  (`ReservationController.java:76-78`), e `DocumentService.formatCpf` assume 11 dígitos (`DocumentService.java:52-57`).
- **O modelo:** `Reservation` é um saco de getters e setters (`domain/model/Reservation.java`), e as transições ficam
  no serviço. Isso é aceitável aqui.

## Testes

**Números:** 5 testes rodaram, 0 falhas, 0 erros e 0 pulados; 5 métodos escritos, num único arquivo. A cobertura é
de 9,1% das linhas (83 de 912) e de 8,3% dos ramos (12 de 144). `domain.service`, `domain.repository` e
`api.controller` têm 0% (`measures/tests.md`). Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

Os cinco testes de `src/test/java/br/com/pousadamarealta/RatePlanTest.java` conferem com valores só o preço. São o
STANDARD em março, os R$ 686,22 da Tecnosul, os R$ 820,00 do pacote e os R$ 1.574,37 da estadia longa
(`RatePlanTest.java:31-33`, `:49-51`, `:83-84`, `:100-102`), mais a recusa do convênio sem código (`:66-67`). Fica
sem rede quase tudo o que o texto pede:
- a alta temporada (nenhum teste usa datas de temporada; trocar `1.20` por `1.02` em `SeasonCalculator.java:10` não
  derruba nada);
- Agrovale e Farmavida (trocar `0.18` por `0.81` em `CorporateRatePlan.java:16` passa);
- todas as devoluções: a metade do pacote (`WeekendPackageRatePlan.java:47-51` poderia devolver tudo), o zero do não
  reembolsável e o total dos outros;
- as recusas do pacote fora de sexta a domingo e da estadia longa com menos de 7 noites (trocar `MINIMUM_NIGHTS = 7` por `6` em
  `LongStayRatePlan.java:15` passa);
- o NON_REFUNDABLE e seu arredondamento (37,48);
- as situações da reserva, os códigos de erro, o comprovante e o e-mail, os relatórios e o banco.

O defeito que impede criar reservas com CPF formatado nunca seria pego por estes testes, porque nenhum deles passa
pelo serviço, pela API ou pelo banco. Os testes que existem são bons, com asserções de valor, mas são cinco contas
numa cobertura de 9%. Isso é "quase não há testes" para um sistema com ciclo de vida, cancelamentos e relatórios.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em
`numeric(10,2)` e as datas em `date`. O script acusou uma chave estrangeira sem índice
(`reservations.room_number`), nenhum CHECK sobre situação, plano, tipo ou canal, nenhuma restrição UNIQUE/EXCLUDE,
0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`). O SQL é todo parametrizado e as conexões vêm de
um pool HikariCP com try-with-resources.
**Nota:** ruim

- **Reserva dupla.** A regra "um quarto não pode ter duas reservas na mesma noite" é só um `SELECT` antes do
  `INSERT`, em autocommit (`ReservationService.java:40-47` e `:66`; `DatabaseConfig.java:19`), sem restrição no
  esquema (`DatabaseInitializer.java:27-47`), sem trava e sem transação. Rodei o serviço com CPF só de dígitos e
  mandei 15 pares de pedidos simultâneos para o mesmo quarto e as mesmas noites: **14 pares gravaram as duas
  reservas**. É dado errado: o mesmo quarto vendido duas vezes.
- **O CPF não cabe na coluna.** `guest_cpf VARCHAR(11)` (`DatabaseInitializer.java:33`) recusa o CPF no formato que
  o próprio texto usa ("111.222.333-44"), e o `INSERT` falha com "value too long for type character varying(11)"
  (`measures/behavior-service.log`). É o defeito que derruba o comportamento.
- **Duas escritas sem transação.** A confirmação grava a reserva (`ReservationService.java:93`) e depois a mensagem
  (`:97-101`), cada uma no seu autocommit. Se a segunda falhar, a reserva fica paga e sem o e-mail registrado.
- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` no código
  (`DatabaseInitializer.java:18-58`), sem migrações, e o próximo pedido que acrescentar uma coluna não terá como
  aplicá-la num banco que já existe.
- O índice da chave estrangeira está faltando, não há CHECK nas colunas de situação e de plano, e a capacidade e a
  diária dos quartos não estão no banco (moram no enum `RoomType`). Esses pontos são menores.

## Comportamento

**Números:** 20 de 72 conferências passaram (`measures/behavior.md`):
- a parte comum: 13 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 01: 5 de 21;
- as reservas simultâneas: 1 de 2. Nas 10 rodadas os dois pedidos voltaram 500, então não houve reserva dupla porque
  nada foi criado.

**Nota:** ruim

**O que falhou e por quê.** Quase todas as falhas têm uma só causa: o roteiro cria as reservas com
`guestCpf: "111.222.333-44"`, o formato que aparece no texto, e toda criação volta **500**. Com isso caem a
confirmação, o e-mail, o check-in, o check-out, os cancelamentos, os relatórios com dados e todos os valores dos
planos novos. Também caem as conferências depois do reinício, porque a reserva nunca existiu. Passam só as recusas,
que são decididas antes do `INSERT`: 422, 404, 400, convênio sem código ou com código inválido, pacote fora de sexta a
domingo, estadia longa com 6 noites. O texto não fixa o formato do CPF na entrada, mas um 500 nunca é uma resposta
aceitável: o sistema deveria aceitar o CPF formatado ou recusá-lo com 400. Para o dono, isso quer dizer que o site
não consegue reservar nada.

**O que eu conferi à mão, com CPF só de dígitos.** Os valores dos cinco planos saem certos. O STANDARD de julho dá
599,76, o NON_REFUNDABLE dá 37,48 de desconto e 212,42 de total, a Tecnosul dá 93,58 e 686,22, o pacote deluxe dá
820,00 com devolução de 410,00, a estadia longa dá 174,93 e 1.574,37, e o convênio cancelado devolve 686,22. O
e-mail e o comprovante seguem o modelo do texto, e o relatório de ocupação está certo. Mesmo assim, sobram defeitos:
- **A segunda via sai para reserva cancelada.** A reserva 3, CORPORATE paga e cancelada, devolveu 200 com o
  comprovante, porque o controller olha `amountPaid > 0` e não a situação (`ReservationController.java:164`). O
  texto diz que a segunda via só existe para reserva confirmada, com check-in ou com check-out.
- **O financeiro lista reserva que não deixou dinheiro.** A CORPORATE cancelada com devolução integral aparece com
  `0.00` (`ReservationRepository.java:182` filtra por `amount_paid > 0`, não pelo líquido). O total fica certo, mas
  a linha não deveria estar lá.
- **O dinheiro sem 2 casas.** Na criação, `discount` e `amountPaid` saem como `0` em vez de `0.00`.
- **Reserva dupla sob concorrência.** 14 de 15 pares simultâneos criaram as duas reservas (ver Banco).
- O roteiro também acusou o `number` do quarto como texto (`"201"`) em `/rooms`. O texto não fixa o tipo, e essa é
  uma escolha razoável; não pesou.

## Dúvidas

- **O formato do CPF na entrada.** O texto só mostra o CPF formatado, no comprovante. Um leitor poderia supor que o
  site manda só dígitos, e aí o comportamento seria bem melhor do que o roteiro mostra. Mantive `ruim` porque o
  sistema responde 500 (e não 400) a um formato que o próprio texto usa, e porque, mesmo com dígitos, sobram a
  reserva dupla e a segunda via de reserva cancelada.
- **Testes entre `com problemas` e `ruim`.** Os cinco testes têm asserções de valor boas para a parte nova. Pesou a
  cobertura de 9% e a ausência de qualquer teste das devoluções, da temporada, do ciclo de vida, da API e do banco.
- **O `companyCode` gravado em qualquer plano** (`ReservationService.java:60`). É uma regra pequena fora do plano;
  não muda a classe do pattern.
