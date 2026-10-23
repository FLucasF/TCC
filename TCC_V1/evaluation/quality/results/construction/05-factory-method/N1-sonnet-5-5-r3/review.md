# Revisão: construção, cenário 05

## Visão geral

O projeto tem seis pacotes: `pousada` (o `App`, o `ReservationService`, a `BusinessException` e o `ErrorCode`), `pousada.api` (as rotas do Javalin, a leitura do corpo e as views em JSON), `pousada.domain` (a reserva como `record` imutável, o preço, a temporada, os planos, os quartos fixos no código), `pousada.identity` (as três formas de identificação e o documento), `pousada.persistence` (o pool Hikari, a transação e dois repositórios com JDBC) e `pousada.report` (os dois CSV). Uma requisição entra em `Routes` (`api/Routes.java:30-46`), o corpo é validado em `ReservationRequest.parse` (`api/ReservationRequest.java:25-43`), o serviço abre uma transação (`ReservationService.java:35-42`, `59-70`) e os repositórios gravam em duas tabelas (`reservations` e `messages`) criadas por `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:12-40`). O documento por tipo de hóspede é resolvido por polimorfismo na identidade (`identity/GuestIdentity.java:11-30`), e os três lugares que emitem o documento passam todos por `ReservationDocument.text` (`identity/ReservationDocument.java:18-33`). O comportamento está completo: 83 de 83 conferências passaram, e os 12 testes do agente passam. Os pontos fracos são estruturais e não aparecem pela API: quatro ciclos entre pacotes, que vêm de três peças no lugar errado, e o banco sem migrações, sem chave estrangeira para o quarto e sem nenhuma restrição.

## Arquitetura

**Números:** 6 pacotes, 14 dependências entre eles, 4 ciclos (`pousada`↔`pousada.api`; `pousada`→`persistence`→`identity`→`domain`→`pousada`; `pousada`→`persistence`→`identity`→`pousada`; `domain`↔`identity`). O Javalin só aparece em `pousada` (o `App`) e em `pousada.api`, e o JDBC só em `pousada` (por causa do `Database` recebido) e em `pousada.persistence`. A maior classe tem 177 linhas (`ReservationRepository`).
**Nota:** com problemas

As camadas existem e estão limpas. A borda HTTP é fina: cada handler lê, chama o serviço e escreve (`api/Routes.java:28-58`). As regras de preço, de situação e de devolução moram no domínio, sem Javalin e sem SQL (`domain/Reservation.java:40-58`, `domain/Quote.java:9-17`, `domain/RatePlan.java:17-23`). O SQL fica todo em `persistence` (`persistence/ReservationRepository.java:31-122`), e a montagem acontece num lugar só (`App.java:12-16`).

Os ciclos vêm de três peças no lugar errado:
- o `App` (a montagem) mora no mesmo pacote que o serviço que as rotas usam, e daí sai o ciclo `pousada`↔`api` (`App.java:4`, `api/Routes.java:7`);
- a `BusinessException` e o `ErrorCode` moram no pacote raiz, e o domínio e a identidade dependem deles (`domain/Reservation.java:3-4`, `identity/IdentityKind.java:3-4`), o que fecha os ciclos que passam por `persistence`;
- `domain` e `identity` dependem um do outro: a reserva guarda a `GuestIdentity` (`domain/Reservation.java:19`), o `ReservationDocument`, em `identity`, lê a `Reservation` (`identity/ReservationDocument.java:3`), e o `ConfirmationEmail`, em `domain`, chama o `ReservationDocument` (`domain/ConfirmationEmail.java:3,15`).

Mover o `App` para um pacote próprio e as exceções para o domínio desfaz três dos quatro ciclos. O quarto, entre `domain` e `identity`, é de responsabilidade: o documento precisa da reserva, e a reserva precisa da identidade.

Duas misturas menores. O serviço cria os repositórios por dentro (`ReservationService.java:20-21`). A regra "segunda via só de reserva paga" é chamada direto do handler (`api/Routes.java:43-44`) em vez de passar pelo serviço, como fazem as outras operações.

## Design de baixo nível

**Números:** 3 de 3 tipos de hóspede na estrutura (`CpfIdentity`, `CnpjIdentity`, `PassportIdentity`, que implementam `GuestIdentity`); 3 de 3 lugares de emissão (o e-mail de confirmação, o check-out e a segunda via) pedem o documento a `ReservationDocument`; 0 `if`/`switch` sobre o tipo de hóspede fora da estrutura.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A escolha do documento é polimórfica no próprio tipo de hóspede. Cada identidade diz o título, o idioma, as linhas de quem é o hóspede e as linhas do rodapé (`identity/GuestIdentity.java:18-29`):
- a fatura acrescenta a empresa e o ISS (`identity/CnpjIdentity.java:28-41`);
- o booking receipt escolhe o inglês (`identity/PassportIdentity.java:19-31`).

