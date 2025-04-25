package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.StockPriceMilestone;
import stock.price.analytics.model.prices.context.StockDailyPriceContext;

import static stock.price.analytics.util.Constants.MIN_GAP_AND_GO_PERCENTAGE;

public enum PreMarketPriceMilestone implements StockPriceMilestone<StockDailyPriceContext> {

    KICKING_CANDLE_UP {
        @Override
        public String toString() {
            return "Kicking candlestick UP";
        }
    },
    KICKING_CANDLE_DOWN {
        @Override
        public String toString() {
            return "Kicking candlestick DOWN";
        }
    },
    PRE_NEW_4W_LOW {
        @Override
        public String toString() {
            return "New 4w Low";
        }
    },
    PRE_NEW_4W_HIGH {
        @Override
        public String toString() {
            return "New 4w High";
        }
    },
    PRE_NEW_52W_LOW {
        @Override
        public String toString() {
            return "New 52w Low";
        }
    },
    PRE_NEW_52W_HIGH {
        @Override
        public String toString() {
            return "New 52w High";
        }
    },
    PRE_NEW_ALL_TIME_LOW {
        @Override
        public String toString() {
            return "New All-time Low";
        }
    },
    PRE_NEW_ALL_TIME_HIGH {
        @Override
        public String toString() {
            return "New All-time High";
        }
    };

    @Override
    public boolean isMetFor(StockDailyPriceContext context) {
        return switch (this) {
            // previous day DOWN more than 1% && pre-market GAP UP more than 4%
            case KICKING_CANDLE_UP ->
                    context.stock().getDailyPerformance() < -1.0d && context.dailyPrice().getClose() > context.stock().getClose() * (1 + MIN_GAP_AND_GO_PERCENTAGE);
            // previous day UP more than 1% && pre-market GAP DOWN more than 4%
            case KICKING_CANDLE_DOWN ->
                    context.stock().getDailyPerformance() > 1.0d && context.dailyPrice().getClose() < context.stock().getClose() * (1 - MIN_GAP_AND_GO_PERCENTAGE);
            // new 4w, 52w, all-time high-low in pre-market
            case PRE_NEW_4W_HIGH -> context.dailyPrice().getClose() > context.stock().getHigh4w();
            case PRE_NEW_4W_LOW -> context.dailyPrice().getClose() < context.stock().getLow4w();
            case PRE_NEW_52W_HIGH -> context.dailyPrice().getClose() > context.stock().getHigh52w();
            case PRE_NEW_52W_LOW -> context.dailyPrice().getClose() < context.stock().getLow52w();
            case PRE_NEW_ALL_TIME_HIGH -> context.dailyPrice().getClose() > context.stock().getHighest();
            case PRE_NEW_ALL_TIME_LOW -> context.dailyPrice().getClose() < context.stock().getLowest();
        };
    }
}