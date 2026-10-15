package com.loja.checkout.controller;

import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.CupomFactory;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.ModalidadeEntregaFactory;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.FormaPagamentoFactory;
import com.loja.checkout.model.dto.ErroResponse;
import com.loja.checkout.model.dto.ItemPedido;
import com.loja.checkout.model.dto.PedidoRequest;
import com.loja.checkout.model.dto.ResumoResponse;
import com.loja.checkout.service.CalculadoraResumo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadoraResumo;

    public CheckoutController(CalculadoraResumo calculadoraResumo) {
        this.calculadoraResumo = calculadoraResumo;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody PedidoRequest pedido) {

        String erro = validarPedido(pedido);
        if (erro != null) {
            return ResponseEntity.badRequest().body(new ErroResponse(erro));
        }

        if (pedido.nivelClube() == null || pedido.nivelClube().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErroResponse("NIVEL_CLUBE_INVALIDO"));
        }
        NivelClube nivelClube;
        try {
            nivelClube = NivelClube.valueOf(pedido.nivelClube());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErroResponse("NIVEL_CLUBE_INVALIDO"));
        }

        if (pedido.regiao() == null || pedido.regiao().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErroResponse("REGIAO_INVALIDA"));
        }
        Regiao regiao;
        try {
            regiao = Regiao.valueOf(pedido.regiao());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErroResponse("REGIAO_INVALIDA"));
        }

        if (pedido.modalidadeEntrega() == null || pedido.modalidadeEntrega().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErroResponse("MODALIDADE_INVALIDA"));
        }
        if (!ModalidadeEntregaFactory.existe(pedido.modalidadeEntrega())) {
            return ResponseEntity.badRequest().body(new ErroResponse("MODALIDADE_INVALIDA"));
        }
        ModalidadeEntrega modalidadeEntrega = ModalidadeEntregaFactory.criar(pedido.modalidadeEntrega());

        if (!modalidadeEntrega.estaDisponivel(pedido.itens())) {
            return ResponseEntity.badRequest().body(new ErroResponse("MODALIDADE_INDISPONIVEL"));
        }

        Cupom cupom = null;
        if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
            if (!CupomFactory.existe(pedido.cupom())) {
                return ResponseEntity.badRequest().body(new ErroResponse("CUPOM_INVALIDO"));
            }
            cupom = CupomFactory.criar(pedido.cupom());
        }

        if (pedido.formaPagamento() == null || pedido.formaPagamento().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErroResponse("FORMA_PAGAMENTO_INVALIDA"));
        }
        if (!FormaPagamentoFactory.existe(pedido.formaPagamento())) {
            return ResponseEntity.badRequest().body(new ErroResponse("FORMA_PAGAMENTO_INVALIDA"));
        }
        FormaPagamento formaPagamento = FormaPagamentoFactory.criar(pedido.formaPagamento());

        int numeroParcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        if (!formaPagamento.parcelamentoValido(numeroParcelas)) {
            return ResponseEntity.badRequest().body(new ErroResponse("PARCELAMENTO_INVALIDO"));
        }

        erro = validarCupomEFormaPagamento(pedido, modalidadeEntrega, cupom, formaPagamento, nivelClube, regiao);
        if (erro != null) {
            return ResponseEntity.badRequest().body(new ErroResponse(erro));
        }

        ResumoResponse resumo = calculadoraResumo.calcular(
            pedido,
            modalidadeEntrega,
            cupom,
            formaPagamento,
            nivelClube,
            regiao
        );

        return ResponseEntity.ok(resumo);
    }

    private String validarPedido(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            return "PEDIDO_INVALIDO";
        }

        for (ItemPedido item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                return "PEDIDO_INVALIDO";
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                return "PEDIDO_INVALIDO";
            }
            if (item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                return "PEDIDO_INVALIDO";
            }
        }

        return null;
    }

    private String validarCupomEFormaPagamento(
        PedidoRequest pedido,
        ModalidadeEntrega modalidadeEntrega,
        Cupom cupom,
        FormaPagamento formaPagamento,
        NivelClube nivelClube,
        Regiao regiao
    ) {
        com.loja.checkout.domain.DadosCalculo dadosTemp = new com.loja.checkout.domain.DadosCalculo();
        dadosTemp.setItens(pedido.itens());

        BigDecimal subtotalProdutos = pedido.itens().stream()
            .map(item -> item.precoUnitario()
                .multiply(new BigDecimal(item.quantidade()))
                .setScale(2, RoundingMode.HALF_EVEN))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        dadosTemp.setSubtotalProdutos(subtotalProdutos);

        BigDecimal frete = modalidadeEntrega.calcularFrete(pedido.itens());
        if (nivelClube.temFreteGratis()) {
            frete = new BigDecimal("0.00");
        }
        dadosTemp.setFrete(frete);

        if (cupom != null) {
            if (!cupom.podeAplicar(dadosTemp)) {
                return "CUPOM_NAO_APLICAVEL";
            }
            cupom.aplicarDesconto(dadosTemp);
        } else {
            dadosTemp.setDescontoCupom(new BigDecimal("0.00"));
        }

        BigDecimal seguro = subtotalProdutos
            .multiply(regiao.getTaxa())
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(dadosTemp.getDescontoCupom())
            .add(frete)
            .add(seguro);

        if (!formaPagamento.estaDisponivel(totalPedido)) {
            return "FORMA_PAGAMENTO_INDISPONIVEL";
        }

        return null;
    }
}
