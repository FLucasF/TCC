# Revisão: manutenção, cenário 06

## Visão geral

O projeto (herdado da base) tem sete pacotes: `hotel.web` (Javalin, a borda HTTP), `hotel.reservations` (as regras e o acesso ao banco da reserva), `hotel.rooms`, `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports` e `hotel` (a montagem). Uma requisição `POST /reservations` passa por `HttpApi.readNewReservation` (`src/main/java/hotel/web/HttpApi.java:109`), que monta um `NewReservation`, vai para `ReservationService.create` (`src/main/java/hotel/reservations/ReservationService.java:27`) e chega a `ReservationStore.add`, que grava no PostgreSQL por JDBC. As tabelas são criadas e evoluem por migrações do Flyway. O agente fez a mudança pedida do jeito mais curto: um campo `children` a mais no pedido HTTP, no `NewReservation`, na `Reservation`, na resposta e no comprovante, a soma `guests + children` na conferência da capacidade, a recusa de número negativo e uma migração nova `V2__add_children.sql`. Os 61 itens do roteiro passaram, os 26 testes da base continuam passando e o agente escreveu mais 6. Nada sério a apontar. Os pontos menores: o README não fala do campo novo, e os testes novos não conferem que a criança não muda o preço nem que a linha "Crianças" vem logo depois da linha do quarto.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais aos da base (`measures/dependencies-base.md`). Só mudou o número de linhas: `hotel.reservations` passou de 508 para 519, `hotel.web` de 236 para 240 e `hotel.documents` de 79 para 84.
**Nota:** bom

Cada pedaço da mudança ficou na camada onde já ficava o que ele estende. A borda HTTP só lê o campo opcional e põe 0 quando ele falta (`src/main/java/hotel/web/HttpApi.java:128`). As regras (o número não pode ser negativo, a capacidade conta adultos e crianças) ficam no serviço, que não conhece Javalin nem SQL (`src/main/java/hotel/reservations/ReservationService.java:34-44`). O SQL fica só em `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:18-19`, `:31-50` e `:132`). O agente não criou pacote, dependência nem classe nova. O ciclo `documents` ↔ `reservations` já existia na base: o serviço monta o comprovante em `ReservationService.java:67`, e o comprovante lê a `Reservation` em `ReservationReceipt.java:3`. O agente não o piorou. Nenhuma classe ficou desproporcional: a maior continua sendo `HttpApi`, com 171 linhas.

## Design de baixo nível

**Números:** 11 arquivos tocados (8 de código, 1 migração, 3 de teste) e nenhuma classe, interface, fábrica ou builder novo. A regra nova está em 2 lugares (a validação e a soma, `ReservationService.java:34` e `:40`). O comprovante ganhou 1 `if` (`ReservationReceipt.java:47-49`) e o banco ganhou 1 coluna.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O controle negativo testa se o agente cria estrutura por causa da frase "cobrar alguma coisa das crianças maiores no futuro". Ele não criou: não há política de preço para criança nem builder ou fábrica de reserva. `PriceCalculator` ficou intacto e nem sequer recebe os hóspedes. O agente pôs o campo no record do pedido (`src/main/java/hotel/reservations/NewReservation.java:6`) e na entidade (`src/main/java/hotel/reservations/Reservation.java:14`, `:38` e `:70-72`), e a reserva continua sendo criada direto, como na base (`ReservationStore.java:53`). A linha do comprovante é um `if` no mesmo estilo da linha de desconto que já existia (`ReservationReceipt.java:47-49`, ao lado de `:54-56`). Isso é exatamente a "contenção" que `references/controls.md` descreve para este cenário.

O resto do código continua no padrão da base: records imutáveis para o pedido e a resposta, `BigDecimal` para dinheiro, `LocalDate` para datas, enums para a situação e o plano. Os detalhes que um revisor notaria vêm da base: o `catch (Exception)` ao ler o corpo (`HttpApi.java:113`, que pelo menos responde 400 e explica por quê) e o estado mutável controlado da `Reservation`. Do agente, só pontos pequenos:
- a mensagem de capacidade ainda diz "guests" (`ReservationService.java:43`), embora agora conte adultos e crianças;
- o teste usa `assertTrue(!...)` em vez de `assertFalse` (`src/test/java/hotel/reservations/ReservationServiceTest.java:204`).

Nada disso precisa ser corrigido antes de aceitar.

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 32 métodos de teste escritos (26 da base e 6 novos). A cobertura é de 94,0% das linhas (482 de 513) e 87,7% dos ramos (107 de 122). Na base era de 93,8% e 87,1%. `hotel.documents` tem 100% de linhas e de ramos.
**Nota:** bom

