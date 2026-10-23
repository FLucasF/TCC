# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (serviço, cálculo
de preço e `ReservationStore` com JDBC), `hotel.rooms`, `hotel.reports`, `hotel.documents`, `hotel.messages` e a
montagem em `hotel.Hotel`. Os dados ficam no PostgreSQL, com o esquema criado por migrações do Flyway. O agente mexeu
só no envio de e-mails: criou `EmailValidator`, `MessageProcessor`, `ProcessedMessage` e `MessageStatus`, fez o
`EmailSender.send` gravar a lista que o processador devolve (`final/src/main/java/hotel/messages/EmailSender.java:16-26`),
acrescentou a coluna `status` numa migração nova (`V2__add_message_status.sql`) e lê `HOTEL_TEST_MODE` no `main`
(`Hotel.java:46`). Tudo o que o roteiro confere funciona (65 de 65), e os testes passam (40 de 40). O ponto mais
sério é o design. O pedido diz que vão chegar mais regras e que cada instalação pode querer outra combinação, mas as
três regras ficaram em `if`s dentro de um único método, `MessageProcessor.process` (`MessageProcessor.java:19-38`). A
combinação por modo também está escrita ali dentro. Com isso, a "regra da madrugada", o "limite por dia" e a "pousada
sem cópia" vão exigir mexer nesse método. Há também um detalhe na validação: o domínio só precisa conter um ponto em
qualquer posição, quando o texto pede um ponto "no meio".

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos
da base (`measures/dependencies-base.md`). O agente não criou nenhum pacote, nenhuma dependência e nenhum ciclo;
`hotel.messages` passou de 3 para 7 classes (de 75 para 161 linhas).
**Nota:** bom

O ciclo entre `documents` e `reservations` já existia na base (`ReservationReceipt.java:3` importa `Reservation`, e
`ReservationService.java:3` importa `ReservationReceipt`), e o agente não o piorou. O código novo está no pacote
certo. As regras de envio (`MessageProcessor`, `EmailValidator`) não conhecem nem o Javalin nem o JDBC. O SQL continua
só em `SentMessages` (`SentMessages.java:20-31`). A montagem das peças novas fica num lugar só, `Hotel.java:26-28`, e o
modo de teste é lido do ambiente só no `main` (`Hotel.java:46`) e entra como parâmetro. As rotas não mudaram.

## Design de baixo nível

**Números:** das 3 regras pedidas (bloqueio, cópia, desvio para testes), nenhuma virou uma peça própria. As três são
`if`s dentro de `MessageProcessor.process` (`MessageProcessor.java:22`, `:27`, `:31-34`), e a combinação por modo
(cópia só fora do teste) também está nesse método. Não há interface de envio nem embrulhos.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern: a referência do Decorator chama de omissão "os três `if` (o endereço, o modo de teste, a cópia) dentro do
envio", e foi o que o agente fez, só que dentro de uma classe à parte chamada pelo envio. `process` primeiro
pergunta se o endereço é válido (`MessageProcessor.java:22-25`), depois pergunta `if (testMode)` e escolhe entre
desviar para testes (`:27-30`) e mandar com a cópia (`:31-34`). A regra "cópia só fora do modo de teste" é o `else`
desse `if`, e não uma escolha feita na montagem. Não existe uma interface de envio que o envio real e cada regra
implementem. O que aproxima o código do pattern: a lógica saiu do `EmailSender` e o validador é uma peça separada
(`EmailValidator.java`). O que afasta: o pedido anuncia mais duas regras que "podem valer sempre, só no modo de teste
ou só fora dele" e uma instalação "que não quer a cópia". Neste desenho, cada uma delas exige outro `if` ou outra
flag no mesmo método, que já mistura as regras com o modo. `Hotel.java:26` só consegue escolher entre `testMode`
verdadeiro ou falso, e não montar uma combinação.

O exagero: não há. `ProcessedMessage` é um record simples, que separa a decisão da gravação, e não há interface com
uma implementação só, nem fábrica, nem builder. O resultado é até enxuto demais para o que o texto anuncia.

O resto do código:
- `EmailValidator.java:20-21` aceita qualquer domínio que contenha um ponto (`domain.contains(".")`). Por isso
  `ana@gmail.` e `ana@.com` passam, e o texto pede "um nome com pelo menos um ponto no meio". É uma regra que o dono
  definiu e que ficou mais frouxa que o pedido.
- `EmailSender.java:18-25` guarda a primeira mensagem com uma variável que começa `null` e é conferida no laço.
  Funciona, mas esconde que a primeira mensagem é sempre a original: o laço depende da ordem que o processador
  devolve.
- O modo chega como um `boolean` solto (`MessageProcessor.java:12`), quando o pedido fala de combinações por
  instalação.

São defeitos localizados. Os nomes são claros, os tipos estão certos (`MessageStatus` é um `enum`) e não há estado
mutável compartilhado.

## Testes

