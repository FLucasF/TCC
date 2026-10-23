# Revisão: construção, cenário 07

## Visão geral

O agente montou um projeto Maven com Javalin 6.7.0, HikariCP e JDBC puro, dividido em sete pacotes: `api` (o controlador e os DTOs), `service` (reservas, preço, mensagens, relatórios), `messaging` (as regras de envio), `repository` (o SQL), `domain`, `infra` (cria o esquema) e a raiz `App`, onde tudo é montado. Uma requisição entra pelas rotas em `App.java:70-80`, passa por um método do `ApiController`, que chama o `ReservationService`, e este chama os repositórios. Os dados ficam em três tabelas que `DatabaseSetup` cria com `CREATE TABLE IF NOT EXISTS`. O defeito mais sério quebra o sistema: **nenhuma reserva pode ser criada**. A consulta de conflito, em `repository/ReservationRepository.java:137`, tem `(? IS NULL OR id != ?)`, e o parâmetro recebe `setObject(4, null)` (`:146`). O PostgreSQL recusa a consulta com "could not determine data type of parameter $4" (reproduzi com `PREPARE` no banco da revisão). O `catch (Exception e)` em `api/ApiController.java:75` esconde essa falha atrás de um `400 INVALID_REQUEST "Invalid request"`. Como nada é criado, quase tudo o que vem depois falha: confirmação, e-mail, check-in, check-out, cancelamento, relatórios e todas as regras de envio do cenário. Os testes não chegaram a ver isso: nenhum deles fala com o banco ou com a API, e o único teste de integração é um `assertTrue(true)`.

## Arquitetura

**Números:** 7 pacotes e 14 dependências entre eles, sem nenhum ciclo. Javalin aparece só em `api` e na raiz, JDBC só em `repository`, `infra` e na raiz. As maiores classes são `ReservationRepository` (231 linhas), `ApiController` (204) e `ReservationService` (186).
**Nota:** com problemas

As camadas existem e as dependências apontam no sentido certo: api → service → repository → domain. As regras (`service`, `messaging`) não conhecem nem o Javalin nem o JDBC, e todo o SQL está nos repositórios. A montagem é feita num lugar só, em `App.java:36-60`, com injeção pelo construtor. Há misturas localizadas:
- a regra "a segunda via só existe para reserva paga" ficou no handler, em `api/ApiController.java:154`, e não no serviço;
- o controlador fala direto com os repositórios para listar quartos e mensagens (`api/ApiController.java:39`, `:169`), sem passar pelo serviço;
- a combinação das regras de envio por modo é decidida dentro do `MessageService` (`service/MessageService.java:21-34`), a partir de dois booleanos que o `main` passa (`App.java:46`), e não na própria montagem;
- `domain.SentMessage` depende de `messaging.MessageStatus` (`domain/SentMessage.java:3`), o que inverte a direção do domínio para um pacote de serviço.

Nenhuma delas é grave, mas um revisor exigente pediria para corrigir.

## Design de baixo nível

**Números:** as 3 regras de envio estão numa lista de regras (`EmailValidationRule`, `TestModeRule`, `ArchiveCopyRule`), e nenhuma ficou de fora. Fora da estrutura ficam a combinação por modo, que está no serviço (`service/MessageService.java:25-31`), e a gravação, que fica depois da corrente (`service/MessageService.java:82-102`).
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede o Decorator. O agente fez uma lista de regras aplicada por um despachante (`messaging/MessageDispatcher.java:12-18`), algo próximo de uma Chain of Responsibility, que a referência aceita como alternativa equivalente. O que aproxima do certo:
- cada regra é uma peça própria com a mesma interface (`messaging/MessageRule.java:3-5`);
- nenhuma regra pergunta pelo modo de teste: a cópia não verifica o modo (`messaging/ArchiveCopyRule.java:8-19`);
- a combinação é montada de fora das regras, com a cópia só fora do teste (`service/MessageService.java:29`).

O que afasta:
- a combinação é escrita no construtor do `MessageService`, com um booleano por regra (`service/MessageService.java:16-18`). Cada regra nova ("sempre", "só no teste", "só fora") pede um parâmetro novo e mais um `if` ali, em vez de ser empilhada no `main`;
- cada regra precisa lembrar de perguntar se a mensagem já foi bloqueada (`messaging/TestModeRule.java:9`, `messaging/ArchiveCopyRule.java:9`), em vez de a corrente parar;
- o envio de verdade não é a ponta da estrutura: ele fica em `MessageService.send`, que grava as mensagens extras sempre como `SENT` (`service/MessageService.java:99`).

