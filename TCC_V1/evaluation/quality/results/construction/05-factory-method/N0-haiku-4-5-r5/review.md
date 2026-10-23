# Revisão: construção, cenário 05

## Visão geral

O projeto é um Javalin com seis pacotes. `Application` (326 linhas) declara todas as rotas, lê o corpo como `Map`, chama serviços estáticos (`service`), que chamam repositórios estáticos (`repository`). Os repositórios usam JDBC por um pool Hikari guardado num `static` em `config/DatabaseConfig.java:13`, que também cria as três tabelas com `CREATE TABLE IF NOT EXISTS` (`config/DatabaseConfig.java:64-110`). A escolha do documento por tipo de hóspede está num lugar só, `DocumentService.generateDocument` (`service/DocumentService.java:35-44`), e os três lugares que emitem o documento passam por ele. Quando a confirmação recebe um valor, os três documentos saem certos (conferi à mão: fatura com "ISS retido (5%): R$ 38,99", booking receipt com "BRL 1,559.60"). O defeito mais sério é a confirmação: ela exige um `amountPaid` no corpo (`Application.java:144-151`). O texto não pede esse campo e diz que o pago é o total. Sem ele, a confirmação responde 400, e por isso não sai nenhum e-mail, check-in, check-out, segunda via nem relatório pelo caminho que o texto descreve (40 de 83 conferências passaram). Além disso, o valor mandado pelo cliente é gravado sem conferência: aceitei `amountPaid: 1` numa reserva de R$ 779,80. Outros problemas: nada impede a mesma noite reservada duas vezes (houve duas reservas em 9 de 10 rodadas simultâneas), há só nove testes de unidade (3,5% das linhas cobertas), e falta validar a identificação incompleta (CNPJ sem razão social, passaporte sem país).

## Arquitetura

**Números:** 6 pacotes, 12 dependências entre eles, nenhum ciclo. `service` usa JDBC, e o pacote raiz (`Application`) usa Javalin, JDBC e Jackson. A maior classe é `Application.java`, com 326 linhas (de `measures/dependencies.md`).
**Nota:** com problemas

As dependências seguem a direção certa (raiz → service → repository → model) e as regras não conhecem o Javalin. As camadas, porém, vazam em vários pontos:

- **SQL dentro de serviço:** `ReportService` monta e executa SQL direto (`service/ReportService.java:23-35` e `62-76`) e chama o repositório de quartos dentro do laço (`service/ReportService.java:43`).
- **Handlers que pulam o serviço:** alguns handlers vão direto aos repositórios (`Application.java:69`, `Application.java:223`) e montam a resposta à mão como `Map` (`Application.java:70-77`, `224-232`).
- **Regra de negócio na borda HTTP:**
  - a regra "segunda via só para reserva paga" mora no handler, e errada: só recusa PENDING (`Application.java:209`);
  - a recusa por falta de campo também está no handler (`Application.java:102-106`).
- **Serviço preso a DTO de HTTP:** o serviço de documentos devolve um DTO da camada HTTP (`service/DocumentService.java:17`, `dto.DocumentResponse`), o que explica a dependência `service -> dto`.
- **Sem montagem:** tudo é `static`. A ligação entre as peças é feita por chamadas estáticas globais (`DatabaseConfig.getConnection()` em cada repositório; `ReservationService.java:106` chama `MessageService` estático), e não há como trocar uma peça.
- **Erros como texto:** os serviços sinalizam o erro pelo texto de uma `IllegalArgumentException` (`service/ReservationService.java:27`), e a borda o traduz por comparação de strings (`Application.java:277-292`).

São misturas localizadas, sem ciclo, num sistema pequeno.

## Design de baixo nível

**Números:** 3 tipos de documento, os 3 numa estrutura só (`DocumentService.generateDocument`). Os 3 lugares que emitem o documento (e-mail `service/MessageService.java:13`, check-out `Application.java:193`, segunda via `Application.java:212`) pedem o documento a ela, e nenhum o monta à mão.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A decisão de qual documento criar mora num método só (`service/DocumentService.java:35-44`), que olha a identificação da reserva e chama o gerador de cada tipo (`generateComprovante`, `generateFatura`, `generateBookingReceipt`). Os três pontos de emissão só chamam esse método. A referência aceita essa fábrica concentrada como acerto, e o documento de uma agência entraria num único lugar.

