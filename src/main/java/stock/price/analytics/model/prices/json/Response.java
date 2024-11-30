package stock.price.analytics.model.prices.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Response{

	@JsonProperty("quoteResponse")
	private QuoteResponse quoteResponse;

}