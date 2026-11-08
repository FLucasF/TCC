# Revisão: construção, cenário 09

## Visão geral

O projeto tem 6 pacotes: `com.pousadamareaalta` (só o `Main`, com as 13 rotas do Javalin), `service` (reserva e
mensagens), `service.report` (os quatro relatórios atrás de uma interface `ReportGenerator`), `database` (uma
`Connection` estática e três repositórios estáticos com JDBC), `domain` (records) e `util` (preço e formatação). Uma
requisição entra num lambda do `Main`, que lê o corpo como `Map`, chama um método estático do serviço, que chama um
repositório estático, que usa a conexão única de `Database`. Os dados ficam no PostgreSQL, em três tabelas criadas
por `CREATE TABLE IF NOT EXISTS` no código. O mais sério: a coluna `guest_cpf` é `VARCHAR(11)`
(`src/main/java/com/pousadamareaalta/database/Database.java:21`), e por isso **toda reserva criada com o CPF escrito
como o texto mostra (111.222.333-44) é recusada com 400**. O roteiro de comportamento passou só em 24 de 64
conferências. Mesmo com CPF só de dígitos, há outros problemas: nada impede duas reservas do mesmo quarto feitas ao
mesmo tempo (20 de 20 rodadas criaram as duas, no meu teste manual); o relatório financeiro e o de planos deixam de
fora a reserva não reembolsável cancelada; o dinheiro passa por `double` no código; e os quatro relatórios são
escritos inteiros, cada um com a sua cópia da linha de título, sem o esqueleto comum que o texto pedia. Há um único
arquivo de teste, com 5 testes, que cobre 4,9% das linhas.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Só o pacote raiz usa
Javalin; JDBC aparece em `database`, `service` e `service.report`. A maior classe é o `Main`, com 348 linhas, contra 174
da segunda.
**Nota:** com problemas

As dependências vão na direção certa (HTTP → serviço → dados) e o SQL fica todo em `database/`. O resto tem
problemas:

- **A borda HTTP faz trabalho de regra.** A regra "a segunda via só existe para reserva paga" está no handler
  (`Main.java:232-238`), e não em `ReservationService`. A montagem do comprovante está copiada em dois handlers
  (`Main.java:197-210` e `Main.java:247-260`). O envio do e-mail depois da confirmação é orquestrado na rota
  (`Main.java:127-130`), fora do serviço que confirma. A tradução de erro para status HTTP compara textos de
  `IllegalArgumentException` e se repete em cada rota (`Main.java:90-101`, `133-140`, `152-159`, `171-178`,
  `213-220`).
- **Não há montagem.** Tudo é `static`: a conexão é um campo estático global (`database/Database.java:48`), os
  serviços chamam os repositórios por nome de classe (`service/ReservationService.java:16`, `:33`, `:61`), e os
  relatórios vão direto ao repositório (`service/report/RevenueReport.java:19`). Por isso nenhuma peça pode ser
  trocada nem testada isoladamente.
- **O JDBC vaza para as regras.** Serviços e relatórios declaram `throws SQLException`
  (`service/ReservationService.java:14`, `service/report/ReportGenerator.java:6`), e a camada de regras fica presa ao
  JDBC. O próprio `jdeps` mostra isso.

São misturas localizadas, sem ciclo, mas com uma classe muito maior que as outras e sem lugar onde as peças são
ligadas.

## Design de baixo nível

**Números:** 4 relatórios, 4 implementações de `ReportGenerator`. Nenhum passo do esqueleto (título, cabeçalho,
linhas, total) está num lugar comum: 0 relatórios usam uma estrutura compartilhada, e 4 escrevem a sequência inteira à
mão, cada um com a sua cópia da linha de título.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O texto pede que "quando o jeito dos relatórios mudar, [mude] em todos de uma vez", ou seja, um
esqueleto comum. O que existe é uma interface com `generate()` e `getReportName()`
(`service/report/ReportGenerator.java:5-8`), e cada implementação escreve todo o CSV sozinha:

- o título está copiado quatro vezes (`OccupancyReport.java:23`, `RevenueReport.java:22-24`,
  `TourismTaxReport.java:25-27`, `RatePlansReport.java:41-43`), e nas três mensais o zero do mês é montado à mão
  (`if (month.getMonthValue() < 10) csv.append("0")`);
- `getReportName()` existe, mas ninguém o usa: o nome do relatório está escrito de novo, à mão, em cada título;
- a linha de total é montada à mão em cada classe (`OccupancyReport.java:43`, `RevenueReport.java:45`,
  `TourismTaxReport.java:46`, `RatePlansReport.java:62`).

Se o contador pedir a linha com a data em que o relatório foi tirado, vai ser preciso mexer nas quatro classes, que é
justamente o que o dono não queria. A interface só dá um tipo comum para o `Main`, sem método-modelo nem composição
dos passos. Pela referência, isso é omissão: os quatro relatórios escritos inteiros, cada um com a sua linha de
título.

**Exagero.** Não há estrutura sobrando. A interface tem quatro implementações, e não há fábricas nem camadas que só
repassam. O único resto é `getReportName()`, que não é usado. Contenção.

**O resto do código.** Os defeitos se espalham pelo projeto:

- **Dinheiro em `double`** no domínio inteiro (`domain/Reservation.java:30-40`, `domain/Room.java:13`), com idas e
  voltas por `String.format("%.2f")` para virar `BigDecimal` (`util/PriceCalculator.java:32`, `:43`, `:52`,
  `:60-61`; `database/ReservationRepository.java:27-29`, `:70`, `:80`). Os totais dos relatórios são somados em
  `double` (`RevenueReport.java:28-42`, `RatePlansReport.java:33-37`, `:59`). Como `String.format` usa o locale da
  máquina, numa JVM em pt-BR o `new BigDecimal("249,90")` lança exceção, e o CSV sairia com vírgula decimal
  (`util/FormatterUtil.java:23`).
- **Formatação de moeda errada:** `String.format("R$ %.2f").replace(".", ",")` (`util/FormatterUtil.java:19`) não põe
  o separador de milhar. R$ 1.739,70 sai como "R$ 1739,70", e o texto pede "R$ 1.234,56".
- **Alta temporada errada no ano bissexto:** fevereiro só vai até o dia 28 (`util/PriceCalculator.java:25-26`). Uma
  diária em 29/02/2028 saiu a 249,90 em vez de 299,88, conferido pela API.
- **Situação, plano e tipo como texto solto** (`"PENDING"`, `"CONFIRMED"`… em `service/ReservationService.java:56`,
  `78`, `91`, `102`, `113`, `118`). O `ratePlan` não é validado: uma reserva com `"BOGUS"` foi criada com 201, e
  qualquer plano diferente de `NON_REFUNDABLE` vira normal (`util/PriceCalculator.java:51`).
