# Cenário 05

## Visão geral

O agente concentrou a criação do documento num único método de fábrica, `ReservationReceipt.of(reservation, room)`. É ele que decide, pelo tipo de identificação do hóspede, qual dos três documentos montar. Os três lugares que emitem o documento (confirmação por e-mail, check-out e segunda via) agora pedem o documento a esse método e não usam mais `new`. Os tipos de hóspede viraram uma `sealed interface Identification` com três records, e a escolha do documento acontece só dentro da fábrica. O ponto mais fraco não chega a ser defeito: os três documentos são o mesmo record (`ReservationReceipt`), com título e texto diferentes, e a decisão aparece em dois `switch` no mesmo método. Para incluir as agências, alguém terá de editar esse método. Como o `switch` sobre um tipo selado é exaustivo, o compilador aponta esses lugares. O comportamento pedido confere na leitura.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. `hotel.Hotel` monta e sobe o servidor. `hotel.web.HttpApi` define as rotas e converte o JSON em objetos de domínio. `hotel.reservations.ReservationService` guarda as regras do ciclo da reserva (criar, confirmar, check-in, check-out, cancelar, segunda via) e grava num `ReservationStore` em memória. `hotel.documents` tem o documento (`ReservationReceipt`) e a formatação de texto (`TextFormat`). `hotel.messages` cuida do e-mail simulado (`EmailSender`, `SentMessages`), e `hotel.reports` dos relatórios em CSV.

O caminho de uma requisição de criação é este. `POST /reservations` chega a `HttpApi.readNewReservation` (`final/src/main/java/hotel/web/HttpApi.java:110-130`), que valida os campos comuns e chama `readIdentification` (`HttpApi.java:132-156`). Esse método produz um `Identification.Cpf`, `Company` ou `Foreign`, que entra no `Guest` (`final/src/main/java/hotel/reservations/Guest.java:4`). Depois o pedido segue para o `ReservationService`.

A rota de confirmação chama `ReservationService.confirm`. O check-out (`HttpApi.java:77-78`) e a segunda via (`HttpApi.java:79-80`) devolvem o próprio `ReservationReceipt` serializado em JSON.

O agente escreveu um teste novo, `final/src/test/java/hotel/web/DocumentTypesTest.java`. Ele cobre a fatura com ISS, o *booking receipt* em inglês, o comprovante de quem tem CPF, os campos null na consulta da reserva e sete combinações de identificação inválida, que devem dar 400. Os testes que já existiam continuam valendo graças ao construtor de compatibilidade `Guest(name, email, cpf)` (`Guest.java:6-8`).

**A parte que pede o pattern.**
- Os casos são representados por `Identification` (`final/src/main/java/hotel/reservations/Identification.java:7-17`), uma interface selada com `Cpf`, `Company` e `Foreign`.
- A fábrica é `ReservationReceipt.of` (`final/src/main/java/hotel/documents/ReservationReceipt.java:37-75`):
  - extrai os campos de identificação aplicáveis (`:39-43`);
  - escolhe o título com um `switch` sobre o tipo (`:44-48`);
  - escolhe o texto com outro `switch` (`:67-74`): `portugueseText` sem ISS para quem tem CPF, `portugueseText` com a linha da empresa e o ISS para a empresa, e `englishText` para o estrangeiro.
- O texto em português da base virou `portugueseText` (`:83-101`), e o inglês está em `englishText` (`:103-118`).
- Os três lugares de emissão chamam a fábrica: `final/src/main/java/hotel/reservations/ReservationService.java:68` (confirm), `:110` (checkOut) e `:119` (receiptCopy).
- Não sobrou nenhum `new ReservationReceipt` fora do próprio record (conferido com grep).

**O raciocínio.** As instruções do cenário aceitam como acerto uma fábrica concentrada, isto é, um método que escolhe o documento pelo tipo de hóspede. É exatamente o caso: a decisão mora num lugar só, e os três lugares de emissão passam por ela.

Não é malfeito, por dois motivos. Nenhum dos três lugares monta o documento à mão. E a escolha não acontece fora da fábrica: o `ReservationService` nunca pergunta o tipo do hóspede.

