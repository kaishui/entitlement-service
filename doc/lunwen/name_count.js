// 读取CSV文件并统计不同姓名数量
const fs = require('fs');
const csv = require('csv-parser');

function countUniqueNames(filePath='./filter_result.csv') {
  return new Promise((resolve, reject) => {
    const names = new Set();

    // 防御性检查：验证文件是否存在
    if (!fs.existsSync(filePath)) {
      reject(new Error(`数据文件不存在: ${filePath}`));
      return;
    }

    fs.createReadStream(filePath)
      .pipe(csv())
      .on('data', (row) => {
        // 提取姓名字段并去重
        if (row['xingming']) {
          names.add(row['xingming'].trim());
        }
      })
      .on('end', () => {
        resolve(names.size);
      })
      .on('error', (err) => {
        reject(err);
      });
  });
}

// 使用示例
countUniqueNames('./filter_result.csv')
  .then(count => console.log(`不同姓名数量: ${count}`))
  .catch(err => console.error(`统计失败: ${err.message}`));
