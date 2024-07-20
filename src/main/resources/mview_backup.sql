--
-- PostgreSQL database dump
--

-- Dumped from database version 16.3 (Debian 16.3-1.pgdg120+1)
-- Dumped by pg_dump version 16.2

-- Started on 2024-07-20 10:20:20 UTC

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 232 (class 1259 OID 25780)
-- Name: daily_prices_performance_view; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.daily_prices_performance_view AS
 SELECT ticker,
    date,
    performance
   FROM public.daily_prices
  WHERE (date = ( SELECT max(daily_prices_1.date) AS max
           FROM public.daily_prices daily_prices_1
          WHERE ((daily_prices_1.ticker)::text = 'AAPL'::text)))
  WITH NO DATA;


--
-- TOC entry 228 (class 1259 OID 25235)
-- Name: high_low4w_view; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.high_low4w_view AS
 WITH cte AS (
         SELECT weekly_prices.ticker,
            weekly_prices.start_date,
            weekly_prices.low,
            weekly_prices.high,
            weekly_prices.close,
            min(weekly_prices.low) OVER (PARTITION BY weekly_prices.ticker ORDER BY weekly_prices.start_date ROWS BETWEEN 4 PRECEDING AND CURRENT ROW) AS min_low,
            max(weekly_prices.high) OVER (PARTITION BY weekly_prices.ticker ORDER BY weekly_prices.start_date ROWS BETWEEN 4 PRECEDING AND CURRENT ROW) AS max_high
           FROM public.weekly_prices
          WHERE ((weekly_prices.start_date >= date_trunc('week'::text, (CURRENT_DATE - '72800 days'::interval))) AND (weekly_prices.start_date <= date_trunc('week'::text, (CURRENT_DATE)::timestamp with time zone)))
        )
 SELECT ticker,
    (date_trunc('week'::text, (start_date)::timestamp with time zone))::date AS start_date,
    close,
    min_low AS low,
    max_high AS high
   FROM cte
  WHERE (start_date >= date_trunc('week'::text, (CURRENT_DATE - '72772 days'::interval)))
  GROUP BY ticker, (date_trunc('week'::text, (start_date)::timestamp with time zone)), min_low, max_high, close
  ORDER BY ticker, ((date_trunc('week'::text, (start_date)::timestamp with time zone))::date) DESC
  WITH NO DATA;


--
-- TOC entry 227 (class 1259 OID 25230)
-- Name: high_low52w_view; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.high_low52w_view AS
 WITH cte AS (
         SELECT weekly_prices.ticker,
            weekly_prices.start_date,
            weekly_prices.low,
            weekly_prices.high,
            weekly_prices.close,
            min(weekly_prices.low) OVER (PARTITION BY weekly_prices.ticker ORDER BY weekly_prices.start_date ROWS BETWEEN 52 PRECEDING AND CURRENT ROW) AS min_low,
            max(weekly_prices.high) OVER (PARTITION BY weekly_prices.ticker ORDER BY weekly_prices.start_date ROWS BETWEEN 52 PRECEDING AND CURRENT ROW) AS max_high
           FROM public.weekly_prices
          WHERE ((weekly_prices.start_date >= date_trunc('week'::text, (CURRENT_DATE - '72800 days'::interval))) AND (weekly_prices.start_date <= date_trunc('week'::text, (CURRENT_DATE)::timestamp with time zone)))
        )
 SELECT ticker,
    (date_trunc('week'::text, (start_date)::timestamp with time zone))::date AS start_date,
    close,
    min_low AS low,
    max_high AS high
   FROM cte
  WHERE (start_date >= date_trunc('week'::text, (CURRENT_DATE - '72436 days'::interval)))
  GROUP BY ticker, (date_trunc('week'::text, (start_date)::timestamp with time zone)), min_low, max_high, close
  ORDER BY ticker, ((date_trunc('week'::text, (start_date)::timestamp with time zone))::date) DESC
  WITH NO DATA;


--
-- TOC entry 229 (class 1259 OID 25240)
-- Name: high_low_30d_52w_weekly_count; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.high_low_30d_52w_weekly_count AS
 SELECT wp.start_date,
    sum(
        CASE
            WHEN (wp.high > hl30d.high30d) THEN 1
            ELSE NULL::integer
        END) AS new_high30d,
    sum(
        CASE
            WHEN (wp.high > hl52w.high52w) THEN 1
            ELSE NULL::integer
        END) AS new_high52w,
    sum(
        CASE
            WHEN (wp.low < hl30d.low30d) THEN 1
            ELSE NULL::integer
        END) AS new_low30d,
    sum(
        CASE
            WHEN (wp.low < hl52w.low52w) THEN 1
            ELSE NULL::integer
        END) AS new_low52w
   FROM ((public.weekly_prices wp
     LEFT JOIN public.high_low30d hl30d ON ((((hl30d.ticker)::text = (wp.ticker)::text) AND ((wp.high > hl30d.high30d) OR (wp.low < hl30d.low30d)) AND (wp.start_date = (hl30d.start_date + '7 days'::interval)))))
     LEFT JOIN public.high_low52w hl52w ON ((((hl52w.ticker)::text = (wp.ticker)::text) AND ((wp.high > hl52w.high52w) OR (wp.low < hl52w.low52w)) AND (wp.start_date = (hl52w.start_date + '7 days'::interval)))))
  WHERE ((wp.high > hl30d.high30d) OR (wp.low < hl30d.low30d) OR (wp.high > hl52w.high52w) OR (wp.low < hl52w.low52w))
  GROUP BY wp.start_date
  ORDER BY wp.start_date DESC
  WITH NO DATA;


