# Revisão: construção, cenário 07

## Visão geral

O projeto tem seis pacotes: `pousada` (só o `Main`), `web`, `service`, `domain`, `messaging` e `persistence`. Uma
requisição entra em `web/Routes.java`, que lê o corpo (`web/ReservationRequestParser.java`) e chama
`service/ReservationService.java` ou `service/ReportService.java`. As regras de preço e de situação da reserva ficam
no domínio (`domain/Pricing.java`, `domain/RatePlan.java`, `domain/Reservation.java:26-44`), e o SQL fica nos
repositórios de `persistence/`, com JDBC puro e uma transação por chamada (`persistence/Database.java:24-38`). Os dados
ficam no PostgreSQL, em três tabelas que o próprio código cria ao subir (`persistence/Schema.java:4-44`). As regras de
envio de e-mail formam uma pilha de embrulhos sobre `MessageSink`, montada conforme o modo em `Main.java:31`. O
roteiro do comportamento passou inteiro (65 de 65) e os 11 testes passam. Os pontos mais sérios ficam no banco: o
esquema é criado no código com `CREATE TABLE IF NOT EXISTS` e não tem migração, não há índice na chave estrangeira,
não há `CHECK` e não há pool. Há também um problema de atomicidade: a confirmação da reserva e o e-mail são gravados
em transações separadas.

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `pousada` e `pousada.web`, e o JDBC só em `pousada.persistence`. A maior classe é `ReservationRepository` (153 linhas).
**Nota:** bom

- **Borda HTTP fina:** cada rota é uma linha que lê o parâmetro, chama o serviço e escreve a resposta
  (`web/Routes.java:54-65`). A validação do corpo fica separada em `web/ReservationRequestParser.java:17-36`, e os
  erros viram JSON num lugar só (`web/Routes.java:47-52`).
- **Regras sem Javalin e sem SQL:** `service/ReservationService.java` não importa Javalin nem `java.sql`. O preço, os
  planos e as transições de situação ficam no domínio (`domain/Pricing.java:17-37`, `domain/RatePlan.java:17-23`,
  `domain/Reservation.java:26-69`). As regras de mensagem (`messaging/`) não dependem de nenhum outro pacote do
  projeto.
- **Acesso ao banco isolado** nos três repositórios e em `Database`.
- **Montagem num lugar só:** `Main.java:25-35` cria os repositórios, a pilha de mensagens, os serviços e as rotas. Não
  há `static` mutável nem `new` de repositório dentro de serviço.
- **Ressalvas pequenas, que não mudam a nota:**
  - `GET /messages` pula a camada de serviço: `web/Routes.java:21,30,63` usa `MessageRepository` direto, e é daí que
    vem a dependência `web -> persistence`.
  - `domain/ApiException.java:7-39` guarda os códigos HTTP (400, 404, 409, 422) dentro do domínio.

## Design de baixo nível

