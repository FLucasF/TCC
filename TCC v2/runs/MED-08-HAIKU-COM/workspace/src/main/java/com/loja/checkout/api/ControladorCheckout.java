package com.loja.checkout.api;

import com.loja.checkout.exceptions.ErroCheckout;
import com.loja.checkout.service.ServicoCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class ControladorCheckout {
    private final ServicoCheckout servicoCheckout = new ServicoCheckout();

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody RequisicaoCheckout requisicao) {
        try {
            RespostaCheckout resposta = servicoCheckout.calcularResumo(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (ErroCheckout erro) {
            RespostaErro respostaErro = new RespostaErro(erro.getCodigo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respostaErro);
        }
    }
}
