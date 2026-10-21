# Roteiro da base: o sistema do hotel (rascunho)

> Rascunho de trabalho, nada congelado. A base é o projeto de onde todos os cenários partem: cada
> execução recebe uma cópia idêntica dela e um pedido de mudança.

## Para que serve a base

A pergunta da fase de manutenção é: **numa base sem pattern, quando o pedido de mudança precisa de um
pattern, o agente aplica (e aplica certo)? E, quando não precisa, ele se segura?** Os patterns medidos
são Strategy, Observer, Factory (Factory Method ou Abstract Factory, em aberto), Decorator e Template Method.

A base precisa ter, para cada pattern, uma **semente**: um ponto do código escrito sem o pattern, que
hoje está certo assim e que o pedido de mudança vai pressionar. Precisa ter também os pontos que os
**controles negativos** vão mexer, onde a mudança certa é pequena.

## O princípio: a um passo da regra de três

Cada semente fica **exatamente um passo antes** de o pattern se justificar: dois casos num `switch`,
dois relatórios parecidos, um único aviso, um único tipo de documento. Assim, sem o pedido de mudança,
um bom programador deixaria o código como está, e com o pedido, o terceiro caso (ou o segundo
aviso, ou o segundo tipo de documento) é o que justifica o pattern. Isso dá ao TCC um critério para o
gabarito que não depende de gosto: a regra de três (Fowler, *Refactoring*) e o *Refactoring to
Patterns* (Kerievsky, 2004).

A base **não pode ser um espantalho**: nomes bons, código limpo, testes verdes, nada duplicado fora
das sementes. Se a base for ruim em tudo, todo modelo refatora tudo, e o exagero fica inflado.

## A pilha

| peça | escolha |
|---|---|
| linguagem | Java 21 |
| servidor HTTP | **Javalin 6.7.0** (a última da linha 6; a 7 já existe, mas mudou a API, e os modelos conhecem melhor a 6: não queremos medir conhecimento de API nova) |
| JSON | Jackson (o que o Javalin usa) |
| build | Maven |
| testes | JUnit 5 |
| armazenamento | PostgreSQL 18 (desde 21/10/2026), por JDBC puro com o HikariCP; o esquema em migrações do Flyway; o endereço em `DB_URL` (o usuário em `DB_USER`, a senha em `DB_PASSWORD`) |
| montagem das peças | à mão, numa classe `App`, sem contêiner de injeção |

**Por que Javalin e não Spring Boot.** O Spring entrega pronto parte do que se quer medir: os eventos
(`ApplicationEventPublisher` + `@EventListener`) são um Observer de graça, o contêiner é uma Factory
de graça, e o AOP faz o papel do Decorator. Os pilotos do construction-v6 já mostraram o efeito: o Factory virou
medida só descritiva, porque o contêiner criava todas as estratégias em todos os pilotos. Com o Javalin,
quem cria, avisa e embrulha os objetos é o código do agente, e é isso que a régua e o avaliador leem.

**Onde o tipo é fraco.** O Java é estático, mas a fronteira HTTP não é: o corpo JSON vira objeto pelo
Jackson em tempo de execução, e os parâmetros de caminho e de consulta chegam como texto. A base valida
essas entradas na borda (o `ctx.bodyValidator` do Javalin ou uma conferência à mão) e devolve `400`
com um código de erro. Dentro do domínio, tudo é tipado (`enum`, `record`, `BigDecimal`, `LocalDate`).

## O que o hotel faz

Uma pousada pequena: quartos, reservas, a conta da estadia, os comprovantes, as mensagens para o
hóspede e dois relatórios.

