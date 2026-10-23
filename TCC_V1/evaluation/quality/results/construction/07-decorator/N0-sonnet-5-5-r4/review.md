# Revisão: construção, cenário 07

## Visão geral

O projeto tem um pacote só (`br.com.marealta.pousada`) com 9 arquivos e 914 linhas. Cada camada é uma classe: `Api` é a borda HTTP (Javalin), `ReservationService` guarda as regras da reserva, os relatórios e o texto do e-mail, `Pricing` faz o preço, `Repo` concentra todo o SQL, `Db` cuida do pool Hikari e das transações, `MailDispatcher` e `MailConfig` cuidam do envio, e `Main` monta tudo. Uma requisição entra numa rota de uma linha (`Api.java:46-54`) e chama o serviço. O serviço abre uma transação por `Db.write`/`Db.read` (`Db.java:42-48`) e trabalha sobre o `Repo`. Os dados ficam em três tabelas do PostgreSQL, criadas por `CREATE TABLE IF NOT EXISTS` no código (`Repo.java:25-71`). As 65 conferências do comportamento passaram, inclusive os dois modos de envio, os dados depois de reiniciar e as reservas simultâneas. Os 17 testes passam.

O defeito mais sério está no design da parte que o cenário pede. As três regras de e-mail (o bloqueio, a cópia e o desvio do modo de teste) e as duas anunciadas (a madrugada e o limite diário) são `if`s dentro de um único método `MailDispatcher.send`/`blocked` (`MailDispatcher.java:47-78`). Não há peças que se empilhem. Os outros defeitos são menores: o esquema é criado no código sem migrações, e o código HTTP do erro mora no domínio.

## Arquitetura

**Números:** 1 pacote (22 classes contando as internas, 914 linhas), 0 dependências entre pacotes, 0 ciclos. Esse único pacote usa Javalin, JDBC, Jackson e o pool. A maior classe é `Repo.java` (262 linhas); depois vêm `ReservationService` (167) e `Api` (135).
**Nota:** com problemas

Por classe, a separação está boa:
- **A borda é fina:** cada rota só lê o pedido e chama o serviço (`Api.java:46-54`). A conversão do corpo e as validações de formato ficam em `Api.java:95-134`, que é o lugar certo para elas.
- **As regras não conhecem o Javalin nem o SQL:** `ReservationService.java` só conversa com o `Repo`.
- **Todo o SQL está no `Repo`:** a única exceção é a trava em `Db.java:56`.
- **A montagem é feita num lugar só:** `Main.java:11-17`.

Mas com um pacote só nada impede que as camadas se misturem, e o jdeps não tem como mostrar a direção das dependências. Há misturas localizadas:
- **O código HTTP mora nas regras:** o serviço lança `ApiException` já com o status HTTP (404, 422, 409, em `ReservationService.java:29`, `:31`, `:35`, `:128`, `:137`). A classe da exceção fica no domínio, com campos públicos (`Domain.java:36-38`).
- **A camada de dados depende do envio:** `Repo` implementa uma interface interna do `MailDispatcher` (`Repo.java:14`, `MailDispatcher.java:22`).
- **O serviço acumula funções:** monta os CSVs dos relatórios e o texto do comprovante e do e-mail (`ReservationService.java:89-118`, `:147-166`). Hoje é pouco código, mas é ali que o próximo pedido vai crescer.

## Design de baixo nível

**Números:** cinco regras de envio (o endereço inválido, a cópia para o arquivo, o desvio do modo de teste, a madrugada e o limite diário). Nenhuma delas é uma peça própria: as cinco são `if`s em `MailDispatcher.send`/`blocked` (`MailDispatcher.java:49`, `:55`, `:58`, `:69-76`). A combinação por modo também é decidida dentro do envio, por `config.X().active(config.testMode())`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Decorator).** A referência considera acerto um embrulho por regra com a mesma interface do envio, e a combinação montada no `main`. Aqui não há interface de envio nem embrulhos. Um único método faz tudo:
- testa o endereço (`MailDispatcher.java:69`), a madrugada (`:71`) e o limite (`:72-76`);
- decide a cópia com `if (config.archiveCopy().active(config.testMode()))` (`:55`);
- decide o desvio de teste com `config.testMode() ? redirected(m) : m` (`:58`).

