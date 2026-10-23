# Revisão: construção, cenário 05

## Visão geral

O projeto tem cinco pacotes em camadas: `App` (cria o pool Hikari, faz os `CREATE TABLE` e registra as rotas), `handlers` (Javalin: lê o JSON com `JsonNode`, chama o serviço, escreve a resposta), `service` (`ReservationService` com as regras, `DocumentService` que escolhe e monta o documento, `UtilService` com datas, dinheiro e temporada), `repository` (JDBC puro com `PreparedStatement`) e `model` (classes de dados com campos públicos). Os dados ficam no PostgreSQL, em três tabelas criadas no código. A regra de preço e os três documentos estão certos por dentro (conferi à mão: a reserva gravada tem total 599,76, e os e-mails de confirmação trazem a fatura com "ISS retido (5%): R$ 38,99" e o booking receipt com "1 night" e "BRL 86.98"). O mais sério é que **a API não funciona**. O `pom.xml` não traz o `jackson-datatype-jsr310`, e o Jackson não sabe escrever `LocalDate`: toda resposta que leva uma reserva ou um documento falha (`InvalidDefinitionException: Java 8 date/time type java.time.LocalDate not supported`, no log do serviço). Na criação, o `catch (Exception)` de `ReservationHandler.java:75-76` troca essa falha por 400 INVALID_REQUEST depois de a reserva já ter sido gravada. Na confirmação, no check-in e no check-out a resposta é 500, também depois de a mudança ter sido gravada (e o e-mail, enviado). Fora isso, a alta temporada pega dezembro inteiro (`UtilService.java:18`), e a checagem de disponibilidade ignora as reservas pendentes (`ReservationRepository.java:131`). Com isso, o mesmo quarto pode ser reservado duas vezes para a mesma noite.

## Arquitetura

**Números:** 5 pacotes, 7 dependências entre eles, nenhum ciclo. Os pacotes `App` e `handlers` usam JDBC: no caso dos handlers, é só o `DataSource`, que eles recebem e repassam. O `service` não usa Javalin nem JDBC. A maior classe tem 194 linhas (`ReservationRepository` e `ReservationHandler`).
**Nota:** com problemas

As camadas existem e as dependências apontam para o lado certo (handlers → service → repository → model). As regras de reserva não conhecem o Javalin nem o SQL (`ReservationService.java:24-176`). Mas há mistura localizada:

- **A montagem fica espalhada.** O `App` passa o `DataSource` cru a cada handler (`App.java:23-26`), e cada handler cria os próprios repositórios e o serviço (`ReservationHandler.java:25-30`, `ReportHandler.java:20-23`, `RoomHandler.java:9`, `MessageHandler.java:9`). Por isso existem várias instâncias de cada repositório, e nada pode ser trocado num teste.
- **Os relatórios são regra dentro do handler.** O `ReportHandler` lê os repositórios direto, monta o CSV e calcula o dinheiro que fica com a pousada, pago menos devolvido (`ReportHandler.java:38-56`, `87-92`). O total de quartos está fixo como `"/5"` (`ReportHandler.java:56`).
- **Uma regra de documento também mora no handler.** Quem decide que a segunda via só existe para reserva paga é o handler (`ReservationHandler.java:168`). No check-out e na segunda via, o handler busca o quarto no repositório e chama a fábrica do documento (`ReservationHandler.java:147-149`, `173-175`). Na confirmação, quem faz isso é o serviço (`ReservationService.java:109-112`).
- `DocumentService` e `UtilService` são `static` (`DocumentService.java:10`, `UtilService.java:14-92`).

Não é grave para um sistema deste tamanho, mas o próximo relatório ou documento vai cair nos handlers.

## Design de baixo nível

**Números:** os 3 tipos de documento ficam numa fábrica só (`DocumentService.generate`, um `if/else` em `DocumentService.java:28-37`). Os 3 lugares que emitem o documento chamam essa fábrica: a confirmação (`ReservationService.java:112`), o check-out (`ReservationHandler.java:149`) e a segunda via (`ReservationHandler.java:175`). Nenhum lugar decide o documento por fora dela.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A escolha de qual documento criar fica num lugar só, e os três lugares pedem o documento a ele. A referência aceita essa forma, a fábrica concentrada, como acerto. Há três pontos que a afastam do ideal:

