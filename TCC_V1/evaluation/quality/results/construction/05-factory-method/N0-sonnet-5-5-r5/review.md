# Revisão: construção, cenário 05

## Visão geral

O projeto é um pacote só (`pousada`, 26 classes, 903 linhas), mas as camadas estão separadas por classe. `Main`
monta tudo (`src/main/java/pousada/Main.java:10-11`). `Api` é a borda Javalin: lê e valida o corpo, chama o
serviço e escreve o JSON (`Api.java:42-69`). `ReservationService` guarda as regras: situação da reserva, cancelamento,
devolução, e-mail. `Pricing` calcula o preço com `BigDecimal` e HALF_EVEN. `Reports` monta os dois CSV. `Store`
concentra o JDBC sobre um pool HikariCP. Os dados ficam em duas tabelas do PostgreSQL (`reservations` e `messages`),
criadas com `CREATE TABLE IF NOT EXISTS` em `Database.java:40-81`. Os quartos são uma lista fixa no código
(`Room.java:7-12`). A variação pedida (o documento por tipo de hóspede) é um método polimórfico no tipo selado
`Identification` (`Identification.java:9-79`), usado pelos três lugares que emitem o documento. O comportamento passa
em tudo (83 de 83). Não há nada sério. Os pontos fracos são estes: os testes cobrem pouco (19% das linhas, sem
testes da API nem do banco), o esquema não tem migrações nem restrições e as regras ainda tocam no JDBC e em
pedaços de SQL.

## Arquitetura

**Números:** 1 pacote (`pousada`, 26 classes, 903 linhas), 0 dependências entre pacotes, nenhum ciclo. O único pacote usa Javalin, JDBC, Jackson e o pool. Maiores classes: `Store` (183), `Api` (162), `ReservationService` (112).
**Nota:** com problemas

O que está bem:
- **A borda HTTP é fina.** Cada rota é uma linha que chama o serviço e devolve a resposta (`Api.java:44-60`), e
  os erros viram JSON num tratador só (`Api.java:62-67`). A validação do corpo fica na borda (`Api.java:79-130`),
  o que é razoável.
- **A montagem fica num lugar só** (`Main.java:10-11`), com as dependências passadas pelo construtor
  (`ReservationService.java:12`, `Reports.java:12`).
- **O acesso ao banco fica quase todo em `Store`.**

As camadas vazam em três pontos localizados:
- **As regras conhecem o JDBC.** `ReservationService` abre transação com lambdas que recebem um
  `java.sql.Connection` (`ReservationService.java:27-35`, `43-50`) e declara `throws java.sql.SQLException`
  (`ReservationService.java:93`).
- **`Reports` escreve SQL.** As cláusulas `WHERE status IN (...) AND check_in <= ? ...` e
  `... ORDER BY check_in, id` ficam em `Reports.java:17-19` e `33-36`, e vão para um `Store.query(Connection, String
  where, Object...)` genérico (`Store.java:124-125`). Isso abre a porta para SQL concatenado fora da camada de dados.
- **A exceção da regra carrega o código HTTP.** `ApiException(404, "ROOM_NOT_FOUND", ...)` e
  `ApiException(422, ...)` são lançadas pelo serviço (`ReservationService.java:21-24`, `30`, `102`).

Ter um pacote só não pesa num sistema deste tamanho. Esses vazamentos pesam: quem mudar o banco vai ter de mexer no
serviço e nos relatórios.

## Design de baixo nível

**Números:** os 3 tipos de documento estão na estrutura (`Identification.Cpf`, `Cnpj`, `Passport`) e nenhum fica
fora dela. Os 3 lugares que emitem o documento (o e-mail de confirmação, o check-out e a segunda via) pedem o
documento ao tipo de hóspede, e nenhum o monta à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É a forma "método polimórfico no próprio tipo de hóspede", que a referência aceita como acerto.
- `Identification` é uma interface selada com `title()` e `render(Reservation)` (`Identification.java:9-14`), e
  cada record decide o seu documento: o comprovante (`Identification.java:37-45`), a fatura com o ISS de 5%
  (`Identification.java:47-59`) e o booking receipt em inglês (`Identification.java:61-78`).
