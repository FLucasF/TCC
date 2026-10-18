# As instruções do pattern: Factory Method

Este avaliador cuida de um enunciado, que você reconhece pela parte do fim do `enunciado.md`: o do **documento
por tipo de hóspede** (cenário 05, que pede o pattern).

## O que o enunciado descreve

Na parte comum, o comprovante sai em três lugares: no e-mail de confirmação, no check-out e na segunda via. A
reserva e o pedido de reserva são objetos de dados.

## Cenário 05: o documento por tipo de hóspede (pede o pattern)

O enunciado traz três tipos de hóspede (pessoa com CPF, empresa, estrangeiro), cada um com o seu documento (o
comprovante, a fatura de hospedagem, o *booking receipt* em inglês), que sai nos mesmos três lugares, e diz que as
agências de turismo vão precisar de um documento próprio. A resposta certa é concentrar a criação: a escolha de qual
documento criar mora num lugar só, e os três lugares pedem o documento a esse lugar.

É **acerto** a forma clássica do Factory Method (um criador com o método de fábrica sobrescrito por tipo) e também
uma fábrica concentrada (uma classe ou um método que escolhe o documento pelo tipo de hóspede) ou um método
polimórfico no próprio tipo de hóspede (`guest.issueDocument(...)`). Conte nas dúvidas qual das formas foi usada.

É **omissão** quando o `if` ou o `switch` sobre o tipo de hóspede aparece repetido nos três lugares (mesmo que o
texto de cada documento esteja em funções auxiliares separadas: o que importa é onde se decide qual criar).

É **malfeito** quando a fábrica existe, mas um dos três lugares cria o documento à mão, ou quando a fábrica devolve
sempre o mesmo documento e a escolha acontece depois, fora dela.

O comportamento pedido: a reserva aceita exatamente um jeito de identificar o hóspede (o CPF; ou o CNPJ com a razão
social; ou o passaporte com o país) e recusa nenhum, dois ou um incompleto; a fatura mostra a empresa no lugar do
CPF e o ISS retido de 5% sobre o total (779,80 dá R$ 38,99); o *booking receipt* é em inglês, com `BRL 1,234.56`,
datas `2027-03-10` e "1 night" ou "N nights"; o documento sai igual no e-mail de confirmação, no check-out e na
segunda via, e o JSON dele tem o campo `text`; a pessoa com CPF recebe o comprovante da parte comum.
