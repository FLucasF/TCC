# Revisão: construção, cenário 05

## Visão geral

O projeto (`projeto/`) é um serviço Javalin com 8 pacotes: `handlers` (as rotas), `service` (as regras da reserva e o e-mail), `repository` (JDBC puro com HikariCP), `util` (o cálculo do preço e o texto dos documentos), `model`, `dto`, `database` (cria as tabelas com `CREATE TABLE IF NOT EXISTS` e põe os quartos) e a raiz com o `App`. Uma requisição entra num lambda de `ReservationHandler`, passa por um método `static` de `ReservationService`, que chama métodos `static` dos repositórios, que pegam conexões de um `HikariDataSource` guardado num campo `static` (`database/Database.java:13`). Os dados ficam em três tabelas no PostgreSQL. O defeito mais sério é que **nenhuma reserva pode ser criada**. A consulta de conflito usa `AND (? IS NULL OR id != ?)` com `setObject(4, null)` (`repository/ReservationRepository.java:101,106`), e o PostgreSQL recusa essa consulta ("could not determine data type of parameter $4", em `measures/behavior-service.log`). Por isso todo `POST /reservations` responde 500, e só 21 das 83 conferências do roteiro passaram. Mesmo sem esse erro, a confirmação exige um campo `amountPaid` no corpo e grava o valor que o cliente manda, em vez do total (`handlers/ReservationHandler.java:63-66`, `service/ReservationService.java:98`). Também não há nenhuma estrutura que concentre a escolha do documento por tipo de hóspede: o `if` sobre CPF/CNPJ/passaporte está repetido em dois lugares. Os testes são 9, todos sobre o cálculo de preço, e cobrem 4,1% das linhas.

## Arquitetura

**Números:** 8 pacotes, 14 dependências entre eles, sem ciclo (`measures/dependencies.md`). O Javalin só aparece em `handlers` e na raiz, e o JDBC só em `repository` e `database`. A maior classe é `ReservationHandler`, com 224 linhas.
**Nota:** com problemas

O que está bom: as dependências seguem a direção certa (handlers → service → repository → database) e não há ciclo. O `ReservationService` não conhece o Javalin nem monta SQL (`service/ReservationService.java:28-153`), e o SQL fica nos repositórios.

Os problemas:
- **Regras dentro dos handlers.** O handler escolhe o documento pelo tipo de hóspede e calcula o ISS (`handlers/ReservationHandler.java:207-218`, `:213-214`). Ele decide quando a segunda via existe, olhando `amountPaid` em vez da situação da reserva (`:144`). Ele lê o corpo da confirmação com um `ObjectMapper` criado ali mesmo (`:63-66`) e vai direto aos repositórios, sem passar pelo serviço (`:45`, `:109`, `:141`, `:150`). Os dois relatórios inteiros, com consulta, filtro, soma e formatação do CSV, moram no `ReportHandler` (`handlers/ReportHandler.java:30-52`, `:78-99`). Não há serviço de relatório.
- **Montagem por `static` global.** Não há um lugar que crie e ligue as peças. Tudo é método `static` e o pool vive num campo `static` mutável (`database/Database.java:13`, `:119-121`), usado por cada repositório (`repository/ReservationRepository.java:20`). Trocar o banco num teste, ou injetar uma dependência, exige mexer em tudo.
- **O e-mail fica fora do serviço.** É o handler que chama o `MessageService` depois de confirmar (`handlers/ReservationHandler.java:71-75`), e não o serviço da reserva.

São misturas localizadas, sem ciclo, e as camadas existem: nota `com problemas`.

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt), 0 na estrutura de criação (não há estrutura). A escolha por tipo de hóspede é uma cadeia `if/else if` sobre `guestCpf`/`companyCnpj`/`guestPassport` repetida em 2 lugares: `handlers/ReservationHandler.java:207-218`, que serve o check-out e a segunda via, e `service/MessageService.java:41-50`, que serve o e-mail.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede três documentos que saem nos mesmos três lugares e avisa que vem um quarto, o das agências. Nada concentra a criação. `DocumentFormatter` tem um método estático por documento (`util/DocumentFormatter.java:15`, `:38`, `:66`), mas quem decide qual chamar são dois `if` copiados (`handlers/ReservationHandler.java:207-218` e `service/MessageService.java:42-49`). A referência chama isso de omissão: funções auxiliares separadas por documento não bastam, o que importa é onde se decide qual criar. Também não há um tipo de documento nem um tipo de hóspede. A reserva carrega os cinco campos soltos (`model/Reservation.java:11-15`), e o tipo de hóspede é deduzido de qual campo não é nulo. O título do documento é escrito à mão no handler (`:208`, `:211`, `:216`), e o ISS é calculado em dois lugares (`handlers/ReservationHandler.java:213-214` e `util/DocumentFormatter.java:39-40`). A agência de turismo exigiria mexer no handler, no `MessageService`, no `DocumentFormatter` e na validação (`service/ReservationService.java:162-183`).

**O exagero.** Não há interface sem uso, fábrica, builder nem camada que só repasse: contenção.

