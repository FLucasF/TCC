# Revisão: construção, cenário 01

## Visão geral

O projeto é um serviço Javalin com oito pacotes: `api` (os controllers), `service` (as regras e as exceções),
`rateplan` (os planos de tarifa), `repository` (JDBC puro), `database` (a conexão e o `CREATE TABLE`), `domain`,
`messaging` e o `Main`, que monta tudo à mão (`Main.java:25-67`). Uma requisição entra no controller, que lê o corpo
como `Map` e monta uma `Reservation` (`api/ReservationController.java:28-47`). Daí vai para o `ReservationService`,
que pede ao plano a validação, o preço e a devolução, e grava pelo `ReservationRepository` no PostgreSQL. As tabelas
saem de `CREATE TABLE IF NOT EXISTS` no código (`database/DatabaseManager.java:23-82`). Os cinco planos vivem em
classes próprias atrás de uma interface, e os valores do cenário conferem todos (21 de 21). O mais sério é outro: nada
impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. A consulta de conflito e o
`INSERT` correm sem transação, sem trava e sem restrição no banco, e o roteiro criou as duas reservas em 10 das 10
rodadas. Há também uma série de erros pequenos na parte comum: quarto inexistente devolve 400 em vez de 404, a segunda
via de reserva pendente devolve 400 em vez de 409, o comprovante escreve "R$" com espaço não separável, e o dinheiro
sai `0` em vez de `0.00`. O `ReservationService` ainda pergunta se o plano é o convênio (`instanceof`), e os testes
cobrem só os planos (8,4% das linhas).

## Arquitetura

**Números:** 8 pacotes, 15 dependências entre eles, nenhum ciclo. O Javalin só aparece em `api` e no `Main`, e o JDBC
só em `repository` e `database`. A maior classe é `ReservationRepository` (258 linhas), seguida de
`ReservationController` (227).
**Nota:** com problemas

As camadas existem e as dependências vão no sentido certo: `api → service → rateplan/repository → database`.
`ReservationService` não conhece o Javalin nem o SQL, e a montagem fica num lugar só (`Main.java:25-48`). Há, porém,
misturas localizadas:

- **A regra da segunda via mora no handler.** Quem decide que só reserva paga tem comprovante é o controller, e decide
  pelo valor pago, não pela situação da reserva (`api/ReservationController.java:134-137`). Por isso uma reserva
  cancelada com devolução total ainda recebe o comprovante: conferi pela API e veio 200.
- **O tipo do quarto está escrito à mão, pelo número, em três lugares fora do repositório:**
  `api/ReservationController.java:185-192`, `service/MessageService.java:82-89` e `service/ReportService.java:84-91`.
  Os três ignoram a tabela `rooms` e o `RoomType` que o próprio projeto tem. O total de quartos do relatório também é
  uma constante (`service/ReportService.java:15`), e não vem do banco.
- **O controller recebe um `MessageService` que não usa** (`api/ReservationController.java:17,21`).
- **`RatePlanFactory` é um `static` global** (`rateplan/RatePlanFactory.java:8-18`), chamado de dentro do serviço
  (`service/ReservationService.java:43,102`) em vez de ser injetado pelo `Main`. Pesa pouco.

Nenhum desses pontos derruba a estrutura, mas cada um é um lugar a corrigir antes de o sistema crescer.

## Design de baixo nível

**Números:** 5 de 5 planos têm classe própria, atrás da interface `RatePlanStrategy` (`validate`, `calculatePrice`,
`calculateRefund`) e escolhidos por um mapa. Fora da estrutura fica 1 `instanceof CorporateRatePlan` no serviço, e não
há nenhum `switch` sobre o plano.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do Strategy.** Cada plano, os dois do ponto de partida inclusive, tem a sua classe com a validação, o
preço e a devolução: `StandardRatePlan`, `NonRefundableRatePlan`, `CorporateRatePlan`, `WeekendPackageRatePlan` e
`LongStayRatePlan`. As regras do cenário ficam dentro de cada plano: sexta a domingo em
`rateplan/WeekendPackageRatePlan.java:18-30`, 7 noites em `rateplan/LongStayRatePlan.java:13-17` e o código da
empresa em `rateplan/CorporateRatePlan.java:17-21`. O cancelamento pede a devolução ao plano sem nomeá-lo
(`service/ReservationService.java:102-103`), e o único ponto de seleção é um mapa (`rateplan/RatePlanFactory.java:8-14`).

**O que afasta.** A interface não leva o código da empresa para o cálculo do preço
(`rateplan/RatePlanStrategy.java:11`). Por isso o convênio implementa o método da interface lançando exceção
(`rateplan/CorporateRatePlan.java:23-26`) e acrescenta uma sobrecarga própria (`rateplan/CorporateRatePlan.java:28`).
O serviço, que deveria só pedir o preço ao plano, pergunta qual plano é:
`if (strategy instanceof CorporateRatePlan corporate)` (`service/ReservationService.java:58-65`). É o caso que a
referência chama de "quem usa a estrutura ainda perguntando qual é o caso". O próximo plano que precisar de um dado a
mais (a antecedência de 60 dias, a idade da melhor idade) vai abrir outro `instanceof` no serviço. A estrutura existe
e cobre os cinco planos, mas convive com esse desvio: malfeito, e não acerto.

