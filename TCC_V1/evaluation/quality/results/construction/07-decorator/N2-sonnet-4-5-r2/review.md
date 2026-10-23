# Revisão: construção, cenário 07

## Visão geral

O projeto tem 9 pacotes: `api` (os controllers do Javalin) e `api.dto`, `service` (reservas, relatórios, comprovante),
`domain` (a reserva com a máquina de estados, o cálculo de preço, os enums), `repository` (JDBC puro), `database`
(abre as conexões e cria as tabelas) e `messaging` com `messaging.rules` (as regras de envio). Uma requisição entra
pelas rotas registradas em `Main.java:61-71`, vai para o controller (`ReservationController.java:32-70`), que converte
o corpo e chama o `ReservationService`. O serviço valida, consulta conflitos e grava pelo `ReservationRepository`.
Todos os dados ficam no PostgreSQL, em três tabelas criadas com `CREATE TABLE IF NOT EXISTS`
(`DatabaseManager.java:27-64`). As regras de e-mail formam uma lista de `MessageRule` percorrida por
`MessageRuleChain`, montada por modo dentro de `MessagingService`. As regras novas do cenário passam todas no roteiro
(14 de 14). O que há de mais sério:

- duas reservas do mesmo quarto na mesma noite são criadas ao mesmo tempo nas 10 rodadas da medição: não há trava,
  transação nem restrição no banco;
- entradas ruins (corpo que não é JSON, campo faltando) dão 500 sem corpo;
- o dinheiro aparece mal formatado em três lugares: o JSON, o CSV financeiro e o comprovante;
- os testes cobrem 12,7% das linhas.

## Arquitetura

**Números:** 9 pacotes, 22 dependências entre eles, 1 ciclo (`messaging` ↔ `messaging.rules`). A maior classe é
`ReservationRepository` (237 linhas). O Javalin só aparece em `api` e no `Main`, e o JDBC só em `repository` e
`database` (`measures/dependencies.md`).
**Nota:** com problemas

As camadas existem e, na maior parte, estão no lugar. As regras (`service`, `domain`, `messaging`) não conhecem o
Javalin nem o JDBC. O SQL está todo nos repositórios, sempre com `?`. A montagem fica no `Main` (`Main.java:32-51`).
Os defeitos são localizados:

- **Uma regra de negócio dentro do handler.** "A segunda via só existe para reserva paga" é decidida no controller,
  e por um atalho (`amountPaid.signum() == 0`) em vez da situação da reserva (`ReservationController.java:160-164`).
- **O controller monta o comprovante.** Ele depende do `RoomRepository` para isso (`ReservationController.java:21`,
  `177-194`), enquanto o texto do comprovante é feito em `DocumentService`. A mesma ideia mora em dois lugares e em
  duas camadas.
- **Um ciclo novo, pequeno, entre dois pacotes vizinhos.** As regras implementam `messaging.MessageRule`
  (`rules/ArchiveCopyRule.java:3`), e `MessagingService` cria as regras concretas com `new`
  (`MessagingService.java:5-7`, `22-33`). Ou seja, a escolha de quais regras valem em cada modo não está no `Main`,
  onde fica o resto da montagem: está dentro do serviço, que recebe um `boolean testMode` (`Main.java:41`).
- **Uma rota que pula o serviço.** `RoomController` fala direto com o repositório (`RoomController.java:17`). Num
  sistema deste tamanho isso pesa pouco.

## Design de baixo nível

**Números:** as 3 regras do texto (o endereço inválido, a cópia para o arquivo, o modo de teste) estão na estrutura,
uma classe cada (`messaging/rules/*`), e nenhuma ficou fora dela. A escolha por modo é um único `if/else` em
`MessagingService.java:26-30`, e nenhuma regra pergunta pelo modo.
**Classe:** alternativa equivalente
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** Não é um Decorator: não há uma interface de envio embrulhada. É uma lista de filtros, `MessageRule`,
percorrida por `MessageRuleChain.process` (`MessageRuleChain.java:12-18`), que para quando uma regra devolve `false`.
A referência aceita isso de antemão, desde que cada regra seja uma peça própria e a combinação por modo fique fora
das regras. As duas condições valem aqui:

