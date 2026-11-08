# Revisão: construção, cenário 01

## Visão geral

O projeto tem 7 pacotes e cerca de 1.800 linhas em `src/main`. Uma requisição entra pelas rotas declaradas em `Main.java:70-83` e passa por um controller (`controller/`), que lê o corpo como `Map` e o converte à mão. Daí vai para um serviço (`service/`), que reúne a regra, o SQL (JDBC puro, com `DriverManager` a cada chamada) e o texto do e-mail. Os dados ficam em três tabelas, criadas com `CREATE TABLE IF NOT EXISTS` em `config/Database.java:28-76`. Os planos de tarifa são um Strategy bem feito: uma interface, cinco implementações e um mapa que escolhe a certa. O defeito mais sério está na criação da reserva: `ReservationController.java:34-37` faz cast para `String` em `roomNumber` e nos outros campos de texto. Quando o site manda `"roomNumber": 201` como número JSON, a resposta é 500 com `ClassCastException`, e foi isso que derrubou 61 das 72 conferências do roteiro. Corpo que não é JSON também dá 500. O segundo defeito sério: nada impede duas reservas do mesmo quarto na mesma noite. Chamando a API à mão, as duas foram criadas em 10 de 10 rodadas simultâneas.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Só `controller` e `Main` usam Javalin; `config` e `service` usam JDBC; `service.rateplan` não usa Javalin nem JDBC. A maior classe é `ReservationService`, com 393 linhas, mais que o dobro da seguinte.
**Nota:** com problemas

- **O que está bem.** A montagem acontece num lugar só (`Main.java:34-44`), com as dependências passadas pelo construtor. As dependências vão da borda para os serviços e dos serviços para o domínio, sem ciclo. Os planos (`service/rateplan/`) não conhecem Javalin nem JDBC.
- **Não há camada de dados.** Os quatro serviços montam e executam SQL direto: `ReservationService.java:56-91`, `:286-365`, `ReportService.java:26-57`, `RoomService.java:24-44` e `MessageService.java:21-35`.
- **`ReservationService` faz de tudo.** Junta a máquina de situações (`:122-199`), o acesso ao banco, o texto do e-mail (`:221-244`), a formatação do comprovante, do CPF e do dinheiro (`:246-284`) e o mapeamento do `ResultSet` (`:367-392`).
- **A borda conhece regra.** O controller converte o corpo à mão e decide parte das recusas, como saída antes da entrada (`ReservationController.java:46`). A regra, por sua vez, conhece HTTP: os planos lançam `ApiException(400, ...)` com o status embutido (`CorporateRatePlan.java:23`).

São misturas localizadas, não a ausência de camadas.

## Design de baixo nível

**Números:** os 5 planos de tarifa estão na estrutura e nenhum fica fora dela. Preço, validação e devolução de cada plano moram na sua própria classe. Não há `switch` nem `if` sobre o plano fora de `service/rateplan/`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.**
- A interface `RatePlanStrategy` (`service/rateplan/RatePlanStrategy.java:8-12`) tem `validate`, `calculatePricing` e `calculateRefund`, e há uma implementação para cada um dos cinco planos, inclusive os dois do ponto de partida: `StandardRatePlan` e `NonRefundableRatePlan`.
- A escolha é um único mapa (`ReservationService.java:25-31`). O cálculo (`:49-51`) e o cancelamento (`:150-151`) pedem ao plano sem nomeá-lo.
- As regras de quem pode usar ficam no próprio plano:
  - a sexta ao domingo, em `WeekendPackageRatePlan.java:22-34`;
  - as 7 noites, em `LongStayRatePlan.java:14-17`;
  - o código da empresa, em `CorporateRatePlan.java:20-28`.
- As devoluções também: a metade em `WeekendPackageRatePlan.java:46-50` e nada em `NonRefundableRatePlan.java:28-30`.
- O único resto de plano fora da estrutura é pequeno: o controller guarda `companyCode` sempre que ele vem (`ReservationController.java:62-64`), e só o convênio o lê.
- Um plano novo é uma classe e uma linha no mapa (e uma constante no `enum RatePlan`).

**O exagero.** Não há interface com uma implementação só, fábrica, builder nem camada que só repassa: contenção. A herança de `StandardRatePlan` para aproveitar o preço por noite com a temporada é uma escolha razoável.

