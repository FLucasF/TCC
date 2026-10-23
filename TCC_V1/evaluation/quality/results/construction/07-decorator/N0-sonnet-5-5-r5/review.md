# Revisão: construção, cenário 07

## Visão geral

O projeto é um pacote só, `br.com.marealta`, com 23 classes e 946 linhas. O caminho de uma requisição: as rotas do Javalin em `App.java:45-68` leem e validam o corpo (`App.java:99-126`) e chamam `ReservationService` ou `ReportService`. Esses serviços abrem uma transação com `Db.tx` (`Db.java:43-57`) e escrevem o SQL ali mesmo, com o JDBC. O preço fica em `Pricing` e as regras de envio em `MessagePolicy`, as duas sem banco. Os dados ficam no PostgreSQL, em quatro tabelas criadas com `CREATE TABLE IF NOT EXISTS` (`Db.java:59-114`). O sistema funciona inteiro pela API: 65 de 65 conferências passaram, contando o modo de teste e as reservas simultâneas. Os pontos mais sérios ficam no código:
- **O pattern pedido não foi aplicado.** As três regras de envio (endereço inválido, cópia para o arquivo, modo de teste) e as duas anunciadas (madrugada, limite diário) são `if`s dentro de um único método, `MessagePolicy.process`.
- **Não há camada de dados.** O SQL mora nos serviços, junto com as regras da reserva.
- **Os testes não chegam ao resto.** Cobrem só o domínio puro (17,7% das linhas).

## Arquitetura

**Números:** 1 pacote (`br.com.marealta`, 23 classes, 946 linhas), 0 dependências entre pacotes, 0 ciclos. Esse pacote único usa Javalin, JDBC, Jackson e o pool. Maiores classes: `ReservationService` (188 linhas), `App` (135), `Db` (115).
**Nota:** com problemas

O que está bem:
- **Borda HTTP fina.** Cada rota só converte a requisição e chama o serviço (`App.java:45-52`), e nenhuma rota tem SQL nem regra de reserva.
- **Montagem num lugar só.** As peças são ligadas em `App.main`/`App.create` (`App.java:28-38`), sem `static` mutável.
- **Algumas regras isoladas.** `Pricing` (`Pricing.java:24-35`) e `MessagePolicy` não conhecem o Javalin nem o JDBC.

O que pesa:
- **Não há camada de dados.** Os três serviços misturam regra e SQL. Em `ReservationService`, a regra de devolução (`ReservationService.java:105-106`) e as transições de situação (`ReservationService.java:140-143`) ficam na mesma classe que os `INSERT`/`UPDATE`/`SELECT` (`ReservationService.java:47-79`, `145-187`). `ReportService` junta a consulta e a montagem do CSV (`ReportService.java:20-43`).
- **A `Connection` do JDBC circula pelo código.** `MessageService.send` recebe a conexão (`MessageService.java:26`), e uma rota abre a transação direto (`App.java:53`, `db.tx(messages::list)`).
- **Tudo num pacote só.** Ainda dá para crescer, mas o próximo pedido vai mexer em classes que fazem SQL e regra ao mesmo tempo.

É uma mistura espalhada pelos serviços, não um sistema sem camadas, por isso a nota é `com problemas` e não `ruim`.

## Design de baixo nível

**Números:**
- Regras de envio numa estrutura própria (uma peça por regra): 0 de 5. Endereço inválido, madrugada e limite diário são três `if`s em `MessagePolicy.java:89-91`. A cópia para o arquivo é um `if` em `MessagePolicy.java:71`, e o desvio do modo de teste outro em `MessagePolicy.java:78`.
- A combinação por modo é configurável por regra (enum `Scope`, `MessagePolicy.java:18-29`, lido em `Config.java:23-28`), mas cada regra chama `applies(cfg.testMode())` dentro do próprio método.

