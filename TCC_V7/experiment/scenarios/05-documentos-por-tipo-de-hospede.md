## Comprovante para empresa e para estrangeiro

Hoje todo hóspede precisa de CPF, e todo mundo recebe o mesmo comprovante. Isso não serve mais: empresas reservam para os funcionários e precisam de uma fatura com o CNPJ, e estão chegando muitos estrangeiros, que não têm CPF e não entendem o comprovante em português.

A partir de agora, a reserva pode ser feita de três jeitos, e cada um recebe o seu documento:

| quem | o que informa | o documento |
|---|---|---|
| pessoa com CPF | o CPF, como hoje | o comprovante de hoje, igual |
| empresa | o CNPJ e a razão social da empresa (o nome e o e-mail continuam sendo os do hóspede) | a fatura de hospedagem |
| estrangeiro | o número do passaporte e o país | o *booking receipt*, em inglês |

O documento sai nos mesmos lugares de hoje: no e-mail de confirmação, no check-out e na segunda via. O texto do e-mail continua em português; só o documento dentro dele muda.

A **fatura de hospedagem** é como o comprovante, mas mostra a empresa no lugar do CPF e, no fim, o ISS retido de 5% sobre o total (arredondado para centavos, como sempre). Por exemplo:

```
Fatura de hospedagem nº 3
Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
Hóspede: Ana Souza
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
ISS retido (5%): R$ 38,99
```

O **booking receipt** tem as mesmas informações do comprovante, em inglês, com o dinheiro em reais escrito como `BRL 1,234.56` e as datas como `2027-03-10`. Quando tem desconto, entra a linha `Discount:` antes do total, como no comprovante. Por exemplo:

```
Booking receipt no. 4
Guest: John Smith (passport X1234567, United Kingdom)
Room: 201 (DELUXE)
Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
Nights: BRL 779.80
Total: BRL 779.80
```

(Com uma noite só, fica `1 night`.)

E já tem agência de turismo querendo fechar parceria com a gente; elas também vão precisar de um documento próprio.

**Para o pessoal do site:** na criação da reserva, o `guestCpf` deixa de ser obrigatório. Vem o `guestCpf`, ou o `companyCnpj` com o `companyName`, ou o `guestPassport` com o `guestCountry`: um desses jeitos, e só um; senão, `400 INVALID_REQUEST`. A reserva (o JSON) mostra esses campos, e os que não se aplicam vêm `null`. O documento (o JSON) mantém os campos de hoje (os de identificação que não se aplicam vêm `null`), ganha os quatro campos novos e ganha o campo `text`, com o texto do documento como ele sai no e-mail.
