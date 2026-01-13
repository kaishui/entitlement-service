const fs = require('fs');
const XLSX = require('xlsx');
const { processCSV } = require('./utils/dataCleaner');

// 科室颜色映射配置
const DEPT_COLORS = {
    '内四科(呼吸)': 'FFFF00', // 黄色
    '呼吸与危重症医学科': 'FFFF00', // 黄色（同一科室）
    'ICU': 'FF0000', // 红色
    'default': 'FFFFFF' // 白色
};

(async () => {
    try {
        // 1. 数据清洗
        const rawData = await processCSV('./filter_result.csv');

        // 2. 按患者分组
        const patients = rawData.reduce((acc, record) => {
            if (!acc[record.xingming]) {
                acc[record.xingming] = [];
            }
            acc[record.xingming].push(record);
            return acc;
        }, {});

        // 3. 创建工作簿
        const wb = XLSX.utils.book_new();

        // 4. 准备工作表数据
        const wsData = [
            ['姓名', '科室', 'NLR', 'PLR', 'IL-6', '送检时间'], // 表头
            ...Object.entries(patients).flatMap(([name, records]) =>
                records.map(record => [
                    name,
                    record.kebie,
                    record.NLR,
                    record.PLR,
                    record.il6_clean,
                    record.songjianshijian
                ])
            )
        ];

        // 5. 创建工作表
        const ws = XLSX.utils.aoa_to_sheet(wsData);

        // 6. 添加单元格样式
        Object.keys(ws).forEach(key => {
            if (key.startsWith('B')) { // 科室列
                const cell = ws[key];
                const dept = cell.v;
                cell.s = {
                    fill: {
                        patternType: 'solid',
                        fgColor: { rgb: DEPT_COLORS[dept] || DEPT_COLORS.default }
                    }
                };
            }
        });

        // 7. 添加工作表到工作簿
        XLSX.utils.book_append_sheet(wb, ws, '患者数据');

        // 8. 保存Excel文件
        XLSX.writeFile(wb, './output/patient_data.xlsx');
        console.log('Excel文件已生成: output/patient_data.xlsx');

    } catch (err) {
        console.error('生成Excel失败:', err);
    }
})();