O agente manteve os 26 testes da base. Só acrescentou o argumento `0` nos auxiliares que montam o pedido (`ReservationServiceTest.java:36` e `src/test/java/hotel/reports/ReportsTest.java:33`), sem apagar nem afrouxar nenhum.

Os testes novos conferem com valores:
- a capacidade: 1 adulto e 1 criança cabem no quarto 101 (capacidade 2), e 1 adulto e 2 crianças no 102 dão `CAPACITY_EXCEEDED` (`ReservationServiceTest.java:164-176`). Trocar `>` por `>=`, ou voltar para `guests() > capacity`, faria esse teste falhar;
- o número negativo: −1 dá `INVALID_REQUEST` (`:179-184`). Apagar a validação faria o teste falhar, porque 1 + (−1) cabe no quarto;
- a linha no comprovante, com 2 crianças e sem nenhuma (`:187-205`);
- pela API, o campo vai e volta, e a consulta lê do banco (`src/test/java/hotel/web/HttpApiTest.java:124-139`), e o valor é 0 quando o campo falta (`:142-147`).

O que ficou sem rede:
- **o preço:** nenhum teste confere que a criança não muda o total. O teste do comprovante (`ReservationServiceTest.java:187-196`) não olha o valor. Se alguém passasse a cobrar por pessoa, nenhum teste do agente falharia;
- **a posição da linha:** o teste só usa `contains("Crianças: 2")` (`:195`) e não confere que a linha vem logo depois da linha do quarto;
- **o número negativo pela API:** a recusa de `children` negativo só é testada no serviço, nunca pela API.

São lacunas pequenas, porque o roteiro de comportamento confere os três pontos e eles passam.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway (`V1__create_tables.sql`, igual à da base, e `V2__add_children.sql`, nova). A tabela `reservations` tem 17 colunas, 3 CHECKs e 1 EXCLUDE. As conferências não acusaram tabela sem chave primária, dinheiro fora de `numeric(10,2)`, chave estrangeira sem índice nem data em texto. As colunas de situação e tipo em texto sem CHECK (`status`, `rate_plan`, `type`, `channel`) vêm da base.
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a que já existia. Ela acrescenta `children integer NOT NULL DEFAULT 0 CHECK (children >= 0)` (`src/main/resources/db/migration/V2__add_children.sql:2`). Com isso, quem já tem reservas no banco sobe sem erro, as reservas antigas ficam com zero crianças e o banco também protege a regra do número não negativo. O SQL continua com parâmetros (`?`): a coluna entrou na lista constante `COLUMNS` (`ReservationStore.java:18-19`), no `INSERT` (`:31-33`, com a numeração dos parâmetros refeita em `:43-50`) e na leitura (`:132`). As conexões continuam em try-with-resources e com pool. O banco não confere a capacidade (adultos e crianças), mas também não conferia na base: a regra mora no serviço.

## Comportamento

**Números:** 61 de 61 conferências passaram:
- a parte comum: 46 de 46;
- depois de reiniciar: 3 de 3;
- as crianças na reserva: 7 de 7;
- o comprovante: 2 de 2;
- as crianças depois de reiniciar: 1 de 1;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer:
- o site pode mandar `children` ou não mandar (sem o campo, são zero crianças);
- a criança não muda o preço;
- dois adultos e uma criança cabem no quarto para três e são recusados (422) no quarto para dois;
- um número negativo é recusado (400);
- a consulta mostra as crianças, e elas continuam lá depois de reiniciar;
- o comprovante ganha "Crianças: N" logo depois da linha do quarto só quando há criança.

Tudo o que já funcionava continua funcionando, inclusive a recusa de duas reservas ao mesmo tempo para o mesmo quarto.

## Dúvidas

- Os testes ficaram com nota `bom`, mas no limite: a regra "criança não paga" e a posição da linha no comprovante não estão conferidas pelos testes do agente (só pelo roteiro). Um revisor mais estrito poderia dar `com problemas`.
- O README não foi atualizado com o campo `children` no corpo do `POST /reservations` (`README.md:61-72`). Não entra em nenhuma coluna, mas quem for integrar o site vai ler a documentação antiga.
- A segunda via do comprovante é serializada como JSON a partir do record (`HttpApi.java:79`), então ela agora traz também o campo `children`. É uma mudança que só acrescenta e não deve quebrar o site, mas o pedido não fala dela.
