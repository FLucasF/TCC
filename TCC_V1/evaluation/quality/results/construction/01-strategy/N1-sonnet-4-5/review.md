# Revisão: construção, cenário 01

## Visão geral

O projeto tem seis pacotes: `br.com.marealta` (só a classe `Application`), `domain`, `domain.rateplan`, `repository`, `service` e `web.dto`. Uma requisição entra numa lambda do Javalin dentro de `Application.main` (`Application.java:53-217`). A lambda valida e converte os campos, chama um serviço (`ReservationService`, `ReportService` ou `MessageService`), e o serviço carrega e grava pelo repositório JDBC. A resposta sai por um DTO, e é o DTO que calcula o preço (`ReservationDTO.java:27-29`). Os dados ficam no PostgreSQL, em três tabelas que o próprio código cria com `CREATE TABLE IF NOT EXISTS`. Os planos de tarifa seguem uma interface `RatePlan` com uma classe para cada um dos cinco planos, e os 21 valores do cenário conferem pela API. O defeito mais sério é que nada impede duas reservas do mesmo quarto na mesma noite quando os pedidos chegam juntos. Não há restrição no banco, nem trava, nem transação, e o roteiro criou as duas reservas em 9 de 10 rodadas. Há ainda três defeitos menores:
- o convênio fica fora da estrutura no cálculo do desconto (`Reservation.java:99-101`);
- um corpo que não é JSON responde 500;
- a segunda via de uma reserva pendente responde 400, quando o texto pede 409.

## Arquitetura

**Números:** 6 pacotes e 10 dependências entre eles, com 1 ciclo entre `domain` e `domain.rateplan`. Javalin e Jackson só aparecem em `br.com.marealta`, e JDBC em `repository` e `br.com.marealta`, onde é só o pool (`measures/dependencies.md`). A maior classe é `Application` (263 linhas), seguida de `ReservationRepository` (254).
**Nota:** com problemas

O que está bem:
- **A montagem fica num lugar só.** O `main` cria o pool, os repositórios e os serviços e os liga por construtor (`Application.java:28-41`).
- **Os serviços não conhecem o Javalin nem o JDBC.** Eles só falam com os repositórios (`ReservationService.java:22-76`).
- **O SQL está todo nos três repositórios.**

Os defeitos:
- **As rotas moram todas no `main`.** O `main` sozinho tem 160 linhas de handlers (`Application.java:53-217`), e cada handler repete o mesmo `try/catch` que traduz exceção em código HTTP (`Application.java:111-157`, cinco vezes).
- **Uma regra de negócio está na borda.** Quem decide se a segunda via existe é o handler, com `if (!reservation.getStatus().isPaid())` (`Application.java:164-167`), e não o serviço.
- **Os DTOs da web fazem conta.** `ReservationDTO.from` e `DocumentDTO.from` chamam `calculateSubtotal/Discount/Total` (`ReservationDTO.java:27-29`, `DocumentDTO.java:23-25`). O preço só passa a existir na hora de serializar.
- **Há um ciclo pequeno entre `domain` e `domain.rateplan`.** `Reservation` importa `RatePlanFactory` e `CorporateRatePlan` (`Reservation.java:3-5`), e os planos importam `DateRange`, `Money` e `RoomType` do `domain`.

Nenhum desses defeitos derruba a coluna, mas o arquivo de rotas e a regra na borda são onde o próximo pedido vai doer.

## Design de baixo nível

