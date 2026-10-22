# Revisão: construção, cenário 09

## Visão geral

O projeto (`projeto/`) tem 6 pacotes: `pousada` (só a montagem, `Main.java`), `pousada.web` (as rotas Javalin e a leitura do JSON, `Api.java`), `pousada.service` (os casos de uso da reserva e o aviso por e-mail), `pousada.domain` (reserva, preço, planos, quartos, comprovante, dinheiro), `pousada.report` (os quatro relatórios CSV) e `pousada.db` (JDBC puro, sem pool). Um pedido como `POST /reservations/1/confirmation` passa por `Api.java:52` → `ReservationService.confirm` (`ReservationService.java:59`), que abre uma transação (`Database.java:44`), lê a reserva no repositório, aplica a transição do domínio (`Reservation.java:54`), grava e guarda o e-mail na tabela `messages`. Os dados ficam no PostgreSQL, em duas tabelas (`reservations` e `messages`) criadas com `CREATE TABLE IF NOT EXISTS` (`Database.java:67-100`). Os quartos ficam só no código (`RoomCatalog.java:7-12`). Os relatórios seguem um Template Method limpo (`Report.java:26-35`, `final`), com as quatro subclasses. O comportamento passou inteiro (64/64). Os pontos fracos estão no banco: não há migrações, nenhuma restrição protege as regras, a sobreposição de reservas só é barrada por um `synchronized` que vale dentro de um processo, e não há pool. Nos testes, tudo está concentrado em dois métodos, um deles enorme, e várias regras ficaram sem conferência.

## Arquitetura

**Números:** 6 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin só aparece em `pousada` e `pousada.web`, e o JDBC só em `pousada.db`. A maior classe é `Api.java`, com 134 linhas, e as outras ficam perto disso (123, 121, 117, 101).
**Nota:** bom

- **Borda HTTP fina:** cada rota é uma linha que lê a requisição, chama o serviço e escreve a resposta (`web/Api.java:49-58`). O handler só valida o formato da entrada (`Api.java:75-105`), que é papel da borda. As exceções de negócio viram o JSON de erro num lugar só (`Api.java:59-64`).
- **Regras sem Javalin e sem SQL:** as transições ficam em `domain/Reservation.java:54-75`, o preço em `domain/Pricing.java:25-34` e o desconto e a devolução no enum `domain/RatePlan.java:6-33`. O serviço só orquestra (`service/ReservationService.java:84-92`) e conhece o `Database` apenas para demarcar a transação, sem montar SQL.
- **Dados isolados:** todo o SQL está em `db/ReservationRepository.java` e `db/MessageRepository.java`.
- **Montagem num lugar:** `Main.java:30-41` cria e liga todas as peças, e nenhuma classe faz `new` das suas dependências por dentro.
- Ressalvas menores, que não mudam a nota:
  - `web` depende de `db` porque `GET /messages` chama o repositório direto (`Api.java:57`), sem passar por um serviço.
  - `report` depende de `db` (`MonthlyReport.java:39`) porque os relatórios leem o repositório. Num sistema deste tamanho, as duas coisas são aceitáveis.

## Design de baixo nível

**Números:** 4 de 4 relatórios (ocupação, financeiro, taxa de turismo, planos) passam pelo método-modelo `Report.generate` (`report/Report.java:26`, `final`), e nenhum relatório fica fora da estrutura. A linha de título só existe em `Report.java:30`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.**
- `Report<P>` é uma classe abstrata com o esqueleto fixo (`Report.java:26-35`): ler o período, montar a tabela, escrever o título `Pousada Maré Alta,<nome>,<período>`, o cabeçalho, as linhas e o total. Os passos que variam são abstratos: `name()`, `parameter()`, `parse()` e `table()` (`Report.java:17-24`).
- Os quatro relatórios são subclasses:
  - `OccupancyReport.java:13`;
  - `RevenueReport.java:10`;
  - `TourismTaxReport.java:10`;
  - `RatePlanReport.java:12`.
