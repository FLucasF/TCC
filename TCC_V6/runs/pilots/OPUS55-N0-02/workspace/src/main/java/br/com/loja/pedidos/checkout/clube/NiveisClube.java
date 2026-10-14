package br.com.loja.pedidos.checkout.clube;

import br.com.loja.pedidos.comum.Registro;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NiveisClube {

    private final Registro<NivelClube> registro;

    public NiveisClube(List<NivelClube> niveis) {
        this.registro = new Registro<>(niveis, NivelClube::codigo);
    }

    public Optional<NivelClube> buscar(String codigo) {
        return registro.buscar(codigo);
    }
}