Os três lugares usam a mesma função:
- o e-mail (`domain/ConfirmationEmail.java:15`);
- o check-out e a segunda via, pelo `Views.document` (`api/Routes.java:40,45`, `api/Views.java:50,62`).

Nenhum deles pergunta qual é o tipo. A criação da identidade a partir do pedido também fica num lugar só: o `enum IdentityKind` guarda os campos e a fábrica de cada forma (`identity/IdentityKind.java:14-31`). A validação de "uma e só uma" forma de identificação (`identity/IdentityKind.java:55-77`) e a projeção dos cinco campos com `null` (`identity/IdentityKind.java:43-49`) são genéricas. Uma agência entra com uma classe nova e uma linha no `enum`.

O que afasta um pouco da forma clássica: o documento não é um objeto. É um texto montado por um esqueleto fixo (`identity/ReservationDocument.java:21-32`) com ganchos da identidade. Um documento de agência com estrutura muito diferente teria de caber nesses ganchos.

**O exagero.** Não há estrutura sem necessidade. O `DocumentLanguage` tem duas línguas de fato, o `IdentityKind` serve à leitura do pedido e ao banco, e o `SqlWork` serve à transação. Nenhuma interface tem uma implementação só que nada substitua.

**O resto.** O dinheiro é `BigDecimal` com `HALF_EVEN` (`domain/Money.java:13-15`), arredondado a cada passo (`domain/Season.java:24`, `domain/RatePlan.java:18`, `domain/Quote.java:16`). As datas são `LocalDate`, as situações, os planos e os tipos são `enum`. A reserva é imutável e cada transição confere a situação num lugar só (`domain/Reservation.java:77-82`). Não há `static` mutável. Observações menores, que não mudam a nota:
- o construtor de `DocumentLanguage` recebe 11 textos posicionais (`identity/DocumentLanguage.java:14-19,33-35`), fácil de trocar um pelo outro;
- os rótulos ficam divididos entre a língua e as identidades ("Hóspede:" e "Guest:" estão escritos à mão em `identity/CpfIdentity.java:30` e `identity/PassportIdentity.java:30`);
- a identidade junta duas responsabilidades, a identificação e o documento, inclusive a alíquota do ISS (`identity/CnpjIdentity.java:10`).

## Testes

**Números:** 12 testes rodaram, 0 falharam, 0 erros, 0 pulados; 12 métodos escritos (10 em `DocumentsTest`, de unidade, e 2 em `ApiTest`, ponta a ponta contra o PostgreSQL de `DB_URL`); cobertura de 92,3% das linhas (493 de 534) e de 76,4% dos ramos (81 de 106); `domain` com 79 de 79 linhas e `identity` com 94 de 97.
**Nota:** bom

Os testes conferem as regras com valores:
- a temporada com o exemplo do texto, 599,76 (`src/test/java/pousada/DocumentsTest.java:23-27`), e as bordas de 14/12, 15/12, 29/02 e 01/03 (`DocumentsTest.java:39-45`);
- o "meio para o par" no desconto, 86,985 que vira 86,98 (`DocumentsTest.java:30-36`);
- as devoluções nos três casos (`DocumentsTest.java:116-126`).

A parte nova está testada pelo texto inteiro:
- o comprovante (`DocumentsTest.java:48-58`);
- a fatura com o ISS de R$ 38,99 (`DocumentsTest.java:61-73`);
- o booking receipt com "1 night", desconto e BRL (`DocumentsTest.java:76-87`);
- o separador de milhar nas duas línguas (`DocumentsTest.java:90-93`);
- o e-mail em volta do documento (`DocumentsTest.java:96-104`);
- a regra de "uma e só uma" identificação (`DocumentsTest.java:107-113`).

A pergunta da mutação: trocar 0,05 por 0,5 no ISS, `== 1` por `> 1` no singular, ou apagar a linha do desconto quebra um teste. O `ApiTest` passa pelo fluxo inteiro com os códigos de status, os dois CSV e a reserva cancelada que libera o quarto (`src/test/java/pousada/ApiTest.java:80-144`).

