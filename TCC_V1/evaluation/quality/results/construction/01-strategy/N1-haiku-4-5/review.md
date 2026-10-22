# Revisão: construção, cenário 01

## Visão geral

O projeto tem 8 pacotes: `api` (uma classe, `ApiRoutes`, com todas as rotas do Javalin), `service` (`ReservationService` e `ReportService`), `service.pricing` (uma estratégia por plano de tarifa, uma fábrica e uma `RefundPolicy`), `database` (o pool Hikari, o `CREATE TABLE` e três repositórios em JDBC), `model`, `exception` e `util`. Uma requisição entra em `ApiRoutes`, que lê o corpo como `Map` e o converte à mão, chama o `ReservationService`, que valida, pede o preço à estratégia do plano e grava pelo `ReservationRepository`. Os dados ficam em três tabelas criadas no código (`Database.java:29-70`). O mais sério está no comportamento. A coluna `guest_cpf` é `VARCHAR(11)` (`Database.java:44`), então criar uma reserva com o CPF escrito como no comprovante (`111.222.333-44`) dá 500, e quase todo o roteiro cai: 20 de 72 conferências passaram. Há também defeitos que não dependem do CPF e que conferi à mão com o CPF só em dígitos:
- a confirmação exige um `amountPaid` no corpo e dá 500 sem ele (`ApiRoutes.java:131`);
- reserva que não existe dá 500 (`ApiRoutes.java:118`);
- o e-mail escreve o dinheiro como `R$ 499.80` (`Formatter.java:25`);
- o relatório de ocupação conta reservas pendentes (`ReportService.java:38`);
- não há nenhuma proteção contra duas reservas simultâneas do mesmo quarto: reproduzi a reserva dupla em 8 de 10 rodadas.

## Arquitetura

**Números:** 8 pacotes, 15 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). `com.pousada.api` usa Javalin, JDBC e Jackson. `com.pousada.service` usa JDBC. A maior classe é `ApiRoutes` (298 linhas).
**Nota:** com problemas

As direções estão certas (api → service → database → model, sem ciclo) e as regras de preço em `service.pricing` não conhecem nem Javalin nem JDBC. Mas as camadas vazam em pontos localizados:
- **O SQL mora no serviço de relatórios.** `ReportService.java:36-39` e `ReportService.java:84-89` montam e executam as consultas. A conexão que eles usam é aberta pela rota (`ApiRoutes.java:216` e `ApiRoutes.java:235`) e passada como parâmetro, e por isso o pacote `api` depende de JDBC.
- **A borda HTTP tem regra e fala direto com os repositórios.**
  - `getReservation` e `getDocument` vão direto ao `ReservationRepository` (`ApiRoutes.java:117`, `ApiRoutes.java:172`).
  - A regra de que a segunda via só existe para reserva paga fica no handler (`ApiRoutes.java:175-180`), não no serviço.
  - O check-out busca o quarto no repositório (`ApiRoutes.java:162`).
- **A montagem não fica no `main`.** As peças são criadas no construtor de `ApiRoutes` (`ApiRoutes.java:37-44`), não em `Application.java`. É aceitável num sistema deste tamanho, mas faz a borda HTTP conhecer o `Database`.
- **Serviços estáticos.** O serviço chama `PricingStrategyFactory.create` (estático, `ReservationService.java:58`) e `PriceCalculator` (estático). Isso não pesa muito.

## Design de baixo nível

