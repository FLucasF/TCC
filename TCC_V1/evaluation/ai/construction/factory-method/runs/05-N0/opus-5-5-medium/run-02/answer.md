# Cenário 05

## Visão geral

O agente montou um sistema pequeno e direto. A escolha do documento ficou no próprio tipo de hóspede: `GuestIdentity` tem três records, `Person`, `Company` e `Foreigner`, e cada um responde em `template()` qual `DocumentTemplate` usar (comprovante, fatura ou *booking receipt*). Os três lugares onde o documento sai (e-mail de confirmação, check-out e segunda via) passam todos por `Reservation.document()`, e em nenhum ponto o código pergunta qual é o tipo concreto do hóspede. Cada caso de identificação corresponde a uma entrada na tabela de `IdentityReader`, que garante que vem exatamente uma forma de identificação. Pela leitura, as regras pedidas conferem. Não achei problema sério. O ponto mais fraco é pequeno: cada template lê os campos da identidade (`cpf()`, `cnpj()`) por getters default que valem null nos outros tipos, e isso cria um acoplamento implícito entre template e tipo.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo está num pacote só, `com.pousadamaretalta`. `Main` lê a variável `PORT`, usa 7070 se ela não vier e sobe `App.create(new ReservationService())` (`src/main/java/com/pousadamaretalta/Main.java:7-9`). `App` registra as rotas Javalin, converte os objetos em JSON com `LinkedHashMap` e trata `ApiException` como `{error, message}` (`App.java:16-36`). `ReservationService` concentra as regras e guarda os dados em memória: um `TreeMap` de reservas, a lista `outbox` de mensagens e um contador de id que começa em 1 (`ReservationService.java:15-17`). Os cálculos de preço ficam em `Pricing`, a formatação de dinheiro em `Money`, e os dados fixos nos enums `RoomType`, `RatePlan` e `ReservationStatus` e no record `Room`. Uma requisição segue este caminho: rota em `App`, depois `Json.parseObject`, depois um método do `ReservationService`, que valida, altera a `Reservation` e devolve a reserva ou um `ReservationDocument` para `App` serializar. O `pom.xml` usa Java 21, Javalin 6.7.0 e o shade plugin com `mainClass` `Main` (`pom.xml:14-16,60-61`). Os testes estão em `AppTest`: são seis testes HTTP que cobrem preço na alta temporada, o ciclo completo com o e-mail, fatura e *booking receipt*, validações, cancelamento com relatórios, e ocupação.

**A parte do pattern.**
- `GuestIdentity` é uma interface com `template()` e getters default que devolvem null. Cada record sobrescreve `template()`: `Person` devolve `COMPROVANTE`, `Company` devolve `FATURA` e `Foreigner` devolve `BOOKING_RECEIPT` (`GuestIdentity.java:8-27`).
- `DocumentTemplate` é a interface com `title()` e `render()`. Ela tem três implementações, cada uma com o texto do seu documento:
  - comprovante em `DocumentTemplate.java:14-24`;
  - fatura com ISS em `DocumentTemplate.java:26-38`;
  - receipt em inglês em `DocumentTemplate.java:40-57`.
  
  O trecho em português que comprovante e fatura têm em comum fica em `ptBody` (`DocumentTemplate.java:60-68`).
- `ReservationDocument` é o instantâneo dos dados. `title()` e `text()` dele delegam para `identity.template()` (`ReservationDocument.java:12-14`).
- O documento é criado num lugar só, `Reservation.document()` (`Reservation.java:54-57`), e os três lugares chamam esse método:
  - e-mail: `ReservationService.java:68-71`;
  - check-out: `ReservationService.java:100`;
  - segunda via: `ReservationService.java:106`.
- O JSON do documento sai de um único `documentJson` e inclui `text` (`App.java:93-108`).
- A escolha do tipo na entrada mora em `IdentityReader`, numa tabela `Kind` com os campos e o construtor de cada tipo (`IdentityReader.java:12-15`).

