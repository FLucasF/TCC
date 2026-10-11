package br.com.loja.checkout.servico;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ErroCheckout;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.dominio.Resumo;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Map<String, NivelClube> niveis;
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = indexar(niveis, NivelClube::codigo);
        this.modalidades = indexar(modalidades, ModalidadeEntrega::codigo);
        this.cupons = indexar(cupons, Cupom::codigo);
        this.formasPagamento = indexar(formasPagamento, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> indexar(List<T> lista, Function<T, String> chave) {
        return lista.stream().collect(Collectors.toMap(chave, Function.identity()));
    }

    public Resumo calcular(ResumoRequest req) {
        if (req == null) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        Pedido pedido = montarPedido(req);

        NivelClube nivel = buscar(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = Regiao.de(req.regiao());
        if (regiao == null) {
            throw new ErroCheckout("REGIAO_INVALIDA");
        }
        ModalidadeEntrega entrega = buscar(modalidades, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!entrega.atende(pedido)) {
            throw new ErroCheckout("MODALIDADE_INDISPONIVEL");
        }
        Cupom cupom = null;
        if (req.cupom() != null) {
            cupom = buscar(cupons, req.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(pedido)) {
                throw new ErroCheckout("CUPOM_NAO_APLICAVEL");
            }
        }
        FormaPagamento pagamento = buscar(formasPagamento, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = parcelas(req.parcelas());
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : entrega.frete(pedido);
        BigDecimal desconto = cupom == null ? Dinheiro.ZERO : cupom.desconto(pedido, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!pagamento.atende(total)) {
            throw new ErroCheckout("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = pagamento.cobrar(total, parcelas);

        return new Resumo(subtotal, desconto, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(total), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private static <T> T buscar(Map<String, T> mapa, String codigo, String erro) {
        T valor = codigo == null ? null : mapa.get(codigo);
        if (valor == null) {
            throw new ErroCheckout(erro);
        }
        return valor;
    }

    private static int parcelas(BigDecimal valor) {
        if (valor == null) {
            return 1;
        }
        try {
            return valor.intValueExact();
        } catch (ArithmeticException e) {
            throw new ErroCheckout("PARCELAMENTO_INVALIDO");
        }
    }

    private static Pedido montarPedido(ResumoRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new ErroCheckout("PEDIDO_INVALIDO");
        }
        List<Item> itens = new ArrayList<>();
        for (ResumoRequest.ItemRequest i : req.itens()) {
            if (i == null || i.precoUnitario() == null || i.quantidade() == null || i.pesoKg() == null
                    || i.precoUnitario().signum() <= 0 || i.quantidade().signum() <= 0 || i.pesoKg().signum() <= 0) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            int quantidade;
            try {
                quantidade = i.quantidade().intValueExact();
            } catch (ArithmeticException e) {
                throw new ErroCheckout("PEDIDO_INVALIDO");
            }
            itens.add(new Item(i.nome(), i.precoUnitario(), quantidade, i.pesoKg()));
        }
        return new Pedido(List.copyOf(itens));
    }
}
