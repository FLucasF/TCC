# Revisão: construção, cenário 03

## Visão geral

O projeto tem cinco pacotes: `api` com os handlers do Javalin, `services` com as regras e os avisos, `infrastructure` com o JDBC, o pool e o cálculo de preço, `domain` com as entidades e os enums, e a raiz com o `Main`. Todos os métodos são `static`. Uma requisição entra num handler estático (`Main.java:62-72`), que lê o corpo como `Map`, chama `ReservationService` e devolve um `Map` como JSON. O serviço chama os repositórios estáticos, que tiram a conexão de um `HikariDataSource` estático (`Database.java:9`). O esquema é criado com `CREATE TABLE IF NOT EXISTS` no código. Os avisos passam por um `ReservationEventPublisher` estático com uma única implementação, `HostelNotificationListener`, que manda os e-mails do hóspede, da governança e do financeiro.

O mais sério é que o sistema não funciona pela API. Todo `POST /reservations` responde 500 e mesmo assim grava a reserva. Cada aviso grava só a primeira mensagem e perde as outras. Os dois defeitos têm a mesma causa: um `INSERT ... RETURNING id` preparado com `Statement.RETURN_GENERATED_KEYS` e lido com `executeQuery()` (`ReservationRepository.java:34-36,51`, `MessageRepository.java:17-18,24`). Além disso, o CPF com pontuação, como o do comprovante, estoura a coluna `VARCHAR(11)` (`Database.java:41`). O roteiro passou em 18 de 66 conferências.

## Arquitetura

**Números:** 5 pacotes e 9 dependências entre eles, sem ciclo. O Javalin aparece só em `api` e na raiz, o JDBC e o pool só em `infrastructure`. As maiores classes são `ReservationController` (208 linhas) e `ReservationRepository` (198 linhas).
**Nota:** com problemas

O que está bem:
- As camadas existem e as dependências apontam para o lado certo: `api` → `services` → `infrastructure` → `domain`, sem ciclo.
- `services` não conhece o Javalin nem o JDBC.

O que está fora do lugar:
- **Handlers que pulam o serviço e decidem regra.** `ReservationController.java:56`, `:110` e `:145`, `RoomController.java:11` e `MessageController.java:10` vão direto aos repositórios. A regra de quem tem direito à segunda via mora no handler (`ReservationController.java:113`), e está errada: só recusa a reserva PENDING e deixa a CANCELLED passar.
- **Regra de negócio na camada de dados.** O cálculo de preço e de temporada, `PriceCalculator`, fica no pacote `infrastructure` (`PriceCalculator.java:1`).
- **Não há montagem.** Tudo é estático: os repositórios, `Database.dataSource` (`Database.java:9`) e a lista de ouvintes (`ReservationEventPublisher.java:10`). As classes se acham por nome de classe. O `Main` só inicializa o banco e registra um ouvinte (`Main.java:32,36`), e nada é passado de fora para quem usa.
- **O comprovante é montado em dois lugares, de dois jeitos.** Em texto no ouvinte (`HostelNotificationListener.java:107-128`) e como JSON no controller (`ReservationController.java:144-164`).

A mistura é localizada e não há ciclo, mas o estático em toda parte dificulta trocar peças e testar.

## Design de baixo nível

**Números:** há 1 implementação de ouvinte (`HostelNotificationListener`) para 3 interessados (hóspede, governança e financeiro). Nenhum interessado tem ouvinte próprio, e os 3 destinos estão escritos dentro da mesma classe. A interface tem 4 métodos, e 2 deles ficam vazios na única implementação.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** Existe um mecanismo de publicação: a interface `ReservationEventListener` (`ReservationEventListener.java:3-11`) e um publicador que percorre a lista (`ReservationEventPublisher.java:16-54`). O serviço só anuncia o evento (`ReservationService.java:82,103`) e não monta e-mail, e isso aproxima do Observer.

Mesmo assim, o único ouvinte é um notificador que conhece todos os destinos:
- o hóspede em `HostelNotificationListener.java:21-26`;
- a governança em `:29-44` e `:70-83`;
- o financeiro em `:47-57` e `:86-98`.

