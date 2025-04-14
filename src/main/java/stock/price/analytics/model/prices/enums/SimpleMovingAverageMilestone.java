package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

import java.util.List;

public enum SimpleMovingAverageMilestone implements PriceMilestone {

    ABOVE_200_SMA("sma-milestone") {
        @Override
        public String toString() {
            return "> 200 SMA";
        }
    },
    ABOVE_50_SMA("sma-milestone") {
        @Override
        public String toString() {
            return "> 50 SMA";
        }
    },
    BELOW_200_SMA("sma-milestone") {
        @Override
        public String toString() {
            return "< 200 SMA";
        }
    },
    BELOW_50_SMA("sma-milestone") {
        @Override
        public String toString() {
            return "< 50 SMA";
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

    public static List<PriceMilestone> smaMilestones() {
        return List.of(ABOVE_50_SMA, ABOVE_200_SMA, BELOW_50_SMA, BELOW_200_SMA);
    }
}
