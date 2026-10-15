package com.loja.checkout.dominio;

import com.loja.checkout.erro.Codigo;
import com.loja.checkout.erro.PedidoRecusado;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Como o cliente vai pagar. Cada forma define o proprio ajuste sobre o total do
 * pedido, em quantas vezes aceita parcelar e as proprias limitacoes.
 */
public enum FormaPagamento {

    /** Sempre a vista, com 5% de desconto no total do pedido. */
    PIX(1) {
        @Override
        public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.percentual(totalPedido, new BigDecimal("0.05"));
            return Cobranca.aVista(totalPedido.subtract(desconto));
        }
    },

    /** Sempre a vista, com a tarifa de R$ 3,49 do banco somada ao total. */
    BOLETO(1) {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
            return Cobranca.aVista(totalPedido.add(TARIFA));
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }
    },

    /** Em ate 3x sem juros; de 4x a 12x com juros de 1,99% ao mes, pela tabela Price. */
    CARTAO(12) {
        private static final int MAXIMO_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
            return parcelas <= MAXIMO_SEM_JUROS
                    ? semJuros(totalPedido, parcelas)
                    : comJuros(totalPedido, parcelas);
        }

        /** O valor final e o proprio total do pedido, dividido entre as parcelas. */
        private static Cobranca semJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128);
            return new Cobranca(Dinheiro.centavos(totalPedido), Dinheiro.centavos(parcela));
        }

        /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa) ^ -parcelas). */
        private static Cobranca comJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas);
            BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
            BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.multiply(TAXA_MENSAL).divide(divisor, MathContext.DECIMAL128));
            return new Cobranca(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }
    };

    private final int maximoDeParcelas;

    FormaPagamento(int maximoDeParcelas) {
        this.maximoDeParcelas = maximoDeParcelas;
    }

    /** Quanto o cliente vai pagar ao todo e por parcela nessa forma de pagamento. */
    public abstract Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    /** Se essa forma atende o pedido; por padrao atende qualquer total. */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public void exigirParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > maximoDeParcelas) {
            throw new PedidoRecusado(Codigo.PARCELAMENTO_INVALIDO);
        }
    }

    public void exigirQueAtenda(BigDecimal totalPedido) {
        if (!atende(totalPedido)) {
            throw new PedidoRecusado(Codigo.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }
}
