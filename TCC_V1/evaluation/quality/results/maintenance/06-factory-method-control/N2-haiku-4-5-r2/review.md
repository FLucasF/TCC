# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em sete pacotes. `hotel.web` é a borda HTTP: `HttpApi` lê o JSON num `CreateReservationRequest` e devolve um `ReservationResponse`. `hotel.reservations` guarda as regras (`ReservationService`, `PriceCalculator`) e o acesso ao banco (`ReservationStore`, JDBC com `?`). `hotel.documents` monta o comprovante. Os dados ficam no PostgreSQL, num esquema que o Flyway cria. O pedido era acrescentar o número de crianças à reserva, e o agente fez uma mudança pequena que segue o mesmo caminho dos campos que já existiam:
- o campo opcional `children` no pedido HTTP, com zero quando falta (`src/main/java/hotel/web/HttpApi.java:127`);
- o campo em `NewReservation` e em `Reservation`;
- a recusa de número negativo e a soma de adultos e crianças na conferência da capacidade (`src/main/java/hotel/reservations/ReservationService.java:34-45`);
- a coluna nas consultas do `ReservationStore`;
- uma migração nova, `V2__add_children_column.sql`;
- a linha "Crianças: N" no comprovante (`src/main/java/hotel/documents/ReservationReceipt.java:47-49`);
- o campo na resposta.

Não criou nada para a "cobrança futura". As 61 conferências do roteiro passam. O ponto mais fraco são os testes. O agente mudou o JSON do teste HTTP da base para mandar sempre `"children": 0`. Assim nenhum teste cria mais uma reserva sem o campo, que é justamente o caso que o pedido manda preservar. Algumas regras novas ficaram sem teste.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). O ciclo já existia na base: `measures/dependencies-base.md` tem o mesmo grafo, só as linhas mudam.
**Nota:** bom

