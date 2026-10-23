# Revisão: construção, cenário 05

## Visão geral

O projeto tem quatro pacotes: `br.com.marealta`, com a montagem (`Main` e `Application`), `web` (Javalin e Jackson),
`domain` (as regras, os documentos, os relatórios e os serviços) e `persistence` (o JDBC com HikariCP). Uma requisição
entra em `web/Api.java:28-41`. O `RequestReader` valida o corpo e o transforma em `NewReservation`
(`web/RequestReader.java:23-40`), o `ReservationService` aplica a regra dentro de uma transação
(`domain/ReservationService.java:21-36` e `:68-76`) e o `ReservationRepository` grava em duas tabelas
(`reservations` e `messages`), criadas por `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:17-48`).
Os quartos ficam no código (`domain/RoomCatalog.java:7-12`). A variação do cenário foi resolvida assim: cada tipo de
hóspede cria o próprio documento (`GuestIdentity.documentFor`), e os três lugares que emitem o documento passam por
`Reservation.document()`. O roteiro de comportamento passou inteiro (83 de 83) e os 13 testes passam. Os defeitos
existem, mas são moderados:
- um ciclo entre `domain` e `persistence`, porque os serviços do domínio importam os repositórios concretos e o
  `Database`;
- um esquema sem migrações e sem nenhuma restrição (CHECK, UNIQUE ou EXCLUDE);
- a regra "um quarto, uma reserva por noite" protegida só por um `synchronized`, que vale para um processo só.

## Arquitetura

**Números:** 4 pacotes, 6 dependências entre eles, 1 ciclo (`domain` ↔ `persistence`). O Javalin aparece só em
`br.com.marealta` e `web`, e o JDBC só em `persistence` (`measures/dependencies.md`).
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota de `web/Api.java:28-41` só lê, chama o serviço e escreve. Os erros são traduzidos
  num lugar só (`web/Api.java:43-48`, com o status HTTP no próprio `ErrorCode`, em `domain/ErrorCode.java:3-9`). Os
  formatos JSON ficam em `web/Views.java`.
- **As regras não conhecem o Javalin nem o SQL.** Preço em `domain/Pricing.java`, situação em
  `domain/ReservationStatus.java:14-26`, cancelamento e devolução em `domain/Reservation.java:55-63`.
- **O acesso ao banco fica isolado** nos dois repositórios.
- **A montagem fica num lugar só**, `Application.java:16-21`.
- **O ciclo.** `ReservationService` importa `persistence.Database`, `MessageRepository` e `ReservationRepository`
  (`domain/ReservationService.java:3-5`) e controla a transação pelo `Database` (`:28` e `:69`). `ReportService` faz o
  mesmo com o repositório (`domain/ReportService.java:3`). Ao mesmo tempo, `persistence` depende de `domain` para
  montar `Reservation` (`persistence/ReservationRepository.java:3` e `:137-157`). Por isso as regras não podem ser
  usadas nem testadas sem o pacote de dados.
- **O tamanho das classes é equilibrado:** a maior tem 158 linhas (`ReservationRepository`).

É um ciclo pequeno entre dois pacotes vizinhos, numa estrutura que, fora isso, está bem separada. Por isso a nota é
"com problemas" e não "ruim".

## Design de baixo nível

**Números:** 3 tipos de hóspede, e os 3 têm o documento criado na estrutura (nenhum fora). Os 3 lugares que emitem o
documento (o e-mail de confirmação, o check-out e a segunda via) passam por `Reservation.document()`. Não há nenhum
`if` ou `switch` sobre o tipo de hóspede no código.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** O agente usou a forma do método polimórfico no próprio tipo de hóspede:
- `GuestIdentity.documentFor(Reservation)` (`domain/GuestIdentity.java:7`) é sobrescrito em
  `CpfIdentity.java:22-24` (`ReceiptDocument`), `CnpjIdentity.java:22-25` (`InvoiceDocument`) e
  `PassportIdentity.java:22-25` (`BookingReceiptDocument`).
- A escolha mora num lugar só, `Reservation.document()` (`domain/Reservation.java:81-87`), que também faz a regra de
  que "só existe documento para reserva paga".
- Os três lugares pedem o documento a esse método: o e-mail em `domain/ConfirmationEmail.java:11`, o check-out em
  `domain/ReservationService.java:56` e a segunda via em `domain/ReservationService.java:60`.
