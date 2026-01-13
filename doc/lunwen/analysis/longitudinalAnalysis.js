// analysis/longitudinalAnalysis.js
const _ = require('lodash');

// 手动实现线性回归（避免第三方库问题）
function linearRegression(x, y) {
    const n = x.length;
    const sumX = x.reduce((a,b) => a+b, 0);
    const sumY = y.reduce((a,b) => a+b, 0);
    const sumXY = x.map((v,i) => v*y[i]).reduce((a,b) => a+b, 0);
    const sumXX = x.map(v => v*v).reduce((a,b) => a+b, 0);

    const slope = (n*sumXY - sumX*sumY) / (n*sumXX - sumX*sumX);
    return { slope };
}

// 修复导出方式（使用CommonJS规范）
module.exports = {
    calculateSlopes: function(patientData) {
        return _.chain(patientData)
            .groupBy('xingming')
            .mapValues(records => {
                const days = records.map(r => (new Date(r.songjianshijian) - new Date(records[0].songjianshijian)) / (1000*60*60*24));
                return {
                    nlrSlope: linearRegression(days, records.map(r => r.NLR)).slope,
                    plrSlope: linearRegression(days, records.map(r => r.PLR)).slope,
                    count: records.length
                };
            })
            .value();
    }
};
