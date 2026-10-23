# Revisão: construção, cenário 07

## Visão geral

O projeto (Java 21, Javalin, JDBC puro com HikariCP) está dividido em `api.handler`/`api.dto`/`api.mapper` (a borda HTTP), `service` (as regras de reserva, o preço e o envio de mensagens), `messaging` + `messaging.rules` (as regras de envio), `reports` (os dois CSV), `database` (os DAOs e o `CREATE TABLE` no código) e `domain`. Uma requisição entra pela rota registrada em `Application.java:67-80` e vai para um handler (`ReservationHandler`), que valida o corpo e chama o `ReservationService`. O serviço calcula o preço (`PricingCalculator`), grava pelo `ReservationDao` e, na confirmação, manda o e-mail pelo `MessageService`, que passa a mensagem pelo `MessagePipeline` e grava o resultado em `messages`. Os dados ficam no PostgreSQL, em três tabelas criadas por `Database.java:43-81`. As regras de e-mail do cenário ficaram numa lista de regras montada conforme o modo em `Application.java:90-102`, e o roteiro do cenário passou inteiro (14 de 14). O mais sério é que nada impede reservar duas vezes o mesmo quarto: nas 10 rodadas de pedidos simultâneos, as duas reservas foram criadas em todas as 10. Não há restrição no banco, nem transação, nem trava. Além disso, o dinheiro sai no JSON como `double` (`0.0`, `779.8`, `389.9`), contra os 2 casas que o texto pede, e um corpo que não é JSON (ou um id que não é número) dá 500. Também saem a segunda via de uma reserva cancelada e um e-mail para `ana@gmail.`, endereço que o texto manda bloquear.

## Arquitetura

**Números:** 11 pacotes, 25 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece na raiz e em `api.handler`, e o JDBC só em `database`. A maior classe é `ReservationDao`, com 253 linhas.
**Nota:** com problemas

As camadas existem e as dependências vão na direção certa: a borda chama o serviço, e o serviço chama os DAOs. O serviço não conhece o Javalin nem o SQL (`service/ReservationService.java`). Todo o SQL fica em `database/`. A montagem está num lugar só (`Application.java:38-57`), com injeção pelo construtor. Há porém misturas localizadas:

- A regra de quem tem direito à segunda via está no handler (`api/handler/ReservationHandler.java:131`), e não no serviço. Por isso ela ficou incompleta: só a reserva PENDING é recusada, e a CANCELLED também deveria ser.
- O handler e o mapeador falam direto com o acesso a dados. `ReservationHandler` recebe um `RoomDao` (`api/handler/ReservationHandler.java:21,103,135`), e o `ApiMapper.toDocumentResponse` faz uma consulta ao banco para descobrir o tipo do quarto (`api/mapper/ApiMapper.java:45`). Um mapeador de DTO que consulta o banco está fora do lugar. O `RoomHandler` também usa o DAO direto, o que num GET simples pesa menos.
- O texto do comprovante e do e-mail é montado dentro do `ReservationService` (`service/ReservationService.java:156-210`): a formatação de apresentação mora junto da regra.
- Não há um tratador de exceções central. Cada handler mapeia exceção para código HTTP com o seu próprio `try/catch` (`api/handler/ReservationHandler.java:51-58` e seguintes), e o que escapa (JSON inválido, id não numérico) vira 500.

## Design de baixo nível

**Números:** as 3 regras do cenário (bloquear endereço inválido, copiar para o arquivo, desviar no modo de teste) estão todas na estrutura, cada uma numa classe que implementa `MessageRule`. Nenhuma ficou fora, e nenhuma regra pergunta pelo modo de teste. A combinação por modo é decidida em 1 lugar (`Application.java:90-102`).
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente não usou embrulhos com a mesma interface do envio, como no Decorator. Fez uma lista de filtros que o `MessagePipeline` aplica em ordem sobre um `MessageContext` (`messaging/MessagePipeline.java:13-25`). É a alternativa que a referência aceita, porque:

- cada regra é uma peça própria: `messaging/rules/EmailValidationRule.java`, `ArchiveCopyRule.java` e `TestModeRedirectRule.java`;
- a combinação por modo fica na montagem: a validação sempre, e o desvio ou a cópia conforme `testMode` (`Application.java:93-99`);
- o envio (`service/MessageService.java:20-35`) não sabe de modo nenhum.

O que afasta do Decorator, sem mudar a classe:

- As regras dependem da ordem da lista. A cópia e o desvio conferem `context.isBlocked()` (`messaging/rules/ArchiveCopyRule.java:13`, `messaging/rules/TestModeRedirectRule.java:14`), e isso só funciona porque a validação vem antes.
- O estado compartilhado (`MessageContext` com `setMessage` e `addMessage`) é mutável.

