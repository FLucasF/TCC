# Revisão: construção, cenário 03

## Visão geral

O projeto está em oito pacotes: `App` (montagem e rotas), `api.controller` (os handlers do Javalin), `api.dto`
(records de entrada e saída), `domain` (a `Reservation` com o preço e a máquina de estados, `ReservationDocument`,
enums), `notification` (a interface `ReservationObserver`, três ouvintes e o `NotificationService`), `repository`
(JDBC puro sobre HikariCP, com o `CREATE TABLE` em `DatabaseInitializer`) e `util`. Uma requisição entra no
controller, que valida o corpo, consulta o `RoomRepository` e o `ReservationRepository`, chama o método de estado da
`Reservation` (`confirm`, `cancel`...) e salva; a própria entidade percorre a lista de observadores ao confirmar e ao
cancelar, e cada observador grava um e-mail na tabela `messages`. Os dados ficam em três tabelas do PostgreSQL. O
Observer está bem aplicado e, com um CPF só de dígitos, o sistema faz quase tudo o que o texto pede. O que há de mais
sério: (1) a coluna `guest_cpf` é `VARCHAR(11)` e o controller não valida nem normaliza o CPF, então o CPF no formato
que o próprio texto usa (`111.222.333-44`) derruba **toda** criação de reserva com 500, e o roteiro inteiro falha a
partir daí; (2) nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos (10 de 10
rodadas duplicaram no meu teste).

## Arquitetura

**Números:** 8 pacotes, 18 dependências entre eles, 2 ciclos (`domain` ↔ `notification` e `repository` ↔
`notification`), de `measures/dependencies.md`. Maiores classes: `ReservationRepository` (246 linhas),
`Reservation` (232), `ReservationController` (225).
**Nota:** com problemas

As camadas existem e estão razoavelmente limpas: o Javalin só aparece em `App` e em `api.controller`; o JDBC só em
`repository` e na criação do pool em `App` (tabela de bibliotecas em `measures/dependencies.md`); a montagem fica num
lugar só, `App.java:28-53`, e todas as peças recebem as dependências pelo construtor. O domínio não conhece Javalin
nem SQL (`Reservation.java:1-11`).

O que pesa:

- **Dois ciclos, com a mesma origem.** O domínio importa a interface do observador (`Reservation.java:3`) e guarda a
  lista de ouvintes dentro da entidade (`Reservation.java:35`, `85-87`), enquanto `notification` importa `domain`
  (`ReservationObserver.java:3`). E o repositório recebe os observadores (`ReservationRepository.java:16-21`) e os
  pendura em cada reserva que carrega do banco (`ReservationRepository.java:240-242`), enquanto o
  `NotificationService` depende do `MessageRepository` (`NotificationService.java:5`). Fazer a entidade ser o
  "assunto" do Observer obrigou o repositório a conhecer a notificação; um serviço de reservas como assunto
  desfaria os dois ciclos.
- **Não há camada de regra entre o controller e o repositório.** A disponibilidade do quarto, a capacidade e a
  elegibilidade da segunda via moram no handler (`ReservationController.java:46-71`, `174-177`); o fluxo
  "busca, muda estado, salva" se repete em cinco handlers (`ReservationController.java:103-166`). É uma mistura
  localizada: o handler orquestra, mas não tem SQL.
- As credenciais padrão do banco embutidas em `App.java:98-106` são um detalhe de montagem que um revisor pediria
  para tirar.

## Design de baixo nível

