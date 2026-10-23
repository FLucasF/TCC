# Revisão: construção, cenário 07

## Visão geral

O projeto tem cinco pacotes: `api` (controllers do Javalin), `service` (preço, situação da reserva, relatórios e regras de envio), `infrastructure` (JDBC puro com `DriverManager`), `domain` e a classe `Application`, que monta as peças. Uma requisição entra pelas rotas de `Application.java:50-60` e vai para o controller. O controller valida, chama os repositórios e o serviço e monta o JSON com Gson. Os dados ficam em três tabelas, criadas com `CREATE TABLE IF NOT EXISTS` em `Database.java:23-67`.

O defeito mais sério é que **o sistema não responde a nenhuma chamada em JSON**. Os controllers montam objetos do Gson e chamam `ctx.json(...)` (por exemplo, `ReservationController.java:127`, `RoomController.java:30`). Só que o `pom.xml` não traz o Jackson nem configura outro mapeador JSON. O Javalin responde 500 em todas essas rotas, com a mensagem "It looks like you don't have an object mapper configured" (`measures/behavior-service.log`, e eu reproduzi com `curl` em `GET /rooms`). Com o banco intacto, a reserva até é gravada e confirmada, mas o site recebe 500.

Há outros defeitos sérios:
- a coluna `guest_cpf VARCHAR(11)` (`Database.java:40`) recusa o CPF com pontuação, e a criação da reserva cai com 500;
- o cancelamento de uma reserva NON_REFUNDABLE confirmada devolve tudo o que foi pago (`ConfirmedState.java:26`);
- o SQL é montado com texto que vem de fora;
- não há nenhum teste.

## Arquitetura

**Números:** 5 pacotes (`com.pousada.marealta` com 1 classe e 65 linhas; `api` com 4 e 530; `domain` com 8 e 386; `infrastructure` com 4 e 370; `service` com 25 e 643), 9 dependências entre eles e nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em `api` e na raiz, e o JDBC só em `infrastructure`. A maior classe é `ReservationController` (410 linhas), mais que o dobro da segunda maior (`ReservationRepository`, 168).
**Nota:** com problemas

O que está certo:
- as dependências apontam no sentido certo (`api` → `service` → `infrastructure` → `domain`) e não há ciclo;
- as regras de preço (`PriceCalculator`, `*PricingStrategy`) e de situação (`*State`) não conhecem o Javalin nem o SQL;
- o acesso ao banco está isolado nos três repositórios.

O problema está na borda HTTP, que carrega o caso de uso inteiro da reserva:
- `ReservationController.java:139-189` valida a capacidade e as datas, consulta a disponibilidade, escolhe o plano de tarifa, calcula o preço e monta a `Reservation` campo a campo;
- `ReservationController.java:351` decide quando existe a segunda via;
- `ReservationController.java:363-418` compõe o e-mail e o texto do comprovante;
- cada handler busca a reserva, pede a transição ao `ReservationStateMachine` e grava (`:224-229`, `:258-263`, …), sem um serviço de reservas no meio.

Os controllers falam direto com os repositórios (`api -> infrastructure`).

A montagem também está espalhada:
- `Application.java:25-42` liga as peças;
- `MessageService.java:21-26` cria e encadeia as regras de envio a cada mensagem;
- `ReportController.java:32` e `:48` criam os relatórios a cada requisição;
- `ReservationController.java:166-169` faz `new` da estratégia de preço.

Não chega a ser `ruim`, porque as camadas existem e não há ciclo, mas o controller de 410 linhas é o ponto onde o próximo pedido vai doer.

## Design de baixo nível

**Números:** as 3 regras de envio estão em processadores encadeados (`EmailValidationProcessor`, `TestModeProcessor`, `ArchiveProcessor`). Mas 2 delas decidem o modo por dentro (`TestModeProcessor.java:15` e `ArchiveProcessor.java:16`), e a corrente é sempre a mesma, montada dentro de `MessageService.java:21-26`. Nada é decidido na montagem: `Application.java:36` só passa dois booleanos.
**Classe:** malfeito
**Exagero:** exagero
**Nota:** ruim

**O pattern.** O agente escolheu uma corrente de regras (Chain of Responsibility), que a referência aceita como alternativa equivalente desde que a combinação por modo seja decidida na montagem. Aqui não é.

O que aproxima:
- cada regra é uma peça própria com a mesma interface (`MessageProcessor.java:5-6`), e cada peça chama a próxima (`EmailValidationProcessor.java:20-21`);
- o bloqueio vem primeiro, então a mensagem bloqueada não gera cópia e mantém o endereço original. Pelo banco, isso funciona.

