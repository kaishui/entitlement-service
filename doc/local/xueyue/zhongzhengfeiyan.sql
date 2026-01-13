WITH feiyan as (
    SELECT  * FROM (
                       select *from "2024xuechanggui1-3"
                       UNION ALL SELECT * FROM "2024xuechanggui4-6"
                       UNION ALL SELECT * FROM "2024xuechanggui7-9"
                       UNION ALL SELECT * FROM "2024xuechanggui10-12"
                       UNION ALL SELECT * FROM "2025xuechanggui1-6") WHERE xiangmudaima in ( 'PLT', 'LYMPH#', 'NLR')
                                                                       AND linchuangzhenduan like '肺炎'
),
     pivoted_data AS (
         SELECT
             songjianshijian,
             xingming,
             linchuangzhenduan,
             MAX(CASE WHEN xiangmudaima = 'PLT' THEN jieguo END) AS PLT,
             MAX(CASE WHEN xiangmudaima = 'LYMPH#' THEN jieguo END) AS LYMPH,
             MAX(CASE WHEN xiangmudaima = 'NLR' THEN jieguo END) AS NLR
         FROM feiyan
         WHERE xiangmudaima IN ('PLT', 'LYMPH#', 'NLR')
         GROUP BY songjianshijian, xingming, linchuangzhenduan
     ), il as (
         select songjianshijian,
                xingming,
                linchuangzhenduan,  kebie,            MAX(CASE WHEN xiangmudaima = 'IL-6' THEN jieguo END) AS IL6
         from baijiesu6xinde where xiangmudaima = 'IL-6' group by songjianshijian, xingming, linchuangzhenduan,kebie
), xuechanggui as (
SELECT
    songjianshijian,
    xingming,
    linchuangzhenduan,
    NLR,
    PLT,
    LYMPH as "LYMPH#",
    ROUND(
            CASE
                WHEN PLT IS NOT NULL AND LYMPH IS NOT NULL
                    AND TRIM(PLT) != '' AND TRIM(LYMPH) != ''
                    AND NULLIF(REGEXP_REPLACE(PLT, '[<>]', '', 'g'), '') IS NOT NULL
                    AND NULLIF(REGEXP_REPLACE(LYMPH, '[<>]', '', 'g'), '') IS NOT NULL
                    AND CAST(NULLIF(REGEXP_REPLACE(LYMPH, '[<>]', '', 'g'), '') AS NUMERIC) != 0
                    THEN
                    CAST(NULLIF(REGEXP_REPLACE(PLT, '[<>]', '', 'g'), '') AS NUMERIC) /
                    CAST(NULLIF(REGEXP_REPLACE(LYMPH, '[<>]', '', 'g'), '') AS NUMERIC)
                ELSE NULL
                END,
            2) AS PLR
FROM pivoted_data
ORDER BY  xingming, songjianshijian)
select il.songjianshijian, il.xingming, il.kebie, nlr, plr, il6, il.linchuangzhenduan from xuechanggui left join il on xuechanggui.songjianshijian = il.songjianshijian
                                              and xuechanggui.xingming = il.xingming
                                              and xuechanggui.linchuangzhenduan = il.linchuangzhenduan
where il6 is not null order by xingming, songjianshijian
