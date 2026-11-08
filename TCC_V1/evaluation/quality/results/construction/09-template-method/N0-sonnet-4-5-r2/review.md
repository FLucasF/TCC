# Revisão: construção, cenário 09

## Visão geral

O agente montou um serviço Javalin em sete pacotes: `controller` (os handlers), `service` (as regras da reserva e o
preço), `report` (os quatro relatórios CSV, com uma classe base `BaseReport`), `repository` (JDBC puro, com
`DriverManager`), `model`, `util` e o `Main`, que registra as rotas e traduz as exceções em códigos HTTP. Uma
requisição de reserva passa por `ReservationController.createReservation` (lê o JSON à mão), por
`ReservationService.createReservation` (quarto, capacidade, conflito, preço) e por `ReservationRepository.insert`. Os
quartos ficam numa lista fixa em memória (`repository/RoomRepository.java:10-16`); as reservas e as mensagens ficam em
duas tabelas criadas com `CREATE TABLE IF NOT EXISTS` no código (`repository/Database.java:24-63`). O mais sério:
a coluna `guest_cpf` é `VARCHAR(11)` (`repository/Database.java:31`), e o CPF no formato que o próprio texto mostra
("111.222.333-44") não cabe nela. Toda reserva criada assim falha, e o erro some num `catch (Exception)`
(`controller/ReservationController.java:44-46`) que devolve 400 `INVALID_REQUEST`. O mesmo `catch` transforma
também `ROOM_NOT_FOUND`, `ROOM_UNAVAILABLE` e `CAPACITY_EXCEEDED` em 400. Além disso, nada impede duas reservas
simultâneas do mesmo quarto: com o CPF só com dígitos, em 10 rodadas de dois pedidos ao mesmo tempo, as 10 criaram as
duas reservas. Os quatro relatórios fazem a conta certa quando há reservas, mas o "esqueleto" comum é só um conjunto
de utilitários de CSV.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Só `controller` e o
`Main` usam Javalin, e só `repository` usa JDBC. As maiores classes são `ReservationRepository` (212 linhas) e
`ReservationService` (192).
**Nota:** com problemas

As camadas existem e as dependências seguem a direção certa: as rotas não têm SQL, as regras não conhecem o Javalin
(`service/ReservationService.java` não importa `io.javalin`) e o SQL fica em `repository/`. Os defeitos estão na
montagem e em alguns pontos que fogem da separação:

- Não há montagem num lugar só. Cada classe cria as suas dependências por dentro: `ReservationService` faz `new` de
  três repositórios e do `PricingService` (`service/ReservationService.java:22-27`), os controllers fazem `new` de
  repositório e de serviço (`controller/RoomController.java:15-17`, `controller/MessageController.java:15-17`,
  `controller/ReservationController.java:19-21`), e cada relatório cria o seu repositório
  (`report/OccupancyReport.java:15-18`, `report/TourismTaxReport.java:17-20`). O acesso ao banco depende de estado
  `static` global (`repository/Database.java:9-11`). O `Main` só liga os controllers (`Main.java:25-28`). Por isso não
  dá para trocar uma peça nem testá-la isolada.
- A borda HTTP fala direto com os dados: `controller/ReservationController.java:113-114` faz
  `new RoomRepository().findByNumber(...).get()` dentro do mapeamento do comprovante, e `RoomController` e
  `MessageController` leem o repositório sem passar por serviço nenhum.
- O comprovante é montado em dois lugares, em camadas diferentes: o texto em `service/ReservationService.java:138-166`
  e o JSON em `controller/ReservationController.java:106-122`. A regra "segunda via só para reserva paga" não está em
  nenhum dos dois (`controller/ReservationController.java:79-83` chama só `getReservation`).
- O `controller` depende de `report` e cria cada relatório com `new` dentro do handler
  (`controller/ReportController.java:25,40,55,70`). Isso é aceitável num sistema deste tamanho.

Não há ciclo nem classe gigante. O problema é a montagem espalhada e a borda que vai direto aos dados.

## Design de baixo nível

**Números:** 4 relatórios, os 4 são subclasses de `BaseReport`, mas os 4 repetem a sequência inteira (título →
cabeçalho → consulta → linhas → total → `super.generate()`). A classe base tem 3 utilitários de linha e um `generate()`
que só junta as linhas; nenhum passo é abstrato e não há método-modelo.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede o esqueleto (título, cabeçalho, linhas, total) num lugar só, para que "quando o jeito dos
relatórios mudar, mude em todos de uma vez". `report/BaseReport.java:11-25` oferece só `addTitleLine`,
`addHeaderLine`, `addDataLine` e um `generate()` que faz `String.join`. Cada relatório tem o seu próprio
`generate(...)` com outra assinatura e chama os passos na ordem, à mão:

