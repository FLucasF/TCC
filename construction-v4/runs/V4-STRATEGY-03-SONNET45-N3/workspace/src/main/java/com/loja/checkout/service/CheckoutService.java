package com.loja.checkout.service;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.*;
import com.loja.checkout.domain.modalidade.*;
import com.loja.checkout.domain.pagamento.*;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private static final Map<String, ModalidadeEntrega> MODALIDADES = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    private static final Map<String, Cupom> CUPONS = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    private static final Map<String, FormaPagamento> FORMAS_PAGAMENTO = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        validarPedido(pedido);

        NivelClube nivelClube = obterNivelClube(pedido.nivelClube());
        Regiao regiao = obterRegiao(pedido.regiao());
        ModalidadeEntrega modalidade = obterModalidade(pedido.modalidadeEntrega());
        FormaPagamento formaPagamento = obterFormaPagamento(pedido.formaPagamento());
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());

        if (!modalidade.verificarDisponibilidade(pesoTotal)) {
            throw new ValidacaoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = calcularSubtotal(pedido.itens());

        BigDecimal frete = nivelClube.isFreteGratis()
            ? BigDecimal.ZERO
            : modalidade.calcularFrete(pesoTotal);
        frete = frete.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal descontoCupom = new BigDecimal("0.00");
        if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
            Cupom cupom = obterCupom(pedido.cupom(), pedido.itens());
            if (!cupom.verificarAplicavel(subtotalProdutos)) {
                throw new ValidacaoException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete);
            descontoCupom = descontoCupom.setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal seguro = subtotalProdutos.multiply(regiao.getTaxa())
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        if (!formaPagamento.verificarDisponibilidade(totalPedido, parcelas)) {
            throw new ValidacaoException(parcelas != 1
                ? "PARCELAMENTO_INVALIDO"
                : "FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal totalFinal = formaPagamento.calcularTotalFinal(totalPedido, parcelas);
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);

        BigDecimal valorParcela = totalFinal.divide(
            new BigDecimal(parcelas),
            2,
            RoundingMode.HALF_EVEN
        );

        BigDecimal creditoProximaCompra = subtotalProdutos
            .multiply(nivelClube.getPorcentagemCredito())
            .setScale(2, RoundingMode.HALF_EVEN);

        boolean brinde = nivelClube.isElegibilidadeBrinde()
            && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoDias(),
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            creditoProximaCompra,
            brinde
        );
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new ValidacaoException("PEDIDO_INVALIDO");
        }

        for (ItemRequest item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacaoException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube obterNivelClube(String nivel) {
        try {
            return NivelClube.valueOf(nivel);
        } catch (Exception e) {
            throw new ValidacaoException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao obterRegiao(String regiaoStr) {
        try {
            return Regiao.valueOf(regiaoStr);
        } catch (Exception e) {
            throw new ValidacaoException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega obterModalidade(String modalidadeStr) {
        ModalidadeEntrega modalidade = MODALIDADES.get(modalidadeStr);
        if (modalidade == null) {
            throw new ValidacaoException("MODALIDADE_INVALIDA");
        }
        return modalidade;
    }

    private Cupom obterCupom(String cupomStr, List<ItemRequest> itens) {
        Cupom cupom = CUPONS.get(cupomStr);
        if (cupom == null) {
            throw new ValidacaoException("CUPOM_INVALIDO");
        }
        if (cupom instanceof Leve3Pague2 leve3Pague2) {
            leve3Pague2.setItens(itens);
        }
        return cupom;
    }

    private FormaPagamento obterFormaPagamento(String formaPagamentoStr) {
        FormaPagamento formaPagamento = FORMAS_PAGAMENTO.get(formaPagamentoStr);
        if (formaPagamento == null) {
            throw new ValidacaoException("FORMA_PAGAMENTO_INVALIDA");
        }
        return formaPagamento;
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemRequest> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static class ValidacaoException extends RuntimeException {
        public ValidacaoException(String codigo) {
            super(codigo);
        }
    }
}
