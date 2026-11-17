package loja;

public class Produto {
    private final int id;
    private final String nome;
    private final String categoria;
    private final String descricao;
    private final String emoji;
    private final String tom;
    private final int precoCentavos;
    private int estoque;

    public Produto(int id, String nome, String categoria, String descricao,
                   String emoji, String tom, int precoCentavos, int estoque) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.descricao = descricao;
        this.emoji = emoji;
        this.tom = tom;
        this.precoCentavos = precoCentavos;
        this.estoque = estoque;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public String getEmoji() { return emoji; }
    public String getTom() { return tom; }
    public int getPrecoCentavos() { return precoCentavos; }
    public int getEstoque() { return estoque; }
    public void diminuirEstoque(int unidades) { estoque -= unidades; }
    public void devolverAoEstoque(int unidades) { estoque += unidades; }

    public String toJson() {
        return "{\"id\":" + id + ",\"nome\":" + Json.entreAspas(nome) +
               ",\"categoria\":" + Json.entreAspas(categoria) +
               ",\"descricao\":" + Json.entreAspas(descricao) +
               ",\"emoji\":" + Json.entreAspas(emoji) +
               ",\"tom\":" + Json.entreAspas(tom) +
               ",\"precoCentavos\":" + precoCentavos +
               ",\"estoque\":" + estoque + "}";
    }
}
