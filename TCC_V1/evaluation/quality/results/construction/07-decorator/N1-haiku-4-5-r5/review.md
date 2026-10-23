# Revisão: construção, cenário 07

## Visão geral

O projeto tem 6 pacotes: `api`, `service`, `database`, `model`, `util` e a raiz com `Main`. Tudo é `static`, e nenhuma peça é montada em lugar nenhum: `Main` chama `Database.initialize()` e `HotelAPI.setup(app)` (`Main.java:9-13`). Uma requisição entra num handler de `HotelAPI`. O handler valida e chama métodos estáticos dos serviços (`ReservationService`, `RoomService`, `MessageService`, `DocumentService`, `ReportService`), e esses serviços escrevem o SQL direto com JDBC sobre um pool Hikari global (`Database.java:9`, `:31`). O esquema sai de `CREATE TABLE IF NOT EXISTS` no código (`Database.java:35-78`). Há dois defeitos graves:

- **A confirmação quebra.** O handler lê um corpo JSON que não usa (`HotelAPI.java:124`). Sem corpo, como o site chama, a resposta é 500, e com isso não há e-mail, check-in, check-out nem relatório. O roteiro do comportamento passou só 32 de 65 conferências.
- **A cópia para o arquivo nunca é gerada.** A condição `!TEST_MODE && !finalTo.equals(to)` é sempre falsa fora do modo de teste (`MessageService.java:46`).

As três regras de envio também ficaram como `if`s dentro de `MessageService.sendMessage`, sem a estrutura que o texto pedia (o Decorator).

## Arquitetura

**Números:** 6 pacotes, 8 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `api` e na raiz; JDBC em `service` e `database`. A maior classe é `HotelAPI` (325 linhas), seguida de `ReservationService` (195).
**Nota:** com problemas

As dependências vão na direção certa (api → service → database) e não há ciclo. Mas as camadas se misturam em dois pontos:

- **Regras nos handlers.** O handler de criação decide a ordem das recusas e as faz ele mesmo: a capacidade, o mínimo de hóspedes e a disponibilidade (`HotelAPI.java:81-94`). O handler de confirmação orquestra o caso de uso inteiro (confirmar, buscar o quarto, montar o comprovante, mandar o e-mail) e ainda escreve no modelo (`HotelAPI.java:132-138`). A regra de "segunda via só para reserva paga" mora no handler (`HotelAPI.java:219`), e a busca de 404 se repete em cada handler (`HotelAPI.java:108-113`, `151-156`, `170-175`, `193-198`, `212-217`).
- **Regra e SQL na mesma classe.** Os serviços escrevem o SQL junto com a regra, sem classe de acesso a dados. A devolução do cancelamento é calculada no meio de um SELECT e de um UPDATE (`ReservationService.java:124-156`), e a "taxa" de ocupação tem o 5 fixo dentro do SQL do relatório (`ReportService.java:47`).

Não há montagem: tudo é estático, e o modo de teste é lido do ambiente dentro do serviço de mensagens (`MessageService.java:15`), não na partida. Por isso não dá para trocar uma peça nem combinar regras por instalação sem mexer no código.

## Design de baixo nível

**Números:** as 3 regras de envio que o texto pede (bloqueio, cópia para o arquivo, modo de teste) estão como 3 `if`s dentro de um único método, `MessageService.sendMessage` (`MessageService.java:28-50`). Nenhuma delas fica numa peça própria, e não há interface de envio.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto anuncia mais regras pela frente (madrugada, limite por dia), cada uma valendo sempre, só no teste ou só fora dele, e combinações diferentes por instalação. O que ele pedia era o Decorator: uma interface de envio e um embrulho por regra, empilhados na montagem. O agente escreveu tudo num método só:

