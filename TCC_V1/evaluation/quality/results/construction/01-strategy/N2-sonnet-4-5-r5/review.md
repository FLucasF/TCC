# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin em 8 pacotes (`api`, `service`, `rateplan`, `repository`, `domain`, `exception`,
`util` e a raiz com o `Main`). Uma requisição entra num controller de `api/` (por exemplo
`ReservationController.createReservation`, `api/ReservationController.java:35-54`). O controller lê o corpo como `Map`,
chama o `ReservationService`, que pede ao plano de tarifa (`RatePlanRegistry` → `RatePlanStrategy`) a validação, o preço
e a devolução, e grava pelos repositórios JDBC (`repository/`). O esquema é criado com `CREATE TABLE IF NOT EXISTS` em
`util/DatabaseManager.java:28-66`, e tudo é montado à mão no `Main.java:30-52`. O Strategy existe para os cinco planos,
mas o serviço ainda pergunta pelo convênio para calcular o preço (`service/ReservationService.java:57-61`). Há três
problemas mais sérios:
- **O tipo do quarto na criação.** `roomNumber` só é aceito como texto (`api/ReservationController.java:38,146-151`).
  O roteiro oficial manda o número JSON `201`, e por isso **todas** as criações dele foram recusadas com 400, o que dá
  18 de 72 conferências.
- **A reserva dupla.** Nada no banco nem no código impede duas reservas simultâneas do mesmo quarto. Numa rodada à
  parte, com o quarto mandado como texto, as duas reservas foram criadas em 10 de 10 rodadas.
- **Os testes.** São poucos: 14% das linhas, só o pacote `rateplan`.

## Arquitetura

**Números:** 8 pacotes, 18 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em
`api` e na raiz, o JDBC só em `repository` e `util`. `rateplan`, `service` e `domain` não usam Javalin nem JDBC. A maior
classe é `ReservationRepository` (223 linhas), seguida de `ReservationController` (182).
**Nota:** com problemas

As camadas existem e as dependências vão na direção certa: `api → service → rateplan/repository → domain`, sem ciclo.
As regras de preço, validação e devolução ficam em `rateplan/` sem nenhuma dependência técnica, e todo o SQL fica em
`repository/` (`repository/ReservationRepository.java:22-200`). A montagem fica num lugar só (`Main.java:30-52`), e a
regra de erro HTTP também (`Main.java:58-73`).

As misturas são localizadas:
- **Uma regra no handler.** A regra de quem pode ter segunda via ("só reserva paga") mora no handler
  (`api/ReservationController.java:94-98`), não no serviço. Por estar ali, saiu com o código errado (400 em vez de 409).
- **O comprovante montado na borda.** O controller monta o comprovante do check-out e da segunda via indo direto ao
  `RoomRepository` e ao `NotificationService` (`api/ReservationController.java:84-87,100-103`). É a mesma montagem que o
  serviço faz na confirmação (`service/ReservationService.java:93-94`), e daí vem a dependência `api → repository` do
  jdeps. O mesmo vale para `RoomController.java:21` e `MessageController.java:20`, que leem o repositório direto.
  Para leituras simples isso pesa pouco.
- **A seleção do plano fora da montagem.** O `RatePlanRegistry` é um `static` global (`rateplan/RatePlanRegistry.java:10-26`)
  que o serviço chama direto (`service/ReservationService.java:48,109`), em vez de ser ligado no `Main`.

## Design de baixo nível

