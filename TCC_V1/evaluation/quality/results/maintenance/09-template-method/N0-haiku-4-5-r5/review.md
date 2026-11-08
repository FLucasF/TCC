# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em sete pacotes. A borda HTTP fica em `hotel.web`, as regras e o acesso ao banco em
`hotel.reservations` (`ReservationService`, `ReservationStore` com JDBC), os relatórios em `hotel.reports`, e a
montagem em `hotel.Hotel`. Os dados ficam no PostgreSQL, num esquema criado pelo Flyway. Uma requisição de relatório
entra numa rota de `HttpApi`, que lê o período e chama `generate(...)` do relatório. O relatório lê todas as reservas
de `ReservationStore.all()` e monta o CSV.

O pedido trazia dois relatórios novos (taxa de turismo e planos de tarifa) e uma linha de título nos quatro, avisando
que outros relatórios virão e que o formato vai mudar "em todos de uma vez". O agente criou `TourismTaxReport` e
`RatePlansReport` como cópias inteiras do padrão dos dois que já existiam. Também colou a linha de título à mão em
cada um dos quatro. Não há esqueleto comum: o Template Method que o texto pede não foi feito (omissão).

O defeito mais sério é de cálculo. A taxa de turismo conta as noites com `LocalDate.compareTo`
(`src/main/java/hotel/reports/TourismTaxReport.java:42`), que só dá a diferença de dias quando as duas datas estão no
mesmo mês. Pela API, uma estadia de 30/07 a 02/08 com 2 hóspedes sai com 1 noite e R$ 9,00, quando o certo é 3 noites
e R$ 27,00. O mesmo acontece de 30/12 a 02/01. Os 64 passos do roteiro e os 30 testes passam, mas nenhum deles tem uma
estadia que atravessa o fim do mês.

## Arquitetura

