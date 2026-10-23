# Revisão: construção, cenário 07

## Visão geral

O projeto tem cinco pacotes: `web` (rotas Javalin, leitura do JSON, visões e `ApiException`), `reservation` (regras de reserva, preço, comprovante, relatórios e os repositórios JDBC), `message` (as regras de envio e o repositório das mensagens), `db` (o pool Hikari e a transação) e a raiz (`App`, `AppConfig`, `Money`). Uma requisição passa por `Routes` (`web/Routes.java:47-64`), que chama `ReservationService` ou `ReportService`. O serviço abre uma transação por operação com `Database.run` (`db/Database.java:80-94`), carrega a reserva com `FOR UPDATE`, aplica a transição no record imutável `Reservation` (`reservation/Reservation.java:16-35`) e grava pelo repositório. Na confirmação, o e-mail passa pelo `MessageDispatcher`, que roda a `MessagePipeline` e grava as mensagens na mesma transação (`reservation/ReservationService.java:55-61`). Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código. O roteiro do comportamento passa inteiro (65 de 65) e os 14 testes passam com 94,6% de cobertura de linhas. Não achei nada que quebre o sistema. Os defeitos mais sérios são de estrutura: o domínio lança a `ApiException` do pacote `web`, o que cria um ciclo entre `web` e `reservation`, e o esquema não tem migrações nem restrições `CHECK`.

## Arquitetura

**Números:** 5 pacotes, 11 dependências entre eles, 2 ciclos (`web` ↔ `reservation` e raiz → `reservation` → raiz), de `measures/dependencies.md`. Javalin só em `web` e na raiz; JDBC em `db`, `message`, `reservation` e `web`.
**Nota:** com problemas

O que está bem:
- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e escreve a resposta (`web/Routes.java:47-64`). A leitura e a validação do corpo ficam em `web/ReservationRequestParser.java:17-36`, e o formato do JSON em `web/Views.java`.
- **O SQL fica isolado** nos repositórios (`reservation/ReservationRepository.java`, `reservation/RoomRepository.java`, `message/MessageRepository.java`).
- **A montagem fica num lugar só,** `App.start` (`App.java:24-41`), sem `new` de repositório dentro dos serviços.
- **As regras não conhecem o Javalin.**

O que pesa:
- **O ciclo `web` ↔ `reservation`.** O domínio depende da borda HTTP: `Reservation.java:4,44`, `NewReservation.java:4,12` e `ReservationService.java:6,35,37,40,81,103` lançam `br.com.marealta.web.ApiException`, uma exceção que carrega o status HTTP (`web/ApiException.java:5,8`). A regra da reserva fica presa ao vocabulário do HTTP.
- **O ciclo da raiz** vem de `Money`, que mora no pacote raiz (`Money.java:1`) e é usado pelas regras (`reservation/Pricing.java:3`, `reservation/RatePlan.java:3`). A raiz também monta tudo. Ele é pequeno, mas mostra uma classe utilitária no lugar errado.
- **Uma rota fala com o banco sem passar por um serviço:** `Routes` recebe o `Database` e abre a transação de `GET /messages` sozinha (`web/Routes.java:25,61-62`).
- **SQL fora dos repositórios:** `MessageDispatcher` executa um `pg_advisory_xact_lock` direto (`message/MessageDispatcher.java:20-22`).
- **O serviço recebe o id como texto** e converte o número, um trabalho da borda (`reservation/ReservationService.java:98-104`).

São misturas localizadas, mais dois ciclos curtos entre vizinhos. Nenhuma classe é desproporcional: a maior tem 125 linhas.

## Design de baixo nível

**Números:** as 3 regras do texto (validar o endereço, copiar para o arquivo, desviar no modo de teste) estão na estrutura, cada uma numa classe que implementa `MessageRule`. Nenhuma ficou fora dela, e nenhuma regra pergunta pelo modo.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Não é um Decorator: as regras não embrulham o envio. É uma lista de filtros que a `MessagePipeline` aplica sobre um objeto `Delivery` mutável (`message/MessagePipeline.java:16-27`), e a referência aceita esse formato de antemão:
- **Cada regra é uma peça própria:** `message/AddressValidationRule.java:12-16`, `message/ArchiveCopyRule.java:18-20` e `message/TestRedirectRule.java:18-22`. Nenhuma delas consulta o modo de teste.
- **A combinação por modo é decidida na montagem.** Ela é uma configuração com escopo por regra (`message/RuleCatalog.java:15`, `"address-validation,test-redirect:test,archive-copy:live"`), lida em `App.java:34`. O escopo é genérico (`message/RuleScope.java:7-13`) e atende ao "sempre, só no teste ou só fora dele" que o texto anuncia.
- **Outra instalação pode tirar a cópia** só pela variável `HOTEL_MESSAGE_RULES` (`AppConfig.java:16`).
- **Uma regra nova** é uma classe e uma linha no catálogo (`message/RuleCatalog.java:17-20`).

