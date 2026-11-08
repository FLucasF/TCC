# Revisão: construção, cenário 01

## Visão geral

O projeto tem seis pacotes: `Main` (todas as rotas Javalin num só `main`), `api` (DTOs de entrada e saída), `services`
(reserva, comprovante, relatórios), `rateplans` (a interface `RatePlan`, uma classe por plano e um mapa que escolhe o
plano), `database` (um pool Hikari estático e três repositórios JDBC estáticos) e `models`. Uma requisição entra no
lambda de `Main.java`, que valida os campos, às vezes consulta o repositório direto, chama um método estático de
`ReservationService`, e este pede o preço, a validação e a devolução ao `RatePlan` e grava pelo `ReservationRepository`.
Os dados ficam em três tabelas PostgreSQL criadas por `CREATE TABLE IF NOT EXISTS` no código. Os planos de tarifa
calculam todos os valores do texto (21 de 21 no cenário). O mais sério está fora deles. **O serviço não sobe de novo
sobre um banco que já tem reservas.** `RoomRepository.java:16` faz `DELETE FROM rooms` a cada partida, e isso esbarra
na chave estrangeira de `reservations` (reproduzi: `violates foreign key constraint "reservations_room_number_fkey"`).
Depois da primeira reserva, qualquer reinício deixa a pousada sem sistema. Além disso, nada impede duas reservas do
mesmo quarto ao mesmo tempo: em 8 de 10 rodadas simultâneas, as duas foram criadas.

## Arquitetura

**Números:** 6 pacotes, 11 dependências entre eles, nenhum ciclo. `Main` tem 294 linhas e é a maior classe. Ele usa
Javalin, JDBC e Jackson; `services` usa JDBC (só `SQLException`, sem SQL).
**Nota:** com problemas

As dependências apontam no sentido certo (borda → serviços → planos/dados → modelos, sem ciclo) e o SQL fica todo em
`database/`. Mas a borda não é fina e a montagem não existe:

- **Validação duplicada.** Os handlers validam e consultam o banco por conta própria: campos, datas, `checkOut >
  checkIn` e quarto existente (`Main.java:66-98`, com `RoomRepository.findById` em `Main.java:94`). O serviço repete
  as mesmas validações (`ReservationService.java:30-42`).
- **Regra de negócio no handler.** A regra "segunda via só para reserva paga" mora no handler (`Main.java:224-236`) e
  está errada. Ela olha só `amountPaid` (`Main.java:230`), então uma reserva confirmada e depois cancelada ainda
  recebe a segunda via. O handler também lê o repositório direto (`Main.java:138`, `207`, `235`).
- **Erro escolhido pelo texto da mensagem.** O código HTTP sai de `message.contains("Room not available")`,
  `"capacity"`, `"Rate plan"` (`Main.java:116-128`). Basta reescrever uma mensagem no serviço para o código HTTP mudar.
- **Nada é montado num lugar só.** Tudo é `static`: o pool é um campo estático global (`Database.java:10`), os
  repositórios e os serviços só têm métodos estáticos (`ReservationService.java:18`, `RoomRepository.java:26`), e o
  mapa de planos é preenchido num bloco `static` (`RatePlanFactory.java:9-15`). Não dá para trocar uma peça (num teste,
  por exemplo) sem mexer no código.
- **A exceção do JDBC atravessa as camadas.** Os serviços declaram `throws SQLException`
  (`ReservationService.java:28`, `94`).

## Design de baixo nível

