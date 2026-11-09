# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em 7 pacotes. `hotel.web` (rotas e DTOs JSON) chama `hotel.reservations`, onde ficam a regra
(`ReservationService`, `PriceCalculator`) e o acesso ao banco (`ReservationStore`, JDBC com pool), e `hotel.documents`
monta o comprovante. Os dados ficam no PostgreSQL, com o esquema criado por migrações do Flyway. O pedido era pôr
crianças na reserva. O agente fez a mudança pequena, no lugar onde cada coisa já mora: o campo opcional `children` no
pedido HTTP (`web/CreateReservationRequest.java:15`), com zero quando falta (`web/HttpApi.java:127`); o campo no
`NewReservation`, na `Reservation` e na resposta; a recusa de número negativo e a soma na conferência da capacidade
(`reservations/ReservationService.java:34-45`); a linha no comprovante (`documents/ReservationReceipt.java:47-49`); e
uma migração nova, `V2__add_children_column.sql`, com `NOT NULL DEFAULT 0 CHECK (children >= 0)`. O roteiro passou
inteiro (61 de 61). O defeito mais sério é que o agente não escreveu nenhum teste para a parte nova: só acrescentou
`0` nos dois ajudantes de teste que já existiam. O README também ficou sem o campo `children` no exemplo do corpo
(`README.md:62-72`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies-base.md`). Há um ciclo,
`hotel.documents` ↔ `hotel.reservations`, que já existia na base. Nenhum pacote nem classe novos; as classes que
mudaram cresceram de 1 a 6 linhas (por exemplo, `ReservationService` passou de 138 para 142 linhas).
**Nota:** bom

O agente pôs cada parte na camada certa. A borda HTTP só lê o campo e aplica o valor padrão
(`web/HttpApi.java:127-130`). As regras (o número negativo e a capacidade) ficam no serviço, sem Javalin nem SQL
(`reservations/ReservationService.java:34-45`). O SQL ficou no `ReservationStore`: a coluna em `COLUMNS` (`:18-19`), o
`INSERT` (`:31-50`) e a leitura (`:132`). O texto do comprovante ficou no `ReservationReceipt` (`:47-49`). Nenhuma
dependência nova entre pacotes. O ciclo documents↔reservations vem da base (o serviço cria o comprovante em
`ReservationService.java:68,113,122`, e o comprovante lê a `Reservation`), e o agente não o piorou.

## Design de baixo nível

**Números:** controle negativo. São 9 arquivos de produção mudados, mais 1 migração e 2 testes, num total de cerca de 30
linhas novas. Nenhuma classe, interface, fábrica ou *builder* novos; nenhum rastro de "preço de criança".
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O agente não caiu na tentação de "cobrar alguma coisa das crianças maiores no futuro". O `PriceCalculator` não foi
tocado, e não há política de preço de criança nem faixa etária configurável. A reserva continua criada direto, com um
`int children` a mais no record `NewReservation` (`reservations/NewReservation.java:6`) e na `Reservation`
(`reservations/Reservation.java:14,38,70-72`). É exatamente o que `references/controls.md` chama de contenção: um campo
a mais, a soma na capacidade (`ReservationService.java:40-41`), a linha no comprovante e a coluna nova.

No resto do código, as mudanças seguem o estilo da base. Os tipos são os certos (`int` para a contagem, `Integer` no DTO
para que o campo possa faltar, em `web/CreateReservationRequest.java:15`). A recusa usa o mesmo `HotelException` com
`INVALID_REQUEST` (`ReservationService.java:34-36`), e o comentário do DTO foi corrigido para "exceto children"
(`CreateReservationRequest.java:6`). Um ponto pequeno: `NewReservation` agora tem dois `int` vizinhos (`guests`,
`children`) num construtor posicional, fáceis de trocar sem o compilador perceber. A mensagem de capacidade ainda diz
"guests" quando a conta já inclui crianças (`ReservationService.java:44`). Os defeitos que restam vêm da base e o agente
não os piorou: o `catch (Exception)` ao ler o corpo (`web/HttpApi.java:113`) e o `synchronized` em `create`
(`ReservationService.java:27`).

## Testes

**Números:** 26 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 26 métodos de teste, os mesmos 26 da base.
Cobertura de linhas: 93,6% (481/514), contra 93,8% na base. Cobertura de ramos: 85,2% (104/122), contra 87,1% na base.
Em `hotel.documents`, 32 de 33 linhas e 5 de 6 ramos: a linha "Crianças" nunca roda nos testes.
**Nota:** com problemas

O agente manteve os 26 testes da base passando e não apagou nem enfraqueceu nenhum. A única mudança foi passar `0`
crianças nos ajudantes `stay(...)` (`src/test/java/hotel/reservations/ReservationServiceTest.java:36`) e `book(...)`
(`src/test/java/hotel/reports/ReportsTest.java:33`). Não há nenhuma ocorrência de `children` nos testes, então nada da
parte nova é conferido. Pela pergunta da mutação:

- apagar `+ request.children()` em `ReservationService.java:40`: nenhum teste falharia;
- apagar a recusa de número negativo (`ReservationService.java:34-36`): nenhum teste falharia (a migração ainda
  recusaria no banco, mas com erro 500);
- apagar a linha do comprovante (`ReservationReceipt.java:47-49`), ou trocar `> 0` por `>= 0`: nenhum teste falharia;
- deixar de gravar ou de ler `children` no `ReservationStore` (`:43`, `:132`): nenhum teste falharia;
- trocar o padrão `0` em `HttpApi.java:127`: nenhum teste falharia.

As regras antigas continuam cobertas com valores pelos testes da base. A parte que o dono pediu agora fica sem rede.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway: `V1__create_tables.sql` igual à da base, e a `V2__add_children_column.sql`
nova. Nenhuma tabela sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma
data guardada como texto. Há um CHECK novo, `children >= 0`. As colunas `status`, `rate_plan`, `type` e `channel` não
têm CHECK, mas isso vem da base.
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a V1
(`src/main/resources/db/migration/V2__add_children_column.sql:1`). O `NOT NULL DEFAULT 0` faz as reservas que já
existem num banco em uso ficarem com zero crianças, sem perder dado, e o `CHECK (children >= 0)` repete no banco a regra
do serviço. O SQL continua com parâmetros (`ReservationStore.java:31-50`: o `INSERT` passou de 14 para 15 `?`, com os
índices renumerados corretamente), e a leitura pega a coluna pelo nome (`:132`). Os candidatos de "SQL montado com `+`"
só concatenam constantes (`COLUMNS`). Contra reserva duplicada, a restrição `EXCLUDE USING gist` da base continua
valendo.

## Comportamento

**Números:** 61 de 61 conferências passaram:

- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- as crianças na reserva: 7/7;
- o comprovante: 2/2;
- as crianças depois de reiniciar: 1/1;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API, e tudo o que já funcionava continua funcionando:

- sem o campo `children`, a reserva tem zero crianças;
- dois adultos e uma criança cabem num quarto para três, mas não num quarto para dois (422 `CAPACITY_EXCEEDED`);
- criança não muda o preço;
- a consulta mostra as crianças, também depois de reiniciar;
- um número negativo é recusado com 400;
- o comprovante ganha a linha "Crianças: 2" logo depois da do quarto, e fica igual quando não há criança.

## Dúvidas

O texto não diz se `guests` pode ser 0 quando há crianças (só crianças). O agente manteve a regra da base, que exige
pelo menos um adulto (`ReservationService.java:31-33`). Parece a escolha sensata, e o roteiro não a confere.
