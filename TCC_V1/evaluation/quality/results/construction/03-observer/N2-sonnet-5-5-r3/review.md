# Revisão: construção, cenário 03

## Visão geral

O projeto tem seis pacotes: `web` (as rotas do Javalin), `service` (as regras que pedem transação e os relatórios),
`domain` (a reserva e o seu ciclo de vida, o preço, os planos, o comprovante e os quartos), `events` (os avisos),
`persistence` (o JDBC puro) e a raiz, com a `App`, que monta tudo. Uma requisição entra em `Routes`, que lê o JSON e o
valida, chama `ReservationService`, que abre uma transação (`Database.transaction`, com a conexão guardada num
`ThreadLocal`), carrega a reserva com `FOR UPDATE`, chama o método da `Reservation` que muda a situação, grava e
publica um `ReservationEvent` para a lista de `ReservationListener` que recebe da montagem. Os dados ficam no
PostgreSQL, em duas tabelas (`reservations` e `messages`) criadas com `CREATE TABLE IF NOT EXISTS` no código. Os
quartos ficam numa lista fixa no código (`RoomCatalog`). O roteiro do comportamento passou inteiro (66 de 66) e o
Observer está bem feito. Não há nada sério. Os defeitos que achei são menores: o esquema não tem migração nem
restrição nenhuma, não há pool de conexões, e os testes não conferem parte do texto dos avisos novos. Além disso, o
`pom.xml` impede que a cobertura seja medida.

## Arquitetura

**Números:** 6 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em
`web` e na raiz, o JDBC só em `persistence` e o Jackson só em `web`. `domain` não depende de nenhum pacote do projeto. A
maior classe é `Routes` (222 linhas), seguida de `ReservationRepository` (158).

**Nota:** bom

- A borda HTTP é fina. Cada rota só lê a requisição, chama o serviço e escreve a resposta
  (`src/main/java/com/marealta/web/Routes.java:50-70`). A validação da entrada também fica ali
  (`Routes.java:73-98`), o que é o lugar certo para ela.
- As regras não conhecem Javalin nem SQL. As mudanças de situação estão na própria `Reservation`
  (`domain/Reservation.java:53-74`), o preço em `domain/Quote.java:11-36` e a devolução em `domain/RatePlan.java`.
  O serviço só delimita a transação e orquestra (`service/ReservationService.java:85-95`).
- Todo o SQL está nos repositórios (`persistence/ReservationRepository.java`, `persistence/MessageRepository.java`).
  O domínio declara a porta de saída das mensagens (`domain/MessageOutbox.java:4`), e a persistência a implementa
  (`persistence/MessageRepository.java:13`). Assim, `events` não depende de `persistence`.
- A montagem fica num lugar só (`App.java:28-46`), inclusive a lista de quem é avisado (`App.java:35-38`).
- Um ponto pequeno: a rota `GET /messages` lê o repositório direto, sem passar por um serviço
  (`web/Routes.java:14,32,58`), e é daí que vem a dependência `web -> persistence`. É uma leitura simples, não é regra
  de negócio dentro do handler. Além disso, `ReportService` monta o texto do CSV (`service/ReportService.java:27-48`),
  o que mistura formato de saída e regra, mas de um jeito que se aceita num sistema deste tamanho.

## Design de baixo nível

