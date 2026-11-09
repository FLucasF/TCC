# Revisão: manutenção, cenário 06

## Visão geral

A base é um serviço Javalin em 7 pacotes. `hotel.web` (`HttpApi`) lê o JSON, monta um `NewReservation` e chama o
`ReservationService` (`hotel.reservations`), que confere as regras e grava pelo `ReservationStore` (JDBC com `?`,
pool) no PostgreSQL. O esquema vem de migrações do Flyway. O pedido era pontual: um campo `children` opcional
(padrão 0, nunca negativo) que entra na conta da capacidade, não muda o preço e acrescenta a linha "Crianças: N" no
comprovante. O agente fez exatamente isso, nos lugares onde as coisas já moravam: um componente a mais no pedido
(`NewReservation.java:5-6`), um campo na reserva (`Reservation.java:14`, `:38`, `:71-73`), a soma na conferência da
capacidade (`ReservationService.java:40`), a recusa do negativo (`ReservationService.java:34-36`), o padrão 0 na
borda (`HttpApi.java:130`), o campo na resposta (`ReservationResponse.java:18`, `:38`), a linha condicional no
comprovante (`ReservationReceipt.java:47-49`) e uma migração nova `V2__add_children.sql`, sem mexer na V1. Ele não
criou nenhuma estrutura para a cobrança futura das crianças. O roteiro do comportamento passou inteiro (61 de 61), e
o `mvn verify` passa com 28 testes. O ponto mais fraco são os testes da parte nova: não conferem o comprovante sem
criança nem o campo pela API.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). As linhas cresceram pouco:
`hotel.reservations` passou de 508 para 527, `hotel.web` de 236 para 240 e `hotel.documents` de 79 para 84.
**Nota:** bom

A mudança respeitou as camadas. O campo opcional e o padrão 0 ficam na borda HTTP (`web/CreateReservationRequest.java:15`,
`web/HttpApi.java:130`). As regras (o negativo e a capacidade) ficam no serviço, que não conhece Javalin nem SQL
(`reservations/ReservationService.java:34-44`). A coluna nova só aparece no acesso a dados
(`reservations/ReservationStore.java:19`, `:32`, `:43`, `:132`). O agente não criou pacote, dependência nem classe
nova. O ciclo vem da base: `ReservationService.java:3` importa `ReservationReceipt`, e `ReservationReceipt.java:3`
importa `Reservation`. O agente não o piorou. A maior classe continua sendo o `HttpApi`, com 171 linhas (eram 170).

## Design de baixo nível

**Números:** 12 arquivos tocados no patch (11 de código, mais o README), cerca de 60 linhas de produção. Nenhuma
classe, interface, fábrica ou *builder* novo; a única peça extra é um construtor de conveniência no record
`NewReservation`.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O controle negativo pede contenção, e foi o que veio. O agente fez as cinco coisas que `controls.md` espera: o campo
na reserva e no pedido, a soma na capacidade (`ReservationService.java:40`), a linha no comprovante
(`ReservationReceipt.java:47-49`, logo depois da linha do quarto na `:46`) e a coluna nova no banco
(`V2__add_children.sql:2`). A reserva continua criada direto pelo construtor do record (`HttpApi.java:128-130`),
sem *builder* nem fábrica. Ele não criou nenhuma política de preço de criança: o `PriceCalculator` não foi tocado, e
a frase sobre "cobrar das crianças maiores no futuro" ficou fora do código.

O construtor de 6 argumentos de `NewReservation.java:9-13` delega com `children = 0`. É uma sobrecarga para os
chamadores antigos (os testes da base, `ReservationServiceTest.java:35` e `ReportsTest.java:32`), não estrutura nova.
Também é aceitável que o padrão 0 seja resolvido na borda (`HttpApi.java:130`) e a validação fique no serviço
(`ReservationService.java:34`). O resto do código continua como na base, com dinheiro em `BigDecimal`, datas em
`LocalDate`, `enum` para situação e plano, e erros com código. Duas observações pequenas, nenhuma que exija mudança:
o `ReservationReceipt` ganhou o componente `children` (`ReservationReceipt.java:17`), e com isso o JSON do check-out
e da segunda via passa a ter esse campo (um acréscimo inofensivo); e o `catch (Exception)` de `HttpApi.java:113`
veio da base.

## Testes