O que afasta este desenho da forma clássica é que não há um tipo de documento por caso. Há um único record, cujo conteúdo varia, e dois `switch` no mesmo método. Mesmo assim, os dois ficam dentro da fábrica, e é isso que as instruções medem.

A `ReservationResponse` também consulta o tipo com `instanceof` (`final/src/main/java/hotel/web/ReservationResponse.java:56-74`). Isso serve para expor os campos da reserva, não para criar o documento, e não pesa contra o pattern.

Quanto ao exagero, não encontrei estrutura sobrando. A interface selada é o próprio dado novo, e o resto são métodos privados. No comportamento, as regras conferem na leitura (detalhes abaixo).

## Item a item

**O pattern pedido.** A criação do documento ficou concentrada em `ReservationReceipt.of` (`final/src/main/java/hotel/documents/ReservationReceipt.java:37-75`), que escolhe título e texto pelo tipo de `Identification` (`:44-48`, `:67-74`). Os três lugares de emissão usam a fábrica (`final/src/main/java/hotel/reservations/ReservationService.java:68`, `:110`, `:119`), inclusive para quem tem CPF. Nenhum deles decide o caso nem monta o documento sozinho. As instruções aceitam essa forma, a fábrica concentrada, como resposta certa.
Conclusão: acerto

**O exagero colateral.** Não há. `Identification` (`final/src/main/java/hotel/reservations/Identification.java:7`) modela um dado que o pedido exige, com três implementações em uso. `withText` (`ReservationReceipt.java:77-81`) é só um jeito um pouco desajeitado de preencher o campo `text`, não uma camada. O construtor `Guest(name, email, cpf)` (`Guest.java:6-8`) é uma conveniência para o código e os testes que já existiam.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura, sem subir o serviço nem rodar testes:
- **Identificação:** `readIdentification` exige exatamente um tipo. Um tipo conta como informado se qualquer um dos seus campos vier preenchido, então nenhum, dois tipos ou um tipo incompleto dão 400 `INVALID_REQUEST` (`HttpApi.java:133-155`).
- **Fatura:** mostra "Empresa: … (CNPJ …)" e depois "Hóspede:", no lugar da linha do CPF (`ReservationReceipt.java:70-72`). O ISS é `cents(total × 0,05)` com HALF_EVEN, como no resto do sistema (`:96-99`), e 779,80 dá R$ 38,99.
- ***Booking receipt*:**
  - título "no.", datas `LocalDate.toString()` (2027-03-10) e "1 night"/"N nights" (`:105-111`);
  - dinheiro em `BRL 1,234.56` via `Locale.US` (`final/src/main/java/hotel/documents/TextFormat.java:22-24`);
  - "Discount:" antes do total (`:113-115`).
- **Pessoa com CPF:** o texto é idêntico ao da base (compare `base/src/main/java/hotel/documents/ReservationReceipt.java:40-54` com `final/.../ReservationReceipt.java:68-69, 83-95`).
- **Mesmo documento nos três lugares:** os três chamam a mesma fábrica. O e-mail continua em português e inclui `receipt.text()` (`ReservationService.java:126-129`).
- **JSON do documento:** traz os quatro campos novos e `text` como componentes do record (`ReservationReceipt.java:21-33`), com null nos que não se aplicam.
- **Consulta da reserva:** mostra os campos novos, com null nos que não se aplicam (`ReservationResponse.java:38-42`).

Conclusão: segue

## Dúvidas

A forma usada foi a **fábrica concentrada**, um método estático no próprio record do documento. Não é o Factory Method clássico nem um método polimórfico no tipo de hóspede. Os três documentos são um só record com conteúdo variável, e a escolha aparece em dois `switch` no mesmo método (`ReservationReceipt.java:44-48` e `:67-74`), não em uma classe por documento. Como os dois `switch` ficam dentro da fábrica e o resto do sistema não decide nada, mantive acerto. Uma leitura mais exigente poderia ver nisso uma fábrica menos extensível para o documento das agências.
