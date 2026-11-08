# Revisão: construção, cenário 09

## Visão geral

O projeto (`projeto/`) é um Javalin 6.7.0 com JDBC puro, em 7 pacotes: `routes` (os handlers), `service` (as regras e os
quatro relatórios), `repository` (o SQL), `db` (a conexão e a criação do esquema), `model` (classes com campos
públicos), `util` (formatação) e a raiz (`Application`). Uma requisição entra num método de `*Routes`, que lê o corpo
como `Map`, chama um `*Service`, que chama um `*Repository`, que abre uma conexão nova pelo `DriverManager`
(`db/Database.java:95-96`) e roda SQL com parâmetros. Os dados ficam no PostgreSQL, em três tabelas criadas com
`CREATE TABLE IF NOT EXISTS` no código. O problema mais sério: **o serviço não sobe**. `Database.init()` chama
`conn.commit()` numa conexão com autoCommit ligado (`db/Database.java:74` e `:91`), o driver lança "Cannot commit when
autoCommit is enabled" e o `main` morre antes de abrir a porta (`Application.java:12`). Os dois scripts que sobem o
serviço (banco e comportamento) registraram essa falha. O `mvn verify` passa porque o único teste não sobe o serviço
nem usa o banco. Fora isso, os quatro relatórios estão escritos inteiros, cada um com a sua cópia do título, do
cabeçalho e do total, sem o esqueleto comum que o texto pede.

## Arquitetura

**Números:** 7 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `routes` e na raiz, JDBC só em `db` e `repository`, `service` sem Javalin e sem JDBC. A maior classe é `ReservationRoutes` (210 linhas).
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: as regras (`service/ReservationService.java`,
`service/ReportService.java`) não conhecem nem o `Context` nem o SQL, e todo o SQL está nos repositórios
(`repository/ReservationRepository.java`). Os defeitos:

- **A montagem está espalhada.** Cada classe cria as suas dependências por dentro: `service/ReservationService.java:13-15`
  (`new ReservationRepository()`, `new RoomRepository()`, `new MessageService()`), `service/ReportService.java:18-19`,
  `service/MessageService.java:9`, `routes/ReservationRoutes.java:13`, `routes/ReportRoutes.java:12`. O `main` cria uma
  instância nova de `ReservationRoutes`, e com ela um `ReservationService` e repositórios novos, para cada rota
  (`Application.java:20-26`, sete cópias), e outra de `ReportRoutes` para cada relatório (`Application.java:30-33`).
  A conexão é um `static` global (`db/Database.java:8-10`, preenchido em `init()`). Assim não dá para trocar nenhuma
  peça, nem num teste.
- **A borda nem sempre passa pela regra.** `routes/RoomRoutes.java:11` e `routes/MessageRoutes.java:10` chamam o
  repositório direto (por isso existe a dependência `routes -> repository`).
- **A borda decide o código HTTP comparando texto.** O `errorCode` da exceção vira status num `if` por texto, copiado
  em seis handlers (`routes/ReservationRoutes.java:42-51`, `:75-79`, e assim por diante), em vez de um tratador de
  exceções só.

Não é `ruim`: as camadas existem, não há ciclo e a regra não depende do Javalin. Mas a montagem e o estado global são
defeitos que alguém vai ter de corrigir.

## Design de baixo nível

**Números:** 4 relatórios, 0 dentro de um esqueleto comum e 4 escritos inteiros (`service/ReportService.java:21-47`, `:50-86`, `:89-121`, `:124-177`), cada um com a sua cópia da linha de título (`:26`, `:57`, `:97`, `:156`), do cabeçalho e da linha de total.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede o Template Method: quatro relatórios com os mesmos passos (título, cabeçalho, linhas,
total), mais dois a caminho, e "quando o jeito dos relatórios mudar, quero que mude em todos de uma vez". O
`ReportService` tem quatro métodos `generate*Report` que repetem a sequência inteira à mão. Cada um monta o próprio
`StringBuilder`, escreve "Pousada Maré Alta,<nome>,<período>" (`service/ReportService.java:26`, `:57`, `:97`, `:156`),
o cabeçalho (`:27`, `:58`, `:98`, `:157`) e a linha `total` (`:46`, `:85`, `:120`, `:174`). Não há classe abstrata,
método-modelo nem um escritor genérico que receba as partes que variam. Para acrescentar a linha com a data, pedida
para o ano que vem, alguém teria de editar os quatro métodos, e depois os do relatório de cancelamentos e o de
hóspedes por estado. É a definição de omissão da referência. A taxa de R$ 4,50, que a prefeitura muda todo ano, é uma
variável local dentro do método (`service/ReportService.java:94`).

