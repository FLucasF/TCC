# Revisão: construção, cenário 05

## Visão geral

O agente montou o projeto em 8 pacotes: as rotas do Javalin ficam todas no `main` de `com.pousada.Application`
(`Application.java:52-220`), os DTOs em `api.dto`, as regras em `application` (`ReservationService`, `ReportService`)
e em `domain.service` (`PricingService`, `EmailService`), o modelo em `domain.model`, os tipos de hóspede em
`domain.guest` e o JDBC puro, com HikariCP, em `infrastructure`. Os repositórios são interfaces em `domain.repository`.
Uma requisição faz este caminho: o handler lê o corpo, chama o `ReservationService`, que usa os repositórios; os
dados ficam no PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código
(`infrastructure/Database.java:29-70`). A variação do cenário foi bem resolvida: cada tipo de hóspede gera o próprio
documento, e uma fábrica escolhe o tipo. O mais sério está em outro lugar:
- nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos (9 de 10 rodadas criaram
  as duas);
- o comprovante e a fatura em português saem com um espaço não separável (U+00A0) depois de "R$", e não com o espaço
  comum que o texto mostra;
- o `mvn package` gera um jar fino que depende de `target/lib/`, e essa pasta leva também as dependências de teste.
  Por isso os scripts de banco e de comportamento escolheram o jar errado e não mediram nada. Medi à mão (explico
  abaixo).

## Arquitetura

**Números:** 8 pacotes, 18 dependências entre eles, 1 ciclo (`domain.guest` ↔ `domain.model`). O JDBC só aparece em
`infrastructure`; o Javalin só aparece em `com.pousada`. Maiores classes: `Application` (258 linhas),
`ReservationRepositoryImpl` (255), `ReservationService` (190).
**Nota:** com problemas

As regras não conhecem nem o Javalin nem o SQL: `ReservationService` e `ReportService` só falam com as interfaces de
repositório, e todo o SQL está em `infrastructure/*RepositoryImpl.java`. A montagem fica num lugar só
(`Application.java:34-42`). As dependências apontam da borda para as regras e daí para os dados. Os defeitos:

- **O ciclo:** `domain.model.Reservation` guarda um `GuestType` e delega a ele (`domain/model/Reservation.java:3`,
  `:127`). Do outro lado, `GuestType.generateDocument(Reservation)` recebe `Reservation` e devolve `Document`, que
  também moram em `domain.model` (`domain/guest/GuestType.java:3-6`). É um ciclo pequeno entre dois pacotes vizinhos,
  mas o projeto foi construído do zero, então ele é novo.
- **A borda HTTP não é só borda:** o `main` tem 190 linhas de rotas, com validação misturada:
  - a saída depois da entrada é conferida no handler (`Application.java:66`), e o "menos de um hóspede" no serviço;
  - o mês é lido no próprio handler (`Application.java:206-212`);
  - `GET /rooms` e `GET /messages` chamam o repositório direto, sem passar pelo serviço (`Application.java:53`,
    `:177`);
  - o mapeamento de exceção para código HTTP se repete em 7 handlers, em vez de um `app.exception(...)`.
- **Dependências criadas por dentro:** o `ReservationService` faz `new PricingService()` e `new EmailService()`
  (`application/ReservationService.java:28-29`). Essas classes não guardam estado, então isso pesa pouco.

## Design de baixo nível

