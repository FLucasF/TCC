Hoje todo hóspede precisa ter CPF, e todo mundo recebe o mesmo comprovante. Isso não está servindo mais. Tem empresa reservando quarto para funcionário e pedindo fatura com CNPJ, e estão chegando muitos estrangeiros, que não têm CPF e não entendem o comprovante em português.

Então a reserva vai poder ser feita de três jeitos. A pessoa com CPF continua como hoje e recebe o mesmo comprovante de hoje. A empresa informa o CNPJ e a razão social dela (o nome e o e-mail continuam sendo os do hóspede que vai ficar) e recebe uma fatura de hospedagem. E o estrangeiro informa o número do passaporte e o país, e recebe um booking receipt, que é o comprovante em inglês.

O documento sai nos mesmos lugares de hoje: no e-mail de confirmação, no check-out e na segunda via. O e-mail continua em português; muda só o documento que vai dentro dele.

A fatura é parecida com o comprovante, mas mostra a empresa no lugar do CPF e, no fim, o ISS retido, que é 5% do total, arredondado para centavos como sempre. Ficaria mais ou menos assim:

Fatura de hospedagem nº 3
Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
Hóspede: Ana Souza
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
ISS retido (5%): R$ 38,99

O booking receipt tem as mesmas informações do comprovante, só que em inglês, com o dinheiro escrito do jeito deles (BRL 1,234.56) e as datas no formato 2027-03-10. Quando tem desconto, entra a linha "Discount:" antes do total, como no comprovante, e quando é uma noite só fica "1 night". Assim:

Booking receipt no. 4
Guest: John Smith (passport X1234567, United Kingdom)
Room: 201 (DELUXE)
Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
Nights: BRL 779.80
Total: BRL 779.80

E já tem agência de turismo querendo fechar parceria com a gente, e elas também vão precisar de um documento próprio.

O pessoal do site pediu para eu passar os detalhes deles. Na criação da reserva, o `guestCpf` deixa de ser obrigatório: vem ele, ou o `companyCnpj` junto com o `companyName`, ou o `guestPassport` junto com o `guestCountry`. Tem que ser um desses jeitos, e só um; se não for, a resposta é 400 com INVALID_REQUEST. Quando o site consulta a reserva, ela mostra esses campos, e os que não se aplicam vêm null. E o documento continua com os campos de hoje (os de identificação que não se aplicam vêm null), mais esses quatro novos e um campo `text`, com o texto do documento do jeito que ele sai no e-mail.
