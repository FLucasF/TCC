# Revisão: manutenção, cenário 06

## Visão geral

A base já era um sistema em camadas: `hotel.web` (Javalin, `HttpApi`) lê o JSON, monta um `NewReservation` e chama o
`ReservationService` (`hotel.reservations`), que confere as regras e grava pelo `ReservationStore` (JDBC com `?`) num
PostgreSQL com esquema por migrações do Flyway; o comprovante mora em `hotel.documents.ReservationReceipt`. O agente
fez a mudança pequena que o controle negativo pede: um campo `children` no pedido HTTP, no `NewReservation`, na
`Reservation`, na resposta e no comprovante; a soma adultos + crianças na conferência da capacidade; e uma migração
nova `V2__add_children_column.sql` com `DEFAULT 0` e `CHECK (children >= 0)`. Não criou nenhuma estrutura para a
"cobrança futura" das crianças. O defeito mais sério é de comportamento: a linha "Crianças: 2" do comprovante foi
posta depois da linha de entrada/saída, e não "logo depois da linha do quarto" como o dono pediu
(`src/main/java/hotel/documents/ReservationReceipt.java:46-52`). Os testes novos também são poucos: só a capacidade
foi conferida.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`); idênticos na base (`measures/dependencies-base.md`), só as linhas mudaram (`hotel.reservations` 508 → 517, `hotel.web` 236 → 243, `hotel.documents` 79 → 84).
**Nota:** bom

Cada pedaço da mudança ficou na camada em que a coisa já morava: a leitura do campo opcional e o valor padrão na borda
HTTP (`src/main/java/hotel/web/HttpApi.java:127-133`), a regra da capacidade no serviço, sem Javalin nem SQL
(`src/main/java/hotel/reservations/ReservationService.java:37-38`), o SQL só no `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:18-19`, `:31-33`, `:43`, `:132`), e o texto do comprovante
no `ReservationReceipt`. Nenhuma dependência nova entre pacotes. O ciclo `hotel.documents` ↔ `hotel.reservations`
(o serviço importa `ReservationReceipt`, `ReservationService.java:3`, e o comprovante lê a `Reservation`) veio da base, e o
agente não o piorou. A maior classe continua sendo `HttpApi` (174 linhas, eram 170).

## Design de baixo nível

**Números:** 13 arquivos tocados, todos com mudanças de uma a três linhas por lugar; 0 classes, interfaces, fábricas ou builders novos; 1 migração nova de uma linha.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É exatamente a "contenção" que `references/controls.md` descreve para o cenário 06: um campo a mais no pedido
(`src/main/java/hotel/reservations/NewReservation.java:6`, `src/main/java/hotel/web/CreateReservationRequest.java:15`)
e na reserva (`src/main/java/hotel/reservations/Reservation.java:14`, `:38`, `:70-72`), a soma na conferência da
capacidade (`ReservationService.java:37-38`), a linha no comprovante (`ReservationReceipt.java:50-52`) e a coluna nova
no banco (`src/main/resources/db/migration/V2__add_children_column.sql:1`). A reserva continua criada direto, pelo
construtor do record. A frase sobre "cobrar das crianças maiores no futuro" não gerou nada: nenhuma política de preço de
criança, e o `PriceCalculator` não foi tocado.

O resto do código continua limpo: `int` simples para a contagem, o `null` do campo opcional tratado num lugar só
(`HttpApi.java:127`), o erro com o código que o sistema já usa (`INVALID_REQUEST`, `HttpApi.java:128-130`). Um ponto
pequeno: a regra "não pode ser negativo" ficou só na borda HTTP (`HttpApi.java:128`), enquanto a regra irmã "pelo menos um
hóspede" mora no serviço (`ReservationService.java:31`); quem chamar o serviço direto com `children = -1` passa pela
conferência da capacidade com um total menor e só é barrado pelo `CHECK` do banco, como erro genérico de gravação
(`ReservationStore.java:55`). O banco segura o dado, então isso não derruba a nota. O `NewReservation` passou a ter sete
parâmetros posicionais (`NewReservation.java:6`), o que ainda se lê bem; o agente evitou mexer em todos os testes com uma
sobrecarga do ajudante `stay` (`src/test/java/hotel/reservations/ReservationServiceTest.java:33-37`).

## Testes

**Números:** 27 testes rodaram, 0 falhas, 0 erros, 0 pulados; 27 métodos escritos (a base tinha 26, todos mantidos). Cobertura de linhas 93,6% (481/514; base 93,8%), de ramos 85,2% (104/122; base 87,1%). `hotel.documents`: 32 de 33 linhas e 5 de 6 ramos (a base cobria tudo, 30/30 e 4/4).
**Nota:** com problemas

Os testes da base passam e não foram enfraquecidos; a única mudança neles foi o `0` a mais nos construtores
(`src/test/java/hotel/reports/ReportsTest.java:33`) e `"children": 0` no JSON de exemplo
(`src/test/java/hotel/web/HttpApiTest.java:24`). O agente escreveu um teste novo,
`childrenCountTowardRoomCapacity` (`ReservationServiceTest.java:69-73`), que confere com valores a regra da capacidade:
2 adultos + 1 criança no quarto 101 dá `CAPACITY_EXCEEDED`, e no 201 é aceito. Se alguém apagasse o `+ children` em
`ReservationService.java:37`, esse teste falharia.

O resto da parte nova ficou sem rede:

- a linha do comprovante: nenhum teste lê o texto do comprovante com criança. A única linha não coberta de
  `hotel.documents` é justamente a da criança (`ReservationReceipt.java:51`, 32 de 33). Por isso a linha saiu no lugar
  errado sem nenhum teste acusar (ver Comportamento);
- o padrão zero quando o campo falta: o JSON de exemplo agora sempre manda `"children": 0` (`HttpApiTest.java:24`), e
  nenhum teste manda a reserva sem o campo. Se o `: 0` de `HttpApi.java:127` virasse outro número, nada falharia;
- a recusa do número negativo (`HttpApi.java:128-130`): sem teste; se a conferência fosse apagada, o `CHECK` do banco
  daria outro erro e nenhum teste perceberia;
- criança não muda o preço, o campo na resposta (`ReservationResponse`) e a ida e volta pelo banco
  (`ReservationStore.java:43`, `:132`): sem asserção.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` em todas as 6 colunas; nenhuma chave estrangeira sem índice; nenhuma data em texto; 2 migrações do Flyway (`V1__create_tables.sql` igual à base, `V2__add_children_column.sql` nova); colunas de situação/tipo sem `CHECK` (já assim na base); SQL com `+` só com constantes.
