# Revisão: construção, cenário 09

## Visão geral

O projeto tem 7 pacotes (`routes`, `services`, `database`, `models`, `dto`, `utils` e a raiz com o `main`). Uma
requisição entra num handler de `routes` (por exemplo, `ReservationRoutes.createReservation`), que valida, decide a
situação da reserva e chama um serviço de `services`. O serviço calcula com `PricingUtils` e fala direto com o
PostgreSQL por JDBC, através de um pool Hikari (`database/Database.java`). As tabelas são criadas com
`CREATE TABLE IF NOT EXISTS` em `Database.java:30-77`. O defeito mais sério derruba o sistema inteiro: o `pom.xml`
(`pom.xml:21-64`) não traz o Jackson (o Gson declarado em `pom.xml:52-53` não é usado em lugar nenhum), e o Javalin 6
precisa de um mapeador de JSON. Com isso, todo `ctx.json(...)` e todo `ctx.bodyAsClass(...)` dão 500 ("It looks like
you don't have an object mapper configured", em `measures/behavior-service.log`). Não dá para listar quartos, criar
reserva nem receber um código de erro: o roteiro passou em 11 de 64 conferências. Só os relatórios CSV respondem, e
vazios. O Template Method pedido não existe: os quatro relatórios estão escritos inteiros, um ao lado do outro, em
`ReportService.java`. Os testes cobrem 5,2% das linhas e não pegaram nada disso.

## Arquitetura

**Números:** 7 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `com.pousada` e `routes`, e o JDBC em `services` e `database`. As maiores classes são `ReservationRoutes` (295
linhas), `ReportService` (215) e `ReservationService` (196).
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo (rotas → serviços → banco, sem ciclo). Os serviços não
conhecem o Javalin. Mesmo assim, há três misturas:

- **A regra de negócio está nos handlers.** A máquina de situações da reserva fica toda em `ReservationRoutes`:
  confirmar só se `PENDING` (`ReservationRoutes.java:141`), cancelar só antes do check-in (`:173-174`), check-in só se
  `CONFIRMED` (`:201`), check-out só se `CHECKED_IN` (`:226`), segunda via só para reserva paga (`:267`). Também ficam
  lá as validações de datas, hóspedes e capacidade (`:65-82`), o envio do e-mail na confirmação (`:150-153`) e a
  montagem do comprovante (`:233-247` e `:273-287`). `ReservationService.checkIn`/`checkOut`/`confirmReservation`
  (`ReservationService.java:89-133`) só fazem o `UPDATE` sem conferir nada; quem chamar o serviço por outro caminho
  passa por cima das regras. Por isso `ReservationRoutes` é a maior classe do projeto.
- **Não há uma camada de acesso a dados.** Os serviços misturam regra e SQL: `ReservationService.cancelReservation`
  decide a devolução e faz o `UPDATE` no mesmo método (`ReservationService.java:135-155`), e `ReportService` monta o
  CSV e a consulta juntos (`ReportService.java:24-214`).
- **A montagem está espalhada.** O `main` só cria o `Database` (`Application.java:18`). Cada classe de rotas recebe o
  `Database` e cria os serviços por dentro (`ReservationRoutes.java:27-32`, `RoomRoutes.java:14-16`,
  `MessageRoutes.java:14-16`, `ReportRoutes.java:15-17`). Por isso o pacote `routes` depende de `database`, e
  `RoomService`/`MessageService` são instanciados duas vezes.

## Design de baixo nível

**Números:** dos 4 relatórios pedidos, nenhum passa por uma estrutura comum. São 4 métodos escritos inteiros em
`ReportService` (`:24`, `:69`, `:116`, `:163`), cada um com a sua própria linha de título (`:26`, `:71`, `:118`,
`:165`), o seu cabeçalho, o seu laço e o seu total. Os 4 handlers de `ReportRoutes` também são cópias
(`ReportRoutes.java:19-97`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede que o jeito dos relatórios mude "em todos de uma vez" (a linha com a data no ano que vem)
e avisa que vêm mais dois relatórios. A resposta seria um esqueleto (título, cabeçalho, linhas, total) num lugar só.
O agente escreveu os quatro relatórios inteiros, um depois do outro, com `StringBuilder`: a sequência
`csv.append(LODGING_NAME)...` → cabeçalho → `try (Connection ...)` → laço → `csv.append("total,...")` se repete quatro
vezes (`ReportService.java:24-67`, `69-114`, `116-161`, `163-214`). Pôr a linha de data exige mexer nos quatro
métodos, e um relatório novo será a quinta cópia. Não há classe abstrata, método-modelo nem escritor genérico. É
omissão.

**O exagero.** Não há interfaces, fábricas nem camadas de repasse. Os DTOs (`dto/ReservationDTO.java`,
`dto/RoomDTO.java`, `dto/MessageDTO.java`) copiam os modelos campo a campo sem acrescentar nada. Isso é redundante, mas
é pouco para chamar de exagero: contenção.

**O resto do código.** Os defeitos estão espalhados:

- **Texto solto no domínio.** A situação e o plano são `String` (`models/Reservation.java:15,20`), comparados por
  literal em vários lugares (`ReservationRoutes.java:141,173,201,226,267`, `PricingUtils.java:47`,
  `ReservationService.java:138`, `ReportService.java:190-193`). Não há validação do plano: `ratePlan: "XYZ"` é aceito
  e gravado sem desconto (`ReservationRoutes.java:39-41,54`).
- **Erros engolidos.** Todo handler termina em `catch (Exception e)`. Em cancelamento, check-in, check-out e segunda
  via, isso vira `404 RESERVATION_NOT_FOUND` (`ReservationRoutes.java:184-187,209-212,250-253,290-293`), então uma
  falha de banco ou do JSON aparece como "reserva não encontrada". Na criação e na confirmação, qualquer erro vira
  `400` com a mensagem interna da exceção (`:99-101`, `:156-158`).
- **Uma regra errada no lugar errado.** A confirmação exige que o cliente mande `amountPaid` no corpo e grava esse
  valor (`ReservationRoutes.java:128-148`, `dto/ConfirmReservationRequest.java`). O texto diz que o pago é o total da
  reserva.
- **Dinheiro sem escala fixa.** `formatCurrencyCSV` usa só `toPlainString()` (`FormatUtils.java:35-37`), e a taxa é
  `BigDecimal.valueOf(4.50)`, que tem escala 1 (`ReportService.java:18,143`). Conferi à mão, com reservas gravadas
  direto no banco: a taxa sai `27.0` e `18.0`, com total `45.0`, em vez de `27.00`/`45.00`, e os totais de mês vazio
  saem `0` em vez de `0.00`. Os detectores de "campo faltando" usam `== 0` em `int`
  (`ReservationRoutes.java:39-41`).
- **Duplicação.** O `VoucherDTO` é montado duas vezes, igual (`ReservationRoutes.java:235-247` e `275-287`), e o
  "/5" está fixo no texto (`ReportService.java:65`).
- **Estado.** Todos os modelos e DTOs têm campos públicos mutáveis (`models/*.java`, `dto/*.java`).

O cálculo de preço em si está bem feito: `BigDecimal`, `HALF_EVEN` a cada passo e `LocalDate`
(`PricingUtils.java:25-55`).

## Testes

**Números:** 7 testes rodaram, 0 falharam, 0 pulados; 7 métodos escritos, num arquivo só
(`src/test/java/com/pousada/ReservationTest.java`). Cobertura: 39 de 756 linhas (5,2%) e 18 de 166 ramos (10,8%). Só
`utils` é coberto (39 de 49 linhas); `routes`, `services`, `database`, `dto` e `models` ficam com 0%. Não há teste
pela API, contra o banco nem com mocks (`measures/tests.md`).
**Nota:** ruim

O que existe confere valores de verdade, mas só do cálculo de preço e da formatação:

- alta temporada (`ReservationTest.java:14-28`);
- diária de 299,88 (`:36`);
- 599,76 de 10 a 12/07 (`:51`);
- desconto de 89,96 com meio para o par (`:64`);
- total de 509,80 (`:73`);
- formatação de data, de dinheiro e do CPF (`:86-97`).

Nada testa as situações da reserva, o cancelamento e a devolução, os códigos de erro, o comprovante, o e-mail, a
indisponibilidade do quarto ou qualquer um dos quatro relatórios. A parte nova do cenário (a linha de título, a taxa
de turismo, o relatório por plano) não tem nenhum teste. Pela pergunta da mutação: trocar 4.50 por 5.40
(`ReportService.java:18`), trocar `res.check_out >` por `>=` na ocupação (`ReportService.java:38`) ou tirar
`'CANCELLED'` do filtro do financeiro (`ReportService.java:83`) não faria nenhum teste falhar. O `mvn verify` passa
com o serviço incapaz de responder qualquer JSON, porque nenhum teste sobe a aplicação; o defeito mais grave do
projeto passou sem rede. Também não pega o `27.0` da taxa. Com 5% de cobertura e só o cálculo de preço conferido, a
coluna está quase ausente.

## Banco

**Números:** o serviço subiu contra um banco vazio e criou 3 tabelas (`rooms`, `reservations`, `messages`), todas com
chave primária. Todas as 6 colunas de dinheiro estão em `numeric(10,2)` e as datas em `date`. As conferências
acusaram uma chave estrangeira sem índice (`reservations.room_number`), nenhum `CHECK` nas colunas de
situação/plano/tipo/canal, nenhuma restrição `UNIQUE`/`EXCLUDE`, nenhuma transação explícita, nenhum `FOR UPDATE`, e
11 linhas de SQL montado com `+` (`measures/database.md`).
**Nota:** com problemas

Os tipos estão certos: dinheiro em `numeric(10,2)`, datas em `date`, `NOT NULL` no que é obrigatório, e a reserva
ligada ao quarto por chave estrangeira (`Database.java:39-57`). A conexão vem de um pool Hikari
(`Database.java:13-20`) e é devolvida com try-with-resources. Os defeitos:

- **O esquema não evolui.** Não há migrações. O esquema é um `CREATE TABLE IF NOT EXISTS` no código
  (`Database.java:30-77`), que não sabe acrescentar uma coluna numa tabela que já existe.
- **Nada no banco impede a reserva dupla.** A disponibilidade é um `SELECT COUNT(*)` (`ReservationService.java:157-174`)
  seguido de um `INSERT` em outra conexão (`ReservationRoutes.java:85-92` → `ReservationService.java:33-57`), sem
  transação, trava ou restrição `EXCLUDE`. Dois pedidos simultâneos podem criar as duas reservas. O roteiro não chegou
  a mostrar isso porque toda criação dá 500.
- **SQL montado com `+`.** São 11 lugares (`ReservationService.java:77,162-164`, `RoomService.java:46`,
  `ReportService.java:38,81-82,128-129,178-179`). Conferi o que entra: são sempre valores já tipados (`long`, `int`,
  `LocalDate`, `YearMonth`, convertidos pelo Javalin ou pelo `parse`), então não há injeção hoje. Ainda assim é um
  hábito arriscado, e o mesmo código usa `?` em outros lugares.
- **Escritas que deveriam andar juntas, separadas.** A confirmação grava a reserva e depois a mensagem em conexões
  separadas, sem transação (`ReservationRoutes.java:148-153`).
- **Integridade solta.** Não há índice na chave estrangeira (`Database.java:56`) nem `CHECK` em `status`/`rate_plan`
  (`Database.java:48,53`).
- **Persistência.** Pela API não dá para provar que os dados ficam, porque nada é criado. Com reservas inseridas direto
  no banco, os relatórios as leram corretamente.

## Comportamento

**Números:** 11 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 4 de 46 |
| os dados depois de reiniciar | 1 de 3 |
| o cenário 09 | 5 de 13 |
| as duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** ruim

O sistema não funciona para o dono da pousada. Toda resposta em JSON dá 500, porque o projeto não tem o Jackson
(`pom.xml:21-64`) e o Javalin 6 recusa serializar ("It looks like you don't have an object mapper configured", em
`measures/behavior-service.log`). Na prática:

- `GET /rooms` dá 500.
- `POST /reservations` dá 500: o `bodyAsClass` falha, o `catch` tenta responder com `ctx.json` e falha de novo.
- Nenhuma reserva é criada, então confirmação, e-mail, check-in, check-out, cancelamento, devolução e segunda via
  ficam sem como acontecer.
- Os códigos de erro (`INVALID_REQUEST`, `ROOM_NOT_FOUND`, `CAPACITY_EXCEEDED`...) também chegam como 500, inclusive o
  mês fora do formato nos relatórios.

As conferências que passaram não provam regra nenhuma:

- os relatórios respondem em `text/csv` com a linha de título e o cabeçalho certos, mas vazios;
- "refund null" e "as mensagens continuam lá" passam por vácuo;
- "nenhuma rodada com as duas reservas criadas" passa porque as duas deram 500.

Para ver o que os relatórios fariam com dados, subi o serviço contra um banco próprio, inseri três reservas de julho
direto por SQL e chamei os relatórios:

- A ocupação, o financeiro e o relatório por plano vieram certos, inclusive a não reembolsável cancelada contando
  795,40 e a soma de 2.390,92.
- A taxa de turismo veio `27.0`/`18.0`/`45.0` em vez de `27.00`/`18.00`/`45.00`.
- Um mês sem reservas dá `0` no lugar de `0.00` nos totais.

Mesmo com o Jackson no lugar, a confirmação ainda exigiria um `amountPaid` no corpo, que o texto não prevê
(`ReservationRoutes.java:128-132`), e a reserva dupla simultânea não está protegida (ver Banco).

## Dúvidas

- Não sei como o resto do sistema se comportaria se o Jackson estivesse no `pom.xml`. Sem o módulo de datas
  (`jackson-datatype-jsr310`), o `LocalDate` dos DTOs talvez também não serializasse. A evidência só mostra que, como
  entregue, nada em JSON funciona. As outras falhas apontadas (a confirmação exigindo `amountPaid`, a escala da taxa, a
  corrida na criação) vêm da leitura do código e do teste manual dos relatórios, não do roteiro.
- No design, a nota `ruim` (em vez de `com problemas`) pesa a soma de defeitos espalhados: texto solto para situação e
  plano, `catch (Exception)` que disfarça erros como 404, regra de pagamento errada e duplicação. Cada um sozinho seria
  localizado.
