# Revisão: manutenção, cenário 05

## Visão geral

A base é um serviço Javalin com sete pacotes. `hotel.web.HttpApi` lê a requisição e chama `hotel.reservations.ReservationService`. O serviço calcula o preço (`PriceCalculator`), grava pelo `ReservationStore` (JDBC com parâmetros, PostgreSQL com migrações do Flyway) e monta o documento do pacote `hotel.documents`. Os dados ficam nas tabelas `rooms`, `reservations` e `messages`, e a montagem é feita em `hotel.Hotel`. Para atender o pedido, o agente fez cinco coisas:
- transformou o record `ReservationReceipt` numa classe abstrata `sealed` com três subclasses (`CpfReceipt`, `CnpjInvoice` e `PassportReceipt`);
- criou `DocumentFactory.create(reservation, room)`, que escolhe o documento pelos campos nulos do `Guest`, e passou os três lugares que emitem documento a usar essa fábrica;
- acrescentou quatro campos ao `Guest`, ao pedido e à resposta;
- moveu a validação "um jeito só de identificar" para o `HttpApi`;
- criou a migração `V2`.

Os textos dos três documentos saem certos e as recusas funcionam. **O defeito mais sério é uma regressão no JSON do documento:** a classe nova não tem getters que o Jackson reconheça, e só `@JsonProperty` em `text()` e `title()` (`src/main/java/hotel/documents/ReservationReceipt.java:100-104`). Por isso o documento devolvido no check-out e na segunda via perdeu todos os campos de hoje (`reservationId`, `guestName`, `guestCpf`, `total`…). Agora ele traz só `type`, `text` e `title`, inclusive para quem tem CPF. A falha aparece em duas conferências do comportamento e nenhum teste a pega.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais à base (`measures/dependencies.md` e `measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. O que mudou: `hotel.documents` passou de 79 para 392 linhas e agora usa o Jackson (na base, não usava). `HttpApi` passou de 170 para 185 linhas.
**Nota:** com problemas

A forma geral se manteve:
- a borda HTTP continua fina (`src/main/java/hotel/web/HttpApi.java:61-91`);
- o SQL continua só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:31-56`, `:128-137`);
- a montagem continua em `src/main/java/hotel/Hotel.java:20-29`;
- os três pontos do serviço pedem o documento à fábrica (`src/main/java/hotel/reservations/ReservationService.java:65`, `:110`, `:119`).

O ciclo veio da base, e o agente não o piorou.

Há duas misturas novas, ambas localizadas:
- **Serialização HTTP dentro do pacote de documentos.** O pacote ganhou anotações do Jackson (`@JsonTypeInfo` e `@JsonSubTypes` em `src/main/java/hotel/documents/ReservationReceipt.java:12-21`, `@JsonProperty` em `:100-104`). O resultado é que o formato do JSON passou a depender de como a hierarquia de documentos foi desenhada, e foi justamente isso que quebrou a resposta. O certo seria `HttpApi` responder com um DTO próprio, como já faz com `ReservationResponse`.
- **Uma regra de negócio no handler.** A regra "exatamente um jeito de identificar o hóspede" mora no handler (`src/main/java/hotel/web/HttpApi.java:127-140`). O mesmo conhecimento é refeito por outro caminho, com `null`, em `src/main/java/hotel/documents/DocumentFactory.java:12-18`. Duas partes do código decidem o tipo de hóspede, cada uma com o seu critério.

## Design de baixo nível

**Números:** há 3 tipos de documento, todos na estrutura: o comprovante da base virou `CpfReceipt`. Nenhum dos três pontos de emissão cria o documento à mão (3 de 3 passam pela fábrica). A escolha está num único `if/else` dentro de `DocumentFactory`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É a forma "fábrica concentrada" que a referência aceita:
- `DocumentFactory.create` (`src/main/java/hotel/documents/DocumentFactory.java:11-21`) é o único lugar que decide qual documento criar;
- `confirm`, `checkOut` e `receiptCopy` pedem o documento a ela (`ReservationService.java:65`, `:110`, `:119`);
- o e-mail usa `receipt.text()` sem perguntar o tipo (`ReservationService.java:126-129`);
- o caso que já existia (CPF) também entrou na estrutura.

