## Regras para as mensagens que a pousada manda

Tivemos três problemas com os e-mails, e cada um pede uma regra.

1. **E-mail digitado errado.** O hóspede escreve "ana@gmail" ou "ana.gmail.com", e a mensagem vai para lugar nenhum. A partir de agora, a mensagem para um endereço inválido não é enviada: ela aparece na lista de mensagens como bloqueada, para a recepção ligar para o hóspede. Um endereço é válido quando tem um único @, alguma coisa antes dele e, depois dele, um nome com pelo menos um ponto no meio (como ana@gmail.com).

2. **Arquivo.** O contador quer cópia de tudo o que a pousada manda. Toda mensagem enviada gera uma cópia para `arquivo@pousadamarealta.com.br`, com o mesmo texto e com o assunto começando com "[Cópia] ". Mensagem bloqueada não gera cópia.

3. **Testes do site.** Quando o pessoal do site testa o sistema, os e-mails estão indo para hóspedes de verdade! Quando o sistema estiver rodando em modo de teste, nenhuma mensagem pode sair para o endereço real: ela vai para `testes@pousadamarealta.com.br`, com o assunto começando com "[TESTE] " e com uma primeira linha a mais no texto: "Destinatário original: " e o endereço. No modo de teste não se faz a cópia para o arquivo, e o endereço inválido continua sendo bloqueado (a mensagem bloqueada fica com o endereço original).

E não acaba aí: já pediram para não mandar mensagem de madrugada e para ter um limite de mensagens por hóspede por dia. Cada regra dessas pode valer sempre, só no modo de teste ou só fora dele.

**Para o pessoal do site:** cada mensagem da lista (`GET /messages`) ganha o campo `status`, que é `SENT` ou `BLOCKED`. A cópia para o arquivo aparece na lista como outra mensagem, logo depois da original. O modo de teste é ligado pela variável de ambiente `HOTEL_TEST_MODE=true`; sem ela, o sistema funciona normalmente.
