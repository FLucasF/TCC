# Revisão: construção, cenário 09

## Visão geral

O projeto (`projeto/`) tem 9 pacotes: `api` (os controllers do Javalin e os DTOs), `domain` (a reserva, o quarto, as
situações da reserva como State em `domain.state` e os planos de tarifa em `domain.rateplan`), `service` (preço e
comprovante), `report` (os quatro relatórios) e `repository` (JDBC puro com `DriverManager`). As peças são criadas e
ligadas em `Application.main`. Uma requisição entra pela rota (`Application.java:63-75`) e vai ao controller.
`ReservationController` chama direto os repositórios e o `PricingService` e monta a reserva, o comprovante e o e-mail.
Não há um serviço de reservas. Os dados ficam em três tabelas que `Database.initializeSchema` cria com
`CREATE TABLE IF NOT EXISTS` (`Database.java:28-66`). O pattern pedido (Template Method nos relatórios) está bem feito:
`Report.generate()` é `final`, e as quatro subclasses preenchem só os passos que mudam. O sistema, porém, quebra em
três pontos graves:

- **O CPF com pontos e traço é recusado.** A coluna `GUEST_CPF` tem `VARCHAR(11)` (`Database.java:42`). Um CPF como
  `111.222.333-44` estoura a coluna, e o `catch (Exception)` do controller (`ReservationController.java:84-85`)
  devolve 400 `INVALID_REQUEST` com a mensagem "Failed to save reservation". Por isso o roteiro não conseguiu criar
  nenhuma reserva.
- **O serviço não sobe de novo sobre um banco que já tem os quartos.** A carga inicial dos quartos só ignora a
  mensagem de chave duplicada do H2 (`RoomRepository.java:38`). No PostgreSQL, o segundo início morre com
  `duplicate key value violates unique constraint "rooms_pkey"`.
- **Os relatórios novos erram.** Os planos saem fora da ordem pedida, porque `Map.of` não garante ordem. O financeiro
  e o de planos contam reservas que não deixaram dinheiro.

## Arquitetura

**Números:** 9 pacotes, 22 dependências entre eles, 1 ciclo (`com.pousada.domain` ↔ `com.pousada.domain.state`).
O Javalin aparece só em `com.pousada` e `com.pousada.api`; o JDBC, só em `com.pousada.repository`. A maior classe é
`ReservationController` (289 linhas), seguida de `ReservationRepository` (250).
**Nota:** com problemas

- **O que está bem.** O SQL fica isolado nos repositórios, as regras de preço e de situação da reserva não conhecem
  o Javalin nem o JDBC, e a montagem acontece num lugar só (`Application.java:36-53`).
- **A borda HTTP é grossa.** Falta a camada de regras para as operações da reserva, e `ReservationController` faz o
  papel de serviço. Ele confere a capacidade (`ReservationController.java:47-49`) e o conflito de datas (`:51-53`),
  calcula o preço (`:55-60`) e grava (`:78`). Na confirmação, grava a reserva (`:111`), monta o comprovante e o
  e-mail com o assunto escrito ali mesmo (`:113-123`) e grava a mensagem (`:125`).
- **Regra no handler.** A regra "segunda via só para reserva paga" também mora no handler (`:207-209`).
- **Os relatórios buscam os próprios dados.** Eles recebem os repositórios e fazem as consultas dentro dos passos do
  template (`OccupancyReport.java:44-45`, `RevenueReport.java:43`). Isso é aceitável neste tamanho, mas liga
  `report` direto a `repository`.
- **O ciclo.** O ciclo `domain` ↔ `domain.state` vem do State: `Reservation` cria `PendingState`
  (`Reservation.java:44`), e os estados mexem na `Reservation`. É pequeno e entre pacotes vizinhos.
- **Fábricas estáticas.** `RatePlanFactory` e `ReservationStateFactory` são globais e chamadas por dentro do
  repositório (`ReservationRepository.java:224, 239`).

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto `Report.generate()` (`Report.java:8-22`). Nenhum relatório
escreve a sequência título/cabeçalho/linhas/total por conta própria.
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.**

- **O esqueleto.** `Report` é abstrata e tem o método-modelo `final` (`Report.java:8`). O título com o nome da
  pousada fica num lugar só (`Report.java:24-26`), e há cinco passos abstratos (`Report.java:28-32`).
- **As subclasses.** `OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlansReport` preenchem só os
  passos. A linha com a data, que o dono já anunciou, entraria só em `generate()`.
- **O que afasta do ideal.** Os passos se comunicam por campos mutáveis: `buildDataLines` preenche `reservations`,
  `totalRevenue` e `totalTax`, que `buildTotalLine` depois lê (`RevenueReport.java:43-44, 53, 58`;
  `TourismTaxReport.java:44-49, 58, 63`; `RatePlansReport.java:60-61`). É um acoplamento de ordem que o esqueleto
  esconde.
