# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel.web` (`HttpApi`) lê o JSON e chama `hotel.reservations.ReservationService`, onde ficam as regras. O SQL fica nas classes `*Store`/`Rooms`/`SentMessages`, com JDBC puro e um pool. O esquema vem de migrações Flyway (`src/main/resources/db/migration`). O pedido era pontual: o campo opcional `children` na reserva, a soma de adultos e crianças na conferência da capacidade, a linha "Crianças: N" no comprovante e criança sem pagar diária. O agente fez exatamente isso, com uma mudança pequena que segue o desenho existente. Passou o campo por `CreateReservationRequest` → `HttpApi.readNewReservation` → `NewReservation` → `Reservation` → `ReservationStore` → `ReservationResponse`/`ReservationReceipt`, e o banco ganhou a migração nova `V2__add_children_column.sql`, sem mexer na V1. Não criou estrutura para a "cobrança futura" das crianças. As 61 conferências de comportamento passaram, e os 31 testes também. Não há defeito sério. O que dá para apontar é pequeno: a regra "não pode ser negativo" ficou na borda HTTP e não no serviço; testes da parte nova com lacunas (preço inalterado, recusa do negativo e ausência da linha no comprovante não são conferidos); e o README não foi atualizado.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base são os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). A mudança só acrescentou linhas: `hotel.reservations` passou de 508 para 517, `hotel.web` de 236 para 243 e `hotel.documents` de 79 para 84.
**Nota:** bom

A mudança respeita as camadas da base. O JSON e o valor padrão do campo opcional ficam na borda (`src/main/java/hotel/web/HttpApi.java:127`, `src/main/java/hotel/web/CreateReservationRequest.java:15`). A regra da capacidade fica no serviço, sem Javalin nem SQL (`src/main/java/hotel/reservations/ReservationService.java:37-38`). O SQL continua isolado em `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:18-19`, `:31-33`, `:43`, `:132`). A resposta e o comprovante só leem o novo acessor (`src/main/java/hotel/web/ReservationResponse.java:38`, `src/main/java/hotel/documents/ReservationReceipt.java:36`). O ciclo `documents` ↔ `reservations` veio da base, porque o serviço monta o comprovante (`ReservationService.java:3`, `:65`) e o comprovante lê a `Reservation` (`ReservationReceipt.java:3`). O agente não o piorou. Nenhuma classe cresceu muito: a maior, `HttpApi`, foi de 170 para 174 linhas.

## Design de baixo nível

**Números:** 11 arquivos tocados (8 de produção, 1 migração, 3 de teste). Nenhuma classe, interface, fábrica ou builder novo. Nenhum tipo de preço para crianças. Um campo `int children` acrescentado nos 5 registros e classes por onde a reserva passa.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É um controle negativo: a frase "a gente já pensou em cobrar alguma coisa das crianças maiores no futuro" (`pedido.md`) tenta puxar uma política de preço por criança. O agente não caiu nela. `PriceCalculator` não foi tocado, a reserva continua sendo criada direto pelo construtor do registro `NewReservation` (`src/main/java/hotel/reservations/NewReservation.java:6`), sem builder nem fábrica, e a mudança é exatamente a que `controls.md` descreve como contenção:
- um campo a mais no pedido e na reserva (`Reservation.java:14`, `:38`, `:70-72`);
- a soma na capacidade (`ReservationService.java:37`);
- a linha condicional no comprovante, logo depois da linha do quarto (`ReservationReceipt.java:46-49`);
- a coluna nova no banco (`V2__add_children_column.sql:1`).

O resto do código está no mesmo nível da base. Pontos menores, que não impedem a aceitação:
- A conferência "não pode ser negativo" ficou em `HttpApi.java:128-130`, enquanto a regra irmã "pelo menos um hóspede" mora em `ReservationService.java:31-33`. Quem chamar `ReservationService.create` sem passar pela API com `children = -1` passa pela capacidade com a soma reduzida e só é barrado pelo CHECK do banco, como `IllegalStateException` (`ReservationStore.java:60`), ou seja, um 500 e não um 400. Pela API isso não acontece.
- A mensagem de capacidade continua dizendo "guests" (`ReservationService.java:41`), embora agora conte adultos e crianças. É cosmético.
- A renumeração manual dos índices do `PreparedStatement` (`ReservationStore.java:43-50`) segue o estilo da base e está certa.

Os defeitos de design que já existiam, como o `catch (Exception)` em `HttpApi.java:113`, vêm da base.

## Testes

**Números:** 31 testes rodaram, 0 falhas, 0 erros, 0 pulados; 31 métodos escritos. A base tinha 26, e todos foram mantidos e passam. Os 5 novos são `ReservationServiceTest` (+3) e `HttpApiTest` (+2). A cobertura de linhas é de 93,8% (482/514; a base também tinha 93,8%) e a de ramos de 86,9% (106/122; a base tinha 87,1%). `hotel.documents` tem 6/6 ramos e `hotel.web` 21/32.
**Nota:** com problemas

