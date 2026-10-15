package loja.checkout;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import loja.checkout.clube.NivelClube;
import loja.checkout.cupom.Cupom;
import loja.checkout.entrega.ModalidadeEntrega;
import loja.checkout.pagamento.Cobranca;
import loja.checkout.pagamento.FormaPagamento;

@Service
public class ResumoService {
    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> pagamentos;

    public ResumoService(List<ModalidadeEntrega> entregas, List<Cupom> cupons, List<NivelClube> niveis,
            List<FormaPagamento> pagamentos) {
        this.entregas = new Catalogo<>(entregas);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.pagamentos = new Catalogo<>(pagamentos);
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        Carrinho carrinho = carrinho(pedido);
        NivelClube nivel = niveis.buscar(pedido.nivelClube()).orElseThrow(() -> new ErroPedido("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.de(pedido.regiao()).orElseThrow(() -> new ErroPedido("REGIAO_INVALIDA"));
        ModalidadeEntrega entrega = entregas.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new ErroPedido("MODALIDADE_INVALIDA"));
        if (!entrega.atende(carrinho.pesoKg())) {
            throw new ErroPedido("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.isentoDeFrete() ? Dinheiro.ZERO : entrega.frete(carrinho.pesoKg());
        BigDecimal descontoCupom = descontoCupom(pedido.cupom(), carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        FormaPagamento pagamento = pagamentos.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new ErroPedido("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new ErroPedido("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.atende(totalPedido)) {
            throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(subtotal, descontoCupom, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(totalPedido), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private Carrinho carrinho(ResumoRequest pedido) {
        List<Item> itens = pedido == null ? null : pedido.itens();
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(i -> i == null || !i.valido())) {
            throw new ErroPedido("PEDIDO_INVALIDO");
        }
        return new Carrinho(itens);
    }

    private BigDecimal descontoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new ErroPedido("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(carrinho)) {
            throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(carrinho, frete);
    }
}
