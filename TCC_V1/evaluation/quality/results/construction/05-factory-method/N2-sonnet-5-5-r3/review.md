# Revisão: construção, cenário 05

## Visão geral

O projeto é um pacote só (`br.com.mare.pousada`, 35 classes, 1289 linhas). Os papéis estão separados por classe, mas não por pacote. Uma requisição entra pelas rotas do Javalin em `Api` (`Api.java:35-50`), que lê o corpo e o converte num `BookingRequest` (`Api.java:97-122`). Daí ela vai para `ReservationService` (sem Javalin nem SQL), que usa a entidade `Reservation` (as mudanças de situação ficam em `Reservation.java:52-76`) e `PricingPolicy` para o preço. A gravação passa pelas interfaces `ReservationRepository` e `MessageRepository`, implementadas em `JdbcReservationRepository` e `JdbcMessageRepository`. O esquema é criado com `CREATE TABLE IF NOT EXISTS` em `JdbcDatabase.java:29-56`. Os quartos ficam no código (`RoomCatalog.java:7-12`). A variação do cenário (o documento por tipo de hóspede) está bem resolvida: cada identificação (`CpfIdentity`, `CompanyIdentity`, `PassportIdentity`) cria o seu documento, e os três lugares pedem o documento a ela. O roteiro de comportamento passou inteiro (83/83). Não há defeito grave. Os defeitos que existem estão no banco: sem migrações, nenhuma restrição, a proteção contra reserva dupla só dentro do processo e uma conexão nova por chamada. Os testes também deixam lacunas: nenhum teste toca o JDBC, e a cobertura não sai pelo `mvn verify` do projeto.

## Arquitetura

**Números:** 1 pacote (`br.com.mare.pousada`, 35 classes, 1289 linhas), 0 dependências entre pacotes, nenhum ciclo. Esse único pacote usa Javalin, JDBC e Jackson. A maior classe é `Api.java`, com 195 linhas; a seguinte é `JdbcReservationRepository.java`, com 111.
**Nota:** com problemas

Por classe, as camadas existem e estão no lugar certo:
- A borda HTTP é fina. Cada rota é uma linha que lê, chama o serviço e escreve (`Api.java:36-50`). A regra de negócio fica fora dela. Em `Api.java:97-122` só há validação de formato da entrada.
- As regras não conhecem Javalin nem JDBC: `ReservationService.java:1-81`, `Reservation.java`, `PricingPolicy.java`, `RatePlan.java`.
- O SQL está isolado nas duas classes `Jdbc*Repository`, atrás de interfaces (`ReservationRepository.java:6-15`).
- A montagem fica num lugar só (`Main.java:4-16`), e é ela que deixa os testes trocarem os repositórios pelos de memória (`ApiTest.java:27`).

Mesmo assim, há três defeitos:
- **Tudo num pacote só.** Nada no código impede uma rota de chamar o JDBC ou uma regra de importar o Javalin. Quem mantém essa separação é só a disciplina de quem escreve. Para um sistema que vai crescer (as agências já vêm aí), é a primeira coisa que um revisor pediria.
- **O formato de gravação mora no registro do domínio.** `IdentityKinds`, o registro dos tipos de identificação, também serializa os dados de identificação para o banco com Jackson (`IdentityKinds.java:3,17,52-72`). Esse formato é chamado direto de dentro do repositório (`JdbcReservationRepository.java:85-86,105`).
- **A borda contorna o serviço.** `Api` recebe o `MessageRepository` e lê dele direto (`Api.java:19,48`).

Pelo critério da skill, "tudo num pacote" pesa para `ruim`. Dei `com problemas` porque a separação por classe é real e limpa.

## Design de baixo nível

**Números:** 3 tipos de hóspede e 3 documentos. Os 3 estão na estrutura e 0 fora dela. Os 3 lugares que emitem documento passam todos pelo método polimórfico: o e-mail (`Notifier.java:13`), o check-out (`ReservationService.java:65`) e a segunda via (`ReservationService.java:75`). Não há nenhum `switch`/`instanceof` sobre o tipo de hóspede no código principal.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O agente usou a forma "método polimórfico no próprio tipo de hóspede", que a referência aceita como acerto:
- A interface `GuestIdentity` declara `Document document(Reservation)` (`GuestIdentity.java:12`).
- Cada implementação devolve o seu documento: `CpfIdentity.java:21-23` cria um `ReceiptDocument`, `CompanyIdentity.java:21-23` um `InvoiceDocument` e `PassportIdentity.java:21-23` um `BookingReceiptDocument`.
- Os três lugares pedem o documento sem saber qual é: `Notifier.java:13`, `ReservationService.java:65` e `ReservationService.java:75`.
- A leitura do corpo também ficou concentrada. Cada tipo declara os seus campos e a sua fábrica (`CpfIdentity.java:7-8`), e `IdentityKinds` é o único lugar que lista os tipos (`IdentityKinds.java:12-15`). Esse mesmo registro valida "exatamente um tipo, completo" (`IdentityKinds.java:30-46`) e monta os cinco campos com `null` na resposta (`Api.java:129-135`).
- Uma agência nova é uma identificação nova, um documento novo e uma linha em `IdentityKinds.ALL`, sem mexer nos três lugares.
- Os documentos compartilham o esqueleto do texto em `Document.text()` (`Document.java:31-48`). O idioma e os formatos ficam num `DocumentStyle` (`DocumentStyle.java:16-22`). Por isso o comprovante e o booking receipt não duplicam código.