O que aproxima do pedido é o `enum Scope` com `ALWAYS/TEST_ONLY/LIVE_ONLY/OFF` (`MailConfig.java:19-29`), que deixa cada instalação escolher, por variável de ambiente, quando cada regra vale (`MailConfig.java:48-59`). Isso atende à "segunda pousada sem cópia" sem mexer no código. Mas é configuração de flags, e não estrutura. Uma regra nova (por exemplo, um canal SMS ou outra regra de bloqueio) obriga a abrir `send`/`blocked`, acrescentar mais um `if` e mais um campo no record `MailConfig`, cujo construtor já tem 7 parâmetros posicionais (`MailConfig.java:10-14`). Pela referência, os `if`s dentro do envio são omissão.

**O exagero.** Não há estrutura sobrando. A interface `MailStore` (`MailDispatcher.java:22-27`) tem uma implementação de produção, mas o teste a substitui (`MailDispatcherTest.java:15-22`), então se justifica. Os aliases de `Scope.parse` (`MailConfig.java:31-40`: `ON`, `TRUE`, `PROD`, `PRODUCTION`...) passam do necessário, mas são um detalhe, não uma camada.

**O resto do código.** É enxuto e usa os tipos certos:
- `BigDecimal` com `HALF_EVEN` a cada passo (`Pricing.java:24-26`, `:40-50`);
- `LocalDate`, e `enum` para tipo de quarto, plano e situação (`Domain.java:9-13`).

Os defeitos são localizados:
- **Situação da mensagem em texto solto:** `String status` (`Domain.java:34`, `MailDispatcher.java:19`), com `"SENT"`/`"BLOCKED"` repetidos em literais (`MailDispatcher.java:50`, `:54`, `:56`, `Repo.java:235`).
- **Campos públicos na exceção:** `Domain.java:37-38`.
- **Construção repetida a cada chamada:** o `DateTimeFormatter` (`ReservationService.java:156`) e o `DecimalFormat` (`Pricing.java:54`).
- **Busca linear no relatório:** o tipo do quarto é procurado numa lista, linha a linha (`ReservationService.java:95`).
- **Erro só no console:** o tratamento genérico imprime a pilha com `printStackTrace()` (`Api.java:42`) em vez de usar um log.

## Testes

**Números:** 17 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 17 métodos em 3 classes: `MailDispatcherTest` 9, `PricingTest` 4 e `ApiIT` 4. `ApiIT` passa pela API real contra o PostgreSQL de `DB_URL`, num schema próprio. Cobertura: 86,3% das linhas (365/423) e 79,3% dos ramos (134/169).
**Nota:** bom

Os testes conferem as regras com valores:

**Preço**
- O exemplo do texto, 599,76 (`PricingTest.java:15-21`).
- As bordas da temporada: 14/12, 15/12, 29/02, 01/03, 30/06, 31/07 e 01/08 (`PricingTest.java:24-32`).
- O desconto de 15% e o meio para o par, com 0,125 → 0,12 e 0,135 → 0,14 (`PricingTest.java:35-44`).
- O formato R$ (`PricingTest.java:47-50`).

**Fluxo pela API**
- O fluxo inteiro, com o texto do e-mail comparado palavra por palavra (`ApiIT.java:111-125`).
- Os relatórios em CSV exato (`ApiIT.java:129`, `:144`).
- As devoluções: o plano normal devolve 1159,80, o não reembolsável cancelado continua no financeiro com 985,83 (`ApiIT.java:148-174`).
- Os códigos de erro (`ApiIT.java:189-207`).

**A parte nova do cenário**

Cada regra tem um teste próprio:
- o endereço válido e os inválidos, inclusive `ana@.com` e `ana@gmail.` (`MailDispatcherTest.java:31-41`);
- a cópia com o assunto e o texto (`:44-53`);
- o bloqueio sem cópia nos dois modos (`:56-65`);
- o desvio de teste com a primeira linha exata (`:68-76`);
- a cópia desligada (`:88-93`);
- a madrugada e o limite diário (`:96-117`);
- a leitura das variáveis de ambiente (`:120-125`).

A pergunta da mutação na parte nova:
- **Trocar o default da cópia para `ALWAYS`:** falha `testModeRedirectsAndSkipsArchive`.
- **Trocar `i = 1` por `i = 0` no ponto do domínio:** falha `ana@.com`.
- **Apagar o "Destinatário original":** falha `:75`.

