# Revisão: construção, cenário 05

## Visão geral

O projeto é um pacote só (`pousada`, 9 classes de código, 936 linhas) dividido por papel em classes: `App` é a borda
HTTP (Javalin), que lê e valida o JSON, chama o `Service` e monta as respostas em mapas; `Service` junta as regras da
reserva (situações, cancelamento e devolução, o e-mail de confirmação) e todo o SQL (JDBC puro, em transações abertas
por `Db.tx`); `Db` cria o pool (HikariCP) e o esquema com `CREATE TABLE IF NOT EXISTS` ao subir; `Pricing`, `Money` e
`Identification` são regras puras (preço, dinheiro, documento por tipo de hóspede). Uma requisição segue
`App` → `Service` → `Db`/JDBC → PostgreSQL. A montagem fica em `Main`. Os dados ficam em três tabelas (`rooms`,
`reservations`, `messages`). O pattern pedido (Factory Method) foi resolvido com um método polimórfico no tipo de
hóspede (`Identification.render`), chamado de um lugar só. O comportamento passou em todas as 83 conferências e os 13
testes passam com 92,8% de cobertura de linhas. O defeito mais visível está na arquitetura: o `Service` mistura regra
e SQL. O banco também tem pontos fracos: o esquema é criado no código, sem migrações, e não há restrições `CHECK` nem
de exclusão.

## Arquitetura

**Números:** 1 pacote (`pousada`, 23 classes compiladas, 936 linhas), 0 dependências entre pacotes, 0 ciclos; o mesmo pacote usa Javalin, JDBC, Jackson e o pool. As maiores classes: `Service` (302 linhas) e `App` (243).
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota só lê, chama o serviço e responde (`src/main/java/pousada/App.java:48-56`).
  `App` também valida a entrada (`App.java:95-133`) e faz a conversão para JSON (`App.java:177-242`). Nada de preço
  nem de situação da reserva mora nos handlers.
- **As regras puras estão isoladas.** `Pricing`, `Money` e `Identification` não conhecem nem Javalin nem JDBC.
- **A montagem fica num lugar só** (`src/main/java/pousada/Main.java:10-12`), com o `Db` injetado no `Service`
  (`Service.java:19-23`).
- **O problema: `Service` é ao mesmo tempo regra e acesso a dados.** As transições de situação
  (`src/main/java/pousada/Service.java:134`, `:157`, `:174`, `:183`) e a regra de devolução (`Service.java:160`)
  ficam lado a lado com `PreparedStatement`, `ResultSet` e SQL escrito à mão (`Service.java:73-127`, `:199-251`).
  O mapeamento posicional de 20 colunas também está ali (`Service.java:285-301`). Não existe uma classe de acesso a
  dados, e o próprio comentário admite isso (`Service.java:12`). O texto do e-mail é montado dentro da transação de
  confirmação (`Service.java:138-142`).
- **O serviço conhece os códigos HTTP.** Ele lança `ApiException` com 404, 409 e 422 (`Service.java:77`, `:82`,
  `:93`, `:290`).
- **Tudo está num pacote só,** então nada no compilador impede que a borda chame o SQL ou que a regra use o Javalin.

Não há ciclo, e as camadas existem em parte (a borda separada, as regras puras separadas). Isso tira o projeto do
`ruim`, mas a fusão regra+SQL num `Service` de 302 linhas é o lugar onde o próximo pedido vai doer.

## Design de baixo nível

