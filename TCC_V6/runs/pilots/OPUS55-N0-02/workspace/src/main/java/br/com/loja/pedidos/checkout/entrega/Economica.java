package br.com.loja.pedidos.checkout.entrega;

import br.com.loja.pedidos.checkout.entrega.transportadora.ServicoTransporte;
import br.com.loja.pedidos.checkout.entrega.transportadora.Transportadora;
import org.springframework.stereotype.Component;

@Component
public class Economica extends EntregaPorTransportadora {

    public Economica(Transportadora transportadora) {
        super(transportadora, ServicoTransporte.ECONOMICO);
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }
}
