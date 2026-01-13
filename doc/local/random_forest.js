const fs = require('fs');
const csv = require('csv-parser');
const {MultivariateLinearRegression} = require('ml-regression');
const {RandomForestClassifier} = require('ml-random-forest');

class PneumoniaAnalysis {
    constructor() {
        this.patients = {};
        // 模型初始化
        this.durationModel = null;
        this.severityModel = null;
    }

    /**
     * 异步加载并处理CSV数据。
     */
    async loadData(filePath) {
        const records = [];
        await new Promise((resolve, reject) => {
            fs.createReadStream(filePath)
                .pipe(csv())
                .on('data', (row) => records.push(row))
                .on('end', resolve)
                .on('error', reject);
        });

        records.forEach(row => this.processRow(row));
        this.analyzeAndBuildModels();
    }

    /**
     * 处理CSV文件中的每一行数据。
     */
    processRow(row) {
        const name = row['名字'];
        if (!name) return;

        const record = {
            date: new Date(row['日期']),
            pct: this.parseValue(row['PCT(化学发光法)']),
            il6: this.parseValue(row['IL-6']),
            nlr: this.parseValue(row['NLR']),
            isSevere: /重度|重症/.test(row['诊断'] || '') ? 1 : 0
        };

        if (isNaN(record.date.getTime())) return;

        if (!this.patients[name]) {
            this.patients[name] = {records: []};
        }
        this.patients[name].records.push(record);
    }

    parseValue(val) {
        if (typeof val !== 'string' || val.trim() === '') return NaN;
        const cleanedVal = val.trim();
        return cleanedVal.startsWith('<') ? parseFloat(cleanedVal.substring(1)) / 2 : parseFloat(cleanedVal);
    }

    /**
     * 统一进行数据分析和模型构建。
     */
    analyzeAndBuildModels() {
        // 为每个病人的记录按日期排序
        for (const patient of Object.values(this.patients)) {
            patient.records.sort((a, b) => a.date - b.date);
        }

        console.log("--- 开始构建模型 ---");
        this.buildDurationPredictionModel();
        this.buildSeverityPredictionModel();
        console.log("--- 模型构建完成 ---\n");
    }

    /**
     * 构建预测治疗周期的回归模型。
     */
    buildDurationPredictionModel() {
        console.log("1. 正在训练 [治疗周期] 预测模型 (线性回归)...");
        const features = [];
        const targets = [];

        for (const patient of Object.values(this.patients)) {
            if (patient.records.length > 1) {
                const firstRecord = patient.records[0];
                const lastRecord = patient.records[patient.records.length - 1];

                // 确保初始指标有效
                if (![firstRecord.pct, firstRecord.il6, firstRecord.nlr].some(isNaN)) {
                    const duration = (lastRecord.date - firstRecord.date) / (1000 * 60 * 60 * 24);
                    features.push([firstRecord.pct, firstRecord.il6, firstRecord.nlr]);
                    targets.push([duration]);
                }
            }
        }

        if (features.length >= 2) {
            this.durationModel = new MultivariateLinearRegression(features, targets);
            console.log("   [治疗周期] 模型训练成功！");
        } else {
            console.warn("   [治疗周期] 数据不足，无法训练模型。");
        }
    }

    /**
     * 构建预测严重程度的分类模型。
     */
    buildSeverityPredictionModel() {
        console.log("2. 正在训练 [严重程度] 预测模型 (随机森林)...");
        const features = [];
        const targets = [];

        for (const patient of Object.values(this.patients)) {
            for (const record of patient.records) {
                // 确保指标有效
                if (![record.pct, record.il6, record.nlr].some(isNaN)) {
                    features.push([record.pct, record.il6, record.nlr]);
                    targets.push(record.isSevere);
                }
            }
        }

        if (features.length >= 2) {
            // 随机森林模型参数，可以调整
            const options = {
                seed: 3,
                maxFeatures: 2,
                replacement: false,
                nEstimators: 50 // 构建50棵树
            };
            this.severityModel = new RandomForestClassifier(options);
            this.severityModel.train(features, targets);
            console.log("   [严重程度] 模型训练成功！");
        } else {
            console.warn("   [严重程度] 数据不足，无法训练模型。");
        }
    }

    /**
     * 预测治疗周期。
     * @param {number} pct
     * @param {number} il6
     * @param {number} nlr
     * @returns {number|null} 预测的天数或null
     */
    predictDuration(pct, il6, nlr) {
        if (!this.durationModel) {
            console.warn("治疗周期模型未初始化。");
            return null;
        }
        const prediction = this.durationModel.predict([[pct, il6, nlr]])[0];
        return Math.round(Math.max(1, prediction[0])); // 确保天数至少为1
    }

    /**
     * 预测是否为重症。
     * @param {number} pct
     * @param {number} il6
     * @param {number} nlr
     * @returns {string|null} "重症" 或 "非重症"
     */
    predictSeverity(pct, il6, nlr) {
        if (!this.severityModel) {
            console.warn("严重程度模型未初始化。");
            return null;
        }
        const prediction = this.severityModel.predict([[pct, il6, nlr]])[0];
        return prediction === 1 ? "重症风险" : "非重症风险";
    }
}

// --- 使用示例 ---
(async () => {
    try {
        const analyzer = new PneumoniaAnalysis();
        await analyzer.loadData('./filtered_results2.csv');

        // --- 进行预测 ---
        console.log("--- 模型预测示例 ---");

        // 案例1: 一个看起来比较轻微的病例
        const case1 = {pct: 0.5, il6: 50, nlr: 5};
        console.log(`\n案例1: PCT=${case1.pct}, IL-6=${case1.il6}, NLR=${case1.nlr}`);
        const duration1 = analyzer.predictDuration(case1.pct, case1.il6, case1.nlr);
        const severity1 = analyzer.predictSeverity(case1.pct, case1.il6, case1.nlr);
        console.log(`  -> 预测严重程度: ${severity1}`);
        console.log(`  -> 预测治疗周期: ${duration1} 天`);

        // 案例2: 一个看起来比较严重的病例
        const case2 = {pct: 3.0, il6: 150, nlr: 12};
        console.log(`\n案例2: PCT=${case2.pct}, IL-6=${case2.il6}, NLR=${case2.nlr}`);
        const duration2 = analyzer.predictDuration(case2.pct, case2.il6, case2.nlr);
        const severity2 = analyzer.predictSeverity(case2.pct, case2.il6, case2.nlr);
        console.log(`  -> 预测严重程度: ${severity2}`);
        console.log(`  -> 预测治疗周期: ${duration2} 天`);

    } catch (error) {
        console.error('分析过程中出错:', error);
    }
})();