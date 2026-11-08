# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin com seis pacotes: `infrastructure.web` (os controllers), `domain.service` (os serviços), `domain.service.rateplan` (os planos de tarifa), `domain.model`, `infrastructure.database` (os repositórios JDBC com HikariCP) e a montagem em `Application.main`. Uma requisição passa pelo controller (`ReservationController.createReservation`, `ReservationController.java:32`), que lê o corpo como `Map`, chama o `ReservationService`, que por sua vez pede o plano ao `RatePlanRegistry` e grava pelo `ReservationRepository`. O esquema é criado por `CREATE TABLE IF NOT EXISTS` no código (`DatabaseInitializer.java:21-61`). A organização em camadas é boa e os cálculos dos planos dão os valores do texto. O sistema, porém, tem defeitos graves de dados:
- `guest_cpf VARCHAR(11)` (`DatabaseInitializer.java:36`) faz toda reserva com CPF formatado (`111.222.333-44`) cair em 500. Por isso o roteiro passou só 19 de 72 conferências.
- O `DELETE FROM rooms` a cada partida (`DatabaseInitializer.java:66`) bate na chave estrangeira. Assim que existe uma reserva, o serviço **não sobe mais** (testei: `violates foreign key constraint "reservations_room_number_fkey"`).
- A releitura de uma reserva cancelada zera o `amountPaid` (`ReservationRepository.java:234-237`), e com isso o relatório financeiro perde as canceladas que deixaram dinheiro.
- Nada impede duas reservas simultâneas do mesmo quarto: 10 de 10 rodadas duplicaram com CPF só de dígitos.

## Arquitetura

**Números:** 6 pacotes, 10 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em `infrastructure.web` e em `Application`, e o JDBC só em `infrastructure.database` e em `Application`. A maior classe é `ReservationController` (258 linhas), seguida de `ReservationRepository` (241).
**Nota:** com problemas

O que está bom:
- A montagem fica num lugar só: `Application.java:28-69` cria o pool, os repositórios, os serviços e os controllers, e liga as rotas.
- As dependências vão da borda para as regras (`web -> domain.service`) e das regras para os dados (`domain.service -> infrastructure.database`), sem ciclo.
- Os serviços não conhecem o `Context` nem montam SQL. Todo o SQL fica nos repositórios (`ReservationRepository.java`, `RoomRepository.java`, `MessageRepository.java`).
- Os planos (`domain/service/rateplan`) dependem só do `domain.model`.

Os defeitos são localizados:
- **Uma regra de negócio no handler.** A segunda via só existe para reserva paga, e essa regra mora em `ReservationController.getDocument` (`ReservationController.java:171-177`), não no serviço.
- **O comprovante montado em dois lugares.** O JSON sai no controller (`ReservationController.java:209-226`, que ainda busca o quarto pelo `RoomService`), e o texto sai no `MessageService.generateDocument` (`MessageService.java:40-63`).
- **O controller grande por repetição.** O mapeamento de exceção para HTTP é copiado em cada handler (`ReservationController.java:53-78`, `86-91`, `99-108`, `117-126`, `135-144`, `153-163`, `180-185`), em vez de um `app.exception(...)` central.
- **Erros fora do contrato.** Um id que não é número (`Long.parseLong`, `ReservationController.java:83` e seguintes) dá 500.
- **Dois pontos `static`.** O `RatePlanRegistry` (`RatePlanRegistry.java:8-18`) e o `DatabaseInitializer` são estáticos. Num sistema deste tamanho isso é aceitável.

## Design de baixo nível

