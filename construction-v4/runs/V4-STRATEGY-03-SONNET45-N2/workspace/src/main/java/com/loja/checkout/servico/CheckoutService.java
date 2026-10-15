package com.loja.checkout.servico;

import com.loja.checkout.dominio.*;
import com.loja.checkout.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CheckoutService {

    private final FabricaFrete fabricaFrete = new FabricaFrete();
    private final FabricaCupom fabricaCupom = new FabricaCupom();
    private final FabricaPagamento fabricaPagamento = new FabricaPagamento();
    private final FabricaClube fabricaClube = new FabricaClube();

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        validarPedido(pedido);

        NivelClube nivelClube = validarNivelClube(pedido.nivelClube());
        Regiao regiao = validarRegiao(pedido.regiao());
        ModalidadeEntrega modalidadeEntrega = validarModalidadeEntrega(pedido.modalidadeEntrega());

        BigDecimal subtotalProdutos = calcularSubtotalProdutos(pedido.itens());
        BigDecimal pesoTotal = calcularPesoTotal(pedido.itens());

        CalculadoraFrete calculadoraFrete = fabricaFrete.obter(modalidadeEntrega);
        if (!calculadoraFrete.aceitaPedido(pesoTotal)) {
            throw new ErroCheckoutException("MODALIDADE_INDISPONIVEL");
        }

        AplicadorCupom aplicadorCupom = validarCupom(pedido.cupom(), pedido.itens(), subtotalProdutos);
        FormaPagamento formaPagamento = validarFormaPagamento(pedido.formaPagamento());
        int parcelas = validarParcelas(pedido.parcelas());

        CalculadoraPagamento calculadoraPagamento = fabricaPagamento.obter(formaPagamento);
        if (!calculadoraPagamento.aceitaParcelas(parcelas)) {
            throw new ErroCheckoutException("PARCELAMENTO_INVALIDO");
        }

        BeneficiosClube beneficiosClube = fabricaClube.obter(nivelClube);

        BigDecimal frete = beneficiosClube.temFreteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : calculadoraFrete.calcular(pesoTotal);
        int prazo = calculadoraFrete.prazoEmDias();

        BigDecimal descontoCupom = aplicadorCupom != null
                ? aplicadorCupom.calcularDesconto(subtotalProdutos, frete)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        if (!calculadoraPagamento.aceitaTotal(totalPedido)) {
            throw new ErroCheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal totalFinal = calculadoraPagamento.calcularTotalFinal(totalPedido, parcelas);
        BigDecimal ajustePagamento = calculadoraPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal valorParcela = calculadoraPagamento.calcularValorParcela(totalFinal, parcelas);

        BigDecimal credito = beneficiosClube.calcularCredito(subtotalProdutos);
        boolean brinde = beneficiosClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazo,
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            credito,
            brinde
        );
    }

    private void validarPedido(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new ErroCheckoutException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : pedido.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ErroCheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube validarNivelClube(String nivelStr) {
        if (nivelStr == null) {
            throw new ErroCheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(nivelStr);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao validarRegiao(String regiaoStr) {
        if (regiaoStr == null) {
            throw new ErroCheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(regiaoStr);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega validarModalidadeEntrega(String modalidadeStr) {
        if (modalidadeStr == null) {
            throw new ErroCheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(modalidadeStr);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckoutException("MODALIDADE_INVALIDA");
        }
    }

    private AplicadorCupom validarCupom(String cupom, java.util.List<ItemCarrinho> itens, BigDecimal subtotalProdutos) {
        if (cupom == null || cupom.isEmpty()) {
            return null;
        }

        if (!fabricaCupom.existe(cupom)) {
            throw new ErroCheckoutException("CUPOM_INVALIDO");
        }

        AplicadorCupom aplicador = fabricaCupom.obter(cupom, itens);
        if (!aplicador.podeAplicar(subtotalProdutos)) {
            throw new ErroCheckoutException("CUPOM_NAO_APLICAVEL");
        }

        return aplicador;
    }

    private FormaPagamento validarFormaPagamento(String formaPagamentoStr) {
        if (formaPagamentoStr == null) {
            throw new ErroCheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(formaPagamentoStr);
        } catch (IllegalArgumentException e) {
            throw new ErroCheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private int validarParcelas(Integer parcelas) {
        return parcelas != null ? parcelas : 1;
    }

    private BigDecimal calcularSubtotalProdutos(java.util.List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal valorItem = item.precoUnitario()
                    .multiply(BigDecimal.valueOf(item.quantidade()));
            subtotal = subtotal.add(valorItem);
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(java.util.List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal pesoItem = item.pesoKg()
                    .multiply(BigDecimal.valueOf(item.quantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }
        return pesoTotal;
    }
}