O que afasta da forma ideal:
- o tipo de hóspede não existe como tipo: a escolha é inferida por campos `null`, e qualquer reserva sem CPF e sem CNPJ cai no `else` do booking receipt (`DocumentService.java:41-44`);
- a lista de jeitos de se identificar se repete na validação (`service/ReservationService.java:44-50`).

**Exagero.** Não há interfaces de uma implementação só nem camadas que apenas repassam: contenção.

**O resto do código** tem defeitos espalhados, porém localizados:
- `generateComprovante` e `generateFatura` repetem as mesmas oito linhas (`DocumentService.java:49-67` e `69-91`);
- o mesmo documento consulta o quarto no banco duas vezes (`DocumentService.java:27` e `54`/`75`/`99`);
- `formatUsd` formata BRL (`DocumentService.java:120`);
- `createReservation` recebe 12 parâmetros soltos (`service/ReservationService.java:18-21`);
- `catch (Exception)` transforma qualquer falha em 400 INVALID_REQUEST, inclusive erro de banco nos relatórios (`Application.java:118`, `157`, `251`, `268`);
- há estado global mutável (`config/DatabaseConfig.java:13`), DTOs com campos públicos (`dto/DocumentResponse.java:7-23`) e loggers declarados e nunca usados (`repository/RoomRepository.java:18`, `repository/MessageRepository.java:17`, `repository/ReservationRepository.java:16`).

Os tipos do domínio estão certos: `BigDecimal` com `HALF_EVEN` (`service/PricingService.java:15-43`), `LocalDate` e enums para situação e plano.

## Testes

**Números:** 9 testes rodaram, 0 falhas, 0 erros, 0 pulados; 9 métodos escritos, num arquivo só. Cobertura de 27 de 780 linhas (3,5%) e 14 de 182 ramos (7,7%): só o pacote `service` tem algo coberto, e todos os outros ficam em 0 (de `measures/tests.md`).
**Nota:** ruim

**O que existe.** O arquivo `ReservationIntegrationTest` não é de integração: testa só funções de `PricingService`, com valores certos:
- 599,76 em alta temporada (`ReservationIntegrationTest.java:27`);
- desconto de 89,96 (`:50`);
- ISS de 38,99 (`:103`).

O `testBankerRounding` (`:107-122`) testa o `BigDecimal` do JDK, e não o código do projeto.

**O que falta.** Não há teste pela API nem contra o banco, e nada confere:
- as situações da reserva;
- os cancelamentos e as devoluções;
- os códigos de erro;
- os relatórios;
- a parte nova do cenário: qual documento cada hóspede recebe, o texto da fatura e do booking receipt, "1 night", e a recusa de identificação dupla ou incompleta.

**A pergunta da mutação.** Estas trocas passariam sem nenhum teste falhar:
- inverter a escolha entre fatura e comprovante em `DocumentService.java:35-44`;
- apagar a linha do ISS da fatura (`DocumentService.java:87-88`);
- trocar `day >= 15` por `day > 15` em `service/PricingService.java:59` (só 20/12 é testado).

