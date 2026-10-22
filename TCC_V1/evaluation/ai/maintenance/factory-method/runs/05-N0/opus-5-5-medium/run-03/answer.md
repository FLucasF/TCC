# Cenário 05

## Visão geral

O agente pôs a escolha do documento num lugar só. O construtor `ReservationReceipt(Reservation, Room)` olha o tipo do hóspede e escolhe o título e o texto por dois `switch`, os dois dentro da mesma classe. Os três lugares que emitem o documento (o e-mail de confirmação, o check-out e a segunda via) continuam chamando esse mesmo construtor e não sabem qual é o tipo de hóspede. Isso basta para ser uma fábrica concentrada, que as instruções aceitam como acerto, mas é a forma mais fraca delas: há um único record de documento, a variação mora num `switch` de enum e o tipo de hóspede vem de checagens de `null`. Os textos e os valores estão certos, e o comprovante da pessoa com CPF ficou igual ao da base. O problema mais sério está na validação da entrada. Ela conta só as identificações completas, então um pedido que manda o CPF junto com um CNPJ sem razão social (ou com um passaporte sem país) é aceito como CPF, quando deveria ser recusado.

## Como foi implementado, e por que o julgamento

**O projeto.** `hotel.Hotel` monta as peças à mão (`final/src/main/java/hotel/Hotel.java:19-27`): `Rooms`, `ReservationStore`, `PriceCalculator`, `EmailSender` com `SentMessages`, o `ReservationService` e o `HttpApi` (Javalin), que expõe as rotas. Os dados ficam em memória, num `LinkedHashMap` dentro de `ReservationStore` (`final/src/main/java/hotel/reservations/ReservationStore.java:9-12`). Uma requisição `POST /reservations` passa por `HttpApi.readNewReservation`, que lê o `CreateReservationRequest`, valida os campos, monta o `Guest` e o `NewReservation` e chama o `ReservationService`. O serviço calcula o preço e guarda a reserva, e depois cuida das transições (confirmar, check-in, check-out, cancelar). Os relatórios ficam em `hotel.reports`, e o texto e a formatação do documento em `hotel.documents`. O agente escreveu três testes de serviço (empresa, estrangeiro, CPF) em `ReservationServiceTest` e seis testes HTTP em `HttpApiTest`: criação de empresa e de estrangeiro com check-out, e recusas para nenhum, dois, só CNPJ e só passaporte. Os testes conferem trechos com `contains` e não o texto inteiro, nem o valor do ISS.

**A parte que pede o pattern.** O `Guest` ganhou os campos `companyCnpj`, `companyName`, `passport` e `country`. Ganhou também três métodos estáticos de criação (`withCpf`, `withCompany`, `withPassport`) e um `type()` que deduz o tipo pelos campos nulos, com o enum `GuestType` (`final/src/main/java/hotel/reservations/Guest.java:12-32`).

O documento continua sendo um único record, `ReservationReceipt`. Ele ganhou os quatro campos novos e o componente `text` (`final/src/main/java/hotel/documents/ReservationReceipt.java:12-29`). O construtor de criação (`:31-50`) escolhe o título por `titleFor` (`:52-58`) e o texto por `generateText` (`:60-67`), os dois por `switch (guest.type())`. Cada caso tem a sua função de texto:

- o comprovante da pessoa com CPF (`:69`);
- a fatura com o ISS (`:87-108`);
- o *booking receipt* (`:111-128`), que usa os novos `TextFormat.moneyEnglish` e `TextFormat.dateIso` (`final/src/main/java/hotel/documents/TextFormat.java:27-33`).

No `ReservationService`, os três lugares continuam como na base: `new ReservationReceipt(reservation, roomOf(reservation))` em `final/src/main/java/hotel/reservations/ReservationService.java:68` (confirmação), `:110` (check-out) e `:119` (segunda via). O e-mail usa `receipt.text()` (`:129`).

**O raciocínio.** O critério do cenário é onde se decide qual documento criar. Aqui a decisão está num ponto só, o construtor de `ReservationReceipt` com `generateText` e `titleFor`, e nenhum dos três lugares pergunta pelo tipo do hóspede nem monta o documento por conta própria. Isso descarta a omissão, que exige o `if` ou o `switch` repetido nos três lugares. Descarta também o malfeito, porque nenhum lugar fura a fábrica e a escolha não acontece depois dela. A forma cabe em "um método que escolhe o documento pelo tipo de hóspede", aceita como acerto.

O que afasta o desenho do Factory Method clássico:

- não há um tipo de documento por caso, e o "produto" que varia é só uma `String`;
- a escolha aparece em dois `switch` paralelos (título e texto), embora no mesmo arquivo;
- a criação está escondida num construtor de record de dados.