O `Main` registra só esse ouvinte (`Main.java:36`). A referência do cenário diz que um notificador único que conhece todos os destinos é omissão, mesmo numa classe separada. Para o restaurante entrar ou sair, alguém teria de editar essa classe ou desmontar o que ela faz. Há ainda outros afastamentos:
- **O publicador é global e estático.** A lista é `static` e mutável (`ReservationEventPublisher.java:10`), e o serviço não a recebe de fora.
- **Todo ouvinte novo carrega métodos que não usa.** A interface obriga a implementar `onCheckedIn` e `onCheckedOut`, que ficam vazios (`HostelNotificationListener.java:101-105`).
- **As falhas somem no log.** O publicador engole as exceções (`ReservationEventPublisher.java:20-22`), e foi isso que escondeu a perda das mensagens da governança e do financeiro (veja Comportamento).

**O exagero.** Fora da parte do pattern não há estrutura sem necessidade: nenhuma fábrica, builder ou camada que só repassa. `ReservationEvent.EventType` nunca é lido (`ReservationEvent.java:10-15`), e os eventos de check-in e check-out não têm ouvinte. É pouca coisa e fica dentro da parte do pattern.

**O resto do código.** Os defeitos estão espalhados:
- **Erros por texto.** O erro é um `Exception` com o código no texto (`ReservationService.java:28,32,36,41`), traduzido por um `switch` sobre `e.getMessage()` que se repete em dois controllers (`ReservationController.java:166-207`, `ReportController.java:47-65`). Uma exceção sem texto dá `NullPointerException` dentro desse `switch`. Qualquer outra exceção vira 500 e devolve a mensagem interna como código de erro (`ReservationController.java:204`).
- **Handler global que não responde.** O `app.exception` do `Main` não faz nada quando há mensagem (`Main.java:74-82`).
- **Dinheiro formatado pelo locale da máquina.** `String.format("%.2f")` aparece em `HostelNotificationListener.java:51,91,147` e `ReportService.java:68,73`, sem o separador de milhar que o texto pede (R$ 1.234,56). Na prática sai "R$ 779.80".
- **Tipos fracos para dinheiro.** O desconto é `double` (`RatePlan.java:8`, `PriceCalculator.java:34`), e o valor pago chega por `doubleValue()` (`ReservationController.java:68`).
- **Estado e tipos.** Os campos das entidades são públicos (`Reservation.java:7-22`), e os enums repetem o nome num campo `value` (`ReservationStatus.java:10`).
- **Código morto e repetição.** `PriceCalculator.calculateSubtotal` não é usado (`PriceCalculator.java:29-32`), e há formatadores de data copiados em quatro classes.

## Testes

**Números:** 6 testes rodaram, 0 falharam, 0 foram pulados, e há 6 métodos de teste escritos, todos em `PriceCalculatorTest`. A cobertura é de 12 de 731 linhas (1.6%) e 10 de 147 ramos (6.8%). `services`, `api` e `domain` têm 0% de cobertura.
**Nota:** ruim

Os testes conferem só a temporada (`PriceCalculatorTest.java:12-30`) e o preço de uma noite de alta temporada (`:33-37`). O "exemplo" refaz a soma das noites dentro do próprio teste (`:45-46`), em vez de chamar o código do sistema. Ficaram sem teste:
- o desconto do NON_REFUNDABLE;
- as situações da reserva e o cancelamento com a devolução;
- os códigos de erro;
- os relatórios;
- toda a parte do cenário, os avisos da governança e do financeiro.

Não há nenhum teste pela API nem contra o banco. Por isso o 500 em toda criação de reserva e as mensagens perdidas passaram despercebidos. A pergunta da mutação: se alguém apagasse a linha que manda o e-mail da governança (`HostelNotificationListener.java:40-44`), trocasse `signum() > 0` por `>= 0` (`:86`) ou tirasse a volta antecipada da reserva pendente (`:65`), nenhum teste falharia.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro é `numeric(10,2)` e as datas são `date`. A chave estrangeira `room_number` tem índice. O script acusou:
- esquema criado no código, sem migração;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- colunas de situação, plano e tipo sem CHECK;
- 0 transações e 0 `FOR UPDATE`.
**Nota:** ruim

O que está bem: o SQL usa parâmetros (`?`) e as conexões são devolvidas ao pool com try-with-resources. O que está errado:

