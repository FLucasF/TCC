# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em sete pacotes. A requisição entra por `hotel.web` (`HttpApi` lê o JSON em
`CreateReservationRequest` e monta um `NewReservation`), passa pelas regras em `hotel.reservations`
(`ReservationService`, `PriceCalculator`) e chega ao acesso a dados em `ReservationStore`, que usa JDBC sobre um
PostgreSQL com o esquema feito por migrações do Flyway. O comprovante fica em `hotel.documents`. O pedido era pontual:
crianças na reserva. O agente fez a mudança pequena, no lugar de cada coisa: um campo `children` no pedido HTTP
(opcional, 0 quando falta), no `NewReservation`, na `Reservation`, na resposta e no comprovante; a soma de adultos e
crianças na conferência da capacidade; a recusa do número negativo; e uma migração nova, `V2`, com `NOT NULL DEFAULT 0
CHECK (children >= 0)`. Não criou nenhuma estrutura para a cobrança futura das crianças. Os 61 pontos do roteiro passam
e os 33 testes passam. Não há nada sério. Os defeitos são pequenos: dois pontos do pedido não têm teste (o número
negativo e "criança não paga"), e o README não fala do campo novo.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` comparado com
`measures/dependencies-base.md`; só mudam as contagens de linhas). Há 1 ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base.
**Nota:** bom

A mudança segue as camadas que já existiam. O padrão do campo (0 quando falta) é resolvido na borda HTTP
(`src/main/java/hotel/web/HttpApi.java:127`), a regra fica no serviço, que não conhece o Javalin nem o JDBC
(`src/main/java/hotel/reservations/ReservationService.java:34-44`), e o SQL fica só em `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:19,32,43,132`). O ciclo vem da base:
`ReservationService.java:3` importa `hotel.documents.ReservationReceipt`, e `ReservationReceipt.java:3` importa
`hotel.reservations.Reservation`. O agente não o piorou. Nenhum pacote novo, nenhuma dependência nova, e as classes
cresceram de 1 a 6 linhas cada (`HttpApi` foi de 170 para 171).

## Design de baixo nível

**Números:** 8 arquivos de `src/main` mudados, mais 1 migração nova. Nenhum tipo novo em `src/main`: nem interface, nem
fábrica, nem builder, nem política de preço de criança. `PriceCalculator` ficou como estava.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este é o controle negativo do cenário 06. A frase que tenta o agente ("cobrar alguma coisa das crianças maiores no
futuro") não produziu estrutura nenhuma. O que entrou é exatamente o que `controls.md` chama de contenção:
- um componente a mais nos records (`NewReservation.java:6`, `CreateReservationRequest.java:15`,
  `ReservationResponse.java:18`, `ReservationReceipt.java:17`);
- um campo final com seu acessor na `Reservation` (`Reservation.java:14,38,70-71`);
- a soma na conferência da capacidade (`ReservationService.java:40`);
- um `if` para a linha do comprovante, logo depois da linha do quarto (`ReservationReceipt.java:47-49`);
- a coluna nova no banco.

A reserva continua sendo criada direto, pelo construtor do record. Os tipos são os certos: `int` no domínio e
`Integer` só no corpo HTTP, para distinguir o campo ausente (`HttpApi.java:127`). O preço não foi tocado, e por isso a
criança não paga.

Defeitos pequenos, nenhum que impeça aceitar:
- a mensagem do erro de capacidade continua dizendo "guests" (`ReservationService.java:43`), quando agora conta adultos
  e crianças;
- o comentário de `CreateReservationRequest.java:6` foi atualizado, mas o README (`README.md:60-83`) não mostra o campo
  `children` nem diz que `CAPACITY_EXCEEDED` passou a contar as crianças.

O resto do código vem da base e está limpo: `BigDecimal` para dinheiro, `LocalDate` para datas, `enum` para situação e
plano, estado privado e final na `Reservation`.

## Testes

**Números:** 33 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 33 métodos escritos. A base tinha 26 (todos
mantidos; só a chamada de `NewReservation` ganhou o `0` nos ajudantes `ReportsTest.java:33` e
`ReservationServiceTest.java:36`). Cobertura de linhas: 93,8% (481 de 513), igual à base. Cobertura de ramos: 86,9%
(106 de 122), contra 87,1% na base. `hotel.documents` está com 33 de 33 linhas e 6 de 6 ramos (`measures/tests.md`).
**Nota:** com problemas

O agente testou com valores a maior parte do que mudou:
- adultos e crianças somam contra a capacidade, com 1 adulto e 2 crianças no quarto 101, que comporta 2, recusados
  com `CAPACITY_EXCEEDED` (`src/test/java/hotel/reservations/ReservationServiceTest.java:164-170`);
- as crianças ficam guardadas e voltam na consulta (`ReservationServiceTest.java:179-188`);
- pela API, `children: 2` volta com `guests: 1` (`src/test/java/hotel/web/HttpApiTest.java:124-135`), e a ausência do
  campo vira 0 (`HttpApiTest.java:138-142`);
- o comprovante mostra "Crianças: 2" e, sem criança, não mostra a linha
  (`src/test/java/hotel/documents/ReservationReceiptTest.java:35-58`).

Pela pergunta da mutação: apagar a parcela `+ request.children()` faz falhar o teste da linha 164; trocar `> 0` por
`>= 0` no comprovante faz falhar o da linha 48. Mas há regras do pedido sem rede:
- **o número negativo:** se alguém apagar a recusa de `ReservationService.java:34-36`, nenhum teste falha. O `CHECK`
  do banco ainda barraria a gravação, mas como erro interno, não como 400;
- **"criança não paga":** nenhum teste compara o total de uma reserva com crianças ao de uma sem crianças;
- **a posição da linha "Crianças":** os testes só usam `contains` (`ReservationReceiptTest.java:44`), e não conferem
  que ela vem logo depois da linha do quarto;
- **um teste que não testa o que diz:** `childrenAreOptionalAndDefaultToZero` (`ReservationServiceTest.java:173-176`)
  passa o 0 explicitamente pelo ajudante `stay`, então não exercita padrão nenhum. Quem cobre o padrão é o teste HTTP.

## Banco

**Números:** 3 tabelas, todas com chave primária, e 2 migrações do Flyway aplicadas. A `V1__create_tables.sql` está
igual à da base, e a `V2__add_children_to_reservations.sql` é nova. Dinheiro em `numeric(10,2)`, nenhuma chave
estrangeira sem índice, nenhuma data em texto. O `CHECK (children >= 0)` novo aparece entre as restrições
(`measures/database.md`). As colunas de situação e de plano em `text`, sem `CHECK`, vêm da base.
**Nota:** bom

O esquema evolui do jeito certo: uma migração nova, de uma linha
(`src/main/resources/db/migration/V2__add_children_to_reservations.sql:1`), com `NOT NULL DEFAULT 0`. Num banco que já
tem reservas, as antigas ficam com zero crianças, sem perder nada, e o `CHECK` repete no banco a regra do serviço. A
`V1` não foi tocada. O SQL continua com parâmetros, e a posição dos `?` foi reajustada sem erro
(`ReservationStore.java:31-50`). A leitura traz a coluna nova (`ReservationStore.java:132`). A proteção contra reserva
dupla continua sendo o `EXCLUDE USING gist` da base (`V1__create_tables.sql:39`).

## Comportamento

**Números:** 61 de 61 conferências passaram, 0 falharam (`measures/behavior.md`):
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- as crianças na reserva: 7 de 7;
- o comprovante: 2 de 2;
- as crianças depois de reiniciar: 1 de 1;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- sem o campo, são zero crianças;
- dois adultos e uma criança cabem no quarto para três e são recusados (422) no quarto para dois;
- a criança não muda o preço;
- a consulta mostra as crianças;
- o número negativo dá 400;
- o comprovante ganha "Crianças: 2" logo depois da linha do quarto e fica como era quando não há criança;
- a informação continua lá depois de reiniciar.

O que já funcionava continua funcionando, inclusive as duas reservas ao mesmo tempo.

## Dúvidas

Nenhuma sobre as notas. Um detalhe que o texto não fecha: a segunda via em JSON (`HttpApi.java:79` serializa o record
`ReservationReceipt`) passou a ter também um campo `children`. É um acréscimo coerente com o pedido e não quebra quem
já lia a resposta.
