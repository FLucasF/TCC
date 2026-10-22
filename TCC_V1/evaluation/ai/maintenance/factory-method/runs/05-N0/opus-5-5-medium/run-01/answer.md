Já li o pedido, o patch e os arquivos da `final/`. Agora escrevo a correção.

# Cenário 05

## Visão geral

O agente manteve um único tipo de documento, o `ReservationReceipt`, e colocou a variação por tipo de hóspede dentro dele. São duas cadeias de `if/else` sobre campos que podem ser nulos (`guestPassport != null`, `companyCnpj != null`): uma escolhe o título e a outra escolhe o texto. Os três lugares do `ReservationService` continuam fazendo `new ReservationReceipt(...)` como na base. Por isso não existe fábrica, criador, método polimórfico nem tipos distintos de documento, e o tipo de hóspede nem chega a ser um tipo. O resultado é que, para incluir a agência, será preciso mexer no `Guest`, nas duas cadeias de `if` e no record. O ponto bom é que a decisão não ficou espalhada pelos três lugares, mas também não foi concentrada num ponto de criação. Esse é o problema mais sério do desenho. No comportamento, os exemplos do pedido saem certos, com estas falhas:
- a fatura omite a linha de desconto;
- o ISS é arredondado com HALF_UP, enquanto o sistema usa HALF_EVEN;
- um campo de identificação enviado em branco ("") pode trocar o documento emitido.

## Como foi implementado, e por que o julgamento

**O projeto.** A aplicação é montada em `Hotel` (`final/src/main/java/hotel/Hotel.java:19-28`), que cria `Rooms`, `ReservationStore` (dados em memória), `PriceCalculator`, `EmailSender`/`SentMessages`, `ReservationService`, os relatórios e a `HttpApi`, a camada Javalin. Uma requisição entra numa rota da `HttpApi`. O corpo de `POST /reservations` é lido em `readNewReservation` (`final/src/main/java/hotel/web/HttpApi.java:123-174`), que valida os campos e monta `Guest` e `NewReservation`. Daí a chamada segue para o `ReservationService`, onde ficam as regras de estado, preço e e-mail, e os dados ficam no `ReservationStore`. O agente não escreveu nem alterou testes: `src/test` é idêntico ao da base.

**A parte que pede o pattern.**
- O `Guest` passou a ter sete campos (`cpf`, `companyCnpj`, `companyName`, `guestPassport`, `guestCountry`), em que os que não se aplicam ficam nulos (`final/src/main/java/hotel/reservations/Guest.java:3-14`).
- O `ReservationReceipt` ganhou esses quatro campos e o componente `text`, e implementa uma interface nova, `Document`, que só tem `text()` (`final/src/main/java/hotel/documents/ReservationReceipt.java:27`; `final/src/main/java/hotel/documents/Document.java:4-6`).
- No construtor, o título vem de `determineTitleAndReturn`, um `if` sobre passaporte, depois CNPJ, senão CPF (`ReservationReceipt.java:82-90`).
- O texto vem de `computeDocumentText`, outro `if` com a mesma ordem de testes, que chama `formatBookingReceipt`, `formatCompanyInvoice` ou `formatCpfReceipt` (`ReservationReceipt.java:66-80`, `:92-145`).
- Os três lugares que emitem o documento continuam com `new` direto: `confirm` (`final/src/main/java/hotel/reservations/ReservationService.java:73`), `checkOut` (`:115`) e `receiptCopy` (`:124`).
- O agente também acrescentou `Reservation.createReceipt(room)`, que só faz `new ReservationReceipt(this, room)` (`final/src/main/java/hotel/reservations/Reservation.java:113-115`). Esse método é usado só por `ReservationResponse.of` para pôr um campo `text` na resposta da reserva (`final/src/main/java/hotel/web/ReservationResponse.java:33-57`).

**O raciocínio.** As instruções aceitam como acerto três formas: um criador com método de fábrica, uma fábrica que escolhe o documento pelo tipo de hóspede, ou um método polimórfico no hóspede. Nenhuma delas está aqui. Há um documento só, que olha para si mesmo e decide como se formatar com `if/else`. A escolha está até duplicada, uma vez para o título e outra para o texto. E os três lugares não pedem o documento a ninguém: constroem a mesma classe com `new`.
- O que aproxima o desenho do pattern é que a decisão não se repete nos três lugares, porque está dentro de uma única classe.
- O que o afasta é que essa classe não cria documentos diferentes. Ela é o `if` da variação, posto no próprio documento.

Isso se encaixa melhor em "a variação ficou sem o pattern (um `if`…)" do que em fábrica concentrada.

No exagero colateral pesaram três coisas:
- a interface `Document`, com uma única implementação, que nenhum código usa como tipo;
- o método `Reservation.createReceipt`, que só repassa a chamada;
- o código morto: `ReservationResponse.of(Reservation)` (`ReservationResponse.java:59-82`), que ninguém chama, e `roomOf(long)` (`ReservationService.java:27-30`), que também não é usado.