- `report/OccupancyReport.java:20-41`;
- `report/RevenueReport.java:19-48`;
- `report/TourismTaxReport.java:22-53`;
- `report/RatePlansReport.java:20-67`.

Os quatro repetem `addTitleLine(...)` e `addHeaderLine(...)` no começo, `addDataLine("total", ...)` no fim e
`return super.generate()`. Isso é o caso da referência: "o 'esqueleto' é só uma função utilitária de CSV e cada
relatório ainda repete a sequência dos passos".

O que aproxima do pattern: os quatro relatórios passam pela mesma classe base, o nome da pousada está num lugar só
(`report/BaseReport.java:7`), e uma linha de rodapé com a data poderia ser posta em `BaseReport.generate()`. O que
afasta:

- a ordem dos passos não é garantida por ninguém: um quinto relatório pode esquecer o título ou o total;
- não há passos abstratos (`header()`, `rows()`, `total()`);
- o período é formatado à mão em três classes (`String.format("%04d-%02d", ...)` em `RevenueReport.java:20`,
  `TourismTaxReport.java:23` e `RatePlansReport.java:22`);
- o `generate()` público sem argumento da base (`BaseReport.java:23`) devolve texto vazio se for chamado direto;
- o estado `lines` é mutável e protegido (`BaseReport.java:9`): chamar `generate` duas vezes no mesmo objeto
  duplica as linhas.

**Exagero.** Não há interface de uma implementação só, fábrica nem camada que só repassa. Contenção.

**O resto do código.**

- `catch (Exception e)` em `controller/ReservationController.java:44-46` engole todas as exceções de domínio e as de
  SQL e responde 400 `Invalid request`, escondendo a causa. É esse `catch` que transforma a falha do CPF (coluna do
  banco) e as recusas 404, 409 e 422 em 400 (ver Comportamento).
- Menos de um hóspede é tratado como `CapacityExceededException` (`service/ReservationService.java:36-38`). O texto
  pede 400 para isso; o 400 só sai por acaso, por causa do `catch` acima.
- Duplicação:
  - o "dinheiro que fica com a pousada" (pago menos devolvido, só se for maior que zero) está copiado em
    `report/RevenueReport.java:27-32` e `report/RatePlansReport.java:35-40`;
  - a leitura e a validação do mês estão três vezes em `controller/ReportController.java:32-75`;
  - o comprovante é montado duas vezes (`service/ReservationService.java:138-166` e
    `controller/ReservationController.java:106-122`).
- Valores mágicos: a taxa de turismo de R$ 4,50 é um literal dentro do método
  (`service/PricingService.java:50`), embora o texto avise que "a prefeitura muda o valor da taxa todo ano".
- Dinheiro sem a escala certa: `BigDecimal.ZERO` com escala 0 em `model/Reservation.java:26`,
  `service/PricingService.java:42` e `service/ReservationService.java:107`, por isso `amountPaid`, `discount` e
  `refund` saem como `0` em vez de `0.00`.
- O formatador de CSV faz um `replace('.', ',').replace(',', '.')` que não faz nada (`util/MoneyUtils.java:17`), e há um
  import sem uso (`report/RevenueReport.java:4`).
- `Optional.get()` sem cuidado em `report/OccupancyReport.java:29`, `controller/ReservationController.java:114` e
  `service/ReservationService.java:139`.
- O que está certo: `BigDecimal` com `HALF_EVEN` a cada passo (`util/MoneyUtils.java:8-10`), `LocalDate` para as datas,
  `enum` para situação, plano e tipo de quarto, e o tipo de quarto carregando capacidade e diária
  (`model/RoomType.java:5-24`).

São defeitos localizados, mas o `catch (Exception)` é sério.

## Testes

**Números:** 19 testes rodaram, 0 falhas, 0 erros, 0 pulados; 19 métodos escritos em 3 arquivos. Cobertura: linhas
330 de 738 (44,7%), ramos 45 de 106 (42,5%). Por pacote: `report` 0 de 109 linhas, `controller` 0 de 169, `Main` 0
de 50, `service` 115 de 122, `util` 19 de 26 (`measures/tests.md`).
**Nota:** com problemas

O que está testado com valores:

- a temporada nas fronteiras (`DateUtilsTest.java`: 14/12 fora, 15/12 dentro, março fora);
- o preço da noite (`PricingServiceTest.java:183-191`, R$ 299,88) e a soma das noites do exemplo do texto (`:193-202`,
  R$ 599,76);
- o desconto não reembolsável com arredondamento (`:204-211`, R$ 89,96; `IntegrationTest.java:47-58`, R$ 116,97 e
  R$ 662,83);
- a taxa de turismo do exemplo do texto (`PricingServiceTest.java:222-227`, R$ 27,00);
- o conflito de quarto (`IntegrationTest.java:60-75`), a capacidade (`:77-86`), o fluxo das situações (`:118-136`), as
  transições proibidas (`:138-155`) e a devolução nos dois planos (`:88-116`).

O que falta:

- **A parte nova do cenário quase não tem teste.** O pacote `report` tem 0% de cobertura: nenhum teste lê um
  relatório.
  - Se alguém trocasse `"tourism-tax"` por outro nome no título (`report/TourismTaxReport.java:23`), nenhum teste
    falharia.
  - O mesmo se tirasse o filtro de reservas pagas (`report/TourismTaxReport.java:30-32`).
  - O mesmo se trocasse `> 0` por `>= 0` no relatório por plano (`report/RatePlansReport.java:40`), ou se a não
    reembolsável cancelada deixasse de contar.
  - Só a conta da taxa por hóspede e noite tem rede.
- **Nenhum teste passa pela API.** `controller` e `Main` têm 0%. Por isso nem o `catch (Exception)` que derruba os
  códigos 404, 409 e 422 nem o CPF com máscara foram vistos. Todos os testes usam CPF só com dígitos
  (`IntegrationTest.java:30`).
- **Os testes rodam em H2, não no PostgreSQL** (`IntegrationTest.java:23`; `pom.xml:52` com `h2`). O banco real não é
  exercitado.
- Ficaram de fora: o e-mail e o texto do comprovante, a segunda via, o cancelamento de reserva pendente e as reservas
  simultâneas.

Os testes passam e conferem com valores a parte do preço, mas deixam sem teste quase toda a parte nova e toda a borda
HTTP.

## Banco

**Números:** 2 tabelas (`reservations` com 16 colunas, `messages` com 5), ambas com chave primária. Dinheiro em
`numeric(10,2)` e datas em `date`, ok. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. Esquema criado por `CREATE
TABLE` no código, sem migração. 0 pool, 0 transações, 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** ruim

- **Um tipo errado que impede gravar as reservas.** `guest_cpf VARCHAR(11)` (`repository/Database.java:31`) só aceita
  o CPF sem pontuação. O texto mostra o CPF como "111.222.333-44", e o roteiro manda assim. O `INSERT` estoura, e a
  reserva nunca chega ao banco. Confirmei chamando a API: o mesmo corpo com `"guestCpf":"111.222.333-44"` volta 400, e
  com `"11122233344"` volta 201.
- **Nada protege a regra "um quarto, uma reserva por noite".** O serviço consulta os conflitos
  (`repository/ReservationRepository.java:108-135`) e depois insere (`service/ReservationService.java:44-73`), em
  conexões separadas, sem transação, sem trava e sem restrição `EXCLUDE` no banco. Com CPF só com dígitos, disparei
  10 rodadas de dois pedidos simultâneos para o 301 nas mesmas noites: as 10 criaram as duas reservas. É dado errado,
  não só um risco.
- **Sem tabela de quartos nem chave estrangeira.** Os quartos ficam em memória (`repository/RoomRepository.java:10-16`),
  e `reservations.room_number` não aponta para nada. Também não há CHECK em `status` e `rate_plan`.
- **Sem como evoluir.** `CREATE TABLE IF NOT EXISTS` (`repository/Database.java:25-54`) não acrescenta coluna numa
  tabela que já existe; o próximo pedido (a data no fim dos relatórios, hóspedes por estado) vai exigir mexer à mão no
  banco de quem já roda.
- **Conexões.** `DriverManager.getConnection` a cada chamada (`repository/Database.java:20-21`), sem pool. As conexões
  são fechadas com try-with-resources, mas os `ResultSet` de `getGeneratedKeys` e de `executeQuery` ficam fora do
  bloco (`repository/ReservationRepository.java:38,96`), o que pesa pouco porque a conexão fecha logo depois.
