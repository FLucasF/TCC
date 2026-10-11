package com.loja.checkout.entrega;

import com.loja.checkout.comum.RegistroDeCodigos;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegistroEntregas extends RegistroDeCodigos<ModalidadeEntrega> {

    public RegistroEntregas(List<ModalidadeEntrega> modalidades) {
        super(modalidades);
    }
}
