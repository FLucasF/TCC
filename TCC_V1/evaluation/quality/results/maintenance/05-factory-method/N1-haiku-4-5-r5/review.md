# Revisão: manutenção, cenário 05

## Visão geral

O agente manteve a estrutura da base: `hotel.web` (Javalin; `HttpApi` lê o pedido e chama o serviço),
`hotel.reservations` (`ReservationService` com as regras, `ReservationStore` com o SQL por JDBC), `hotel.documents`
(o `ReservationReceipt`) e as migrações do Flyway no PostgreSQL. Para a mudança, ele pôs quatro campos de texto que
podem vir nulos no `Guest` e no `ReservationReceipt`, e uma migração `V2` com as colunas novas. O tipo de documento é
escolhido por um `if` dentro de `ReservationReceipt.text()`, que olha quais campos vieram nulos. O mais sério é que
`Database.java:30` agora roda o `Flyway.clean()` toda vez que o serviço sobe: **todas as reservas e mensagens somem a
cada reinício** (o roteiro do comportamento deu 0 de 3 no reinício). Também há um defeito no que o pedido descreve: o
JSON do documento (check-out e segunda via) continua com o título "Comprovante de reserva" e não traz o campo `text`,
então 17 das 32 conferências do cenário falharam.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos
da base (`measures/dependencies.md` e `measures/dependencies-base.md`). `HttpApi` foi de 170 para 215 linhas e
`ReservationReceipt` de 55 para 108.
**Nota:** com problemas

As camadas da base continuam: o JDBC só aparece em `reservations`, `messages`, `rooms` e na montagem, e o Javalin só
em `web` e `hotel`. O ciclo entre `documents` e `reservations` veio da base, e o agente não criou nenhum ciclo novo.
Os problemas são localizados e foram introduzidos pelo agente:
- A borda HTTP passou a montar o documento. Os cinco handlers de `HttpApi.java:64-89` buscam o quarto
  (`HttpApi.java:212-214`, uma cópia de `ReservationService.java:121-123`, com um `orElseThrow()` sem `HotelException`).
  Depois, `ReservationResponse.java:34-57` faz `new ReservationReceipt(...).text()` para qualquer reserva, até as
  pendentes. A emissão do documento fica assim em dois lugares: no serviço e na camada web.
- A regra "exatamente um jeito de identificar o hóspede" é uma regra de negócio, mas ficou no handler
  (`HttpApi.java:141-159`), e nada no domínio a protege: `Guest.java:3-14` aceita qualquer combinação.
- A montagem (`Database.java:29-34`) ganhou uma operação destrutiva. O efeito dela está nas colunas do banco e do
  comportamento.

## Design de baixo nível

**Números:** há 3 tipos de documento (comprovante, fatura, booking receipt), e nenhum virou um tipo próprio. Os três
são métodos privados do mesmo record, escolhidos por um `if/else if/else` (`ReservationReceipt.java:49-57`). Os três
lugares que emitem o documento continuam com `new ReservationReceipt(...)` à mão (`ReservationService.java:64`,
`:109`, `:118`), e há um quarto lugar novo, em `ReservationResponse.java:56`. Não existe fábrica nem método
polimórfico.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O pedido quer que a escolha do documento more num lugar só e que caiba o próximo caso (a agência). O
agente não concentrou a *criação*. Há um único tipo, `ReservationReceipt`, que sempre se chama "Comprovante de
reserva" (`ReservationReceipt.java:31`). A variação ficou num `if` sobre campos nulos dentro de `text()`
(`ReservationReceipt.java:50-56`). O que aproxima do pattern: o `if` está num lugar só, não repetido nos três pontos
de emissão. O que afasta: não há criador, nem fábrica, nem `guest.issueDocument(...)`. O "tipo de hóspede" não existe
como tipo: são quatro `String` que podem ser nulas (`Guest.java:7-10`) e a ordem dos testes de nulo. O objeto criado
é sempre o mesmo, e por isso o título e o resto do JSON do documento não mudam com o tipo (veja o comportamento). Para
pôr a agência, seria preciso mexer no record `Guest`, no record `ReservationReceipt`, na cadeia de `if`, na validação
de `HttpApi` e no `ReservationResponse`.

