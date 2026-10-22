# Revisão: construção, cenário 07

## Visão geral

O agente montou o projeto em sete pacotes: `api` (os controllers do Javalin, que registram as rotas), `service` (as
regras de reserva, o preço e os relatórios), `domain` (as entidades e os enums), `infrastructure.persistence` (JDBC
puro com `DriverManager`, com o esquema criado por `CREATE TABLE IF NOT EXISTS` dentro de `Database.java`),
`infrastructure.messaging` (o envio de e-mail, com uma lista de "políticas") e `util` (formatação, arredondamento,
temporada, validação de e-mail). Uma requisição entra num handler de `ReservationController`, que lê o JSON com
Gson, chama `ReservationService` (às vezes vai direto ao repositório) e responde com `ctx.json`. Os dados ficam no
PostgreSQL. O problema mais grave é que o sistema não funciona. As rotas usam a sintaxe `:id`, que o Javalin 6 recusa.
O `main` lança `IllegalArgumentException` em `api/ReservationController.java:68` (chamado de `Application.java:49`),
e por isso as reservas por id, os relatórios e as mensagens nunca são registrados. Além disso, o projeto não tem
Jackson, então todo `ctx.json` falha e as duas rotas que chegam a existir (`GET /rooms` e `POST /reservations`)
respondem 500. O roteiro de comportamento passou em 3 de 65 conferências. Mesmo respondendo 500, o `POST
/reservations` grava a reserva, e o banco aceitou duas reservas do mesmo quarto na mesma noite nas 10 rodadas
simultâneas.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece
em `api` e no pacote raiz, e o JDBC só em `infrastructure.persistence`. A maior classe é `ReservationController`, com
242 linhas.
**Nota:** com problemas

A direção geral está certa: a borda depende das regras, as regras dependem dos dados, não há ciclo, e a montagem fica
num lugar só (`Application.java:32-51`). As regras não conhecem nem o Javalin nem o SQL (`service/ReservationService.java`
não importa `io.javalin` nem `java.sql`).

As misturas ficam na borda:
- **Os controllers pulam o serviço e vão direto aos repositórios.** `ReservationController` recebe `ReservationRepository`
  e `RoomRepository` (`api/ReservationController.java:23-26`). Ele lê a reserva direto do banco em
  `api/ReservationController.java:71` e `:177`, e busca o quarto em `:145` e `:184`.
- **A regra da segunda via mora no handler, e está errada.** Ela só pode sair para reserva paga, mas o handler monta
  o comprovante sem conferir a situação (`api/ReservationController.java:174-199`).
- **O comprovante existe em dois lugares.** O handler monta a versão JSON duas vezes (`:146-158` e `:185-197`), e o
  serviço tem a versão texto (`service/ReservationService.java:164-188`).
- **Os códigos de erro saem do texto das mensagens de exceção.** Os handlers procuram trechos como `"Room not found"`
  ou `"not found"` na mensagem (`api/ReservationController.java:92`, `:230-240`), o que acopla a borda às mensagens
  escritas no serviço.
- **A combinação das regras de envio não é decidida no `main`.** O `main` só passa um `boolean` para o
  `MessageSender`, que cria as próprias políticas por dentro (`infrastructure/messaging/MessageSender.java:24-25`).
- **O pacote `infrastructure.messaging` guarda regra de negócio.** Lá ficam a validação do endereço e o modo de
  teste, e o pacote depende de `persistence`.

## Design de baixo nível

**Números:** o pacote de envio tem uma interface `MessagePolicy` com 2 implementações, a validação do endereço e o
modo de teste, aplicadas em lista por `MessageSender.applyPolicies` (`MessageSender.java:43-52`). Das três regras, 2
estão na estrutura. A terceira, a cópia para o arquivo, ficou fora, num `if` do próprio envio. A escolha pelo modo de
teste aparece em 2 lugares dentro das peças: `TestModePolicy.java:15` e `MessageSender.java:38`.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** ruim

**O pattern.** A lista de filtros aplicada pelo envio estaria perto da alternativa equivalente que a referência aceita.
Cada política é uma peça própria, com a mesma assinatura, aplicada em sequência, e o bloqueio interrompe a corrente
(`MessageSender.java:45-49`). Três coisas afastam o código disso e o põem em malfeito:
- **A cópia para o arquivo ficou fora da estrutura.** Ela é um `if` dentro do próprio envio
  (`MessageSender.java:38-40`, `sendArchiveCopy` em `:54-58`).