- Os três mensais compartilham numa classe intermediária a validação do mês e a seleção "começa no mês, por entrada e número" (`MonthlyReport.java:27-44`). É um segundo nível do mesmo template, sem repetir a sequência.
- A linha de data pedida para o ano que vem entra só em `generate`, então muda em todos de uma vez. A taxa da prefeitura é um parâmetro do construtor (`TourismTaxReport.java:13`, configurado em `Main.java:20`).
- O que afasta um pouco: o passo `table()` é grosso. Cada subclasse devolve o cabeçalho, as linhas e o total já como texto (`RevenueReport.java:21-29`), e o esqueleto só garante a ordem e o título, não os passos "selecionar, ordenar, uma linha por reserva". Mesmo assim, nenhum relatório repete a sequência título, cabeçalho, linhas e total, e o `Table` (`Report.java:14`) obriga as três partes. Por isso, é acerto.

**O exagero.**
- `ReportRegistry` (`ReportRegistry.java:8-19`) é usado de verdade pela rota genérica `/reports/{name}` (`Api.java:58,68-73`) e evita uma rota por relatório.
- `Database.SqlFunction` (`Database.java:11`) é uma interface funcional usada pelos repositórios.
- Não há interface com uma implementação só, nem fábrica, nem camada que só repassa: contenção.

**O resto do código.**
- O código usa os tipos certos:
  - `BigDecimal` com `HALF_EVEN` em cada passo (`Money.java:12-14`, `Pricing.java:22,33`, `RatePlan.java:21`);
  - `LocalDate` e `YearMonth`;
  - enums para situação, plano, tipo e canal;
  - o desconto e a devolução polimórficos no `RatePlan` em vez de `if`.
- O estado da reserva só muda pelos métodos de transição, que conferem a situação (`Reservation.java:54-81`).
- Pontos pequenos, que um revisor comentaria sem bloquear:
  - a soma do "retido" se repete em `RevenueReport.java:28` e `RatePlanReport.java:28,31`;
  - `parse` lança `DateTimeParseException` só para pegá-la duas linhas abaixo (`MonthlyReport.java:29-31`, `OccupancyReport.java:35-37`);
  - um nome de relatório desconhecido responde 400 em vez de 404 (`Api.java:70`);
  - `SQLException` vira `IllegalStateException` genérica (`Database.java:40,62`), mas a causa é preservada e registrada no log (`Api.java:62`).

## Testes

**Números:** 2 testes rodaram, 0 falhas, 0 erros, 0 pulados; 2 métodos de teste escritos, num arquivo só (`SystemTest.java`). Cobertura de 92,1% das linhas (456/495) e 65,4% dos ramos (68/104); `pousada.report` tem 86/89 linhas e 9/12 ramos (`measures/tests.md`).
**Nota:** com problemas

**O que é bom:**
- Os testes conferem valores exatos em pontos importantes:
  - o preço de alta temporada e o meio para o par (599,76; desconto de 37,48; total de 212,42, em `SystemTest.java:54-58`);
  - o CSV inteiro dos quatro relatórios, com a linha de título, as linhas em ordem e o total (`SystemTest.java:85-103`). Isso inclui a NON_REFUNDABLE cancelada que continua no financeiro e no relatório por plano, e a cancelada fora da taxa de turismo.
- Pela pergunta da mutação, trocar a taxa de 4,50, tirar o título do `generate` ou trocar `paid()` na taxa de turismo quebraria algum teste.

**O que fica fraco:**
- Tudo está num único método de 48 linhas (`SystemTest.java:62-109`), em que cada passo depende do anterior.
- O teste roda em H2 em memória (`SystemTest.java:26`), não no PostgreSQL de `DB_URL`.
- As recusas só conferem o status HTTP, nunca o código de erro (`SystemTest.java:68-73`).
- Ficam sem rede:
  - os limites da alta temporada: 14 e 15/12, fim de fevereiro e bissexto. Trocar `>= 15` por `> 15` em `Pricing.java:18` não quebra nenhum teste;
  - a devolução do STANDARD cancelado e o cancelamento de uma reserva pendente;
  - o relatório por plano com um plano sem reservas;
  - uma reserva pendente no mês, que o financeiro e o relatório por plano teriam de ignorar. Trocar `signum() > 0` por `>= 0` em `MonthlyReport.java:48` não quebra nenhum teste, porque não há reserva pendente em março no cenário do teste;
  - a regra "quem sai naquele dia não conta" na ocupação, que só é conferida pelo total (`SystemTest.java:87`).