Não chega a ser malfeito, porque as três regras estão na estrutura e nenhuma conhece o modo.

**O exagero.** Não há fábricas, builders nem interfaces com uma implementação só fora do pattern. Dois detalhes ficam abaixo do limiar do exagero:
- os DTOs da API copiam campo a campo os records do domínio (`api/DocumentResponse.java:9-38` repete `domain/ReservationDocument.java`);
- o parâmetro `excludeId`, sempre `null` (`service/ReservationService.java:54`), é uma generalidade que ninguém usa, e é justamente ele que quebra a consulta.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`service/PricingService.java:15-42`), `LocalDate` para datas e `enum` para situação, plano e tipo. Os defeitos:
- `catch (Exception e)` em `api/ApiController.java:75` transforma qualquer falha, inclusive de banco, em 400 `INVALID_REQUEST`, e foi isso que escondeu o erro que quebra o sistema;
- o mesmo bloco `try/catch` de `NumberFormatException` e `ReservationNotFoundException` se repete em seis handlers (`api/ApiController.java:85, 97, 111, 125, 140, 161`), em vez de um `app.exception(...)`;
- `formatMoney` usa `String.format("R$ %,.2f")` com a localidade padrão da JVM e depois troca os caracteres (`service/MessageService.java:117`). Numa JVM em pt_BR, o resultado sai invertido ("R$ 1,234.56");
- o total do relatório financeiro começa com `BigDecimal.ZERO`, sem casas decimais (`service/ReportService.java:57`), e sai "total,0" quando não há reserva (visto no roteiro);
- o `if/else` de `service/ReservationService.java:117-121` grava o mesmo valor nos dois ramos;
- as exceções de negócio não têm mensagem (`service/ReservationService.java` no fim do arquivo).

## Testes

**Números:** 19 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 19 métodos de teste escritos. A cobertura é de 12,4% das linhas (93 de 751) e 18,6% dos ramos (29 de 156). `messaging` tem 64 de 66 linhas cobertas, `service` 26 de 196, e `api`, `repository`, `infra` e a raiz têm 0.
**Nota:** com problemas

**O que os testes conferem bem.** Há duas classes de teste de unidade que conferem valores:
- `PricingServiceTest` confere o preço em baixa e alta temporada (299,88 em `PricingServiceTest.java:33`), dezembro antes e depois do dia 15, o subtotal de 599,76 (`:75`), o desconto de 116,97 (`:84`) e o total;
- `MessageRulesTest` confere a parte nova regra por regra: o bloqueio de "ana.gmail.com" e de "ana@gmail" (`MessageRulesTest.java:13-31`), o desvio para testes com assunto e primeira linha exatos (`:53-55`), a cópia com "[Cópia] " (`:80-82`) e a falta de cópia quando a mensagem é bloqueada (`:94`, `:122`).

**O que fica sem rede.**
- `BasicIT.java:15` é um `assertTrue(true)`. Nenhum teste toca a API, o banco, a máquina de estados da reserva, o cancelamento e a devolução, os códigos de erro ou os relatórios. Por isso o sistema passou no `mvn verify` sem conseguir criar uma reserva.
- A pergunta da mutação na parte nova:
  - apagar `!testMode` em `service/MessageService.java:29`, o que faria a cópia sair também no modo de teste, não derruba nenhum teste, porque a montagem das regras não é testada;
  - trocar `atIndex != email.lastIndexOf('@')` em `messaging/EmailValidationRule.java:18` não derruba nenhum teste, porque não há caso com dois @ nem com "@gmail.com";
  - o texto do e-mail de confirmação (`service/MessageService.java:47-80`) também não é conferido.
- Janeiro e fevereiro como alta temporada e o limite exato de 14 para 15 de dezembro ficam sem teste.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- a chave estrangeira `reservations(room_number)` sem índice;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- nenhum CHECK nas colunas de situação, plano e tipo;
- nenhuma migração: o esquema é criado no código;
- 0 transações explícitas e 0 `FOR UPDATE`.

**Nota:** ruim

