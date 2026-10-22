# Revisão: construção, cenário 03

## Visão geral

O projeto (`projeto/`, 1.308 linhas em 26 arquivos) está dividido em sete pacotes: `web` (a classe `Api`, com as rotas
do Javalin, a leitura do JSON e a serialização), `service` (`ReservationService` e `ReportService`), `domain`
(registros imutáveis, enums, `Pricing`, `Money`, `Receipt`), `event` (os eventos de domínio selados, `Message` e a
interface `Subscriber`), `notify` (um ouvinte por interessado: hóspede, governança, financeiro), `store` (a interface
`Store` com uma transação `Tx`, a versão JDBC com HikariCP e uma versão em memória para os testes) e `Main`, onde tudo
é montado. Uma requisição entra em `Api` (`web/Api.java:49-78`), que converte o corpo e chama o serviço; o serviço
abre uma transação em `Store.tx` (`store/JdbcStore.java:77-91`), aplica a regra e, na confirmação ou no cancelamento,
publica um `DomainEvent` para a lista de `Subscriber` que recebeu de fora (`service/ReservationService.java:119-123`).
As mensagens que os ouvintes devolvem são gravadas na mesma transação. Os dados ficam em três tabelas do PostgreSQL,
criadas por `CREATE TABLE IF NOT EXISTS` no código. O roteiro do comportamento passou inteiro (66 de 66), e o Observer
está bem aplicado. O mais sério é pouco: a API e o JDBC não têm nenhum teste (cobertura de 0% em `web` e de 24 em 161
linhas em `store`), e o esquema não tem migrações versionadas nem restrições `CHECK`.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo. O Javalin aparece só em `web` e em `Main`, o JDBC e
o pool só em `store`, o Jackson só em `web`. A maior classe é `JdbcStore`, com 287 linhas, seguida de `Api`, com 203.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota lê o parâmetro, chama um método do serviço e serializa o resultado
  (`web/Api.java:49-62`). A validação de formato (campo que falta, JSON inválido, data no formato errado) fica na borda
  (`web/Api.java:84-124`), que é onde ela deve ficar. As regras (noites, capacidade, conflito de datas, situação da
  reserva) ficam no serviço (`service/ReservationService.java:42-117`).
- **As regras não conhecem nem o Javalin nem o SQL.** `ReservationService` só fala com `Store.Tx`
  (`service/ReservationService.java:46-55`), e o jdeps não mostra nenhuma biblioteca em `service`, `domain`, `event` ou
  `notify`.
- **O acesso ao banco está isolado** em `store/JdbcStore.java`, e todo o SQL mora ali.
- **A montagem fica num lugar só:** `Main.java:16-24` cria o `JdbcStore`, a lista de ouvintes, os serviços e a `Api`.
  Nenhuma classe cria as próprias dependências por dentro.
- As dependências apontam da borda para as regras e das regras para os dados, sem ciclo.

Ressalvas menores, que não mudam a nota:
- `AppException` fica em `domain` mas carrega o código HTTP (`domain/AppException.java:7`, `:16-21`), o que põe um
  detalhe da borda dentro do domínio.
- `ReportService` monta o CSV, que é formato de saída, dentro do serviço (`service/ReportService.java:32-38`, `:48-56`).
- `Message`, que é gravada no banco, mora em `event`, e é só por isso que `store` depende de `event`.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança e financeiro) e 2 eventos (`ReservationConfirmed` e
`ReservationCancelled`), todos dentro da estrutura e nenhum fora dela. O serviço não monta nenhum e-mail e não conhece
nenhum destinatário. Os ouvintes são registrados num único ponto, `Main.java:17`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern pedido (Observer):**
- **O que aproxima.**
  - Há uma interface de ouvinte, `Subscriber.on(DomainEvent)` (`event/Subscriber.java:9-12`), com uma
    implementação por interessado: `notify/GuestNotifier.java:12-22`, `notify/HousekeepingNotifier.java:14-30` e
    `notify/FinanceNotifier.java:14-28`.
  - O e-mail do hóspede também é um ouvinte, e não uma chamada direta.
  - `ReservationService` recebe a lista pronta no construtor (`service/ReservationService.java:29-32`), anuncia o
    evento na confirmação (`:68`) e no cancelamento (`:83`) e percorre os ouvintes sem saber quem são (`:119-123`).
  - Cada ouvinte decide sozinho se o evento lhe interessa: a governança não é avisada quando a reserva cancelada não
    estava confirmada (`HousekeepingNotifier.java:26`, `:29`), e o financeiro não é avisado quando não há nada a
    devolver (`FinanceNotifier.java:23`, `:26`).
  - Os eventos formam uma hierarquia selada (`event/DomainEvent.java:9-21`), e o compilador obriga cada `switch` dos
    ouvintes a tratar todos os casos.
  - Para pôr o restaurante ou o transfer, basta escrever mais uma classe e registrá-la em `Main.java:17`, sem tocar no
    serviço.
