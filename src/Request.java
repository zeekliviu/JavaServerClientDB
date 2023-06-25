import java.io.Serializable;

public class Request implements Serializable {
    private String query;

    private TipOperatie operation;

    private String name;

    public Request(String query, TipOperatie operation, String name) {
        this.query = query;
        this.operation = operation;
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public TipOperatie getOperation() {
        return operation;
    }

    public String getName() {return name;}
}
