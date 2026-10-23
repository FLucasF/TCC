# Revisão: construção, cenário 07

## Visão geral

O projeto tem 7 pacotes e 34 classes, cerca de 1.240 linhas em `src/main`. `App` faz a montagem num lugar só (`App.java:17-34`). `web` traz as rotas Javalin e a leitura do corpo (`Routes.java`, `ReservationRequest.java`), `service` as operações (`ReservationService`, `ReportService`, `Outbox`), `domain` as regras de preço, situação e comprovante, sem Javalin e sem JDBC, `store` o JDBC com HikariCP e transações (`Database.inTransaction`), e `mail` / `mail.rules` as regras de envio. Uma requisição vai de `Routes` para o serviço, que abre uma transação em `Database`, carrega a reserva com `FOR UPDATE`, aplica a transição no record `Reservation` e grava pelo repositório. Os dados ficam no PostgreSQL, num esquema criado por `CREATE TABLE IF NOT EXISTS` no código (`Database.java:10-50`). A parte do cenário é uma lista de regras (`MessageRule`). Cada regra é uma classe própria. A montagem diz em que modo cada uma vale (`MailSetup.java:20-25`), e `MailPipeline` as aplica em sequência. O comportamento passou inteiro (65/65). O que há de mais sério:
- os testes cobrem pouco: 20,8% das linhas, sem nada em `service`, `store` e `web`;
- o esquema não tem migrações;
- a confirmação e o registro do e-mail são gravados em transações separadas.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, 1 ciclo (`mail` ↔ `mail.rules`). Javalin só em `web` e na raiz, JDBC em `store` e em `service`.
**Nota:** com problemas

As dependências vão na direção certa: `web` → `service` → `store` → `domain`, e `domain` não depende de nenhum pacote do projeto (`measures/dependencies.md`).
- **Borda HTTP:** é fina. Cada rota é uma linha que lê o pedido, chama o serviço e escreve a resposta (`Routes.java:37-48`). A validação do corpo fica em `ReservationRequest.java:18-46`.
- **Regras:** moram no domínio.
  - transições e devolução: `Reservation.java:25-43`;
  - preço com temporada e arredondamento: `Pricing.java:14-36`;
  - desconto e reembolso por plano: `RatePlan.java:17-23`.
- **Montagem:** num lugar só (`App.java:21-33`).

Há dois defeitos localizados, que são o que o critério da coluna chama de "com problemas":

1. **Um ciclo pequeno entre `mail` e `mail.rules`.** As regras implementam `mail.MessageRule` e usam `mail.Message`. Em sentido contrário, `mail.MailSetup` instancia as regras (`MailSetup.java:3-5, 22-24`). A combinação das regras é montagem e poderia estar em `App` ou num pacote próprio, o que desfaria o ciclo.
2. **SQL no pacote de serviço.** `Outbox` executa `SELECT pg_advisory_xact_lock(727002)` direto com um `Statement` (`Outbox.java:24-28`). Os serviços também manipulam `java.sql.Connection` (`ReservationService.java:105`), mas isso vem do padrão "unidade de trabalho" de `Database.inTransaction` e é aceitável.

Nenhuma classe é desproporcional: a maior é `ReservationRepository`, com 146 linhas.

## Design de baixo nível

**Números:** 3 de 3 regras pedidas (validar o endereço, copiar para o arquivo, desviar no modo de teste) estão na estrutura, cada uma em sua classe (`mail/rules/*.java`); nenhuma está fora dela. O modo é decidido em 1 lugar (`MailSetup.java:22-24` com `Scope`, aplicado em `MailPipeline.java:11-14`). Nenhuma regra consulta o modo de teste.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** bom

**O pattern.** Não é um Decorator: não há uma interface de envio com embrulhos que chamam o próximo. É uma lista de filtros que o envio aplica, e a referência aceita isso de antemão como alternativa equivalente:
- cada regra é uma peça própria, que implementa `MessageRule.apply(Message) -> List<Message>` (`MessageRule.java:9-11`):
  - `ValidAddressRule.java:11-25` bloqueia o endereço inválido;
  - `ArchiveCopyRule.java:16-19` devolve a original e mais a cópia;
  - `TestRedirectRule.java:16-21` reescreve o destinatário, o assunto e o texto;