**Números:** 3 tipos de hóspede (`IndividualGuest`, `CorporateGuest`, `ForeignGuest`), os 3 na estrutura. O tipo é
escolhido num lugar só (`GuestTypeFactory.create`), e os 3 lugares onde o documento sai chamam
`reservation.generateDocument()` (`application/ReservationService.java:94`, `:137` e `:147`), sem nenhum `if` sobre
o tipo de hóspede fora da fábrica.
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** O agente usou a forma do método polimórfico no próprio tipo de hóspede, mais uma fábrica concentrada.
`GuestType.generateDocument(Reservation)` (`domain/guest/GuestType.java:7`) é sobrescrito em cada tipo
(`IndividualGuest.java:23`, `CorporateGuest.java:27`, `ForeignGuest.java:21`), e `Reservation.generateDocument()`
delega ao tipo (`domain/model/Reservation.java:126-128`). A escolha do tipo está toda em
`domain/guest/GuestTypeFactory.java:12-34`, que o serviço usa na criação (`ReservationService.java:61`) e o
repositório usa ao reler a linha do banco (`ReservationRepositoryImpl.java:222`). Os três lugares (e-mail de
confirmação, check-out e segunda via) pedem o documento ao mesmo método, e nenhum deles decide qual criar. Uma
agência de turismo seria uma classe nova e um ramo novo na fábrica. Isso é acerto.

O que afasta um pouco:
- A fábrica soma condições soltas em vez de exigir "exatamente um jeito". Um pedido com `guestCpf` e só
  `companyCnpj`, sem `companyName`, é aceito como pessoa física, e o CNPJ some sem aviso
  (`GuestTypeFactory.java:20`). Conferi pela API: criou a reserva 2, com `companyCnpj: null`.
- `GuestType` expõe os cinco campos de identificação com `null` nos que não se aplicam (`GuestType.java:9-13`).

**O exagero.** `RoomRepository`, `ReservationRepository` e `EmailRepository` são interfaces com uma implementação só
(`*RepositoryImpl`), e nada as substitui: nenhum teste usa uma implementação falsa. `Document` e `Reservation` também
têm builders escritos à mão (`domain/model/Document.java:67-107`, `domain/model/Reservation.java:130-174`), com 17
campos cada. Os builders ainda se defendem pelo número de campos, mas as três interfaces são estrutura sem uso.

**O resto do código.**
- **Duplicação:** os três tipos de hóspede repetem o bloco `Document.builder()` de cerca de 18 linhas
  (`IndividualGuest.java:26-43`, `CorporateGuest.java:30-47`, `ForeignGuest.java:24-41`). Repetem também
  `nightsText`, `roomTypeName`, a linha do desconto e o `DATE_FORMAT`. `IndividualGuest` e `CorporateGuest` repetem
  ainda o `formatMoney` (`IndividualGuest.java:74`, `CorporateGuest.java:82`). Uma mudança no formato comum precisa
  ser feita em três lugares.
- **Estado compartilhado:** o `NumberFormat` é um `static final` dividido entre as threads do Jetty
  (`IndividualGuest.java:13`, `CorporateGuest.java:14`), e o `NumberFormat` não é seguro entre threads.
- **O espaço depois de "R$":** é esse `NumberFormat` de pt-BR que põe o espaço não separável depois de "R$" (veja
  Comportamento).
- **Erros:**
  - o `catch (Exception e)` em `Application.java:101` transforma qualquer falha, até uma falha do banco, em 400
    INVALID_REQUEST;
  - o `Long.parseLong` do id não é tratado (`Application.java:108`): `GET /reservations/abc` dá 500 em texto puro
    (conferido);
  - a situação da reserva é conferida duas vezes, no serviço (`canBeConfirmed`, `ReservationService.java:87`) e de
    novo na entidade (`Reservation.java:69-71`).
- **Tipos:** `BigDecimal` com `HALF_EVEN` em cada passo do preço (`domain/service/PricingService.java:33`, `:52`,
  `:59`, `:86`) está certo. Mas os zeros nascem com escala 0 (`Reservation.java:150`, `PricingService.java:88`,
  `ReportService.java:50`). O resultado é `"amountPaid":0` e `"discount":0` no JSON, e `total,0` no CSV de um mês
  vazio.
- **Assinatura longa:** `createReservation` recebe 12 parâmetros soltos (`ReservationService.java:32-44`).

## Testes