Para acrescentar o documento das agências basta uma subclasse e um ramo na fábrica. O que afasta do ideal é que o tipo de hóspede não existe como tipo: ele é deduzido de qual campo do `Guest` não é nulo (`src/main/java/hotel/reservations/Guest.java:3-10`, `DocumentFactory.java:12-17`). A ordem dos `if` vira regra implícita.

**O exagero.** Não há interface, builder nem camada nova fora do pattern. As anotações de desserialização polimórfica (`ReservationReceipt.java:12-21`, com `defaultImpl = CpfReceipt.class`) não servem para nada, porque o documento nunca é lido de JSON. Também há um construtor público de 10 a 12 argumentos em cada subclasse que só o outro construtor usa (`CpfReceipt.java:13`, `CnpjInvoice.java:14`, `PassportReceipt.java:14`). Nada disso é estrutura de abstração, por isso dei contenção (ver Dúvidas).

**O resto do código.** Os defeitos estão localizados, mas são reais:
- **Duplicação:** os três `text()` repetem quase linha a linha a montagem de quarto, datas, diárias, desconto e total (`CpfReceipt.java:54-67`, `CnpjInvoice.java:66-82`, `PassportReceipt.java:62-76`). O `CnpjInvoice` só acrescenta a linha da empresa e a do ISS, e um método comum na classe base evitaria a cópia. Os construtores "a partir da reserva" também se repetem três vezes (`CpfReceipt.java:29-42`, `CnpjInvoice.java:32-46`, `PassportReceipt.java:32-46`).
- **Arredondamento diferente do resto do sistema:** o ISS usa `RoundingMode.HALF_UP` com nome qualificado e um `0.05` solto (`CnpjInvoice.java:61-63`). O resto do sistema arredonda meio para o par (`PriceCalculator.java:50-51`), e o pedido diz "arredondado para centavos como sempre". Os exemplos do roteiro dão o mesmo valor, mas um total cujo ISS caia exatamente em meio centavo com o dígito anterior par (um total que termine em ,10, por exemplo) sairia com um centavo a mais.
- **O JSON quebrado:** a classe com campos `protected` e acessores no estilo record (`reservationId()` em vez de `getReservationId()`) foi exposta direto ao Jackson. É a causa da regressão do JSON (`ReservationReceipt.java:26-35`, `:100-104`).
- **Detalhes menores:** validação com `if` de uma linha sem chaves e mensagens muito longas (`HttpApi.java:128-139`), e o comentário do record do pedido apagado sem substituto (`CreateReservationRequest.java:6`).

## Testes

**Números:** 28 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 28 métodos escritos, contra 26 da base, ou seja, 2 novos e nenhum apagado. Cobertura de linhas: 91,8% (584/636), contra 93,8% na base. Cobertura de ramos: 77,7% (115/148), contra 87,1% na base. No pacote `hotel.web`, os ramos cobertos ficaram em 23 de 46 (na base, 18 de 28). Fonte: `measures/tests.md` e `measures/tests-base.md`.
**Nota:** com problemas

Os 26 testes da base continuam passando. A parte nova tem só dois testes de serviço, com asserções fracas, que procuram trechos com `contains`:
- `companyReservationGeneratesCnpjInvoice` (`src/test/java/hotel/reservations/ReservationServiceTest.java:158-170`) confere o título, "Empresa: Tecnosul Ltda" e o rótulo "ISS retido (5%)", mas não o valor R$ 38,99.
- `foreignerReservationGeneratesPassportReceipt` (`:172-187`) confere "BRL" e "Check-in:", mas não o formato `BRL 779.80`, a data `2027-03-10`, "1 night" nem a linha "Discount".

Fazendo a pergunta da mutação, nenhum teste falharia se alguém:
- trocasse `0.05` por `0.5`, ou o modo de arredondamento do ISS (`CnpjInvoice.java:62`);
- trocasse `Locale.US` por `pt-BR` em `moneyEnglish` (`TextFormat.java:23-25`), porque o texto ainda contém "BRL";
- trocasse `ISO_LOCAL_DATE` pelo formato brasileiro.

Também não há nenhum teste das recusas novas (nenhuma identificação, duas, CNPJ sem razão social, passaporte sem país), que estão no `HttpApi` (`HttpApi.java:127-140`; daí os ramos de `hotel.web` cobertos só pela metade). Nenhum teste confere o JSON do documento. Foi por isso que a regressão dos campos sumidos passou com o `mvn verify` verde: `ReservationServiceTest.java:143-145` confere `receipt.reservationId()` em Java, onde o acessor existe, e não no JSON.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está guardada como texto. As migrações são 2: `V1` igual à base e `V2` nova (`measures/database.md`). As conferências não acusaram nada de novo; as colunas `status`, `rate_plan` e `type` sem CHECK vêm da base.
**Nota:** com problemas