- o bloqueio é um `if` (`MessageService.java:29-32`);
- o desvio de teste é outro `if`, que lê uma constante estática tirada do ambiente (`MessageService.java:15`, `38-42`);
- a cópia é um terceiro `if`, com a combinação por modo escrita dentro dela (`MessageService.java:46-49`).

Essa condição da cópia, aliás, está errada: fora do teste `finalTo` é igual a `to`, então a cópia nunca sai. Uma segunda pousada sem a cópia, ou uma regra nova, exigiria mexer nesse método. É a omissão que a referência descreve. Nada aproxima o código do pattern.

**O exagero.** Não há interfaces, fábricas nem camadas de repasse, então a classificação é contenção. O que sobra é código morto: o construtor completo de `Reservation` (`Reservation.java:29-48`) não é usado.

**O resto do código.**
- **Tipos.** Dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`PricingUtil.java:11-13`, `28-55`) e datas em `LocalDate`: certo. Mas a situação da reserva, o plano e o tipo do quarto são `String` soltas (`Reservation.java:23`, `:18`; `ReservationService.java:136`; `PricingUtil.java:47`). O plano nem é validado: qualquer texto vira um "STANDARD" sem desconto.
- **Estado.** Os modelos têm campos públicos mutáveis (`Reservation.java:9-25`, `Room.java:6-9`), e o handler muda `res.roomType` por fora em três lugares (`HotelAPI.java:135`, `179`, `225`).
- **Erros.** Todo handler termina em `catch (Exception)`, que vira 500 e devolve a mensagem interna da exceção ao cliente (por exemplo `HotelAPI.java:100-102`). Por isso um corpo que não é JSON, ou um id de reserva que não é número, vira 500 em vez de 400. A camada de serviço sinaliza recusas com `IllegalStateException("OPERATION_NOT_ALLOWED")` em texto (`ReservationService.java:87`).
- **Duplicação.** A busca de 404 se repete nos handlers, e o mapeamento de linha para `Room` está duplicado (`RoomService.java:20-25` e `39-44`).

## Testes

**Números:** 12 testes rodaram, 0 falharam (12 métodos escritos em 2 classes). Cobertura de linhas 35 de 585 (6,0%) e de ramos 33 de 148 (22,3%); `api`, `service`, `database` e `model` estão em 0% (`measures/tests.md`).
**Nota:** ruim

Os testes que existem conferem valores de verdade, mas cobrem só as duas classes utilitárias:

- a temporada (`PricingUtilTest.java:10-29`);
- R$ 299,88 por noite e R$ 599,76 no total (`PricingUtilTest.java:41`, `51`);
- o desconto de R$ 89,96 (`PricingUtilTest.java:62`);
- o arredondamento (`PricingUtilTest.java:66-74`);
- a validação de e-mail com os exemplos do texto (`ValidationUtilTest.java:101-107`).

Ficaram sem teste nenhum as situações da reserva, o cancelamento e a devolução, os códigos de erro, os relatórios, o comprovante e o e-mail. Da parte nova do cenário, só o validador foi testado: o desvio de teste e a cópia não têm teste.

**A pergunta da mutação.** Se alguém apagasse a cópia, trocasse "[TESTE] " ou removesse a linha "Destinatário original", nenhum teste falharia. A prova é que a cópia já está quebrada (`MessageService.java:46`) e o `mvn verify` passa. A confirmação que devolve 500 também passou sem nenhum teste da API. O validador aceita `ana@gmail.` (o ponto no fim, não "no meio"; `ValidationUtil.java:25`), e nenhum teste confere esse caso. Com 6% de linhas cobertas e nada da parte nova além do validador, a coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram (`measures/database.md`):
- a chave estrangeira `reservations.room_number` sem índice;
- nenhum CHECK nas colunas de situação, plano, tipo e canal;
- nenhuma restrição UNIQUE ou EXCLUDE;
- 0 transações explícitas e 0 `FOR UPDATE`.

Não há SQL montado com dado de fora.
**Nota:** com problemas

