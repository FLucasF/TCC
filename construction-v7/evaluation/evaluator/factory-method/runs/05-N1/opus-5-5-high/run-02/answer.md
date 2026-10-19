# Cenário 01

## Visão geral

O agente pôs o documento por tipo de hóspede dentro do próprio tipo. Há uma interface `Identification`, com uma implementação para CPF, outra para CNPJ e outra para passaporte. Cada uma sabe criar o seu documento por meio de `issue(Voucher)`. O único lugar que monta o documento é `Reservation.document()`, e o e-mail de confirmação, o check-out e a segunda via passam todos por ele. Em nenhum ponto do código se pergunta qual é o tipo de hóspede. A escolha do tipo, ao criar a reserva, também é feita sem `if`: um registro de três "kinds" decide pelos campos que vieram no pedido. O resto do sistema é enxuto, sem interfaces nem camadas sobrando. Nos testes com o serviço no ar, tudo o que conferi saiu como o enunciado pede, e não achei problema sério.

## Como foi implementado, e por que o julgamento

**O projeto.** São três pacotes:

- `pousada` tem só o `App`, que lê a variável `PORT` (7070 quando ela não vem) e monta o `ReservationService` e a `Outbox` em memória (`App.java:14-22`).
- `pousada.web` tem duas classes:
  - `Api` registra as rotas do Javalin, lê o JSON com Jackson, valida campos, datas e mês, e transforma `DomainException` em `{error, message}` com o status HTTP certo (`web/Api.java:32-68`).
  - `Views` monta os mapas JSON de quarto, reserva, documento e mensagem (`web/Views.java`).
- `pousada.domain` tem as regras:
  - `Room` traz os 5 quartos fixos.
  - `Pricing` calcula a alta temporada e arredonda com HALF_EVEN a cada passo (`domain/Pricing.java:26-42`).
  - `RatePlan` é um enum com o desconto e a informação de reembolso.
  - `Reservation` guarda a máquina de estados, o reembolso, o `kept()` usado no relatório financeiro e o `document()`.
  - `ReservationService` cuida dos casos de uso, guarda as reservas num `LinkedHashMap` e é `synchronized`.
  - `Reports` gera os CSV, `Outbox` é a lista de mensagens e `ConfirmationEmail` é o texto do e-mail.

Uma requisição segue este caminho: a rota em `Api` chama `parseNewReservation`, que chama `IdentificationKinds.parse`. Daí vai para `ReservationService.create`, que confere datas, quarto, capacidade e conflito e calcula o preço com `Pricing.quote`. O resultado é uma `Reservation` em memória, e `Views.reservation` a devolve como JSON. O jar é gerado com o maven-shade e tem `pousada.App` como classe principal. Os testes são 7 testes de integração em `InnTest`, que sobem o Javalin numa porta aleatória e cobrem:

- preço de alta temporada;
- e-mail com o comprovante;
- fatura e booking receipt;
- identificação de um jeito só;
- erros;
- cancelamento e relatórios;
- check-out.

**A parte do fim.**

- **Onde cada caso mora.** Cada tipo de hóspede é um `record` que implementa `Identification` (`domain/identification/Identification.java:13-19`). Cada um sabe três coisas: quais campos expõe (`fields()`), como emite o seu documento (`issue(Voucher)`) e, numa classe aninhada `Kind`, quais campos o pedido precisa trazer e como se cria a partir deles.
  - `CpfIdentification` emite o "Comprovante de reserva" usando `PortugueseLayout` (`domain/identification/CpfIdentification.java:21-25`).
  - `CnpjIdentification` emite a "Fatura de hospedagem" com a linha da empresa e o ISS de 5% arredondado (`domain/identification/CnpjIdentification.java:16,24-30`).
  - `PassportIdentification` emite o "Booking receipt" usando `EnglishLayout` (`domain/identification/PassportIdentification.java:21-25`).
- **O que é comum a todos.** Os dados que todo documento mostra ficam no `Voucher` (`domain/document/Voucher.java:9-24`). Os dois layouts só cuidam do idioma e do formato, e recebem cabeçalho e rodapé de quem os chama (`domain/document/PortugueseLayout.java:17-31`, `domain/document/EnglishLayout.java:17-31`).
- **Como o tipo é escolhido.** `IdentificationKinds` guarda a lista dos três kinds (`domain/identification/IdentificationKinds.java:13-16`). O `parse` filtra os kinds que têm algum campo presente no pedido, exige exatamente um e exige que todos os campos dele estejam preenchidos (`:29-47`).
- **Onde o documento é criado.** O único ponto de criação é `Reservation.document()`, que monta o `Voucher` e chama `identification.issue(voucher)` (`domain/Reservation.java:81-86`). Os três lugares passam por ele:
  - a confirmação (`domain/ReservationService.java:72-73`, que leva o `Document` para dentro do `ConfirmationEmail.body`);
  - o check-out (`domain/ReservationService.java:92`);
  - a segunda via (`domain/ReservationService.java:96`).
- **Como o JSON sai.** O JSON da reserva e o do documento preenchem os cinco campos de identificação, com null nos que não se aplicam, por meio de `IdentificationKinds.withNulls` (`web/Views.java:34,55`). Nenhum dos dois pergunta o tipo concreto.

**O raciocínio.**

