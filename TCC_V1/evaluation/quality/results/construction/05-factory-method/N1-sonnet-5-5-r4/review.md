# Revisão: construção, cenário 05

## Visão geral

O projeto tem 7 pacotes. `pousada` faz a montagem (`App.java:13-24`). `web` é a borda HTTP: `Api`, `RequestParser` e `Views`. `service` guarda os casos de uso, `domain` tem a reserva, o preço, a temporada e as situações, e `identity` cuida das três formas de identificar o hóspede e do documento de cada uma. `document` tem a redação e os formatos dos documentos, e `persistence` usa JDBC puro, com HikariCP.

Uma requisição passa pelo handler de uma linha (`web/Api.java:37-49`), vai para o `ReservationService`, que abre uma transação (`persistence/Database.java:81-95`), e chega ao repositório, que usa SQL com `?`. Os dados ficam em duas tabelas do PostgreSQL. Os quartos ficam em código (`domain/RoomCatalog.java:7-12`).

A variação do cenário foi resolvida com um método polimórfico no tipo de hóspede: `Identity.issue(Stay)`. Os três lugares onde o documento sai passam por um único ponto (`service/ReservationService.java:84-87`). O comportamento passou em todas as 83 conferências.

O que há de mais sério:
- dois ciclos pequenos entre pacotes, os dois passando por `domain`;
- o esquema criado com `CREATE TABLE IF NOT EXISTS` no código, sem migrações e sem `CHECK`.

## Arquitetura

**Números:** 7 pacotes e 18 dependências entre eles. O jdeps achou 2 ciclos: `domain ↔ identity` e `domain ↔ document`. Só `pousada` e `web` usam o Javalin. O JDBC aparece em `persistence` e também em `service`.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê a requisição, chama o serviço e serializa a resposta (`web/Api.java:35-56`). Os erros vão para um único `exception` handler (`web/Api.java:58-62`). Não há regra de negócio nem SQL nos handlers.
- **As regras não conhecem o Javalin nem o SQL.** O preço e a temporada estão em `domain/Quote.java:8-17` e `domain/Season.java:13-21`. As transições de situação estão em `domain/Reservation.java:31-64` e `domain/ReservationStatus.java:23-30`. O SQL está todo em `persistence/ReservationRepository.java` e `persistence/MessageRepository.java`.
- **O serviço depende do JDBC, mas só para a transação.** Ele recebe a `Connection` dentro do `db.tx(c -> ...)` (`service/ReservationService.java:38-45`). Não monta SQL.

Defeitos:
- **Os ciclos.** `domain → identity` existe porque a `Reservation` guarda uma `Identity` (`domain/Reservation.java:4,14`). `identity → domain` existe porque as identidades usam `PousadaException` e `Money` (`identity/Fields.java:3`, `identity/CnpjIdentity.java:6`). `domain → document` vem de `Reservation.stay()`, que monta um `document.Stay` (`domain/Reservation.java:3,52-55`). `document → domain` vem de `Stay`, que usa `RoomType` (`document/Stay.java:3`). São ciclos entre pacotes vizinhos e não atrapalham hoje. Mas mostram que a fronteira entre domínio, identidade e documento não está decidida: o domínio sabe montar dados de documento.
- **A montagem está espalhada.** O `App` monta o `Database`, os serviços e a `Api`. Mas cada serviço cria os próprios repositórios por dentro (`service/ReservationService.java:21-22`, `service/ReportService.java:18`). A `Api` cria o `ObjectMapper` e o `RequestParser` (`web/Api.java:22-23`). O catálogo de quartos é estático e global (`domain/RoomCatalog.java:7`), e `Views` o consulta direto (`web/Views.java:54`).
- **Tamanhos:** nenhuma classe domina. A maior é `ReservationRepository`, com 175 linhas.

## Design de baixo nível

**Números:** há 3 tipos de documento: comprovante, fatura e *booking receipt*. Os 3 estão na estrutura (`CpfIdentity`, `CnpjIdentity`, `PassportIdentity`, que implementam `Identity.issue`), e nenhum fica fora dela. Os 3 lugares que emitem o documento (e-mail de confirmação, check-out e segunda via) passam pelo mesmo `ReservationService.document`. Não há nenhum `if` ou `switch` sobre o tipo de hóspede.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A escolha de qual documento criar mora no próprio tipo de hóspede:
- `identity/Identity.java:15` declara `IssuedDocument issue(Stay stay)`;
- cada implementação cria o seu documento: `identity/CpfIdentity.java:41-44`, `identity/CnpjIdentity.java:48-56` (com o ISS de 5% em `:53`) e `identity/PassportIdentity.java:46-49`.

