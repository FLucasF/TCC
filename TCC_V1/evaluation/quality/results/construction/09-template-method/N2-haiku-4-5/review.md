# Revisão: construção, cenário 09

## Visão geral

O agente montou um serviço Javalin com sete pacotes: `api` (as rotas, que escrevem o JSON à mão com `org.json`),
`service` (`ReservationService`, com o preço, as situações e o comprovante), `service.report` (uma classe abstrata
`Report` e os quatro relatórios como subclasses), `repo` (três repositórios JDBC com SQL parametrizado), `domain`
(entidades e enums) e `util` (`Database`, `Money`, `DateUtil`). Uma requisição entra numa rota, por exemplo
`ReservationRoutes.java:20`, que lê o JSON, chama o `ReservationService`, e este usa os repositórios. O acesso ao
banco passa por uma única `Connection` estática e global (`util/Database.java:8`). As tabelas são criadas com
`CREATE TABLE IF NOT EXISTS` (`Database.java:19-73`). O Template Method dos relatórios está bem montado. O defeito
mais sério é que a confirmação exige no corpo um campo `amountPaid` que o texto não pede (`ReservationRoutes.java:98-99`).
Por isso a confirmação do jeito que o site chama responde 400, e com ela falham o e-mail, o check-in, o check-out, a
segunda via e todos os relatórios com dados (36 de 64 conferências passaram). Há outros defeitos: o financeiro e o
relatório por plano deixam de fora a NON_REFUNDABLE cancelada; nada impede duas reservas simultâneas no mesmo quarto
(9 de 10 rodadas criaram as duas); e os testes cobrem 2,1% das linhas.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece
em `com.pousada` e em `api`, e o JDBC só em `repo` e em `util`. As maiores classes são `ReservationRoutes` (281
linhas), `ReservationRepository` (214) e `ReservationService` (206).
**Nota:** com problemas

As dependências seguem na direção certa e o serviço não conhece o Javalin nem o SQL (`ReservationService.java:1-15`).
A montagem fica em `Application.java:30-50`. Há, porém, misturas localizadas:

- **Uma regra no handler:** a segunda via só sai para reserva paga, e quem decide isso é a rota, não o serviço
  (`ReservationRoutes.java:205-208`). O mesmo handler escolhe o código do erro.
- **O JSON do comprovante montado na rota, duas vezes:** `ReservationRoutes.java:169-181` e `:215-227`. No check-out,
  o handler ainda chama `buildReceipt` e descarta o resultado (`:167`).
- **Uma dependência criada fora da montagem:** `ReservationRoutes` cria por dentro o seu próprio `RoomRepository`,
  num campo `static` iniciado só quando é usado (`ReservationRoutes.java:17` e `:275-280`), em vez de receber o que
  `Application` já criou.
- **As rotas falam direto com os repositórios:** `ReportRoutes.register` recebe os repositórios
  (`ReportRoutes.java:16`) e os relatórios os consultam. `RoomRoutes` e `MessageRoutes` também vão direto ao
  repositório. Num sistema deste tamanho isso é aceitável.
- **A conexão é estado global:** `Database.getConnection()` é estática (`Database.java:76`), e todos os repositórios
  a buscam ali.

## Design de baixo nível

