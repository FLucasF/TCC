package br.com.loja.checkout.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.checkout.aplicacao.PedidoSolicitado;
import br.com.loja.checkout.aplicacao.ResumoService;

@RestController
@RequestMapping("/checkout/resumo")
public class ResumoController {

    private final ResumoService resumoService;

    public ResumoController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoResponse calcular(@RequestBody PedidoSolicitado pedido) {
        return ResumoResponse.de(resumoService.calcular(pedido));
    }
}
