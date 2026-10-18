# Cenário 05

## Visão geral

O agente não concentrou a criação do documento. Continua existindo um único tipo, o record `ReservationReceipt`. Ele ganhou os campos dos três jeitos de identificar o hóspede e um campo `guestType`, e decide por dentro, com dois `switch` sobre esse campo, qual título usar e qual texto montar. Os três lugares que emitem o documento ficaram iguais aos da base, com `new ReservationReceipt(...)`, e todos recebem o mesmo objeto. Nada escolhe qual documento criar: o objeto é sempre o mesmo e só muda de forma conforme o tipo. Para pôr a agência, o próximo passo já anunciado no pedido, seria preciso mais um valor no enum, mais um `case` em cada `switch`, mais um método de texto, mais campos no record e mais um ramo em `Guest.type()` e no `if` do `HttpApi`. O comportamento está quase todo certo: a fatura, o *booking receipt*, o comprovante de CPF, o e-mail, o check-out e a segunda via conferem. Mas uma identificação incompleta enviada junto com um CPF é aceita e descartada sem aviso, em vez de dar 400.

## Item a item

**O pedido (pede o pattern).** A escolha do documento não mora numa fábrica nem num criador. Ela fica num código de tipo dentro do próprio documento:
- `final/src/main/java/hotel/reservations/GuestType.java:3-7` cria o enum `INDIVIDUAL/COMPANY/FOREIGN`.
- `final/src/main/java/hotel/reservations/Guest.java:16-24` deduz o tipo com uma cadeia de `if` sobre os campos nulos.
- O record grava esse tipo (`final/src/main/java/hotel/documents/ReservationReceipt.java:32` e `:52`) e faz `switch` sobre ele duas vezes: em `titleForGuestType` (`ReservationReceipt.java:55-61`) e em `text()` (`ReservationReceipt.java:63-70`). Esse segundo `switch` despacha para `individualReceipt`, `companyInvoice` e `foreignReceipt` (`ReservationReceipt.java:72-123`).

Os três lugares de emissão continuam com o `new` direto, sem mudança nenhuma em relação à base: `final/src/main/java/hotel/reservations/ReservationService.java:68` (confirmação por e-mail), `:110` (check-out) e `:119` (segunda via). O arquivo é idêntico ao de `base/`. Nada decide "qual documento criar": todo mundo cria o mesmo `ReservationReceipt`, e a variação acontece depois, num `case` por tipo dentro dele. O mesmo desenho se repete em `HttpApi.java:139-160`, onde a montagem do `Guest` é mais uma cadeia de `if` por tipo. É a solução por "mais um `case`", não uma fábrica concentrada nem um método polimórfico no tipo de hóspede. Pelo menos o `switch` não ficou espalhado nos três lugares de emissão.
Conclusão: omissão

**O exagero colateral.** Não há. O enum `GuestType` e o construtor de conveniência `Guest(name, email, cpf)` (`final/src/main/java/hotel/reservations/Guest.java:12-14`) estão dentro do que o pedido mexe. Não surgiu interface com uma implementação só, nem fábrica, builder ou camada nova em outra parte do código.
Conclusão: contenção

**O comportamento.** Testes feitos numa cópia da `final/` (`mvn -q verify`: 30 testes, 0 falhas, 0 erros; 4 deles são novos do agente, todos em `HttpApiTest`). O serviço rodou na porta 7070:
- **Empresa:** `POST /reservations` com `companyCnpj` e `companyName`, quarto 201, de 10/03 a 12/03/2027. Deu 201, `guestCpf`, `guestPassport` e `guestCountry` vêm null, e o total é 779,80.
- **Fatura:** o check-out devolve "Fatura de hospedagem nº 1 / Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90) / Hóspede: Ana Souza / … / ISS retido (5%): R$ 38,99". É exatamente o exemplo do pedido.
- **Estrangeiro:** "Booking receipt no. 2 / Guest: John Smith (passport X1234567, United Kingdom) / Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights / Nights: BRL 779.80". Com uma noite, plano `NON_REFUNDABLE` e alta temporada sai "1 night", com "Discount: BRL 104.38" antes do total. Com três noites na suíte, o separador de milhar aparece: "BRL 1,739.70".
- **CPF:** recebe o "Comprovante de reserva" igual ao de antes.
- **Os três lugares:** o texto do check-out é idêntico ao de `GET /reservations/{id}/document` nas quatro reservas. Em `/messages`, o e-mail continua em português e leva o documento certo de cada tipo. O JSON do documento tem os campos novos e `text`. Também tem um `guestType` a mais, que não foi pedido mas não atrapalha.
- **Recusas:** sem nenhuma identificação, CPF mais passaporte completo, só `companyCnpj` e só `guestPassport` dão todos 400 `INVALID_REQUEST`.
- **Divergência:** com `"guestCpf":"1","companyCnpj":"12"` (CPF mais uma identificação de empresa incompleta), eu esperava 400, porque o pedido diz "tem que ser um desses jeitos, e só um". Veio 201, e o CNPJ foi descartado sem aviso (`HttpApi.java:128-133` só conta as identificações completas). O mesmo acontece com `guestCpf` mais `guestCountry`.
- **O que não mudou continua igual:** a capacidade (3 hóspedes no quarto 101 dá 422 `CAPACITY_EXCEEDED`), o cancelamento `STANDARD` com reembolso integral, os relatórios de receita e de ocupação, e a segunda via de reserva pendente (409).

O serviço foi derrubado no fim.
Conclusão: em parte

## Dúvidas

A dúvida principal é entre `omissão` e `malfeito`:
- **A favor de `malfeito`:** a regra de `malfeito` fala em fábrica que "devolve sempre o mesmo documento e a escolha acontece depois". Dá para ler o construtor `ReservationReceipt(Reservation, Room)` como um ponto único de criação, com a escolha feita depois, em `text()`.
- **Por que ficou `omissão`:** esse construtor já existia na base, o agente não criou nenhuma estrutura de criação, e a variação foi resolvida com `case` por tipo. Nenhuma das formas aceitas como `acerto` foi usada: nem criador com método de fábrica, nem fábrica concentrada, nem `guest.issueDocument(...)`.

No comportamento, considerei a aceitação de CPF mais identificação incompleta como divergência da regra "só um jeito". O pedido não fala desse caso explicitamente, e por isso a conclusão ficou `em parte`, e não `não segue`.
