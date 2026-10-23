# Revisão: construção, cenário 05

## Visão geral

O agente montou um serviço Javalin em seis pacotes: `web` (rotas, validação do corpo e montagem do JSON), `service`
(os casos de uso e os relatórios), `domain` (reserva, preço, planos, quartos), `document` (os três documentos),
`persistence` (JDBC puro, com o esquema criado no código) e o pacote raiz com `App` e `ApiException`. Uma requisição
entra em `web/Routes.java`, o `RequestParser` a transforma num `NewReservation`, o `ReservationService` abre uma
transação (`Database.tx`), aplica a regra no `Reservation` (record imutável com as transições) e grava pelos
repositórios; os dados ficam em duas tabelas no PostgreSQL (`reservations` e `messages`), e os quartos ficam fixos no
código (`domain/Rooms.java:8`). O documento é escolhido num lugar só (`Document.issue`), e os três tipos são
subclasses de `DocumentType`. As 83 conferências do comportamento passaram. Nada é grave. Os pontos mais fracos são
três: os testes não chegam ao serviço, ao banco nem à API (27,6% das linhas); o esquema é criado com
`CREATE TABLE IF NOT EXISTS`, sem migrações e sem nenhuma restrição; e há um ciclo entre pacotes que passa pelo pacote
raiz, por causa de `ApiException`.

## Arquitetura

**Números:** 6 pacotes, 15 dependências entre eles, com ciclo: o `tsort` acha 4 ciclos, todos passando pelo pacote
raiz `pousada` (`pousada -> web -> pousada`, `pousada -> service -> persistence -> domain -> pousada`, etc.). Javalin
só em `pousada` (o `App`) e `pousada.web`; JDBC em `persistence` e em `service`.
**Nota:** com problemas

A borda HTTP é fina. Cada rota de `web/Routes.java:32-51` lê a requisição, chama um método do serviço e escreve a
resposta. A validação fica em `web/RequestParser.java` e o JSON em `web/Views.java`. As regras não conhecem o Javalin
nem o SQL: o preço está em `domain/Pricing.java:21-29`, as transições em `domain/Reservation.java:33-65` e a
devolução em `domain/RatePlan.java:21-23`. Todo o SQL fica nos repositórios de `persistence`. A montagem acontece num
lugar só, em `App.java:14-19`. Os defeitos são localizados:

- **O ciclo:** ele vem de `ApiException`, que fica no pacote raiz ao lado do `App` (`ApiException.java:1`). O `App`
  depende de `web`, `service` e `persistence` (`App.java:4-7`), e todos os pacotes dependem de volta do raiz para
  lançar o erro. Além disso, a exceção carrega o status HTTP (`ApiException.java:5-10`), e o domínio a lança direto:
  `domain/Reservation.java:6,61` e `document/DocumentTypes.java:8,42`. Com isso, a regra fica presa ao código de
  resposta da API. A correção é barata: tirar a exceção do pacote raiz (ou criar exceções de domínio e mapear o
  status em `web`).
- **O JDBC no serviço:** o serviço conhece o JDBC. `service/ReservationService.java:3-4,88,94` recebe e passa um
  `java.sql.Connection`, porque a transação é aberta ali. Não há SQL no serviço, mas o tipo do JDBC atravessa a camada.
- **Os repositórios criados por dentro:** os serviços criam os seus repositórios com `new`
  (`service/ReservationService.java:22-23`, `service/ReportService.java:16`), em vez de recebê-los na montagem.

Nenhuma classe destoa no tamanho: a maior tem 168 linhas (`persistence/ReservationRepository.java`).

## Design de baixo nível

**Números:** 3 tipos de documento (CPF, CNPJ, passaporte), os 3 dentro da estrutura e nenhum fora dela. Os 3 lugares
que emitem o documento (e-mail de confirmação, check-out, segunda via) pedem todos à mesma fábrica, e nenhum `if` ou
`switch` sobre o tipo de hóspede aparece fora do pacote `document`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A criação está concentrada em `document/Document.java:10-15` (`Document.issue`). A escolha do tipo é
uma busca pelo código guardado na reserva (`document/DocumentTypes.java:28-31`), sem condicional por caso. Os três
lugares chamam essa fábrica: a confirmação em `service/ReservationService.java:58`, o check-out em
`service/ReservationService.java:72` e a segunda via em `service/ReservationService.java:81`. Cada tipo é uma
subclasse de `DocumentType`, e é ela que muda o que varia:

- os campos de identificação, em `document/CompanyInvoice.java:10`;
- o título e o idioma, em `document/PassportReceipt.java:10-11`;
- as linhas de identificação, em `document/CpfReceipt.java:14-16`;
- as linhas do fim, como o ISS em `document/CompanyInvoice.java:21-24`.

O corpo comum é escrito uma vez só (`document/DocumentType.java:32-47`). A mesma lista também decide a validação da
identificação (`document/DocumentTypes.java:37-54`) e os campos que o JSON mostra (`web/Views.java:76-80`,
`web/RequestParser.java:35-37`). Assim, o documento da agência de turismo entra com uma subclasse nova e uma linha em
`document/DocumentTypes.java:13-16`, sem mexer no serviço, nas rotas nem no e-mail. O e-mail continua em português e
só embute `document.text()` (`service/ConfirmationEmail.java:13-19`). O que afasta da forma ideal é pouco:

- `Document.issue` busca o quarto num catálogo estático (`document/Document.java:11`);
- os campos de identificação são chaves de texto num `Map<String, String>` (`domain/Identity.java:6`), repetidas
  entre `fields()` e `identityLines` (`document/CompanyInvoice.java:10,17`).

**O exagero.** Não há estrutura sobrando. A `Language` (`document/Language.java:11-15`) tem dois valores, e os dois
são usados (PT_BR por dois tipos, EN pelo booking receipt). `DocumentData` e `Document` são records simples. Não há
interface com uma implementação só, nem fábrica de fábrica.

**O resto do código.** O código está limpo:

- o dinheiro é `BigDecimal`, arredondado a cada passo com `HALF_EVEN` (`domain/Money.java:13-15`, usado em
  `domain/Pricing.java:24,28`);
- as datas são `LocalDate`, e o status, o plano e o canal são `enum`s;
- a reserva é um record imutável, e as transições ficam centralizadas num método (`domain/Reservation.java:58-65`);
- os erros não são engolidos: o `catch (Exception)` final registra a causa (`web/Routes.java:55-58`).

Ficam detalhes menores:

- o construtor de 16 argumentos da reserva aparece repetido três vezes (`domain/Reservation.java:29,63`,
  `service/ReservationService.java:38`);
- a identidade é tipada como texto solto (`domain/Identity.java:6`);
- um id não numérico vira 404 (`web/Routes.java:70`), uma escolha defensável.

Nada disso faria um revisor exigente recusar o código.

## Testes

**Números:** 12 testes rodaram, 0 falharam, 0 pulados; 12 métodos escritos em 3 classes. Cobertura de linhas: 142 de
515 (27,6%); de ramos: 34 de 90 (37,8%). Por pacote: `document` 79/88 linhas e 14/14 ramos, `domain` 48/57 linhas e
20/22 ramos, e 0% em `persistence`, `service` e `web`. Nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que existe é bom e confere com valores:

- **os três documentos, comparados com o texto inteiro:** o comprovante igual ao do enunciado
  (`src/test/java/pousada/DocumentTest.java:21-32`); a fatura com o ISS de R$ 38,99 (`DocumentTest.java:34-47`); o
  booking receipt com "1 night", a linha Discount e `BRL 1,234.56` (`DocumentTest.java:49-62`);
- **a identificação:** a recusa de nenhuma, de duas e de uma incompleta (`DocumentTest.java:70-80`);
- **o preço:** a temporada nas bordas (`PricingTest.java:101-109`), o 599,76 do enunciado (`PricingTest.java:93-99`),
  o desconto de 15% (`PricingTest.java:111-117`) e o meio para o par (`PricingTest.java:119-124`);
- **as transições e a devolução:** `ReservationTest.java:151-171`.

Na pergunta da mutação, trocar `"0.05"` por `"0.5"` em `document/CompanyInvoice.java:23`, apagar o `if` do desconto em
`document/DocumentType.java:41` ou trocar `"1.20"` em `domain/Pricing.java:18` faz algum teste falhar.

O que ficou sem rede é tudo o que não é domínio puro:

- **`Document.issue` e os três lugares que o chamam:** se a confirmação passasse a mandar sempre o comprovante de
  CPF, nenhum teste notaria.
- **O e-mail de confirmação:** `service/ConfirmationEmail.java` não tem teste.
- **A sobreposição de noites:** inverter `<` por `<=` em `persistence/ReservationRepository.java:40` passaria em
  todos os testes.
- **Os dois relatórios em CSV:** `service/ReportService.java` não tem teste.
- **Os códigos HTTP e o mapeamento das exceções:** `web/Routes.java:53-58` não é testado.
- **A gravação e a leitura da identidade em JSON:** `persistence/ReservationRepository.java:153-167` não é testada.

