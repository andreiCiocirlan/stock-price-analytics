package stock.price.analytics.model.quote;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuoteData{

	@JsonProperty("quoteResponse")
	private QuoteResponse quoteResponse;
}