**Números:** 3 interessados na estrutura (o hóspede, a governança e o financeiro), cada um num `ReservationListener`
próprio, e nenhum aviso mandado fora dela. Os dois acontecimentos pedidos (confirmação e cancelamento) passam pelo
mesmo caminho de publicação (`ReservationService.java:85-95`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern (Observer).** É o caso que a referência descreve como acerto:
- Há uma interface de ouvinte (`events/ReservationListener.java:4-6`) e um evento com o tipo, a reserva, o quarto e a
  situação anterior (`events/ReservationEvent.java:8-10`).
- Há uma implementação por interessado: o hóspede (`events/GuestConfirmationEmail.java:9`), a governança
  (`events/HousekeepingNotifier.java:9`) e o financeiro (`events/FinanceNotifier.java:9`). O e-mail do hóspede também
  é um ouvinte, sem chamada direta.
- O serviço recebe a lista de fora (`ReservationService.java:24-29`) e só a percorre
  (`ReservationService.java:91-92`). Ele não monta nenhum e-mail e não sabe quem é avisado.
- A lista é montada em `App.java:35-38`. Acrescentar o restaurante ou o transfer é escrever uma classe e mudar uma
  linha da lista.
- Cada ouvinte decide se o acontecimento lhe interessa: a governança só avisa um cancelamento quando a reserva estava
  confirmada (`HousekeepingNotifier.java:29`), e o financeiro só avisa quando há algo a devolver
  (`FinanceNotifier.java:26`).
- O cancelamento usa o mesmo mecanismo da confirmação. O check-in e o check-out também publicam eventos
  (`ReservationService.java:64,68`), o que deixa o transfer pronto para se interessar pela chegada.

Há duas ressalvas pequenas, que não mudam a classe:
- Os ouvintes rodam dentro da transação da mudança (`ReservationService.java:86-93`, com `MessageRepository.send`
  reaproveitando a conexão em `Database.java:79-80`). Hoje isso é bom, porque a mensagem e a mudança são gravadas
  juntas. No dia em que o e-mail sair de verdade, uma falha de envio desfaria a confirmação.
- Os ouvintes usam um `switch` sobre o tipo do evento (`FinanceNotifier.java:21`, `HousekeepingNotifier.java:22`),
  o que é razoável com dois tipos.

**O exagero.** Não há estrutura sem uso. `MessageOutbox` tem uma única implementação de produção, mas é substituída
nos testes (`src/test/java/com/marealta/events/ListenersTest.java:21`) e separa `events` de `persistence`. O registro
`NewReservation` (`service/NewReservation.java:7`) é só o comando de criação. Não há fábrica, builder nem camada que
só repassa.

**O resto do código.**
- Os tipos são os certos: `BigDecimal` com `HALF_EVEN` em todo passo (`domain/Money.java:16-18`, `Quote.java:15,26`,
  `RatePlan.java:23`), `LocalDate` e `YearMonth`, e `enum` para a situação, o plano, o tipo de quarto, o canal e os
  códigos de erro com o seu status HTTP (`domain/ErrorCode.java:3-20`).
- O plano de tarifa carrega o próprio desconto e a própria devolução (`RatePlan.java:6-35`), sem `if` espalhado.
- Os erros passam por uma exceção só, tratada num lugar só (`Routes.java:44-48`).

Os defeitos são pequenos:
- O formato de data `dd/MM/yyyy` aparece duas vezes (`events/Dates.java:7` e `domain/Receipt.java:11`).
- `Reservation.assignId` é público e deixa trocar o id de fora (`Reservation.java:82-84`).
- As respostas em JSON são montadas à mão, campo por campo, em `Map` (`Routes.java:157-202`), o que é verboso, mas
  claro.

Nada disso um revisor exigente travaria.

## Testes

**Números:** 16 testes rodaram, nenhum falhou, nenhum foi pulado, e há 16 métodos escritos (5 pela API contra o banco
real, 11 de unidade, sem mocks). A cobertura de linhas e de ramos **não foi medida**: o JaCoCo não gerou dados
(`measures/tests-maven.log:236`, "missing execution data file"), porque o `pom.xml` fixa
`<argLine>-Dfile.encoding=UTF-8</argLine>` no surefire sem `@{argLine}` (`pom.xml:54`). Isso apaga o agente que o
JaCoCo injeta.

**Nota:** com problemas

As regras da parte comum estão conferidas com valores:
- a alta temporada com o exemplo do texto (`QuoteTest.java:10-14`, R$ 599,76) e as bordas de 14 e 15/12 e de 29/02 e
  01/03 (`QuoteTest.java:17-25`);
- o desconto arredondado meio para o par (`QuoteTest.java:28-33`, R$ 116,97);
- o comprovante inteiro, com o desconto e "1 diária" (`ReceiptTest.java:18-35`);
- a devolução por plano e da pendente (`ReceiptTest.java:43-58`);
- os códigos de erro (`ApiTest.java:142-157`);
- o ciclo inteiro com os dois relatórios em CSV comparados por inteiro (`ApiTest.java:80-117`);
- a NON_REFUNDABLE cancelada que continua contando no financeiro (`ApiTest.java:132-133`).

Na parte nova, os testes conferem:
- o e-mail do hóspede inteiro (`ListenersTest.java:49-62`);
- os assuntos da governança e do financeiro (`ListenersTest.java:64,67`);
- o valor pago (`ListenersTest.java:68`);
- que a NON_REFUNDABLE cancelada não avisa o financeiro (`ListenersTest.java:82-88`);
- que a pendente cancelada não avisa ninguém (`ListenersTest.java:91-96`).

A pergunta da mutação deixa três buracos na parte nova:
- se alguém apagasse a linha "Hóspedes" do aviso da governança (`HousekeepingNotifier.java:27`), nenhum teste
  falharia, porque `ListenersTest.java:65` só confere as datas;
- se o aviso de cancelamento da governança perdesse as datas (`HousekeepingNotifier.java:32-33`), nenhum teste
  falharia, porque `ListenersTest.java:77-78` só confere os assuntos;
- se o reembolso escrevesse outro valor no texto (`FinanceNotifier.java:29`), nenhum teste falharia, pelo mesmo
  motivo.

Há também dois pontos menores:
- `ApiTest` dá `TRUNCATE` nas tabelas do banco de `DB_URL` antes de cada teste (`ApiTest.java:42-46`). Rodar
  `mvn verify` apontando para o banco de produção apaga os dados.
- Sem a cobertura, não dá para dizer quanto do código os testes executam.

## Banco

**Números:** 2 tabelas (`reservations` com 16 colunas, `messages` com 5), as duas com chave primária. Todo o dinheiro
está em `numeric(12,2)` e as datas em `date`. As conferências acusaram:
- nenhuma chave estrangeira;
- nenhuma restrição (`CHECK`, `UNIQUE` ou `EXCLUDE`);
- as colunas `status`, `rate_plan` e `channel` em `text` sem `CHECK`;
- o esquema criado por `CREATE TABLE` no código, sem migração;
- nenhum pool de conexões (1 `DriverManager.getConnection`).

**Nota:** com problemas

Os dados são guardados direito:
- os tipos estão certos (`persistence/Database.java:44-69`);
- todo o SQL usa parâmetros, e o que entra por concatenação é só a constante `COLUMNS`
  (`ReservationRepository.java:20-21`);
- as conexões e os comandos são fechados com try-with-resources (`Database.java:82`,
  `ReservationRepository.java:128`);
- cada mudança é uma transação com `FOR UPDATE` na reserva (`ReservationRepository.java:82-85`);
- a duplicação de reserva é evitada por `pg_advisory_xact_lock` antes da checagem de quarto livre
  (`ReservationRepository.java:30-35`, chamado em `ReservationService.java:39`). Essa trava vale entre processos, e o
  roteiro confirmou as 10 rodadas simultâneas.

Os defeitos:
- O esquema nasce com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:41-71`), sem migrações versionadas. A
  próxima coluna exigirá `ALTER` à mão ou perderá dados.
- Os quartos não estão no banco (`domain/RoomCatalog.java:7-12`), então `reservations.room_number` não tem chave
  estrangeira.
- Nenhuma restrição protege as regras no próprio banco: não há `EXCLUDE` de período por quarto, nem `CHECK` em
  `status`, `guests` ou `check_out > check_in`. A regra do quarto livre depende de todo código respeitar a trava
  consultiva.
- Não há índice em `room_number`, `check_in` nem `status`, colunas que todas as consultas filtram
  (`ReservationRepository.java:89,99,108`).
- Cada transação abre uma conexão nova com `DriverManager` (`Database.java:73-75`), sem pool.

Nada disso perde ou corrompe dado hoje, mas são defeitos que alguém precisa corrigir.

## Comportamento

**Números:** 66 de 66 conferências passaram e nenhuma falhou (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Tudo o que o dono da pousada descreveu funciona pela API, a partir de um banco vazio:
- os preços com a temporada e o arredondamento;
- as recusas com os códigos certos;
- o ciclo da reserva e as devoluções por plano;
- o comprovante e o e-mail do hóspede;
- os dois relatórios em CSV;
- os dados que continuam depois de reiniciar.

Na parte nova:
- a governança e o financeiro recebem os avisos da confirmação com as datas, o número de hóspedes e o valor pago;
- no cancelamento de uma reserva confirmada, a governança recebe as datas liberadas, e o financeiro recebe o
  reembolso só quando há algo a devolver;
- uma reserva pendente cancelada não avisa ninguém.

Duas reservas simultâneas do mesmo quarto nunca foram criadas juntas.

## Dúvidas

Não se sabe a cobertura de linhas e de ramos: o `pom.xml:54` impediu o JaCoCo de medir. A nota dos testes se apoia só
na leitura. Se a cobertura fosse medida e se mostrasse alta, os buracos apontados (o texto dos avisos de cancelamento e
o número de hóspedes) continuariam sem conferência, então a nota não mudaria por isso.
