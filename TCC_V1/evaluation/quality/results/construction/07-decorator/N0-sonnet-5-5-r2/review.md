# Revisão: construção, cenário 07

## Visão geral

O projeto tem um pacote só (`br.com.marealta.pousada`), com 11 arquivos e 995 linhas. A montagem fica em `Main.java:7-14`. Ela lê a configuração, cria o `Database` (pool Hikari, esquema criado em `Database.migrate`), o `MessageDispatcher` e o `ReservationService`, e liga tudo no `WebApp`. Uma requisição entra em `WebApp` (rotas de uma linha: `WebApp.java:29-55`), é validada em `WebApp.parseNew` e vai para o `ReservationService`. Lá, cada operação abre uma transação (`Database.tx`) e mistura a regra com o SQL. O preço fica numa classe pura (`Pricing`), e o mesmo vale para o comprovante (`Receipt`). As regras de envio ficam no `MessageDispatcher`, que tem três listas de regras (bloqueio, expansão e transformação), montadas a partir do `Config`. As mensagens são gravadas por `JdbcMessageStore`. Os dados ficam no PostgreSQL, em três tabelas. O sistema funciona por inteiro: 65 de 65 conferências, 22 testes passando, 90,9% de cobertura de linhas. Os pontos mais sérios são de estrutura. O `ReservationService` junta regra de negócio e SQL. O esquema é criado com `CREATE TABLE IF NOT EXISTS` no código, sem migrações.

## Arquitetura

**Números:** 1 pacote (27 classes, 995 linhas), 0 dependências entre pacotes, nenhum ciclo. O único pacote usa Javalin, JDBC, Jackson e o pool. A maior classe é `ReservationService` (262 linhas), seguida de `WebApp` (200).
**Nota:** com problemas

- **O que está bem:**
  - A borda HTTP é fina. Cada rota lê o pedido, chama o serviço e escreve a resposta (`WebApp.java:29-39`). A validação de entrada fica na borda (`WebApp.java:84-115`).
  - A montagem fica num lugar só (`Main.java:7-14`), e o serviço recebe as dependências pelo construtor (`ReservationService.java:23-26`).
  - O preço (`Pricing.java`), o comprovante (`Receipt.java`) e as regras de envio (`MessageDispatcher.java`) não conhecem Javalin nem JDBC. O dispatcher fala com o banco só pela interface `Store` (`MessageDispatcher.java:28-33`).
- **Defeito 1: o `ReservationService` faz regra e acesso a dados ao mesmo tempo.**
  - `create` (`ReservationService.java:62-106`) junta a regra de capacidade, a consulta de conflito em SQL, o cálculo de preço e o `INSERT`.
  - A regra de devolução do cancelamento é escrita como fragmento de SQL: `"status = 'CANCELLED', refund = " + refund` (`ReservationService.java:142-143`).
  - As transições de situação também são `UPDATE` com texto (`ReservationService.java:112, 124, 132`).
  - Os dois relatórios montam SQL e CSV no mesmo método (`ReservationService.java:150-202`).
- **Defeito 2: o acesso a dados não está isolado.** Só as mensagens têm uma classe de acesso própria (`JdbcMessageStore`), e o serviço a cria por dentro com `new JdbcMessageStore(c)` (`ReservationService.java:115`).

O problema é localizado numa classe, não um sistema sem camadas, mas é ali que o próximo pedido vai doer. Num sistema deste tamanho, um pacote só não é defeito por si.

## Design de baixo nível

**Números:**
- Das 3 regras pedidas, as 3 estão na estrutura de regras do `MessageDispatcher`:
  - endereço inválido em `MessageDispatcher.java:50`;
  - cópia para o arquivo em `MessageDispatcher.java:63-65`;
  - desvio do modo de teste em `MessageDispatcher.java:66-70`.
- As 2 regras só anunciadas (madrugada e limite diário) também foram feitas na mesma estrutura (`MessageDispatcher.java:51-62`).
- Nenhum `if` de modo dentro de `dispatch` (`MessageDispatcher.java:82-100`).

**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente não fez embrulhos de um envio (Decorator). Fez uma lista de filtros aplicada pelo envio, em três fases:
- regras `BlockRule`, que bloqueiam e gravam BLOCKED com o endereço original;
- regras `ExpandRule`, que acrescentam a cópia;
- regras `TransformRule`, que desviam para a caixa de testes.

