const fs = require('fs');
const path = require('path');
const xlsx = require('xlsx');
const { Pool } = require('pg');
const { pinyin } = require('pinyin-pro');
// --- 1. 配置区域 ---
const pool = new Pool({
    user: 'myuser',
    host: 'localhost',
    database: 'postgres',
    password: 'zijing',
    port: 5432,
});

const SOURCE_FOLDER = path.resolve(__dirname, 'xueyue');

// --- 2. 辅助函数 ---

/**
 * 将任何字符串转换为安全的、小写的拼音标识符。
 * @param {string} text - 输入文本 (中文或英文)
 * @returns {string} - 例如 "员工姓名" -> "yuangongxingming"
 */
function toSafePinyin(text) {
    if (!text) return '';
    // 使用 pinyin-pro，通过配置直接生成无声调的连续拼音字符串
    return pinyin(text, {
        toneType: 'none',   // 无声调
        nonZh: 'consecutive' // 将非中文字符连续保留
    }).replace(/\s/g, ''); // 移除所有空格
}

/**
 * 根据表头和样本数据推断SQL数据类型。
 * @param {string} header - 原始中文表头
 * @param {*} sampleValue - 该列的第一个数据样本
 * @returns {string} - SQL数据类型 (e.g., 'DATE', 'NUMERIC', 'VARCHAR(255)')
 */
function inferSqlType(header, sampleValue) {
    if (typeof sampleValue === 'number') {
        return 'NUMERIC';
    }
    // 默认使用文本类型
    return 'VARCHAR(255)';
}

// --- 3. 主导入函数 ---
async function importAndCreate() {
    console.log(`开始扫描文件夹: ${SOURCE_FOLDER}`);
    const client = await pool.connect();

    try {
        const allFiles = fs.readdirSync(SOURCE_FOLDER);
        const excelFiles = allFiles.filter(file => file.endsWith('.xlsx') || file.endsWith('.xls'));

        if (excelFiles.length === 0) {
            console.log('在文件夹中没有找到Excel文件。');
            return;
        }

        for (const fileName of excelFiles) {
            const filePath = path.join(SOURCE_FOLDER, fileName);
            console.log(`\n--- 正在处理文件: ${fileName} ---`);

            // 为每个文件开启一个事务
            await client.query('BEGIN');
            try {
                // a. 生成安全的表名
                const baseName = path.parse(fileName).name;
                const tableName = toSafePinyin(baseName);
                console.log(`目标数据库表: "${tableName}"`);

                // b. 读取Excel的原始数据（包括表头）
                const workbook = xlsx.readFile(filePath);
                const worksheet = workbook.Sheets[workbook.SheetNames[0]];
                // header: 1 将每一行转换为一个数组
                const dataAsArrays = xlsx.utils.sheet_to_json(worksheet, { header: 1 });

                if (dataAsArrays.length < 2) {
                    console.log('文件为空或只有表头，跳过。');
                    await client.query('COMMIT'); // 提交空事务
                    continue;
                }

                // c. 分离表头和数据，并转换为拼音
                const rawHeaders = dataAsArrays.shift(); // 取出第一行作为表头
                const pinyinColumns = rawHeaders.map(toSafePinyin);
                const sampleDataRow = dataAsArrays[0]; // 取第一行数据用于类型推断

                // d. 动态构建 CREATE TABLE 语句
                const columnDefinitions = rawHeaders.map((header, i) => {
                    const columnName = pinyinColumns[i];
                    const dataType = inferSqlType(header, sampleDataRow[i]);
                    // 为列名加上双引号，以防是SQL关键字
                    return `"${columnName}" ${dataType}`;
                }).join(', ');

                const createTableQuery = `
                    CREATE TABLE IF NOT EXISTS "${tableName}" (
                        id SERIAL PRIMARY KEY,
                        ${columnDefinitions}
                    );
                `;
                console.log('正在执行建表语句...');
                await client.query(createTableQuery);

                // e. 准备并执行 INSERT 语句
                console.log(`准备插入 ${dataAsArrays.length} 条记录...`);
                const insertColumns = pinyinColumns.map(c => `"${c}"`).join(', ');
                const placeholders = pinyinColumns.map((_, i) => `$${i + 1}`).join(', ');

                const insertQuery = `INSERT INTO "${tableName}" (${insertColumns}) VALUES (${placeholders})`;

                for (const row of dataAsArrays) {
                    // 确保行数据长度与表头匹配
                    if (row.length === pinyinColumns.length) {
                        await client.query(insertQuery, row);
                    }
                }

                await client.query('COMMIT');
                console.log(`文件 "${fileName}" 的数据已成功导入到表 "${tableName}" 中。`);

            } catch (fileError) {
                await client.query('ROLLBACK');
                console.error(`处理文件 "${fileName}" 时发生错误，该文件的所有更改已回滚:`, fileError.message);
            }
        }
    } catch (error) {
        console.error('导入流程发生严重错误:', error);
    } finally {
        await client.release();
        await pool.end();
        console.log('\n所有操作完成，数据库连接已关闭。');
    }
}

// --- 4. 运行脚本 ---
importAndCreate();