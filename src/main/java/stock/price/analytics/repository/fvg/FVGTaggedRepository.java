package stock.price.analytics.repository.fvg;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.fvg.FairValueGap;

import java.util.List;

@Repository
public interface FVGTaggedRepository extends JpaRepository<FairValueGap, Long> {

    @Query(value = """
            SELECT fvg.*
            FROM stocks s
            JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = 'WEEKLY' and fvg.type = 'BEARISH'
            WHERE
                    s.cfd_margin in :cfdMargins
                AND s.lowest <> s.highest AND (1 - (1 - ((s.w_close - s.lowest) / (s.highest - s.lowest)))) > 0.95
                AND (s.w_high between fvg.low AND fvg.high OR s.w_low between fvg.low AND fvg.high)
            """, nativeQuery = true)
    List<FairValueGap> findWeeklyTaggedFVGsBearish95thPercentileAllTimeHigh(@Param("cfdMargins") List<Double> cfdMargins);

    @Query(value = """
            SELECT fvg.*
            FROM stocks s
            JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = 'WEEKLY' and fvg.type = 'BULLISH'
            WHERE
                    s.cfd_margin in :cfdMargins
            	AND s.lowest <> s.highest AND (1 - (s.w_close - s.lowest) / (s.highest - s.lowest)) > 0.95
                AND fvg.date < date_trunc('WEEK', s.last_updated) - INTERVAL '1 week'
                AND (s.w_high between fvg.low AND fvg.high OR s.w_low between fvg.low AND fvg.high)
            """, nativeQuery = true)
    List<FairValueGap> findWeeklyTaggedFVGsBullish95thPercentileAllTimeLow(@Param("cfdMargins") List<Double> cfdMargins);

    @Query(value = """
            SELECT fvg.*
            FROM stocks s
            JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = 'WEEKLY' and fvg.type = 'BEARISH'
            WHERE
                    s.cfd_margin in :cfdMargins
                AND s.low52w <> s.high52w AND (1 - (1 - ((s.w_close - s.low52w) / (s.high52w - s.low52w)))) > 0.95
                AND (s.w_high between fvg.low AND fvg.high OR s.w_low between fvg.low AND fvg.high)
            """, nativeQuery = true)
    List<FairValueGap> findWeeklyTaggedFVGsBearish95thPercentile52wHigh(@Param("cfdMargins") List<Double> cfdMargins);

    @Query(value = """
            SELECT fvg.*
            FROM stocks s
            JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = 'WEEKLY' and fvg.type = 'BULLISH'
            WHERE
                    s.cfd_margin in :cfdMargins
            	AND s.low52w <> s.high52w AND (1 - (s.w_close - s.low52w) / (s.high52w - s.low52w)) > 0.95
                AND fvg.date < date_trunc('WEEK', s.last_updated) - INTERVAL '1 week'
                AND (s.w_high between fvg.low AND fvg.high OR s.w_low between fvg.low AND fvg.high)
            """, nativeQuery = true)
    List<FairValueGap> findWeeklyTaggedFVGsBullish95thPercentile52wLow(@Param("cfdMargins") List<Double> cfdMargins);

    @Query(value = """
            SELECT fvg.*
            FROM stocks s
            JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = 'WEEKLY' and fvg.type = 'BEARISH'
            WHERE
                    s.cfd_margin in :cfdMargins
                AND s.low4w <> s.high4w AND (1 - (1 - ((s.w_close - s.low4w) / (s.high4w - s.low4w)))) > 0.95
                AND (s.w_high between fvg.low AND fvg.high OR s.w_low between fvg.low AND fvg.high)
            """, nativeQuery = true)
    List<FairValueGap> findWeeklyTaggedFVGsBearish95thPercentile4wHigh(@Param("cfdMargins") List<Double> cfdMargins);

    @Query(value = """
            SELECT fvg.*
            FROM stocks s
            JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = 'WEEKLY' and fvg.type = 'BULLISH'
            WHERE
                    s.cfd_margin in :cfdMargins
            	AND s.low4w <> s.high4w AND (1 - (s.w_close - s.low4w) / (s.high4w - s.low4w)) > 0.95
                AND fvg.date < date_trunc('WEEK', s.last_updated) - INTERVAL '1 week'
                AND (s.w_high between fvg.low AND fvg.high OR s.w_low between fvg.low AND fvg.high)
            """, nativeQuery = true)
    List<FairValueGap> findWeeklyTaggedFVGsBullish95thPercentile4wLow(@Param("cfdMargins") List<Double> cfdMargins);

}