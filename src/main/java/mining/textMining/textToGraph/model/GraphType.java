package mining.textMining.textToGraph.model;

public enum GraphType {

    /* modelos de grafo de Adam Schenker: standard, simple, n-distance, n-simple distance, absolute frequency, relative frequency
        standard:
          cada termo nao-repetido do doc vira um nó
          adjacency information as edges
          para cada secao S do doc:
            para cada termo A que preceda imediatamente um outro (B), criar uma aresta direcionada de A para B, com rótulo S
            OBS: esta predececao nao é considerada quando ha, entre A e B, ponto ou interrogacao ou exclamacao
          as secoes consideradas sao:
            'title' (da pagina, e as keywords), 'link' (cada texto de um link), 'text' (textos visiveis exceto title e keywords)
        simple: nao rotula as arestas
        n-distance: dado N parametrico, os nós sao conectados por arestas de rotulo x, onde x <= N,
          sendo x a qtd arestas que leva A para B em standard (lembrando de desconsiderar caminhos contendo punctuation marks..)
          (ou pode-se implementar diretamente sem precisar primeiro montar o grafo de standard)
        n-simple distance: parecido n-distance sem rotular as arestas (so leva em consideracao a limitacao de conectividade...)
        absolute frequency: metodo standard, mas indicar aos nós a qtd de aparicoes no doc, e nas arestas a qtd da conectividade direta de A a B
        relative frequency: 'absolute frequency' normalizado. A value in [0,1] is assigned by dividing each node frequency
          value by the maximum node frequency value that occurs in the graph; a similar procedure is performed for the edges.
     */

    //análise:
    //  - apenas ABSOLUTE_FREQUENCY e RELATIVE_FREQUENCY levam em consideração os TF's dos termos!
    //  - RELATIVE_FREQUENCY parece melhor que ABSOLUTE_FREQUENCY por relativizar as frequencias das arestas e os TF's dos termos
    //  - N_DISTANCE é específico demais ao indicar por rotulo o grau da conectividade. N_SIMPLE_DISTANCE seria seu substituto natural.

    STANDARD            (false,  "section",  "no"),
    SIMPLE              (false,  "empty",    "no"),
    N_DISTANCE          (true,   "reach",    "no"),
    N_SIMPLE_DISTANCE   (true,   "empty",    "no"),
    ABSOLUTE_FREQUENCY  (false,  "empty",    "yes"),
    RELATIVE_FREQUENCY  (false,  "empty",    "normalized");

    private boolean enlargeNeighborhoodReach;
    private String labelType;
    private String countingType;

    private GraphType(boolean enlargeNeighborhoodReach, String labelType, String countingType) {
        this.enlargeNeighborhoodReach = enlargeNeighborhoodReach;
        this.labelType = labelType;
        this.countingType = countingType;
    }

    public boolean isEnlargeNeighborhoodReach() {
        return enlargeNeighborhoodReach;
    }
    public String getLabel(String sectionId, int neighborReach) {
        if ("section".equals(labelType)) {
            return sectionId;
        }
        if ("reach".equals(labelType)) {
            return String.valueOf(neighborReach);
        }
        return "";
    }

    public String getLabelType() {
		return labelType;
	}

    public boolean isCountingRequired() {
        return "yes".equals(countingType) || "normalized".equals(countingType);
    }

    public boolean isCountingRequiredAsRelative() {
        return "normalized".equals(countingType);
    }

    public static GraphType get(String name) {
        for (GraphType t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

	public static GraphType[] getByNames(String[] names) {
		GraphType[] graphTypes = new GraphType[names.length];
		for (int i = 0; i < names.length; i++) {
			graphTypes[i] = get(names[i]);
		}
		return graphTypes;
	}
}