| endpoint | o que faz |
|---|---|
| `GET /rooms` | os quartos (número, tipo, capacidade, diária base) |
| `POST /reservations` | cria a reserva (hóspede, quarto, entrada, saída, plano de tarifa); calcula o valor; fica `PENDING` |
| `GET /reservations/{id}` | a reserva, com o valor e a situação |
| `POST /reservations/{id}/confirmation` | confirma; emite o comprovante; manda a mensagem de confirmação |
| `POST /reservations/{id}/cancellation` | cancela; calcula o reembolso conforme o plano de tarifa |
| `POST /reservations/{id}/check-in` | registra a entrada |
| `POST /reservations/{id}/check-out` | fecha a conta; emite o recibo |
| `GET /reservations/{id}/document` | emite de novo o comprovante (o hóspede perdeu) |
| `GET /messages` | as mensagens enviadas (a caixa de saída que a recepção consulta) |
| `GET /reports/occupancy?date=` | o relatório de ocupação do dia (CSV) |
| `GET /reports/revenue?month=` | o relatório financeiro do mês (CSV) |

As mensagens não saem de verdade: vão para uma caixa de saída que o `GET /messages` mostra. É uma
funcionalidade real (a recepção confere o que foi mandado) e é por ela que se enxergam os avisos de fora.

## As sementes

| pattern | a semente na base | por que hoje está certo assim |
|---|---|---|
| **Strategy** | o plano de tarifa (`STANDARD`, `NON_REFUNDABLE` com 15% de desconto) decidido num `switch` em **dois** lugares: no desconto (`PriceCalculator`) e no reembolso do cancelamento (`ReservationService.cancel`) | dois casos, que não mudam há tempos |
| **Observer** | ao confirmar, o `ReservationService.confirm` chama direto o `EmailSender` | um único interessado na confirmação |
| **Factory** | o comprovante (`ReservationReceipt`) é montado com `new` em **três** lugares do `ReservationService`: `confirm`, `checkOut` e `receiptCopy` | um único tipo de comprovante |
| **Decorator** | um único jeito de mandar mensagem (`EmailSender`, que guarda em `SentMessages`), sem nenhum comportamento extra | ninguém pediu nada além de mandar |
| **Template Method** | os dois relatórios (`OccupancyReport`, `RevenueReport`) escritos separados, com os mesmos passos (filtrar as reservas, ordenar, escrever o CSV com cabeçalho, linhas e total) | dois relatórios; o terceiro é que justificaria tirar o esqueleto |

## O que os controles negativos vão mexer

| pattern | o ponto da base | o pedido negativo | o certo |
|---|---|---|---|
| Strategy | o acréscimo da alta temporada (`HIGH_SEASON_SURCHARGE`, 20%), uma constante | "a temporada passa a ser 25%" | trocar o número |
| Observer | o texto do e-mail de confirmação (`confirmationEmailBody`) | "muda o texto da confirmação" | trocar o texto |
| Factory | a reserva (`Reservation`), uma classe de dados criada direto | "a reserva passa a guardar quantas crianças vêm" | um campo novo, criado direto |
| Decorator | o fim das mensagens (hoje, "Até breve!", sem assinatura) | "toda mensagem termina com a assinatura do hotel" | mudar o modelo da mensagem |
| Template Method | as colunas do relatório de ocupação | "põe a coluna do nome do hóspede" | uma coluna a mais, no próprio relatório |

## O que a base não pode ter

- **nenhum pattern de projeto, em lugar nenhum** (decisão do Lucas, 15/10): nem os cinco medidos, nem
  outros (Singleton, Builder, métodos estáticos de criação como `of(...)`/`from(...)`, Facade, Command...), e
  não só nas sementes. O que o próprio Javalin e o Jackson fazem por dentro não conta: conta o código da base;
- nenhuma `interface` com uma implementação só (seria meio caminho andado);
- nenhum comentário, `TODO` ou nome que aponte para o pattern ("por enquanto", "extensível", `Strategy`, `Handler`);
- nenhuma duplicação fora das sementes (senão o agente refatora outra coisa e o escopo fica sujo);
- nada que dependa de rede ou Docker para o `mvn verify`, além do PostgreSQL de `DB_URL` (desde 21/10/2026: cada execução recebe um banco só dela).

## Tamanho e testes