**O resto do código.**
- **Os tipos.** O dinheiro está certo: `BigDecimal`, com `HALF_EVEN` em cada passo (`util/PriceCalculator.java:33-66`). As datas são `LocalDate`, e a situação e o plano são `enum`. Mas `PriceCalculator.calculatePrice` recebe o plano como `String` e compara com `"NON_REFUNDABLE"` (`util/PriceCalculator.java:46`, `:58`), embora o `enum RatePlan` exista e seja convertido logo antes (`service/ReservationService.java:61-62`).
- **Duplicação.** O `record ErrorResponse` é copiado em cinco classes (`App.java:40`, `handlers/MessageHandler.java:24`, `handlers/ReportHandler.java:115`, `handlers/ReservationHandler.java:223`, `handlers/RoomHandler.java:20`). O bloco `catch` que traduz `ValidationError` em status se repete em cada rota (`handlers/ReservationHandler.java:26-39`, `:48-55`, `:78-86`, `:94-102`, `:111-119`, `:127-135`, `:152-160`). O corpo de `formatComprovante` e o de `formatFatura` são quase iguais (`util/DocumentFormatter.java:19-33` e `:46-60`).
- **Os erros.** Há `catch (Exception)` em toda rota, e cada um escolhe um status diferente para a mesma coisa. Um corpo que não é JSON vira 500 (`handlers/ReservationHandler.java:35-38`), um plano de tarifa inválido também vira 500 (`RatePlan.valueOf` em `service/ReservationService.java:61`), e uma falha do banco no relatório vira 400 INVALID_REQUEST (`handlers/ReportHandler.java:56-59`). A mensagem da exceção interna vai para o cliente (`e.getMessage()` em todas). A falha do e-mail é engolida e a confirmação segue (`handlers/ReservationHandler.java:71-75`).
- **O estado e os dados.** Os DTOs têm campos públicos (`dto/ReservationResponse.java:10-67`). O pool fica em campo `static` mutável (`database/Database.java:13`). A confirmação aceita o valor pago enviado pelo cliente (`service/ReservationService.java:89-98`) em vez de usar o total.

O código é pequeno e legível, e os defeitos estão sobretudo nos handlers: nota `com problemas`.

## Testes

**Números:** 9 testes rodaram, 0 falhas, 0 erros, 0 pulados; 9 métodos de teste escritos, todos em 1 arquivo. Cobertura: 32 de 787 linhas (4,1%) e 16 de 178 ramos (9,0%). Só o pacote `util` tem alguma cobertura (32 de 105 linhas). `handlers`, `service`, `repository` e `database` ficam com 0. Nenhum teste passa pela API ou pelo banco (`measures/tests.md`).
**Nota:** ruim

O único arquivo, `src/test/java/com/pousada/marealta/util/PriceCalculatorTest.java`, confere o preço com valores, e isso é bom: a alta temporada (`:13-32`), a diária com 20% (`:42-46`, 299,88) e os dois planos de 10 a 12/07 (`:58-87`, 599,76; desconto 89,96; total 509,80). O teste que diz conferir o "meio para o par" (`:90-102`) usa 249,90 × 2, que não arredonda nada, e por isso não confere a regra.

Ficaram sem nenhum teste:
- as situações da reserva e as transições;
- o cancelamento e a devolução;
- os códigos de erro;
- os relatórios;
- toda a parte do cenário: a validação de exatamente uma identificação, a escolha do documento, a fatura com ISS e o booking receipt em inglês.

A pergunta da mutação: se trocassem `0.05` por `0.50` no ISS (`util/DocumentFormatter.java:39`), `"1 night"` por `"1 nights"` (`:73`), a vírgula do milhar em BRL (`:100`), ou apagassem a recusa de duas identificações (`service/ReservationService.java:171-173`), nenhum teste falharia. Como não há teste pela API nem contra o banco, o `mvn verify` passa com o sistema incapaz de criar uma reserva (ver Comportamento). Testes quase ausentes para as regras pedidas: `ruim`.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` nas 6 colunas e as datas em `date`. A chave estrangeira `reservations.room_number` tem índice (`idx_reservation_room_dates`). Não há nenhuma restrição CHECK, UNIQUE ou EXCLUDE. `status`, `rate_plan`, `type` e `channel` são `varchar` sem CHECK. O esquema é criado com `CREATE TABLE` no Java, sem migrações. Há 0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** ruim

- **A consulta que guarda a regra principal não roda.** `hasConflict` usa `AND (? IS NULL OR id != ?)` e passa `null` com `setObject` (`repository/ReservationRepository.java:101`, `:106-107`). O driver manda o parâmetro sem tipo e o PostgreSQL recusa a consulta com "could not determine data type of parameter $4" (`measures/behavior-service.log`, repetido em todo `POST /reservations`). Como `createReservation` chama `hasConflict` antes de gravar (`service/ReservationService.java:56`), nenhuma reserva chega ao banco. É o código que fala com o banco quebrando o caminho principal: dado que deveria ficar no banco e não fica.
- **A regra "um quarto, uma reserva por noite" não tem proteção no banco.** Mesmo que a consulta funcionasse, a verificação e o `INSERT` correm em conexões separadas, sem transação nem trava (`repository/ReservationRepository.java:92-113` e `:19-60`). Não há `EXCLUDE` nem outra restrição, e o índice parcial (`database/Database.java:88-92`) não impede nada. Dois pedidos ao mesmo tempo poderiam reservar o mesmo quarto.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` no código (`database/Database.java:44-92`) não sabe acrescentar uma coluna numa tabela que já existe, e o próximo pedido (a agência) vai precisar disso.
- **Detalhes menores.** Os `Statement` de `createTables` não são fechados (`database/Database.java:44`, `:53`, `:78`, `:88`). As colunas de situação e de plano aceitam qualquer texto.

