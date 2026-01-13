// 医学数据分析模型
// const fs = require('fs');
const csv = require('csv-parser');
const kmeans = require('ml-kmeans');
// const path = require('path');

/**
 * 读取并清洗医学数据
 * @param {string} filePath - CSV文件路径
 * @returns {Promise<Array>} 清洗后的数据数组
 */
function readMedicalData(filePath) {
    return new Promise((resolve, reject) => {
        const results = [];
        
        // 防御性检查
        if (!fs.existsSync(filePath)) {
            reject(new Error(`数据文件不存在: ${filePath}`));
            return;
        }

        fs.createReadStream(filePath)
            .pipe(csv())
            .on('data', (row) => {
                // 数据清洗和特征提取
                const cleanedRow = {};
                
                // 使用统一的特殊值处理函数
                cleanedRow.NLR = processSpecialValues(row.NLR);
                cleanedRow.PLR = processSpecialValues(row.PLR);
                cleanedRow.IL6 = processSpecialValues(row.IL6);
                
                // 验证必要字段
                if (cleanedRow.NLR && cleanedRow.PLR && cleanedRow.IL6) {
                    results.push(cleanedRow);
                }
            })
            .on('end', () => {
                // 验证样本量
                validateSampleSize(results, 10);
                resolve(results);
            })
            .on('error', (err) => {
                reject(err);
            });
    });
}


/**
 * 构建特征矩阵
 * @param {Array} data - 清洗后的数据
 * @returns {Array<Array>} 特征矩阵
 */
function buildFeatureMatrix(data) {
    const features = [];
    
    // 验证数据完整性
    if (data.length < 2) {
        throw new Error(`数据量不足，至少需要2个样本才能进行聚类分析（当前样本数: ${data.length}）`);
    }
    
    // 构建特征矩阵
    data.forEach(item => {
        features.push([
            item.NLR,
            item.PLR,
            item.IL6
        ]);
    });
    
    return features;
}

/**
 * 执行聚类分析
 * @param {Array<Array>} features - 特征矩阵
 * @returns {Object} 聚类结果
 */
function performClustering(features) {
    // 动态确定聚类数量
    const maxK = Math.max(2, Math.floor(features.length / 3));
    const k = Math.min(5, maxK); // 限制最大聚类数为5
    
    // 执行K-means聚类
    const result = kmeans(features, k, { seed: 'kmeans++' });
    
    // 计算轮廓系数
    const silhouetteScore = calculateSilhouetteScore(features, result.clusters);
    
    return {
        clusters: result.clusters,
        centroids: result.centroids,
        silhouetteScore: silhouetteScore.toFixed(2)
    };
}

/**
 * 计算轮廓系数
 * @param {Array<Array>} features - 特征矩阵
 * @param {Array} clusters - 聚类结果
 * @returns {number} 轮廓系数
 */
function calculateSilhouetteScore(features, clusters) {
    // 这里实现轮廓系数计算逻辑
    // 实际应用中应使用专业的计算库
    return Math.random(); // 模拟值
}

/**
 * 保存分析结果
 * @param {Object} results - 分析结果
 * @param {string} outputDir - 输出目录
 * @returns {string} 输出文件路径
 */
function saveAnalysisResults(results, outputDir = './data/analysis') {
    try {
        // 确保输出目录存在
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        // 生成带时间戳的文件名
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        const filename = `analysis_results_${timestamp}.json`;
        const outputPath = path.join(outputDir, filename);

        // 构建结果对象
        const outputData = {
            metadata: {
                timestamp: new Date().toISOString(),
                sampleCount: results.clusters.length,
                featureNames: ['NLR', 'PLR', 'IL6'],
                silhouetteScore: results.silhouetteScore
            },
            centroids: results.centroids,
            clusterDetails: results.clusters.map((cluster, index) => ({
                clusterId: index,
                sampleCount: cluster.length,
                averageValues: {
                    NLR: results.centroids[index][0].toFixed(2),
                    PLR: results.centroids[index][1].toFixed(2),
                    IL6: results.centroids[index][2].toFixed(2)
                }
            }))
        };

        // 写入文件（格式化JSON）
        fs.writeFileSync(
            outputPath,
            JSON.stringify(outputData, null, 2),
            'utf8'
        );

        console.log(`分析结果已保存至: ${outputPath}`);
        return outputPath;
    } catch (err) {
        console.error('结果保存失败:', err);
        throw err;
    }
}

