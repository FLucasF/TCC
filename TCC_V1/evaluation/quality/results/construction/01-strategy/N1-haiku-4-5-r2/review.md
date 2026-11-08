# Revisão: construção, cenário 01

## Visão geral

O projeto tem seis pacotes: `com.pousada` (o `Application`, com o `main` e as rotas do Javalin), `api.dto` (os
objetos de entrada e saída), `service` (`ReservationService`, `ReportService` e `ApiException`), `domain` (reserva,
quarto, temporada, `Money`), `domain.rateplans` (uma classe por plano de tarifa e um registro) e `infra` (JDBC puro
com `DriverManager`, o esquema criado por `CREATE TABLE IF NOT EXISTS` no código). Uma requisição entra num handler do
`Application`, passa pelo serviço, que pede o preço, a validação e a devolução ao plano, e o serviço grava pelos
repositórios no PostgreSQL. O problema mais sério é que **o sistema não funciona pela API**. O `pom.xml` não traz o
Jackson, e o Javalin 6 precisa dele para `ctx.json` e `ctx.bodyAsClass`. Por isso quase todo endpoint JSON responde 500
("It looks like you don't have an object mapper configured", `measures/behavior-service.log:22`), inclusive as
respostas de erro, que também saem por `ctx.json` (`Application.java:237`). Só os relatórios CSV respondem, e sempre
vazios. Passaram 6 de 72 conferências. O `mvn verify` passa porque os únicos 3 testes não sobem o sistema. Há outros
defeitos sérios. O serviço não sobe de novo quando o banco já tem uma reserva, porque apaga os quartos a cada partida
(`Database.java:82`; reproduzi: violação da chave estrangeira). A confirmação grava como pago o que vier no corpo, 0 se
nada vier, e não o total (`Application.java:109-119`). O cancelamento aceita uma reserva com check-in
(`ReservationService.java:143-144`).

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em
`com.pousada`; o JDBC aparece em `infra` e também em `service`. As maiores classes são `Application` (242 linhas),
`ReservationService` (209) e `ReservationRepository` (184).
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo: a borda chama os serviços, os serviços chamam os planos
e os repositórios, e não há ciclo. As regras de preço ficam fora do Javalin e do SQL (`domain/rateplans/*`). A
separação, porém, vaza em vários pontos localizados:

- Há regra de negócio na borda. A segunda via é negada em `Application.java:172` por `amountPaid == 0`, e não pela
  situação da reserva. A confirmação lê e interpreta o valor pago no handler, com Gson
  (`Application.java:109-119`), em vez de o serviço usar o total.
- Algumas rotas saltam o serviço e chamam o repositório direto (`Application.java:49`, `:188`). Duas rotas criam o
  repositório por dentro com `new RoomRepository()` (`Application.java:146`, `:177`) em vez de usar o que foi
  montado no `main`.
- O serviço conhece o JDBC e o HTTP. Os métodos lançam `SQLException` (`ReservationService.java:33`, `ReportService.java:23`),
  e a `ApiException` do pacote `service` carrega o status HTTP (`ApiException.java:5`, usada como
  `new ApiException(..., 404)` em `ReservationService.java:44`).
- A configuração do banco é global e estática: `Database.java:8-10` e `Database.getConnection()` é chamado por todos
  os repositórios (`ReservationRepository.java:24` e seguintes). Os repositórios não recebem a conexão nem um pool.
- `api.dto` depende de `infra`: `MessageResponse.java:3` usa o record `Message`, que mora dentro do repositório
  (`MessageRepository.java:9`).

Nenhum desses defeitos derruba a estrutura, mas são vários e espalhados pela borda.

## Design de baixo nível

