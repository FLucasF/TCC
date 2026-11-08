# Revisão: construção, cenário 09

## Visão geral

O agente montou um serviço Javalin em sete pacotes: `api` (os controllers e os DTOs), `service` (as regras da
reserva, o comprovante, o e-mail e a fábrica dos relatórios), `pricing` (o cálculo do preço), `report` (os quatro
relatórios em CSV), `repository` (JDBC puro com HikariCP), `domain` (as entidades e os enums) e o `Main`, que cria e
liga todas as peças (`Main.java:27-66`). Uma requisição como `POST /reservations` passa pelo
`ReservationController`, que lê o corpo e valida os campos. Depois vai ao `ReservationService.createReservation`, que
confere o quarto, as datas, a capacidade e o conflito, calcula o preço com o `PricingCalculator` e grava a reserva
pelo `ReservationRepository`. Os dados ficam no PostgreSQL, em três tabelas criadas com `CREATE TABLE IF NOT EXISTS`
(`DatabaseManager.java:33-72`). Os relatórios seguem o Template Method pedido: `CsvReport.generate()` é o esqueleto,
e os quatro relatórios são subclasses. Os relatórios passam em todas as conferências do cenário. O problema mais sério
é que nada impede duas reservas do mesmo quarto nas mesmas noites quando os pedidos chegam juntos: não há transação,
trava nem restrição. No roteiro, isso gerou reserva dupla em 9 de 10 rodadas. Os testes também são quase inexistentes:
cobrem 4,3% das linhas.

## Arquitetura

**Números:** 7 pacotes, 14 dependências entre eles, nenhum ciclo. O Javalin só aparece em `api` e no `Main`, e o
JDBC só em `repository` (`measures/dependencies.md`). As maiores classes são `ReservationController` (266 linhas) e
`ReservationRepository` (221 linhas).
**Nota:** com problemas

A base é boa. As regras (`service`, `pricing`, `report`) não conhecem nem o Javalin nem o JDBC, o SQL fica todo em
`repository` e a montagem fica num lugar só (`Main.java:27-41`). Há, porém, misturas localizadas:

- **Uma regra de negócio no handler.** A segunda via "só existe para reserva paga", e quem decide isso é o controller:
  `ReservationController.java:131` (`if (!reservation.isPaid())`) devolve o 409 ali mesmo. O serviço não tem esse caso.
- **A borda pulando a camada de regras.** `RoomController` chama o `RoomRepository` direto
  (`RoomController.java:11-17`). `ReservationController` recebe o `RoomRepository` e busca o quarto para montar o
  comprovante (`ReservationController.java:200`). Daí vem a dependência `api -> repository`.
- **O comprovante montado e desmontado.** No check-out, o controller pede ao `DocumentService` o texto do comprovante
  e depois recorta o título dessa string (`ReservationController.java:203`, `lastIndexOf(" nº")`), em vez de pedir a
  um serviço os dados do comprovante.
- **Os relatórios buscam os próprios dados.** Cada relatório recebe o repositório e faz a consulta dentro de si
  (`OccupancyReport.java:42`, `RevenueReport.java:48`). Daí vem a dependência `report -> repository`, e o
  `ReportService` só cria o relatório e repassa (`ReportService.java:19-33`).
- **Uma dependência criada por dentro.** `ReservationService.java:25` faz `new PricingCalculator()` em vez de
  receber a peça.

Nenhum desses pontos é grave sozinho, mas são mais de um, e por isso a nota não é `bom`.

## Design de baixo nível