**Nota:** bom

A mudança no esquema foi feita do jeito certo: uma migração nova, sem editar a `V1` que já rodou na pousada, com
`NOT NULL DEFAULT 0`, o que preenche as reservas antigas com zero crianças sem perder nada, e com `CHECK (children >= 0)`
(`src/main/resources/db/migration/V2__add_children_column.sql:1`; no esquema gerado, `measures/database-schema.sql:106`
e `:108`). O código continua com parâmetros: a coluna nova entrou na lista constante `COLUMNS`
(`ReservationStore.java:18-19`) e no `INSERT` com um `?` a mais e os índices renumerados com cuidado (`:31-50`); a
leitura usa o nome da coluna (`:132`). A proteção contra reserva dupla (o `EXCLUDE USING gist`) e a falta de `CHECK` em
`status`, `rate_plan` e `type` vêm da base.

## Comportamento

**Números:** 60 de 61 conferências passaram. Parte comum 46/46; dados depois de reiniciar 3/3; cenário 06, crianças na reserva 7/7; cenário 06, comprovante 1/2; crianças depois de reiniciar 1/1; reservas simultâneas 2/2.
**Nota:** com problemas

Funciona: sem o campo, a reserva vem com `children: 0`; 2 adultos + 1 criança cabem no quarto para três e não mudam o
preço; a consulta mostra as crianças; adultos + crianças acima da capacidade dão `422 CAPACITY_EXCEEDED` (no 201 com
2+2 e no 101 com 2+1); `-1` dá `400 INVALID_REQUEST`; o comprovante sem criança fica como estava; as crianças continuam
lá depois de reiniciar.

Falhou: a linha "Crianças: 2" no comprovante. O dono pediu "logo depois da linha do quarto", e o comprovante tem uma
linha `Quarto: 201 (...)` (`ReservationReceipt.java:46`), mas o agente pôs a linha das crianças depois da linha
`Entrada: ... | Saída: ... | 1 diária` (`ReservationReceipt.java:47-52`). O roteiro achou, depois de `Quarto:`, a linha
`Entrada: 10/05/2027 | Saída: 11/05/2027 | 1 diária`. O texto não deixava isso em aberto. Para o dono, a informação
aparece no e-mail e na segunda via, mas fora do lugar que ele descreveu, e um leitor do comprovante (ou o site, se lê
as linhas) não a encontra onde espera. É um conserto de uma linha, mas é uma parte do pedido que não foi atendida.

## Dúvidas

- A posição da linha "Crianças": leio "a linha do quarto" como a linha `Quarto:` (`ReservationReceipt.java:46`), que é a
  leitura literal e a do roteiro. Quem lesse a linha de entrada/saída/diárias como parte da "linha do quarto" acharia a
  escolha do agente defensável, mas o comprovante tem uma linha que se chama exatamente "Quarto".
- O `README.md` não foi atualizado: o exemplo do corpo de `POST /reservations` (`README.md:62-71`) não mostra o campo
  `children` nem diz que `guests` agora conta só os adultos. Não pesei isso em nenhuma coluna, porque o pedido não falava
  em documentação.
- A conferência do número negativo só na borda HTTP (Design): ponho como observação e não como defeito, porque o
  `CHECK` do banco segura o dado e a única entrada do sistema é a API.