**O exagero.** Não há estrutura sem necessidade:
- As interfaces de repositório têm uma segunda implementação de verdade, a dos testes (`InMemoryRepositories.java:240,265`).
- `IdentityKind`/`IdentityKinds` e `DocumentStyle` servem à própria variação.
- O único excesso é `IdentityKinds.kindName` (`IdentityKinds.java:48-50`), que só repassa `identity.kind().name()`, e é pequeno demais para pesar.

**O resto do código.** Os tipos estão certos: `BigDecimal` com `HALF_EVEN` em cada passo (`Money.java:13-19`, `PricingPolicy.java:27-32`), `LocalDate`/`YearMonth` para datas, e enums para a situação, o plano, o tipo de quarto e o código de erro. O estado muda só por transições controladas (`Reservation.java:70-76`). Os defeitos, todos localizados:
- **A causa do erro 500 se perde.** O tratador genérico de `Exception` responde 500 sem registrar a causa em lugar nenhum (`Api.java:32-33`). Uma falha do banco em produção fica invisível. É exatamente o "`catch (Exception)` que esconde a causa".
- **Os nomes dos campos de identificação são texto solto e repetido.** Cada tipo escreve os mesmos nomes duas vezes, na lista de campos e em `values()` (`CpfIdentity.java:7` e `:17`; `CompanyIdentity.java:7` e `:17`; `PassportIdentity.java:7` e `:17`). Esses dois lugares têm de ser mantidos iguais à mão, e um erro de digitação só apareceria em tempo de execução.
- **O texto do ISS repete a alíquota.** "ISS retido (5%)" está escrito à parte da constante `ISS_RATE` (`InvoiceDocument.java:7` e `:35`).
- **Os campos da resposta estão copiados.** `reservationView` e `documentView` repetem o mapeamento de boa parte dos campos (`Api.java:146-184`).
- **O construtor de `Reservation` tem 13 parâmetros posicionais** (`Reservation.java:22-24`), fácil de trocar a ordem sem o compilador perceber.

## Testes

**Números:** 10 testes rodaram, 0 falhas, 0 erros, 0 pulados; 10 métodos escritos (7 em `DomainTest`, 3 em `ApiTest`).
- **Cobertura pelo script:** sem medida. O `pom.xml` define `<argLine>-Dfile.encoding=UTF-8</argLine>` sem `@{argLine}` (`pom.xml:52`), e por isso o agente do JaCoCo não entra no `mvn verify` do projeto.
- **Cobertura numa cópia em `/tmp`:** acrescentei `@{argLine}` e medi cerca de 73% das linhas (395/540) e 73% dos ramos (69/94). As três classes JDBC ficaram em 0%: `JdbcReservationRepository` 0/66, `JdbcMessageRepository` 0/25, `JdbcDatabase` 0/20. `IdentityKinds` ficou em 19/31.
- **Tipos de teste:** 1 arquivo pela API (Javalin de verdade com repositórios em memória), nenhum contra o banco, nenhum mock.

**Nota:** com problemas

**O que está bem testado, com valores:**
- O preço da alta temporada, 599,76 (`DomainTest.java:18-23`), e os limites da temporada: 14/12, 15/12, 29/02 e 01/03 (`DomainTest.java:26-31`).
- O desconto do não reembolsável, com 116,97 e 662,83 (`DomainTest.java:34-40`).
- Os textos inteiros dos três documentos: o comprovante (`DomainTest.java:43-53`), a fatura com ISS de R$ 38,99 (`DomainTest.java:56-68`), e o booking receipt com desconto e milhar, "BRL 2,319.60" (`DomainTest.java:71-82`).
- A devolução nos dois planos (`DomainTest.java:85-98`).
- Pela API: o fluxo completo, os dois CSVs comparados por inteiro, as recusas de identificação (nenhuma, duas, incompleta) e os cinco campos com `null` (`ApiTest.java:52-101`).

**A pergunta da mutação, na parte nova:**
- Trocar `0.05` por outro valor no ISS (`InvoiceDocument.java:7`) é pego pelo teste `invoice`.
- Trocar o `ENGLISH` por `PORTUGUESE` no booking receipt também seria pego.
- Trocar `oneNight` por `manyNights`, ou `count == 1` por `count <= 1` (`DocumentStyle.java:40`), passaria. Nenhum teste usa reserva de uma noite em documento: o "1 night" / "1 diária" fica sem rede.
- Trocar o documento que vai no e-mail passaria. `ApiTest.java:67` só confere o começo do corpo, e só para o CPF.
- Quebrar `IdentityKinds.restore`/`serialize`, que leem e gravam a identificação no banco, passaria: nenhum teste passa pelo JDBC.