- **Código:** 922 linhas em 26 arquivos (a meta era de 1.200 a 1.800; ficou menor, e tudo bem: o agente lê tudo numa sessão).
- **Testes da base:** 26 testes JUnit, verdes (`mvn verify`), cobrindo as diárias, a temporada, o
  arredondamento, o reembolso, as situações da reserva, os relatórios e a API por HTTP. Ficam visíveis
  para o agente, como num projeto de verdade.
- **Relógio:** a base não depende de "hoje": todas as datas vêm nas chamadas. Se um cenário precisar
  de "hoje", o relógio entra pelo cenário.

## Quem escreve e como se confere

A base foi escrita pelo Claude (15/10) e falta a revisão do Lucas. É conferida antes de congelar:

1. a lista "o que a base não pode ter", item por item;
2. as sementes estão a um passo da regra de três (nem antes, nem depois);
3. os testes da base passam;
4. um teste de "poder de discriminação" por cenário, como na v3 do Akita: uma solução de referência com o
   pattern cai no avaliador como "acerto", e uma solução ingênua (o `if` a mais), com o mesmo
   comportamento, cai como "omissão".

**Viés a declarar:** a base foi escrita por um modelo da mesma família de vários modelos testados, e
o domínio também foi sugerido por ele.

## Decisões em aberto

1. **Factory Method ou Abstract Factory** — qual dos dois é a resposta certa **do cenário** (o que o
   agente deveria criar); a base não tem nenhum dos dois. **Decidido (Lucas, 15/10): o Factory Method.** O
   pedido do comprovante por tipo de hóspede justifica sem precisar dizer "família de objetos".
2. **Armazenamento.** Adotado **em memória** para escrever a base. **Decidido (Lucas, 21/10/2026): PostgreSQL**,
   porque o banco é uma das quatro colunas da tabela do TCC (arquitetura, design de baixo nível, testes, banco).
   Veja "A base com banco (21/10/2026)", no fim.
3. **O pattern diferente que também resolve** (ex.: Strategy por passo no lugar de Template Method): o
   gabarito lista antes as alternativas aceitas, e o resultado ganha a classe "alternativa equivalente".
4. **O número de execuções.** São 10 cenários (5 que pedem o pattern + 5 controles negativos; os
   negativos ficam, decisão do Lucas em 15/10, porque são eles que medem o exagero), cada um saindo da
   base limpa. **Decidido (Lucas, 15/10): de início, 1 execução por modelo** (uma réplica). Com os 11
   modelos do construction-v5, isso dá 110 execuções por nível de harness: 440 com os 4 níveis, 110 só com o N0. Ainda em
   aberto: se os 4 níveis continuam nesta fase. Limitação a declarar: com uma réplica, a variação entre
   execuções do mesmo modelo não aparece (nos pilotos do construction-v6, o State do Sonnet 5.5 acertou em 1 de 3 e caiu em 2),
   então o resultado é exploratório por modelo, e as conclusões mais firmes ficam no agregado (por
   cenário, por pattern, por família de modelo).

## Próximos passos

1. Fechar a decisão 2 e os níveis de harness da decisão 4.
2. ~~Escrever a base, seguindo este roteiro.~~ Feita em `hotel/` (15/10).
3. Conferir a base (a lista acima) e ler junto com o Lucas.
4. ~~Escrever os 10 pedidos de mudança, na voz do dono da pousada.~~ Feitos em `../scenarios/` (15/10), com o gabarito em `../../evaluation/answer-key/GABARITO.md`.
5. ~~As soluções de referência.~~ Feitas em `../../evaluation/solutions/` (15/10). Falta a avaliação.

## A base escrita (15/10)

Em [`hotel/`](hotel/), com um [README](hotel/README.md) para o agente (como num projeto de verdade, sem
nada sobre patterns). Conferências já feitas:
- o `mvn verify` passa (26 testes) e o `java -jar target/hotel.jar` sobe e responde pela API;
- nenhuma `interface`, classe abstrata, método estático de criação, `TODO` ou comentário que aponte
  para mudança futura (busca no código). Dois ajustes saíram dessa conferência: a caixa de saída se
  chamava `Outbox`, que também é o nome de um pattern de mensageria (virou `SentMessages`), e um
  comentário dizia "por enquanto";
