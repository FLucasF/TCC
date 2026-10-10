package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.dto.*;
import com.loja.checkout.factory.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.loja.checkout.util.Arredondamento.arredondar;

@Service
public class CheckoutService {

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        validarPedido(pedido);

        NivelClube nivelClube = obterNivelClube(pedido.nivelClube());
        Regiao regiao = obterRegiao(pedido.regiao());
        ModalidadeEntrega modalidade = obterModalidadeEntrega(pedido.modalidadeEntrega());

        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());
        validarModalidadeDisponivel(modalidade, pesoTotal);

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.itens());

        Cupom cupom = obterCupom(pedido.cupom());
        validarCupomAplicavel(cupom, subtotalProdutos);

        BigDecimal frete = nivelClube.temFreteGratis()
            ? new BigDecimal("0.00")
            : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = cupom != null
            ? cupom.calcularDesconto(subtotalProdutos, frete, pedido.itens())
            : new BigDecimal("0.00");

        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        BigDecimal totalPedido = subtotalProdutos
            .subtract(descontoCupom)
            .add(frete)
            .add(seguro);

        FormaPagamento formaPagamento = obterFormaPagamento(pedido.formaPagamento());
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);
        validarFormaPagamentoDisponivel(formaPagamento, totalPedido);

        BigDecimal totalFinal = formaPagamento.calcularValorFinal(totalPedido, parcelas);
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);

        BigDecimal valorParcela = arredondar(totalFinal.divide(
            BigDecimal.valueOf(parcelas),
            10,
            RoundingMode.HALF_EVEN
        ));

        BigDecimal creditoProximaCompra = nivelClube.calcularCredito(subtotalProdutos);
        Boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.obterPrazoDias(),
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
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube obterNivelClube(String codigo) {
        if (codigo == null || !NivelClubeFactory.existe(codigo)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        return NivelClubeFactory.obter(codigo);
    }

    private Regiao obterRegiao(String codigo) {
        if (codigo == null || !RegiaoFactory.existe(codigo)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        return RegiaoFactory.obter(codigo);
    }

    private ModalidadeEntrega obterModalidadeEntrega(String codigo) {
        if (codigo == null || !ModalidadeFactory.existe(codigo)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        return ModalidadeFactory.obter(codigo);
    }

    private void validarModalidadeDisponivel(ModalidadeEntrega modalidade, BigDecimal pesoTotal) {
        if (!modalidade.aceita(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }

    private Cupom obterCupom(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }

        if (!CupomFactory.existe(codigo)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }

        return CupomFactory.obter(codigo);
    }

    private void validarCupomAplicavel(Cupom cupom, BigDecimal subtotalProdutos) {
        if (cupom != null && !cupom.aplicavel(subtotalProdutos)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null || !FormaPagamentoFactory.existe(codigo)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        return FormaPagamentoFactory.obter(codigo);
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarFormaPagamentoDisponivel(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.aceita(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemCarrinho> itens) {
        BigDecimal subtotal = new BigDecimal("0.00");
        for (ItemCarrinho item : itens) {
            BigDecimal totalItem = item.precoUnitario()
                .multiply(BigDecimal.valueOf(item.quantidade()));
            subtotal = subtotal.add(totalItem);
        }
        return arredondar(subtotal);
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = new BigDecimal("0.00");
        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.pesoKg()
                .multiply(BigDecimal.valueOf(item.quantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }
}
