# Revisão: construção, cenário 09

## Visão geral

O projeto é um serviço Javalin em camadas. Os pacotes são `controller` (as rotas), `service` (as regras), `report` (os
quatro CSV), `repository` (JDBC com HikariCP), `domain`, `dto` e `exception`, e tudo é montado no `main`
(`Application.java:29-71`). Uma requisição passa pelo handler (`ReservationController.java:25-49`), que lê o corpo e
valida os campos, depois pelo `ReservationService`, que aplica as regras e calcula o preço com o `PricingService`, e
termina no `ReservationRepository`, que grava em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código
(`Database.java:36-73`). Os relatórios seguem um Template Method limpo (`CsvReport.java:9-16`), e o cenário passou
13 de 13 conferências. Há três problemas sérios:
- nada impede duas reservas do mesmo quarto na mesma noite: sem restrição, sem transação e sem trava. No teste de
  pedidos simultâneos, 9 das 10 rodadas criaram as duas reservas;
- os testes que de fato rodam são só 12 testes de unidade do preço, com 4,4% de cobertura de linhas. O teste de
  integração nunca roda pelo nome que tem;
- há defeitos na borda: um corpo que não é JSON, ou um plano de tarifa inválido, devolve 500, e o dinheiro do
  comprovante sai com espaço não separável.

## Arquitetura

**Números:** 8 pacotes, 17 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só
em `controller` e na raiz, o JDBC só em `repository` (e na raiz, que monta tudo). A maior classe é
`ReservationRepository`, com 267 linhas.
**Nota:** bom

- **A borda HTTP é fina.** Os handlers leem parâmetros, validam o formato e chamam o serviço
  (`ReservationController.java:57-86`, `ReportController.java:22-72`). Nenhum handler tem SQL ou regra de preço ou de
  situação da reserva.
- **As regras não conhecem Javalin nem JDBC.** Em `ReservationService.java:31-150` não há `Context` nem SQL: só
  repositórios e exceções de domínio. Os relatórios recebem as reservas já escolhidas e não falam com o banco
  (`ReportService.java:24-55`).
- **O acesso ao banco fica isolado** nos três repositórios, com um pool (`Database.java:15-20`).
- **A montagem fica num lugar só** (`Application.java:29-46`), com injeção pelo construtor e sem `static` mutável.
- **As dependências apontam para baixo:** controller → service → repository → domain, sem ciclo.

Ressalvas pequenas, que não mudam a nota:
- `RoomController` e `MessageController` vão direto ao repositório (`RoomController.java:18`,
  `MessageController.java:18`). Para leituras triviais, isso é aceitável.
- `RoomRepository` faz a carga inicial dos quartos no próprio construtor (`RoomRepository.java:20-44`), e `Database`
  cria as tabelas no seu construtor (`Database.java:21`). São efeitos colaterais na construção, mas ficam num lugar
  previsível.

## Design de baixo nível

**Números:** 4 de 4 relatórios estão na estrutura, nenhum fora dela. A linha de título só existe na classe base
(`CsvReport.java:18-20`). O método-modelo é `final` (`CsvReport.java:9`), e há 5 passos abstratos.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `CsvReport.generate()` é `final` e fixa a sequência título → cabeçalho → linhas → total
(`CsvReport.java:9-16`). Cada relatório só preenche o nome, o período, o cabeçalho, as linhas e o total:
`OccupancyReport.java:28-61`, `RevenueReport.java:23-65`, `TourismTaxReport.java:24-63` e `RatePlansReport.java:25-72`.
Isso inclui os dois relatórios da parte comum. Nenhum relatório monta o título à mão, e quem chama não pergunta qual é
o relatório: `ReportService` só faz `new XReport(...).generate()` (`ReportService.java:30,38,46,54`). A linha de data
que o dono quer para o ano que vem entra num lugar só, em `CsvReport.generate`. A seleção e a ordem das reservas
ficaram nas consultas do repositório (`ReservationRepository.java:179-220`), passadas pelo construtor de cada
relatório. A referência aceita essa divisão, porque o esqueleto do CSV está num lugar só.

**O exagero.** Não há interface com uma implementação só, fábrica, builder nem camada que só repassa. O único excesso é
`DocumentDto`, uma cópia campo a campo do record `Document` (`DocumentDto.java:10-41` contra `Document.java:6-19`).
É leve demais para contar como exagero.

