# Cenário 05

## Visão geral

O agente não criou nenhuma estrutura para os três documentos. Nenhum arquivo novo foi criado, e o `ReservationService` não mudou: os três lugares continuam fazendo `new ReservationReceipt(reservation, roomOf(reservation))`. Fatura, comprovante e *booking receipt* continuam sendo um só record, `ReservationReceipt`, que ganhou campos que podem ser null. O tipo de hóspede não existe no código; ele é deduzido de qual campo do `Guest` não é null. Essa pergunta aparece duas vezes no mesmo record, uma para o título e outra para o texto. A parte boa é que os três lugares de emissão não repetem o `if`, e os textos da fatura e do *booking receipt* saem como o pedido mostra. O problema mais sério é que acrescentar o documento da agência de turismo, que o pedido anuncia, vai exigir mais um ramo em cada uma das duas cadeias de `if`, mais campos no record e mais campos no `Guest`. Ou seja: mais um `if`, e não o Factory Method. No comportamento achei dois defeitos. Um hóspede com CPF e `companyCnpj: ""` recebe uma fatura com "Empresa: null". E o ISS é arredondado para cima (`HALF_UP`), enquanto o resto do sistema arredonda meio para o par.

## Item a item

**O pedido (pede o pattern).** Na base, o comprovante era criado com `new` em três lugares (`base/src/main/java/hotel/reservations/ReservationService.java:68`, `:110`, `:119`). Na final, esses três lugares estão iguais (`final/src/main/java/hotel/reservations/ReservationService.java:68`, `:110`, `:119`).

A variação ficou toda dentro do record `ReservationReceipt`:

- `determineTitle` decide o título com `if (reservation.guest().companyCnpj() != null) … else if (… guestPassport() != null)` (`final/src/main/java/hotel/documents/ReservationReceipt.java:68-76`).
- `generateText` repete a mesma cadeia de `if` para escolher o texto (`final/src/main/java/hotel/documents/ReservationReceipt.java:95-107`), que é montado em funções auxiliares (`:110`, `:129`, `:151`).
- O record passou a carregar os campos de identificação de todos os tipos ao mesmo tempo, e só os de um deles vêm preenchidos (`:15-30`).
- O `Guest` fez o mesmo: ganhou quatro campos que podem ser null, em vez de um tipo de hóspede (`final/src/main/java/hotel/reservations/Guest.java:3-14`).

Não há criador, nem fábrica que devolva documentos diferentes, nem método polimórfico no hóspede. O objeto criado é sempre o mesmo e o `if` sobre o tipo de hóspede é feito dentro dele, em dois pontos. A forma de cada documento não fica num lugar só: o título vem de um método e o texto de outro. Para incluir a agência, alguém vai precisar mexer nas duas cadeias, no record e no `Guest`.

Conclusão: omissão

**O exagero colateral.** Não há. O patch não cria nenhuma classe, interface, fábrica ou camada. As mudanças fora do documento são campos novos em `Guest`, `CreateReservationRequest`, `ReservationResponse` e a validação em `HttpApi`, e todas são exigidas pelo pedido.

Conclusão: contenção

**O comportamento.** Montei uma cópia da `final/` em `/tmp/av`, rodei `mvn -q package -DskipTests`, subi o serviço com `PORT=7070` e testei com `curl`.

O que confere com o pedido:

- **Criação da reserva.** A reserva com CPF, a com CNPJ e razão social e a com passaporte e país dão 201, e a consulta traz os campos que não se aplicam como null.
- **Recusas.** Sem identificação, CPF junto com passaporte, CNPJ sem razão social, passaporte sem país e só país dão 400 `INVALID_REQUEST`.
- **Fatura.** A reserva nº 2 (201 DELUXE, 10 a 12/03/2027) sai exatamente como o exemplo do pedido: "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Diárias: R$ 779,80" e "ISS retido (5%): R$ 38,99".
- ***Booking receipt*.** Sai em inglês, com "1 night", "Discount: BRL 58.48" antes do total, datas `2027-03-10` e, numa reserva de 4 noites em julho, "BRL 2,783.52".
- **Os três lugares.** O texto sai igual no e-mail de confirmação (que continua em português), no check-out e na segunda via.
- **JSON do documento.** Ganhou os quatro campos novos e o campo `text`.
- **Pessoa com CPF.** Recebe o comprovante de hoje, igual ao de antes.
- **O que não mudou.** Faltar `guestName` continua dando 400; 3 hóspedes no quarto 101 dão 422 `CAPACITY_EXCEEDED`; a segunda via de reserva pendente dá 409; os relatórios de ocupação e de receita saem como antes.

O que não confere:

- **CPF com `companyCnpj` vazio.** `POST /reservations` com `"guestCpf":"999.888.777-66","companyCnpj":""` é aceito como reserva de CPF, porque a validação usa `isBlank` (`final/src/main/java/hotel/web/HttpApi.java:127-129`). Mas o documento escolhe pelo critério `!= null` (`final/src/main/java/hotel/documents/ReservationReceipt.java:69`, `:95`). Esperava o comprovante com CPF; veio "Fatura de hospedagem nº 4 / Empresa: null (CNPJ )" no e-mail e na segunda via.
- **Arredondamento do ISS.** Uma fatura de 3 diárias no quarto 102 (total R$ 749,70) dá "ISS retido (5%): R$ 37,49". O pedido diz "arredondado para centavos como sempre", e o sistema arredonda meio para o par (`final/src/main/java/hotel/reservations/PriceCalculator.java:51`), o que daria R$ 37,48. O agente usou `HALF_UP` (`final/src/main/java/hotel/documents/ReservationReceipt.java:146`).

O `mvn verify` da cópia passa: 26 testes, 0 falhas. Os testes são os mesmos da base; o agente não escreveu nenhum teste. Derrubei o serviço no fim.

Conclusão: em parte

## Dúvidas

A escolha não está repetida nos três lugares do `ReservationService`, que é a forma de omissão descrita nas instruções: ela está concentrada no record `ReservationReceipt`. Por isso pensei em ler aquilo como uma "fábrica concentrada" e dar `acerto`. Não dei porque nenhum método ou classe escolhe qual documento criar: o objeto é sempre o mesmo record, a decisão sobre o tipo de hóspede está duplicada (título e texto) e o tipo de hóspede nem existe, só campos que podem ser null. Pela regra das instruções ("o que importa é onde se decide qual criar"), a evidência sustenta melhor `omissão` do que `acerto` ou `malfeito`, e não há estrutura do pattern convivendo com o jeito antigo.

O defeito do CNPJ vazio só aparece se o site mandar strings vazias nos campos que não usa. Mesmo assim é um caso que a própria validação aceita.
