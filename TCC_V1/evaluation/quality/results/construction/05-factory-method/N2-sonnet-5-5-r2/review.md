# Revisão: construção, cenário 05

## Visão geral

O agente montou um serviço Javalin pequeno (cerca de 1.270 linhas em `src/main`) em quatro pacotes: `web` (rotas, leitura do corpo, montagem do JSON), o pacote raiz `br.com.marealta` (`Main`, `ReservationService`, `ApiException`, `NewReservation`), `domain` (reserva, preço, planos, identificações e documentos, relatórios) e `persistence` (JDBC puro com `DriverManager`). Uma requisição entra em `web/Routes.java:29-32`: o `RequestParser` transforma o corpo num `NewReservation` (já com a `Identity` escolhida por `IdentityKinds.from`). Depois o `ReservationService` abre uma transação (`persistence/Database.java:27-41`), aplica a regra que está no `Reservation` e grava com o `ReservationRepository`. `Views` monta a resposta. Os dados ficam em duas tabelas (`reservations` e `messages`), criadas no código com `create table if not exists`. Os quartos são uma lista fixa no código (`domain/Rooms.java:7-12`). A variação pedida (um documento por tipo de hóspede) é um método polimórfico no tipo de identificação, e o roteiro de comportamento passou inteiro (83 de 83). O que há de mais sério está em três lugares. O banco não tem migrações e as colunas de identificação saem do registro de tipos, então um tipo novo não chega a uma tabela que já existe. Os testes são poucos: 6 testes, 20% das linhas. E há um ciclo pequeno entre `web` e o pacote raiz.

## Arquitetura

**Números:** 4 pacotes, 6 dependências entre eles, 1 ciclo (`br.com.marealta` ↔ `br.com.marealta.web`). `domain` não depende de nada e não usa Javalin nem JDBC. Só `persistence` e o pacote raiz usam JDBC. A maior classe é `ReservationRepository`, com 146 linhas (`measures/dependencies.md`).
**Nota:** com problemas

As camadas existem e estão quase sempre no lugar:
- **A borda HTTP é fina.** Cada handler lê, chama o serviço e escreve a resposta (`web/Routes.java:27-50`). A validação do corpo fica em `web/RequestParser.java:24-50`.
- **As regras não conhecem Javalin nem SQL.** As transições de situação estão em `domain/Reservation.java:47-71`, o preço em `domain/Quote.java:9-28`, o desconto e a devolução em `domain/RatePlan.java`.
- **O SQL fica todo em `persistence`**, com parâmetros.
- **A montagem é feita no `Main`** (`Main.java:11-14`).

Os defeitos:
- **O ciclo.** `web` usa `ReservationService`, `ApiException` e `NewReservation` do pacote raiz (`web/Routes.java:3-4`, `web/RequestParser.java:3-4`). O pacote raiz depende de `web` porque o `Main` mora junto com o serviço (`Main.java:4,14`). O ciclo é pequeno e vem só do lugar do `Main`, mas existe.
- **O serviço conhece JDBC e HTTP.** Ele recebe uma `java.sql.Connection` na interface `AfterChange` (`ReservationService.java:93-96`) e lança exceções com o código HTTP dentro (`ReservationService.java:35-41`, `ApiException.java:7`). A regra fica amarrada à borda.
- **O serviço cria os repositórios por dentro**, com `new ReservationRepository()` e `new MessageRepository()` (`ReservationService.java:22-23`), em vez de recebê-los do `Main`. Pesa pouco, porque os repositórios não têm estado.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 na estrutura (`CpfIdentity`, `CnpjIdentity`, `PassportIdentity`) e nenhum fora dela. A escolha do tipo acontece em 1 lugar (`IdentityKinds.from`). Os 3 lugares onde o documento sai (e-mail, check-out, segunda via) pedem o documento por 1 único método (`Reservation.document()`). Não há nenhum `if` nem `switch` sobre o tipo de hóspede no código.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** O agente usou a forma "método polimórfico no próprio tipo de hóspede", que a referência aceita como acerto:
- `Identity.document(ReceiptData)` (`domain/Identity.java:40-53`) cria o documento, e cada subclasse decide o que é dela: o título, as linhas da parte e o rodapé.
  - A fatura acrescenta a empresa e o ISS em `domain/CnpjIdentity.java:32-39`.
  - O booking receipt troca rótulos, formato de dinheiro e formato de data em `domain/PassportIdentity.java:30-75`.
  - O que os dois documentos em português têm em comum (`nº`, `dd/MM/yyyy`, `R$ 1.234,56`) fica em `domain/PortugueseIdentity.java`.
