# Revisão: construção, cenário 05

## Visão geral

O agente fez o projeto num pacote só (`br.com.maralta.pousada`, 32 classes, 1230 linhas), mas cada camada tem as suas próprias classes. `App` monta as peças (`App.java:9-11`). `Api` só lê a requisição, chama o serviço e escreve a resposta (`Api.java:27-48`). `ReservationService` cuida das transições e da transação (`ReservationService.java:75-82`). `ReservationRepository` e `MessageRepository` concentram o SQL, e `Database` traz o pool Hikari, a transação e o `CREATE TABLE IF NOT EXISTS` (`Database.java:29-63`). Os quartos ficam no código, numa lista estática (`Rooms.java:8-13`), e as reservas e as mensagens ficam no PostgreSQL. A identificação do hóspede é polimórfica: há uma interface `Identification` com três registros (CPF, CNPJ, passaporte), e cada registro dá ao `Document` o título, o idioma, as linhas de quem se hospeda e o rodapé. O documento nasce num lugar só, `Document.of` (`Document.java:24`), e os três pontos de uso chamam esse método. O roteiro passou inteiro (83 de 83). Não há defeito grave. Os pontos fracos são estes:
- o esquema é criado no código, sem migrações, sem chave estrangeira para o quarto e sem CHECK;
- as regras levam o código HTTP dentro da exceção que lançam (`ApiException`);
- os testes deixam sem conferir partes das regras: a fronteira da temporada, os códigos de erro e o documento do CNPJ e do passaporte pela API.

## Arquitetura

**Números:** 1 pacote (32 classes, 1230 linhas), nenhuma dependência entre pacotes, nenhum ciclo. Esse pacote usa Javalin, JDBC, Jackson e o pool. As maiores classes são `ReservationRepository` (148 linhas), `Api` (113) e `ReservationService` (101).
**Nota:** com problemas

O que está bom:
- **A borda HTTP é fina.** Cada rota é uma linha que chama o serviço (`Api.java:29-48`). A validação do corpo fica em `ReservationRequest.parse` (`ReservationRequest.java:22-43`), e a dos parâmetros de consulta em funções pequenas (`Api.java:56-78`).
- **O SQL fica todo nos repositórios** (`ReservationRepository.java`, `MessageRepository.java`) e no `Database`.
- **As regras de preço e de situação não conhecem o Javalin nem o SQL.** Elas moram em `Pricing.java:27-36`, `RatePlan.java:17-23` e `Reservation.java:24-42`.
- **A montagem fica no `main`** (`App.java:9-11`).

O que pede correção:
- **Não há fronteira entre pacotes.** Tudo está num pacote só, então nada impede que uma classe de regra passe a usar o Javalin ou o JDBC. Hoje a separação depende só de disciplina.
- **A exceção das regras carrega o código HTTP.** `Reservation` lança `ApiException` com 409 (`Reservation.java:51` e `:61`, que usam `ApiException.java:37-38`). `Rooms.get` lança com 404 (`Rooms.java:23`) e `Identifications.from` com 400 (`Identifications.java:44`).
- **O serviço conhece o JDBC.** `ReservationService` recebe `java.sql.Connection` e trata `SQLException` (`ReservationService.java:3-4`, `:70-72`, `:84`). Ele não monta SQL, mas está preso à forma da transação.
- **A montagem não é toda no `main`.** O serviço e os relatórios criam os seus repositórios por dentro (`ReservationService.java:12-13`, `Reports.java:10`). Os quartos são um `static` global (`Rooms.java:8`).
- **A forma do JSON está espalhada.** Uma parte fica na borda (`Api.java:84-112`), outra no domínio (`Document.java:52-68`, `Message.java:7-15`).

Tudo isso é localizado e não há ciclo. Por isso a nota é "com problemas", e não "ruim", mesmo com um pacote só (ver Dúvidas).

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt). Os 3 vêm da mesma estrutura, e nenhum é decidido fora dela. Há 1 ponto de criação (`Document.of`) e 3 pontos de uso. O código não tem nenhum `if`, `switch` ou `instanceof` sobre o tipo de hóspede.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A escolha do documento é polimórfica, no próprio tipo de hóspede:
- `Identification` declara título, idioma, linhas de identificação e rodapé (`Identification.java:11-28`).
- `CpfIdentification`, `CnpjIdentification` e `PassportIdentification` implementam a interface. A fatura acrescenta o ISS no rodapé (`CnpjIdentification.java:33-37`). O booking receipt usa `Language.EN`, com `BRL 1,234.56` e datas ISO (`PassportIdentification.java:17-19`, `Language.java:30-31`).
- O documento é criado num lugar só, `Document.of` (`Document.java:24-29`). Os três pontos de uso chamam esse método:
  - o e-mail de confirmação (`ReservationService.java:97`);
  - o check-out (`ReservationService.java:56`);
  - a segunda via (`ReservationService.java:62`).