Pontos menores, que não derrubam a nota:
- `ApiIT.java:184` é uma asserção tautológica (`assertEquals("1 diária", "1 diária")`).
- `ApiIT.java:130-131` aceita tanto `total,0/5` quanto `total,1/5`, então não confere a regra de quem sai naquele dia.
- Nenhum teste confere a linha "Desconto:" do comprovante (`ReservationService.java:164`).
- O modo de teste só é testado em unidade, não pela API.
- Nenhum teste confere duas reservas simultâneas.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária e com o dinheiro em `numeric(12,2)`. O que as conferências acusaram:
- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhum CHECK, UNIQUE ou EXCLUDE;
- as 5 colunas de situação ou tipo em `text` sem CHECK;
- o esquema criado no código (`CREATE TABLE` em `Repo.java`), sem migrações.

**Nota:** com problemas

**O que guarda certo**
- Os tipos estão certos: datas em `date` (`Repo.java:40-41`) e `NOT NULL` em tudo o que é obrigatório.
- A reserva está ligada ao quarto por chave estrangeira (`Repo.java:36`).
- Todo o SQL usa `?`. A única concatenação é a da constante `WRITE_LOCK` (`Db.java:56`).
- As conexões vêm do pool Hikari e são devolvidas com try-with-resources (`Db.java:51`), e cada operação roda numa transação com rollback (`Db.java:52-64`).
- A confirmação grava a reserva e as mensagens na mesma transação (`ReservationService.java:49-53`).
- A regra "um quarto, uma reserva por noite" é protegida entre processos: toda escrita pega `pg_advisory_xact_lock` antes de verificar a sobreposição (`Db.java:54-58`, `Repo.java:123-137`). O roteiro confirmou isso em 10 rodadas simultâneas.

**Os defeitos**
- **O esquema não sabe evoluir:** é `CREATE TABLE IF NOT EXISTS` no código (`Repo.java:27-61`). Esse comando não acrescenta uma coluna numa tabela que já existe, então o próximo pedido que mudar o banco não tem caminho.
- **A sobreposição depende só da trava:** o banco não tem nenhuma restrição própria (um `EXCLUDE` sobre o quarto e as datas). Os CHECKs de situação e de capacidade também faltam.
- **Faltam índices:** não há índice na chave estrangeira, nem em `messages(original_to, created_at)`, que o limite diário consulta (`Repo.java:233-236`).
- **A trava serializa todas as escritas:** é uma trava global, que vale até para a confirmação de reservas diferentes. Para uma pousada de 5 quartos isso basta.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam:

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário no modo normal | 9 de 9 |
| o cenário no modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede. Os preços (599,76; o desconto de 37,48 com meio para o par), as recusas com os códigos certos, a confirmação com o e-mail e o comprovante, o check-in, o check-out, as devoluções e os dois relatórios em CSV estão certos. Os dados continuam lá depois de reiniciar, e duas reservas simultâneas do mesmo quarto nunca são criadas juntas.

Nas regras de e-mail:
- o endereço inválido fica BLOCKED, com o endereço original e sem cópia, nos dois modos;
- a cópia `[Cópia] ` vem logo depois da original;
- com `HOTEL_TEST_MODE=true`, tudo vai para `testes@pousadamarealta.com.br`, com `[TESTE] ` e a primeira linha "Destinatário original:".

Nenhuma falha a explicar. Uma observação de fora do roteiro: as regras de madrugada e de limite diário existem, mas vêm desligadas por padrão (`MailConfig.java:45`). Para ligá-las é preciso conhecer as variáveis `HOTEL_MAIL_QUIET_HOURS` e `HOTEL_MAIL_DAILY_LIMIT_SCOPE` (`MailConfig.java:54-58`). O texto só anunciava essas regras, então desligar por padrão é uma escolha razoável.

## Dúvidas

1. **A classe do pattern:** dei `omissão`, mas a dúvida é com `malfeito`. A configuração por `Scope` é uma estrutura de verdade para "cada regra vale sempre, só no teste ou só fora dele", e a combinação por instalação sai do código. Pesou para `omissão` que cada regra continua sendo um `if` dentro do mesmo envio, e que o próprio envio pergunta pelo modo de teste (`MailDispatcher.java:55`, `:58`, `:71-72`). Pela referência, esse é o caso de omissão. Não há embrulhos nem peças que convivam com um jeito sem elas.
2. **A nota da arquitetura:** com um pacote só, ela depende da leitura das classes e não do jdeps. A separação por classe é limpa. Dei `com problemas` pela falta de pacotes e pelas misturas localizadas (o código HTTP no serviço, o `Repo` preso ao `MailDispatcher`). Um revisor menos exigente daria `bom`.
