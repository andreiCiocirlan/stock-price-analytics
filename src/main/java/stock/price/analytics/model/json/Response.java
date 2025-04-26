package stock.price.analytics.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonDeserialize(using = ResponseDeserializer.class)
public class Response {

    @JsonProperty("quoteResponse")
    private QuoteResponse quoteResponse;

}