**O resto do código** tem defeitos localizados:
- **Casts sem cuidado** em `ReservationController.java:34-37`, `:46-47`, `:57` e `:63`, sobre `Map` cru (`:22`). Um tipo inesperado no JSON vira `ClassCastException`, e a resposta é 500 em vez de 400. É a causa da quebra do comportamento.
- **Dinheiro sem 2 casas.** `BigDecimal.ZERO` sem escala vai para o JSON como `0` em vez de `0.00`, em `Reservation.java:27` (o `amountPaid` da reserva nova) e em `NonRefundableRatePlan.java:29` (a devolução).
- **Formatação que depende da máquina.** `formatMoney` (`ReservationService.java:283`) usa `String.format("R$ %,.2f")` com o locale padrão da JVM e depois troca vírgula por ponto com `replace`. Numa máquina em pt_BR, o comprovante sairia com `1,234.56`.
- **Desconto copiado.** As mesmas linhas, com só a porcentagem mudando, aparecem três vezes: `CorporateRatePlan.java:34-44`, `LongStayRatePlan.java:23-33` e `NonRefundableRatePlan.java:14-24`.
- **Estado espalhado.** As estratégias preenchem a reserva por setters (`calculatePricing` é `void` e muda o objeto recebido). A conversão para a resposta é um `HashMap` montado campo a campo (`ReservationController.java:121-138`).
- **Total do mês sem casas.** O total vazio sai `0` (`ReportService.java:79`, `:112`).

`BigDecimal` com `HALF_EVEN`, `LocalDate` e `enum` estão nos lugares certos.

## Testes

**Números:** 6 testes rodaram, 0 falharam, 0 erros, 0 pulados; 6 métodos escritos, todos em `ReservationTest.java` (`measures/tests.md`). Cobertura de linhas: 110 de 804 (13,7%). Cobertura de ramos: 9 de 114 (7,9%). Os pacotes `service`, `controller`, `config` e `Main` têm 0%. Não há teste pela API nem contra o banco.
**Nota:** com problemas

**O que os testes conferem, com valores do texto, chamando cada plano direto:**
- o STANDARD em alta temporada dá 599,76 (`ReservationTest.java:15-30`);
- a Tecnosul dá 779,80, 93,58 e 686,22 (`:33-49`);
- o pacote deluxe dá 820,00, com devolução de 410,00 (`:52-70`);
- a estadia longa dá 1.749,30, 174,93 e 1.574,37 (`:73-88`);
- as devoluções do normal e do não reembolsável (`:91-110`).

**O que nenhum teste confere:**
- as validações dos três planos novos;
- os descontos da Agrovale e da Farmavida;
- o preço do não reembolsável;
- a devolução do convênio e da estadia longa;
- as bordas da temporada (15/12, fim de fevereiro);
- a máquina de situações, os códigos de erro, o choque de datas, o e-mail, o comprovante e os relatórios.

**A pergunta da mutação.** Testes que passariam com o código errado:
- trocar `< 7` por `<= 7` em `LongStayRatePlan.java:15`;
- apagar a conferência da sexta-feira em `WeekendPackageRatePlan.java:27`;
- trocar 0.25 por 0.52 na Farmavida (`CorporateRatePlan.java:16`).

Nenhum teste chama a API, por isso a quebra que derruba a criação de reservas (`ReservationController.java:34`) passou sem ser notada.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; datas em `date` (`measures/database.md`). As conferências que acusaram algo:
- a chave estrangeira `reservations(room_number) -> rooms` não tem índice;
- não há nenhuma restrição UNIQUE, EXCLUDE ou CHECK, nem nas colunas de situação, plano e tipo;
- não há pool de conexões, transação explícita nem `FOR UPDATE`.

**Nota:** ruim

