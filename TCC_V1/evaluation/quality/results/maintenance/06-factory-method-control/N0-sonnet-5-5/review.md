# Revisão: manutenção, cenário 06

## Visão geral

O projeto (que veio da base) tem sete pacotes: `hotel.web` (as rotas do Javalin, os registros do JSON de entrada e de
saída), `hotel.reservations` (o serviço com as regras, o cálculo de preço e o `ReservationStore`, que fala JDBC),
`hotel.rooms`, `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports` e `hotel` (a montagem). Uma
requisição `POST /reservations` passa por `HttpApi.readNewReservation` (`src/main/java/hotel/web/HttpApi.java:109`),
vira um `NewReservation`, é conferida em `ReservationService.create`
(`src/main/java/hotel/reservations/ReservationService.java:27`) e é gravada por `ReservationStore.add` no PostgreSQL,
com o esquema criado por migrações do Flyway. O agente fez o pedido do jeito pequeno: um campo `children` no pedido
HTTP, no `NewReservation`, na `Reservation`, na resposta e no comprovante; a soma `guests + children` na conferência
da capacidade; a recusa de número negativo; e uma migração nova (`V2__add_children.sql`) com `NOT NULL DEFAULT 0` e
`CHECK (children >= 0)`. Não criou nenhuma estrutura para o "cobrar das crianças maiores no futuro". As 61
conferências do comportamento passaram, os 27 testes passam. Não há nada sério. O que falta são dois testes: um para o
comprovante sem crianças e outro mostrando que o preço não muda com crianças.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos
da base (`measures/dependencies-base.md`). Só o número de linhas mudou: `hotel.reservations` passou de 508 para 527,
`hotel.web` de 236 para 240 e `hotel.documents` de 79 para 84.
**Nota:** bom