- a checagem de "reserva paga" estava repetida fora das sementes e virou `Reservation.isPaid()`;
- uma regra de negócio corrigida no teste manual: o relatório financeiro conta o dinheiro que ficou com a
  pousada (`amountKept`), então uma reserva não reembolsável cancelada continua contando.
- **um defeito achado depois (15/10, pelas soluções do cenário 09):** os relatórios saíam como `text/csv`
  sem charset, e o Jetty os mandava em Latin-1; nada aparecia porque eles não tinham acento. A base passou a
  mandar `text/csv; charset=utf-8`.

Os pacotes: `rooms` (os quartos), `reservations` (o preço, a reserva, o ciclo dela), `documents` (o
comprovante), `messages` (o e-mail e as mensagens enviadas), `reports` (os dois relatórios), `web` (a API) e
a classe `Hotel`, que monta as peças à mão e sobe o servidor.

## A base com banco (21/10/2026)

A base passou da memória para o PostgreSQL, sem mudar o comportamento da API nem as sementes:

- **O esquema** fica numa migração do Flyway (`src/main/resources/db/migration/V1__create_tables.sql`): `rooms` (os
  cinco quartos vêm com o esquema), `reservations` (com a chave estrangeira para o quarto, o dinheiro em
  `numeric(10, 2)`, as datas em `date` e as conferências simples: hóspedes ≥ 1, saída depois da entrada) e
  `messages`. Uma restrição de exclusão (`btree_gist`) impede duas reservas não canceladas no mesmo quarto e na
  mesma noite, mesmo com dois pedidos simultâneos; quando ela barra, a resposta é o mesmo `409 ROOM_UNAVAILABLE`.
- **O acesso** é JDBC puro com o HikariCP, em três classes que antes guardavam em memória: `Rooms`,
  `ReservationStore` e `SentMessages`. A classe `Database` abre a conexão a partir de `DB_URL`, `DB_USER` e
  `DB_PASSWORD` e roda as migrações; o `Hotel` recebe o `DataSource`. O serviço grava cada mudança da reserva
  (`store.update`) logo depois de mudá-la; a conferência de quarto ocupado virou uma consulta
  (`isRoomTaken`). Os relatórios continuam lendo todas as reservas e filtrando em Java, como antes, para a semente
  do Template Method ficar igual.
- **As sementes não mudaram:** os dois `switch` do plano de tarifa, a chamada direta ao `EmailSender`, os três `new
  ReservationReceipt`, o `EmailSender` sem nada em volta e os dois relatórios com os mesmos passos estão como eram.
- **Os testes** (os mesmos 26) rodam contra o banco de `DB_URL`; a classe de teste `EmptyDatabase` esvazia as reservas
  e as mensagens e volta a numeração a 1 antes de cada teste. Três conferências passaram a reler a reserva do banco
  (`service.find`), porque o objeto devolvido antes não muda mais sozinho; o `PriceCalculatorTest` monta os dois
  quartos à mão, sem banco.
- **Conferido:** o `mvn verify` passa (26 testes) contra um PostgreSQL 18.6, e um passeio de 28 chamadas pela API
  (criar, recusar, confirmar, cancelar, check-in, check-out, segunda via, mensagens, os dois relatórios e os erros)
  deu a mesma saída, byte a byte, na base antiga (em memória) e na nova.
- **Tamanho:** 1.147 linhas de Java em 27 arquivos de código (antes, 922 em 26), mais a migração.
- **O que o agente vai encontrar de novo nos pedidos:** o 01 (o `companyCode`), o 05 (CNPJ, razão social, passaporte e
  país) e o 07 (o `status` da mensagem) pedem coluna nova, e portanto uma migração nova; é isso que a coluna do
  banco mede na manutenção.
