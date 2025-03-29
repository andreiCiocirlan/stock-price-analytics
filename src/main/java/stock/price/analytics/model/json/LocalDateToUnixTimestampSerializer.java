package stock.price.analytics.model.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class LocalDateToUnixTimestampSerializer extends StdSerializer<LocalDate> {

    public LocalDateToUnixTimestampSerializer() {
        super(LocalDate.class);
    }

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        ZonedDateTime zdt = value.atStartOfDay(ZoneId.systemDefault());
        Instant instant = zdt.toInstant();
        long unixTimestamp = instant.getEpochSecond();
        gen.writeNumber(unixTimestamp);
    }
}