**Números:** 40 testes rodaram, 0 falhas, 0 erros, 0 pulados; 40 métodos escritos, contra 26 na base. A cobertura de
linhas passou de 93,8% para 94,2% (516 de 548) e a de ramos de 87,1% para 89,1% (123 de 138). Em `hotel.messages`
são 73 de 77 linhas e 24 de 24 ramos (`measures/tests.md`).
**Nota:** bom

O agente testou a parte nova em três níveis, sempre com valores:
- unidade: `MessageProcessorTest.java` (normal, teste, bloqueado nos dois modos) e `EmailValidatorTest.java`
  (`ana@gmail`, `ana.gmail.com`, `@gmail.com`, `ana@`, `ana@@gmail.com`, vazio, `null`);
- contra o banco: `EmailSenderIntegrationTest.java`;
- pela API: `MessageApiTest.java`, que sobe o `Hotel` com `testMode` verdadeiro e falso e confere `status`, `to`, o
  assunto com "[Cópia] " ou "[TESTE] " e a linha "Destinatário original: ..." (`MessageApiTest.java:93-94`).

Os testes da base foram ajustados ao novo comportamento, sem ficar mais fracos: a contagem de mensagens passou de 1
para 2 e ganhou conferências da cópia (`ReservationServiceTest.java:93-100`, `HttpApiTest.java:81-85`).

A pergunta da mutação:
- trocar o assunto "[Cópia] " ou tirar a linha do destinatário original faz testes falharem;
- inverter o `if (testMode)` também;
- trocar `atCount != 1` por `atCount > 1` faz falhar o caso `ana.gmail.com`.

O que fica sem rede:
- a exigência do ponto "no meio" do domínio: nenhum teste usa `ana@gmail.` ou `ana@.com`, e é por isso que o desvio
  do validador passa;
- a leitura de `HOTEL_TEST_MODE` no `main` (`Hotel.java:46`).

Isso é pouco, e o `mvn verify` passa.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, não há chave estrangeira sem
índice nem data em texto. `V1__create_tables.sql` está igual ao da base, e `V2__add_message_status.sql` é nova. A
coluna nova, `messages.status`, é `text NOT NULL` sem CHECK, como as outras colunas de situação que a base já tinha
(`measures/database.md`).
**Nota:** bom

A mudança no esquema foi feita do jeito certo: uma migração nova, sem editar a da base (`V2__add_message_status.sql:2`).
O `DEFAULT 'SENT'` dá um valor sensato às mensagens que já estavam gravadas, e quem já roda o sistema sobe sem perder
dados. O SQL novo usa parâmetros (`SentMessages.java:23-29`) e devolve a conexão (try-with-resources). A falta de
CHECK em `status` segue o padrão da base, em que nenhuma coluna de situação tem CHECK; a leitura com
`MessageStatus.valueOf` (`SentMessages.java:51`) quebraria com um valor fora do `enum`, mas só o código grava essa
coluna. Um ponto menor: a original e a cópia são gravadas em duas conexões separadas, sem transação
(`EmailSender.java:19-24`), e uma falha entre as duas deixaria a original sem a cópia do contador. Não chega a pesar
na nota. A proteção contra reserva dupla (EXCLUDE com `daterange`) vem da base e continua lá.

## Comportamento

**Números:** 65 de 65 conferências passaram:
- parte comum: 46 de 46;
- reinício: 3 de 3;
- cenário, modo normal: 9 de 9;
- cenário, modo de teste: 5 de 5;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, as três regras funcionam como ele pediu:
- endereço inválido (`ana@gmail`, `ana.gmail.com`, `ana@@gmail.com`, `@gmail.com`) aparece como `BLOCKED`, com o
  endereço original e sem cópia;
- a mensagem enviada ganha logo depois a cópia para `arquivo@pousadamarealta.com.br`, com "[Cópia] " no assunto e o
  mesmo texto;
- com `HOTEL_TEST_MODE=true`, tudo vai para `testes@pousadamarealta.com.br`, com "[TESTE] " e a linha do destinatário
  original, sem cópia, e o inválido continua bloqueado.

Tudo o que já funcionava continua funcionando, e os dados resistem ao reinício. Fora do roteiro, pela leitura de
`EmailValidator.java:20-21`, um endereço como `ana@gmail.` (ponto no fim do domínio) seria enviado em vez de
bloqueado. É um desvio pequeno da regra "ponto no meio"; o roteiro não o testa e não muda a nota (ele já pesou no
design).

## Dúvidas

- A classe do pattern ficou entre `omissão` e `malfeito`. Existe uma peça separada (`MessageProcessor`), mas ela é um
  único método com os três `if`s e a combinação por modo dentro, sem interface de envio e sem uma peça por regra. Pela
  referência, isso é a descrição de omissão, só que fora do `EmailSender`; por isso fiquei com `omissão`.
- Não confirmei pela API que `ana@gmail.` é aceito; a conclusão vem da leitura de `EmailValidator.java:20-21`.
