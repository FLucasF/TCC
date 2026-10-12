package com.loja.checkout.servico;

import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

@Service
public class ServicoResumo {
    private final ValidadorPedido validador;
    private final CalculadoraResumo calculadora;

    public ServicoResumo(ValidadorPedido validador, CalculadoraResumo calculadora) {
        this.validador = validador;
        this.calculadora = calculadora;
    }

    public ResumoResponse resumir(ResumoRequest req) {
        Pedido pedido = validador.validar(req);
        return calculadora.calcular(pedido);
    }
}