**Números:** 5 testes rodaram, 0 falhas, 0 erros, 0 pulados; 5 métodos de teste em 2 arquivos. Cobertura de linhas
de 29,5% (284 de 962) e de ramos de 14,7% (25 de 170). Os pacotes `application`, `infrastructure`, `com.pousada` e
`api.dto` têm 0% de cobertura. Não há teste pela API nem contra o banco.
**Nota:** com problemas

O que está testado, com valores:
- o exemplo de alta temporada do texto, R$ 599,76 (`PricingServiceTest.java:15-30`);
- o NON_REFUNDABLE, 779,80 com desconto de 116,97 e total de 662,83 (`PricingServiceTest.java:32-47`);
- os três documentos com os exemplos do texto (`DocumentGenerationTest.java:16-81`).

A pergunta da mutação na parte nova: trocar o `0.05` do ISS faria `contains("38,99")` falhar
(`DocumentGenerationTest.java:58`), e trocar "nights" ou o formato BRL também faria um teste falhar. Mas há regras
sem rede:
- apagar a conferência de "exatamente um jeito" na fábrica (`GuestTypeFactory.java:30`) não derruba nenhum teste;
- trocar `"1 night"`/`"1 diária"` não derruba nenhum teste;
- apagar a linha do desconto nos documentos não derruba nenhum teste.

As asserções do texto são fracas: usam `contains("779,80")` em vez da linha inteira (`DocumentGenerationTest.java:35`)
e por isso deixaram passar o espaço não separável depois de "R$". As regras centrais da parte comum não têm teste
nenhum:
- as situações da reserva e as transições recusadas;
- o cancelamento e a devolução;
- o choque de datas;
- a capacidade;
- os códigos de erro;
- os dois relatórios.

## Banco

**Números:** o script não mediu. Ele subiu o jar errado, o `target/lib/byte-buddy-1.12.21.jar`: é o maior jar com
`Main-Class` em `target/`, e foi copiado para lá porque o `maven-dependency-plugin` copia também as dependências de
teste (`pom.xml:89-105`). Por isso o resultado foi "0 tabelas". Subi à mão o jar do projeto
(`target/mare-alta-1.0.0.jar`) num banco vazio, e ele criou 3 tabelas:
- `rooms`, com PK `number`;
- `reservations`, com PK `id` bigserial;
- `emails`, com PK `id`.

Dinheiro em `numeric(10,2)`, datas em `date`, `NOT NULL` no que é obrigatório. Não há nenhuma chave estrangeira, CHECK,
UNIQUE ou EXCLUDE; não há nenhuma transação explícita nem `FOR UPDATE`.
**Nota:** ruim

- **O esquema:** é criado com `CREATE TABLE IF NOT EXISTS` dentro do Java (`infrastructure/Database.java:29-70`), sem
  migrações. Um pedido futuro que acrescente uma coluna não tem como chegar a um banco que já existe.
- **Sem chave estrangeira:** `reservations.room_number` não aponta para `rooms` (`Database.java:39`). O tipo do
  quarto também é copiado na reserva (`room_type`, `Database.java:40`).
- **Sem CHECK:** `status`, `rate_plan` e `channel` são texto livre.
- **A regra mais importante não tem proteção nenhuma.** "Um quarto não pode ter duas reservas na mesma noite" é um
  `SELECT` seguido de um `INSERT` em conexões separadas e sem transação: `ReservationService.java:57` consulta
  (`ReservationRepositoryImpl.java:101-129`), e `ReservationService.java:81` grava. Não há `synchronized`, trava,
  transação ou restrição de exclusão. Pelo roteiro do comportamento, rodado com o jar certo, 9 de 10 rodadas de dois
  pedidos simultâneos criaram as duas reservas para o mesmo quarto e as mesmas noites. É dado errado gravado no
  banco. Também confirmar, cancelar e check-in fazem ler, mudar e gravar sem trava (`ReservationService.java:84-122`).
- **O que está certo:** o SQL é todo com `?`, o script não achou SQL montado com `+`, as conexões vêm de um pool e são
  devolvidas por try-with-resources. Os dados continuam depois de reiniciar (3 de 3 no roteiro).