- **Carga.** Os relatórios mensais filtram com `EXTRACT(YEAR/MONTH FROM check_in)`
  (`repository/ReservationRepository.java:140-141`), o que impede o uso de índice; o relatório de ocupação faz uma
  busca do quarto por linha (`report/OccupancyReport.java:29`, em memória, sem custo de banco).
- **O que está certo.** O SQL é todo parametrizado com `?`, e o script não achou SQL montado com `+`.

O tipo do CPF faz o banco recusar as reservas no formato do texto, e as reservas simultâneas duplicam sem nada que
impeça: a nota é `ruim`.

## Comportamento

**Números:** 21 de 64 conferências passaram (`measures/behavior.md`):

- a parte comum: 12 de 46;
- os dados depois de reiniciar: 1 de 3;
- o cenário 09: 7 de 13;
- as reservas simultâneas: 1 de 2.

**Nota:** ruim

**O que derruba quase tudo.** `POST /reservations` com o CPF "111.222.333-44", o formato que o texto usa, responde 400
`INVALID_REQUEST` "Invalid request". A causa está na coluna `VARCHAR(11)`, que pesa na coluna do banco, mais o
`catch (Exception)` que esconde o erro. Sem reserva criada, falham em cascata:

- a confirmação, o e-mail, o check-in e o check-out;
- os cancelamentos e as devoluções;
- as linhas da ocupação e do financeiro;
- os dados depois de reiniciar;
- as linhas da taxa de turismo e do relatório por plano.

Para o dono da pousada, isso quer dizer que o site não consegue reservar quando o hóspede digita o CPF com pontos e
traço.

**Falhas que não vêm do CPF**, conferidas por mim na API com o CPF só com dígitos:

- Os códigos de erro da criação estão errados. Quarto que não existe, quarto já ocupado e hóspedes demais respondem
  todos 400 `INVALID_REQUEST` em vez de 404 `ROOM_NOT_FOUND`, 409 `ROOM_UNAVAILABLE` e 422 `CAPACITY_EXCEEDED`
  (`controller/ReservationController.java:42-46`). O site não consegue dizer ao hóspede que o quarto está ocupado.
- A segunda via de reserva pendente responde 200 com o comprovante (`controller/ReservationController.java:79-83`). O
  texto diz que ela só existe para reserva paga.
- Duas reservas do mesmo quarto ao mesmo tempo: as duas são criadas em 10 de 10 rodadas. O "OK" do roteiro nessa parte
  só passou porque as 20 tentativas voltaram 400. Para a pousada, é quarto vendido duas vezes.
- `amountPaid`, `discount` e `refund` saem como `0` em vez de `0.00` quando são zero (`model/Reservation.java:26`). O
  texto pede número com 2 casas.
- `GET /rooms` devolve `number` como texto (`"201"`), e o roteiro procura o número 201. O texto não fixa o tipo, então
  essas duas conferências (capacidade e diária do 201) vêm de uma escolha razoável e não pesam.

**O que funciona, conferido à mão.** Com o CPF só com dígitos:

- o preço da alta temporada (R$ 599,76) e o desconto não reembolsável com arredondamento;
- o e-mail com o comprovante no texto pedido;
- a devolução zero da não reembolsável cancelada;
- a ocupação (`total,1/5`) e o financeiro;
- a taxa de turismo (2 hóspedes × 2 noites = 18.00);
- o relatório por plano, com o STANDARD zerado e a não reembolsável cancelada contando;
- a linha de título nos quatro relatórios, que o roteiro também confirmou;
- o mês fora do formato, que dá 400.

Mesmo assim, pela API do jeito que o texto descreve, a parte comum quebra logo na criação da reserva.

## Dúvidas

- O texto não diz se o CPF chega com ou sem pontuação. Mostra o formato com máscara só no comprovante, e o agente
  formata o CPF de 11 dígitos para imprimir (`util/CpfUtils.java:5-13`), o que sugere que ele esperava receber só
  dígitos. Mesmo que fosse essa a leitura, aceitar o formato com máscara era o mais seguro: uma coluna que derruba a
  reserva com um erro genérico 400 não é uma decisão razoável. As outras falhas (os códigos de erro, a segunda via de
  pendente e as reservas simultâneas duplicadas) não dependem dessa leitura e já sustentam o `ruim` no comportamento.
- Entre `malfeito` e `omissão` no pattern: há uma classe base comum aos quatro relatórios, com o título num lugar só,
  o que afasta a omissão. Mas a sequência dos passos está repetida nas quatro subclasses, então fico com `malfeito`.
