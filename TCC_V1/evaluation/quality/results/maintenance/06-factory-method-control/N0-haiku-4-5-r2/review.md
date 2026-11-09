# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em 7 pacotes. `hotel.web` (rotas, DTOs) chama `hotel.reservations` (serviço, regras,
`ReservationStore` com JDBC), que usa `hotel.rooms`, `hotel.documents` (comprovante) e `hotel.messages` (e-mail).
Os dados ficam no PostgreSQL, num esquema criado por migrações do Flyway, e tudo é montado em `hotel.Hotel`. O pedido
era pontual: o campo `children`, opcional, padrão 0, sem negativo, somado aos adultos na conferência da capacidade,
sem preço, mostrado na consulta e numa linha "Crianças: N" no comprovante, logo depois da linha do quarto. O agente
fez uma mudança pequena e no lugar certo (`changes.patch`: um campo nos records e na entidade, a soma em
`ReservationService.java:37`, a linha no comprovante, uma migração nova `V2__add_children_column.sql`), sem nenhuma
estrutura nova. Tem dois defeitos. A linha "Crianças" saiu depois da linha de entrada e saída, e não depois da linha
do quarto, como o dono pediu (`ReservationReceipt.java:46` é o quarto, `:47-49` são as datas, `:50-52` são as
crianças). E não há nenhum teste novo para a parte nova.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md` só diferem nas contagens de linhas). Um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base.
**Nota:** bom

A mudança respeitou as camadas que já existiam:
- o campo é lido e defaultado na borda HTTP (`web/HttpApi.java:127`), ao lado da validação de campos obrigatórios que
  a base já fazia ali (`HttpApi.java:116-126`);
- a regra de capacidade fica no serviço (`reservations/ReservationService.java:37`), que não conhece Javalin nem SQL;
- o SQL fica só em `ReservationStore` (`reservations/ReservationStore.java:31-33`, `:43`, `:132`).

O agente não criou dependência nova nem classe nova. O ciclo `documents` ↔ `reservations` vem da base:
`ReservationService.java:3` importa `ReservationReceipt`, e `ReservationReceipt.java:3` importa `Reservation`.

Um reparo menor: a recusa de número negativo (`HttpApi.java:128-130`) ficou na borda, enquanto a regra equivalente
dos adultos mora no serviço (`ReservationService.java:31-33`). É uma validação de entrada e está localizada, por isso
não muda a nota.

## Design de baixo nível

**Números:** controle negativo. Nenhuma classe, interface, fábrica ou builder novo. O patch mexe em 9 arquivos de
`src/main` e cria 1 migração; cada mudança é um campo, um parâmetro, uma soma ou um `if`.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

A frase que tenta o agente ("cobrar alguma coisa das crianças maiores no futuro") não virou estrutura:
- não há política de preço de criança: `PriceCalculator` não foi tocado, e `ReservationService.java:46` continua
  calculando o preço sem olhar as crianças;
- a reserva continua criada direto: `NewReservation.java:6` só ganhou o componente `int children`, e `Reservation`
  ganhou o campo e o getter (`Reservation.java:14`, `:38`, `:70-72`);
- não há builder nem fábrica.

O resultado é exatamente o que `controls.md` descreve como contenção: o campo, a soma na capacidade
(`ReservationService.java:37`), a linha no comprovante (`ReservationReceipt.java:50-52`) e a coluna nova no banco.

O resto do código continua bom:
- o opcional é tratado uma vez só (`HttpApi.java:127`), e daí para dentro o tipo é `int` primitivo;
- o JavaDoc do DTO foi atualizado (`CreateReservationRequest.java:6`);
- os índices do `PreparedStatement` foram renumerados certos (`ReservationStore.java:36-50`).

Duas observações, nenhuma das quais um revisor travaria:
- a mensagem de capacidade continua dizendo "guests" (`ReservationService.java:40`), embora agora conte adultos e
  crianças;
- a regra "não pode ser negativo" fica fora do serviço (ver Arquitetura). Se alguém chamar o serviço direto com
  crianças negativas, quem barra é só o CHECK do banco, e o erro sai como `IllegalStateException`
  (`ReservationStore.java:60`).

O `catch (Exception)` de `HttpApi.java:113` veio da base.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos escritos, os mesmos 26 da base (o agente não
escreveu nenhum). Cobertura de linhas 93,6% (480 de 513), contra 93,8% na base; de ramos 85,2% (104 de 122), contra
87,1% na base. Em `hotel.documents`, 32 de 33 linhas e 5 de 6 ramos; em `hotel.web`, 20 de 32 ramos
(`measures/tests.md` e `measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os testes da base passando e não enfraqueceu nenhum. Só ajustou as chamadas ao construtor, passando
`0` crianças (`ReservationServiceTest.java:36`, `ReportsTest.java:33`). Mas nenhuma regra nova foi testada, e
`grep children src/test` não acha nada além desses dois zeros. A pergunta da mutação:
- apagar o `+ request.children()` em `ReservationService.java:37` não derruba nenhum teste. O único teste de
  capacidade (`ReservationServiceTest.java:60`) usa só adultos;