O que afasta, e decide a classe:
- **a combinação por modo está escrita dentro das regras.** A cópia pergunta `context.isCopyToArchiveEnabled() && !context.isTestMode()` (`ArchiveProcessor.java:16`), e o desvio pergunta `context.isTestMode()` (`TestModeProcessor.java:15`). É exatamente o caso de "malfeito" da referência: a cópia que verifica se está em modo de teste;
- a corrente é fixa, criada por dentro de `MessageService.sendAndStoreMessage` (`MessageService.java:21-26`). Uma regra nova ("não mandar de madrugada só fora do teste") exige mexer no `MessageService` e repetir o `if` de modo dentro da regra;
- a "segunda pousada sem cópia" virou mais um booleano global (`COPY_TO_ARCHIVE`, `Application.java:23`), carregado pelo `MessageProcessingContext`;
- o `MessageProcessingChain.java:22-27` reescreve a situação depois da corrente, e o `ProcessingResult.Status.CONTINUE` (`MessageProcessor.java:10`) nunca é usado.

**O exagero.** Fora da parte das mensagens, o agente acrescentou estrutura que o texto não pede:
- **um State completo** para a reserva: uma interface, cinco classes, quase todas só com `throw`, e um registro estático em `ReservationStateMachine.java:6-19`, onde bastava uma tabela de transições;
- **um Strategy de preço com um `PriceCalculator` em volta** (`PriceCalculator.java:9-13`). As duas estratégias repetem a mesma regra da alta temporada (`StandardPricingStrategy.java:8-16` e `NonRefundablePricingStrategy.java:8-17`), e quem escolhe a estratégia ainda é um `if` no controller (`ReservationController.java:166-168`).

**O resto do código.** Os defeitos estão espalhados:
- **Duplicação:**
  - o bloco "reserva não encontrada" aparece seis vezes (`ReservationController.java:199-206`, `215-222`, `249-256`, `280-287`, `311-318`, `342-349`);
  - o `catch` de `OPERATION_NOT_ALLOWED`, quatro vezes;
  - a busca do tipo do quarto, três (`:364-365`, `:386-387` e `OccupancyReport.java:28-29`), com a variável `roomType` de `:365` nunca usada.
- **Erros:**
  - o único tratador transforma qualquer exceção em 500 sem corpo (`Application.java:45-48`). Corpo que não é JSON, data fora do formato, plano desconhecido (`RatePlan.valueOf`, `ReservationController.java:181`) e mês inválido (`ReportController.java:47`) viram 500 em vez de 400;
  - zero hóspedes responde `CAPACITY_EXCEEDED` (`ReservationController.java:139`), e o texto pede 400.
- **Regras erradas:**
  - o cancelamento de uma confirmada devolve o pago sem olhar o plano (`ConfirmedState.java:26`). Reproduzi: uma NON_REFUNDABLE confirmada e cancelada ficou com `refund` 591.50;
  - a segunda via usa `amountPaid > 0` (`ReservationController.java:351`), então uma reserva cancelada que tinha sido paga também tem segunda via;
  - o dinheiro do comprovante sai sem separador de milhar e depende da locale (`ReservationController.java:420-422`), quando o texto pede "R$ 1.234,56".
- **Estado:** campos públicos e mutáveis em `PriceCalculator.java:57-61` e `MessageProcessor.java:15-16`, e os objetos de estado alteram a `Reservation` recebida.

O que está certo: `BigDecimal` com `HALF_EVEN` a cada passo (`PriceCalculator.java:15-16`, `47-52`) e `LocalDate` para as datas.

## Testes

**Números:** 0 testes rodaram (0 falhas, 0 erros); 0 métodos de teste escritos; 0 arquivos em `src/test` (a pasta `src/test/java/com/pousada/marealta` existe, mas está vazia). Não há cobertura, porque nenhum teste rodou (`measures/tests.md`). O `mvn verify` passa (`BUILD SUCCESS`), mas com "No tests to run" (`measures/tests-maven.log`).
**Nota:** ruim

O `pom.xml` declara o JUnit 4 (`pom.xml:52-57`), mas o agente não escreveu nenhum teste. Nenhuma regra está protegida:
- o preço com a temporada e o meio para o par;
- os planos e as devoluções;
- as transições de situação e os códigos de erro;
- a parte do cenário: o bloqueio do endereço, a cópia para o arquivo e o desvio no modo de teste.

Um único teste pela API teria mostrado que todas as respostas JSON dão 500. Pela pergunta da mutação: trocar `!context.isTestMode()` em `ArchiveProcessor.java:16`, ou remover a checagem do ponto em `EmailValidator.java:40`, não derruba nada.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)` (6 colunas, ok); datas em `date`. As conferências acusaram (`measures/database.md`):
- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhuma restrição (UNIQUE, EXCLUDE ou CHECK) e nenhum CHECK nas colunas de situação, plano, tipo e canal;
- 0 transações explícitas, 0 `FOR UPDATE` e nenhum pool (`DriverManager` a cada chamada);
- o esquema criado no código, sem migração.
**Nota:** ruim

**SQL montado com dado de fora.** O nome, o e-mail e o CPF do hóspede, e o assunto e o texto das mensagens, entram no SQL por `String.formatted`, só com a troca de `'` por `''` (`ReservationRepository.java:25-42`, `MessageRepository.java:20-28`, com `escape` em `ReservationRepository.java:165-167` e `MessageRepository.java:65-67`). Nenhuma consulta usa `PreparedStatement`.