O que afasta do ideal: a regra "mensagem bloqueada não gera cópia" depende da ordem configurada. A pipeline só para depois do bloqueio (`message/MessagePipeline.java:19-21`), e `Delivery.result()` devolve as cópias mesmo com a mensagem bloqueada (`message/Delivery.java:54-58`). Uma instalação que escreva `archive-copy` antes de `address-validation` mandaria a cópia de uma mensagem bloqueada. Do mesmo jeito, `test-redirect` antes da validação faria validar o endereço de testes, e o endereço inválido passaria.

**O exagero.** Não há estrutura sobrando: nenhuma fábrica, builder ou camada que só repassa. `ConfiguredRule` (`message/ConfiguredRule.java:3`) e `RuleScope` fazem trabalho de verdade. O único resto é `MessageRule.name()` (`message/MessageRule.java:6`). As três regras implementam esse método, mas ninguém o chama, porque o catálogo repete os nomes nas próprias chaves (`message/RuleCatalog.java:17-20`). É código morto, não estrutura.

**O resto do código.** Os tipos estão bem escolhidos: `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:17-19`, `reservation/Pricing.java:36-41`), `LocalDate`, e `enum` para a situação e o plano, com o desconto e a devolução no próprio `RatePlan` (`reservation/RatePlan.java:20-26`). As transições ficam num record imutável, com uma guarda só (`reservation/Reservation.java:42-47`). Os defeitos são localizados:
- o tipo do quarto é texto solto (`reservation/Room.java:5`, `reservation/RoomRepository.java:16-20`), não um `enum`;
- o domínio lança uma exceção com o status HTTP dentro (`reservation/Reservation.java:44`, `reservation/NewReservation.java:12`);
- o `Delivery` é mutável e a correção depende da ordem das regras (o problema descrito acima);
- `name()` está duplicado e não é usado.

## Testes

**Números:** 14 testes rodaram, 0 falhas, 0 erros, 0 pulados; 14 métodos de teste escritos. Cobertura: 94,6% das linhas (510 de 539) e 81,7% dos ramos (107 de 131); no pacote `message`, 112 de 119 linhas e 39 de 45 ramos (`measures/tests.md`).
**Nota:** bom

Os testes conferem as regras com valores:
- **Preço:** a fronteira da alta temporada, inclusive 29/02 e 14/12 contra 15/12 (`PricingTest.java:269-276`), o exemplo do texto, 599,76 (`PricingTest.java:280-284`), o desconto de 116,97 (`PricingTest.java:288-294`), a estadia que atravessa a temporada (`PricingTest.java:297-301`) e o "meio para o par" (`PricingTest.java:305-306`).
- **API:** o e-mail inteiro, texto a texto (`ApiTest.java:114-127`), as devoluções (`ApiTest.java:149,152`), o relatório financeiro exato, com a não reembolsável cancelada contando (`ApiTest.java:161-163`), a ocupação na noite de saída (`ApiTest.java:134`) e todos os códigos de erro (`ApiTest.java:177-190`).
- **A parte do cenário:** tem testes de unidade próprios (`MessageRulesTest.java`):
  - a cópia com assunto e texto (`:212-218`);
  - seis endereços inválidos bloqueados, sem cópia e com o endereço original (`:223-228`);
  - o desvio com "[TESTE] " e a linha do destinatário original (`:233-237`);
  - o bloqueio no modo de teste (`:242-245`);
  - a instalação sem a cópia (`:250`).

A pergunta da mutação: trocar `at <= 0` por `at < 0` (`message/AddressValidationRule.java:23`) seria pego por `"@gmail.com"`. Tirar o `break` do bloqueio (`message/MessagePipeline.java:19-21`) seria pego em `MessageRulesTest.java:244`. Mudar o prefixo "[Cópia] " seria pego em `:217`.