**Números:** 5 de 5 planos têm a sua classe, com preço, validação e devolução (`StandardPlan`, `NonRefundablePlan`,
`CorporatePlan`, `WeekendPackagePlan`, `LongStayPlan`), escolhida por um mapa (`RatePlanFactory.java:7-22`). Um ponto
fora da estrutura: o serviço pergunta `instanceof CorporatePlan` para calcular o preço (`ReservationService.java:66-70`).
A devolução e a validação passam só pela interface (`ReservationService.java:56`, `143`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima do Strategy: a interface `RatePlan` (`RatePlan.java:7-14`), com uma implementação por
plano, os dois antigos inclusive. Cada uma tem a sua regra de uso (sexta a domingo em `WeekendPackagePlan.java:22-33`,
7 noites em `LongStayPlan.java:18-23`, código da empresa em `CorporatePlan.java:22-26`) e a sua devolução
(`WeekendPackagePlan.java:45-47`, `NonRefundablePlan.java:43-45`). O cancelamento pede a devolução ao plano sem nomear
nenhum (`ReservationService.java:143`).

O que afasta: o convênio não cabe na interface. `CorporatePlan.calculatePrice(room, in, out)` lança
`UnsupportedOperationException` (`CorporatePlan.java:29-31`) e há uma segunda assinatura com `companyCode`
(`CorporatePlan.java:33`). Quem usa a estrutura volta a perguntar qual é o plano (`ReservationService.java:66-70`). O
próximo plano que precisar de um dado a mais (a idade, para a melhor idade; a data da reserva, para a antecedência de 60
dias) vai pedir mais um `instanceof` no serviço. O código da empresa já chega a `validateAppliesTo`
(`RatePlan.java:10`), então bastava que o preço recebesse o mesmo contexto. Por isso a classe é **malfeito**, e não
acerto.

**O exagero:** contenção. A única estrutura além dos modelos é o mapa de planos, que é a seleção do próprio pattern.
Não há interfaces de uma implementação só nem camadas que só repassam. Sobra um `isPlanValid` que ninguém usa
(`RatePlanFactory.java:25`).

**O resto do código:**

- **Duplicação.** O laço de noites com a temporada e o arredondamento está copiado em quatro planos
  (`StandardPlan.java:21-31`, `NonRefundablePlan.java:23-33`, `CorporatePlan.java:34-44`, `LongStayPlan.java:27-37`).
  `formatCpf` está duplicado (`ReceiptService.java:42-47` e `ReceiptResponse.java:58-63`). A validação está repetida
  entre `Main` e o serviço.
- **Texto solto no lugar de tipos próprios.** A situação da reserva é texto: `"PENDING"`, `"CONFIRMED"`,
  `"CHECKED_IN"` aparecem soltos (`ReservationService.java:100`, `131`, `157`, `172`) e de novo no SQL
  (`ReservationRepository.java:129`, `147`). O tipo do quarto e o plano também são `String`.
- **Formatação que depende do locale.** O dinheiro sai de `String.format("%.2f")`, que segue o locale da JVM, no CSV
  (`ReportService.java:64`, `69`) e no comprovante (`ReceiptService.java:39`). Numa máquina em pt-BR o CSV sairia
  `599,76`. Os testes comparam com o mesmo `String.format`.
- **O comprovante fora do formato pedido.** Não há separador de milhar: R$ 1.749,30 sai `R$ 1749,30`
  (`ReceiptService.java:38-39`).
- **Erros mal tratados.** `catch (Exception)` nos relatórios responde 400 "formato inválido" até para uma falha do banco
  (`Main.java:269-271`, `286-288`). O corpo que não é JSON não é tratado e vira 500 (`Main.java:64`).
- **Código morto.** `DateTimeFormatter CURRENCY_FORMAT = null` não é usado (`ReceiptService.java:12`).

## Testes

**Números:** 9 testes rodaram, 0 falharam, 0 pulados; 9 métodos de teste. Cobertura: linhas 98 de 710 (13,8%), ramos
23 de 202 (11,4%). `rateplans` tem 92 de 124 linhas; `services`, `database`, `api` e `Main` têm 0. Não há nenhum teste
pela API nem contra o banco.
**Nota:** com problemas

O que está testado confere valores de verdade, e só a parte dos planos:

- os três exemplos do texto: TECNOSUL 779,80 / 93,58 / 686,22 (`CorporatePlanTest.java:18-22`), o pacote deluxe a
  820,00 e a devolução de 410,00 (`WeekendPackageTest.java:19-29`), e a estadia longa 1.749,30 / 174,93 / 1.574,37
  (`LongStayTest.java:19-23`);
- a temporada de julho (`StandardPlanTest.java:26-35`) e o não reembolsável (`NonRefundablePlanTest.java:18-22`);
- a recusa de 6 noites (`LongStayTest.java:27-37`) e a de um código inválido (`CorporatePlanTest.java:26-36`).

A pergunta da mutação:

- **Ficariam sem rede:** trocar 0.18 ou 0.25 (Agrovale e Farmavida nunca são calculados); trocar `FRIDAY` por
  `SATURDAY` (nenhum teste recusa o pacote); fazer o convênio ou a estadia longa devolver zero (nenhum teste de
  devolução nesses planos); mudar o dia 15 de dezembro da temporada.
- **Seriam pegas:** trocar 0.12 por 0.21, e trocar `< 7` por `<= 7` (o teste de 7 noites passaria a falhar).

Fora dos planos não há nenhum teste: as situações da reserva e as transições recusadas, o cancelamento pelo serviço, os
códigos de erro, o comprovante, o e-mail e os dois relatórios. Isso explica que tanto a falha do reinício quanto a
segunda via de reserva cancelada tenham passado sem ser vistas.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` (6 colunas, todas ok), as datas em
`date`, há 1 chave estrangeira com índice e 3 CHECKs (`guests >= 1`, `nights >= 1`, `check_out > check_in`). Nenhum
CHECK sobre `status` ou `rate_plan`, nenhum UNIQUE ou EXCLUDE, 0 transações explícitas, 0 `FOR UPDATE`, nenhum SQL
montado com dado de fora.
**Nota:** ruim

Os tipos estão certos, o SQL usa parâmetros e as conexões voltam ao pool (try-with-resources, `Database.java:24`,
`ReservationRepository.java:29`). Os defeitos:

- **O serviço não sobe sobre o próprio banco.** `initializeRooms` roda a cada partida (`Main.java:40`), apaga os
  quartos e insere tudo de novo (`RoomRepository.java:16-22`). Com uma reserva gravada, o `DELETE` viola
  `reservations_room_number_fkey` e o `main` morre (`Main.java:28`, `throws SQLException`). O texto pede que "nada se
  perca quando o sistema reiniciar". Os dados continuam lá, mas o sistema não volta a funcionar sem que alguém mexa no
  banco à mão. Esse é o defeito sério da coluna.
- **Nada impede a reserva em dobro.** A disponibilidade é um `SELECT` (`ReservationRepository.java:102-119`) seguido de
  um `INSERT` em outra conexão (`ReservationRepository.java:29-56`), sem transação, sem trava e sem restrição de
  exclusão por período. Na medição, 8 de 10 pares simultâneos criaram as duas reservas.
- **Duas escritas que deviam andar juntas e não andam.** A confirmação grava o pagamento
  (`ReservationService.java:105`) e depois a mensagem (`ReservationService.java:119`), sem transação. Se a segunda
  falhar, a reserva fica confirmada sem o e-mail.
- **O esquema não evolui.** Ele é criado por `CREATE TABLE IF NOT EXISTS` dentro do código (`Database.java:27-77`), e
  há uma cópia que ninguém lê em `src/main/resources/schema.sql`. Uma coluna nova não chega a um banco que já existe.
- **Situação e plano sem CHECK.** As colunas `status` e `rate_plan` são `varchar` livres (`Database.java:44`, `50`).

## Comportamento

**Números:** 66 de 70 conferências passaram.

| parte | passaram |
|---|---|
| parte comum | 44 de 46 |
| depois de reiniciar | 0 de 1 |
| cenário 01 | 21 de 21 |
| reservas simultâneas | 1 de 2 |

**Nota:** ruim

O cenário inteiro passa: os três convênios (686,22, 639,44 e 584,85), as recusas do convênio, do pacote e da estadia
longa com 400, o pacote com preço fechado e a devolução da metade (410,00), a estadia longa com a temporada (1.754,30)
e as devoluções integrais. O que falhou:

- **O serviço não sobe de novo sobre o mesmo banco** (a parte do reinício). Para o dono da pousada, isso quer dizer que
  depois da primeira reserva qualquer queda, atualização ou reinício da máquina deixa o site e a recepção sem sistema.
  A causa está no banco (seção acima).
- **Quarto vendido duas vezes.** Em 8 de 10 rodadas, dois hóspedes que reservaram o mesmo quarto ao mesmo tempo
  receberam os dois a reserva.
- **Corpo que não é JSON responde 500**, e não 400 `INVALID_REQUEST`. O `bodyAsClass` não é tratado (`Main.java:64`).
- **Segunda via de reserva pendente responde 404** `RESERVATION_NOT_FOUND`, e não 409 `OPERATION_NOT_ALLOWED`
  (`Main.java:230-232`). A reserva existe, então o 404 engana a recepção.

Achados fora do roteiro, chamando a API e lendo o código:

- No e-mail de confirmação sai uma linha em branco a mais entre o comprovante e "A entrada é a partir das 14h",
  porque o comprovante já termina em `\n` (`ReceiptService.java:33`) e o modelo do e-mail acrescenta mais dois
  (`ReservationService.java:112`).
- Nos comprovantes acima de mil reais falta o ponto de milhar (`R$ 1749,30`).
- Uma reserva confirmada e depois cancelada continua tirando a segunda via (`Main.java:230`).

## Dúvidas

- **A classe do design.** A estrutura cobre os cinco planos em tudo menos num ponto: o `instanceof CorporatePlan` no
  cálculo do preço (`ReservationService.java:66-70`). Dei `malfeito` porque quem usa a estrutura ainda pergunta qual é
  o caso, e a interface obriga o convênio a lançar `UnsupportedOperationException`. Um leitor mais tolerante poderia
  ver ali um acerto com um remendo localizado.
- **A falha do reinício pesa em duas colunas.** Ela é um defeito do banco (o seed que apaga os quartos), mas o roteiro
  do comportamento a mede pela API, e a skill do comportamento põe "o serviço não sobe / os dados somem ao reiniciar"
  entre os motivos de `ruim`. Por isso ela pesa nas duas notas. Sem ela, o comportamento ficaria `com problemas`, por
  causa das reservas simultâneas e das duas recusas com código errado.
