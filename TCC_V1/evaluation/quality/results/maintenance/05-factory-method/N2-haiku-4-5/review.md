# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a forma da base: 7 pacotes (`hotel` monta tudo, `hotel.web` é a borda Javalin, `hotel.reservations`
tem o serviço, as regras e o `ReservationStore` com JDBC, `hotel.documents` tem o comprovante). Uma requisição entra em
`HttpApi` (que valida o corpo e monta o `Guest`), passa pelo `ReservationService` e chega ao PostgreSQL por
`ReservationStore`, com o esquema criado por Flyway. A mudança acrescentou quatro campos ao `Guest`, à tabela (duas
migrações novas), à resposta e ao documento, e concentrou a escolha do texto do documento num método só,
`ReservationReceipt.generateText` (`src/main/java/hotel/documents/ReservationReceipt.java:50-58`). O que há de mais
sério: o campo `title` do documento continua fixo em "Comprovante de reserva" para os três tipos
(`ReservationReceipt.java:31`), e por isso falharam 3 conferências do roteiro; a API aceita um CPF com `companyCnpj: ""`
e emite uma fatura "Empresa: null (CNPJ )" para quem tem CPF; o ISS arredonda com `HALF_UP`, não "meio para o par"
como o resto do sistema; e a fatura de uma reserva com desconto não traz a linha do desconto.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, iguais à base (`measures/dependencies.md` e `measures/dependencies-base.md`); 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base. As linhas cresceram em `hotel.documents` (79 → 140), `hotel.web` (236 → 271) e `hotel.reservations` (508 → 533). A maior classe continua sendo `HttpApi` (170 → 194 linhas).
**Nota:** bom

