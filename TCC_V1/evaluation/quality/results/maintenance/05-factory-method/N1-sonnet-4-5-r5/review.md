# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: `hotel.web.HttpApi` lê a requisição e chama o `ReservationService`
(`hotel.reservations`), que guarda tudo no PostgreSQL pelo `ReservationStore` (JDBC com `?`, migrações do Flyway) e
cria o documento com `new ReservationReceipt(reservation, room)` nos três lugares de sempre
(`src/main/java/hotel/reservations/ReservationService.java:64`, `:109`, `:118`). A mudança acrescentou quatro campos ao
`Guest` (`src/main/java/hotel/reservations/Guest.java:3-10`) com um `type()` deduzido de quais campos estão nulos
(`Guest.java:16-24`), uma migração nova (`V2__add_guest_identification_types.sql`) com um CHECK de "um jeito só", a
validação na borda (`src/main/java/hotel/web/HttpApi.java:127-136`) e, dentro do próprio `ReservationReceipt`, um
`switch` pelo tipo de hóspede que escolhe o título e o texto (`src/main/java/hotel/documents/ReservationReceipt.java:67-97`).
O roteiro passa inteiro (83 de 83) e os 29 testes passam. Nada é grave. Encontrei dois defeitos testando à mão, fora
do roteiro: a fatura de uma reserva com desconto não mostra a linha "Desconto" (vai de "Diárias: R$ 779,80" direto para
"Total: R$ 662,83", `ReservationReceipt.java:146-147`), e um CPF que chega junto com um CNPJ sem razão social é aceito,
e o CNPJ some sem aviso (`HttpApi.java:128-142`). O código novo dos documentos também tem duplicação e métodos com 10
ou 11 parâmetros.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais na base e na final; 1 ciclo (`hotel.documents` ↔
`hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). As linhas de `hotel.documents` passaram
de 79 para 219, as de `hotel.reservations` de 508 para 546 e as de `hotel.web` de 236 para 266. `ReservationReceipt`
passou de 55 para 185 linhas e virou a segunda maior classe.
**Nota:** bom

O agente encaixou a mudança nas camadas que já existiam e não criou nenhuma dependência nova entre pacotes. A borda
continua fina: o JSON novo fica nos records `CreateReservationRequest` e `ReservationResponse`
(`src/main/java/hotel/web/ReservationResponse.java:35-41`), e o SQL novo fica só no `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:32-55`, `:129-137`). A montagem não mudou (`hotel.Hotel`). O
ciclo `documents` ↔ `reservations` vem da base: `ReservationService` importa `ReservationReceipt` e o recibo importa
`Reservation`. O agente só acrescentou mais um import no mesmo sentido (`ReservationReceipt.java:3`, `Guest`). A regra
"um jeito de identificar, e só um" mora na borda HTTP (`HttpApi.java:127-136`), ao lado da conferência de campos
obrigatórios que já era assim na base, e o banco a repete num CHECK. É uma validação de entrada, aceitável neste
tamanho. Fica a ressalva de que um segundo canal de entrada teria de repeti-la.

## Design de baixo nível

**Números:** os três tipos de documento são escolhidos num lugar só, dentro de `ReservationReceipt`. Os três pontos de
emissão (e-mail de confirmação, check-out e segunda via) não perguntam o tipo; nenhum faz isso à mão. Mas, dentro do
recibo, a escolha pelo tipo aparece em dois `switch` (`ReservationReceipt.java:67-73` e `:87-96`), além do `if` que
deduz o tipo em `Guest.java:16-24`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A referência aceita como acerto "uma fábrica concentrada (uma classe ou um método que escolhe o
documento pelo tipo de hóspede)" para a qual os três lugares pedem o documento. É o que se vê aqui: `confirm`,
`checkOut` e `receiptCopy` continuam com `new ReservationReceipt(reservation, roomOf(reservation))`
(`ReservationService.java:64`, `:109`, `:118`) e não sabem qual documento sai. A escolha mora no construtor do
recibo, que chama `titleFor` e `generateText` (`ReservationReceipt.java:31-44`), e cada documento tem o seu gerador
(`:99`, `:126`, `:153`). O comprovante da base foi para a estrutura como o caso `INDIVIDUAL` (`:88`). O que afasta da
forma ideal:
- a decisão está repartida em dois `switch` sobre o mesmo enum, um para o título e um para o texto (`:67-73`, `:87-96`);
- todos os documentos são o mesmo record, com o texto calculado lá dentro;
- para a agência de turismo será preciso mexer no enum, em `Guest.type()` e nos dois `switch`.

Ainda assim, são dois arquivos, e nenhum chamador muda.

**O exagero.** Não há estrutura sobrando: nada de interface com uma implementação só, de builder ou de camada que só
repassa. O enum `GuestType` (`Guest.java:26-28`) é o mínimo necessário.

**O resto do código.**
- **Parâmetros demais e mal aproveitados.** Os três geradores recebem 10 ou 11 parâmetros
  (`ReservationReceipt.java:75-86`, `:99-110`, `:126-136`, `:153-164`), todos tirados de `reservation` e `room`, que
  poderiam ser passados direto. O construtor privado (`:46-65`) só existe para passar o texto e chama `titleFor`
  pela segunda vez (`:33` e `:48`).
- **Duplicação.** As linhas de quarto, datas, diárias e total do comprovante e da fatura são cópias
  (`:114-118` e `:142-147`). Foi nessa cópia que a fatura perdeu a linha do desconto que o comprovante tem (`:119-121`).
- **Número mágico.** A taxa do ISS está solta no meio do texto (`new BigDecimal("0.05")`, `:148`), com um
  arredondamento próprio, em vez de uma constante como as do `PriceCalculator`
  (`src/main/java/hotel/reservations/PriceCalculator.java:16-17`).
- **O tipo deduzido dos nulos.** `Guest.type()` deduz o tipo pelos campos nulos e cai em `FOREIGN` por exclusão
  (`Guest.java:16-24`): um `Guest` sem nenhuma identificação vira estrangeiro sem erro. O construtor de três
  argumentos (`Guest.java:12-14`) mantém esse caminho aberto.

São defeitos localizados, no código novo.

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas 94,8% (565 de 596; base 93,8%) e de ramos 89,2% (141 de 158; base 87,1%). `hotel.documents` fica
com 87 de 88 linhas e 13 de 16 ramos.
**Nota:** com problemas

O agente acrescentou três testes pela API, sem apagar nem enfraquecer os da base (o patch só acrescenta linhas a
`src/test/java/hotel/web/HttpApiTest.java`). O que eles conferem com valores:
- a fatura: o título, a empresa com o CNPJ e o ISS de R$ 38,99 (`HttpApiTest.java:151-156`);
- o booking receipt: as datas ISO, "2 nights" e "BRL 779.80" (`:190-195`);
- o e-mail em português com o documento dentro (`:143-146`, `:178-181`);
- os campos nulos na consulta (`:136`, `:171`);
- as quatro recusas: nenhum jeito, dois jeitos, CNPJ sem razão social e passaporte sem país (`:199-231`).

A segunda via passa pelo banco, então a ida e volta das colunas novas também fica conferida. Mas as asserções são
`contains` sobre trechos, e a pergunta da mutação acha regras pedidas sem rede:
- trocar `nights == 1 ? " night"` por sempre `" nights"` (`ReservationReceipt.java:173`): nenhum teste falha, porque
  "1 night" nunca é testado;
- apagar a linha "Discount:" do booking receipt (`:175-177`): nenhum teste falha;
- trocar o `Locale.US` por um sem separador de milhar (`TextFormat.java:24`): nenhum teste falha, porque nenhum valor
  passa de mil.

Nenhum teste confere a fatura com desconto (o defeito da linha "Desconto" passou por isso), nem o documento do
check-out para empresa ou estrangeiro, nem o CPF que chega junto com um CNPJ incompleto.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` em todas as colunas; nenhuma chave
estrangeira sem índice; nenhuma data em texto. A V1 da base ficou igual e a V2 é nova. Há 6 restrições, entre elas o
EXCLUDE da base contra dupla reserva e o CHECK novo de um jeito só de identificação. As colunas de situação e de plano
continuam sem CHECK, como na base.
**Nota:** bom

O agente evoluiu o esquema do jeito certo, com uma migração nova e sem editar a V1
(`src/main/resources/db/migration/V2__add_guest_identification_types.sql:2-12`). O `DROP NOT NULL` do CPF e as
quatro colunas `text` anuláveis não quebram as linhas que já existem, que têm CPF e passam no CHECK. O CHECK
(`V2:8-12`) protege no banco a regra de um jeito só. É frouxo num ponto: não obriga `company_name` e `guest_country` a
serem nulos nos ramos em que não se aplicam (`V2:9`, `:10`). Hoje a aplicação os anula (`HttpApi.java:141-145`). O
SQL novo usa parâmetros e posições conferidas (`ReservationStore.java:32-55`), e a leitura monta o `Guest` com as
colunas novas (`ReservationStore.java:130-137`). Os candidatos a SQL montado com `+` que o script lista só juntam
constantes (`COLUMNS`, `ReservationStore.java:18-20`).

## Comportamento

**Números:** 83 de 83 conferências passaram: parte comum 46 de 46, dados depois de reiniciar 3 de 3, cenário 05
32 de 32, reservas simultâneas 2 de 2.
**Nota:** com problemas

Tudo o que o roteiro pede funciona:
- o comprovante de quem tem CPF continua igual;
- a fatura sai com a empresa e o ISS de R$ 38,99;
- o booking receipt sai em inglês, com "1 night", a linha "Discount" e "BRL 2,783.52";
- o documento é o mesmo no e-mail, no check-out e na segunda via;
- as quatro recusas respondem 400 INVALID_REQUEST;
- os dados ficam depois de reiniciar, e as reservas simultâneas não duplicam.

Fora do roteiro, chamando a API à mão, achei dois defeitos que o dono notaria:
- **A fatura com desconto.** Uma empresa com plano NON_REFUNDABLE recebe uma fatura que vai de "Diárias: R$ 779,80"
  direto para "Total: R$ 662,83", sem a linha "Desconto: R$ 116,97" (`ReservationReceipt.java:146-147`). O texto diz
  que a fatura "é parecida com o comprovante" e só troca o CPF pela empresa e acrescenta o ISS no fim. O comprovante
  tem essa linha. Uma fatura cujas contas não fecham é justamente o documento que a empresa vai conferir.
- **O CPF com um CNPJ incompleto.** Um pedido com `guestCpf` e `companyCnpj`, mas sem `companyName`, é aceito com 201
  como pessoa física, e o CNPJ some sem aviso (`HttpApi.java:128`, `:131`, `:142`). O texto pede "um desses jeitos, e
  só um; se não for, 400", e quem mandou um CNPJ provavelmente esperava uma fatura. O caso contrário, um CPF em branco
  com um passaporte completo, ser tratado como estrangeiro é uma escolha razoável.

## Dúvidas

- A forma do pattern: é uma fábrica concentrada dentro do próprio record (`ReservationReceipt`), com a escolha
  repartida em dois `switch` no mesmo arquivo, e não um criador por tipo nem um método polimórfico no hóspede. Dei
  acerto porque a escolha mora num lugar só e os três pontos de emissão não perguntam o tipo. Quem olhar os dois
  `switch` e o tipo deduzido dos nulos pode achar que a estrutura está mais perto de um `switch` concentrado que de
  uma fábrica.
- Se a fatura deve repetir a linha "Desconto": o exemplo do texto não tem desconto. Li "parecida com o comprovante"
  como manter as linhas dele, e por isso a falta pesou no comportamento.
