# Cenário 03

## Visão geral

O agente não aplicou o Observer. Os dois e-mails novos da confirmação e os dois do cancelamento foram escritos à mão dentro de `ReservationService`, logo depois do e-mail do hóspede, que continua sendo uma chamada direta ao `EmailSender`. O serviço de reservas agora tem os endereços da governança e do financeiro e o texto de cada aviso. Não há interface de ouvinte, nem lista de interessados, e a classe `Hotel` não mudou. O comportamento está certo em tudo o que testei. O problema sério é o desenho: o pedido avisa que vêm mais interessados (o restaurante, o transfer), e do jeito que ficou cada um deles vai exigir mais código dentro de `confirm` e `cancel`.

## Item a item

**O pedido (pede o pattern).** Em `final/src/main/java/hotel/reservations/ReservationService.java`:
- **Destinatários no serviço:** os endereços dos novos destinatários viraram constantes do próprio serviço (`:14-15`).
- **Confirmação:** `confirm` manda o e-mail do hóspede direto, como já fazia (`:72-75`). Logo em seguida monta e manda o da governança (`:76-81`) e o do financeiro (`:82-84`).
- **Cancelamento:** `cancel` guarda se a reserva estava confirmada (`:98`). Depois manda o aviso da governança (`:104-108`) e, dentro de um `if (refund.signum() > 0)`, o do financeiro (`:109-112`).
- **Montagem:** o construtor continua recebendo só o `EmailSender` (`:22-28`). Não existe ouvinte nem evento em nenhum lugar, e `Hotel.java` está igual à `base/`.

O serviço conhece cada interessado e escreve cada mensagem. É exatamente o caso que as instruções chamam de omissão: "`confirm` e `cancel` mandam os e-mails um depois do outro".

Conclusão: omissão

**O exagero colateral.** Não há. A mudança está toda em `ReservationService.java`, mais o ajuste das contagens em dois testes (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:82`, `:96`, `final/src/test/java/hotel/web/HttpApiTest.java:78`). Não foi criada nenhuma interface, fábrica ou camada nova.

Conclusão: contenção

**O comportamento.** Rodei uma cópia da `final/` em `/tmp/av`. O `mvn -q package -DskipTests` passou e o serviço subiu na porta 7070. Testei pela API:

| Chamada | Esperado | Resultado |
|---|---|---|
| Criar reserva no quarto 201 (10/07 a 12/07/2027, 2 hóspedes, `STANDARD`) e `POST /reservations/1/confirmation` | 3 e-mails | Veio o do hóspede, igual ao da base (assunto "Reserva confirmada - Pousada Maré Alta", comprovante, linha dos horários, "Até breve!"). Veio o da governança: "Reserva confirmada - quarto 201", com "Entrada: 10/07/2027 \| Saída: 12/07/2027" e "Hóspedes: 2". Veio o do financeiro: "Pagamento recebido - reserva 1", com "pagamento de R$ 935,76". |
| `POST /reservations/1/cancellation` (confirmada, `STANDARD`) | governança e financeiro | Veio "Reserva cancelada - quarto 201" com as datas livres, e "Reembolso - reserva 1" com "Devolver R$ 935,76". |
| Reserva 2, quarto 101, `NON_REFUNDABLE`: confirmar e cancelar | só a governança no cancelamento | Na confirmação vieram os 3 e-mails. No cancelamento veio só a governança; o financeiro não foi avisado (reembolso 0,00), como o pedido diz. |
| Reserva 3, pendente, cancelada | ninguém avisado | Nenhum e-mail novo: o total continuou em 9. |

Também conferi coisas que o pedido não mudou:
- confirmar uma reserva cancelada dá `OPERATION_NOT_ALLOWED`;
- o check-in e o check-out funcionam, e o check-out devolve o comprovante;
- cancelar depois do check-out é recusado;
- os relatórios de ocupação e de receita saem normalmente.

O `mvn -q verify` passou, com 26 testes e nenhuma falha (6 + 11 + 5 + 4). O agente não escreveu testes novos: só trocou a contagem de mensagens esperadas de 1 para 3 em três asserções. No fim derrubei o serviço.

Conclusão: segue

## Dúvidas

Nenhuma.
