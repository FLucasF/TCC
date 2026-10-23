# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: sete pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.documents`,
`hotel.messages`, `hotel.reports`, `hotel.rooms`), a requisição entra pelo `HttpApi` (Javalin), vira um
`NewReservation`, passa pelo `ReservationService` e é gravada pelo `ReservationStore` (JDBC, PostgreSQL, esquema por
migrações do Flyway). A mudança acrescentou ao `Guest` os quatro campos novos (`final/src/main/java/hotel/reservations/Guest.java:3-14`),
uma migração V2 com as colunas, a validação de "um jeito e só um" no `HttpApi` (`HttpApi.java:139-155`) e concentrou
a escolha do documento num método do próprio `ReservationReceipt` (`ReservationReceipt.java:57-102`), que os três
lugares (confirmação, check-out, segunda via) continuam chamando com `new ReservationReceipt(reservation, room)`. O
roteiro do comportamento passou inteiro (83 de 83). O mais sério: o agente **não escreveu nenhum teste** para a
parte nova (os 26 testes são os da base, e o único arquivo de teste mudado passou a engolir um erro), e a validação
trata texto vazio como "não veio" enquanto a escolha do documento só olha `null`. Com isso, uma empresa que mande
`"guestCpf": ""` recebe o comprovante de pessoa física, com "CPF )" e sem ISS (conferido à mão pela API).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da
base (`measures/dependencies.md` e `measures/dependencies-base.md`); só cresceram as linhas de `hotel.documents` (79 → 194),
`hotel.web` (236 → 272) e `hotel.reservations` (508 → 533).
**Nota:** bom

A mudança respeitou as camadas que já existiam: o HTTP continua só em `hotel.web` (a leitura do corpo e a validação
dos campos, `HttpApi.java:109-155`, no mesmo lugar onde a base já conferia os campos obrigatórios), o SQL continua só
no `ReservationStore` (`ReservationStore.java:31-56` e `:129-138`), e o documento continua em `hotel.documents`, sem
Javalin nem JDBC. A montagem não mudou (`Hotel.java`). O ciclo `documents` ↔ `reservations` veio da base
(`ReservationService.java:3,64` usa o comprovante, e o comprovante lê `Reservation`); o agente acrescentou mais uma
aresta no mesmo sentido (`ReservationReceipt.java:3`, `import hotel.reservations.Price`), sem criar ciclo novo. Um
reparo menor: a regra "exatamente uma identificação" mora só na borda HTTP (`HttpApi.java:139-155`); o
`ReservationService.create` (`ReservationService.java:27-48`) aceita um `Guest` sem identificação nenhuma, e quem
descobre isso é o documento, com um `IllegalStateException` (`ReservationReceipt.java:79-80`). Isso segue o estilo da
base (que também validava os obrigatórios no `HttpApi`), por isso não derruba a nota.

## Design de baixo nível

**Números:** os três tipos de documento (comprovante, fatura, booking receipt) são escolhidos num lugar só
(`ReservationReceipt.java:62-81`); os três lugares que emitem o documento (`ReservationService.java:64`, `:109`,
`:118`) chamam o mesmo construtor e nenhum pergunta o tipo de hóspede; 0 casos fora da estrutura. O texto de cada
documento fica em três métodos privados (`ReservationReceipt.java:104-159`), dois deles quase iguais.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A base criava o comprovante com `new` em três lugares do `ReservationService`. O agente manteve as três
chamadas, mas o construtor público agora delega a um método de fábrica estático, `generateReceipt`
(`ReservationReceipt.java:31-33` e `:57-102`), que decide pelo tipo de hóspede qual documento montar (título, ISS e
texto) e inclui o caso que já existia (CPF, `:66-69`). É a "fábrica concentrada (um método que escolhe o documento
pelo tipo de hóspede)" que a referência aceita como acerto: a escolha mora num lugar só, e uma agência de turismo
entraria nesse `if` e num novo método de texto, sem mexer no serviço. O que afasta da forma clássica: não há um tipo
por documento nem por hóspede; o documento é um único `record` que carrega os campos de todos os tipos
(`ReservationReceipt.java:11-29`), e o "tipo de hóspede" não existe como tipo, é deduzido de quais campos vêm `null`
(`:62-64`). A próxima variação (a agência) vai pedir mais campos opcionais no `Guest`, no `record` e na tabela.

**O exagero.** Nenhuma interface, fábrica ou camada nova: contenção.

**O resto do design.**
- Duplicação: `generateBrazilianReceipt` e `generateInvoice` repetem as mesmas oito linhas (quarto, datas, diárias,
  desconto, total) (`ReservationReceipt.java:109-118` e `:128-137`); o título também aparece duas vezes, no campo
  `title` (`:67`, `:71`, `:76`) e na primeira linha de cada texto (`:106`, `:124`, `:144`).
- O arredondamento do ISS refaz à mão o que `PriceCalculator.cents` já faz (`ReservationReceipt.java:72-73` contra
  `PriceCalculator.java:50-52`), com o `RoundingMode` escrito pelo nome completo no meio do código.
- Um construtor privado de cópia que só existe para o construtor público poder chamar o método estático
  (`ReservationReceipt.java:35-55`): 20 linhas que repetem os 18 campos; um método `static of(...)` resolveria.
- `null` sem cuidado e regra em dois lugares que não concordam: a validação considera "ausente" o texto vazio
  (`HttpApi.java:140-144`, `isBlank`), mas o `Guest` é montado com os textos crus (`HttpApi.java:127-134`) e a fábrica
  considera presente qualquer valor não nulo (`ReservationReceipt.java:62-64`). O efeito aparece no comportamento
  (abaixo); a causa é de design.
- O `Guest` virou um saco de sete textos em que quatro são opcionais e mutuamente exclusivos
  (`Guest.java:3-14`), sem nada que garanta a combinação válida.

