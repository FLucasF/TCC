# Revisão: manutenção, cenário 09

## Visão geral

O sistema é o da base: `hotel.web.HttpApi` recebe a requisição (Javalin), chama `hotel.reservations` (serviço, preço e
`ReservationStore` com JDBC sobre PostgreSQL, esquema pelo Flyway) e os relatórios em `hotel.reports` leem o
`ReservationStore` e devolvem CSV. A montagem é feita em `Hotel.java`. O agente acrescentou duas classes,
`TourismTaxReport` e `RatePlansReport`, duas rotas em `HttpApi.java:99-106` e a ligação em `Hotel.java:29-35`, e pôs a
linha de título em cada um dos quatro relatórios. Não criou nenhum esqueleto comum: os quatro relatórios continuam
escritos inteiros, cada um com a sua cópia do título, que é justamente o que o dono pediu para evitar ("quando o jeito
dos relatórios mudar, quero que mude em todos de uma vez"). O mais sério é um erro de dinheiro: a taxa de turismo
escolhe as reservas por "ficou dinheiro" (`amountKept() > 0`) e não pela situação paga. Por isso cobra a taxa de uma
reserva não reembolsável que foi cancelada (`TourismTaxReport.java:28`). O roteiro acusa isso, com R$ 72,00 no lugar
de R$ 45,00.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Um ciclo (`hotel.documents` ↔
`hotel.reservations`), que já existia na base. `hotel.reports` passou de 2 para 4 classes e de 95 para 215 linhas.
`HttpApi` passou de 170 para 186 linhas.
**Nota:** bom

As peças novas foram para o lugar certo. As duas classes de relatório ficam em `hotel.reports`, dependem só de
`hotel.reservations` (`TourismTaxReport.java:3-4`, `RatePlansReport.java:3-5`) e não conhecem Javalin nem JDBC. As rotas
novas são finas: leem o mês, chamam `generate` e escrevem o CSV (`HttpApi.java:99-106`). As peças são criadas e
ligadas num lugar só (`Hotel.java:29-35`). O agente não trouxe nenhuma dependência nova nem nenhum ciclo novo. O ciclo
`documents` ↔ `reservations` vem da base. O único custo é que o construtor de `HttpApi` cresce um parâmetro por
relatório (`HttpApi.java:37-44`). Isso fica tolerável com quatro relatórios, mas é mais um sintoma da falta de uma
abstração comum, que a seção de design trata.

## Design de baixo nível

**Números:** dos 4 relatórios, nenhum passa por um esqueleto comum: 0 na estrutura e 4 fora. A linha de título está
copiada 4 vezes (`OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:36`,
`RatePlansReport.java:50`). O laço que escolhe as reservas está copiado 3 vezes.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido é o Template Method, ou um escritor genérico de relatório. A referência chama de omissão o caso em
que "os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título", e foi isso que aconteceu. Não
há classe abstrata, nem escritor comum, nem função que receba as partes que variam. Cada `generate` repete a mesma
sequência: escolhe as reservas, ordena, escreve título, cabeçalho, linhas e total. O texto avisava que viriam mais
relatórios (cancelamentos, hóspedes por estado) e que todos vão ganhar uma linha de rodapé com a data. Com esse
código, essa linha terá de ser escrita em quatro (depois seis) lugares. Também não há exagero: o agente não criou
nenhuma interface nem camada sem uso.

O design do resto do código novo tem estes defeitos:
- **Seleção copiada.** A escolha das reservas em `RatePlansReport.java:25-32` e `TourismTaxReport.java:26-33` é a mesma
  de `RevenueReport.java:24-31`. Na taxa, essa cópia escolheu o critério errado: o texto pede as reservas pagas
  (confirmadas, com check-in ou com check-out), e já existe `Reservation.isPaid()` para isso
  (`Reservation.java:91-95`, usado em `OccupancyReport.java:27`).
- **Nome da pousada solto.** O nome está escrito à mão quatro vezes, embora a base já tivesse a constante
  `ReservationService.HOTEL_NAME` (`ReservationService.java:12`).
- **Planos fixos no código.** `RatePlansReport.java:35-58` trata os planos com uma variável e um `if/else if` para cada
  um, em vez de percorrer `RatePlan.values()`. Um terceiro plano ficaria de fora do relatório sem nenhum aviso.
- **Noites recalculadas.** `TourismTaxReport.java:40` calcula as noites à mão (`toEpochDay`), quando a reserva já tem
  `price().nights()`, que `ReservationReceipt.java:34` usa.
- **Taxa numa constante.** O valor da taxa é uma constante no código (`TourismTaxReport.java:17`), mas o texto diz
  que ele muda todo ano. É aceitável, mas obriga a recompilar a cada ano.

O dinheiro está em `BigDecimal` com 2 casas, e o código não tem estado mutável nem erro engolido.

## Testes

**Números:** 30 testes rodaram, nenhum falhou (a base tinha 26, todos continuam passando). São 30 métodos escritos,
4 deles novos, todos em `ReportsTest`. A cobertura é de 94,1% das linhas e 87,1% dos ramos (a base tinha 93,8% e
87,1%). Em `hotel.reports`, são 113 de 116 linhas e 43 de 46 ramos.
**Nota:** com problemas

As partes boas: o agente ajustou os testes da base para a linha de título, comparando o CSV inteiro
(`ReportsTest.java:61-66`, `87-93`), sem apagar nem enfraquecer nenhum. Os relatórios novos também são conferidos com
o CSV inteiro, inclusive no mês vazio (`ReportsTest.java:123-177`).

O problema é que os dados de teste não exercitam as regras novas:
- **Hóspedes.** Todas as reservas do cenário de teste têm 1 hóspede (`ReportsTest.java:35`). Se alguém apagasse a
  multiplicação pelos hóspedes (`TourismTaxReport.java:42`), nenhum teste falharia.
- **Reserva cancelada.** Nenhum teste da taxa tem uma não reembolsável cancelada no mês, e por isso o erro do filtro
  (`TourismTaxReport.java:28`) passou. O exemplo do texto (2 hóspedes por 3 noites dão R$ 27,00) também não foi
  testado.
- **Plano não reembolsável.** No relatório de planos, `NON_REFUNDABLE` sempre aparece com 0 (`ReportsTest.java:160`,
  `173`). Se alguém trocasse ou apagasse o ramo desse plano (`RatePlansReport.java:44-46`), nenhum teste falharia. Esse
  ramo é justamente o que o texto pede, com a cancelada que continua contando.
- **Testes pela API.** Os testes pela API só conferem que o título aparece em algum lugar (`contains`) e o status 200
  (`HttpApiTest.java:123-129`), sem olhar os valores.
- **Import sem uso.** Sobrou um import que nada usa (`ReportsTest.java:20`, `Nested`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira
está sem índice e nenhuma data está em texto. Há um `EXCLUDE` que impede duas reservas do mesmo quarto nas mesmas
noites. A única migração (`V1__create_tables.sql`) está igual à da base. As colunas de situação e de plano são `text`
sem `CHECK`, o que vem da base.
**Nota:** bom

O pedido não exigia mudar o esquema, e o agente não o mexeu. A migração da base ficou intacta, e o SQL continua com
parâmetros (`ReservationStore.java`, sem mudança). Os dois relatórios novos usam `store.all()`
(`ReservationStore.java:108`): trazem a tabela inteira e filtram o mês em Java (`TourismTaxReport.java:26`,
`RatePlansReport.java:25`). É o mesmo jeito que a base já usava em `RevenueReport` e `OccupancyReport`. Isso pesa à
medida que as reservas crescem, mas segue o padrão da base e não derruba a coluna.

## Comportamento

**Números:** 63 de 64 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09: 12 de 13;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** com problemas

Os quatro relatórios começam com a linha de título certa. A ocupação e o financeiro continuam iguais depois do título.
O relatório de planos tem as duas linhas na ordem pedida, conta a não reembolsável cancelada e mostra o plano sem
reserva com 0.

A falha está na taxa de turismo de julho. O esperado era 2 reservas e total de R$ 45,00. O relatório trouxe também a
reserva 2 (quarto 201, 3 hóspedes, 2 noites, não reembolsável, cancelada) e somou R$ 72,00. A causa é o filtro
`amountKept() > 0` em `TourismTaxReport.java:28`, copiado do financeiro: ele inclui a cancelada que não teve devolução,
mas o texto pede só as pagas (confirmadas, com check-in ou com check-out). Para o dono, isso quer dizer que o relatório
que vai para a prefeitura manda pagar taxa de hóspedes que cancelaram e não vieram: ele pagaria imposto a mais, num
documento oficial, toda vez que houver um cancelamento não reembolsável no mês.

## Dúvidas

Nenhuma sobre as notas. No relatório de planos, o texto diz "contando as mesmas reservas que o relatório financeiro
conta". Por isso o agente usou ali o mesmo critério de `amountKept()`, e o roteiro confirma que é o certo. O erro
ficou só em ter usado esse mesmo critério na taxa de turismo, onde o texto define as reservas pela situação.
