# Revisão: construção, cenário 09

## Visão geral

O projeto tem um pacote só, `com.pousada`, com 27 classes e 1405 linhas. Uma requisição entra numa lambda de `App.main` (`App.java:15-125`) e vai para uma classe de serviço só com métodos `static` (`ReservationService`, `RoomService`, `MessageService`, `ReportService`). Essas classes buscam a conexão num pool global (`Database.getConnection()`, `Database.java:101`) e montam o SQL com `String.format`. As tabelas são criadas com `CREATE TABLE IF NOT EXISTS` no próprio código (`Database.java:38-77`). Os relatórios seguem um Template Method correto (`Report.java:14-26` e as quatro subclasses). O defeito mais sério está na confirmação. Ela lê o valor pago de um corpo JSON que o texto não prevê (`App.java:40-41`, `ReservationService.java:71-88`). Quando o corpo não vem, ela cai com 500 (NPE em `App.java:41`). Quando vem, grava o número que o cliente mandou, e não o total da reserva. Com isso o fluxo inteiro para: confirmação, e-mail, segunda via, check-in, check-out e os relatórios, que dependem de reservas pagas. Há outros erros graves:
- o relatório financeiro e o de planos deixam de fora a reserva não reembolsável cancelada (`RevenueReport.java:38`, `RatePlansReport.java:38`);
- o dinheiro sai sem separador de milhar (`Money.java:17`);
- o SQL é montado com dados do hóspede e um escape feito à mão;
- o dinheiro é tratado como `double`;
- não há nenhum teste.

## Arquitetura

**Números:** 1 pacote (`com.pousada`, 27 classes, 1405 linhas), 0 dependências entre pacotes, sem ciclo (`measures/dependencies.md`). O único pacote usa Javalin, JDBC e o pool. A maior classe é `ReservationService` (314 linhas), seguida de `App` (138).
**Nota:** ruim

Não existem camadas. Está tudo num pacote, e as classes de "serviço" misturam regra de negócio, SQL e HTTP:
- **Regras com SQL e Javalin.** `ReservationService` guarda as regras de situação e de devolução (`ReservationService.java:77`, `:161-171`) no mesmo método do `UPDATE` montado à mão (`:83-88`, `:175-180`). Também importa `io.javalin.http.HttpStatus` (`ReservationService.java:3`) e decide códigos HTTP (`:15`, `:28`, `:32`). `ApiException` depende do Javalin (`ApiException.java:3`).
- **Relatórios.** Cada relatório abre a sua conexão e escreve o seu SQL dentro da classe que formata o CSV (`OccupancyReport.java:31-45`, `RevenueReport.java:31-46`, `TourismTaxReport.java:32-46`, `RatePlansReport.java:31-47`). Não há uma classe de acesso a dados para as reservas.
- **E-mail.** O texto do e-mail e do comprovante mora em `ReservationService` (`ReservationService.java:282-309`).
- **Montagem.** Não há montagem: tudo é `static`, e as classes se chamam diretamente (`ReservationService.java:22`, `:38`, `:279`). O pool é um `static` mutável global (`Database.java:11`).
- **Borda HTTP.** A borda até é fina na maioria das rotas. Mas as quatro rotas de relatório repetem a validação do parâmetro (`App.java:80-85`, `:92-97`, `:104-109`, `:116-121`), e a resposta 404 está escrita duas vezes no handler (`App.java:30-34`, `:66-70`).

## Design de baixo nível

**Números:** 4 relatórios, todos subclasses de `Report`, e nenhum fora da estrutura. A linha de título é escrita num lugar só (`Report.java:28-30`). Os passos abstratos são 3 (`getHeader`, `getLines`, `getTotal`). A seleção das reservas (o mesmo filtro `status IN (...)` e mês) está copiada 7 vezes nas subclasses.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** `Report` é uma classe abstrata com o método-modelo `generate()`, que monta título, cabeçalho, linhas e total (`Report.java:14-26`). O título fica num método privado (`Report.java:28-30`), e os passos que mudam são abstratos (`Report.java:32-34`). Os quatro relatórios, os dois antigos inclusive, são subclasses (`OccupancyReport.java:11`, `RevenueReport.java:11`, `TourismTaxReport.java:11`, `RatePlansReport.java:11`). Uma linha de rodapé com a data entraria só em `generate()`, e um relatório novo seria só mais uma subclasse.