As interfaces estão em `MessageDispatcher.java:35-37`, e o laço que as aplica em `MessageDispatcher.java:82-100`. A referência aceita essa forma quando cada regra é uma peça própria e a combinação por modo fica fora das regras.

O que aproxima a solução da referência:
- Cada regra é um objeto separado de uma interface tipada.
- `dispatch` não pergunta pelo modo de teste.
- A combinação é decidida pela configuração de cada instalação, sem mexer no código: `Config.Scope` com ALWAYS, TEST_ONLY, NON_TEST_ONLY e NEVER (`Config.java:24-35`). Por padrão, a cópia vale só fora do teste (`Config.java:64`).
- Isso atende bem a "a segunda pousada não quer a cópia" (`HOTEL_ARCHIVE_COPY=NEVER`).

O que afasta:
- As regras são lambdas anônimas escritas dentro do construtor do próprio dispatcher (`MessageDispatcher.java:50-70`).
- A combinação por modo é feita ali, e não no `Main`.
- Uma regra nova obriga a editar o `MessageDispatcher`. As interfaces das regras são privadas ao pacote.
- O desvio do modo de teste é ligado por um `if (test)` fixo (`MessageDispatcher.java:66`). É uma escolha defensável, para que nada saia para endereço real.

**O exagero.**
- A interface `Store` tem duas implementações: `JdbcMessageStore` e o `MemStore` dos testes (`MessageDispatcherTest.java:21`).
- `Database.Work` é o callback da transação.
- Não há fábricas, builders nem camadas que só repassam. Por isso, contenção.
- As regras de madrugada e de limite foram feitas antes de serem pedidas. Ficam na parte do pattern e desligadas por padrão, então não contam como exagero.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` a cada passo (`Pricing.java:18-44`), `LocalDate`, e `enum` para plano, situação e status da mensagem (`Domain.java:10-32`). Os defeitos são localizados:
- **Situações escritas como texto no SQL, fora do enum.**
  - `'CONFIRMED'`, `'CHECKED_IN'`, `'CANCELLED'` em `ReservationService.java:112, 124, 132, 143`.
  - A regra "reserva paga" está escrita duas vezes, em `Domain.java:16-18` e no `IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')` de `ReservationService.java:157`.
- **Um `update` genérico que recebe a cláusula `SET` como texto concatenado** (`ReservationService.java:243-244`). Hoje só entra constante, mas o método convida a montar SQL à mão.
- **O tipo do quarto como `String` solta** (`Domain.java:21`).
- **A validação de data e de mês repetida com o mesmo truque** (`throw new DateTimeParseException("", "", 0)` em `WebApp.java:49` e `WebApp.java:77`).
- **O JSON montado à mão com `Map`**: `WebApp.java:143-199` repete os campos do comprovante e da reserva.

Os erros são tratados com cuidado: `ApiException` com código, e um tratador geral que registra a causa (`WebApp.java:57-61`).

## Testes

**Números:**
- 22 testes rodaram, 0 falhas, 0 erros, 0 pulados. 22 métodos escritos em 3 classes:
  - `ApiTest`: 9, pela API, contra o PostgreSQL;
  - `MessageDispatcherTest`: 8, de unidade, com um `Store` em memória;
  - `PricingTest`: 5.
- Cobertura: 419 de 461 linhas (90,9%) e 132 de 172 ramos (76,7%).

**Nota:** bom

**As regras comuns são conferidas com valores:**
- o exemplo da alta temporada, 599.76 (`PricingTest.java:15-21`);
- os limites da temporada (`PricingTest.java:24-31`);
- o meio-para-o-par 37.48 / 212.42 (`PricingTest.java:43-48`);
- o texto do comprovante e do e-mail, inteiros (`PricingTest.java:51-61`, `ApiTest.java:131-144`);
- as devoluções 249.90 / 0.00 / 0.00 (`ApiTest.java:168-183`);
- os códigos de erro (`ApiTest.java:186-207`);
- os dois CSV, comparados inteiros (`ApiTest.java:231-255`).

**A parte do cenário está bem coberta:**
- os endereços válidos e inválidos, com 10 casos (`MessageDispatcherTest.java:50-57`);
- o bloqueio sem cópia nos dois modos (`MessageDispatcherTest.java:73-84`, `ApiTest.java:221-228`);
- a cópia com "[Cópia] " e o mesmo texto (`MessageDispatcherTest.java:60-70`);
- o desvio de teste com o corpo exato (`MessageDispatcherTest.java:87-93`, `ApiTest.java:258-267`);
- a instalação sem cópia (`MessageDispatcherTest.java:104-107`);
- a madrugada e o limite diário (`MessageDispatcherTest.java:110-132`).

