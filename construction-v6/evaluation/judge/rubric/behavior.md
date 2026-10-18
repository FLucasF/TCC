# A dimensão: o comportamento

Aqui a pergunta é a que as outras dimensões não fazem: **o serviço faz o que o enunciado pede?** Você responde
testando, como um testador que recebeu o enunciado e o serviço: sobe o serviço, chama a API do jeito que o site
chamaria e compara com o que o `prompt.md` diz. Os valores certos estão no próprio enunciado (as regras, os exemplos
conferidos pelo financeiro, a tabela "Quando dá erro"); não há outro gabarito.

## Como subir

- **O banco.** Um PostgreSQL 18 já está rodando ao lado, só para você; o endereço, o usuário e a senha estão nas
  variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`, as mesmas que o enunciado manda o serviço ler. Ele
  começa vazio. Não há como apagá-lo entre um teste e outro: use CPFs e quantidades novas em cada teste e confira
  o estoque pela diferença.
- **O serviço.** Copie o `workspace/` para outro lugar (nunca mude a pasta de trabalho), compile com
  `mvn -q package -DskipTests` e suba o jar com `java -jar <jar> --server.port=8081 &`. Para duas máquinas, suba
  um segundo na 8082, com o mesmo banco.
- **A TransLog.** É você quem a imita: escreva um servidor pequeno com o `HttpServer` do JDK (um `Stub.java` que se
  roda com `java Stub.java`), que responda como o anexo "A TransLog" descreve, e passe o endereço na variável
  `TRANSLOG_URL` ao subir o serviço. Faça-o responder do jeito que cada teste pede (um preço com mais de duas
  casas, o CEP não atendido, uma resposta que demora mais que o tempo limite, o site fora do ar).
- **As chamadas.** Com `curl` e, se ajudar, `jq` (os dois estão instalados). Derrube tudo no fim.

Se o serviço não subir, diga por quê (o erro do log) e marque os itens que dependem dele como `não se aplica`.

## Os itens

**C1, o serviço sobe.** Ele sobe com o banco vazio e cria o que precisa sozinho? Quanto tempo leva?

**C2, o catálogo.** Os produtos do anexo "O catálogo inicial" estão lá, com os valores do anexo, e o produto fora
de linha aparece como tal?

**C3, o resumo.** Os dez "Exemplos conferidos pelo financeiro" dão os mesmos valores, campo a campo? E pelo menos
três fronteiras que o próprio texto marca (um peso exatamente no limite do motoboy, um valor exatamente no limite
de um cupom, o limite de 30% do cupom, o arredondamento "meio para o par")?

**C4, as recusas.** Os casos da tabela "Quando dá erro" devolvem o status e o código pedidos? Inclua um pedido com
dois problemas ao mesmo tempo, para ver se vale a ordem que o enunciado diz.

**C5, a TransLog.** A econômica e a expressa usam o preço e o prazo da TransLog, traduzidos como o anexo pede
(centavos para reais, horas para dias arredondando para cima, a resposta de CEP não atendido, o tempo limite)? E o
motoboy e a retirada continuam funcionando com a TransLog fora do ar?

**C6, o ciclo do pedido.** Registrar, pagar, enviar, entregar e cancelar fazem o que o enunciado diz: o estoque baixa
só no pagamento; o cancelamento de um pedido pago devolve o estoque e o crédito, e o de um não pago não devolve
nada; o pagamento depois do prazo de 48 horas é recusado (mande um `paidAt` além do prazo); as operações fora de
ordem são recusadas; os avisos aparecem e podem ser marcados como lidos.

**C7, o aviso de pagamento repetido.** O enunciado diz que o banco às vezes avisa o mesmo pagamento duas vezes, que
o aviso repetido não pode baixar o estoque nem dar o crédito duas vezes, e que o serviço roda em duas máquinas ao
mesmo tempo: o aviso repetido pode chegar depois do primeiro ou junto com ele. Teste os dois jeitos:
- **um depois do outro**: o mesmo aviso, repetido, devolve exatamente a mesma resposta da primeira vez e não faz
  nada de novo (o estoque não baixa duas vezes)? E outro `transactionId`, num pedido que já não aguarda pagamento, é
  recusado?
- **ao mesmo tempo**: dispare o mesmo aviso umas cinco vezes juntas, divididas entre as duas máquinas. O estoque
  baixa uma vez só, o crédito entra uma vez só, sai um aviso `PAYMENT_APPROVED` só, e todas as respostas são a da
  primeira vez? E cinco avisos com `transactionId` diferentes, juntos, no mesmo pedido: só um é aceito, e os outros
  são `409 OPERATION_NOT_ALLOWED`?

**C8, a última peça.** O enunciado diz que dois clientes já pagaram a última peça quase no mesmo segundo, que isso
não pode acontecer, que o estoque nunca fica negativo e que, sem estoque na hora do pagamento, o pedido é cancelado
e o cliente é avisado. Com o serviço em duas máquinas e o mesmo banco, dispare juntos, um em cada máquina, vários
pagamentos de pedidos que disputam as mesmas unidades (dois pedidos para a última unidade, e mais pedidos que
unidades, como seis pedidos para duas), várias vezes:
- só passam tantos pagamentos quantas unidades havia, e o estoque nunca fica negativo?
- quem perde recebe `409 INSUFFICIENT_STOCK`, o pedido dele fica `CANCELLED` e sai o aviso `ORDER_CANCELLED`?

**C9, os dados não se perdem.** Derrube o serviço e suba de novo: os pedidos, o estoque e os avisos continuam lá?

**C10, os testes do próprio agente.** O `mvn -q verify` da cópia passa? Quantos testes rodam? (Só conte; a qualidade
dos testes é outra dimensão.)

Em cada item, diga o que testou, com as chamadas principais, o que esperava e o que veio. A conclusão é `segue`
quando tudo o que você testou confere, `em parte` quando uma parte confere e outra não, `não segue` quando o
principal do item falha, e `não se aplica` quando não deu para testar (diga por quê). Um ponto que o enunciado não decide (duas leituras
possíveis, ou um caso que ele não cobre) não muda a conclusão: conte o que viu na seção "Dúvidas".
