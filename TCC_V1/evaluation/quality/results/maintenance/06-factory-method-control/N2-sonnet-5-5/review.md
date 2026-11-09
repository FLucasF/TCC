# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel` (montagem e `main`), `hotel.web` (rotas e JSON), `hotel.reservations` (serviço, regras, preço e `ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` (o comprovante) e `hotel.reports`. Uma requisição entra em `HttpApi`, que monta o `NewReservation` a partir do JSON (`final/src/main/java/hotel/web/HttpApi.java:109-131`) e chama o `ReservationService`. O serviço confere as regras e grava pelo `ReservationStore`. Os dados ficam no PostgreSQL, num esquema criado por migrações do Flyway. O pedido era acrescentar crianças à reserva: um campo opcional `children` (zero quando falta, nunca negativo), adultos e crianças somados na conferência da capacidade, crianças sem cobrança, e a linha "Crianças: N" no comprovante. O agente fez uma mudança pequena e no lugar certo, em 13 arquivos: o campo passa pelo pedido HTTP, pelo `NewReservation`, pela `Reservation`, pela resposta e pelo comprovante. Entrou uma migração nova (`V2__add_children.sql`) com `NOT NULL DEFAULT 0 CHECK (children >= 0)`, a soma na conferência da capacidade e dois testes. Nenhum dos 28 testes falha, e as 61 conferências do roteiro passaram. Não há nada sério. O único ponto solto é que o `README.md` não foi atualizado: o corpo de `POST /reservations` (`final/README.md:60-71`) não mostra `children`, e o `CAPACITY_EXCEEDED` ainda diz "mais hóspedes do que o quarto comporta" (`final/README.md:82`). Justamente o pessoal do site, que pediu o campo, lê essa documentação.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números da base são idênticos: 7 pacotes, 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). O agente só mudou o tamanho de algumas classes: `HttpApi` foi de 170 para 171 linhas, `ReservationStore` de 143 para 145, `ReservationService` de 138 para 141 e `Reservation` de 115 para 122.
**Nota:** bom

A mudança respeita as camadas que já existiam:
- **Borda HTTP:** só lê o campo e aplica o padrão zero (`final/src/main/java/hotel/web/HttpApi.java:129-130`), e o expõe na resposta (`final/src/main/java/hotel/web/ReservationResponse.java:18,38`).
- **Regras:** a recusa do número negativo e a soma na capacidade ficam no serviço, que não conhece Javalin nem SQL (`final/src/main/java/hotel/reservations/ReservationService.java:34-44`).
- **Dados:** o SQL novo fica só no `ReservationStore` (`final/src/main/java/hotel/reservations/ReservationStore.java:19,31-50,132`).
- **Comprovante:** a linha nova fica em `hotel.documents` (`final/src/main/java/hotel/documents/ReservationReceipt.java:47-49`).

O ciclo `documents` ↔ `reservations` vem da base: o `ReservationService` importa `ReservationReceipt` (`final/src/main/java/hotel/reservations/ReservationService.java:3`), e o comprovante lê a `Reservation`. O agente não o criou nem o piorou. Nenhuma dependência nova surgiu entre pacotes.

## Design de baixo nível

**Números:** controle negativo. Entrou um campo `int children` em 5 tipos (`CreateReservationRequest`, `NewReservation`, `Reservation`, `ReservationResponse`, `ReservationReceipt`), uma soma e uma recusa no serviço, um `if` no comprovante e uma coluna nova. Nenhuma classe, interface, fábrica ou builder novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

**O que o pedido tentava provocar:** "cobrar alguma coisa das crianças maiores no futuro". O agente não caiu nisso. Não há política de preço por criança, nem preço de criança sempre zero, nem builder ou fábrica para a reserva. O `PriceCalculator` ficou intocado: `ReservationService.java:49` continua chamando `priceCalculator.calculate(room, checkIn, checkOut, ratePlan)` sem nada de criança. A reserva continua criada direto (`final/src/main/java/hotel/reservations/ReservationStore.java:53`). É exatamente a resposta que `controls.md` descreve como contenção: um campo na reserva e no pedido, a soma na capacidade (`ReservationService.java:40`), a linha no comprovante (`ReservationReceipt.java:47-49`) e a coluna nova no banco.

**O design do resto do código:**
- O padrão zero para o campo ausente fica num lugar só, a borda (`HttpApi.java:130`). A regra de não ser negativo fica no serviço (`ReservationService.java:34-36`) e é repetida no banco como `CHECK` (`V2__add_children.sql:2`).
- Os tipos são os certos: `int` no domínio, `Integer` só no DTO, para distinguir "ausente".
- A renumeração dos parâmetros do `INSERT` está correta (`ReservationStore.java:42-50`). Os testes e o roteiro gravam e leem o valor.
- O que um revisor apontaria já vinha da base: o `catch (Exception)` na leitura do corpo (`HttpApi.java:113`, com o motivo comentado) e o `synchronized` no serviço.
- Do agente, só um detalhe de documentação: o `README.md` não diz que `children` existe (`final/README.md:60-71`). Isso não pesa na nota de design.

