# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes em `br.com.marealta`: `api` (as rotas do Javalin em `RestApi` e a conversão de/para JSON
em `ApiMapper`), `service` (as regras: `ReservationService`, `PricingService`, `MessageService`, `DocumentService`),
`report` (os quatro relatórios CSV, sob uma classe abstrata `CsvReport`), `repository` (JDBC puro com HikariCP, uma
interface e uma implementação por tabela, e o esquema criado em `Database`) e `domain` (entidades e enums). Uma
requisição entra em `RestApi`, o `ApiMapper` lê o corpo, o `ReservationService` aplica a regra e chama o repositório,
e os dados ficam em três tabelas do PostgreSQL (`rooms`, `reservations`, `messages`), criadas com `CREATE TABLE IF NOT
EXISTS` no código. O pattern pedido (Template Method para os relatórios) está bem aplicado. Os problemas mais sérios:
nada impede que o mesmo quarto seja reservado duas vezes ao mesmo tempo (o roteiro conseguiu a reserva dupla em 9 de
10 rodadas), os testes cobrem só o cálculo de preço (3,7% das linhas), e o dinheiro sai em alguns pontos fora do
formato pedido (`0` em vez de `0.00` no JSON e nos CSV, espaço não separável no `R$` do e-mail).

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo. Javalin só em `api` e no `main`, JDBC só em
`repository` (e o `main`, que monta o pool); `service`, `report` e `domain` não usam nem um nem outro. Maior classe:
`ReservationRepositoryImpl`, 231 linhas.
**Nota:** bom

As dependências vão da borda para as regras e das regras para os dados, sem ciclo. A montagem é toda feita num lugar
só, em `Application.java:15-40`, com injeção pelo construtor; nenhuma classe cria as próprias dependências. A borda
HTTP é fina: cada handler lê o parâmetro, chama o serviço e escreve a resposta (`api/RestApi.java:94-146`), e a
tradução das exceções para os códigos de erro fica concentrada em `api/RestApi.java:58-80`. O SQL está todo em
`repository/*Impl.java`; o serviço não conhece `Context` nem SQL (`service/ReservationService.java`).

Pontos menores, que não mudam a nota: a borda pula o serviço para os quartos (`api/RestApi.java:86`) e monta ela
mesma os relatórios passando o repositório (`api/RestApi.java:163`, `173`, `183`, `193`), de modo que as classes de
`report` consultam o banco diretamente, sem nenhum serviço entre elas e o repositório. Os quatro handlers de
relatório repetem a mesma leitura e validação do parâmetro (`api/RestApi.java:157-195`). Um `id` que não é número cai
em `Long.parseLong` sem tratamento (`api/RestApi.java:113`, `119`, `125`, `131`, `137`, `143`) e vira 500 em vez de
404/400. Nenhuma classe domina o sistema.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto `CsvReport.generate()` (`final`); 0 relatórios fora dele; a
linha de título existe só na classe base (`report/CsvReport.java:19-21`). 3 interfaces de repositório, cada uma com
uma implementação só e nenhum substituto (nem nos testes).
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** `report/CsvReport.java:8-17` é o método-modelo, `final`: título, cabeçalho, linhas e total, nessa
ordem. Os passos que mudam são abstratos (`getReportName`, `getPeriod`, `buildHeaderLine`, `buildDataLines`,
`buildTotalLine`, `report/CsvReport.java:23-31`), e os quatro relatórios, inclusive os dois da parte comum
(`OccupancyReport`, `RevenueReport`), são subclasses que só preenchem esses passos. A linha de rodapé com a data que o
dono anuncia para o ano que vem entra num lugar só (`generate()`), e um relatório novo é uma subclasse nova. É o
Template Method como a referência descreve: **acerto**.

O que afasta um pouco: o passo "escolher as reservas" não está no esqueleto, e por isso cada subclasse consulta o
banco duas vezes, uma para as linhas e outra para o total (`report/RevenueReport.java:40` e `62`,
`report/RatePlanReport.java:42` e `69`, `report/OccupancyReport.java:43` e `62`), o que pode até dar linhas e total
inconsistentes se uma reserva mudar entre as duas consultas. A formatação do dinheiro, que é comum aos relatórios,
foi copiada em três subclasses como `amount.toString()` (`report/RevenueReport.java:71`, `report/RatePlanReport.java:84`,
`report/TourismTaxReport.java:86`), e é justamente ela que imprime `0` em vez de `0.00` quando a soma parte de
`BigDecimal.ZERO` (`report/TourismTaxReport.java:66`, `report/RatePlanReport.java:55`): o formato devia estar na base.