**O exagero.** Não há interface com uma implementação só, fábrica nem camada que só repassa: contenção.

**O resto do código.** Os defeitos estão espalhados:

- **Texto solto no lugar de tipos.** A situação, o plano e o tipo do quarto são `String` em todo lugar
  (`model/Reservation.java`, `service/ReservationService.java:57`, `:80`, `:88-89`, `service/ReportService.java:63`, `:68`,
  `:104`). O plano nunca é validado: um `ratePlan` qualquer é gravado sem desconto, e no relatório por plano
  `countByPlan.get(plan)` devolve `null` e dá `NullPointerException` (`service/ReportService.java:145`).
- **Estado sem controle.** Todos os modelos têm campos públicos e mutáveis (`model/Reservation.java:7-22`,
  `model/Document.java`, `model/Room.java`, `model/Message.java`), e a conexão é `static` mutável (`db/Database.java:8-10`).
- **Duplicação.**
  - A mesma `ErrorResponse` aparece quatro vezes (`Application.java:38`, `routes/MessageRoutes.java:33`,
    `routes/ReportRoutes.java:82`, `routes/ReservationRoutes.java:201`).
  - O comprovante vira JSON em dois trechos copiados (`routes/ReservationRoutes.java:123-135` e `:154-166`).
  - O `Document` é montado à mão em dois lugares (`service/ReservationService.java:67-69` e `:132-133`).
  - O filtro "deixou dinheiro com a pousada" está copiado no financeiro e no por plano (`service/ReportService.java:63-68`
    e `:139-143`).
  - Os quatro handlers de relatório são iguais (`routes/ReportRoutes.java:14-80`).
- **Erros engolidos.** Todo handler termina em `catch (Exception e)` → 400 `INVALID_REQUEST` (`routes/ReservationRoutes.java:52`,
  `:64`, `:80`, `:96`, `:112`, `:144`, `:175`, `routes/ReportRoutes.java:26`, `:43`, `:60`, `:77`). Uma falha do banco ou um
  `NullPointerException` chega ao site como "pedido inválido", com a mensagem interna. Em `routes/MessageRoutes.java:28`
  o erro sai como 400 com o código `ERROR`.
- **O dinheiro sai do `BigDecimal` para `double`** na resposta (`routes/ReservationRoutes.java:192-197`,
  `routes/RoomRoutes.java:22`). Assim se perde o "2 casas" que o texto pede (779.80 sai como 779.8).
- **Um código de erro errado.** `guests < 1` dá `CAPACITY_EXCEEDED` (`service/ReservationService.java:24`), mas o texto
  pede 400 `INVALID_REQUEST`.

O cálculo do preço é o ponto bom: `BigDecimal` com `HALF_EVEN` em cada passo (`service/PricingService.java:16-17`,
`:29`, `:35`, `:42`).

## Testes

**Números:** 7 testes rodaram, 0 falharam, 0 pulados; 7 métodos de teste escritos, todos em `PricingServiceTest`. Cobertura de 26 de 706 linhas (3,7%) e 16 de 152 ramos (10,5%). Nenhum teste pela API, nenhum contra o banco (`measures/tests.md`).
**Nota:** ruim

A coluna está quase ausente. O único arquivo, `src/test/java/com/pousadamarealta/service/PricingServiceTest.java`,
confere com valores certos só o preço:

- a temporada (`:11-29`);
- a noite com 20% a mais (`:40-41`, 299,88);
- o exemplo do texto (`:44-54`, 599,76);
- o desconto (`:65-66`, 89,96) e o total (`:75`, 509,80).

Ficam sem nenhum teste:

- a parte nova do cenário: os quatro relatórios, a linha de título, a taxa de turismo (R$ 4,50 × hóspedes × noites)
  e o relatório por plano. Trocar 4.50 por 5.40 em `service/ReportService.java:94`, apagar a linha de título ou mudar o
  filtro de `:63-68` não derruba nenhum teste;
- as situações da reserva e as recusas (os códigos de erro);
- o cancelamento e a devolução;
- o comprovante e o e-mail;
- a disponibilidade do quarto;
- o banco e a API.

