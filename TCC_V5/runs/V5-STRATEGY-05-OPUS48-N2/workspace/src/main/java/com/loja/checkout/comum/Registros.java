package com.loja.checkout.comum;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reúne os registros de cada dimensão. O Spring injeta todos os componentes de
 * cada tipo, então cada novo caso criado entra aqui sozinho.
 */
@Component
public class Registros {

    private final Registro<ModalidadeEntrega> entregas;
    private final Registro<Cupom> cupons;
    private final Registro<NivelClube> niveis;
    private final Registro<FormaPagamento> pagamentos;

    public Registros(List<ModalidadeEntrega> entregas,
                     List<Cupom> cupons,
                     List<NivelClube> niveis,
                     List<FormaPagamento> pagamentos) {
        this.entregas = new Registro<>(entregas);
        this.cupons = new Registro<>(cupons);
        this.niveis = new Registro<>(niveis);
        this.pagamentos = new Registro<>(pagamentos);
    }

    public Registro<ModalidadeEntrega> entregas() {
        return entregas;
    }

    public Registro<Cupom> cupons() {
        return cupons;
    }

    public Registro<NivelClube> niveis() {
        return niveis;
    }

    public Registro<FormaPagamento> pagamentos() {
        return pagamentos;
    }
}
