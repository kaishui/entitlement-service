const { plot } = require('nodeplotlib');

module.exports = {
  plotClusters: function(results) {
    if (!results.clusters || !Array.isArray(results.clusters)) {
      throw new Error('无效的聚类结果格式');
    }

    const traces = results.clusters
        .filter(cluster => cluster.length > 0)
        .map((cluster, i) => ({
          x: cluster.map((_, idx) => idx),
          y: cluster.map(p => p.NLR),
          type: 'scatter',
          mode: 'lines+markers',
          name: `Cluster ${i} (N=${cluster.length})`,
          line: { width: 2 }
        }));

    plot(traces, {
      title: 'Inflammatory Marker Trajectories',
      xaxis: { title: 'Time Sequence' },
      yaxis: { title: 'NLR Value' }
    });
  }
};