- **O tipo de hóspede não existe no código.** Ele é deduzido de qual campo não é null (`DocumentService.java:28,31,34`). As três formas de identificação são conferidas de novo, à parte, em `ReservationService.java:49-62`. Para a agência de turismo, alguém vai mexer na fábrica, na validação, no `Document` e no esquema.
- **A fábrica não tem `else`.** Uma reserva sem identificação sai com `title` e `text` nulos (`DocumentService.java:37-39`).
- **O documento é sempre a mesma classe `Document`,** com os cinco campos de identificação (`Document.java:6-23`). Só o texto e o título mudam. A escolha, porém, acontece dentro da fábrica, então isso não é o "malfeito" da referência.

**O exagero.** Não há interface, fábrica ou camada sem necessidade: contenção.

**O resto do código.** Os defeitos são localizados, mas um deles derrubou o sistema:

- **Erros:** `catch (Exception)` que esconde a causa e devolve 400 (`ReservationHandler.java:75-76`, `ReportHandler.java:60,107`). Foi isso que escondeu a falha de serialização. Os códigos de erro viajam como texto na mensagem de uma `IllegalArgumentException` e são comparados por string, com o mesmo bloco copiado em cada rota (`ReservationHandler.java:64-74`, `97-106`, `114-123`, `131-140`, `152-160`).
- **Tipos:** situação, plano e tipo de quarto são `String` soltas, sem `enum` (`Reservation.java:19,24`; `ReservationService.java:100,130,135`). O plano nem é validado: um `ratePlan` qualquer é aceito, não dá desconto e, cancelado, não devolve nada (`ReservationService.java:85,135`).
- **Estado:** todos os modelos têm campos públicos e mutáveis (`Reservation.java:7-26`, `Document.java:7-23`).
- **Uma regra errada:** `isHighSeason` calcula o dia e não o usa, e trata dezembro inteiro como alta temporada (`UtilService.java:16-18`).
- **Duplicação:** o comprovante e a fatura repetem as mesmas linhas (`DocumentService.java:46-54` e `67-75`), e `formatMoney` e `formatMoneyEnglish` são cópias (`UtilService.java:48-80`).
- **Métodos longos:** `createReservation` tem 12 parâmetros e 70 linhas (`ReservationService.java:24-94`).
- **Campo enviado como null:** `body.has(...)` trata um campo que veio como `null` no JSON como o texto `"null"` (`ReservationHandler.java:45-49`).

## Testes

**Números:** 17 testes rodaram, 0 falhas, 0 erros, 0 pulados; 17 métodos escritos. Cobertura de 128 de 667 linhas (19,2%) e de 37 de 204 ramos (18,1%). `handlers`, `repository` e `App` ficaram em 0%. Nenhum teste chama a API ou usa o banco.
**Nota:** ruim

- **Seis testes não executam o código do projeto.** Os 6 de `ReservationFlowTest` (um terço do total) só conferem variáveis locais do próprio teste. `ReservationFlowTest.java:40` testa uma expressão escrita no teste, e `:61`, `:70` e `:79` afirmam o valor que o próprio teste acabou de atribuir (`assertTrue(cnpj != null && companyName == null)`).
- **Os testes de verdade conferem pouco.** São os de `UtilServiceTest` e `DocumentServiceTest`, e só o formato do dinheiro, as datas e o arredondamento são conferidos com valor (`UtilServiceTest.java:24-57`). `DocumentServiceTest` usa `contains` e não confere os valores que o texto dá. O ISS só confere o rótulo `"ISS retido (5%)"` (`DocumentServiceTest.java:51`), não os R$ 38,99. O desconto só confere `"Desconto:"` (`:87`).
- **A pergunta da mutação:**
  - trocar `0.05` por `0.50` no ISS (`DocumentService.java:62`) passa;
  - trocar `0.15` por `0.51` no desconto (`ReservationService.java:85-86`) passa;
  - apagar a linha "Discount" do booking receipt (`DocumentService.java:93-95`) passa;
  - trocar os ramos da fábrica seria pego pelos `assertEquals` do título (`DocumentServiceTest.java:25,46,66`).
- **Regras sem nenhum teste:** as situações da reserva, o cancelamento e a devolução, os conflitos de quarto, os códigos de erro, os relatórios e a validação das identificações. O teste da temporada só confere 25 de dezembro (`UtilServiceTest.java:14`), por isso o erro de 1º a 14 de dezembro passou.
- **O que nenhum teste viu:** como nenhum teste passa pela API, ninguém notou que nenhuma resposta com reserva sai.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- a chave estrangeira `reservations.room_number` sem índice;
- nenhum CHECK sobre `status`, `rate_plan`, `type` e `channel`;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK que proteja uma regra;
- 0 transações e 0 `FOR UPDATE`;
- o esquema criado por `CREATE TABLE` no Java, sem migração.

