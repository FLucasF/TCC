# Cenário 05

## Visão geral

O agente não mexeu no `ReservationService`: os três lugares continuam criando o comprovante com `new ReservationReceipt(reservation, roomOf(reservation))`. A escolha do documento foi para dentro do próprio `ReservationReceipt`. O construtor chama dois `switch` sobre o tipo de hóspede (`GuestType`): um escolhe o título e o outro escolhe o texto. Os dois ficam no mesmo arquivo e não se repetem nos pontos de uso. A decisão de qual documento criar fica, portanto, num lugar só, e os três lugares passam por ele. É uma forma fraca do pattern: há um único tipo de documento cujo construtor escolhe o texto, e a variação está dividida em dois `switch` paralelos. Mas cabe no que as instruções aceitam como fábrica concentrada. O comportamento lido no código confere com o pedido. Há uma brecha pequena: um CPF somado a um CNPJ sem razão social é aceito, e o CNPJ some sem aviso.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. O `hotel.web.HttpApi` registra as rotas (`final/src/main/java/hotel/web/HttpApi.java:61-91`), lê o corpo da requisição para um `CreateReservationRequest` e o converte num `NewReservation`, que vai para o `hotel.reservations.ReservationService`. O serviço cuida do ciclo de vida da reserva: criar, confirmar, cancelar, fazer check-in, fazer check-out e emitir a segunda via. Os dados ficam num `ReservationStore`. O preço vem do `PriceCalculator`, e o e-mail sai por um `EmailSender`, cujas mensagens podem ser consultadas em `/messages`. O documento é o record `hotel.documents.ReservationReceipt`, que o Javalin serializa como JSON nas rotas de check-out (`HttpApi.java:76-77`) e de segunda via (`HttpApi.java:78-79`). Na confirmação, o texto dele vai no corpo do e-mail (`ReservationService.java:126-132`).

O agente fez estas mudanças:
- criou o enum `GuestType` (INDIVIDUAL, COMPANY, FOREIGN);
- ampliou o record `Guest` com CNPJ, razão social, passaporte e país, com construtores para cada caso e um `type()` que deduz o tipo pelos campos não nulos (`final/src/main/java/hotel/reservations/Guest.java:24-32`);
- criou o `EnglishFormat`, para formatar dinheiro e datas em inglês;
- tornou público o `PriceCalculator.cents`;
- acrescentou os campos novos ao `CreateReservationRequest` e ao `ReservationResponse`;
- escreveu a validação "exatamente um jeito de identificar" no `HttpApi.createGuest` (`HttpApi.java:132-152`);
- acrescentou três testes de API: reserva de empresa, reserva de estrangeiro e as recusas (`final/src/test/java/hotel/web/HttpApiTest.java`, a partir da linha 120);
- deixou um `test_documents.sh` manual, que sobe o jar e usa curl.

**A parte do pattern.** O `ReservationService` continua igual ao da base, com `new ReservationReceipt(...)` em `confirm` (`ReservationService.java:68`), em `checkOut` (`:110`) e em `receiptCopy` (`:119`). Quem decide é o construtor do record (`final/src/main/java/hotel/documents/ReservationReceipt.java:32-51`):
- o `titleFor` (`:53-59`) escolhe o título;
- o `buildText` (`:61-68`) escolhe o texto entre `buildIndividualReceipt` (`:70-86`), `buildCompanyInvoice` (`:88-107`, com o ISS na linha 90) e `buildForeignReceipt` (`:109-126`).

Cada caso é um valor do enum, e o caso é escolhido pelo `Guest.type()`. As regras de cada documento estão em métodos estáticos privados da mesma classe. O texto deixou de ser calculado sob demanda (`text()` na base) e passou a ser um componente do record, por isso aparece no JSON.

**O raciocínio.**

O que aproxima do pattern:
- a decisão de qual documento criar está num único lugar;
- os três pontos de uso não perguntam o tipo do hóspede e recebem o documento certo;
- o caso da base (CPF) também passa pela mesma escolha (`ReservationReceipt.java:64`), e não ficou um caminho à parte.

As instruções contam como omissão o `switch` repetido nos três lugares, e isso não acontece aqui. Contam como malfeito um dos lugares criar à mão, ou a escolha acontecer depois, fora da fábrica, e isso também não acontece.

O que afasta do pattern:
- não há uma família de documentos, só um record com o texto já pronto;
- a variação está em dois `switch` paralelos sobre o mesmo enum (título e texto);
- o tipo é deduzido por checagem de nulos no `Guest` (`Guest.java:24-32`).

Uma agência exigiria mexer no enum, no `Guest.type()` e nos dois `switch`. Os dois `switch` estão no mesmo arquivo e são exaustivos, então o compilador avisa se faltar um caso. Pesando tudo, a escolha está concentrada: é acerto, na forma de fábrica concentrada.

