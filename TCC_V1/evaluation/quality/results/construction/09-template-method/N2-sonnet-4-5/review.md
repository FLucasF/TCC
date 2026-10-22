# Revisão: construção, cenário 09

## Visão geral

O projeto tem seis pacotes sob `com.pousadamarealta`: `api` (os controladores do Javalin), `service` (preço, reserva,
comprovante e mensagens), `report` (os quatro relatórios), `repository` (JDBC puro com HikariCP), `domain` (entidades
e enums) e o `Main`, que monta tudo. Uma requisição entra numa rota registrada em `Main.java:63-75`, passa por um
controlador (por exemplo `api/ReservationController.java:29`), que chama o `ReservationService`, que por sua vez usa os
repositórios. Estes abrem conexões pelo `Database` estático (`repository/Database.java:12`). Os dados ficam no
PostgreSQL, em três tabelas criadas por `CREATE TABLE IF NOT EXISTS` no código. Os relatórios seguem um Template
Method limpo (`report/ReportGenerator.java:21`), e os quatro passam por ele. O problema mais sério está na criação da
reserva: ela consulta se há conflito e depois insere, sem transação, sem trava e sem restrição no banco
(`service/ReservationService.java:48-70`). No roteiro de reservas simultâneas, o mesmo quarto foi reservado duas vezes
para as mesmas noites em 9 de 10 rodadas. Os testes quase não existem: são 6 testes, com 6,1% de cobertura de linhas.

## Arquitetura

**Números:** 6 pacotes, 13 dependências entre eles, nenhum ciclo. O Javalin só aparece em `api` e no `Main`, e o JDBC
só em `repository`. A maior classe é `ReservationController`, com 200 linhas.
**Nota:** com problemas

As camadas existem e as dependências seguem a direção certa: `api → service → repository → domain`, e `report →
repository`. As regras de preço e de situação da reserva ficam em `service/PricingService.java` e
`service/ReservationService.java:77-142`, sem Javalin e sem SQL. O SQL está todo em `repository/`, sempre com `?`.

Há algumas misturas localizadas:
- **Uma regra no handler.** A regra "a segunda via só existe para reserva paga" está dentro do handler
  (`api/ReservationController.java:123`), e não no serviço.
- **O comprovante montado no controlador.** No check-out e na segunda via, é o controlador que vai ao
  `RoomRepository` e ao `DocumentService` para montar o comprovante (`api/ReservationController.java:108` e `:128`).
  Com isso, a borda HTTP conhece três peças da camada de baixo.
- **Controladores ligados direto ao acesso a dados.** Os controladores de quartos, de mensagens e de relatórios falam
  direto com os repositórios (`api/ReportController.java:29`, por exemplo). Os geradores de relatório fazem a própria
  consulta (`report/RevenueReportGenerator.java:36`).
- **Um estado global.** A montagem acontece no `Main`, mas a conexão com o banco é um `static` global
  (`repository/Database.java:12`), que todo repositório chama por dentro (11 chamadas a `Database.getConnection`).
  Por isso não dá para trocar o banco num teste.
- **Um número fixo no `Main`.** O total de quartos está fixo, `5`, em `Main.java:52`, em vez de vir dos quartos
  cadastrados.

Nada disso é grave, mas são defeitos que alguém precisa corrigir.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto, 0 fora dele. O método-modelo é `final` e tem 5 passos
abstratos. Os relatórios novos não têm nenhuma linha de título copiada.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** `report/ReportGenerator.java:11` é uma classe abstrata com o método-modelo `public final String
generate()` (`:21`). Ele escreve o título "Pousada Maré Alta,<nome>,<período>" num lugar só (`:36-38`), depois o
cabeçalho, as linhas e o total. Os passos que variam são abstratos (`:30-34`), e a formatação comum (data, mês,
decimal) é compartilhada (`:40-50`).

Os quatro relatórios são subclasses e preenchem só os passos: `OccupancyReportGenerator`, `RevenueReportGenerator`,
`TourismTaxReportGenerator` e `RatePlansReportGenerator`. Isso inclui os dois que já existiam no texto. A linha final
com a data, pedida para o ano que vem, entraria só em `generate()`.

