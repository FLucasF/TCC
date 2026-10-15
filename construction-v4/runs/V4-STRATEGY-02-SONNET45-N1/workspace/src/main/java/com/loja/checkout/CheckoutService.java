package com.loja.checkout;

import com.loja.checkout.calculadora.CalculadoraResumo;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.validacao.ValidadorPedido;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
    private final ValidadorPedido validador;
    private final CalculadoraResumo calculadora;

    public CheckoutService(ValidadorPedido validador, CalculadoraResumo calculadora) {
        this.validador = validador;
        this.calculadora = calculadora;
    }

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validador.validar(request);
        return calculadora.calcular(request);
    }
}