A mudança respeita as camadas que já existiam. A borda HTTP só lê o campo opcional e aplica o padrão 0
(`src/main/java/hotel/web/HttpApi.java:130`). As regras novas (o número negativo e a capacidade com as crianças) ficam
no serviço, que não conhece Javalin nem SQL (`src/main/java/hotel/reservations/ReservationService.java:34-44`). O SQL
novo fica só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:19,32,43,132`). O
comprovante lê o campo da `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:39`). O ciclo entre
`documents` e `reservations` veio da base: `ReservationService` importa `ReservationReceipt`
(`src/main/java/hotel/reservations/ReservationService.java:3`), e o comprovante importa `Reservation`
(`src/main/java/hotel/documents/ReservationReceipt.java:3`). O agente não o piorou e não acrescentou dependência nova.

## Design de baixo nível

**Números:** 11 arquivos mudados, mais 1 migração nova; nenhuma classe, interface, fábrica ou *builder* novo. A
regra das crianças está em 2 condições no serviço e em 1 `if` no comprovante.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É a resposta que `controls.md` descreve para o cenário 06. O agente acrescentou um campo no pedido e na reserva
(`src/main/java/hotel/reservations/NewReservation.java:6`, `src/main/java/hotel/reservations/Reservation.java:14,71`,
`src/main/java/hotel/web/CreateReservationRequest.java:15`, `src/main/java/hotel/web/ReservationResponse.java:18`) e
somou as crianças na conferência da capacidade
(`src/main/java/hotel/reservations/ReservationService.java:40`). Pôs a linha no comprovante logo depois da linha do
quarto, só quando há criança (`src/main/java/hotel/documents/ReservationReceipt.java:47-49`), e criou a coluna nova no
banco. A reserva continua criada direto pelo construtor. Não há política de preço de criança: o
`PriceCalculator.calculate` continua sem receber as crianças
(`src/main/java/hotel/reservations/ReservationService.java:49`).

A única peça a mais é um segundo construtor em `NewReservation`, que assume zero crianças
(`src/main/java/hotel/reservations/NewReservation.java:9-13`). Ele não é usado no código de produção, só pelos testes
da base (`src/test/java/hotel/reservations/ReservationServiceTest.java:35`, `src/test/java/hotel/reports/ReportsTest.java:32`),
para que eles compilassem sem mudança. É uma sobrecarga de construtor, não uma fábrica nem um *builder*, por isso não
conta como exagero. Há dois detalhes pequenos. Primeiro, `children` entrou como o último componente do
`ReservationReceipt` (`src/main/java/hotel/documents/ReservationReceipt.java:23`), embora no texto saia logo depois do
quarto; a ordem do texto está certa, só a declaração ficou fora de ordem. Segundo, a mensagem de capacidade ainda chama
o total de "guests" (`src/main/java/hotel/reservations/ReservationService.java:43`), mas acrescenta "children
included". O resto do código (o `catch (Exception)` de `HttpApi.java:113`, os `synchronized` do serviço) veio da base,
sem mudança.

## Testes

**Números:** 27 testes rodaram, 0 falhas, 0 erros e 0 pulados; 27 métodos escritos, contra 26 na base, todos mantidos.
Cobertura de linhas: 94,2% (485 de 515), contra 93,8% na base. Cobertura de ramos: 88,5% (108 de 122), contra 87,1% na
base. `hotel.documents` tem 6 de 6 ramos cobertos.
**Nota:** com problemas

O agente escreveu um único teste pela API, longo, para a parte nova
(`src/test/java/hotel/web/HttpApiTest.java:108-127`). Ele confere com valores que:

- sem o campo, a reserva vem com `children` 0 (`:111`);
- 2 adultos e 1 criança cabem no 202, que comporta 3 (`:113-118`); por isso trocar `>` por `>=` na capacidade seria
  pego;
- 2 adultos e 1 criança no 101, que comporta 2, dão 422 (`:123-124`); por isso apagar o `+ children` seria pego;
- `children: -1` dá 400 (`:125-126`);
- o e-mail de confirmação traz "Crianças: 1" (`:121`); como a confirmação relê a reserva do banco, isso também mostra
  que a coluna é gravada e lida.

Algumas regras do pedido ficaram sem rede:

- "sem criança, o comprovante fica como está": nenhum teste compara o texto inteiro do comprovante, e trocar
  `children > 0` por `children >= 0` (`ReservationReceipt.java:47`) não derrubaria nenhum teste;
- a posição da linha, logo depois do quarto: o teste só usa `contains` (`HttpApiTest.java:121`);
- "criança não paga": nenhum teste confere o total de uma reserva com crianças;
- a consulta (`GET`) mostrando as crianças.

Não há teste da regra nova no nível do serviço (`ReservationServiceTest` não mudou). Nenhum teste da base foi apagado
ou enfraquecido.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway: a `V1__create_tables.sql` está igual à da base, e a
`V2__add_children.sql` é nova. Nenhuma conferência acusou chave primária faltando, dinheiro fora de `numeric(10,2)`,
chave estrangeira sem índice ou data em texto. Há uma restrição nova, `CHECK (children >= 0)`. As colunas
`status`, `rate_plan`, `type` e `channel` estão sem CHECK, como já estavam na base.
**Nota:** bom

O esquema evoluiu do jeito certo. O agente não editou a migração da base e criou uma nova:
`ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0)`
(`src/main/resources/db/migration/V2__add_children.sql:2`). O `DEFAULT 0` dá zero crianças às reservas que já
existiam, que é o que o pedido diz quando a reserva não informa nada. O `CHECK` repete no banco a recusa do número
negativo. O SQL novo usa parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:31-33,43`), e a leitura
pega a coluna (`:132`). A reserva duplicada continua barrada pela restrição `EXCLUDE` da base. A falta de CHECK nas
colunas de situação veio da base.

## Comportamento

**Números:** 61 de 61 conferências passaram:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- as crianças na reserva: 7 de 7;
- o comprovante: 2 de 2;
- as crianças depois de reiniciar: 1 de 1;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer:

- o site pode mandar `children`, ou deixar de mandar, e aí são zero crianças;
- a consulta mostra as crianças;
- adultos e crianças juntos não passam da capacidade (o 101 recusa 2 adultos e 1 criança);
- criança não muda o preço;
- número negativo é recusado;
- o comprovante ganha "Crianças: 2" logo depois do quarto e fica igual quando não há criança.

Tudo o que já funcionava continua funcionando, inclusive depois de reiniciar o serviço e com duas reservas
simultâneas.

## Dúvidas

Nenhuma.