**O exagero.** `RoomRepository`, `ReservationRepository` e `MessageRepository` são interfaces com uma implementação
cada, que nada substitui (o único teste não usa nenhuma). `DocumentService` (`service/DocumentService.java:8-22`) só
repassa campos para um construtor. E `ReservationRepository.findByStatusIn` (`repository/ReservationRepository.java:15`)
não é usado por ninguém.

**O resto do código.**
- Duplicação de regra: o que é "reserva paga" está em `Reservation.isPaid()` (`domain/Reservation.java:122`), mas é
  reescrito à mão em `report/TourismTaxReport.java:73-75` e no SQL de `findPaidByDate`; o filtro "deixou dinheiro"
  (`getNetAmount() > 0`) aparece quatro vezes (`report/RevenueReport.java:45`, `65`, `report/RatePlanReport.java:45`,
  `77`).
- Dinheiro: `BigDecimal` com `HALF_EVEN` está certo no preço (`service/PricingService.java`), mas o zero sem escala
  vaza para a API (`service/ReservationService.java:71`, `143`), e o desconto é calculado de forma tortuosa,
  `0.85 - 1` em valor absoluto (`service/PricingService.java:33`), em vez de `0.15`. O e-mail usa
  `NumberFormat.getCurrencyInstance(pt-BR)` (`service/MessageService.java:21`), que escreve `R$` seguido de espaço não
  separável (U+00A0) e não do espaço do texto.
- Erros: `catch (Exception e)` em `api/ApiMapper.java:111` engole a causa e até a mensagem das próprias
  `InvalidRequestException` lançadas no bloco; `asInt()` transforma `"abc"` em 0 sem reclamar
  (`api/ApiMapper.java:86`, `92`); há duas `InvalidRequestException`, e a de `service/ReservationService.java:168`
  não está mapeada na `RestApi` (se fosse lançada daria 500); `Optional.get()` sem checagem em
  `service/ReservationService.java:94`, `125`, `158`.
- Responsabilidades: o texto do comprovante é montado no `MessageService` (`service/MessageService.java:42`), não
  junto do comprovante. A taxa de turismo, que a prefeitura muda todo ano, é uma constante compilada
  (`report/TourismTaxReport.java:14`).

São defeitos localizados, mas espalhados por várias classes; daí `com problemas`.

## Testes

**Números:** 10 testes rodaram, 0 falharam, 0 pulados; 10 métodos de teste escritos, todos em
`PricingServiceTest`. Cobertura: linhas 31 de 849 (3,7%), ramos 14 de 134 (10,4%). Pacotes `report`, `api`,
`repository` e o `main` com 0%; `service` com 28 de 150 linhas.
**Nota:** ruim

