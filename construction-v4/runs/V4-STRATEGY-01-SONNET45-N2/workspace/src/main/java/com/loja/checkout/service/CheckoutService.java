package com.loja.checkout.service;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.cupom.Cupom;
import com.loja.checkout.domain.cupom.ResultadoCupom;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.ResultadoEntrega;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.ResultadoPagamento;
import com.loja.checkout.domain.regiao.Regiao;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.exception.CheckoutException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.util.Moeda.arredondar;

@Service
public class CheckoutService {

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        validarPedido(request.itens());

        NivelClube nivelClube = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        ModalidadeEntrega modalidade = validarModalidadeEntrega(request.modalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request.itens());
        BigDecimal pesoTotal = calcularPesoTotal(request.itens());

        validarModalidadeDisponivel(modalidade, pesoTotal);

        ResultadoEntrega entrega = modalidade.calcular(pesoTotal);
        BigDecimal frete = nivelClube.isFreteGratis() ? BigDecimal.ZERO.setScale(2) : entrega.frete();

        Cupom cupom = validarCupom(request.cupom());
        ResultadoCupom resultadoCupom = aplicarCupom(cupom, subtotalProdutos, request.itens(), frete);

        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);
        BigDecimal totalPedido = arredondar(
            subtotalProdutos.subtract(resultadoCupom.desconto()).add(frete).add(seguro)
        );

        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        ResultadoPagamento pagamento = aplicarPagamento(formaPagamento, totalPedido, request.parcelas());

        BigDecimal credito = calcularCredito(subtotalProdutos, nivelClube);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new CheckoutResponse(
            subtotalProdutos,
            resultadoCupom.desconto(),
            frete,
            entrega.prazoEntregaDias(),
            seguro,
            pagamento.ajuste(),
            pagamento.totalFinal(),
            pagamento.parcelas(),
            pagamento.valorParcela(),
            credito,
            brinde
        );
    }

    private void validarPedido(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemCarrinho item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarNivelClube(String nivel) {
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivel);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String nomeRegiao) {
        if (nomeRegiao == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(nomeRegiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidadeEntrega(String modalidade) {
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceitaPedido(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom validarCupom(String codigoCupom) {
        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigoCupom);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }

    private FormaPagamento validarFormaPagamento(String forma) {
        if (forma == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(forma);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private BigDecimal calcularSubtotalProdutos(List<ItemCarrinho> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return arredondar(total);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            total = total.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return total;
    }

    private ResultadoCupom aplicarCupom(Cupom cupom, BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
        if (cupom == null) {
            return new ResultadoCupom(BigDecimal.ZERO.setScale(2));
        }
        if (!cupom.aplicavel(subtotalProdutos, itens, frete)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.calcular(subtotalProdutos, itens, frete);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        return arredondar(subtotalProdutos.multiply(regiao.getPorcentagemSeguro()));
    }

    private ResultadoPagamento aplicarPagamento(FormaPagamento forma, BigDecimal totalPedido, Integer parcelas) {
        if (!forma.aceitaPedido(totalPedido, parcelas)) {
            if (parcelas != null && parcelas != 1) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        return forma.calcular(totalPedido, parcelas);
    }

    private BigDecimal calcularCredito(BigDecimal subtotalProdutos, NivelClube nivel) {
        return arredondar(subtotalProdutos.multiply(nivel.getPorcentagemCredito()));
    }
}