**Números:** quatro relatórios, os quatro como subclasses de `Report`, e nenhum fora da estrutura. A linha de título
existe num lugar só (`Report.java:32-34`). Os passos que mudam são abstratos: cabeçalho, linhas e total
(`Report.java:36-40`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `Report.generate()` (`Report.java:16-30`) é o método-modelo: título, cabeçalho, linhas e total, nessa
ordem. Ocupação, financeiro, taxa de turismo e planos só preenchem `getColumnHeader`, `getDataRows` e `getTotalLine`
(`OccupancyReport.java:28-65`, `RevenueReport.java:25-57`, `TourismTaxReport.java:26-58`, `RatePlansReport.java:28-74`).
Uma linha de data no fim de todos os relatórios entraria num lugar só, em `generate()`. O que afasta do ideal:

- `generate()` não é `final`.
- Cada subclasse refaz a consulta no `getTotalLine` e recalcula o total por conta própria
  (`RevenueReport.java:47`, `TourismTaxReport.java:48`, `RatePlansReport.java:64`, `OccupancyReport.java:60`). São
  duas idas ao banco por relatório, e as linhas e o total podem divergir.
- `formatMoney` está copiado em três subclasses (`RevenueReport.java:69-71`, `TourismTaxReport.java:76-78`,
  `RatePlansReport.java:80-82`), em vez de morar na base.
- O cálculo do líquido (pago menos devolvido) está copiado quatro vezes (`RevenueReport.java:50`, `:60`,
  `RatePlansReport.java:48`, `:67`).

Nada disso tira a classe de acerto.

**O exagero.** Não há interface com uma implementação só, fábrica nem camada que só repassa. Os enums com um
`displayName` igual ao `name()` (`RatePlan.java:3-15`, `RoomType.java`, `ReservationStatus.java`) são redundância
pequena, não estrutura.

**O resto do código.** Os defeitos são vários, localizados, e alguns afetam o resultado:

- **Exceções engolidas:** nos relatórios, uma falha do banco vira `printStackTrace()` e o relatório sai vazio com
  status 200 (`OccupancyReport.java:51-53`, `RevenueReport.java:36-38`). No `getTotalLine`, vira um "total,0.00"
  falso (`RevenueReport.java:52-54`).
- **`catch (Exception)` que esconde a causa:** no `POST /reservations`, qualquer erro, inclusive do banco, vira 400
  INVALID_REQUEST (`ReservationRoutes.java:65-71`).
- **Duplicação nas rotas:** o bloco que monta o JSON de erro está copiado em todas elas (`ReservationRoutes.java`
  inteiro, `ReportRoutes.java`), e o JSON do comprovante aparece duas vezes.
- **Dinheiro no JSON:** sai por `doubleValue()` (`ReservationRoutes.java:258-263`), o que perde as duas casas
  (`"amountPaid":0`, `"discount":0`).
- **Dinheiro no comprovante:** a formatação não põe o separador de milhar (`ReservationService.java:197-205`) e
  escreve "R$ 1403,64" em vez de "R$ 1.403,64".
- **Dinheiro no CSV:** `String.format("%.2f")` depende da localidade da JVM (`RevenueReport.java:70`). Numa JVM em
  pt-BR, o CSV sairia com vírgula decimal.
- **Valores fixos no código:** `totalRooms = 5` (`OccupancyReport.java:19`) em vez de contar os quartos.
- **"Pago" decidido pelo valor:** `isPaid()` olha `amountPaid > 0` e não a situação (`Reservation.java:126-128`).
  Por isso uma reserva cancelada tem segunda via.
- **Estado `static` mutável:** em `Database.java:8` e em `ReservationRoutes.java:17`.

O uso de `BigDecimal` com HALF_EVEN a cada passo (`Money.java`) e de `LocalDate` está certo.

## Testes

**Números:** 7 testes rodaram, 0 falharam; 7 métodos escritos, num arquivo só. Cobertura: 20 de 951 linhas (2,1%) e
13 de 216 ramos (6,0%). Os pacotes `service`, `service.report`, `repo`, `api` e `domain` têm 0% (`measures/tests.md`).
**Nota:** ruim

Os testes conferem só utilitários: o arredondamento de um número (`ReservationServiceTest.java:19-23`), a alta
temporada (`:26-41`), a contagem de noites (`:44-49`), uma soma e uma porcentagem (`:52-64`) e o formato das datas
(`:67-78`). Apesar do nome, nenhum teste chama o `ReservationService`. Ficaram sem nenhum teste:

- o preço de uma reserva;
- o desconto do NON_REFUNDABLE;
- as situações e as recusas;
- o cancelamento e a devolução;
- o comprovante e o e-mail;
- a API e o banco;
- os quatro relatórios, que são a parte do cenário.

A pergunta da mutação: trocar 4.50 por 5.40 (`TourismTaxReport.java:17`), tirar a linha de título
(`Report.java:19`) ou apagar o plano sem reserva (`RatePlansReport.java:39-42`) não derrubaria nenhum teste. Os
testes também não teriam pegado o defeito que quebra o sistema: a confirmação exigir `amountPaid`.

## Banco

**Números:** 3 tabelas (`reservations`, `rooms`, `sent_messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, as datas em `date`, e a chave estrangeira da reserva para o quarto tem índice. Nenhum CHECK,
UNIQUE nem EXCLUDE. Status, plano, tipo e canal são `varchar` sem CHECK. O esquema vem de `CREATE TABLE` no código,
sem migração. Não há pool, há uma chamada a `DriverManager.getConnection`, nenhuma transação, nenhum `FOR UPDATE`, e
nenhum SQL montado com `+` (`measures/database.md`). No roteiro do comportamento, 9 das 10 rodadas de duas reservas
simultâneas criaram as duas.
**Nota:** ruim

O que está certo: os tipos e as chaves (`Database.java:19-57`), e o SQL sempre com `?` (`ReservationRepository.java`
inteiro). O que pesa:

- **Nada impede a reserva dupla.** A criação consulta os conflitos (`ReservationRepository.java:113-138`) e depois
  insere (`:19-56`), sem transação, sem trava e sem restrição no banco. Pela API, isso deixou duas reservas no mesmo
  quarto nas mesmas noites em 9 de 10 tentativas. É dado errado gravado: o quarto fica vendido duas vezes.
- **Uma conexão só, estática, para o processo inteiro** (`Database.java:8`, `:12`). Não há pool nem reconexão: se o
  PostgreSQL derrubar a conexão, o serviço para de funcionar até ser reiniciado. As requisições concorrentes do Jetty
  também disputam essa mesma conexão.
- **O esquema não evolui:** `CREATE TABLE IF NOT EXISTS` (`Database.java:19`, `:28`) não acrescenta coluna numa
  tabela que já existe.
- **Faltam CHECKs** nas situações, nos planos e nos hóspedes (`Database.java:36-42`). `amount_paid` aceita nulo
  (`:43`).
- **Duas escritas sem transação:** a confirmação grava a situação e depois o e-mail (`ReservationService.java:66-70`).
  Se o e-mail falhar, a reserva fica confirmada sem a mensagem.

## Comportamento

**Números:** 36 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 27 de 46 |
| os dados depois de reiniciar | 1 de 3 |
| o cenário 09 | 7 de 13 |
| as reservas ao mesmo tempo | 1 de 2 |

**Nota:** ruim

**A falha principal.** A confirmação lê `amountPaid` do corpo (`ReservationRoutes.java:98-99`). O texto diz que o
pago é o total da reserva, e o site chama sem corpo, então a resposta é 400. Em consequência:

- nenhuma reserva é confirmada;
- nenhum e-mail sai;
- o check-in e o check-out respondem 409;
- a segunda via responde 404;
- o financeiro, a ocupação, a taxa de turismo e os planos saem só com título, cabeçalho e total zerado;
- depois de reiniciar, a reserva aparece CANCELLED em vez de CHECKED_OUT: o roteiro conseguiu cancelar a reserva
  que nunca saiu de PENDING.

Para o dono da pousada, isso quer dizer que o site não consegue confirmar uma reserva. Mesmo quando recebe o campo,
o sistema grava qualquer valor como pago: testei com R$ 1.000,00 numa reserva de R$ 1.193,09.

**O que funciona quando se manda o campo.** Chamei a API eu mesmo, com `curl`, mandando `amountPaid`:

- o e-mail sai com o texto certo;
- a ocupação mostra `101,STANDARD,1,2027-07-10,2027-07-13` e `total,1/5`;
- a taxa de turismo dá `27.00` para 2 hóspedes em 3 noites.

Ou seja, a estrutura dos relatórios funciona.

**Falhas próprias, que não vêm da confirmação:**

- **O financeiro e o relatório por plano deixam de fora a NON_REFUNDABLE cancelada.** A consulta só pega CONFIRMED,
  CHECKED_IN e CHECKED_OUT (`ReservationRepository.java:150`), contra o texto: "uma reserva não reembolsável
  cancelada continua contando". Confirmei pelo `curl`: o dinheiro que fica com a pousada sai a menos no financeiro e
  nos planos.
- **Zero hóspedes** responde 422 CAPACITY_EXCEEDED em vez de 400 INVALID_REQUEST (`ReservationService.java:35`).
- **A segunda via de uma reserva pendente** responde 404 RESERVATION_NOT_FOUND em vez de 409 OPERATION_NOT_ALLOWED
  (`ReservationRoutes.java:205-208`). Já uma reserva cancelada que tinha pago tem segunda via (`Reservation.java:126-128`).
- **O dinheiro no JSON** sai sem as 2 casas (`"amountPaid":0`).
- **Reservas simultâneas:** 9 de 10 rodadas criaram duas reservas para o mesmo quarto nas mesmas noites. Esse defeito
  foi contado na coluna do banco.

Também está fora do roteiro, e não muda a nota: o comprovante escreve valores acima de mil sem o ponto de milhar
("R$ 1403,64").

## Dúvidas

O texto não diz o que responder quando se pede a segunda via de uma reserva não paga. O roteiro espera 409, e o
agente escolheu 404. É uma escolha discutível, mas pesa pouco perto da confirmação quebrada, que sozinha decide a
nota do comportamento.