**O exagero.** Não há estrutura sem necessidade. O mapa de seleção e o `record PriceCalculation` fazem parte do
pattern, e os repositórios e serviços são os de um sistema deste tamanho, sem interfaces sobrando: contenção.

**O resto do código.** Vários defeitos localizados:

- **O laço de preço por noite está copiado em quatro planos:** `rateplan/StandardRatePlan.java:15-25`,
  `rateplan/NonRefundableRatePlan.java:17-30`, `rateplan/LongStayRatePlan.java:21-34` e
  `rateplan/CorporateRatePlan.java:29-43`. Os três com desconto repetem também as linhas de desconto e total. Faltou um
  cálculo base compartilhado.
- **O `switch` do tipo do quarto pelo número aparece três vezes** (ver Arquitetura).
- **A checagem da situação da reserva está duas vezes:** no serviço (`service/ReservationService.java:82,97-98,115,128`)
  e de novo nos métodos da entidade (`domain/Reservation.java:35-62`).
- **Um `catch (Exception e)` engole a causa** (`api/ReservationController.java:56-57`). É ele que transforma o
  `RoomNotFoundException` em 400 `INVALID_REQUEST` em vez de 404 `ROOM_NOT_FOUND`. O tratamento de erro se repete em
  cada handler (`api/ReservationController.java:61-145`), em vez de usar os `app.exception` do Javalin.
- **`Reservation` é um saco de setters.** Situação, valor pago e devolução são mutáveis por qualquer um
  (`domain/Reservation.java:180-198`). O zero do valor pago e do desconto nasce com escala 0
  (`domain/Reservation.java:28`, `rateplan/StandardRatePlan.java:25`) e por isso sai `0` no JSON.
- **O dinheiro do comprovante sai de `NumberFormat.getCurrencyInstance(pt_BR)`** (`service/MessageService.java:47`).
  Ele põe um espaço não separável (U+00A0) entre "R$" e o número, e o texto deixa de ser o que o dono pediu.
- **Os tipos do domínio estão certos:** `BigDecimal` com `HALF_EVEN` (`rateplan/SeasonCalculator.java:37-39`),
  `LocalDate` e `enum`.

## Testes

**Números:** 9 testes rodaram, nenhuma falha, nenhum pulado. Há 9 métodos de teste num arquivo só
(`RatePlanTest`). A cobertura é de 8,4% das linhas (73 de 870) e 17,1% dos ramos (26 de 152). `rateplan` tem 64 de 90
linhas; `service`, `repository`, `api` e `messaging` têm 0. Não há nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que existe confere valores de verdade, e justamente na parte nova:

- os exemplos do texto: R$ 599,76 (`src/test/.../RatePlanTest.java:23-25`), Tecnosul R$ 93,58 / R$ 686,22
  (`:39-41`), pacote R$ 820,00 (`:64-66`) e devolução de R$ 410,00 (`:74`), estadia longa R$ 174,93 / R$ 1.574,37
  (`:97-99`);
- as recusas de código inválido, pacote fora da sexta e 6 noites (`:103-142`);
- as bordas da temporada (`:145-157`).

Pela pergunta da mutação, ficam sem rede:

- trocar 0.18 ou 0.25 da Agrovale ou da Farmavida (só a Tecnosul é testada);
- trocar o 0.15 do não reembolsável;
- trocar a devolução do convênio, da estadia longa, do normal ou do não reembolsável (só a do pacote é testada);
- tirar a checagem do domingo do pacote (`rateplan/WeekendPackageRatePlan.java:27-29`), porque o teste de sexta a domingo
  passaria igual.

E nada testa a parte comum fora dos planos: as situações da reserva, o conflito de datas, os códigos de erro, o
comprovante e o e-mail, os relatórios e a persistência. Os erros que o roteiro achou (404, 409, R$ com U+00A0, `0`
sem casas) passaram sem nenhum teste ver.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. O script acusou: 0 chaves estrangeiras, 0 restrições `CHECK`/`UNIQUE`/`EXCLUDE`,
nenhum pool, `DriverManager.getConnection` a cada chamada, 0 transações explícitas, 0 `FOR UPDATE`, e o esquema criado
por `CREATE TABLE` no código, sem migração. No roteiro do comportamento, as duas reservas simultâneas foram criadas em
10 de 10 rodadas.
**Nota:** ruim

Os tipos estão certos e todo o SQL usa parâmetros (`repository/ReservationRepository.java`, sem concatenação). As
conexões são fechadas com try-with-resources. O defeito sério é outro: a regra central da pousada, um quarto por noite,
não tem nenhuma proteção.