**Números:** 28 testes rodaram, sem falhas, erros ou testes pulados; 28 métodos escritos (a base tinha 26, todos
mantidos). Cobertura de linhas 94,0% (484/515; base 93,8%); de ramos 86,9% (106/122; base 87,1%). O pacote
`hotel.documents` tem 33/33 linhas e 6/6 ramos cobertos.
**Nota:** com problemas

O agente escreveu dois testes contra o banco real:

- `ReservationServiceTest.java:69-79` confere, com valores, que uma criança é gravada e lida de volta pelo banco
  (`service.find(...).children()`, linha 75), que o preço é igual ao de um quarto do mesmo tipo sem criança (linhas
  76-77) e que o comprovante contém "Crianças: 1" (linha 78).
- `ReservationServiceTest.java:81-89` confere que 2 adultos + 1 criança no quarto 101 (capacidade 2) dá
  `CAPACITY_EXCEEDED` e que -1 criança dá `INVALID_REQUEST`.

A pergunta da mutação dá dois resultados:

- **Bem protegido:** tirar `request.children()` da soma da capacidade, ou trocar `>` por `>=`, quebra um dos dois
  testes, porque 1+1 no quarto de 2 tem de passar e 2+1 tem de falhar.
- **Sem rede:** trocar `children > 0` por `children >= 0` em `ReservationReceipt.java:47`, ou seja, imprimir
  "Crianças: 0" em todo comprovante, não quebra nenhum teste. Nenhum teste confere o comprovante sem criança nem a
  posição da linha: a asserção da linha 78 é um `contains`. Também não há teste pela API do campo opcional (sem
  `children`, são 0) nem do `children` na resposta: `HttpApiTest` não foi tocado, e trocar o `0` de
  `HttpApi.java:130` por outro valor passaria.

O comportamento confere esses dois pontos, e eles funcionam. Mas a rede de testes do projeto deixa de fora duas das
regras novas que o dono pediu.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` nas 6 colunas de valor, nenhuma chave
estrangeira sem índice e nenhuma data em texto. Duas migrações: `V1__create_tables.sql` igual à da base e
`V2__add_children.sql` nova. As colunas de situação e de tipo (`status`, `rate_plan`, `type`, `channel`) estão sem
CHECK, como já estavam na base.
**Nota:** bom

O agente fez a mudança de esquema do jeito certo, com uma migração nova em vez de editar a V1. Quem já roda o sistema
sobe sem problema, e as reservas antigas ganham `children = 0`. A V2 tem `integer NOT NULL DEFAULT 0 CHECK
(children >= 0)` (`src/main/resources/db/migration/V2__add_children.sql:2`), e o banco também protege a regra do
negativo. O SQL continua com parâmetros. Os candidatos que o script lista (`ReservationStore.java:33`, `:84`, `:99`,
`:112`) só concatenam constantes (`COLUMNS`). A renumeração dos parâmetros do INSERT está certa
(`ReservationStore.java:43`, 15 `?` na `:33`). A dupla reserva continua protegida pela restrição `EXCLUDE` da base.
A falta de CHECK nas colunas de texto veio da base.

## Comportamento

**Números:** 61 de 61 conferências passaram, nenhuma falhou. Parte comum: 46/46. Dados depois de reiniciar: 3/3.
Cenário 06: 7/7 nas crianças da reserva, 2/2 no comprovante e 1/1 depois de reiniciar. Reservas simultâneas: 2/2.
**Nota:** bom

Pela API, tudo o que o dono pediu funciona:

- sem o campo, a reserva tem zero crianças;
- dois adultos e uma criança cabem no quarto para três, e no quarto para dois são recusados com 422
  `CAPACITY_EXCEEDED`;
- criança não muda o preço;
- número negativo é recusado com 400;
- a consulta mostra `children`;
- o comprovante ganha "Crianças: 2" logo depois da linha do quarto e fica igual quando não há criança;
- o número de crianças continua lá depois de reiniciar.

O que já funcionava continua funcionando, inclusive a recusa da segunda reserva simultânea do mesmo quarto.

## Dúvidas

O agente acrescentou `children` ao JSON do comprovante (check-out e segunda via, `ReservationReceipt.java:17`) sem
que o texto pedisse. Pelo roteiro isso não atrapalha, e um cliente que leia o JSON do comprovante só ganha um campo. Na coluna dos testes, a nota ficou entre `bom` e `com problemas`. As regras centrais (a
capacidade, o negativo e o preço) estão conferidas com valores. Dei `com problemas` porque duas das regras novas (o
comprovante sem criança e o campo opcional pela API) ficaram sem teste.
