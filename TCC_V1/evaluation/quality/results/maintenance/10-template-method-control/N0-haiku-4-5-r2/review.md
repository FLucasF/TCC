# Revisão: manutenção, cenário 10

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, em `HttpApi`), `hotel.reservations` (as regras, em
`ReservationService`, e o acesso ao banco, em `ReservationStore`), `hotel.rooms`, `hotel.reports`, `hotel.documents`,
`hotel.messages` e `hotel` (a montagem e o `Database`, com Flyway e pool). Uma requisição a
`GET /reports/occupancy` passa por `HttpApi.java:83-86`, que lê a data e chama `OccupancyReport.generate`, que lê as
reservas do PostgreSQL por `ReservationStore.all()`. O pedido era pontual: uma coluna `guest` depois de `reservation`
no relatório de ocupação, com a vírgula do nome trocada por espaço. O agente mudou só dois arquivos
(`changes.patch`): três linhas em `src/main/java/hotel/reports/OccupancyReport.java` (o cabeçalho na linha 34, o nome
na linha 37 e a coluna na linha 41) e as duas asserções do relatório em `src/test/java/hotel/reports/ReportsTest.java`.
Não criou estrutura nova para o "telefone do futuro". As 55 conferências do comportamento passam e os 26 testes
passam. O mais sério é pequeno: a única regra nova, a troca da vírgula, não tem teste.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual na
base (`measures/dependencies.md` e `measures/dependencies-base.md` só diferem nas 2 linhas a mais de `hotel.reports`).
**Nota:** bom

A mudança fica no lugar onde o relatório já mora (`final/src/main/java/hotel/reports/OccupancyReport.java:34-43`), sem
dependência nova: `hotel.reports` continua dependendo só de `hotel.reservations` e `hotel.rooms`, e o nome do hóspede
vem de `Reservation.guest()`, que já existia. A borda HTTP continua fina (`HttpApi.java:83-86` lê a data e devolve o
CSV) e o relatório não conhece Javalin nem JDBC (a tabela de bibliotecas não marca nada em `hotel.reports`). O único
ciclo, `hotel.reservations` → `hotel.documents` (`ReservationService.java:3` importa `ReservationReceipt`) e de volta
(`ReservationReceipt` usa `Reservation`), vem da base, e o agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, +3/−1 linhas; nenhuma classe, interface ou método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É o que `references/controls.md` chama de contenção para o cenário 10: uma coluna a mais no `OccupancyReport` e a
troca da vírgula por espaço (`OccupancyReport.java:37`, `reservation.guest().name().replace(',', ' ')`), numa variável
local, ao lado das colunas que já existiam (`OccupancyReport.java:38-43`). A frase sobre o telefone não levou a nenhum
esqueleto de relatório nem a colunas configuráveis, e o `RevenueReport` ficou intocado. O resto do código, que veio
da base, está limpo: dinheiro em `BigDecimal`, datas em `LocalDate`, `enum` para situação e plano
(`ReservationService.java:252-255`), erros com código próprio (`HttpApi.java:94-107`). O nome não pode chegar nulo,
porque `HttpApi.java:118` recusa um nome vazio, e por isso a chamada da linha 37 não quebra. O CSV ainda não escapa
aspas nem quebras de linha num nome, mas o pedido só falava da vírgula.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos escritos (os mesmos 26 da base, nenhum
novo); cobertura de linhas 473/504 (93,8%) e de ramos 101/116 (87,1%), contra 471/502 e 101/116 na base;
`hotel.reports` com 50/50 linhas e 22/22 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente atualizou, com valores, as duas asserções que conferem o CSV inteiro (`ReportsTest.java:60-65` e `72-76`):
agora elas conferem o cabeçalho com `guest` no lugar certo e o nome em cada linha. Nenhum teste foi apagado ou
enfraquecido. Mas todos os hóspedes dos testes se chamam "Bruno Lima" (`ReportsTest.java:31`), um nome sem vírgula.
A pergunta da mutação mostra o buraco: se alguém apagasse o `.replace(',', ' ')` de `OccupancyReport.java:37`, os 26
testes continuariam passando, embora a cobertura dê a linha como coberta. A única regra nova do pedido ficou sem
rede. O teste pela API (`HttpApiTest.java:113-116`) só confere o status, o tipo e o total, como já fazia na base.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` nas 6 colunas; nenhuma chave
estrangeira sem índice; nenhuma data em texto; `EXCLUDE USING gist` contra duas reservas na mesma noite; 4 colunas
de situação ou tipo em `text` sem `CHECK`; a migração `V1__create_tables.sql` está igual à da base
(`measures/database.md`).
**Nota:** bom

O pedido não precisava mexer no banco, porque o nome já é guardado (`guest_name` em
`ReservationStore.java:18`), e o agente não mexeu: nenhuma migração editada nem acrescentada, e o serviço sobe
contra um banco vazio. O que se pode apontar vem da base, e o agente não piorou: `status`, `rate_plan`, `type` e
`channel` não têm `CHECK`, e `ReservationStore.all()` (`ReservationStore.java:108-111`) traz a tabela inteira para o
relatório filtrar em Java (`OccupancyReport.java:25-30`). O SQL usa parâmetros; o que se concatena é a constante
`COLUMNS`.

## Comportamento

**Números:** 55 de 55 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário 10 4/4,
reservas ao mesmo tempo 2/2 (`measures/behavior.md`).
**Nota:** bom

Pela API, o relatório de ocupação traz o cabeçalho `room,type,reservation,guest,check_in,check_out`, e o hóspede
"Silva,Ana" aparece como `301,SUITE,<id>,Silva Ana,2027-03-10,2027-03-12`. O total continua igual e o financeiro
mantém o mesmo cabeçalho. Para o dono da pousada, isso quer dizer que a recepção vê o nome do hóspede, que o CSV não
se desalinha com uma vírgula no nome e que nada do que já funcionava quebrou.

## Dúvidas

Um nome com aspas ou quebra de linha ainda bagunçaria o CSV. Como o pedido só falava da vírgula, não contei isso como
defeito.