No comportamento pesaram a linha de desconto ausente na fatura, o arredondamento diferente e o caso dos campos em branco.

## Item a item

**O pattern pedido.** A escolha do documento é feita por duas cadeias de `if/else` sobre campos nulos dentro do próprio `ReservationReceipt` (`final/src/main/java/hotel/documents/ReservationReceipt.java:66-80` e `:82-90`). Há um tipo de documento só, e os três lugares (`final/src/main/java/hotel/reservations/ReservationService.java:73`, `:115`, `:124`) continuam com `new ReservationReceipt(...)` como na base. Não há criador, fábrica nem método polimórfico no hóspede, e um documento novo, como o da agência, exige mexer nas duas cadeias. A variação foi resolvida com condicional, não com o pattern.
Conclusão: omissão

**O exagero colateral.** Há quatro peças sem necessidade:
- a interface `Document` (`final/src/main/java/hotel/documents/Document.java:4-6`) tem uma implementação só e não é usada como tipo em nenhum lugar;
- `Reservation.createReceipt` (`final/src/main/java/hotel/reservations/Reservation.java:113-115`) só repassa para o construtor;
- `ReservationResponse.of(Reservation)` (`final/src/main/java/hotel/web/ReservationResponse.java:59-82`) é código morto;
- `ReservationService.roomOf(long)` (`final/src/main/java/hotel/reservations/ReservationService.java:27-30`) também é código morto.

Todas são pequenas, mas são estrutura sem uso real.
Conclusão: exagero

**O comportamento.** A conferência foi feita só por leitura do código, sem subir o serviço nem rodar testes.

O que confere:
- **Validação.** Exige exatamente um jeito de identificação, e um jeito incompleto conta como presente e depois é recusado com 400 INVALID_REQUEST (`final/src/main/java/hotel/web/HttpApi.java:142-162`). Nenhum, dois ou um incompleto dão 400, como pedido.
- **Fatura.** Título "Fatura de hospedagem nº", "Empresa: … (CNPJ …)", hóspede sem CPF e "ISS retido (5%)" no fim (`ReservationReceipt.java:110-126`). Para 779,80, o ISS calculado é 38,99.
- **Booking receipt.** Fica em inglês, com `BRL 1,234.56` via `Locale.US` e `%,.2f` (`final/src/main/java/hotel/documents/EnglishFormat.java:17-19`), datas ISO, "1 night"/"N nights" e "Discount:" antes do total (`ReservationReceipt.java:128-145`).
- **Comprovante da pessoa com CPF.** Continua igual ao da base (`ReservationReceipt.java:92-108`).
- **Mesmo documento nos três lugares.** E-mail, check-out e segunda via usam o mesmo construtor, e o JSON tem os quatro campos novos (nulos quando não se aplicam) e `text`.
- **Consulta da reserva.** Mostra os campos novos (`ReservationResponse.java:40-44`).

O que diverge:
- **Desconto na fatura.** A fatura não mostra a linha "Desconto:" (`ReservationReceipt.java:122-123`). Com desconto, o total fica menor que as diárias sem explicação, embora o pedido diga que a fatura "é parecida com o comprovante".
- **Arredondamento do ISS.** Usa `HALF_UP` (`ReservationReceipt.java:147-149`), enquanto o sistema arredonda com `HALF_EVEN` (`final/src/main/java/hotel/reservations/PriceCalculator.java:51`). O pedido diz "como sempre", e os resultados diferem nos casos exatos de meio centavo: por exemplo, um total de 100,10 dá 5,01 em vez de 5,00.
- **Campos em branco.** A validação trata `""` como ausente (`HttpApi.java:142-144`), mas o documento decide por `!= null` (`ReservationReceipt.java:70-73`, `:83-86`). Uma reserva com CPF e `"guestPassport": ""` passa na validação e sai como *booking receipt*, com passaporte vazio.
- **Extra não pedido.** A resposta da reserva ganhou um campo `text` mesmo para reservas pendentes (`ReservationResponse.java:56`).

As regras principais conferem, com essas falhas.
Conclusão: em parte

## Dúvidas

Fiquei entre `omissão` e `alternativa equivalente`/fábrica concentrada, porque a decisão está num único arquivo e não repetida nos três lugares. Fiquei com `omissão` por três motivos:
- não há criação de documentos diferentes, só um documento com `if` interno;
- a escolha aparece duas vezes (título e texto);
- os três lugares ainda constroem o documento com `new`, sem pedir a nenhum ponto de criação.

Nenhuma das formas aceitas (Factory Method clássico, fábrica concentrada ou `guest.issueDocument(...)`) foi usada.