- A escolha de qual identidade criar é concentrada num registro de fábricas. Cada tipo declara os campos que exige e o construtor (`domain/CpfIdentity.java:7-8`, `domain/CnpjIdentity.java:8-9`, `domain/PassportIdentity.java:13-14`), e `domain/IdentityKinds.java:9-12,26-38` escolhe exatamente um tipo completo.
- Os três lugares passam por `Reservation.document()` (`domain/Reservation.java:79-84`), que delega para `identity.document(...)`:
  - o e-mail em `domain/ConfirmationEmail.java:13`;
  - o check-out em `ReservationService.java:70`;
  - a segunda via em `ReservationService.java:75`.

A agência de turismo entraria como uma subclasse nova mais uma linha em `IdentityKinds`. As colunas da API (`web/Views.java:76-80`, `web/RequestParser.java:70-83`) e as do banco já seguem o registro. Dentro do `Identity`, o pattern vem combinado com um Template Method de ganchos bem miúdos (nove métodos abstratos, alguns com o mesmo valor nos dois idiomas, como `totalLabel`). Isso é granular, mas está dentro da parte que o pattern resolve.

**O exagero.** Fora da parte do pattern não há estrutura sobrando. As interfaces funcionais pequenas são usadas de verdade: `Database.Work` em `persistence/Database.java:12-15`, `Binder` em `persistence/ReservationRepository.java:111-114`, `AfterChange` em `ReservationService.java:93-96`. Não há interfaces de serviço com uma implementação só, nem builders, nem camadas de repasse.

**O resto do código** está limpo:
- **Os tipos.** O dinheiro é `BigDecimal` com `HALF_EVEN` a cada passo (`domain/Money.java:11-13`, `domain/Quote.java:20-23`, `domain/RatePlan.java:20`). As datas são `LocalDate`. Situação, plano e tipo de quarto são `enum`, e a regra "paga" mora no enum (`domain/ReservationStatus.java:6-27`).
- **As transições** estão num lugar só (`domain/Reservation.java:65-71`), e os métodos são curtos.

Há pontos pequenos que não chegam a mudar a nota:
- o `id` mutável com `assignId` (`domain/Reservation.java:8,91-93`);
- a geração de CSV no pacote de domínio (`domain/Reports.java`);
- as exceções da regra com o código HTTP dentro, já citadas na arquitetura;
- o `catch (Exception)` geral das rotas, que só imprime a pilha (`web/Routes.java:22-25`). É aceitável como último recurso.

## Testes

**Números:** 6 testes rodaram, 0 falhas, 0 erros, 0 pulados; 6 métodos escritos, todos em `DomainTest`. A cobertura é de 20,3% das linhas (109 de 536) e 20,0% dos ramos (20 de 100). `domain` tem 109 de 209 linhas cobertas; o pacote raiz, `persistence` e `web` têm 0. Não há nenhum teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

O que existe confere valores de verdade:
- o preço na alta temporada, 599,76 (`DomainTest.java:22-28`);
- o desconto não reembolsável com meio para o par, 116,97 e 662,83 (`DomainTest.java:30-37`);
- o texto inteiro do comprovante, da fatura com o ISS de R$ 38,99 e do booking receipt com `BRL 1,000.00` (`DomainTest.java:47-86`);
- a regra "exatamente um jeito completo" de identificação (`DomainTest.java:88-96`).

A pergunta da mutação na parte nova:
- **Pegaria:** trocar 0,05 do ISS ou mudar o formato BRL faz algum teste falhar.
- **Não pegaria:** trocar `== 1 ? " night"` (`domain/PassportIdentity.java:53`) ou `" diária"` (`domain/PortugueseIdentity.java:26`), porque nenhum teste usa uma noite. Também passaria trocar `signum() > 0` da linha de desconto (`domain/Identity.java:47`) em português, porque só o booking receipt é testado com desconto, e com um desconto absurdo (1.000,00 sobre 779,80, que dá total negativo, `DomainTest.java:84-85`).

Ficam sem nenhum teste:
- a máquina de situações e as recusas (`domain/Reservation.java:47-71`);
- a devolução no cancelamento (`domain/RatePlan.java:13,24`);
- o choque de datas (`persistence/ReservationRepository.java:79-91`);
- os dois relatórios (`domain/Reports.java`);
- o e-mail (`domain/ConfirmationEmail.java`);
- os códigos de erro da API.

