# Revisão: construção, cenário 09

## Visão geral

O agente montou o projeto em camadas por pacote. O `api` tem `ApiHandlers`, com um método por rota, e os DTOs em
`api.dto`. O `service` tem `ReservationService`, `NotificationService` e `ReportService`, e `service.report` guarda os
quatro relatórios. O `domain` tem `Reservation`, que concentra o preço, a temporada, o desconto, as situações e a
devolução. O `infrastructure` tem os repositórios com JDBC puro. Tudo é ligado no `Main`. Uma requisição entra pelo
handler (`Main.java:48-60`), que chama o serviço. O serviço carrega a `Reservation` pelo repositório, aplica a regra
no objeto de domínio e grava de novo com um `UPDATE` (`infrastructure/ReservationRepository.java:22-75`). Os dados ficam no
PostgreSQL, em três tabelas criadas com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:27-59`).

A parte do cenário, os relatórios, saiu bem. Os quatro passam por uma classe abstrata com o esqueleto (`Report.java`),
e o roteiro do cenário passou inteiro (13 de 13).

O mais sério é que nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. A
disponibilidade é conferida e a reserva é gravada depois, sem transação, trava ou restrição
(`service/ReservationService.java:31-35`). No roteiro, as duas reservas foram criadas em 10 de 10 rodadas. Há também falhas
menores na borda: um corpo que não é JSON ou um plano desconhecido respondem 500, e o dinheiro zero sai como `0`.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Só `api` e o pacote
raiz usam o Javalin, e só `infrastructure` usa o JDBC. As maiores classes são `ApiHandlers` (254 linhas),
`ReservationRepository` (239) e `Reservation` (229).
**Nota:** com problemas

O que está bem:

- **Regras isoladas.** As regras ficam no domínio, sem Javalin e sem SQL: as situações estão em
  `domain/Reservation.java:47-75` e o preço em `domain/Reservation.java:88-130`.
- **SQL só nos repositórios.** O SQL mora apenas em `infrastructure`, sempre com parâmetros.
- **Montagem num lugar só.** As peças são criadas e ligadas no `Main` (`Main.java:24-38`), sem `new` de repositório
  dentro dos serviços.
- **Sem ciclo.** As dependências apontam da borda para o serviço e do serviço para os dados.

Há duas misturas localizadas:

- **A borda fala direto com os dados.** `api` depende de `infrastructure`: `ApiHandlers` recebe e chama
  `RoomRepository` e `MessageRepository` sem passar por um serviço (`api/ApiHandlers.java:5-6`, `:38`, `:173`). Recebe
  também um `NotificationService` que nunca usa (`api/ApiHandlers.java:22`, `:33`).
- **Uma regra no handler.** A regra "a segunda via só existe para reserva paga" está no handler
  (`api/ApiHandlers.java:158-162`), não no serviço.

Além disso, o tratamento de erro é repetido em cada handler, com o mesmo bloco `try/catch` copiado seis vezes
(`api/ApiHandlers.java:44-170`), em vez de um `app.exception(...)` num lugar só. Por isso as exceções não previstas
viram 500 (veja a coluna Comportamento).

## Design de baixo nível

**Números:** 4 de 4 relatórios são subclasses de `Report`, e 0 ficaram fora da estrutura. A linha de título está num
lugar só (`service/report/Report.java:24-26`) e nenhum relatório monta o próprio título.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `Report` é uma classe abstrata com o método-modelo `generate()`
(`service/report/Report.java:9-22`). Ele escreve o título, o cabeçalho, as linhas e o total, nessa ordem. Os passos
que variam são abstratos: `getReportName`, `getPeriod`, `buildHeader`, `buildDataLines` e `buildTotal`
(`service/report/Report.java:28-32`). Os quatro relatórios, os dois de antes e os dois novos, só preenchem esses
passos: `OccupancyReport.java:20-55`, `RevenueReport.java:19-59`, `TourismTaxReport.java:19-61` e
`RatePlansReport.java:22-69`.

A linha com a data em que o relatório foi tirado, que o dono anuncia para o ano que vem, entra em um lugar só, o
`generate()`. Um relatório novo, de cancelamentos ou de hóspedes por estado, é só mais uma subclasse.

O que afasta um pouco do ideal:

- **`generate()` não é `final`.** Uma subclasse pode reescrever o esqueleto.
- **A seleção das reservas ficou em dois lugares.** Parte está no repositório (`findByMonth`, `findByNight`) e parte
  no filtro de cada relatório.
- **O filtro de "deixou dinheiro" está copiado.** O `getNetRevenue() > 0` aparece três vezes
  (`RevenueReport.java:37`, `RatePlansReport.java:40`, `:61`).

Nada disso põe a classe em dúvida.

**Exagero:** contenção. Não há interfaces de uma implementação só, fábricas nem builders. O `ReportService` só escolhe
a consulta e instancia o relatório (`service/ReportService.java:20-42`), o que é pouco, mas tem papel.

**O resto do código** tem defeitos localizados:

- **O preço do quarto em dois lugares.** `Reservation` decide a diária e o tipo por um `switch` sobre o número do
  quarto (`domain/Reservation.java:160-176`), em vez de usar o `Room` e o `RoomType` que já existem. Um quarto novo
  precisa ser lembrado em dois `switch`, além da tabela `rooms` (`infrastructure/Database.java:75-79`). O mesmo vale
  para `TOTAL_ROOMS = 5` fixo (`service/ReportService.java:12`).
- **A taxa de turismo fixa no domínio.** O valor está como constante dentro do método
  (`domain/Reservation.java:133`), embora o dono avise que a prefeitura muda o valor todo ano.
- **Formatação que depende do locale.** O dinheiro é formatado com `String.format` sem `Locale`
  (`service/report/Report.java:46`, `service/NotificationService.java:82`). Numa máquina com locale `pt_BR`, o CSV
  sairia `599,76` e o comprovante `R$ 1,234.56`.
- **Exceções de domínio genéricas.** O domínio sinaliza "operação não permitida" com `IllegalStateException`
  (`domain/Reservation.java:49`), enquanto a `OperationNotAllowedException` criada para isso nunca é usada
  (`service/ReservationService.java:130-134`).
- **O quarto buscado duas vezes.** A validação busca o quarto uma vez (`service/ReservationService.java:69-72`) e de
  novo com um `orElseThrow()` sem mensagem (`service/ReservationService.java:85`).

Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo, `LocalDate` e `enum`s.

## Testes

**Números:** 25 testes rodaram, nenhum falhou, nenhum foi pulado, e há 25 métodos de teste no código. A cobertura é de
26,1% das linhas (212 de 811) e 34,7% dos ramos (50 de 144). Por pacote:

- `domain`: 104 de 137 linhas;
- `service.report`: 108 de 109 linhas;
- `api`, `service` e `infrastructure`: 0 linhas cobertas.

Não há nenhum teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

**O que está testado com valores:**

- **O preço:**
  - alta temporada de julho (599,76, `ReservationTest.java:13-29`);
  - baixa temporada (`:32-48`);
  - o desconto do NON_REFUNDABLE com arredondamento (116,97, `:51-70`);
  - dezembro, janeiro e fevereiro (`:315-363`).
- **As transições e as recusas** (`:73-228`).
- **A devolução:** o plano normal devolve o pago e o NON_REFUNDABLE devolve 0 (`:116-157`).
- **A taxa de turismo:** 2 hóspedes por 3 noites dão 27,00 (`:231-245`).

Nos relatórios, os quatro têm teste do título, do cabeçalho e de uma linha (`ReportTest.java:17-142`), e também o
plano sem reserva aparecendo com 0 (`:195-218`).

**As lacunas:**

- **A camada HTTP, os serviços e o banco não têm teste.** Os códigos de erro, o e-mail, o comprovante e a
  persistência ficam sem rede. Havia um teste de integração, mas ele foi desligado ao ser renomeado para
  `src/test/java/com/pousadamarealta/ReservationSystemIT.java.manual`, e por isso não compila nem roda.
- **As asserções dos relatórios são fracas.** Todas usam `csv.contains(...)`, que não confere a ordem: o título vir
  primeiro e o total vir por último.

**A pergunta da mutação:**

- **O dinheiro do NON_REFUNDABLE e o total do relatório por plano.** Ninguém confere esses valores
  (`ReportTest.java:141` só verifica `"NON_REFUNDABLE,1,"`). Trocar a soma, ou deixar de contar a não reembolsável
  cancelada, passaria.
- **A ordem do financeiro e da taxa** (por data e depois por número). Não é testada.
- **O `"%s,%s,%s"` do título.** Trocá-lo quebraria os testes, então essa parte está coberta.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram (`measures/database.md`):

- 0 chaves estrangeiras;
- 0 restrições `CHECK`, `UNIQUE` ou `EXCLUDE`;
- 4 colunas de situação ou tipo sem `CHECK`;
- 0 transações e 0 `FOR UPDATE`;
- nenhum pool, com uma conexão nova por chamada via `DriverManager`.

Não apareceu SQL montado com `+`.
**Nota:** com problemas

**O que está certo:** os tipos, o SQL sempre com `?` e as conexões fechadas com try-with-resources.

**Os defeitos:**

- **A regra "um quarto, uma reserva por noite" não tem proteção nenhuma.** O serviço consulta os conflitos
  (`infrastructure/ReservationRepository.java:120-159`) e depois insere (`service/ReservationService.java:35`), em
  duas conexões em autocommit. Não há transação, trava, nem uma restrição `EXCLUDE` no banco. No roteiro, duas
  reservas simultâneas foram criadas nas 10 de 10 rodadas.
- **A reserva não aponta para o quarto.** Falta a chave estrangeira de `reservations.room_number` para `rooms`
  (`infrastructure/Database.java:37`).
- **Status e plano são texto livre.** Não há `CHECK` sobre eles (`infrastructure/Database.java:44-45`).
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código, sem migração
  (`infrastructure/Database.java:27-59`), e não saberá acrescentar uma coluna.
- **A confirmação não é atômica.** Ela grava a reserva e depois a mensagem sem transação
  (`service/ReservationService.java:41-42`). Uma falha entre as duas deixa a reserva confirmada sem e-mail registrado.
- **Sem pool de conexões** (`infrastructure/Database.java:20`).

Os dados persistem: os 3 de 3 testes depois de reiniciar passaram.

## Comportamento

**Números:** 59 de 64 conferências passaram. Por parte:

- parte comum: 42 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 09 (os relatórios): 13 de 13;
- reservas simultâneas: 1 de 2 (`measures/behavior.md`).

**Nota:** com problemas

**O cenário funciona inteiro.** Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>". A taxa de
turismo e o relatório por plano trazem os valores, a ordem e os totais pedidos. O plano sem reserva aparece com 0, e
a NON_REFUNDABLE cancelada continua contando.

**As falhas:**

- **Duas reservas ao mesmo tempo, no mesmo quarto e nas mesmas noites.** As duas foram aceitas nas 10 rodadas. Se
  dois hóspedes reservarem o mesmo quarto no site quase no mesmo instante, a pousada terá duas reservas confirmáveis
  para o mesmo quarto. Este é o defeito que mais pesa; a causa está descrita na coluna Banco.
- **Corpo que não é JSON responde 500 `Server Error` em texto, em vez de 400 `INVALID_REQUEST`.** O `bodyAsClass`
  lança uma `JsonParseException` que nenhum `catch` trata (`api/ApiHandlers.java:46`). Conferi à mão que um plano de
  tarifa desconhecido (`"ratePlan":"XYZ"`) também dá 500, pela `InvalidFormatException` no log. O site recebe um erro
  sem o código combinado.
- **Dinheiro zero sai no JSON como `0`, não `0.00`.** Isso vale para `amountPaid` e `discount` (`BigDecimal.ZERO` em
  `domain/Reservation.java:44` e `:120`). O texto pede número com 2 casas. O efeito é pequeno, mas é um desvio do
  combinado.
- **"O 201 comporta 3" e "a diária do 201 é 389.90" falharam no roteiro só porque o `number` do quarto sai como texto
  (`"201"`) e o roteiro procura o número `201`.** Conferi à mão que o 201 vem com `capacity` 3 e `nightlyRate`
  389.90. O texto não fixa o tipo, e tratar o número do quarto como texto é uma escolha razoável. Esta falha não pesa.

## Dúvidas

- **A nota do Banco.** A falta total de proteção contra a reserva duplicada (10 de 10 rodadas), somada à falta de
  chave estrangeira e de transação, fica entre `com problemas` e `ruim`. Dei `com problemas` porque a skill põe a
  duplicação sob concorrência nesse nível e porque, fora da concorrência, os dados ficam certos e persistem. Um
  revisor mais rígido poderia ler isso como "risco de dado errado" e dar `ruim`.
- **A nota da Arquitetura.** Ela fica entre `bom` e `com problemas`. As misturas (o handler falando com os
  repositórios e a regra da segunda via no handler) são pequenas e localizadas.
