# Revisão: construção, cenário 01

## Visão geral

O projeto tem seis pacotes: `web` (Javalin e Jackson: `Api`, `BookingParser`, `Views`), `service` (`ReservationService`, `ReportService`, o e-mail de confirmação), `domain` (reserva, quarto, dinheiro, comprovante, erros), `plan` (os cinco planos de tarifa), `persistence` (JDBC puro: `Database`, `ReservationRepository`, `MessageRepository`) e `App`, que monta as peças. Uma requisição entra numa rota de uma linha em `web/Api.java:33-45`. O `BookingParser` transforma o corpo num `BookingRequest`, e o `ReservationService` valida o quarto e a capacidade e pede o preço ao plano (`service/ReservationService.java:39-40`). Depois ele grava tudo numa transação, com uma trava do PostgreSQL por quarto (`persistence/ReservationRepository.java:26-31`). Os dados ficam em duas tabelas do PostgreSQL (`reservations` e `messages`), criadas por `CREATE TABLE IF NOT EXISTS` no código. Os quartos não estão no banco: são uma lista fixa em `domain/Rooms.java:8-13`. O comportamento está todo certo: o roteiro passou 72 de 72. Não há defeito sério. Os pontos fracos são do tipo que se corrige depois: o esquema sem migrações e sem restrições no banco, um ciclo pequeno entre `domain` e `plan`, e testes que deixam parte das regras novas sem conferir.

## Arquitetura

**Números:** 6 pacotes, 13 dependências entre eles, 1 ciclo (`domain` ↔ `plan`). JDBC aparece em `persistence` e em `service`; Javalin só em `web` e na `App` (`measures/dependencies.md`). A maior classe tem 131 linhas (`ReservationRepository`).
**Nota:** com problemas

A borda HTTP é fina. Cada rota lê a requisição, chama o serviço e escreve a resposta (`web/Api.java:33-45`), e os erros viram JSON num lugar só (`web/Api.java:47-52`, com o status em `domain/ErrorCode.java:3-9`). As regras não conhecem o Javalin: as transições de situação ficam em `domain/Reservation.java:48-68`, o preço e a devolução nos planos. O SQL fica isolado nos repositórios. A montagem acontece num lugar só, `App.java:18-22`, com as dependências passadas pelo construtor. Os tamanhos são equilibrados, sem nenhuma classe que faz de tudo.

Os defeitos:
- **Um ciclo novo entre `domain` e `plan`.** A `Reservation` guarda o objeto `RatePlan` (`domain/Reservation.java:3`, `:17`) e o chama no cancelamento (`domain/Reservation.java:67`). Os planos, por sua vez, dependem de `Stay`, `Quote`, `Money` e `BusinessException`, que estão em `domain` (por exemplo, `plan/CorporatePlan.java:3-4`). O ciclo é pequeno e entre pacotes vizinhos, mas a regra da skill o conta.
- **O serviço conhece o JDBC.** Ele importa `java.sql.Connection` e `SQLException` (`service/ReservationService.java:15-16`), declara uma interface interna que recebe uma `Connection` (`service/ReservationService.java:83-85`) e repassa a conexão aos repositórios (`:44-52`, `:87-95`). Não monta SQL, mas a fronteira da transação vaza a API do JDBC para a camada de regras. O mesmo acontece em `service/ReportService.java:24` e `:36-37`.

## Design de baixo nível