**Números:** 5 planos, os 5 com classe própria que implementa `RatePlanStrategy` (`validate`, `calculatePrice`,
`calculateRefund`), escolhida por um único `EnumMap` (`rateplan/RatePlanRegistry.java:12-18`). Há 1 desvio por plano
fora da estrutura: `if (ratePlan == RatePlan.CORPORATE && strategy instanceof CorporateRatePlan)` no serviço
(`service/ReservationService.java:57-61`). O laço das noites com a temporada está copiado em 4 classes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do acerto.** Os cinco planos, os dois antigos inclusive, têm preço, validação e devolução em classe
própria:
- o fim de semana valida sexta a domingo em `rateplan/WeekendPackageRatePlan.java:23-37`;
- a estadia longa exige 7 noites em `rateplan/LongStayRatePlan.java:15-21`;
- o código da empresa é conferido em `rateplan/CorporateRatePlan.java:21-29`;
- as devoluções são 100%, 0% e 50% (`StandardRatePlan.java:39`, `NonRefundableRatePlan.java:45`,
  `WeekendPackageRatePlan.java:47-50`).

O cancelamento pede a devolução ao plano sem nomear nenhum (`service/ReservationService.java:109-110`).

**O que afasta.** O cálculo do preço, que é o centro do pattern, ainda pergunta qual é o caso. A interface não sabe
receber o código da empresa (`rateplan/RatePlanStrategy.java:12`). Por isso o serviço testa
`ratePlan == CORPORATE && instanceof CorporateRatePlan` e chama um método que só o convênio tem
(`service/ReservationService.java:57-58`). E o `calculatePrice` "oficial" do convênio devolve, em silêncio, a reserva
**sem desconto** (`rateplan/CorporateRatePlan.java:32-37`, com `getOrDefault(..., ZERO)`). Quem usar a interface do jeito
que ela promete recebe um preço errado. O próximo plano que precisar de um dado do pedido (o "melhor idade" precisaria
da idade) repete o `instanceof`. Pelo critério da skill ("quem usa a estrutura ainda perguntando qual é o caso"), isso
é malfeito.

**O exagero.** Não há. A interface tem cinco implementações, `PricingResult` é um valor simples e não há fábrica,
builder nem camada de repasse.

**O resto do código:**
- **Duplicação.** O laço noite a noite com o acréscimo de `1.20` e o arredondamento está copiado em
  `StandardRatePlan.java:20-33`, `NonRefundableRatePlan.java:20-33`, `CorporateRatePlan.java:41-54` e
  `LongStayRatePlan.java:27-40`. Mudar a temporada ou o percentual é mexer em quatro lugares, e nenhum deles usa uma
  constante.
- **Dinheiro sem 2 casas.** `BigDecimal.ZERO` sem escala vira `0` no JSON em vez de `0.00`, no `amountPaid` da reserva
  nova (`domain/Reservation.java:28`) e no desconto do STANDARD (`rateplan/StandardRatePlan.java:35`).
- **Formatação que depende do ambiente.** O financeiro usa `String.format("%.2f")` sem `Locale`
  (`service/ReportService.java:66,71`): numa JVM em pt-BR, o CSV sai `599,76`. O comprovante usa
  `NumberFormat.getCurrencyInstance(pt-BR)` (`service/NotificationService.java:46`), que põe um espaço não separável
  (U+00A0) entre "R$" e o valor. Conferi rodando à parte: `R$ 1.234,56` sai com os bytes `52 24 a0 31...`. O texto
  pedia o espaço comum.
- **Erros.** `Long.parseLong` do id sem tratamento (`api/ReservationController.java:57,63,69...`) e o `bodyAsClass` do
  corpo (`:36`) caem no `catch (Exception)` geral e viram 500 (`Main.java:66-73`). `(String) request.get("companyCode")`
  (`:46`) dá `ClassCastException` se o código vier como número.
- **Código morto e estado.** `lines` é calculado e não é usado (`api/ReservationController.java:128`). A `Reservation` é
  só getters e setters (`domain/Reservation.java:31-165`), e as transições ficam no serviço. Num sistema deste tamanho
  isso é aceitável.

## Testes

**Números:** 9 testes rodaram, 0 falharam, 0 pulados; 9 métodos escritos, todos em `RatePlanTest`. A cobertura é de
112 de 802 linhas (14,0%) e 24 de 140 ramos (17,1%). O pacote `rateplan` tem 84 de 116 linhas cobertas; `service`,
`api` e `repository` têm 0. Não há nenhum teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