--
-- TOC entry 233 (class 1259 OID 25835)
-- Name: latest_prices; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.latest_prices AS
 SELECT dp.id,
    dp.close,
    dp.high,
    dp.low,
    dp.open,
    dp.ticker,
    dp.date,
    dp.performance
   FROM (( SELECT daily_prices.ticker AS stock,
            max(daily_prices.date) AS max_date
           FROM public.daily_prices
          GROUP BY daily_prices.ticker) latest
     JOIN public.daily_prices dp ON ((((latest.stock)::text = (dp.ticker)::text) AND (latest.max_date = dp.date))))
  WITH NO DATA;


--
-- TOC entry 230 (class 1259 OID 25371)
-- Name: monthly_prices_performance_view; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.monthly_prices_performance_view AS
 SELECT close,
    high,
    low,
    open,
    ticker,
    end_date,
    performance,
    start_date
   FROM public.monthly_prices
  WHERE (start_date >= ( SELECT date_trunc('month'::text, (( SELECT max(monthly_prices_1.start_date) AS max
                   FROM public.monthly_prices monthly_prices_1))::timestamp with time zone) AS date_trunc))
  WITH NO DATA;


--
-- TOC entry 235 (class 1259 OID 34138)
-- Name: prev_two_months; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.prev_two_months AS
 SELECT id,
    ticker,
    start_date,
    end_date,
    high,
    low,
    open,
    close,
    performance
   FROM ( SELECT monthly_prices.id,
            monthly_prices.ticker,
            monthly_prices.start_date,
            monthly_prices.end_date,
            monthly_prices.high,
            monthly_prices.low,
            monthly_prices.open,
            monthly_prices.close,
            monthly_prices.performance,
            row_number() OVER (PARTITION BY monthly_prices.ticker ORDER BY monthly_prices.start_date DESC) AS rn
           FROM public.monthly_prices) subquery
  WHERE (rn <= 2)
  ORDER BY ticker, start_date DESC
  WITH NO DATA;


--
-- TOC entry 234 (class 1259 OID 34133)
-- Name: prev_two_weeks; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.prev_two_weeks AS
 SELECT id,
    ticker,
    start_date,
    end_date,
    high,
    low,
    open,
    close,
    performance
   FROM ( SELECT weekly_prices.id,
            weekly_prices.ticker,
            weekly_prices.start_date,
            weekly_prices.end_date,
            weekly_prices.high,
            weekly_prices.low,
            weekly_prices.open,
            weekly_prices.close,
            weekly_prices.performance,
            row_number() OVER (PARTITION BY weekly_prices.ticker ORDER BY weekly_prices.start_date DESC) AS rn
           FROM public.weekly_prices) subquery
  WHERE (rn <= 2)
  ORDER BY ticker, start_date DESC
  WITH NO DATA;


--
-- TOC entry 236 (class 1259 OID 34143)
-- Name: prev_two_years; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.prev_two_years AS
 SELECT id,
    ticker,
    start_date,
    end_date,
    high,
    low,
    open,
    close,
    performance
   FROM ( SELECT yearly_prices.id,
            yearly_prices.ticker,
            yearly_prices.start_date,
            yearly_prices.end_date,
            yearly_prices.high,
            yearly_prices.low,
            yearly_prices.open,
            yearly_prices.close,
            yearly_prices.performance,
            row_number() OVER (PARTITION BY yearly_prices.ticker ORDER BY yearly_prices.start_date DESC) AS rn
           FROM public.yearly_prices) subquery
  WHERE (rn <= 2)
  ORDER BY ticker, start_date DESC
  WITH NO DATA;


--
-- TOC entry 231 (class 1259 OID 25375)
-- Name: weekly_prices_performance_view; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.weekly_prices_performance_view AS
 SELECT close,
    high,
    low,
    open,
    ticker,
    end_date,
    performance,
    start_date
   FROM public.weekly_prices
  WHERE (start_date >= ( SELECT date_trunc('week'::text, (( SELECT max(weekly_prices_1.start_date) AS max
                   FROM public.weekly_prices weekly_prices_1))::timestamp with time zone) AS date_trunc))
  WITH NO DATA;


--
-- TOC entry 237 (class 1259 OID 34206)
-- Name: yearly_prices_performance_view; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.yearly_prices_performance_view AS
 SELECT ticker,
    start_date,
    end_date,
    open,
    high,
    low,
    close,
    performance
   FROM ( SELECT yearly_prices.ticker,
            yearly_prices.start_date,
            yearly_prices.end_date,
            yearly_prices.open,
            yearly_prices.high,
            yearly_prices.low,
            yearly_prices.close,
            yearly_prices.performance,
            row_number() OVER (PARTITION BY yearly_prices.ticker ORDER BY yearly_prices.start_date DESC) AS rn
           FROM public.yearly_prices) unnamed_subquery
  WHERE (rn = 1)
  WITH NO DATA;


-- Completed on 2024-07-20 10:20:20 UTC

--
-- PostgreSQL database dump complete
--

