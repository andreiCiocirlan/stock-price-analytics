package stock.price.analytics.model.annual;

import com.fasterxml.jackson.annotation.JsonProperty;

//@Entity
//@Table(name = "fin_data")
public class FinancialData {

//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_fin_data")
//    @SequenceGenerator(name = "seqGen_fin_data", sequenceName = "seq_fin_data")
    private Long id;

    @JsonProperty("metricType")
    private String metricType;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("metric")
    private Metric metric;

    @JsonProperty("series")
    private Series series;

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public Series getSeries() {
        return series;
    }

    @Override
    public String toString() {
        return
                STR."Response{metricType = '\{metricType}\{'\''},symbol = '\{symbol}\{'\''},metric = '\{metric}\{'\''},series = '\{series}\{'\''}}";
    }
}