- **Toda inserção falha depois de gravar.** As duas inserções usam `RETURNING id` junto com `Statement.RETURN_GENERATED_KEYS` e leem com `executeQuery()`. O driver responde "No results were returned by the query." depois de gravar a linha (`ReservationRepository.java:34-36,51`, `MessageRepository.java:17-18,24`). Rodei à mão com um CPF só de dígitos e vi três efeitos:
  - a reserva fica gravada, mas o cliente recebe 500;
  - num aviso com várias mensagens, só a primeira é gravada: na confirmação ficou o e-mail do hóspede e na cancelação o da governança, e as outras se perdem;
  - como o modo é autocommit, não há transação que desfaça o que já foi gravado.

  É dado perdido.
- **O CPF não cabe.** `guest_cpf VARCHAR(11)` (`Database.java:41`) não aceita um CPF com pontuação (111.222.333-44), e o erro do PostgreSQL vaza como 500.
- **Nada impede duas reservas na mesma noite.** O serviço faz consulta e depois inserção sem transação nem trava (`ReservationService.java:39-56`), e o banco não tem restrição de exclusão.
- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` em `Database.java:23-75`, que não sabe acrescentar uma coluna a um banco que já existe.
- **Um índice repete a chave primária.** `CREATE UNIQUE INDEX idx_rooms ON rooms(number)` (`Database.java:66`) duplica o índice que a chave primária já cria.

## Comportamento

**Números:** 18 de 66 conferências passaram e 48 falharam:
- parte comum: 14 de 46;
- depois de reiniciar: 1 de 3;
- cenário 03: 2 de 15;
- reservas simultâneas: 1 de 2 (todas as 20 tentativas deram 500).
**Nota:** ruim

O roteiro manda o CPF formatado, e toda criação deu 500 ("value too long for type character varying(11)"). Por isso quase tudo o que vem depois falhou em cascata. Para separar essa causa das outras, subi o serviço à mão com o CPF "11122233344". A criação continuou dando 500 ("No results were returned by the query."), embora a reserva tenha sido gravada. Com o id 1, o resto funcionou em parte:

- **Confirmação.** Sem corpo ela dá 500. Só funciona com `{"amountPaid": ...}` no corpo (`ReservationController.java:67-68`), e o texto diz que o pago é o total da reserva.
- **Avisos da confirmação.** Depois de confirmar, `GET /messages` tinha só o e-mail do hóspede. Os e-mails da governança e do financeiro não foram gravados.
- **Avisos do cancelamento.** Ficou só o e-mail da governança, e o reembolso ao financeiro se perdeu.
- **Formato do dinheiro.** O comprovante no e-mail saiu "Diárias: R$ 779.80", e o texto pede "R$ 779,80".
- **Códigos de erro.** Corpo que não é JSON, campo faltando, data fora do formato, plano desconhecido e id não numérico dão 500 em vez de 400 INVALID_REQUEST.
- **O que funciona.** Os preços batem (NON_REFUNDABLE de uma noite: desconto 37.48, total 212.42), o check-in funciona e o relatório de ocupação sai certo.
- **Relatório financeiro.** Ele exclui toda reserva CANCELLED (`ReservationRepository.java:147`), então uma NON_REFUNDABLE cancelada, que o texto diz que continua contando, some do relatório.

Para o dono da pousada, isso quer dizer:
- o site recebe erro em toda reserva que cria, mesmo quando ela foi gravada;
- a governança e o financeiro não recebem os avisos de confirmação, justamente o problema que o pedido queria resolver;
- os valores saem no formato errado;
- o financeiro do mês não mostra o dinheiro das não reembolsáveis canceladas.

## Dúvidas

- **A classe do pattern.** Ficou entre omissão e malfeito. O mecanismo de ouvintes existe e o serviço não monta e-mails, mas o único ouvinte faz o trabalho dos três interessados. Fiquei com omissão porque a referência classifica explicitamente assim o "notificador único que conhece todos os destinos".
- **O formato do CPF.** O texto não diz em que formato o CPF chega. Guardar só os 11 dígitos seria razoável, se o sistema tirasse a pontuação ou recusasse com 400, mas ele responde 500. De todo modo, a criação também falha com o CPF só de dígitos, então a nota não depende disso.
- **A linha de total do financeiro.** Ela sai como `total,,,,,212.42`, com as colunas vazias, e o roteiro espera `total,<soma>`. O texto não fecha esse formato.
