package stock.price.analytics.model.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Response {

    private final QuoteResponse quoteResponse;

    @JsonCreator
    public Response(@JsonProperty("quoteResponse") QuoteResponse quoteResponse) {
        this.quoteResponse = quoteResponse;
    }
}