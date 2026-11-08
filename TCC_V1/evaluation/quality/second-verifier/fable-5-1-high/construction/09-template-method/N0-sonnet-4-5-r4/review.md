# Revisão: construção, cenário 09

## Visão geral

O projeto é um Maven simples com Javalin 6.7.0 e JDBC puro, em sete pacotes: `App` (montagem e rotas), `handler`
(a borda HTTP), `service` (as regras: `ReservationService`, `PricingService`, `ReportService`), `repository` (o SQL),
`database` (abre conexões e cria as tabelas), `model` e `dto`. Uma requisição entra numa rota declarada em
`App.java:54-69`, cai num handler que lê o corpo e traduz exceções em códigos HTTP, chama o serviço, e o serviço fala
com o banco pelos repositórios; os dados ficam em três tabelas do PostgreSQL (`rooms`, `reservations`, `messages`)
criadas por `CREATE TABLE IF NOT EXISTS` dentro de `DatabaseManager.java:100-137`. A parte comum funciona quase toda
pela API (59 de 64 conferências) e os quatro relatórios saem com os valores certos. O que há de mais sério: o pattern
que o texto pede não existe (os quatro relatórios estão escritos inteiros, um ao lado do outro, em
`ReportService.java:32-188`), não há nada que impeça duas reservas do mesmo quarto na mesma noite quando os pedidos
chegam juntos (10 de 10 rodadas criaram as duas), a segunda via sai para reserva pendente, e os testes cobrem 1,5% do
código.

## Arquitetura

**Números:** 7 pacotes, 12 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em
`com.pousadamarealta` e `handler`; JDBC só em `database` e `repository`; `service` não usa nem um nem outro. As maiores
classes têm 260, 244 e 232 linhas (`ReservationRepository`, `ReservationService`, `ReservationHandler`).
**Nota:** com problemas

As camadas existem e apontam na direção certa. A montagem fica num lugar só, `App.java:27-44`, com todas as peças
criadas no `main` e ligadas por construtor. As regras ficam em classes que não conhecem o Javalin nem o JDBC: a
criação da reserva com as recusas em `ReservationService.java:221-257`, o preço em `ReservationService.java:259-283`
e `PricingService.java:448-476`, as situações em `ReservationService.java:285-361`. O SQL mora só nos repositórios
(`ReservationRepository.java:31-36`, `139-150`, `210-218`) e no `DatabaseManager`. Os handlers leem a requisição,
traduzem as exceções do serviço em código HTTP e escrevem a resposta (`ReservationHandler.java:190-243`).

O que tira o `bom` é uma mistura localizada: a borda HTTP passa por cima do serviço e fala com os repositórios
diretamente. `ReservationHandler` recebe `ReservationRepository` e `RoomRepository` no construtor
(`ReservationHandler.java:182-188`) e os usa em `ReservationHandler.java:249`, `312`, `342` e `345`; é a dependência
`handler -> repository` que o jdeps mostra. A consequência prática apareceu: a regra "segunda via só para reserva
paga" existe em `ReservationService.generateReceipt` (`ReservationService.java:364-366`), mas `getDocument` não passa
por ela e monta o comprovante direto do repositório (`ReservationHandler.java:339-361`), e a reserva pendente ganha
segunda via. Além disso, a montagem do comprovante em JSON, que é um objeto do domínio, está escrita duas vezes dentro
do handler (`ReservationHandler.java:314-326` e `347-359`) em vez de vir do serviço.

## Design de baixo nível

