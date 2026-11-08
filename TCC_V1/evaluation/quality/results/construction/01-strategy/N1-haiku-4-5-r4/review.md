# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin em seis pacotes (`handler`, `service`, `persistence`, `model`, `util` e o
`Application`), com JDBC puro sobre HikariCP e Gson como mapeador de JSON. Uma requisição entra pela rota em
`Application.java:50-69`, passa por um handler que lê o JSON à mão (`handler/ReservationHandler.java:28-45`), chega ao
`ReservationService`, que valida, calcula o preço pelo `PricingService` e grava pelo `ReservationDao`. Os dados ficam
em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:42-89`). Os números do
texto saem certos (21 de 21 conferências do cenário passam), mas há quatro problemas sérios:

- **Reserva dupla:** duas reservas pedidas ao mesmo tempo para o mesmo quarto foram as duas criadas em 9 de 10
  rodadas. A disponibilidade é conferida e a reserva é gravada depois, sem transação, sem trava e sem restrição no
  banco (`service/ReservationService.java:48-72`).
- **Relatório financeiro:** ele deixa de fora as reservas canceladas que deixaram dinheiro com a pousada
  (`persistence/ReservationDao.java:137`).
- **Valor pago:** a confirmação aceita do cliente qualquer `amountPaid` (`handler/ReservationHandler.java:101-108`).
- **Os planos de tarifa:** ficaram em três `switch` (`service/PricingService.java:30`, `:116`,
  `service/ReservationService.java:143`), e não no Strategy que o texto pedia.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `handler`, `util` e na raiz, e o JDBC em `persistence`, na raiz e em `service`. Em `service`, o jdeps acusa o JDBC só
porque os métodos declaram `SQLException` (`service/MessageService.java:8`), e não há SQL nenhum nesse pacote. A maior
classe é `ReservationHandler` (238 linhas).
**Nota:** com problemas

O que está certo:

- **As camadas existem e apontam na direção certa:** os handlers chamam os serviços, os serviços chamam os DAOs, e
  todo o SQL está em `persistence/` (`ReservationDao.java`, `RoomDao.java`, `MessageDao.java`).
- **A montagem fica num lugar só:** `Application.java:28-42`.
- **As regras não conhecem o Javalin.**

O que está errado:

- **O contexto do Javalin vira um repositório de dependências:** as rotas de check-out e de segunda via enfiam o
  `RoomDao` como atributo do `Context` (`Application.java:57-64`), e o handler o tira de lá com um cast
  (`handler/ReservationHandler.java:147`, `:206`). O handler então consulta o banco para montar o comprovante.
- **A montagem do comprovante está fora da regra e duplicada:** o handler monta o comprovante duas vezes
  (`handler/ReservationHandler.java:150-162` e `:209-221`), enquanto o texto do mesmo comprovante para o e-mail é
  montado à parte, em `service/MessageService.java:41-62`.
- **Uma regra da reserva mora no handler:** a segunda via só é recusada para `PENDING`
  (`handler/ReservationHandler.java:201`), e o serviço nem fica sabendo dessa regra.
- **A confirmação lê um `amountPaid` do corpo** (`handler/ReservationHandler.java:101-108`), uma decisão de negócio
  tomada na borda HTTP.
- **Handlers que pulam o serviço:** `RoomHandler` e `MessageHandler` falam direto com os DAOs (`handler/RoomHandler.java:19`,
  `handler/MessageHandler.java:19`). Isso é aceitável para leituras simples, mas é inconsistente com o resto.
- **Um `static` global:** o `DataSource` é um `static` mutável (`persistence/Database.java:15`).
- **Um campo sem uso:** o `MessageService` é injetado no `ReservationHandler` e nunca usado
  (`handler/ReservationHandler.java:19`, `:23`).

São misturas localizadas, sem ciclo, e por isso a nota é `com problemas`.

## Design de baixo nível

**Números:** 5 planos de tarifa, 0 numa estrutura própria. O comportamento dos planos é decidido em 3 `switch` sobre
`RatePlan`, mais 2 `switch` internos de valores por caso:

| o que o `switch` decide | onde |
|---|---|
| o preço | `service/PricingService.java:30-36` |
| a devolução | `service/PricingService.java:116-123` |
| quem pode usar | `service/ReservationService.java:143-165` |
| o desconto de cada empresa | `service/PricingService.java:71-76` |
| o preço do pacote por tipo de quarto | `service/PricingService.java:87-91` |

**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto avisa que "o comercial vive inventando plano" e que cada plano tem o seu preço, as suas regras
de quem pode usar e a sua devolução. O agente deixou `RatePlan` como um `enum` sem comportamento
(`model/RatePlan.java:3-9`) e espalhou cada um dos três aspectos por um `switch`:

- o preço em `PricingService.calculatePrice` (`service/PricingService.java:30-36`), com um método privado por plano;
- a devolução em `PricingService.calculateRefund` (`service/PricingService.java:116-123`);
- a validação em `ReservationService.validateRatePlan` (`service/ReservationService.java:143-165`): o código da
  empresa, sexta a domingo e 7 noites.

Um plano novo (a melhor idade, a antecedência de 60 dias) obriga a mexer nos três `switch` e no `enum`. Não há uma
interface nem um `enum` com corpo por constante. Pelo critério da referência, é omissão. Os dois planos que já
existiam também estão no `switch`. Os códigos das empresas estão escritos duas vezes, na validação
(`service/ReservationService.java:145`) e na tabela de descontos (`service/PricingService.java:72-74`).

**O exagero.** Não há estrutura sem necessidade: nenhuma interface de uma implementação só, nenhuma fábrica. O que
sobra é código morto, que não é exagero: `model/Receipt.java` inteiro não é usado por ninguém, nem
`MessageService.getReceiptAsString` (`service/MessageService.java:64-66`).

**O resto do código:**

- **Os erros passam como texto.** Os serviços lançam `IllegalArgumentException("ROOM_NOT_FOUND")` e similares
  (`service/ReservationService.java:35`, `:39`, `:43`, `:50`). Cada handler repete a mesma escada de
  `if ("...".equals(msg))` (`handler/ReservationHandler.java:58-70`, `:85-89`, `:113-119`, `:131-137`, `:166-172`,
  `:184-190`, `:225-229`).
- **Erros inesperados viram 400.** O `catch (Exception)` final manda tudo para `ErrorHandler.handleException`, que
  responde 400 `INVALID_REQUEST` com a mensagem da exceção (`handler/ErrorHandler.java:18-19`, `:40-42`). Uma falha
  do banco ou um `NullPointerException` chega ao cliente como erro de dado, e a causa vai junto no texto.
- **Mais duplicação no tratamento de erro:** `handle400` a `handle422` são quatro cópias do mesmo método
  (`handler/ErrorHandler.java:45-71`), e o `errorMap` montado na linha 12 nunca é usado.
- **O estado aberto:** `PriceCalculation` tem campos públicos e mutáveis (`service/PricingService.java:15-19`), e o
  `DataSource` é um `static` (`persistence/Database.java:15`).
- **O dinheiro:**
  - O `BigDecimal` e o `HALF_EVEN` estão certos (`util/MoneyRounder.java:10`).
  - A noite com o acréscimo da temporada não é arredondada a cada passo, como o texto pede, só a soma
    (`service/PricingService.java:45-52`). Com as diárias de hoje o resultado é o mesmo, porque a conta dá exata.
  - O JSON sai por `doubleValue()` (`util/GsonConfig.java:23`), e o valor perde as duas casas (`0.0`, `1749.3`).
  - A formatação depende da localidade da JVM: `String.format("%.2f")` aparece em `service/ReportService.java:84` e
    em `service/MessageService.java:73`, que ainda não põe o separador de milhar.
- **A reserva:** `Reservation` é um saco de setters (`model/Reservation.java`), e as transições de situação são
  `setStatus` soltos no serviço.

São defeitos reais, mas concentrados na borda e no tratamento de erro, e o código continua fácil de ler. Por isso a
nota é `com problemas`.

## Testes

**Números:** 6 testes rodaram, 0 falharam; 6 métodos escritos, numa única classe. Cobertura de 80 de 919 linhas (8,7%)
e de 24 de 203 ramos (11,8%). `handler`, `persistence` e `Application` estão com 0% (`measures/tests.md`).
**Nota:** ruim

O que os testes conferem: `service/PricingServiceTest.java` testa o cálculo com valores, e esses valores são os do
texto:

- STANDARD na alta temporada, 599,76 (`:23-26`);
- Tecnosul, 779,80, 93,58 e 686,22 (`:37-40`);
- o pacote deluxe, 820,00 (`:51-54`);
- a estadia longa, 1.749,30, 174,93 e 1.574,37 (`:65-68`);
- as cinco devoluções, inclusive os 410,00 (`:85-89`).

O que fica de fora:

- **A pergunta da mutação.** Trocar o 0.18 da Agrovale ou o 0.25 da Farmavida (`service/PricingService.java:73-74`)
  não derruba nenhum teste. O NON_REFUNDABLE não tem teste de preço, nem os pacotes standard e suíte.
- **As regras de quem pode usar o plano** ficam no `ReservationService` (`service/ReservationService.java:143-165`) e
  não têm teste: trocar `nights < 7` por `nights < 6`, ou aceitar um código de empresa qualquer, passa. O único teste
  de validação é o do pacote com uma quinta-feira (`:72-79`), e ele chama o método estático, não o caminho da reserva.
- **Nada do resto:** as situações da reserva, os códigos de erro da API, o comprovante, o e-mail, os dois relatórios,
  a gravação no banco. Por isso os três defeitos que o roteiro achou (o hóspede zero, o financeiro, a reserva dupla)
  passariam despercebidos.

Para um sistema com o tamanho e as regras deste, 8,7% de linhas e uma classe de testes é a coluna quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, nas 6 colunas, e as datas em `date`. O que as conferências acusaram (`measures/database.md`):

- a chave estrangeira `reservations(room_number) -> rooms` não tem índice;
- as 4 colunas de situação, plano, tipo e canal não têm `CHECK`;
- não há nenhuma restrição `UNIQUE`, `EXCLUDE` ou `CHECK`;
- não há nenhuma transação explícita e nenhum `FOR UPDATE`;
- o esquema é criado por `CREATE TABLE` no código, sem migração.

**Nota:** ruim

O que está certo:

- **Os tipos estão certos.**
- **O SQL usa sempre parâmetros `?`:** o script não achou nenhum SQL montado com `+` a partir de dado de fora.
- **As conexões voltam ao pool** por try-with-resources (`persistence/ReservationDao.java:32`, `:69`, ...).

O que está errado:

- **A regra mais importante dos dados não é protegida.** "Um quarto não pode ter duas reservas na mesma noite" depende
  só de um `SELECT` (`service/ReservationService.java:48`, `persistence/ReservationDao.java:108-129`) seguido de um
  `INSERT` noutra conexão (`service/ReservationService.java:72`, `persistence/ReservationDao.java:26-60`). Não há
  transação, nem trava, nem uma restrição `EXCLUDE` sobre o período, nem sequer um `synchronized`. O roteiro do
  comportamento mediu o efeito: em 9 de 10 rodadas de dois pedidos simultâneos, as duas reservas foram gravadas.
  É dado errado no banco (o mesmo quarto vendido duas vezes), e foi isso que decidiu a nota.
- **O esquema não sabe evoluir:** é `CREATE TABLE IF NOT EXISTS` em `persistence/Database.java:47-80`. O próximo
  plano que precisar de uma coluna nova não chega a quem já tem a tabela.
- **Faltam restrições baratas:** `CHECK` para `status`, `rate_plan` e `guests >= 1`, e o índice em
  `reservations.room_number`.
- **A consulta do financeiro não usa índice:** ela filtra por `EXTRACT(YEAR/MONTH FROM check_in)`
  (`persistence/ReservationDao.java:136`), e não por um intervalo de datas. É menor.

## Comportamento

**Números:** 68 de 72 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 43 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 01 | 21 de 21 |
| as reservas simultâneas | 1 de 2 |

**Nota:** com problemas

Todo o cenário passa: os três convênios, as recusas com 400, o pacote com preço fechado e a meia devolução de 410,00,
a estadia longa com a temporada, a recusa com 6 noites e o `companyCode` ignorado nos outros planos. As falhas:

- **Duas reservas ao mesmo tempo: em 9 de 10 rodadas as duas foram criadas.** Para o dono, o mesmo quarto pode ser
  vendido duas vezes quando dois hóspedes reservam no mesmo instante pelo site. O defeito é do banco e pesa naquela
  coluna; aqui ele entra só como o que o roteiro observou.
- **O financeiro deixa de fora as reservas canceladas que deixaram dinheiro.** O NON_REFUNDABLE cancelado (212,42)
  não aparece, porque a consulta só pega `CONFIRMED`, `CHECKED_IN` e `CHECKED_OUT`
  (`persistence/ReservationDao.java:137`). O texto diz com todas as letras que essa reserva "continua contando". Pela
  mesma consulta, a metade que fica com a pousada num pacote cancelado também some. Para o contador, o relatório mostra
  menos receita do que a pousada teve.
- **O hóspede zero responde 422 `CAPACITY_EXCEEDED`, e não 400 `INVALID_REQUEST`.** O motivo é que
  `guests < 1 || guests > capacity` cai no mesmo erro (`service/ReservationService.java:38-40`). É um código de erro
  errado para o site.
- **O dinheiro no JSON não sai com 2 casas** (`0.0`, `1749.3`), por causa de `util/GsonConfig.java:23`. O site recebe
  o número certo, mas fora do formato combinado.

Achei mais três desvios chamando a API eu mesmo, fora do roteiro:

- a segunda via de uma reserva cancelada responde 200 com o comprovante (`handler/ReservationHandler.java:201` só
  recusa a `PENDING`), e o texto diz que ela só existe para reserva paga;
- a confirmação aceita `{"amountPaid":"1.00"}` no corpo e grava 1,00 como pago numa reserva de 499,80
  (`handler/ReservationHandler.java:101-108`), quando o texto diz que o pago é o total;
- o e-mail sai com "R$ 1749,30", e não "R$ 1.749,30", como o texto pede (`service/MessageService.java:73`).

A parte comum funciona no geral e o cenário passa inteiro. Mas há um relatório financeiro errado e reservas duplicadas
sob concorrência, e por isso a nota é `com problemas`, não `bom`.

## Dúvidas

- **Onde contar a reserva dupla.** Pesei a reserva dupla na coluna do banco, e por isso dei `ruim` ali: a skill do
  banco põe "protegida só dentro do processo" em `com problemas`, e aqui não há proteção nenhuma, e o roteiro mediu 9
  em 10. Um leitor que siga a letra da skill pode preferir `com problemas` no banco.
- **A nota dos testes.** Os testes que existem conferem valores certos da parte nova. A nota `ruim` vem da cobertura
  de 8,7% e da falta de qualquer teste fora do cálculo; ela fica entre `ruim` e `com problemas`.
- **Os valores com a localidade da máquina.** No ambiente do roteiro, o CSV saiu com ponto (`599.76`). Numa JVM com
  localidade pt-BR, o `String.format("%.2f")` de `service/ReportService.java:84` passaria a escrever `599,76` e
  quebraria o CSV. Não testei com outra localidade.
