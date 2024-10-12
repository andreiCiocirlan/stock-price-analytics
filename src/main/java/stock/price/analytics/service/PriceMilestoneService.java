package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.PriceMilestone;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    @PersistenceContext
    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<String> findTickersForMilestone(PriceMilestone priceMilestone, double cfdMargin) {
        String queryStr = queryPriceMilestone(priceMilestone, cfdMargin);
        Query nativeQuery = entityManager.createNativeQuery(queryStr, String.class);

        return (List<String>) nativeQuery.getResultList();
    }

    private String queryPriceMilestone(PriceMilestone priceMilestone, double cfdMargin) {
        String highLowTable = PriceMilestone.tableNameFrom(priceMilestone);
        String whereClause = PriceMilestone.whereClauseFrom(priceMilestone);
        String currentMondayFormatted = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        String joinDateFormatted = PriceMilestone.joinDateFrom(priceMilestone);

        return STR."""
                SELECT wp.ticker
                FROM weekly_prices wp
                JOIN \{highLowTable} hl ON hl.ticker = wp.ticker
                    AND hl.start_date = '\{joinDateFormatted}'
                JOIN stocks s ON s.ticker = wp.ticker
                WHERE
                	wp.start_date = '\{currentMondayFormatted}'
                	AND \{whereClause}
                	AND s.cfd_margin = \{cfdMargin};
                """;
    }
}
