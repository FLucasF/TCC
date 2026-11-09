Oi! Sou o dono da Pousada Maré Alta, uma pousada pequena no litoral. O sistema de reservas que a gente usa está nesta pasta. Ele funciona bem, e quem fez deixou um README explicando como ele é.

Preciso de uma mudança, que explico abaixo.

A recepção pediu para ver o nome do hóspede no relatório de ocupação. Pode colocar uma coluna `guest` logo depois da coluna `reservation`, com o nome como está na reserva. Se o nome tiver vírgula, troca por um espaço, para não bagunçar o CSV. O resto do relatório continua igual.

Talvez um dia a recepção peça o telefone do hóspede também, mas a gente ainda nem guarda telefone.

Tudo o que funciona hoje tem que continuar funcionando, menos o que eu pedi para mudar. Quando terminar, o `mvn verify` precisa passar. Não precisa me perguntar nada: se alguma coisa não estiver clara, decida do jeito que achar mais sensato e siga em frente.
