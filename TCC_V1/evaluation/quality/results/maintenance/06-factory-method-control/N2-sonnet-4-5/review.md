# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas em `HttpApi` e os registros do JSON),
`hotel.reservations` (as regras em `ReservationService`, o SQL em `ReservationStore`), `hotel.rooms`,
`hotel.documents` (o comprovante), `hotel.messages`, `hotel.reports` e `hotel` (a montagem). Uma reserva entra por
`HttpApi.readNewReservation` (`src/main/java/hotel/web/HttpApi.java:109`), vira um `NewReservation`, passa pelas
conferências de `ReservationService.create` (`src/main/java/hotel/reservations/ReservationService.java:27`) e é
gravada por `ReservationStore.add` no PostgreSQL, num esquema criado por migrações do Flyway. O pedido era pôr as
crianças na reserva. O agente fez a mudança pequena, no lugar certo: o campo `children` passa pelo pedido HTTP, pela
reserva, pelo SQL e pela resposta; a soma adultos + crianças entra na conferência da capacidade; a linha "Crianças: N"
vai para o comprovante; e a coluna nova chega numa migração nova, `V2`. Não criou estrutura para a cobrança futura de
crianças. Os 61 testes do roteiro de comportamento passam, os 34 testes do projeto também (eram 26), e nenhum teste da
base foi apagado nem afrouxado. Não há defeito sério. Ficaram dois detalhes pequenos: o README não foi atualizado
(o corpo de `POST /reservations` em `README.md:61-72` não mostra `children`) e a mensagem de capacidade ainda diz
"guests" (`ReservationService.java:44`).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tem
os mesmos números: a mudança não criou dependência nova, só acrescentou linhas (`hotel.reservations` passou de 508
para 520 linhas, `hotel.web` de 236 para 240, `hotel.documents` de 79 para 84).
**Nota:** bom

Cada parte da mudança está na camada onde a coisa já mora:
- **Borda HTTP:** o campo opcional e o valor padrão zero ficam em `src/main/java/hotel/web/CreateReservationRequest.java:15`
  e `src/main/java/hotel/web/HttpApi.java:128`. A resposta mostra o campo em
  `src/main/java/hotel/web/ReservationResponse.java:18,38`.
- **Regras:** a recusa de número negativo e a soma na capacidade ficam no serviço, sem Javalin e sem SQL
  (`src/main/java/hotel/reservations/ReservationService.java:34-45`).
- **Dados:** o SQL só aparece em `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:18-19,31-50,132`).
- **Comprovante:** a linha nova fica no próprio documento (`src/main/java/hotel/documents/ReservationReceipt.java:47-49`).

O ciclo `documents` ↔ `reservations` veio da base: o serviço monta o `ReservationReceipt`, e o comprovante lê a
`Reservation`. O agente não o piorou. A maior classe continua sendo `HttpApi`, com 171 linhas (eram 170).

## Design de baixo nível

**Números:** 13 arquivos mudados (10 de código, 1 migração nova, 3 de teste). Nenhuma classe, interface, fábrica ou
*builder* novo. A mudança são um campo em 4 registros/classes, um `if` de validação, uma soma, um `if` no comprovante e
uma coluna.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O pedido termina com a frase que tenta o agente ("cobrar alguma coisa das crianças maiores no futuro"). O agente não
mordeu a isca. Não há política de preço de criança: `PriceCalculator.calculate` continua sem receber crianças
(`ReservationService.java:50`). Não há *builder* nem fábrica para a reserva: `NewReservation` só ganhou um componente
(`src/main/java/hotel/reservations/NewReservation.java:6`), e a reserva continua criada direto
(`ReservationService.java:51`). Isso é exatamente a contenção que os critérios do cenário 06 descrevem: o campo, a
soma na capacidade (`ReservationService.java:40-41`), a linha no comprovante (`ReservationReceipt.java:47-49`) e a
coluna nova no banco.

O resto do design está limpo:
- **O padrão de zero:** o campo opcional é `Integer` só no DTO da borda (`CreateReservationRequest.java:15`), e o padrão
  é aplicado uma vez só (`HttpApi.java:128`). Do domínio para dentro, `children` é `int` (`Reservation.java:14,70-72`).
- **Os nomes:** "guests" continua contando os adultos, como o pedido manda, e a soma tem um nome que diz o que é
  (`totalOccupants`, `ReservationService.java:40`).

Há dois detalhes que não mudam a nota:
- **A mensagem de capacidade:** ela ainda diz "takes at most N guests" (`ReservationService.java:44`), embora agora
  conte adultos e crianças. O código do erro, que é o que o site lê, está certo.
- **O README:** ele não foi atualizado. O corpo de exemplo (`README.md:61-72`) e a explicação de `CAPACITY_EXCEEDED`
  (`README.md:84`) não falam das crianças.

O `catch (Exception)` de `HttpApi.java:113` veio da base.

## Testes