O que está certo: SQL sempre com `?` (o script não achou SQL montado com `+`), try-with-resources nas conexões, um pool (HikariCP) e os tipos de dinheiro e data corretos. Mas a consulta quebrada impede que qualquer reserva seja gravada: `ruim`.

## Comportamento

**Números:** 21 de 83 conferências passaram (62 falharam), segundo `measures/behavior.md`:
- parte comum: 15 de 46;
- dados depois de reiniciar: 1 de 3;
- cenário 05: 4 de 32;
- reservas simultâneas: 1 de 2.

**Nota:** ruim

**O sistema não cria reservas.** Todo `POST /reservations` válido responde 500 INTERNAL_ERROR, pelo erro de SQL descrito em Banco (o defeito pesa naquela coluna e aparece aqui porque está na chamada principal do texto). Daí vêm quase todas as falhas: preço, confirmação, e-mail, check-in, check-out, cancelamentos, relatórios, os três documentos, o reinício e as dez rodadas simultâneas (todas 500/500). As conferências que passaram são, em geral, recusas que acontecem antes da consulta ao banco: capacidade, quarto inexistente, datas, campos faltando e as quatro identificações inválidas do cenário. Algumas passam no vazio: "as mensagens continuam lá" com a lista vazia, e "nenhuma rodada com as duas criadas" porque nenhuma foi criada. Para o dono da pousada, o site não consegue fazer nenhuma reserva.

Para ver o que viria depois, criei uma reserva de empresa direto no banco, num banco de teste à parte, e chamei a API:
- **A confirmação sem corpo responde 400.** A mensagem é a de um `NullPointerException`, porque o código exige `amountPaid` no corpo (`handlers/ReservationHandler.java:66`). Com `{"amountPaid":1}`, a reserva ficou CONFIRMED com `amountPaid` 1, e não com o total de 779,80. O texto diz que o pago é o total. O roteiro do comportamento falharia aqui mesmo sem o erro do SQL.
- **O e-mail e a fatura saíram certos nesse caso:** "Fatura de hospedagem nº 1", "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "ISS retido (5%): R$ 38,99", e o JSON com `text`. A geração do texto da fatura funciona quando é alcançada.
- **A segunda via sai para uma reserva CANCELADA.** Responde 200, porque a regra olha `amountPaid > 0` e não a situação (`handlers/ReservationHandler.java:144`). O texto limita a segunda via a reservas confirmadas, com check-in ou com check-out.
- **Os campos que não se aplicam somem do JSON em vez de vir `null`.** É efeito do `@JsonInclude(NON_NULL)` (`dto/ReservationResponse.java:7`, `dto/DocumentResponse.java:7`). O mesmo vale para `refund` antes do cancelamento.
- **O relatório financeiro lista a STANDARD cancelada e devolvida com 0,00.** Ela não deixou dinheiro com a pousada e não deveria aparecer (`repository/ReservationRepository.java:153-154`). O total sai como `total,,,,0.00` em vez de `total,0.00`, que é a forma que o texto sugere.
- **Recusas com status errado.** Um corpo que não é JSON responde 500 em vez de 400 (roteiro), e um `ratePlan` desconhecido também responde 500.

A parte comum quebra e quase todo o cenário falha: `ruim`.

## Dúvidas

- **A classe do pattern.** A referência descreve a omissão como o `if` repetido "nos três lugares". Aqui ele está em dois, porque o check-out e a segunda via compartilham `getDocumentResponse` (`handlers/ReservationHandler.java:189`). Mesmo assim, não existe estrutura de criação (fábrica, tipo de documento ou tipo de hóspede polimórfico), e a escolha não está num lugar só. Por isso dei `omissão` e não `malfeito`.
- **A divisão do erro do SQL entre colunas.** O mesmo erro (`repository/ReservationRepository.java:101`) derruba Banco e Comportamento. Dei `ruim` em Banco por ele: dados que deveriam ficar no banco não ficam. Comportamento seria `ruim` mesmo sem ele, porque a confirmação exige `amountPaid` no corpo e não usa o total.
- **A linha `total` do financeiro.** O formato `total,,,,0.00` pode ser uma leitura possível do texto ("uma linha total com a soma"). Não pesei isso na nota.
