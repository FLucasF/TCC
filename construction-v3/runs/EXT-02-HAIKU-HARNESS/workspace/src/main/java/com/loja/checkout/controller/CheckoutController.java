package com.loja.checkout.controller;

import com.loja.checkout.dto.RequisicaoResumoDTO;
import com.loja.checkout.dto.RespostaResumoDTO;
import com.loja.checkout.excecao.ErroCheckout;
import com.loja.checkout.servico.ServicoCheckout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private ServicoCheckout servicoCheckout;

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody RequisicaoResumoDTO requisicao) {
        try {
            RespostaResumoDTO resposta = servicoCheckout.calcularResumo(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (ErroCheckout e) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getCodigo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
        }
    }
}