**Números:** 5 de 5 planos têm uma classe própria que implementa `RatePlan` (`validate`, `calculatePrice` e
`calculateRefund`), escolhida por um mapa (`RatePlanRegistry.java:9-15`). Nenhum `switch` sobre o plano. Um único
lugar fora da estrutura pergunta qual é o plano: o `instanceof CorporateRatePlan` no cálculo do preço
(`ReservationService.java:68-72`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O que aproxima do Strategy:

- Os cinco planos, o normal e o não reembolsável inclusive, têm cada um a sua classe com preço, validação e devolução:
  - `StandardRatePlan.java:21-31`;
  - `NonRefundableRatePlan.java:21-31`;
  - `CorporateRatePlan.java:22-45`;
  - `WeekendPackageRatePlan.java:23-49`;
  - `LongStayRatePlan.java:16-35`.
- As regras de quem pode usar ficam dentro do plano: a sexta a domingo em `WeekendPackageRatePlan.java:26-37`, as 7
  noites em `LongStayRatePlan.java:19`, o código da empresa em `CorporateRatePlan.java:24`.
- O cancelamento pede a devolução ao plano sem nomeá-lo (`ReservationService.java:148-149`).

O que afasta: o cálculo do preço ainda pergunta qual é o plano. Como a interface não passa o `companyCode` para
`calculatePrice` (`RatePlan.java:12`), o serviço faz `if (plan instanceof CorporateRatePlan corpPlan)` e chama uma
sobrecarga só dele (`ReservationService.java:68-72`). A versão da interface no convênio lança `IllegalStateException`
(`CorporateRatePlan.java:30-32`): o plano não pode substituir os outros pela interface. O próximo plano que precisar
de um dado da reserva (a antecedência de 60 dias, a idade) vai acrescentar outro `instanceof` ali. É o "quem usa a
estrutura ainda perguntando qual é o caso" da classe malfeito.

Exagero: nenhum. O `BaseRatePlan` reúne o cálculo das diárias, que quatro planos usam. O registro é um mapa simples.
Não há interface de uma implementação só, fábrica nem camada que só repassa.

O resto do código tem defeitos em vários pontos, cada um localizado:

- **Dinheiro em ponto flutuante.** A temporada e os percentuais entram como `double`: `Season.java:21-22`,
  `Money.java:18-28` e `CorporateRatePlan.java:10` (`Map<String, Double>`). Os DTOs expõem o dinheiro como `double`
  (`ReservationResponse.java:16-21`, `ReceiptResponse.java:16-18`, `RoomResponse.java:9`), e um valor como 520.00 vira
  `520.0` no JSON, contra o "número com 2 casas" pedido.
- **Duplicação.** A formatação de data está copiada em quatro lugares (`ReservationService.java:193`,
  `ReportService.java:85`, `ReceiptResponse.java:37`, `ReservationResponse.java:44`), e as cópias já divergem. O
  relatório de ocupação usa `dd/MM/yyyy` (`ReportService.java:42-43`) quando o texto pede `2027-07-10`. O mesmo bloco
  `try/catch (ApiException)/catch (Exception)` se repete em onze handlers do `Application.java`. A checagem de
  hóspedes existe no serviço (`ReservationService.java:34`) e de novo em todo plano (`BaseRatePlan.java:31-35`).
- **Erros.** Uma exceção é engolida em `Application.java:119` (`catch (Exception ignored) {}`). Um `catch (Exception)`
  devolve 500 com a mensagem interna (`Application.java:89-90`). Um id que não é número vira 500, não 400
  (`Application.java:96`).
- **Regras erradas no código.** O valor pago vem do corpo da confirmação (`Application.java:116`) e não é o total. O
  cancelamento bloqueia só `CHECKED_OUT` e `CANCELLED` e aceita `CHECKED_IN` (`ReservationService.java:143-144`).
- **Texto solto.** O plano é guardado como texto (`Reservation.java:15`). Há um `HashMap` de chaves duplas
  (`Application.java:237`) e o `Gson` convive com o mapeador do Javalin (`Application.java:23`).

O núcleo (a `Reservation` imutável, `BigDecimal` com `HALF_EVEN` em `Money.java:7`, `LocalDate`, o `enum` de
situação) é razoável. Por isso a nota fica em "com problemas" e não em "ruim".

## Testes

**Números:** rodaram 3 testes, 0 falharam (3 métodos escritos, em 1 arquivo). Cobertura de 5 de 687 linhas (0,7%) e 0
de 138 ramos (0,0%). Em `domain.rateplans`, `service`, `infra` e `com.pousada`, a cobertura é 0
(`measures/tests.md`). Nenhum teste pela API nem contra o banco.
**Nota:** ruim

O único arquivo, `MoneyTest.java`, confere três contas de `Money` com valores do texto. São `299.88`
(`MoneyTest.java:14-15`), `93.58` com 12% (`:20-21`) e `174.93` com 10% (`:27-28`). Esses testes chamam
`Money.percentOf` com o percentual escrito no próprio teste. Nenhum teste chama um plano, o serviço, um repositório ou
a API. Pela pergunta da mutação, ficam sem rede:

- os percentuais do convênio: trocar 12.0 por 21.0 em `CorporateRatePlan.java:11` não falha nada;
- a regra das 7 noites: trocar `< 7` por `< 6` em `LongStayRatePlan.java:19` também não;
- a metade devolvida pelo pacote (`WeekendPackageRatePlan.java:48`);
- a recusa do convênio sem código;
- as situações da reserva, o cancelamento, os relatórios e o e-mail.

Um teste que subisse o sistema uma vez teria mostrado que nenhum endpoint JSON funciona (falta o Jackson). O
`mvn verify` passa (`measures/tests-maven.log`, BUILD SUCCESS) sem conferir quase nada.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram:

- 1 chave estrangeira sem índice (`reservations.room_number`);
- nenhum `CHECK` nas colunas de situação, plano, tipo e canal;
- nenhuma restrição que proteja uma regra;
- nenhum pool, nenhuma transação explícita, nenhum `FOR UPDATE`;
- o esquema criado por `CREATE TABLE` no código, sem migração.

O SQL é todo com parâmetros (`measures/database.md`).
**Nota:** ruim

Os tipos estão certos e o SQL usa `?` em todas as consultas (`ReservationRepository.java:16-22`, `:72-81`). O
defeito sério é que, depois que existe uma reserva no banco, o serviço não sobe mais. A cada partida,
`insertInitialRooms` faz `DELETE FROM rooms` (`Database.java:82`), e a chave estrangeira de `reservations`
(`Database.java:59`) impede o apagamento. Reproduzi assim: subi o jar num banco vazio, inseri uma reserva no banco,
reiniciei, e o processo morreu com `Failed to initialize database schema ... violates foreign key constraint
"reservations_room_number_fkey"`. Os dados ficam no banco, mas o sistema para de vez no primeiro reinício depois da
primeira reserva. O roteiro do comportamento não chegou a ver isso, porque nenhuma reserva foi criada pela API.

Os outros defeitos:

- **Reserva dupla.** Nada no banco impede duas reservas no mesmo quarto e na mesma noite: nenhum `EXCLUDE`, nenhuma
  trava, nenhuma transação. A verificação (`ReservationService.java:62`) e a gravação (`:96`) andam em conexões
  separadas, com corrida entre elas.
- **Evolução do esquema.** O `CREATE TABLE IF NOT EXISTS` (`Database.java:31-72`) não sabe acrescentar uma coluna
  numa tabela que já existe.
- **Conexões.** Uma conexão nova por consulta, com `DriverManager` (`Database.java:24`) e sem pool.
- **Escritas soltas.** Duas escritas que deveriam andar juntas, a confirmação e o e-mail, ficam sem transação
  (`ReservationService.java:113-116`).
- **O relatório de ocupação.** Faz uma consulta por quarto (`ReportService.java:31`).
- **O relatório financeiro.** Filtra com `EXTRACT` sobre `check_in` (`ReservationRepository.java:140-141`), o que
  impede o uso de um índice. A tabela também não tem índice nenhum além da chave.

## Comportamento

**Números:** 6 de 72 conferências passaram. A parte comum passou 4 de 46, os dados depois de reiniciar 1 de 3, o
cenário 01 0 de 21, e as reservas simultâneas 1 de 2 (`measures/behavior.md`).
**Nota:** ruim

O serviço sobe, mas não tem mapeador de JSON. Falta o `jackson-databind` no `pom.xml`, e o log repete "It looks like
you don't have an object mapper configured" (`measures/behavior-service.log:22`). O resultado:

- `GET /rooms` e `POST /reservations` respondem 500, e todas as recusas (400, 404, 409, 422) também saem como 500.
- Nenhuma reserva é criada, e por isso confirmação, check-in, check-out, cancelamento, e-mail e segunda via falham em
  cascata.
- Os relatórios respondem em `text/csv`, mas sempre vazios (`total,0/5`, `total,0.00`).
- Os 21 itens do cenário (convênio, pacote, estadia longa) falham todos.
- As 6 conferências que passaram não provam nada: um cabeçalho de CSV, o `refund` nulo de uma resposta vazia, as
  mensagens (vazias) depois de reiniciar, e "nenhuma rodada com duas reservas criadas" (as duas deram 500).

Para o dono da pousada, isso quer dizer que o site não consegue listar quartos nem fazer uma reserva.

Mesmo com o Jackson, o código tem erros que o roteiro pegaria:

- A confirmação sem corpo grava 0 como pago (`Application.java:109-121`). O hóspede pagaria e o sistema registraria
  zero: a segunda via seria negada (`Application.java:172`), o financeiro não contaria a reserva
  (`ReservationRepository.java:142`) e o cancelamento não devolveria nada.
- O cancelamento depois do check-in é aceito (`ReservationService.java:143-144`).
- A ocupação sai com as datas em `dd/MM/yyyy` (`ReportService.java:42-43`).
- O dinheiro sai sem 2 casas quando termina em zero (DTOs em `double`).
- O serviço não sobe de novo depois da primeira reserva (contado na coluna do banco).

## Dúvidas

- A classe do pattern ficou entre `acerto` e `malfeito`. Preço, validação e devolução estão nos cinco planos, mas há
  um único `instanceof CorporateRatePlan` no cálculo do preço (`ReservationService.java:68`). Dei `malfeito`, porque é
  quem usa a estrutura perguntando qual é o caso, e de um jeito que o próximo plano com dado próprio vai repetir.
- O texto não diz como o hóspede "paga" na confirmação. O agente aceitou um `amountPaid` no corpo. Mas o texto diz que
  "o que ele pagou é o total da reserva", e o padrão de 0 quando o corpo não vem contraria isso. Contei como erro.
- O formato das datas no JSON do comprovante (`ReceiptResponse.java:28-29`, `dd/MM/yyyy`) não está fechado no texto
  para o comprovante. Não contei.
