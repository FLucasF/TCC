package br.com.loja.checkout.web;

import br.com.loja.checkout.pedido.PedidoBruto;
import br.com.loja.checkout.resumo.CalculadoraResumo;
import br.com.loja.checkout.resumo.ResumoCompra;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResumoCompra resumo(@RequestBody PedidoBruto pedido) {
        return calculadora.calcular(pedido);
    }
}
