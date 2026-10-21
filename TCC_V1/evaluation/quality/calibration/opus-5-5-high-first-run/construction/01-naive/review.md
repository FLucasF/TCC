# Revisão: construção, cenário 01

## Visão geral

O projeto tem 7 pacotes por assunto: `hotel` (montagem e banco), `web`, `reservations`, `rooms`, `messages`, `documents` e `reports`. Uma requisição chega ao `HttpApi` (Javalin), que transforma o JSON em `NewReservation` e chama o `ReservationService`. O serviço valida a reserva, pede o preço ao `PriceCalculator` e grava pelo `ReservationStore`, que usa JDBC com `PreparedStatement` sobre um pool Hikari. Os dados ficam no PostgreSQL, criados por migrações do Flyway (`V1`, `V2`), e uma restrição `EXCLUDE` impede reservar o mesmo quarto duas vezes na mesma noite. O sistema faz tudo o que o texto pede: 72 de 72 conferências passaram, e os 26 testes do agente também. O mais sério é o design da parte que o texto pedia que crescesse. Os cinco planos de tarifa continuam sendo um `enum` sem comportamento, e cada regra de plano fica num `switch` ou num `if` em três lugares diferentes (`PriceCalculator.java:32-46`, `ReservationService.java:48-63` e `ReservationService.java:101-105`). Além disso, nenhum teste confere os três planos novos.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Javalin só em `hotel` e `hotel.web`; JDBC em `hotel`, `hotel.reservations`, `hotel.rooms` e `hotel.messages`. A maior classe é `HttpApi`, com 170 linhas.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota só lê a requisição, chama o serviço e escreve a resposta (`web/HttpApi.java:62-90`). Os erros viram status num único ponto (`web/HttpApi.java:94-107`). Nenhuma regra de preço ou de situação da reserva está no handler.
- **As regras não conhecem o Javalin nem o SQL.** O `ReservationService` não recebe `Context` e não monta SQL. O acesso ao banco está isolado em `ReservationStore`, `Rooms` e `SentMessages`, e as conexões vêm de um `DataSource` injetado.
- **A montagem fica num lugar só.** Tudo é criado e ligado em `Hotel.java:20-28`, e o `main` só lê o ambiente (`Hotel.java:41-45`).
- **Há um ciclo pequeno entre dois pacotes vizinhos.**
  - `reservations` usa `documents`: `ReservationService` monta o comprovante (`reservations/ReservationService.java:3`, `:83`, `:129`, `:138`).
  - `documents` usa `reservations`: o construtor de `ReservationReceipt` recebe um `Reservation` (`documents/ReservationReceipt.java:3`, `:24`).

  O ciclo se desfaria se o comprovante fosse montado fora do serviço, ou se `documents` recebesse só os dados de que precisa.
- **Há texto de apresentação dentro da regra.** O corpo do e-mail de confirmação é montado no próprio serviço (`reservations/ReservationService.java:145-151`). Isso ajuda a explicar por que o serviço depende de `documents`.

Fora o ciclo, as camadas estão bem separadas para um sistema deste tamanho.

## Design de baixo nível

**Números:** dos 5 planos, nenhum tem comportamento próprio: `RatePlan` é um `enum` só de nomes (`reservations/RatePlan.java:3-14`). As regras dos planos estão em 3 lugares fora dele:
- 1 `switch` de desconto com os 5 casos, mais 1 `if` com um `switch` por tipo de quarto para o pacote (`PriceCalculator.java:32-46`);
- 3 `if (ratePlan == …)` de validação no serviço (`ReservationService.java:48-63`);
- 1 `switch` de devolução com os 5 casos (`ReservationService.java:101-105`).

**Classe:** omissão
**Exagero:** contenção
**Nota:** bom

- **O pattern pedido.** O texto pedia que cada plano tivesse num lugar só o seu preço, quem pode usá-lo e a sua devolução, e avisava que "o comercial vive inventando plano". O agente fez o contrário:
  - **Preço:** um `switch (ratePlan)` decide o desconto dos cinco planos (`reservations/PriceCalculator.java:40-46`). Antes dele, um `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` traz o preço fechado com outro `switch` por tipo de quarto (`:32-38`).
  - **Quem pode usar:** a validação do convênio, do pacote e da estadia longa são três `if` sobre o plano dentro de `ReservationService.create` (`reservations/ReservationService.java:48-63`).
  - **Devolução:** é outro `switch` por plano em `cancel` (`reservations/ReservationService.java:101-105`).
  - **Consequência:** o próximo plano (o da melhor idade, ou o de 60 dias de antecedência) obriga a mexer no `enum`, nos dois `switch` e no serviço. O serviço ainda lê o mapa de descontos do calculador (`PriceCalculator.COMPANY_DISCOUNTS`, usado em `ReservationService.java:50`), ou seja, a regra do convênio está em dois arquivos. É exatamente o caso que a referência chama de omissão.
- **O exagero.** Não há interface com uma implementação só, fábrica ou camada que só repasse. `PriceCalculator`, `ReservationStore` e `EmailSender` são classes concretas e diretas. Por isso, contenção.
- **O resto do código** está bem tipado e legível:
  - dinheiro é `BigDecimal` com `HALF_EVEN` em cada passo (`PriceCalculator.java:67-69`);
  - datas são `LocalDate`, e situação, plano e tipo de quarto são `enum`;
  - a reserva muda de estado só por métodos de pacote (`Reservation.java:102-114`);
  - os erros têm código próprio (`HotelException`, `ErrorCode`), e as exceções de SQL viram `IllegalStateException` com a causa;
  - o `catch (Exception)` em `web/HttpApi.java:113` só transforma corpo ilegível em 400, o que é aceitável.
