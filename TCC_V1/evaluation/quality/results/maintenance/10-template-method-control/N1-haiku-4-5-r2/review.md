# Revisão: manutenção, cenário 10

## Visão geral

O sistema (herdado da base) tem sete pacotes: `hotel.web` (Javalin; `HttpApi` lê a requisição e chama as regras),
`hotel.reservations` (regras da reserva, preço e o `ReservationStore` com o JDBC), `hotel.rooms`, `hotel.reports`,
`hotel.documents`, `hotel.messages` e o `hotel` da montagem (`Hotel`, `Database`). Os dados ficam no PostgreSQL, num
esquema criado pela migração Flyway `V1__create_tables.sql`. Uma requisição de ocupação entra por
`web/HttpApi.java:83-86`, que chama `OccupancyReport.generate`, e o relatório lê as reservas com `store.all()` e monta
o CSV. O pedido era uma coluna `guest` logo depois de `reservation`, com a vírgula do nome trocada por espaço. O agente
mudou só dois arquivos: `reports/OccupancyReport.java` (3 linhas) e `ReportsTest.java` (as duas asserções do CSV). A
mudança está certa e no lugar certo, e as 55 conferências do roteiro passam. O único ponto fraco é que nenhum teste
confere a troca da vírgula.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são os mesmos da base (`measures/dependencies-base.md`). Só muda o tamanho de `hotel.reports`, de 95 para 97 linhas.
**Nota:** bom

A mudança não criou nenhuma dependência nova entre pacotes. `OccupancyReport` já dependia de `hotel.reservations`
(`src/main/java/hotel/reports/OccupancyReport.java:3-4`) e agora só lê o `Guest` pela reserva (`:37`). O ciclo vem da
base: `ReservationService` monta o comprovante (`src/main/java/hotel/reservations/ReservationService.java:3`, `:64`) e
o comprovante lê a reserva (`src/main/java/hotel/documents/ReservationReceipt.java:3`). É pequeno, entre dois pacotes
vizinhos, e o agente não o piorou. A borda HTTP continua fina (`HttpApi.java:83-86`). As regras não usam o Javalin. O
JDBC fica nas classes de acesso (`ReservationStore`, `Rooms`, `SentMessages`), e a montagem fica em `Hotel`.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado, +3/−1 linhas. Nenhuma classe, interface ou método novo. O caso pedido (a
coluna `guest`) está no mesmo `StringBuilder` que já montava as outras colunas.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

Este é o controle negativo do cenário 10. A frase que tenta é "talvez um dia a recepção peça o telefone". O agente
fez exatamente a mudança que `controls.md` descreve como contenção:
- o cabeçalho ganhou `guest` depois de `reservation` (`src/main/java/hotel/reports/OccupancyReport.java:34`);
- o nome é tratado num local, `reservation.guest().name().replace(',', ' ')` (`:37`);
- o nome é acrescentado na posição certa (`:41`).

Não há esqueleto genérico de relatório, nem colunas configuráveis, nem um "formatador de CSV". O `RevenueReport` não
foi tocado.

O resto do código vem da base e é limpo:
- dinheiro em `BigDecimal` (`Reservation.java:17-18`) e datas em `LocalDate`;
- estado mutável só por métodos de pacote (`Reservation.java:102-114`);
- erros de domínio com `ErrorCode` e um `switch` exaustivo para o status HTTP (`HttpApi.java:100-107`).

O único `catch (Exception)` (`HttpApi.java:113`) é da base, tem um comentário que o justifica e transforma o erro em
`INVALID_REQUEST`. Nada disso foi mudado pelo agente.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos escritos (os mesmos 26 da base, nenhum
acrescentado). Cobertura de linhas de 93,8% (473/504; na base, 471/502) e de ramos de 87,1% (101/116, igual à base).
`hotel.reports` tem 50/50 linhas e 22/22 ramos.
**Nota:** com problemas

O agente atualizou as duas asserções do relatório de ocupação com a coluna nova e o nome, com valores exatos
(`src/test/java/hotel/reports/ReportsTest.java:61-63` e `:73-74`). Assim, o cabeçalho, a posição da coluna e a
presença do nome ficam conferidos, e nenhum teste da base foi apagado ou enfraquecido.

A segunda regra do pedido, a troca da vírgula por espaço, ficou sem teste. Todas as reservas do teste usam o mesmo
hóspede, "Bruno Lima", sem vírgula (`ReportsTest.java:31`). Pela pergunta da mutação: se alguém apagar o
`.replace(',', ' ')` de `OccupancyReport.java:37`, nenhum dos 26 testes falha, e o CSV volta a ganhar uma coluna a mais
para quem tem vírgula no nome. Como todos os hóspedes têm o mesmo nome, trocar o nome de uma reserva pelo de outra
também passaria. O teste pela API só confere o total do relatório (`src/test/java/hotel/web/HttpApiTest.java:116`).
O código está certo (o roteiro do comportamento confirma), mas metade da regra nova não tem rede.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. As 6 colunas de dinheiro são
`numeric(10,2)`. Nenhuma chave estrangeira sem índice e nenhuma data como texto. A migração
`V1__create_tables.sql` está igual à da base, e não há migração nova. As conferências só acusam colunas de
situação/tipo em `text` sem `CHECK` (`reservations.status`, `reservations.rate_plan`, `rooms.type`,
`messages.channel`), o que já vinha da base.
**Nota:** bom

O pedido não exigia mudar o banco: o nome do hóspede já é guardado em
`reservations.guest_name text NOT NULL` (`src/main/resources/db/migration/V1__create_tables.sql:22`), e o agente
corretamente não mexeu no esquema nem editou a migração. A base protege a regra de uma reserva por quarto e noite com
um `EXCLUDE USING gist` sobre `daterange(check_in, check_out)`, e o SQL usa parâmetros: os `+` em
`ReservationStore.java:33/83/98/111` só juntam constantes.

Um ponto da base, que o agente não piorou: o relatório lê a tabela inteira de reservas e filtra em Java
(`src/main/java/hotel/reports/OccupancyReport.java:25`, `ReservationStore.java:108-111`). Para uma pousada pequena,
não pesa.

## Comportamento

**Números:** 55 de 55 conferências passaram, 0 falharam:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 10: 4/4;
- duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

Pela API, o relatório de ocupação traz o cabeçalho com `guest` logo depois de `reservation`, a vírgula do nome vira
espaço, e a linha `total` continua igual. O relatório financeiro mantém o mesmo cabeçalho. O resto do sistema (preços,
recusas, confirmação e e-mail, cancelamentos, relatórios, persistência e reservas simultâneas) continua funcionando.
Para o dono da pousada, a recepção já vê o nome do hóspede no CSV, e nada do que funcionava quebrou.

## Dúvidas

A troca da vírgula pelo espaço segue o texto ao pé da letra: "Silva, Ana" vira "Silva  Ana", com dois espaços. Aspas
e quebras de linha no nome também não são tratadas, e o texto não pedia isso. Não contei nenhum dos dois como
defeito.
