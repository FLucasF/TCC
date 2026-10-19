# Cenário 05

## Visão geral

O agente entregou as regras certas, mas não concentrou a criação do documento. A decisão sobre qual documento criar (fatura, *booking receipt* ou comprovante) é o mesmo `if / else if / else` sobre o tipo de hóspede, copiado nos três lugares que emitem o documento: `confirm`, `checkOut` e `receiptCopy`, em `final/src/main/java/hotel/reservations/ReservationService.java:71-80`, `:124-133` e `:145-154`. Os textos ficaram em funções auxiliares separadas, mas a escolha continua espalhada. Um quarto tipo, como o documento das agências que o pedido já anuncia, obrigaria a mexer nos três lugares de novo. No comportamento está tudo certo: validação, fatura com ISS, recibo em inglês, documento igual nos três lugares e o comprovante do CPF igual ao da base.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin com estes pacotes:

- `hotel.web`: `HttpApi` (rotas e validação), `CreateReservationRequest` e `ReservationResponse`.
- `hotel.reservations`: `ReservationService` (as regras de reserva, confirmação, check-in, check-out e segunda via), `Reservation`, `Guest`, `PriceCalculator` e `ReservationStore`.
- `hotel.documents`: `ReservationReceipt` (o registro do documento) e `TextFormat`.
- `hotel.messages`: o e-mail, que guarda as mensagens em memória.
- `hotel.reports`: os relatórios em CSV.

Uma requisição chega na rota do `HttpApi`. O corpo vira `NewReservation` e `Guest` em `final/src/main/java/hotel/web/HttpApi.java:116-139` e segue para o `ReservationService`, que guarda tudo em memória no `ReservationStore`. O `Hotel` monta as peças e sobe o servidor.

O agente escreveu uma classe de teste, `DocumentsByGuestTest`, com 3 testes que conferem o título e trechos do texto de cada tipo pela segunda via. Os testes da base continuam idênticos.

**A parte que pede o pattern.**
- **O hóspede.** `Guest` ganhou os campos `companyCnpj`, `companyName`, `passport` e `country`, um construtor de três argumentos para o caso do CPF e os predicados `isCompany()` e `isForeigner()` (`final/src/main/java/hotel/reservations/Guest.java:20-26`). Não há um tipo por hóspede: o tipo é deduzido de quais campos estão preenchidos.
- **O documento.** `ReservationReceipt` continua sendo um registro só. Agora ele recebe de fora o título e o texto pronto (`final/src/main/java/hotel/documents/ReservationReceipt.java:29-47`).
- **Os textos.** Moram em `DocumentTexts`, em três funções estáticas: `personReceipt` (`final/src/main/java/hotel/documents/DocumentTexts.java:15`), `companyInvoice` (`:33`, com o ISS em `:48-50`) e `foreignReceipt` (`:55`, com `brl` em `:74-76`).
- **A escolha.** Não existe um lugar que receba o hóspede e devolva o documento. Cada um dos três métodos do `ReservationService` testa `isCompany()`, depois `isForeigner()`, e chama `new ReservationReceipt(...)` com o título e a função de texto daquele caso (`ReservationService.java:69-80`, `:122-134` e `:143-155`). Os três blocos são iguais linha a linha; só muda o nome da variável.

**O raciocínio.**
- **O que o afastou do pattern.** As instruções dizem que é omissão quando o `if` ou o `switch` sobre o tipo de hóspede aparece repetido nos três lugares, mesmo com o texto de cada documento em funções auxiliares separadas, porque o que importa é onde se decide qual criar. É exatamente o caso aqui.
- **O que o aproxima do pattern.** Só a separação dos textos em funções, que as instruções dizem não bastar. Não há fábrica que três lugares chamem, nem método polimórfico no hóspede, nem criador com método sobrescrito.
- **Exagero colateral.** Não há. `DocumentTexts` é um utilitário estático que tem uso, e não foi criada nenhuma interface, fábrica ou camada de repasse.
- **Comportamento.** Tudo o que testei conferiu, como detalhado abaixo.

