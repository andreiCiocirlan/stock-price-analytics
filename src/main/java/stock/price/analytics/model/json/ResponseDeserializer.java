package stock.price.analytics.model.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class ResponseDeserializer extends StdDeserializer<Response> {
    public ResponseDeserializer() {
        super(Response.class);
    }

    @Override
    public Response deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        QuoteResponse quoteResponse = parser.getCodec().treeToValue(node.get("quoteResponse"), QuoteResponse.class);
        Response response = new Response();
        response.setQuoteResponse(quoteResponse);
        return response;
    }
}