O que há é bom no que cobre. Os testes conferem os valores do texto com `BigDecimal` exato:
- 599,76 na alta temporada (`RatePlanTest.java:40-42`);
- Tecnosul 779,80 / 93,58 / 686,22 (`:70-72`);
- o pacote deluxe 820,00, com 410,00 de devolução (`:98-103`);
- a estadia longa 1.749,30 / 174,93 / 1.574,37 (`:129-131`);
- as recusas de código inválido, de pacote fora da sexta e de 6 noites (`:83-85`, `:114-116`, `:142-144`).

Mas quase todo o resto do sistema não tem rede. Ficam sem teste a máquina de estados (confirmar, check-in, check-out,
cancelar), o conflito de datas, a capacidade, os códigos de erro, o comprovante, o e-mail, os dois relatórios e a
persistência. O `service` está 0% coberto.

A pergunta da mutação, na parte nova:
- trocar o 0.18 da Agrovale ou o 0.25 da Farmavida (`CorporateRatePlan.java:16-17`) não derruba nada, porque só a
  Tecnosul é testada (`RatePlanTest.java:68`);
- trocar `nights < 7` por `nights <= 7` (`LongStayRatePlan.java:18`) não derruba nada, porque nenhum teste valida
  exatamente 7 noites;
- apagar a conferência do domingo (`WeekendPackageRatePlan.java:34-36`) não derruba nada, porque o único caso negativo
  começa num sábado;
- as devoluções do convênio, da estadia longa e do não reembolsável não são testadas.