Ficaram sem rede:
- a linha "Desconto:" e o singular "1 diária" do comprovante (`reservation/Receipt.java:29,31-33`);
- a leitura de `HOTEL_TEST_MODE` (`AppConfig.java:15`), porque o modo de teste só é testado pela pipeline, nunca pela variável;
- os erros de configuração do catálogo (`message/RuleCatalog.java:39-45`).

Mais dois detalhes:
- **A devolução do STANDARD confirmado** só é conferida de forma indireta, pelo relatório (`ApiTest.java:156-163`).
- **O `ApiTest` é pulado quando falta `DB_URL`** (`ApiTest.java:40`). Aqui ele rodou contra o banco de verdade, num esquema descartável.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`, e a chave estrangeira `reservations.room_number` tem índice. As conferências acusaram as 5 colunas de situação e tipo sem `CHECK`, nenhuma restrição `UNIQUE`/`EXCLUDE`/`CHECK` e 0 arquivos de esquema ou migração (`measures/database.md`).
**Nota:** com problemas

O que está bem:
- **O dado é guardado certo:** os tipos estão corretos, `NOT NULL` em tudo o que é obrigatório e `refund` anulável como o texto pede (`db/Database.java:17-49`).
- **O SQL usa parâmetros em todo lugar.** As únicas concatenações são constantes: o `for update` em `reservation/ReservationRepository.java:60` e o `search_path` dos testes em `db/Database.java:64`, que vale `null` em produção (`App.java:19`).
- **As conexões são bem tratadas:** pool Hikari, try-with-resources e uma transação por operação (`db/Database.java:80-94`). A mudança de situação e a gravação do e-mail entram juntas (`reservation/ReservationService.java:56-61`).
- **Os relatórios filtram no SQL** (`reservation/ReservationRepository.java:84-94`), sem trazer a tabela inteira.
- **A reserva dupla é barrada por uma trava no banco,** não no processo: o `SELECT ... FOR UPDATE` na linha do quarto (`reservation/RoomRepository.java:47-50`, usado em `reservation/ReservationService.java:34`) vale entre vários processos.
- **Uma trava consultiva serializa o envio** (`message/MessageDispatcher.java:21`), para a cópia ganhar o id logo depois da original.

O que pesa:
- **O esquema não evolui.** Ele é criado no código com `CREATE TABLE IF NOT EXISTS` (`db/Database.java:16-50,69-77`), sem migrações versionadas, e o próximo pedido que acrescentar uma coluna não terá como mudar um banco que já existe.
- **O banco não protege nenhuma regra.** Não há `CHECK` na situação, no plano, em `guests >= 1` nem em `check_out > check_in`. Também não há restrição `EXCLUDE` contra a sobreposição de noites, e a proteção depende de todo código que grava passar pela trava.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| depois de reiniciar | 3 de 3 |
| cenário, modo normal | 9 de 9 |
| cenário, modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 2 de 2 (em 10 rodadas, nunca as duas criadas) |

**Nota:** bom

Tudo o que o dono da pousada descreveu funciona pela API:
- o preço com a temporada e o "meio para o par";
- as recusas com os códigos certos;
- a confirmação com o e-mail e o comprovante;
- o check-in e o check-out;
- as devoluções por plano;
- os dois relatórios em CSV.

Os dados continuam lá depois de reiniciar o serviço. No cenário, o endereço inválido fica bloqueado, com o endereço original e sem cópia, nos dois modos. A cópia para o arquivo vem logo depois da original. No modo de teste, tudo vai para a caixa de testes com "[TESTE] " e o destinatário original, sem a cópia. Duas reservas do mesmo quarto ao mesmo tempo nunca são criadas as duas.

Fora do roteiro, o problema da ordem das regras (contado no design) não aparece, porque a ordem padrão está certa (`message/RuleCatalog.java:15`). Ele só apareceria numa instalação configurada com outra ordem.

## Dúvidas

- **A classe do pattern.** Dei `alternativa equivalente`, e não `acerto`, porque as regras são filtros sobre um `Delivery` mutável e não embrulhos do envio. Considerei `malfeito` pela regra "bloqueada não gera cópia", que depende da ordem configurada. Não dei porque nenhuma regra pergunta pelo modo, a combinação fica na montagem e a configuração padrão está certa.
- **A nota do design.** Ficou entre `bom` e `com problemas`. Os defeitos são pequenos e localizados (o tipo do quarto em texto, `name()` sem uso, a dependência da ordem). Dei `com problemas` porque um revisor exigente pediria para corrigi-los.