**A coluna do CPF quebra a criação.** `guest_cpf VARCHAR(11)` (`Database.java:40`) recusa o CPF no formato `111.222.333-44`. O log do serviço mostra 20 `PSQLException: value too long for type character varying(11)` durante o roteiro, e esse erro sozinho já faz o `POST /reservations` responder 500.

**Nenhuma proteção contra reserva dupla.** A disponibilidade é um `SELECT COUNT(*)` (`ReservationRepository.java:85-101`) seguido de um `INSERT` em outra conexão (`ReservationRepository.java:24-53`), sem transação, sem trava e sem restrição de exclusão. Duas reservas simultâneas podem passar. O roteiro não conseguiu provar isso, porque todas as tentativas deram 500.

**Duas escritas sem transação.** A confirmação grava a reserva (`ReservationController.java:229`) e depois as mensagens (`:232`, `MessageRepository.java:36-40`), cada uma em sua conexão.

**O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`Database.java:26-64`) não acrescenta coluna numa tabela que já existe.

## Comportamento

**Números:** 5 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 3 de 46 |
| dados depois de reiniciar | 1 de 3 |
| cenário, modo normal | 0 de 9 |
| cenário, modo de teste | 0 de 5 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

O serviço sobe e cria o banco, mas para o site ele não funciona.

**As rotas em JSON quebram.** Todas as rotas que devolvem JSON (quartos, reservas, confirmação, check-in, check-out, segunda via, mensagens e todos os erros com `{error, message}`) respondem 500, porque falta o mapeador JSON (ver a Visão geral). Em cima disso, o CPF com pontuação faz o `INSERT` da reserva falhar.

**O que passou:** só os cabeçalhos dos dois relatórios em CSV, "as mensagens continuam lá" (passou com a lista vazia) e "nenhuma rodada com as duas reservas criadas", que passou porque nenhuma reserva foi criada.

**As regras de envio, pelo banco.** Chamei a API à mão com um CPF só de dígitos e conferi as tabelas:
- a reserva é gravada com o preço certo (599.76 para 10 a 12/07);
- a confirmação grava a mensagem `SENT` e a cópia `[Cópia] …` logo depois;
- `ana@gmail` fica `BLOCKED`, com o endereço original e sem cópia;
- no modo de teste, a mensagem vai para `testes@…` com `[TESTE] ` e a primeira linha "Destinatário original: …".

A lógica das regras de envio existe. Mas o dono da pousada nunca a vê pela API, porque o `GET /messages` também dá 500.

**Erros que apareceram nas chamadas à mão** e que o roteiro não alcançou:
- o relatório de ocupação conta reserva PENDING (`ReservationRepository.java:107` filtra só `!= 'CANCELLED'`), quando o texto fala em "reservas pagas";
- o financeiro deixa de fora a NON_REFUNDABLE cancelada (`ReservationRepository.java:129`), que o texto manda continuar contando;
- a NON_REFUNDABLE cancelada devolve tudo (`ConfirmedState.java:26`);
- mês fora do formato (`2027-13`) dá 500.

Para o dono da pousada: o site não consegue listar quartos, ver uma reserva nem ler a resposta de uma confirmação. Os relatórios contariam reservas não pagas e perderiam dinheiro que ficou com a pousada.

## Dúvidas

- A linha de total do financeiro sai como `total,,,,599.76` (`RevenueReport.java:37`), e o roteiro espera `total,599.76`. O texto só mostra o formato `total,2/5` no relatório de ocupação, então esse ponto estava aberto. Não pesou, porque o relatório já falha por outros motivos.
- O formato do CPF de entrada não é fixado pelo texto. Mesmo assim, recusar `111.222.333-44` com 500 (e não com 400) é defeito em qualquer leitura.
- No exagero, o State da reserva e o Strategy de preço são estruturas defensáveis em si. Marquei `exagero` pelo conjunto: as duas foram acrescentadas sem que o texto pedisse, com a regra da temporada repetida nas estratégias e a escolha da estratégia ainda feita por um `if`.
- O modo de teste põe uma linha em branco depois de "Destinatário original: …" (`TestModeProcessor.java:19`), e o e-mail tem uma linha em branco a mais antes de "A entrada é…" (`ReservationController.java:375-377`, com o `\n` final do comprovante). O texto não fixa isso com exatidão.