- **O que afasta, pouco.**
  - O ouvinte devolve `List<Message>` em vez de agir por conta própria (`Subscriber.java:11`). Hoje isso serve, porque
    todo aviso é um e-mail, e ainda põe o aviso na mesma transação da mudança de situação. Mas um interessado futuro
    que precise de outra coisa (chamar a API do transfer, por exemplo) não cabe nesse contrato sem mudá-lo.
  - Quem decide o valor do reembolso e se a reserva estava confirmada é o serviço, que já calcula o valor e o passa no
    evento (`service/ReservationService.java:79-80`, `event/DomainEvent.java:20`). É uma escolha razoável.

**O exagero:** não há estrutura sem uso.
- `Store` e `Tx` têm duas implementações, e a de memória é usada de verdade nos testes
  (`src/test/java/br/com/marealta/ReservationFlowTest.java:29-31`).
- Não há fábricas, builders nem camadas que só repassam a chamada.

**O resto do código** é limpo:
- O dinheiro é `BigDecimal`, arredondado com `HALF_EVEN` a cada passo (`domain/Money.java:15-17`,
  `domain/Pricing.java:24`, `:34-37`).
- As datas são `LocalDate`, e situação, plano e tipo de quarto são enums.
- Os registros são imutáveis, com métodos `with…` (`domain/Reservation.java:12-25`), e não há `static` mutável.
- A máquina de situações cabe em poucos métodos curtos (`service/ReservationService.java:62-106`).

Pontos que um revisor apontaria, todos localizados:
- O construtor de 16 argumentos posicionais de `Reservation` é repetido em `domain/Reservation.java:12-25` (três
  vezes), em `store/MemoryStore.java:39-41` e em `store/JdbcStore.java:187-189`. Um `withId` evitaria as duas cópias
  dos `store`.
- `Api` lança uma `DateTimeParseException` falsa só para cair no `catch` (`web/Api.java:72`, `:119`).
- A formatação de datas fica em `Money` (`domain/Money.java:33-35`), um nome que não diz isso.
- Em `JdbcStore.tx`, se o `rollback` falhar, a exceção dele esconde a original (`store/JdbcStore.java:84-87`).

## Testes

**Números:** 9 testes rodaram, nenhum falhou ou deu erro, nenhum foi pulado. São 9 métodos de teste em 2 classes. A
cobertura é de 215 de 481 linhas (44,7%) e de 96 de 140 ramos (68,6%). Por pacote: `domain` 65/66, `service` 95/96,
`notify` 25/25, `store` 24/161 (só a versão em memória), `web` 0/121. Não há testes pela API nem contra o banco.
**Nota:** com problemas

**O que os testes conferem bem, com valores:**
- O preço na alta temporada (R$ 599,76) e o plano não reembolsável (499,80, desconto de 74,97 e total de 424,83), em
  `ReservationFlowTest.java:44-51`.
- A virada da temporada em 14 e 15/12 (`:54-58`) e o meio para o par (`MoneyTest.java:18-22`).
- A máquina de situações e os códigos de erro (`ReservationFlowTest.java:105-120`), as recusas (`:123-133`) e os dois
  relatórios com o CSV inteiro (`:136-151`).
- O texto completo do e-mail do hóspede (`:68-71`).

**A parte nova do cenário está testada em parte:**
- Confere o número de mensagens na confirmação (`:65`), o assunto da governança (`:72-73`) e o assunto e o valor do
  financeiro (`:74-75`).
- Confere que a reserva pendente cancelada não avisa ninguém (`:83`), que a STANDARD confirmada cancelada gera 2
  mensagens (`:92`) e que a NON_REFUNDABLE gera só a da governança (`:99-100`).
- Pela pergunta da mutação:
  - Tirar a guarda `when e.wasConfirmed()` (`HousekeepingNotifier.java:26`) ou trocar `> 0` por `>= 0` no reembolso
    (`FinanceNotifier.java:23`) faz o teste falhar.
  - Ficam sem rede o corpo do e-mail de confirmação da governança (as datas e o número de hóspedes,
    `HousekeepingNotifier.java:24-25`), o corpo do cancelamento com as datas liberadas (`:28`) e o assunto e o valor do
    "Reembolso - reserva N" (`FinanceNotifier.java:24-25`), que só entram na contagem de mensagens. Apagar
    `"\nHóspedes: " + r.guests()` não quebraria nenhum teste.