- **Código repetido entre as subclasses.** `formatAmount` aparece três vezes (`RevenueReport.java:76`,
  `TourismTaxReport.java:83`, `RatePlansReport.java:90`), `calculateRevenue` duas (`RevenueReport.java:70-74`,
  `RatePlansReport.java:84-88`), e o formatador de data e o período também se repetem. Esse código caberia na
  classe-mãe.

**O exagero.**

- **O State.** As situações da reserva viraram um State completo: uma interface, cinco classes e uma fábrica, 190
  linhas em `domain.state`. Um `enum` com as transições resolveria. A lógica de cancelamento ficou copiada em
  `PendingState.java:14-17` e `ConfirmedState.java:13-16`.
- **Um método que só repassa.** `PricingService.calculateDiscount` (`PricingService.java:25-27`) só chama
  `ratePlan.calculateDiscount`.

**O resto do código.**

- **Erros escondidos.** O `catch (Exception)` em `ReservationController.java:84-85` transforma qualquer falha
  (inclusive do banco) em 400 `INVALID_REQUEST`. Foi isso que escondeu o defeito do CPF.
- **Tratamento repetido.** O bloco "achar a reserva ou 404" e os mesmos `catch` se repetem em seis handlers
  (`ReservationController.java:89-221`). Os quatro handlers de relatório também são cópias
  (`ReportController.java:25-99`).
- **A escala do `BigDecimal`.** O código usa `BigDecimal`, mas não cuida da escala. `BigDecimal.ZERO` sai como `0` e
  não como `0.00`: `Reservation.java:45`, `StandardRatePlan.java:8`, os totais vazios em `RevenueReport.java:44`,
  `TourismTaxReport.java:49` e `RatePlansReport.java:61`.
- **Ordem que depende de um `Map.of`.** A ordem do relatório de planos vem de um `Map.of` (`RatePlanFactory.java:6`,
  usado em `RatePlansReport.java:47`), e a ordem de iteração do `Map.of` não é garantida.
- **A mesma regra em dois lugares.** "Reserva paga" existe em Java (`Reservation.java:65-70`) e, de novo, como lista
  de situações no SQL (`ReservationRepository.java:164`).
- **Dinheiro que depende do locale.** `DocumentService.formatMoney` troca vírgula e ponto à mão sobre um
  `String.format` que depende do locale da máquina (`DocumentService.java:59-61`).

## Testes

**Números:** 22 testes rodaram, 0 falharam, 0 pulados; 22 métodos escritos. Cobertura de 42,3% das linhas (375 de
887) e 31,7% dos ramos (39 de 123). Os pacotes `report` (0 de 136 linhas), `api` (0 de 247) e `com.pousada` (0 de 47)
não têm cobertura nenhuma.
**Nota:** com problemas

**O que está testado, com valores:**

- o preço com a temporada e o arredondamento: 599,76; 779,80; desconto 116,97; total 662,83
  (`PricingServiceTest.java:17-36`, `IntegrationIT.java:79, 153-155`);
- as transições e a devolução por plano (`ReservationTest.java:16-92`);
- o texto do comprovante, em parte (`IntegrationIT.java:177-183`).

**Os buracos:**

- **A parte nova do cenário não tem nenhum teste.** Nenhum dos quatro relatórios é exercitado. Pela pergunta da
  mutação: trocar `4.50` por `5.40` (`TourismTaxReport.java:13`), apagar o filtro `isPaid` (`TourismTaxReport.java:46`)
  ou deixar `NON_REFUNDABLE` antes de `STANDARD` não derruba nenhum teste. É exatamente o que acontece hoje (veja
  Comportamento).
- **Não há teste pela API.** Os códigos de erro (`ROOM_UNAVAILABLE`, `CAPACITY_EXCEEDED` etc.) e o formato do JSON
  ficam sem rede.
- **O teste de integração roda em H2, não no PostgreSQL de `DB_URL`** (`IntegrationIT.java:30`). Foi isso que
  escondeu a mensagem de erro específica do H2 em `RoomRepository.java:38`, que derruba o reinício.