**Números:** 5 planos, cada um numa implementação de `RatePlan` (`validate`, `calculateSubtotal`, `calculateDiscount`, `calculateRefund`), escolhidos por um mapa (`RatePlanRegistry.java:8-14`). A validação e a devolução estão nos 5 planos. O desconto passa pela estrutura em 4 planos, e no CORPORATE o serviço nomeia o plano e chama um método fora da interface. O laço da soma das noites está copiado em 4 classes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do Strategy:**
- Cada plano, os dois antigos inclusive, tem a sua classe com preço, validação e devolução: `StandardRatePlan`, `NonRefundableRatePlan`, `CorporateRatePlan`, `WeekendPackageRatePlan` e `LongStayRatePlan`.
- O cancelamento pede ao plano sem nomeá-lo (`ReservationService.java:101-103`).
- A validação também é do plano (`ReservationService.java:48`). Exemplos: sexta a domingo em `WeekendPackageRatePlan.java:21-32`, 7 noites em `LongStayRatePlan.java:16-21`, o código da empresa em `CorporateRatePlan.java:19-23`.

**O que afasta:** o cálculo do preço, justamente o ponto central, ainda pergunta qual é o plano.
- Em `ReservationService.java:60-64` há `if (ratePlanType == RatePlanType.CORPORATE && ratePlan instanceof CorporateRatePlan corporatePlan) discount = corporatePlan.calculateDiscount(subtotal, companyCode)`.
- A interface não recebe o código da empresa, e por isso o método da interface em `CorporateRatePlan.java:40-42` devolve `ZERO` (um desconto errado, se alguém chamar pela interface). O cálculo real é uma sobrecarga fora do contrato (`CorporateRatePlan.java:44-47`).
- O próximo plano que precise de um dado da reserva (o da melhor idade, o de 60 dias de antecedência) vai abrir outro `instanceof` no serviço.

Pela referência, é quem usa a estrutura ainda perguntando qual é o caso: **malfeito**, não acerto.

**Exagero:** não há. A interface tem cinco implementações de verdade, e o registro é um mapa simples. O `ErrorCode` e os DTOs em `Map` não são estrutura sobrando.

**O resto do código:**
- **Duplicação.** O laço da soma das noites com temporada é o mesmo em `StandardRatePlan.java:17-28`, `NonRefundableRatePlan.java:18-29`, `CorporateRatePlan.java:26-37` e `LongStayRatePlan.java:24-35`. Uma mudança na regra da temporada precisa ser feita em quatro lugares. O tratamento de erro também se repete em cada handler (ver Arquitetura).
- **Estado reidratado por transições.** `ReservationRepository.mapReservation` (`ReservationRepository.java:206-240`) reconstrói o estado chamando `confirm()`/`checkIn()`/`checkOut()`/`cancel()` em vez de ler as colunas. O `amount_paid` gravado é ignorado: a cancelada volta com `amountPaid` 0. É um defeito de design que vira dado errado (ver Banco e Comportamento).
- **Erros.** Todo repositório faz `catch (Exception e) { throw new RuntimeException(...) }` (por exemplo `ReservationRepository.java:44-46`). Assim, uma violação do banco (o CPF longo) vira 500 sem código. O `RoomService.getRoomByNumber` devolve `null` (`RoomService.java:19-20`): o `MessageService.java:48` confere, o `ReportService.java:33` não.
- **Tipos.** Os tipos são bons no geral (`BigDecimal` com `HALF_EVEN` em `SeasonCalculator.java:28-30`, `LocalDate`, `enum` para o plano, a situação e o tipo de quarto). Mas o zero sai sem escala (`BigDecimal.ZERO` em `StandardRatePlan.java:32`, `Reservation.java:43`, `NonRefundableRatePlan.java:38`), e o JSON mostra `"discount":0` e `"amountPaid":0` em vez de `0.00`. Além disso, o `NumberFormat.getCurrencyInstance(pt-BR)` (`MessageService.java:16`) escreve `R$` seguido de um espaço não separável (U+00A0), não do espaço comum do texto.

## Testes