## Banco

**Números:** 2 tabelas (`reservations`, `messages`), ambas com chave primária. O dinheiro está em `numeric(12,2)` e as datas em `date`. 0 chaves estrangeiras, 0 CHECK, 0 UNIQUE/EXCLUDE. O esquema é criado por `CREATE TABLE` no código, com 0 arquivos de migração. Pool: 0, com 1 `DriverManager.getConnection`, 1 transação explícita e 0 `FOR UPDATE`. Os dois candidatos a SQL montado com `+` concatenam só a constante `COLUMNS` (`measures/database.md`).
**Nota:** com problemas

**O que está certo:**
- os tipos (`Database.java:72-95`);
- o SQL todo com `?` (`ReservationRepository.java:30-112`), e a concatenação de `ReservationRepository.java:75,86` usa só a constante da linha 19;
- as conexões em try-with-resources (`Database.java:36,48`);
- a transação que junta a mudança de situação com o e-mail (`ReservationService.java:84-92`).

**Os defeitos:**
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` em `Database.java:71,90`, sem migração. A primeira coluna nova numa instalação que já tem dados não será criada.
- **A regra de não ter duas reservas no mesmo quarto e na mesma noite só é protegida dentro do processo.** A consulta de sobreposição (`ReservationRepository.java:98-112`) roda dentro de uma transação READ COMMITTED sem trava, e quem impede a corrida é o `synchronized` de `ReservationService.java:32`. Com duas instâncias do serviço, as duas reservas passariam. Não há restrição `EXCLUDE` nem trava no banco.
- **A confirmação não trava a linha.** `change` lê a reserva sem `FOR UPDATE` (`ReservationService.java:86`, `ReservationRepository.java:75`). Dois `confirmation` simultâneos podem ambos ver PENDING, confirmar e gravar dois e-mails.
- **Faltam a tabela de quartos e as restrições.** Não há tabela de quartos nem chave estrangeira de `room_number` (os quartos vivem em `RoomCatalog.java:7-12`). Também não há CHECK em `status`, `rate_plan`, `guests >= 1` ou `check_out > check_in`.
- **Não há pool.** Uma conexão nova a cada consulta (`Database.java:26-28`).
- **Os relatórios trazem a tabela inteira para filtrar em Java.** `reservations.findAll()` em `MonthlyReport.java:39` e `OccupancyReport.java:46`. Pesa à medida que as reservas crescem.

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`):
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 09, os relatórios: 13/13;
- duas reservas ao mesmo tempo: 2/2, em 10 rodadas, sempre uma criada e a outra recusada com 409.

**Nota:** bom

O sistema faz o que o dono da pousada descreveu:
- preços e arredondamento;
- recusas com os códigos certos;
- confirmação com o e-mail e o comprovante;
- check-in, check-out e cancelamentos com as devoluções certas;
- os quatro relatórios com a linha de título e os valores pedidos. Isso inclui a taxa de turismo de um mês vazio com total 0.00 e o plano sem reservas aparecendo com 0.

Os dados continuam depois de reiniciar. As reservas simultâneas passaram, mas pelo `synchronized` de um processo só, o que pesa na coluna do banco e não aqui.

## Dúvidas

- A classe do pattern: o passo variável `table()` é grosso (cada subclasse monta cabeçalho, linhas e total como texto), mas o esqueleto `final` controla a ordem e o título dos quatro relatórios. Dei `acerto`. Um leitor mais estrito poderia querer passos mais finos (`header()`, `row(r)`, `total(rs)`), mas não há repetição da sequência que justifique `malfeito`.
- O relatório desconhecido responder 400 em vez de 404 (`Api.java:70`) é uma escolha num ponto que o texto não fecha.