Nenhum teste sobe o serviço. Por isso o `mvn verify` passa com um sistema que não abre a porta (a seção do banco
mostra a falha).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária e o dinheiro em `numeric(10,2)`. As conferências acusaram: a chave estrangeira `reservations(room_number)` sem índice; nenhum CHECK em `status`, `rate_plan`, `type` e `channel`; nenhuma restrição UNIQUE/EXCLUDE; 0 transações explícitas; 0 `FOR UPDATE`; nenhum pool, só `DriverManager`. E o principal: **o serviço não subiu contra um banco vazio** (`measures/database.md`).
**Nota:** ruim

- **O serviço não sobe.** Os `CREATE TABLE` rodam, porque cada comando se confirma sozinho, mas o `conn.commit()` logo
  depois (`db/Database.java:74`) lança `PSQLException: Cannot commit when autoCommit is enabled`, e o `main` aborta. O
  mesmo erro está em `seedRooms` (`db/Database.java:91`), que nunca chega a rodar: as tabelas ficam vazias, sem os
  cinco quartos. Como o `CREATE TABLE IF NOT EXISTS` é seguido do mesmo `commit`, o serviço também não sobe de novo
  sobre o banco já criado.
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`db/Database.java:33-71`), sem
  migrações, e não sabe acrescentar uma coluna numa tabela que já existe.
- **Nada protege a regra principal.** Contra duas reservas na mesma noite há só um `SELECT` antes do `INSERT`, em
  conexões separadas (`repository/ReservationRepository.java:88-99`, chamado em
  `service/ReservationService.java:34`): dois pedidos ao mesmo tempo passam os dois.
- **Escritas que deviam andar juntas vão separadas.** Na confirmação, a situação, o valor pago e o e-mail são três
  escritas em três conexões, sem transação (`service/ReservationService.java:61-62`, `:72`). No cancelamento são duas
  (`:84`, `:96`). Uma falha no meio deixa uma reserva confirmada com o valor pago zero, ou confirmada sem o e-mail.
- **Cada chamada abre uma conexão nova**, sem pool (`db/Database.java:95-96`).

O SQL usa parâmetros em todo lugar (`repository/*.java`), e as datas estão em `date`.

## Comportamento

**Números:** nenhuma conferência rodou. O roteiro `check_09.sh` não conseguiu testar: o serviço não subiu contra um banco vazio (`measures/behavior.md`, mesmo erro em `db/Database.java:74`).
**Nota:** ruim

Para o dono da pousada, o sistema entregue não funciona: o `java -jar` termina com erro antes de abrir a porta 7070, e
o site, a recepção e o contador não têm a quem chamar. Não há quartos, reservas, comprovantes, e-mails nem relatórios.

Lendo o código, aparecem outros erros que o roteiro pegaria se o serviço subisse. Pela regra da skill, não mudam a nota:

- **Ocupação:** o relatório conta reservas pendentes, porque o filtro é só `status != 'CANCELLED'`
  (`repository/ReservationRepository.java:121`). O texto pede só as pagas.
- **Cancelamento:** ao cancelar uma reserva NON_REFUNDABLE, ou uma que ainda estava pendente, o `refund` continua
  `null` e não vira 0, porque só se grava a devolução quando ela é maior que zero
  (`service/ReservationService.java:95-97`).
- **Financeiro e por plano:** uma reserva STANDARD confirmada e depois cancelada (pago menos devolvido = 0) entra no
  financeiro com 0.00 e é contada no relatório por plano (`service/ReportService.java:63-68`, `:139-145`). O texto pede
  só as que "deixaram dinheiro com a pousada".
- **Hóspedes:** com 0 hóspedes, a resposta é 422 `CAPACITY_EXCEEDED`, e o texto pede 400 `INVALID_REQUEST`
  (`service/ReservationService.java:24`).
- **Dinheiro no JSON:** os valores saem como `double` e perdem as 2 casas (`routes/ReservationRoutes.java:192-197`).

## Dúvidas

- Se o serviço subisse, não sei se o `app.error(404, ...)` de `Application.java:35` sobrescreveria o corpo dos 404
  que os handlers já escrevem (`ROOM_NOT_FOUND` e `RESERVATION_NOT_FOUND`) com `NOT_FOUND`. O serviço não sobe, e não
  deu para conferir pela API.
- Os erros de comportamento listados acima vêm só da leitura. O roteiro não chegou a medi-los.
