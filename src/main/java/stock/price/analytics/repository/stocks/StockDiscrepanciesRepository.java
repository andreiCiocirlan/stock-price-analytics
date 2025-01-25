package stock.price.analytics.repository.stocks;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.stocks.Stock;

import java.util.List;

@Repository
public interface StockDiscrepanciesRepository extends StockRepository {

    @Query(value = """
            SELECT s.*
            FROM public.stocks s
            JOIN high_low4w hl ON hl.start_date = DATE_TRUNC('week', s.last_updated) AND hl.ticker = s.ticker
            WHERE s.delisted_date IS NULL AND (hl.high <> s.high4w OR hl.low <> s.low4w)
            """, nativeQuery = true)
    List<Stock> findStocksWithHighLow4wDiscrepancy();

    @Query(value = """
            SELECT s.*
            FROM public.stocks s
            JOIN high_low52w hl ON hl.start_date = DATE_TRUNC('week', s.last_updated) AND hl.ticker = s.ticker
            WHERE s.delisted_date IS NULL AND (hl.high <> s.high52w OR hl.low <> s.low52w)
            """, nativeQuery = true)
    List<Stock> findStocksWithHighLow52wDiscrepancy();

    @Query(value = """
            SELECT s.*
            FROM public.stocks s
            JOIN highest_lowest hl ON hl.start_date = DATE_TRUNC('week', s.last_updated) AND hl.ticker = s.ticker
            WHERE s.delisted_date IS NULL AND (hl.high <> s.highest OR hl.low <> s.lowest)
            """, nativeQuery = true)
    List<Stock> findStocksWithHighestLowestDiscrepancy();

    @Modifying
    @Transactional
    @Query(value = """
            WITH discrepancies AS (
                SELECT s.ticker, hl.high, hl.low
                FROM public.stocks s
                JOIN high_low4w hl ON hl.start_date = DATE_TRUNC('week', s.last_updated)
                                     AND hl.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (hl.high <> s.high4w OR hl.low <> s.low4w)
            )
            UPDATE public.stocks s
            SET high4w = d.high, low4w = d.low
            FROM discrepancies d
            WHERE s.ticker = d.ticker;
            """, nativeQuery = true)
    void updateStocksWithHighLow4wDiscrepancy();

    @Modifying
    @Transactional
    @Query(value = """
            WITH discrepancies AS (
                SELECT s.ticker, hl.high, hl.low
                FROM public.stocks s
                JOIN high_low52w hl ON hl.start_date = DATE_TRUNC('week', s.last_updated)
                                     AND hl.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (hl.high <> s.high52w OR hl.low <> s.low52w)
            )
            UPDATE public.stocks s
            SET high52w = d.high, low52w = d.low
            FROM discrepancies d
            WHERE s.ticker = d.ticker
            """, nativeQuery = true)
    void updateStocksWithHighLow52wDiscrepancy();

    @Modifying
    @Transactional
    @Query(value = """
            WITH discrepancies AS (
                SELECT s.ticker, hl.high, hl.low
                FROM public.stocks s
                JOIN highest_lowest hl ON hl.start_date = DATE_TRUNC('week', s.last_updated) AND hl.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (hl.high <> s.highest OR hl.low <> s.lowest)
            )
            UPDATE public.stocks s
            SET highest = d.high, lowest = d.low
            FROM discrepancies d
            WHERE s.ticker = d.ticker AND DATE_TRUNC('week', s.last_updated) = DATE_TRUNC('week', CURRENT_DATE);
            """, nativeQuery = true)
    void updateStocksWithHighestLowestDiscrepancy();

    @Query(value = """
            SELECT s.*
            FROM public.stocks s
            JOIN weekly_prices wp ON wp.start_date = DATE_TRUNC('week', s.last_updated) AND wp.ticker = s.ticker
            WHERE s.delisted_date IS NULL AND (wp.open <> s.w_open)
            """, nativeQuery = true)
    List<Stock> findStocksWithWeeklyOpeningDiscrepancy();

