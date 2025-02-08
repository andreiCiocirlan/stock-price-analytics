package stock.price.analytics.util;


import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class EnumParser {

    public static <T extends Enum<T>> Optional<T> parseEnumWithNoneValue(String enumStr, Class<T> enumClass) {
        if (isNotValidEnum(enumStr, enumClass)) return Optional.empty();

        T enumValue = Enum.valueOf(enumClass, enumStr);
        return isNoneEnum(enumValue) ? Optional.empty() : Optional.of(enumValue);
    }

    public static <T extends Enum<T>> Optional<T> parseEnum(String enumStr, Class<T> enumClass) {
        return isValidEnum(enumStr, enumClass) ? Optional.of(Enum.valueOf(enumClass, enumStr)) : Optional.empty();
    }

    public static <T extends Enum<T>> boolean isNoneEnum(String enumStr, Class<T> enumClass) {
        return isValidEnum(enumStr, enumClass) && Enum.valueOf(enumClass, enumStr).name().equals("NONE");
    }

    public static <T extends Enum<T>> boolean isNoneEnum(T enumValue) {
        return enumValue != null && enumValue.name().equals("NONE");
    }

    public static <T extends Enum<T>> boolean isNotValidEnum(String enumStr, Class<T> enumClass) {
        if (StringUtils.isBlank(enumStr)) return true;

        try {
            Enum.valueOf(enumClass, enumStr);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static <T extends Enum<T>> boolean isValidEnum(String enumStr, Class<T> enumClass) {
        if (StringUtils.isBlank(enumStr)) return false;

        try {
            Enum.valueOf(enumClass, enumStr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
