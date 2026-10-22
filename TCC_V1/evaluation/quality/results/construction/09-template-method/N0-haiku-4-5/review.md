# Revisão: construção, cenário 09

## Visão geral

O projeto tem quatro pacotes: `com.pousada` (o `main`, o `Database` com um pool estático e todas as rotas em `RouteConfig`), `model`, `repository` (JDBC puro, métodos `static`) e `service` (regras e relatórios, também `static`). Uma requisição entra numa lambda de `RouteConfig`, que lê o corpo com Gson, chama um método estático de `ReservationService` ou `ReportService`, e esses chamam os repositórios estáticos, que pegam a conexão em `Database.getConnection()`. O esquema é criado com `CREATE TABLE IF NOT EXISTS` em `Database.java:41-79`. O problema mais sério é que o serviço não consegue escrever nenhuma resposta JSON. O `pom.xml` não tem Jackson (o Gson só lê o corpo) e o `Javalin.create()` (`Application.java:14`) não recebe outro mapeador de JSON. Por isso todo `ctx.json(...)` dá 500, até as respostas de erro. `GET /rooms`, a criação, a confirmação, o check-in, o check-out, a segunda via, `/messages` e todos os códigos de erro respondem 500 com a mensagem "It looks like you don't have an object mapper configured". E a escrita acontece antes da resposta que falha: a reserva é gravada e confirmada no banco, e o cliente recebe 500. Ou seja, o site não funciona. O comportamento passou em 12 de 64 conferências. Os relatórios não usam o template que o texto pedia: são quatro métodos escritos inteiros. E duas reservas simultâneas para o mesmo quarto entram as duas.

## Arquitetura

**Números:** 4 pacotes (`com.pousada` com 3 classes e 475 linhas, `model` com 5 e 163, `repository` com 3 e 312, `service` com 5 e 439), 7 dependências entre pacotes e 1 ciclo, entre `com.pousada`, `service` e `repository`. A maior classe é `RouteConfig`, com 333 linhas (`measures/dependencies.md`).
**Nota:** com problemas

As camadas existem e apontam quase sempre no sentido certo. O JDBC fica só em `repository` (e em `Database`), e `service` não usa Javalin nem JDBC (tabela de bibliotecas em `measures/dependencies.md`). Os defeitos:

- **O ciclo.** Os repositórios importam `com.pousada.Database` (`repository/ReservationRepository.java:3`, `RoomRepository.java:3`, `MessageRepository.java:3`), e o pacote raiz, onde estão as rotas, importa os repositórios e os serviços. A infraestrutura de banco está no mesmo pacote que a borda HTTP.
- **A borda HTTP não é fina.**
  - `RouteConfig.java:44-69` valida as datas e a quantidade de hóspedes. A mesma validação se repete em `ReservationService.java:18` e `:32`.
  - A segunda via (`RouteConfig.java:181-212`) vai direto ao `ReservationRepository` e ao `RoomRepository`, sem passar por um serviço. Por isso a regra "só para reserva paga" não existe em lugar nenhum.
  - O check-out monta o comprovante dentro da rota (`RouteConfig.java:157-170`) e ainda gera um texto que fica sem uso (`RouteConfig.java:155`).
- **A montagem.** Não há montagem: tudo é `static`. Há o `DataSource` estático e mutável (`Database.java:13`), e serviços e repositórios chamados por nome de classe (`ReservationService.java:22`, `DocumentService.java:19`, `ReportService.java:25`). Nada pode ser trocado nem testado isolado.
- **O tamanho.** `RouteConfig` concentra todas as 13 rotas, a montagem do JSON e o tratamento de erro, com 333 linhas.

## Design de baixo nível