    @Query(value = """
            SELECT s.*
            FROM public.stocks s
            JOIN monthly_prices wp ON wp.start_date = DATE_TRUNC('month', s.last_updated) AND wp.ticker = s.ticker
            WHERE s.delisted_date IS NULL AND (wp.open <> s.m_open)
            """, nativeQuery = true)
    List<Stock> findStocksWithMonthlyOpeningDiscrepancy();

    @Query(value = """
            SELECT s.*
            FROM public.stocks s
            JOIN quarterly_prices wp ON wp.start_date = DATE_TRUNC('quarter', s.last_updated) AND wp.ticker = s.ticker
            WHERE s.delisted_date IS NULL AND (wp.open <> s.q_open)
            """, nativeQuery = true)
    List<Stock> findStocksWithQuarterlyOpeningDiscrepancy();

    @Query(value = """
            SELECT s.*
            FROM public.stocks s
            JOIN yearly_prices wp ON wp.start_date = DATE_TRUNC('year', s.last_updated) AND wp.ticker = s.ticker
            WHERE s.delisted_date IS NULL AND (wp.open <> s.y_open)
            """, nativeQuery = true)
    List<Stock> findStocksWithYearlyOpeningDiscrepancy();

    @Modifying
    @Transactional
    @Query(value = """
            WITH discrepancies AS (
                SELECT s.ticker, wp.open
                FROM public.stocks s
                JOIN weekly_prices wp ON wp.start_date = DATE_TRUNC('week', s.last_updated) AND wp.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (wp.open <> s.w_open)
            )
            UPDATE stocks s SET w_open = dscr.open
            FROM discrepancies dscr
            WHERE s.ticker = dscr.ticker;
            """, nativeQuery = true)
    void updateStocksWithWeeklyOpeningDiscrepancy();

    @Modifying
    @Transactional
    @Query(value = """
            WITH discrepancies AS (
                SELECT s.ticker, wp.open
                FROM public.stocks s
                JOIN monthly_prices wp ON wp.start_date = DATE_TRUNC('month', s.last_updated) AND wp.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (wp.open <> s.m_open)
            )
            UPDATE stocks s SET m_open = dscr.open
            FROM discrepancies dscr
            WHERE s.ticker = dscr.ticker;
            """, nativeQuery = true)
    void updateStocksWithMonthlyOpeningDiscrepancy();

    @Modifying
    @Transactional
    @Query(value = """
            WITH discrepancies AS (
                SELECT s.ticker, wp.open
                FROM public.stocks s
                JOIN quarterly_prices wp ON wp.start_date = DATE_TRUNC('quarter', s.last_updated) AND wp.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (wp.open <> s.q_open)
            )
            UPDATE stocks s SET q_open = dscr.open
            FROM discrepancies dscr
            WHERE s.ticker = dscr.ticker;
            """, nativeQuery = true)
    void updateStocksWithQuarterlyOpeningDiscrepancy();

    @Modifying
    @Transactional
    @Query(value = """
            WITH discrepancies AS (
                SELECT s.ticker, wp.open
                FROM public.stocks s
                JOIN yearly_prices wp ON wp.start_date = DATE_TRUNC('year', s.last_updated) AND wp.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (wp.open <> s.y_open)
            )
            UPDATE stocks s SET y_open = dscr.open
            FROM discrepancies dscr
            WHERE s.ticker = dscr.ticker;
            """, nativeQuery = true)
    void updateStocksWithYearlyOpeningDiscrepancy();

