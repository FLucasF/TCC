# As instruções do pattern: Factory Method (cenário 05)

## O ponto de partida, na construção (o que a parte comum do enunciado descreve)

O comprovante sai em três lugares: no e-mail de confirmação, no check-out e na segunda via. A reserva e o pedido de
reserva são objetos de dados.

## O ponto de partida, na manutenção (o que havia na base)

O comprovante (`ReservationReceipt`) é montado com `new` em três lugares do `ReservationService`: `confirm` (que o
manda por e-mail), `checkOut` e `receiptCopy` (a segunda via). Havia um único tipo de comprovante. A reserva
(`Reservation`) e o pedido de reserva (`NewReservation`) são objetos de dados criados direto. A pessoa com CPF deve
continuar recebendo o comprovante da base.

## O que o cenário pede

O texto traz três tipos de hóspede (pessoa com CPF, empresa, estrangeiro), cada um com o seu documento (o comprovante,
a fatura de hospedagem, o *booking receipt* em inglês), que sai nos mesmos três lugares (o e-mail de confirmação, o
check-out e a segunda via), e diz que as agências de turismo vão precisar de um documento próprio. A resposta certa é
concentrar a criação: a escolha de qual documento criar mora num lugar só, e os três lugares pedem o documento a esse
lugar.

É **acerto** a forma clássica do Factory Method (um criador com o método de fábrica sobrescrito por tipo) e também uma
fábrica concentrada (uma classe ou um método que escolhe o documento pelo tipo de hóspede) ou um método polimórfico no
próprio tipo de hóspede (`guest.issueDocument(...)`). Conte nas dúvidas qual das formas foi usada.

É **omissão** quando o `if` ou o `switch` sobre o tipo de hóspede aparece repetido nos três lugares (mesmo que o texto
de cada documento esteja em funções auxiliares separadas: o que importa é onde se decide qual criar).

É **malfeito** quando a fábrica existe, mas um dos três lugares cria o documento à mão, ou quando a fábrica devolve
sempre o mesmo documento e a escolha acontece depois, fora dela.

O comportamento pedido: a reserva aceita exatamente um jeito de identificar o hóspede (o CPF; ou o CNPJ com a razão
social; ou o passaporte com o país) e recusa nenhum, dois ou um incompleto; a fatura mostra a empresa no lugar do CPF
e o ISS retido de 5% sobre o total (779,80 dá R$ 38,99); o *booking receipt* é em inglês, com `BRL 1,234.56`, datas
`2027-03-10` e "1 night" ou "N nights"; o documento sai igual no e-mail de confirmação, no check-out e na segunda
via, e o JSON dele tem o campo `text`; a pessoa com CPF recebe o comprovante como o ponto de partida diz.