O que está certo:
- o agente evoluiu o esquema com uma migração nova, sem editar a da base (`src/main/resources/db/migration/V2__add_identification_types.sql`), e um banco que já roda o sistema sobe sem problema;
- os campos novos entram com `?` (`ReservationStore.java:32-54`) e são lidos de volta (`ReservationStore.java:129-136`), e o comportamento confirma que sobrevivem ao reinício.

Os defeitos:
- **Uma coluna morta:** a `V2` cria `guest_cpf_alt TEXT` (`V2__add_identification_types.sql:2`), que nenhum código usa (o `grep` só a acha na migração). Ela fica no esquema de todo mundo até que outra migração a remova.
- **Nenhuma proteção no banco para a identificação:** a `V2` tira o `NOT NULL` de `guest_cpf` (`:8-9`) e não põe nada no lugar. Não há CHECK de "exatamente uma forma de identificação" nem de "CNPJ junto com razão social". A integridade dessa regra depende só da validação em `HttpApi.java:127-140`.

## Comportamento

**Números:** 81 de 83 conferências passaram (`measures/behavior.md`):
- parte comum: 45 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 31 de 32;
- reservas simultâneas: 2 de 2.

**Nota:** com problemas

O que o dono pediu e funciona:
- quem tem CPF continua recebendo o comprovante de antes;
- a fatura mostra a empresa no lugar do CPF e o ISS de R$ 38,99;
- o booking receipt sai em inglês, com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a linha "Discount";
- o e-mail continua em português e leva o documento certo;
- as quatro recusas respondem 400 com INVALID_REQUEST;
- a consulta da reserva mostra os quatro campos novos, com `null` nos que não se aplicam.

As duas falhas têm a mesma causa e não vêm de nada que o texto deixasse em aberto:
- "check-out responde 200 com o comprovante" veio com `reservationId` nulo (parte comum);
- "empresa: o documento traz os cinco campos" veio com tudo nulo (cenário).

Chamei a API à mão para confirmar. `GET /reservations/{id}/document` agora devolve só isto, tanto para CPF quanto para CNPJ:

```
{"type":"CNPJ","text":"...","title":"Fatura de hospedagem"}
```

Sumiram do JSON todos os campos que a base devolvia (número da reserva, hóspede, CPF, quarto, datas, diárias, subtotal, desconto, total), e os quatro campos novos que o pedido mandava acrescentar nunca aparecem. O pedido dizia "o documento continua com os campos de hoje… mais esses quatro novos e um campo `text`" e "tudo o que funciona hoje tem que continuar funcionando". Para o dono, isso quer dizer o seguinte: o texto do documento está certo, mas qualquer tela do site ou da recepção que leia os dados do comprovante pelos campos fica em branco, inclusive para os hóspedes com CPF de sempre. O contrato da API mudou sem ninguém pedir. As demais regras, os reinícios e as reservas simultâneas funcionam.

## Dúvidas

- **A forma do pattern:** é a "fábrica concentrada" (uma classe com um método estático que escolhe a subclasse pelo tipo de hóspede), não o Factory Method clássico com criador sobrescrito. A referência conta as duas como acerto.
- **O exagero:** dei contenção. Ainda assim, as anotações `@JsonTypeInfo` e `@JsonSubTypes`, com `defaultImpl` (`ReservationReceipt.java:12-21`), montam uma desserialização polimórfica que nada usa, e os construtores longos das subclasses só são usados pelo outro construtor. Quem considerar isso estrutura sem necessidade pode ler `exagero`.
- **A nota do comportamento:** pela regra, ficou entre `com problemas` (só 2 falhas, e o resto do cenário certo) e `ruim` (uma regressão na parte comum que muda o contrato do documento para todos os hóspedes). Dei `com problemas` porque o texto do documento, o e-mail e todos os fluxos de reserva funcionam, mas a regressão é séria.
- **O arredondamento do ISS:** o roteiro não tem um total em que o meio para cima (`HALF_UP`) e o meio para o par divirjam, então a divergência de um centavo é só teórica nesta medição.
