package kware.app.collector;

import com.google.gson.Gson;
import kware.app.collector.domain.config.Config;
import kware.app.collector.domain.config.ConfigDao;
import kware.app.collector.domain.metric.Metric;
import kware.app.collector.domain.metric.MetricDao;
import kware.app.node.ResourceType;
import kware.app.node.domain.NodeInfo;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 메트릭 수집 워커
 */
@RequiredArgsConstructor
@Component
public class MetricCollectTask implements Runnable {

    private final MetricDao metricDao;
    private final ConfigDao configDao;

    @Override
    public void run() {
        Gson gson = new Gson();
        LocalDateTime now = LocalDateTime.now();

        final String configKey = "PROM_URL";

        List<Config> configList = configDao.list(new Config(configKey));
        configList.forEach(config -> {
            String prometheusUrl = config.getValue();
            List<NodeInfo> dataList = collect(prometheusUrl);

            dataList.forEach(nodeInfo -> {
                String nodeName = nodeInfo.getNodeName();
                String instance = nodeInfo.getInstance();
                String data = gson.toJson(nodeInfo, NodeInfo.class);
                metricDao.insert(new Metric(now, instance, nodeName, data));
            });
        });
    }

    private List<NodeInfo> collect(String prometheusApiUrl) {
        List<NodeInfo> nodeInfos = new ArrayList<>();
        OkHttpClient httpClient = new OkHttpClient();

        try {
            Map<String, String> instances = getNodeMetricsInstance(httpClient, prometheusApiUrl, ResourceType.NODE_INFO.query);

            Map<String, Double> cpuCapacities = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.CPU_CAPACITY.query);
            Map<String, Double> memoryCapacities = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.MEMORY_CAPACITY.query);
            Map<String, Double> gpuCapacities = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.GPU_CAPACITY.query);

            Map<String, Double> cpuAllocatable = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.CPU_ALLOCATABLE.query);
            Map<String, Double> memoryAllocatable = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.MEMORY_ALLOCATABLE.query);
            Map<String, Double> gpuAllocatable = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.GPU_ALLOCATABLE.query);

            Map<String, Double> cpuUsages = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.CPU_USAGE.query);
            Map<String, Double> memoryUsages = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.MEMORY_USAGE.query);
            Map<String, Double> gpuUsages = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.GPU_USAGE.query);

            Map<String, Double> networkReceiveBytes = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.NETWORK_RECEIVE_BYTES.query);
            Map<String, Double> networkTransmitBytes = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.NETWORK_TRANSMIT_BYTES.query);
            Map<String, Double> networkReceivePackets = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.NETWORK_RECEIVE_PACKETS.query);
            Map<String, Double> networkTransmitPackets = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.NETWORK_TRANSMIT_PACKETS.query);
            Map<String, Double> networkReceiveErrs = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.NETWORK_RECEIVE_ERRS.query);
            Map<String, Double> networkTransmitErrs = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.NETWORK_TRANSMIT_ERRS.query);

            Map<String, Double> diskCapacities = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.DISK_ALLOCATABLE.query);
            Map<String, Double> diskAllocatable = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.DISK_CAPACITY.query);
            Map<String, Double> diskIoTimes = getNodeMetricsValue(httpClient, prometheusApiUrl, ResourceType.DISK_IO_TIME.query);

            String ipAddress = new URL(prometheusApiUrl).getHost();

            for (String nodeName : instances.keySet()) {
                String instance = instances.get(nodeName);
                String instanceIpAddress = instance.split(":")[0];
                if (instanceIpAddress.equals(ipAddress)) {
                    // exclude master
                    continue;
                }

                NodeInfo nodeInfo = NodeInfo.builder()
                        .instance(instance)
                        .nodeName(nodeName)
                        .cpuCapacity(cpuCapacities.getOrDefault(nodeName, 0.0))
                        .memoryCapacity(memoryCapacities.getOrDefault(nodeName, 0.0))
                        .gpuCapacity(gpuCapacities.getOrDefault(nodeName, 0.0))
                        .cpuAllocatable(cpuAllocatable.getOrDefault(nodeName, 0.0))
                        .memoryAllocatable(memoryAllocatable.getOrDefault(nodeName, 0.0))
                        .gpuAllocatable(gpuAllocatable.getOrDefault(nodeName, 0.0))
                        .cpuUsage(cpuUsages.getOrDefault(nodeName, 0.0))
                        .memoryUsage(memoryUsages.getOrDefault(nodeName, 0.0))
                        .gpuUsage(gpuUsages.getOrDefault(nodeName, 0.0))
                        .networkReceiveBytes(networkReceiveBytes.getOrDefault(nodeName, 0.0))
                        .networkTransmitBytes(networkTransmitBytes.getOrDefault(nodeName, 0.0))
                        .networkReceivePackets(networkReceivePackets.getOrDefault(nodeName, 0.0))
                        .networkTransmitPackets(networkTransmitPackets.getOrDefault(nodeName, 0.0))
                        .networkReceiveErrs(networkReceiveErrs.getOrDefault(nodeName, 0.0))
                        .networkTransmitErrs(networkTransmitErrs.getOrDefault(nodeName, 0.0))
                        .diskCapacity(diskCapacities.getOrDefault(nodeName, 0.0))
                        .diskAllocatable(diskAllocatable.getOrDefault(nodeName, 0.0))
                        .diskIoTime(diskIoTimes.getOrDefault(nodeName, 0.0))
                        .build();

                nodeInfos.add(nodeInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return nodeInfos;
    }

    private Response sendRequest2Api(OkHttpClient httpClient, String prometheusApiUrl, String query) throws IOException {
        String queryUrl = new URL(new URL(prometheusApiUrl), "api/v1/query?query=" + query).toString();
        Request request = new Request.Builder().url(queryUrl).build();
        return httpClient.newCall(request).execute();
    }

    private Map<String, String> getNodeMetricsInstance(OkHttpClient httpClient, String prometheusApiUrl, String query) throws IOException {
        try (Response response = sendRequest2Api(httpClient, prometheusApiUrl, query)) {
            if (response.body() != null) {
                JSONObject responseObject = new JSONObject(response.body().string());
                JSONArray results = responseObject.getJSONObject("data").getJSONArray("result");

                Map<String, String> nodeMetrics = new HashMap<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    JSONObject metric = result.getJSONObject("metric");

                    String nodeName = metric.getString("node");
                    String instance = metric.getString("instance");
                    nodeMetrics.put(nodeName, instance);
                }

                return nodeMetrics;
            }
        }


        return new HashMap<>();
    }

    private Map<String, Double> getNodeMetricsValue(OkHttpClient httpClient, String prometheusApiUrl, String query) throws IOException {
        try (Response response = sendRequest2Api(httpClient, prometheusApiUrl, query)) {
            if (response.body() != null) {
                JSONObject responseObject = new JSONObject(response.body().string());
                JSONArray results = responseObject.getJSONObject("data").getJSONArray("result");

                Map<String, Double> nodeMetrics = new HashMap<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    JSONObject metric = result.getJSONObject("metric");

                    String nodeName;
                    try {
                        nodeName = metric.getString("node");
                    } catch (JSONException ignored) {
                        try {
                            nodeName = metric.getString("instance");
                        } catch (JSONException ignored2) {
                            nodeName = metric.getString("exported_node");
                        }
                    }
                    double metricValue = result.getJSONArray("value").getDouble(1);
                    nodeMetrics.put(nodeName, metricValue);
                }
                return nodeMetrics;
            }
        }

        return new HashMap<>();
    }
}