- **Duas reservas na mesma noite.** Nada protege a regra de que um quarto não pode ter duas reservas na mesma noite. `ReservationService.java:53-54` faz um `SELECT COUNT(*)` (`:286-320`), e o `INSERT` acontece depois, em outra conexão (`:56-91`), sem transação, trava nem restrição. Com duas chamadas simultâneas para o quarto 301 (com `roomNumber` como texto), as duas reservas foram criadas em 10 de 10 rodadas. A consulta no banco confirmou 10 pares de reservas não canceladas do mesmo quarto na mesma data. É dado errado gravado: o mesmo quarto vendido duas vezes.
- **Escritas que deviam andar juntas, em conexões separadas.** A confirmação faz duas escritas, a situação e depois o valor pago (`ReservationService.java:130-133`). O cancelamento também, a situação e depois a devolução (`:153-154`). Uma falha entre as duas deixa, por exemplo, uma reserva CONFIRMED com `amount_paid` 0.
- **Esquema sem migração.** O esquema é criado no código com `CREATE TABLE IF NOT EXISTS` (`config/Database.java:28-76`), que não sabe acrescentar uma coluna numa tabela que já existe.
- **Uma conexão nova por consulta.** `DriverManager.getConnection` abre uma conexão a cada consulta (`Database.java:20-22`), sem pool.

O resto está certo: todo SQL usa parâmetros (o único trecho montado com `+`, `ReservationService.java:296`, é constante), as conexões fecham com try-with-resources, e os dados continuam lá depois de reiniciar (conferido à mão).

## Comportamento

**Números:** 11 de 72 conferências passaram (`measures/behavior.md`):
- a parte comum: 9 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 01: 0 de 21;
- as reservas simultâneas: 1 de 2. Nenhuma rodada criou duas reservas, mas só porque todas as respostas foram 500.

**Nota:** ruim

**Por que quase tudo falhou.** O roteiro manda `"roomNumber": 201` como número JSON, e o serviço responde 500 a toda criação de reserva (`ClassCastException` em `ReservationController.java:34`, no `measures/behavior-service.log`). Sem reserva criada, caem em cascata a confirmação, o e-mail, o check-in, o check-out, os cancelamentos, os relatórios, a persistência e todos os planos novos. O texto não fixa o tipo do campo, e o agente escolheu texto de forma coerente (`GET /rooms` também devolve `"number":"101"`). Por isso o "comporta 3" e a "diária 389.90" do 201 também falharam: o roteiro procura o número 201. Mas um sistema que derruba com 500, em vez de aceitar o número ou recusar com 400, quebra a chamada principal do site.

**Chamando a API à mão, com `roomNumber` como texto,** as regras de preço e as situações funcionam:
- os valores do texto conferem: 599,76; 37,48 e 212,42; 686,22; Farmavida 584,85; 820,00 e 410,00; 1.200,00 na suíte em julho; 1.574,37; estadia longa com temporada 1.754,30;
- convênio sem código ou com código desconhecido, pacote fora da sexta ao domingo e estadia longa de 6 noites dão 400;
- capacidade, quarto inexistente, datas e hóspedes inválidos dão os códigos certos;
- a confirmação, o e-mail com o texto pedido, o check-in, o check-out com o comprovante e as devoluções estão certos.

**Mesmo assim, o dono da pousada veria estes erros:**
- **Quarto vendido duas vezes.** Duas reservas simultâneas do mesmo quarto foram criadas nas 10 rodadas (defeito do banco, contado lá).
- **Corpo que não é JSON dá 500**, não o 400 `INVALID_REQUEST` que o texto pede.
- **Financeiro com linhas a mais.** O relatório lista reservas canceladas com devolução total, com valor 0.00 (`ReportService.java:75` filtra por `amount_paid > 0`, não pelo que ficou com a pousada). O texto pede só as que deixaram dinheiro. O total continua certo.
- **Segunda via de reserva cancelada.** É emitida com 200 (`ReservationService.java:193` só recusa a PENDING), mas o texto diz que a segunda via só existe para reserva confirmada, com check-in ou com check-out.
- **Dinheiro sem 2 casas.** O `amountPaid` da reserva nova e a devolução do não reembolsável vão como `0`, não `0.00`.

## Dúvidas

- **O tipo de `roomNumber`.** O texto não diz se é número ou texto no JSON, e o agente escolheu texto de forma coerente. Se o site combinar texto, quase todo o roteiro passaria. Sobrariam a reserva duplicada, o 500 no corpo que não é JSON, o financeiro, a segunda via de cancelada e o `0` sem casas, e a nota do comportamento seria `com problemas`. Mantive `ruim` porque a medida oficial deu 11 de 72 e porque responder 500 a um tipo inesperado é defeito do agente, qualquer que seja a interpretação.
- **A nota dos testes.** Fica entre `com problemas` e `ruim`: são poucos testes (6, com 13,7% das linhas), mas conferem com valores os exemplos do texto. Dei `com problemas`.
