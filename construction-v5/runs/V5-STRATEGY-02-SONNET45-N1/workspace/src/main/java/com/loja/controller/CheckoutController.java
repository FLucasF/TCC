package com.loja.controller;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ErroResponse;
import com.loja.exception.*;
import com.loja.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (PedidoInvalidoException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("PEDIDO_INVALIDO"));
        } catch (NivelClubeInvalidoException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("NIVEL_CLUBE_INVALIDO"));
        } catch (RegiaoInvalidaException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("REGIAO_INVALIDA"));
        } catch (ModalidadeInvalidaException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("MODALIDADE_INVALIDA"));
        } catch (ModalidadeIndisponivelException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("MODALIDADE_INDISPONIVEL"));
        } catch (CupomInvalidoException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("CUPOM_INVALIDO"));
        } catch (CupomNaoAplicavelException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("CUPOM_NAO_APLICAVEL"));
        } catch (FormaPagamentoInvalidaException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("FORMA_PAGAMENTO_INVALIDA"));
        } catch (ParcelamentoInvalidoException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("PARCELAMENTO_INVALIDO"));
        } catch (FormaPagamentoIndisponivelException e) {
            return ResponseEntity.badRequest().body(new ErroResponse("FORMA_PAGAMENTO_INDISPONIVEL"));
        }
    }
}