Ainda assim, as regras anunciadas (madrugada, limite por dia) entrariam como mais uma classe e mais uma linha na montagem.

**O exagero.** Não há estrutura sem necessidade. A única interface (`MessageRule`) tem 3 implementações. Não há fábricas nem builders, e nenhuma camada só repassa.

**O resto do código.**

- **Dinheiro como `double` na API.** O cálculo usa `BigDecimal` com `HALF_EVEN` em todos os passos (`service/PricingCalculator.java:52-53,97-104,136-138`). Mas os DTOs convertem tudo para `double` (`api/mapper/ApiMapper.java:19,35-40,58-60`), e o JSON perde as 2 casas: `0.0`, `779.8`.
- **Duplicação.**
  - A busca com `orElseThrow(ReservationNotFoundException)` aparece 5 vezes (`service/ReservationService.java:78,95,109,123,140`).
  - O mesmo bloco `try/catch` de mapeamento de erro se repete em cada método dos handlers (`api/handler/ReservationHandler.java`).
  - A validação do formato das datas e do plano é feita duas vezes: em `validateRequest` (`api/handler/ReservationHandler.java:171-187`) e de novo logo depois (`:34-36`).
- **Estado.**
  - `Reservation` é mutável por setters livres (`domain/Reservation.java`). As transições de situação ficam no serviço, com um `canX()` no domínio.
  - `OutgoingMessage` é meio imutável (`withModifications`, `copy`) e meio mutável (`block()`, `messaging/OutgoingMessage.java:49`).
- **Detalhes.**
  - Nome qualificado inteiro dentro do código (`messaging/MessageContext.java:32`).
  - Imports sem uso (`JsonInclude` em `api/dto/DocumentResponse.java:3`).
  - Um `@JsonInclude(CUSTOM)` sem filtro (`api/dto/ReservationResponse.java:22`).
  - Datas do comprovante em JSON como `dd/MM/yyyy` (`api/mapper/ApiMapper.java:46,55-56`), diferente do resto da API.
- **A regra do endereço está incompleta.** `domain.contains(".")` (`messaging/rules/EmailValidationRule.java:30`) aceita `ana@gmail.` e `ana@.com`, e o texto pede um ponto "no meio".

São defeitos localizados, não espalhados.

## Testes

**Números:** 11 testes rodaram, 0 falharam, 0 pulados; 11 métodos escritos em 2 classes. A cobertura é de 15,4% das linhas (130 de 845) e 17,4% dos ramos (31 de 178). Fora da cobertura: `api.*`, `database`, `reports` e `com.pousadamarealta` (0%) e `service` (40 de 148 linhas). Não há teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

O que existe confere valores:

- `PricingCalculatorTest.java` confere o exemplo do texto (599.76 em julho, `:169-181`), a baixa temporada (779.80, `:196-208`) e o desconto do não reembolsável com arredondamento (116.97 e 662.83, `:211-223`).
- `MessagePipelineTest.java` confere a parte nova:
  - o bloqueio com o endereço original (`:21-38`);
  - a cópia logo depois, com "[Cópia] " e o mesmo texto (`:60-85`);
  - nenhuma cópia para mensagem bloqueada (`:88-105`);
  - o desvio no modo de teste, com "[TESTE] " e a primeira linha "Destinatário original" (`:108-129`);
  - o bloqueio também no modo de teste (`:132-150`).

As lacunas:

- O teste de dezembro a fevereiro só confere o número de noites (`PricingCalculatorTest.java:184-193`). Trocar `day >= 15` por `day >= 16` (`service/PricingCalculator.java:121`) não derrubaria nenhum teste.
- Na validação de e-mail, só se testam `invalid.email`, `invalid` e um endereço válido. Apagar a conferência do segundo `@` (`messaging/rules/EmailValidationRule.java:25`) ou trocar `atIndex <= 0` por `< 0` (`:21`) passaria. O caso `ana@gmail.`, que o código aceita errado, não foi testado.
- A montagem por modo (`Application.java:90-102`), que é onde se decide que a cópia não sai no modo de teste, não tem teste.
- Ficaram sem nenhum teste: as transições de situação, o cancelamento e a devolução, os códigos de erro, o comprovante e o e-mail, os dois relatórios e a persistência.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências de `measures/database.md` acusaram:

- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhuma restrição CHECK, UNIQUE ou EXCLUDE;
- 0 transações explícitas e 0 `FOR UPDATE`.

