package com.loja.checkout.web;

import com.loja.checkout.calculo.ErroPedido;
import com.loja.checkout.calculo.ServicoResumo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ServicoResumo servico;

    public CheckoutController(ServicoResumo servico) {
        this.servico = servico;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest req) {
        return servico.calcular(req);
    }

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<ErroResponse> trataErro(ErroPedido e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.codigo()));
    }
}
