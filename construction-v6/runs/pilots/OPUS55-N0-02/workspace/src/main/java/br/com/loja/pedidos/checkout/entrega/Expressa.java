package br.com.loja.pedidos.checkout.entrega;

import br.com.loja.pedidos.checkout.entrega.transportadora.ServicoTransporte;
import br.com.loja.pedidos.checkout.entrega.transportadora.Transportadora;
import org.springframework.stereotype.Component;

@Component
public class Expressa extends EntregaPorTransportadora {

    public Expressa(Transportadora transportadora) {
        super(transportadora, ServicoTransporte.EXPRESSO);
    }

    @Override
    public String codigo() {
        return "EXPRESSA";
    }
}
