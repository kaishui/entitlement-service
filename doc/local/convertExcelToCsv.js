const fs = require('fs');
const path = require('path');
const XLSX = require('xlsx');

// 配置参数
const INPUT_FOLDER = './xueyue'; // Excel文件输入目录
const OUTPUT_FOLDER = './output_csv'; // CSV输出目录

// 确保输出目录存在
if (!fs.existsSync(OUTPUT_FOLDER)) {
  fs.mkdirSync(OUTPUT_FOLDER, { recursive: true });
}

// 获取所有Excel文件
const excelFiles = fs.readdirSync(INPUT_FOLDER).filter(file => {
  return ['.xlsx', '.xls'].includes(path.extname(file).toLowerCase());
});

// 转换每个文件
excelFiles.forEach(file => {
  try {
    // 读取Excel文件
    const workbook = XLSX.readFile(path.join(INPUT_FOLDER, file));

    // 遍历每个工作表
    workbook.SheetNames.forEach(sheetName => {
      const worksheet = workbook.Sheets[sheetName];

      // 转换为CSV
      const csvData = XLSX.utils.sheet_to_csv(worksheet);

      // 生成输出文件名 (原文件名_工作表名.csv)
      const outputName = `${path.basename(file, path.extname(file))}_${sheetName}.csv`;

      // 写入CSV文件
      fs.writeFileSync(path.join(OUTPUT_FOLDER, outputName), csvData);
      console.log(`已转换: ${file} -> ${outputName}`);
    });
  } catch (err) {
    console.error(`转换失败 ${file}:`, err.message);
  }
});

console.log('转换完成！');