**Números:** 5 planos, 5 implementações de `PricingStrategy` (`StandardPricingStrategy`, `NonRefundablePricingStrategy`, `CorporatePricingStrategy`, `WeekendPackagePricingStrategy`, `LongStayPricingStrategy`). Preço e devolução estão nas 5. A validação de 3 planos fica fora delas, num `switch` sobre o plano no serviço (`ReservationService.java:128-147`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Aproxima do Strategy:
- os cinco planos, inclusive os dois do ponto de partida, têm o seu preço numa classe própria (`calculatePrice`) e a sua devolução (`getRefundPolicy`, `RefundPolicy.java:8-29`);
- a seleção fica num único `switch` que só escolhe a implementação (`PricingStrategyFactory.java:7-13`), o que a referência aceita;
- o cancelamento pede a devolução à estratégia sem nomear o plano (`ReservationService.java:118-120`).

O que afasta é a regra de quem pode usar cada plano: ela ficou no serviço, num `switch (ratePlan)` com um `case` por plano (`ReservationService.java:128-147`):
- o código da empresa: `case CORPORATE`, que chama o estático `CorporatePricingStrategy.isValidCompanyCode`;
- sexta a domingo: `case WEEKEND_PACKAGE`;
- 7 noites: `case LONG_STAY`.

A referência classifica esse caso como malfeito: o próximo plano ("melhor idade", "60 dias de antecedência") mexe na estratégia, na fábrica e nesse `switch`. As estratégias até repetem parte da validação, por conta própria, com `IllegalArgumentException` (`WeekendPackagePricingStrategy.java:18-20`, `LongStayPricingStrategy.java:14-16`). Esse código nunca é alcançado, e se fosse daria 500, porque `ApiRoutes.java:283-288` só trata `ApiException`.

**O exagero.** Não há estrutura sem necessidade fora da parte do pattern. A fábrica é o ponto único de seleção, e `PricingResult` e `RefundPolicy` servem ao pattern.

**O resto do código.**
- **Duplicação.** O laço das noites com a temporada e o arredondamento está copiado em quatro estratégias (`StandardPricingStrategy.java:11-20`, `NonRefundablePricingStrategy.java:13-22`, `CorporatePricingStrategy.java:24-33`, `LongStayPricingStrategy.java:18-27`). Uma mudança na regra da temporada precisa ser feita em quatro lugares.
- **Formatação errada do dinheiro.** `Formatter.java:25` usa `String.format("R$ %,.2f").replace(",", ".")`. Com o locale padrão, 499,80 sai como `R$ 499.80`. Com o separador de milhar, sairia `R$ 1.234.56`. O texto pede `R$ 1.234,56`.
- **Os erros.**
  - Todos os handlers têm `catch (Exception)`, e `handleException` transforma tudo o que não é `ApiException` em 500, sem log (`ApiRoutes.java:283-288`). A causa some, e foi por isso que o log do serviço não mostrou nada sobre os 500.
  - Duas rotas lançam `IllegalArgumentException("RESERVATION_NOT_FOUND")` em vez da `ApiException` que já existe (`ApiRoutes.java:118`, `ApiRoutes.java:173`).
- **A conversão da entrada.** O corpo é lido como `Map` cru, com casts `(Number)` e `LocalDate.parse` sem tratamento (`ApiRoutes.java:75-101`). Data fora do formato, plano desconhecido ou corpo que não é JSON viram 500.
- **Código morto.** `model/Receipt.java` inteiro, `Reservation.withStatus`/`withAmountPaid`/`withRefund` (`Reservation.java:117-133`), `Formatter.formatMonthIso` (`Formatter.java:20`) e `PriceCalculator.isWeekend` (`PriceCalculator.java:44`) não são usados.
- **O que está certo.** Os tipos são adequados: `BigDecimal` com `HALF_EVEN` (`PriceCalculator.java:8,30`), `LocalDate` e `enum`s para status e plano. Os objetos de modelo são imutáveis.

## Testes

**Números:** 11 testes rodaram, 0 falhas, 0 erros, 0 pulados. 11 métodos escritos, em 2 arquivos. Cobertura de linhas 84/795 (10,6%) e de ramos 20/153 (13,1%). `service`, `api` e `database` têm 0%. Não há nenhum teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

**O que está testado, com valores:**
- os três exemplos da parte nova: Tecnosul 779,80/93,58/686,22 (`PricingStrategyTest.java:16-25`), o pacote deluxe em 820,00 (`PricingStrategyTest.java:28-35`) e a estadia longa 1.749,30/174,93/1.574,37 (`PricingStrategyTest.java:38-47`);
- a devolução de 410,00 do pacote (`PricingStrategyTest.java:50-54`);
- as bordas da alta temporada (`PriceCalculatorTest.java:9-39`);
- a validação de sexta a domingo (`PriceCalculatorTest.java:48-53`).

**O que fica sem rede:**
- Agrovale (0,18) e Farmavida (0,25): trocar a taxa não derruba nenhum teste.
- O código de empresa inválido.
- O mínimo de 7 noites: trocar `nights < 7` por `nights < 6` em `ReservationService.java:142` não derruba nenhum teste.
- A devolução integral do convênio e da estadia longa.
- O normal e o não reembolsável: não há teste do exemplo de R$ 599,76 nem do desconto de 15%.
- A máquina de estados da reserva, o cancelamento, os códigos de erro, o comprovante e o e-mail, e os dois relatórios.

Todos os defeitos achados no comportamento passariam por esses testes: o CPF, a moeda no e-mail, a ocupação com pendentes e o 500 na reserva inexistente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas, todas ok). Datas em `date`. 1 chave estrangeira, `reservations.room_number → rooms`, sem índice. 0 CHECK, 0 UNIQUE/EXCLUDE. O esquema é criado por `CREATE TABLE` no código, sem migrações. 0 transações explícitas, 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

**O que está certo.** Os tipos estão certos, o SQL usa parâmetros em todo lugar, as conexões são devolvidas por try-with-resources e há pool (Hikari, `Database.java:13-21`).