- O e-mail chama `r.identification().render(r)` (`ReservationService.java:108`).
- O check-out e a segunda via passam por `ReservationService.document` → `Document.of`, que chama `i.title()` e
  `i.render(r)` (`ReservationService.java:74-78`, `Document.java:25-30`).
- Nenhum `if` ou `switch` sobre o tipo de hóspede aparece nesses três lugares. A agência de turismo seria um record
  novo com o seu `render`.

O tipo de hóspede é decidido em dois pontos, mas os dois tratam de criar a identificação, não de escolher o
documento: a leitura do pedido (`Api.java:111-130`) e a leitura do banco (`Store.java:160-164`). Acrescentar a agência
ainda vai pedir mexer nesses dois pontos, no `permits` (`Identification.java:9`) e nas colunas. O comentário "basta
implementar esta interface" (`Identification.java:7`) promete mais do que a interface selada entrega.

**O exagero.** Não há estrutura sem necessidade. A única interface auxiliar é `Store.Work` (`Store.java:29-31`), um
tipo funcional para as transações que tem uso real. Não há fábricas, builders nem camadas que só repassam.

**O resto do código.**
- **Os tipos são os certos:** `BigDecimal` com HALF_EVEN arredondado a cada passo (`Money.java:15-17`,
  `Pricing.java:20-36`), `LocalDate`, `enum` para o tipo de quarto, o plano e a situação (`RoomType.java`,
  `RatePlan.java`, `Status.java`).
- **Os records são imutáveis**, e o único `static` é constante.
- **As transições** têm um método só (`ReservationService.java:84-91`), sem cópia.

Defeitos pequenos:
- `Store.map` deduz o tipo por uma cascata de nulos que cai em `Passport` se o CPF e o CNPJ forem nulos
  (`Store.java:162-164`).
- `Reservation.roomType()` faz `orElseThrow()` sem mensagem (`Reservation.java:24-26`).

Nada disso pede mudança antes de aceitar. O `Store.query` com SQL vindo de fora está contado na arquitetura.

## Testes

**Números:** 8 testes rodaram, 0 falhas, 0 erros, 0 pulados. Há 8 métodos de teste em 1 arquivo. Cobertura: 78 de 404 linhas (19,3%) e 21 de 146 ramos (14,4%). Nenhum teste passa pela API nem pelo banco.
**Nota:** com problemas

O que está testado, com valores exatos:
- o preço na alta temporada (`PricingAndDocumentTest.java:18-23`, 599,76);
- as bordas da temporada (`:26-31`, 14/12, 15/12, 29/02 e 01/03);
- o desconto com meio-para-o-par (`:34-40`);
- os formatos de dinheiro (`:43-46`);
- os três documentos, comparados com o texto inteiro: o comprovante (`:49-59`), a fatura com o ISS de R$ 38,99
  (`:62-74`) e o booking receipt com desconto e "1 night" (`:77-88`);
- o começo e o fim do e-mail (`:91-98`).

Pela pergunta da mutação, trocar 0.05 por outro valor no ISS, "night" por "nights" ou o formato `BRL` faria um teste
falhar.

Ficam sem rede:
- **A identificação "exatamente um jeito".** Nada testa `Api.java:117-129`: apagar a checagem `!= 1` passaria.
- **As transições de situação e as recusas 409** (`ReservationService.java:42-91`).
- **O cancelamento e a devolução** (`ReservationService.java:67`): trocar `STANDARD` por `NON_REFUNDABLE` passaria.
- **A disponibilidade do quarto** (`Store.java:56-67`): trocar `<` por `<=` passaria.
- **Os dois relatórios** (`Reports.java`).
- **Os códigos de erro da API e a persistência.**

