// /Users/kaishui/workspace/entitlement-service/doc/local/pneumoniaAnalysis.js

const fs = require('fs');
const csv = require('csv-parser');
// --- 核心修复：使用解构赋值直接导入所需的类 ---
// --- 核心修复：使用解构赋值直接导入所需的类 ---
const { MultivariateLinearRegression } = require('ml-regression');
const { PCA } = require('ml-pca');
const { plot } = require('nodeplotlib');

class PneumoniaAnalysis {
    constructor() {
        this.patients = {};
        this.severityThresholds = {
            pct: 2,    // PCT > 2ng/mL 提示重症
            il6: 100,  // IL-6 > 100pg/mL
            nlr: 10    // NLR > 10
        };
        this.treatmentModel = null;
        this.pcaResults = null;
        this.stats = {};
    }

    /**
     * 异步加载并处理CSV数据。
     * @param {string} filePath - CSV文件的路径。
     * @returns {Promise<void>}
     */
    loadData(filePath) {
        return new Promise((resolve, reject) => {
            if (!fs.existsSync(filePath)) {
                return reject(new Error(`文件未找到: ${filePath}`));
            }

            fs.createReadStream(filePath)
                .pipe(csv())
                .on('data', (row) => this.processRow(row))
                .on('end', () => {
                    try {
                        this.analyzeData();
                        resolve();
                    } catch (analysisError) {
                        reject(analysisError);
                    }
                })
                .on('error', reject);
        });
    }

    /**
     * 处理CSV文件中的每一行数据。
     * @param {object} row - 从CSV解析出的一行数据。
     */
    processRow(row) {
        const name = row['名字'];
        if (!name) return; // 跳过没有名字的行

        const record = {
            date: new Date(row['日期']),
            pct: this.parseValue(row['PCT(化学发光法)']),
            il6: this.parseValue(row['IL-6']),
            nlr: this.parseValue(row['NLR']),
            diagnosis: row['诊断'] || '',
            isSevere: /重度|重症/.test(row['诊断'] || '')
        };

        if (isNaN(record.date.getTime())) return; // 跳过无效日期

        if (!this.patients[name]) {
            this.patients[name] = {
                records: [],
                relapseCount: 0
            };
        }

        const patientRecords = this.patients[name].records;
        // 在添加前排序，确保记录按时间顺序排列
        patientRecords.push(record);
        patientRecords.sort((a, b) => a.date - b.date);

        // 复发逻辑可以在数据完全加载后统一计算，这里暂时简化
    }

    /**
     * 将可能包含 '<' 符号的字符串值解析为浮点数。
     * @param {string} val - 输入的字符串值。
     * @returns {number} - 解析后的浮点数或 NaN。
     */
    parseValue(val) {
        if (typeof val !== 'string' || val.trim() === '') return NaN;
        const cleanedVal = val.trim();
        // 处理如 "<0.02" 的情况，取其一半作为估计值
        return cleanedVal.startsWith('<') ? parseFloat(cleanedVal.substring(1)) / 2 : parseFloat(cleanedVal);
    }

    /**
     * 在所有数据加载后进行统计分析和模型构建。
     */
    analyzeData() {
        if (!this.patients || Object.keys(this.patients).length === 0) {
            console.warn('没有可分析的患者数据。');
            return;
        }

        // 更新复发计数
        this.updateRelapseCounts();

        this.stats = {
            totalPatients: Object.keys(this.patients).length,
            severeCases: Object.values(this.patients).filter(p =>
                p.records.some(r => r.isSevere)).length,
            avgTreatmentDays: this.calculateAvgTreatmentDays(),
            relapseRate: this.calculateRelapseRate()
        };

        this.buildPredictionModel();
    }

    /**
     * 遍历所有病人记录，计算复发次数。
     */
    updateRelapseCounts() {
        const RELAPSE_INTERVAL_DAYS = 7;
        Object.values(this.patients).forEach(patient => {
            patient.relapseCount = 0;
            for (let i = 1; i < patient.records.length; i++) {
                const prevRecord = patient.records[i - 1];
                const currentRecord = patient.records[i];
                const daysBetween = (currentRecord.date - prevRecord.date) / (1000 * 60 * 60 * 24);
                if (daysBetween > RELAPSE_INTERVAL_DAYS) {
                    patient.relapseCount++;
                }
            }
        });
    }

    calculateAvgTreatmentDays() {
        const treatmentDurations = Object.values(this.patients)
            .filter(p => p.records.length > 1)
            .map(p => {
                const firstDate = p.records[0].date;
                const lastDate = p.records[p.records.length - 1].date;
                return (lastDate - firstDate) / (1000 * 60 * 60 * 24);
            });

        if (treatmentDurations.length === 0) return 0;

        const totalDays = treatmentDurations.reduce((sum, days) => sum + days, 0);
        return Math.round(totalDays / treatmentDurations.length);
    }

    calculateRelapseRate() {
        const totalPatients = Object.keys(this.patients).length;
        if (totalPatients === 0) return '0.0';

        const relapsePatients = Object.values(this.patients).filter(p => p.relapseCount > 0).length;
        return (relapsePatients / totalPatients * 100).toFixed(1);
    }

