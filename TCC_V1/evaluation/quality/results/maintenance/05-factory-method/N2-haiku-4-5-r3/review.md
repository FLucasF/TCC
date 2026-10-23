# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: `hotel.web` (Javalin, `HttpApi`) lê a requisição e chama o `ReservationService` (`hotel.reservations`), que usa o `ReservationStore` (JDBC, PostgreSQL com Flyway) e monta os documentos de `hotel.documents`. Para o pedido, ele criou a interface `Document` (`src/main/java/hotel/documents/Document.java:4`), dois documentos novos (`Invoice`, `BookingReceipt`), um método que escolhe o documento pelo hóspede (`src/main/java/hotel/reservations/ReservationService.java:124-133`), quatro colunas novas numa migração nova (`V2__add_company_and_passport_fields.sql`) e a validação de "um jeito só" na borda HTTP (`src/main/java/hotel/web/HttpApi.java:127-142`). O que há de mais sério: o JSON da fatura e do booking receipt não tem `text` nem `title`. Na `Invoice` e no `BookingReceipt`, `text()` é um método, não um componente do record (`Invoice.java:39`, `BookingReceipt.java:40`), e o Jackson não o serializa. Por isso o check-out e a segunda via devolvem esses dois documentos sem o texto, e 16 das 32 conferências do cenário falham. O e-mail sai certo. A fatura também perdeu o desconto: numa reserva NON_REFUNDABLE, ela mostra "Diárias: R$ 779,80" e logo depois "Total: R$ 662,83", sem a linha do desconto.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base; um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já vinha da base (`measures/dependencies.md` e `measures/dependencies-base.md`). `HttpApi` passou de 170 para 194 linhas e `ReservationService` de 138 para 152.
**Nota:** bom

O agente não mudou a forma do sistema. A escolha do documento ficou na regra (`ReservationService.java:124-133`), sem Javalin e sem SQL. O SQL novo ficou só no `ReservationStore` (`ReservationStore.java:32-55` e `read`). A camada web só ganhou um envelope para a resposta (`src/main/java/hotel/web/DocumentResponse.java:6`). O ciclo `documents`/`reservations` já existia (o `ReservationReceipt` da base já lia `Reservation`); os documentos novos (`Invoice.java:3-4`, `BookingReceipt.java:3`) só o repetem e não criam ciclo novo. Um ponto que pesa pouco: a regra "exatamente um jeito de identificar o hóspede" mora só no handler (`HttpApi.java:127-142`). `ReservationService.create` aceita um `Guest` sem identificação nenhuma, e o banco também (veja Banco). É uma mistura pequena e localizada, no mesmo lugar onde a base já validava os campos obrigatórios.

## Design de baixo nível

**Números:** 3 de 3 documentos (comprovante, fatura, booking receipt) são criados por um único método de fábrica (`ReservationService.java:124-133`), e os 3 lugares que entregam documento chamam esse método: `confirm` (`:67`), `checkOut` (`:112`) e `receiptCopy` (`:121`). Nenhum lugar cria o documento à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É uma fábrica concentrada. `generateDocument` decide qual documento criar, e quem o usa só conhece a interface `Document` (`ReservationService.java:139-145` chama `document.text()` sem perguntar o tipo). O comprovante que já existia também passou por ela. Uma agência de turismo seria mais um ramo nesse método e mais uma classe. Uma coisa afasta do ideal: o tipo de hóspede não existe como tipo. Ele é deduzido de qual campo veio preenchido (`guest.companyCnpj() != null`, `guest.guestPassport() != null` em `:126-128`), num `Guest` com sete `String` que podem vir null (`src/main/java/hotel/reservations/Guest.java:3-10`). Uma quarta forma de identificação vai exigir mais campos nulos e mais um `if` sobre nulidade.

**O exagero.** A estrutura nova é a que o pattern pede: a interface `Document`, com três implementações. O `DocumentResponse` (`DocumentResponse.java:6`) é um envelope com `@JsonUnwrapped` que só repassa o documento. É dispensável, mas é pequeno e não chega a ser exagero.

**O resto.**
- Os três documentos não seguem a mesma forma, e isso causa o defeito mais sério do comportamento. No `ReservationReceipt`, `text` virou componente do record (`ReservationReceipt.java:23`), calculado no construtor por um `generateText` estático de 12 parâmetros que repete um a um os componentes do record (`:39-50`, `:53-65`). Na `Invoice` e no `BookingReceipt`, `text()` é método (`Invoice.java:39`, `BookingReceipt.java:40`). Por isso o JSON da fatura e do booking receipt sai sem `text`.
- A `Invoice` não tem os componentes `discount` e `title` (`Invoice.java:11-22`), e o texto dela não tem a linha de desconto (`Invoice.java:49-51`). Numa reserva NON_REFUNDABLE, a fatura fica com "Diárias" diferente de "Total" sem nada que explique a diferença.
- Os três textos repetem quase as mesmas linhas, cada um com os seus rótulos (`ReservationReceipt.java:66-78`, `Invoice.java:41-52`, `BookingReceipt.java:41-55`).
- O `TextFormat` ganhou `moneyPtBr`/`datePtBr`, mas manteve `money`/`date` como apelidos que só repassam (`TextFormat.java:22-32`). Hoje dois nomes fazem a mesma coisa.
- Pontos bons: o ISS usa `BigDecimal`, com `setScale(2, HALF_EVEN)` (`Invoice.java:55-58`), o mesmo arredondamento da base, e o dinheiro em inglês usa `Locale.US` (`TextFormat.java:34-36`).

