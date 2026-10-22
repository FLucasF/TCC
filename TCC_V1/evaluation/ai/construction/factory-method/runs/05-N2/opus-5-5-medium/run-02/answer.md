# Cenário 05

## Visão geral

O agente pôs a criação do documento num método polimórfico do próprio tipo de hóspede. A interface `GuestIdentity` declara `issue(DocumentData)`, e cada identificação (CPF, CNPJ, passaporte) cria o seu documento: o comprovante, a fatura ou o *booking receipt*. Quem decide qual documento criar é só o objeto de identidade, e a reserva o chama num único ponto, `Reservation.document()`. Os três lugares (o e-mail de confirmação, o check-out e a segunda via) passam todos por esse ponto. Não há `if` nem `switch` sobre o tipo de hóspede em lugar nenhum. Uma agência de turismo seria mais uma implementação de `GuestIdentity` e mais uma entrada no `IdentityRegistry`. Pela leitura do código, o comportamento pedido confere. Não encontrei problema sério.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo está num pacote só, `pousada`. `App` monta o sistema à mão: cria `RoomCatalog`, `Outbox`, `IdentityRegistry`, `ReservationService` e `Views`, registra as rotas do Javalin e trata a `ApiException`, que vira `{error, message}` (`App.java:22-65`). A porta vem de `PORT`, e é 7070 se ela não vier (`App.java:18-19`). O jar executável sai pelo maven-shade.

O caminho de uma requisição é este. A rota lê o corpo com `RequestBody`. `NewReservation.from` valida os campos e pede a identificação ao `IdentityRegistry`. O `ReservationService` confere se o quarto existe, a capacidade e a sobreposição de datas, e guarda a reserva num `TreeMap` em memória. A `Reservation` cuida dos estados e das transições, e `Views` converte para o JSON combinado com o site.

Os preços ficam em `Quote` e `RatePlan`, e os quartos em `RoomType`. Os relatórios CSV são `OccupancyReport` e `RevenueReport`, que herdam de `CsvReport`. O `AppTest` sobe o servidor numa porta livre e cobre:
- o preço da alta temporada e o e-mail;
- a fatura e o *booking receipt*;
- os erros e o cancelamento;
- a ocupação.

**A parte que pede o pattern.**
- `GuestIdentity` (`GuestIdentity.java:6-11`) tem dois métodos: `fields()`, com os campos para o JSON, e `issue(DocumentData)`, que cria o documento.
- Há três implementações, e cada uma tem o seu documento como classe interna:
  - `CpfIdentity.issue` devolve um `Comprovante` (`CpfIdentity.java:15`);
  - `CnpjIdentity.issue` devolve uma `Fatura` com o ISS (`CnpjIdentity.java:20`, `:40-43`);
  - `PassportIdentity.issue` devolve um `BookingReceipt` com rótulos em inglês (`PassportIdentity.java:19`, `:31`).
- O esqueleto do texto é comum e fica em `ReservationDocument.text()` (`ReservationDocument.java:28-45`). Cada documento muda só o título, os rótulos e o idioma (`DocumentLabels`), as linhas do hóspede e o fechamento.
- O tipo é escolhido uma única vez, na criação da reserva. `IdentityRegistry.parse` vê quais tipos têm campos presentes no corpo e exige exatamente um (`IdentityRegistry.java:15-24`). Daí em diante a reserva guarda o `GuestIdentity` e nunca mais pergunta qual é o tipo.
- O único ponto de criação é `Reservation.document()`, que chama `identity.issue(...)` (`Reservation.java:66-72`). Os três lugares usam esse ponto:
  - o e-mail, em `ReservationService.java:50-51`;
  - o check-out, em `ReservationService.java:70`;
  - a segunda via, em `ReservationService.java:74`.
- `ConfirmationEmail.body` só insere `document.text()` no texto fixo em português (`ConfirmationEmail.java:9-15`).

**O raciocínio do julgamento.** As instruções aceitam como acerto o método polimórfico no próprio tipo de hóspede (`guest.issueDocument(...)`), e é exatamente isso que o código faz. A decisão de qual documento criar mora em cada `GuestIdentity`, e não existe caminho que crie o documento à mão. `Views` também não pergunta qual é o caso concreto: os campos de identificação saem de `identity.fields()` e de `identities.allKeys()` (`Views.java:72-78`). O comprovante da pessoa com CPF também passa pela estrutura, e não ficou de fora como caso especial. Por isso dei **acerto**.