No exagero colateral, nada pesou. O `GuestType` é usado nos dois `switch`, e o `EnglishFormat` espelha o `TextFormat`, que já existia, com a mesma necessidade.

No comportamento, o que pesou foi a validação no `HttpApi.java:133-139`. Ela só conta um jeito quando ele está completo. Por isso, um CPF acompanhado de um CNPJ sem razão social é aceito como CPF, e o CNPJ é descartado.

## Item a item

**O pattern pedido.** A escolha do documento fica num único método, chamado pelo construtor do `ReservationReceipt`: o `switch` em `final/src/main/java/hotel/documents/ReservationReceipt.java:61-68`, junto com o `titleFor` em `:53-59`. Os três lugares (`ReservationService.java:68`, `:110` e `:119`) pedem o documento a esse construtor e não tomam nenhuma decisão. O caso da base também passa por ali (`:64`). Isso é uma fábrica concentrada que escolhe pelo tipo de hóspede, forma que as instruções aceitam como acerto. É uma forma fraca: um único tipo de documento, a variação dividida em dois `switch` no mesmo arquivo, e nenhum objeto por documento. Mas não há `switch` repetido nos pontos de uso e nenhum lugar cria o documento à mão.
Conclusão: acerto

**O exagero colateral.** Não há. O `GuestType` (`final/src/main/java/hotel/reservations/GuestType.java:3-7`) é o discriminante dos `switch`. O `EnglishFormat` (`final/src/main/java/hotel/documents/EnglishFormat.java:9-24`) é a contraparte do `TextFormat`, que já existia. Os construtores extras do `Guest` (`Guest.java:12-22`) são só conveniência e não criam camadas. O `test_documents.sh` é um script de teste, não estrutura.
Conclusão: contenção

**O comportamento.** A conferência foi feita só por leitura do código, sem subir o serviço nem rodar o Maven.

| O que se conferiu | Onde | O que se esperava | O que o código faz |
|---|---|---|---|
| Identificação do hóspede | `HttpApi.java:133-143` | 400 para nenhum jeito, dois jeitos completos ou um incompleto sozinho | Recusa com 400 e INVALID_REQUEST |
| Brecha da identificação | `HttpApi.java:133-145` | Recusar CPF com CNPJ sem razão social | Aceita como CPF e descarta o CNPJ, sem 400 |
| Fatura | `ReservationReceipt.java:90-105` | Empresa no lugar do CPF; ISS retido de 5% | Mostra "Empresa: … (CNPJ …)" e depois "Hóspede"; ISS = `cents(total × 0,05)`; 779,80 dá R$ 38,99 |
| *Booking receipt* | `ReservationReceipt.java:111-124`, `EnglishFormat.java:17-23` | Texto em inglês, `BRL 1,234.56`, datas `2027-03-10`, "1 night"/"N nights", linha "Discount:" antes do total | Confere, com dinheiro em `%,.2f` no `Locale.US` e datas em ISO |
| Comprovante de quem tem CPF | `ReservationReceipt.java:70-86` | Igual ao da base | Mesmo texto do `text()` da base (`base/src/main/java/hotel/documents/ReservationReceipt.java:40-55`) |
| Os três lugares | `ReservationService.java:68`, `:110`, `:119` e `:129` | Mesmo documento no e-mail de confirmação, no check-out e na segunda via | Os três usam o mesmo construtor; o e-mail usa o `text()` |
| JSON do documento | `ReservationReceipt.java:13-30` | Campo `text`, os quatro campos novos e os de identificação que não se aplicam como null | Confere |
| Consulta da reserva | `ReservationResponse` | Os campos novos aparecem, com null nos que não se aplicam | Confere |

Os relatórios não usam o CPF, então o resto do sistema não foi afetado.
Conclusão: segue

## Dúvidas

- **A forma usada.** É uma fábrica concentrada, mas dentro do construtor do próprio documento: um único record com `switch` no título e no texto (`ReservationReceipt.java:53-68`). Não é o Factory Method clássico nem um método polimórfico no hóspede. Ela fica perto da fronteira com a omissão, porque a variação é resolvida com `switch`. Classifiquei como acerto porque a regra do cenário julga onde se decide qual documento criar, e essa decisão está num lugar só, sem repetição nos três pontos de uso.
- **O CPF com CNPJ incompleto.** A requisição com CPF e CNPJ sem razão social é aceita, e o CNPJ é descartado (`HttpApi.java:133-145`). Uma leitura estrita do "só um" pediria 400 nesse caso. Como os casos explícitos (nenhum, dois completos, um incompleto sozinho) estão certos, mantive "segue", mas o caso fica anotado.