**Números:** uma interface de ouvinte (`ReservationObserver`, 2 métodos), 3 implementações (hóspede, governança,
financeiro), 0 e-mails montados fora dos ouvintes; registro dos ouvintes feito em 1 lugar na montagem (`App.java:40-44`)
mas repassado às instâncias em 2 pontos (`ReservationController.java:85-87`, `ReservationRepository.java:240-242`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede o Observer e ele está lá: `ReservationObserver.java:5-8` define `onReservationConfirmed`
e `onReservationCancelled`; a `Reservation` percorre a lista que recebe de fora ao confirmar e ao cancelar
(`Reservation.java:98`, `130`, `133-143`) e não sabe quem é avisado; o hóspede também é um ouvinte
(`GuestNotifier.java:14-29`), a governança (`HousekeepingNotifier.java:16-55`) e o financeiro
(`FinanceNotifier.java:18-53`) decidem sozinhos se reagem (`wasConfirmed`, e o `refund == 0` em
`FinanceNotifier.java:34-40`); os três são criados e listados só em `App.java:40-44`. Entrar o restaurante ou o
transfer é uma classe nova e uma linha na lista. O que afasta um pouco: o assunto é a entidade, não um serviço, então
cada instância carregada do banco precisa receber os ouvintes de novo, e isso é feito em dois lugares; e o evento de
cancelamento passa um `boolean wasConfirmed` em vez de dizer o que aconteceu, o que obriga cada ouvinte a repetir o
mesmo `if` (`HousekeepingNotifier.java:38`, `FinanceNotifier.java:34`). Nada disso tira a classe.

**O exagero.** Não há interface com uma implementação só, fábrica ou builder. O `NotificationService`
(`NotificationService.java:14-17`) é uma classe de três linhas que só monta a `Message` e repassa ao repositório;
é fino, mas dá um ponto único para o "canal" quando os e-mails saírem de verdade. Contenção.

**O resto do código.** O que está bem: `BigDecimal` com `HALF_EVEN` a cada passo (`Reservation.java:57-83`),
`LocalDate`, enums para situação, plano, tipo e canal, records nos DTOs, SQL com `?`. O que um revisor pediria para
mudar:

- **Dinheiro vira `double` na borda** (`ReservationResponse.java:20-25`, `DocumentResponse.java:16-18`,
  `RoomResponse.java:10`): o JSON sai `0.0`, `249.9`, `779.8`, e não "número com 2 casas" como o texto pede.
- **O preço gravado nunca é lido de volta.** `mapReservation` reconstrói a reserva pelo construtor, que recalcula
  `subtotal`, `discount` e `total` a partir da tarifa atual (`ReservationRepository.java:220-231`,
  `Reservation.java:51-54`); as colunas gravadas são selecionadas e ignoradas. Se a diária mudar, o total de uma
  reserva antiga muda na leitura enquanto `amount_paid` fica com o valor antigo.
- **Os observadores disparam antes de persistir**: `confirm()` grava os três e-mails (`Reservation.java:98`) e só
  depois o controller faz o `save` (`ReservationController.java:110-111`). Se o `UPDATE` falhar, ficam e-mails de
  uma confirmação que não existiu.
- **Erro genérico engole a causa**: `App.java:66-70` transforma qualquer exceção (inclusive JSON inválido do Gson)
  em 500 `INTERNAL_ERROR`, quando o texto pede 400 `INVALID_REQUEST` para corpo que não é JSON.
- **CPF sem validação nem normalização** na entrada (`ReservationController.java:193-195` só vê se está em branco),
  com `Formatter.formatCpf` supondo 11 dígitos (`Formatter.java:22-30`) e a coluna `VARCHAR(11)`; o resultado é o
  500 do comportamento.
- Duplicação: `findById(...).orElseThrow(...)` com a mesma mensagem seis vezes e quatro `try/catch` idênticos em
  `ReservationController.java:94-181`.
- Estado: `setStatus`, `setAmountPaid`, `setRefund` públicos contornam a máquina de estados
  (`Reservation.java:209-227`); `getObservers` expõe a lista mutável (`Reservation.java:229-231`); variável
  `previousStatus` sem uso (`Reservation.java:94`).

## Testes

**Números:** 18 testes rodaram, 0 falharam, 0 pulados; 18 métodos de teste; cobertura de 13,9% das linhas (110 de
790) e 22,7% dos ramos (35 de 154); `domain` 101 de 181 linhas, `notification` 0 de 56, `api.controller` 0 de 191,
`repository` 0 de 217 (de `measures/tests.md`). Só testes de unidade, sem API, sem banco, sem mocks.
**Nota:** com problemas

O que é conferido com valores: o preço da alta temporada com arredondamento (`ReservationTest.java:27-30`: 2 noites,
599.76), o desconto do NON_REFUNDABLE (`ReservationTest.java:48-50`: 779.80, 116.97, 662.83), as transições
permitidas e proibidas (`ReservationTest.java:53-150`, `214-233`), a devolução em cada caso de cancelamento
(`ReservationTest.java:152-212`), as bordas da temporada (`SeasonCalculatorTest.java:19-24`, 14/12 falso e 15/12
verdadeiro) e o mecanismo do Observer com um ouvinte de teste: dois ouvintes avisados na confirmação, a sequência
confirmação + cancelamento com `wasConfirmed=true`, e o cancelamento de pendente com `false`
(`ReservationObserverTest.java:28-102`).

O que ficou sem rede é justamente o conteúdo da parte nova: nenhum teste instancia `GuestNotifier`,
`HousekeepingNotifier` ou `FinanceNotifier` (pacote `notification` com 0 linhas cobertas). Pergunta da mutação: trocar
o destinatário `governanca@...` (`HousekeepingNotifier.java:7`), o assunto "Pagamento recebido - reserva"
(`FinanceNotifier.java:19`) ou apagar o `if (refund == 0) return` (`FinanceNotifier.java:38-40`) não faz nenhum teste
falhar; trocar 0.15 por 0.21 (`RatePlan.java:7`) derruba `ReservationTest.java:49`, e trocar o dia 15 por 16
(`SeasonCalculator.java:16`) derruba `SeasonCalculatorTest.java:21`. Também não há teste do comprovante
(`ReservationDocument.render`: "1 diária", a linha "Desconto", `R$ 1.234,56`), dos relatórios, dos códigos de erro
da API nem do repositório; o pom declara Testcontainers (`pom.xml:62-73`) e o failsafe, mas nenhum teste os usa. O
`mvn verify` passa.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`
nas 5 colunas; datas em `date`; 0 chaves estrangeiras; 0 CHECK; 0 UNIQUE/EXCLUDE; esquema por `CREATE TABLE IF NOT
EXISTS` no código, 0 migrações; 0 transações explícitas, 0 `FOR UPDATE`; SQL todo com parâmetros (de
`measures/database.md`).
**Nota:** ruim

O que está certo: tipos de dinheiro e de data (`DatabaseInitializer.java:34-44`), `BIGSERIAL` numerando de 1, SQL
sempre com `?`, pool HikariCP (`App.java:108-114`), conexões em try-with-resources, quartos semeados com `ON
CONFLICT DO NOTHING` (`RoomRepository.java:20`), e o serviço sobe num banco vazio.

O que derruba a coluna:

- **Nada impede a reserva dupla.** A conferência é "consulta e depois insere" em duas conexões diferentes
  (`ReservationController.java:65-71` → `ReservationRepository.java:123-154`, depois `31-55`), sem transação, sem
  trava e sem restrição no esquema. Subi o serviço e mandei dois POSTs simultâneos para o 301 nas mesmas noites, dez
  vezes: as dez rodadas criaram as duas reservas (dois registros não cancelados por `check_in` na tabela). É dado
  errado, no único ponto em que o texto diz "um quarto não pode ter duas reservas na mesma noite".
- **`guest_cpf VARCHAR(11)`** (`DatabaseInitializer.java:33`): o CPF escrito como no texto (`111.222.333-44`) estoura
  a coluna e o `INSERT` falha (`measures/behavior-service.log`: `value too long for type character varying(11)`),
  virando 500 para o site.
- Sem chave estrangeira de `reservations.room_number` para `rooms` (`DatabaseInitializer.java:29`), sem CHECK nas
  colunas de situação, tipo e plano.
- `subtotal`, `discount` e `total` são gravados mas nunca lidos (`ReservationRepository.java:220-231` recalcula);
  ver o design.
- Os e-mails são inseridos (cada um na sua conexão) antes do `UPDATE` da reserva e fora de qualquer transação
  (`Reservation.java:98` → `MessageRepository.java:18-39`, depois `ReservationRepository.java:57-76`): uma falha no
  meio deixa mensagens sem a confirmação correspondente.
- `CREATE TABLE IF NOT EXISTS` no código não sabe acrescentar coluna numa tabela existente; não há migrações.

## Comportamento

**Números:** 17 de 66 conferências passaram (de `measures/behavior.md`): parte comum 13 de 46; dados depois de
reiniciar 1 de 3; cenário 03 2 de 15; duas reservas ao mesmo tempo 1 de 2.
**Nota:** ruim

Quase todas as falhas têm uma causa só: cada `POST /reservations` do roteiro respondeu 500 `INTERNAL_ERROR`, porque o
roteiro manda o CPF como o texto o mostra (`111.222.333-44`) e a coluna é `VARCHAR(11)` (log do serviço, pilha em
`ReservationRepository.java:45-53`). Sem reserva criada, confirmação, e-mails, check-in, check-out, cancelamentos,
relatórios e persistência falham em cascata, e as duas conferências do cenário que "passaram" (nada a devolver,
pendente sem aviso) passaram por não haver mensagem nenhuma. Para o dono da pousada: com o site mandando o CPF
formatado, o sistema não cria reserva nenhuma.

Para separar o que é só esse defeito do que é outro, subi o serviço num banco vazio e repeti o roteiro com o CPF só
de dígitos (`11122233344`). Aí o sistema acerta: 101 de 10 a 12/07 dá 2 noites e 599.76; NON_REFUNDABLE de uma
noite em março dá desconto 58.48 e total 331.42 (meio para o par); PENDING → CONFIRMED (pago 599.76) → CHECKED_IN →
CHECKED_OUT com o comprovante; confirmar de novo, cancelar depois do check-in e segunda via de pendente dão 409
`OPERATION_NOT_ALLOWED`; quarto ocupado dá 409 `ROOM_UNAVAILABLE`; cancelar STANDARD confirmada devolve 779.80 e
NON_REFUNDABLE devolve 0.00; o e-mail do hóspede sai com o texto e o comprovante exatos (CPF formatado, "1 diária",
linha "Desconto", `R$ 599,76`); a governança recebe "Reserva confirmada - quarto 201" com 10/03/2027, 12/03/2027 e
"2 hóspedes", o financeiro "Pagamento recebido - reserva 2" com `R$ 779,80`; no cancelamento, "Reserva cancelada -
quarto 201" com as datas e "Reembolso - reserva 2" com `R$ 779,80`; a NON_REFUNDABLE cancelada avisa a governança e
não o financeiro; a pendente cancelada não manda nada; os dois CSVs saem certos (`101,STANDARD,1,...` e `total,1/5`;
`1,101,...,CHECKED_OUT,599.76` e `total,599.76`; a NON_REFUNDABLE cancelada com 331.42 continua no financeiro). As
mensagens sobreviveram ao reinício no roteiro (1 de 3 que passou).

Os defeitos que restam mesmo com o CPF de dígitos:

- **Duas reservas ao mesmo tempo:** 10 de 10 rodadas com as duas criadas (meu teste; no roteiro, as rodadas vieram
  500/500 pelo CPF). Pesa na coluna do banco; aqui é o que o site vai ver.
- **Corpo que não é JSON responde 500** em vez de 400 `INVALID_REQUEST` (`App.java:66-70`).
- **Dinheiro no JSON sem 2 casas:** `0.0`, `249.9`, `779.8` (`ReservationResponse.java:20-25`); e `refund` some do
  JSON em vez de vir `null` antes do cancelamento (Gson omite nulos).
- `GET /rooms` devolve `number` como texto (`"101"`); o roteiro esperava número, e por isso "o 201 comporta 3" e
  "a diária do 201" falharam. O texto não fecha o tipo; não pesa.

## Dúvidas

- O texto mostra o CPF só no comprovante (`111.222.333-44`) e não diz em que formato o site o manda. Mesmo assim, uma
  entrada sem validação que vira 500 e trava a criação de reserva é defeito; se o site mandasse só dígitos, a nota do
  comportamento subiria para `com problemas` (ficariam a reserva dupla, o 500 do JSON inválido e as 2 casas).
- O tipo de `number` em `GET /rooms` (texto ou número) fica em aberto no texto.