- **A combinação por modo está escrita dentro das peças, e não na montagem.** O envio pergunta
  `!testModeEnabled` para decidir a cópia (`MessageSender.java:38`). A `TestModePolicy` sempre entra na lista e
  pergunta por dentro se o modo está ligado (`TestModePolicy.java:15-17`).
- **A lista é montada no construtor do `MessageSender`** (`MessageSender.java:22-25`), e não no `main`. Uma segunda
  pousada sem a cópia, ou uma regra de madrugada que valha "só fora do teste", exigiria mudar o `MessageSender`.

Também há um defeito de conteúdo: a mensagem bloqueada perde o assunto e o texto, gravados como `""`
(`MessagePolicy.java:20-22`).

**O exagero.** Não há estrutura sem necessidade fora do pattern. O único resto é o `addPolicy`, que ninguém chama
(`MessageSender.java:60-62`), e isso não chega a ser uma camada. Por isso, contenção.

**O resto do código.** Os defeitos se espalham pelo projeto:
- **Dinheiro em `double` no domínio inteiro:** `Reservation.java:16-21`, `RoomType.java:10`, `PriceCalculator.java:12-14`
  e `:38`, `SeasonCalculator.java:28`. O arredondamento passa por `new BigDecimal(double)` (`MoneyRounder.java:10`).
  O desconto de 15% sobre 249,90 é exatamente 37,485, que "meio para o par" levaria a 37,48. Esse caso só sai certo
  porque o `double` guarda 37,48499…; o "meio para o par" pedido nunca é aplicado sobre o valor exato.
- **A mesma reconstrução repetida quatro vezes.** A reserva tem um construtor de 16 argumentos, copiado para cada
  transição de situação (`ReservationService.java:69-76`, `:108-115`, `:130-137`, `:152-159`).
- **O mesmo bloco de `catch` copiado** em quatro handlers (`ReservationController.java:91-101`, `:109-119`,
  `:127-137`, `:161-171`).
- **Os erros saem do texto das mensagens de exceção** (`ReservationController.java:230-240`).
- **`catch (Exception)` que esconde a causa.** Qualquer falha vira 400 `INVALID_REQUEST`
  (`ReservationController.java:63-64`, `ReportController.java:52-53`), inclusive uma falha de banco.
- **Campos `public` no resultado do preço** (`PriceCalculator.java:11-14`).
- **Campos Gson que ninguém usa** (`Application.java:16`, `RoomController.java:14`).
- **O total de quartos fixo em `"/5"`** no relatório de ocupação (`ReportService.java:37`).
- **O valor em dinheiro formatado com o locale `pt-BR`.** O espaço depois de "R$" é um U+00A0 e não o espaço comum
  do modelo pedido (`MoneyFormatter.java:7-10`; conferido com `jshell`).

## Testes

**Números:** rodaram 5 testes, com 0 falhas, 0 erros e 0 pulados. O script conta "0 métodos de teste escritos"
porque os testes estão no estilo JUnit 3 (`extends TestCase`, sem `@Test`): são 5 métodos em 3 arquivos. A cobertura
é de 3,2% das linhas (27 de 854) e 10,4% dos ramos (24 de 230), toda no pacote `util`. Os pacotes `api`, `service`,
`domain`, `persistence` e `messaging` ficam em 0% (`measures/tests.md`).
**Nota:** ruim

A coluna está quase ausente. Os três arquivos testam só utilitários:
- `EmailValidatorTest.java:6-19`, que confere a regra do endereço, com valores;
- `MoneyRounderTest.java:6-11`;
- `SeasonCalculatorTest.java:6-31`, que confere as bordas da temporada, inclusive 14/12 e 15/12.

Ficam sem nenhum teste:
- o preço completo (o exemplo de R$ 599,76 e o desconto do NON_REFUNDABLE);
- as transições da reserva;
- o cancelamento e a devolução;
- os códigos de erro;
- os relatórios;
- a parte nova do cenário: a cópia para o arquivo, o desvio do modo de teste e o `BLOCKED`.

Pela pergunta da mutação: se alguém apagasse a cópia para o arquivo (`MessageSender.java:38-40`), trocasse `"[TESTE] "`
ou o endereço de testes (`TestModePolicy.java:4-5`), ou tirasse o `if (result.blocked)` (`MessageSender.java:47`),
nenhum teste falharia. Nenhum teste sobe o Javalin, e por isso o `mvn verify` passa
(`measures/tests-maven.log`, BUILD SUCCESS) com um serviço que cai ao registrar as rotas e não serializa JSON.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram:
- 1 chave estrangeira sem índice (`reservations.room_number`);
- 5 colunas de situação ou tipo sem `CHECK`;
- nenhuma restrição que proteja uma regra (UNIQUE, EXCLUDE ou CHECK);
- nenhum pool de conexões;
- 0 transações explícitas e 0 `FOR UPDATE`.

