package loja.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import loja.checkout.clube.NivelClube;
import loja.checkout.cupom.Cupom;
import loja.checkout.entrega.Entrega;
import loja.checkout.pagamento.Cobranca;
import loja.checkout.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Map<String, Entrega> entregas;
    private final Map<String, Cupom> cupons;
    private final Map<String, NivelClube> niveis;
    private final Map<String, FormaPagamento> pagamentos;

    public ResumoService(List<Entrega> entregas, List<Cupom> cupons, List<NivelClube> niveis,
                         List<FormaPagamento> pagamentos) {
        this.entregas = Codificado.indexar(entregas);
        this.cupons = Codificado.indexar(cupons);
        this.niveis = Codificado.indexar(niveis);
        this.pagamentos = Codificado.indexar(pagamentos);
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        Carrinho carrinho = carrinho(pedido);
        NivelClube nivel = escolher(niveis, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = regiao(pedido.regiao());
        Entrega entrega = escolher(entregas, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!entrega.atende(carrinho)) {
            throw new ErroNegocio("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.freteCobrado(entrega.frete(carrinho));
        BigDecimal desconto = descontoCupom(pedido.cupom(), carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento pagamento = escolher(pagamentos, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new ErroNegocio("PARCELAMENTO_INVALIDO");
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new ErroNegocio("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(subtotal, desconto, frete, entrega.prazoDias(), seguro,
                Dinheiro.arredondar(cobranca.totalFinal().subtract(totalPedido)),
                Dinheiro.arredondar(cobranca.totalFinal()), parcelas, cobranca.valorParcela(),
                nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = escolher(cupons, codigo, "CUPOM_INVALIDO");
        if (!cupom.aplicavel(carrinho)) {
            throw new ErroNegocio("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(carrinho, frete);
    }

    private static <T> T escolher(Map<String, T> opcoes, String codigo, String erro) {
        T opcao = codigo == null ? null : opcoes.get(codigo);
        if (opcao == null) {
            throw new ErroNegocio(erro);
        }
        return opcao;
    }

    private static Regiao regiao(String codigo) {
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ErroNegocio("REGIAO_INVALIDA");
        }
    }

    private static Carrinho carrinho(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new ErroNegocio("PEDIDO_INVALIDO");
        }
        List<Item> itens = pedido.itens().stream().map(i -> {
            if (i == null || i.precoUnitario() == null || i.quantidade() == null || i.pesoKg() == null
                    || i.precoUnitario().signum() <= 0 || i.quantidade() <= 0 || i.pesoKg().signum() <= 0) {
                throw new ErroNegocio("PEDIDO_INVALIDO");
            }
            return new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg());
        }).toList();
        return new Carrinho(itens);
    }
}