O SQL é todo com parâmetros e há pool.
**Nota:** ruim

- **O que está certo:** os tipos, as chaves e o uso do JDBC estão corretos (try-with-resources e `PreparedStatement` em `ReservationRepository.java:29-56`).
- **O esquema não sabe evoluir.** Ele é criado com `CREATE TABLE IF NOT EXISTS` em `App.java:66-110`. Quando a agência de turismo trouxer uma coluna nova, um banco que já existe não vai recebê-la.
- **O defeito sério:** nada impede que um quarto seja reservado duas vezes para a mesma noite.
  - A consulta de disponibilidade só considera `CONFIRMED`, `CHECKED_IN` e `CHECKED_OUT` (`ReservationRepository.java:131`). O texto diz para contar todas as reservas que não foram canceladas, e as pendentes ficam de fora.
  - A confirmação não confere a disponibilidade de novo (`ReservationService.java:96-104`).
  - A checagem e o `INSERT` não ficam numa transação (`ReservationService.java:65-72` e `:91`).
  - Não há restrição no banco.
  - Conferi à mão: dois pedidos seguidos para o quarto 201 em 01/12/2027 gravaram as duas reservas como PENDING. As duas podiam ser confirmadas, o que dá overbooking, um dado errado.
- **Confirmações ao mesmo tempo:** a confirmação lê e depois atualiza sem condição sobre a situação (`ReservationRepository.java:99-110`). Duas confirmações simultâneas passam as duas e mandam dois e-mails.
- **A carga:** o relatório de ocupação faz uma consulta por quarto (`ReportHandler.java:44-45`). Não pesa com cinco quartos.

## Comportamento

**Números:** 22 de 83 conferências passaram (61 falharam):
- a parte comum: 16 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 05: 4 de 32;
- duas reservas ao mesmo tempo: 1 de 2.
**Nota:** ruim

**A falha que explica quase tudo.** Toda criação de reserva responde 400 INVALID_REQUEST, embora a reserva seja gravada. O motivo é o `LocalDate` que o Jackson não escreve, escondido pelo `catch (Exception)`. O resto falha em cascata:
- o site nunca recebe o `id`, e sem ele as conferências seguintes chamam `/reservations//...` e recebem 404;
- chamado à mão com o `id` certo, o `GET /reservations/1` dá 400;
- a confirmação, o check-in, o check-out e a segunda via dão 500, mas a mudança fica gravada e o e-mail sai.

**O que passou.** Os quartos, as recusas que acontecem antes de gravar (capacidade, quarto inexistente, datas, corpo inválido, as quatro recusas de identificação do cenário), os cabeçalhos dos relatórios e as mensagens depois de reiniciar.

**O que isso quer dizer para o dono da pousada.** O site não consegue criar uma reserva e mostrá-la ao hóspede. Cada nova tentativa grava outra reserva pendente. A recepção não recebe o comprovante no check-out, e a segunda via não sai. Pelo banco, os relatórios funcionam: chamados à mão, mostraram a reserva com check-out na ocupação (`total,1/5`) e no financeiro (599.76). O total do financeiro sai como `total,,,,599.76` e não `total,599.76`; o texto não fechava esse formato.

**A conferência de reservas simultâneas que passou não prova nada.** Os dois pedidos recebem 400, mas as duas reservas são gravadas, porque as pendentes não bloqueiam o quarto (ver Banco).

**Dois erros fora do roteiro**, que só não aparecem nele porque a criação já quebra antes:
- entre 1º e 14 de dezembro a noite sai 20% mais cara (no 201, em 01/12/2027, o subtotal foi 467,88 em vez de 389,90; `UtilService.java:18`);
- o mesmo quarto aceita duas reservas para a mesma noite (contado no Banco).

## Dúvidas

- **A forma do pattern** é uma fábrica concentrada num método `static` com `if/else` sobre qual campo de identificação veio preenchido, e não o Factory Method clássico nem um método no tipo de hóspede. Dei acerto porque a referência aceita a fábrica concentrada. Como o tipo de hóspede não é modelado e a validação repete a mesma distinção em outro lugar, há quem leia isso como acerto fraco.
- **Testes entre `com problemas` e `ruim`:** os 17 testes passam e alguns conferem valores (dinheiro, datas, arredondamento). Dei `ruim` porque um terço deles não executa o código e as regras centrais ficaram sem teste.
- **Banco:** o overbooking com reservas pendentes é um erro de regra que mora numa consulta SQL. Pesei na coluna do banco, que é onde a regra "um quarto, uma reserva por noite" deveria estar protegida, e não mudei a nota do comportamento por ele.