Um único teste da confirmação pela API teria mostrado a falha que derruba o comportamento.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` e datas em `date`. As conferências acusaram:
- a chave estrangeira `reservations(room_number) -> rooms` não tem índice;
- as 4 colunas de situação ou tipo não têm `CHECK`;
- não há nenhuma restrição UNIQUE, EXCLUDE ou CHECK;
- 0 transações e 0 `FOR UPDATE`;
- o esquema é criado no código, sem migração.

Os números são de `measures/database.md`.
**Nota:** ruim

**Reservas duplicadas.** Nada no banco nem no código protege a regra "um quarto, uma reserva por noite". A disponibilidade é consultada (`repository/ReservationRepository.java:84-102`) e a reserva gravada depois, em outra conexão, sem transação, trava ou restrição de exclusão (`service/ReservationService.java:53` e `88`). O roteiro mediu duas reservas criadas para o mesmo quarto e as mesmas noites em 9 de 10 rodadas simultâneas: é dado errado de verdade, não um risco teórico.

**Os outros defeitos:**
- o esquema nasce de `CREATE TABLE IF NOT EXISTS` no código (`config/DatabaseConfig.java:64`, `74`, `101`), que não sabe evoluir uma tabela que já existe;
- a confirmação grava a reserva e o e-mail em duas escritas sem transação (`service/ReservationService.java:103` e `106`);
- `status`, `rate_plan` e `type` aceitam qualquer texto (`config/DatabaseConfig.java:87`, `92`, `66`);
- o financeiro filtra com `EXTRACT` sobre `check_in` (`service/ReportService.java:65-66`), o que impede um índice.

**O que está certo.** O SQL usa sempre parâmetros `?`, as conexões vêm do pool e são devolvidas por try-with-resources, e os dados ficam no banco depois de reiniciar (as mensagens continuaram lá).

## Comportamento

**Números:** 40 de 83 conferências passaram.

| parte | passaram |
|---|---|
| parte comum | 29 de 46 |
| depois de reiniciar | 1 de 3 |
| cenário 05 | 9 de 32 |
| reservas simultâneas | 1 de 2 |

Os números são de `measures/behavior.md`.
**Nota:** ruim

**A confirmação.** A falha central é a confirmação: `POST /reservations/{id}/confirmation` sem corpo responde 400 INVALID_REQUEST, porque o handler exige `amountPaid` (`Application.java:142-148`). O texto diz que o pago é o total e não descreve corpo nenhum. Para o dono da pousada, isso quer dizer:
- nenhuma reserva é confirmada pelo site do jeito combinado;
- não sai e-mail, e não dá para fazer check-in, check-out nem tirar a segunda via;
- os relatórios ficam vazios (`total,0/5` e `total,0`);
- os cancelamentos de reservas "confirmadas" devolvem 0,00 em vez de 299,88.

**Falhas em cadeia.** Quase todas as 23 falhas do cenário e as 2 de "depois de reiniciar" vêm daí. A reserva que deveria estar CHECKED_OUT foi cancelada no passo seguinte, e por isso volta CANCELLED e não ocupa mais o quarto.

**Quando a confirmação recebe um valor.** Chamei a API à mão com `amountPaid`. Os documentos saem como o texto pede, inclusive no e-mail em português. Mas o valor do cliente é gravado sem conferência: `amountPaid: 1` para um total de 779,80 foi aceito.

**Falhas independentes da confirmação:**
- CNPJ sem razão social é aceito (201), e passaporte sem país também passa da validação, que só conta CPF, CNPJ e passaporte (`service/ReservationService.java:44-50`). O roteiro registrou 409 em vez de 400 porque a reserva anterior ocupou o quarto.
- O `amountPaid` 0 sai como `0`, e não `0.00` (`model/Reservation.java:51`). O `refund` 0 também (`service/ReservationService.java:122`).
- Em 9 de 10 rodadas simultâneas, as duas reservas foram criadas. Esse defeito conta na coluna do banco.

**Achados pela leitura, fora do roteiro:**
- a segunda via sai também para reserva cancelada (`Application.java:209` só recusa PENDING; conferi: 200 com o booking receipt de uma reserva CANCELLED);
- o financeiro incluiria a reserva STANDARD cancelada com devolução, com valor 0,00, porque filtra `amount_paid > 0` (`service/ReportService.java:67`), e o texto pede só as que deixaram dinheiro.

## Dúvidas

- **Design.** O pattern foi resolvido como fábrica concentrada: um método escolhe o documento pelos campos de identificação. Não há o Factory Method clássico, com criador sobrescrito, nem método polimórfico no tipo de hóspede. Como o tipo de hóspede não existe e a escolha se baseia em `null`, o acerto é pela forma da decisão, que está num lugar só, e não por um modelo do tipo de hóspede.
- **Banco.** Fiquei entre `com problemas` e `ruim`. A lista da skill para `ruim` não cita a falta de proteção contra reserva dupla, mas aqui não há proteção nenhuma, nem dentro do processo, e a duplicação foi medida em 9 de 10 rodadas. Por isso dei `ruim`.
