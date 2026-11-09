# Revisão: manutenção, cenário 06

## Visão geral

O projeto (que vem da base) tem sete pacotes: `hotel.web` (as rotas Javalin e os registros do JSON), `hotel.reservations` (o serviço com as regras, o cálculo de preço e o `ReservationStore` com o JDBC), `hotel.rooms`, `hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports` e `hotel`, onde fica a montagem. Uma requisição `POST /reservations` passa por `HttpApi.readNewReservation`, que valida o corpo e monta um `NewReservation`. Depois vem `ReservationService.create`, que confere datas, hóspedes, capacidade e disponibilidade e calcula o preço, e por fim `ReservationStore.add`, que grava no PostgreSQL (o esquema vem das migrações do Flyway). Para pôr as crianças na reserva, o agente fez a mudança pequena, no lugar onde cada coisa já mora. Entrou um campo `children` no pedido HTTP, no `NewReservation`, na `Reservation`, na resposta e no comprovante. A conferência da capacidade passou a somar adultos e crianças. Há uma migração nova, `V2`, com `NOT NULL DEFAULT 0 CHECK (children >= 0)`. O agente não criou estrutura para o "cobrar das crianças no futuro". Todas as 61 conferências do roteiro passam e os 32 testes também. O defeito mais visível é pequeno: o teste chamado "criança não paga" não confere o preço, e o README não foi atualizado com o campo novo.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que a base já tinha (`measures/dependencies-base.md`). O tamanho das classes mal mudou: `HttpApi` passou de 170 para 171 linhas, `ReservationStore` de 143 para 145 e `ReservationService` de 138 para 142.
**Nota:** bom

