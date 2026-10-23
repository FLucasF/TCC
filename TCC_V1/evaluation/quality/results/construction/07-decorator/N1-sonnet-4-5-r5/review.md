# Revisão: construção, cenário 07

## Visão geral

O projeto está dividido em camadas comuns: `api` (os controllers do Javalin), `service` (as regras), `service.messaging` (as regras de envio), `repository` (JDBC puro com HikariCP), `domain` (entidades e enums) e `config` (o pool e o `CREATE TABLE`). Tudo é montado em `App.main` (`App.java:30-71`). Uma requisição faz este caminho: `ReservationController` lê o JSON, chama `ReservationService`, que usa `Reservation` (preço e transições), grava pelo `ReservationRepository` e, na confirmação, chama `MessageService.send`. Esse método passa a mensagem pelo `MessageProcessor` (a lista de regras) e grava cada resultado na tabela `messages`. Os dados ficam em três tabelas no PostgreSQL. A parte do cenário (bloqueio, cópia, modo de teste) funciona e passou nas 14 conferências. O mais sério: nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. Não há restrição, trava nem transação, e o roteiro criou as duas reservas nas 10 rodadas de 10. Há também duas recusas descritas no texto (saída igual à entrada, corpo que não é JSON) que respondem 500 em vez de 400. Os testes são só de unidade e cobrem 16,7% das linhas.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo. Javalin só em `com.pousada` e `api`, JDBC só em `config` e `repository`. A maior classe é `ReservationRepository` (246 linhas), seguida de `ReservationService` (239).
**Nota:** bom

- **A borda é fina.** Os controllers leem a requisição, chamam o serviço e traduzem as exceções em códigos de erro (`api/ReservationController.java:22-59`). A única regra que mora ali é a checagem de que a saída vem depois da entrada (`ReservationController.java:35-37`). É validação de entrada, aceitável na borda, mas está separada da checagem de hóspedes, que fica no serviço (`service/ReservationService.java:36-38`).
- **As regras não conhecem Javalin nem SQL.** `ReservationService`, `ReportService` e `MessageService` só falam com os repositórios e com o domínio. As regras de envio (`service/messaging/`) dependem só de `domain`.
- **O acesso ao banco está isolado** nos três repositórios, que recebem o `Database`.
- **A montagem fica num lugar só:** `App.java:30-48`, inclusive a combinação das regras de envio por modo (`App.java:76-88`).
- **Ressalvas pequenas:**
  - os construtores têm efeitos colaterais: `Database` cria o esquema (`config/Database.java:21`) e `RoomRepository` insere os quartos (`repository/RoomRepository.java:17`);
  - as exceções de negócio são classes aninhadas de `ReservationService` (`ReservationService.java:204-238`), o que faz o controller depender da classe concreta.

Nada disso mistura as camadas.

## Design de baixo nível

**Números:** 3 regras de envio, 3 dentro da estrutura e 0 fora: `EmailValidationRule`, `ArchiveCopyRule` e `TestModeRule`, todas com a interface `MessageRule`, aplicadas em lista pelo `MessageProcessor`. A combinação por modo é decidida em `App.createMessageProcessor` (`App.java:76-88`), e nenhuma regra pergunta pelo modo.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Não é um Decorator: a classe que envia (`MessageService.send`, `service/MessageService.java:21-30`) não é embrulhada. Em vez disso, uma lista de filtros (`MessageRule`, `service/messaging/MessageRule.java:3-5`) é percorrida pelo `MessageProcessor` (`MessageProcessor.java:13-31`). A referência aceita isso de antemão: cada regra é uma peça própria e a combinação fica na montagem (`App.java:79-85`: a validação sempre; o modo de teste ou a cópia conforme o modo). Uma regra nova (madrugada, limite por dia) entra como uma classe a mais e uma linha na montagem.

O que afasta do ideal:
- as regras mudam a mesma `OutgoingMessage`, que é mutável (`TestModeRule.java:13-15`, `OutgoingMessage.java:34-52`);
- a ordem da lista importa. O processador aplica cada regra também às mensagens que as regras anteriores criaram (`MessageProcessor.java:20-27`). Com o `TestModeRule` depois do `ArchiveCopyRule`, a cópia também seria desviada;
- `ArchiveCopyRule.java:10` e `TestModeRule.java:8` conferem `isBlocked()` de novo, embora o processador já pule as bloqueadas (`MessageProcessor.java:21`).

**O exagero.** Não há interfaces sem uso, fábricas ou builders. As duas peças menores não pesam:
- `ProcessingResult` (`ProcessingResult.java:6-20`) só embrulha uma lista;
- `RoomService` só repassa para o repositório (`service/RoomService.java:15-21`), e `getRoomByNumber` nem é usado.

