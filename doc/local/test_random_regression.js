const fs = require('fs');
const csv = require('csv-parser');
const { Matrix } = require('ml-matrix');
const regression = require('regression');

class PneumoniaAnalysis {
    constructor() {
        this.patients = {};
        this.models = {
            treatmentDays: null,
            severity: null
        };
        this.metrics = {
            treatmentDays: {
                variance: null,
                mse: null,
                rSquared: null
            },
            severity: {
                variance: null,
                accuracy: null
            }
        };
        this.thresholds = {
            pct: 2,     // ng/mL
            il6: 100,   // pg/mL
            nlr: 10     // ratio
        };
    }

    // 主处理流程
    async analyze(filePath) {
        try {
            await this.loadData(filePath);
            this.buildModels();
            return this.generateReport();
        } catch (error) {
            console.error('分析失败:', error);
            throw error;
        }
    }

    // 数据加载与预处理
    loadData(filePath) {
        return new Promise((resolve, reject) => {
            if (!fs.existsSync(filePath)) {
                reject(new Error(`数据文件不存在: ${filePath}`));
                return;
            }

            const stream = fs.createReadStream(filePath)
                .pipe(csv())
                .on('data', (row) => this.processRow(row))
                .on('end', () => {
                    this.preprocessData();
                    resolve();
                })
                .on('error', reject);
        });
    }

    processRow(row) {
        const name = row.name;
        const record = {
            date: new Date(row.date),
            pct: this.parseValue(row['PCT(化学发光法)']),
            il6: this.parseValue(row['IL-6']),
            nlr: this.parseValue(row['NLR']),
            age: parseInt(row.age) || 0,
            gender: row.gender,
            diagnosis: row.diagnosis,
            isSevere: this.isSevereCase(row.diagnosis)
        };

        if (!this.patients[name]) {
            this.patients[name] = {
                records: [],
                demographic: { age: record.age, gender: record.gender },
                relapseCount: 0
            };
        }

        // 复发检测（间隔>7天视为新发作）
        const lastRecord = this.patients[name].records.slice(-1)[0];
        if (lastRecord && (record.date - lastRecord.date) > 7 * 86400000) {
            this.patients[name].relapseCount++;
        }

        this.patients[name].records.push(record);
    }

    parseValue(val) {
        if (!val) return 0;
        if (val.startsWith('<')) return parseFloat(val.substring(1)) / 2;
        return parseFloat(val);
    }

    isSevereCase(diagnosis) {
        return /重度|重症/.test(diagnosis);
    }

    preprocessData() {
        Object.values(this.patients).forEach(patient => {
            patient.records.sort((a, b) => a.date - b.date);
        });
    }

    // 模型构建
    buildModels() {
        this.buildTreatmentDayModel();
        this.buildSeverityModel();
    }

    buildTreatmentDayModel() {
        const data = [];

        Object.values(this.patients).forEach(patient => {
            if (patient.records.length > 1) {
                const first = patient.records[0];
                const last = patient.records[patient.records.length - 1];
                const days = (last.date - first.date) / (24 * 60 * 60 * 1000);

                data.push([
                    first.pct,
                    first.il6,
                    first.nlr,
                    patient.demographic.age / 100, // 年龄归一化
                    days
                ]);
            }
        });

        if (data.length >= 10) {
            try {
                const result = regression.linear(data, {
                    order: [0, 1, 2, 3],
                    precision: 6
                });

                this.models.treatmentDays = result;
                this.calculateMetrics('treatmentDays', data);
            } catch (e) {
                console.error('治疗天数模型构建失败:', e);
            }
        } else {
            console.warn(`治疗天数模型样本不足: ${data.length}/10`);
        }
    }