- **Defeitos pequenos e localizados:**
  - A mensagem "room … is taken" está repetida (`ReservationService.java:46` e `ReservationStore.java:57-58`).
  - O pacote calcula as diárias com a temporada e depois as descarta (`PriceCalculator.java:267-277`).
  - O `companyCode` é gravado em qualquer plano (`ReservationStore.java:50`), embora a migração diga que ele fica nulo nos outros (`V2__reservation_company_code.sql:1`).

## Testes

**Números:** 26 testes rodaram, 0 falharam, 0 pulados; 26 métodos escritos. A cobertura é de 91,3% das linhas (485/531) e 74,5% dos ramos (105/141). No pacote `hotel.reservations`, os ramos ficam em 53 de 77. Os testes rodam contra o banco real (4 arquivos) e pela API (1); não há mocks.
**Nota:** com problemas

- **A parte comum está bem testada, e com valores:**
  - a temporada nos limites (15/12, 29/02 de ano bissexto, julho) em `PriceCalculatorTest.java:31-54`;
  - o "meio para o par" do desconto de 15% (37,485 vira 37,48) em `PriceCalculatorTest.java:57-64`;
  - a devolução no normal, no não reembolsável e na reserva pendente em `ReservationServiceTest.java:185-198`;
  - as transições de situação, as recusas com código de erro e o e-mail com assunto e valores em `ReservationServiceTest.java:129-182` e `:200-232`;
  - os relatórios com o CSV inteiro em `ReportsTest`;
  - a API com status e códigos em `HttpApiTest.java:91-121`.
- **Nenhum teste confere os três planos novos.** Não há nenhuma ocorrência de `CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY` ou `companyCode` em `src/test`. Os exemplos que o financeiro passou no texto (686,22, 820,00 e 410,00, 1.574,37) não estão em nenhum teste.
- **A pergunta da mutação:** nenhum teste falharia se alguém
  - trocasse 0.12 por 0.21 no Tecnosul (`PriceCalculator.java:21`);
  - mudasse a devolução do pacote de 0.5 para 1 (`ReservationService.java:104`);
  - trocasse `< 7` por `<= 7` na estadia longa (`ReservationService.java:61`);
  - apagasse a checagem de sexta-feira (`ReservationService.java:55`).

  Justamente a parte nova do cenário fica sem rede.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, criadas por 2 migrações do Flyway. O dinheiro está todo em `numeric(10,2)`, as datas em `date`, e a chave estrangeira `reservations.room_number → rooms` tem índice. As conferências acusaram só isto: `status`, `rate_plan`, `type` e `channel` são `text` sem CHECK. Não há transação explícita.
**Nota:** bom

- **O esquema evolui por migrações versionadas.** A segunda migração acrescenta a coluna (`V2__reservation_company_code.sql:2`) em vez de editar a primeira. Os quartos chegam numa carga inicial (`V1__create_tables.sql:9-14`), e a migração roda ao subir (`Database.java:28`).
- **O banco protege as regras:**
  - a reserva duplicada é impedida por `EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&) WHERE (status <> 'CANCELLED')` (`V1__create_tables.sql:38-40`). A violação vira 409 em `ReservationStore.java:56-59`, o que vale entre processos, e não só dentro do `synchronized`;
  - há CHECKs para saída depois da entrada e para pelo menos um hóspede (`V1__create_tables.sql:27`, `:36`).
- **O código que fala com o banco está correto.** Todo SQL usa `?`; o que é concatenado é só a constante `COLUMNS` (`ReservationStore.java:18-19`). As conexões ficam em try-with-resources, sobre um pool Hikari (`Database.java:23-27`).
- **Pontos menores:**
  - os relatórios trazem a tabela inteira de reservas para filtrar em Java (`ReservationStore.java:109-122`, usado em `OccupancyReport.java:25` e `RevenueReport.java:25`);
  - confirmar a reserva e guardar o e-mail são duas escritas sem transação (`ReservationService.java:82-87`): se a segunda falhar, a reserva fica confirmada sem mensagem;
  - não há CHECK nas colunas de situação e de plano.

## Comportamento

**Números:** 72 de 72 conferências passaram:

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 01 | 21 de 21 |
| duas reservas ao mesmo tempo | 2 de 2 (10 rodadas) |

**Nota:** bom

Nada falhou. Para o dono da pousada, isto quer dizer:
- **Convênio:** os três descontos estão certos, inclusive o exemplo do Tecnosul (686,22). Convênio sem código, ou com código errado, dá 400.
- **Pacote de fim de semana:** cobra 520, 820 ou 1.200 mesmo em julho, recusa datas fora de sexta a domingo e devolve a metade (410,00).
- **Estadia longa:** dá 1.574,37 no exemplo, mantém a temporada e recusa 6 noites.
- **Planos antigos e dados:** o normal e o não reembolsável continuam iguais, os dados sobrevivem a um reinício e duas reservas simultâneas do mesmo quarto nunca passam juntas.

## Dúvidas

- **A ordem das validações.** O serviço confere se o quarto está ocupado (`ReservationService.java:44`) antes das regras do plano (`:48-63`). Um pacote de sábado num quarto já ocupado recebe 409 `ROOM_UNAVAILABLE`, e não 400. O texto não fecha essa ordem, e o roteiro não testa isso.
- **A nota do design.** Ficou em `bom` porque a classe do pattern (omissão) não entra nela, e os demais defeitos são pequenos. Um revisor mais rígido poderia contar o texto do e-mail dentro do serviço e a regra do convênio dividida entre o calculador e o serviço como motivo para `com problemas`.
