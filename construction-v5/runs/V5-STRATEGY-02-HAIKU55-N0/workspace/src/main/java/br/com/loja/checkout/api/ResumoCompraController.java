package br.com.loja.checkout.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.checkout.aplicacao.ResumoCompraService;

@RestController
public class ResumoCompraController {

    private final ResumoCompraService servico;

    public ResumoCompraController(ResumoCompraService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompraResponse resumo(@RequestBody ResumoCompraRequest pedido) {
        return servico.calcular(pedido);
    }
}