    @Query(value = """
            SELECT s.ticker, 'day_discrepancy' FROM public.stocks s
            join daily_prices dp on dp.date = s.last_updated and dp.ticker = s.ticker and s.delisted_date is null
            group by s.ticker, dp.high, dp.low, dp.open, dp.close, dp.performance, s.d_high, s.d_low, s.d_open, s.d_close, s.d_performance
            having dp.high <> s.d_high or dp.low <> s.d_low or dp.open <> s.d_open or dp.close <> s.d_close or dp.performance <> s.d_performance
                UNION ALL
            SELECT s.ticker, 'week_discrepancy' FROM public.stocks s
            join weekly_prices wp on wp.start_date = DATE_TRUNC('week', s.last_updated) and wp.ticker = s.ticker and s.delisted_date is null
            group by s.ticker, wp.high, wp.low, wp.open, wp.close, wp.performance, s.w_high, s.w_low, s.w_open, s.w_close, s.w_performance
            having wp.high <> s.w_high or wp.low <> s.w_low or wp.open <> s.w_open or wp.close <> s.w_close or wp.performance <> s.w_performance
                UNION ALL
            SELECT s.ticker, 'month_discrepancy' FROM public.stocks s
            join monthly_prices mp on mp.start_date = DATE_TRUNC('month', s.last_updated) and mp.ticker = s.ticker and s.delisted_date is null
            group by s.ticker, mp.high, mp.low, mp.open, mp.close, mp.performance, s.m_high, s.m_low, s.m_open, s.m_close, s.m_performance
            having mp.high <> s.m_high or mp.low <> s.m_low or mp.open <> s.m_open or mp.close <> s.m_close or mp.performance <> s.m_performance
                UNION ALL
            SELECT s.ticker, 'quarter_discrepancy' FROM public.stocks s
            join quarterly_prices qp on qp.start_date = DATE_TRUNC('quarter', s.last_updated) and qp.ticker = s.ticker and s.delisted_date is null
            group by s.ticker, qp.high, qp.low, qp.open, qp.close, qp.performance, s.q_high, s.q_low, s.q_open, s.q_close, s.q_performance
            having qp.high <> s.q_high or qp.low <> s.q_low or qp.open <> s.q_open or qp.close <> s.q_close or qp.performance <> s.q_performance
                UNION ALL
            SELECT s.ticker, 'year_discrepancy' FROM public.stocks s
            join yearly_prices yp on yp.start_date = DATE_TRUNC('year', s.last_updated) and yp.ticker = s.ticker and s.delisted_date is null
            group by s.ticker, yp.high, yp.low, yp.open, yp.close, yp.performance, s.y_high, s.y_low, s.y_open, s.y_close, s.y_performance
            having yp.high <> s.y_high or yp.low <> s.y_low or yp.open <> s.y_open or yp.close <> s.y_close or yp.performance <> s.y_performance
                UNION ALL
            SELECT s.ticker, 'highest_lowest_discrepancy' FROM public.stocks s
            join highest_lowest hl on hl.start_date = DATE_TRUNC('week', s.last_updated) and hl.ticker = s.ticker and s.delisted_date is null
            group by s.ticker, hl.high, s.highest, hl.low, s.lowest
            having hl.high <> s.highest or hl.low <> s.lowest
                UNION ALL
            SELECT s.ticker, 'high_low52w_discrepancy' FROM public.stocks s
            join high_low52w hl on hl.start_date = DATE_TRUNC('week', s.last_updated) and hl.ticker = s.ticker and s.delisted_date is null
            group by s.ticker, hl.high, s.high52w, hl.low, s.low52w
            having hl.high <> s.high52w or hl.low <> s.low52w
                UNION ALL
            SELECT s.ticker, 'high_low4w_discrepancy' FROM public.stocks s
            join high_low4w hl on hl.start_date = DATE_TRUNC('week', s.last_updated) and hl.ticker = s.ticker and s.delisted_date is null
            group by s.ticker, hl.high, s.high4w, hl.low, s.low4w
            having hl.high <> s.high4w or hl.low <> s.low4w
            """, nativeQuery = true)
    List<Object[]> findStocksHighLowsOrHTFDiscrepancies();
}