**O que está certo.** Os tipos, as chaves, o `NOT NULL` e o SQL com `?` em todo lugar (por exemplo `ReservationService.java:33-50`). As conexões são devolvidas por try-with-resources sobre um pool Hikari (`Database.java:20-28`). Os dados sobrevivem ao reinício: a conferência das mensagens passou. As duas falhas de reinício do roteiro vêm do fluxo quebrado da confirmação, não do banco.

**Os defeitos:**
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código, sem migração (`Database.java:35-78`), e uma coluna nova numa tabela que já existe não seria aplicada.
- **Nada impede a reserva dupla.** `isRoomAvailable` faz um `SELECT COUNT(*)` e o INSERT vem depois, em outra conexão, sem transação, trava ou restrição (`HotelAPI.java:91`, `96`; `ReservationService.java:158-174`). O roteiro de concorrência criou as duas reservas em 10 de 10 rodadas: o quarto vendido duas vezes na mesma noite.
- **O cancelamento não é atômico.** Ele lê e depois atualiza sem transação nem condição de situação no UPDATE (`ReservationService.java:126-152`), e pode, numa corrida, cancelar uma reserva que acabou de fazer check-in.
- **A confirmação também não é atômica.** Ela grava a reserva e depois as mensagens fora de transação (`HotelAPI.java:132-138`): se o envio falhar, a reserva fica confirmada sem e-mail.

## Comportamento

**Números:** 32 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 29 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário, modo normal | 1 de 9 |
| cenário, modo de teste | 0 de 5 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

**O fluxo principal quebra na confirmação.** `POST /reservations/{id}/confirmation` sem corpo, como o site chama, responde 500 "No content to map due to end-of-input". Reproduzi com `curl`. A causa é o `ctx.bodyAsClass(JsonNode.class)` sem uso no handler (`HotelAPI.java:124`). Em cascata:
- nenhuma reserva é confirmada e nenhum e-mail sai;
- a segunda via, o check-in e o check-out dão 409;
- o cancelamento "depois do check-in" é aceito, porque a reserva continua pendente;
- o cancelamento STANDARD devolve 0,00;
- os relatórios de ocupação e financeiro saem vazios.

Para o dono da pousada: o site não conseguiria confirmar nenhuma reserva paga.

**Outras falhas:**
- Um corpo que não é JSON dá 500 em vez de 400 INVALID_REQUEST (`HotelAPI.java:47`, `100-102`).
- As duas reservas simultâneas foram criadas em 10 de 10 rodadas, o que significa quarto vendido duas vezes. O defeito foi contado na coluna do banco.

**O cenário.** O roteiro não chegou a medir as regras de envio, porque elas dependem da confirmação. Conferi à mão, mandando `{}` como corpo:
- o bloqueio funciona, com status BLOCKED e o endereço original, também no modo de teste;
- o modo de teste funciona: vai para `testes@…`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: …";
- a cópia para `arquivo@pousadamarealta.com.br` nunca aparece no modo normal (`MessageService.java:46`).

Mesmo com o corpo contornado, o contador não receberia cópia de nada.

Duas escolhas que o texto deixava em aberto, sem peso na nota: as datas do comprovante vêm em `dd/MM/yyyy` no JSON (`DocumentService.java:54-55`), e a segunda via é liberada também para uma reserva cancelada que tinha sido paga (`HotelAPI.java:219`).

## Dúvidas

A nota da arquitetura ficou entre `com problemas` e `ruim`. As camadas existem e não há ciclo, mas há regras nos handlers, SQL nos serviços e nenhuma montagem. Fiquei com `com problemas` porque o SQL não está nas rotas e as regras não dependem do Javalin. No banco, a reserva dupla sem proteção nenhuma (10 de 10 rodadas) pesa perto de `ruim`. Fiquei com `com problemas` porque os tipos, as chaves e o SQL com parâmetros estão certos e os dados não se perdem.