**O raciocínio.** As instruções aceitam como acerto o método polimórfico no próprio tipo de hóspede, e é essa a forma usada aqui: `identity.template()` decide qual documento criar, e cada tipo responde por si. Procurei `instanceof`, `switch` ou `if` sobre o tipo de hóspede no serviço, em `App` ou nos templates e não encontrei nenhum. Também nenhum dos três lugares monta o texto à mão. Para criar o documento da agência basta um record novo, um template novo e uma linha em `IdentityReader.ALL`. O que afasta o desenho um pouco do ideal é que cada template lê os campos da identidade pelos getters genéricos (`DocumentTemplate.java:20,32,47`). O `COMPROVANTE` supõe, sem que o tipo garanta, que vai receber um `Person`. Isso não traz a escolha de volta para fora da estrutura, então não muda a classe. Em exagero colateral não vi estrutura sem uso: `DocumentTemplate` tem três implementações, `ReservationDocument` é um record de dados e `IdentityReader` cumpre uma regra real. No comportamento, por leitura, os valores, os formatos e as recusas conferem com o pedido.

## Item a item

**O pattern pedido.** A escolha do documento está num método polimórfico do tipo de hóspede, `GuestIdentity.template()`, sobrescrito em cada record (`src/main/java/com/pousadamaretalta/GuestIdentity.java:17-27`). A criação do documento está concentrada em `Reservation.document()` (`Reservation.java:54-57`), e o texto e o título vêm de `identity.template()` (`ReservationDocument.java:12-14`). Os três lugares passam por esse caminho:
- e-mail de confirmação: `ReservationService.java:68-71`;
- check-out: `ReservationService.java:100`;
- segunda via: `ReservationService.java:106`.

Não existe condição sobre o tipo de hóspede em nenhum desses lugares. O comprovante da pessoa com CPF, que é o caso que já existia, também passa pela mesma estrutura (`DocumentTemplate.java:14-24`). É uma das formas que as instruções aceitam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. `DocumentTemplate` tem três implementações reais (`DocumentTemplate.java:14,26,40`). `ReservationDocument` é só um record de dados (`ReservationDocument.java:7-10`). `IdentityReader` aplica a regra de "exatamente um jeito" de identificar o hóspede (`IdentityReader.java:19-41`). Não há fábrica, builder ou camada que só repassa chamadas.
Conclusão: contenção

**O comportamento.** A conferência foi feita só pela leitura do código. Não subi o serviço, não rodei o Maven e não chamei a API.
- **Identificação.** `IdentityReader` recusa com 400 INVALID_REQUEST quando não vem nenhuma forma (`IdentityReader.java:36-38`), quando vêm duas ou quando uma vem incompleta (`IdentityReader.java:30-32`).
- **JSON da reserva e do documento.** Os dois mostram os cinco campos, com null nos que não se aplicam (`App.java:64-70,78,98`). O documento tem o campo `text` (`App.java:107`).
- **Fatura.** Mostra "Empresa: … (CNPJ …)", depois "Hóspede: …", e no fim o ISS calculado como `round(total × 0.05)` com HALF_EVEN. Para 779,80, isso dá 38,99 (`DocumentTemplate.java:32-35`, `Money.java:13-15`).
- **Booking receipt.** Sai em inglês, com `BRL 1,234.56` (`Money.java:28-30`), datas ISO e "1 night" ou "N nights" (`DocumentTemplate.java:50-51`), e com a linha "Discount:" quando há desconto (`DocumentTemplate.java:53`).
- **Mesmo documento nos três lugares.** E-mail, check-out e segunda via usam o mesmo `text()`, e o e-mail continua em português em volta do documento (`ReservationService.java:69-73`).
- **Comprovante da pessoa com CPF.** Segue o modelo do enunciado: "Hóspede: Nome (CPF …)", "1 diária" ou "N diárias", e "Desconto:" antes do total (`DocumentTemplate.java:19-21,60-68`).
- **Resto do sistema.** Também confere:
  - estados e devolução: `ReservationService.java:63-107`;
  - preço e alta temporada: `Pricing.java:14-34`;
  - relatórios CSV: `ReservationService.java:113-156`;
  - códigos de erro: `ReservationService.java:37-51`, `ApiException.java:13-19`.
Conclusão: segue

## Dúvidas

A forma usada é o método polimórfico no próprio tipo de hóspede (`guest.template()`, e o documento é redigido a partir dele). Não é a forma clássica do Factory Method, com um criador sobrescrito, nem uma fábrica concentrada. Há uma ressalva menor que não muda a classe: os templates leem os campos da identidade por getters genéricos que valem null nos outros tipos (`GuestIdentity.java:11-15`, `DocumentTemplate.java:20,32,47`), então o par template–tipo não é verificado pelo compilador.
