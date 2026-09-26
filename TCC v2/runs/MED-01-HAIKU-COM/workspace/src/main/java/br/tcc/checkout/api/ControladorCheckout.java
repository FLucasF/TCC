package br.tcc.checkout.api;

import br.tcc.checkout.dominio.ServicoCheckout;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.dto.RespostaErro;
import br.tcc.checkout.dto.RespostaResumo;
import br.tcc.checkout.exception.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class ControladorCheckout {
    private final ServicoCheckout servico;

    public ControladorCheckout() {
        this.servico = ServicoCheckout.criar();
    }

    @PostMapping("/resumo")
    public ResponseEntity<RespostaResumo> resumo(@RequestBody RequisicaoResumo requisicao) {
        RespostaResumo resposta = servico.calcularResumo(requisicao);
        return ResponseEntity.ok(resposta);
    }

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<RespostaErro> tratarErroCheckout(ErroCheckout erro) {
        RespostaErro resposta = new RespostaErro(erro.getCodigo());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
}