- **O que o aproxima do pattern.** As instruções aceitam como acerto o método polimórfico no próprio tipo de hóspede (`guest.issueDocument(...)`), e é exatamente o que existe aqui: `identification.issue(voucher)`. A decisão de qual documento criar não está em nenhum `if` nem `switch`. Ela é a classe concreta da identificação, escolhida uma única vez, na criação da reserva, pelo registro. Os três lugares pedem o documento ao mesmo método. O caso que já vinha da parte comum (o comprovante com CPF) também passa pela estrutura. Para uma agência entrar, basta uma classe nova e uma linha no registro.
- **O que o afastaria.** Não achei nada que o afaste.
- **O exagero colateral.** Fora dessa parte não há interface de implementação única, fábrica, builder nem camada que só repassa:
  - `Outbox` é uma classe concreta;
  - `Pricing` e `Reports` são utilitários estáticos;
  - `RatePlan` é um enum com dados;
  - `Api` vai direto ao serviço.

  `IdentificationKind` e o registro ficam dentro da parte do fim e servem à variação, então não contam como exagero.
- **O comportamento.** Tudo o que testei com o serviço no ar conferiu.

## Item a item

**O enunciado (pede o pattern).** Cada tipo de hóspede é uma implementação de `Identification` com o seu próprio `issue(Voucher)`:

- `domain/identification/CpfIdentification.java:21-25`
- `domain/identification/CnpjIdentification.java:24-30`
- `domain/identification/PassportIdentification.java:21-25`

A criação do documento está concentrada em `Reservation.document()` (`domain/Reservation.java:81-86`). O e-mail de confirmação (`domain/ReservationService.java:72-73`), o check-out (`:92`) e a segunda via (`:96`) passam todos por esse método. Não há `if` nem `switch` sobre o tipo em lugar nenhum. O tipo é decidido uma vez, ao criar a reserva, pelo registro `IdentificationKinds` (`domain/identification/IdentificationKinds.java:13-16,29-47`). A pessoa com CPF, que é o caso da parte comum, também passa pela estrutura. É a forma "método polimórfico no tipo de hóspede", que as instruções contam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há:

- `Outbox` é uma classe concreta (`domain/Outbox.java:7`);
- `Pricing` e `Reports` são utilitários estáticos (`domain/Pricing.java:8`, `domain/Reports.java:12`);
- `RatePlan` é um enum com dados (`domain/RatePlan.java:5-24`);
- `Api` chama o `ReservationService` direto (`web/Api.java:35-59`).

A interface `IdentificationKind` e o registro ficam dentro da parte do fim e servem à variação.
Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp`. O `mvn -q verify` passou com 7 testes, 0 falhas e 0 erros. Subi o jar `pousada.jar` com `PORT=7070`, chamei a API com `curl` e no fim derrubei o serviço. O que conferi:

- **Identificação de um jeito só:**
  - sem nenhum dos campos, ou com CPF e passaporte juntos: 400 `INVALID_REQUEST` ("Informe a identificação de um único jeito…");
  - só o `companyCnpj`, só o `guestCountry` ou `guestCpf:""`: 400 ("Campo obrigatório ausente: …");
  - na consulta, os campos que não se aplicam vêm `null`.
- **Fatura:** a reserva com CNPJ de 779,80 mostra "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)", "Hóspede: Ana Souza" e "ISS retido (5%): R$ 38,99". Com desconto, um total de 397,70 dá ISS de R$ 19,88, que confere com o arredondamento meio para o par.
- **Booking receipt:**
  - uma noite saiu com "1 night", "Check-in: 2027-03-10", "Discount: BRL 37.48" e "Total: BRL 212.42";
  - a suíte em julho saiu com "2 nights" e "BRL 1,391.76".
- **Comprovante (CPF):** é idêntico ao do enunciado, com "Comprovante de reserva nº 1", "(CPF 111.222.333-44)", "2 diárias" e "R$ 779,80".
- **O mesmo documento nos três lugares:** para as 4 reservas, o `text` da segunda via é igual ao do check-out e aparece dentro do corpo do e-mail. O e-mail sai sempre em português, com o assunto certo. O JSON do documento traz `title`, os cinco campos de identificação com null e o `text`.
- **Parte comum:**
  - o STANDARD de 10 a 12/07/2027 dá 599.76;
  - quarto ocupado dá 409 `ROOM_UNAVAILABLE`, hóspedes demais dá 422 `CAPACITY_EXCEEDED`, corpo que não é JSON dá 400, quarto 999 dá 404;
  - a segunda via de reserva pendente dá 409 `OPERATION_NOT_ALLOWED`;
  - no cancelamento, o plano normal devolve 599.76, a reserva pendente devolve 0.00 e o não reembolsável devolve 0.00 e continua no relatório financeiro;
  - o relatório de ocupação sai em `text/csv; charset=utf-8`, com `total,3/5`.
Conclusão: segue

## Dúvidas

Sobre a forma do pattern: é o método polimórfico no próprio tipo de hóspede (`Identification.issue(Voucher)`), e não um criador clássico com subclasses nem uma fábrica separada. As instruções aceitam essa forma como acerto. Um detalhe pequeno, que não muda a classe: um pedido com `guestCpf` preenchido e `companyCnpj: null` é aceito, porque o valor null é tratado como campo ausente (`web/Api.java:114-115`). Isso me parece uma leitura sensata de "só um jeito".