    buildSeverityModel() {
        const data = [];

        Object.values(this.patients).forEach(patient => {
            patient.records.forEach(record => {
                data.push([
                    record.pct,
                    record.il6,
                    record.nlr,
                    record.isSevere ? 1 : 0
                ]);
            });
        });

        if (data.length >= 20) {
            try {
                // 改用ml-logistic-regression库
                const LogisticRegression = require('ml-logistic-regression');
                const features = data.map(row => row.slice(0, 3));
                const labels = data.map(row => row[3]);

                const model = new LogisticRegression({
                    numSteps: 1000,
                    learningRate: 0.05
                });
                model.train(features, labels);

                this.models.severity = {
                    predict: (x) => model.predict(x),
                    equation: model.toJSON()
                };

                this.calculateMetrics('severity', data);
            } catch (e) {
                console.error('严重程度模型构建失败:', e);
            }
        } else {
            console.warn(`严重程度模型样本不足: ${data.length}/20`);
        }
    }
    // 指标计算
    calculateMetrics(modelType, data) {
        const model = this.models[modelType];
        const predictions = data.map(row => model.predict(row.slice(0, -1)));
        const actuals = data.map(row => row[row.length - 1]);

        // 计算均值
        const mean = actuals.reduce((sum, val) => sum + val, 0) / actuals.length;

        // 总方差
        const totalVariance = actuals.reduce(
            (sum, val) => sum + Math.pow(val - mean, 2), 0) / actuals.length;

        // 残差方差
        const residualVariance = predictions.reduce(
            (sum, pred, i) => sum + Math.pow(pred - actuals[i], 2), 0) / actuals.length;

        this.metrics[modelType].variance = {
            total: totalVariance,
            residual: residualVariance,
            explained: totalVariance - residualVariance
        };

        // 模型特定指标
        if (modelType === 'treatmentDays') {
            this.metrics.treatmentDays.mse = residualVariance;
            this.metrics.treatmentDays.rSquared = 1 - (residualVariance / totalVariance);
        } else {
            const correct = predictions.filter((pred, i) =>
                Math.round(pred) === actuals[i]).length;
            this.metrics.severity.accuracy = correct / actuals.length;
        }
    }

    // 预测接口
    predictTreatmentDays(features) {
        if (!this.models.treatmentDays) {
            throw new Error('治疗天数模型未初始化');
        }
        return this.models.treatmentDays.predict([
            features.pct,
            features.il6,
            features.nlr,
            features.age / 100
        ]);
    }

    assessSeverity(features) {
        if (!this.models.severity) {
            throw new Error('严重程度模型未初始化');
        }
        return this.models.severity.predict([
            features.pct,
            features.il6,
            features.nlr
        ]);
    }

    // 报告生成
    generateReport() {
        const totalPatients = Object.keys(this.patients).length;
        const severeCases = Object.values(this.patients)
            .filter(p => p.records.some(r => r.isSevere)).length;
        const relapseCases = Object.values(this.patients)
            .filter(p => p.relapseCount > 0).length;

        return {
            statistics: {
                totalPatients,
                severeCases,
                relapseCases,
                severeRate: `${(severeCases / totalPatients * 100).toFixed(1)}%`,
                relapseRate: `${(relapseCases / totalPatients * 100).toFixed(1)}%`
            },
            models: {
                treatmentDays: this.models.treatmentDays ? {
                    equation: this.models.treatmentDays.equation,
                    rSquared: this.metrics.treatmentDays.rSquared.toFixed(3),
                    mse: this.metrics.treatmentDays.mse.toFixed(1)
                } : null,
                severity: this.models.severity ? {
                    equation: this.models.severity.equation,
                    accuracy: this.metrics.severity.accuracy.toFixed(3)
                } : null
            },
            thresholds: this.thresholds
        };
    }
}

// 使用示例
(async () => {
    try {
        const analyzer = new PneumoniaAnalysis();
        const report = await analyzer.analyze('./filtered_results_with_demographics.csv');

        console.log('===== 肺炎数据分析报告 =====');
        console.log(report);

        // 示例预测
        const testCase = {
            pct: 1.5,
            il6: 80,
            nlr: 8,
            age: 60
        };

        console.log('\n预测案例:', testCase);
        console.log('预计治疗天数:', analyzer.predictTreatmentDays(testCase).toFixed(1) + '天');
        console.log('严重程度评分:', analyzer.assessSeverity(testCase).toFixed(3));

    } catch (error) {
        console.error('分析失败:', error.message);
    }
})();