**Números:** 9 testes rodaram, 0 falhas, 0 erros, 0 pulados. São 9 métodos, todos numa classe só, `RatePlanTest`. A cobertura é de 10,5% das linhas (85 de 809) e 16,0% dos ramos (24 de 150). O pacote `rateplan` tem 76 de 86 linhas cobertas; `domain.service`, `domain.model`, `infrastructure.database` e `infrastructure.web` estão em 0% ou quase. Não há teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

**O que os testes conferem:** são testes de unidade dos planos, com valores, e conferem os exemplos do texto.
- Temporada 599.76 (`RatePlanTest.java:130-139`).
- Tecnosul 779.80/93.58 (`RatePlanTest.java:44-57`).
- Pacote 820.00 e devolução 410.00 (`RatePlanTest.java:72-86`).
- Estadia longa 1749.30/174.93 (`RatePlanTest.java:101-115`).
- Não reembolsável 116.97 (`RatePlanTest.java:29-41`).
- As recusas de código inválido, de datas fora de sexta a domingo e de 6 noites (`RatePlanTest.java:60-69`, `89-98`, `118-127`).

**O que fica sem rede** (a pergunta da mutação):
- Trocar `0.18` por `0.81` (Agrovale) ou `0.25` (Farmavida) em `CorporateRatePlan.java:14-15`: nenhum teste falha.
- Trocar o preço do pacote standard ou da suíte (`WeekendPackageRatePlan.java:15,17`): nenhum teste falha.
- Trocar `day >= 15` por `day > 15` na temporada (`SeasonCalculator.java:14`): nenhum teste falha.
- Apagar o `if` do `instanceof` em `ReservationService.java:60-64`, o que zeraria o desconto do convênio: nenhum teste falha, porque o teste chama a sobrecarga diretamente (`RatePlanTest.java:53`).
- O convênio sem código (`null`) não é testado.

Nada confere as situações da reserva, a sobreposição de datas, a capacidade, os códigos de erro, o e-mail, o comprovante, os relatórios nem a persistência. Por isso nenhum teste pegou os três defeitos graves: o CPF que derruba a criação, o serviço que não sobe de novo e o `amountPaid` perdido na releitura.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências acusaram:
- uma chave estrangeira sem índice (`reservations.room_number -> rooms`);
- nenhuma restrição que proteja uma regra (nem `CHECK` nas situações, nem `EXCLUDE` contra a sobreposição);
- 0 transações explícitas e 0 `FOR UPDATE`;
- SQL sempre com `?` (`measures/database.md`).

**Nota:** ruim

**Quatro defeitos sérios:**
1. **O serviço não sobe de novo depois da primeira reserva.** `DatabaseInitializer.java:66` faz `DELETE FROM rooms` a cada partida, e `reservations.room_number` referencia `rooms` (`DatabaseInitializer.java:33`). Subi o jar, criei reservas e reiniciei: `Exception in thread "main" ... violates foreign key constraint "reservations_room_number_fkey"`, e o processo terminou. Os dados ficam no banco, mas o sistema fica fora do ar até alguém apagar as reservas à mão.
2. **CPF limitado a 11 caracteres sem validação.** `guest_cpf VARCHAR(11)` (`DatabaseInitializer.java:36`) e nenhuma validação no controller fazem um CPF formatado estourar no `INSERT` (`value too long for type character varying(11)`, em `measures/behavior-service.log`), e a resposta é 500.
3. **Dado errado na releitura.** `mapReservation` (`ReservationRepository.java:224-237`) ignora a coluna `amount_paid` e refaz as transições. A cancelada volta com `amountPaid` 0. A coluna no banco está certa, mas tudo o que lê a reserva de volta (o `GET` e o relatório financeiro) usa o valor errado.
4. **Reserva duplicada.** A disponibilidade é conferida lendo as reservas ativas (`ReservationRepository.java:101-111`, `ReservationService.java:143-149`) e depois inserindo (`ReservationRepository.java:22-47`), sem transação, sem trava e sem restrição. Duas reservas simultâneas do mesmo quarto passam: 10 de 10 rodadas criaram as duas no meu teste.