**A pergunta da mutação:**
- Trocar "[Cópia] " ou "[TESTE] ", apagar a regra do endereço ou trocar `at <= 0` por `at < 0` faz algum teste falhar.

**Ficam sem rede:**
- **A janela do dia no limite diário.** O `MemStore.countSent` ignora `from` e `to` (`MessageDispatcherTest.java:32-38`), então errar o início do dia passaria.
- **A linha "Desconto:" e o "1 diária" no texto do comprovante.** Não há teste do texto com desconto nem com uma noite só.
- **As duas reservas simultâneas.**

São lacunas pequenas diante do que está coberto.

## Banco

**Números:**
- 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária.
- Dinheiro em `numeric(12,2)` nas 6 colunas.
- A chave estrangeira `reservations.room_number` tem índice.
- As datas estão em `date`.
- 1 CHECK (`check_out > check_in`).
- As conferências acusaram as colunas de situação, plano, tipo e canal em `text` sem CHECK (5 colunas).
- O esquema é criado por `CREATE TABLE` no código. Não há migrações.

**Nota:** com problemas

- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` em `Database.java:44-103`, sem migrações versionadas. A primeira coluna nova numa tabela que já existe não vai ser aplicada a quem já roda o sistema.
- **O banco protege pouco.**
  - Não há CHECK em `status`, `rate_plan`, `type` nem `guests >= 1` (`Database.java:57-74`).
  - Não há restrição contra noites sobrepostas (por exemplo, `EXCLUDE`).
- **A sobreposição está protegida de um jeito que vale entre processos.** O `create` trava a linha do quarto com `SELECT … FOR UPDATE` (`ReservationService.java:215-224`), dentro de uma transação, antes de conferir e inserir (`ReservationService.java:63-105`). As mudanças de situação travam a reserva (`ReservationService.java:226-228`).
- **O resto está correto:**
  - Todo SQL usa parâmetros. Os candidatos com `+` só concatenam constantes (`ReservationService.java:142-143, 228, 244`).
  - As conexões vêm de um pool e são devolvidas por try-with-resources (`Database.java:27-41`).
  - Cada operação, inclusive a confirmação com o e-mail, roda numa transação só (`ReservationService.java:108-119`).
  - O `migrate` usa `pg_advisory_lock` para várias instâncias subindo juntas (`Database.java:48`).

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- o cenário no modo normal: 9 de 9;
- o cenário no modo de teste: 5 de 5;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer o seguinte:
- **A parte comum funciona como ele descreveu:** preços com temporada e arredondamento, recusas com os códigos combinados, confirmação com e-mail e comprovante, check-in e check-out, devoluções e os dois relatórios em CSV.
- **Os dados sobrevivem a um reinício.**
- **As regras de e-mail fazem o que ele pediu:**
  - um endereço como "ana@gmail" fica BLOCKED na lista, com o endereço original e sem cópia;
  - cada envio normal gera a cópia "[Cópia] " para o arquivo logo depois;
  - com `HOTEL_TEST_MODE=true`, tudo vai para a caixa de testes com "[TESTE] " e "Destinatário original:", sem cópia, e o bloqueio continua valendo.
- **Duas reservas simultâneas do mesmo quarto** nunca foram criadas juntas em 10 rodadas.

## Dúvidas

- **A classe do pattern ficou entre `alternativa equivalente` e `malfeito`.** As regras são peças separadas, e a combinação por modo vem da configuração da instalação, não de dentro das regras. Isso sustenta a alternativa equivalente. Mas as regras são lambdas escritas dentro do próprio `MessageDispatcher`, e é o construtor dele (`MessageDispatcher.java:45-71`), não o `Main`, que decide a combinação. Uma leitura mais estrita poderia ver aí a combinação dentro do envio.
- **O tempo é gravado com `now()` do banco.** O limite diário conta as mensagens por `created_at` (`JdbcMessageStore.java:43`), que vem do relógio do banco, enquanto a janela é calculada com o relógio da aplicação (`MessageDispatcher.java:57-60`). Os dois relógios só divergem se o banco e o serviço estiverem em máquinas com horas diferentes. A evidência não decidiu se isso importa na instalação real.