## Item a item

**O pattern pedido.** A criação do documento continua espalhada. A decisão `if (reservation.guest().isCompany()) … else if (reservation.guest().isForeigner()) … else …`, seguida de `new ReservationReceipt(...)`, aparece três vezes: em `confirm` (`final/src/main/java/hotel/reservations/ReservationService.java:71-80`), em `checkOut` (`:124-133`) e em `receiptCopy` (`:145-154`). Os textos estão separados em `final/src/main/java/hotel/documents/DocumentTexts.java:15`, `:33` e `:55`, mas nenhum lugar concentra a escolha, e não há fábrica nem `guest.issueDocument(...)`. O documento das agências exigiria um quarto ramo em cada um dos três métodos. Esta é a omissão que as instruções descrevem.
Conclusão: omissão

**O exagero colateral.** Não há. As únicas estruturas novas são a classe utilitária `DocumentTexts` (`final/src/main/java/hotel/documents/DocumentTexts.java:10`), que tem uso real, os predicados de `Guest` (`final/src/main/java/hotel/reservations/Guest.java:20-26`) e o auxiliar `nullIfBlank` no `HttpApi`. Não foi criada nenhuma interface com uma implementação só, nem fábrica, builder ou camada de repasse.
Conclusão: contenção

**O comportamento.** Fiz uma cópia da `final/`, gerei `target/hotel.jar` e subi o serviço na porta 7070, com a base na 7071 para comparar. Chamei a API com `curl` e `jq`.

- **Validação.** Só o CPF, CNPJ com razão social, ou passaporte com país dão 201. Estes casos dão 400 `INVALID_REQUEST`: nenhuma identificação, CPF junto com passaporte, só CNPJ, só passaporte, só país, ou CPF em branco. A validação está em `final/src/main/java/hotel/web/HttpApi.java:126-134`.
- **Consulta da reserva.** `GET /reservations/{id}` mostra os quatro campos novos, e os que não se aplicam vêm `null`.
- **Fatura.** A reserva da empresa no quarto 201, de 10/03 a 12/03/2027, deu um texto idêntico ao exemplo do pedido. A linha do ISS saiu `ISS retido (5%): R$ 38,99`. Com o plano `NON_REFUNDABLE` e uma diária só, saíram a linha "Desconto", "1 diária" e o ISS de R$ 12,74 sobre R$ 254,90. Isso é meio para o par, como o README diz que o sistema arredonda.
- **Booking receipt.** Saiu igual ao exemplo, com `2027-03-10` e `2 nights`. Na suíte, em dezembro, com desconto: `Nights: BRL 2,783.52`, `Discount: BRL 417.53` e `Total: BRL 2,365.99`. Com uma noite saiu `1 night`.
- **Os três lugares.** O texto do e-mail de confirmação (`/messages`), o do check-out e o da segunda via são iguais para os três tipos. O e-mail continua em português em volta do documento, e o JSON do documento tem os campos de identificação (os que não se aplicam vêm `null`), os quatro novos e o `text`.
- **CPF e o que não mudou.** Fiz a mesma reserva com CPF na base e na final. O JSON do documento tem os mesmos valores, mais os campos novos `null` e o `text`. O e-mail de confirmação é igual ao da base, exceto pelo número da reserva. Os relatórios de faturamento e de ocupação têm o mesmo formato, e as recusas antigas continuam ("check-out must be after check-in"; um campo faltando dá "every field of the reservation is required").
- **Testes.** O `mvn verify` da cópia terminou com BUILD SUCCESS, 29 testes e nenhuma falha. São 3 testes novos do agente, em `DocumentsByGuestTest`, e 26 da base. Depois derrubei os serviços.

Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Nenhuma das formas aceitas como acerto foi usada: nem fábrica concentrada, nem método no tipo de hóspede, nem criador clássico. A repetição do `if/else` nos três métodos é literal e corresponde à definição de omissão.