    buildPredictionModel() {
        const features = [];
        const targets = [];

        Object.values(this.patients).forEach(patient => {
            if (patient.records.length > 1) {
                const firstRecord = patient.records[0];
                // 确保所有特征值都是有效的数字
                if (!isNaN(firstRecord.pct) && !isNaN(firstRecord.il6) && !isNaN(firstRecord.nlr)) {
                    const days = (patient.records[patient.records.length - 1].date - firstRecord.date) / (1000 * 60 * 60 * 24);
                    features.push([firstRecord.pct, firstRecord.il6, firstRecord.nlr]);
                    // 优化：ml-regression 期望 y 是一个二维数组
                    targets.push([days]);
                }
            }
        });

        if (features.length < 2) {
            console.warn('数据不足（少于2个样本），无法建立预测模型。');
            this.treatmentModel = null;
            this.pcaResults = null;
            return;
        }

        try {
            // ★★★ 关键逻辑修复 ★★★
            // 构造函数的正确顺序是 (特征, 目标)，即 (features, targets)
            this.treatmentModel = new MultivariateLinearRegression(features, targets);
            console.log('回归模型创建成功。');

            // PCA分析
            try {
                const pca = new PCA(features);
                this.pcaResults = pca.predict(features, { nComponents: 2 });
                console.log('PCA分析完成。');
            } catch (pcaError) {
                console.error('PCA分析失败:', pcaError.message);
                this.pcaResults = null;
            }
        } catch (regressionError) {
            console.error('回归模型创建失败:', regressionError.message);
            this.treatmentModel = null;
            this.pcaResults = null;
        }
    }

    predictTreatmentDays(pct, il6, nlr) {
        if (!this.treatmentModel) {
            console.warn('预测模型未初始化，无法预测。');
            return null;
        }
        try {
            const prediction = this.treatmentModel.predict([[pct, il6, nlr]])[0];
            // 优化：prediction 本身是一个数组，需要取出第一个元素
            return Math.max(7, Math.round(prediction[0]));
        } catch (e) {
            console.error('预测失败:', e.message);
            return null;
        }
    }

    assessSeverity(pct, il6, nlr) {
        let score = 0;
        if (pct > this.severityThresholds.pct) score++;
        if (il6 > this.severityThresholds.il6) score++;
        if (nlr > this.severityThresholds.nlr) score++;
        return score;
    }

    visualize() {
        if (!this.pcaResults) {
            console.warn('无法可视化：PCA结果不可用。');
            return;
        }

        const plotData = [{
            x: this.pcaResults.getColumn(0),
            y: this.pcaResults.getColumn(1),
            mode: 'markers',
            type: 'scatter',
            name: 'PCA分析'
        }];

        plot(plotData, { title: '患者指标PCA降维分析' });
    }

    generateReport() {
        if (!this.stats || this.stats.totalPatients === 0) {
            return {
                summary: {
                    totalPatients: 0,
                    severeCases: 0,
                    severeRate: '0.0%',
                    avgTreatmentDays: 0,
                    relapseRate: '0.0%'
                },
                modelStatus: '未训练',
                severityThresholds: this.severityThresholds
            };
        }
        return {
            summary: {
                totalPatients: this.stats.totalPatients,
                severeCases: this.stats.severeCases,
                severeRate: `${(this.stats.severeCases / this.stats.totalPatients * 100).toFixed(1)}%`,
                avgTreatmentDays: this.stats.avgTreatmentDays,
                relapseRate: `${this.stats.relapseRate}%`
            },
            modelStatus: this.treatmentModel ? '已训练' : '未训练',
            severityThresholds: this.severityThresholds
        };
    }
}
// 使用示例
(async () => {
    try {
        const analyzer = new PneumoniaAnalysis();
        await analyzer.loadData('./filtered_results2.csv');

        const report = analyzer.generateReport();
        console.log('\n=== 肺炎数据分析报告 ===');
        console.log(`总患者数: ${report.summary.totalPatients}`);
        console.log(`重症病例: ${report.summary.severeCases} (${report.summary.severeRate})`);
        console.log(`平均治疗天数: ${report.summary.avgTreatmentDays}天`);
        console.log(`复发率: ${report.summary.relapseRate}`);
        console.log(`模型状态: ${report.modelStatus}`);

        // 示例预测
        const testCases = [
            { pct: 0.5, il6: 50, nlr: 5 },
            { pct: 3.0, il6: 150, nlr: 12 }
        ];

        if (report.modelStatus === '已训练') {
            testCases.forEach(({ pct, il6, nlr }) => {
                const days = analyzer.predictTreatmentDays(pct, il6, nlr);
                const severity = analyzer.assessSeverity(pct, il6, nlr);
                console.log(`\n预测案例 PCT=${pct}, IL-6=${il6}, NLR=${nlr}:`);
                console.log(`  -> 预测治疗天数: ${days !== null ? days + '天' : '无法预测'}`);
                console.log(`  -> 严重程度评分: ${severity}/3`);
            });
        }

        // 可视化 (默认注释掉，因为它会打开一个浏览器窗口)
        analyzer.visualize();

    } catch (error) {
        console.error('分析过程中出错:', error);
    }
})();