- cada regra é uma classe: `EmailValidationRule`, `ArchiveCopyRule`, `TestModeRule`;
- nenhuma regra conhece o modo de teste;
- a cópia só existe fora do teste porque a montagem só a inclui nesse caso (`MessagingService.java:26-30`).

O que afasta do acerto:

- **A combinação está no serviço, não na montagem.** Ela fica dentro do construtor de `MessagingService`, não no
  `Main`. Para a "segunda pousada sem cópia", ou para uma regra "só no teste", alguém vai mexer nesse `if/else` ou
  passar mais booleans.
- **A cópia não passa pelas regras.** Ela vira uma "mensagem adicional" pendurada na original
  (`ArchiveCopyRule.java:16-23`, `OutgoingMessage.java:42-44`) e é gravada direto, sem passar pela corrente
  (`MessagingService.java:42-46`). As regras anunciadas (madrugada, limite por hóspede) não alcançariam a cópia sem
  outra mudança.
- **As regras mexem num objeto mutável compartilhado,** por setters (`OutgoingMessage.java:30-40`). Por isso a ordem
  da lista faz parte da regra.
- **Código morto.** As checagens `isBlocked()` em `ArchiveCopyRule.java:12` e `TestModeRule.java:12` nunca são
  verdadeiras, porque a corrente já parou na validação (`EmailValidationRule.java:10-11`).

**O exagero.** Não há estrutura sobrando. `MessageRule` tem três implementações, `MessageRuleChain` é a própria
corrente, e o resto do código não tem interfaces, fábricas nem camadas que só repassam.

**O resto do código.** Tem defeitos localizados, por isso `com problemas`:

- **Os mesmos `catch` copiados.** Os seis handlers repetem os mesmos blocos `catch` com os mesmos códigos
  (`ReservationController.java:77-83`, `91-100`, `108-117`, `125-134`, `143-152`, `168-174`), quando um
  `app.exception` por tipo resolveria uma vez.
- **Erros mal tratados.** `RatePlan.valueOf(null)` e `LocalDate.parse(null)` lançam `NullPointerException`, que o
  `catch (DateTimeParseException | IllegalArgumentException)` não pega (`ReservationController.java:36-38`, `54`).
  O corpo que não é JSON também escapa. Tudo cai no tratador genérico, que só imprime o stack trace e devolve 500 sem
  corpo (`Main.java:73-76`). Os campos de texto nem são validados: nome ou e-mail ausente só para no `NOT NULL` do
  banco.
- **O dinheiro formatado de três jeitos errados:**
  - `NumberFormat.getCurrencyInstance(pt-BR)` põe um espaço não separável (U+00A0) depois de "R$"
    (`DocumentService.java:12`, `58-60`). Conferi: o e-mail sai com os bytes `R$ \302\240 749,70`;
  - `stripTrailingZeros()` faz o CSV sair `749.7` em vez de `749.70` (`ReportService.java:74-76`);
  - `BigDecimal.ZERO` sem escala faz o JSON sair `"amountPaid":0` (`Reservation.java:53`; também em `88`, para o
    `refund` das NON_REFUNDABLE).
- **O `refund` some do JSON em vez de vir `null`.** `@JsonInclude(NON_NULL)` o omite (`ReservationResponse.java:9`),
  e o texto pede `null` enquanto a reserva não for cancelada.
- **O preço é recalculado a cada leitura.** O construtor de `Reservation` refaz subtotal, desconto e total
  (`Reservation.java:40-42`). Uma mudança na regra de temporada mudaria o total de reservas antigas, já pagas.
- **A validação do e-mail aceita endereços que o texto recusa.** Ela só conta se há algum ponto depois do @
  (`EmailValidationRule.java:39-47`), então `ana@gmail.` e `ana@.com` passam, embora o texto peça o ponto "no meio".
