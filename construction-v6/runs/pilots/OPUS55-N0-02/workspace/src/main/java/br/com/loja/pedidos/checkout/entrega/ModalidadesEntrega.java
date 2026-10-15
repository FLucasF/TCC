package br.com.loja.pedidos.checkout.entrega;

import br.com.loja.pedidos.comum.Registro;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ModalidadesEntrega {

    private final Registro<ModalidadeEntrega> registro;

    public ModalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        this.registro = new Registro<>(modalidades, ModalidadeEntrega::codigo);
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return registro.buscar(codigo);
    }
}