Lacunas pequenas:
- pela API, só o comprovante do CPF é conferido nos três lugares; a fatura e o booking receipt dentro do e-mail e no check-out ficam só na unidade;
- as duas reservas simultâneas não têm teste;
- os testes de API são dois métodos longos com muitas asserções (`ApiTest.java:81-115`), o que dificulta saber o que quebrou;
- o `ApiTest` se desliga sozinho sem `DB_URL` (`ApiTest.java:35`); aqui ele rodou.

## Banco

**Números:** 2 tabelas (`reservations`, com 17 colunas e 16 `NOT NULL`, e `messages`, com 5 e 5), as duas com chave primária; dinheiro todo em `numeric(12,2)`; datas em `date`; 0 chaves estrangeiras, 0 `CHECK`, 0 `UNIQUE`/`EXCLUDE`; esquema criado por `CREATE TABLE` no código, 0 arquivos de migração; 1 transação explícita, 1 `FOR UPDATE`, pool Hikari.
**Nota:** com problemas

Os dados ficam certos e no banco, e o SQL é todo com parâmetros: os trechos montados com `+` só concatenam a constante `COLUMNS` (`persistence/ReservationRepository.java:27-28,95,99,104,115`). As conexões são devolvidas por try-with-resources (`persistence/Database.java:73`). A confirmação e o e-mail são gravados na mesma transação (`ReservationService.java:59-70`). A duplicação de reserva é impedida entre processos por uma trava consultiva por quarto dentro da transação (`persistence/ReservationRepository.java:31-36`, `ReservationService.java:36-37`). As transições travam a linha com `FOR UPDATE` (`persistence/ReservationRepository.java:98-100`).

Os defeitos:
- **Sem migrações.** O esquema nasce de `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:12-40,61`), que não sabe acrescentar uma coluna numa tabela que já existe. O próximo pedido que mudar o banco vai precisar de remendo à mão.
- **O quarto não existe no banco.** Os quartos estão fixos em `domain/Rooms.java:8-13`, então `reservations.room_number` não tem chave estrangeira (`persistence/Database.java:15`).
- **Nada protege as regras no banco.** Não há `CHECK` em `status`, `rate_plan` nem `identity_kind`, e não há `EXCLUDE` contra noites sobrepostas: a regra só vale para quem passa pela trava do serviço.
- **A identificação é um JSON dentro de uma coluna `TEXT`** (`persistence/Database.java:19`, `persistence/ReservationRepository.java:63,150,161-176`). Não dá para consultar nem restringir o CPF ou o CNPJ pelo banco.
- **A trava consultiva usa o número do quarto como chave** (`persistence/ReservationRepository.java:33`), num espaço de chaves que todo o banco compartilha. É um ponto menor.

## Comportamento

**Números:** 83 de 83 conferências passaram, 0 falharam: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário 05 32 de 32, as duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

Tudo o que o texto pede funcionou pela API, a partir de um banco vazio:
- os preços com a temporada e o "meio para o par" (599,76; desconto de 37,48);
- todas as recusas com o código certo;
- a confirmação com o e-mail;
- o check-in e o check-out;
- as devoluções;
- os dois relatórios, inclusive a reserva não reembolsável cancelada que continua contando;
- os dados que continuam lá depois de reiniciar.

Na parte do cenário:
- a fatura sai com a empresa e o ISS de 5%;
- o booking receipt sai em inglês, com BRL, as datas em ISO, "1 night" e o milhar com vírgula;
- o e-mail continua em português com o documento certo dentro;
- o check-out devolve o documento do tipo do hóspede;
- as recusas de identificação ausente, dupla ou incompleta respondem 400.

Nas dez rodadas de duas reservas simultâneas, nenhum quarto ficou com duas reservas. Para o dono da pousada, o sistema faz o que ele descreveu.

## Dúvidas

- A forma do pattern é o método polimórfico no próprio tipo de hóspede (`GuestIdentity` com título, idioma, linhas e rodapé), com um esqueleto único de texto em `ReservationDocument`. Ela não é o criador clássico com o método de fábrica sobrescrito. A referência aceita essa forma como acerto. Fica em aberto se uma agência com um documento de estrutura muito diferente caberia nos ganchos sem mexer no esqueleto.
- Na arquitetura, há quatro ciclos pelo `tsort`, o que se aproxima do "vários ciclos" da nota `ruim`. Dei `com problemas` porque três deles vêm só de onde moram o `App` e a `BusinessException`, e as camadas em si (HTTP, regras, dados) estão separadas.
