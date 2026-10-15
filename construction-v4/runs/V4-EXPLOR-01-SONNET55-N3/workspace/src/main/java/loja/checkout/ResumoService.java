package loja.checkout;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import loja.checkout.clube.NivelClube;
import loja.checkout.comum.Catalogo;
import loja.checkout.comum.Compra;
import loja.checkout.comum.Dinheiro;
import loja.checkout.comum.Item;
import loja.checkout.comum.RecusaException;
import loja.checkout.cupom.Cupom;
import loja.checkout.entrega.Entrega;
import loja.checkout.pagamento.Cobranca;
import loja.checkout.pagamento.FormaPagamento;

@Service
public class ResumoService {
    private final Catalogo<Entrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoService(List<Entrega> entregas, List<Cupom> cupons, List<NivelClube> niveis,
            List<FormaPagamento> formasPagamento) {
        this.entregas = new Catalogo<>(entregas);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        Compra compra = Compra.de(itens(pedido));
        NivelClube nivel = niveis.buscar(pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = regiao(pedido.regiao());
        Entrega entrega = entregas.buscar(pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!entrega.atende(compra)) {
            throw new RecusaException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivel.frete(entrega.frete(compra));
        BigDecimal desconto = desconto(pedido.cupom(), compra, frete);
        BigDecimal seguro = Dinheiro.percentual(compra.subtotal(), regiao.taxaSeguro());
        BigDecimal total = compra.subtotal().subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = formasPagamento.buscar(pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new RecusaException("PARCELAMENTO_INVALIDO");
        }
        if (!forma.atende(total)) {
            throw new RecusaException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = forma.cobrar(total, parcelas);

        return new ResumoResponse(
                compra.subtotal(),
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(total),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(compra),
                nivel.brinde(compra));
    }

    private BigDecimal desconto(String codigoCupom, Compra compra, BigDecimal frete) {
        if (codigoCupom == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigoCupom, "CUPOM_INVALIDO");
        if (!cupom.aplicavel(compra)) {
            throw new RecusaException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(compra, frete);
    }

    private static Regiao regiao(String codigo) {
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RecusaException("REGIAO_INVALIDA");
        }
    }

    private static List<Item> itens(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new RecusaException("PEDIDO_INVALIDO");
        }
        return pedido.itens().stream().map(ResumoService::item).toList();
    }

    private static Item item(PedidoRequest.ItemRequest item) {
        if (item == null
                || item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
            throw new RecusaException("PEDIDO_INVALIDO");
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }
}
