package stock.price.analytics.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuoteResponse {

	@JsonProperty("result")
	private List<DailyPricesJSON> result;

	@JsonProperty("error")
	private String error;

}