**Os defeitos:**
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` em `Database.java:29-70` não acrescenta uma coluna a uma tabela que já existe.
- **A coluna do CPF.** `guest_cpf VARCHAR(11)` (`Database.java:44`) só aceita o CPF sem pontuação. O CPF com máscara estoura a coluna e vira 500, e essa é a causa da maior parte das falhas do comportamento.
- **Nada impede duas reservas do mesmo quarto na mesma noite.** A verificação (`ReservationRepository.java:107-125`) e o `INSERT` (`ReservationRepository.java:22-58`) usam conexões diferentes, sem transação, sem trava e sem restrição no banco. O roteiro não conseguiu medir isso (todas as criações deram 500). Medi à mão, com o CPF só em dígitos: em 8 de 10 rodadas de dois pedidos simultâneos para a suíte nas mesmas noites, os dois deram 201 e ficaram duas reservas no banco.
- **Escritas que deviam andar juntas estão separadas.** Confirmar faz dois `UPDATE` em conexões separadas (`ReservationService.java:75-76`), e cancelar também (`ReservationService.java:122-123`). Uma falha entre eles deixa a reserva `CONFIRMED` com `amount_paid` 0, ou `CANCELLED` com `refund` null.
- **Faltam um índice e as restrições.** A chave estrangeira não tem índice, e não há CHECK em `status`/`rate_plan`.

## Comportamento

**Números:** 20 de 72 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 13 de 46 |
| depois de reiniciar | 1 de 3 |
| o cenário | 5 de 21 |
| as reservas simultâneas | 1 de 2 |

**Nota:** ruim

**A causa principal.** Toda criação de reserva do roteiro deu 500, porque o roteiro manda o CPF como `111.222.333-44` e a coluna só aceita 11 caracteres (`Database.java:44`). O texto não fixa o formato do CPF na entrada, mas um 500 para o CPF escrito do jeito que o próprio texto o mostra não é uma escolha razoável: o normal seria normalizar ou recusar com 400. Daí caem em cascata a confirmação, o e-mail, o check-in e o check-out, os cancelamentos, os relatórios, os dados depois de reiniciar e quase todo o cenário. Passaram só as recusas que acontecem antes do `INSERT`: capacidade, quarto inexistente, datas e hóspedes inválidos, convênio sem código ou com código inválido, pacote fora de sexta a domingo e estadia longa com 6 noites. Essas recusas também passariam num sistema que não implementasse os planos.

**Repetindo à mão com o CPF só em dígitos,** os cálculos do cenário estão certos: R$ 820,00 no pacote, devolução de R$ 410,00, e os três planos vêm dos exemplos testados. Mas há falhas que não dependem do CPF:
- **Confirmar sem corpo dá 500** (`ApiRoutes.java:131`). E o que a confirmação grava como pago é o valor que o cliente manda: uma reserva de R$ 499,80 ficou "paga" com R$ 1,00. O texto diz que o pago é o total, então o caixa e o relatório financeiro podem sair errados.
- **Reserva que não existe dá 500,** não 404 (`ApiRoutes.java:118`). Corpo que não é JSON e mês fora do formato também dão 500, não 400.
- **O e-mail e o comprovante escrevem o dinheiro como `R$ 499.80`,** não `R$ 499,80` (`Formatter.java:25`).
- **O relatório de ocupação conta reservas pendentes** (`ReportService.java:38` filtra só `!= CANCELLED`). Uma reserva não paga apareceu como quarto ocupado (`total,2/5`), e o texto pede só as pagas.
- **O financeiro lista com 0,00 a reserva STANDARD paga e cancelada com devolução integral** (`ReportService.java:88`). O total fica certo, mas o texto pede só as reservas que deixaram dinheiro.
- **Duas reservas simultâneas do mesmo quarto podem ser criadas** (8 de 10 rodadas, na minha medição; ver Banco).

**Para o dono da pousada:** como o site está, nenhuma reserva com CPF formatado entra. Mesmo com o CPF só em dígitos, a confirmação depende de um campo que o site não sabe que precisa mandar, o hóspede recebe o e-mail com o valor escrito errado, o relatório de ocupação conta quem não pagou, e o mesmo quarto pode ser vendido duas vezes.

## Dúvidas

- **O CPF na entrada.** O texto não diz se o CPF vem com máscara. Se o site mandasse só dígitos, parte das falhas do roteiro não aconteceria. A nota `ruim` se sustenta mesmo assim, pelas falhas que medi à mão sem a máscara: confirmação sem corpo, 404 virando 500, a moeda no e-mail, a ocupação com pendentes e a reserva dupla.
- **A nota dos testes** fica entre `com problemas` e `ruim`. Os 11 testes conferem com valores os exemplos da parte nova, mas a cobertura é de 10,6%, e serviço, API e banco não têm nenhum teste.
- **A nota do banco** fica entre `com problemas` e `ruim`. A rubrica não lista "nenhuma proteção contra reserva dupla" entre os casos de `ruim`, mas a reserva dupla foi reproduzida, e confirmação e cancelamento fazem duas escritas sem transação. Ficou `com problemas` porque o esquema, os tipos e o SQL estão certos.
- **Dois servidores na medição da corrida.** Durante a minha medição da corrida, um serviço de uma tentativa anterior ainda estava de pé na mesma porta, ligado ao banco de mesmo nome. As reservas duplicadas foram gravadas no banco medido, mas não sei qual dos dois processos atendeu os pedidos. Os dois são o mesmo jar, então o resultado vale para o projeto.
