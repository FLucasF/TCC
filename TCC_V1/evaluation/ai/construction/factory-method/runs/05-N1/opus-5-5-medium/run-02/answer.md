# Cenário 05

## Visão geral

O agente resolveu a variação com um método polimórfico no próprio tipo de hóspede. A interface `GuestIdentity` tem três implementações, `CpfIdentity`, `CnpjIdentity` e `PassportIdentity`. Cada uma sabe montar o seu documento com `renderDocument(...)` e sabe o título dele com `documentTitle()`. Os três lugares onde o documento sai (o e-mail de confirmação, o check-out e a segunda via) passam por um único método, `ReservationService.documentText`, que só chama `r.identity().renderDocument(...)`. Não existe `if` nem `switch` sobre o tipo de hóspede em nenhum ponto do sistema. A escolha do tipo acontece uma vez, na criação da reserva, por um registro de formas de identificação. Para criar uma forma nova (a agência, por exemplo), basta uma classe nova e uma linha nesse registro. Não encontrei problema sério de desenho nem de comportamento.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `pousada` tem estas peças:
- `Main` lê a variável PORT e usa 7070 quando ela não vem.
- `App` monta o Javalin, define as rotas e converte o domínio em mapas para o JSON (`reservationView`, `documentView`, `messageView`).
- `ReservationRequest` valida o corpo do `POST /reservations`.
- `ReservationService` guarda as reservas numa lista em memória e aplica as regras de disponibilidade e de capacidade, as transições de estado, a segunda via e o e-mail.
- `Reservation` guarda o estado e tem as transições com `requireStatus`.
- `Quote` calcula as noites, a alta temporada, o desconto e o total.
- `RatePlan` é um enum com a taxa de desconto e a regra de reembolso.
- `RoomType`, `Room` e `RoomCatalog` são os quartos fixos.
- `Outbox` é a lista de mensagens.
- `Reports` gera os dois CSVs.
- `Money` arredonda com HALF_EVEN e formata nos jeitos brasileiro e inglês.

O pacote `pousada.identity` traz a parte nova.

O caminho de uma requisição: a rota em `App` chama `ReservationRequest.parse` ou um método do `ReservationService`, a regra mora em `Reservation`, `Quote` ou `RatePlan`, e a resposta volta pelas views de `App`. O `pom.xml` usa o shade plugin para gerar o jar executável. O `ApiTest` sobe o servidor e cobre a alta temporada, a fatura no e-mail com o ISS, o check-out com `guestCpf: null`, o *booking receipt* com desconto na segunda via, a recusa de duas identificações, o corpo inválido e o relatório financeiro com um não reembolsável cancelado.

**A parte que pede o pattern.**
- `GuestIdentity` (`identity/GuestIdentity.java:6-13`) declara `fields()`, `documentTitle()` e `renderDocument(DocumentData)`.
- Cada caso é um record que implementa a interface e monta o seu documento:
  - `CpfIdentity` produz o comprovante (`identity/CpfIdentity.java:23-26`).
  - `CnpjIdentity` produz a fatura, com as linhas da empresa e do hóspede e o rodapé do ISS de 5% arredondado (`identity/CnpjIdentity.java:11`, `identity/CnpjIdentity.java:27-33`).
  - `PassportIdentity` produz o *booking receipt* com o rótulo inglês (`identity/PassportIdentity.java:23-26`).
- O esqueleto comum das linhas fica em `DocumentLayout.render`, que recebe os rótulos (`PORTUGUESE` ou `ENGLISH`), as linhas de quem paga e as linhas de rodapé (`identity/DocumentLayout.java:22-30`, `identity/DocumentLayout.java:35-51`).
- O tipo é escolhido uma única vez, na criação da reserva. `IdentityKinds.parse` percorre a lista `ALL` (`identity/IdentityKinds.java:15-18`), e cada `IdentityKind` traz os nomes dos seus campos e a função que cria a identidade (`identity/CpfIdentity.java:9-10`, `identity/IdentityKind.java:7-10`).
- Os três lugares usam o mesmo método, `ReservationService.documentText` (`ReservationService.java:86-89`):
  - o e-mail em `confirmationBody` (`ReservationService.java:94`);
  - o check-out e a segunda via pelo `documentView` (`App.java:48-49`, `App.java:142`).
- O título do JSON vem de `r.identity().documentTitle()` (`App.java:130`). Os cinco campos de identificação, com null nos que não se aplicam, vêm de `IdentityKinds.toJsonFields` (`identity/IdentityKinds.java:29-36`).

**O raciocínio.** As instruções do pattern aceitam como acerto o método polimórfico no tipo de hóspede, e é isso que está no código. A decisão de qual documento criar mora no objeto de identidade. Ninguém de fora pergunta qual é o caso concreto: não há `instanceof`, `if` ou `switch` sobre o tipo em `App`, `ReservationService` ou `Reports`. Nenhum dos três lugares monta o documento à mão. O comprovante do CPF, que é o caso "que já existia", também passa pela estrutura, sem tratamento especial.