Contra a estrutura há dois pontos. Cada subclasse busca as reservas duas vezes, uma em `buildDataLines` e outra em
`buildTotalLine` (`report/RevenueReportGenerator.java:36` e `:54`, `report/OccupancyReportGenerator.java:37` e `:51`),
e repete o mesmo filtro nos dois passos (`report/RatePlansReportGenerator.java:42`, `:67` e `:72`). Falta no esqueleto
um passo "selecionar as reservas" feito uma vez só. Além disso, a taxa de R$ 4,50, que a prefeitura muda todo ano,
está numa constante dentro do relatório (`report/TourismTaxReportGenerator.java:11`). Nada disso tira a classe
`acerto`.

**O exagero.** Não há interface com uma implementação só, nem fábrica, nem camada que só repassa. `PricingService`,
`DocumentService` e `MessageService` são classes simples e têm papel próprio.

**O resto do código:**
- **Duplicação nos relatórios:**
  - Os quatro handlers de relatório são cópias quase idênticas, com o mesmo `try`, a mesma validação e o mesmo
    `contentType` (`api/ReportController.java:20`, `:37`, `:54` e `:71`).
  - A ocupação descobre o tipo do quarto com `if`s sobre o número, fixos no código
    (`report/OccupancyReportGenerator.java:55-60`), duplicando o que já está em `RoomType`.
- **Erros:**
  - `catch (Exception e)` na criação transforma qualquer falha em 400, inclusive um erro de banco
    (`api/ReservationController.java:53`).
  - `Long.parseLong` do id não é tratado (`:60`, `:70` e outros): `GET /reservations/abc` devolve um 500 sem o
    formato de erro `{error, message}`.
  - `Optional.get()` sem cuidado aparece em `api/ReservationController.java:108`.
- **Detalhes do domínio:**
  - `Reservation` é um saco de setters, e a máquina de estados fica toda no serviço com `if`s
    (`service/ReservationService.java:77`, `:103`, `:125` e `:137`). É aceitável neste tamanho.
  - `amountPaid` nasce como `BigDecimal.ZERO` sem escala (`domain/Reservation.java:26`), por isso sai `0` no JSON.
  - O comprovante usa `NumberFormat.getCurrencyInstance(pt-BR)` (`service/DocumentService.java:13`), que põe um
    espaço inseparável depois de "R$". O texto pede um espaço comum.

## Testes

**Números:** 6 testes rodaram, nenhum falhou nem foi pulado, e há 6 métodos de teste no código, em 2 arquivos. A
cobertura é de 55 de 898 linhas (6,1%) e de 11 de 136 ramos (8,1%). Os pacotes `api` e `repository` e o `Main` têm
0%. Não há nenhum teste pela API nem contra o banco.
**Nota:** ruim

O que existe confere valores e está certo:
- `PricingServiceTest` confere a diária fora de temporada, a diária de alta temporada (299.88), a soma de 10 a 12/07
  (599.76) e o desconto de 15% (116.97) (`src/test/java/com/pousadamarealta/service/PricingServiceTest.java:17-63`).
- `ReportGeneratorTest` confere só a ordem dos passos do esqueleto, numa subclasse falsa
  (`src/test/java/com/pousadamarealta/report/ReportGeneratorTest.java:14-23`). Do título, confere apenas que começa
  com "Pousada Maré Alta,".

A parte nova do cenário não tem nenhum teste. Nenhum teste chama `TourismTaxReportGenerator`,
`RatePlansReportGenerator`, `RevenueReportGenerator` ou `OccupancyReportGenerator`, e o pacote `report` tem 0 de 30
ramos cobertos.

A pergunta da mutação: se alguém trocasse 4.50 por 5.40 (`report/TourismTaxReportGenerator.java:11`), tirasse o
filtro `isPaid()` (`:41`) ou trocasse o `> 0` do filtro do relatório por plano por `>= 0`
(`report/RatePlansReportGenerator.java:42`), nenhum teste falharia.

Também ficaram sem teste:
- as transições de situação da reserva;
- o cancelamento e a devolução;
- os códigos de erro;
- o conflito de datas;
- o comprovante e o e-mail.