**O exagero.** Nenhuma estrutura sem necessidade. Pelo contrário, falta estrutura.

**O resto do código:**
- Exceções engolidas: `catch (Exception e) { // Continue even if clean fails }` em volta de um `clean()` destrutivo
  (`Database.java:29-33`), e `catch (Exception e) { // Continue even if migration fails }` no apoio dos testes
  (`EmptyDatabase.java:28-32`). Se a migração falhar, os testes seguem sem dizer por quê.
- Duplicação: `invoiceText` (`ReservationReceipt.java:75-90`) copia quase linha por linha o `standardReceiptText`
  (`:59-73`). Os cinco handlers de `HttpApi.java:64-89` repetem o mesmo trecho de três linhas.
- Código morto: o construtor `ReservationResponse(Reservation)` (`ReservationResponse.java:59`) não tem mais nenhum
  uso.
- Arredondamento: o ISS usa `RoundingMode.HALF_UP` (`ReservationReceipt.java:76-77`). O pedido diz "arredondado para
  centavos como sempre", e o sistema arredonda meio para o par (`PriceCalculator.java:50-52`, `cents`). Com um total
  de R$ 100,10, o ISS sai R$ 5,01 em vez de R$ 5,00.
- A fatura não mostra a linha de desconto (`ReservationReceipt.java:85-88`). Numa reserva `NON_REFUNDABLE`, as
  diárias e o total aparecem diferentes sem explicação.

Os defeitos estão espalhados por seis arquivos, e as exceções engolidas escondem justamente a operação que apaga o
banco. Por isso a nota é `ruim`.

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 30 métodos (a base tinha 26, todos mantidos;
os 4 novos estão em `HttpApiTest`). A cobertura de linhas foi de 93,8% para 90,3% (557 de 617) e a de ramos de 87,1%
para 84,2% (133 de 158). `hotel.documents` ficou com 69 de 70 linhas e 13 de 18 ramos (`measures/tests.md` e
`measures/tests-base.md`).
**Nota:** com problemas

Os testes da base continuam passando, e o agente testou as recusas novas com o código de erro: nenhuma identificação
(`HttpApiTest.java:158-167`) e CPF com CNPJ juntos (`:170-181`). Ele não testou CNPJ sem razão social, nem passaporte
sem país. Os testes da parte nova conferem quase nada do documento. A fatura e o booking receipt só são verificados
com `contains("Fatura de hospedagem")` e `contains("Booking receipt")` (`HttpApiTest.java:137`, `:154`), e só na
resposta da criação.

Pela pergunta da mutação, nenhum teste falharia se alguém:
- trocasse o 0.05 do ISS por 0.50;
- formatasse o dinheiro em inglês com `Locale` brasileiro;
- trocasse `ISO_LOCAL_DATE` por `dd/MM/yyyy`;
- apagasse a linha "Discount:";
- trocasse "night" por "nights".

Também não há teste do documento no check-out, na segunda via ou no e-mail para empresa e estrangeiro, que é
exatamente onde o comportamento falha (o JSON do documento sem `text`). Além disso, `EmptyDatabase.java:20-32` passou
a apagar o histórico do Flyway e a engolir falhas de migração, o que enfraquece a rede dos testes. E nenhum teste
percebe que o serviço apaga o banco ao subir, porque os testes já começam esvaziando o banco.

## Banco

**Números:** 3 tabelas, com chave primária em todas. O dinheiro está em `numeric(10,2)`, as datas em `date`, a chave
estrangeira tem índice e o `EXCLUDE` contra reserva dupla continua no lugar. Há 2 migrações: a `V1` ficou igual à da
base e a `V2` é nova. O script só acusou as colunas de situação e de tipo sem `CHECK`, que já vinham da base
(`measures/database.md`).
**Nota:** ruim