**Menores:**
- O esquema é criado por `CREATE TABLE IF NOT EXISTS` no código, sem migração versionada, e não sabe evoluir uma tabela que já existe.
- A chave estrangeira não tem índice.
- As colunas de situação e de plano não têm `CHECK`.
- A sobreposição de datas é filtrada em Java sobre todas as reservas ativas do quarto (`ReservationService.java:144-148`).
- As conexões são devolvidas (try-with-resources) e há pool (HikariCP, `Application.java:85-91`).

## Comportamento

**Números:** 19 de 72 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| parte comum | 12 de 46 |
| dados depois de reiniciar | 1 de 3 |
| cenário 01 | 5 de 21 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

**A causa da maior parte das falhas.** O roteiro manda o CPF como `111.222.333-44`, e toda criação de reserva dá 500 (o `VARCHAR(11)` do Banco). Daí em diante quase tudo falha em cascata: confirmação, e-mail, check-in, check-out, cancelamentos, relatórios, os planos novos e as rodadas simultâneas (todas 500/500). O que passou são as recusas que acontecem antes do `INSERT`: capacidade, quarto inexistente, datas, campos faltando, convênio sem código ou com código inválido, pacote fora de sexta a domingo, estadia longa de 6 noites.

**O que vi chamando a API com CPF só de dígitos:**
- **Os cálculos estão certos.** Saíram 599.76; Tecnosul 779.80/93.58/686.22; pacote 820.00 e devolução 410.00; estadia longa 1749.30/174.93/1574.37; convênio devolvendo tudo. O ciclo de situações, o comprovante, o 409 da segunda via de pendente, o CSV de ocupação e o `text/csv; charset=utf-8` também funcionam.
- **O sistema para depois de reiniciar.** Com reservas no banco, o serviço não sobe. Para o dono da pousada, basta reiniciar uma vez para o site e a recepção ficarem fora do ar.
- **O relatório financeiro erra.** Em 2027-03, com uma NON_REFUNDABLE e um pacote cancelados depois de pagos, veio `total,0`. O certo era 492.92 + 410.00. O `GET /reservations/{id}` de uma cancelada também mostra `amountPaid` 0. O contador recebe números errados.
- **O mesmo quarto pode ser vendido duas vezes.** Isso acontece quando dois pedidos chegam juntos (10 de 10 rodadas).
- **O corpo que não é JSON dá 500, não 400.** O `bodyAsClass` lança uma exceção que o controller não captura (`ReservationController.java:34`).
- **O dinheiro zero sai sem as 2 casas.** O JSON traz `"discount":0` e `"amountPaid":0`.
- **O e-mail não fica idêntico ao texto.** A linha sai `R$` + U+00A0 + `599,76`, não com o espaço comum do texto.

**Um ponto em aberto que não pesou:** o `number` do quarto sai como texto (`"201"`), e o roteiro procura o número `201`. Por isso falharam "o 201 comporta 3" e "a diária do 201". O texto não fecha esse tipo, então não pesou.

## Dúvidas

- **O formato do CPF.** O texto não diz se o site manda o CPF com ou sem pontuação. O comprovante mostra `111.222.333-44`, mas isso é o formato de saída. Supor só dígitos é defensável. Mesmo assim, responder 500 (e não 400) a um CPF formatado é erro, e os outros defeitos graves (o serviço que não sobe de novo, o financeiro errado, a duplicação) independem dessa escolha. Por isso a nota do comportamento fica `ruim` de qualquer jeito.
- **A classe do pattern.** Ficou entre `malfeito` e `acerto`. Todos os cinco planos estão na estrutura e só um ponto nomeia o plano. Mas esse ponto é o cálculo do desconto, o centro da variação, e a interface não comporta o dado de que o plano precisa. Por isso escolhi `malfeito`.