Os três lugares pedem o documento ao mesmo método, `ReservationService.document` (`service/ReservationService.java:84-87`):
- a confirmação, para o e-mail (`service/ReservationService.java:57`);
- o check-out (`web/Api.java:46-47` → `web/Api.java:66-68`);
- a segunda via (`web/Api.java:48-49`).

A criação da identidade a partir do pedido também é uma fábrica por tipo. Cada `IdentityKind` sabe os seus campos e cria a sua `Identity` (`identity/CpfIdentity.java:13-28`). O registro único é `IdentityKinds.ALL` (`identity/IdentityKinds.java:10-13`). Ele escolhe a forma pelos campos recebidos e recusa nenhuma ou duas (`identity/IdentityKinds.java:24-33`), e restaura a identidade ao ler do banco (`identity/IdentityKinds.java:35-39`). `Views` e `RequestParser` percorrem `allFieldNames()` (`web/Views.java:75-80`, `web/RequestParser.java:84-92`). Para acrescentar a agência de turismo, basta uma classe nova e uma linha em `IdentityKinds`. A forma usada é a do método polimórfico no tipo de hóspede, que a referência aceita como acerto.

**O exagero.** Não há estrutura sem uso:
- `Wording` (`document/Wording.java:15-63`) separa idioma e formato em duas instâncias, `PORTUGUESE` e `ENGLISH`, e as duas são usadas;
- `IdentityKind` tem três implementações;
- `Database.Work` (`persistence/Database.java:44-47`) só existe para deixar a lambda lançar `SQLException`.