- o modo vem da montagem, não das regras:
  - `MailSetup.pousadaMareAlta()` declara cada regra com o seu `Scope` (`ALWAYS`, `LIVE_ONLY`, `TEST_ONLY`, em `Scope.java:4-15`);
  - `App.java:19,28` passa o modo;
  - `MailPipeline` descarta as regras que não valem (`MailPipeline.java:11-14`).

As regras pedidas para o futuro cabem nessa estrutura com uma classe e uma linha em `MailSetup`, sem tocar no envio: a madrugada e o limite diário, cada um "sempre, só no teste ou só fora dele". Um detalhe afasta do Decorator:
- o bloqueio para as regras seguintes é tratado de forma genérica pela lista (`MailPipeline.java:22-26`), e não por cada embrulho;
- a ordem das regras é uma convenção da lista, documentada em `MailSetup.java:8-12`.

Os dois são coerentes com uma lista de filtros. O ponto fraco é a "outra instalação" (a segunda pousada sem a cópia): hoje ela exige editar ou duplicar `MailSetup.pousadaMareAlta()`, porque não há configuração externa. O texto não pedia mais que isso.

**O exagero.** Não há. `InstalledRule` e `Scope` respondem diretamente ao "cada regra pode valer sempre, só no teste ou só fora dele". `Database.Work` existe para permitir `SQLException` no lambda. Não há fábricas, builders nem interfaces com uma implementação só que nada substitui (`MessageRule` tem três).

**O resto do código.** Está limpo:
- dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`Money.java:16-18`, `Pricing.java:24,31,35`, `RatePlan.java:18`);
- datas em `LocalDate`;
- `enum` para situação, plano, canal e situação da mensagem;
- records imutáveis com transições que devolvem cópias (`Reservation.java:61-64`);
- métodos curtos e nomes claros.

Pontos menores, que não mudam a nota:
- `Reservation.occupies` (`Reservation.java:50-52`) é código morto. A mesma regra está em SQL em `ReservationRepository.java:68`.
- `Routes.java:78` captura `NullPointerException` como fluxo de controle.
- `Room.type` é `String`, não `enum` (`Room.java:5`).
- `Reservation` é montado com 16 argumentos posicionais (`ReservationService.java:50-53`).
- `ErrorCode`, no domínio, carrega o status HTTP (`ErrorCode.java:4-9`).

## Testes

**Números:** 9 testes rodaram, 0 falhas, 0 erros, 0 pulados; 9 métodos escritos em 3 classes. Cobertura de 20,8% das linhas (97/467) e 38,1% dos ramos (40/105).

| pacote | linhas cobertas |
|---|---|
| `mail` | 100% |
| `mail.rules` | 19/20 |
| `domain` | 41/86 |
| `service`, `store`, `web` | 0% |

Não há nenhum teste pela API nem contra o banco.
**Nota:** com problemas

**O que é testado tem bons valores.**
- **A parte do cenário.**
  - A cópia vem depois da original, com destinatário, "[Cópia] " e o mesmo texto (`MailPipelineTest.java:16-23`).
  - O desvio no modo de teste confere destinatário, "[TESTE] " e a primeira linha "Destinatário original: …", sem cópia (`MailPipelineTest.java:26-32`).
  - Seis endereços inválidos ficam bloqueados nos dois modos, com o endereço original e sem cópia (`MailPipelineTest.java:35-46`).
  - Pela pergunta da mutação: tirar a cópia, trocar o prefixo ou mudar o `Scope` de uma regra em `MailSetup` faria um teste falhar.
- **O preço.**
  - 599,76 do exemplo do texto (`PricingTest.java:67-71`);
  - os limites da temporada (`PricingTest.java:74-81`);
  - o desconto de 15%, 116,97 sobre 779,80 (`PricingTest.java:84-89`);
  - o meio para o par, 0,045 → 0,04 (`PricingTest.java:92-97`).
- **O comprovante:** o texto exato e o formato R$ 1.234,56 (`ReceiptTest.java:111-127`).

**O que ficou sem rede.**
- As transições de situação e o cancelamento com devolução (`Reservation.java:25-43`, `RatePlan.refundFor`). Trocar `refundable ? amountPaid : 0` por `0`, ou aceitar cancelar uma reserva com check-in, não derrubaria nenhum teste.
- Os dois relatórios CSV (`ReportService.java:28-61`).
- A detecção de sobreposição de reservas (`ReservationRepository.java:19-31`).
- As recusas e os códigos de erro (`ReservationRequest.java`, `Routes.java`).
- O texto do e-mail de confirmação (`ConfirmationEmail.java:13-20`).
- Endereços válidos limítrofes: nenhum endereço válido diferente de `ana@gmail.com` é testado. Trocar `domain.length() > 2` por `> 3` em `ValidAddressRule.java:24` passaria.

