package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

public enum SimpleMovingAverageMilestone implements PriceMilestone {

    ABOVE_200_SMA("sma_milestone") {
        @Override
        public String toString() {
            return "> 200 SMA";
        }
    },
    ABOVE_50_SMA("sma_milestone") {
        @Override
        public String toString() {
            return "> 50 SMA";
        }
    },
    BELOW_200_SMA("sma_milestone") {
        @Override
        public String toString() {
            return "< 200 SMA";
        }
    },
    BELOW_50_SMA("sma_milestone") {
        @Override
        public String toString() {
            return "< 50 SMA";
        }
    },
    NONE("sma_milestone") {
        @Override
        public String toString() {
            return "None";
        }
    };

    private final String type;

    SimpleMovingAverageMilestone(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return this.type;
    }
}
