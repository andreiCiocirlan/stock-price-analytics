package stock.price.analytics.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class JsonUtil {

    public static String mergedPricesJSONs(List<String> pricesJSONs) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode mergedQuoteResponse = objectMapper.createObjectNode();
            ArrayNode mergedResults = objectMapper.createArrayNode();
            for (String json : pricesJSONs) {
                JsonNode rootNode = objectMapper.readTree(json);
                JsonNode results = rootNode.path("quoteResponse").path("result");

                if (results.isArray()) {
                    results.forEach(mergedResults::add);
                }
            }
            mergedQuoteResponse.set("result", mergedResults);
            ObjectNode finalResponse = objectMapper.createObjectNode();
            finalResponse.set("quoteResponse", mergedQuoteResponse);

            return objectMapper.writeValueAsString(finalResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