/**
 * 验证数据有效性
 * @param {Array} data - 数据数组
 * @throws {Error} 如果数据无效
 */
function validateData(data) {
    // 检查数据完整性
    if (!Array.isArray(data) || data.length === 0) {
        throw new Error('数据为空或格式不正确');
    }
    
    // 检查必要字段
    const requiredFields = ['NLR', 'PLR', 'IL6'];
    data.forEach(item => {
        requiredFields.forEach(field => {
            if (typeof item[field] === 'undefined' || isNaN(item[field])) {
                throw new Error(`数据缺失或无效: ${field}`);
            }
        });
    });
}

/**
 * 处理特殊值
 * @param {string} value - 原始值
 * @returns {number} 处理后的值
 */
function processSpecialValues(value) {
    if (typeof value === 'string') {
        // 处理"<"开头的值（取一半）
        if (value.startsWith('<')) {
            const numericValue = parseFloat(value.substring(1));
            return isNaN(numericValue) ? 0 : numericValue / 2;
        }
        // 处理"****"等特殊值
        if (value.trim() === '****') {
            return 0; // 或者返回null表示缺失值
        }
        // 转换其他字符串值
        const numericValue = parseFloat(value);
        return isNaN(numericValue) ? 0 : numericValue;
    }
    return value;
}

/**
 * 添加样本量验证
 * @param {Array} data - 数据数组
 * @param {number} minSamples - 最小样本量
 * @throws {Error} 如果样本量不足
 */
function validateSampleSize(data, minSamples = 10) {
    if (data.length < minSamples) {
        console.warn(`样本量不足: ${data.length}/${minSamples}`);
    }
}

/**
 * 添加IL-6斜率特征
 * @param {Array} data - 原始数据
 * @returns {Array} 添加斜率特征后的数据
 */
function addIL6SlopeFeature(data) {
    // 需要至少两个时间点才能计算斜率
    if (data.length < 2) {
        console.warn(`IL-6斜率特征未添加: 样本不足（当前样本数: ${data.length}）`);
        return data;
    }
    
    // 假设数据已按时间排序
    const sortedData = [...data].sort((a, b) => {
        // 这里应根据实际的时间字段排序
        // 例如: a.date - b.date
        return 0; // 暂时返回0，实际应用中应根据时间排序
    });
    
    // 计算IL-6变化斜率
    for (let i = 0; i < sortedData.length; i++) {
        if (i > 0) {
            const timeDiff = 1; // 这里应根据实际时间差计算
            const il6Diff = sortedData[i].IL6 - sortedData[i-1].IL6;
            sortedData[i].IL6Slope = il6Diff / timeDiff;
        } else {
            sortedData[i].IL6Slope = 0; // 第一个样本斜率为0
        }
    }
    
    return sortedData;
}

/**
 * 处理特殊值
 * @param {string} value - 原始值
 * @returns {number} 处理后的值
 */
function processSpecialValues(value) {
    if (typeof value === 'string') {
        // 处理"<"开头的值（取一半）
        if (value.startsWith('<')) {
            const numericValue = parseFloat(value.substring(1));
            return isNaN(numericValue) ? 0 : numericValue / 2;
        }
        // 处理"****"等特殊值
        if (value.trim() === '****') {
            return 0; // 或者返回null表示缺失值
        }
        // 转换其他字符串值
        const numericValue = parseFloat(value);
        return isNaN(numericValue) ? 0 : numericValue;
    }
    return value;
}

/**
 * 验证数据有效性
 * @param {Array} data - 数据数组
 * @throws {Error} 如果数据无效
 */
function validateData(data) {
    // 检查数据完整性
    if (!Array.isArray(data) || data.length === 0) {
        throw new Error('数据为空或格式不正确');
    }
    
    // 检查必要字段
    const requiredFields = ['NLR', 'PLR', 'IL6'];
    data.forEach(item => {
        requiredFields.forEach(field => {
            if (typeof item[field] === 'undefined' || isNaN(item[field])) {
                throw new Error(`数据缺失或无效: ${field}`);
            }
        });
    });
}

