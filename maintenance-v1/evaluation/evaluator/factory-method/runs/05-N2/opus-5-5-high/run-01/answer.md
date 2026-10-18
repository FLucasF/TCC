# Cenário 05

## Visão geral

O agente transformou o hóspede num tipo fechado (`sealed`) com três casos e criou um documento para cada um. A escolha de qual documento criar fica num lugar só: um `switch` sobre o tipo de hóspede em `ReservationDocument.create`. Os três lugares que já criavam o comprovante (confirmação, check-out e segunda via) passam por esse método sem mudar uma linha, porque o construtor de `ReservationReceipt` chama a fábrica. A pessoa com CPF recebe o mesmo comprovante de antes, a fatura e o *booking receipt* saem como o pedido descreve, e o `mvn verify` passa. O desenho tem um ponto fraco: para montar o JSON, `ReservationReceipt` pergunta qual é o documento concreto para preencher os campos de identificação. O problema mais sério está no comportamento: quando uma identificação completa vem junto com uma incompleta, a reserva é aceita e os campos a mais são descartados sem aviso, em vez de dar 400.

## Item a item

**O pedido (pede o pattern).** A decisão de qual documento criar mora num só método, `final/src/main/java/hotel/documents/ReservationDocument.java:30-38`. É um `switch` sobre `reservation.guest()` que devolve `GuestReceipt`, `CorporateInvoice` ou `BookingReceipt`. Como `Guest` é `sealed` (`final/src/main/java/hotel/reservations/Guest.java:4`), o compilador obriga a tratar todos os casos. Na base, os três lugares faziam `new ReservationReceipt(reservation, roomOf(reservation))` (`base/src/main/java/hotel/reservations/ReservationService.java:68`, `:110`, `:119`), e na final continuam idênticos (`final/src/main/java/hotel/reservations/ReservationService.java:68`, `:110`, `:119`). A diferença é que esse construtor agora só repassa para a fábrica (`final/src/main/java/hotel/documents/ReservationReceipt.java:33-35`). Portanto nenhum dos três lugares escolhe o documento nem o cria à mão, e o `if` sobre o tipo de hóspede não se repete. O comprovante de antes também passou pela estrutura nova, como `GuestReceipt` (`final/src/main/java/hotel/documents/GuestReceipt.java:25-56`), e o texto de cada documento fica no próprio documento (`text()` em `CorporateInvoice.java:51-67` e `BookingReceipt.java:44-60`). Quando a agência chegar, basta um novo caso de `Guest`, um documento novo e uma linha no `switch`. Há uma ressalva: o objeto que vai para o JSON pergunta o tipo concreto do documento para preencher CPF, CNPJ e passaporte (`final/src/main/java/hotel/documents/ReservationReceipt.java:42-46`). Isso é só mapeamento de campos para a saída. O título, os valores e o texto vêm do documento sem perguntar o tipo, e a escolha de qual documento criar não acontece ali.
Conclusão: acerto

**O exagero colateral.** Não há. A hierarquia `Guest` → `IndividualGuest`/`CorporateGuest`/`ForeignGuest` (`final/src/main/java/hotel/reservations/Guest.java:4-7`, `IndividualGuest.java:4`, `CorporateGuest.java:4`, `ForeignGuest.java:4`) é a variação que o próprio pedido trouxe. `EnglishTextFormat` (`final/src/main/java/hotel/documents/EnglishTextFormat.java:9-24`) atende ao formato em inglês que foi pedido. Fora disso, só mudou a visibilidade de `PriceCalculator.cents`, que virou `public` para o cálculo do ISS (`final/src/main/java/hotel/reservations/PriceCalculator.java:50`). Não há interface com implementação única, fábrica, builder nem camada nova fora do que o pedido tocava.
Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp/ev05`, com o serviço na porta 7070.
- **O que confere:**
  - A reserva com CPF (201, de 10 a 12/03/2027) recebeu no e-mail e no check-out o mesmo comprovante de antes. O JSON veio com `companyCnpj`, `companyName`, `guestPassport` e `guestCountry` nulos e com o campo `text`.
  - A reserva de empresa (202, mesmas datas) gerou a "Fatura de hospedagem nº 2" com "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza", "Total: R$ 779,80" e "ISS retido (5%): R$ 38,99". O texto saiu igual no e-mail e no `GET /reservations/2/document`, com `guestCpf` nulo.
  - O estrangeiro com uma noite e `NON_REFUNDABLE` recebeu "Booking receipt no. 3", "Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night", "Discount: BRL 37.48" e "Total: BRL 212.42". O texto foi o mesmo no e-mail, no check-out e na segunda via.
  - Na suíte com 4 noites em julho, veio "4 nights" e "BRL 2,783.52". O e-mail continua em português em volta do documento.
  - A consulta da reserva mostra os campos novos, com null nos que não se aplicam.
  - Deram 400 `INVALID_REQUEST`: nenhuma identificação; CPF junto com CNPJ e razão social; CNPJ com razão social junto com passaporte e país; CNPJ sozinho; passaporte sozinho.
  - O que o pedido não mudou continua igual: capacidade estourada (422), quarto ocupado (409), saída antes da entrada (400), falta de e-mail (400), segunda via de reserva pendente (409) e os relatórios de receita e de ocupação.
- **O que não confere:** uma identificação completa junto com um pedaço de outra é aceita.
  - `POST /reservations` com `guestCpf` e `companyCnpj`, sem `companyName`, devia dar 400 e deu 201: virou pessoa com CPF e o CNPJ foi descartado.
  - O mesmo aconteceu com `guestCpf` mais `guestPassport` sem país (201) e com o CNPJ completo mais `guestCountry` solto (201).
  - A causa é que só identificações completas entram na contagem (`final/src/main/java/hotel/web/HttpApi.java:132-137`).
- **Testes do agente:** o `mvn -q verify` passou com 29 testes e nenhuma falha (PriceCalculatorTest 6, ReservationServiceTest 11, ReportsTest 5, HttpApiTest 7). Desses, 3 são novos do agente, todos em `HttpApiTest`: empresa, estrangeiro e recusa de nenhuma ou duas identificações. O serviço foi derrubado no fim.
Conclusão: em parte

## Dúvidas

- **Qual forma foi usada:** uma fábrica concentrada, o método estático `ReservationDocument.create`, com um `switch` sobre o tipo de hóspede `sealed`. Não é a forma clássica, com um criador sobrescrito por tipo, nem o método polimórfico `guest.issueDocument(...)`.
- **Por que não é malfeito:** os `instanceof` sobre o documento em `final/src/main/java/hotel/documents/ReservationReceipt.java:42-46` lembram "quem usa a estrutura ainda pergunta qual é o caso concreto". Também podem ser só o achatamento dos campos de identificação no JSON que o pedido exigiu, e o mesmo se repete em `final/src/main/java/hotel/web/ReservationResponse.java:40-44`. Fiquei com acerto porque a escolha e a criação do documento estão num lugar só, e os três lugares não decidem nada. Mesmo assim, quando a agência chegar, essas linhas também vão precisar mudar.
- **O que conta como "incompleto":** o pedido não diz como tratar uma identificação completa que vem junto com um pedaço de outra. Li "tem que ser um desses jeitos, e só um" como recusa, e por isso marquei o comportamento como "em parte".