**Números:** os 5 planos (`STANDARD`, `NON_REFUNDABLE`, `CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY`) estão todos na estrutura, cada um com o seu preço, a sua validação e a sua devolução. Nenhum `switch` ou `if` sobre o plano aparece fora dela. O plano é escolhido num único mapa (`plan/RatePlans.java:12-18`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** O pattern é o Strategy. A interface `RatePlan` (`plan/RatePlan.java:9-18`) reúne os três pontos de variação que o texto descreve: `quote`, que calcula o preço e recusa a estadia que não pode usar o plano, e `refund`. Cada plano tem a sua classe:
- O normal e o não reembolsável, que já existiam no texto, também entraram na estrutura (`plan/StandardPlan.java:12-20`, `plan/NonRefundablePlan.java:13-21`).
- O convênio guarda o seu mapa de empresas e a sua recusa (`plan/CorporatePlan.java:11-31`).
- O pacote tem o preço fechado por tipo de quarto, a regra de sexta a domingo e a devolução de metade (`plan/WeekendPackagePlan.java:15-38`).
- A estadia longa exige 7 noites e dá 10% de desconto (`plan/LongStayPlan.java:15-25`).

Quem usa o plano não pergunta qual ele é:
- o serviço só chama `request.ratePlan().quote(...)` (`service/ReservationService.java:39-40`);
- o cancelamento chama `ratePlan.refund(amountPaid)` (`domain/Reservation.java:67`);
- a leitura do banco reconstrói o plano pelo código, pelo mesmo ponto de seleção (`persistence/ReservationRepository.java:126`).

Um plano novo é uma classe e uma linha em `plan/RatePlans.java:13-17`. A classe abstrata `DiscountedNightsPlan` (`plan/DiscountedNightsPlan.java:22-35`) reúne o cálculo comum aos quatro planos de "noites de tabela menos um percentual" sem impedir o pacote de implementar `RatePlan` direto. Ela faz parte da solução do pattern, não é exagero. O único ponto de dúvida é o `companyCode`: ele viaja em `Stay` para todos os planos (`domain/Stay.java:8`), mas só o convênio o lê. É um dado de entrada, não uma regra fora do plano.

**O exagero.** Não há estrutura sem necessidade fora do pattern: nenhuma interface com uma implementação só, nenhuma fábrica, nenhuma camada que só repassa. Os tipos auxiliares têm uso real: `Stay`, `Quote`, `Database.Work` (`persistence/Database.java:19-22`) e a interface privada `SideEffect` (`service/ReservationService.java:83-85`).

**O resto do código.** O dinheiro é `BigDecimal`, arredondado com `HALF_EVEN` em cada passo, num lugar só (`domain/Money.java:16-27`). As datas são `LocalDate`, e a situação, o tipo de quarto e o canal são `enum` (`domain/ReservationStatus.java:3-10`, `domain/RoomType.java:5-8`). As transições de situação ficam centralizadas num único método de verificação (`domain/Reservation.java:79-86`), e as quatro operações do serviço passam por um mesmo `change` (`service/ReservationService.java:87-95`), sem duplicação. Os nomes dizem o que as coisas fazem, e os métodos são curtos.

Só pontos menores:
- O construtor da `Reservation` tem 13 parâmetros (`domain/Reservation.java:23-25`).
- A coluna `total` é gravada (`persistence/ReservationRepository.java:61`) e nunca lida: o total é recalculado a partir de `subtotal - discount` (`domain/Quote.java:6-8`, `ReservationRepository.java:22-23`).
- Um `companyCode` que não é texto é descartado em silêncio (`web/BookingParser.java:45-46`).

Nada disso pede mudança antes de aceitar.

## Testes

**Números:** 9 testes rodaram, nenhum falhou (0 erros, 0 pulados). São 9 métodos escritos: 8 de unidade em `PlansTest` e 1 de ponta a ponta, pela API e contra o banco, em `ApiTest`. A cobertura é de 92,5% das linhas (456 de 493) e de 77,5% dos ramos (79 de 102). O pacote `plan` tem 49 de 51 linhas e 15 de 16 ramos cobertos (`measures/tests.md`).
**Nota:** com problemas

O que os testes conferem bem, com valores:
- os três exemplos do texto, com diárias, desconto e total (`src/test/java/br/com/marealta/PlansTest.java:40-47`, `:51-55`, `:63-68`);
- a alta temporada com o arredondamento (`PlansTest.java:26-30`) e o desconto do não reembolsável (`PlansTest.java:33-37`);
- as recusas do convênio (sem código, em minúsculas, código inexistente), do pacote (sábado; três noites) e da estadia longa com 6 noites;
- o preço fechado do pacote em alta temporada (`PlansTest.java:57`);
- o texto do comprovante e o formato `R$ 1.234,56` (`PlansTest.java:83-94`).

O `ApiTest` percorre o fluxo inteiro e confere valores:
- os códigos 409, 422, 400 e 404;
- o `amountPaid`;
- o e-mail com a linha de desconto;
- o CSV de ocupação inteiro;
- a devolução de 410.00 do pacote cancelado;
- o financeiro com a não reembolsável cancelada contando (`ApiTest.java:172-236`, nas linhas do arquivo).

Ele usa um schema descartável no banco de `DB_URL`, mas é um único método com mais de 30 asserções. Uma falha no começo esconde todo o resto.

A pergunta da mutação deixa sem rede parte da regra nova:
- trocar 0.18 (Agrovale) ou 0.25 (Farmavida) em `plan/CorporatePlan.java:13-14` não faz nenhum teste falhar, porque só a Tecnosul é testada;
- fazer `CorporatePlan.refund` ou `LongStayPlan.refund` devolver zero (`plan/CorporatePlan.java:34-36`, `plan/LongStayPlan.java:28-30`) também passa, porque `refunds` só confere o normal e o não reembolsável (`PlansTest.java:72-75`);
- a estadia longa com noites de alta temporada não é testada, nem o limite exato de 7 noites aceito contra 6 recusadas pela API.

Também não há teste do cancelamento de uma reserva pendente (devolução zero) nem de duas reservas ao mesmo tempo.

## Banco

**Números:** 2 tabelas (`reservations` com 16 colunas, `messages` com 5), as duas com chave primária. O dinheiro está em `numeric(12,2)` nas 5 colunas, e as datas em `date`. As conferências acusaram:
- 0 chaves estrangeiras e 0 restrições (CHECK, UNIQUE ou EXCLUDE);
- `status`, `rate_plan` e `channel` em `text` sem CHECK;
- o esquema criado por `CREATE TABLE` no código, sem migrações;
- nenhum pool, com 2 chamadas a `DriverManager.getConnection`;
- 4 trechos de SQL montados com `+`, todos com constantes (`measures/database.md`).

**Nota:** com problemas

O que está certo:
- **Os tipos e o SQL.** Os tipos estão certos, e o `NOT NULL` está em tudo o que é obrigatório: só `refund` aceita nulo, como o texto pede (`persistence/Database.java:47-65`). Todo valor de fora entra por `?`. Os quatro trechos montados com `+` juntam só constantes: `COLUMNS` (`persistence/ReservationRepository.java:22-23`) e cláusulas fixas (`:36`, `:96-97`, `:102`).
- **A duplicação de reserva.** Ela é protegida entre processos, não só dentro de um: cada criação trava o quarto com `pg_advisory_xact_lock` dentro da transação antes de conferir a sobreposição (`ReservationRepository.java:26-31`, `service/ReservationService.java:44-52`). As transições usam `SELECT ... FOR UPDATE` (`ReservationRepository.java:74-75`).
- **As escritas que andam juntas.** A confirmação e o e-mail são gravados na mesma transação (`service/ReservationService.java:60`, `:87-95`). As conexões são devolvidas com try-with-resources (`persistence/Database.java:25`).

Os defeitos:
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:47-74`), sem migração versionada. A próxima coluna, por exemplo o `companyCode` se um dia for guardado, não chega a um banco que já existe.
- **O banco não protege nenhuma regra sozinho.**
  - Os quartos não são tabela, então `room_number` não tem chave estrangeira (`domain/Rooms.java:8-13`).
  - Não há CHECK sobre `status` e `rate_plan`.
  - Não há EXCLUDE contra noites sobrepostas: a regra depende de todo código que grava passar pela trava.
- **Não há pool.** Cada transação abre uma conexão nova com `DriverManager` (`persistence/Database.java:25`).

## Comportamento

**Números:** 72 de 72 conferências passaram, nenhuma falhou (`measures/behavior.md`):
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o texto descreve funciona pela API:
- os três exemplos do financeiro (R$ 686,22, R$ 820,00 com devolução de R$ 410,00, R$ 1.574,37);
- os descontos da Agrovale (R$ 140,36) e da Farmavida (R$ 194,95);
- as recusas com 400 `INVALID_REQUEST` (convênio sem código ou com código inexistente, pacote fora de sexta a domingo, estadia longa com 6 noites);
- a temporada valendo na estadia longa (R$ 1.754,30);
- as devoluções integrais do convênio e da estadia longa;
- o `companyCode` ignorado nos outros planos.

A parte comum também passa inteira: preço, arredondamento, situações, e-mail, comprovante, relatórios. Os dados continuam lá depois de reiniciar, e em 10 rodadas de duas reservas simultâneas para o mesmo quarto só uma foi criada em cada uma. Para o dono da pousada, o sistema faz o que ele pediu.

## Dúvidas

Na arquitetura, o ciclo `domain` ↔ `plan` vem de uma escolha defensável: a `Reservation` guarda o seu plano para pedir a devolução. Ele pesou para `com problemas` junto com o JDBC no serviço. Um revisor que não contasse esse ciclo pequeno poderia dar `bom`. Nos testes, a nota fica no limite entre `bom` e `com problemas`: a cobertura é alta e os exemplos do texto são conferidos, mas faltam duas das três empresas e as devoluções do convênio e da estadia longa.