Como o texto não pedia testes, os que existem mostram iniciativa e são bem escritos, mas deixam mais da metade das regras e todas as camadas de integração sem conferência.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(12,2)` nas 6 colunas e datas em `date`. As conferências acusaram:
- 1 chave estrangeira sem índice: `reservations(room_number)` → `rooms`;
- 0 restrições `CHECK`, `UNIQUE` ou `EXCLUDE`;
- 5 colunas de situação ou tipo sem `CHECK`;
- esquema criado por `CREATE TABLE` no código, sem migrações.

Os 3 candidatos a "SQL com `+`" são constantes.
**Nota:** com problemas

**O que está certo:**
- SQL sempre com parâmetros `?` (`ReservationRepository.java:21-26, 56-61, 68-73`);
- pool HikariCP, e conexões e statements em try-with-resources (`Database.java:58-93`);
- transação explícita com rollback (`Database.java:79-93`);
- a dupla reserva impedida por trava de linha no quarto (`SELECT … FOR UPDATE`, `RoomRepository.java:26-33`, usada em `ReservationService.java:39`) antes da conferência de sobreposição (`ReservationRepository.java:19-31`). A trava vale entre processos, e as 10 rodadas simultâneas passaram.

**Os defeitos:**
1. **O esquema não tem como evoluir.** É um bloco `CREATE TABLE IF NOT EXISTS` no código (`Database.java:10-50`), sem migrações versionadas. O próximo pedido que acrescente uma coluna não vai alterar um banco que já existe.
2. **A regra de "uma reserva por quarto por noite" só existe no código.** Não há `EXCLUDE` com `daterange`. Faltam também os `CHECK` em `status`, `rate_plan` e `guests` (`Database.java:25-31`), e o índice em `room_number`, usado em toda conferência de sobreposição.
3. **A confirmação e o registro do e-mail não andam juntos.** `confirm` grava a nova situação numa transação (`ReservationService.java:63`, via `transition`, `ReservationService.java:90-96`). Depois lê o quarto em outra (`ReservationService.java:98-103`) e grava as mensagens numa terceira (`Outbox.java:24-33`). Se a gravação das mensagens falhar, a reserva fica CONFIRMED e paga sem e-mail registrado, e o cliente recebe um erro 500 para uma confirmação que já valeu.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam.

| parte | passaram |
|---|---|
| parte comum | 46/46 |
| dados depois de reiniciar | 3/3 |
| cenário 07, modo normal | 9/9 |
| cenário 07, modo de teste | 5/5 |
| duas reservas ao mesmo tempo | 2/2 |

**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API:
- preços com temporada e meio para o par, desconto do não reembolsável, recusas com os códigos certos;
- confirmação com o e-mail e o comprovante, check-in, check-out e segunda via;
- devoluções no cancelamento e os dois relatórios CSV;
- os dados persistem depois de reiniciar, e duas reservas simultâneas do mesmo quarto nunca são aceitas juntas.

Nas regras novas:
- o endereço inválido fica BLOCKED, sem cópia, nos dois modos;
- a cópia para `arquivo@…` vem logo depois da original;
- com `HOTEL_TEST_MODE=true`, tudo vai para `testes@…` com "[TESTE] " e "Destinatário original: …", sem cópia.

O defeito de transação apontado no Banco (`ReservationService.java:63-64`) não apareceu em nenhuma chamada do roteiro.

## Dúvidas

- **A classe do pattern.** Fiquei entre "alternativa equivalente" e "acerto". Não há embrulhos com a interface do envio: cada regra devolve uma lista de mensagens, e o `MailPipeline` itera. Isso é uma lista de filtros, e por isso "alternativa equivalente".
- **A validação de endereço é mais permissiva que o texto em casos-limite.** Aceita `ana@.gmail.com` e `ana@gmail.com.`, porque basta haver um ponto em algum lugar entre o primeiro e o último caractere do domínio (`ValidAddressRule.java:23-24`). O texto ("um nome com pelo menos um ponto no meio") não fecha esses casos, e o roteiro não os testa.