A mudança entrou nas camadas que a base já tinha, sem criar dependência nova. A validação "um jeito de identificar,
e só um" ficou em `src/main/java/hotel/web/HttpApi.java:126-142`, ao lado da validação dos campos obrigatórios, onde a
base já conferia o CPF. O SQL novo ficou só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:32-55`,
`:130-137`), e o texto dos documentos ficou em `hotel.documents`. As regras continuam sem Javalin e sem JDBC, e a
montagem continua num lugar só (`hotel/Hotel.java`). O ciclo `documents` ↔ `reservations` veio da base: o
`ReservationReceipt` lê a `Reservation`, e o `ReservationService` cria o recibo. O agente não o piorou. Há um único
reparo: a regra de identificação só existe na borda HTTP. O `ReservationService` aceita um `Guest` sem identificação
nenhuma, e os próprios testes do serviço criam reservas assim (`src/test/java/hotel/reservations/ReservationServiceTest.java:177-178`).
Não chega a mudar a nota.

## Design de baixo nível

**Números:** a decisão sobre o documento fica em 1 lugar (`ReservationReceipt.java:50-58`). Os 3 lugares que emitem o documento (`ReservationService.java:64`, `:109` e `:118`) chamam o mesmo construtor e não perguntam o tipo de hóspede. Há 3 geradores de texto, um por tipo, e o CPF também passou para dentro da estrutura. O título (`title`) ficou fora da escolha: é o mesmo para os 3 tipos.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha de qual documento emitir fica num método só. `generateText` decide pelo tipo de hóspede
(`ReservationReceipt.java:51-57`), e o e-mail de confirmação, o check-out e a segunda via pedem o documento ao mesmo
construtor (`ReservationService.java:64`, `:109`, `:118`), sem `if` próprio. Isso é a "fábrica concentrada num
método" que a referência aceita como acerto. O caso que já existia (CPF) também entrou na estrutura
(`generateCpfReceipt`, `:60-74`). Uma agência de turismo pede um ramo a mais em `generateText`, e mais nada nos três
lugares.

Três coisas afastam a solução do acerto limpo. Primeiro, a fábrica escolhe só o texto: o documento é sempre o mesmo
record com `title = "Comprovante de reserva"` (`:31`), então a fatura e o *booking receipt* saem no JSON com o título
do comprovante. Segundo, não há um tipo de hóspede: o tipo é deduzido de qual campo está diferente de `null`
(`:51`, `:53`) num `Guest` com sete `String` que podem ser nulas (`src/main/java/hotel/reservations/Guest.java:3-10`).
Terceiro, a borda HTTP decide o tipo por outro critério, `isBlank` (`HttpApi.java:126-128`), e por isso um
`companyCnpj: ""` passa na validação e depois vira fatura (ver Comportamento).

**Exagero:** nenhum. Não há interface com uma implementação só, nem fábrica ou camada que só repassa.

**O resto do código.**

- **Duplicação.** As linhas de quarto, datas e diárias estão copiadas, iguais, em `ReservationReceipt.java:64-68` e
  `:81-85`, e com outra língua em `:96-100`. A linha do desconto existe no comprovante (`:69-71`) e no *booking receipt*
  (`:101-103`), mas foi esquecida na fatura (`:85-86`). Com isso, uma fatura com desconto mostra "Diárias: R$ 779,80"
  e "Total: R$ 662,83" sem explicar a diferença.
- **Arredondamento.** O ISS arredonda com `RoundingMode.HALF_UP` (`:87`), mas o sistema arredonda "meio para o par"
  (`PriceCalculator.java:51`, `HALF_EVEN`; README:33), e o pedido diz "arredondado para centavos como sempre".
- **Coerência do record.** O construtor canônico do record é público e recebe o `text` pronto, então nada garante que
  o texto corresponda aos campos.
- **Comentários apagados.** O agente apagou os comentários de documentação de `ReservationReceipt` e de `TextFormat`.
  O de `CreateReservationRequest` ("Every field is required") tinha mesmo de sair.

São defeitos localizados, mas reais.

## Testes

**Números:** 32 testes rodaram, 0 falhas, 0 erros e 0 pulados; 32 métodos escritos (a base tinha 26, todos mantidos). Cobertura de linhas 94,3% (541/574), contra 93,8% na base; cobertura de ramos 85,7% (132/154), contra 87,1% na base. Em `hotel.documents`, 68/69 linhas e 11/14 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente acrescentou 6 testes e não apagou nem enfraqueceu os da base.

- **Os testes da fatura e do *booking receipt* só usam `contains` com pedaços fixos do texto.**
  - A fatura confere "Fatura de hospedagem nº", "Empresa: Tecnosul Ltda" e o rótulo "ISS retido (5%)", sem o valor
    (`src/test/java/hotel/reservations/ReservationServiceTest.java:185-187`).
  - O *booking receipt* confere "BRL " (`:205`), sem valor, sem data e sem "nights".
  - O teste do CPF confere só "Comprovante de reserva nº" e "(CPF" (`:170-171`).
- **A pergunta da mutação.** Todos estes passariam: trocar 0.05 por 0.5 em `ReservationReceipt.java:87`; trocar
  `dateEnglish` por `date`; trocar o separador de milhar inglês; apagar a linha "Discount"; trocar "night" por
  "nights". Nenhum teste confere o valor R$ 38,99 que o pedido dá, o campo `title`, nem que o check-out e o e-mail levam
  o mesmo documento. É por isso que os 3 defeitos que o roteiro achou passaram.
- **O que está bem coberto.** A validação pela API está razoável: aceitar empresa e estrangeiro, e recusar nenhuma
  identificação, CPF+CNPJ e CNPJ sem razão social (`src/test/java/hotel/web/HttpApiTest.java:124-174`). Faltam o
  passaporte sem o país e os campos em branco (`""`).

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; a chave estrangeira tem índice; nenhuma data guardada em texto. Flyway com 3 migrações: V1 da base igual, V2 e V3 novas. A conferência das colunas de tipo e situação acusa `status`, `rate_plan`, `rooms.type` e `messages.channel` sem CHECK, o que já vinha da base (`measures/database.md`).
**Nota:** com problemas

- **O que está certo.**
  - O agente evoluiu o esquema com migrações novas (`src/main/resources/db/migration/V2__add_company_and_passport_fields.sql:1-5`,
    `V3__make_identification_fields_nullable.sql:1-2`), sem editar a V1. Um banco que já roda o sistema sobe sem
    perder dados.
  - O SQL novo usa parâmetros (`ReservationStore.java:32-55`).
  - O `EXCLUDE` contra reserva duplicada continua no lugar.
- **O defeito.** A V3 tira o `NOT NULL` de `guest_cpf` e não põe nada no lugar. Na base, o banco garantia que toda
  reserva tinha identificação; agora aceita uma reserva sem CPF, sem CNPJ e sem passaporte, ou com CNPJ sem razão
  social. Bastava um `CHECK` do tipo `num_nonnulls(...)` sobre os três jeitos. Como a borda grava o texto vazio como
  veio, uma reserva com CPF e `company_cnpj = ''` fica gravada assim (visto na chamada manual, reserva 2).
- **Fazer em duas migrações** uma mudança só (V2 e V3) é só deselegante.

## Comportamento

**Números:** 80 de 83 conferências passaram. Parte comum: 46/46. Dados depois de reiniciar: 3/3. Cenário 05: 29/32. Reservas simultâneas: 2/2 (`measures/behavior.md`).
**Nota:** com problemas

**As 3 falhas do roteiro** têm a mesma causa: o documento da empresa e o do estrangeiro saem com
`"title": "Comprovante de reserva"`, na segunda via e no check-out (`ReservationReceipt.java:31`). O texto (`text`)
está certo nos dois casos: a fatura com ISS de R$ 38,99, o *booking receipt* com `BRL 2,783.52`, as datas
`2027-03-10`, "1 night" e "Discount". O e-mail também leva o documento certo. Mas quem lê o JSON pelo título vê o
comprovante.

**Fora do roteiro**, chamei a API à mão e achei três problemas que o dono notaria:

1. **CPF com `companyCnpj: ""`.** É comum um formulário mandar os campos vazios assim. A reserva é aceita (201), e o
   documento sai como "Fatura de hospedagem nº 2 / Empresa: null (CNPJ ) / … ISS retido (5%): R$ 38,99". Um hóspede
   com CPF recebe uma fatura quebrada, com um ISS que não se aplica (`HttpApi.java:126-128` contra
   `ReservationReceipt.java:51`).
2. **Fatura com desconto.** Numa reserva NON_REFUNDABLE da empresa, a fatura mostra "Diárias: R$ 779,80" e
   "Total: R$ 662,83", sem a linha "Desconto: R$ 116,97".
3. **ISS no caso de empate.** Três diárias do quarto 101 em março dão R$ 749,70, e a fatura mostra
   "ISS retido (5%): R$ 37,49". Pelo arredondamento do sistema ("meio para o par", README:33), seriam R$ 37,48.

A parte comum, os dados depois de reiniciar e as reservas simultâneas estão intactos. Para o dono: a mudança funciona
para o caso feliz, mas o título errado e a fatura emitida por engano para quem tem CPF precisam de correção antes de ir
para o site.

## Dúvidas

- **A classe do pattern.** Ficou entre `acerto` e `malfeito`. A escolha está concentrada num método só e os três
  lugares não decidem nada, que é a forma "método que escolhe o documento" que a referência aceita. Mas o método
  escolhe só o texto, e o `title` do documento fica o mesmo para os três tipos, o que lembra "a fábrica devolve sempre
  o mesmo documento". Dei `acerto` porque a decisão de qual documento emitir não acontece em nenhum outro lugar.
- **A forma usada.** Não é a forma clássica nem um método polimórfico no hóspede. É uma fábrica num método
  (`generateText`, chamado do construtor do record), que decide por quais campos do `Guest` não são nulos.
- **A linha do desconto na fatura.** O pedido não diz se a fatura deve ter essa linha (o exemplo é sem desconto). Contei
  a falta como defeito porque a fatura "é parecida com o comprovante" e, sem a linha, as contas não fecham.