**O resto do código** tem defeitos localizados:
- **Exceções como controle de fluxo:**
  - `getRequiredField` lança `NullPointerException` de propósito (`ReservationController.java:134`), e o handler pega `NullPointerException | IllegalArgumentException` em bloco (`:46`), o que esconde NPEs de verdade;
  - o controller declara a sua própria `InvalidRequestException` (`:185`), que nenhum `catch` pega (os `catch` são para a do serviço, `:56`). É a causa do 500 na saída igual à entrada.
- **Duplicação:**
  - a checagem de situação é feita no serviço (`ReservationService.java:62, 103, 114, 125`) e de novo na entidade (`domain/Reservation.java:117, 125, 132, 139`);
  - os seis handlers repetem `Long.parseLong` e os mesmos `catch` (`ReservationController.java:61-129`), e nenhum trata id não numérico (`/reservations/abc` dá 500).
- **Apresentação dentro do serviço de regras:** o texto do comprovante, o CPF e o dinheiro em R$ são montados em `ReservationService` (`:164-202`). `formatMoney` usa `String.format("R$ %,.2f")` sem `Locale` e depois troca os separadores com `replace` (`:200-201`). Com a JVM em `pt_BR`, R$ 1.234,56 sairia "R$ 1,234.56".
- **Valores e dados de leitura:**
  - `amountPaid` começa em `BigDecimal.ZERO`, com escala 0 (`Reservation.java:37`), e sai `0` no JSON em vez de `0.00`;
  - `getTotal` recalcula a soma das noites duas vezes a cada chamada (`Reservation.java:85-95`);
  - `RoomRepository.mapRow` ignora as colunas `capacity` e `nightly_rate` do banco e lê tudo do enum (`RoomRepository.java:93-96`).
- **No lado bom:** `BigDecimal` com `HALF_EVEN` em cada passo, `LocalDate` e enums para situação, plano e tipo.

## Testes

**Números:** 21 testes rodaram, 0 falharam, 0 pulados; 21 métodos escritos. Cobertura: 148 de 884 linhas (16,7%) e 47 de 172 ramos (27,3%). Por pacote: `messaging` 77/80 linhas, `domain` 71/185, e `api`, `service`, `repository` e `App` com 0. Não há testes pela API nem contra o banco.
**Nota:** com problemas

**O que está testado, com valores:**
- o preço com temporada e arredondamento: 599,76 em julho, 74,97 e 424,83 no NON_REFUNDABLE (`domain/ReservationTest.java:37, 52-53`);
- confirmação e devolução (`ReservationTest.java:105, 121, 137`);
- as três regras de envio uma a uma (`ArchiveCopyRuleTest.java:22-24`, `TestModeRuleTest.java:20-22`, `EmailValidationRuleTest.java`) e duas combinações (`MessageProcessorTest.java:13-64`).

**O que fica sem rede:**
- **A combinação por modo, que é o coração do cenário.** `App.createMessageProcessor` não tem teste (0 linhas cobertas em `com.pousada`). Trocar os ramos do `if (testMode)` (`App.java:81-85`) não derruba nenhum teste.
- **A validação do endereço.** Ninguém testa "ana@.com" nem "ana@gmail.", então apagar a condição `dotIndex == 0 || dotIndex == domain.length() - 1` (`EmailValidationRule.java:34`) passa.
- **O modo de teste com endereço inválido** (bloqueado, mantendo o endereço original) também não tem teste.
- **O corpo desviado no modo de teste.** `TestModeRuleTest.java:22` só confere o começo; perder o texto original depois da primeira linha não seria pego.
- **A borda de 15 de dezembro.** O teste começa em 15/12 (`ReservationTest.java:77`), e trocar `day >= 15` por `day >= 14` (`Reservation.java:105`) passa. Ninguém testa a virada de fevereiro para março.
- **Nada da borda, do serviço e dos relatórios:** recusas e códigos de erro, conflito de datas, transições proibidas, comprovante e e-mail formatados, relatórios CSV, persistência. Os dois 500 do roteiro de comportamento estariam à vista com um único teste pela API.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)`, datas em `date`. Conferências que acusaram algo:
- a chave estrangeira `reservations(room_number)` não tem índice;
- nenhuma coluna de situação, tipo ou canal tem `CHECK`;
- nenhuma restrição (UNIQUE, EXCLUDE, CHECK) protege regra alguma;
- 0 transações explícitas e 0 `FOR UPDATE`.

**Nota:** ruim

**O defeito sério: a regra central da pousada não tem proteção nenhuma contra pedidos simultâneos.** A disponibilidade é conferida com um `SELECT` (`ReservationService.java:40`, `ReservationRepository.java:125-156`) e a reserva é gravada depois, numa outra conexão em autocommit (`ReservationService.java:51`, `ReservationRepository.java:38`). Não há transação, trava de linha, `synchronized` nem restrição `EXCLUDE` no esquema (`config/Database.java:37-53`). O roteiro de comportamento mediu o resultado: nas 10 rodadas de dois pedidos simultâneos para o mesmo quarto e a mesma noite, as duas reservas foram criadas 10 vezes. É dado errado gravado: um quarto vendido duas vezes.

**Outros defeitos:**
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` dentro do código (`Database.java:28-64`), sem migrações. O próximo pedido que precise de uma coluna nova não tem como alterar um banco que já existe.
- **Não há restrições de domínio:** nenhum `CHECK` para `guests >= 1`, `check_out > check_in` nem para os valores válidos de `status` e `rate_plan` (`Database.java:44-50`).
- **A chave estrangeira sem índice** (`Database.java:40`) obriga a varrer a tabela tanto para conferir conflitos por quarto quanto para a ocupação.
- **A confirmação não é atômica:** ela grava a reserva (`ReservationService.java:67`) e depois cada mensagem numa conexão própria (`MessageService.java:25-29`). Se uma falhar no meio, a reserva fica confirmada sem o e-mail ou sem a cópia.
- **Dados guardados e nunca lidos:** `rooms.capacity` e `rooms.nightly_rate` são gravados (`RoomRepository.java:42-50`), mas a leitura usa o enum (`:93-96`).
- **Consultas extras:** o relatório de ocupação busca o quarto uma vez por linha (`ReportService.java:36, 80-83`). É pouco, porque são só cinco quartos.

