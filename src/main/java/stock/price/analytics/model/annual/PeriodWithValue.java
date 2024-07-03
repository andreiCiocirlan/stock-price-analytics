package stock.price.analytics.model.annual;

import com.fasterxml.jackson.annotation.JsonProperty;

//@Entity
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class PeriodWithValue {

//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_pwv")
//    @SequenceGenerator(name = "seqGen_pwv", sequenceName = "seq_pwv")
    private Long id;

    @JsonProperty("period")
    protected String period;

    @JsonProperty("v")
    protected double V;

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getPeriod() {
        return period;
    }

    public void setV(double V) {
        this.V = V;
    }

    public double getV() {
        return V;
    }

    @Override
    public String toString() {
        return
                "ResponseItem{" +
                        "period = '" + period + '\'' +
                        ",v = '" + V + '\'' +
                        "}";
    }
}