Os testes que existem são bons no que fazem: conferem o preço com valores, inclusive o exemplo do texto (duas noites
de julho, R$ 599,76, `PricingServiceTest.java:56-64`), a alta temporada em julho, dezembro e janeiro
(`PricingServiceTest.java:26-53`) e o desconto de 15% com arredondamento (`PricingServiceTest.java:78-85`). Mas é só
isso. Não há nenhum teste das situações da reserva, do cancelamento e da devolução, das recusas e dos códigos de erro,
do comprovante e do e-mail, da persistência, nem da API. E nada testa a parte do cenário: os quatro relatórios, a
linha de título e a taxa de turismo têm 0 de 137 linhas cobertas. Na pergunta da mutação: trocar `4.50` por `5.40`
(`report/TourismTaxReport.java:14`), trocar a ordem de `RatePlan.values()` ou apagar a linha de título em
`report/CsvReport.java:10` não derrubaria nenhum teste. Também ficaram sem teste os limites da temporada (14/12 e o
fim de fevereiro). Para um sistema com essas regras, a coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`
(5 colunas, todas ok); datas em `date`; 0 chaves estrangeiras; 0 CHECK; 0 UNIQUE/EXCLUDE; 0 transações explícitas;
0 `FOR UPDATE`. Esquema criado por `CREATE TABLE` no código, sem migrações. O único SQL montado com `+` só junta
`?` constantes (`repository/ReservationRepositoryImpl.java:145`).
**Nota:** ruim

O que está certo: tipos corretos para dinheiro e datas, `NOT NULL` no que é obrigatório, SQL sempre com parâmetros,
conexões devolvidas com try-with-resources e um pool HikariCP (`repository/Database.java:15-20`), e os dados ficam no
banco depois de reiniciar (o roteiro do comportamento confirmou).

O defeito sério é a regra principal do negócio, um quarto não pode ter duas reservas na mesma noite, não ter
proteção nenhuma: `ReservationService.createReservation` consulta os conflitos (`service/ReservationService.java:45-49`)
e depois insere (`service/ReservationService.java:75`) em conexões separadas, sem transação, sem trava, sem
`synchronized` e sem restrição `EXCLUDE` no banco (`repository/Database.java:41-60`). O roteiro do comportamento criou
as duas reservas simultâneas do mesmo quarto em 9 de 10 rodadas: é dado errado no banco, um quarto vendido duas vezes.

Os outros defeitos: `reservations.room_number` não tem chave estrangeira para `rooms` (`repository/Database.java:44`);
as colunas de situação, plano e tipo não têm CHECK; o esquema é criado com `CREATE TABLE IF NOT EXISTS`
(`repository/Database.java:32-74`), que não sabe evoluir uma tabela que já existe; e a confirmação grava a reserva e a
mensagem em duas escritas sem transação (`service/ReservationService.java:92` e `96`), que podem ficar pela metade.
A taxa, a capacidade e a diária dos quartos ficam num `enum` e não no banco (`domain/RoomType.java`), o que o texto
aceita ("os quartos quase nunca mudam").

## Comportamento

**Números:** 59 de 64 conferências passaram. Parte comum: 44 de 46. Depois de reiniciar: 3 de 3. Cenário 09: 11 de 13.
Reservas simultâneas: 1 de 2 (em 9 de 10 rodadas as duas reservas foram criadas).
**Nota:** com problemas

O que falhou e o que quer dizer para o dono:
- **Reserva dupla.** Dois pedidos ao mesmo tempo para o mesmo quarto e as mesmas noites resultaram em duas reservas em
  9 de 10 rodadas. Com o site recebendo pedidos simultâneos, a pousada pode vender o mesmo quarto duas vezes. (A causa
  está na coluna do banco.)
- **Relatórios com `0` em vez de `0.00`.** A taxa de turismo de um mês sem reservas termina em `total,0`, e o
  relatório de planos mostra `NON_REFUNDABLE,0,0` quando o plano não tem reserva no mês. O texto pede o valor escrito
  como no financeiro (`27.00`). Quem importar o CSV numa planilha talvez nem perceba, mas o formato não é o combinado.
  Causa: `report/TourismTaxReport.java:66`, `report/RatePlanReport.java:55` e o `toString()` nos `formatAmount`.
- **`amountPaid` como `0` no JSON** da reserva recém-criada (o texto pede número com 2 casas), por
  `service/ReservationService.java:71`; depois de lida do banco ela volta como `0.00`.
- **O e-mail não traz `Total: R$ 599,76` exatamente:** o `R$` é seguido de um espaço não separável (U+00A0), porque o
  texto do comprovante usa o formatador de moeda do Java (`service/MessageService.java:21`). Para o hóspede, na tela,
  é igual; para quem compara o texto (ou procura por ele), não é o que o dono escreveu.

O resto funciona: preços com temporada e arredondamento, recusas com os códigos certos, confirmação, check-in,
check-out, cancelamentos e devoluções, os dois relatórios da parte comum, os dados depois de reiniciar e, na parte do
cenário, a linha de título nos quatro relatórios, a taxa de turismo e o relatório por plano com valores.

## Dúvidas

- O exagero: as três interfaces de repositório com uma implementação só são um costume comum e baratas; contei como
  exagero porque nada as substitui (nem os testes), junto com o `DocumentService` que só repassa e o
  `findByStatusIn` sem uso. Um revisor mais tolerante daria `contenção`.
- O banco entre `com problemas` e `ruim`: a skill põe a duplicação protegida "só dentro do processo" em
  `com problemas`; aqui não há proteção nenhuma, nem dentro do processo, e o roteiro mostrou o dado errado (9 de 10
  rodadas), por isso `ruim`. A mesma falha aparece no comportamento, onde pesou como `com problemas`.