- O serviço faz um `SELECT COUNT(*)` de conflito (`repository/ReservationRepository.java:145-179`) e depois um
  `INSERT` (`repository/ReservationRepository.java:21-65`). Cada um abre a sua própria conexão em autocommit
  (`service/ReservationService.java:52-71`), sem transação, sem trava e sem `synchronized`.
- No banco não há `EXCLUDE` nem outra restrição sobre `room_number`/datas (`database/DatabaseManager.java:31-51`).
- O resultado medido: dois pedidos iguais criaram duas reservas em todas as 10 rodadas. A pousada venderia o mesmo
  quarto duas vezes na mesma noite. É dado errado gravado, não um caso raro.

Além disso:

- `reservations.room_number` não tem chave estrangeira para `rooms` (`database/DatabaseManager.java:34`).
- `status`, `rate_plan`, `type` e `channel` são `varchar` sem `CHECK`.
- O esquema não evolui: `CREATE TABLE IF NOT EXISTS` (`database/DatabaseManager.java:23-61`) não sabe acrescentar a
  coluna que o próximo plano vai pedir.
- Sem pool, cada consulta abre uma conexão nova (`database/DatabaseManager.java:19-21`).

## Comportamento

**Números:** 65 de 72 conferências passaram.

| parte | passaram |
|---|---|
| a parte comum | 40 de 46 |
| depois de reiniciar | 3 de 3 |
| o cenário 01 | 21 de 21 |
| reservas simultâneas | 1 de 2 |

**Nota:** com problemas

O cenário está inteiro certo:

- Tecnosul R$ 686,22, Agrovale e Farmavida certas;
- convênio sem código ou com código inválido recusado com 400;
- pacote a R$ 520/820/1.200, inclusive em julho, com devolução de R$ 410,00;
- pacote fora de sexta a domingo recusado;
- estadia longa R$ 1.574,37, com a temporada valendo, e 6 noites recusadas;
- `companyCode` ignorado nos outros planos.

Os dados sobrevivem ao reinício.

As falhas da parte comum:

- **Quarto que não existe: 400 `INVALID_REQUEST` em vez de 404 `ROOM_NOT_FOUND`.** O texto fecha esse ponto; é o
  `catch (Exception)` de `api/ReservationController.java:56-57`. O site não consegue distinguir "quarto errado" de
  "dados errados".
- **Segunda via de reserva pendente: 400 em vez de 409 `OPERATION_NOT_ALLOWED`** (`api/ReservationController.java:134`).
  O texto diz que operação que a situação não permite é 409. Do outro lado, uma reserva cancelada com valor pago
  recebe a segunda via (conferi: 200), quando o texto diz que ela só existe para reserva confirmada, com check-in ou
  com check-out.
- **O e-mail e o comprovante não trazem "R$ 599,76", e sim "R$ 599,76".** Conferi o byte `302 240` no corpo da
  mensagem. Na tela parece igual, mas o texto não é o que o dono pediu, e qualquer comparação ou busca pelo valor
  falha.
- **`amountPaid` e `discount` saem `0` no JSON, e não `0.00`,** contra o "número com 2 casas" do texto. Depois de
  recarregar do banco, viram `0.00`.
- **"o 201 comporta 3" e "a diária do 201":** o projeto devolve `number` como texto (`"201"`) e o roteiro procura o
  número 201. O texto não diz o tipo, então é uma escolha razoável e não pesa. Conferi pela API: capacidade 3 e diária
  389.90 corretas.

E a falha das reservas simultâneas: em 10 de 10 rodadas as duas foram criadas. Para o dono, isso quer dizer overbooking
quando dois hóspedes reservam o mesmo quarto quase ao mesmo tempo. O defeito pesa na coluna do banco. Aqui ele soma com
as falhas de código de erro e de formato na parte comum: a pousada funciona e o cenário está certo, mas há vários
pontos que o texto fechava e que saíram diferentes.

## Dúvidas

- **A classe do pattern.** Os cinco planos estão na estrutura, e o desvio é um único `instanceof` no serviço,
  `service/ReservationService.java:58`, causado pela interface sem o código da empresa. Um revisor mais brando poderia
  chamar de acerto com um defeito. Fiquei com malfeito porque a referência nomeia exatamente esse caso ("quem usa a
  estrutura ainda perguntando qual é o caso") e porque `CorporateRatePlan.calculatePrice` de quatro argumentos lança
  exceção.
- **A nota do banco.** A skill põe em `com problemas` a duplicação "protegida só dentro do processo". Aqui não há
  proteção nenhuma, nem no processo, e a duplicação aconteceu em 10 de 10 rodadas. Por isso dei `ruim`, pelo risco de
  dado errado.
- **A nota dos testes.** Ficou entre `com problemas` e `ruim`: 8,4% de cobertura e nada fora dos planos, mas os 9
  testes conferem valores reais da parte nova. Dei `com problemas`.
