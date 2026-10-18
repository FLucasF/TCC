# Cenário 05

## Visão geral

O agente não criou nenhuma estrutura de criação. Continua existindo uma única classe de documento, `ReservationReceipt`, e foram postos dentro dela dois `if/else` sobre o tipo de hóspede: um escolhe o título e o outro escolhe o texto. Os pontos de uso continuam fazendo `new ReservationReceipt(...)`, e agora são quatro em vez de três. Nada no código decide "qual documento criar". Há um único documento que confere o próprio caso concreto toda vez que monta o texto. Quando entrar o documento da agência de turismo, vai ser preciso mexer de novo nos dois `if`, no record e no `Guest`.

Os caminhos principais funcionam: fatura com ISS, *booking receipt* em inglês, recusas e o documento igual nos três lugares. Achei, porém, um defeito de comportamento. Um hóspede com CPF que manda `companyCnpj: ""` recebe uma "Fatura de hospedagem" com a empresa vazia. Os testes do agente são zero: os 26 testes que passam são todos os da base.

## Item a item

**O pedido (pede o pattern).** O pedido traz três documentos por tipo de hóspede, nos mesmos três lugares, e anuncia um quarto, o da agência de turismo. A variação foi resolvida com condicionais dentro da classe que já existia:

- **Título:** `final/src/main/java/hotel/documents/ReservationReceipt.java:49-57` (`selectTitle`) testa `companyCnpj != null`, depois `guestPassport != null` e, no resto, "Comprovante de reserva".
- **Texto:** `final/src/main/java/hotel/documents/ReservationReceipt.java:60-68` (`text()`) refaz a mesma pergunta, na ordem inversa (passaporte antes de CNPJ), e despacha para `textBookingReceipt`, `textInvoice` ou `textReservationReceipt` (`:70`, `:86`, `:105`).
- **Dados:** o record ganhou os campos de todos os tipos ao mesmo tempo (`:15-19`).

Não há criador, fábrica nem método polimórfico no hóspede. `Guest` é só um record com sete campos anuláveis (`final/src/main/java/hotel/reservations/Guest.java:3-14`). O documento é criado à mão com `new ReservationReceipt(...)` em `final/src/main/java/hotel/reservations/ReservationService.java:52`, `:71`, `:114` e `:123`, igual à base (`base/src/main/java/hotel/reservations/ReservationService.java:68`, `:110`, `:119`).

A pergunta sobre o tipo de hóspede não se repete nos pontos de uso, mas também não virou a escolha de qual documento criar. Ela continua dentro do único documento, em dois lugares e com ordens diferentes. É a forma "mais um `if`" que o enunciado descreve como omissão, e não uma estrutura de Factory Method que conviva com o jeito antigo (malfeito).

Conclusão: omissão

**O exagero colateral.** Não há interface, fábrica, builder nem camada nova. As adições fora do documento são campos e métodos simples:

- `documentText` com getter e setter em `final/src/main/java/hotel/reservations/Reservation.java:19,81-87`;
- o construtor de conveniência de `Guest` (`Guest.java:12-14`);
- dois formatadores em `final/src/main/java/hotel/documents/TextFormat.java:27-33`.

Há também um campo `text` na resposta da reserva (`final/src/main/java/hotel/web/ReservationResponse.java:30,54`), que o pedido não pediu (o `text` era só do documento). Ele aparece até numa reserva `PENDING`, mas é dado a mais, não estrutura.

Conclusão: contenção

**O comportamento.** Rodei o `mvn -q verify` numa cópia: passou, com 26 testes (5 + 6 + 11 + 4). Os testes da `final/` são idênticos aos da `base/`, ou seja, o agente não escreveu nenhum teste. Subi o serviço na porta 7070 e testei pela API.

Funcionou como o pedido diz:

- **Empresa:** o `POST /reservations` com `companyCnpj` e `companyName` no quarto 201, de 10/03 a 12/03/2027, trouxe "Fatura de hospedagem nº 1", "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza", "Total: R$ 779,80" e "ISS retido (5%): R$ 38,99", igual ao exemplo do pedido.
- **Estrangeiro:** com `guestPassport` e `guestCountry`, veio "Booking receipt no. 2", "Guest: John Smith (passport X1234567, United Kingdom)", "Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights" e "Nights: BRL 779.80".
- **Uma noite com desconto:** "1 night", "Discount: BRL 104.38" antes do total.
- **Milhar:** três noites na suíte em julho deram "BRL 2,087.64".
- **CPF:** o comprovante é o de hoje.
- **Recusas:** voltam 400 `INVALID_REQUEST` para nenhuma identificação, CPF com CNPJ, CPF com passaporte, só o CNPJ, só o passaporte e só o país.
- **Consulta da reserva:** traz os quatro campos novos, com `null` nos que não se aplicam.
- **E-mail de confirmação:** continua em português e leva dentro a fatura, o *booking receipt* ou o comprovante, conforme o hóspede.
- **Check-out e segunda via:** devolvem o mesmo texto, com o campo `text` e os campos de identificação que não se aplicam em `null`.
- **O que não mudou:** a segunda via de reserva pendente continua com 409, o cancelamento `NON_REFUNDABLE` devolve 0,00, o excesso de hóspedes dá 422, e os relatórios de ocupação e financeiro saem como antes.

O que falhou:

- **CNPJ vazio com CPF:** `POST /reservations` com `guestCpf: "999.888.777-66"`, `companyCnpj: ""` e `companyName: ""` foi aceito (201). Isso está certo, porque a validação trata o vazio como ausente. Mas o `Guest` guarda as strings vazias (`HttpApi.java:153-160`), e o documento testa `!= null` (`ReservationReceipt.java:50,63`). Resultado: o texto veio como "Fatura de hospedagem nº 6 / Empresa:  (CNPJ ) … ISS retido (5%): R$ 24,99", quando o esperado era o comprovante de hoje com o CPF.
- **Passaporte em branco com CPF:** com `guestPassport: " "` e `guestCountry: " "`, saiu um "Booking receipt" com "passport  ,  ".

Um site que mande os campos que não se aplicam como string vazia cai nesse defeito. Os caminhos principais seguem o pedido, mas este não.

Conclusão: em parte

## Dúvidas

1. **Omissão ou malfeito.** A pergunta sobre o tipo de hóspede não está espalhada nos três pontos de uso, como no caso típico de omissão. Está dentro da única classe de documento. Classifiquei como omissão porque não existe nenhuma estrutura de criação (fábrica, criador ou método no hóspede) que decida qual documento criar. A variação foi absorvida com `if/else` duplicados (`ReservationReceipt.java:49-57` e `:60-68`), e os quatro pontos continuam fazendo `new` do mesmo documento. Com uma leitura mais generosa de "a decisão mora num lugar só", alguém poderia defender malfeito. Acerto não cabe: o documento devolvido é sempre o mesmo, e a escolha acontece depois, no `text()`.
2. **Os casos com string vazia.** O defeito só aparece quando os campos que não se aplicam chegam como `""` ou `" "`. Não sei se o site real manda campos assim. Mesmo assim, a validação do próprio agente aceita esse corpo, e o resultado é um documento errado.