- **Os testes dependem da ordem e do estado entre si.** `IntegrationIT` usa `@Order` e ids fixos (`findById(1L)`,
  `findById(2L)`).

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` e datas em `date`. As conferências
acusaram:

- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhuma restrição (UNIQUE/EXCLUDE/CHECK) que proteja uma regra;
- `status`, `rate_plan`, `type` e `channel` sem CHECK;
- nenhum pool (`DriverManager.getConnection`, `Database.java:20`);
- 0 transações explícitas e 0 `FOR UPDATE`.

O esquema é criado por `CREATE TABLE` no código, sem migrações.
**Nota:** ruim

**Os defeitos sérios:**

- **O serviço não sobe de novo sobre o banco que ele mesmo criou.** A cada início, `Application.java:43` insere os
  cinco quartos, e `RoomRepository.java:36-41` só ignora o erro se a mensagem contiver "Unique index or primary key
  violation", que é o texto do H2. No PostgreSQL o segundo início sai com `RuntimeException: Failed to save room`,
  causada por `duplicate key value violates unique constraint "rooms_pkey"`. Reproduzi isso subindo o jar duas vezes
  sobre o mesmo banco. O roteiro também acusou ("o serviço sobe de novo sobre o mesmo banco: não subiu"). Na prática,
  no primeiro reinício a pousada fica sem sistema.
- **O CPF no formato com pontos e traço não cabe.** É o formato que o próprio texto mostra no comprovante. A coluna
  `GUEST_CPF VARCHAR(11)` (`Database.java:42`) recusa `111.222.333-44`, e a reserva não é gravada.

**Os outros:**

- **Nada impede a reserva em dobro no banco.** A conferência de conflito (`ReservationRepository.java:121-157`) e o
  `INSERT` (`ReservationController.java:78`) correm em conexões separadas, sem transação, sem trava e sem restrição
  EXCLUDE. Dois pedidos ao mesmo tempo podem gravar duas reservas para as mesmas noites. O roteiro não conseguiu
  medir isso, porque nenhuma reserva foi criada.
- **A confirmação grava em dois passos soltos.** A reserva (`ReservationController.java:111`) e a mensagem (`:125`)
  são gravadas sem transação.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`Database.java:28, 37, 59`) não sabe acrescentar uma coluna
  numa tabela que já existe.

**O que está bem:** o SQL usa parâmetros (`?`) em todo lugar, e as conexões são fechadas com try-with-resources.

## Comportamento

**Números:** 21 de 62 conferências passaram.

| parte | passaram |
|---|---|
| a parte comum | 14 de 46 |
| os dados depois de reiniciar | 0 de 1 |
| o cenário 09 | 6 de 13 |
| duas reservas ao mesmo tempo | 1 de 2 (todas as tentativas deram 400) |

**Nota:** ruim

**O que falhou e por quê:**

- **Nenhuma reserva é criada com o CPF que o site manda** (`111.222.333-44`): a resposta é 400 "Failed to save
  reservation". Por isso falham, em cascata:
  - a confirmação e o e-mail;
  - o check-in e o check-out;
  - os cancelamentos e as devoluções;
  - a segunda via;
  - as linhas dos relatórios.

  Para o dono, o site simplesmente não consegue reservar. As recusas que não dependem de gravar passaram: 422, 404,
  400 para corpo inválido, para campo faltando e para mês fora do formato.
- **O serviço não volta depois de reiniciar** (veja Banco).
- **Duas conferências de `/rooms` falharam por um ponto em aberto.** `number` vem como texto (`"201"`), e o roteiro
  procura um número. O texto não fixa o tipo, então isso não pesa.

**Os relatórios.** Chamei a API à mão, com um CPF só de dígitos, para ver os relatórios funcionando:

- os títulos estão certos;
- a ocupação está certa;
- a taxa de turismo deu 18,00 + 27,00 = 45,00, como o texto pede.

Mas os relatórios ainda erram:

- **A ordem dos planos.** O relatório de planos saiu `NON_REFUNDABLE` antes de `STANDARD`, contra a ordem pedida
  (`RatePlanFactory.java:6`).
- **Reservas que não deixaram dinheiro.** O financeiro listou a STANDARD cancelada com devolução como
  `CANCELLED,0.00`, e o de planos a contou entre as `STANDARD`: o financeiro não filtra nada
  (`RevenueReport.java:43`). Uma reserva pendente também entraria nos dois.
- **Os totais de um mês vazio.** Saem `total,0` em vez de `0.00`.
- **O JSON da reserva criada.** Traz `"discount":0` e `"amountPaid":0`, sem as 2 casas pedidas.

## Dúvidas

- **O exagero.** O State das situações da reserva (5 classes, interface e fábrica) pode ser lido como um design
  legítimo para uma máquina de estados, e não como estrutura sem necessidade. Marquei `exagero` porque um `enum` com
  as transições bastaria neste tamanho, e porque o State gerou o único ciclo entre pacotes e a cópia da lógica de
  cancelamento.
- **A nota de Testes.** Ficou em `com problemas`, e não em `ruim`, porque os testes que existem conferem valores de
  verdade. Mas a parte nova do cenário está inteiramente sem teste.
- **A reserva em dobro.** A ausência de proteção contra duas reservas simultâneas vem da leitura do código. O roteiro
  não conseguiu medi-la, porque nenhuma reserva foi criada.