- apagar ou mudar a linha "Crianças" em `ReservationReceipt.java:50-52` também não derruba nenhum. É a linha sem
  cobertura em `hotel.documents` (32 de 33). É justamente aí que o comportamento falhou, e um teste teria pegado o
  erro de posição;
- tirar a recusa de negativo em `HttpApi.java:128-130` não derruba nenhum. `HttpApiTest.java:24` nem manda o campo.

Os testes existem, passam e cobrem bem a base, mas a parte nova inteira ficou sem rede.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 de 6 colunas), nenhuma chave
estrangeira sem índice, nenhuma data em texto. 2 migrações: `V1` igual à da base e `V2` nova
(`measures/database.md`). As colunas de situação, plano, tipo e canal sem CHECK vêm da base.
**Nota:** bom

O esquema evoluiu do jeito certo, com uma migração nova e sem editar a V1:
`src/main/resources/db/migration/V2__add_children_column.sql:1` acrescenta `children integer NOT NULL DEFAULT 0
CHECK (children >= 0)`. As reservas que já existem ficam com zero crianças, e o banco também protege a regra do não
negativo.

O código continua parametrizado: o `INSERT` tem 15 `?` (`ReservationStore.java:31-33`), e o `+` só junta constantes
(`COLUMNS`, `ReservationStore.java:18-19`). Continua com try-with-resources e pool. A proteção contra reserva dupla é o
`EXCLUDE` da base (`V1__create_tables.sql`), que o agente não tocou. O roteiro confirma que as crianças persistem
depois de reiniciar.

## Comportamento

**Números:** 60 de 61 conferências passaram (`measures/behavior.md`):
- parte comum: 46 de 46;
- reinício: 3 de 3;
- crianças na reserva: 7 de 7;
- comprovante: 1 de 2;
- crianças depois de reiniciar: 1 de 1;
- reservas simultâneas: 2 de 2.

**Nota:** com problemas

Tudo o que já funcionava continua funcionando. A regra nova também funciona:
- sem o campo, são zero crianças;
- a criança não muda o preço;
- a consulta mostra as crianças;
- adultos e crianças acima da capacidade dão 422;
- número negativo dá 400;
- sem criança, o comprovante fica como estava.

A falha é a posição da linha no comprovante. O dono pediu "Crianças: 2" logo depois da linha do quarto, e o roteiro
achou ali a linha "Entrada: 10/05/2027 | Saída: 11/05/2027 | 1 diária": o agente pôs as crianças depois das datas
(`ReservationReceipt.java:46-52`). O texto era explícito nesse ponto, então é erro do agente, e não uma escolha em
aberto. Para o dono da pousada, o comprovante (o do e-mail de confirmação, a segunda via e o do check-out) mostra a
informação certa, só que fora do lugar que ele pediu. A correção é trocar duas linhas de lugar.

## Dúvidas

- Testes: entre `com problemas` e `ruim`. Os 26 testes da base passam e cobrem bem o código, mas nenhum confere a parte
  nova. Fiquei em `com problemas` porque a coluna não está ausente.
- Arquitetura e design: a recusa de crianças negativas na borda HTTP (`HttpApi.java:128-130`), e não no serviço junto
  da regra dos adultos, pode ser lida como uma regra no lugar errado. Julguei que é validação de entrada localizada, e
  que o CHECK do banco cobre o resto, por isso não mudou a nota.
