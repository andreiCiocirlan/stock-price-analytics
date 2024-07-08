package stock.price.analytics.model.annual;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;
import stock.price.analytics.model.quarterly.Quarterly;

@JsonIgnoreProperties(ignoreUnknown = true)
//@Embeddable
public class Series {

    @JsonProperty("annual")
    private Annual annual;

    @JsonProperty("quarterly")
    @Transient
    private Quarterly quarterly;

    public void setAnnual(Annual annual) {
        this.annual = annual;
    }

    public Annual getAnnual() {
        return annual;
    }

    public Quarterly getQuarterly() {
        return quarterly;
    }

    public void setQuarterly(Quarterly quarterly) {
        this.quarterly = quarterly;
    }

    @Override
    public String toString() {
        return
                STR."Series{annual = '\{annual}\{'\''}}";
    }
}