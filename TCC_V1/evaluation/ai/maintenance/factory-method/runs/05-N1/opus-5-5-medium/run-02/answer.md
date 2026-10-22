Já li o pedido, o patch e os arquivos da `final/` que ele toca. Segue a correção.

# Cenário 05

## Visão geral

O agente juntou num lugar só a escolha de qual documento emitir. O construtor `ReservationReceipt(Reservation, Room)` chama um método privado, `generateText`, que olha qual identificação o hóspede tem e escolhe entre comprovante, fatura e *booking receipt* (`final/src/main/java/hotel/documents/ReservationReceipt.java:51-94`). Os três lugares do `ReservationService` continuam com o mesmo `new ReservationReceipt(...)` da base e não decidem nada (`final/src/main/java/hotel/reservations/ReservationService.java:68,110,119`). Não há `if` sobre o tipo de hóspede repetido no serviço. Por isso é a forma "fábrica concentrada", só que na versão mais crua. Não existe um tipo de hóspede nem um tipo por documento: há um registro único com os campos dos três documentos somados, e o caso é deduzido de qual campo não é nulo. O problema mais sério está no comportamento. Uma reserva com CPF e uma identificação incompleta (só o CNPJ, sem a razão social) é aceita, e o CNPJ é jogado fora sem aviso. Além disso, o campo `title` do JSON continua "Comprovante de reserva" na fatura e no *booking receipt*.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin com estes pacotes:
- `hotel.web`: `HttpApi` com as rotas, mais os DTOs `CreateReservationRequest` e `ReservationResponse`.
- `hotel.reservations`: `ReservationService` com as regras de estado (criar, confirmar, cancelar, check-in, check-out, segunda via), `PriceCalculator`, `ReservationStore` em memória e os registros `Guest`, `NewReservation` e `Reservation`.
- `hotel.documents`: `ReservationReceipt` e `TextFormat`.
- `hotel.messages`: `EmailSender` e `SentMessages`, um envio simulado que só guarda as mensagens.
- `hotel.rooms` e `hotel.reports`.

Uma requisição `POST /reservations` passa por `HttpApi.readNewReservation`, que valida e monta o `Guest`, e segue para `ReservationService.create`, que calcula o preço e grava no `ReservationStore`. O sistema é montado em `Hotel.java`, que não mudou.

O agente mudou pouco:
- `Guest` ganhou quatro campos que podem ser nulos: `companyCnpj`, `companyName`, `guestPassport` e `guestCountry` (`final/src/main/java/hotel/reservations/Guest.java:3-10`).
- `CreateReservationRequest` e `ReservationResponse` ganharam os mesmos quatro campos.
- A validação conta quantas identificações completas vieram (`final/src/main/java/hotel/web/HttpApi.java:127-135`).
- `TextFormat` ganhou `moneyEnglish` e `dateEnglish`.

