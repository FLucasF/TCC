# Revisão: manutenção, cenário 04

## Visão geral

O sistema tem 7 pacotes. `hotel.web` é a borda HTTP (Javalin), `hotel.reservations` guarda as regras e o acesso ao banco
das reservas, e há também `hotel.rooms`, `hotel.documents` (o comprovante), `hotel.messages` (os e-mails guardados) e
`hotel.reports`. Tudo é montado em `hotel/Hotel.java:20-29`. Uma requisição entra numa rota de
`hotel/web/HttpApi.java:61-79`, que chama o `ReservationService`. O serviço valida e grava pelo `ReservationStore`
(JDBC com parâmetros, num PostgreSQL criado pelo Flyway). Na confirmação, o e-mail sai pelo `EmailSender`, que o guarda
na tabela `messages`. O pedido mudava só o texto do e-mail de confirmação. O agente fez exatamente isso, nos dois lugares
onde o texto já estava: o assunto em `src/main/java/hotel/reservations/ReservationService.java:67` e as três linhas novas
no lugar da antiga em `ReservationService.java:129-131`. Também ajustou o teste do assunto
(`src/test/java/hotel/reservations/ReservationServiceTest.java:94`). Não criou nenhuma estrutura para o WhatsApp que o
pedido só menciona. Não há defeito sério. Os pontos fracos são dois: nenhum teste confere o corpo novo do e-mail, e os
dois arquivos tocados passaram a usar fim de linha CRLF.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. A única
diferença é `hotel.reservations`, que tinha 508 linhas e agora tem 510 (as duas linhas novas do texto).
**Nota:** bom

O agente não mexeu na estrutura. A mudança ficou dentro do serviço que já montava o e-mail
(`ReservationService.java:64-68`, `:125-133`). A borda HTTP continua fina, só lendo a requisição e devolvendo a resposta
(`hotel/web/HttpApi.java:64-79`). As regras não dependem do Javalin, e o JDBC fica no `ReservationStore`, `Rooms` e
`SentMessages`, como na base. O ciclo também vem da base: `hotel/documents/ReservationReceipt.java:3` importa
`Reservation`, e `ReservationService.java:3` importa `ReservationReceipt`. É pequeno, entre dois pacotes vizinhos, e o
agente não o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudou, com 1 linha trocada no assunto e 1 linha trocada por 3 no corpo. Nenhuma
classe, interface ou método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O cenário é um controle negativo. A frase "talvez um dia a gente mande essa confirmação por WhatsApp" tenta o agente a
criar um canal de mensagem abstrato. O agente resistiu e fez a mudança pontual onde o texto já morava:

- o assunto em `ReservationService.java:67`, reaproveitando a constante `HOTEL_NAME` (`:12`), como a base já fazia;
- as três linhas novas em `confirmationEmailBody` (`ReservationService.java:129-131`), mantendo a saudação (`:126-127`),
  o comprovante (`:128`) e o "Até breve!" (`:132`).

O `EmailSender` (`hotel/messages/EmailSender.java:4-15`) ficou como estava. Não apareceu nenhuma interface de canal,
nenhum ouvinte e nenhum modelo de mensagem. Isso é contenção, sem exagero.

O resto do código é o da base e continua bom:

- dinheiro em `BigDecimal`, arredondado a centavos (`hotel/reservations/PriceCalculator.java:50-52`);
- situações e planos como `enum`;
- os métodos que mudam a reserva são privados do pacote (`hotel/reservations/Reservation.java:102-115`);
- os erros têm códigos próprios (`HotelException`/`ErrorCode`).

Há uma ressalva de higiene, não de design. `ReservationService.java` e `ReservationServiceTest.java` passaram a ter fim
de linha CRLF: 140 e 162 linhas com `\r`, contra 0 na base e 0 nos demais arquivos. Assim, um `diff` sem tratar fim de
linha mostra o arquivo inteiro como mudado. Veja "Dúvidas".

## Testes

**Números:** 26 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 26 métodos de teste escritos, os mesmos 26 da
base. Cobertura de 93,8% das linhas (471/502) e 87,1% dos ramos (101/116), idêntica à da base
(`measures/tests.md` e `measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base passando e não apagou nem enfraqueceu nenhum. Atualizou o teste que confere o
assunto, com o valor exato: `ReservationServiceTest.java:94`. Mas o corpo novo do e-mail ficou sem rede. O teste
`confirmingSendsTheReceiptByEmail` só confere o desconto e o total do comprovante no corpo
(`ReservationServiceTest.java:95-96`). Nenhum teste procura "Café da manhã das 7h às 10h", o telefone "(79) 3200-0000",
a linha nova de entrada e saída, a ausência da linha antiga ou o "Até breve!". Pela pergunta da mutação: apagar a
linha `ReservationService.java:130` ou `:131`, ou trocar o telefone, não derruba nenhum teste. A base também não
conferia a linha antiga, mas o texto do corpo era justamente o que o pedido mudava, e uma asserção a mais naquele
mesmo teste bastaria.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro em `numeric(10,2)`, sem
chave estrangeira sem índice e sem data guardada como texto. A única migração, `V1__create_tables.sql`, está igual à da
base (`measures/database.md`). As colunas de situação, plano, tipo e canal são `text` sem CHECK, o que vem da base.
**Nota:** bom

O agente não tocou no banco, e o pedido não pedia isso. A migração da base continua intacta, então o Flyway sobe sem
problema num banco que já roda. O esquema da base protege a regra principal: a restrição
`EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&) WHERE (status <> 'CANCELLED')`
(`src/main/resources/db/migration/V1__create_tables.sql:39-40`) impede duas reservas do mesmo quarto na mesma noite. O
SQL usa parâmetros e try-with-resources (`hotel/reservations/ReservationStore.java:34`, `:80-87`). Os
`"SELECT " + COLUMNS` apontados pelo script juntam só uma constante. A falta de CHECK em `status`/`rate_plan`
(`V1__create_tables.sql:28`, `:33`) vem da base.

## Comportamento

**Números:** 57 de 57 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 04 | 6 de 6 |
| as reservas simultâneas | 2 de 2 |

No cenário 04, o roteiro conferiu o assunto novo, a saudação, o comprovante, as três linhas novas em ordem com o
"Até breve!" depois delas, e a saída da linha antiga.
**Nota:** bom

Para o dono da pousada, isso quer dizer o seguinte:

- o hóspede que confirma a reserva recebe o e-mail com o assunto "Sua reserva na Pousada Maré Alta está confirmada!";
- no corpo, a linha antiga do horário deu lugar às três linhas pedidas, com a saudação, o comprovante e o "Até breve!"
  mantidos;
- todo o resto continua funcionando como antes: preços, recusas, cancelamentos, relatórios, os dados guardados depois
  de reiniciar e a proteção contra duas reservas simultâneas do mesmo quarto.

## Dúvidas

A troca do fim de linha para CRLF em `ReservationService.java` e `ReservationServiceTest.java` pode vir do ambiente onde
o agente rodou (um editor ou um checkout no Windows), e não de uma escolha dele. A evidência não decide qual das duas.
Ela não muda o comportamento nem a compilação: o `mvn verify` passou e as 57 conferências passaram. Por isso não pesou
na nota. Ainda assim, deixaria um `git diff` sem `--ignore-cr` com o arquivo inteiro marcado como mudado.
