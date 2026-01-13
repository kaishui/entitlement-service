const csv = require('csv-parser');
const fs = require('fs');
const path = require('path');
const { stringify } = require('csv-stringify/sync');

function cleanIL6(value) {
  if (value.startsWith('<')) return parseFloat(value.substring(1)) * 0.5;
  if (value.startsWith('>')) return parseFloat(value.substring(1)) * 1.2;
  return parseFloat(value);
}

function saveToCSV(data, outputDir = './data/cleaned') {
  try {
    // 确保输出目录存在
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true });
    }
    
    // 生成带时间戳的文件名
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const filename = `cleaned_data_${timestamp}.csv`;
    const outputPath = path.join(outputDir, filename);
    
    // 写入文件
    const csvData = stringify(data, {
      header: true,
      columns: ['xingming', 'songjianshijian', 'NLR', 'PLR', 'il6_clean', 'date']
    });
    fs.writeFileSync(outputPath, csvData, 'utf8');
    
    console.log(`数据已保存至: ${outputPath}`);
    return outputPath;
  } catch (err) {
    console.error('数据保存失败:', err);
    throw err;
  }
}

module.exports = {
  processCSV: function(inputPath, outputPath) {
    return new Promise((resolve, reject) => {
      const results = [];
      fs.createReadStream(inputPath)
          .pipe(csv())
          .on('data', (data) => {
            try {
              if (!data.songjianshijian || !data.xingming) {
                console.warn('缺少关键字段:', data);
                return;
              }
              if (!data.NLR || !data.PLR || !data['IL-6']) {
                console.warn('缺少炎症指标字段:', data.xingming);
                return;
              }
              results.push({
                ...data,
                NLR: parseFloat(data.NLR),
                PLR: parseFloat(data.PLR),
                il6_clean: cleanIL6(data['IL-6']),
                date: new Date(data.songjianshijian)
              });
            } catch (err) {
              console.warn('数据清洗失败:', err.message);
            }
          })
          .on('end', () => {
            if (outputPath) {
              saveToCSV(results, outputPath);
            }
            resolve(results);
          })
          .on('error', reject);
    });
  },
  cleanIL6,
  saveToCSV
};