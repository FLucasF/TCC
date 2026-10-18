# Cenário 05

## Visão geral

O agente juntou num lugar só a escolha de qual documento emitir. Criou um tipo fechado `Identification`, com `Cpf`, `Company` e `Foreigner`. Criou também uma interface `DocumentLayout`, com uma implementação para cada documento, e um método de fábrica estático, `DocumentLayout.forGuest`, que é o único lugar que decide pelo tipo de hóspede. Os três pontos que emitem o documento (confirmação, check-out e segunda via) continuam chamando o mesmo construtor de `ReservationReceipt`, e esse construtor pede o layout à fábrica. Nenhum deles pergunta qual é o caso concreto. O comprovante de hoje também passou pela estrutura nova: virou o `PortugueseReceiptLayout`. Para atender as agências, basta uma variante nova em `Identification`, um layout novo e um `case` novo, e o `switch` sobre o tipo fechado não compila enquanto esse `case` faltar. Não achei problema sério. Nos testes pela API, todas as regras do pedido conferiram.

## Item a item

**O pedido (pede o pattern).** A escolha do documento mora só em `final/src/main/java/hotel/documents/DocumentLayout.java:15-21`. Ali, `forGuest` faz um `switch` sobre `guest.identification()` e devolve `PortugueseReceiptLayout`, `LodgingInvoiceLayout` ou `BookingReceiptLayout`. O construtor público de `ReservationReceipt` (`final/src/main/java/hotel/documents/ReservationReceipt.java:33-35`) pede o layout a essa fábrica. O título e o texto do documento saem do layout (`ReservationReceipt.java:39` e `ReservationReceipt.java:55`).

Os três lugares que na base montavam o comprovante com `new` (`base/src/main/java/hotel/reservations/ReservationService.java:68,110,119`) não mudaram: `final/src/main/java/hotel/reservations/ReservationService.java:68,110,119`. Nenhum deles tem `if` ou `switch` sobre o tipo de hóspede, e todos passam pela fábrica. O comprovante antigo saiu de `ReservationReceipt.text()` (o trecho removido no patch) e foi para `final/src/main/java/hotel/documents/PortugueseReceiptLayout.java:13-32`, de modo que o caso que já existia também está na estrutura. A fatura herda do comprovante e só troca as linhas de identificação e o fecho (`LodgingInvoiceLayout.java:9,19-29`).

Em `final/src/main/java/hotel/reservations/Guest.java:10-30` há `instanceof` sobre a identificação. Esses `instanceof` só projetam os campos de identificação, para o JSON e para os textos; não escolhem documento.
Conclusão: acerto

**O exagero colateral.** Não há. O `Identification` (`final/src/main/java/hotel/reservations/Identification.java:4-17`) modela os três jeitos de identificar o hóspede, que o pedido pede. O `readIdentification` (`final/src/main/java/hotel/web/HttpApi.java:132-148`) é a validação pedida. O resto do código (preço, relatórios, mensagens, quartos) ficou intocado, e o agente não criou builder, camada nova nem interface com uma implementação só fora do que o pedido tocava.
Conclusão: contenção

**O comportamento.** Rodei uma cópia da `final/` na porta 7070 e testei pela API:

- **Hóspede com CPF:** `POST /reservations` com `guestCpf`, depois confirmação e `GET /reservations/1/document`. Veio o título "Comprovante de reserva", o mesmo texto de hoje, `companyCnpj`, `companyName`, `guestPassport` e `guestCountry` como null, e o campo `text` presente.
- **Empresa:** com `companyCnpj` e `companyName` no quarto 202, de 10/03/2027 a 12/03/2027, a fatura saiu idêntica ao exemplo do pedido. Tem a linha "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", depois "Hóspede: Ana Souza", e fecha com "ISS retido (5%): R$ 38,99". O texto foi o mesmo no e-mail de confirmação (`GET /messages`, que continua em português), no `POST .../check-out` e na segunda via.
- **Empresa com desconto:** 3 noites em julho, `NON_REFUNDABLE`. Veio "Desconto: R$ 134,95", "Total: R$ 764,69" e "ISS retido (5%): R$ 38,23", que é 5% de 764,69 arredondado para centavos.
- **Estrangeiro:** saiu "Booking receipt no. 3", "Guest: … (passport X1234567, United Kingdom)", "Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights" e "BRL 1,159.80" com separador de milhar.
- **Estrangeiro, uma noite com desconto:** veio "1 night", "Discount: BRL 37.48" antes de "Total: BRL 212.42".
- **`GET /reservations/{id}`:** traz os quatro campos novos, com null nos que não se aplicam.
- **Recusas:** todas as combinações abaixo deram 400 `INVALID_REQUEST`:
  - nenhuma identificação;
  - CPF com empresa;
  - CPF com passaporte;
  - empresa com passaporte;
  - só `companyCnpj`, só `companyName`, só `guestPassport` ou só `guestCountry`;
  - `guestCpf` em branco;
  - CPF com `guestCountry`;
  - `companyName` em branco.
- **O que não devia mudar:** continua igual. Três hóspedes no quarto 101 dão 422 `CAPACITY_EXCEEDED`. A segunda via de uma reserva pendente dá 409. Os relatórios de receita e de ocupação saem em CSV como antes. Uma noite de alta temporada continua custando 299,88.

O `mvn verify` da cópia passou: 31 testes e nenhuma falha. Cinco deles são do agente, três no `ReservationServiceTest` e dois no `HttpApiTest`. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

A forma usada é a fábrica concentrada: um método estático, `DocumentLayout.forGuest`, com `switch` sobre o tipo fechado `Identification`. Não é a forma clássica, com um criador sobrescrito por tipo.

O que varia por tipo de hóspede é o layout (título e texto). O registro `ReservationReceipt` é o mesmo para os três e serve de envelope de dados para o JSON. Considerei se isso seria a fábrica "que devolve sempre o mesmo documento e a escolha acontece depois". Não é: a escolha acontece dentro da fábrica, e o que ela devolve já é o documento específico, que depois só é embrulhado. Também é um detalhe que a fábrica seja chamada de dentro do construtor do registro, e não pelo `ReservationService`: os três lugares continuam pedindo o documento por um único ponto.
