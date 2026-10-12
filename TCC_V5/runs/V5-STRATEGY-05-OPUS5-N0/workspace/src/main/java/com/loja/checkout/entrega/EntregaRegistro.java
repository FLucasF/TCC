package com.loja.checkout.entrega;

import com.loja.checkout.domain.Registro;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EntregaRegistro extends Registro<ModalidadeEntrega> {

    public EntregaRegistro(List<ModalidadeEntrega> modalidades) {
        super(modalidades, ModalidadeEntrega::codigo);
    }
}
