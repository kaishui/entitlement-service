const fs = require('fs');
const csv = require('csv-parser');
const { stringify } = require('csv/sync');

const inputFile = './test.csv'; // 确保输入文件路径正确
const outputFile = './filtered_results_with_demographics.csv'; // 使用新的输出文件名

async function processCSV() {
    console.log('开始处理CSV文件...');

    if (!fs.existsSync(inputFile)) {
        console.error(`错误：输入文件 ${inputFile} 不存在`);
        return;
    }

    // 1. 使用 Promise 异步读取整个文件，使代码更清晰
    const allRows = await new Promise((resolve, reject) => {
        const data = [];
        fs.createReadStream(inputFile)
            .pipe(csv({
                headers: [
                    'date', 'name', 'gender', 'age', 'id',
                    'test_name', 'test_value', 'department',
                    'sample_type', 'sample_date', 'doctor1',
                    'doctor2', 'empty1', 'empty2', 'diagnosis',
                    'period'
                ],
                skipLines: 1 // 跳过原始文件的表头
            }))
            .on('data', (row) => data.push(row))
            .on('error', (err) => reject(err))
            .on('end', () => resolve(data));
    });

    console.log(`文件读取完成，共 ${allRows.length} 条原始记录`);

    // 2. 筛选诊断包含“肺炎”的记录
    const pneumoniaRows = allRows.filter(row =>
        row.diagnosis && (row.diagnosis.includes('肺炎') || row.diagnosis.includes('重症肺炎'))
    );

    if (pneumoniaRows.length === 0) {
        console.log('没有找到诊断为“肺炎”的记录。');
        return;
    }

    // 3. 按姓名和日期分组，整合检测项
    const groupedByVisit = {};
    pneumoniaRows.forEach(row => {
        try {
            const key = `${row.date}_${row.name}`;
            if (!groupedByVisit[key]) {
                groupedByVisit[key] = {
                    date: row.date,
                    name: row.name,
                    // ★ 新增：记录年龄和性别
                    gender: row.gender,
                    age: row.age,
                    tests: {},
                    diagnosis: row.diagnosis
                };
            }

            // 只记录需要的测试项目
            const testName = row.test_name;
            if (['PCT(化学发光法)', 'IL-6', 'NLR'].includes(testName)) {
                groupedByVisit[key].tests[testName] = row.test_value;
            }
        } catch (e) {
            console.error('分组时出错:', row, e);
        }
    });

    console.log(`分组完成，共 ${Object.keys(groupedByVisit).length} 次就诊记录`);

    // 4. 筛选出三项检测指标都存在的记录
    const filteredData = Object.values(groupedByVisit)
        .filter(item => {
            const hasAllTests = item.tests['PCT(化学发光法)'] &&
                item.tests['IL-6'] &&
                item.tests['NLR'];
            return hasAllTests;
        })
        .sort((a, b) => new Date(a.date) - new Date(b.date))
        .map(item => ({
            date: item.date,
            name: item.name,
            // ★ 新增：将年龄和性别加入最终输出对象
            gender: item.gender,
            age: item.age,
            'PCT(化学发光法)': item.tests['PCT(化学发光法)'],
            'IL-6': item.tests['IL-6'],
            'NLR': item.tests['NLR'],
            diagnosis: item.diagnosis
        }));

    console.log(`筛选后剩余 ${filteredData.length} 条完整记录`);

    if (filteredData.length === 0) {
        console.log('没有找到同时包含三项测试的记录。');
        return;
    }

    // 5. 将最终结果写入新的CSV文件
    try {
        const output = stringify(filteredData, {
            header: true,
            // ★ 新增：在文件头中加入 gender 和 age
            columns: ['date', 'name', 'gender', 'age', 'PCT(化学发光法)', 'IL-6', 'NLR', 'diagnosis']
        });

        // 使用 fs.promises.writeFile 简化异步写入
        await fs.promises.writeFile(outputFile, output);
        console.log(`成功导出 ${filteredData.length} 条记录到 ${outputFile}`);
    } catch (e) {
        console.error('生成或写入CSV时出错:', e);
    }
}

// 运行脚本
processCSV().catch(err => console.error('处理CSV时发生未捕获的错误:', err));