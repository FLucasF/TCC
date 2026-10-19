# Cenário 01

## Visão geral

O agente resolveu o documento por tipo de hóspede com um método polimórfico no próprio tipo de hóspede. A interface `Identity` tem `render(DocumentData)` e `title()`, e há três implementações: `CpfIdentity`, `CnpjIdentity` e `PassportIdentity`. Um registro único (`IdentityTypes`) transforma o corpo da requisição na identidade certa. O texto do documento passa sempre pelo mesmo ponto, `Documents.text(r)`, que é usado no e-mail de confirmação, no check-out e na segunda via. Não há `if`, `switch` nem `instanceof` sobre o tipo de hóspede em nenhum lugar do código. O caso da pessoa com CPF, que já vinha da parte comum, também está na estrutura. O resto do sistema é enxuto: enums, records e classes utilitárias, sem camadas a mais. Tudo o que testei bate com o enunciado. Não encontrei nenhum problema sério.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo está num pacote só, `br.com.mealta.pousada`:
- **Entrada.** `Main` lê a porta da variável `PORT`, ou usa 7070 se ela não vier (`Main.java:5-6`). `App` registra as rotas do Javalin, interpreta e valida o corpo e monta os JSONs à mão com `LinkedHashMap` (`App.java:25-41`, `App.java:130-168`). Os erros saem como `DomainException`, que já carrega o status e o código, e um handler único os trata (`App.java:27`, `DomainException.java:4-23`).
- **Caminho de uma requisição.** A rota chama `ReservationService`, que guarda os quartos e as reservas em `TreeMap` na memória (`ReservationService.java:10-11`) e verifica quarto, capacidade e conflito de datas (`ReservationService.java:27-49`). Daí a chamada vai para `Reservation`, que guarda o estado e as transições (`Reservation.java:63-84`).
- **Regras.** O preço fica em `Pricing`, com a alta temporada e o desconto do plano (`Pricing.java:16-36`). O desconto e a devolução são dados do enum `RatePlan` (`RatePlan.java:6-24`). O arredondamento meio-para-o-par e a formatação do dinheiro ficam em `Money` (`Money.java:13-36`). Os CSVs saem de `Reports` (`Reports.java:12-40`), e os e-mails vão para a lista em memória `Outbox` (`Outbox.java:10-12`).
- **Build.** O maven-shade gera `target/pousada.jar`, com `Main` no manifesto (`pom.xml:53-63`).
- **Testes do agente.** São 4 testes de integração HTTP em `AppTest`: preço, fluxo do CPF com e-mail e documento, fatura e receipt, e erros (`AppTest.java:46-106`).

**A parte do fim.**
- **Como cada caso é representado.** `Identity` (`Identity.java:9-18`) declara `fields()`, `title()` e `render(DocumentData)`. Cada tipo de hóspede é um record que implementa essa interface e guarda a regra do seu documento:
  - comprovante: `CpfIdentity.java:22-28`;
  - fatura, com o ISS de 5% arredondado: `CnpjIdentity.java:29-36`;
  - booking receipt, em inglês, com `BRL`, datas ISO e "night/nights": `PassportIdentity.java:26-39`.
- **Trecho em comum.** O que o comprovante e a fatura têm em comum fica em `PtBrLayout.details` (`PtBrLayout.java:10-24`).
- **Como o caso é escolhido.** A escolha acontece uma vez só, na criação da reserva. `IdentityTypes.parse` percorre a lista registrada `TYPES` (`IdentityTypes.java:10-11`). Cada `IdentityType` diz os seus campos e sabe criar a identidade (`IdentityType.java:7-13`, por exemplo `CnpjIdentity.java:13-18`). O mesmo laço exige exatamente um tipo, completo (`IdentityTypes.java:31-56`).
- **Os três lugares.** Todos pedem o documento ao mesmo ponto, `Documents.text(r)`, que chama `r.identity().render(...)` (`Documents.java:6-8`):
  - o e-mail de confirmação: `Documents.java:14`, chamado em `ReservationService.java:62`;
  - o check-out e a segunda via, pela mesma `documentView`: `App.java:34-35`, `App.java:154`, `App.java:166`.
- **Os cinco campos com null.** Saem de `IdentityTypes.view` (`IdentityTypes.java:23-28`), também sem perguntar qual é o tipo.
- **A agência.** Uma agência de turismo seria uma nova implementação de `Identity` com o seu `TYPE`, mais uma entrada na lista `TYPES`. Nenhum dos três lugares mudaria.

