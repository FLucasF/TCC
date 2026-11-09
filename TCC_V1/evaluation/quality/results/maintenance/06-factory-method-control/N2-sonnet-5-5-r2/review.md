# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em 7 pacotes. `hotel.web` (`HttpApi`) lê o JSON e chama `ReservationService` (`hotel.reservations`), que confere as regras e grava pelo `ReservationStore` (JDBC com `?`) no PostgreSQL. O esquema é criado por migrações do Flyway. O pedido era pôr crianças na reserva, e o agente fez a mudança pequena, no lugar onde cada coisa já ficava. O campo `children` entrou no corpo do pedido (`final/src/main/java/hotel/web/CreateReservationRequest.java:15`), com 0 quando vem vazio (`final/src/main/java/hotel/web/HttpApi.java:130`). Também entrou em `NewReservation`, em `Reservation` e na resposta da API. A recusa de número negativo e a soma de adultos e crianças na conferência da capacidade ficaram em `final/src/main/java/hotel/reservations/ReservationService.java:34-44`. A linha do comprovante está em `final/src/main/java/hotel/documents/ReservationReceipt.java:47-49`, e o banco ganhou uma migração nova (`V2__add_children.sql`). O agente não criou estrutura nenhuma para a "cobrança futura" das crianças. Os 29 testes passam, e as 61 conferências do roteiro também. Não achei nada sério. Os poucos defeitos que existem vieram da base.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). As linhas mudaram pouco: `hotel.reservations` passou de 508 para 526, `hotel.web` de 236 para 240 e `hotel.documents` de 79 para 84.
**Nota:** bom

O agente não mexeu na estrutura, só acrescentou um campo em cada camada.
- **A borda HTTP:** só traduz o campo opcional para 0 (`final/src/main/java/hotel/web/HttpApi.java:130`).
- **As regras:** a recusa e a capacidade ficam no serviço, sem Javalin e sem SQL (`final/src/main/java/hotel/reservations/ReservationService.java:34-44`).
- **Os dados:** o SQL continua só no `ReservationStore` (`final/src/main/java/hotel/reservations/ReservationStore.java:19,32,43,132`).
- **A montagem:** continua num lugar só (`final/src/main/java/hotel/Hotel.java:25-26`).

O ciclo entre `documents` e `reservations` vem da base, e o agente não o piorou. A maior classe continua sendo `HttpApi`, com 171 linhas, uma a mais que na base.

## Design de baixo nível

**Números:** o agente mudou 9 arquivos de `src/main`. Em todos, a mudança foi um campo, uma condição ou uma coluna. Não há classe, interface, fábrica nem builder novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O que o agente fez é exatamente o que `controls.md` descreve como contenção para este cenário:
- um campo a mais no pedido e na reserva (`final/src/main/java/hotel/reservations/NewReservation.java:12`, `final/src/main/java/hotel/reservations/Reservation.java:14,38,70`);
- a soma na conferência da capacidade (`final/src/main/java/hotel/reservations/ReservationService.java:40`);
- a linha no comprovante (`final/src/main/java/hotel/documents/ReservationReceipt.java:47-49`);
- a coluna nova no banco.

A reserva continua criada direto, e não há uma "política de preço de criança": o `PriceCalculator` não foi tocado. Os tipos são os certos: `int` dentro do domínio e `Integer` na borda, que é o que deixa o campo ser opcional. Não há duplicação.

Um detalhe pequeno: a mensagem da recusa por capacidade ainda diz "takes at most N guests" (`final/src/main/java/hotel/reservations/ReservationService.java:43`), e agora `guests` quer dizer só os adultos. O código de erro está certo, então isso não muda a nota.

O `catch (Exception)` em `final/src/main/java/hotel/web/HttpApi.java:113` veio da base.

## Testes

**Números:** 29 testes rodaram, com 0 falhas e 0 pulados; há 29 métodos escritos, contra 26 na base. A cobertura de linhas foi de 93,8% para 94,2% (483 de 513), e a de ramos de 87,1% para 88,5% (108 de 122). `hotel.documents` tem 6 de 6 ramos cobertos.
**Nota:** bom

O agente escreveu três testes novos, e eles conferem as regras novas com valores:
- **A capacidade e a recusa:** num quarto para 3, 2 adultos e 1 criança cabem, e 2 adultos e 2 crianças dão `CAPACITY_EXCEEDED`. Com −1 criança, a resposta é `INVALID_REQUEST`. O preço fica em 779.80, e uma reserva sem o campo tem 0 crianças (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:72-80`).
- **O comprovante:** confere a linha "Crianças: 2" exatamente entre a linha do quarto e a da entrada, e confere que ela não aparece quando não há criança (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:83-90`).
- **Pela API:** o campo é opcional, volta na consulta, e a API responde 422 e 400 (`final/src/test/java/hotel/web/HttpApiTest.java:109-120`).

Pela pergunta da mutação: se a soma perdesse as crianças, se `>` virasse `>=`, ou se a linha aparecesse com 0 crianças, algum teste falharia. A leitura da coluna também está coberta, porque `service.find` vai ao banco.

Os 26 testes da base continuam lá, e o agente não enfraqueceu nenhum: só acrescentou o `0` nas chamadas (`final/src/test/java/hotel/reports/ReportsTest.java`, `ReservationServiceTest.java:36`).

Dois detalhes de estilo, sem peso: `assertTrue(!...)` no lugar de `assertFalse` (`ReservationServiceTest.java:90`), e linhas de mais de 120 caracteres (`HttpApiTest.java:118,120`).

## Banco

**Números:** 3 tabelas e 2 migrações aplicadas. A V1 está igual à da base, e a V2 é nova. As conferências deram:
- tabela sem chave primária: nenhuma;
- dinheiro: tudo em `numeric(10,2)`;
- chave estrangeira sem índice: nenhuma;
- data guardada como texto: nenhuma.

As colunas `status`, `rate_plan`, `type` e `channel` estão em `text`, sem CHECK, e assim já estavam na base.
**Nota:** bom

O agente acrescentou uma migração e não editou a da base, então quem já roda o sistema sobe sem problema. A migração é `ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0)` (`final/src/main/resources/db/migration/V2__add_children.sql:2`). O `DEFAULT 0` dá zero crianças às reservas antigas, e o CHECK repete no banco a regra do "nunca negativo". O INSERT e a leitura usam parâmetros, e o agente renumerou todos certo (`final/src/main/java/hotel/reservations/ReservationStore.java:31-50,132`). Os candidatos a "SQL montado com `+`" só juntam constantes, como `COLUMNS`. A proteção contra reserva dupla é a restrição EXCLUDE da base.

## Comportamento

**Números:** 61 de 61 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- crianças na reserva: 7 de 7;
- o comprovante: 2 de 2;
- as crianças depois de reiniciar: 1 de 1;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Nada falhou. Para o dono da pousada, o resultado é este:
- O site pode mandar `children` ou deixar o campo de fora, e sem ele são zero crianças.
- Criança não muda o preço.
- Um quarto para dois recusa dois adultos e uma criança, e um número negativo de crianças é recusado.
- O comprovante mostra "Crianças: 2" logo abaixo do quarto, e fica como antes quando não há criança.
- As crianças continuam na reserva depois que o sistema reinicia.
- Tudo o que funcionava antes continua funcionando.

## Dúvidas

nenhuma
