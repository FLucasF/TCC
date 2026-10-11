package com.loja.checkout.service;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinhoDto;
import com.loja.checkout.entrega.EntregaRegistry;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.PagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private static final BigDecimal BRINDE_MINIMO = new BigDecimal("500.00");

    private final EntregaRegistry entregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final PagamentoRegistry pagamentoRegistry;

    public CheckoutService(EntregaRegistry entregaRegistry,
                           CupomRegistry cupomRegistry,
                           PagamentoRegistry pagamentoRegistry) {
        this.entregaRegistry = entregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.pagamentoRegistry = pagamentoRegistry;
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        List<ItemCarrinhoDto> itens = req.itens();
        validarItens(itens);

        NivelClube nivelClube = resolverNivelClube(req.nivelClube());
        Regiao regiao = resolverRegiao(req.regiao());

        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal subtotalProdutos = calcularSubtotal(itens);

        ModalidadeEntrega entrega = resolverEntrega(req.modalidadeEntrega(), pesoTotal);

        BigDecimal frete = nivelClube.isFreteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : entrega.calcularFrete(pesoTotal);

        Cupom cupom = resolverCupom(req.cupom(), subtotalProdutos, itens);
        BigDecimal descontoCupom = cupom != null
                ? cupom.calcularDesconto(subtotalProdutos, frete, itens)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal seguro = subtotalProdutos
                .multiply(regiao.getTaxaSeguro())
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        int numeroParcelas = req.parcelas() != null ? req.parcelas() : 1;
        FormaPagamento pagamento = resolverPagamento(req.formaPagamento(), numeroParcelas, totalPedido);
        ResultadoPagamento resultado = pagamento.calcular(numeroParcelas, totalPedido);

        BigDecimal creditoProximaCompra = BigDecimal.ZERO.setScale(2);
        if (nivelClube.getTaxaCredito().compareTo(BigDecimal.ZERO) > 0) {
            creditoProximaCompra = subtotalProdutos
                    .multiply(nivelClube.getTaxaCredito())
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        boolean brinde = nivelClube.isBrindePossivel()
                && subtotalProdutos.compareTo(BRINDE_MINIMO) > 0;

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.getPrazoEntregaDias(),
                seguro,
                resultado.ajustePagamento(),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private void validarItens(List<ItemCarrinhoDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemCarrinhoDto item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube resolverNivelClube(String codigo) {
        if (codigo == null) throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        try {
            return NivelClube.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao resolverRegiao(String codigo) {
        if (codigo == null) throw new CheckoutException("REGIAO_INVALIDA");
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega resolverEntrega(String codigo, BigDecimal pesoTotal) {
        if (codigo == null) throw new CheckoutException("MODALIDADE_INVALIDA");
        ModalidadeEntrega entrega = entregaRegistry.buscar(codigo);
        if (entrega == null) throw new CheckoutException("MODALIDADE_INVALIDA");
        if (!entrega.aceita(pesoTotal)) throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        return entrega;
    }

    private Cupom resolverCupom(String codigo, BigDecimal subtotalProdutos, List<ItemCarrinhoDto> itens) {
        if (codigo == null || codigo.isBlank()) return null;
        Cupom cupom = cupomRegistry.buscar(codigo);
        if (cupom == null) throw new CheckoutException("CUPOM_INVALIDO");
        if (!cupom.isAplicavel(subtotalProdutos, itens)) throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        return cupom;
    }

    private FormaPagamento resolverPagamento(String codigo, int parcelas, BigDecimal totalPedido) {
        if (codigo == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        FormaPagamento pagamento = pagamentoRegistry.buscar(codigo);
        if (pagamento == null) throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        pagamento.validarParcelas(parcelas);
        pagamento.validarLimite(totalPedido);
        return pagamento;
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinhoDto> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinhoDto item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinhoDto> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinhoDto item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }
}
