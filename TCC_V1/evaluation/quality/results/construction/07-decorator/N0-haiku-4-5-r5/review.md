# Revisão: construção, cenário 07

## Visão geral

O projeto tem cinco pacotes: `api` (os handlers do Javalin, todos `static`), `service` (preço, comprovante, validação
de e-mail e envio de mensagens), `database` (um pool HikariCP num campo `static` e três repositórios JDBC), `model`
(as entidades e os enums) e a raiz com o `main`. Uma requisição entra num método `static` de um handler, que abre a
conexão, aplica ali mesmo as regras da reserva (soma das noites, transições de situação, devolução) e chama os
repositórios. Os dados ficam em três tabelas do PostgreSQL, criadas com `CREATE TABLE IF NOT EXISTS` no código. O
mais sério: **o sistema não funciona pela API**. O `pom.xml` não traz o Jackson, e as anotações são do Gson, que o
Javalin não usa. Por isso toda chamada que lê ou devolve JSON (`ctx.bodyAsClass`, `ctx.json`) responde 500 com "It
looks like you don't have an object mapper configured" (`measures/behavior-service.log`): só 6 das 65 conferências
passam. Há outros dois defeitos sérios. Ao subir, o serviço apaga e recria os quartos (`Database.java:88`); com uma
reserva no banco, a chave estrangeira recusa o `DELETE`, e o serviço não sobe mais (conferi à mão: `exit=1`, "violates
foreign key constraint"). E não existe nenhum teste. Por fim, as três regras de envio do cenário ficaram em `if`s
dentro de um único método (`MessageService.java:47-66`), sem nenhuma estrutura que permita combiná-las.

## Arquitetura

**Números:** 5 pacotes, 8 dependências entre eles, nenhum ciclo. `api` depende de `database`, `model` e `service`;
`service` depende de `database` e `model` e usa JDBC; a maior classe é `ReservationHandler` (339 linhas, contra 181 da
segunda).
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: o SQL fica todo em `database/` e não há ciclo. Mas a borda HTTP não é fina. As regras da reserva moram em `api/ReservationHandler.java`:

- a soma das noites com a temporada (`api/ReservationHandler.java:76-83`);
- as transições de situação (`:148`, `:196`, `:230`, `:258`);
- o pagamento (`:154`) e a regra da devolução (`:201-204`);
- a regra de quando existe a segunda via (`:303`).

O `PricingService` só tem as contas de cada passo. Os handlers chamam os repositórios direto, sem passar por um
serviço (`api/ReservationHandler.java:71`, `:107`; `api/ReportHandler.java:59-60`), e o cálculo do financeiro também
fica no handler (`api/ReportHandler.java:94-108`).

A regra de envio depende de JDBC: `service/MessageService.java:19,35` recebe uma `java.sql.Connection`.

Não há montagem. O `main` só chama `Database.init()` e os `register` estáticos (`Application.java:20-32`). Cada classe
alcança as outras por métodos `static`, e o pool é um `static` global (`database/Database.java:12`). O modo de teste é
lido de dentro do serviço (`service/MessageService.java:17`). Com isso, combinar regras de envio por instalação, como o
texto pede, não tem um lugar para acontecer.

## Design de baixo nível

**Números:** das três regras de envio do cenário (o bloqueio do endereço inválido, a cópia para o arquivo, o desvio do
modo de teste), nenhuma está numa estrutura. As três são `if`s dentro de `MessageService.processMessage`, e a
combinação por modo também está escrita ali. Não há interface de envio, nem embrulho, nem montagem.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede três comportamentos em volta do mesmo envio, combinados conforme o modo, e anuncia mais
dois (madrugada, limite por dia), cada um valendo sempre, só no teste ou só fora dele, além de instalações com
combinações diferentes. O código resolve tudo num método só:

- o bloqueio é um `if` (`service/MessageService.java:47-50`);
- o desvio de teste é um `if (TEST_MODE)` (`:56-60`);
- a cópia é um `if (!TEST_MODE)` (`:64-66`).

O modo é uma constante `static final` lida da variável de ambiente (`:17`). Não há nada que se possa embrulhar ou
empilhar. Uma regra nova obriga a mexer nesse método, e a segunda pousada, que não quer a cópia, só seria atendida com
mais um `if`. Pela referência, isso é omissão.

**O exagero.** Não há estrutura sem necessidade: nada de interfaces com uma implementação só, fábricas ou camadas que
só repassam. Há código morto (`PricingService.calculateNightlyRate` e `calculateSubtotal`, em
`service/PricingService.java:24,35`, que ninguém chama), mas isso não é exagero.

**O resto do código** tem defeitos espalhados:

- **Dinheiro em `double`** no modelo inteiro (`model/Reservation.java:24-30`, `model/Room.java:10`,
  `model/Document.java:23-25`). O arredondamento é feito com `new BigDecimal(double)` (`service/PricingService.java:51`),
  que arredonda a expansão binária, e não o valor decimal. O "meio para o par" pedido só acerta por acaso nos casos de
  empate. A soma do financeiro também é feita em `double` (`api/ReportHandler.java:93-108`).
- **Duplicação.** O mesmo `new Document(...)` de 11 argumentos aparece três vezes (`api/ReservationHandler.java:160`,
  `:269`, `:311`). O bloco "busca a reserva, responde 404, trata `NumberFormatException` e `Exception`" se repete nos
  sete métodos.
- **Erros.** Há `catch (Exception)` genérico em todos os handlers, o que transforma tudo em 500
  (`api/ReservationHandler.java:112`, `api/ReportHandler.java:34`). Assim, uma data de ocupação fora do formato vira 500,
  e não 400. Há `Optional.get()` sem verificação (`api/ReservationHandler.java:158`).
- **Validação.** `hasRequiredFields` (`api/ReservationHandler.java:334-337`) usa `int` primitivo e `> 0` para saber se
  o campo veio. Um hóspede zero vira "Missing required fields", e não a regra "menos de um hóspede".
- **Tipos.** O plano de tarifa vai como texto para o desconto (`service/PricingService.java:39-40`), embora exista o
  `enum RatePlan`.
- **Estado.** Campos públicos em `api/CreateReservationRequest.java:7-27`, e `static` mutável em
  `database/Database.java:12`.
- **Anotações erradas.** As anotações `@SerializedName` do Gson (`model/Reservation.java:8-29`) não servem para nada no
  Javalin, que usa Jackson, e são a origem da quebra de toda a API.
- **Validação do e-mail.** `service/EmailValidator.java:15` aceita "ana@gmail." e "ana@.com", porque só pede um ponto
  em qualquer lugar do domínio, e não "no meio".

## Testes

**Números:** 0 testes rodaram (0 falhas); 0 métodos de teste escritos; não há pasta `src/test`; sem cobertura (o
JaCoCo não tinha o que medir). O `mvn verify` passa (BUILD SUCCESS em `measures/tests-maven.log`) só porque não há
teste nenhum.
**Nota:** ruim

O agente declarou o JUnit no `pom.xml:56-61`, mas não escreveu nenhum teste. Nenhuma regra tem rede de segurança: a
temporada, o arredondamento, o desconto, as transições de situação, a devolução, os relatórios e as regras de envio do
cenário. Um único teste pela API teria mostrado que todas as rotas com JSON respondem 500.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; o dinheiro em `numeric(10,2)`;
as datas em `date`. Conferências que acusaram algo:

- a chave estrangeira `reservations(room_number)` sem índice;
- nenhum CHECK nas colunas de situação, plano, tipo e canal;
- nenhuma restrição (UNIQUE, EXCLUDE ou CHECK) que proteja uma regra;
- 0 transações explícitas e 0 `FOR UPDATE`;
- um SQL montado com `+` (`database/RoomRepository.java:35`), em que o que entra é um `int`, então não abre injeção.

**Nota:** ruim

O defeito sério está em `database/Database.java:88`: a cada subida, o serviço faz `DELETE FROM rooms` e insere os
quartos de novo. Com uma reserva no banco, a chave estrangeira (`database/Database.java:51`) recusa o `DELETE`, a
inicialização falha e o `main` sai com `System.exit(1)` (`Application.java:22-24`). Conferi à mão: subi o serviço num
banco vazio, gravei uma reserva e subi de novo. Resultado: `exit=1` e "update or delete on table rooms violates foreign
key constraint reservations_room_number_fkey". Ou seja, depois da primeira reserva o sistema não sobe mais sobre os
próprios dados. O roteiro do comportamento não pegou isso só porque nenhuma reserva chegou a ser criada.

Os outros defeitos:

- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`database/Database.java:39-80`),
  sem migrações, e não saberia acrescentar uma coluna.
