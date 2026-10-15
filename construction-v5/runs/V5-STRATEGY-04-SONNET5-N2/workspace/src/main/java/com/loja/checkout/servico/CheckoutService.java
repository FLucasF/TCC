package com.loja.checkout.servico;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CuponsRegistry;
import com.loja.checkout.dinheiro.Dinheiro;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.MetodoEntrega;
import com.loja.checkout.entrega.MetodosEntregaRegistry;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.pedido.ItemBruto;
import com.loja.checkout.pedido.Pedido;
import com.loja.checkout.regiao.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CheckoutService {

    private final MetodosEntregaRegistry metodosEntregaRegistry;
    private final CuponsRegistry cuponsRegistry;
    private final FormasPagamentoRegistry formasPagamentoRegistry;

    public CheckoutService(MetodosEntregaRegistry metodosEntregaRegistry,
                            CuponsRegistry cuponsRegistry,
                            FormasPagamentoRegistry formasPagamentoRegistry) {
        this.metodosEntregaRegistry = metodosEntregaRegistry;
        this.cuponsRegistry = cuponsRegistry;
        this.formasPagamentoRegistry = formasPagamentoRegistry;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = Pedido.criar(paraItensBrutos(request));
        NivelClube nivel = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        MetodoEntrega metodoEntrega = validarModalidade(request.modalidadeEntrega());
        if (!metodoEntrega.disponivel(pedido)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        Cupom cupom = validarCupom(request.cupom());
        if (cupom != null && !cupom.aplicavel(pedido)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        FormaPagamento formaPagamento = validarFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = Dinheiro.arredondar(pedido.subtotalProdutos());
        BigDecimal frete = nivel.freteGratis() ? BigDecimal.ZERO.setScale(2) : metodoEntrega.custo(pedido);
        BigDecimal descontoCupom = cupom == null ? BigDecimal.ZERO.setScale(2) : cupom.desconto(pedido, frete);
        BigDecimal seguro = Dinheiro.arredondar(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = Dinheiro.arredondar(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        if (formaPagamento.indisponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal credito = Dinheiro.arredondar(subtotal.multiply(nivel.percentualCredito()));
        boolean brinde = nivel.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                metodoEntrega.prazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde);
    }

    private java.util.List<ItemBruto> paraItensBrutos(ResumoRequest request) {
        if (request.itens() == null) {
            return java.util.List.of();
        }
        return request.itens().stream()
                .map(this::paraItemBruto)
                .toList();
    }

    private ItemBruto paraItemBruto(ItemRequest item) {
        return new ItemBruto(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private NivelClube validarNivelClube(String nivelClube) {
        try {
            return NivelClube.valueOf(nivelClube);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
    }

    private Regiao validarRegiao(String regiao) {
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
    }

    private MetodoEntrega validarModalidade(String modalidadeEntrega) {
        return metodosEntregaRegistry.buscar(modalidadeEntrega)
                .orElseThrow(() -> new CheckoutException(CodigoErro.MODALIDADE_INVALIDA));
    }

    private Cupom validarCupom(String codigoCupom) {
        if (codigoCupom == null) {
            return null;
        }
        return cuponsRegistry.buscar(codigoCupom)
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
    }

    private FormaPagamento validarFormaPagamento(String formaPagamento) {
        return formasPagamentoRegistry.buscar(formaPagamento)
                .orElseThrow(() -> new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
    }
}
