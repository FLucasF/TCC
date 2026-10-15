package com.loja.checkout.cupom;

import com.loja.checkout.comum.RegistroDeCodigos;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegistroCupons extends RegistroDeCodigos<Cupom> {

    public RegistroCupons(List<Cupom> cupons) {
        super(cupons);
    }
}