- **Erros por texto de exceção:** os códigos viajam como mensagem de `IllegalArgumentException`
  (`service/ReservationService.java:18`, `:26`, `:34`), e o `catch (Exception)` transforma qualquer falha em 400
  INVALID_REQUEST com a mensagem crua do banco (`Main.java:102-104`; foi assim que o roteiro recebeu "value too long
  for type character varying(11)"). Nos relatórios, o `catch (Exception)` responde "Invalid date format" até para
  uma falha de banco (`Main.java:292-295`).
- **Duplicação:** o comprovante é montado duas vezes no `Main`, além da versão em texto de
  `MessageService.generateReceiptText`. O mapeamento de erro é copiado em cinco rotas. As três consultas de reserva
  repetem a mesma lista de colunas (`database/ReservationRepository.java:43-44`, `109-110`, `132-133`).

## Testes

**Números:** 5 testes rodaram, 0 falharam, 0 pulados; 5 métodos escritos, num único arquivo. Cobertura de linhas:
32 de 650 (4,9%). Cobertura de ramos: 17 de 144 (11,8%). Só o pacote `util` tem cobertura (32 de 47 linhas); os
pacotes `service`, `service.report`, `database` e o `Main` têm 0% (`measures/tests.md`).
**Nota:** ruim

O único arquivo (`src/test/java/com/pousadamareaalta/PriceCalculatorTest.java`) confere, com valores, a alta temporada
(`:11-21`), a diária de 299,88 (`:31`), a soma de 599,76 (`:41`) e o desconto e o total do não reembolsável (`:52`,
`:61`). Mesmo esses testes têm falhas:

- o desconto e o total são comparados com tolerância de 0,01 (`:52`, `:61`), e por isso não pegariam um erro de
  arredondamento de um centavo, que é justamente a regra "meio para o par";
- o caso que falha (29/02 de ano bissexto) não é testado.

Não há nenhum teste das situações da reserva, do cancelamento e da devolução, das recusas e dos códigos de erro, do
comprovante, do e-mail, nem de nenhum dos quatro relatórios, que são a parte nova do cenário. A pergunta da mutação
mostra o tamanho do buraco: trocar o 4,50 da taxa de turismo (`TourismTaxReport.java:14`), tirar `CANCELLED` do
filtro do financeiro ou pôr `NON_REFUNDABLE` antes de `STANDARD` no relatório de planos não faria nenhum teste
falhar. Tampouco há teste pela API ou contra o banco.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. As 6 colunas de dinheiro são
`numeric(10,2)`. O script acusou: 1 chave estrangeira sem índice (`reservations.room_number`), 4 colunas de
situação/tipo sem CHECK, nenhuma restrição UNIQUE/EXCLUDE/CHECK, 0 arquivos de migração (esquema criado no código), 0
pools, 1 `DriverManager.getConnection`, 0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`).
**Nota:** ruim

- **O CPF não cabe.** `guest_cpf VARCHAR(11)` (`database/Database.java:21`) só aceita o CPF sem pontuação, e o código
  não normaliza nem valida a entrada. Com o CPF escrito como no texto, o `INSERT` falha e a reserva é recusada. É a
  causa da maior parte das falhas do comportamento.
- **Reserva dupla sem proteção nenhuma.** A disponibilidade é conferida com um `SELECT COUNT(*)`
  (`database/ReservationRepository.java:89-105`) e só depois vem o `INSERT` (`service/ReservationService.java:33` e
  `:61`). Não há transação, trava, restrição de exclusão nem `synchronized`. Num teste meu com CPF só de dígitos, 20
  de 20 rodadas de dois pedidos simultâneos criaram **as duas** reservas para o mesmo quarto e as mesmas noites, e o
  banco ficou com pares duplicados. O roteiro não viu isso porque lá as duas foram recusadas pelo CPF.
- **Escritas que deviam andar juntas, separadas:** confirmar grava a situação e o valor pago em dois `UPDATE`s
  independentes (`service/ReservationService.java:82-83`), e cancelar faz o mesmo com a situação e a devolução (`:126-128`).
  Se o segundo falhar, a reserva fica CONFIRMED sem pagamento.
- **Dinheiro em `double`:** sai do banco com `rs.getDouble` (`database/ReservationRepository.java:166-170`,
  `database/RoomRepository.java:19`) e volta por `String.format("%.2f")`, que depende do locale (`:27-29`).
- **Uma conexão só, estática e compartilhada** por todas as threads do Jetty (`database/Database.java:48-51`), sem
  pool e sem reconexão: se ela cair, o serviço para de atender até reiniciar.
- **O esquema não evolui:** `CREATE TABLE IF NOT EXISTS` no código (`database/Database.java:6-46`) não sabe acrescentar
  uma coluna numa tabela que já existe. Também não há CHECK em `status`/`rate_plan`/`guests` nem índice na chave
  estrangeira.

O SQL usa sempre parâmetros (`?`), e as conexões dos comandos são fechadas com try-with-resources. Mas a recusa do CPF
no formato do texto e a reserva dupla demonstrada são risco de dado errado e de recusa de dado válido.

## Comportamento

**Números:** 24 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 16 de 46 |
| depois de reiniciar | 1 de 3 |
| o cenário 09 | 6 de 13 |
| reservas simultâneas | 1 de 2 (nas 10 rodadas, os dois pedidos voltaram 400) |

**Nota:** ruim

**A causa principal.** O roteiro manda o CPF como "111.222.333-44", o formato que o próprio texto usa, e **nenhuma
reserva é criada**: a resposta é 400 INVALID_REQUEST com a mensagem "value too long for type character varying(11)".
Daí vêm em cascata quase todas as falhas: confirmação, e-mail, check-in, check-out, cancelamentos, devoluções, segunda
via (404), relatórios vazios ("total,0/5", "total,,,,0.00") e dados depois de reiniciar. O texto não diz em que
formato o CPF chega, mas um sistema que recusa o CPF escrito como o próprio dono escreve quebra o fluxo principal do
site. Passaram as recusas que não precisam de reserva criada (422, 404, 400) e as linhas de título dos relatórios.

**Com CPF só de dígitos,** que testei pela API, o fluxo funciona: os preços, o e-mail igual ao modelo, a ocupação e a
taxa de turismo saem certos. Ainda assim aparecem estes erros:

- **O financeiro e o de planos perdem a não reembolsável cancelada.** Uma NON_REFUNDABLE confirmada e cancelada
  (795,40) não aparece em nenhum dos dois, porque a consulta filtra `status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')`
  (`database/ReservationRepository.java:113`). Para o dono, o financeiro do mês e o relatório de planos ficam
  menores do que o dinheiro que de fato ficou com a pousada, contra o que o texto diz explicitamente.
- **A linha de total do financeiro e da taxa de turismo** sai "total,,,,899.64" e "total,,,,,27.00"
  (`RevenueReport.java:45`, `TourismTaxReport.java:46`). O texto pede "total" e o valor ("total,27.00"), e uma
  planilha que lê a última linha não acha o valor onde espera.
- **O dinheiro no JSON não vem com 2 casas:** `"amountPaid":0.0`, `"total":795.4`, `"nightlyRate":249.9`, contra o
  que o texto combinou (599.76). E `refund` some do JSON em vez de vir `null` (`domain/Reservation.java:7`).
- **Reserva dupla:** duas reservas simultâneas do mesmo quarto são as duas aceitas (20 de 20 rodadas, ver Banco).
- **Pela leitura do código e por chamadas fora do roteiro:** a diária de 29/02/2028 sai sem o acréscimo da alta
  temporada; um plano de tarifa inexistente é aceito; e o comprovante no e-mail escreve "R$ 1739,70" sem o ponto de
  milhar.

A parte comum quebra pela API do jeito que o texto a descreve, e a maior parte do cenário falha. Mesmo com o CPF
corrigido, dois dos quatro relatórios dariam valores errados.

## Dúvidas

- O texto não diz em que formato o CPF chega no `POST /reservations`. Tratei a recusa do formato com pontuação, que é
  o formato que o texto mostra, como defeito do agente, e não como escolha razoável: o sistema nem normaliza nem
  devolve um erro claro, só estoura o tamanho da coluna. Se o site combinasse mandar só dígitos, boa parte da parte
  comum passaria, mas a nota de comportamento continuaria baixa pelo financeiro e pelo de planos sem a não
  reembolsável cancelada e pela reserva dupla.
- A linha "total,,,,valor" do financeiro (com colunas vazias) poderia ser lida como escolha para alinhar com o
  cabeçalho. Para a taxa de turismo, o texto diz explicitamente "total" e o valor.
