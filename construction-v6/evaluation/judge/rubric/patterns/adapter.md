# As instruções do pattern: Adapter

Este pattern cuida de um ponto que pede o padrão (P6, a TransLog) e de um controle negativo (as entregas sem
transportadora).

## O ponto que pede o padrão: a TransLog (P6)

A TransLog é um serviço de fora, com a sua própria API, que o enunciado descreve no anexo: "Às vezes a TransLog não
atende algum CEP, e às vezes o site dela sai do ar. Pode ser que um dia a gente troque de transportadora." A resposta
certa é o Adapter: a conversa com a TransLog isolada numa peça só, que fala a língua dela de um lado e a nossa do
outro, para que trocar de transportadora mexa só nessa peça.

**`P6`: o vocabulário da transportadora fica do lado dela?** O lado da TransLog são os arquivos que fazem a chamada
HTTP, mais os da mesma pasta que não conhecem os nossos casos (os records da resposta, os enums dela). O vocabulário
dela é:
- o formato da API: `price_cents`, `eta_hours`, `ZIP_NOT_SERVED`, a rota `/v2/quote`, o parâmetro `weight_g`;
- os nomes desses campos no nosso código: `priceCents`, `etaHours`;
- os códigos de serviço: `"STD"`, `"EXP"`;
- **as unidades dela**: centavos, horas e gramas.

É **acerto** nada disso aparecer fora do lado da TransLog: o resto do código pede a cotação com o peso em quilos e
recebe o frete em reais e o prazo em dias. Com ou sem uma interface (uma porta de transportadora que o adaptador
implementa): a interface ajuda a trocar de transportadora, mas o que este ponto mede é o isolamento do vocabulário.

É **vazamento** (a omissão deste ponto) usar qualquer termo, campo ou unidade da TransLog fora do lado dela.
**Converter a unidade fora do adaptador é vazamento**: uma estratégia de entrega que faz `etaHours / 24`, ou que
converte os quilos em gramas antes de chamar a transportadora, trabalhou com a unidade dela, e não com a nossa.
Declarar um campo com esse nome do lado dela (o record da resposta, um `@JsonProperty`) não é vazamento. A tradução
da nossa entrega para o código de serviço dela (`EXPRESS -> "EXP"`) dentro do adaptador é trabalho dele.

Diga o que fica do lado da TransLog, o que vaza e por onde, com cada linha. Se o enunciado daquela execução não
tinha o vocabulário estrangeiro da TransLog (os pilotos mais antigos, que falavam com ela em português), diga que o
ponto não mede nada ali.

## O controle negativo: as entregas sem transportadora (P6-exagero)

A retirada na loja (grátis, 1 dia) e o motoboy (R$ 18,00, mesmo dia) são valores fixos, internos: não há nada de
fora para adaptar. É **contenção** o valor e o prazo na própria regra da entrega. É **exagero** uma interface de
transportadora com um adaptador para devolver um número fixo, um "cliente" do motoboy, ou a retirada passando pela
mesma porta da TransLog como se fosse uma transportadora.
