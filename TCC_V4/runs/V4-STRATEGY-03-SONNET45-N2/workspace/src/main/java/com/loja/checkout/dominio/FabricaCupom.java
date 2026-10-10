package com.loja.checkout.dominio;

import com.loja.checkout.dominio.cupom.*;
import com.loja.checkout.dto.ItemCarrinho;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class FabricaCupom {

    private final Map<String, Function<List<ItemCarrinho>, AplicadorCupom>> fabricas;

    public FabricaCupom() {
        this.fabricas = Map.of(
            "BEMVINDO10", itens -> new CupomBemvindo10(),
            "MENOS50", itens -> new CupomMenos50(),
            "FRETEGRATIS", itens -> new CupomFreteGratis(),
            "LEVE3PAGUE2", itens -> new CupomLeve3Pague2(itens)
        );
    }

    public AplicadorCupom obter(String codigo, List<ItemCarrinho> itens) {
        Function<List<ItemCarrinho>, AplicadorCupom> fabrica = fabricas.get(codigo);
        return fabrica != null ? fabrica.apply(itens) : null;
    }

    public boolean existe(String codigo) {
        return fabricas.containsKey(codigo);
    }
}
