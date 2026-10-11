package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {
    @Override
    public String getCodigo() { return "MOTOBOY"; }

    @Override
    public boolean aceita(double pesoTotalKg) { return pesoTotalKg <= 5.0; }

    @Override
    public ResultadoFrete calcular(double pesoTotalKg) {
        return new ResultadoFrete(18.00, 0);
    }
}