Os testes tocaram só o que era preciso: o `0` a mais nos construtores (`src/test/java/hotel/reservations/ReservationServiceTest.java:36`, `src/test/java/hotel/reports/ReportsTest.java:33`) e `"children": 0` no JSON comum (`src/test/java/hotel/web/HttpApiTest.java:24`). Nenhum teste da base ficou mais fraco. Parte da regra nova está conferida com valores:
- a capacidade no limite, com 2 adultos + 1 criança aceitos no quarto 201 de capacidade 3 (`ReservationServiceTest.java:164-173`), e acima dele, com 2 + 2 recusados com `CAPACITY_EXCEEDED` (`:176-182`). Isso pega a troca de `>` por `>=` e a remoção da soma;
- o campo opcional valendo 0 pela API (`HttpApiTest.java:139-150`);
- o eco de `children` na resposta (`HttpApiTest.java:124-136`);
- a presença de "Crianças: 2" no e-mail (`ReservationServiceTest.java:185-196`).

O resto da regra nova fica sem rede (a pergunta da mutação):
- **Criança não paga:** o teste se chama `childrenDoNotPayButOccupySpace` (`ReservationServiceTest.java:164`), mas só confere `guests` e `children` (`:171-172`), nunca o preço. Se alguém somasse as crianças no preço, nenhum teste falharia.
- **Negativo → 400:** nenhum teste manda `children` negativo. Apagar `HttpApi.java:128-130` não derruba nenhum teste.
- **"Sem criança, o comprovante fica como está":** nenhum teste confere a ausência da linha, nem a posição dela logo depois de "Quarto:". O teste novo usa só `contains` (`ReservationServiceTest.java:195`). Trocar `children > 0` por `children >= 0` em `ReservationReceipt.java:47`, o que imprimiria "Crianças: 0" em todo comprovante, não derruba nenhum teste. A cobertura de 6/6 ramos em `hotel.documents` executa os dois lados do `if`, mas não confere o lado sem criança.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. O dinheiro está todo em `numeric(10,2)`. Não há chave estrangeira sem índice nem data em texto. Há 2 migrações Flyway: a V1 está igual à da base e a V2 é nova. A conferência acusou `status`, `rate_plan`, `type` e `channel` em `text` sem CHECK. Os 4 trechos de SQL montados com `+` só concatenam constantes.
**Nota:** bom

A mudança no banco é a certa para um sistema que já roda. É uma migração nova, `src/main/resources/db/migration/V2__add_children_column.sql:1` (`ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0)`), e a V1 ficou intocada. O `DEFAULT 0` dá zero crianças às reservas antigas, como o pedido manda, e o `CHECK` protege no banco a regra do não negativo. O código grava e lê a coluna com parâmetros (`ReservationStore.java:31-33`, `:43`, `:132`), e as crianças continuam lá depois de reiniciar (conferência de comportamento). O que a conferência acusou vem todo da base, que o agente não tocou: colunas de situação e de tipo sem CHECK (`V1__create_tables.sql`) e nenhuma transação explícita. A reserva duplicada continua barrada pela restrição `EXCLUDE` da V1.

## Comportamento

**Números:** 61 de 61 conferências passaram, 0 falharam:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- crianças na reserva: 7/7;
- comprovante: 2/2;
- crianças depois de reiniciar: 1/1;
- reservas simultâneas: 2/2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:
- sem o campo, a reserva fica com zero crianças;
- 2 adultos + 1 criança cabem no quarto para três e são recusados com 422 no quarto para dois;
- adultos e crianças acima da capacidade dão 422;
- um número negativo dá 400;
- criança não muda o preço;
- a consulta mostra as crianças;
- o comprovante traz "Crianças: 2" logo depois da linha do quarto e fica igual ao de antes quando não há criança;
- tudo continua lá depois de reiniciar.

O que já funcionava continua funcionando: os preços, os cancelamentos, os relatórios e a recusa de reserva dupla nas 10 rodadas simultâneas.

## Dúvidas

- Na nota do design, pesei a validação do negativo na borda HTTP (`HttpApi.java:128-130`) em vez do serviço como ponto menor. Pela API ela funciona, e o banco tem o CHECK por trás. Um revisor mais rigoroso poderia pedir para movê-la para `ReservationService.create` antes de aceitar, o que levaria a nota a `com problemas`.
- O README não foi atualizado: `final/README.md` é igual ao da base, e o exemplo de JSON em `README.md:69` não mostra `children`. Não entrou em nenhuma nota, porque a revisão julga o código e não a documentação.
