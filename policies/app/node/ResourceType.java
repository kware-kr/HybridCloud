package kware.app.node;

/**
 * 프로메테우스 API로 수집할 메트릭 종류와 쿼리
 */
public enum ResourceType {
    NODE_INFO("kubelet_node_name"),

    // MAX
    CPU_CAPACITY("sum(kube_node_status_capacity{resource=\"cpu\",job=\"lect-metrics\"})by(exported_node)*1000"),
    MEMORY_CAPACITY("sum(kube_node_status_capacity{resource=\"memory\",job=\"lect-metrics\"})by(exported_node)"),
    GPU_CAPACITY("sum(kube_node_status_capacity{resource=\"nvidia_com_gpu\",job=\"lect-metrics\"})by(exported_node)"),

    // 가용량 (cpu millicore, memory bytes, gpu core)
    CPU_ALLOCATABLE("(sum(count(node_cpu_seconds_total{mode=\"idle\",job=\"lect-metrics\"})without(cpu,mode))by(instance,node)*1000)*((avg(rate(node_cpu_seconds_total{mode=\"idle\",job=\"lect-metrics\"}[1m]))by(instance,node)*100)*0.01)"),
    MEMORY_ALLOCATABLE("sum(node_memory_MemAvailable_bytes{job=\"lect-metrics\"})by(instance,node)"),
    GPU_ALLOCATABLE("sum(kube_node_status_allocatable{resource=\"nvidia_com_gpu\",job=\"lect-metrics\"})by(node)"),

    // 사용량 (0.0-1.0)
    CPU_USAGE("1-(avg(rate(node_cpu_seconds_total{mode=\"idle\",job=\"lect-metrics\"}[1m]))by(instance,node))"),
    MEMORY_USAGE("sum((node_memory_MemTotal_bytes{job=\"lect-metrics\"}-node_memory_MemAvailable_bytes{job=\"lect-metrics\"})/node_memory_MemTotal_bytes{job=\"lect-metrics\"})by(instance,node)"),
    GPU_USAGE("avg_over_time(DCGM_FI_DEV_GPU_UTIL[1m])"),

    // 디스크 사용량과 I/O 처리 시간
    DISK_CAPACITY("sum(node_filesystem_size_bytes{job=\"lect-metrics\"}) by (instance, node)"),
    DISK_ALLOCATABLE("sum(node_filesystem_avail_bytes{job=\"lect-metrics\"})by(instance,node)"),
    DISK_IO_TIME("sum(node_disk_io_time_seconds_total{job=\"lect-metrics\"})by(instance,node)"),

    // 네트워크
    NETWORK_RECEIVE_BYTES("sum(node_network_receive_bytes_total{job=\"lect-metrics\"})by(instance,node)"),
    NETWORK_TRANSMIT_BYTES("sum(node_network_transmit_bytes_total{job=\"lect-metrics\"})by(instance,node)"),
    NETWORK_RECEIVE_PACKETS("sum(node_network_receive_packets_total{job=\"lect-metrics\"})by(instance,node)"),
    NETWORK_TRANSMIT_PACKETS("sum(node_network_transmit_packets_total{job=\"lect-metrics\"})by(instance,node)"),
    NETWORK_RECEIVE_ERRS("sum(node_network_receive_errs_total{job=\"lect-metrics\"})by(instance,node)"),
    NETWORK_TRANSMIT_ERRS("sum(node_network_transmit_errs_total{job=\"lect-metrics\"})by(instance,node)");

    public final String query;

    ResourceType(String query) {
        this.query = query;
    }
}