O que afasta do ideal:
- `generate()` não é `final` (`Report.java:14`).
- A seleção das reservas não é um passo do esqueleto. Cada subclasse repete o filtro de reservas pagas e o filtro de mês, e calcula o total numa segunda consulta separada (`RevenueReport.java:71-79`, `TourismTaxReport.java:75-84`, `RatePlansReport.java:71-79`). Por isso as linhas e o total podem divergir, e o filtro errado (sem a cancelada não reembolsável) ficou copiado em dois relatórios.
- A taxa de R$ 4,50 é uma constante `double` (`TourismTaxReport.java:13`), quando a prefeitura muda esse valor todo ano.
- O "5" do total de ocupação está fixo no código (`OccupancyReport.java:80`).

Mesmo com isso, a estrutura cobre os quatro relatórios, e a classe é `acerto`.

**O exagero.** Não há interfaces nem fábricas sem uso. `ReportService` (`ReportService.java:3-23`) só repassa para `new XReport(...).generate()`, mas é pequeno e segue o padrão dos outros serviços. Há código morto que não chega a ser estrutura: `RoomType` com um `displayName` igual ao nome e nunca usado (`RoomType.java:3-17`), `ReservationStatus.fromString` (`ReservationStatus.java:10`) e `Money.format` (`Money.java:12`).

**O resto do código.** Os defeitos estão espalhados:
- **Dinheiro em `double`** em todo o domínio (`Reservation.java:14-19`, `PriceCalculator.java:7-10`, `Room.java:7`). O arredondamento passa por `BigDecimal` e volta a `double` a cada passo (`Money.java:7-9`). A soma da taxa é feita em ponto flutuante no SQL (`TourismTaxReport.java:76`).
- **Formatação de dinheiro.** `Money.formatBRL` troca "." por "," e não põe separador de milhar (`Money.java:17`). Os valores com `%.2f` dependem do locale padrão da JVM (`RevenueReport.java:50`, `ReservationService.java:41`).
- **Texto solto no lugar de enum.** A situação e o plano viajam como `String` (`Reservation.java:12`, `:17`), e o código compara com `.equals(ReservationStatus.X.toString())` em toda parte (`ReservationService.java:77`, `:107`, `:134`, `:161`, `:227-229`).
- **Duplicação.** A checagem "reserva não encontrada" está copiada em quatro métodos (`ReservationService.java:74-76`, `:104-106`, `:131-133`, `:158-160`). `escapeString` existe duas vezes (`ReservationService.java:311`, `MessageService.java:47`), e o mapeamento de `Room` também (`RoomService.java:17-22`, `:36-41`).
- **Erros.** Tudo é envolvido em `catch (Exception e) -> RuntimeException` (`ReservationService.java:66-67` e outros), e uma data inválida vira 500. `getDocument` devolve `null` para "não existe" e também para "não está paga" (`ReservationService.java:225`, `:230`), e a rota responde 404 nos dois casos.
- **Estado.** Os campos são públicos e mutáveis em todos os objetos de dados (`Reservation.java:4-19`, `ApiException.java:6-9`).

## Testes

**Números:** 0 testes rodaram, 0 métodos de teste escritos, nenhum arquivo em `src/test`, sem cobertura (`measures/tests.md`). O `mvn verify` passa (`BUILD SUCCESS` em `measures/tests-maven.log`), mas não verifica nada.
**Nota:** ruim

O agente não escreveu nenhum teste. O `pom.xml` declara JUnit 4 sem nenhum uso. Os scripts `integration-test.sh` e `test-api.sh` usam `curl` contra um serviço que precisa estar de pé. Eles só olham o código HTTP e imprimem o corpo (`integration-test.sh:29-36`), não rodam no `mvn verify` e não comparam nenhum valor.