**Números:** dos quatro relatórios, os quatro estão na estrutura (`OccupancyReport`, `RevenueReport`,
`TourismTaxReport` e `RatePlanReport` estendem `CsvReport`), e nenhum ficou fora. A linha de título é escrita num só
lugar (`CsvReport.java:15`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `CsvReport` é uma classe abstrata com o método-modelo `generate()` (`CsvReport.java:12-26`), que
escreve o título, o cabeçalho, as linhas e o total, nesta ordem. Cada relatório só preenche os passos que mudam: o
nome, o período, as colunas, as linhas e o total (`CsvReport.java:6-10`). Os dois relatórios da parte comum também
passam pelo esqueleto, e a linha com a data que o dono quer para o ano que vem entraria num lugar só. O teste
`CsvReportTest.java:94-107` confere o esqueleto com um relatório falso. Dois detalhes afastam um pouco do ideal:

- `generate()` não é `final`, então uma subclasse pode reescrever o esqueleto;
- o esqueleto não cuida da formatação comum. Cada subclasse monta o próprio `DecimalFormat` e o próprio
  `DateTimeFormatter` (`RevenueReport.java:18-28`, `TourismTaxReport.java:20-30`, `RatePlanReport.java:27-30`,
  `OccupancyReport.java:17`).

Mesmo assim, é o Template Method pedido, aplicado aos quatro relatórios.

**O exagero.** Não há interface com uma implementação só, nem builder ou fábrica sem necessidade. O `ReportService`
só cria o relatório e chama `generate()` (`ReportService.java:19-33`). É uma casca fina, mas segue o mesmo jeito do
resto da camada de serviço, e não a conto como exagero (ver "Dúvidas").

**O resto do código.**

- **Dinheiro e datas bem tratados.** O dinheiro está em `BigDecimal`, arredondado com `HALF_EVEN` a cada passo
  (`PricingCalculator.java:62-64`), as datas em `LocalDate` e as situações em `enum`.
- **Os mesmos dados e o mesmo filtro, duas vezes.** Em cada relatório, a linha do total refaz a consulta e repete o
  filtro das linhas, em vez de somar as linhas já escolhidas: `RevenueReport.java:48-53` e `:70-73`,
  `RatePlanReport.java:50-54` e `:77-81`, `TourismTaxReport.java:50-54` e `:73-75`, `OccupancyReport.java:42` e `:61`.
  São duas consultas por relatório, e as linhas e o total podem divergir se uma reserva mudar entre as duas.
- **Um domínio anêmico.** `Reservation` tem um setter para cada campo (`Reservation.java:29-155`), e `confirm()`,
  `checkIn()` e `cancel()` mudam a situação sem conferir de onde ela parte (`Reservation.java:157-177`). A regra da
  transição só existe no serviço (`ReservationService.java:85`, `:100`, `:113`, `:126-127`).
- **Duas fontes para o mesmo dado.** A capacidade e a diária ficam gravadas na tabela `rooms`
  (`RoomRepository.java:46-47`), mas são lidas do `enum` (`Room.java:21-27`) e a coluna nunca é usada.
- **Código morto.** `MessageService` recebe um `RoomRepository` que não usa (`MessageService.java:14,21`).
- **Os erros.** Os blocos `catch` de `ReservationController.java:54-123` repetem a mesma tradução de exceção para
  status em cada handler, em vez de usar um `app.exception(...)`. `Long.parseLong` no id (`:69`, `:79`, ...) não é
  tratado, então um id que não é número vira 500. O `bodyAsClass` também não é tratado (`:33`), então um corpo que
  não é JSON vira 500.
- **O zero sem escala.** `Reservation.java:26` inicializa `amountPaid` com `BigDecimal.ZERO`, sem a escala de 2
  casas, e é isso que a API devolve.

São defeitos localizados, mas são vários: a nota é `com problemas`.

## Testes

**Números:** 8 testes rodaram, 0 falharam, 8 métodos escritos em 2 arquivos. A cobertura é de 39 de 911 linhas
(4,3%) e de 15 de 160 ramos (9,4%). `pricing` está coberto em 27 de 28 linhas; `api`, `service` e `repository` têm 0
linhas cobertas, e `report` tem 9 de 136 (`measures/tests.md`).
**Nota:** ruim

**O que está testado.** Os 7 testes de `PricingCalculatorTest` conferem o preço com valores:

- a noite fora da temporada e as noites de julho e de dezembro, a R$ 299,88 (`PricingCalculatorTest.java:15-42`);
- o exemplo do texto, 599.76 (`:52`);
- o desconto não reembolsável, 89.96 (`:61`), e o total, 509.80 (`:80`).

`CsvReportTest` confere o esqueleto com um relatório falso (`CsvReportTest.java:94-107`).

**O que não está testado.**

- Nenhum dos quatro relatórios reais: nem a taxa de R$ 4,50 por hóspede por noite, nem o plano sem reserva aparecer
  com 0, nem a não reembolsável cancelada continuar contando.
- Nenhuma regra da reserva: as transições, o cancelamento e a devolução, o conflito de noites, a capacidade, os
  códigos de erro.
- O comprovante, o e-mail, o banco e a API.

**A pergunta da mutação.** Se alguém trocasse `4.50` por `5.40` em `TourismTaxReport.java:16`, nenhum teste
falharia. O mesmo vale para quem tirasse o filtro `isPaid` (`:54`) ou o `net > 0` do relatório por planos
(`RatePlanReport.java:53`). Um teste com valores só pega a temporada: mudar o limite `day >= 15`
(`PricingCalculator.java:51`) para `> 15` não falharia, porque o teste usa o dia 20.

O texto não pedia testes, mas o que existe cobre só o cálculo do preço e o esqueleto. Isso é "quase nada" para um
sistema com estas regras.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. As conferências acusaram:

- a chave estrangeira `reservations(room_number) -> rooms` sem índice;
- nenhum `CHECK` nas colunas de situação, plano, tipo e canal;
- nenhuma restrição que proteja uma regra (UNIQUE, EXCLUDE ou CHECK);
- 0 transações explícitas e 0 `FOR UPDATE`;
- o esquema criado no código, sem migração (`measures/database.md`).

**Nota:** ruim

**O que está certo.** O SQL usa sempre parâmetros `?`, e não há SQL montado com `+`. As conexões vêm de um pool
(HikariCP, `DatabaseManager.java:15-21`) e são devolvidas com try-with-resources. Os dados sobrevivem a um reinício
(3 de 3 no roteiro).

**O defeito sério: a reserva dupla.** A regra central do sistema ("um quarto não pode ter duas reservas na mesma
noite") não é protegida em lugar nenhum. O serviço consulta os conflitos
(`ReservationService.java:47`, `ReservationRepository.java:111-138`) e só depois grava (`ReservationService.java:74`).
São duas conexões separadas, em autocommit, sem transação, sem trava, sem `synchronized` e sem restrição de exclusão
no banco. No roteiro, dois pedidos simultâneos para o mesmo quarto e as mesmas noites criaram as duas reservas em 9
de 10 rodadas (`measures/behavior.md`). É dado errado gravado: o quarto fica vendido duas vezes, e o relatório de
ocupação pode mostrar mais quartos ocupados do que existem.

**O mesmo padrão nas mudanças de situação.** Confirmar, cancelar, fazer o check-in e o check-out leem a reserva,
conferem a situação em Java e gravam com um `UPDATE ... WHERE id = ?` sem conferir a situação anterior
(`ReservationRepository.java:67-90`). Duas confirmações simultâneas passariam as duas e mandariam dois e-mails.

**Os outros defeitos.**

- O esquema é criado com `CREATE TABLE IF NOT EXISTS` no código (`DatabaseManager.java:33-72`), sem migrações. O
  próximo pedido que precisar de uma coluna nova não terá como acrescentá-la num banco que já existe.
- A chave estrangeira não tem índice (`DatabaseManager.java:60`).
- Não há `CHECK` em `status`, `rate_plan` e `guests`.
- A carga inicial dos quartos confere `COUNT(*)` e depois insere, sem transação (`RoomRepository.java:19-31`).
- Os relatórios fazem cada consulta duas vezes (ver "Design de baixo nível").

## Comportamento

**Números:** 61 de 64 conferências passaram:

- a parte comum: 44 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09 (os relatórios): 13 de 13;
- duas reservas ao mesmo tempo: 1 de 2 (`measures/behavior.md`).

**Nota:** com problemas

**O que passou.** A parte nova do cenário passa inteira:

- os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>";
- a taxa de turismo calcula e soma certo, inclusive num mês vazio (total 0.00);
- o relatório por planos traz STANDARD e NON_REFUNDABLE mesmo sem reserva no mês;
- o mês fora do formato dá 400.

Na parte comum, o preço com a temporada e o "meio para o par", as recusas, a confirmação com o e-mail, o comprovante,
o check-out, as devoluções e os dois relatórios antigos também estão certos.

**As falhas.**

- **Corpo que não é JSON: 500 em vez de 400 INVALID_REQUEST.** O `ctx.bodyAsClass` (`ReservationController.java:33`)
  lança uma exceção do Jackson que nenhum `catch` trata. Se o site mandar um corpo quebrado, recebe um erro genérico
  do servidor em vez do código combinado. Pela leitura, fora do roteiro, um id que não é número (`/reservations/abc`)
  também dá 500, porque o `Long.parseLong` não é tratado (`:69`).
- **`amountPaid` sai como `0` e não como `0.00`** na resposta da criação. A reserva recém-criada usa o
  `BigDecimal.ZERO` da memória (`Reservation.java:26`), e a combinação com o site era um número com 2 casas. É um
  detalhe de formato. Depois de ler do banco, o valor já vem com 2 casas.
- **Duas reservas simultâneas: 9 de 10 rodadas criaram as duas.** Para o dono, isso quer dizer o mesmo quarto
  vendido duas vezes num fim de semana de movimento. O defeito é do banco e pesa naquela coluna, mas aparece aqui
  também.

Fora da concorrência, as falhas são pequenas e o cenário passa todo, por isso a nota é `com problemas`.

## Dúvidas

- **O exagero.** O `ReportService` (`ReportService.java:19-33`) só cria o relatório e chama `generate()`, e poderia
  ser lido como "uma camada que só repassa". Dei `contenção` porque ele segue o mesmo desenho dos outros serviços e
  esconde dos controllers os repositórios que os relatórios usam. Um revisor mais rígido poderia dar `exagero`.
- **A nota do banco.** A skill do banco põe "a duplicação de reserva protegida só dentro do processo" em
  `com problemas`. Aqui não há proteção nem dentro do processo, e o roteiro mostrou reservas duplicadas gravadas (9
  de 10). Por isso tratei o caso como risco de dado errado e dei `ruim`.