Nos testes, o agente escreveu quatro casos no `ReservationServiceTest` (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:155,169,185,205`), sempre pela segunda via, e quatro no `HttpApiTest`: nenhuma identificação, duas identificações, empresa e passaporte.

**A parte que pede o pattern.** Há uma só classe de documento, o record `ReservationReceipt`. Ele tem os campos de identificação dos três casos e um novo componente `text` (`final/src/main/java/hotel/documents/ReservationReceipt.java:11-28`). O construtor de conveniência é o único lugar de criação e calcula o `text` chamando `generateText` (`:48`).

`generateText` é uma cadeia de `if`/`else if` sobre qual campo do hóspede não é nulo: CPF (`:52`), CNPJ (`:65`) e passaporte (`:79`). Se nenhum vier, devolve `""` (`:94`). Cada ramo chama um gerador de texto privado:
- `generateReceipt` (`:97`) produz o mesmo texto da base: "Comprovante de reserva nº" (`:110`).
- `generateInvoice` (`:124`) calcula o ISS em `:137`.
- `generateBookingReceipt` (`:155`).

O serviço não mudou nada na criação: `confirm`, `checkOut` e `receiptCopy` dão `new ReservationReceipt(reservation, roomOf(reservation))`. O e-mail usa `receipt.text()` (`final/src/main/java/hotel/reservations/ReservationService.java:126-131`).

**O raciocínio.** O critério do cenário é que a escolha de qual documento criar more num lugar só e que os três lugares peçam o documento a esse lugar. Isso foi cumprido. A decisão está só em `generateText`, e os três pontos do serviço passam por ela sem perguntar qual é o caso. As instruções aceitam explicitamente "um método que escolhe o documento pelo tipo de hóspede", e é o que se tem. Não é omissão, porque o `if` não se repete nos três lugares. Não é malfeito, porque nenhum dos três lugares monta o documento à mão e a escolha não acontece depois, fora do construtor.

O que afasta o desenho do pattern clássico:
- Não há tipos de documento: é um registro único com todos os campos.
- Não há tipo de hóspede: o caso é deduzido pela nulidade dos campos.
- A agência de turismo vai exigir mexer no record, no `Guest` e na cadeia de `if`.

Isso torna a solução frágil, mas a escolha continua concentrada. Por isso a classe é `acerto`, com a ressalva nas dúvidas.

Não há exagero colateral: nenhuma interface, fábrica ou camada nova. No comportamento, os textos e os valores conferem, mas a validação deixa passar o caso "CPF mais identificação incompleta" e o `title` ficou fixo.

## Item a item

**O pattern pedido.** A criação está concentrada no construtor de `ReservationReceipt`. Ele delega a `generateText` (`final/src/main/java/hotel/documents/ReservationReceipt.java:48,51-94`), que escolhe entre comprovante, fatura e *booking receipt* conforme a identificação do hóspede. Os três lugares (`final/src/main/java/hotel/reservations/ReservationService.java:68,110,119`) pedem o documento a esse ponto único e não têm nenhuma condição sobre o tipo de hóspede. É a forma "fábrica concentrada / método que escolhe pelo tipo", que as instruções aceitam como acerto. É a versão mínima: um só tipo de documento, e o caso deduzido de campos nulos.
Conclusão: acerto

**O exagero colateral.** Não há. Não foi criada nenhuma interface, fábrica, builder ou camada de repasse. As mudanças fora do documento são campos novos e dois formatadores em `TextFormat` (`final/src/main/java/hotel/documents/TextFormat.java:27-33`), todos usados.
Conclusão: contenção

**O comportamento.** A conferência foi feita só por leitura do código, sem subir o serviço nem rodar o Maven.

O que confere:
- **Comprovante do CPF:** idêntico ao da base (`ReservationReceipt.java:110-121` contra `base/src/main/java/hotel/documents/ReservationReceipt.java:42-53`).
- **Fatura:** "Fatura de hospedagem nº", a linha "Empresa: … (CNPJ …)", o hóspede sem CPF e "ISS retido (5%)" calculado com `HALF_UP` sobre o total (`:137-151`). 779,80 × 0,05 = 38,99, que confere.
- ***Booking receipt*:** "Booking receipt no.", "Guest: … (passport …, …)", datas ISO, "1 night"/"N nights", `BRL` com `Locale.US` e `%,.2f`, o que dá `BRL 1,234.56`, e a linha "Discount:" antes do total (`:169-182`, `TextFormat.java:27-33`).
- **Mesmo documento nos três lugares:** o e-mail continua em português (`ReservationService.java:126-131`).
- **JSON:** passa a trazer `text` e os quatro campos novos, com nulo nos que não se aplicam.
- **Validação:** nenhuma identificação ou duas completas dão 400 INVALID_REQUEST (`HttpApi.java:131-135`).

O que não confere:
- **Identificação incompleta junto com CPF:** a validação só conta as identificações completas (`HttpApi.java:128-129`). Com `guestCpf` e só `companyCnpj`, sem `companyName`, a contagem dá 1 e a reserva é aceita, com o CNPJ descartado em silêncio (`:139-145`). O pedido manda recusar quando não for "só um" desses jeitos. Uma identificação incompleta sozinha é recusada corretamente.
- **Campo `title`:** continua fixo em "Comprovante de reserva" para todos os documentos (`ReservationReceipt.java:32`). No JSON, a fatura e o *booking receipt* saem com um título que contradiz o `text`.

Conclusão: em parte

## Dúvidas

A forma usada foi a de fábrica concentrada: um método privado, `generateText`, chamado pelo único construtor do documento. Não foi o Factory Method clássico nem um método polimórfico no hóspede.

A dúvida é entre `acerto` e `malfeito`. A fábrica devolve sempre o mesmo tipo (`ReservationReceipt`) e só o texto varia, o que lembra "devolve sempre o mesmo documento". Mas a escolha acontece dentro da criação, não depois e fora dela, então a evidência sustenta melhor `acerto`. A fragilidade do desenho (o caso deduzido pela nulidade, um registro único com todos os campos, nada pronto para a agência) pesa contra a qualidade, mas não muda a classe pelos critérios dados.

Também não está claro se o `title` fixo conta como "campo de hoje" que deveria acompanhar o documento. Registrei como desvio de comportamento menor.