- A identificação nasce de um registro (`Identifications.java:15-16`) que escolhe o tipo pelos campos informados e monta o objeto com a função de fábrica de cada `Kind` (`Identifications.java:36-55`).
- Para a agência de turismo bastam uma classe nova e uma linha em `KINDS`, sem tocar no serviço.

O que afasta um pouco da forma clássica:
- O `Document` é um registro único que monta o texto como um molde (`Document.java:35-50`). Os pedaços vêm da identificação.
- Uma agência com documento de forma muito diferente teria de caber nesse molde: título, linhas de identificação, quarto, datas, valores, rodapé.

**O exagero.** Não há estrutura sem necessidade:
- `Language` (`Language.java:11-47`) atende dois idiomas de verdade.
- O registro `Kind` serve ao parse e à saída dos cinco campos com `null` (`Identifications.java:22-33`).
- O único excesso é o campo `name` de `Kind` (`Identifications.java:12`, por exemplo `CpfIdentification.java:7`), que nada lê. É pouco para contar como exagero.

**O resto do código.** Os tipos são os certos:
- `BigDecimal` com `HALF_EVEN` em cada passo (`Money.java:13-15`, `Pricing.java:24` e `:31`, `RatePlan.java:18`);
- `LocalDate` para as datas e `enum` para a situação, o plano, o tipo de quarto e o canal (`Status.java`, `RatePlan.java`, `RoomType.java`, `Channel.java`);
- as transições, com as suas guardas, num lugar só (`Reservation.java:24-62`).

Os métodos são curtos. Os defeitos são pequenos:
- O construtor de 16 argumentos posicionais se repete em quatro lugares (`ReservationService.java:25-28`, `ReservationRepository.java:131-137`, `ReservationRepository.java:144-146`, `Reservation.java:65-66`). A reserva nasce com `id` 0 e é recriada com o `id` gerado (`ReservationRepository.java:52`, `:143`). Isso é frágil a uma troca de ordem de argumentos.
- A identificação vira `Map<String,String>` em JSON dentro de uma coluna de texto (`ReservationRepository.java:110` e `:129-132`). O código fica simples, mas o tipo se perde no banco (ver Banco).

Nada disso impede aceitar o código.

## Testes

**Números:** 7 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 7 métodos escritos: 3 em `ApiTest`, ponta a ponta contra o banco, e 4 em `DocumentTest`, de unidade. Cobertura de linhas de 92,8% (437 de 471) e de ramos de 75,5% (77 de 102).
**Nota:** com problemas

O que é testado com valores:
- **Os três textos de documento, linha a linha.** São o comprovante (`DocumentTest.java:18-26`), a fatura com o ISS de R$ 38,99 (`DocumentTest.java:29-41`) e o booking receipt com desconto, milhar e "1 night" (`DocumentTest.java:44-55`). Trocar 0.05 por outro valor, ou o separador de milhar, derruba um teste.
- **O preço de julho com e sem desconto:** 599,76, 89,96 e 509,80 (`DocumentTest.java:58-66`).
- **O fluxo completo pela API, com total, pago, conflito de noites e ocupação** (`ApiTest.java:80-118`). O teste confere também o CSV da ocupação inteiro e o total do financeiro.
- **A NON_REFUNDABLE cancelada continua na receita** (`ApiTest.java:121-128`).
- **A recusa de duas identificações e de uma incompleta** (`ApiTest.java:133-136`).

O que fica sem rede (a pergunta da mutação):
- **A temporada só é testada em julho.** Trocar `>= 15` por `> 15` em dezembro (`Pricing.java:19`) não derruba nada. O mesmo vale para tirar fevereiro.
- **Os erros só conferem o status, nunca o campo `error`** (`ApiTest.java:131-143`). Trocar `ROOM_UNAVAILABLE` por outro código passaria.
- **A devolução do plano STANDARD e o cancelamento de reserva pendente nunca são conferidos.** Trocar `amountPaid` por `ZERO` em `RatePlan.java:22` passaria.
- **Pela API, só o documento do CPF é testado, e só pelo título** (`ApiTest.java:115`). Ficam sem conferir:
  - o JSON do documento, com os cinco campos com `null` e o campo `text`;
  - a fatura e o booking receipt no e-mail, no check-out e na segunda via.