- **Nada impede duas reservas na mesma noite**, a não ser a consulta prévia (`database/ReservationRepository.java:87-107`)
  seguida de um `INSERT` separado (`api/ReservationHandler.java:71,107`), em autocommit. Não há transação, trava nem
  restrição `EXCLUDE`, então dois pedidos simultâneos podem gravar as duas.
- **Duas escritas sem transação na confirmação.** A situação é gravada e o e-mail é inserido em comandos separados
  (`api/ReservationHandler.java:155,174`). Se o envio falhar, a reserva fica CONFIRMED sem o comprovante.
- **O dinheiro passa por `double`** (`database/ReservationRepository.java:32-36`, `:173-177`), embora a coluna seja
  `numeric`.
- **O relatório mensal não usa índice:** ele filtra por `to_char(check_in, 'YYYY-MM')`
  (`database/ReservationRepository.java:116`).

O SQL usa parâmetros, as conexões são devolvidas com try-with-resources e há pool.

## Comportamento

**Números:** 6 de 65 conferências passaram.

| parte | passaram |
|---|---|
| a parte comum | 4 de 46 |
| os dados depois de reiniciar | 1 de 3 |
| o cenário no modo normal | 0 de 9 |
| o cenário no modo de teste | 0 de 5 |
| as reservas simultâneas | 1 de 2 |