## Testes

**Números:** 32 testes rodaram, 0 falharam, 0 foram pulados, 32 métodos escritos; a base tinha 26. Cobertura de linhas 85,1% (524 de 616; a base tinha 93,8%) e de ramos 84,9% (124 de 146; a base tinha 87,1%). O pacote `hotel.documents` caiu de 30 de 30 linhas para 45 de 104 (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

Os 6 testes novos estão todos em `src/test/java/hotel/web/HttpApiTest.java`:
- dois confirmam que a reserva com CNPJ e a com passaporte são criadas e mostram os campos novos (`:125`, `:141`);
- quatro conferem as recusas com 400 INVALID_REQUEST: sem identificação (`:156`), CPF e CNPJ juntos (`:168`), CNPJ sem razão social (`:181`) e passaporte sem país (`:194`).

O que é o centro do pedido ficou sem nenhum teste:
- a fatura;
- o ISS de 5% (R$ 38,99);
- o booking receipt em inglês ("BRL 1,234.56", "2027-03-10", "1 night");
- qual documento sai no e-mail, no check-out e na segunda via;
- o campo `text` no JSON.

A pergunta da mutação: trocar `0.05` por `0.5` (`Invoice.java:56`), trocar `Locale.US` por `BRAZIL` (`TextFormat.java:35`) ou trocar a ordem dos ramos da fábrica (`ReservationService.java:126-131`) não faria nenhum teste falhar. Um teste que pedisse `GET /reservations/{id}/document` de uma reserva de empresa teria pegado o `text` ausente. Os 26 testes da base continuam passando. Os testes do check-out e da segunda via só ganharam casts para `ReservationReceipt` (`ReservationServiceTest.java:145-146`, `:156`); não ficaram mais fracos, mas também não cobrem os documentos novos.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma data guardada como texto. A V1 está igual à da base, e a V2 é nova (`measures/database.md`). As colunas de situação e de tipo sem CHECK já vinham da base.
**Nota:** bom

A mudança no esquema veio como migração nova, sem editar a V1 (`src/main/resources/db/migration/V2__add_company_and_passport_fields.sql:2-8`). Ela acrescenta quatro colunas `text` e tira o `NOT NULL` de `guest_cpf`, o que o pedido exige. O SQL novo usa parâmetros (`ReservationStore.java:35` e os `setString` seguintes) e lista colunas constantes. A proteção contra duas reservas na mesma noite continua sendo a `EXCLUDE` da base. Um ponto que um revisor poderia pedir, sem que derrube a nota: ao tirar o `NOT NULL`, o banco passou a aceitar uma reserva sem identificação nenhuma. Não há um CHECK que garanta exatamente um dos três jeitos (CPF; CNPJ e razão social; passaporte e país), e essa regra fica só na borda HTTP.

## Comportamento

**Números:** 67 de 83 conferências passaram (`measures/behavior.md`):
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- duas reservas ao mesmo tempo: 2 de 2;
- o cenário 05: 16 de 32.
**Nota:** com problemas

O que funciona no cenário:
- o hóspede com CPF continua com o comprovante de antes, agora com `text`;
- a criação aceita CNPJ com razão social e passaporte com país, e a consulta da reserva mostra os campos que não se aplicam como null;
- as quatro recusas respondem 400 INVALID_REQUEST;
- o e-mail de confirmação em português leva a fatura certa. Conferi chamando a API: "Fatura de hospedagem nº 1 … ISS retido (5%)".

As 16 falhas têm uma causa só. No check-out e na segunda via, o JSON da fatura e o do booking receipt vêm sem `title` e sem `text`. Chamei a API e o `GET /reservations/1/document` de uma reserva de empresa devolveu só `reservationId, companyName, companyCnpj, guestName, roomNumber, roomType, checkIn, checkOut, nights, subtotal, total`, sem `guestCpf`, sem `discount`, sem `text`. O texto existe e sai no e-mail. Para o dono da pousada, isso quer dizer que a recepção, no check-out, e quem pede a segunda via não recebem a fatura nem o booking receipt para mostrar ou imprimir; só o e-mail de confirmação os leva.

Fora do roteiro, chamando a API:
- uma fatura NON_REFUNDABLE sai sem a linha de desconto ("Diárias: R$ 779,80" e "Total: R$ 662,83");
- uma reserva com CPF e mais `companyName` e `guestCountry` soltos é aceita, e esses campos são gravados (`HttpApi.java:127-142` só conta CPF, CNPJ e passaporte). Isso contradiz o "só um" do pedido.

## Dúvidas

- A forma do pattern é uma fábrica concentrada num método privado do serviço (`ReservationService.java:124-133`), e não o Factory Method clássico nem um método no tipo de hóspede. A referência aceita essa forma como acerto.
- Na nota do comportamento, metade das conferências do cenário falha, mas todas pela mesma causa (o `text` fora do JSON de dois documentos), e o e-mail sai certo. Por isso dei `com problemas` e não `ruim`. Quem pesar mais a fatura e o booking receipt ausentes no check-out e na segunda via pode ver como `ruim`.
- O pedido diz que a fatura é "parecida com o comprovante", mas o exemplo dele não tem desconto. Ele não decide se a fatura com desconto precisa da linha de desconto; tratei a falta dela como defeito de leitura, sem peso próprio na nota.
