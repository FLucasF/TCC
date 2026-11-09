# Revisão: manutenção, cenário 04

## Visão geral

O sistema da base tem 7 pacotes (`hotel`, `hotel.web`, `hotel.reservations`, `hotel.rooms`, `hotel.messages`,
`hotel.documents`, `hotel.reports`). Uma requisição entra pelo `hotel.web.HttpApi`, passa pelo `ReservationService`
e chega às classes de acesso a dados (`ReservationStore`, `Rooms`, `SentMessages`), que gravam no PostgreSQL. O
esquema vem de uma migração do Flyway, e todas as peças são montadas em `Hotel.java:21-27`. O pedido era trocar o
assunto e uma linha do e-mail de confirmação, e o agente fez só isso. O `changes.patch` muda dois arquivos: em
`src/main/java/hotel/reservations/ReservationService.java:67` troca o assunto, em
`ReservationService.java:129-131` põe as três linhas novas no corpo, e em
`src/test/java/hotel/reservations/ReservationServiceTest.java:94` atualiza a asserção do assunto. Não há estrutura
nova para o "talvez um dia por WhatsApp". O roteiro passa inteiro (58 de 58). Nenhum defeito é sério. Há dois
pequenos: o nome da pousada ficou escrito à mão no assunto, em vez de usar a constante `HOTEL_NAME`, e as três linhas
novas do corpo não têm teste.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base (`measures/dependencies-base.md`). A única diferença no jdeps é o
`ReservationService`, que passou de 138 para 140 linhas.
**Nota:** bom

O agente não mexeu na estrutura: a mudança ficou dentro do método que já montava o e-mail
(`ReservationService.java:125-133`). O ciclo vem da base: `ReservationService.java:3` importa
`hotel.documents.ReservationReceipt`, e `ReservationReceipt.java:3` importa `hotel.reservations.Reservation`. A
borda HTTP fica em `hotel.web`, o único pacote com Javalin além da montagem. O JDBC fica nas classes de dados, e a
montagem num lugar só (`Hotel.java:21-27`). Nada disso foi piorado.

## Design de baixo nível

**Números:** 2 trechos de produção mudados no mesmo método e no mesmo arquivo (o assunto e o corpo). Nenhuma classe,
interface ou método novo.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O controle negativo pedia a mudança pequena, no lugar onde o texto já mora, e foi o que o agente fez. O assunto mudou
em `ReservationService.java:67`, e as três linhas novas entraram no lugar da antiga, dentro de
`confirmationEmailBody` (`ReservationService.java:129-131`). A saudação, o comprovante e o "Até breve!" continuam
(`ReservationService.java:126-128,132`). A frase sobre o WhatsApp não gerou canal abstrato, modelo de mensagem nem
ouvinte: o `EmailSender` está igual ao da base (`hotel/messages/EmailSender.java`).

O resto do código vem da base e é limpo: dinheiro em `BigDecimal` arredondado em `PriceCalculator.cents`, datas em
`LocalDate`, situações e planos em `enum`, e o estado da `Reservation` só muda por métodos do pacote. O único reparo
no que o agente escreveu é pequeno. O assunto traz "Pousada Maré Alta" escrito à mão
(`ReservationService.java:67`), mas a classe tem a constante `HOTEL_NAME` (`ReservationService.java:12`), e o corpo
a usa (`ReservationService.java:127`). Se o nome mudar, o assunto fica para trás. É uma observação menor, e não
justifica baixar a nota. Outra mudança pequena, que o pedido não fazia: o `"\n\n"` em
`ReservationService.java:131` põe uma linha em branco antes do "Até breve!", e na base não havia. É uma escolha
razoável de formatação.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros e 0 pulados; 26 métodos escritos. Linhas cobertas: 471 de 502
(93,8%); ramos cobertos: 101 de 116 (87,1%). São os mesmos números da base (`measures/tests-base.md`). O agente
não acrescentou nem apagou teste.
**Nota:** com problemas

O agente manteve os 26 testes da base passando e atualizou a asserção do assunto com o valor exato
(`ReservationServiceTest.java:94`). O corpo novo ficou sem conferência: o teste do e-mail só confere o desconto e o
total do comprovante (`ReservationServiceTest.java:95-96`), e nenhum teste procura as três linhas novas, o
"Até breve!" ou a falta da linha antiga. A pergunta da mutação mostra o furo. Os 26 testes continuariam passando se
alguém apagasse a linha do café da manhã, trocasse o telefone (79) 3200-0000 ou devolvesse a linha antiga dos
horários. Metade do que o dono pediu (o corpo) não tem rede de proteção.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`) e 1 migração do Flyway, igual à da base. Nenhuma tabela
sem chave primária, dinheiro todo em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto.
O `EXCLUDE` com `gist` impede duas reservas do mesmo quarto em noites que se cruzam. As colunas `status`, `rate_plan`,
`type` e `channel` não têm `CHECK`.
**Nota:** bom

O pedido não tocava no banco, e o agente não tocou: `V1__create_tables.sql` é igual ao da base, e nenhuma classe de
dados mudou. A sobreposição de reservas é protegida no próprio banco (`V1__create_tables.sql:39-40`). Os SQL montados
com `+` em `ReservationStore.java:33,83,98,111` só juntam constantes, e os valores entram por `?`. A falta de `CHECK`
nas colunas de texto que guardam `enum` vem da base e é menor.

## Comportamento

**Números:** 58 de 58 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário
04 7 de 7 e as reservas simultâneas 2 de 2.
**Nota:** bom

O hóspede recebe o e-mail com o assunto novo. A saudação e o comprovante continuam, as três linhas novas vêm na
ordem pedida, a linha antiga saiu, e o "Até breve!" continua no fim. O resto do sistema funciona como na base: preços,
recusas, confirmação, check-in e check-out, cancelamentos, relatórios, os dados depois de reiniciar e as reservas
simultâneas. Para o dono da pousada, o pedido foi atendido sem quebrar nada.

## Dúvidas

O agente pôs uma linha em branco entre as linhas novas e o "Até breve!" (`ReservationService.java:131`), e o pedido
dizia que o resto continuava "como está". Considerei isso só formatação, porque o roteiro aceita, e não pesou em
nenhuma coluna.
