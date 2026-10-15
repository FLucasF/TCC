package com.loja.checkout.cupom;

import com.loja.checkout.comum.RegistroPorChave;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CupomRegistro {

    private final RegistroPorChave<String, Cupom> registro;

    public CupomRegistro(List<Cupom> cupons) {
        this.registro = new RegistroPorChave<>(cupons, Cupom::codigo);
    }

    public Optional<Cupom> buscar(String codigo) {
        return registro.buscar(codigo);
    }
}