## Comportamento

**Números:** o script oficial (`measures/behavior.md`) não testou nada, pelo mesmo motivo do banco: subiu o
`byte-buddy-1.12.21.jar` e não o jar do projeto. Rodei o mesmo `check_05.sh` sem mexer no código: o projeto foi
copiado para `/tmp/alt`, e só o `main_jar` foi trocado para apontar para `target/mare-alta-1.0.0.jar`. Resultado: 76
de 83 conferências passaram. Por parte:
- a parte comum: 42 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 05: 30 de 32;
- as duas reservas ao mesmo tempo: 1 de 2.
**Nota:** com problemas

As falhas, e o que elas querem dizer para o dono da pousada:

- **"R$" seguido de um espaço não separável (U+00A0) e não do espaço comum.** Os bytes do texto são `R $ 302 240 7 7 9
  , 8 0`. Isso derrubou o total e o ISS da fatura no cenário e o total do comprovante no e-mail da parte comum. Na
  tela parece igual, mas o texto não é o que o dono mostrou: quem procurar ou comparar "R$ 779,80" não acha. Vem do
  `NumberFormat.getCurrencyInstance` de pt-BR (`IndividualGuest.java:13`, `CorporateGuest.java:14`). Os valores em
  si estão certos: o ISS dá 38,99.
- **Duas reservas para o mesmo quarto e as mesmas noites, criadas ao mesmo tempo (9 de 10 rodadas).** Se dois
  hóspedes reservarem pelo site no mesmo instante, a pousada vende o mesmo quarto duas vezes. Esse defeito pesa na
  coluna do banco.
- **`amountPaid` sai `0` e não `0.00`, antes da confirmação.** O texto pede o número com 2 casas. O mesmo acontece
  com `discount` na criação (conferido: `"discount":0`). É um detalhe para o site, mas foge do combinado.
- **Capacidade e diária do 201 vieram vazias.** O roteiro procura `number == 201` (número), e o agente devolve o
  número do quarto como texto, `"201"`. Os valores estão certos (`"capacity":3,"nightlyRate":389.90`). O texto não diz
  se o número é texto ou número, então essa falha não pesa.

O resto passa:
- os preços com temporada e o arredondamento;
- todos os códigos de erro;
- as transições de situação;
- as devoluções;
- os dois relatórios;
- os três documentos nos três lugares;
- as quatro recusas de identificação (nenhuma, CPF+CNPJ, CNPJ sem razão social, passaporte sem país).

Achei também, fora do roteiro, dois defeitos:
- `GET /reservations/abc` responde 500 em texto puro;
- um CPF com um CNPJ sem razão social é aceito, e o CNPJ some.

## Dúvidas

- **O jar.** O texto pede "um jar que sobe sozinho com `java -jar`". O `target/mare-alta-1.0.0.jar` sobe com
  `java -jar`, mas só enquanto `target/lib/` estiver ao lado, e essa pasta leva até as dependências de teste (JUnit,
  AssertJ, byte-buddy). Não está claro se isso cumpre o "sozinho". Foi essa montagem que fez os scripts de banco e de
  comportamento pegarem o jar errado. Por isso avaliei com o jar do projeto, medido à mão; os números das seções de
  banco e de comportamento vêm daí, e não dos arquivos em `measures/`, que dizem "não subiu".
- **A nota do banco** ficou entre `com problemas` e `ruim`. A lista do `ruim` na skill não cita a falta de proteção
  contra reserva duplicada, mas a skill põe em `com problemas` a proteção "só dentro do processo", e aqui não há
  nenhuma. A duplicação apareceu de fato em 9 de 10 rodadas, por isso dei `ruim`.
- **A forma do pattern:** um método polimórfico no tipo de hóspede (`GuestType.generateDocument`) e uma fábrica
  concentrada (`GuestTypeFactory`) que escolhe o tipo.