## Testes

**Números:**
- Testes: 28 rodaram, 0 falhas, 0 erros, 0 pulados, 28 métodos escritos. A base tinha 26, e os 26 continuam passando.
- Cobertura: linhas 94,0% (482 de 513) e ramos 87,7% (107 de 122). Na base, eram 93,8% e 87,1%.
- `hotel.documents` tem 33 de 33 linhas e 6 de 6 ramos cobertos.

**Nota:** bom

O agente acrescentou dois testes, um no serviço e um pela API, e conferiu as regras novas com valores:
- `childrenTakeRoomSpaceButAreNotCharged` (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:64-78`) confere quatro coisas:
  - a criança gravada e lida de volta (`:71`);
  - o preço igual ao de uma reserva sem criança num quarto de mesma diária, 101 e 102 a R$ 249,90 (`:72-73`);
  - a capacidade com 2 adultos e 2 crianças no 201, que comporta 3, dando `CAPACITY_EXCEEDED` (`:74-75`);
  - o número negativo dando `INVALID_REQUEST` (`:76-77`).
- `childrenAreOptionalAndShowOnTheReceipt` (`final/src/test/java/hotel/web/HttpApiTest.java:91-103`) confere o campo ausente virando 0 (`:94`), o campo devolvido (`:99`) e "Crianças: 2" no e-mail do comprovante (`:102`).

**A pergunta da mutação:**
- Tirar `request.children()` da soma da capacidade faria `:74` falhar.
- Trocar `>` por `>=` faria o próprio teste falhar já na primeira reserva, porque 1 adulto e 1 criança no 101, que comporta 2, fica no limite.
- Cobrar a criança faria `:72` falhar.

Uma parte da regra do comprovante ficou sem rede:
- Nenhum teste confere que, sem criança, o comprovante fica como está. Trocar `children > 0` por `children >= 0` (`ReservationReceipt.java:47`) imprimiria "Crianças: 0" em todo comprovante, e nenhum teste falharia.
- A posição "logo depois da linha do quarto" também não é conferida, porque o teste só usa `contains`.

As mudanças nos testes da base só acrescentaram o argumento `0` (`ReportsTest.java:33`, `ReservationServiceTest.java:36`). Nada foi apagado nem enfraquecido.

## Banco

**Números:**
- O serviço subiu contra um banco vazio: 3 tabelas e 2 migrações do Flyway aplicadas.
- `V1__create_tables.sql` está igual à da base, e `V2__add_children.sql` é nova.
- Conferências sem problema: nenhuma tabela sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto.
- A restrição nova `CHECK ((children >= 0))` aparece no esquema.
- O que o script acusa vem da base: as colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`) são `text` sem `CHECK`.

**Nota:** bom

O agente fez a mudança de esquema do jeito certo: uma migração nova, sem editar a V1. A coluna é `integer NOT NULL DEFAULT 0 CHECK (children >= 0)` (`final/src/main/resources/db/migration/V2__add_children.sql:2`). Com o `DEFAULT 0`, as reservas que já existem num banco em uso ganham zero crianças, e a migração aplica sem perder dado. O `CHECK` repete no banco a regra do serviço.

O SQL continua com parâmetros (`ReservationStore.java:31-50`). A concatenação que o script lista é só da constante `COLUMNS` (`ReservationStore.java:18-19,99,112`). A exclusão por `daterange` contra reserva dupla e o pool vêm da base e não mudaram.

## Comportamento

**Números:** 61 de 61 conferências passaram, nenhuma falhou:

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| crianças na reserva | 7 de 7 |
| comprovante | 2 de 2 |
| crianças depois de reiniciar | 1 de 1 |
| reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, o site já pode mandar `children` ou deixá-lo de fora, e uma reserva sem o campo tem zero crianças. Uma família de 2 adultos e 1 criança cabe no quarto para três, mas não no quarto para dois (422). A criança não muda o preço, e um número negativo é recusado (400). O comprovante ganha "Crianças: 2" logo depois da linha do quarto e fica igual quando não há criança. Tudo o que já funcionava continua funcionando: preço, temporada, planos, cancelamentos, relatórios, os dados depois de reiniciar e a recusa da reserva dupla simultânea.

## Dúvidas

- Na nota dos testes, ficaram sem teste a ausência da linha "Crianças" quando não há criança e a posição da linha no comprovante. Pesei isso como uma lacuna pequena, que não muda a nota. Um revisor mais estrito poderia pedir essas duas asserções antes de aceitar e dar `com problemas`.
- O `README.md` não foi atualizado com o campo `children`. Ele não entra no `changes.patch`, e a comparação de `base/` com `final/` confirma que ficou igual. Pesei isso como documentação, fora das notas.