**Números:**
- **Final:** 34 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 34 métodos escritos. Cobertura de linhas: 484 de
  514 (94,2%). Cobertura de ramos: 108 de 122 (88,5%).
- **Base:** 26 testes. Cobertura de linhas: 471 de 502 (93,8%). Cobertura de ramos: 101 de 116 (87,1%).
- **Por pacote:** `hotel.documents` tem 6 de 6 ramos cobertos (eram 4 de 4).

**Nota:** bom

O agente escreveu 8 testes, todos com valores, nos dois níveis (serviço contra o banco e API por HTTP):
- **Capacidade no limite e acima dele:** 2+1 num quarto para 3 é aceito; 2+2 e 1+3 são recusados com
  `CAPACITY_EXCEEDED` (`src/test/java/hotel/reservations/ReservationServiceTest.java:177-184`). Isso pega quem trocar
  `>` por `>=` ou esquecer de somar as crianças.
- **Número negativo:** recusado com `INVALID_REQUEST` (`ReservationServiceTest.java:187-190`).
- **Comprovante:** "Crianças: 2" quando há criança e nenhuma linha quando não há (`ReservationServiceTest.java:193-211`).
  O teste passa por `receiptCopy`, que relê a reserva do banco (`ReservationService.java:117-118`), então também
  confere a leitura da coluna.
- **API:** o campo aparece na resposta (`src/test/java/hotel/web/HttpApiTest.java:124-137`), é opcional com padrão zero
  (`HttpApiTest.java:140-147`), e a capacidade estourada responde 422 (`HttpApiTest.java:150-161`).

Os testes da base só foram adaptados à assinatura nova. Em `src/test/java/hotel/reports/ReportsTest.java:33`, o agente
passou `0` crianças. Em `ReservationServiceTest.java:33-35`, criou uma sobrecarga do ajudante com zero crianças.
Nenhum teste foi apagado ou afrouxado.

Três regras ficaram sem rede, todas pequenas:
- Nenhum teste confere que o total não muda com crianças ("criança não paga"). Hoje o preço nem recebe as crianças, mas
  quem passasse a cobrar não quebraria nenhum teste.
- A posição da linha "logo depois da linha do quarto" não é conferida, porque o teste usa `contains`
  (`ReservationServiceTest.java:200`).
- A consulta `GET /reservations/{id}` com crianças não é testada pela API.

## Banco

**Números:** 3 tabelas, 2 migrações do Flyway. `V1__create_tables.sql` está igual à da base, e
`V2__add_children_column.sql` é nova. As conferências do script não acusaram nada: não há tabela sem chave primária,
o dinheiro está em `numeric(10,2)`, não há chave estrangeira sem índice nem data em texto. Ficou a falta de `CHECK`
nas colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`), que vem da base.
**Nota:** bom

A coluna nova chega numa migração nova, sem editar a da base: `ALTER TABLE reservations ADD COLUMN children integer
NOT NULL DEFAULT 0 CHECK (children >= 0)` (`src/main/resources/db/migration/V2__add_children_column.sql:2-3`). Com isso:
- as reservas que já existem num banco em uso ficam com zero crianças, sem perda de dado;
- o banco também protege a regra "não pode ser negativo".

O SQL continua com parâmetros. A coluna entrou na lista de colunas, no `INSERT` com a renumeração dos índices e na
leitura (`src/main/java/hotel/reservations/ReservationStore.java:18-19,31-50,132`), e as conexões continuam em
try-with-resources. A capacidade (adultos + crianças) é conferida só no serviço, como a capacidade de adultos já era
na base. A dupla reserva continua protegida pela restrição `EXCLUDE` da base.

## Comportamento

**Números:** 61 de 61 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- as crianças na reserva: 7 de 7;
- o comprovante: 2 de 2;
- as crianças depois de reiniciar: 1 de 1;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Nada falhou. Para o dono da pousada, isto quer dizer:
- **O campo:** o site pode mandar `children` ou não mandar (sem o campo, são zero crianças), e a consulta mostra o
  número.
- **A capacidade:** dois adultos e uma criança cabem num quarto para três. No quarto para dois, a mesma família é
  recusada com 422 `CAPACITY_EXCEEDED`.
- **O preço:** a criança não muda o preço.
- **Os erros:** um número negativo é recusado com 400.
- **O comprovante:** traz "Crianças: 2" logo depois da linha do quarto e fica igual ao de antes quando não há criança.
- **O banco:** a informação continua lá depois de reiniciar o serviço.

Todo o resto do sistema (preços, cancelamentos, relatórios, e-mails, duas reservas ao mesmo tempo) continua
funcionando como antes.

## Dúvidas

O README desatualizado (`README.md:61-72,84`) não pesou em nenhuma coluna, porque nenhuma delas mede a documentação.
Um revisor que cobre a documentação da API para o pessoal do site poderia pedir esse ajuste antes de aceitar.
