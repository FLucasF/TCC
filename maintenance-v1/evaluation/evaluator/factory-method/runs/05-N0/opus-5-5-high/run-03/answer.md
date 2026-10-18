# Cenário 05

## Visão geral

O agente modelou bem a identificação do hóspede: criou um `GuestIdentity` selado com três casos e uma leitura do pedido que aceita um jeito só de identificar. O documento, porém, não ganhou fábrica nem tipos próprios. Continua existindo um único `ReservationReceipt`. Ele escolhe o título com um `switch` sobre o tipo de hóspede e monta o texto com `if`s que testam se os campos são nulos (`guestPassport != null`, `companyCnpj != null`), espalhados por três métodos da mesma classe. Os três lugares que geram o documento continuam com `new ReservationReceipt(...)`, como na base. Nenhum lugar decide qual documento criar: há um documento só, que se pergunta várias vezes qual é o caso concreto. O documento das agências exigiria mexer em todos esses pontos. O comportamento está todo certo e o `mvn verify` passa. O problema sério é a falta do pattern.

## Item a item

**O pedido (pede o pattern).** A criação nos três lugares não mudou desde a base. `confirm` faz `new ReservationReceipt(reservation, roomOf(reservation))` em `final/src/main/java/hotel/reservations/ReservationService.java:68`, `checkOut` faz o mesmo na linha 110 e `receiptCopy` na linha 119. Não existe criador, fábrica nem método `issueDocument` no tipo de hóspede. O `GuestIdentity` (`final/src/main/java/hotel/reservations/GuestIdentity.java:4-17`) só guarda dados, sem nenhum comportamento.

Toda a variação foi para dentro do `ReservationReceipt`, como mais condicionais:
- um `switch` de título por tipo, em `final/src/main/java/hotel/documents/ReservationReceipt.java:57-63`;
- `if (guestPassport != null)` para escolher entre texto em inglês e em português, na linha 68;
- `if (companyCnpj != null)` para a linha da empresa, na linha 77;
- de novo `if (companyCnpj != null)` para o ISS, na linha 92.

O record também passou a carregar os campos de identificação dos três tipos, com nulos nos que não se aplicam (linhas 21-24 do patch, campos `companyCnpj`…`guestCountry`).

A escolha do documento, portanto, não mora num lugar só. Está repartida em quatro pontos e usa dois mecanismos diferentes: o tipo da identidade e a nulidade de campos. O tipo do documento é sempre o mesmo, e a distinção acontece depois, dentro do `text()`. É exatamente o "mais um `if`, mais um `case`" que o pedido devia evitar, já que avisa que vem um quarto documento (o das agências).
Conclusão: omissão

**O exagero colateral.** Não há. O `GuestIdentity` selado (`final/src/main/java/hotel/reservations/GuestIdentity.java:4`) e os acessores de `Guest` (`final/src/main/java/hotel/reservations/Guest.java:3-28`) ficam dentro do que o pedido tocava, que é a identificação do hóspede, e o selado tem três implementações. `TextFormat.englishMoney` e `englishDate` (`final/src/main/java/hotel/documents/TextFormat.java`) são só funções auxiliares. Tornar `PriceCalculator.cents` público é mudança de visibilidade, não estrutura nova.
Conclusão: contenção

**O comportamento.** Fiz o build de uma cópia da `final/`, subi o serviço na porta 7070 e testei com `curl`:
- **Criação e recusas.** As reservas com CPF, com CNPJ e razão social, e com passaporte e país dão 201, e a resposta traz os quatro campos novos, com `null` nos que não se aplicam. Dão 400 `INVALID_REQUEST`: nenhuma identificação, CPF junto com passaporte e país, só `companyCnpj`, só `guestCountry`, CPF junto com `companyName`, e CPF em branco.
- **Fatura (empresa, quarto 201, 10 a 12/03/2027).** O texto sai idêntico ao do pedido, terminando em `ISS retido (5%): R$ 38,99`. Com desconto e uma diária, o ISS de 212,42 deu R$ 10,62, o que está correto.
- **Booking receipt.** O texto sai idêntico ao do pedido. Numa suíte em julho com `NON_REFUNDABLE` veio `Nights: BRL 2,087.64`, `Discount: BRL 313.15` e `Total: BRL 1,774.49`, com separador de milhar. Com uma noite veio "1 night".
- **CPF.** Recebe o comprovante de hoje.
- **Os três lugares.** O documento sai igual no e-mail (`/messages`, que continua em português), no check-out e no `/document`. O JSON ganhou os quatro campos de identificação, com `null` nos que não se aplicam, e o campo `text`.
- **O que não mudou.** A capacidade continua dando 422, o quarto ocupado dá 409, o cancelamento `STANDARD` devolve o valor pago, e os relatórios de receita e de ocupação saem como antes.

O `mvn -q verify` da cópia passou: 30 testes, 0 falhas. São 4 testes novos do agente (fatura, booking receipt, recusas de identificação, desconto com uma noite) e os 26 da base. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Pode-se perguntar se isto é uma "fábrica concentrada", porque o `if` não se repete nos três lugares que geram o documento e toda a decisão está numa classe só. Escolhi `omissão` porque essa classe não escolhe qual documento criar. Ela é sempre o mesmo documento e consulta o caso concreto em quatro pontos (`ReservationReceipt.java:57`, `:68`, `:77`, `:92`), metade deles por nulidade de campo e não pelo tipo. Isso fica mais perto de "a escolha acontece depois, fora da fábrica" e de "mais um `if`" do que de um lugar único que decide a criação. Das formas que contariam como acerto, nenhuma foi usada: nem criador com método sobrescrito, nem fábrica por tipo, nem `guest.issueDocument(...)`.
