# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin dividido em pacotes por assunto: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations`
(o serviço, o preço e o acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.reports`,
`hotel.documents`, e a montagem em `hotel.Hotel`. Os dados ficam no PostgreSQL, com o esquema nas migrações do Flyway.
Uma confirmação passa por `HttpApi` → `ReservationService.confirm` → `EmailSender.send` → `SentMessages.add`, que grava
na tabela `messages`. A mudança ficou toda em `hotel.messages`. Cada regra é uma peça própria que implementa
`MessageRule`: `BlockInvalidAddress`, `RedirectToTests` e `CopyToArchive`. As peças agem sobre um `Draft` imutável. Uma
lista de `ScopedRule` (a regra mais o seu `RuleScope`: sempre, só no teste ou só fora dele) é montada em
`EmailRules.mareAlta()` e entregue ao `EmailSender` em `Hotel.java:29`. O `EmailSender` aplica as regras em ordem e
grava a mensagem com `status`; uma migração nova, `V2__message_status.sql`, cria essa coluna. O modo de teste vem de
`HOTEL_TEST_MODE` (`Hotel.java:50`). As 65 conferências do roteiro passaram, nos dois modos, e os 31 testes também.
Não há defeito sério. O que existe de mais fraco é pequeno: o `EmailSender` ainda recebe o `testMode` para filtrar as
regras pelo escopo, e a mensagem original e a cópia são gravadas sem uma transação que as una.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, na `final` e na `base` igualmente (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.messages` passou de 3 classes e 75 linhas para 12 classes e 233 linhas, e continua sem depender de nenhum outro
pacote do projeto (0 dependências de saída, 3 pacotes que o usam).
**Nota:** bom

A mudança não criou nenhuma dependência nova entre pacotes. As regras de envio moram em `hotel.messages` e não
conhecem nem o Javalin nem o JDBC: `BlockInvalidAddress.java:12-14`, `CopyToArchive.java:9-11` e
`RedirectToTests.java:9-13` só transformam um `Draft`. O SQL continua só em `SentMessages.java:116-132`, que ganhou a
coluna `status`. A combinação de regras de cada instalação está declarada em `EmailRules.java:12-17`, e a montagem, a
leitura da variável de ambiente e a passagem do modo continuam num lugar só, a classe `Hotel` (`Hotel.java:25-35` e
`Hotel.java:50-51`). A borda HTTP ficou igual: `/messages` continua só serializando `sentMessages.all()`
(`HttpApi.java:81`), e o campo `status` aparece porque foi acrescentado ao record `Message` (`Message.java:3`). O
`ReservationService` continua chamando `emailSender.send` sem saber de regra nenhuma (`ReservationService.java:65`). O
ciclo `documents`↔`reservations` veio da base.

## Design de baixo nível

**Números:** 3 regras pedidas, as 3 dentro da estrutura (`BlockInvalidAddress`, `CopyToArchive`, `RedirectToTests`) e
nenhuma fora dela. Não sobrou nenhum `if` de endereço, de modo de teste ou de cópia fora das peças. A combinação por
modo está declarada em `EmailRules.java:12-17`. Na `final`, o único lugar que olha o modo é a checagem genérica de
escopo em `EmailSender.java:31`.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** O caso pede um Decorator. O agente fez outra estrutura que a referência aceita: uma lista de filtros
aplicada pelo envio, com cada regra como peça própria (`MessageRule.java:4-7`, implementada por
`BlockInvalidAddress.java:6`, `CopyToArchive.java:4` e `RedirectToTests.java:4`). Cada peça recebe o `Draft` e devolve
o seguinte: ela pode reescrever destino, assunto e texto (`RedirectToTests.java:10-12`), acrescentar uma cópia
(`CopyToArchive.java:10`) ou bloquear (`BlockInvalidAddress.java:13`).

O que aproxima do acerto:
- Nenhuma regra conhece o modo de teste. A cópia não pergunta "estou em teste?"; quem decide é o escopo declarado na
  montagem (`EmailRules.java:15-16`).
- A ordem também é decidida ali: o bloqueio vem primeiro e interrompe a corrente (`EmailSender.java:28-30`).
- Uma segunda pousada sem a cópia é só outra lista, e o teste em `EmailSenderTest.java:77-86` monta exatamente essa
  combinação.
- As regras anunciadas (madrugada, limite por dia) entram como novas peças com um `RuleScope`, que é o próprio
  "sempre / só no teste / só fora dele" do pedido (`RuleScope.java:9-15`).

O que afasta do acerto: o envio de dentro ainda recebe o `testMode` (`EmailSender.java:12`, `EmailSender.java:21`) e
filtra as regras pelo escopo a cada envio (`EmailSender.java:31`). A pergunta é genérica, não por regra, então não
chega a ser o "envio que pergunta pelo modo" que a referência chama de malfeito. Mas o filtro poderia ser feito uma vez,
na montagem. Esse ponto está nas Dúvidas.

**O exagero.** Não há estrutura sem uso:
- a interface `MessageRule` tem três implementações reais;
- `RuleScope` e `ScopedRule` respondem diretamente ao que o pedido anuncia;
- `EmailRules` é uma classe só com o método estático das regras desta instalação, que o pedido justifica ("cada
  instalação do sistema pode querer outra combinação");
- os métodos `with*` do `Draft` (`Draft.java:13-33`) são usados pelas regras.

Os construtores antigos `EmailSender(SentMessages)` (`EmailSender.java:15-17`) e `Hotel(DataSource)`
(`Hotel.java:21-23`) foram mantidos para os testes da base. É pouco e não pesa.

**O resto do código.** O `send` é curto e legível (`EmailSender.java:25-43`). O status virou `enum`
(`MessageStatus.java:3-6`), e não texto solto. A mensagem bloqueada guarda o endereço, o assunto e o texto originais,
porque é gravada a partir dos parâmetros de entrada e não do `Draft` (`EmailSender.java:36`), como o pedido quer. A
validação do endereço está numa expressão só, com o critério do dono escrito no comentário
(`BlockInvalidAddress.java:8-9`). Os detalhes são menores:
- o construtor canônico do record `Draft` aceita uma lista mutável sem fazer cópia (`Draft.java:7`); só `withCopy` faz
  `List.copyOf` (`Draft.java:32`);
- o `Hotel` lê a variável com `Boolean.parseBoolean` (`Hotel.java:50`), que trata qualquer valor diferente de "true"
  (por exemplo, "TRUE " com espaço) como modo normal. É uma escolha aceitável para o pedido.

Os defeitos de design da base (o ciclo de pacotes, o `synchronized` no serviço) não foram tocados.

## Testes

**Números:**
- Na `final`, 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 31 métodos de teste escritos.
- Na `base`, eram 26 testes, todos passando.
- Cobertura na `final`: 94,3% das linhas (525 de 557) e 88,7% dos ramos (118 de 133). Na base: 93,8% e 87,1%.
- `hotel.messages` passou de 29/33 linhas e 2/2 ramos para 80/84 linhas e 19/19 ramos.

**Nota:** bom

O agente escreveu `EmailSenderTest` (5 testes), contra o banco de verdade, e conferiu as regras novas com valores:
- envio normal: o destino, o assunto e o status da original, e a cópia logo depois, para
  `arquivo@pousadamarealta.com.br`, com "[Cópia] Olá", o mesmo texto e SENT (`EmailSenderTest.java:25-38`);
- oito endereços inválidos, entre eles `ana@gmail`, `ana.gmail.com`, `a@@b.com`, `a@.com`, `a@gmail.` e o vazio, todos
  BLOCKED, com o assunto original e sem cópia (são 8 mensagens para 8 envios) (`EmailSenderTest.java:40-54`);
- modo de teste: o destino de testes, "[TESTE] Olá", o texto com "Destinatário original: ana@gmail.com\n" e uma
  mensagem só (`EmailSenderTest.java:56-65`);
- endereço inválido no modo de teste: continua BLOCKED, com o endereço original (`EmailSenderTest.java:67-75`);
- uma combinação diferente, sem a cópia fora do teste (`EmailSenderTest.java:77-86`).

Um teste da base foi reforçado, não enfraquecido: `HttpApiTest.java:81-84` agora espera 2 mensagens, o `status` SENT e
a cópia em segundo lugar. `ReservationServiceTest` ficou idêntico ao da base.

Pela pergunta da mutação, há duas brechas:
1. Nenhum teste usa um endereço válido com mais de um ponto depois do @ (como `ana@mail.com.br`). Uma expressão que
   aceitasse um ponto só passaria em todos os testes e bloquearia hóspedes com e-mail `.com.br`.
2. A leitura de `HOTEL_TEST_MODE` em `Hotel.main` (`Hotel.java:50`) não é testada: o pacote `hotel` tem 28 de 34
   linhas cobertas.

Além disso, o teste de escopo confere só a quantidade de mensagens (`EmailSenderTest.java:85`). São lacunas pequenas
perto do que está conferido.

## Banco

**Números:**
- O serviço criou 3 tabelas num banco vazio (`messages`, `reservations` e `rooms`), e todas têm chave primária.
- Há 2 migrações do Flyway: a `V1__create_tables.sql` está igual à da base e a `V2__message_status.sql` é nova.
- O dinheiro está todo em `numeric(10,2)`. Não há chave estrangeira sem índice nem data guardada como texto.
- Colunas de situação sem CHECK: `messages.status` (nova) e, desde a base, `messages.channel`,
  `reservations.status`, `reservations.rate_plan` e `rooms.type`.
- O código usa 0 transações explícitas.

**Nota:** bom

O agente evoluiu o esquema do jeito certo. Fez uma migração nova, sem editar a da base, com
`ALTER TABLE messages ADD COLUMN status text NOT NULL DEFAULT 'SENT'` (`V2__message_status.sql:2`). Com isso, quem já
roda o sistema sobe sem erro, e as mensagens antigas ficam como enviadas. O SQL novo usa parâmetros (`?`), inclusive
para o status (`SentMessages.java:119-124`), e a conexão é devolvida por try-with-resources.

Pontos menores:
- `messages.status` não tem CHECK (`SENT`/`BLOCKED`). Segue a convenção da base, que também não tem CHECK nas colunas
  de situação, mas um valor estranho no banco quebraria o `GET /messages` em `MessageStatus.valueOf`
  (`SentMessages.java:147`).
- A original e a cópia são dois `INSERT` em conexões separadas, sem transação (`EmailSender.java:38-41`). Se a segunda
  gravação falhar, a original fica sem a cópia. A ordem "logo depois" depende da sequência do `id`. Num processo só,
  ela está garantida porque `ReservationService.confirm` é `synchronized` (`ReservationService.java:57`, que vem da
  base).

## Comportamento

**Números:** 65 de 65 conferências passaram, e nenhuma falhou (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 07 no modo normal | 9 de 9 |
| cenário 07 no modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, isto quer dizer que tudo o que ele pediu funciona pela API.

No modo normal:
- o e-mail para um endereço válido sai com `status` SENT;
- a cópia vem logo depois, para `arquivo@pousadamarealta.com.br`, com "[Cópia] " no assunto e o mesmo texto;
- `ana@gmail`, `ana.gmail.com`, `ana@@gmail.com` e `@gmail.com` aparecem como uma mensagem só, BLOCKED, com o endereço
  digitado e sem cópia.

Com `HOTEL_TEST_MODE=true`:
- a mensagem vai só para `testes@pousadamarealta.com.br`, com "[TESTE] " e a primeira linha
  "Destinatário original: ana@example.com", e sem a cópia;
- o endereço inválido continua BLOCKED, com o endereço original.

O que já funcionava (preços, recusas, cancelamentos, relatórios, os dados depois de reiniciar, as reservas
simultâneas) continua funcionando.

## Dúvidas

1. **A classe do pattern.** Ficou entre `alternativa equivalente` e `acerto`, mas o `EmailSender` recebe o `testMode`
   e filtra a lista de regras pelo escopo a cada envio (`EmailSender.java:31`). A referência pede que "a combinação por
   modo seja decidida na montagem". Aqui ela é declarada na montagem (`EmailRules.java:12-17`, `Hotel.java:29`) e
   avaliada no envio por uma checagem genérica, sem nenhuma regra perguntando pelo modo. Li isso como alternativa
   equivalente e não como malfeito. Um leitor mais estrito poderia ver o envio "ainda perguntando pelo modo de teste".
2. **O que o pedido não fechou.** Não ficou definido o que acontece com a cópia para o arquivo se, numa instalação,
   ela valer também no modo de teste. Com a ordem atual, a cópia sairia com "[Cópia] [TESTE] ". O pedido não diz nada
   sobre isso.