- **O que está bem:** os tipos (`BigDecimal` com `HALF_EVEN` em `PriceCalculator.java:10`, `LocalDate`, `enum` para
  situação, plano, canal e status) e a máquina de estados, que fica na entidade (`Reservation.java:56-90`).

## Testes

**Números:** rodaram 14 testes, com 0 falhas, 0 erros e 0 pulados, e há 14 métodos escritos em 3 arquivos. A
cobertura é de 106 de 835 linhas (12,7%) e de 40 de 140 ramos (28,6%). `service`, `repository`, `api` e `database`
têm 0%, e `messaging.rules` tem 36 de 40 linhas. Não há teste pela API nem contra o banco (`measures/tests.md`).
**Nota:** com problemas

**O que é testado, e com valores:**

- **A temporada,** nas bordas: 14/12 fora, 15/12 dentro, fevereiro e julho
  (`PriceCalculatorTest.java:13-38`);
- **O preço e o desconto:** a noite de alta temporada a 299.88, a soma a 599.76, o desconto de 89.96
  (`PriceCalculatorTest.java:41-70`);
- **A validação do e-mail:** sem @, sem ponto, com @@ (`EmailValidationRuleTest.java`);
- **A corrente de regras:** com a cópia, com o bloqueio e com o modo de teste (`MessageRuleChainTest.java`).

**O que ficou sem rede:**

- **A maior parte das regras pedidas:**
  - a máquina de estados e as recusas;
  - o cancelamento com a devolução;
  - o conflito de datas;
  - a capacidade;
  - os dois relatórios;
  - o texto do comprovante e do e-mail (que tem o defeito do espaço não separável);
  - os códigos HTTP.
- **A pergunta da mutação, na parte nova:**
  - trocar `"[Cópia] "` por outro prefixo em `ArchiveCopyRule.java:8` não derruba nenhum teste, porque
    `MessageRuleChainTest.java:34` só confere o destinatário da cópia;
  - inverter o `if/else` do modo em `MessagingService.java:26-30` também não, porque nenhum teste monta o
    `MessagingService`;
  - o endereço inválido no modo de teste (que deve ficar bloqueado com o endereço original) não tem teste.
- **O arredondamento "meio para o par":** nenhum caso de empate está testado, então trocar `HALF_EVEN` por `HALF_UP`
  (`PriceCalculator.java:10`) passa.