O enunciado não pedia testes, mas, do jeito que estão, cobrem pouco mais de um quarto do código.

## Banco

**Números:** 2 tabelas (`reservations` com 17 colunas, `messages` com 5), as duas com chave primária. O dinheiro está
todo em `numeric(12,2)` e as datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. As 4 colunas de
situação e de tipo são `text` sem CHECK. O esquema é criado por `CREATE TABLE` no código, sem arquivo de migração. Não
há pool, e há 1 `DriverManager.getConnection`. Os 3 trechos de SQL montados com `+` só concatenam constantes.
**Nota:** com problemas

Os dados são guardados certos:

- o dinheiro e as datas têm os tipos certos, e o obrigatório é `NOT NULL` (`persistence/Database.java:49-66`);
- todo SQL usa parâmetros: os trechos com `+` (`persistence/ReservationRepository.java:103-104,110-111,119`) só juntam
  texto fixo e a lista de status vinda do `enum` (`ReservationRepository.java:24-25`);
- as conexões são fechadas por try-with-resources (`persistence/Database.java:26`);
- cada operação roda numa transação, com rollback se falhar (`persistence/Database.java:25-39`), o que mantém juntas
  a mudança da reserva e a gravação do e-mail (`service/ReservationService.java:56-60`).

A reserva dupla é evitada por uma trava do banco por quarto, `pg_advisory_xact_lock(2, room)`
(`persistence/ReservationRepository.java:28-33`), seguida da consulta de sobreposição na mesma transação
(`ReservationRepository.java:36-49`). A trava vale entre processos, não só dentro da JVM. As mudanças de situação usam
`FOR UPDATE` (`ReservationRepository.java:86-88`).

Os defeitos:

- **O esquema não evolui:** ele nasce de `CREATE TABLE IF NOT EXISTS` dentro do Java (`persistence/Database.java:48-73`),
  sem migrações. O próximo pedido que acrescentar uma coluna não terá como mudar um banco que já tem dados.
- **O banco não protege nenhuma regra por conta própria:** não há `EXCLUDE` contra sobreposição de noites, nem CHECK
  em `status`, `rate_plan`, `identity_type` e `guests`. A regra só vale se todo código passar pela trava.
- **Não há tabela de quartos:** sem ela, `room_number` não tem chave estrangeira (os quartos ficam em
  `domain/Rooms.java:8-13`).
- **A identificação é um JSON em texto:** ela fica numa coluna `TEXT` com JSON (`identity_data`,
  `persistence/Database.java:55`), opaca para consultas e sem tipo.
- **Não há pool:** cada requisição abre uma conexão nova por `DriverManager` (`persistence/Database.java:26`).

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 05: 32 de 32;
- duas reservas ao mesmo tempo: 2 de 2 (em 10 rodadas, sempre uma criada e a outra recusada com 409).

**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API:

- os preços com a temporada e o arredondamento (599,76; desconto de 37,48);
- as recusas com os códigos certos;
- a confirmação com o e-mail, o check-in, o check-out e os cancelamentos com as devoluções certas;
- os dois relatórios, inclusive a reserva não reembolsável cancelada que continua contando;
- os dados, que continuam lá depois de reiniciar.

No cenário, a pessoa com CPF recebe o comprovante do enunciado, a empresa recebe a fatura com o ISS de 5%, e o
estrangeiro recebe o booking receipt em inglês, com `BRL 2,783.52`, datas ISO e "1 night". O documento é o mesmo no
e-mail (que continua em português), no check-out e na segunda via. A identificação ausente, dupla ou incompleta é
recusada com 400. Duas reservas simultâneas do mesmo quarto nunca passaram juntas.

## Dúvidas

- **A forma do pattern:** é uma fábrica concentrada (`Document.issue`, que escolhe o `DocumentType` pelo código salvo
  na reserva), com subclasses polimórficas que fazem a parte variável do texto, num esqueleto comum
  (`document/DocumentType.java:32-47`). Não é a forma clássica de um criador com o método de fábrica sobrescrito. A
  referência aceita as duas como acerto.
- **O título no JSON:** o `title` do documento muda com o tipo ("Fatura de hospedagem", "Booking receipt"). O texto só
  dá o exemplo "Comprovante de reserva", e a escolha do agente parece razoável. O roteiro não marcou nada.
- **A nota da arquitetura:** o ciclo é real pelo `jdeps`, mas vem só de uma classe (`ApiException`) no lugar errado.
  Entre `com problemas` e `bom`, fiquei com a primeira, porque o domínio também lança essa exceção, que carrega o
  status HTTP.