O esquema é criado por `CREATE TABLE IF NOT EXISTS` no código, sem migrações.
**Nota:** ruim

O que está certo:

- os tipos (`database/Database.java:62-67`);
- `NOT NULL` no obrigatório;
- a chave estrangeira para o quarto (`database/Database.java:68`);
- todo o SQL com parâmetros `?`, e nenhum SQL montado com `+`;
- as conexões devolvidas por try-with-resources, com pool Hikari (`database/Database.java:15-21`).

O defeito sério: nada protege a regra "um quarto não pode ter duas reservas na mesma noite". O serviço consulta (`service/ReservationService.java:47`, que chama `database/ReservationDao.java:126`) e depois insere (`service/ReservationService.java:73`, que chama `database/ReservationDao.java:21`), cada passo numa conexão com autocommit. Não há transação, trava nem restrição de exclusão no banco, nem mesmo um `synchronized`. O roteiro do comportamento reproduziu isso em 10 de 10 rodadas: as duas reservas simultâneas foram criadas. É risco de dado errado (quarto vendido duas vezes).

Os outros defeitos:

- O esquema não evolui. `CREATE TABLE IF NOT EXISTS` (`database/Database.java:44,51,73`) não acrescenta coluna numa tabela que já existe.
- Não há CHECK sobre `status`, `rate_plan` e `type`, nem sobre `guests >= 1`.
- A confirmação grava a reserva (`service/ReservationService.java:86`) e depois as mensagens (`service/MessageService.java:24-34`), em escritas separadas, sem transação.
- A tabela `rooms` guarda só número e tipo. A capacidade e a diária ficam no enum `RoomType`, o que é aceitável, já que "os quartos quase nunca mudam".
- `database-example.sql` é um arquivo solto, sem uso.

## Comportamento

**Números:** 60 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 42 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário no modo normal | 9 de 9 |
| o cenário no modo de teste | 5 de 5 |
| as reservas simultâneas | 1 de 2 |

**Nota:** com problemas

O que falhou e o que quer dizer para o dono da pousada:

- **Reservas simultâneas: duas reservas para o mesmo quarto nas mesmas noites, nas 10 rodadas.** Se dois hóspedes reservarem pelo site ao mesmo tempo, o quarto é vendido duas vezes. O defeito pesa na coluna do banco; aqui ele é citado.
- **Dinheiro sem as 2 casas no JSON** (`"discount":0.0`, `"subtotal":779.8`, `"nightlyRate":389.9`). O texto pede "número com 2 casas", e o site que espera `0.00` recebe outro formato. Vem dos `doubleValue()` em `api/mapper/ApiMapper.java:19,35-40`.
- **Corpo que não é JSON: 500 em vez de 400 INVALID_REQUEST.** O `bodyAsClass` (`api/handler/ReservationHandler.java:30`) lança uma exceção que ninguém trata. Chamando a API à mão, `GET /reservations/abc` também deu 500 (`Long.parseLong` em `:64`).
- **"o 201 comporta 3" e "a diária do 201 é 389.90".** O agente devolve `number` como texto (`"201"`), e o roteiro procura o número `201`. O texto não fixava o tipo, então essa escolha é razoável e não pesa. A capacidade veio certa (3), mas a diária veio `389.9`, que é a mesma falha das 2 casas.

Fora do roteiro, chamando a API à mão:

- A segunda via de uma reserva **cancelada** respondeu 200 com o comprovante (`api/handler/ReservationHandler.java:131` só recusa PENDING). O texto diz que ela só existe para reserva confirmada, com check-in ou com check-out.
- O endereço `ana@gmail.` recebeu o e-mail como SENT e gerou a cópia para o arquivo, quando deveria ficar BLOCKED (`messaging/rules/EmailValidationRule.java:30`).

O resto da parte comum funciona:

- o preço com temporada e arredondamento;
- os erros 404, 409 e 422;
- a confirmação com o e-mail e o comprovante no formato do texto;
- o check-in e o check-out;
- as devoluções;
- os dois relatórios.

O cenário 07 passou inteiro nos dois modos.

## Dúvidas

- O tipo de `number` em `GET /rooms` (texto ou número) o texto não fixou. Tratei como escolha razoável.
- Na nota do banco, pesei a falta total de proteção contra reserva dupla (reproduzida em 10 de 10 rodadas) como "risco de dado errado", e por isso dei `ruim`. A skill põe em `com problemas` só a proteção "dentro do processo"; aqui não há nenhuma.
- Entre `acerto` e `alternativa equivalente`, a evidência é de uma lista de filtros aplicada pelo pipeline, e não de embrulhos com a interface do envio. Por isso, `alternativa equivalente`.