Os testes que existem passam e conferem valores de verdade, mas deixam de fora quase todo o código das regras.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)` e não há data em texto. A
medição acusou:

- uma chave estrangeira sem índice, `reservations(room_number) -> rooms`;
- 5 colunas de situação ou de tipo sem `CHECK`;
- nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações, 0 `FOR UPDATE` e 0 pool: as conexões vêm de `DriverManager` a cada chamada.

Não há SQL montado com `+` (`measures/database.md`).
**Nota:** com problemas

**O que está certo.** Os tipos estão certos: `numeric(10,2)` e `date` (`DatabaseManager.java:43-50`). A reserva está
ligada ao quarto por chave estrangeira (`DatabaseManager.java:51`). Todo SQL usa parâmetros
(`ReservationRepository.java:127-143`), e as conexões são fechadas com try-with-resources. Os dados sobrevivem ao
reinício (3 de 3 no roteiro).

**Os defeitos:**

- **Nada impede duas reservas do mesmo quarto na mesma noite.** O serviço faz "consulta e depois insere" em conexões
  separadas, sem transação nem trava (`ReservationService.java:47-56`). O banco não tem uma restrição `EXCLUDE`
  sobre o intervalo de datas. Nem há um `synchronized`. O roteiro criou as duas reservas nas 10 de 10 rodadas
  simultâneas.
- **O esquema não evolui.** Ele é criado com `CREATE TABLE IF NOT EXISTS` no código (`DatabaseManager.java:27-64`),
  sem migração versionada, e não sabe acrescentar uma coluna numa tabela que já existe.
- **Faltam índices.** Não há índice em `reservations(room_number)`, a coluna da consulta de conflito, nem em
  `check_in`, usado pelo relatório financeiro.
- **Gravações que deveriam andar juntas não andam.** A original e a cópia do e-mail são duas gravações separadas,
  sem transação (`MessagingService.java:39-46`). A confirmação grava a reserva antes e a mensagem depois, também sem
  transação (`ReservationService.java:73-81`).
- **O relatório financeiro não usa índice.** Ele filtra com `EXTRACT(YEAR/MONTH FROM check_in)`
  (`ReservationRepository.java:191-192`), em vez de um intervalo de datas.
- **Os preços do banco não são lidos.** A tabela `rooms` grava `capacity` e `nightly_rate`, mas o código lê só
  `number` e `type` (`RoomRepository.java:48`) e pega o preço do `enum`. O que está no banco não é a fonte de
  verdade.

## Comportamento

**Números:** 60 de 65 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 42 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário, no modo normal | 9 de 9 |
| o cenário, no modo de teste | 5 de 5 |
| duas reservas ao mesmo tempo | 1 de 2 |

**Nota:** com problemas

As regras de envio pedidas funcionam todas pela API:

- `status` com SENT ou BLOCKED;
- a cópia logo depois da original, com "[Cópia] " e o mesmo texto;
- os quatro endereços inválidos bloqueados e sem cópia;
- no modo de teste, o desvio para `testes@`, com "[TESTE] " e a linha "Destinatário original: ", sem a cópia, e o
  endereço inválido bloqueado com o endereço original.

O que falhou, e o que isso quer dizer para o dono da pousada:

- **Duas reservas para o mesmo quarto, ao mesmo tempo, passam as duas.** Isso aconteceu nas 10 rodadas. Se dois
  hóspedes reservarem o mesmo quarto pelo site quase juntos, os dois ficam com reserva e um deles chega sem quarto.
  É a falha mais séria.
- **Um corpo que não é JSON, ou com campo faltando, recebe 500 sem corpo,** em vez de 400 INVALID_REQUEST. O site
  não tem como dizer ao hóspede o que corrigir.
- **O dinheiro no JSON não tem sempre 2 casas.** `amountPaid` vem `0` antes da confirmação, e não `0.00`, ao contrário
  do que foi combinado com o site.
- **O e-mail e o comprovante não trazem "R$ 599,76" igual ao modelo.** Entre "R$" e o número vai um espaço não
  separável. Na tela parece igual, mas quem procurar o texto exato não acha.

Fora do roteiro, verificado à mão:

- o CSV financeiro escreve `749.7` em vez de `749.70` sempre que o valor termina em zero, o que atrapalha o contador;
- o `refund` some do JSON em vez de vir `null`;
- endereços como `ana@gmail.` passam como válidos.

A parte comum não quebra: preço, temporada, desconto, estados, cancelamentos, devoluções e relatórios funcionam. Por
isso a nota não é `ruim`.

## Dúvidas

- **A classe do pattern:** dei `alternativa equivalente`, porque as três regras são peças próprias e nenhuma conhece
  o modo. Mas a combinação por modo está no construtor de `MessagingService` (`MessagingService.java:17-33`), não no
  `Main`. Uma leitura mais estrita do "decidida na montagem" poderia chamar isso de `malfeito`.
- **A nota do banco:** dei `com problemas`, que é o que a skill prevê para a duplicação mal protegida. Mas aqui não
  há proteção nenhuma, nem dentro do processo, e a medição confirmou a reserva dupla em 10 de 10 rodadas. Isso
  aproxima a coluna de `ruim` (risco de dado errado).
- **A nota dos testes:** dei `com problemas`, porque os testes que existem conferem valores, inclusive a parte nova.
  Mas a cobertura de 12,7% e a falta de qualquer teste da reserva, do cancelamento e dos relatórios ficam perto do
  "quase não há testes" de `ruim`.