A migração nova está correta na forma: é um arquivo novo, sem editar a `V1`
(`V2__add_company_and_passport_fields.sql:2-6`), e o SQL do `ReservationStore` continua com parâmetros
(`ReservationStore.java:32-55`, `:130-137`). Mesmo assim, o agente acrescentou em `Database.java:30`
`Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean()`, que roda toda vez que o serviço sobe
(é o construtor de `Database`, usado pelo `main`). O `clean()` derruba todas as tabelas do esquema, e o `migrate()`
cria tudo de novo, vazio. O roteiro do comportamento confirma: depois de reiniciar, a reserva dá 404, as mensagens são
0 e o quarto volta a ficar livre. Na pousada, cada reinício (um deploy, uma queda de luz) apaga todas as reservas.

Há um defeito menor: a `V2` tira o `NOT NULL` de `guest_cpf` (`V2__add_company_and_passport_fields.sql:2`), mas não
põe nenhum `CHECK` que exija exatamente uma identificação. O banco aceita uma reserva sem documento nenhum.

## Comportamento

**Números:** 63 de 83 conferências passaram (`measures/behavior.md`):
- parte comum: 46 de 46;
- reinício: 0 de 3;
- cenário 05: 15 de 32;
- reservas simultâneas: 2 de 2.

**Nota:** ruim

- **Os dados somem ao reiniciar (0 de 3).** É o efeito do `clean()` de `Database.java:30`. Para o dono, isso
  significa perder todas as reservas, os pagamentos registrados e os e-mails a cada vez que o serviço sobe de novo.
- **O documento no check-out e na segunda via não mudou (17 falhas).** O `ReservationReceipt` é um record, e o
  `text()` não é um componente dele, então o Jackson não o põe no JSON. O título continua fixo em "Comprovante de
  reserva" (`ReservationReceipt.java:31`). Confirmei chamando a API: a segunda via de uma reserva de empresa volta com
  `"title":"Comprovante de reserva"` e sem `text`. Por isso falharam:
  - o texto do comprovante com CPF;
  - "o documento é a fatura" e "o documento é o booking receipt";
  - as cinco linhas da fatura, entre elas o ISS de R$ 38,99;
  - as oito linhas do booking receipt;
  - "o check-out devolve a fatura".

  O pedido dizia que o documento ganharia o campo `text`. O agente pôs o `text` na resposta da *reserva*
  (`ReservationResponse.java:32`, `:56`). Lá o texto está certo: a fatura sai com "ISS retido (5%): R$ 38,99", e o
  e-mail de confirmação da empresa leva a fatura (essa conferência passou). Mas o site e a recepção, quando pedem a
  segunda via ou fazem o check-out, recebem um documento que não diz se é fatura ou booking receipt e não traz o
  texto.

O que funciona: a parte comum inteira, as quatro recusas de identificação (nenhuma, duas, CNPJ sem razão social,
passaporte sem país), os campos nulos que não se aplicam e as reservas simultâneas.

## Dúvidas

- **Omissão ou malfeito.** A escolha do documento está num único `if` (`ReservationReceipt.java:49-57`), não repetida
  nos três lugares. Mas não há fábrica nem tipo de documento: o objeto criado é sempre o mesmo, e só o texto varia
  depois. Escolhi `omissão` porque não existe a estrutura que o `malfeito` pressupõe. Uma leitura mais generosa
  poderia ver o `if` concentrado como uma "fábrica" rudimentar com a escolha feita depois da criação, e aí a classe
  seria `malfeito`.
- **A linha de desconto na fatura.** O pedido não diz se a fatura deve ter a linha de desconto ("parecida com o
  comprovante"), e o roteiro não confere isso. Contei a ausência só como ponto menor de design.