- Nenhum deles faz `new` de documento, e o e-mail continua um só, em português, mudando apenas o documento de dentro.
- A criação da própria identidade também fica concentrada: cada tipo traz o seu `KIND` (`CpfIdentity.java:4-14` e
  equivalentes), e `IdentityKinds.from` (`domain/IdentityKinds.java:15-23`) exige exatamente um tipo presente.
- Para a agência de turismo, basta escrever um novo `GuestIdentity` com o seu documento e registrá-lo em
  `IdentityKinds.java:7-10`.
- O texto dos documentos é um Template Method (`domain/ReservationDocument.java:38-51`), com a parte em português
  compartilhada em `PortugueseDocument`. Assim, o comprovante e a fatura não repetem as linhas comuns, e a linha de
  desconto aparece numa regra só (`ReservationDocument.java:45-47`).
- O que afasta da forma ideal é pouco. Acrescentar um tipo com campos novos ainda obriga a mexer no `IdentityFields`
  de cinco campos fixos (`domain/IdentityFields.java:4-5`), nas `Views` e no esquema. Isso, porém, vem do contrato da
  API que o texto pede, não da criação do documento.

**O exagero.** A estrutura fora do pattern é pouca e cada peça tem mais de um uso. `CsvReport`
(`domain/CsvReport.java:7-21`) tem duas implementações reais (ocupação e financeiro). Não há interface com uma
implementação só, nem builder, nem camada que só repassa. O `IdentityKind` com classes anônimas é um pouco
cerimonioso, mas pertence à parte que o pattern resolve.

**O resto do código.** Está limpo:
- **Os tipos:** `BigDecimal` com `HALF_EVEN` arredondado a cada passo (`domain/Money.java:16-18`,
  `domain/Pricing.java:27-36`), `LocalDate`, enums para situação, plano e tipo de quarto.
- **As regras:** as transições ficam num mapa (`ReservationStatus.java:14-26`), e o desconto e a devolução no próprio
  `RatePlan` (`domain/RatePlan.java:17-23`).
- **Os erros:** nenhuma exceção engolida. O `catch (Exception)` de `web/Api.java:45-48` registra a causa antes de
  responder 500.

Ressalvas pequenas:
- o construtor de 16 parâmetros e o `id` mutável por `assignId` (`domain/Reservation.java:24-44` e `:99-101`);
- o `RequestReader` usa `IdentityKind.require` para todo campo obrigatório, inclusive `guestName` e `ratePlan`
  (`web/RequestReader.java:76-78`): o nome do método não diz o que ele faz ali.

Nada disso dificulta mudar o código.

## Testes

**Números:** 13 testes rodaram, 0 falharam, 0 pulados; 13 métodos escritos (8 em `DomainTest` e 5 em `ApiTest`, este
pela API HTTP contra o PostgreSQL de `DB_URL`). Cobertura de 95,0% das linhas (532 de 560) e de 74,5% dos ramos (79 de
106); no domínio, 296 de 297 linhas (`measures/tests.md`).
**Nota:** bom

Os testes conferem as regras com valores. O nível é misto, com domínio puro e API de ponta a ponta com banco real.

- **Preço:**
  - alta temporada e arredondamento, 599.76 e as bordas 15/12 e 14/12: `DomainTest.java:21-29`;
  - NON_REFUNDABLE, 779.80, 116.97 e 662.83: `DomainTest.java:32-38`;
  - formatação e meio-para-o-par: `DomainTest.java:110-114`.
- **A parte nova do cenário, com o texto inteiro de cada documento:**
  - comprovante: `DomainTest.java:41-51`;
  - fatura com ISS de R$ 38,99: `DomainTest.java:54-67`;
  - booking receipt com "1 night", desconto e BRL: `DomainTest.java:70-82`;
  - a recusa de nenhuma identificação, de duas e de uma incompleta: `DomainTest.java:85-90` e, pela API, com
    INVALID_REQUEST, `ApiTest.java:206-207`.
- **Pela API:**
  - o e-mail leva a fatura: `ApiTest.java:247`;
  - o check-out e a segunda via devolvem o mesmo documento, com os cinco campos: `ApiTest.java:256-262`;
  - os dois CSV, com o texto exato: `ApiTest.java:251-253` e `:264-265`;
  - a NON_REFUNDABLE cancelada continua no financeiro: `ApiTest.java:270-282`;
  - sobreposição e liberação do quarto: `ApiTest.java:222-228`.