**O resto do código** está limpo:
- dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:13-15`, `domain/Quote.java:15-16`, `domain/Season.java:20`);
- datas em `LocalDate`;
- planos, situações, canais e códigos de erro em `enum`, com as regras dentro deles (`domain/RatePlan.java:17-23`, `domain/ReservationStatus.java:23-30`, `domain/ErrorCode.java:3-9`);
- a reserva é um `record` imutável, e as transições devolvem uma reserva nova (`domain/Reservation.java:57-64`);
- os erros inesperados são registrados no log, não engolidos (`web/Api.java:59-62`).

Pequenos pontos, que não mudam a nota:
- **O conjunto das situações "pagas" está repetido.** Está no `enum` (`domain/ReservationStatus.java:6-10`) e de novo como texto no SQL (`persistence/ReservationRepository.java:27`).
- **A regra do "dinheiro que fica com a pousada" está em dois lugares:** no SQL (`persistence/ReservationRepository.java:112`) e em Java (`service/ReportService.java:44`).
- **Um nome qualificado inteiro no meio do código:** `pousada.domain.RoomCatalog` (`web/Views.java:54`).

## Testes

**Números:** 11 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 11 métodos de teste em 2 arquivos. A cobertura é de 93,3% das linhas (501 de 537) e de 78,6% dos ramos (77 de 98). Os pacotes de regra ficam perto de 100%: `domain` 88/89, `identity` 58/58, `service` 72/72 e `document` 23/23.
**Nota:** bom

O texto não pedia testes, e o agente escreveu dois níveis.

**Testes de unidade, com valores exatos** (`src/test/java/pousada/PricingAndDocumentsTest.java`):
- os limites da alta temporada: 14/12, 15/12, 29/02, 01/03, 31/07 e 01/08 (`:26-31`, numeradas como no arquivo);
- o exemplo do texto, 599,76 (`:37-40`);
- o desconto arredondado meio para o par: 86,985 vira 86,98 (`:46-49`);
- o texto completo do comprovante (`:60-69`), da fatura com o ISS de R$ 38,99 (`:80-90`) e do *booking receipt* (`:95-103`);
- a variante com desconto e uma noite, nos dois idiomas (`:74-75`, `:108-109`).

**Testes de integração pela API, contra o PostgreSQL de `DB_URL`, num schema descartável** (`ApiIntegrationTest.java:40-55`). Eles cobrem:
- o fluxo inteiro, com os valores: o total, o pago, o e-mail com assunto, destinatário e corpo, os relatórios em CSV linha a linha e a noite de saída sem conflito (`ApiIntegrationTest.java:94-154`);
- o cancelamento NON_REFUNDABLE, que fica no financeiro (`:157-167`);
- o *booking receipt* no e-mail e na segunda via (`:169-178`);
- as recusas da identificação: nenhuma, duas e incompleta (`:187-189`).

**A pergunta da mutação.** Trocar o ISS de 0.05 para 0.06, o separador do BRL ou "night" por "nights" derruba algum teste. Algumas regras ficam sem rede:
- **A devolução no plano STANDARD.** Se `RatePlan.refundOf` (`domain/RatePlan.java:21-23`) devolvesse sempre zero, nenhum teste falharia: não há cancelamento STANDARD de uma reserva confirmada, nem de uma pendente.
- **O código de erro nas recusas.** Quase todas conferem só o status HTTP, não o código no corpo (`ApiIntegrationTest.java:183-190`).
- **O reinício do serviço.** Nenhum teste confere que os dados continuam depois de reiniciar.
- **A fatura só no texto.** A fatura não é conferida no e-mail nem no check-out pela API, só como texto isolado.

São lacunas pequenas diante do que está conferido.

## Banco

**Números:** 2 tabelas, `reservations` (17 colunas, 16 NOT NULL) e `messages` (5 colunas). As duas têm chave primária. O dinheiro está todo em `numeric(14,2)` e as datas em `date`. Não há chaves estrangeiras nem nenhuma restrição `CHECK`, `UNIQUE` ou `EXCLUDE`. `status`, `rate_plan`, `identity_kind` e `channel` ficam em `text`, sem `CHECK`. O esquema é criado no código Java, sem migrações.
**Nota:** com problemas

O que está certo:
- **Tipos e chaves:** os tipos são os certos e as duas tabelas têm chave (`persistence/Database.java:13-41`).
- **SQL só com `?`.** Os trechos montados com `+` juntam apenas constantes: `COLUMNS` e `PAID` (`persistence/ReservationRepository.java:25-27`).
- **Pool e transação.** Há um pool HikariCP (`persistence/Database.java:56-66`), as conexões são devolvidas com try-with-resources e há uma transação explícita com rollback (`persistence/Database.java:81-95`). A confirmação grava a reserva e o e-mail na mesma transação (`service/ReservationService.java:53-59`).
- **A sobreposição de reservas é protegida entre processos.** Há um `pg_advisory_xact_lock` por quarto antes da conferência de sobreposição (`persistence/ReservationRepository.java:81-86`, `service/ReservationService.java:39-40`), e as transições usam `SELECT ... FOR UPDATE` (`persistence/ReservationRepository.java:76-78`). O roteiro de concorrência passou.

Defeitos:
- **O esquema não tem como evoluir.** Ele sai de `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:11-42`, `:68-79`), sem migrações versionadas. A próxima coluna nova não chega a um banco que já existe.
- **O banco não protege nenhuma regra sozinho.** Não há `CHECK` para situação, plano, `guests >= 1` ou `check_out > check_in`, nem `EXCLUDE` contra sobreposição: a regra depende da trava no código. Os quartos não estão no banco, então `room_number` não tem chave estrangeira.
- **A identificação é um JSON guardado como `text`.** Fica em `identity_data` (`persistence/Database.java:19`, `persistence/ReservationRepository.java:40,140`). Não dá para consultar o CPF ou o CNPJ pelo banco, e nada garante o formato.

## Comportamento

**Números:** 83 de 83 conferências passaram e nenhuma falhou:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 05: 32/32;
- duas reservas ao mesmo tempo: 2/2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto descreve, sem nenhuma conferência falhando:
- **O preço:** a alta temporada e o desconto não reembolsável estão certos, com arredondamento meio para o par (599,76; 37,48 e 212,42).
- **Situações e devoluções:** as situações, as devoluções nos dois planos e na reserva pendente, e as recusas, cada uma com o seu código.
- **E-mail e relatórios:** o e-mail de confirmação e os dois relatórios em CSV.
- **Persistência:** os dados continuam depois de reiniciar.
- **Concorrência:** duas reservas simultâneas do mesmo quarto nunca são criadas juntas.

Na parte do cenário:
- quem tem CPF continua recebendo o comprovante;
- a empresa recebe a fatura com a empresa, o hóspede e o ISS retido de R$ 38,99, tanto no e-mail (que segue em português) quanto no check-out;
- o estrangeiro recebe o *booking receipt* em inglês, com `BRL 2,783.52`, datas `2027-03-10`, "1 night" e a linha "Discount";
- a reserva recusa com 400 a falta de identificação, CPF e CNPJ juntos e as identificações incompletas.

## Dúvidas

- **A forma do pattern.** O agente usou um método polimórfico no tipo de hóspede (`Identity.issue`). A criação da identidade passa por um `IdentityKind` por tipo, registrado em `IdentityKinds`. Não é a forma clássica, com um criador por subclasse, mas a referência aceita essa forma como acerto.
- **A nota do design.** Hesitei entre `bom` e `com problemas`, por causa da duplicação do conjunto de situações pagas e da regra do dinheiro retido entre SQL e Java (`persistence/ReservationRepository.java:27,112`). Julguei que são pequenos e localizados demais para baixar a nota.
