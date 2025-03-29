package stock.price.analytics.model.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class UnixTimestampToLocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Long timestamp = p.getLongValue();
        return (timestamp == null) ? null : Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}