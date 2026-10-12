package com.loja.checkout.cupom;

import com.loja.checkout.domain.Registro;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CupomRegistro extends Registro<Cupom> {

    public CupomRegistro(List<Cupom> cupons) {
        super(cupons, Cupom::codigo);
    }
}
