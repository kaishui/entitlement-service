const {processCSV} = require('./utils/dataCleaner');
const {clusterTrajectories} = require('./analysis/trajectoryClustering');
const {plotClusters} = require('./visualization/plotTrajectories');
const fs = require('fs');
const path = require('path');

(async () => {
    try {
        // 1. 数据清洗
        const rawData = await processCSV('filter_result.csv', 'data');
        console.log(`有效记录数: ${rawData.length}`);

        // 2. 轨迹聚类
        const results = await clusterTrajectories(rawData);
        console.log(`生成聚类数: ${results.clusters.length}`);

        // 3. 可视化
        plotClusters(results);

        // 4. 输出统计摘要
        results.clusters.forEach((cluster, i) => {
            const avgNLR = cluster.reduce((sum, p) => sum + p.NLR, 0) / cluster.length;
            const avgPLR = cluster.reduce((sum, p) => sum + p.PLR, 0) / cluster.length;
            const avgIL6 = cluster.reduce((sum, p) => sum + p.il6_clean, 0) / cluster.length;

            console.log(`
      Cluster ${i} (n=${cluster.length}) 临床特征分析:
      - 炎症反应强度: ${getInflammationLevel(avgNLR, avgPLR, avgIL6)}
      - 中性粒细胞/淋巴细胞比值(NLR): ${avgNLR.toFixed(2)} ${interpretNLR(avgNLR)}
      - 血小板/淋巴细胞比值(PLR): ${avgPLR.toFixed(2)} ${interpretPLR(avgPLR)} 
      - 白介素-6水平(IL-6): ${avgIL6.toFixed(2)} pg/mL ${interpretIL6(avgIL6)}
      - 可能临床表型: ${getPhenotype(avgNLR, avgPLR, avgIL6)}
      `);
        });


// 新增解释函数
        function getInflammationLevel(nlr, plr, il6) {
            if (nlr > 9 || plr > 300 || il6 > 100) return '重度全身炎症反应';
            if (nlr > 6 || plr > 200 || il6 > 40) return '中度炎症反应';
            return '轻度局部炎症';
        }

        function interpretNLR(nlr) {
            if (nlr > 9) return '(提示严重粒细胞激活)';
            if (nlr > 6) return '(提示明显炎症反应)';
            return '(在正常波动范围内)';
        }

        function interpretPLR(plr) {
            if (plr > 300) return '(血小板消耗显著)';
            if (plr > 200) return '(微循环障碍可能)';
            return '(相对正常)';
        }

        function interpretIL6(il6) {
            if (il6 > 100) return '(细胞因子风暴风险)';
            if (il6 > 40) return '(显著炎症激活)';
            return '(基础水平)';
        }

        function getPhenotype(nlr, plr, il6) {
            if (nlr > 9 && il6 > 100) return '高炎症型（可能需免疫调节治疗）';
            if (plr > 300 && il6 > 40) return '微循环障碍型（关注血栓风险）';
            if (nlr < 6 && plr < 200) return '低炎症型（常规治疗可能足够）';
            return '混合型（需个体化评估）';
        }

        // ... existing code ...

        function saveClusterResults(clusters, outputDir = './data/results') {
            try {
                // 确保输出目录存在
                if (!fs.existsSync(outputDir)) {
                    fs.mkdirSync(outputDir, { recursive: true });
                }

                // 生成带时间戳的文件名
                const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
                const filename = `cluster_results_${timestamp}.txt`;
                const outputPath = path.join(outputDir, filename);

                // 构建输出内容
                let content = '聚类分析结果报告\n\n';
                content += `生成时间: ${new Date().toISOString()}\n\n`;

                clusters.forEach((cluster, i) => {
                    const avgNLR = cluster.reduce((sum, p) => sum + p.NLR, 0) / cluster.length;
                    const avgPLR = cluster.reduce((sum, p) => sum + p.PLR, 0) / cluster.length;
                    const avgIL6 = cluster.reduce((sum, p) => sum + p.il6_clean, 0) / cluster.length;

                    content += `Cluster ${i} (n=${cluster.length}) 临床特征分析:\n`;
                    content += `- 炎症反应强度: ${getInflammationLevel(avgNLR, avgPLR, avgIL6)}\n`;
                    content += `- 平均NLR: ${avgNLR.toFixed(2)} ${interpretNLR(avgNLR)}\n`;
                    content += `- 平均PLR: ${avgPLR.toFixed(2)} ${interpretPLR(avgPLR)}\n`;
                    content += `- 平均IL-6: ${avgIL6.toFixed(2)} pg/mL ${interpretIL6(avgIL6)}\n`;
                    content += `- 可能临床表型: ${getPhenotype(avgNLR, avgPLR, avgIL6)}\n\n`;
                });

                // 写入文件
                fs.writeFileSync(outputPath, content, 'utf8');
                console.log(`分析结果已保存至: ${outputPath}`);
                return outputPath;
            } catch (err) {
                console.error('结果保存失败:', err);
                throw err;
            }
        }

        const resultPath = saveClusterResults(results.clusters);
        console.log('TXT报告路径:', resultPath);


    } catch (err) {
        console.error('分析失败:', err);
    }
})();
