package stock.price.analytics.util.query.stddevprojection;

public interface StdDevProjectionQueryProvider {
    String findLast3TopProjections(String ticker);
    String findLast3BottomProjections(String ticker);
}