Os testes também chamam `calculatePriceWithCompanyCode` direto (`:68`), e por isso não pegam o desvio do serviço.
E `assertEquals(BigDecimal.ZERO, result.getDiscount())` (`:26,41,99`) consagra o desconto sem 2 casas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` nas 5 colunas, as datas em `date`, e há 1 chave estrangeira (`reservations.room_number → rooms`).

As conferências acusaram:
- a chave estrangeira sem índice;
- as colunas `status`, `rate_plan`, `channel` e `type` sem CHECK;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- nenhum pool, uma chamada a `DriverManager.getConnection`, 0 transações e 0 `FOR UPDATE`;
- nenhum SQL montado com `+`.

(`measures/database.md`)
**Nota:** com problemas

**O que está certo.** Os tipos estão certos e o SQL usa sempre `?` (`repository/ReservationRepository.java:23-47`). As
conexões são fechadas com try-with-resources. Os dados ficam no banco: na rodada à parte, as 3 conferências de
reinício passaram.

**Os defeitos:**
- **O esquema não evolui.** Ele nasce de `CREATE TABLE IF NOT EXISTS` no código (`util/DatabaseManager.java:28-66`),
  sem migrações, e não saberá acrescentar uma coluna numa tabela que já existe. O plano novo que o comercial já
  anuncia vai precisar disso.
- **A FK está sem índice** (`util/DatabaseManager.java:54`).
- **Não há pool.** Cada chamada abre uma conexão nova (`util/DatabaseManager.java:20-22`).
- **A regra central do negócio não tem proteção.** O "um quarto, uma reserva por noite" é só um "consulta e depois
  insere" sem transação nem trava (`service/ReservationService.java:51-54` seguido de
  `repository/ReservationRepository.java:22-59`), e não há `EXCLUDE` no esquema. Na rodada à parte, com o quarto
  mandado como texto, as duas reservas simultâneas foram criadas em **10 de 10** rodadas.
- **As transições de situação podem se perder.** O `UPDATE` não confere a situação anterior
  (`repository/ReservationRepository.java:62-65`), então dois pedidos simultâneos (confirmar e cancelar) podem se
  sobrescrever.
- **A confirmação não é atômica.** Ela grava a reserva e depois a mensagem em conexões separadas
  (`service/ReservationService.java:91-95`).

## Comportamento

**Números:** o roteiro oficial (`measures/behavior.md`) deu 18 de 72 conferências: parte comum 11 de 46, reinício 1 de
3, cenário 01 5 de 21, reservas simultâneas 1 de 2. Numa rodada à parte, eu rodei uma cópia do roteiro em `/tmp` (o
projeto e as medidas oficiais ficaram como estavam), mudando só o `roomNumber` para texto (`"201"`). Ela deu 67 de 72:
parte comum 42 de 46, reinício 3 de 3, cenário 21 de 21, simultâneas 1 de 2.
**Nota:** com problemas

**A falha em massa do roteiro oficial.** Ela tem uma causa só: o projeto trata o número do quarto como texto
(`domain/Room.java:6`, `GET /rooms` devolve `"number":"201"`) e recusa com 400 `Missing or invalid field: roomNumber`
quando o site manda `201` como número (`api/ReservationController.java:38,146-151`). O texto não diz o tipo do campo.
Por isso tratei como uma escolha num ponto aberto, mas rígida: um site que mande o número como número não consegue
fazer reserva nenhuma.

**Com o quarto como texto, o cenário passa inteiro.** Conferem os três convênios (686,22, 639,44, 584,85), o pacote
(820,00, 520,00, 1.200,00, o de julho a 820,00 e 410,00 de devolução), a estadia longa (1.574,37, e 1.754,30 com
temporada), as recusas com 400 e o `companyCode` ignorado fora do convênio. As falhas que pesam para o dono da pousada
são estas:
- **Reserva dupla.** Em 10 de 10 rodadas, dois pedidos simultâneos para o mesmo quarto e as mesmas noites criaram as
  duas reservas, e a pousada venderia o mesmo quarto duas vezes. O defeito em si pesa no banco; aqui ele aparece como a
  falha das simultâneas.
- **A segunda via de reserva pendente** devolve 400 `INVALID_REQUEST` em vez de 409 `OPERATION_NOT_ALLOWED`
  (`api/ReservationController.java:97`).
- **Corpo que não é JSON** dá 500 `INTERNAL_ERROR` em vez de 400 (`api/ReservationController.java:36`, `Main.java:66-73`).
- **O dinheiro sem 2 casas.** `amountPaid` e `discount` saem como `0` em vez de `0.00` no JSON
  (`domain/Reservation.java:28`, `rateplan/StandardRatePlan.java:35`).
- **O e-mail não traz "Total: R$ 599,76" como o texto escreve.** O valor vem com espaço não separável depois do "R$"
  (`service/NotificationService.java:46`). Na tela o hóspede quase não nota, mas o texto não é o pedido.

## Dúvidas

- **O tipo de `roomNumber`.** Se o combinado com o site for lido como número JSON (os quartos são 101 a 301 e o campo
  se chama `number`), a parte comum quebra no roteiro oficial e o Comportamento seria `ruim`. Tratei como ponto que o
  texto deixou aberto, pela regra da skill, mas a recusa de um número é uma escolha frágil.
- **A nota do Banco.** A falta de qualquer proteção contra a reserva dupla, comprovada em 10 de 10 rodadas, é um risco
  de dado errado na regra central do negócio. Pela lista da skill (duplicação mal protegida conta como "com problemas")
  dei `com problemas`, mas um revisor mais severo daria `ruim`.
- **A classe do design.** Entre `malfeito` e `acerto`: os cinco planos estão na estrutura, e o desvio é um único `if`
  do convênio. Mas é no cálculo do preço, e a interface entrega o preço errado se chamada sem ele. Por isso fiquei com
  `malfeito`.
- **Os testes.** 9 testes e 14% de cobertura ficam perto do "quase não há testes". Dei `com problemas` porque os que
  existem conferem com valores exatamente a parte nova do cenário.
