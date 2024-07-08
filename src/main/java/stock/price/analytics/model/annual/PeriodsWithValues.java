package stock.price.analytics.model.annual;

import java.util.List;

public class PeriodsWithValues {

    private List<PeriodWithValue> periodWithValueList;

    public void setPeriodWithValueList(List<PeriodWithValue> periodWithValueList) {
        this.periodWithValueList = periodWithValueList;
    }

    public List<PeriodWithValue> getPeriodWithValueList() {
        return periodWithValueList;
    }

    @Override
    public String toString() {
        return STR."PeriodsWithValues{periodWithValueList=\{periodWithValueList}\{'}'}";
    }
}