# Revisão: construção, cenário 07

## Visão geral

O projeto tem quatro pacotes. `br.com.marealta` tem a montagem (`Main`) e o acesso ao banco (`Db`, que abre conexão e transação e cria o esquema). `domain` tem as regras e também o SQL delas: `ReservationService`, `Reports`, `Pricing`, `Receipt` e os records. `mail` tem o envio: `Mailer` grava na tabela `messages`, e `MessagePolicy`, `MailSettings` e `Scope` guardam as regras de envio. `web` tem uma classe só, `Api`, com as rotas Javalin. Uma requisição chega na rota (`web/Api.java:59-65`), que lê o JSON e chama o serviço. O serviço abre uma transação com `db.tx` e roda o SQL ali mesmo (`domain/ReservationService.java:45-80`). Na confirmação, o e-mail é gravado na mesma transação (`ReservationService.java:99`). Tudo fica no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` (`Db.java:46-86`). O sistema faz o que o texto pede: as 65 conferências do roteiro passaram. O que há de mais sério é o design da parte do cenário. As regras de envio não são peças separadas: são `if` dentro de uma classe só, `MessagePolicy.apply`/`isBlocked` (`mail/MessagePolicy.java:22-46`). Há também três defeitos menores: o SQL fica junto com as regras no serviço, o esquema não tem migrações, e a proteção contra reserva dupla só vale dentro de um processo.

## Arquitetura

**Números:** 4 pacotes, 8 dependências entre eles, 2 ciclos (`br.com.marealta` ↔ `web`, e `br.com.marealta` → `web` → `domain` → `br.com.marealta`). JDBC aparece em todos os pacotes; Javalin em `br.com.marealta` (por causa do `Main`) e em `web`. As maiores classes são `ReservationService` (209 linhas) e `Api` (195).
**Nota:** com problemas

O que está bom:
- A borda HTTP é fina. Cada rota lê a entrada, chama o serviço e monta o JSON (`web/Api.java:59-65`). A validação da entrada fica em `parseNew` (`Api.java:104-133`), que é trabalho da borda.
- As regras não conhecem o Javalin.
- A montagem fica num lugar só: `Main.java:15-20` cria `Db`, `MailSettings`, `Mailer`, `ReservationService`, `Reports` e `Api` e liga as peças.

Os problemas:
- **O SQL mora junto com as regras.** `ReservationService` mistura as transições de estado e o cálculo da devolução (`ReservationService.java:86-151`) com o acesso a dados (`ReservationService.java:153-208`). `Reports` tem SQL e montagem de CSV no mesmo método (`domain/Reports.java:19-46`, `:49-71`). Não existe uma camada de dados separada.
- **A rota de mensagens passa por cima do serviço.** `GET /messages` abre a transação e chama `Mailer::list` direto (`Api.java:68`). Por isso `Api` recebe o `Db` (`Api.java:35`).
- **Os dois ciclos vêm do lugar do `Db`.** Ele está no pacote raiz junto com o `Main`, e o raiz depende de `web` e de `domain`. `domain` importa `br.com.marealta.Db` (`ReservationService.java:3`, `Reports.java:3`) e `web` também (`Api.java:3`). Bastava pôr o `Db` num pacote próprio.
- **Um `new` fora da montagem.** O `Mailer` cria a própria `MessagePolicy` (`mail/Mailer.java:20`).

Os defeitos estão localizados e não chegam a "camadas ausentes".

## Design de baixo nível

**Números:** há 5 regras de envio: validar o endereço, desviar no modo de teste, copiar para o arquivo, horário de madrugada e limite diário. Nenhuma é uma peça própria com a interface do envio. Todas são `if` dentro de `MessagePolicy` (`mail/MessagePolicy.java:24-45`). A combinação por modo é um `Scope` por regra, lido de variáveis de ambiente (`MailSettings.java:32-47`). Não há nenhum embrulho e nenhuma lista de filtros.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede regras em volta do mesmo envio, que se combinam conforme o modo e conforme a instalação, e anuncia mais regras. Esse é o Decorator: um embrulho por regra, empilhado na montagem.

O que o agente fez:
- `Mailer.send` (`Mailer.java:23-52`) chama `MessagePolicy.apply`, que resolve tudo com condicionais:
  - o bloqueio por endereço, madrugada e limite é uma sequência de `if` (`MessagePolicy.java:41-46`);
  - o desvio do modo de teste é `if (s.testMode())` (`MessagePolicy.java:29-33`);
  - a cópia é `if (s.archiveCopy().applies(s.testMode()))` (`MessagePolicy.java:35-37`).
- O desvio do modo de teste nem é configurável (`MailSettings.java:8`).

É o caso que a referência chama de omissão: os três `if` ficam dentro do envio.

O que aproxima do pattern: a combinação por instalação é configuração, não código. `Scope.applies` (`mail/Scope.java:7-14`) e `MailSettings.fromEnv` deixam a segunda pousada desligar a cópia sem mexer no código (`HOTEL_MAIL_ARCHIVE_COPY=off`). Em compensação, uma regra nova exige mexer em três lugares: um campo novo no record `MailSettings`, uma linha em `fromEnv` e um `if` em `MessagePolicy`. A ordem entre as regras também fica fixa no código.

**O exagero.** Não há interface com uma implementação só, fábrica nem camada que só repassa. `MailSettings` lê da variável de ambiente os endereços, o fuso e o horário (`MailSettings.java:41-46`). É um pouco de configuração especulativa, mas pequena e ligada às regras que o texto anuncia: contenção.

**O resto do código.** O que está bom:
- Dinheiro em `BigDecimal`, com `HALF_EVEN` a cada passo (`domain/Pricing.java:16-18`, `:30-40`).
- Datas em `LocalDate`, plano e situação da reserva em `enum` (`RatePlan.java:3`, `Status.java:3-9`).
- Os records são imutáveis, e `Reservation.with` cria uma reserva nova em vez de alterar a antiga (`Reservation.java:7-10`).
- Os erros de negócio são tipados (`ApiException.java:3-20`).

Os defeitos localizados:
- **Texto solto onde caberia um `enum`.**
  - A situação da mensagem é texto: as constantes `SENT` e `BLOCKED` em `Outgoing.java:5-6`, e `Message.status` como `String` (`Message.java:3`).
  - O tipo do quarto é `String` (`Room.java:5`).
- **Campos públicos.** `ApiException` expõe campos públicos (`ApiException.java:4-5`).
- **Exceção embrulhada.** A `SQLException` vira `IllegalStateException` dentro do lambda de contagem (`Mailer.java:31-37`).
- **Trava com número fixo.** O `Mailer` usa uma trava de banco global, com o número 7071 escrito no código (`Mailer.java:27-30`). Ela serializa todos os envios, e não só os do mesmo hóspede, como o comentário diz.
- **SQL e CSV no mesmo método.** `Reports` junta as duas coisas em métodos de cerca de 25 linhas (`Reports.java:19-46`).

Nada disso deixa o código difícil de mudar, mas um revisor exigente pediria essas mudanças.

## Testes

**Números:** 11 testes rodaram, 0 falharam, 0 foram pulados. Há 11 métodos escritos em 2 classes (`MessagePolicyTest` com 7, `PricingTest` com 4). A cobertura é de 20,0% das linhas (89 de 444) e 36,0% dos ramos (59 de 164). Por pacote: `mail` 56/97 linhas, `domain` 33/189, `web` 0/112, raiz 0/46. Não há teste pela API nem contra o banco.
**Nota:** com problemas

O que os testes conferem bem:
- **A parte nova, com valores.** São conferidos:
  - os endereços válidos e os inválidos (`MessagePolicyTest.java:22-30`);
  - a cópia logo depois da original, com "[Cópia] " no assunto e o mesmo texto (`MessagePolicyTest.java:33-40`);
  - o bloqueio sem cópia (`:43-48`);
  - o desvio do modo de teste: endereço, "[TESTE] ", a primeira linha "Destinatário original: " e nenhuma cópia (`:51-56`);
  - o endereço inválido bloqueado também no modo de teste, com o endereço original (`:57-59`);
  - a segunda instalação sem cópia (`:63-68`);
  - a madrugada e o limite diário em cada modo, inclusive o limite exato: 1 enviada passa, 2 bloqueia (`:71-81`).
- **A pergunta da mutação na parte nova.** Trocar `>=` por `>` no limite (`MessagePolicy.java:45`) faz falhar `:77`. Tirar o prefixo "[Cópia] " faz falhar `:38`. Fazer a cópia sair no modo de teste faz falhar `:53`.
- **O preço.** São conferidos o exemplo de 599,76 (`PricingTest.java:108-112`), as bordas da temporada (`:115-122`), o desconto com meio para o par (`:125-134`) e o texto do comprovante, com R$ 1.234,56 (`:137-149`).

O que ficou sem rede:
- `ReservationService` inteiro:
  - as transições de estado;
  - a devolução no cancelamento: trocar `RatePlan.STANDARD` por `NON_REFUNDABLE` em `ReservationService.java:112` não faz falhar nenhum teste;
  - a recusa de quarto ocupado (`ReservationService.java:165-175`): trocar `<` por `<=` passa;
  - a capacidade.
- Os relatórios: os filtros em `Reports.java:32` e `:55`.
- Os códigos de erro da API (`Api.java` tem 0% de cobertura).
- A gravação das mensagens com `status` e na ordem certa (`Mailer.java:38-51`).

Os testes passam e as asserções são fortes, mas metade das regras pedidas não tem teste.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está todo em `numeric(12,2)` e as datas em `date`. Há uma chave estrangeira (`reservations.room_number`), com índice. As conferências acusaram:
- 0 restrições que protegem uma regra (sem UNIQUE, EXCLUDE ou CHECK);
- 5 colunas de situação ou tipo sem CHECK;
- esquema criado no código (0 migrações);
- nenhum pool e 1 `DriverManager.getConnection`.
**Nota:** com problemas

O que está bom:
- Os tipos e o `NOT NULL` estão certos (`Db.java:46-79`).
- O SQL é todo com parâmetros. As concatenações que o script listou só juntam constantes (`COLS`, `" FOR UPDATE"`, `ReservationService.java:19-20`, `:179`).
- As conexões são fechadas com try-with-resources e cada operação roda numa transação (`Db.java:22-36`).
- A confirmação trava a linha com `FOR UPDATE` (`ReservationService.java:89`). A mudança de situação e a mensagem são gravadas na mesma transação (`ReservationService.java:92`, `:99`).
- A criação do esquema é protegida por uma trava de banco (`Db.java:44`).

Os defeitos:
- **O esquema não tem migrações.** São `CREATE TABLE IF NOT EXISTS` (`Db.java:46`, `:52`, `:71`), que não sabem acrescentar coluna numa tabela que já existe. O próximo pedido que mudar uma tabela vai precisar de `ALTER` feito à mão.
- **A reserva dupla só é impedida dentro do processo.** A proteção é um `synchronized (lock)` (`ReservationService.java:26`, `:46`) com um `SELECT` antes do `INSERT` (`:54`, `:165-175`). Não há restrição `EXCLUDE` nem trava de banco na criação. Dois processos sobre o mesmo banco podem gravar duas reservas na mesma noite. O roteiro passou porque havia um processo só.
- **Os valores permitidos não são conferidos no banco.** Não há CHECK em `status`, `rate_plan`, `capacity` ou `guests`.
- **Não há pool.** Cada transação abre uma conexão nova com `DriverManager` (`Db.java:23`).

Nada perde ou corrompe dado no uso que o texto descreve.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário no modo normal: 9 de 9;
- cenário no modo de teste: 5 de 5;
- duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o roteiro conferiu passou:
- os preços (599,76; o desconto de 37,48 com meio para o par);
- as recusas, com os códigos e status certos;
- a confirmação com o e-mail e o comprovante;
- o check-in, o check-out e a segunda via;
- as devoluções nos dois planos e na reserva pendente;
- os dois relatórios em CSV;
- os dados depois de reiniciar.

Para o dono da pousada, as regras de envio funcionam como ele pediu:
- O e-mail inválido aparece como BLOCKED, sem cópia e com o endereço original, também no modo de teste.
- Fora do teste, a cópia para `arquivo@pousadamarealta.com.br` vem logo depois da original, com "[Cópia] ".
- Com `HOTEL_TEST_MODE=true`, nada sai para o hóspede: vai tudo para `testes@`, com "[TESTE] " e a linha do destinatário original.

As dez rodadas de reservas simultâneas deram sempre uma reserva criada e a outra recusada com 409. Elas rodaram num processo só, em que o `synchronized` basta. A falta de proteção entre processos está na coluna do banco e não muda esta nota.

## Dúvidas

- A classe do pattern ficou entre "omissão" e "malfeito". As regras são `if` dentro de uma classe só, `MessagePolicy`, o que a referência descreve como omissão. Em compensação, a combinação por modo e por instalação foi tirada do código e posta em configuração (`Scope`, `MailSettings`). Não há nenhuma estrutura de embrulho ou de filtros que pudesse ser "malfeita", então fiquei com omissão.
- O roteiro não testou a madrugada nem o limite diário pela API. Esses dois só têm a conferência de unidade (`MessagePolicyTest.java:71-81`).