**O resto do código.** Tem defeitos localizados que um revisor pediria para corrigir:
- **A mesma regra de negócio está copiada quatro vezes.** O "dinheiro que fica com a pousada" (pago menos devolvido)
  é calculado em `RevenueReport.java:42-44`, `RevenueReport.java:59-61`, `RatePlansReport.java:51-53` e
  `RatePlansReport.java:66-68`. O mesmo acontece com `formatDecimal` (`RevenueReport.java:67`,
  `RatePlansReport.java:74`, `TourismTaxReport.java:69`) e com os `DATE_FORMATTER`/`MONTH_FORMATTER` de cada
  relatório. Isso caberia na classe base ou no domínio (`Reservation`).
- **O formato do dinheiro no comprovante está errado.** `NumberFormat.getCurrencyInstance(new Locale("pt","BR"))`
  (`DocumentService.java:14`) gera `R$` + U+00A0 (espaço não separável) + valor, e não o "R$ 779,80" do texto. Conferi
  pela API: os bytes `302 240` aparecem no corpo do e-mail. O construtor `new Locale` também está obsoleto no Java 21.
- **Os erros de JSON não são tratados.** O handler só trata `ApiException` e `BadRequestResponse`
  (`Application.java:73-81`). A exceção do Jackson, num corpo inválido ou num `ratePlan` desconhecido, cai no
  `catch` genérico e vira 500 (`Application.java:83-87`).
- **`guests` é um `int` primitivo no pedido** (`CreateReservationRequest.java:12`). Por isso, a falta do campo vira 0
  e só por acaso cai na regra "menos de um hóspede".
- **Os zeros saem sem escala:** `BigDecimal.ZERO` em `ReservationService.java:68` e `PricingService.java:38`. Assim a
  reserva recém-criada sai com `"amountPaid":0` e `"discount":0`, e não `0.00`.
- **`Reservation` é um saco de setters** (`Reservation.java:50-176`). As transições de situação ficam espalhadas no
  serviço com `setStatus` (`ReservationService.java:81,99,110,127`), sem que o domínio proteja nada.
- **No check-out, o handler descarta o retorno e lê a reserva de novo** (`ReservationController.java:71-72`).

Os tipos do domínio estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`PricingService.java:64-66`),
`LocalDate`, e `enum` para situação, plano, tipo e canal.

## Testes

**Números:** rodaram 12 testes, com 0 falhas, 0 erros e 0 pulados, todos de `PricingServiceTest`. Há 25 métodos de
teste escritos. A cobertura é de 39 de 892 linhas (4,4%) e de 16 de 168 ramos (9,5%). Os pacotes `report`,
`repository`, `controller` e `dto` têm 0% (`measures/tests.md`).
**Nota:** ruim

- **O que roda confere bem o preço, com valores.** São testados a alta temporada em julho, dezembro, janeiro e
  fevereiro, o limite de 14/12 (`PricingServiceTest.java:46-49`), o exemplo de R$ 599,76 (`:62-69`), o desconto
  NON_REFUNDABLE de R$ 116,97 (`:72-76`) e o total (`:86-91`).
- **Todo o resto fica sem rede**: a parte nova do cenário (os quatro relatórios, a linha de título, a taxa de turismo
  e o relatório por plano), as transições de situação, o cancelamento e a devolução, a recusa por conflito e o
  e-mail.
- **`IntegrationTestManual.java` tem 13 testes pela API, mas nunca roda.** O nome não segue o padrão do Surefire
  (`*Test`) nem o do Failsafe (`*IT`), embora o Failsafe esteja configurado (`pom.xml:96-107`). O log só mostra
  `PricingServiceTest`. Mesmo que rodasse, depende de Docker e Testcontainers (`IntegrationTestManual.java:37-41`), e
  não do banco de `DB_URL`. As asserções dele sobre os relatórios usam `contains`
  (`IntegrationTestManual.java:214-262`), sem conferir a ordem nem o total.
- **A pergunta da mutação.** Se alguém trocasse `4.50` por `5.40` (`TourismTaxReport.java:14`), apagasse a linha de
  título (`CsvReport.java:11`) ou deixasse de descontar a devolução (`RevenueReport.java:42-44`), nenhum teste que
  roda falharia.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As