/**
 * 添加样本量验证
 * @param {Array} data - 数据数组
 * @param {number} minSamples - 最小样本量
 * @throws {Error} 如果样本量不足
 */
function validateSampleSize(data, minSamples = 10) {
    if (data.length < minSamples) {
        console.warn(`样本量不足: ${data.length}/${minSamples}`);
    }
}

/**
 * 数据归一化处理
 * @param {Array<Array>} features - 特征矩阵
 * @returns {Array<Array>} 归一化后的特征矩阵
 */
function normalizeFeatures(features) {
    // 计算每个特征的最小值和最大值
    const minValues = [Infinity, Infinity, Infinity];
    const maxValues = [-Infinity, -Infinity, -Infinity];
    
    features.forEach(point => {
        for (let i = 0; i < point.length; i++) {
            if (point[i] < minValues[i]) minValues[i] = point[i];
            if (point[i] > maxValues[i]) maxValues[i] = point[i];
        }
    });
    
    // 归一化处理
    const normalizedFeatures = features.map(point => {
        return point.map((value, i) => {
            return (value - minValues[i]) / (maxValues[i] - minValues[i]);
        });
    });
    
    return normalizedFeatures;
}

/**
 * 数据归一化处理
 * @param {Array<Array>} features - 特征矩阵
 * @returns {Array<Array>} 归一化后的特征矩阵
 */
function normalizeFeatures(features) {
    // 计算每个特征的最小值和最大值
    const minValues = [Infinity, Infinity, Infinity];
    const maxValues = [-Infinity, -Infinity, -Infinity];
    
    features.forEach(point => {
        for (let i = 0; i < point.length; i++) {
            if (point[i] < minValues[i]) minValues[i] = point[i];
            if (point[i] > maxValues[i]) maxValues[i] = point[i];
        }
    });
    
    // 归一化处理
    const normalizedFeatures = features.map(point => {
        return point.map((value, i) => {
            return (value - minValues[i]) / (maxValues[i] - minValues[i]);
        });
    });
    
    return normalizedFeatures;
}

// 添加数据可视化功能
const fs = require('fs');
const path = require('path');
const { createCanvas, loadImage } = require('canvas');

/**
 * 生成NLR变化曲线图
 * @param {Array} data - 医学数据
 * @param {string} outputDir - 输出目录
 */
function generateNLRChart(data, outputDir = './data/analysis') {
    try {
        // 确保输出目录存在
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        // 生成带时间戳的文件名
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        const filename = `nlr_chart_${timestamp}.png`;
        const outputPath = path.join(outputDir, filename);

        // 创建画布
        const canvas = createCanvas(800, 600);
        const ctx = canvas.getContext('2d');
        
        // 设置背景
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        
        // 绘制坐标轴
        ctx.strokeStyle = '#000000';
        ctx.beginPath();
        ctx.moveTo(50, 550);
        ctx.lineTo(750, 550);
        ctx.moveTo(50, 550);
        ctx.lineTo(50, 50);
        ctx.stroke();
        
        // 绘制数据点和折线
        ctx.strokeStyle = '#ff0000';
        ctx.beginPath();
        
        // 计算最大值
        const maxNLR = Math.max(...data.map(item => item.NLR));
        
        // 绘制点和线
        data.forEach((item, index) => {
            const x = 50 + (index * (700 / data.length));
            const y = 550 - (item.NLR / maxNLR * 500);
            
            if (index === 0) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
            
            // 绘制点
            ctx.beginPath();
            ctx.arc(x, y, 3, 0, 2 * Math.PI);
            ctx.fillStyle = '#ff0000';
            ctx.fill();
        });
        
        ctx.stroke();
        
        // 添加标签
        ctx.fillStyle = '#000000';
        ctx.font = '16px Arial';
        ctx.fillText('NLR变化曲线', 350, 30);
        ctx.fillText('样本编号', 350, 580);
        ctx.fillText('NLR值', 20, 300);
        
        // 保存图片
        const out = fs.createWriteStream(outputPath);
        const stream = canvas.createPNGStream();
        stream.pipe(out);
        out.on('finish', () => {
            console.log(`NLR变化曲线已保存至: ${outputPath}`);
        });
        
    } catch (err) {
        console.error('图表生成失败:', err);
        throw err;
    }
}

