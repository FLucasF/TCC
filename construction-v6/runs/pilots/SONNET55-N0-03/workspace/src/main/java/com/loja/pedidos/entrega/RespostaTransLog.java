package com.loja.pedidos.entrega;

public sealed interface RespostaTransLog {

    record Preco(int centavos, int horas) implements RespostaTransLog {
    }

    record CepNaoAtendido() implements RespostaTransLog {
    }

    record ForaDoAr() implements RespostaTransLog {
    }
}
