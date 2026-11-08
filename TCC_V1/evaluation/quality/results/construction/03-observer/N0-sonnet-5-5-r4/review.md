# Revisão: construção, cenário 03

## Visão geral

O projeto tem seis pacotes: `web`, `service`, `events`, `persistence`, `domain` e a raiz com `Main`. Uma requisição entra em `web/Api.java`, que lê e valida o corpo (`Api.java:108-138`) e chama `ReservationService` ou `ReportService`. O serviço abre uma transação com `Database.inTransaction` (`persistence/Database.java:76-90`) e usa os repositórios JDBC que vêm no `Tx` (`persistence/Tx.java:12-17`). As regras de preço e de situação da reserva ficam em records imutáveis do domínio (`domain/Pricing.java`, `domain/Reservation.java:32-51`). Os dados ficam no PostgreSQL, em três tabelas criadas no próprio código ao subir. Os avisos da confirmação e do cancelamento seguem o Observer. `ReservationService` publica um `ReservationEvent` para uma lista de `NotificationRule` que recebe de fora (`ReservationService.java:28-31,113-117`). Hóspede, governança e financeiro são uma implementação cada um, registrada em `Main.java:23-28`. O roteiro de comportamento passou inteiro (66 de 66). O que há de mais sério é pouco:
- o esquema é criado com `CREATE TABLE IF NOT EXISTS` no código, sem migração versionada;
- o banco não tem nenhuma restrição CHECK;
- os testes da parte nova conferem os assuntos dos e-mails, mas não o texto da governança e do financeiro, nem qual destinatário recebe qual assunto.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `web` e na raiz, e o JDBC só em `persistence`. A maior classe é `web/Api.java`, com 159 linhas.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota é uma linha que lê a entrada, chama o serviço e escreve a resposta (`web/Api.java:41-51`). Os erros são traduzidos num lugar só (`Api.java:53-58`), a partir do `ErrorCode` com o status HTTP (`domain/ErrorCode.java:3-9`). A validação de formato (campos, datas, saída depois da entrada, ao menos um hóspede) fica na borda (`Api.java:118-136`). Ela é validação de entrada, não regra de negócio.
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService` não recebe `Context` nem monta SQL: ele usa os repositórios do `Tx` (`service/ReservationService.java:45-59`). As transições ficam no domínio (`domain/Reservation.java:32-51`), e o preço também (`domain/Pricing.java:12-22`).
- **O acesso ao banco fica isolado** em três repositórios e no `Database` (`persistence/`). Os repositórios nem podem ser criados fora do pacote, porque os construtores são de pacote (`ReservationRepository.java:21`, `Tx.java:12`).
- **A montagem fica num lugar só,** em `Main.createApp` (`Main.java:30-33`). Os avisos são registrados ali mesmo (`Main.java:23-28`).

Há um ponto pequeno. A lista dos quartos está no domínio (`domain/Room.java:9-14`), e quem a grava no banco é o `Database.migrate` (`persistence/Database.java:69-71`). Isso é aceitável para um catálogo que "quase nunca muda". Não há classe grande demais nem `static` mutável.

## Design de baixo nível

**Números:** 3 interessados, e os 3 estão na estrutura (hóspede, governança, financeiro). Nenhum e-mail é montado fora dela. Há 2 eventos, `Confirmed` e `Cancelled`, num `sealed interface`. O serviço não conhece nenhum destinatário.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A estrutura segue o Observer:
- `NotificationRule` é a interface do ouvinte (`events/NotificationRule.java:10-14`), com uma implementação por interessado: `GuestNotifications`, `HousekeepingNotifications` e `FinanceNotifications`.
- O e-mail do hóspede também é um ouvinte (`events/GuestNotifications.java:8-21`). Não sobrou nenhuma chamada direta no serviço.
- O serviço recebe a lista de fora (`ReservationService.java:28-31`) e a percorre sem perguntar quem é cada um (`ReservationService.java:113-117`).
- Os endereços entram pelo construtor, na montagem (`Main.java:26-27`). Pôr ou tirar o restaurante e o transfer é mexer só nessa lista.
- Cada ouvinte decide sozinho o que manda e quando:
  - a governança só é avisada do cancelamento de reserva paga (`HousekeepingNotifications.java:28`);
  - o financeiro só é avisado quando há devolução (`FinanceNotifications.java:24`).
- Os ouvintes devolvem mensagens em vez de enviá-las. O serviço as grava na mesma transação da mudança de situação (`ReservationService.java:94-110`), e assim o aviso não se perde nem sai sem a confirmação.

**O que afasta um pouco.** O serviço traduz a nova situação no evento com um `switch` e `default -> null` (`ReservationService.java:100-108`). Um evento novo, como o check-in que o transfer vai querer, pede um `case` ali e um record novo. O `switch` exaustivo sobre o `sealed interface` também obriga a mexer em todos os ouvintes que usam `switch` (`HousekeepingNotifications.java:21-33`, `FinanceNotifications.java:19-29`). O compilador ajuda a não esquecer nenhum, mas o "nada mais no sistema muda" do comentário (`NotificationRule.java:8`) só vale para um interessado novo, não para um evento novo.

**O exagero.** Não há estrutura sem necessidade. O `Tx` agrupa os repositórios de uma transação e tem uso real (`Tx.java`). Não há interface com uma implementação só fora do pattern, nem fábrica, nem builder.

**O resto do código.**
- Os tipos estão certos. O dinheiro é `BigDecimal`, arredondado com `HALF_EVEN` a cada passo (`domain/Money.java:16-18`, `domain/Pricing.java:18,25-26`). As datas são `LocalDate`, e a situação, o plano e o tipo são `enum`s.
- O estado é imutável: os records mudam por cópia (`Reservation.java:69-72`), e a guarda de situação fica num lugar só (`Reservation.java:74-82`).
- Há defeitos pequenos:
  - a cópia dos 16 campos se repete em `withId` e `copy` (`Reservation.java:64-72`);
  - o formato de data mora em `Receipt.DATE` e é importado pela governança (`HousekeepingNotifications.java:7`);
  - `room(tx, r)` usa `orElseThrow()` sem mensagem (`ReservationService.java:124`);
  - o `try/catch SQLException` se repete em cada método dos repositórios.

Nada disso dificulta a mudança.

## Testes

**Números:** 9 testes rodaram, 0 falhas, 0 erros, 0 pulados. Há 9 métodos de teste em 2 arquivos: 5 de unidade em `PricingTest` e 4 pela API contra o PostgreSQL em `ApiIntegrationTest`. A cobertura é de 89,6% das linhas (413 de 461) e de 80,0% dos ramos (92 de 115). Os pacotes `events` e `service` têm 100% de linhas (`measures/tests.md`).
**Nota:** com problemas

**O que os testes conferem com valores:**
- a alta temporada (`PricingTest.java:12-17`, os 599,76 do texto);
- as bordas da temporada, 14/12 e 15/12, 28/02 e 01/03 (`PricingTest.java:28-34`);
- o desconto com meio para o par, 37,48 (`PricingTest.java:37-45`);
- o texto do comprovante inteiro (`PricingTest.java:48-58`);
- o formato do dinheiro (`PricingTest.java:61-64`);
- todas as recusas com o código (`ApiIntegrationTest.java:97-108`);
- a liberação do quarto ao cancelar (`ApiIntegrationTest.java:110-114`);
- a devolução nos dois planos (`ApiIntegrationTest.java:174,187`);
- os dois relatórios, com linhas exatas (`ApiIntegrationTest.java:147-149,162,190`).

**Na parte nova,** os testes conferem:
- os assuntos dos e-mails (`ApiIntegrationTest.java:139-141,177-178`);
- quantas mensagens cada evento gera: 3 na confirmação, 2 no cancelamento com devolução, 1 sem devolução e 0 na reserva pendente (`ApiIntegrationTest.java:129,137,176,188`).

**A pergunta da mutação deixa buracos:**
- **Os destinatários trocados passam.** Os dois endereços são procurados na lista inteira convertida em texto (`ApiIntegrationTest.java:138,142-143`). Trocar `governanca@` por `financeiro@` em `Main.java:26-27` não derruba nenhum teste.
- **O texto da governança não é conferido.** Apagar as linhas de entrada, saída ou hóspedes (`HousekeepingNotifications.java:25-27`) não derruba nada.
- **O valor pago no aviso do financeiro não é conferido** (`FinanceNotifications.java:23`).
- **O valor do reembolso é conferido de um jeito fraco.** A asserção de "R$ 579,90" (`ApiIntegrationTest.java:179`) olha a lista inteira, que já tem o e-mail de confirmação do hóspede com "Total: R$ 579,90" e o "Valor pago" do financeiro. Ela passaria mesmo sem o valor no aviso de reembolso.
- **O texto do e-mail do hóspede só é conferido pelo assunto.**

As guardas `wasPaid` e `refund > 0` estão cobertas pelas contagens. Há mais um risco: o `ApiIntegrationTest` se desliga sozinho com `assumeTrue` quando não há banco (`ApiIntegrationTest.java:35,42`). Num ambiente sem `DB_URL`, o `mvn verify` passa com só os 5 testes de unidade.

## Banco

**Números:**
- 3 tabelas, todas com chave primária;
- 6 colunas de dinheiro, todas `numeric(12,2)`;
- 1 chave estrangeira (reserva para quarto), com índice;
- nenhuma data em texto;
- 0 restrições CHECK, UNIQUE ou EXCLUDE;
- 4 colunas de situação e tipo em `varchar` sem CHECK;
- o esquema é criado por `CREATE TABLE` no código Java, com 0 arquivos de migração (`measures/database.md`).

**Nota:** com problemas

**O que está certo:**
- os tipos (`persistence/Database.java:35-65`) e o `NOT NULL` no que é obrigatório;
- o índice `(room_number, check_in, check_out)` para a consulta de conflito (`Database.java:58`);
- o SQL sempre com `?`: os trechos concatenados que o script listou são só as constantes `COLUMNS` e `PAID` (`ReservationRepository.java:16-18`);
- o pool HikariCP (`Database.java:16-27`) e o try-with-resources em todo lugar;
- a transação explícita com rollback (`Database.java:76-90`).

**A reserva duplicada** é impedida por uma trava no banco: `SELECT ... FOR UPDATE` na linha do quarto (`RoomRepository.java:48-57`), antes de procurar conflito e inserir (`ReservationService.java:47-58`). A trava vale entre processos, e o roteiro confirmou isso (10 de 10 rodadas). A reserva também é travada nas transições (`ReservationRepository.java:72-74`), e a criação do esquema usa um advisory lock (`Database.java:33`).

**Os defeitos:**
- **O esquema não tem migração versionada.** É `CREATE TABLE IF NOT EXISTS` dentro de `Database.migrate` (`Database.java:34-65`), e o próximo pedido que acrescentar uma coluna numa tabela já existente não tem como evoluir o banco sem um passo à mão.
- **O banco não protege nenhuma regra por conta própria.** Não há CHECK em `status`, `rate_plan`, `guests >= 1` nem `check_out > check_in`, e não há EXCLUDE para a sobreposição. A trava do serviço é o único guarda.

Nada disso perde dado hoje.

## Comportamento

**Números:** 66 de 66 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o texto pede funcionou pela API, a partir de um banco vazio:
- os preços com temporada e meio para o par (599,76; 37,48; 212,42);
- as recusas com os códigos certos;
- o ciclo da reserva e as devoluções;
- os dois relatórios em CSV;
- os dados depois de reiniciar.

Na parte do cenário:
- Na confirmação, o hóspede recebe o e-mail com o comprovante. A governança recebe "Reserva confirmada - quarto 201", com as datas e os hóspedes, e o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.
- No cancelamento de uma reserva confirmada, a governança recebe as datas liberadas e o financeiro recebe o reembolso com o valor.
- Na NON_REFUNDABLE cancelada, só a governança é avisada.
- A reserva pendente cancelada não manda nada.

Para o dono da pousada, isso quer dizer que a governança e o financeiro ficam sabendo do que precisam. As duas reservas simultâneas para o mesmo quarto nunca passaram juntas.

## Dúvidas

Nenhuma sobre as notas. Um único ponto de leitura: o `switch` que traduz a situação no evento (`ReservationService.java:100-108`) faz um evento novo (por exemplo, o check-in para o transfer) mexer no serviço e nos ouvintes com `switch` exaustivo. Isso não muda a classe, porque o serviço continua sem conhecer nenhum interessado e a referência pede ouvintes por interessado, recebidos de fora.