- O nome do teste diz "FourMessages", mas a asserção espera 3 (`:61` e `:65`).

**O que pesa na nota:** os testes rodam todos contra o `MemoryStore`. Ficam fora o JSON da API (os campos, o dinheiro
com 2 casas, os códigos HTTP, o `400` do corpo inválido), todo o SQL (a consulta de conflito
`JdbcStore.java:148-160`, a trava `:137-145`, o mapeamento `:274-280`) e o que sobrevive a um reinício. Metade do código
nunca roda nos testes.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Todo o dinheiro está em
`numeric(12,2)` e as datas em `date`. A única chave estrangeira (`reservations.room_number`) tem índice. As
conferências acusaram as colunas de situação, plano, tipo e canal em `text` sem `CHECK`, nenhuma restrição
(`UNIQUE`/`EXCLUDE`/`CHECK`) que proteja uma regra, e nenhum arquivo de esquema ou migração. Nenhum SQL é montado com
dado de fora.
**Nota:** com problemas

**O que está certo:**
- Os tipos, as chaves e os `NOT NULL` estão no lugar (`store/JdbcStore.java:46-56`). Só `refund` aceita nulo, que é o
  "null enquanto não for cancelada".
- Todo o SQL usa `?`. A única concatenação é o sufixo constante `" for update"` (`JdbcStore.java:196-204`).
- As conexões saem de um pool HikariCP e voltam por try-with-resources (`:30-37`, `:78`).
- A mudança de situação e as mensagens dos ouvintes são gravadas na mesma transação, com `rollback` em caso de erro
  (`:77-91`).
- A duplicação de reserva é impedida por uma trava de linha no quarto (`select … for update`, `JdbcStore.java:137-145`,
  chamada em `ReservationService.java:49`) antes da consulta de conflito. A trava vale entre processos, não só dentro de
  um, e o roteiro confirmou isso nas 10 rodadas simultâneas.
- A subida concorrente é protegida por `pg_advisory_lock` (`:44`).

**Os defeitos:**
- O esquema é criado por `CREATE TABLE IF NOT EXISTS` dentro de `JdbcStore.migrate()` (`:41-74`), sem migrações
  versionadas. O próximo pedido que acrescentar uma coluna não terá como chegar a um banco que já existe.
- Não há `CHECK` em `status`, `rate_plan`, `type` e `channel` (`:46-56`), nem `guests >= 1` ou `check_out > check_in`.
  O banco aceita qualquer texto ou valor.
- Os dois relatórios trazem a tabela `reservations` inteira (`JdbcStore.java:229-237`) para filtrar em Java
  (`service/ReportService.java:28-31`, `:44-47`). A tabela só cresce, e o filtro por data e por mês deveria estar no
  `WHERE`.

## Comportamento

**Números:** 66 de 66 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário 03 15/15, duas
reservas ao mesmo tempo 2/2.
**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer o seguinte:
- Os preços saem certos, com temporada, desconto e arredondamento.
- As recusas usam os códigos combinados com o site, e a confirmação manda o e-mail do hóspede com o comprovante.
- O check-in, o check-out, os cancelamentos e as devoluções seguem as regras, e os dois relatórios em CSV saem como
  pedido.
- Nada se perde quando o sistema reinicia.
- Os avisos novos chegam como pedido:
  - na confirmação, a governança recebe as datas e o número de hóspedes, e o financeiro recebe o valor pago;
  - no cancelamento de uma reserva confirmada, a governança recebe as datas liberadas, e o financeiro recebe o
    reembolso só quando há o que devolver;
  - uma reserva pendente cancelada não avisa ninguém.
- Dois pedidos simultâneos para o mesmo quarto nunca geram duas reservas.

## Dúvidas

- O contrato `Subscriber.on(DomainEvent) -> List<Message>` (`event/Subscriber.java:11`) prende os ouvintes a produzir
  mensagens. Ele atende a tudo o que o texto pede hoje, e por isso dei `acerto`. Se o próximo interessado precisar de
  outra ação, esse contrato vai ter de mudar.
- Na nota do design, os defeitos (o construtor de 16 argumentos copiado, a exceção falsa usada para controle de fluxo)
  ficaram entre `bom` e `com problemas`. Dei `bom` porque são poucos, localizados e não tornam o código arriscado de
  mudar.
