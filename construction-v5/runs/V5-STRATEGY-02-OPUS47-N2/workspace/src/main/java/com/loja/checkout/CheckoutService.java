package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.Niveis;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.modalidade.Modalidade;
import com.loja.checkout.modalidade.Modalidades;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.regiao.Regiao;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutService {
    private final Modalidades modalidades;
    private final Cupons cupons;
    private final Niveis niveis;
    private final FormasPagamento formasPagamento;

    public CheckoutService(Modalidades modalidades, Cupons cupons, Niveis niveis, FormasPagamento formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        List<Pedido.Item> itens = validarItens(req);
        NivelClube nivel = niveis.buscar(req.nivelClube())
                .orElseThrow(() -> new ErroCheckoutException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.buscar(req.regiao())
                .orElseThrow(() -> new ErroCheckoutException("REGIAO_INVALIDA"));
        Modalidade modalidade = modalidades.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new ErroCheckoutException("MODALIDADE_INVALIDA"));

        Pedido pedido = Pedido.de(itens);
        if (!modalidade.atende(pedido)) throw new ErroCheckoutException("MODALIDADE_INDISPONIVEL");

        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = cupons.buscar(req.cupom()).orElseThrow(() -> new ErroCheckoutException("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(pedido)) throw new ErroCheckoutException("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento pagamento = formasPagamento.buscar(req.formaPagamento())
                .orElseThrow(() -> new ErroCheckoutException("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelamentoValido(parcelas)) throw new ErroCheckoutException("PARCELAMENTO_INVALIDO");

        BigDecimal frete = nivel.ajustarFrete(modalidade.frete(pedido), pedido);
        BigDecimal descontoCupom = cupom == null ? Dinheiro.ZERO : cupom.desconto(pedido, frete);
        BigDecimal seguro = regiao.seguro(pedido);
        BigDecimal totalPedido = Dinheiro.emCentavos(
                pedido.subtotal().subtract(descontoCupom).add(frete).add(seguro));

        if (!pagamento.atende(totalPedido)) throw new ErroCheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");

        ResultadoPagamento rp = pagamento.calcular(totalPedido, parcelas);

        return new CheckoutResponse(
                pedido.subtotal(),
                Dinheiro.emCentavos(descontoCupom),
                frete,
                modalidade.prazoDias(),
                seguro,
                rp.ajuste(),
                rp.totalFinal(),
                rp.parcelas(),
                rp.valorParcela(),
                nivel.credito(pedido),
                nivel.brinde(pedido)
        );
    }

    private List<Pedido.Item> validarItens(CheckoutRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) throw new ErroCheckoutException("PEDIDO_INVALIDO");
        List<Pedido.Item> itens = new ArrayList<>(req.itens().size());
        for (CheckoutRequest.ItemRequest i : req.itens()) {
            if (i == null
                    || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                    || i.quantidade() == null || i.quantidade() <= 0
                    || i.pesoKg() == null || i.pesoKg().signum() <= 0) {
                throw new ErroCheckoutException("PEDIDO_INVALIDO");
            }
            itens.add(new Pedido.Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()));
        }
        return itens;
    }
}