O esquema é criado no código, sem migrações (`measures/database.md`).
**Nota:** ruim

O que está certo:
- os tipos das colunas (`Database.java:81-86`);
- a chave estrangeira da reserva para o quarto (`Database.java:87`);
- o SQL sempre com parâmetros `?` (o script não achou SQL montado com `+`);
- as conexões fechadas por try-with-resources.

O defeito sério: nada impede duas reservas do mesmo quarto na mesma noite. O serviço consulta os conflitos e depois
insere, em conexões separadas e sem transação, trava ou restrição (`ReservationService.java:41-56`,
`ReservationRepository.java:114-140`). Depois do roteiro, o banco `review_behavior_4` tinha as 10 rodadas simultâneas
gravadas em dobro: duas reservas `PENDING` para o quarto 301 em cada noite, de 2030-02-02 a 2030-02-20. A conferência
"nenhuma com as duas criadas" só passou porque a API respondeu 500 às duas.

Os outros defeitos:
- **O esquema não evolui.** É `CREATE TABLE IF NOT EXISTS` no construtor de `Database` (`Database.java:21-109`), que
  não sabe acrescentar uma coluna.
- **Cada consulta abre uma conexão nova** por `DriverManager` (`Database.java:17-19`), sem pool.
- **A chave estrangeira não tem índice.**
- **As situações e os planos são texto livre, sem `CHECK`.**
- **O `double` do domínio vai e volta do `numeric`** por `BigDecimal.valueOf`/`doubleValue`
  (`ReservationRepository.java:48-54`, `:207-214`).
- **Um insert vira um segundo select** (`findById`) logo depois (`ReservationRepository.java:59-63`).

## Comportamento

**Números:** 3 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 1 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário, modo normal | 0 de 9 |
| cenário, modo de teste | 0 de 5 |
| duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** ruim

O serviço sobe a porta, mas o `main` cai no meio da montagem das rotas. O log mostra `IllegalArgumentException: Path
'/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'` (`measures/behavior-service.log:21`,
`ReservationController.java:68`). Por isso:
- todas as rotas `/reservations/{id}/...`, `/reports/...` e `/messages` respondem 404 "Endpoint not found";
- `GET /rooms` e `POST /reservations` existem, mas respondem 500, porque não há Jackson no `pom.xml` e o Javalin
  responde "It looks like you don't have an object mapper configured".

As 3 conferências que passaram não provam regra nenhuma:
- "refund null" passou porque o campo não veio;
- "as mensagens continuam lá" passou com a lista vazia;
- "nenhuma com as duas criadas" passou porque as duas receberam 500, e no banco as duas foram gravadas (veja a seção
  Banco).

Para o dono da pousada, isso quer dizer:
- o site não consegue listar os quartos;
- o site recebe erro ao reservar, embora a reserva fique gravada, e o hóspede que tenta de novo cria outra;
- não há como confirmar, fazer o check-in ou o check-out, cancelar, tirar os relatórios nem ver as mensagens;
- nenhuma das regras de e-mail do cenário pode ser usada.

Pela leitura, mesmo com as rotas consertadas, sobrariam outros desvios:
- a segunda via sairia para reserva pendente (`ReservationController.java:174-199`);
- a mensagem bloqueada ficaria sem assunto e sem texto (`MessagePolicy.java:20-22`);
- o relatório financeiro listaria canceladas que não deixaram dinheiro (`ReservationRepository.java:175`) e terminaria
  em `total,,,,,` em vez de `total,` (`ReportService.java:65`);
- o comprovante teria um espaço U+00A0 depois de "R$".

## Dúvidas

- **A nota do banco ficou entre `com problemas` e `ruim`.** Os tipos e as chaves estão certos, mas a ausência de
  qualquer proteção contra a reserva dupla deixou duplicatas reais no banco nas 10 rodadas. Esse risco de dado errado
  pesou mais.
- **A classe do design ficou entre `malfeito` e `omissão`.** A estrutura de políticas existe e cobre duas das três
  regras, e por isso não é omissão. Mas a cópia ficou fora, e o modo é decidido dentro das peças.
- **Como o serviço não funciona pela API, o comportamento das regras de envio não pôde ser observado.** A leitura do
  código sugere que a validação do endereço, o desvio do modo de teste e a cópia funcionariam no geral, com os
  desvios citados acima.
