# Revisão: manutenção, cenário 09

## Visão geral

O sistema da base é dividido por assunto: `hotel.web` (as rotas Javalin em `HttpApi`), `hotel.reservations` (as
regras em `ReservationService`, o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents` e `hotel.reports`. Tudo é montado em `hotel/Hotel.java`. Uma requisição de relatório chega na rota
(`web/HttpApi.java:91-106`), que lê o período e chama `generate(...)` do relatório. O relatório lê todas as reservas
com `ReservationStore.all()` e monta o CSV à mão. Os dados ficam no PostgreSQL, com o esquema criado pelo Flyway. O
agente criou dois relatórios novos, `TourisimTaxReport` e `RatePlanReport`, cada um escrito do começo ao fim. Também
colou a linha de título, com o texto fixo, nos quatro relatórios e ligou as duas rotas novas. Há dois problemas
sérios. O primeiro: o texto pedia um único esqueleto para os relatórios ("quando o jeito dos relatórios mudar, quero
que mude em todos de uma vez") e ele não existe, porque os quatro relatórios repetem os mesmos passos. O segundo: o
relatório da taxa de turismo copiou o filtro do financeiro e por isso cobra a taxa também de reserva NON_REFUNDABLE
cancelada. No roteiro, a taxa de julho deu R$ 72,00 em vez de R$ 45,00.

## Arquitetura

**Números:** 7 pacotes (os mesmos da base), 17 dependências entre pacotes (as mesmas da base), 1 ciclo
(`hotel.documents` ↔ `hotel.reservations`, que já vinha da base). `hotel.reports` passou de 2 classes e 95 linhas
para 4 classes e 208 linhas. `hotel.web` passou de 236 para 252 linhas.
**Nota:** bom

As mudanças respeitam as camadas que a base já tinha. As classes novas ficam em `hotel.reports` e não usam Javalin
nem JDBC: segundo a tabela de bibliotecas, `hotel.reports` não usa nenhuma. Elas leem os dados pelo
`ReservationStore` (`reports/TourisimTaxReport.java:24`, `reports/RatePlanReport.java:22`), como os relatórios da base
já faziam. As rotas novas são finas: só leem o mês e escrevem o CSV (`web/HttpApi.java:99-106`). A montagem continua
num lugar só (`Hotel.java:29-30`). O agente não criou dependência nova entre pacotes nem ciclo novo. O ciclo
documents ↔ reservations já vinha da base. Um ponto menor: o construtor de `HttpApi` recebe um parâmetro a mais para
cada relatório (`web/HttpApi.java:37-44`, agora com 7). Com os dois relatórios que o contador ainda vai pedir, isso
continua crescendo, mas é consequência do design (ver abaixo), não uma mistura de camadas.

## Design de baixo nível

**Números:** 4 relatórios, 0 numa estrutura comum e 4 escritos inteiros. O título "Pousada Maré Alta,<nome>,…" está
colado como texto fixo em 4 lugares. O filtro "ficou dinheiro e começa no mês" foi copiado em 3 classes.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto descreve quatro relatórios com os mesmos passos: título, cabeçalho, linhas e total. Também
avisa que o contador vai pedir mais relatórios e que a próxima mudança de formato (a linha com a data no fim) tem de
valer para todos de uma vez. O esperado era um esqueleto único, um Template Method ou um escritor genérico. O agente
não criou nada parecido: não há classe abstrata, interface ou função comum.

- Cada relatório monta o próprio CSV, do título ao total: `reports/OccupancyReport.java:33-45`,
  `reports/RevenueReport.java:34-48`, `reports/TourisimTaxReport.java:33-53` e `reports/RatePlanReport.java:48-54`.
- A linha de título está escrita à mão em cada um: `OccupancyReport.java:34`, `RevenueReport.java:35`,
  `TourisimTaxReport.java:34` e `RatePlanReport.java:49`.

Para acrescentar a linha de data no ano que vem, será preciso mexer nos quatro arquivos, e nos seis quando chegarem
os dois relatórios anunciados. É exatamente o que o dono pediu para evitar. Pela referência do pattern, isso é
**omissão**.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com uma implementação só, nenhuma fábrica, nenhuma
camada que só repassa. Por isso, contenção.

**O resto do código.** O que o agente escreveu tem defeitos localizados:

- **A regra copiada.** O laço de seleção do financeiro (`RevenueReport.java:24-31`) foi copiado em
  `TourisimTaxReport.java:23-30` e `RatePlanReport.java:21-28`. A cópia levou a um erro: a taxa de turismo usa
  `amountKept() > 0` (`TourisimTaxReport.java:25`). Esse filtro inclui a NON_REFUNDABLE cancelada. O pedido falava
  das reservas pagas, ou seja, confirmadas, com check-in ou com check-out. A base já tinha essa regra pronta em
  `Reservation.isPaid()` (`reservations/Reservation.java:91-95`), e o agente não a usou.
- **Os planos repetidos à mão.** `RatePlanReport` tem um contador e uma soma para cada plano
  (`RatePlanReport.java:30-43`) e uma linha de CSV escrita para cada um (`RatePlanReport.java:51-52`). Ele não percorre
  `RatePlan.values()`. Um plano novo hoje some do relatório sem nenhum aviso: o `else if` sem `else` em
  `RatePlanReport.java:39` simplesmente o ignora.
- **O nome errado.** A classe se chama `TourisimTaxReport` (`reports/TourisimTaxReport.java:13`), e o campo
  `tourisimTaxReport` (`web/HttpApi.java:34`) repete o erro de digitação de "tourism".
- **O que está certo.** Os tipos estão corretos: o valor da taxa é `BigDecimal` numa constante
  (`TourisimTaxReport.java:15`) e o arredondamento para 2 casas é explícito (`TourisimTaxReport.java:39-42`).

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 32 métodos escritos, contra 26 na base. A
cobertura é de 94,6% das linhas (547/578) e 88,6% dos ramos (124/140); na base era 93,8% e 87,1%. Em `hotel.reports`,
a cobertura é de 114/114 linhas e 45/46 ramos.
**Nota:** com problemas

**O que está certo:**

- Os testes da base continuam passando.
- O agente acrescentou a linha de título nas asserções dos testes de ocupação e de financeiro, que comparam o CSV
  inteiro (`reports/ReportsTest.java:63`, `:89`, `:106` e `:118`).
- O agente escreveu 6 testes novos que comparam o CSV inteiro dos relatórios novos (`reports/ReportsTest.java:124-203`),
  inclusive o mês vazio e o plano sem reserva, que aparece com zero.

**O que falta:**

- **A NON_REFUNDABLE cancelada na taxa.** Nenhum teste da taxa usa uma reserva desse tipo. A cancelada do `setUp` é
  STANDARD com devolução (`ReportsTest.java:49-55`). Por isso o defeito da taxa passa sem ser visto.
- **Hóspedes e noites.** Todas as reservas dos testes da taxa têm 1 hóspede (`ReportsTest.java:37`). O exemplo do
  pedido (2 hóspedes por 3 noites dão R$ 27,00) não está testado.
- **A pergunta da mutação.** Se alguém trocasse o fator `guests()` por `1`, ou multiplicasse as noites duas vezes no
  lugar dos hóspedes (`TourisimTaxReport.java:40-41`), nenhum teste falharia.
- **O plano por relatório.** O relatório por plano também não tem caso com a NON_REFUNDABLE cancelada, que precisa
  contar.
- **Os testes pela API são fracos.** Para os relatórios novos, eles só conferem o status 200 e que o título aparece
  em algum ponto do corpo (`web/HttpApiTest.java:123-129`, com `contains`). Não conferem os valores nem que o título é
  a primeira linha.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`. Nenhuma chave estrangeira sem
índice e nenhuma data guardada como texto. Há uma restrição EXCLUDE contra duas reservas do mesmo quarto na mesma
noite. A única migração, `V1__create_tables.sql`, está igual à da base. As colunas de situação e de plano não têm
CHECK; isso vem da base.
**Nota:** bom

O agente não mexeu no banco, e o pedido não precisava de mudança no esquema. A migração da base ficou intacta.
Os relatórios novos não escrevem SQL: reaproveitam `ReservationStore.all()` (`reservations/ReservationStore.java:108`),
que traz a tabela inteira ordenada para ser filtrada em Java. Isso já acontecia nos relatórios da base. O agente só
acrescentou mais dois lugares que fazem a mesma leitura completa por chamada. Fica como nota de carga para quando as
reservas crescerem, sem pesar na coluna.

## Comportamento

**Números:** 63 de 64 conferências passaram:

- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 09: 12/13;
- as duas reservas ao mesmo tempo: 2/2.

**Nota:** com problemas

Estas partes funcionam:

- os quatro relatórios começam com a linha de título no formato pedido;
- a ocupação e o financeiro continuam iguais depois do título;
- o relatório por plano está certo, inclusive a NON_REFUNDABLE cancelada e o plano sem reserva;
- o mês vazio e o mês fora do formato respondem como deviam.

A única falha é a soma da taxa de turismo. Em julho, o roteiro tem uma reserva NON_REFUNDABLE confirmada e depois
cancelada (reserva 2, 3 hóspedes, 2 noites). O relatório a incluiu com R$ 27,00 e deu total de R$ 72,00, quando o
certo era R$ 45,00. O pedido define as reservas da taxa como "as confirmadas, as que já fizeram check-in e as que já
fizeram check-out", e uma reserva cancelada não está entre elas. Para o dono da pousada, isso quer dizer que o
relatório levado à prefeitura cobra taxa de hóspedes que não vieram. A pousada pagaria imposto a mais em todo mês que
tiver um cancelamento NON_REFUNDABLE. O erro está em `reports/TourisimTaxReport.java:25`.

## Dúvidas

A taxa de uma reserva NON_REFUNDABLE cancelada poderia ser uma escolha razoável num ponto em aberto? Não parece. O
pedido diz quais reservas entram, pelo status, e o roteiro concorda. Também não fica claro se a falta do esqueleto
comum foi deliberada: o agente não deixou rastro disso no código, e o README não foi atualizado com as rotas novas.
Isso não muda a classe, que é omissão.