O texto não pedia testes. O que existe é bom, mas só cobre o preço e o texto dos documentos.

## Banco

**Números:** 2 tabelas (`reservations` com 20 colunas, `messages` com 5), as duas com chave primária. O dinheiro está em `numeric(12,2)` nas 5 colunas e as datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. `status`, `rate_plan` e `channel` são `text` sem CHECK. O esquema é criado por `CREATE TABLE` no código, sem migrações.
**Nota:** com problemas

O que está certo:
- **Os tipos:** o dinheiro e as datas guardam o valor certo (`Database.java:56-66`), e as colunas obrigatórias são
  `NOT NULL`.
- **O SQL:** tem sempre parâmetros. As concatenações que o script lista juntam só constantes (`COLS`,
  `" FOR UPDATE"`, as cláusulas fixas de `Reports`).
- **As conexões:** saem do pool HikariCP com try-with-resources (`Store.java:33-47`), e cada operação roda numa
  transação.
- **A reserva dupla:** fica protegida entre processos por um `pg_advisory_xact_lock` por quarto dentro da transação
  (`Store.java:49-54`, chamado em `ReservationService.java:28`), e a transição lê a linha com `FOR UPDATE`
  (`Store.java:104`). O roteiro confirmou isso nas 10 rodadas simultâneas.

Os defeitos:
- **O esquema não evolui.** O `CREATE TABLE IF NOT EXISTS` (`Database.java:45-76`) não sabe acrescentar a coluna
  que o próximo pedido vai exigir, por exemplo os campos da agência de turismo.
- **O banco não protege nenhuma regra.** Não há tabela de quartos nem chave estrangeira para `room_number`, que
  aceita qualquer inteiro. Também não há CHECK para:
  - `status` e `rate_plan`;
  - `guests >= 1`;
  - `check_out > check_in`;
  - "exatamente uma identificação", que a cascata de `Store.java:162-164` presume sem garantia.
- **A falta de quarto ocupado em duas reservas depende só da trava no código.** Uma restrição `EXCLUDE` faria isso
  no banco.

Nada disso perde dado hoje, mas o banco confia todo no código.

## Comportamento

**Números:** 83 de 83 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 05: 32 de 32. Duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada, isto quer dizer:
- **Preços, recusas e operações:** os preços com a temporada e o desconto saem certos, as recusas vêm com os
  códigos combinados com o site, e a confirmação, o e-mail, o check-in, o check-out e os cancelamentos com a devolução
  funcionam como ele descreveu.
- **Relatórios e dados:** os dois relatórios batem, e os dados continuam lá depois de reiniciar.
- **Os três documentos:** a pessoa com CPF recebe o comprovante, a empresa recebe a fatura com o ISS de R$ 38,99, e
  o estrangeiro recebe o booking receipt em inglês com `BRL 2,783.52`, "1 night" e a linha Discount. Os três saem no
  e-mail em português, no check-out e na segunda via, e as identificações vazias, duplas ou incompletas são recusadas
  com 400.
- **Reserva dupla:** duas reservas simultâneas do mesmo quarto nunca passam juntas.

## Dúvidas

- **A forma do pattern:** é um método polimórfico no tipo de hóspede (`Identification.render`), não a forma
  clássica com criador e método de fábrica sobrescrito. A referência aceita as duas como acerto.
- **O campo `title`:** no JSON do documento ele muda conforme o tipo ("Fatura de hospedagem", "Booking receipt"),
  e o texto do dono só cita "Comprovante de reserva". Parece a leitura sensata, e o roteiro aceitou.
- **A nota da arquitetura:** ficou entre `bom` e `com problemas`. Pesou o que a skill diz explicitamente: as regras
  usam `Connection` do JDBC e `Reports` monta SQL.
