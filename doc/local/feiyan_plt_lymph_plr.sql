insert into IL(SONGJIANSHIJIAN, XINGMING, LI6, KEBIE)
SELECT songjianshijian, xingming, jieguo AS LI6, kebie FROM (select *
                                                             from (select *
                                                                   from zhuyuanbaijiesu6
                                                                   union all
                                                                   select *
                                                                   from baijiesu6xinde)
                                                             WHERE xiangmudaima = 'IL-6')
SELECT * FROM IL;

SELECT  * FROM (
                   select *from "2024xuechanggui1-3"
                   UNION ALL SELECT * FROM "2024xuechanggui4-6"
                   UNION ALL SELECT * FROM "2024xuechanggui7-9"
                   UNION ALL SELECT * FROM "2024xuechanggui10-12"
                   UNION ALL SELECT * FROM "2025xuechanggui1-6") WHERE xiangmudaima in ( 'PLT', 'LYMPH#', 'NLR')
                                                                   AND linchuangzhenduan = '重症肺炎' and  xingming = '汤义昌'

SELECT
    songjianshijian,
    xingming,
    SUM(CASE WHEN xiangmudaima = 'PLT' THEN CAST(jieguo AS DECIMAL(10,2)) ELSE 0 END) /
    NULLIF(SUM(CASE WHEN xiangmudaima = 'LYMPH#' THEN CAST(jieguo AS DECIMAL(10,2)) ELSE 0 END), 0) AS PLR
FROM
    (SELECT  * FROM (
                        select *from "2024xuechanggui1-3"
                        UNION ALL SELECT * FROM "2024xuechanggui4-6"
                        UNION ALL SELECT * FROM "2024xuechanggui7-9"
                        UNION ALL SELECT * FROM "2024xuechanggui10-12"
                        UNION ALL SELECT * FROM "2025xuechanggui1-6") WHERE xiangmudaima in ( 'PLT', 'LYMPH#')
                                                                        AND linchuangzhenduan like '%肺炎%')
GROUP BY
    songjianshijian,
    xingming
HAVING
    SUM(CASE WHEN xiangmudaima = 'LYMPH#' THEN CAST(jieguo AS DECIMAL(10,2)) ELSE 0 END) != 0;
-- 获取所有肺炎的结果
WITH feiyan as (
    SELECT  * FROM (
                       select *from "2024xuechanggui1-3"
                       UNION ALL SELECT * FROM "2024xuechanggui4-6"
                       UNION ALL SELECT * FROM "2024xuechanggui7-9"
                       UNION ALL SELECT * FROM "2024xuechanggui10-12"
                       UNION ALL SELECT * FROM "2025xuechanggui1-6") WHERE xiangmudaima in ( 'PLT', 'LYMPH#')
                                                                       AND linchuangzhenduan like '%肺炎%'
)
SELECT
    songjianshijian,
    xingming,
    linchuangzhenduan,
    MAX(CASE WHEN xiangmudaima = 'PLT' THEN jieguo END) AS PLT,
    MAX(CASE WHEN xiangmudaima = 'LYMPH#' THEN jieguo END) AS LYMPH
FROM feiyan
WHERE xiangmudaima IN ('PLT', 'LYMPH#')
GROUP BY songjianshijian, xingming, linchuangzhenduan
ORDER BY songjianshijian, xingming;

