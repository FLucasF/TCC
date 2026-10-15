package com.loja.checkout.servico;

import com.loja.checkout.web.CheckoutResposta;

public sealed interface ResultadoCheckout {
    record Sucesso(CheckoutResposta resposta) implements ResultadoCheckout {}
    record Falha(CheckoutErro codigo) implements ResultadoCheckout {}
}