**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern (Decorator).** O texto pede regras em volta do mesmo envio, que se combinam por modo e por instalação e que vão crescer. O esperado é um embrulho por regra com a mesma interface do envio, empilhados na montagem. O agente fez outra coisa:
- **Uma só classe, `MessagePolicy`, cujo método `process` (`MessagePolicy.java:63-86`) faz tudo em sequência:** bloqueia (`blocked`, `MessagePolicy.java:88-92`), acrescenta a cópia (`MessagePolicy.java:71-74`) e desvia para a caixa de testes (`MessagePolicy.java:76-84`).
- **Não há interface de envio nem peça própria por regra.** `MessageService.send` (`MessageService.java:26-54`) chama a política e grava.
- **O que aproxima.** A combinação de cada regra por modo (OFF/ALWAYS/TEST_ONLY/LIVE_ONLY) vem da configuração de cada instalação, por variável de ambiente (`Config.java:23-28`). Isso responde ao "a segunda pousada não quer a cópia" sem mexer no código.
- **O que afasta.** Uma regra nova (a madrugada e o limite diário já entraram assim) é mais um `if`, mais um campo no `record Config` (`MessagePolicy.java:31-41`) e mais um parâmetro em `Config.fromEnv`. A ordem entre as regras está escrita à mão em `process`.

É o caso "os `if` ficam dentro do envio" da referência: omissão.

**Exagero:** não há interface com uma implementação só, fábrica, builder ou camada de repasse. `Receipt.of`, `Ids` e `Documents` são pequenos e usados.

**O resto do código:**
- **Os tipos certos onde mais importa.** `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:15-17`, `Pricing.java:20-35`) e `LocalDate` nas datas.
- **Situação e plano como texto solto, sem `enum`.** Os campos `status` e `ratePlan` são `String` (`Reservation.java:15`, `Reservation.java:20`). As comparações são feitas com literais espalhados: `"NON_REFUNDABLE"` em `Pricing.java:31`, `"STANDARD"`/`"CONFIRMED"` em `ReservationService.java:105`, `"PENDING"` em `ReservationService.java:76`, o conjunto em `App.java:26`. A lista das situações pagas está duplicada entre `Reservation.java:25-27` e o SQL de `ReportService.java:23`.
- **Campos públicos.** Em `ApiException` (`ApiException.java:4-5`).
- **Tratamento de erro solto em dois pontos.** O tratador genérico só faz `printStackTrace` (`App.java:71-74`), e `sentToday` embrulha a `SQLException` numa `RuntimeException` sem tipo (`MessageService.java:82-84`).
- **Parse de data repetido.** A leitura de `month` (`App.java:59-66`) repete a de `parseDate` (`App.java:90-97`).
- **Um método longo.** `ReservationService.create` (`ReservationService.java:39-82`) faz a capacidade, a trava, a sobreposição, o preço e o `INSERT` num bloco só.

São defeitos localizados, por isso a nota é `com problemas`.

## Testes

**Números:**
- 10 testes rodaram, 0 falhas, 0 erros, 0 pulados. 10 métodos escritos, num arquivo só (`DomainTest.java`).
- Cobertura: 74 de 418 linhas (17,7%) e 58 de 152 ramos (38,2%).
- Nenhum teste pela API nem contra o banco.

**Nota:** com problemas

**O que está testado, com valores:**
- o preço da alta temporada, 599.76 (`DomainTest.java:15-19`);
- os limites da temporada, 14/12, 15/12, 29/02 e 01/03 (`DomainTest.java:22-28`);
- o desconto não reembolsável, 116.97 e 662.83 (`DomainTest.java:31-36`);
- o texto do comprovante e o formato R$ 1.234,56 (`DomainTest.java:39-51`).

A parte nova também está bem conferida:
- a validação do endereço, nos casos do texto (`DomainTest.java:54-61`);
- a cópia com "[Cópia] " e o mesmo texto (`DomainTest.java:72-79`);
- o modo de teste com o endereço, "[TESTE] " e a primeira linha (`DomainTest.java:82-88`);
- o bloqueio sem cópia nos dois modos (`DomainTest.java:97-106`);
- a madrugada e o limite diário (`DomainTest.java:109-117`).

Na pergunta da mutação, trocar o prefixo "[Cópia] " ou "[TESTE] ", tirar a cópia ou deixar de bloquear "ana@gmail" faz um teste falhar.

**O que fica sem rede:**
- toda a máquina de situações e o cancelamento com devolução (`ReservationService.java:88-138`). Trocar `"STANDARD"` por `"NON_REFUNDABLE"` em `ReservationService.java:105` não derruba nenhum teste;
- a recusa de quarto ocupado (`ReservationService.java:47-58`; trocar `<` por `<=` passa);
- a capacidade, os códigos de erro e a validação do corpo (`App.java:99-126`);
- os dois relatórios CSV (`ReportService.java`; trocar `check_out > ?` por `>=` passa);
- a gravação das mensagens com o `status` (`MessageService.java:26-54`).