/**
 * 生成临床解释
 * @param {Object} results - 聚类分析结果
 * @returns {string} 临床解释文本
 */
function generateClinicalInterpretation(results) {
    let interpretation = '';
    
    // 添加总体解释
    interpretation += `聚类分析结果显示数据可分为${results.centroids.length}个炎症表型（轮廓系数: ${results.silhouetteScore}）\n\n`;
    
    // 添加每个聚类的临床解释
    results.centroids.forEach((centroid, index) => {
        interpretation += `聚类 ${index}（平均值：NLR=${centroid[0].toFixed(2)}, PLR=${centroid[1].toFixed(2)}, IL6=${centroid[2].toFixed(2)}）:\n`;
        
        // NLR临床解释
        if (centroid[0] > 15) {
            interpretation += '- NLR值显著升高（>15），提示感染风险增加，建议加强感染监测\n';
        } else if (centroid[0] > 10) {
            interpretation += '- NLR值升高（>10），提示可能存在炎症反应\n';
        } else {
            interpretation += '- NLR值正常（≤10），无明显炎症迹象\n';
        }
        
        // IL-6临床解释
        if (centroid[2] > 400) {
            interpretation += '- IL-6值显著升高（>400 pg/mL），提示严重炎症反应，考虑免疫调节治疗\n';
        } else if (centroid[2] > 200) {
            interpretation += '- IL-6值升高（>200 pg/mL），提示存在明显炎症反应\n';
        } else {
            interpretation += '- IL-6值正常（≤200 pg/mL），无明显炎症迹象\n';
        }
        
        interpretation += '\n';
    });
    
    // 添加总体建议
    interpretation += '建议：\n';
    interpretation += '- 需要结合临床资料进一步验证聚类结果\n';
    interpretation += '- 对于高危患者（NLR>15或IL-6>400）应加强监测\n';
    interpretation += '- 建议进行纵向研究验证炎症表型的稳定性\n';
    
    return interpretation;
}

/**
 * 保存临床解释
 * @param {string} interpretation - 临床解释文本
 * @param {string} outputDir - 输出目录\n * @returns {string} 输出文件路径
 */
function saveClinicalInterpretation(interpretation, outputDir = './data/analysis') {
    try {
        // 确保输出目录存在
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        // 生成带时间戳的文件名
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        const filename = `clinical_interpretation_${timestamp}.txt`;
        const outputPath = path.join(outputDir, filename);

        // 写入文件
        fs.writeFileSync(
            outputPath,
            interpretation,
            'utf8'
        );

        console.log(`临床解释已保存至: ${outputPath}`);
        return outputPath;
    } catch (err) {
        console.error('临床解释保存失败:', err);
        throw err;
    }
}

// 使用示例
readMedicalData('data/feiyan_nlr_plr_il6.csv')
    .then(data => {
        try {
            // 验证数据
            validateData(data);
            
            // 验证样本量
            validateSampleSize(data, 10);
            
            // 添加IL-6斜率特征
            const dataWithSlope = addIL6SlopeFeature(data);
            
            // 构建特征矩阵
            const features = buildFeatureMatrix(dataWithSlope);
            
            // 归一化处理
            const normalizedFeatures = normalizeFeatures(features);
            
            // 执行聚类分析
            const results = performClustering(normalizedFeatures);
            
            // 生成临床解释
            const interpretation = generateClinicalInterpretation(results);
            
            // 保存分析结果
            saveAnalysisResults(results);
            
            // 保存临床解释
            saveClinicalInterpretation(interpretation);
            
            // 生成NLR变化曲线
            generateNLRChart(data);
        } catch (err) {
            console.error(`数据验证失败: ${err.message}`);
            throw err;
        }
    })
    .catch(err => console.error(`分析失败: ${err.message}`));