**Números:** 3 de 3 regras de envio são embrulhos de `MessageSink` (`ArchiveCopy`, `BlockInvalidAddress`, `TestRedirect`). Nenhuma das três fica fora da estrutura, e nenhum embrulho pergunta pelo modo. O modo é decidido num ponto só, na montagem (`MessagePipeline.assemble` com a lista de `MareAltaRules`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern pedido (Decorator).**

- **A estrutura:** `messaging/MessageSink.java:4-5` é a interface do envio, e o destino real é
  `persistence/MessageRepository.java:12-37`, que grava na lista. Cada regra é um embrulho com a mesma interface, que
  chama o próximo:
  - `messaging/BlockInvalidAddress.java:14-19` deixa passar o endereço válido e grava o inválido como `BLOCKED`, com o
    endereço original;
  - `messaging/ArchiveCopy.java:14-20` repassa a mensagem e, se ela saiu `SENT`, manda a cópia com "[Cópia] ";
  - `messaging/TestRedirect.java:14-19` troca o destinatário, o assunto e a primeira linha do texto.
- **A combinação por modo fica fora dos embrulhos.** Cada regra é declarada com o modo em que vale (`ALWAYS`,
  `TEST_ONLY`, `OUTSIDE_TEST_ONLY`) numa lista de fora para dentro (`messaging/MareAltaRules.java:20-25`).
  `messaging/MessagePipeline.java:30-39` empilha só as que valem no modo, e `Main.java:20,31` decide o modo a partir
  de `HOTEL_TEST_MODE`. Nenhum embrulho e nenhum ponto de chamada pergunta pelo modo. O envio só acontece em
  `service/ReservationService.java:53`, sem `if`.
- **A ordem da pilha resolve as interações que o texto pede:**
  - o bloqueio fica fora do desvio de teste, então a mensagem bloqueada guarda o endereço original;
  - a cópia fica por fora do bloqueio, então a mensagem bloqueada não gera cópia.
- **As regras futuras cabem na estrutura.** Madrugada e limite por dia podem valer sempre, só no teste ou só fora
  dele: cada uma vira mais um embrulho e mais uma linha na lista. A "segunda pousada sem cópia" é outra lista, como
  mostra `MessagePipelineTest.java:81-85`.
- **O que afasta um pouco:** `BlockInvalidAddress` recebe o destino final como segundo parâmetro
  (`messaging/BlockInvalidAddress.java:8-11`). Assim, a mensagem bloqueada pula os embrulhos de dentro. Isso é
  intencional e está certo para as regras atuais, mas acopla esse embrulho ao destino.

**O exagero.** `MessagePipeline.Rule`/`Applies` é a forma de decidir a combinação na montagem e faz parte do pattern,
não é estrutura a mais. Fora dele não há interface com uma implementação só, fábrica, builder nem camada que só
repassa. `Database.Work` (`persistence/Database.java:8-11`) é só o tipo do lambda da transação. Por isso, contenção.

**O resto do código.**

- **O que está bem:**
  - dinheiro em `BigDecimal` com `HALF_EVEN` em cada passo (`domain/Money.java:16-18`, `domain/Pricing.java:29`,
    `domain/RatePlan.java:18`);
  - datas em `LocalDate`, e situação, plano e tipo em `enum`;
  - `Reservation` imutável, com as transições em métodos que conferem a situação (`domain/Reservation.java:26-44`,
    `62-70`);
  - sem `static` mutável e sem exceção engolida (o `catch (Exception)` de `web/Routes.java:49-52` imprime a pilha e
    responde 500).
- **Pontos menores:**
  - `ReservationService.apply` usa um parâmetro booleano para escolher entre ler e gravar, e `get` passa uma mudança
    que não muda nada (`service/ReservationService.java:47-49`, `75-84`).
  - `Message.email` nasce com `status` `SENT` antes de passar pela pilha (`messaging/Message.java:5-6`).
  - Confirmar a reserva e mandar o e-mail não são atômicos (`service/ReservationService.java:52-53`). Esse ponto está
    contado no banco.

Nenhum desses pontos torna o código difícil de mudar.

## Testes

**Números:** 11 testes rodaram, nenhum falhou, nenhum foi pulado; 11 métodos escritos em 3 classes. Cobertura de 91,0% das linhas (385/423) e de 77,0% dos ramos (87/113). Por pacote: `messaging` 58/59 linhas e 24/25 ramos, `domain` 76/77 linhas, `service` 56/58 linhas, `web` 10/20 ramos, `pousada` 7/16 linhas.
**Nota:** bom

- **A parte nova está bem testada, com valores** (`MessagePipelineTest.java`):
  - a validação do endereço, inclusive os casos de borda "ana@.com" e "ana@gmail." (`:31-40`);
  - a cópia com o assunto e o texto (`:43-50`);
  - o bloqueio sem cópia, com o endereço original (`:53-58`);
  - o modo de teste com o endereço, o "[TESTE] " e a linha "Destinatário original: …" exatos (`:61-69`);
  - o bloqueio no modo de teste com o endereço original (`:72-78`);
  - uma instalação sem a cópia (`:81-85`).
- **A pergunta da mutação na parte nova:**
  - tirar a conferência `SENT` de `ArchiveCopy` faria `:53-58` falhar (2 mensagens em vez de 1);
  - inverter a ordem bloqueio/desvio faria `:72-78` falhar;
  - trocar `dot > 0` por `dot >= 0` em `EmailAddress` faria `:38` falhar.
- **A parte comum:**
  - o preço com a temporada e as fronteiras (14/12, 15/12, 28/02, 01/03) e o meio-para-o-par do desconto
    (`PricingTest.java:19-42`);
  - o fluxo completo pela API, contra o PostgreSQL real, num schema descartável (`ApiIntegrationTest.java:33-46`): os
    códigos 409, 422, 400 e 404, o texto do comprovante no e-mail e os dois relatórios (`ApiIntegrationTest.java:72-120`);
  - o financeiro com a não reembolsável cancelada (`ApiIntegrationTest.java:123-136`).
- **O que fica sem rede:**
  - A devolução do plano STANDARD não é conferida em lugar nenhum. Trocar `refundable ? amountPaid : Money.ZERO` por
    `Money.ZERO` (`domain/RatePlan.java:22`) não derruba nenhum teste.
  - O cancelamento de reserva pendente não é testado.
  - Nenhum teste confere as linhas "1 diária" e "Desconto:" do comprovante (`domain/Receipt.java:36,38-40`).
  - Ninguém lê `HOTEL_TEST_MODE` pelo `Main` (`Main.java:18-23` sem cobertura).
  - Algumas asserções são fracas: `ApiIntegrationTest.java:135` só procura `"BLOCKED"` no texto inteiro da lista.
  - Sem `DB_URL`, o teste de integração é pulado em silêncio (`ApiIntegrationTest.java:36`).

São lacunas pontuais. O núcleo das regras e a parte nova estão conferidos com valores.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; 6 colunas de dinheiro, todas `numeric(12,2)`; datas em `date`. As conferências acusaram uma chave estrangeira sem índice (`reservations.room_number -> rooms`), 5 colunas de situação ou tipo sem `CHECK`, nenhuma restrição `UNIQUE`/`EXCLUDE`/`CHECK`, nenhum pool (1 `DriverManager.getConnection`), nenhum SQL montado com dado de fora e 2 `FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

- **O esquema não evolui.**
  - Ele é um texto com `CREATE TABLE IF NOT EXISTS` em `persistence/Schema.java:4-44`, executado a cada subida por
    `persistence/Database.java:41-63`, sem nenhuma migração versionada.
  - O próximo pedido que acrescentar uma coluna não terá como alterar uma tabela que já existe.
  - A carga dos quartos usa `ON CONFLICT DO NOTHING` (`persistence/Schema.java:11-17`), então uma mudança de diária
    nunca chegaria a um banco já criado.
- **Restrições ausentes:**
  - nenhum índice em `reservations.room_number` (`persistence/Schema.java:20`), e é por essa coluna que a consulta de
    sobreposição filtra (`persistence/ReservationRepository.java:32-35`);
  - nenhum `CHECK` em `status`, `rate_plan`, `guests >= 1` ou `check_out > check_in` (`persistence/Schema.java:26-34`).
    Essas regras só existem no código.
- **A sobreposição de reservas está bem protegida.** A transação trava a linha do quarto com
  `SELECT ... FOR UPDATE` (`persistence/ReservationRepository.java:28-31`) antes de conferir a sobreposição e inserir.
  A trava vale entre processos, e o roteiro confirmou isso nas 10 rodadas simultâneas. Não há, porém, um `EXCLUDE` que
  garanta a regra no próprio banco.
- **Escritas que deveriam andar juntas não andam:**
  - A confirmação grava a reserva numa transação (`persistence/ReservationRepository.java:54-71`) e o e-mail em outra
    (`persistence/MessageRepository.java:20-37`), chamadas em sequência em `service/ReservationService.java:52-53`. Se
    a gravação da mensagem falhar, a reserva fica `CONFIRMED`, sem e-mail, e o cliente recebe 500.
  - A original e a cópia para o arquivo também são gravadas em transações separadas.
- **Conexões:** cada operação abre uma conexão nova com `DriverManager` (`persistence/Database.java:25`), sem pool.
  Elas são fechadas corretamente com try-with-resources.
- **O que está certo:**
  - o SQL usa sempre parâmetros `?`. A única concatenação é a constante `" FOR UPDATE"`
    (`persistence/ReservationRepository.java:106`);
  - as atualizações de situação travam a linha (`persistence/ReservationRepository.java:54-71`);
  - os relatórios filtram no SQL e não em Java (`persistence/ReservationRepository.java:74-87`).

## Comportamento

**Números:** 65 de 65 conferências passaram, nenhuma falhou: 46/46 da parte comum, 3/3 depois de reiniciar, 9/9 do cenário no modo normal, 5/5 do cenário no modo de teste e 2/2 das reservas simultâneas (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, tudo o que o roteiro confere funciona como o texto descreve:

- os preços com a alta temporada e o desconto arredondado meio para o par;
- as recusas, com os códigos certos;
- a confirmação com o e-mail e o comprovante;
- as devoluções nos dois planos e na reserva pendente;
- os dois relatórios em CSV;
- os dados que continuam lá depois de reiniciar;
- as duas reservas simultâneas, em que nunca entram as duas.

A parte nova também funciona nos dois modos:

- o endereço inválido fica `BLOCKED`, com o endereço original e sem cópia;
- a cópia para o arquivo vem logo depois da original, com "[Cópia] " no assunto;
- com `HOTEL_TEST_MODE=true`, tudo vai para a caixa de testes, com "[TESTE] " no assunto e a linha
  "Destinatário original:", sem cópia.

A falta de atomicidade entre a confirmação e o e-mail (contada no banco) não aparece em nenhuma chamada do roteiro.

## Dúvidas

- **Testes:** dei `bom`, mas a devolução do plano STANDARD e o cancelamento de reserva pendente não têm teste. Um
  revisor mais exigente poderia pedir esses testes e dar `com problemas`.
- **Design:** o parâmetro booleano de `ReservationService.apply` e os códigos HTTP dentro de `domain/ApiException`
  são pequenos. Pesei que não bastam para tirar o `bom`.