Para a agência de turismo será preciso mexer no enum, nos dois `switch`, no `Guest.type()` e acrescentar campos ao record, mas sempre nesses mesmos lugares e nunca nos três pontos de uso.

No exagero colateral, os três métodos estáticos do `Guest` são construtores com nome que evitam passar `null` à mão: não são uma camada nem uma fábrica sem uso. No comportamento, pesou o furo da validação descrito acima.

## Item a item

**O pattern pedido.** A escolha do documento está concentrada no construtor de `ReservationReceipt`, que decide o título e o texto por `switch (guest.type())` (`final/src/main/java/hotel/documents/ReservationReceipt.java:31-67`). Os três lugares, confirmação, check-out e segunda via, pedem o documento a esse único ponto sem saber o tipo de hóspede (`final/src/main/java/hotel/reservations/ReservationService.java:68`, `:110`, `:119`). Isso inclui a pessoa com CPF, o caso que já existia, que passa pelo mesmo caminho (`ReservationReceipt.java:63`). É a forma "fábrica concentrada" que as instruções aceitam. Não é a forma clássica: o documento é um tipo só e o que varia é a `String` do texto.
Conclusão: acerto

**O exagero colateral.** Não há. O `GuestType` e os métodos `Guest.withCpf/withCompany/withPassport` (`final/src/main/java/hotel/reservations/Guest.java:12-32`) são usados de fato: em `HttpApi`, nos testes e na escolha do documento. Também não foram criadas interfaces, builders ou camadas de repasse.
Conclusão: contenção

**O comportamento.** Conferi só pela leitura do código, sem subir o serviço nem rodar testes.

O que confere com o pedido:

- **Fatura.** Mostra "Fatura de hospedagem nº", "Empresa: <razão> (CNPJ …)" e "Hóspede:" sem CPF. O ISS é `total × 0,05` arredondado com `HALF_EVEN` em duas casas, o mesmo arredondamento do `PriceCalculator` da base (`base/src/main/java/hotel/reservations/PriceCalculator.java:51`). Para 779,80 isso dá exatamente 38,99, e a linha "ISS retido (5%): R$ 38,99" sai no fim (`ReservationReceipt.java:87-107`).
- **Booking receipt.** Está em inglês, com `BRL` e `Locale.US` (o que dá `BRL 1,234.56`), datas ISO e "1 night"/"N nights". A linha "Discount:" vem antes do total (`ReservationReceipt.java:111-127`, `TextFormat.java:27-33`).
- **Comprovante da pessoa com CPF.** O texto é idêntico ao da base (`ReservationReceipt.java:69-85` comparado com `base/src/main/java/hotel/documents/ReservationReceipt.java:40-54`).
- **Mesmo documento nos três lugares.** Os três chamam o mesmo construtor, e o e-mail continua em português com o documento dentro (`ReservationService.java:126-131`).
- **JSON.** `text` agora é componente do record, então aparece no JSON do documento. A consulta da reserva mostra os quatro campos novos, com `null` nos que não se aplicam (`final/src/main/java/hotel/web/ReservationResponse.java`).

O que não confere: a validação conta só as identificações completas (`final/src/main/java/hotel/web/HttpApi.java:127-132`).

- Esperava 400 para nenhuma identificação, e é isso que acontece.
- Esperava 400 para duas completas, e é isso que acontece.
- Esperava 400 para uma incompleta sozinha, e é isso que acontece.
- Esperava 400 para CPF mais `companyCnpj` sem `companyName`, mas o pedido é aceito como CPF e o CNPJ é descartado em silêncio. O mesmo vale para CPF mais passaporte sem país, ou empresa completa mais passaporte sem país. Isso contraria o "tem que ser um desses jeitos, e só um", e a mensagem antiga "every field of the reservation is required" continua sendo usada para os outros campos.
Conclusão: em parte

## Dúvidas

A forma usada foi a de fábrica concentrada, num construtor com `switch` sobre o tipo de hóspede. Não é a do criador com método sobrescrito nem a do método polimórfico no hóspede. A dúvida é se isso é acerto ou se é, no fundo, um `switch` de enum que só por acaso não se repete. As instruções põem a fronteira em "onde se decide qual criar", e aqui a decisão está num ponto só, por isso fiquei com acerto. Há porém dois `switch` paralelos na mesma classe e um único tipo de documento.

Também ficou em aberto se a aceitação de CPF junto com um CNPJ ou passaporte incompleto conta como "dois" ou como "incompleto". Pelo texto do pedido, as duas leituras pedem recusa, por isso marquei o comportamento como em parte.