Os testes existem, passam e conferem bem o domínio puro, mas a maior parte das regras da reserva e dos relatórios não é verificada.

## Banco

**Números:**
- 4 tabelas (`rooms`, `reservations`, `messages`, `counters`), todas com chave primária.
- Dinheiro em `numeric(12,2)` nas 6 colunas e datas em `date`.
- A chave estrangeira `reservations.room_number` tem índice.
- O que as conferências acusaram:
  - esquema criado no código (`CREATE TABLE` em `Db.java`), sem migrações;
  - 5 colunas de situação ou tipo em `text` sem `CHECK`;
  - nenhuma restrição `UNIQUE`/`EXCLUDE`/`CHECK` protegendo regra.

**Nota:** com problemas

**O que está bem:**
- **Tipos certos.** `NUMERIC(12,2)` e `DATE` (`Db.java:64-102`).
- **SQL sempre com `?`.** As concatenações apontadas pelo script juntam só constantes (`COLS` e o `FOR UPDATE` em `ReservationService.java:173`).
- **Pool e conexões.** HikariCP (`Db.java:22-30`) e try-with-resources em toda conexão.
- **Uma transação por operação** (`Db.java:43-57`). O e-mail de confirmação é gravado na mesma transação que muda a situação (`ReservationService.java:88-98`).
- **Sobreposição de reservas travada no banco.** Um `SELECT ... FOR UPDATE` na linha do quarto (`ReservationService.java:46`, `158`) funciona entre processos. O roteiro confirmou nas 10 rodadas simultâneas.
- **Numeração sem buracos.** A tabela `counters` faz a numeração dentro da transação (`Ids.java:12-21`).

**Os defeitos:**
- **O esquema não evolui.** Ele nasce de `CREATE TABLE IF NOT EXISTS` em `Db.java:63-103`, sem migrações versionadas. O próximo pedido que acrescentar uma coluna não terá como alterar um banco que já roda.
- **Nenhum `CHECK` no banco.** Não há restrição em `status`, `rate_plan`, `guests > 0` ou `check_out > check_in` (`Db.java:74-90`).
- **Nenhuma restrição contra a sobreposição.** O banco não tem um `EXCLUDE` que impeça duas reservas no mesmo quarto e noite; quem garante é a trava no código.
- **Um único contador de mensagens trava todos os envios.** O contador de `messages` fica bloqueado até o fim da transação (`MessageService.java:29`). Isso é aceitável neste tamanho.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam.

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário no modo normal | 9 de 9 |
| cenário no modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API:
- **Preços.** A temporada e o arredondamento meio para o par (599.76; desconto 37.48) saem certos.
- **Recusas.** Respondem com os códigos combinados.
- **Situações da reserva.** Confirmação, check-in, check-out e cancelamento com e sem devolução funcionam.
- **Relatórios.** Os dois CSV vêm no formato pedido; a reserva não reembolsável cancelada continua contando.
- **Reinício.** As reservas e as mensagens continuam lá depois de reiniciar.
- **E-mails.**
  - O endereço inválido aparece como `BLOCKED`, sem cópia.
  - A cópia para `arquivo@...` vem logo depois da original, com "[Cópia] ".
  - Com `HOTEL_TEST_MODE=true` nada sai para o hóspede: vai para `testes@...` com "[TESTE] " e "Destinatário original: ...", sem cópia. O bloqueio continua no modo de teste, com o endereço original.
- **Reservas simultâneas.** Duas reservas pedidas ao mesmo tempo para o mesmo quarto nunca passam juntas.

## Dúvidas

- **A classe do pattern ficou entre omissão e malfeito.** A política de envio tem um embrião de estrutura: cada regra tem um escopo próprio, configurável por instalação (`MessagePolicy.Scope`, `Config.java:23-28`). Mas não há peça própria por regra nem interface de envio, e todas as regras são `if`s no mesmo método (`MessagePolicy.java:63-92`). Pela referência, isso é o caso "os `if` ficam dentro do envio", e por isso escolhi omissão. Quem der mais peso ao escopo configurável poderia ler como malfeito.
- **A arquitetura ficou entre `com problemas` e `ruim`.** O projeto é "tudo num pacote", que a skill cita como exemplo de `ruim`. Pesei que a borda HTTP está separada e que as regras puras (`Pricing`, `MessagePolicy`) não conhecem Javalin nem JDBC, e por isso dei `com problemas`.