O texto não pedia testes. A iniciativa existe e acerta o centro da parte nova, mas deixa sem rede a maior parte das regras.

## Banco

**Números:** 2 tabelas (`reservations` com 20 colunas e `messages` com 5), ambas com chave primária. O dinheiro está em `numeric(12,2)`, e as datas em `date`. As conferências acusaram:
- nenhuma restrição que proteja uma regra (0 CHECK, 0 UNIQUE/EXCLUDE);
- `status`, `rate_plan` e `channel` em `text` sem CHECK;
- 0 arquivos de esquema (é `CREATE TABLE` no código);
- nenhum pool de conexões (1 `DriverManager.getConnection`) e 0 `FOR UPDATE`.

Nenhum SQL montado com dado de fora (`measures/database.md`).
**Nota:** com problemas

Os dados são guardados certo: os tipos estão corretos, todo SQL usa `?`, e as duas escritas da confirmação (a reserva e a mensagem) andam na mesma transação (`ReservationService.java:98-113`, `persistence/Database.java:27-41`). As concatenações em `persistence/ReservationRepository.java:94-96` e `:29-35` usam só constantes do código (os nomes dos enums e os campos do registro).

Os defeitos:
- **O esquema não evolui.** Ele é criado com `create table if not exists` (`persistence/Database.java:71-82`), sem migrações. E as colunas de identificação são geradas a partir de `IdentityKinds.allFields()` (`persistence/Database.java:65-67,74`), com os nomes de campo da API (`guestCpf` vira `guestcpf`). Quando o tipo "agência" for acrescentado, o `insert` e o `select` vão pedir uma coluna nova (`persistence/ReservationRepository.java:29-35,131-133`) que o `create table if not exists` não acrescenta numa tabela que já existe. O resultado é erro 500 em toda reserva, até alguém alterar a tabela à mão. Justamente o caso que o texto anuncia é o que quebra o banco de quem já roda o sistema.
- **A regra "um quarto por noite" é protegida só dentro do processo,** por um `synchronized` (`ReservationService.java:25,32`), sem restrição (EXCLUDE) nem trava no banco. Com duas instâncias do serviço, ou outra escrita no banco, a reserva dupla passaria.
- **Faltam restrições e índices.** Não há CHECK em situação, plano, hóspedes ou datas, nem índice para a consulta de choque e para os relatórios (`room_number`, `check_in`). O quarto não está no banco (`domain/Rooms.java:7-12`), então `room_number` não tem chave estrangeira. Essa é uma escolha aceitável para quartos que "quase nunca mudam".
- **Não há pool:** é uma conexão nova por requisição (`persistence/Database.java:28`).

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 32 de 32;
- as reservas simultâneas: 2 de 2.

(`measures/behavior.md`)
**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona pela API:
- os preços com a temporada e o arredondamento meio para o par;
- as recusas com os códigos certos;
- a confirmação com o e-mail;
- o check-in, o check-out e a segunda via;
- os cancelamentos com e sem devolução;
- os dois relatórios em CSV;
- os dados depois de reiniciar.

Na parte nova, os três documentos saem certos e iguais no e-mail, no check-out e na segunda via:
- o comprovante com CPF;
- a fatura com a empresa e o ISS de R$ 38,99;
- o booking receipt em inglês, com `BRL 2,783.52`, datas ISO e "1 night".

As identificações erradas (nenhuma, duas, ou uma incompleta) são recusadas com 400. As duas reservas simultâneas também se comportaram bem, mas só porque há um único processo (veja a coluna do banco). O problema do esquema com um tipo novo de hóspede não aparece no roteiro, que parte de um banco vazio, e pesa só na coluna do banco.

## Dúvidas

- **Qual forma do pattern foi usada.** Foi a do método polimórfico no tipo de hóspede (`identity.document(...)`), com a criação da identidade concentrada num registro de fábricas (`IdentityKinds`). Por dentro do `Identity` há um Template Method de ganchos miúdos. Contei como acerto, como a referência aceita. Um documento de agência com forma bem diferente (por exemplo, sem a linha de diárias) teria de caber nesse esqueleto ou sobrescrever vários ganchos, mas o texto não dá detalhes desse documento.
- **O ciclo entre `web` e o pacote raiz.** Ele vem só do lugar do `Main`. Contei como "com problemas" pelo critério da skill, mas ele é leve.