O agente não mudou nenhuma dependência entre pacotes, e cada parte da mudança ficou na sua camada:
- A borda só lê o campo opcional e aplica o padrão zero (`src/main/java/hotel/web/HttpApi.java:128`).
- A regra da capacidade fica no serviço, que não conhece Javalin nem SQL (`src/main/java/hotel/reservations/ReservationService.java:34-41`).
- O SQL fica só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:19`, `:32`, `:43`, `:132`).
- A montagem continua num lugar só (`src/main/java/hotel/Hotel.java:21-27`).

O ciclo `documents` ↔ `reservations` vem da base: `ReservationReceipt` lê a `Reservation` e o serviço produz o comprovante. O agente só acrescentou mais um campo lido por esse caminho já existente (`src/main/java/hotel/documents/ReservationReceipt.java:33`).

## Design de baixo nível

**Números:** a mudança tem 9 arquivos de código de produção tocados, 1 migração nova e nenhuma classe, interface, fábrica ou *builder* novo. O preço não recebe as crianças: `PriceCalculator.calculate` continua com a mesma assinatura (`src/main/java/hotel/reservations/ReservationService.java:50`).
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O cenário é um controle negativo, e a resposta segue o que `controls.md` chama de contenção:
- um campo a mais no pedido e na reserva (`src/main/java/hotel/reservations/NewReservation.java:6`, `src/main/java/hotel/reservations/Reservation.java:14`, `:38`);
- a soma na conferência da capacidade (`src/main/java/hotel/reservations/ReservationService.java:40-41`);
- a linha condicional no comprovante, logo depois da linha do quarto (`src/main/java/hotel/documents/ReservationReceipt.java:46-49`);
- a coluna nova no banco.

A reserva continua criada direto. A frase "cobrar alguma coisa das crianças maiores no futuro" não virou uma política de preço de criança, nem idade por criança, nem um *builder* para o `NewReservation`, que passou a ter 7 parâmetros.

O resto do código continua no estilo da base:
- registros imutáveis;
- `int` para a contagem, com o `null` do JSON tratado num único ponto (`src/main/java/hotel/web/HttpApi.java:128`);
- a validação de negativo com o mesmo código de erro das outras recusas (`src/main/java/hotel/reservations/ReservationService.java:34-36`).

Há dois detalhes pequenos, nenhum suficiente para mudar a nota:
- A mensagem de capacidade ainda diz "takes at most N guests" (`src/main/java/hotel/reservations/ReservationService.java:44`), embora agora conte adultos e crianças.
- O exemplo do corpo de `POST /reservations` no README não ganhou o campo `children` (`README.md:61-72`).

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 32 métodos escritos. A base tinha 26 testes, todos mantidos. Os 6 novos são 3 em `ReservationServiceTest` e 3 em `HttpApiTest`. A cobertura de linhas foi de 93,8% para 94,2% (484 de 514) e a de ramos de 87,1% para 88,5% (108 de 122). O pacote `hotel.documents` tem 33 de 33 linhas e 6 de 6 ramos.
**Nota:** com problemas

O que os testes novos conferem, com valores:
- o campo opcional virando zero, o valor na criação e o valor na consulta `GET` (`src/test/java/hotel/web/HttpApiTest.java:124-148`);
- o negativo dando 400 `INVALID_REQUEST`, no serviço e na API (`src/test/java/hotel/reservations/ReservationServiceTest.java:178-183`, `src/test/java/hotel/web/HttpApiTest.java:151-160`);
- a capacidade no limite: 2+1 aceito e 2+2 recusado no quarto para três (`src/test/java/hotel/reservations/ReservationServiceTest.java:164-175`), e 1+2 recusado com 422 no quarto para dois (`src/test/java/hotel/web/HttpApiTest.java:163-172`);
- a linha "Crianças: 2" no comprovante (`src/test/java/hotel/reservations/ReservationServiceTest.java:186-195`).

O agente usou as mudanças mecânicas nos testes da base (o `0` nos construtores, em `ReportsTest.java:33` e `ReservationServiceTest.java:36`) sem enfraquecer nenhuma asserção.

Ficaram sem rede três regras pedidas:
- **Criança não paga.** O teste `childrenDoNotPayButOccupySpace` (`ReservationServiceTest.java:164`) não confere o preço, só `guests`, `children` e a recusa. Se alguém passasse a cobrar as crianças, nenhum teste falharia.
- **A posição da linha.** `receiptShowsChildrenWhenPresent` usa `contains` (`ReservationServiceTest.java:195`) e não confere que a linha vem logo depois da linha do quarto.
- **O comprovante sem criança.** Nenhum teste confere que ele fica como estava: se o `if (children > 0)` virasse `>= 0`, imprimindo "Crianças: 0", nenhum teste falharia.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, nenhuma chave estrangeira está sem índice e nenhuma data está guardada como texto. A migração `V1` está igual à da base e a `V2` é nova. As colunas de situação e de tipo em `text` sem `CHECK` vêm da base.
**Nota:** bom

O agente fez a evolução do esquema do jeito certo:
- Criou uma migração nova sem editar a `V1` (`src/main/resources/db/migration/V2__add_children_to_reservations.sql:1`).
- A coluna nova é `integer NOT NULL DEFAULT 0 CHECK (children >= 0)`. As reservas que já existem num banco em uso ficam com zero crianças, o que bate com "se a reserva não disser nada, são zero crianças", e o banco também recusa negativo.

O SQL continua todo com parâmetros: o `INSERT` foi reajustado para 15 posições sem erro de ordem (`src/main/java/hotel/reservations/ReservationStore.java:31-50`), e a leitura pega a coluna (`:132`). O roteiro confirma que a criança continua na reserva depois de reiniciar. A proteção contra reserva dupla é a restrição `EXCLUDE` da base, que não foi mexida.

## Comportamento

**Números:** 61 de 61 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- as crianças na reserva: 7 de 7;
- o comprovante: 2 de 2;
- as crianças depois de reiniciar: 1 de 1;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Nada falhou. Para o dono da pousada, isso quer dizer o seguinte:
- O site pode mandar `children` ou não mandar (vira zero).
- Um número negativo é recusado.
- Adultos e crianças juntos não passam da capacidade, inclusive no quarto para dois com dois adultos e uma criança.
- O preço não muda com as crianças.
- O comprovante ganha a linha "Crianças: N" logo depois do quarto e fica igual quando não há criança.
- Tudo o que já funcionava (preços, recusas, confirmação, cancelamentos, relatórios, reservas simultâneas) continua funcionando.

## Dúvidas

A nota de testes ficou em `com problemas` porque a regra "criança não paga" e o comprovante sem criança não têm asserção. O código está certo e o roteiro confirma as duas regras, de modo que um revisor mais tolerante poderia dar `bom`.