- **A falta de identificação não tem teste.** Se `used.size() != 1` virasse `used.size() > 1` (`Identifications.java:43`), nenhum teste falharia.
- **O `ApiTest` se desliga sem falhar quando não há banco** (`ApiTest.java:32` e `:37`, com `Assumptions`). Nesse caso o `mvn verify` passa com só 4 testes de unidade. Aqui ele rodou.

## Banco

**Números:** 2 tabelas (`reservations` com 16 colunas, `messages` com 5), ambas com chave primária. Dinheiro em `numeric(12,2)` nas 5 colunas, datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE ou EXCLUDE. `status`, `rate_plan` e `channel` são `text` sem CHECK. O esquema é criado no código Java, sem migrações.
**Nota:** com problemas

O que está certo:
- **Os tipos:** `NUMERIC(12,2)` para dinheiro e `DATE` para datas (`Database.java:40-50`).
- **O SQL tem parâmetros em todas as consultas.** Os candidatos que o script listou concatenam só constantes: `COLUMNS` e `" FOR UPDATE"` (`ReservationRepository.java:19-20` e `:69`).
- **Conexões, pool e transações:** as conexões vêm de um pool Hikari com try-with-resources (`Database.java:16-25` e `:66`). Cada operação roda numa transação (`Database.java:65-79`), e a mudança de situação e o e-mail de confirmação são gravados juntos (`ReservationService.java:76-81`).
- **A dupla reserva é protegida no banco, não no processo.** A criação trava o quarto com `pg_advisory_xact_lock` antes de verificar e inserir (`ReservationRepository.java:23-28`, `ReservationService.java:29-35`), e as transições usam `FOR UPDATE` (`ReservationRepository.java:69`). O roteiro confirmou: 10 de 10 rodadas sem duplicar.

O que pede correção:
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`Database.java:33-59`), sem migrações versionadas. O próximo pedido que mudar uma coluna não terá como alterar um banco que já tem dados.
- **Não há tabela de quartos nem chave estrangeira da reserva para o quarto.** Os quartos só existem no código (`Rooms.java:8-13`), e `room_number` é um `INT` solto (`Database.java:36`).
- **A identificação fica em JSON dentro de uma coluna `TEXT`** (`Database.java:39`, `ReservationRepository.java:110`). Não é `jsonb` nem são colunas próprias. O banco não garante "um jeito e só um", e não dá para consultar reservas por CNPJ sem ler a tabela inteira.
- **Nenhuma restrição protege as regras no próprio banco:** não há CHECK em `status`, `rate_plan` ou `guests >= 1`, nem EXCLUDE de sobreposição. A trava resolve a concorrência, mas um INSERT por fora do serviço passaria.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam. A parte comum passou 46 de 46, os dados depois de reiniciar 3 de 3, o cenário 05 (o documento por tipo de hóspede) 32 de 32, e as duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

Tudo o que o texto pede funciona pela API, a partir de um banco vazio:
- **Os preços:** a temporada e o "meio para o par" (desconto de 37,48).
- **As recusas, com os códigos certos.**
- **A confirmação com o e-mail, o check-in e o check-out.**
- **Os cancelamentos com a devolução de cada plano.**
- **Os dois relatórios CSV.**
- **No cenário:**
  - a fatura com o ISS de R$ 38,99;
  - o booking receipt em inglês, com `BRL 2,783.52`, datas ISO, "1 night" e a linha Discount;
  - o e-mail em português com o documento certo dentro;
  - os cinco campos com `null`;
  - a recusa de nenhuma, duas ou uma identificação incompleta.

Para o dono da pousada, o sistema faz o que ele descreveu. Os dados sobrevivem a um reinício, e duas reservas simultâneas do mesmo quarto não passam.

## Dúvidas

- **A forma do pattern.** O agente usou "um método polimórfico no próprio tipo de hóspede", que a referência aceita. A identificação fornece as partes, e um único `Document` monta o texto como molde (`Document.java:35-50`). Não são classes de documento diferentes, cada uma criada por um método de fábrica. Isso basta para os três casos e para uma agência com o mesmo formato. Um documento de forma bem diferente pediria mudar o `Document`.
- **A nota da arquitetura.** A skill põe "tudo num pacote" entre os sinais de `ruim`. Aqui, porém, as camadas existem como classes separadas: rotas finas, SQL só nos repositórios, regras sem Javalin. Por isso dei `com problemas`. Um revisor mais literal poderia dar `ruim`.