**Números:** 5 planos, todos com classe própria que implementa `RatePlan` (`RatePlan.java:7-18`). Há um único `switch` de seleção (`RatePlanFactory.java:6-13`). Fora da estrutura fica um ponto de comportamento: o `instanceof CorporateRatePlan` no cálculo do desconto (`Reservation.java:99-101`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Boa parte está certa:
- Cada plano tem a própria classe, os dois que já existiam inclusive (`StandardRatePlan`, `NonRefundableRatePlan`, `CorporateRatePlan`, `WeekendPackageRatePlan`, `LongStayRatePlan`).
- Em cada classe ficam o preço (`calculateSubtotal`/`calculateDiscount`), a regra de quem pode usar (`validate`) e a devolução (`calculateRefund`).
- O cancelamento pede a devolução ao plano sem nomeá-lo (`Reservation.java:71-72`), e a validação também passa pelo plano (`Reservation.java:48-49`).
- As regras do sexta-a-domingo, das 7 noites e do código da empresa estão nas classes dos planos (`WeekendPackageRatePlan.java:18-22`, `LongStayRatePlan.java:12-16`, `CorporateRatePlan.java:19-26`).

O que afasta do acerto é o cálculo do desconto. Quem usa a estrutura ainda pergunta qual é o caso: `if (ratePlan instanceof CorporateRatePlan corporate) return corporate.calculateDiscount(subtotal, companyCode)` (`Reservation.java:99-101`). Pela interface, o convênio responde desconto zero (`CorporateRatePlan.java:33-36`). Quem chamar `RatePlan.calculateDiscount` sem conhecer essa exceção recebe um preço errado.

O dado do convênio (`companyCode`) também vazou para a assinatura comum, `validate(..., String companyCode)` (`RatePlan.java:9`), que os outros quatro planos recebem e ignoram. O plano não guarda os próprios parâmetros, então o próximo plano com um dado próprio vai pedir outro `instanceof` ou outro parâmetro em todas as classes. Por isso a classe é `malfeito` e não `acerto`.

**O exagero.** Não há estrutura sem necessidade. A `RatePlanFactory` é o ponto único de seleção que o pattern aceita, e não há interface com uma implementação só nem camada que só repassa. O único sobrante é `getType()` (`RatePlan.java:17`), que ninguém chama: é código morto, não estrutura.

**O resto do código.** O dinheiro é bem tratado: `Money` com `BigDecimal` e meio-para-o-par a cada passo (`Money.java:8-13`, `Money.java:43-46`), e as datas são `LocalDate` com `DateRange`. Os defeitos:
- **O plano é texto solto.** Ele circula como `String` (`Reservation.java:16`), e a fábrica o resolve de novo a cada chamada (`Reservation.java:48`, `:71`, `:91`, `:96`). `calculateTotal` calcula o subtotal duas vezes (`Reservation.java:97`, `:107`).
- **O preço da alta temporada mora num intervalo de datas.** O acréscimo de 20% e as datas da temporada estão em `DateRange` (`DateRange.java:34-63`), que passa a saber de preço.
- **O mesmo cálculo de diárias está copiado em quatro planos** (`StandardRatePlan.java:14-16`, `NonRefundableRatePlan.java:16-18`, `CorporateRatePlan.java:29-31`, `LongStayRatePlan.java:19-21`).
- **O estado muda sem controle.** `Reservation` tem `setStatus`, `setAmountPaid` e `setRefund` públicos (`Reservation.java:164-182`), que passam por cima das transições protegidas em `confirm`/`cancel`/`checkIn`/`checkOut`.
- **Exceções genéricas viram código HTTP.** `IllegalArgumentException` vira 400 e `IllegalStateException` vira 409 (`Application.java:92-93`, `:118-119`). Um erro de programação desses tipos sai como erro do cliente.
- **Há código morto e uma utilidade estática no lugar de serviço.** A exceção `CapacityExceededException` nunca é usada (`ReservationService.java:96-100`). `DocumentService` é só uma função estática (`DocumentService.java:11`).

São defeitos localizados, que não tornam o código difícil de mudar.

## Testes

**Números:** 15 testes rodaram e passaram (0 falhas, 0 erros, 0 pulados), e há 15 métodos de teste escritos. A cobertura é de 47,7% das linhas (378 de 792) e 36,9% dos ramos (62 de 168). O pacote `br.com.marealta` (rotas) tem 0 de 173 linhas cobertas, e `web.dto` tem 0 de 52 (`measures/tests.md`).
**Nota:** com problemas

O que está bem: o agente testou, com valores, os três exemplos do texto e o exemplo da alta temporada:
- Tecnosul 779,80 / 93,58 / 686,22 (`src/test/java/br/com/marealta/domain/RatePlanTest.java:32-47` e `IntegrationTest.java:96-110`);
- o pacote deluxe 820,00, que devolve 410,00 no cancelamento (`RatePlanTest.java:50-65`);
- a estadia longa 1.749,30 / 174,93 / 1.574,37 (`RatePlanTest.java:68-83`);
- julho 599,76 (`RatePlanTest.java:14-29`).

As recusas dos planos também têm teste (`RatePlanTest.java:86-122`), e o arredondamento meio-para-o-par está em `MoneyTest.java:12-24`.

O que falta:
- **Nenhum teste passa pela API.** As rotas têm 0% de cobertura, e por isso nenhum teste viu o 500 do corpo inválido nem o 400 da segunda via.
- **Os testes de integração não usam o banco do sistema.** Eles rodam em H2 no modo PostgreSQL (`IntegrationTest.java:157`), não no PostgreSQL.
- **Ficaram sem teste:**
  - as devoluções do convênio, da estadia longa e do não reembolsável;
  - Agrovale e Farmavida;
  - o comprovante e o e-mail;
  - os dois relatórios;
  - as transições recusadas (409);
  - o limite da temporada em 15/12.

**A pergunta da mutação.** Alguns erros passariam sem que nenhum teste falhasse:
- trocar o 18 da Agrovale por 81 (`CorporateRatePlan.java:14`);
- fazer o convênio não devolver nada (`CorporateRatePlan.java:45`);
- trocar `day >= 15` por `day > 15` (`DateRange.java:42`).

O teste unitário do convênio chama direto a sobrecarga com o código (`RatePlanTest.java:42`) e não pega o desconto zero que a interface devolve. A integração é que confere o caminho real (`IntegrationTest.java:107-109`). O desconto de 15% do não reembolsável só é testado no `Money` (`MoneyTest.java:29`), não no plano.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro é `numeric(10,2)` (`amount_paid` e `refund`) e as datas são `date`. Há 0 chaves estrangeiras e 0 restrições CHECK, UNIQUE ou EXCLUDE, e as colunas de status, plano e tipo não têm CHECK. O esquema é criado por `CREATE TABLE` no Java, sem migrações. O código tem 0 transações explícitas e 0 `FOR UPDATE` (`measures/database.md`). No comportamento, a reserva duplicada foi criada em 9 de 10 rodadas simultâneas.
**Nota:** ruim

**O defeito sério: nada protege a regra de que um quarto não pode ter duas reservas na mesma noite.** A disponibilidade é conferida numa consulta (`ReservationRepository.java:132-164`) e a reserva é gravada em outra conexão logo depois (`ReservationService.java:34-39`). Não há transação, trava nem `synchronized`, e não há restrição de exclusão no banco. Com dois pedidos simultâneos, o roteiro criou as duas reservas em 9 de 10 rodadas (`measures/behavior.md`). Para a pousada, isso é um quarto vendido duas vezes.

As outras escritas também não estão protegidas:
- **A mudança de situação não confere a situação anterior.** O `UPDATE` não tem `WHERE status = ?` (`ReservationRepository.java:88-92`). Duas confirmações simultâneas passam as duas e mandam dois e-mails.
- **A confirmação e o e-mail não estão na mesma transação** (`ReservationService.java:49-52`).

Outros defeitos:
- **A reserva não está ligada ao quarto.** Não há chave estrangeira de `reservations.room_number` para `rooms`, e o tipo do quarto é copiado em `reservations.room_type` (`ReservationRepository.java:24-25`).
- **O esquema não sabe evoluir.** `CREATE TABLE IF NOT EXISTS` no código (`ReservationRepository.java:21-38`, `RoomRepository.java:20-25`, `MessageRepository.java:18-26`) não consegue acrescentar uma coluna numa tabela que já existe.
- **O preço da reserva não é guardado.** Subtotal, desconto e total são recalculados a cada leitura a partir das constantes do código (`RoomType.java:4-6`, `Reservation.java:90-110`). Se a diária mudar, o total, o comprovante e a segunda via das reservas antigas mudam junto, enquanto o `amount_paid` gravado fica como era.

O que está certo:
- o SQL usa sempre parâmetros `?`, e o script não achou SQL montado com `+`;
- as conexões são devolvidas com try-with-resources;
- há um pool (`Application.java:225-231`);
- os dados sobrevivem ao reinício (3 de 3 no comportamento).

## Comportamento

**Números:** 67 de 72 conferências passaram (`measures/behavior.md`):
- a parte comum: 42 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- duas reservas ao mesmo tempo: 1 de 2.

**Nota:** com problemas

**O cenário funciona inteiro.** Todos os valores e recusas dos planos novos estão certos:
- Tecnosul 686,22, Agrovale 639,44 e Farmavida 584,85;
- o convênio sem código ou com um código que não existe dá 400;
- o pacote sai 520,00 / 820,00 / 1.200,00 sem temporada, só de sexta a domingo, e devolve 410,00;
- a estadia longa sai 1.574,37, com a temporada valendo, e é recusada com 6 noites;
- o convênio e a estadia longa devolvem tudo;
- o `companyCode` é ignorado nos outros planos.

As falhas da parte comum:
- **Corpo que não é JSON: 500 INTERNAL_ERROR, quando o texto pede 400 INVALID_REQUEST.** O `bodyAsClass` lança `JsonParseException` (`Application.java:62`), que nenhum `catch` do handler pega, e o tratador genérico responde 500 (`Application.java:214-217`). Conferi com `curl`. É um erro do agente: o site recebe "erro interno" por um pedido mal formado.
- **Segunda via de reserva pendente: 400 INVALID_REQUEST, quando o texto pede 409 OPERATION_NOT_ALLOWED.** A resposta está fixa no handler (`Application.java:164-166`). O texto diz que a operação que a situação da reserva não permite é 409, então é um erro do agente.
- **A capacidade e a diária do 201 (2 falhas).** `GET /rooms` devolve `"number":"101"` como texto (`RoomDTO.java:8`), e o roteiro procura o quarto pelo número. A capacidade (3) e a diária (389.90) estão certas na resposta, conferido com `curl`. O texto não diz se o número do quarto é texto ou número, então esta falha não pesa.

**Duas reservas ao mesmo tempo:** as duas foram criadas em 9 de 10 rodadas. É o defeito do banco descrito acima e pesa naquela coluna; aqui fica só registrado. Para o dono, quer dizer que dois hóspedes que reservam o mesmo quarto no mesmo instante podem receber os dois a confirmação.

A nota é `com problemas` pelas duas falhas reais da parte comum (o 500 e o 400 no lugar do 409).

## Dúvidas

- **A classe do pattern.** Ficou entre `malfeito` e `acerto`. Os cinco planos estão na estrutura e só o desconto do convênio escapa, por um `instanceof` (`Reservation.java:99-101`). Dei `malfeito` porque é preço, a regra central do plano, e porque a interface responde errado para o convênio (`CorporateRatePlan.java:33-36`). Quem ler isso como um deslize pontual daria `acerto`.
- **A nota do banco.** A lista de `ruim` da skill não cita a reserva duplicada, e a de `com problemas` cita só a proteção "dentro do processo". Aqui não há proteção nenhuma, nem dentro do processo, e a duplicação aconteceu em 9 de 10 rodadas, além das outras faltas (chave estrangeira, transações, preço não guardado). Por isso a nota é `ruim`.
- **O número do quarto como texto.** O roteiro espera número; tratei como um ponto que o texto deixou em aberto.
