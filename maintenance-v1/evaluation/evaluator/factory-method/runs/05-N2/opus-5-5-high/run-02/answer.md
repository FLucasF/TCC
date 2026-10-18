# Cenário 05

## Visão geral

O agente juntou num lugar só a decisão sobre qual documento criar. A classe `Documents` escolhe o layout pelo tipo de identificação do hóspede, que agora é uma `sealed interface` com três casos. O `confirm`, o `checkOut` e o `receiptCopy` passaram a pedir o documento a ela, e o `new ReservationReceipt(...)` que aparecia nos três lugares acabou. O comprovante de quem tem CPF, que já existia, também entrou na estrutura nova, como `ReceiptLayout`. Para um quarto documento (o da agência) bastam um caso a mais em `Identification`, um layout novo e um braço a mais no único `switch`. O compilador avisa se faltar o braço, porque o `switch` sobre a interface selada precisa cobrir todos os casos. Testei a API e o comportamento confere com o pedido. A única ressalva é pequena: a mensagem de erro da segunda via de uma reserva não paga trocou "receipt" por "document".

## Item a item

**O pedido (pede o pattern).** Na base, o `ReservationReceipt` era criado com `new` em três lugares: `base/src/main/java/hotel/reservations/ReservationService.java` no `confirm`, no `checkOut` e no `receiptCopy`. Na final, os três chamam a mesma fábrica, em `final/src/main/java/hotel/reservations/ReservationService.java:70` (`confirm`), `:112` (`checkOut`) e `:121` (`receiptCopy`). A instância de `Documents` é criada em `:19`. Só um lugar decide qual documento criar: o `switch` sobre `reservation.guest().identification()` em `final/src/main/java/hotel/documents/Documents.java:15-19`. Ele leva cada caso a `RECEIPT`, `INVOICE` ou `BOOKING_RECEIPT`, que são os layouts de `:10-12`, e monta o documento em `:20` com `ReservationDocument.issue(reservation, room, layout)`. Esse método fica em `final/src/main/java/hotel/documents/ReservationDocument.java:33-53`. O código de produção não tem nenhum outro `switch` ou `instanceof` sobre o tipo de hóspede. Os outros `switch` que existem tratam do plano de tarifa e dos códigos de erro, e já estavam na base. A diferença entre os documentos mora nas classes `DocumentLayout` (`final/src/main/java/hotel/documents/DocumentLayout.java:4`). São elas `ReceiptLayout` (`final/src/main/java/hotel/documents/ReceiptLayout.java:4`), `InvoiceLayout`, que estende o comprovante e sobrescreve `identification` e `closing` (`final/src/main/java/hotel/documents/InvoiceLayout.java:7,17,23`), e `BookingReceiptLayout` (`final/src/main/java/hotel/documents/BookingReceiptLayout.java:4`). O tipo devolvido é sempre o mesmo record, `ReservationDocument`. Mesmo assim, o título e o texto já saem prontos da fábrica, de acordo com o layout escolhido, e nada depois dela volta a decidir. Por isso não é o caso de "devolve sempre o mesmo e escolhe depois". É uma fábrica concentrada, e o caso antigo (o CPF) passa por ela como os outros.
Conclusão: acerto

**O exagero colateral.** Não há. Toda a estrutura nova serve à variação que o pedido trouxe. A `Identification` selada (`final/src/main/java/hotel/reservations/Identification.java:7`) é o tipo de hóspede que o pedido introduz. `DocumentLayout` e suas três implementações são os três documentos. `EnglishTextFormat` (`final/src/main/java/hotel/documents/EnglishTextFormat.java:9`) é uma função auxiliar estática, igual à `TextFormat` que já existia. Fora disso, só tornou público o `PriceCalculator.cents` (`final/src/main/java/hotel/reservations/PriceCalculator.java:50`) para arredondar o ISS. Não criou builder, camada nova, nem interface com uma implementação só em parte que o pedido não tocava.
Conclusão: contenção

**O comportamento.** Rodei `mvn -q verify` numa cópia: passou, com 32 testes e nenhuma falha (DocumentsTest 5, ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 11, HttpApiTest 5). Subi o serviço na porta 7070 e chamei a API com `curl`.

- **Identificação do hóspede:**
  - `POST /reservations` com CPF, com CNPJ e razão social, e com passaporte e país: os três deram 201. O JSON da reserva traz os quatro campos novos, com `null` nos que não se aplicam.
  - Deram 400 `INVALID_REQUEST`: nenhum jeito de identificação, CPF com CNPJ, CPF com passaporte, CNPJ sem razão social, razão social sem CNPJ, passaporte sem país, país sem passaporte, CPF com só a razão social, e CPF em branco.
- **Documentos:**
  - Com CNPJ, `GET /reservations/2/document` devolveu a "Fatura de hospedagem nº 2" com "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza", total R$ 779,80 e "ISS retido (5%): R$ 38,99". O `guestCpf` veio null e o campo `text` está presente.
  - Com passaporte, saiu "Booking receipt no. 3" com datas `2027-03-12`, "2 nights" e "BRL 779.80". Para uma noite no plano `NON_REFUNDABLE`, saíram "1 night" e a linha "Discount: BRL 44.98" antes do total. Para 3 noites na suíte, saiu "BRL 2,087.64", com separador de milhar.
  - Com CPF, o comprovante tem o mesmo texto do `text()` da base.
- **Mesmo documento nos três lugares:**
  - Os e-mails de `/messages` têm a moldura em português ("Olá, …", "Sua reserva … está confirmada") com o documento certo dentro.
  - O texto do check-out da empresa é igual ao da segunda via.
- **O que não devia mudar:**
  - Excesso de capacidade continua dando 422 `CAPACITY_EXCEEDED`.
  - Cancelar no plano `STANDARD` devolve 779.80.
  - Os relatórios de faturamento e de ocupação saem como antes.
  - A segunda via de uma reserva pendente continua dando 409 `OPERATION_NOT_ALLOWED`. A mensagem passou de "issue a receipt for" para "issue a document for" (`final/src/main/java/hotel/reservations/ReservationService.java:119`; antes `base/src/main/java/hotel/reservations/ReservationService.java:117`). É uma mudança de texto na mensagem; o código de erro e o status são os mesmos.

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Das formas aceitas, o agente usou a fábrica concentrada, `Documents.issue` com um `switch` exaustivo sobre a `Identification` selada. Não é o Factory Method clássico, com um criador por tipo, nem um método polimórfico no próprio hóspede. O produto é um único record, `ReservationDocument`, e quem varia é o layout, que funciona como uma estratégia de texto. Pesei se isso seria o caso de "a fábrica devolve sempre o mesmo documento e a escolha acontece depois". Concluí que não: a escolha acontece dentro da fábrica, e o documento sai dela com título e texto definitivos. Também anoto que `InvoiceLayout` herda de `ReceiptLayout` usando métodos-gancho (`identification` e `closing`). É reaproveitamento interno dos layouts e não muda a classe.