A coluna está quase ausente.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As
conferências acusaram uma chave estrangeira sem índice (`reservations.room_number → rooms`), nenhuma restrição
CHECK/UNIQUE/EXCLUDE, nenhuma transação explícita e nenhum `FOR UPDATE`. O esquema é criado no código, sem migração.
**Nota:** ruim

Os tipos estão certos, o SQL usa sempre parâmetros, as conexões são devolvidas com try-with-resources e há um pool
HikariCP (`repository/Database.java:14-21`). Os dados sobrevivem a um reinício (3 de 3 no roteiro).

O defeito sério é que nada impede duas reservas do mesmo quarto na mesma noite. `ReservationService.createReservation`
consulta os conflitos (`service/ReservationService.java:48`, que usa `repository/ReservationRepository.java:98`) e
depois insere (`service/ReservationService.java:70`). As duas operações rodam em conexões separadas, em autocommit,
sem trava, sem transação e sem uma restrição de exclusão no banco. Não há nem um `synchronized`. O roteiro do
comportamento mostrou o resultado: as duas reservas simultâneas foram criadas em 9 de 10 rodadas, um quarto vendido
duas vezes.

Também:
- O esquema é feito com `CREATE TABLE IF NOT EXISTS` no código (`repository/Database.java:30`, `:37` e `:59`). Isso
  não sabe evoluir uma tabela que já existe.
- A chave estrangeira (`:54`) não tem índice.
- `status`, `rate_plan` e `type` são `varchar` sem CHECK.
- A confirmação grava a reserva e a mensagem em duas escritas separadas, sem transação
  (`service/ReservationService.java:83` e `:90`).

## Comportamento

**Números:** 59 de 64 conferências passaram:
- a parte comum: 42 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09 (os relatórios): 13 de 13;
- as reservas simultâneas: 1 de 2.
**Nota:** com problemas

Os quatro relatórios do cenário saem certos: o título "Pousada Maré Alta,<nome>,<período>", a taxa de turismo com
R$ 4,50 por hóspede por noite só das pagas, o relatório por plano com STANDARD e NON_REFUNDABLE mesmo sem reserva e
contando a não reembolsável cancelada, e a ocupação e o financeiro como o texto pede. Preço, desconto, recusas,
transições e devoluções também passam.

As falhas:
- **"O 201 comporta 3" e "a diária do 201 é 389.90".** As duas falharam só porque o roteiro compara `number` como
  número, e o sistema devolve `"201"` como texto. Confirmei com `curl` que `GET /rooms` traz `capacity: 3` e
  `nightlyRate: 389.90` para o 201. O texto não fixa o tipo do número do quarto, então essas duas falhas não pesam.
- **`amountPaid` sai `0` e não `0.00` antes da confirmação** (`domain/Reservation.java:26`). Contraria "número com 2
  casas". É um defeito pequeno, mas real.
- **O e-mail traz "Total: R$ 599,76" com um espaço inseparável (U+00A0) depois de "R$".** Vi os bytes `302 240` com
  `od`. O comprovante e o e-mail parecem iguais na tela, mas não são o texto pedido, e uma busca ou comparação pelo
  texto falha. A causa é `service/DocumentService.java:13`.
- **As reservas simultâneas: 9 de 10 rodadas criaram as duas reservas.** Para o dono da pousada, isso quer dizer que
  dois hóspedes podem pagar pelo mesmo quarto na mesma noite se reservarem quase ao mesmo tempo pelo site. O defeito
  pesa na coluna do banco e fica só citado aqui.

Fora do roteiro, `GET /reservations/abc` devolve 500 sem o corpo de erro (`api/ReservationController.java:60`). O
texto não fala de id inválido.

## Dúvidas

- Banco `ruim` ou `com problemas`: a skill põe "duplicação protegida só dentro do processo" como `com problemas`.
  Aqui não há proteção nenhuma, e a duplicação aconteceu em 9 de 10 rodadas, um risco concreto de dado errado. Por
  isso escolhi `ruim`.
- O espaço inseparável no valor do comprovante pode ser visto como detalhe de formatação. Contei como defeito
  pequeno, porque o texto mostra o formato exato.