**As recusas:** quase todas são conferidas só pelo status HTTP (`ApiTest.java:90-124`). O código `error` aparece numa só, `ROOM_UNAVAILABLE` (`ApiTest.java:119`). Um `RESERVATION_NOT_FOUND` trocado por `ROOM_NOT_FOUND` passaria.

## Banco

**Números:** 2 tabelas (`reservations`, com 17 colunas e 16 `NOT NULL`, e `messages`, com 5 colunas), as duas com chave primária. O dinheiro está todo em `numeric(12,2)`, as datas em `date`.
- **O que as conferências acusaram:** 0 chaves estrangeiras, 0 `CHECK`, 0 `UNIQUE`/`EXCLUDE`; `status`, `rate_plan`, `identity_kind` e `channel` em `text` sem `CHECK`.
- **Como o esquema é criado:** sem migrações, com `CREATE TABLE` no código Java.
- **O código:** sem pool (1 `DriverManager.getConnection`), 0 transações explícitas, 0 `FOR UPDATE`, nenhum SQL montado com `+` a partir de dado de fora.

**Nota:** com problemas

O que está certo:
- O SQL todo usa parâmetros (`JdbcReservationRepository.java:22-24,42,56`; `JdbcMessageRepository.java:22`).
- As conexões são devolvidas com try-with-resources.
- O dinheiro tem 2 casas e as datas são `date` (`JdbcDatabase.java:37-47`).
- Os dados ficam no banco e sobrevivem ao reinício: o roteiro conferiu 3 de 3.

Os defeitos:
- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` no código (`JdbcDatabase.java:29-56`), sem migrações versionadas. O próximo pedido que acrescentar uma coluna (o documento da agência, por exemplo) não tem como alterar uma tabela que já existe.
- **A regra "um quarto, uma reserva por noite" não tem nada no banco** que a proteja (nenhum `EXCLUDE` nem trava). Ela depende do `synchronized` do serviço (`ReservationService.java:14`), que vale para um processo só. Além disso, a verificação traz a tabela inteira para filtrar em Java (`ReservationService.java:22-25`), e os relatórios também (`Reports.java:17,32`). É uma carga que cresce sem limite.
- **Não há transação.** A confirmação grava a reserva e depois a mensagem em conexões separadas, sem transação (`ReservationService.java:42-43`). Se a segunda escrita falhar, a reserva fica confirmada sem o e-mail registrado.
- **Os dados de identificação ficam opacos.** Os cinco campos são gravados como um JSON dentro de uma coluna `TEXT` (`identity_data`, `JdbcDatabase.java:36`; `IdentityKinds.java:52-58`). O banco não confere nada nesses dados, e não dá para consultá-los com SQL (por exemplo, as reservas de um CNPJ).
- **Sem chave estrangeira para o quarto.** Não há tabela de quartos, só um `room_number INTEGER` (`JdbcDatabase.java:32`). É aceitável porque o texto diz que os quartos "quase nunca mudam", mas nada impede um número inválido.
- **Sem pool de conexões.** Cada chamada abre uma conexão nova (`JdbcDatabase.java:20-22`).

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam:
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 05: 32/32;
- as duas reservas ao mesmo tempo: 2/2 (em 10 rodadas, sempre uma criada e a outra recusada com 409).

**Nota:** bom

Para o dono da pousada, tudo o que o texto pede funciona pela API: os preços com temporada e arredondamento, as recusas com os códigos certos, a confirmação com e-mail, o check-in e o check-out, os cancelamentos com devolução, os dois relatórios e os dados depois de reiniciar. Na parte do cenário também:
- a fatura com a empresa e o ISS de R$ 38,99;
- o booking receipt em inglês, com "BRL 2,783.52", as datas em formato ISO e "1 night";
- o e-mail sempre em português, com o documento certo dentro;
- a recusa de identificação ausente, dupla ou incompleta.

Um aviso, que pesa na coluna do banco e não aqui: a reserva simultânea só passou porque o serviço roda num processo só (`ReservationService.java:14`). Com duas instâncias do serviço, a duplicação seria possível.

## Dúvidas

- **A nota da arquitetura.** O critério da skill põe "tudo num pacote" em `ruim`, mas aqui as camadas existem por classe: rotas finas, serviço sem Javalin e sem SQL, JDBC isolado atrás de interfaces, montagem em `Main`. Dei `com problemas` pesando a separação real; quem ler o critério ao pé da letra pode dar `ruim`.
- **A forma do pattern.** O acerto é na forma "método polimórfico no próprio tipo de hóspede" (`GuestIdentity.document`), com um registro (`IdentityKinds`) para criar a identificação a partir do corpo. Não é a forma clássica, com um criador por subclasse.
- **A cobertura.** Os números de cobertura não são do script, que não conseguiu medir por causa do `argLine` em `pom.xml:52`. Medi numa cópia em `/tmp` com `@{argLine}` acrescentado, sem mexer em `projeto/`.
