package loja.checkout;

import java.math.BigDecimal;
import java.util.List;
import loja.checkout.clube.NivelClube;
import loja.checkout.cupom.Cupom;
import loja.checkout.entrega.ModalidadeEntrega;
import loja.checkout.pagamento.Cobranca;
import loja.checkout.pagamento.FormaPagamento;
import loja.checkout.web.ItemRequest;
import loja.checkout.web.PedidoRequest;
import loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> pagamentos;

    public ResumoService(Catalogo<ModalidadeEntrega> entregas, Catalogo<Cupom> cupons,
                         Catalogo<NivelClube> niveis, Catalogo<FormaPagamento> pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveis = niveis;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = niveis.exigir(pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = Regiao.de(pedido.regiao()).orElseThrow(() -> new ErroPedido("REGIAO_INVALIDA"));
        ModalidadeEntrega entrega = entregas.exigir(pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!entrega.atende(carrinho)) {
            throw new ErroPedido("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(entrega.frete(carrinho));
        BigDecimal desconto = descontoCupom(pedido.cupom(), carrinho, frete);

        FormaPagamento pagamento = pagamentos.exigir(pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new ErroPedido("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!pagamento.atende(totalPedido)) {
            throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(subtotal, desconto, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(totalPedido), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }
        Cupom cupom = cupons.exigir(codigo, "CUPOM_INVALIDO");
        if (!cupom.aplicavel(carrinho)) {
            throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(carrinho, frete);
    }

    private Carrinho carrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(this::invalido)) {
            throw new ErroPedido("PEDIDO_INVALIDO");
        }
        return new Carrinho(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private boolean invalido(ItemRequest i) {
        return i == null
                || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                || i.quantidade() == null || i.quantidade() <= 0
                || i.pesoKg() == null || i.pesoKg().signum() <= 0;
    }
}