Pesei, no exagero colateral, o `IdentityKind` com a sua função de fábrica, o registro `IdentityKinds`, o `DocumentData` e o `DocumentLayout.Labels`. Todos ficam dentro da variação que o pattern resolve, e cada um tem mais de um uso: três tipos de identidade e dois conjuntos de rótulos. Por isso não contam como exagero.

No comportamento, a conferência foi feita por leitura do código, e as regras pedidas conferem.

## Item a item

**O pattern pedido.** `GuestIdentity.renderDocument` é implementado em `CpfIdentity` (`identity/CpfIdentity.java:23-26`), `CnpjIdentity` (`identity/CnpjIdentity.java:27-33`) e `PassportIdentity` (`identity/PassportIdentity.java:23-26`). Os três lugares passam por `ReservationService.documentText` (`ReservationService.java:86-89`), usado no e-mail (`ReservationService.java:94`) e no check-out e na segunda via (`App.java:48-49`, `App.java:142`). O tipo concreto é decidido uma vez só, na criação, pelo registro `IdentityKinds.ALL` (`identity/IdentityKinds.java:15-18`, `identity/IdentityKinds.java:41-60`). Não há condicional sobre o tipo de hóspede em nenhum dos três lugares. Para incluir a agência, basta uma classe nova e uma linha na lista. É o pattern na forma de método polimórfico no tipo de hóspede, que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. As peças auxiliares estão todas dentro da variação e têm mais de um uso. `IdentityKind` e `IdentityKinds` servem às três formas de identificação, tanto no parse quanto no JSON. `DocumentLayout` tem dois conjuntos de rótulos (`identity/DocumentLayout.java:22-30`). `DocumentData` é só o objeto de parâmetros do documento (`identity/DocumentData.java:8-10`). Fora dessa parte, o desenho é enxuto: o serviço é uma classe concreta, os enums `RatePlan` e `RoomType` guardam os dados, e não há interfaces, builders nem camadas que só repassam.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código: não subi o serviço nem rodei testes. Conferi estes pontos:
- **Identificação na criação.** `IdentityKinds.parse` recusa com 400 INVALID_REQUEST uma forma incompleta (`identity/IdentityKinds.java:52-54`) e também nenhuma ou mais de uma (`identity/IdentityKinds.java:57-59`).
- **Fatura.** Mostra "Empresa: … (CNPJ …)" e "Hóspede: …". O ISS é `Money.round(total × 0,05)` com HALF_EVEN, e 779,80 dá 38,99 (`identity/CnpjIdentity.java:28-32`, `Money.java:15-16`).
- **Booking receipt.** Usa "BRL " com `Money.english` (vírgula de milhar e ponto decimal), datas pelo `LocalDate::toString` (2027-03-10), "night" ou "nights" e "Discount" antes do total (`identity/DocumentLayout.java:27-30`, `identity/DocumentLayout.java:41-48`, `Money.java:25-27`).
- **Comprovante do CPF.** Sai igual ao modelo do enunciado: "nº", "Hóspede: … (CPF …)", dd/MM/yyyy, "diária" ou "diárias", desconto só quando é maior que zero (`identity/CpfIdentity.java:23-26`, `identity/DocumentLayout.java:22-25`).
- **Mesmo texto nos três lugares.** O e-mail, o check-out e a segunda via usam o mesmo `documentText`, e o JSON do documento tem os cinco campos e o `text` (`App.java:128-144`). O e-mail é sempre em português (`ReservationService.java:91-97`).
- **Segunda via.** Só sai para reserva paga, e nos outros casos devolve 409 (`ReservationService.java:78-84`).
- **Resto do sistema.**
  - A alta temporada é calculada em `Quote.java:25-29`.
  - Os arredondamentos por passo estão em `Quote.java:16-22`.
  - As transições de estado estão em `Reservation.java:37-57`.
  - O reembolso fica em `RatePlan.java:21-23`. Uma reserva pendente tem `amountPaid` zero, então não devolve nada.
  - O conflito de quarto ignora as canceladas (`ReservationService.java:28-31`).
  - Os relatórios estão em `Reports.java:13-43`.
  - Os códigos de erro estão em `ReservationRequest.java:24-29`, `ReservationService.java:22-33` e `App.java:85-92`.

Tudo confere com o pedido.
Conclusão: segue

## Dúvidas

A forma usada foi o método polimórfico no próprio tipo de hóspede (`guest.renderDocument(...)`), apoiado num registro com uma função de criação por tipo, que é usado só na leitura da requisição. Não há a forma clássica com um criador sobrescrito por tipo, nem uma fábrica de documentos separada. A conferência do comportamento foi só por leitura. Um ponto não foi conferido em execução: que o Jackson serializa os `BigDecimal` com escala 2 (por exemplo 779.80) como número com duas casas. Pela leitura, todos os valores saem com escala 2 (`Money.java:10`, `Money.java:15-16`, `RoomType.java:15`).