conferências acusaram uma chave estrangeira sem índice, `reservations(room_number) → rooms`, nenhum CHECK em situação,
plano, tipo ou canal, nenhuma restrição UNIQUE ou EXCLUDE, 0 transações explícitas e 0 `FOR UPDATE`
(`measures/database.md`).
**Nota:** ruim

- **Nada impede a reserva dupla, o defeito sério.** O conflito é conferido com um `SELECT COUNT(*)`
  (`ReservationRepository.java:125-159`), e o `INSERT` vem depois, em outra conexão (`ReservationService.java:45` e
  `:71`). Entre os dois não há transação, trava, restrição `EXCLUDE` nem sequer um `synchronized`. No roteiro, 9 das
  10 rodadas de pedidos simultâneos criaram as duas reservas para o mesmo quarto e as mesmas noites
  (`measures/behavior.md`): é dado errado no banco, a pousada vendendo a mesma noite duas vezes.
- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` no código (`Database.java:36-73`), sem
  migrações. O próximo pedido que precisar de uma coluna nova (cancelamentos, hóspedes por estado) não tem como
  alterar uma tabela que já existe.
- **Duas escritas que deviam andar juntas estão separadas.** A confirmação grava a reserva e depois insere o e-mail, em
  conexões diferentes e sem transação (`ReservationService.java:83-87`). Se a segunda falha, fica uma reserva
  CONFIRMED sem mensagem.
- **O que está certo:** o SQL é todo parametrizado com `?` (o script não achou SQL montado com `+`), as conexões são
  devolvidas por try-with-resources, há um pool HikariCP (`Database.java:15-20`) e há chave estrangeira para o quarto
  (`Database.java:61`).

## Comportamento

**Números:** 58 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 41 de 46 |
| reinício | 3 de 3 |
| cenário 09 | 13 de 13 |
| simultâneas | 1 de 2 |

**Nota:** com problemas

O cenário funciona inteiro. Os quatro relatórios saem com a linha de título "Pousada Maré Alta,<nome>,<período>", a
taxa de turismo e o relatório por plano trazem os valores e a ordem certos, o plano sem reserva aparece com 0 e o mês
inválido dá 400. O preço, a confirmação, o check-in e o check-out, os cancelamentos com devolução, os dois relatórios
originais e os dados depois de reiniciar também passam.

As falhas, e o que querem dizer:
- **Corpo que não é JSON dá 500 INTERNAL_ERROR, e não 400.** Conferi à mão que um `ratePlan` desconhecido também dá
  500. É erro do agente (`Application.java:83-87`): o site recebe "erro interno" onde devia receber "pedido inválido".
- **O e-mail não traz "Total: R$ 599,76".** O valor sai com espaço não separável entre "R$" e o número
  (`DocumentService.java:14`). Para o hóspede, que só lê o e-mail, a diferença é invisível. Mas o texto não é o que o
  dono escreveu, e qualquer sistema que procure ou compare o valor falha.
- **`amountPaid` sai como `0`, e não `0.00`, na reserva recém-criada.** O valor está certo, mas sem as 2 casas que o
  texto pede para o JSON.
- **"o 201 comporta 3" e "a diária do 201 é 389.90" falharam** só porque o projeto devolve `number` como texto
  (`"201"`). Conferi pela API que `capacity` 3 e `nightlyRate` 389.90 estão lá. O texto não diz se o número do quarto
  é texto ou número, então essa escolha é razoável e não pesa.
- **As reservas simultâneas:** em 9 de 10 rodadas o quarto foi reservado duas vezes. O defeito pesa na coluna do
  banco, onde está a causa.

## Dúvidas

- **A nota do banco.** A skill põe em `com problemas` a reserva dupla protegida só dentro do processo. Aqui não há
  proteção nenhuma, e a duplicação aconteceu em 9 de 10 rodadas, por isso dei `ruim` (risco de dado errado). Um
  revisor que leia só a lista de `ruim` da skill, que não cita esse caso, poderia dar `com problemas`.
- **Testes.** Os 12 testes que rodam conferem bem o preço. Dei `ruim` porque a cobertura é de 4,4%, nada da parte nova
  é testado e o teste de integração nunca roda. A fronteira com `com problemas` é discutível.
