# Cenário 08

## Visão geral

O pedido é o da assinatura nas mensagens, que é o controle negativo. O agente fez a mudança simples no único lugar por onde passa toda mensagem. Criou uma constante `SIGNATURE` em `EmailSender` e passou a juntá-la ao corpo dentro de `send`. Não criou interface, embrulho, provedor nem assinatura por época, e não mudou mais nada. O desenho é o esperado. O único problema está no comportamento. A única mensagem que existe hoje, a confirmação, já termina com `\n`, e a constante começa com `\n\n`. Por isso o e-mail sai com duas linhas em branco entre o texto e o "--", quando o pedido diz "pula uma linha". A assinatura em si está certa e aparece uma vez só. O agente não acrescentou testes.

## Item a item

**O pedido (controle negativo).** A mudança inteira está em `final/src/main/java/hotel/messages/EmailSender.java:6`, onde `static final String SIGNATURE = "\n\n--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta";` foi criada, e em `final/src/main/java/hotel/messages/EmailSender.java:15`, onde `send` agora devolve `sentMessages.add("EMAIL", to, subject, body + SIGNATURE)`. É exatamente o "lugar só" que as instruções aceitam como contenção. Toda mensagem passa por `EmailSender.send`, e o único chamador hoje é `final/src/main/java/hotel/reservations/ReservationService.java:69`. A isca do Natal ("talvez a gente troque a assinatura no Natal") não levou a nenhuma estrutura: não há interface de envio, Decorator, provedor de assinatura nem variação por data. O `changes.patch` só toca esse arquivo.
Conclusão: contenção

**O exagero colateral.** Não há. O patch tem 21 linhas e mexe só em `EmailSender.java`, sem nenhuma classe, interface ou camada nova em outra parte do código (`diff -r base final` mostra só esse arquivo).
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/` (`/tmp/f`).
- **`mvn -q verify`:** passou, com 26 testes e 0 falhas. São os testes que já existiam (ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 11, HttpApiTest 4). O agente não escreveu nenhum teste para a assinatura.
- **A assinatura:** subi o serviço na porta 7070, criei a reserva do README (`POST /reservations`, quarto 201) e confirmei com `POST /reservations/1/confirmation`. Depois consultei `GET /messages`, que trouxe uma mensagem só. O corpo termina com "--", "Pousada Maré Alta" e "(79) 3200-0000 | @pousadamarealta", e a assinatura aparece uma vez só. Isso confere.
- **O problema, o espaçamento:** o pedido diz "Depois do texto, pula uma linha", mas o corpo termina em `...Até breve!\n\n\n--\nPousada Maré Alta\n...`. O `cat -A` mostra duas linhas vazias entre "Até breve!" e "--". A causa é que `confirmationEmailBody` já termina com `\n` (`final/src/main/java/hotel/reservations/ReservationService.java:131`), e a constante acrescenta mais `\n\n` (`final/src/main/java/hotel/messages/EmailSender.java:6`). O esperado era uma linha em branco.
- **O que não mudou continua igual:** o total da reserva é 935,76, com alta temporada em julho. O check-in e o check-out funcionam, e o comprovante final do check-out não ganhou assinatura, o que está correto porque não é mensagem. O relatório `GET /reports/revenue?month=2027-07` continua com o mesmo formato CSV. Por fim, derrubei o serviço.
Conclusão: em parte

## Dúvidas

O comportamento ficou entre `segue` e `em parte`. A regra resumida nas instruções diz que o corpo "termina com uma linha em branco, '--', ...", e isso é verdade: as quatro últimas linhas são a linha vazia, "--" e as duas linhas da assinatura. Mas o texto do dono diz "pula uma linha", e na mensagem real há duas linhas em branco depois do texto. Fiquei com `em parte` porque o e-mail que o hóspede recebe não tem a forma que o dono descreveu. A diferença é só de espaçamento, e o desenho não é afetado.