Nenhuma regra tem rede de proteção: o preço com a temporada, o meio-para-o-par, os planos, a devolução e os quatro relatórios. Isso inclui a parte nova (a taxa de R$ 4,50 por hóspede por noite e a linha de cada plano). Trocar 4.50 por 5.40 em `TourismTaxReport.java:13`, ou tirar `CANCELLED` de um filtro, não faria nada falhar. Um teste simples teria pegado o erro da confirmação, que derruba o fluxo inteiro.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)`, e as datas, em `date` (`measures/database.md`). As conferências acusaram:
- 1 chave estrangeira sem índice (`reservations.room_number -> rooms`);
- nenhuma restrição (CHECK, UNIQUE ou EXCLUDE) sobre a situação, o plano, o tipo ou a sobreposição;
- 0 transações explícitas e 0 `FOR UPDATE`.

**Nota:** ruim

O esquema guarda os valores com os tipos certos, mas o código que fala com o banco é perigoso:
- **SQL montado com dado de fora.** Nome, e-mail e CPF do hóspede entram no SQL por `String.format` com um escape feito à mão (`ReservationService.java:39-55`, `MessageService.java:14-19`). Não há nenhum `PreparedStatement` no projeto.
- **O escape corrompe dados.** Ele dobra a barra invertida (`ReservationService.java:312`), mas o PostgreSQL usa `standard_conforming_strings`, então a barra é gravada dobrada. No e-mail, o corpo vai entre `$$...$$` depois de passar pelo mesmo escape (`MessageService.java:18`). Dentro de `$$` o `''` é literal, e por isso um hóspede chamado "Ana D'Ávila" recebeu "Olá, Ana D''Ávila!" (conferido à mão via `GET /messages`). Um corpo com `$$` quebraria o comando.
- **Dinheiro em `double`.** O dinheiro sai do banco como `double` (`ReservationService.java:207-212`), é gravado com `%.2f`, que depende do locale (`ReservationService.java:84`), e a taxa é somada em ponto flutuante no SQL (`TourismTaxReport.java:76`).
- **Nada protege a regra "um quarto, uma reserva por noite".** A verificação (`ReservationService.java:252-272`) e o `INSERT` (`:57-58`) usam conexões diferentes, sem transação, trava ou restrição. O roteiro criou as duas reservas simultâneas nas 10 rodadas.
- **Sem transação na confirmação.** O `UPDATE` (`ReservationService.java:83-88`) e a gravação do e-mail (`:92`) não andam juntos.
- **Sem migrações.** O esquema é criado com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:38-77`) e não sabe evoluir uma tabela que já existe.

## Comportamento

**Números:** 35 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 26 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário 09 | 7 de 13 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

**A falha que derruba o resto.** Confirmar uma reserva responde 500 (`NullPointerException: Cannot read field "amountPaid" because "body" is null`, `App.java:41`, em `measures/behavior-service.log`). O sistema espera que o site mande o valor pago num corpo JSON. O texto diz que o valor pago é o total da reserva, e o roteiro não manda corpo. Sem confirmação, também falham o e-mail, a segunda via, o check-in, o check-out e a devolução do STANDARD. Nos relatórios, nenhuma reserva paga aparece: a ocupação dá `total,0/5`, o financeiro `total,0.00`, a taxa de turismo `total,0.00` e os planos `0,0.00`. As falhas em "depois de reiniciar" vêm daí: a reserva já estava CANCELLED e o quarto, livre.

**O que conferi à mão.** Mandando o valor pago no corpo, os quatro relatórios saem com o título, o cabeçalho e os valores certos para reservas confirmadas: `2,101,2027-07-10,2027-07-13,2,3,27.00` e `STANDARD,1,899.64 / NON_REFUNDABLE,0,0.00`. Mesmo assim:
- o pago passa a ser o que o cliente informou: `{}` confirma com `amountPaid` 0.00;
- o relatório financeiro e o de planos excluem a reserva não reembolsável cancelada, que devia continuar contando (`RevenueReport.java:38`, `RatePlansReport.java:38`);
- o comprovante e o e-mail escrevem "R$ 2783,52" em vez de "R$ 2.783,52" (`Money.java:17`);
- no JSON, o dinheiro sai como `0.0` e não com 2 casas;
- quando não há desconto, o campo `discount` some do comprovante, porque o Gson omite `null` (`ReservationService.java:244`).

**Outras recusas erradas.**
- Corpo que não é JSON e campos faltando dão 500 em vez de 400 INVALID_REQUEST.
- Um plano inválido também dá 500.
- A segunda via de reserva pendente dá 404 RESERVATION_NOT_FOUND em vez de 409 (`App.java:66-69`).
- O roteiro registrou "cancelar depois do check-in" como 200, mas esse resultado é efeito da confirmação quebrada: o check-in nunca aconteceu (a reserva continuava PENDING). O código recusa o cancelamento depois do check-in (`ReservationService.java:161`).

**O que isso quer dizer para o dono da pousada.** Do jeito que o site chama, nenhuma reserva é confirmada nem paga. Ninguém recebe e-mail, a recepção não faz check-in e os quatro relatórios saem vazios. Além disso, o mesmo quarto pode ser vendido duas vezes para as mesmas noites.

## Dúvidas

- A confirmação com corpo `{"amountPaid": ...}` é uma interpretação do agente. O texto não fala em corpo e diz que o pago "é o total da reserva". Julguei como erro, e não como ponto em aberto, porque o 500 sem corpo e o pago escolhido pelo cliente contrariam o texto.
- `ReportService` é uma camada que só repassa. Considerei pequena e coerente com o resto do projeto para chamar de exagero, mas um revisor mais estrito poderia marcar `exagero`.