**Números:** 4 relatórios, 0 numa estrutura comum; cada um escreve a sua linha de título
(`ReportService.java:44`, `79`, `115`, `157`), o seu cabeçalho (`45`, `80`, `116`, `158`), o seu laço e a sua linha
de total (`62`, `100`, `137`, `185`). O filtro "reservas que contam" está copiado 3 vezes (`71-75`, `109-111`,
`146-150`) e a conta "pago menos devolvido" 2 vezes (`85-88`, `169-174`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O texto pede, sem dizer o nome, um Template Method: os quatro relatórios têm os mesmos passos (título, cabeçalho,
linhas, total), o contador vai pedir mais dois e "quando o jeito dos relatórios mudar, quero que mude em todos de uma
vez". O agente escreveu os quatro inteiros, um método para cada, em `ReportService.java:32-65` (ocupação), `67-103`
(financeiro), `105-140` (taxa de turismo) e `142-188` (planos). Não há classe abstrata com o esqueleto, nem um escritor
genérico que receba as partes que variam; o único trecho compartilhado é `formatCsvNumber`
(`ReportService.java:190-192`), uma função utilitária. A linha com a data de emissão que o dono anunciou para o ano
que vem exigirá mexer nos quatro métodos, e o relatório de cancelamentos será uma quinta cópia. É omissão: a variação
ficou numa cópia por caso.

Exagero não há: nenhuma interface com uma implementação só, nenhuma fábrica ou builder; os DTOs
(`CreateReservationRequest`, `ErrorResponse`) servem ao Jackson e `PricingService` é pequeno e usado pelos dois
serviços. Contenção.

No resto do código, o que está bem: dinheiro em `BigDecimal` com `HALF_EVEN` a cada passo (`PricingService.java:474-476`;
noite, soma, desconto e total em `ReservationService.java:267-282`), datas em `LocalDate`, situação e plano em `enum`
(o desconto mora no `RatePlan.java:69-71`, a capacidade e a diária no `RoomType.java:281-284`), exceções com tipo
próprio por recusa (`ReservationService.java:417-436`). O que um revisor pediria para mudar:

- duplicação fora dos relatórios: os três handlers de relatório mensal são idênticos, trocando só o método chamado
  (`ReportHandler.java:435-457`, `459-481`, `483-505`); o mapa do comprovante aparece duas vezes
  (`ReservationHandler.java:314-326` e `347-359`); o mesmo bloco de três `catch` repete-se cinco vezes no handler;
- `catch (Exception e)` que devolve 400 `INVALID_REQUEST` e esconde a causa: uma falha do banco ao criar a reserva ou
  ao tirar um relatório vira "Invalid request" (`ReservationHandler.java:241-243`; `ReportHandler.java:430-432`,
  `454-456`, `478-480`, `502-504`);
- o dinheiro sai no JSON como `double` (`ReservationHandler.java:324-326`, `384-389`; `RoomHandler.java:532`), o que
  dá `0.0` onde o texto pede `0.00`;
- código morto: `receipt` calculado e nunca usado em `ReservationHandler.java:310`, e o `catch` de
  `OperationNotAllowedException` em `getDocument` (`ReservationHandler.java:367-369`) que nunca é alcançado porque
  nada ali lança essa exceção.

## Testes

**Números:** 3 testes rodaram, 0 falharam, 0 pulados; 3 métodos de teste escritos, todos em
`PricingServiceTest.java`; cobertura de 14 de 911 linhas (1,5%) e 11 de 152 ramos (7,2%); `handler`, `repository`,
`model` e `database` com 0 linhas cobertas (`measures/tests.md`).
**Nota:** ruim

Os três testes que existem conferem com valores: a alta temporada nas bordas (`PricingServiceTest.java:16-28`, 15/12,
28/02, 1/07 e 31/07 dentro; 14/12, 1/03, 30/06 e 1/08 fora), a noite de R$ 299,88 (`31-38`) e o "meio para o par"
(`41-46`). Mas é só isso. Não há teste de unidade nem pela API para: o total da reserva, o desconto do NON_REFUNDABLE,
as transições de situação e os 409, as devoluções no cancelamento, o comprovante, o e-mail, e nada da parte nova do
cenário (os quatro relatórios, a linha de título, a taxa de R$ 4,50, o relatório por plano). A pergunta da mutação:
trocar `4.50` por `5.00` em `ReportService.java:17`, apagar a linha de título em `ReportService.java:115`, trocar
`0.15` por `0.20` em `RatePlan.java:71` ou inverter `!checkOut.isAfter(checkIn)` em `ReservationService.java:235`,
nenhum teste falharia. O texto não pede testes, mas com 1,5% do código executado a coluna está quase ausente.

## Banco

**Números:** 3 tabelas, todas com chave primária; 5 colunas de dinheiro em `numeric(10,2)`; datas em `date`; 1 chave
estrangeira (`reservations.room_number -> rooms`) sem índice; nenhum CHECK, UNIQUE ou EXCLUDE; esquema criado por
`CREATE TABLE` no código, sem migração; 0 pool, 0 transações explícitas, 0 `FOR UPDATE`; nenhum SQL montado com `+`
(`measures/database.md`).
**Nota:** com problemas

O que está certo: o serviço sobe contra um banco vazio e cria o que precisa; dinheiro em `numeric(10,2)`
(`DatabaseManager.java:119-124`), datas em `date` (`114-115`), `NOT NULL` no obrigatório e `refund` nulo até o
cancelamento (`124`), a reserva ligada ao quarto por chave estrangeira (`125`); todo SQL usa `?`
(`ReservationRepository.java:31-36`, `139-150`), conexões fechadas por try-with-resources (`38-39`, `153-154`), ids
começam em 1 pela `SERIAL`, e os dados continuam lá depois de reiniciar (3 de 3 no roteiro).

Os defeitos:

- **Nada impede duas reservas do mesmo quarto na mesma noite.** A criação é "consulta e depois insere"
  (`ReservationService.java:239-256`), e cada chamada abre a sua conexão (`findConflicting` em
  `ReservationRepository.java:153` e `insert` em `38`), sem transação, sem trava e sem restrição `EXCLUDE` na tabela.
  Não é proteção só dentro do processo: não há proteção nenhuma, e o roteiro criou as duas reservas em 10 de 10
  rodadas.
- O esquema não sabe evoluir: `CREATE TABLE IF NOT EXISTS` (`DatabaseManager.java:100-137`) não acrescenta uma coluna
  numa tabela que já existe; o próximo pedido que mexer na reserva vai exigir uma migração à mão.
- Sem pool: `DatabaseManager.java:92-94` abre uma conexão nova pelo `DriverManager` a cada chamada, e o relatório de
  ocupação abre uma por quarto (`ReportService.java:36-41`).
- Chave estrangeira sem índice e colunas `status`, `rate_plan`, `type` e `channel` em texto sem CHECK; não há CHECK
  de `check_out > check_in` nem de `guests >= 1`, tudo fica só no Java.
- `findByCheckInMonth` filtra por `TO_CHAR(check_in, 'YYYY-MM') = ?` (`ReservationRepository.java:216`), que não usa
  índice; tolerável numa pousada de cinco quartos, mas vale a nota.

## Comportamento

**Números:** 59 de 64 conferências passaram: parte comum 42 de 46, dados depois de reiniciar 3 de 3, cenário 09
13 de 13, duas reservas ao mesmo tempo 1 de 2 (`measures/behavior.md`).
**Nota:** com problemas

A parte comum e o cenário funcionam: os preços (599,76; desconto 37,48 e total 212,42), as recusas com os seis
códigos, a confirmação com o e-mail e o comprovante, o check-in e o check-out, as três devoluções, e os quatro
relatórios com a linha de título, o cabeçalho, as linhas na ordem e o total, inclusive a taxa de um mês vazio e o plano
sem reserva que aparece com 0. As cinco falhas:

- **Duas reservas ao mesmo tempo (pesa):** em 10 rodadas de dois pedidos simultâneos para o mesmo quarto e as mesmas
  noites, as duas foram criadas nas 10 (a segunda linha "OK" só diz que não houve rodada com outro resultado). Para o
  dono: se dois hóspedes reservarem o 301 no site no mesmo instante, os dois recebem a reserva e um vai chegar com o
  quarto ocupado. A causa está na coluna do banco (`ReservationService.java:239-256`, sem transação nem restrição).
- **Segunda via de reserva pendente devolve 200 (pesa):** o texto diz que a segunda via "só existe para reserva
  paga"; `getDocument` não confere a situação (`ReservationHandler.java:339-361`) e entrega o comprovante de uma reserva
  que ninguém pagou. A recepção pode imprimir um comprovante de uma reserva que não existe de fato.
- `amountPaid` sai como `0.0` e não `0.00`: o handler converte para `double` (`ReservationHandler.java:388`). Os
  valores que importam (599.76, 37.48) saem certos; é um desvio pequeno do "número com 2 casas", e não pesa como erro.
- "O 201 comporta 3" e "a diária do 201 é 389.90" falharam porque `GET /rooms` devolve `number` como texto `"201"`
  (`Room.java:253`, `RoomHandler.java:529`) e o roteiro procura o número 201. O texto não fecha o tipo; os valores
  estão lá e o próprio roteiro manda `roomNumber` como número e o sistema aceita. Não pesa como erro, mas o pessoal do
  site vai ter de combinar o tipo.

Fora do roteiro, pela leitura: o financeiro e o relatório por plano incluem toda cancelada NON_REFUNDABLE
(`ReportService.java:70-76`, `145-151`), inclusive uma que foi cancelada ainda pendente, que não pagou nada e aparece
com `0.00` e conta como uma reserva no plano. O texto diz "as que deixaram dinheiro com a pousada"; o certo seria
filtrar pelo valor líquido maior que zero. Não muda a nota, porque o roteiro não chegou a esse caso.

## Dúvidas

- Se `number` em `GET /rooms` deveria ser número ou texto em JSON: o texto não diz; contei a favor do agente.
- A ausência total de proteção contra a reserva dupla fica na fronteira entre `com problemas` e `ruim` no banco: dei
  `com problemas` seguindo a escala da skill (que põe a proteção "só dentro do processo" nesse degrau), mas é o defeito
  mais sério do projeto e deveria ser a primeira correção.
- Se uma NON_REFUNDABLE cancelada ainda pendente deve ou não aparecer no financeiro: pela leitura do texto ("deixaram
  dinheiro") não deveria, mas o roteiro não testa e o texto também não dá o exemplo.