**Nota:** ruim

O serviço sobe, mas toda rota que lê ou escreve JSON responde 500 com "It looks like you don't have an object mapper
configured": falta o `jackson-databind` no `pom.xml`. Entre essas rotas estão `GET /rooms`, `POST /reservations` e
`GET /messages`. Nenhuma reserva é criada, então nada do que vem depois funciona: confirmação, e-mail, check-in,
check-out, cancelamento, devolução, relatórios com dados e todas as regras de envio do cenário, nos dois modos. As
poucas conferências "OK" passam por acidente: os cabeçalhos dos CSV vazios, o `refund` null lido de uma resposta de
erro e "nenhuma rodada com as duas reservas criadas", porque as duas dão 500.

O que vai além do mapeador de JSON:

- o mês fora do formato responde 200 em vez de 400 (`api/ReportHandler.java:42-48` não valida);
- a segunda via de reserva pendente responde 404 RESERVATION_NOT_FOUND em vez de 409 OPERATION_NOT_ALLOWED
  (`api/ReservationHandler.java:303-305`).

Para o dono da pousada, isso quer dizer que o site não consegue nem listar os quartos nem fazer uma reserva. Mesmo
corrigido o JSON, o sistema deixaria de subir no primeiro reinício depois de uma reserva (ver Banco).

## Dúvidas

As conferências das regras de envio não chegaram a rodar, porque a API não cria reservas. Pela leitura do
`MessageService`, o bloqueio, a cópia e o desvio parecem ter o efeito pedido nos dois modos, salvo os endereços que o
validador aceita sem ter um ponto "no meio" (`service/EmailValidator.java:15`). Mas nada disso foi visto funcionando.
