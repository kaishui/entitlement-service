const kmeans = require('ml-kmeans');
const _ = require('lodash');

module.exports = {
  clusterTrajectories: function(data, k=3) {
    // 参数验证
    if (!Array.isArray(data)) {
      throw new Error('clusterTrajectories 需要数组参数');
    }
    if (data.some(r => !r.xingming)) {
      throw new Error('数据缺少必要字段: xingming');
    }

    // 按患者分组
    const patients = _.groupBy(data, 'xingming');

    // 构建特征矩阵
    const features = Object.values(patients)
        .map(records => {
          if (!records[0] || isNaN(records[0].NLR) || isNaN(records[0].PLR)) {
            console.warn('无效患者数据:', records[0]?.xingming);
            return null;
          }
          return [
            records[0].NLR,
            records[0].PLR,
            records[0].il6_clean,
            records.length
          ];
        })
        .filter(Boolean);

    // 数据量验证
    if (features.length < 2) {
      throw new Error(`数据量不足，至少需要2个有效样本（当前有效样本数：${features.length}）`);
    }

    // 动态调整K值
    const safeK = Math.min(k, Math.max(2, Math.floor(features.length / 3)));

    // 执行聚类
    const { centroids, clusters } = kmeans(features, safeK);

    // 格式化聚类结果
    const formattedClusters = Array.from({ length: safeK }, () => []);
    clusters.forEach((clusterIdx, dataIdx) => {
      formattedClusters[clusterIdx].push(
          Object.values(patients)[dataIdx][0]
      );
    });

    return {
      patients: Object.keys(patients),
      centroids,
      clusters: formattedClusters,
      features
    };
  }
};