A pergunta da mutação:
- **ISS:** se alguém trocasse 0.05 por outro valor (`InvoiceDocument.java:8`), `DomainTest.java:66` falharia.
- **Documento:** se alguém trocasse o documento de um tipo de hóspede, os três testes de texto falhariam.
- **Validação:** se alguém mudasse `!= 1` para `> 1` em `IdentityKinds.java:17`, `DomainTest.java:86` falharia.
- **Devolução no STANDARD (sem rede):** se `RatePlan.refundOf` devolvesse sempre zero (`RatePlan.java:22`), nenhum
  teste falharia. O único cancelamento STANDARD testado é de uma reserva pendente (`DomainTest.java:104-106`), que
  devolve zero de qualquer jeito.
- **Fim da alta temporada:** fevereiro e janeiro não são testados diretamente. Só 01/03 aparece, como fora da
  temporada.
- **Reservas simultâneas:** não há teste.

São lacunas pequenas diante do resto. Um detalhe: o `ApiTest` é pulado sem `DB_URL` (`ApiTest.java:145`), mas na
medição ele rodou.

## Banco

**Números:** 2 tabelas (`reservations` com 20 colunas e `messages` com 5), ambas com chave primária. Dinheiro em
`numeric(12,2)` nas 5 colunas, datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE; `status`, `rate_plan`
e `channel` em `text` sem CHECK. O esquema é criado no código Java, sem migrações. Há 1 transação explícita e 0
`FOR UPDATE` (`measures/database.md`).
**Nota:** com problemas

**Os dados estão bem guardados.** Os tipos estão certos e há `NOT NULL` no obrigatório
(`persistence/Database.java:18-39`). Há um índice que serve à busca de sobreposição (`Database.java:40`). Todo SQL usa
`?`: os três candidatos que o script acusou concatenam só a constante `COLUMNS`
(`persistence/ReservationRepository.java:14-17`, `:78`, `:101` e `:110-111`). As conexões vêm de um pool HikariCP e
voltam por try-with-resources (`Database.java:53-61` e `:83-95`). A confirmação grava a reserva e o e-mail na mesma
transação (`domain/ReservationService.java:68-76`, `Database.java:98-118`). Os dados sobrevivem a um reinício
(comportamento, 3 de 3).

**Os defeitos:**
- **O esquema não evolui.** Ele é um `CREATE TABLE IF NOT EXISTS` dentro do código (`Database.java:17-48`). O próximo
  pedido que acrescentar uma coluna (por exemplo, a agência) não terá como alterar uma tabela que já existe.
- **A regra "uma reserva por quarto por noite" não está no banco.** A transação de `create` roda em READ COMMITTED e
  faz um "consulta e depois insere" sem trava (`ReservationService.java:28-35`, `ReservationRepository.java:83-97`).
  O que impede a duplicação é só o `synchronized` de `ReservationService.java:21`. Com duas instâncias do serviço, ou
  com outro processo escrevendo, duas reservas sobrepostas passariam. Uma restrição `EXCLUDE` com `daterange`, ou uma
  trava, protegeria a regra.
- **Não há restrições** nas colunas de situação e plano, nem sobre "exatamente uma identificação".
- **Não há tabela de quartos nem chave estrangeira.** Os quartos estão em `RoomCatalog`. É uma escolha defensável
  ("os quartos quase nunca mudam"), mas `room_number` aceita qualquer inteiro.

## Comportamento

**Números:** 83 de 83 conferências passaram (`measures/behavior.md`):
- a parte comum, 46 de 46;
- os dados depois de reiniciar, 3 de 3;
- o cenário 05, 32 de 32;
- duas reservas ao mesmo tempo, 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API:
- os preços, com a temporada e o meio-para-o-par (599.76 e 212.42);
- as recusas, com os códigos certos;
- o ciclo da reserva e as devoluções;
- os dois relatórios em CSV;
- os três jeitos de identificação, com a recusa de nenhum, de dois ou de um incompleto;
- a fatura com o ISS de R$ 38,99;
- o booking receipt em inglês, com "1 night", "Discount" e "BRL 2,783.52";
- o e-mail sempre em português, com o documento certo dentro.

As reservas simultâneas também passaram, mas só porque o sistema roda num processo só (ver "Banco").

## Dúvidas

- A forma usada para o pattern é o método polimórfico no tipo de hóspede (`GuestIdentity.documentFor`), uma das três
  que a referência aceita como acerto. A criação da identidade a partir dos campos ficou num registro (`IdentityKinds`)
  com uma fábrica por tipo (`KIND`). Contei essa parte dentro do pattern, não como exagero.
- Na nota de testes, a falta de um teste da devolução no plano STANDARD poderia levar a nota a "com problemas". Mantive
  "bom" porque todas as regras da parte nova e quase todas as da parte comum estão conferidas com valores.