Cada parte da mudança ficou na camada a que pertence:
- **Borda HTTP:** só converte o campo opcional em `int` (`src/main/java/hotel/web/HttpApi.java:127-130`) e o devolve (`src/main/java/hotel/web/ReservationResponse.java:18,38`).
- **Regras:** a regra de capacidade e a recusa do negativo ficam no serviço, que continua sem Javalin e sem SQL (`src/main/java/hotel/reservations/ReservationService.java:34-45`).
- **Banco:** o SQL fica só no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:19,32,43,132`).

O agente não criou dependência nova entre pacotes e não fez classe crescer muito: `HttpApi` passou de 170 para 171 linhas e `ReservationService` de 138 para 142. O ciclo entre `documents` e `reservations` (`ReservationService.java:3` importa `ReservationReceipt`, que importa `Reservation`) veio da base, e o agente não o piorou.

## Design de baixo nível

**Números:** 8 arquivos de produção mudados e 1 migração nova. Nenhuma classe, interface, fábrica ou builder novo. A mudança é um campo `int children` em 5 tipos que já existiam, um `if` no comprovante e duas condições no serviço.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O pedido é um controle negativo, e a frase sobre "cobrar alguma coisa das crianças maiores no futuro" não levou a estrutura nenhuma:
- O `PriceCalculator` não foi tocado. Não há política de preço de criança.
- A reserva continua criada direto por `new NewReservation(...)` (`src/main/java/hotel/web/HttpApi.java:129`, `src/main/java/hotel/reservations/ReservationStore.java:126`), sem builder nem fábrica.

Isso é exatamente o que a referência chama de contenção: um campo a mais na reserva e no pedido, a soma na capacidade, a linha no comprovante e a coluna no banco.

O código novo é limpo:
- **Defeitos que o agente não acrescentou:** nada de duplicação. A regra da capacidade continua num lugar só (`ReservationService.java:40-41`).
- **Tipos e nomes:** `int` para a contagem, `Integer` só onde o JSON pode vir sem o campo (`src/main/java/hotel/web/CreateReservationRequest.java:15`), e nomes claros (`totalOccupants`).
- **O comprovante:** a linha nova entra no lugar pedido, depois da linha do quarto (`ReservationReceipt.java:46-49`).

Detalhes menores, que não mudam a nota:
- A mensagem de capacidade ainda diz "guests" quando conta adultos e crianças (`ReservationService.java:44`).
- O README não documenta o campo `children` no corpo de `POST /reservations` (`README.md`, o exemplo de JSON).
- O comprovante, que vira JSON em `/check-out` e `/document` (`HttpApi.java:77,79`), passou a ter também um campo `children`. É um acréscimo inofensivo.

Os pontos fracos do resto do código vieram da base: o `catch (Exception)` em `HttpApi.java:113` e o `synchronized` no serviço.

## Testes

**Números:** 31 testes rodaram, 0 falharam, 0 pulados; 31 métodos escritos (a base tinha 26, todos mantidos e passando). Cobertura de linhas 94,0% (483/514; a base tinha 93,8%) e de ramos 86,9% (106/122; a base tinha 87,1%), de `measures/tests.md` e `measures/tests-base.md`.
**Nota:** com problemas

Há cinco testes novos, em `src/test/java/hotel/reservations/ReservationServiceTest.java`:
- **Adultos e crianças juntos** (`:168`): 2 adultos e 1 criança no quarto 201, que comporta 3, são aceitos.
- **Capacidade excedida** (`:175`): 2 adultos e 2 crianças dão `CAPACITY_EXCEEDED`. Junto com o teste anterior, isso confere a fronteira 3/4: se a soma das crianças sumisse ou o `>` virasse `>=`, um dos dois falharia.
- **Negativo** (`:181`): -1 criança dá `INVALID_REQUEST`.
- **Linha presente** (`:187`): "Crianças: 1" aparece na segunda via. A segunda via relê do banco, então o teste também confere a gravação da coluna.
- **Linha ausente** (`:196`): sem criança, a linha não aparece.

O que ficou sem rede:
- **O padrão de zero crianças quando o campo falta** (`HttpApi.java:127`). O agente mudou o JSON da base em `src/test/java/hotel/web/HttpApiTest.java:24` para mandar sempre `"children": 0`. Com isso, o único teste pela API deixou de criar uma reserva sem o campo, que era o comportamento antigo que o pedido manda preservar. Se alguém tornasse `children` obrigatório, nenhum teste falharia. A cobertura de ramos de `hotel.web` mostra isso: 19 de 30.
- **"Criança não paga":** nenhum teste confere que o total com crianças é igual ao total sem elas.
- **O campo `children` na resposta JSON:** nenhum teste o confere.
- **A posição da linha no comprovante:** o teste usa `contains`, então não confere que a linha vem logo depois da do quarto.

Há ainda uma asserção fraca de forma: `assertTrue(!...)` em vez de `assertFalse` (`ReservationServiceTest.java:201`).

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto. Migrações: `V1__create_tables.sql` igual à da base, `V2__add_children_column.sql` nova. O único aviso das conferências é a falta de `CHECK` em `status`, `rate_plan`, `type` e `channel`, que veio da base (de `measures/database.md`).
**Nota:** bom

O agente fez a mudança de esquema do jeito certo:
- **Uma migração nova**, sem editar a V1: `ALTER TABLE reservations ADD COLUMN children integer NOT NULL DEFAULT 0 CHECK (children >= 0)` (`src/main/resources/db/migration/V2__add_children_column.sql:1`). As reservas que já existem ficam com zero crianças, e o banco também recusa o número negativo.
- **O SQL continua com parâmetros:** a coluna entrou no `INSERT` e no `SELECT` (`src/main/java/hotel/reservations/ReservationStore.java:19,32-33,43,132`), com os índices dos `setXxx` renumerados direito.
- **A regra contra duas reservas na mesma noite** continua protegida pela restrição `EXCLUDE` da base.

A consulta que lê a tabela inteira (`ReservationStore.java:112`) e as colunas de situação sem `CHECK` vieram da base.

## Comportamento

**Números:** 61 de 61 conferências passaram (de `measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| as crianças na reserva | 7 de 7 |
| o comprovante | 2 de 2 |
| as crianças depois de reiniciar | 1 de 1 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Nada falhou. Para o dono da pousada:
- **O site pode mandar o número de crianças ou omiti-lo:** sem o campo, são zero crianças.
- **A capacidade:** adultos e crianças juntos não passam da capacidade do quarto (422).
- **Número negativo** é recusado (400).
- **O preço** não muda com crianças.
- **A consulta** devolve o campo `children`.
- **O comprovante** ganha "Crianças: 2" logo depois da linha do quarto, e fica igual quando não há criança.
- **O que já funcionava** continua funcionando: preços, recusas, confirmação e e-mail, check-in e check-out, cancelamentos, relatórios, os dados depois de reiniciar e as reservas simultâneas.

## Dúvidas

Nenhuma sobre as notas. Uma escolha que o texto deixou aberta: o agente aceita qualquer número de crianças não negativo, e só a capacidade limita. O texto não pede limite próprio, então isso parece sensato.
