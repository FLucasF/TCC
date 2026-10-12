package com.loja.checkout.web;

import com.loja.checkout.domain.CalculadoraResumo;
import com.loja.checkout.domain.ErroCalculo;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Resumo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public Resumo resumo(@RequestBody CheckoutRequest req) {
        return calculadora.calcular(new CalculadoraResumo.Entrada(
                toItens(req.itens()),
                req.modalidadeEntrega(),
                req.cupom(),
                req.formaPagamento(),
                req.parcelas(),
                req.nivelClube(),
                req.regiao()
        ));
    }

    @ExceptionHandler(ErroCalculo.class)
    public ResponseEntity<Map<String, String>> erro(ErroCalculo e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.codigo()));
    }

    private static List<Item> toItens(List<CheckoutRequest.ItemRequest> brutos) {
        if (brutos == null) {
            return null;
        }
        List<Item> itens = new ArrayList<>(brutos.size());
        for (CheckoutRequest.ItemRequest i : brutos) {
            if (i == null) {
                itens.add(null);
                continue;
            }
            itens.add(new Item(
                    i.nome(),
                    i.precoUnitario(),
                    i.quantidade() == null ? 0 : i.quantidade(),
                    i.pesoKg()
            ));
        }
        return itens;
    }
}
