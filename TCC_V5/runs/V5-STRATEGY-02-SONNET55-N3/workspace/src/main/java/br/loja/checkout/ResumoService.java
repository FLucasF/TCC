package br.loja.checkout;

import br.loja.checkout.clube.NivelClube;
import br.loja.checkout.cupom.Cupom;
import br.loja.checkout.entrega.Entrega;
import br.loja.checkout.pagamento.Cobranca;
import br.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Map<String, NivelClube> niveis;
    private final Map<String, Entrega> entregas;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> pagamentos;

    public ResumoService(List<NivelClube> niveis, List<Entrega> entregas, List<Cupom> cupons, List<FormaPagamento> pagamentos) {
        this.niveis = Identificavel.indexar(niveis);
        this.entregas = Identificavel.indexar(entregas);
        this.cupons = Identificavel.indexar(cupons);
        this.pagamentos = Identificavel.indexar(pagamentos);
    }

    public ResumoResponse calcular(ResumoRequest req) {
        Pedido pedido = pedido(req);
        NivelClube nivel = obrigatorio(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = regiao(req.regiao());
        Entrega entrega = obrigatorio(entregas, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        exigir(entrega.atende(pedido), "MODALIDADE_INDISPONIVEL");

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = nivel.frete(entrega.frete(pedido));
        BigDecimal desconto = descontoCupom(req.cupom(), pedido, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento pagamento = obrigatorio(pagamentos, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        exigir(pagamento.aceitaParcelas(parcelas), "PARCELAMENTO_INVALIDO");
        exigir(pagamento.disponivel(total), "FORMA_PAGAMENTO_INDISPONIVEL");
        Cobranca cobranca = pagamento.cobrar(total, parcelas);

        return new ResumoResponse(subtotal, desconto, frete, entrega.prazoDias(pedido), seguro,
                cobranca.totalFinal().subtract(total), cobranca.totalFinal(), parcelas, cobranca.valorParcela(),
                nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, Pedido pedido, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.valor("0");
        }
        Cupom cupom = obrigatorio(cupons, codigo, "CUPOM_INVALIDO");
        exigir(cupom.aplicavel(pedido, frete), "CUPOM_NAO_APLICAVEL");
        return cupom.desconto(pedido, frete);
    }

    private Pedido pedido(ResumoRequest req) {
        List<ResumoRequest.Item> brutos = req.itens();
        exigir(brutos != null && !brutos.isEmpty() && brutos.stream().allMatch(ResumoService::itemValido), "PEDIDO_INVALIDO");
        return new Pedido(brutos.stream()
                .map(i -> new ItemCarrinho(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private static boolean itemValido(ResumoRequest.Item i) {
        return i != null
                && i.precoUnitario() != null && i.precoUnitario().signum() > 0
                && i.quantidade() != null && i.quantidade() > 0
                && i.pesoKg() != null && i.pesoKg().signum() > 0;
    }

    private static Regiao regiao(String codigo) {
        try {
            return Regiao.valueOf(Objects.requireNonNull(codigo));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PedidoRecusadoException("REGIAO_INVALIDA");
        }
    }

    private static <T> T obrigatorio(Map<String, T> opcoes, String codigo, String erro) {
        T opcao = codigo == null ? null : opcoes.get(codigo);
        exigir(opcao != null, erro);
        return opcao;
    }

    private static void exigir(boolean condicao, String erro) {
        if (!condicao) {
            throw new PedidoRecusadoException(erro);
        }
    }
}
