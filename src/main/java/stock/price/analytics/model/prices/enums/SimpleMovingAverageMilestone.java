package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.json.DailyPriceJSON;
import stock.price.analytics.model.prices.StockPriceMilestone;

public enum SimpleMovingAverageMilestone implements StockPriceMilestone<DailyPriceJSON> {

    ABOVE_200_SMA {
        @Override
        public String toString() {
            return "> 200 SMA";
        }
    },
    ABOVE_50_SMA {
        @Override
        public String toString() {
            return "> 50 SMA";
        }
    },
    BELOW_200_SMA {
        @Override
        public String toString() {
            return "< 200 SMA";
        }
    },
    BELOW_50_SMA {
        @Override
        public String toString() {
            return "< 50 SMA";
        }
    };

    @Override
    public boolean isMetFor(DailyPriceJSON dailyPriceJSON) {
        return switch (this) {
            case ABOVE_200_SMA -> dailyPriceJSON.getRegularMarketPrice() > dailyPriceJSON.getTwoHundredDayAverage();
            case ABOVE_50_SMA -> dailyPriceJSON.getRegularMarketPrice() > dailyPriceJSON.getFiftyDayAverage();
            case BELOW_200_SMA -> dailyPriceJSON.getRegularMarketPrice() < dailyPriceJSON.getTwoHundredDayAverage();
            case BELOW_50_SMA -> dailyPriceJSON.getRegularMarketPrice() < dailyPriceJSON.getFiftyDayAverage();
        };
    }
}
