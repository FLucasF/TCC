# Revisão: manutenção, cenário 09

## Visão geral

O sistema da base está organizado em pacotes por assunto: `hotel.web` (as rotas Javalin em `HttpApi`),
`hotel.reservations` (as regras em `ReservationService` e o acesso ao banco em `ReservationStore`), `hotel.reports` (os
relatórios CSV), `hotel.documents`, `hotel.messages` e `hotel.rooms`. Tudo é montado em `hotel.Hotel`. Uma requisição
de relatório entra numa rota de `HttpApi`, que lê o mês e chama `generate` do relatório. O relatório lê todas as
reservas com `ReservationStore.all()`, filtra e monta o CSV à mão. Os dados ficam no PostgreSQL, e o esquema é criado
pela migração Flyway `V1__create_tables.sql`. O agente fez a mudança com o menor esforço possível: escreveu duas classes
novas inteiras, `TourismTaxReport` e `RatePlansReport`, copiando o `RevenueReport`, e pôs à mão a linha de título em cada
um dos quatro relatórios. Não criou nenhum esqueleto comum, e o texto pedia exatamente isso ("quando o jeito dos
relatórios mudar, quero que mude em todos de uma vez"). O problema mais sério vem dessa cópia. A taxa de turismo usou o
filtro do financeiro (dinheiro retido maior que zero) e não o filtro "pagas" (confirmada, check-in, check-out) que o
texto pede. Por isso ela cobra taxa de reservas NON_REFUNDABLE canceladas: no roteiro, o total saiu R$ 72,00 em vez de
R$ 45,00. A prefeitura receberia um valor a mais.

## Arquitetura

**Números:** 7 pacotes (os mesmos da base), 17 dependências entre pacotes (iguais às da base) e 1 ciclo,
`hotel.documents` ↔ `hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). `hotel.reports` passou
de 2 classes e 95 linhas para 4 classes e 211 linhas. `HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

O agente pôs as peças novas nas camadas certas. As duas rotas novas (`src/main/java/hotel/web/HttpApi.java:99-106`) só
leem o mês, chamam o relatório e escrevem o CSV, como as que já existiam (`HttpApi.java:91-98`). Os relatórios novos
ficam em `hotel.reports`, sem Javalin e sem JDBC: eles dependem só de `ReservationStore.all()`
(`src/main/java/hotel/reports/TourismTaxReport.java:27`, `RatePlansReport.java:31`). A montagem continua num lugar só,
`src/main/java/hotel/Hotel.java:28-30`. O ciclo `documents`↔`reservations` veio da base, e o agente não criou nenhum
novo. Uma ressalva pequena: o construtor de `HttpApi` cresce em um parâmetro a cada relatório (agora são 7,
`HttpApi.java:37-44`). Com os dois relatórios que o contador já anunciou, vão ser 9. Mas esse é o jeito da base, e o
agente só o seguiu.

## Design de baixo nível

**Números:** dos 4 relatórios, 0 passam por um esqueleto comum. Os 4 estão escritos inteiros, cada um com a sua cópia
da linha de título. O filtro "reservas que começam no mês com dinheiro retido" aparece copiado em 3 lugares, e o literal
"Pousada Maré Alta" em 4 relatórios, além de `ReservationService.HOTEL_NAME`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto descreve quatro relatórios com os mesmos passos e anuncia mais dois. Pede também que uma mudança
de formato (a linha de data no fim, no ano que vem) "mude em todos de uma vez". Isso pede o Template Method: um
esqueleto com título, cabeçalho, linhas e total, ou um escritor genérico que receba as partes que variam. O agente fez
o que a referência chama de omissão:
- a linha de título foi copiada à mão em cada relatório: `src/main/java/hotel/reports/OccupancyReport.java:34`,
  `RevenueReport.java:35`, `TourismTaxReport.java:37` e `RatePlansReport.java:42`;
- cada relatório repete a sequência inteira: `StringBuilder`, título, cabeçalho, laço das linhas e linha `total`;
- não há classe abstrata, interface nem função comum de montagem.

A linha de data do ano que vem vai exigir mexer em quatro arquivos, e depois em seis. Nada se aproxima do pattern.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface, fábrica ou camada nova. A contenção aqui veio junto
com a falta de estrutura.

**O resto do código.** A cópia é o defeito principal, e ela já produziu um erro. O filtro de seleção de
`RevenueReport.java:25-31` foi copiado em `TourismTaxReport.java:27-33` e `RatePlansReport.java:31-39`. Para o
relatório de planos isso está certo, porque o texto pede "as mesmas reservas que o relatório financeiro". Para a taxa de
turismo está errado: o texto pede as pagas, e o modelo já tinha `Reservation.isPaid()`
(`src/main/java/hotel/reservations/Reservation.java:91-95`), que o agente não usou.

Outros pontos menores:
- as noites são recalculadas por `toEpochDay` (`TourismTaxReport.java:41`), mas `Price` já guarda `nights()`
  (`src/main/java/hotel/reservations/Price.java:5`);
- o arredondamento usa o nome qualificado `java.math.RoundingMode` dentro do código (`TourismTaxReport.java:47`);
- o relatório de planos usa dois `HashMap` paralelos, preenchidos à mão para cada plano (`RatePlansReport.java:24-29`).
  Um `EnumMap` com um registro de quantidade e valor seria mais simples;
- o nome da pousada não reaproveita `ReservationService.HOTEL_NAME`
  (`src/main/java/hotel/reservations/ReservationService.java:12`).

O resto é razoável e foi mantido da base: `BigDecimal` com 2 casas para dinheiro, `YearMonth`/`LocalDate` para os
períodos, o `enum RatePlan` percorrido em `values()` para a ordem STANDARD e depois NON_REFUNDABLE. O valor da taxa é
uma constante (`TourismTaxReport.java:17`). O texto avisa que ela muda todo ano, mas uma constante nomeada é aceitável.

## Testes

**Números:** em `final`, 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados, e há 30 métodos de teste. A cobertura é
de 94,6% das linhas (545/576) e 89,1% dos ramos (123/138). `hotel.reports` tem 112/112 linhas e 44/44 ramos. Na base
eram 26 testes, 93,8% das linhas e 87,1% dos ramos. O agente acrescentou 4 testes de unidade e ampliou 1 teste da API.
**Nota:** com problemas

**O que o agente fez certo.** Os 26 testes da base continuam passando. Os testes da ocupação e do financeiro foram
atualizados só com a linha de título, sem enfraquecer nenhuma asserção (`src/test/java/hotel/reports/ReportsTest.java:63`,
`:89`, `:106`, `:118`). Os testes novos comparam o CSV inteiro com valores: a taxa de abril (`ReportsTest.java:124-138`),
a taxa de um mês vazio (`:140-149`), os planos de abril com NON_REFUNDABLE zerado (`:151-165`) e os planos de um mês
vazio (`:167-177`). O `HttpApiTest` confere o título e o status 200 das quatro rotas (`src/test/java/hotel/web/HttpApiTest.java:113-130`).

**As regras sem rede.** A pergunta da mutação mostra três buracos nas regras novas:
- **Hóspedes.** Todas as reservas do fixture têm 1 hóspede (`ReportsTest.java:35`). Apagar o `.multiply(guests)` de
  `TourismTaxReport.java:43` não faria nenhum teste falhar. O próprio exemplo do texto (2 hóspedes, 3 noites, R$ 27,00)
  não está testado.
- **Filtro da taxa.** O fixture tem uma NON_REFUNDABLE cancelada só em maio (`ReportsTest.java:102-111`), e os testes da
  taxa olham abril e junho. Por isso nenhum teste pega o erro real do filtro, que inclui as canceladas não reembolsáveis.
- **Planos.** O relatório de planos nunca é conferido com um NON_REFUNDABLE com valor nem com uma cancelada que retém
  dinheiro.

No teste da API, as duas rotas novas só conferem o status e o título (`HttpApiTest.java:123-129`), sem o corpo nem o
total.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice,
nenhuma data em texto. Há um `EXCLUDE USING gist` contra duas reservas sobrepostas no mesmo quarto. A migração
`V1__create_tables.sql` está igual à da base, e não há migração nova. As colunas `status`, `rate_plan`, `type` e
`channel` estão sem CHECK, como na base.
**Nota:** bom

O pedido não exigia mudar o banco, e o agente não mexeu nele: a migração da base está intacta
(`src/main/resources/db/migration/V1__create_tables.sql`, "igual" em `measures/database.md`), e o SQL continua com
parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:33`, `:83`, `:98`; os `+` só concatenam a constante
`COLUMNS`). Os dois relatórios novos usam `store.all()` (`TourismTaxReport.java:27`, `RatePlansReport.java:31`), que
traz a tabela de reservas inteira para filtrar o mês em Java. Isso copia o que a base já fazia nos relatórios dela, e
pesa pouco numa pousada de cinco quartos. Mas agora são quatro varreduras completas, e o contador já anunciou mais duas.
Os defeitos que restam (status e plano sem CHECK, nenhuma transação explícita) vieram da base.

## Comportamento

**Números:** 63 de 64 conferências passaram:
- a parte comum: 46/46;
- depois de reiniciar: 3/3;
- o cenário 09: 12/13;
- reservas simultâneas: 2/2.
**Nota:** com problemas

Tudo o que já funcionava continua funcionando. A ocupação e o financeiro ganharam a linha de título e, depois dela,
estão iguais aos da base. O relatório de planos sai certo: STANDARD, depois NON_REFUNDABLE (mesmo zerado), o total, e a
NON_REFUNDABLE cancelada contando como no financeiro. A taxa de turismo de um mês vazio e o 400 para um mês mal escrito
também passam.

A falha é na taxa de turismo de julho, com um valor errado:

| | linhas | total |
|---|---|---|
| esperado | reservas 1 (R$ 27,00) e 3 (R$ 18,00) | R$ 45,00 |
| veio | também a reserva 2, quarto 201, 3 hóspedes, 2 noites, R$ 27,00 | R$ 72,00 |

A reserva 2 é uma NON_REFUNDABLE cancelada: a pousada ficou com o dinheiro, mas o hóspede não veio. O texto diz
claramente que a taxa é das reservas pagas, "as confirmadas, as que já fizeram check-in e as que já fizeram check-out".
A causa é o filtro copiado do financeiro em `src/main/java/hotel/reports/TourismTaxReport.java:29-31`, quando o certo
era `reservation.isPaid()`. Para o dono da pousada, o relatório que vai para a prefeitura cobra taxa de hóspedes que
cancelaram e nunca se hospedaram. Neste exemplo, ele recolheria R$ 27,00 a mais (60% a mais) toda vez que houver um
cancelamento não reembolsável no mês. O valor da taxa por hóspede e por noite está certo: as linhas 1 e 3 batem com o
esperado, inclusive o exemplo de R$ 27,00 do texto.

## Dúvidas

- A cobrança de taxa sobre uma NON_REFUNDABLE cancelada foi tratada como erro porque o texto enumera os três estados
  pagos. Alguém poderia ler "pagas" como "a pousada ficou com o dinheiro", mas a enumeração explícita fecha essa
  leitura.
- O README não foi atualizado com as duas rotas novas (`final/README.md:56-57` ainda lista só a ocupação e o
  financeiro). O texto não pedia isso, então não entrou em nenhuma nota.
