package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.service.Arredondamento;
import org.springframework.stereotype.Component;

@Component
public class CupomLeve3Pague2 implements Cupom {
    @Override
    public String getCodigo() { return "LEVE3PAGUE2"; }

    @Override
    public boolean aplicavel(ContextoCupom ctx) { return true; }

    @Override
    public ResultadoCupom calcular(ContextoCupom ctx) {
        double desconto = 0;
        for (ItemRequest item : ctx.itens()) {
            int qtd = item.quantidade();
            int gratis = qtd / 3;
            desconto += gratis * item.precoUnitario();
        }
        return new ResultadoCupom(Arredondamento.arredondar(desconto), false);
    }
}