**Números:** 4 relatórios, 0 dentro de uma estrutura comum. São 4 métodos estáticos escritos inteiros em `ReportService` (`ReportService.java:17`, `:40`, `:71`, `:111`), cada um com a sua linha de título copiada (`:21`, `:50`, `:83`, `:137`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede, sem dar o nome, um Template Method: o esqueleto do relatório num lugar só (título, cabeçalho, linhas, total), para que um novo relatório, ou uma nova linha final com a data, entre num único ponto. O agente escreveu quatro métodos completos em `ReportService.java`. Cada um tem seu `StringBuilder`, sua linha "Pousada Maré Alta,<nome>,<período>" (`:21`, `:50`, `:83`, `:137`), seu cabeçalho e sua linha de total (`:35`, `:66`, `:106`, `:145`). A linha com a data que vem no ano que vem teria de ser copiada em quatro lugares, e cada relatório novo seria um quinto método copiado. Não há classe abstrata, nem escritor genérico, nem função que receba as partes. É omissão. Não há exagero: nenhuma interface, fábrica ou camada sobrando.

**O resto do código.** Tem defeitos espalhados:

- **Tipos e dependências.** O `pom.xml` não traz um mapeador de JSON, e todos os `ctx.json` quebram (`RouteConfig.java:32`, `:78`, `:116` etc.).
- **Erros.**
  - Os erros de domínio são `IllegalArgumentException` com o código no texto (`ReservationService.java:24`), e a rota volta a compará-los como texto (`RouteConfig.java:290-311`).
  - Há `catch (Exception e)` em todas as rotas (`RouteConfig.java:83`, `:99`, `:119`, `:132`, `:175`, `:208` …), que transformam qualquer falha, inclusive do banco, em 400 INVALID_REQUEST.
  - `RouteConfig.java:79-80` relança o `BadRequestResponse` do Javalin, cujo corpo não é `{error, message}`.
- **Duplicação.**
  - O comprovante em JSON está copiado em `RouteConfig.java:157-170` e `:192-205`.
  - A conta "pago menos devolvido" está em `ReportService.java:55` e `:126`.
  - `formatDate` e `formatDateISO` são idênticos (`ReportService.java:150-156`).
- **Regras no lugar errado ou erradas.**
  - O preço da noite é calculado só pela data de entrada e multiplicado pelas noites (`ReservationService.java:42`). Uma estadia de 13 a 16/12 sai toda pela tarifa baixa.
  - A confirmação exige `amountPaid` no corpo (`RouteConfig.java:110-114`), quando o texto diz que o pago é o total da reserva.
  - O dinheiro do comprovante sai com `String.format("%.2f")` no locale da JVM (`DocumentService.java:53-55`). O e-mail gravado mostra "R$ 599.76", não "R$ 599,76", e não há separador de milhar. O `DecimalFormat` declarado em `DocumentService.java:11-16` nunca é usado.
  - A ocupação fixa "/5" no texto (`ReportService.java:35`).
  - A taxa de R$ 4,50, que o texto diz que muda todo ano, é uma constante local dentro do método (`ReportService.java:87`).
- **Estado.** As entidades são mutáveis, com setters para tudo (`model/Reservation.java`), e todo o código é `static`.

## Testes

**Números:** 8 testes rodaram, 0 falhas, 0 erros, 0 pulados; 8 métodos escritos, todos em `PricingCalculatorTest`. A cobertura de linhas é de 24 de 766 (3,1%), e a de ramos, de 12 de 160 (7,5%). Os pacotes `com.pousada` e `repository` ficam com 0% (`measures/tests.md`).
**Nota:** ruim

Os oito testes são de unidade, sobre `PricingCalculator`, e conferem valores:

- o acréscimo de 20% em julho, de 249,90 para 299,88 (`PricingCalculatorTest.java:14-23`);
- a borda de 14 e 15/12 (`:37-57`);
- o subtotal (`:70-77`);
- o desconto de 15% com 89,964 arredondado para 89,96 (`:80-87`).

Fora isso, não há nada:

- nenhum teste da API, e por isso o `mvn verify` passa com um serviço que responde 500 a quase tudo;
- nenhum teste do banco;
- nenhum teste das situações da reserva, do cancelamento e da devolução, ou dos códigos de erro;
- nenhum teste do comprovante e do e-mail;
- sobretudo, nenhum teste dos quatro relatórios, que são a parte nova do cenário.

A pergunta da mutação: trocar `4.50` por `5.40` (`ReportService.java:87`), apagar a linha do `NON_REFUNDABLE` (`:141`) ou trocar o filtro do financeiro (`:46`) não derrubaria nenhum teste. O preço por noite calculado só pela data de entrada (`ReservationService.java:42`) também passa sem ser visto, porque os testes chamam `calculateNightlyPrice` com uma data só.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Todo o dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram:

- a chave estrangeira `reservations(room_number)` sem índice;
- nenhum CHECK em `status`, `rate_plan`, `type` e `channel`;
- nenhuma restrição (UNIQUE, EXCLUDE ou CHECK) que proteja uma regra;
- 0 transações e 0 `FOR UPDATE`;
- um SQL montado com `+` (`measures/database.md`).

**Nota:** ruim

Os tipos estão certos e a reserva está ligada ao quarto (`Database.java:67`). Os problemas:

- **Reserva duplicada.** A disponibilidade é um `SELECT COUNT(*)` (`ReservationRepository.java:94-116`) seguido de um `INSERT` separado (`ReservationService.java:36` e `:54`), sem transação, trava ou restrição no banco. Conferi à mão, com o serviço de pé: em 5 rodadas de dois pedidos simultâneos para o 101, em 4 delas o banco ficou com **duas** reservas PENDING para as mesmas noites (2028-02, 03, 04 e 05). É dado errado gravado: o mesmo quarto vendido duas vezes. O roteiro não viu isso porque as duas respostas vêm 500.
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:41`, `:50`, `:72`), sem migrações, e não sabe acrescentar uma coluna numa tabela que já existe.
- **SQL montado com `+`.** `RoomRepository.java:20` monta o SQL com `+`. O valor é um `int`, então não há injeção, mas o padrão é ruim.
- **Recursos.** Os `ResultSet` não ficam em try-with-resources (`ReservationRepository.java:62`, `:110`), mas as conexões são devolvidas ao pool (Hikari).
- **As consultas.**
  - O relatório mensal filtra com `EXTRACT(YEAR/MONTH FROM check_in)` (`ReservationRepository.java:122`), que não usa índice.
  - A ocupação busca o quarto uma vez por linha (`ReportService.java:25`).
- **Os quartos.** São semeados só se a tabela estiver vazia (`Database.java:91-113`), o que está bem.

## Comportamento

**Números:** 12 de 64 conferências passaram (`measures/behavior.md`):

- a parte comum: 4 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 09: 6 de 13;
- as reservas simultâneas: 1 de 2.

**Nota:** ruim

O serviço sobe, mas toda resposta JSON dá 500 ("It looks like you don't have an object mapper configured", em `measures/behavior-service.log`; causa em `Application.java:14` e no `pom.xml` sem Jackson). Para o dono da pousada, isso quer dizer:

- **O site não funciona.** O site não consegue listar os quartos, não recebe o número da reserva que criou, não confirma, e a recepção não faz check-in nem check-out pela API. Todos os erros que o site combinou (404, 409, 422, 400) chegam como 500.
- **Os dados mudam mesmo com 500.** A reserva é gravada e a confirmação muda a situação e grava o e-mail, mas o cliente recebe 500. Quem tentar de novo vai achar o quarto ocupado por uma reserva que ele não sabe que existe.

Os CSV escapam porque usam `ctx.result`. A linha de título está certa nos quatro (`occupancy`, `revenue`, `tourism-tax`, `rate-plans`), e o mês vazio da taxa de turismo sai certo. Mas as linhas não aparecem no roteiro, porque nenhuma reserva chegou a ser confirmada (cada confirmação respondeu 500, e o roteiro nem tinha o número da reserva). Um mês fora do formato dá 500, não 400.

Pela leitura e por uma chamada à mão, quando uma reserva é confirmada direto pela API:

- o financeiro sai certo para ela (`1,101,2027-07-10,2027-07-12,CONFIRMED,599.76`);
- mesmo assim, o financeiro e o de planos contariam uma STANDARD cancelada, que devolveu tudo, como linha com 0.00 e como reserva do plano (`ReportService.java:46`, `:117`), quando o texto diz "deixaram dinheiro com a pousada";
- o e-mail sai com "R$ 599.76", e não "R$ 599,76" (`DocumentService.java:53-55`);
- a confirmação exige `amountPaid` no corpo; sem ele, recusa;
- a segunda via de uma reserva pendente não é recusada (`RouteConfig.java:181-212`);
- a estadia que atravessa o começo ou o fim da alta temporada sai com o preço errado (`ReservationService.java:42`).

## Dúvidas

O e-mail com "R$ 599.76" depende do locale da JVM (`String.format` sem `Locale` em `DocumentService.java:54`). Num ambiente em pt-BR sairia "599,76", ainda sem separador de milhar. A conferência à mão foi feita no locale desta revisão.
