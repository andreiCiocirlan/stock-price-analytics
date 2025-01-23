package stock.price.analytics.model.prices.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response{

	@JsonProperty("quoteResponse")
	private QuoteResponse quoteResponse;

}