**No lado bom:** todo o SQL usa parâmetros `?`, as conexões são fechadas com try-with-resources e há pool (HikariCP, `Database.java:15-20`). Os dados continuam lá depois de reiniciar (3 de 3 no roteiro).

## Comportamento

**Números:** 59 de 65 conferências passaram.
- a parte comum: 41 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 07 no modo normal: 9 de 9;
- o cenário 07 no modo de teste: 5 de 5;
- duas reservas ao mesmo tempo: 1 de 2.

**Nota:** com problemas

As regras pedidas no cenário funcionam pela API:
- o endereço inválido fica BLOCKED com o endereço original e sem cópia, nos dois modos;
- a cópia `[Cópia] ` vem logo depois da original, com o mesmo texto;
- no modo de teste, a mensagem vai para `testes@…`, com `[TESTE] ` e "Destinatário original:", sem cópia.

O preço, os planos, as devoluções, o comprovante, o e-mail e os dois relatórios também estão certos. As falhas:

- **Saída igual à entrada responde 500, e não 400 INVALID_REQUEST.** A exceção lançada em `ReservationController.java:36` não é pega por nenhum `catch` (confirmado com `curl`: "Server Error 500", e o log mostra "Uncaught exception … Check-out must be after check-in"). É erro do agente.
- **Corpo que não é JSON responde 500.** O erro do Jackson vindo de `ctx.bodyAsClass` (`:24`) não cai em nenhum `catch`. É erro do agente. Id não numérico (`/reservations/abc`) também dá 500, mas está fora do roteiro.
- **`amountPaid` sai como `0`, e não `0.00`, antes da confirmação.** Vem de `Reservation.java:37`. O texto pede o dinheiro com 2 casas. É uma falha pequena, mas é do agente.
- **"O 201 comporta 3" e "a diária do 201 é 389.90" falharam** só porque o projeto devolve `number` como texto (`"201"`), e o roteiro procura o número 201. Os valores estão certos (`"capacity":3`, `"nightlyRate":389.90`, conferido com `curl`). O texto não diz o tipo de `number`, então a falha não pesa.
- **Duas reservas simultâneas: as duas foram criadas nas 10 rodadas.** O defeito é do banco e pesa naquela coluna.

Para o dono da pousada, quer dizer o seguinte:
- o sistema faz o que ele pediu nos e-mails, nos preços e nos relatórios;
- se o site mandar uma reserva com a mesma data de entrada e saída, ou um pedido malformado, recebe um "erro do servidor" em vez de uma mensagem clara;
- e, em horário de movimento, o mesmo quarto pode ser vendido duas vezes.

## Dúvidas

- **No Banco, `ruim` ou `com problemas`?** A skill cita, para `com problemas`, a reserva duplicada "protegida só dentro do processo". Aqui não há proteção nenhuma, e a reserva duplicada foi gravada 10 vezes em 10. Por isso dei `ruim` (risco de dado errado), mas os tipos, as chaves e o SQL estão corretos.
- **O exagero:** `RoomService` é uma camada que só repassa, e `ProcessingResult` só embrulha uma lista. Considerei as duas pequenas demais para marcar `exagero`.
- **O `formatMoney` dependente de `Locale`** não aparece no ambiente da revisão. Não rodei o serviço com a JVM em `pt_BR` para confirmar; a conclusão vem da leitura de `ReservationService.java:200-201`.
