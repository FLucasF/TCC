# Revisão: manutenção, cenário 07

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras em
`ReservationService` e o SQL em `ReservationStore`), `hotel.rooms`, `hotel.reports`, `hotel.documents`,
`hotel.messages` (o envio de e-mail, que só grava na tabela `messages`) e `hotel`, onde fica a montagem (`Hotel`).
Uma requisição de confirmação vai de `HttpApi` para `ReservationService.confirm`, que grava a reserva e chama
`EmailSender.send` (`final/src/main/java/hotel/reservations/ReservationService.java:56-69`). Os dados ficam no
PostgreSQL, num esquema criado pelo Flyway. O agente pôs a mudança toda dentro de `hotel.messages`. Criou uma
interface `MessageRule` e uma classe para cada regra (`BlockInvalidAddress`, `CopyToArchive`,
`RedirectToTestAddress`), mais um embrulho `ScopedRule` que diz em que modo cada regra vale. A lista das regras de
cada instalação fica em `MessageRules.marealta(testMode)`, que a montagem (`Hotel.java:73`) entrega ao
`EmailSender`, e o modo vem de `HOTEL_TEST_MODE` no `main` (`Hotel.java:92`). O status entrou no banco por uma
migração nova (`V2__message_status.sql`). Todos os testes passam (31) e as 65 conferências do roteiro passam. Não
achei nenhum defeito sério. Os pontos menores são as duas gravações (a original e a cópia) feitas fora de uma
transação e a coluna `status` sem `CHECK`.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base (`measures/dependencies-base.md`). Há 1
ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. `hotel.messages` passou de 3 classes e 75
linhas para 12 classes e 253 linhas, e continua sem depender de nenhum outro pacote do projeto.
**Nota:** bom

O agente não criou nenhuma dependência nova entre pacotes. As regras novas são classes puras em `hotel.messages`,
sem Javalin e sem JDBC. O único acesso ao banco continua em `SentMessages`, que ganhou só a coluna nova
(`final/src/main/java/hotel/messages/SentMessages.java:20-31` e `:41-51`). A combinação das regras é escolhida na
montagem: `Hotel.java:68-73` recebe o modo e passa `MessageRules.marealta(testMode)`, e só o `main` lê a variável de
ambiente (`Hotel.java:92`). A borda HTTP não mudou: `GET /messages` continua serializando os `Message`
(`final/src/main/java/hotel/web/HttpApi.java:81`), que agora trazem o `status`. O ciclo documents ↔ reservations
existe porque `ReservationService.java:3` importa `ReservationReceipt` e `ReservationReceipt.java:3` importa
`Reservation`. Ele veio da base, e o agente não o piorou.

## Design de baixo nível

**Números:** 3 regras pedidas (bloqueio, cópia, desvio para teste), as 3 como classes próprias da estrutura
`MessageRule`. Nenhum `if` sobre o modo de teste dentro do envio ou dentro das regras. A combinação por modo fica
num só lugar (`MessageRules.java:15-20`).
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

O pattern pedido é o Decorator. O agente fez uma lista de filtros aplicada pelo envio, que a referência aceita como
alternativa equivalente: cada regra é uma peça própria, e a combinação por modo é decidida na montagem.

- **O que aproxima:**
  - Cada regra implementa `MessageRule.apply(Draft) -> List<Draft>`, e pode devolver o rascunho mudado, bloqueado
    ou seguido de outros (`MessageRule.java:11-13`).
  - O bloqueio está em `BlockInvalidAddress.java:9-23`, a cópia em `CopyToArchive.java:11-14` e o desvio em
    `RedirectToTestAddress.java:11-17`. Nenhuma das três pergunta pelo modo de teste.
  - O "vale sempre / só no teste / só fora dele", que o texto anuncia para as regras futuras, é um Decorator de
    verdade sobre a regra: `ScopedRule.java:6-34` embrulha qualquer `MessageRule`, decide se está ativo uma vez, na
    construção (`:17-24`), e só repassa ou deixa passar (`:31-33`).
  - Uma instalação sem a cópia, como a segunda pousada, é uma outra lista em `MessageRules`, sem mexer nas regras.
- **O que afasta:** não é um embrulho do próprio envio. O `EmailSender` percorre a lista e decide sozinho que um
  rascunho bloqueado não passa pelas regras seguintes (`EmailSender.java:27-33`). É uma política da corrente
  escrita no envio, e não numa regra. Hoje ela dá o resultado pedido (o bloqueado não ganha cópia nem desvio), mas
  uma regra futura que quisesse tratar mensagens bloqueadas não teria como. É por isso que dei alternativa
  equivalente, e não acerto.

O exagero ficou contido. `Draft` existe porque o rascunho ainda não tem id, ao contrário de `Message`.
`MessageStatus` é um enum para um valor que vai para o banco e para a API. `ScopedRule` responde a um pedido
explícito do texto. O único resto é o construtor `EmailSender(SentMessages)` sem regras (`EmailSender.java:15-17`),
que ficou para os testes antigos e não chega a ser estrutura sem necessidade.

O resto do código está limpo. O status é um tipo (`MessageStatus`, e não texto solto), os records são imutáveis,
não há `static` mutável e os nomes dizem o que as classes fazem. Há dois detalhes:

- `EmailSender.send` devolve `null` se uma regra devolver uma lista vazia (`EmailSender.java:34-41`). Nenhuma regra
  atual faz isso, e quem chama ignora o retorno (`ReservationService.java:65`).
- A linha de `Hotel.java:73` ficou longa.

Nenhum dos dois pede mudança antes de aceitar.

## Testes

**Números:** 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados (a base tinha 26, todos mantidos). São 31
métodos de teste escritos. A cobertura é de 94,5% das linhas (534 de 565) e 89,1% dos ramos (131 de 147); na base,
era de 93,8% e 87,1%. Em `hotel.messages`, cobrem 89 de 93 linhas e 32 de 33 ramos.
**Nota:** bom

O agente escreveu `EmailSenderTest` com 5 testes contra o banco de verdade, montando as regras pelo mesmo
`MessageRules.marealta` da produção, nos dois modos (`final/src/test/java/hotel/messages/EmailSenderTest.java:16-17`).

- **A validação do endereço** é conferida com os exemplos do texto e com as bordas: sem nada antes do @, dois @,
  ponto na ponta, `null` (`:25-35`). Trocar `at < 1` por `at < 0`, ou o `substring(1, length - 1)` por outro corte,
  faria algum teste falhar.
- **A cópia** é conferida com valores: o destinatário, o assunto "[Cópia] Oi", o mesmo texto, `SENT` e a posição
  logo depois da original (`:38-49`).
- **O bloqueio sem cópia** é conferido em `:52-59`.
- **O modo de teste** é conferido com o endereço, o assunto, a primeira linha "Destinatário original: …" e a
  ausência de cópia (`:62-71`). O bloqueio no modo de teste, com o endereço original, também (`:74-82`).

Trocar o escopo de uma regra em `MessageRules` (por exemplo, a cópia para `ALWAYS`) quebraria o teste de `:62`. A
única mudança num teste antigo foi em `HttpApiTest.java:81-84`, que passou de 1 para 2 mensagens e agora confere
também o `status` e a cópia. É a mudança pedida, e não um enfraquecimento.

O que fica sem rede: a leitura de `HOTEL_TEST_MODE` no `main` (`Hotel.java:92`, o pacote `hotel` tem 2 de 4 ramos
cobertos) e o status `BLOCKED` pela API, que só é conferido na unidade. O roteiro do comportamento confere os dois.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`. Não há chave estrangeira sem
índice nem data em texto. São 2 migrações do Flyway: a `V1` igual à da base e a `V2__message_status.sql` nova. As
conferências acusam colunas de situação e de tipo sem `CHECK`, entre elas a nova `messages.status`.
**Nota:** bom

O agente evoluiu o esquema do jeito certo. A migração nova
(`final/src/main/resources/db/migration/V2__message_status.sql:2`) acrescenta a coluna com `NOT NULL DEFAULT 'SENT'`,
o que preserva as mensagens de quem já roda o sistema, e não toca na `V1`. O SQL novo usa parâmetros
(`SentMessages.java:23-28`), e a conexão é devolvida por try-with-resources.

Pontos menores:

- `messages.status` é `text` sem `CHECK`. Segue a convenção da base, que também não tem `CHECK` em `channel` nem
  em `reservations.status`, e o enum em Java (`SentMessages.java:28` e `:51`) é a única guarda.
- A mensagem original e a cópia são gravadas em duas conexões, uma por vez (`EmailSender.java:35-40`), sem
  transação. Se a segunda gravação falhar, a lista fica com a original sem a cópia. A falta de transação entre
  `store.update` e o envio já vinha da base (`ReservationService.java:62-68`).

A proteção contra reservas duplicadas (o `EXCLUDE USING gist`) veio da base e continua intacta.

## Comportamento

**Números:** 65 de 65 conferências passaram e nenhuma falhou:

| parte do roteiro | passaram |
|---|---|
| parte comum do enunciado | 46 de 46 |
| dados depois de reiniciar | 3 de 3 |
| cenário 07, modo normal | 9 de 9 |
| cenário 07, modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, isto quer dizer que:

- Tudo o que funcionava continua funcionando.
- Um e-mail válido sai com `SENT` e ganha logo em seguida a cópia para `arquivo@pousadamarealta.com.br`, com
  "[Cópia] " no assunto e o mesmo texto.
- Os endereços "ana@gmail", "ana.gmail.com", "ana@@gmail.com" e "@gmail.com" ficam na lista como `BLOCKED`, com o
  endereço original e sem cópia.
- Com `HOTEL_TEST_MODE=true`, a mensagem vai só para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a
  linha "Destinatário original: …" no começo, sem cópia. O endereço inválido continua bloqueado com o endereço
  original.
- As mensagens continuam lá depois de reiniciar o serviço.

## Dúvidas

Fiquei entre `acerto` e `alternativa equivalente`. As regras e o `ScopedRule` são embrulhos com a mesma interface,
mas não embrulham o envio: o `EmailSender` aplica a lista e guarda a política de que um rascunho bloqueado não passa
pelas regras seguintes (`EmailSender.java:27-33`). Por isso a estrutura é mais uma lista de filtros do que um
Decorator do envio, e foi isso que decidiu a classe.