**Números:** 3 tipos de documento (comprovante, fatura, booking receipt), os 3 dentro da estrutura e 0 fora dela; os 3 lugares onde o documento sai (e-mail de confirmação, check-out, segunda via) passam por um único método (`Service.document(Connection, Reservation)`); 1 ponto de escolha do tipo (`Identification.from`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `Identification` é uma interface com três implementações: `Individual`, `Company` e `Foreigner`
(`src/main/java/pousada/Identification.java:67`, `:78`, `:93`). Cada uma sabe emitir o próprio documento
(`render(Receipt)`) e o próprio título.

- **Os três lugares pedem o documento ao mesmo ponto.** `confirm` (`Service.java:137`), `checkOut`
  (`Service.java:185`) e a segunda via (`Service.java:193`) chamam o mesmo `document(c, r)`, que faz
  `r.identification().render(receipt)` (`Service.java:266-279`). Nenhum deles pergunta qual é o tipo de hóspede.
- **A escolha de qual tipo criar mora num lugar só,** `Identification.from` (`Identification.java:117-121`). Ele é
  usado na entrada (`App.java:132`) e na leitura do banco (`Service.java:293`).
- **O e-mail fica em português** e só embute o texto do documento (`Service.java:138-142`). O ISS de 5% está na
  própria fatura (`Identification.java:79`, `:88`).
- **Para uma agência de turismo,** basta uma nova implementação e uma linha em `from`. A persistência, porém, ainda
  pediria colunas novas.

É a forma "método polimórfico no próprio tipo de hóspede", que a referência aceita como acerto. Afastam um pouco da
forma ideal:

- os acessores com `null` por padrão para os cinco campos (`Identification.java:24-28`), que deixam a interface saber
  de todos os tipos;
- a validação "exatamente um jeito" feita à parte, com três booleanos no `App` (`App.java:125-130`). Esses booleanos
  servem de validação de entrada, não de escolha do documento.

**Exagero.** Não há estrutura sobrando. `Db.TxWork` (`Db.java:13-15`) é a interface funcional das transações, e
`Identification.Receipt` é um record de dados. Não há fábricas, builders nem camadas de repasse.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo
(`src/main/java/pousada/Money.java:12-14`, `Pricing.java:26-36`), `LocalDate` para as datas e `enum` para situação,
plano e tipo de quarto (`Domain.java:9-13`), com records imutáveis. Os defeitos que um revisor apontaria:

- **Situação como texto solto no SQL,** em vez do `enum`: `update(c, id, "status = 'CONFIRMED', amount_paid = total")`
  (`Service.java:135`, `:175`, `:184`) e `'CANCELLED'` (`Service.java:162`). `update` concatena um trecho de SQL
  recebido como `String` (`Service.java:259-263`). Hoje só entram constantes, mas é uma API frágil.
- **A regra "reserva paga" está duplicada:** `Reservation.isPaid()` (`Domain.java:24-26`) e
  `status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')` no relatório (`Service.java:206`).
- **A mesma consulta de quarto aparece três vezes** (`Service.java:43`, `:74`, `:269`), e há leitura posicional de 20
  colunas (`Service.java:292-298`).
- **Erros de banco viram `IllegalStateException`** genérica (`Service.java:30-36`), e a resposta cai num 500
  (`App.java:37-40`). `ApiException` tem campos públicos (`ApiException.java:4-5`).

São defeitos localizados, que não tornam o código difícil de mudar, mas um revisor exigente pediria para corrigi-los.

## Testes

**Números:** 13 testes rodaram, 0 falhas, 0 erros, 0 pulados (13 métodos escritos: 7 em `UnitTest`, 6 em `ApiTest`); cobertura de linhas 428 de 461 (92,8%), de ramos 125 de 166 (75,3%). São 1 arquivo pela API contra o banco real e 1 de unidade, sem mocks.
**Nota:** bom

Os testes conferem as regras com valores.

**Na unidade:**

- os limites da alta temporada (`src/test/java/pousada/UnitTest.java:15-22`);
- o exemplo do texto, 599,76 (`UnitTest.java:24-30`);
- o desconto com meio para o par, 58,485 → 58,48 (`UnitTest.java:32-38`);
- a formatação de dinheiro (`UnitTest.java:40-45`).

**A parte nova do cenário** tem os três documentos comparados linha a linha:

- o comprovante (`UnitTest.java:52-62`);
- a fatura com ISS de R$ 38,99 (`UnitTest.java:64-78`);
- o booking receipt com milhar, desconto e "1 night" (`UnitTest.java:80-93`).

**Pela API, contra o PostgreSQL de `DB_URL`, num schema próprio:**

- o ciclo de vida inteiro, com os 409 das transições e o texto do e-mail (`src/test/java/pousada/ApiTest.java:98-135`);
- fatura e booking receipt pela segunda via (`ApiTest.java:137-154`);
- as devoluções e o financeiro com a não reembolsável cancelada (`ApiTest.java:156-181`);
- as recusas, inclusive identificação incompleta e dupla (`ApiTest.java:183-207`);
- a ocupação comparada inteira (`ApiTest.java:209-222`).

**A pergunta da mutação:**

- Trocar o ISS de 0.05 ou o arredondamento quebra `faturaComIss`.
- Trocar "1 night"/" nights" quebra o teste do booking receipt.
- Aceitar duas identificações quebra `ApiTest.java:190`.

**Ficam sem rede:**

- "passaporte sem país" e "só a razão social" (só "CNPJ sem razão social" é testado, `ApiTest.java:189`);
- o plural "N nights";
- o e-mail de confirmação de empresa e de estrangeiro;
- o texto exato do documento no check-out da empresa.

**Uma fragilidade:** sem `DB_URL` ou sem banco, o `ApiTest` se desliga em silêncio com `assumeTrue`
(`ApiTest.java:39`, `:44`), e o `mvn verify` passaria só com os testes de unidade. Aqui ele rodou inteiro.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(12,2)` nas 6 colunas; datas em `date`; 1 chave estrangeira (`reservations.room_number`), com índice; 0 restrições CHECK/UNIQUE/EXCLUDE; 4 colunas de situação ou tipo em `text` sem CHECK; esquema criado com `CREATE TABLE` no código (0 migrações); 2 candidatos de SQL montado com `+`, ambos com constantes.
**Nota:** com problemas

O que está certo:

- **Os tipos** estão certos (`src/main/java/pousada/Db.java:77-113`): `NOT NULL` no que é obrigatório e a reserva
  ligada ao quarto por chave estrangeira com índice (`Db.java:86`, `:106`).
- **O SQL usa parâmetros.** Os dois candidatos do script só concatenam constantes: o `RES_COLS`, o sufixo
  `FOR UPDATE` (`Service.java:287`) e os trechos literais passados a `update` (`Service.java:135`, `:175`, `:184`).
- **Conexões e transações:** as conexões vêm do pool e voltam em try-with-resources. Cada operação roda numa
  transação com commit/rollback (`Db.java:58-70`), e a confirmação grava a reserva e a mensagem juntas
  (`Service.java:131-151`).
- **A reserva dupla fica protegida entre processos:** `SELECT ... FROM rooms WHERE number = ? FOR UPDATE`
  (`Service.java:74`) serializa as criações por quarto antes da conferência de sobreposição (`Service.java:85-96`).
- **A criação do esquema** é serializada com `pg_advisory_xact_lock` (`Db.java:35`).

Os defeitos:

- **O esquema vive num `CREATE TABLE IF NOT EXISTS` dentro do código** (`Db.java:77-113`), sem migrações
  versionadas. A primeira coluna nova numa tabela que já existe (o documento da agência vai pedir) não será
  aplicada a um banco que já roda.
- **Nenhuma restrição protege as regras no próprio banco:**
  - não há `EXCLUDE` contra sobreposição de datas;
  - não há `CHECK` em `status`, `rate_plan`, `type` e `channel`, nem em `check_out > check_in` ou `guests >= 1`;
  - nada garante "exatamente uma identificação" nas cinco colunas (`Db.java:89-93`).

  A regra depende só da trava no código.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam: parte comum 46 de 46, dados depois de reiniciar 3 de 3, cenário 05 (documento por tipo de hóspede) 32 de 32, duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

O sistema faz pela API tudo o que o roteiro confere:

- os preços com temporada e meio para o par (599,76; desconto de 37,48);
- todas as recusas com os códigos pedidos;
- o ciclo PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT, com e-mail e comprovante;
- as devoluções por plano;
- os dois relatórios em CSV, inclusive a não reembolsável cancelada no financeiro;
- os dados que continuam depois de reiniciar.

Na parte do cenário:

- a empresa recebe a fatura com ISS de R$ 38,99 no e-mail (em português), no check-out e na segunda via;
- o estrangeiro recebe o booking receipt com `BRL 2,783.52`, datas ISO, "1 night" e a linha Discount;
- os cinco campos de identificação vêm com `null` nos que não se aplicam;
- nenhuma, duas ou uma identificação incompleta dão 400.

Nas dez rodadas de reservas simultâneas, sempre uma foi criada e a outra recebeu 409. Para o dono da pousada: tudo o
que ele descreveu funciona como pedido.

## Dúvidas

- A forma do pattern foi o "método polimórfico no próprio tipo de hóspede" (`Identification.render`). Ela não é a
  forma clássica do Factory Method, com um criador sobrescrito. A referência aceita essa forma como acerto, por isso a
  classe.
- Na arquitetura, a nota fica entre `com problemas` e `ruim`. Pesou para `com problemas` que a borda HTTP e as regras
  puras (preço, dinheiro, documentos) estão separadas e sem ciclo. A fusão regra+SQL ficou restrita ao `Service`.