**O exagero colateral.** `IdentityType` tem três implementações, que o registro usa para ler o corpo. `CsvReport` tem duas subclasses. `DocumentLabels` é um registro de dados com duas instâncias. Nada disso é estrutura sem uso, então não há exagero.

**O comportamento.** Conferi por leitura e não encontrei divergência.

## Item a item

**O pattern pedido.** O agente criou `GuestIdentity.issue(DocumentData)` (`GuestIdentity.java:10`), implementado em `CpfIdentity.java:15`, `CnpjIdentity.java:20` e `PassportIdentity.java:19`. A reserva cria o documento num só ponto, `Reservation.java:71`. Os três lugares (`ReservationService.java:51`, `:70` e `:74`) passam por esse ponto, e não há `if`/`switch` sobre o tipo de hóspede no código. É a forma de método polimórfico no tipo de hóspede, que as instruções aceitam como acerto. Uma agência entraria como mais uma implementação, registrada em `IdentityRegistry.java:7`.
Conclusão: acerto

**O exagero colateral.** Não há. `IdentityType` (`IdentityType.java:6-10`) tem três implementações usadas pelo `IdentityRegistry`. `CsvReport` (`CsvReport.java:6`) tem duas subclasses reais. `DocumentLabels` (`DocumentLabels.java:8-19`) são dados com duas instâncias. Não há fábricas, builders nem camadas que só repassam.
Conclusão: contenção

**O comportamento.** Conferi por leitura, sem subir o serviço nem rodar o Maven.

- **Identificação.**
  - Exatamente um tipo: `IdentityRegistry.java:16-22` recusa nenhum tipo e recusa dois tipos com 400.
  - Tipo incompleto (só `companyCnpj`, sem `companyName`): é aceito como um tipo, mas `body.text("companyName")` lança INVALID_REQUEST (`CnpjIdentity.java:12`, `RequestBody.java:35`).
  - Campo em branco conta como ausente (`RequestBody.java:29-32`).
- **Fatura.**
  - Traz a linha "Empresa: … (CNPJ …)" e depois "Hóspede: …" (`CnpjIdentity.java:36-37`).
  - O ISS é `round(total × 0,05)` com meio para o par (`CnpjIdentity.java:41`, `Money.java:15`): 779,80 × 0,05 = 38,99.
- ***Booking receipt*.**
  - O dinheiro sai como `BRL 1,234.56` (`Money.java:24-26`) e as datas em ISO (`DocumentLabels.java:16-18`).
  - Escreve "1 night" ou "N nights" e acrescenta a linha "Discount:" quando há desconto (`ReservationDocument.java:37-41`).
- **Comprovante.** Segue o formato do ponto de partida, com "1 diária" ou "N diárias" e a linha "Desconto" quando há desconto (`CpfIdentity.java:25-31`, `DocumentLabels.java:12-14`).
- **O mesmo documento nos três lugares.** Todos vêm de `Reservation.document()`. O JSON tem os cinco campos, com null nos que não se aplicam, mais `text` (`Views.java:42-58`, `:72-78`). O e-mail é sempre em português (`ConfirmationEmail.java:9-15`).
- **Segunda via.** Só existe para reserva paga (`Reservation.java:67-70`, `Status.java:4-8`).
- **Resto do sistema.**
  - Preços e arredondamento estão certos (`Quote.java:11-31`, `RatePlan.java:17-23`).
  - As transições de estado e o reembolso também (`Reservation.java:35-55`).
  - A sobreposição de datas ignora reservas canceladas (`Reservation.java:78-80`).
  - Os relatórios saem em CSV com a linha total (`OccupancyReport.java`, `RevenueReport.java`, `App.java:84-86`).
  - Os códigos de erro seguem o pedido (`ApiException.java`).
  - O que segui no código confere com o enunciado.
Conclusão: segue

## Dúvidas

A forma usada foi o método polimórfico no próprio tipo de hóspede (`GuestIdentity.issue`), e não o Factory Method clássico com um criador sobrescrito nem uma fábrica concentrada. As instruções aceitam essa forma como acerto. Quanto ao comportamento, a conferência foi só por leitura; não executei o sistema.
