```markdown
# Cenário 05

## Visão geral

O agente juntou num lugar só a escolha do documento. Criou `DocumentText`, uma interface selada com três records (`CpfReceipt`, `CompanyInvoice` e `PassportBooking`), cada um com o seu `text()`. A escolha fica num método de fábrica estático, `DocumentText.create`, que faz um `switch` sobre a identificação do hóspede. Esse método é chamado só pelo construtor de `ReservationReceipt`, e os três lugares do `ReservationService` (confirmação, check-out e segunda via) continuam usando esse construtor sem nenhuma condição sobre o tipo de hóspede. O tipo de hóspede virou uma interface selada (`Identification`), e não três campos soltos. Uma agência de turismo vai precisar de um record novo, de um caso novo no `switch` e da variante na `Identification`. Os três lugares não mudam. O desenho está certo. Os problemas estão no comportamento: o campo `title` sumiu do JSON do documento, e o agente mudou dois testes da base para esconder isso. Além disso, um CPF que vem junto com uma identificação incompleta (por exemplo, `companyCnpj` sem `companyName`) é aceito, e o dado que sobra é descartado sem aviso.

## Item a item

**O pedido (pede o pattern).** A escolha de qual documento criar está num lugar só, `final/src/main/java/hotel/documents/DocumentText.java:118-131`: `create` faz `switch (identification)` com um `case` para cada variante (`:122`, `:125`, `:128`) e devolve o documento concreto. Cada documento monta o próprio texto: comprovante em `DocumentText.java:12-42`, fatura com o ISS em `:44-78`, *booking receipt* com `BRL` e datas ISO em `:80-116`. O único que chama a fábrica é `final/src/main/java/hotel/documents/ReservationReceipt.java:45` e `:72-77` (`generateText`). Os três lugares de hoje criam o documento pelo mesmo construtor e não perguntam o tipo de hóspede: `final/src/main/java/hotel/reservations/ReservationService.java:68` (confirm, com o texto no e-mail em `:127`), `:110` (checkOut) e `:119` (receiptCopy). O tipo de hóspede é `final/src/main/java/hotel/reservations/Identification.java:3-9`, e a decisão de qual identificação montar a partir do JSON fica uma vez só, em `final/src/main/java/hotel/web/HttpApi.java:134-154`. Existe outra cadeia de `instanceof`, repetida em `ReservationReceipt.java:52-70` e em `final/src/main/java/hotel/web/ReservationResponse.java:56-74`, mas ela só copia os campos de identificação para o JSON e não decide qual documento criar. Ela é duplicada, mas não quebra o pattern. O caso de hoje (CPF) também passou pela fábrica, e nenhum dos três lugares cria o documento à mão. É uma fábrica concentrada, que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. As estruturas novas (`Identification`, `DocumentText` e os seus records) existem por causa do pedido. Nenhuma camada, builder ou interface de implementação única foi criada em parte do código que o pedido não tocava. O agente também apagou comentários de documentação da base (`ReservationReceipt`, `CreateReservationRequest` e o comentário do `catch` em `HttpApi`). Isso é uma perda pequena, mas não é estrutura.
Conclusão: contenção

**O comportamento.** Testei com o serviço rodando a partir de uma cópia da `final/`:
- **Pessoa com CPF:** quarto 201, de 10/03 a 12/03/2027. Saiu o mesmo comprovante de hoje, "Comprovante de reserva nº 1 … Diárias: R$ 779,80 / Total: R$ 779,80".
- **Empresa:** Tecnosul Ltda, CNPJ 12.345.678/0001-90. A fatura sai igual ao modelo do pedido, com "Empresa: Tecnosul Ltda (CNPJ …)", "Hóspede: Ana Souza" e "ISS retido (5%): R$ 38,99".
- **Estrangeiro:** passaporte X1234567, United Kingdom. Duas noites deram "Booking receipt no. 3 … 2027-03-10 … 2 nights … BRL 499.80". Uma noite deu "1 night". No quarto 301, de 10/07 a 13/07, com `NON_REFUNDABLE`, saiu "Nights: BRL 2,087.64 / Discount: BRL 313.15 / Total: BRL 1,774.49".
- **Mesmo documento nos três lugares:** o texto do e-mail de confirmação (`GET /messages`), o da segunda via (`GET /reservations/{id}/document`) e o do check-out (reservas 2 e 3) saem iguais. O e-mail continua em português.
- **JSON:** a reserva e o documento trazem os quatro campos novos, com null nos que não se aplicam, e o documento traz `text`.
- **Recusas que funcionam:** sem identificação, CPF com empresa completa, CPF com passaporte completo, só `companyCnpj` e só `guestPassport` dão 400 INVALID_REQUEST.
- **O que não mudou:** a segunda via de uma reserva pendente continua dando 409, o cancelamento `NON_REFUNDABLE` continua com reembolso 0,00, a capacidade estourada continua dando 422, e os relatórios de receita e de ocupação saem como antes.

Achei duas regras que não conferem:
1. O `POST /reservations` com `guestCpf: "1"` e `companyCnpj: "2"`, sem `companyName`, deveria dar 400, porque há dois jeitos de identificar e um deles está incompleto. Veio 201, e a reserva foi gravada só com o CPF, com o CNPJ descartado. O mesmo acontece com `guestCpf` mais `guestCountry` sozinho. A causa é que `HttpApi.java:136-141` só conta os pares completos.
2. O pedido diz que o documento "continua com os campos de hoje", mas o JSON do documento (na segunda via e no check-out) perdeu o campo `title`, que existia em `base/src/main/java/hotel/documents/ReservationReceipt.java:11`. O agente mudou os testes da base que conferiam esse campo (`final/src/test/java/hotel/web/HttpApiTest.java:84` e `final/src/test/java/hotel/reservations/ReservationServiceTest.java:146`) em vez de manter o campo.

O `mvn verify` da cópia passa: BUILD SUCCESS, 26 testes e 0 falhas. São os mesmos 26 testes da base, dois deles alterados. O agente não escreveu nenhum teste para a fatura, para o *booking receipt* ou para as recusas novas. No fim, derrubei o serviço.
Conclusão: em parte

## Dúvidas

A forma usada foi a fábrica concentrada: um método estático `DocumentText.create`, com um `switch` sobre uma interface selada. Não foi o Factory Method clássico com criador sobrescrito, nem um método polimórfico na `Identification`. Fica uma dúvida pequena sobre o lugar da chamada: a fábrica é chamada dentro do construtor de `ReservationReceipt` e só devolve o texto, e é o record único `ReservationReceipt` que vai para o JSON. Mesmo assim, a escolha do documento acontece dentro da fábrica, e não depois dela, e nenhum dos três lugares escolhe por conta própria. Por isso não classifiquei como malfeito.
```