**Números:** 7 pacotes (os mesmos da base); 17 dependências entre eles (o mesmo conjunto da base); 1 ciclo,
`hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). `hotel.reports`
foi de 2 classes e 95 linhas para 4 classes e 216 linhas. `HttpApi` foi de 170 para 186 linhas.
**Nota:** bom

As peças novas seguiram as camadas da base:

- **Os relatórios:** só dependem de `hotel.reservations`. Não usam Javalin nem JDBC (tabela de bibliotecas em
  `measures/dependencies.md`).
- **As rotas novas:** são finas. Leem o mês, chamam `generate` e escrevem o CSV
  (`src/main/java/hotel/web/HttpApi.java:99-106`).
- **A montagem:** continua num lugar só (`src/main/java/hotel/Hotel.java:28-30`).

O único ciclo vem da base: `ReservationService` importa `hotel.documents.ReservationReceipt`
(`src/main/java/hotel/reservations/ReservationService.java:3`). O agente não piorou nada.

Um ponto pequeno: cada relatório novo acrescenta um parâmetro ao construtor de `HttpApi` (já são 7,
`src/main/java/hotel/web/HttpApi.java:37-45`) e uma rota quase igual às outras. Isso cresce linearmente com os
relatórios que o contador ainda vai pedir, mas não chega a ser um defeito de camadas.

## Design de baixo nível

**Números:** 4 relatórios, 0 numa estrutura comum, 4 escritos inteiros. A linha de título
`"Pousada Maré Alta,<nome>,"` aparece copiada 4 vezes:

- `OccupancyReport.java:34`
- `RevenueReport.java:35`
- `TourismTaxReport.java:38`
- `RatePlansReport.java:36`

(todos em `src/main/java/hotel/reports/`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto deixa claro que a variação vai crescer: "o contador já avisou que vai pedir mais" e "quando o
jeito dos relatórios mudar, quero que mude em todos de uma vez". A resposta esperada era um esqueleto comum (título,
cabeçalho, linhas, total), com cada relatório preenchendo só os passos que mudam. O agente não fez classe abstrata,
nem escritor genérico, nem método compartilhado:

- `TourismTaxReport.generate` (`src/main/java/hotel/reports/TourismTaxReport.java:27-57`) repete a sequência inteira:
  selecionar, ordenar, título, cabeçalho, linhas, total.
- `RatePlansReport.generate` (`src/main/java/hotel/reports/RatePlansReport.java:25-58`) faz o mesmo.
- Os dois que já existiam ganharam o título por cópia de uma linha (`OccupancyReport.java:34`, `RevenueReport.java:35`).

A linha de rodapé com a data, prometida para o ano que vem, vai ter de ser colada em quatro (e logo seis) lugares.
Pela referência, é omissão.

**O exagero.** Nenhuma estrutura sem necessidade: nada de interface com uma implementação só, fábrica ou camada que só
repassa. Contenção.

**O resto do código.**

- **Duplicação:** o filtro de `RatePlansReport.java:26-33` (`amountKept().signum() > 0` e mês do check-in) é cópia
  literal de `RevenueReport.java:24-31`. O pedido diz que os dois contam "as mesmas reservas", e a regra ficou em dois
  lugares que podem divergir. A ordenação de `TourismTaxReport.java:35` também é cópia da de `RevenueReport.java:32`.
- **O tipo usado errado:** `int nights = reservation.checkOut().compareTo(reservation.checkIn())`
  (`TourismTaxReport.java:42`) usa uma comparação como se fosse uma diferença. `LocalDate.compareTo` devolve a
  diferença de anos ou de meses quando eles diferem. O domínio já tinha a resposta certa em
  `reservation.price().nights()` (`src/main/java/hotel/reservations/Price.java:5`), e `ChronoUnit.DAYS.between` também
  serviria. É o defeito que quebra a taxa nas estadias que atravessam o fim do mês.
- **A taxa:** R$ 4,50 fica numa constante (`TourismTaxReport.java:18`). É um lugar só, mas o texto avisa que a taxa
  muda todo ano. Teria sido sensato tirá-la do código ou, pelo menos, deixar claro de qual ano é o valor.
- **O resto:** o dinheiro continua em `BigDecimal` com escala 2. Os nomes são claros.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas 94,6% (546/577; base 93,8%) e de ramos 89,1% (123/138; base 87,1%). `hotel.reports` está com
113/113 linhas e 44/44 ramos.
**Nota:** com problemas

**O que o agente fez certo:**

- Atualizou os 5 testes de relatório da base com a linha de título, sem enfraquecer nenhum
  (`src/test/java/hotel/reports/ReportsTest.java:57-120`).
- Escreveu dois testes de unidade que conferem o CSV inteiro com valores (`ReportsTest.java:123` e `:140`).
- Escreveu dois testes pela API (`src/test/java/hotel/web/HttpApiTest.java:128` e `:140`) e acrescentou conferências
  do título aos relatórios antigos (`HttpApiTest.java:116-121`).

**As lacunas** (a pergunta da mutação):

- **As noites:** nenhum teste tem uma estadia que atravessa o fim do mês. Todos usam check-in e check-out no mesmo mês
  (`ReportsTest.java:43-47`, `HttpApiTest.java:23`). Por isso o erro de `TourismTaxReport.java:42` passa com 100% de
  ramos cobertos.
- **Os hóspedes:** o teste de unidade da taxa só tem reservas de 1 hóspede (`ReportsTest.java:33`). Trocar
  `guests()` por 1 só seria pego pelo teste da API, que confere apenas o total (`HttpApiTest.java:137`).
- **O NON_REFUNDABLE:** nos testes ele aparece sempre com 0 reservas (`ReportsTest.java:152-153`; o da API só tem
  STANDARD). Uma mudança que contasse por `isPaid()` em vez de `amountKept()` deixaria de contar a NON_REFUNDABLE
  cancelada que o pedido manda contar ("as mesmas reservas que o relatório financeiro conta"), e nenhum teste falharia.
- **Os comentários errados:** os comentários em `ReportsTest.java:143-148` mostram um raciocínio deixado pela metade
  ("Actually...", "Let me check..."). Eles dizem que a reserva 5 é NON_REFUNDABLE, mas ela é STANDARD
  (`ReportsTest.java:47`).

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas, todas ok). Nenhuma chave
estrangeira sem índice, nenhuma data em texto. EXCLUDE contra duas reservas na mesma noite. Uma migração Flyway
(`V1__create_tables.sql`), igual à da base. As conferências só acusaram `status`, `rate_plan`, `type` e `channel` como
texto sem CHECK, o que vem da base.
**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: nenhuma migração editada ou acrescentada, nenhum SQL novo. O SQL da
base é parametrizado (`src/main/java/hotel/reservations/ReservationStore.java:33,83,98,111`; o que entra pelo `+` é a
constante `COLUMNS`).

Os dois relatórios novos seguem o padrão da base: trazem a tabela inteira (`store.all()`,
`src/main/java/hotel/reports/TourismTaxReport.java:28`, `src/main/java/hotel/reports/RatePlansReport.java:26`) e
filtram o mês em Java. Isso vale uma nota sobre a carga quando as reservas crescerem, mas é o mesmo jeito dos
relatórios que já existiam.

## Comportamento

**Números:** 64 de 64 conferências passaram:

- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 09: 13/13;
- as reservas simultâneas: 2/2.

**Nota:** com problemas

Pelo roteiro, tudo funciona:

- os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>";
- a ocupação e o financeiro continuam iguais depois do título;
- a taxa de turismo dá R$ 27,00 para 2 hóspedes em 3 noites;
- o relatório de planos lista STANDARD e NON_REFUNDABLE (inclusive com 0) e conta a NON_REFUNDABLE cancelada;
- o mês inválido responde 400.

O roteiro, porém, só usa estadias dentro de um mês. Chamei a API à mão, com o `final/target/hotel.jar` num banco
vazio:

| reserva | o que a API mostrou | o que deveria mostrar |
|---|---|---|
| 2 hóspedes, de 2027-07-30 a 2027-08-02 | `GET /reports/tourism-tax?month=2027-07`: `1,201,2027-07-30,2027-08-02,2,1,9.00` | 3 noites, 27.00 |
| 2 hóspedes, de 2027-12-30 a 2028-01-02 | `GET /reports/tourism-tax?month=2027-12`: 1 noite, 9.00 | 3 noites, 27.00 |

Na primeira, a própria resposta de `POST /reservations` traz `"nights":3`.

Para o dono da pousada: o relatório que vai para a prefeitura subdeclara a taxa de toda reserva que entra num mês e
sai no seguinte, o que numa pousada acontece todo fim de mês. O valor errado sai sem aviso. Não é a maior parte do
cenário, mas é um valor errado num documento fiscal.

## Dúvidas

- **A nota do comportamento:** o roteiro passou inteiro, e o erro só aparece em chamadas que fiz fora dele. A regra
  da coluna põe "parte do cenário falha" em `com problemas`. A regra geral, porém, põe "risco de dado errado" em
  `ruim`, e a taxa subdeclarada à prefeitura chega perto disso. Fiquei com `com problemas` porque o erro atinge só as
  estadias que cruzam o fim do mês. Um revisor mais rigoroso poderia dar `ruim`.
- **O valor da taxa:** se uma constante no código basta para "a prefeitura muda o valor da taxa todo ano", a
  evidência não decide. Contei isso como uma observação, não como defeito.