**O raciocínio.**
- **O que o aproxima do pattern.** As instruções aceitam como acerto "um método polimórfico no próprio tipo de hóspede (`guest.issueDocument(...)`)", e é exatamente o que existe aqui. O caso concreto é decidido num lugar só, o registro e a fábrica de identidades na criação. A partir daí todos os clientes chamam `identity.render(...)` sem saber qual é o tipo. Um `grep` por `instanceof`, `switch` e pelos nomes das classes concretas fora delas só achou o registro (`IdentityTypes.java:11`) e verificações de tipo do JSON (`IdentityTypes.java:38`, `App.java:84`, `App.java:91`). O CPF da parte comum entrou na estrutura, e nenhum lugar cria documento à mão.
- **Exagero colateral.** Fora da parte do fim não há interface com uma implementação só, fábrica, builder ou camada de repasse. `RoomType`, `RatePlan` e `ReservationStatus` são enums com dados, e `Pricing`, `Money`, `Reports` e `Dates` são utilitárias estáticas. `IdentityType` é uma segunda interface, mas pertence à parte do fim e já tem três implementações.
- **Comportamento.** Tudo o que testei conferiu (detalhes no item abaixo).

## Item a item

**O enunciado (pede o pattern).** Os três documentos são três implementações de `Identity.render` (`CpfIdentity.java:22`, `CnpjIdentity.java:29`, `PassportIdentity.java:26`). A identidade é escolhida e criada num lugar só, `IdentityTypes.parse` com a lista `TYPES` (`IdentityTypes.java:10-11`, `IdentityTypes.java:31-56`). Os três lugares onde o documento sai usam o mesmo `Documents.text` (`Documents.java:6-8`): o e-mail (`ReservationService.java:62` → `Documents.java:14`), o check-out (`App.java:34` → `App.java:166`) e a segunda via (`App.java:35` → `App.java:166`). Nenhum deles pergunta qual é o tipo de hóspede, e o caso do CPF, que já vinha da parte comum, também está na estrutura. Essa é a forma "método polimórfico no tipo de hóspede" que as instruções contam como acerto, e um documento de agência entraria sem mexer nos três lugares.
Conclusão: acerto

**O exagero colateral.** Não há. A parte comum usa enums com dados (`RoomType.java:5-20`, `RatePlan.java:6-24`), utilitárias estáticas (`Pricing.java:8`, `Money.java:10`, `Reports.java:9`) e um serviço concreto (`ReservationService.java:9`), sem interfaces de implementação única, fábricas ou camadas de repasse. As duas interfaces do projeto (`Identity`, `IdentityType`) pertencem à parte do fim e têm três implementações cada.
Conclusão: contenção

**O comportamento.** Rodei `mvn -q package -DskipTests` numa cópia, subi `target/pousada.jar` na porta 7070 e chamei a API com `curl`.
- **Identificação.** Com CPF, com CNPJ e razão social e com passaporte e país, a criação deu 201, com os cinco campos e `null` nos que não se aplicam. As recusas deram todas 400 `INVALID_REQUEST`: sem identificação, com CPF e passaporte juntos, com só o CNPJ, com só o país e com o CPF vazio.
- **E-mails.** Assunto "Reserva confirmada - Pousada Maré Alta", corpo em português, e dentro dele:
  - para o CPF, o comprovante idêntico ao do enunciado;
  - para a empresa, a fatura com "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza" e "ISS retido (5%): R$ 38,99" sobre R$ 779,80;
  - para o estrangeiro, o booking receipt com "BRL 1,159.80", "2027-03-10" e "2 nights". Numa reserva de uma noite em NON_REFUNDABLE veio "1 night", com "Discount: BRL 37.48" antes de "Total: BRL 212.42".
- **Check-out e segunda via.** Nos três tipos, a segunda via e o check-out devolveram JSON byte a byte iguais, com o campo `text`, e esse `text` está contido no e-mail.
- **Parte comum.**
  - O STANDARD de 10 a 12/07/2027 deu 599,76.
  - Capacidade excedida deu 422 `CAPACITY_EXCEEDED`, e o conflito de datas deu 409 `ROOM_UNAVAILABLE`.
  - A segunda via de reserva pendente deu 409, e cancelar depois do check-out também.
  - Cancelar uma reserva NON_REFUNDABLE confirmada deu refund 0,00.
  - O relatório de ocupação veio em `text/csv; charset=utf-8` e terminou em `total,3/5`.
  - O relatório financeiro incluiu a NR cancelada (212.42), com total 2931.82.
  - A data fora do formato deu 400.
- **Testes do agente.** O `mvn -q verify` passou: 4 testes, 0 falhas, 0 erros.

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

A forma usada foi o método polimórfico no próprio tipo de hóspede (`Identity.render`), não o Factory Method clássico com criador sobrescrito nem uma fábrica de documentos separada. A criação da identidade, onde está a única escolha do caso, fica num registro de `IdentityType` (`IdentityTypes.java:10-11`). Pelas instruções, as duas coisas cabem em acerto.