São defeitos localizados, numa classe só, e o código continua legível; por isso `com problemas`, e não `ruim`.

## Testes

**Números:** 26 testes rodaram, 0 falharam, 0 pulados; 26 métodos de teste escritos, os mesmos 26 da base (o agente
não escreveu nenhum). Cobertura: linhas 523 de 598 (87,5%; na base 93,8%), ramos 117 de 180 (65,0%; na base 87,1%).
`hotel.documents`: 48 de 91 linhas e 8 de 24 ramos (na base, 30 de 30 e 4 de 4); `hotel.web`: 30 de 72 ramos
(`measures/tests.md`, `measures/tests-base.md`).
**Nota:** ruim

A parte nova não tem nenhum teste: `diff -r base/src/test final/src/test` só mostra o `EmptyDatabase`. Nenhum teste
cria reserva com CNPJ ou passaporte, confere a fatura, o ISS de R$ 38,99, o booking receipt em inglês (`BRL 779.80`,
`2027-03-10`, "1 night"), nem as recusas de nenhuma, duas ou uma identificação incompleta. Pela pergunta da mutação:
trocar `0.05` por `0.06` (`ReservationReceipt.java:72`), trocar `" night"` por `" nights"` (`:152`), trocar
`Locale.US` por `BRAZIL` (`TextFormat.java:24`) ou apagar a chamada `validateIdentification(body)` (`HttpApi.java:126`)
não faria nenhum teste falhar. A queda da cobertura de `hotel.documents` (100% → 53% das linhas) mostra o mesmo.

Pior: o único arquivo de teste mudado foi enfraquecido. O `EmptyDatabase` agora engole a falha do `TRUNCATE`
(`src/test/java/hotel/EmptyDatabase.java:19-23`, com o comentário "Table might not exist... which is OK"); se a
limpeza falhar, os testes passam a rodar sobre os dados do teste anterior, e a numeração das reservas, que os testes
da base conferem, deixa de ser garantida sem que nada avise. Os 26 testes da base continuam passando, mas, para o que
o agente mudou, a coluna está ausente.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; nenhuma chave estrangeira sem índice;
nenhuma data em texto; 2 migrações (V1 igual à da base, V2 nova); SQL sempre com `?` (`measures/database.md`).
**Nota:** com problemas

O agente fez o certo no essencial: não tocou na V1 e acrescentou uma migração nova, `V2__add_company_and_passport.sql`,
que só adiciona colunas e solta o `NOT NULL` do CPF, então um banco que já roda o sistema sobe sem perder nada. O
`ReservationStore` grava e lê as quatro colunas novas com parâmetros (`ReservationStore.java:32-45`, `:130-137`). O
defeito: a V2 tira a única garantia que o banco tinha de que toda reserva identifica o hóspede
(`V2__add_company_and_passport.sql:1`, `guest_cpf DROP NOT NULL`) e não põe nada no lugar (um `CHECK` de que vem
exatamente o CPF, ou o CNPJ com a razão social, ou o passaporte com o país). Agora uma linha sem identificação, ou
com as três, é aceita pelo banco, e o documento dela quebra com `IllegalStateException`
(`ReservationReceipt.java:79-80`). Também entram textos vazios no lugar de `null` (conferido à mão: `guest_cpf = ''`
gravado numa reserva de empresa). As colunas de situação sem `CHECK` (`status`, `rate_plan`) vieram da base.

## Comportamento

**Números:** 83 de 83 conferências passaram: parte comum 46 de 46, dados depois de reiniciar 3 de 3, cenário 05 32 de
32, reservas simultâneas 2 de 2 (`measures/behavior.md`).
**Nota:** com problemas

Pelo roteiro, tudo funciona: o comprovante de quem tem CPF ficou igual ao da base, a fatura sai com a empresa e o ISS
retido de R$ 38,99, o booking receipt sai em inglês com `BRL`, datas ISO, "1 night" e a linha Discount, o documento é o
mesmo no e-mail (que continua em português), no check-out e na segunda via, e as quatro recusas dão 400
INVALID_REQUEST.

Fora do roteiro, achei um erro que o dono da pousada sentiria. Chamei a API com o serviço de pé: uma reserva de
empresa com `"guestCpf": ""`, CNPJ e razão social é aceita (201), porque a validação trata o texto vazio como
ausente (`HttpApi.java:140`), mas o documento sai como **"Comprovante de reserva" com "Hóspede: Ana Souza (CPF )"**,
sem a empresa e sem o ISS retido (`issRetained: null`), porque a fábrica só olha `null` (`ReservationReceipt.java:62`).
Para o dono, isso quer dizer que, se o site mandar os campos que não se aplicam como texto vazio em vez de omiti-los,
toda empresa recebe um comprovante de pessoa física no e-mail, no check-out e na segunda via, sem a fatura com o ISS
que ela precisa. É uma parte do cenário que falha com uma entrada que a própria validação aceita; por isso
`com problemas`, e não `bom`.

## Dúvidas

- A forma do pattern: é a fábrica concentrada (um método estático `generateReceipt` com `if`/`else if` sobre quais
  campos do hóspede vêm preenchidos, chamado pelo construtor do único `record` de documento), não o Factory Method
  clássico com um criador por tipo. A referência aceita essa forma como acerto, e os três lugares passam por ela;
  mas o tipo de hóspede é deduzido de `null`, e o documento é um tipo só com os campos de todos, o que deixa a
  agência de turismo mais cara do que a classe sugere.
- O caso do texto vazio pesei no comportamento, e não no design, para não contar duas vezes. Se o site sempre omitir
  ou mandar `null` nos campos que não se aplicam, ele não aparece; o texto não diz como o site manda os campos.
