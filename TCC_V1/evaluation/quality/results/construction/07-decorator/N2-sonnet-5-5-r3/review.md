# Revisão: construção, cenário 07

## Visão geral

O projeto tem sete pacotes: `web` (Javalin), `service`, `domain`, `mail`, `persistence`, `report` e a raiz com o `App`. Uma requisição faz este caminho: o handler de `web/Api.java:42-61` lê o corpo com `RequestParser`, chama o `ReservationService` e serializa um `ReservationView`. O serviço abre uma transação (`persistence/Database.java:28-42`), trava a linha do quarto (`persistence/RoomRepository.java:29-37`), deixa a regra para o domínio (`domain/Reservation.java`, `domain/Pricing.java`, `domain/RatePlan.java`) e grava pelos repositórios JDBC. Os dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:44-94`). A parte do fim do enunciado (as regras de envio) virou um Decorator limpo. A interface é `mail/Outbox.java`. A mensagem é gravada na lista em `persistence/MessageLog.java`, a peça mais de dentro, e cada regra é um embrulho próprio. A pilha é montada num lugar só, `mail/MailPipeline.java:26-43`, conforme o modo. As 65 conferências do roteiro passaram. Os defeitos mais sérios estão em duas colunas. Nos testes, há poucos: 8 testes e 18,2% das linhas cobertas, sem nada sobre a situação da reserva, o comprovante, os relatórios, a API ou o banco. No banco, o esquema é criado no código, sem migrações, e a chave estrangeira não tem índice.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo. `domain` não depende de nenhum outro pacote do projeto e é usado por 4; `web` depende de 4. O Javalin só aparece em `com.marealta` e `web`. O JDBC aparece em `persistence`, mas também em `service` e `report`, que recebem a `Connection` da transação.
**Nota:** bom

- **A borda HTTP é fina.** Cada rota é uma linha: lê o parâmetro, chama o serviço e escreve a resposta (`web/Api.java:42-61`). A validação do formato fica em `web/RequestParser.java`. Os erros viram resposta num lugar só, `web/Api.java:35-40`, com o status HTTP guardado no próprio `ErrorCode` (`domain/ErrorCode.java:4-9`).
- **As regras ficam no domínio, sem Javalin nem SQL.** As transições de estado e o reembolso estão em `domain/Reservation.java:50-71`. O preço por noite e a temporada estão em `domain/Pricing.java:15-34`. O desconto e a devolução estão em `domain/RatePlan.java:17-23`.
- **O acesso ao banco está isolado.** O SQL fica nos repositórios (`persistence/ReservationRepository.java`, `persistence/RoomRepository.java`, `persistence/MessageLog.java`).
- **A montagem fica num lugar só**, em `App.java:18-35`. Cada peça recebe as suas dependências pelo construtor, e não há `static` mutável.
- **Pontos menores, que não mudam a nota:**
  - O serviço e os relatórios conhecem a `Connection` do JDBC, porque recebem o lambda da transação (`service/ReservationService.java:36-48`, `:81-86`; `report/Reports.java:27`). Eles demarcam a transação, mas não montam SQL.
  - `GET /messages` vai direto do `web` para o `MessageLog` da persistência (`web/Api.java:7`, `:57`), sem passar por um serviço.
  - O `Receipt` do domínio usa uma anotação do Jackson (`domain/Receipt.java:3`, `:16`).
- **Nenhuma classe domina.** A maior tem 127 linhas (`ReservationRepository`).

## Design de baixo nível

**Números:** as 3 regras pedidas (bloquear endereço inválido, copiar para o arquivo, desviar no modo de teste) estão na estrutura: 3 de 3, nenhuma fora dela. São 4 embrulhos de `Outbox`: `AddressValidation`, `ArchiveCopy`, `TestRedirect` e `Serialized`. Nenhum `if` sobre o modo de teste fica fora da montagem.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A interface do envio é `mail/Outbox.java:4-6`. O envio de verdade (que, por enquanto, só grava a mensagem na lista) é `persistence/MessageLog.java:16-41`, que não sabe de modo, de cópia nem de bloqueio. Cada regra é um embrulho com a mesma interface, que chama o seguinte:

- `mail/AddressValidation.java:12-14` marca como bloqueada a mensagem com endereço inválido. A validação em `:16-31` segue a definição do dono: um único @, algo antes dele e um ponto no meio do domínio.
- `mail/ArchiveCopy.java:14-20` manda a cópia logo depois da original, só quando ela saiu com `SENT`.
- `mail/TestRedirect.java:14-21` troca o destinatário, o assunto e a primeira linha do texto.

A combinação por modo é decidida só na montagem, `mail/MailPipeline.java:26-43`. Cada regra recebe um escopo (`ALWAYS`, `TEST_ONLY`, `LIVE_ONLY`, em `:12-18`), que é exatamente o que o enunciado anuncia para as regras futuras (madrugada, limite por dia): "pode valer sempre, só no modo de teste ou só fora dele". A segunda pousada, que não quer a cópia, desliga a regra pela configuração (`mail/MailSettings.java:4`; `App.java:22-24`), sem tocar nos embrulhos. Nenhum embrulho pergunta pelo modo.

O único acoplamento entre embrulhos é este: a mensagem bloqueada não para em `AddressValidation`, ela continua pela pilha. Por isso `ArchiveCopy` (`:16`) e `TestRedirect` (`:15`) olham o `status` dela. Isso diz respeito ao estado da mensagem, não ao modo, e não muda a classe. Mas uma regra nova vai ter de lembrar de deixar passar a mensagem bloqueada.

**O exagero.** `Serialized` (`mail/Serialized.java:11-14`) é um embrulho a mais, com `synchronized`, para que a cópia fique logo depois da original mesmo com requisições ao mesmo tempo. Ele resolve um requisito do texto ("a cópia aparece logo depois da original") e mora dentro do pattern. `Database.Work` (`persistence/Database.java:11-14`) é a interface funcional que a transação precisa. Não há interface com uma implementação só, fábrica ou camada que só repassa.

**O resto do código.**

- Os tipos estão certos: `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:15-17`; `domain/Pricing.java:19`, `:23`, `:28`; `domain/RatePlan.java:18`), `LocalDate` para datas e `enum` para situação, plano, tipo de quarto e canal.
- O estado da `Reservation` só muda pelas transições, que conferem a situação (`domain/Reservation.java:50-71`, `:84-88`).
- Não há duplicação relevante, e os métodos são curtos.
- Pontos menores, que não pedem mudança antes de aceitar:
  - `catch (... | NullPointerException e)` usado como controle de fluxo para o campo ausente (`web/RequestParser.java:46`, `:54`).
  - O e-mail de confirmação é gravado depois do commit da confirmação, numa transação própria (`service/ReservationService.java:57-59`; `persistence/MessageLog.java:25`). Se essa gravação falhar, a reserva fica confirmada sem a mensagem e o cliente recebe 500.
  - O `assignId` público na entidade (`domain/Reservation.java:91`).

## Testes

**Números:** 8 testes rodaram, 0 falhas, 0 erros, 0 pulados; 8 métodos de teste escritos em 2 arquivos. A cobertura de linhas é de 89 de 490 (18,2%), e a de ramos, de 38 de 118 (32,2%). No pacote `mail` é quase total: 61 de 62 linhas e 27 de 28 ramos. Em `persistence`, `service`, `web` e `report` é 0. Nenhum teste pela API nem contra o banco.
**Nota:** com problemas

**O que está testado, com valores.** A parte nova do cenário está bem testada em `MailPipelineTest`, com uma lista de mensagens em memória no lugar da gravação:

- a original seguida da cópia, com "[Cópia] " no assunto e o mesmo texto (`MailPipelineTest.java:27-34`);
- seis endereços inválidos, cada um bloqueado, sem cópia e com o endereço original, inclusive `a@.com` e `a@b.` (`:37-45`);
- o desvio no modo de teste, com o assunto, a primeira linha do texto e a falta da cópia, mesmo com o arquivo ligado (`:48-55`);
- o bloqueio no modo de teste (`:58-63`);
- a instalação sem cópia (`:66-69`).

Pela pergunta da mutação, trocar `LIVE_ONLY` por `ALWAYS`, apagar a conferência de `SENT` em `ArchiveCopy` ou afrouxar o laço de `isValid` faria algum teste falhar. Em `PricingTest` estão o valor do enunciado (599,76, `PricingTest.java:16-21`), a fronteira de 14/15 de dezembro (549,78, `:24-27`) e o desconto NON_REFUNDABLE (89,96 e 509,80, `:30-34`).

**O que ficou sem rede.**

- As transições de estado e as recusas com `OPERATION_NOT_ALLOWED` (`domain/Reservation.java:50-71`).
- O cancelamento e a devolução por plano: trocar `refundable ? amountPaid : ZERO` não derrubaria nenhum teste.
- O comprovante (`domain/Receipt.java:21-34`), com a linha de desconto e o "1 diária", e o formato do dinheiro (`domain/Money.java:20-25`).
- O texto do e-mail de confirmação.
- Os dois relatórios em CSV.
- A validação da requisição, os códigos de erro HTTP e a recusa de quarto ocupado (`persistence/ReservationRepository.java:27-39`).
- A persistência inteira.

O texto não pedia testes. O que existe é bom e confere a parte do cenário, mas metade das regras da parte comum não tem teste nenhum.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. As conferências acusaram duas coisas: a chave estrangeira `reservations(room_number) -> rooms` sem índice, e nenhuma restrição que proteja uma regra (nenhum CHECK sobre situação, plano, tipo ou canal, nenhum UNIQUE ou EXCLUDE). O esquema é criado por `CREATE TABLE` no código Java, sem arquivo de migração. Há 2 `FOR UPDATE`, 1 pool (HikariCP) e 0 `DriverManager`.
**Nota:** com problemas

**O que está certo.**

- Os tipos estão certos: dinheiro em `NUMERIC(12,2)`, datas em `DATE` e `NOT NULL` em tudo o que é obrigatório (`persistence/Database.java:48-82`). Só `refund` aceita nulo, de propósito.
- A reserva fica ligada ao quarto por `REFERENCES rooms(number)` (`:58`).
- Os quartos entram uma vez, com `ON CONFLICT DO NOTHING` (`:83-90`), e a criação do esquema é protegida por `pg_advisory_xact_lock` (`:47`).
- Todo SQL usa parâmetros `?`. As concatenações que o script listou (`persistence/ReservationRepository.java:82`, `:92`, `:98`) só juntam texto constante.
- As conexões vêm do pool e são devolvidas por try-with-resources (`persistence/Database.java:29`).
- A reserva dupla é impedida no banco. A criação trava a linha do quarto com `SELECT ... FOR UPDATE` (`persistence/RoomRepository.java:29-37`) antes de procurar conflito e gravar, na mesma transação (`service/ReservationService.java:36-48`). Isso vale também entre processos, e o roteiro confirma: 10 de 10 rodadas simultâneas corretas.

**Os defeitos.**

- O esquema não evolui. `CREATE TABLE IF NOT EXISTS` (`persistence/Database.java:48-82`) não sabe acrescentar uma coluna numa tabela que já existe, e a próxima mudança vai exigir uma ferramenta de migração ou um `ALTER` escrito à mão.
- `room_number` não tem índice, e a consulta de conflito (`persistence/ReservationRepository.java:29-31`) filtra justamente por ele.
- Nenhum CHECK protege `status`, `rate_plan` ou `guests >= 1`, e não há EXCLUDE contra a sobreposição de datas. A regra depende só da trava no código.
- Num detalhe de atomicidade, a original e a cópia para o arquivo são gravadas em transações separadas (`persistence/MessageLog.java:25`, chamado duas vezes por `mail/ArchiveCopy.java:15-17`).

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam. A parte comum do enunciado passou em 46 de 46, os dados depois de reiniciar em 3 de 3, as regras de envio no modo normal em 9 de 9 e no modo de teste em 5 de 5, e as duas reservas ao mesmo tempo em 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada, isto quer dizer:

- Os preços batem com os do texto, com a alta temporada e o arredondamento meio para o par (599,76; desconto de 37,48 e total de 212,42 no NON_REFUNDABLE de uma noite).
- As recusas voltam com os códigos combinados com o pessoal do site.
- A confirmação manda o e-mail com o comprovante.
- Os cancelamentos devolvem o certo por plano.
- Os dois relatórios em CSV saem como pedido, inclusive a NON_REFUNDABLE cancelada que continua contando no financeiro.
- Reservas e mensagens continuam lá depois de reiniciar.
- As regras de e-mail funcionam nos dois modos: o endereço inválido fica bloqueado, com o endereço original e sem cópia; a cópia para o arquivo vem logo depois da original; no modo de teste, tudo vai para a caixa de testes, com "[TESTE] " no assunto e o destinatário original na primeira linha.
- Duas reservas simultâneas para o mesmo quarto nunca são criadas juntas.

## Dúvidas

- `HOTEL_ARCHIVE_COPY=false` (`App.java:24`) é uma variável que o agente inventou para a segunda pousada desligar a cópia. O texto não diz como cada instalação escolhe a combinação. A escolha é sensata, mas o roteiro não a confere.
- O e-mail de confirmação gravado fora da transação da confirmação (`service/ReservationService.java:57-59`) só faria diferença numa falha do banco entre os dois commits. A evidência não mostra se isso aconteceria na prática.