**O que está certo.** O SQL é sempre parametrizado, as conexões são devolvidas com try-with-resources e há um pool.

**O defeito sério.** Nada impede duas reservas do mesmo quarto na mesma noite. A conferência é um `SELECT COUNT(*)` (`repository/ReservationRepository.java:130-155`) seguido de um `INSERT` em outra conexão, em autocommit (`service/ReservationService.java:54` e `:82`). Não há transação, trava nem restrição de exclusão, e nem sequer um `synchronized`. Dois pedidos simultâneos podem passar os dois pela conferência e gravar a reserva em dobro, um risco direto de dado errado. O roteiro não conseguiu mostrar isso só porque nenhuma reserva é criada.

**O resto.**
- O esquema é criado com `CREATE TABLE IF NOT EXISTS` em `infra/DatabaseSetup.java:19-56`, sem migrações versionadas, então o próximo pedido que mudar uma coluna não terá como evoluir um banco que já existe.
- `confirm` grava a reserva e depois as mensagens sem uma transação em volta (`service/ReservationService.java:94-97`). Se a gravação da mensagem falhar, a reserva fica confirmada sem o e-mail.

A consulta que quebra a criação (`repository/ReservationRepository.java:137`, `:146-147`) é do código de banco, mas aparece numa chamada que o texto descreve, por isso pesa na coluna do comportamento.

## Comportamento

**Números:** 17 de 65 conferências passaram e 48 falharam:
- a parte comum: 14 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário no modo normal: 1 de 9;
- o cenário no modo de teste: 0 de 5;
- as reservas ao mesmo tempo: 1 de 2.

**Nota:** ruim

O serviço sobe e cria o esquema, e `GET /rooms` devolve os cinco quartos. As recusas que acontecem antes da consulta de conflito funcionam: 404 `ROOM_NOT_FOUND`, 422 `CAPACITY_EXCEEDED`, 400 para a saída igual à entrada, para zero hóspedes, para o corpo que não é JSON e para os campos faltando. Também funcionam 404 `RESERVATION_NOT_FOUND` e 400 para o mês fora do formato.

**O que falhou e por quê.**
- Todo `POST /reservations` válido responde `400 {"error":"INVALID_REQUEST","message":"Invalid request"}`, inclusive no teste que fiz à mão com `roomNumber` em texto. A causa é a consulta de `repository/ReservationRepository.java:137`, que o PostgreSQL recusa, com a falha mascarada por `api/ApiController.java:75`. Daí em diante tudo falha em cascata: preço e desconto, numeração a partir de 1, confirmação e e-mail, segunda via, check-in, check-out, os três cancelamentos e devoluções, os dois relatórios, os dados depois de reiniciar e as 14 conferências do cenário.
- Nas dez rodadas simultâneas, os dois pedidos voltaram 400. Por isso "nenhuma com as duas criadas" passa sem provar nada.
- As poucas conferências do cenário que passam ("a cópia tem o mesmo texto", "as mensagens continuam lá") passam sobre uma lista vazia.
- Duas falhas da parte comum vêm, em parte, de uma escolha que o texto deixava em aberto. O agente guarda o número do quarto como texto (`"201"`), e o roteiro procura `number == 201`. Isso não pesa: a criação falha mesmo com o número em texto.

**Para o dono da pousada:** o site não consegue fazer nenhuma reserva. Por consequência, nenhum e-mail sai, as regras de bloqueio, de cópia e de modo de teste nunca chegam a agir, e os relatórios vêm vazios.

## Dúvidas

- **Classe do pattern.** Ficou entre "alternativa equivalente" e "malfeito", porque a combinação por modo mora no `MessageService` (`service/MessageService.java:21-34`) e não no `main`. Dei a primeira porque nenhuma regra conhece o modo e as três estão na estrutura. As regras de envio só foram vistas funcionando nos testes de unidade, nunca pela API.
- **Nota do banco.** A duplicação de reservas simultâneas não foi medida pelo roteiro, porque nenhuma reserva é criada. A nota `ruim` se apoia na leitura do código (conferência e inserção separadas, sem transação, trava nem restrição).
- **Formato do dinheiro.** O defeito de localidade de `formatMoney` (`service/MessageService.java:117`) depende da localidade da JVM onde o sistema roda, e não pude vê-lo no roteiro.
