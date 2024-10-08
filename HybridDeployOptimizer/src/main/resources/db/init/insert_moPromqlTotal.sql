INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(1, '노드 기본정보', '노드 Labels', 'kube_node_info * on(node, ref_id) group_right(internal_ip) kube_node_labels', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "labels": "$.metric.startWith(label)", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(2, '노드 기본정보', '노드 unschedule', 'kube_node_info * on(node,ref_id) group_right(internal_ip) (kube_node_spec_unschedulable == 1)', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]", "unscheduable": "$.value[1]"}'::jsonb, 'taint와는 다르지만 이게 1인 노드는 taint에도 등록되므로 제거한다.', NULL, 'N', 'Y');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(3, '노드 기본정보', '노드 기본정보', 'kube_node_info', '{"os": "$.metric.os_image", "key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.instance", "kube_ver": "$.metric.kubelet_version", "timestamp": "$.value[0]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(4, '노드 기본정보', '노드 상태', 'kube_node_info * on(node,ref_id) group_right(internal_ip) (kube_node_status_condition == 1)', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]", "status_condition": "$.metric.condition:$.metric.status"}'::jsonb, 'DiskPressure, MemoryPressure, NetworkUnavailable, PIDPressure, Ready 정', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(5, '노드 기본정보', '노드 역할', 'kube_node_info * on(node,ref_id) group_right(internal_ip) (kube_node_role)', '{"key": "node_info", "node": "$.metric.node", "role": "$.metric.role", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]"}'::jsonb, '마스터노드 찾는다면 kube_node_role{role=~"control-plane|master"}', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(6, '노드 기본정보', '노드 오염(TAINT)', 'kube_node_info * on(node,ref_id) group_right(internal_ip) (kube_node_spec_taint == 1)', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]", "taint_effect": "$.metric.effect"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(7, '노드 전체용량', 'CPU 전체용량', 'kube_node_info * on(node,ref_id) group_right(internal_ip) (kube_node_status_capacity{resource="cpu"})*1000', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]", "capacity_cpu": "$.value[1]"}'::jsonb, 'kube-prometheus-kube-state-metrics', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(8, '노드 전체용량', 'CPU 전체용량', 'kube_node_info * on(system_uuid, ref_id) group_right(internal_ip, node) sum by(system_uuid, ref_id) (machine_cpu_cores) * 1000', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]", "capacity_cpu": "$.value[1]"}'::jsonb, NULL, NULL, 'N', 'Y');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(9, '노드 전체용량', 'DISK 전체용량', 'kube_node_info * on(node,ref_id) group_right(internal_ip) (kube_node_status_capacity{resource="ephemeral_storage"})', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]", "capacity_disk": "$.value[1]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(10, '노드 전체용량', 'Memory 전체용량', 'kube_node_info * on(system_uuid, ref_id) group_right(internal_ip, node)  sum by(system_uuid, ref_id) (machine_memory_bytes)', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]", "capacity_memory": "$.value[1]"}'::jsonb, '통합에서는 문제가 있네', NULL, 'N', 'Y');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(11, '노드 전체용량', 'Memory 전체용량', 'kube_node_info * on(node, ref_id) group_right(internal_ip) kube_node_status_capacity{resource="memory"}', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.internal_ip", "timestamp": "$.value[0]", "capacity_memory": "$.value[1]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(12, '노드 전체용량', 'GPU 전체 갯수 용량', 'count by (Hostname, exported_instance, ref_id) ((topk(1, DCGM_FI_DEV_GPU_UTIL) by (UUID)))', '{"key": "node_info", "node": "$.metric.Hostname", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "capacity_gpu": "$.value[1]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(13, '노드 전체용량', 'GPU 리스트', 'count by (Hostname, exported_instance, gpu, modelName, ref_id)(topk(1, DCGM_FI_DEV_GPU_UTIL or DCGM_FI_DEV_FB_FREE) by (UUID))', '{"key": "node_info", "node": "$.metric.Hostname", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "gpu_model": "$.metric.gpu:$.metric.modelName", "timestamp": "$.value[0]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(14, '노드 전체용량', 'GPU 개별 메모리 전체용량', 'sum(topk(1, DCGM_FI_DEV_FB_FREE + DCGM_FI_DEV_FB_USED) by (UUID)) by(Hostname,exported_instance, gpu, ref_id)', '{"key": "node_info", "node": "$.metric.Hostname", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "capacity_gpu_memory": "$.metric.gpu:$.value[1]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(15, '노드 가용량', 'CPU 가용량', 'label_replace((sum by(ref_id, node) (kube_node_info * on(node,ref_id) (kube_node_status_capacity{resource="cpu"})*1000)),"nodename", "$1","node", "(.*)") - on (nodename, ref_id) group_left(exported_instance) (sum by(exported_instance, nodename, ref_id) (node_uname_info * on(exported_instance, ref_id) group_right(nodename) (sum by(ref_id, exported_instance) (rate(node_cpu_seconds_total[1m])) - sum by (ref_id, exported_instance) (rate(node_cpu_seconds_total{mode="idle"}[1m]))) * 1000))', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "timestamp": "$.value[0]", "available_cpu": "$.value[1]"}'::jsonb, '1분간 변화율 활용', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(16, '노드 가용량', 'DISK 가용량', 'node_uname_info * on(exported_instance, ref_id) group_right(nodename) node_filesystem_avail_bytes{mountpoint="/"}', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "available_disk": "$.value[1]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(17, '노드 가용량', 'MEMORY 가용량', 'node_uname_info * on(exported_instance, ref_id) group_right(nodename) node_memory_MemAvailable_bytes', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "available_memory": "$.value[1]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(18, '노드 가용량', 'MEMORY 가용량', 'node_uname_info * on(exported_instance, ref_id) group_right(nodename) ((node_memory_Cached_bytes + node_memory_MemFree_bytes + node_memory_Buffers_bytes + node_memory_SReclaimable_bytes))', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "available_memory": "$.value[1]"}'::jsonb, NULL, NULL, 'N', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(19, '노드 가용량', 'GPU 노드 가용량', '   count by(Hostname, exported_instance, ref_id)(topk(1,DCGM_FI_DEV_GPU_UTIL{pod=""}) by(UUID))
or(count by(Hostname, exported_instance, ref_id)(topk(1,DCGM_FI_DEV_GPU_UTIL) by(UUID)) - count by(Hostname, exported_instance, ref_id)(topk(1, DCGM_FI_DEV_GPU_UTIL{pod=~".+"}) by(UUID) )  
 +(count by(Hostname, exported_instance, ref_id)(topk(1,DCGM_FI_DEV_GPU_UTIL{pod=~".+"}) * on(pod, ref_id) group_right(Hostname, exported_instance) (kube_pod_container_status_terminated == 1)) or vector(0)))
or(count by(Hostname, exported_instance, ref_id)(topk(1,DCGM_FI_DEV_GPU_UTIL) by(UUID)) - count by(Hostname, exported_instance, ref_id)(topk(1, DCGM_FI_DEV_GPU_UTIL{pod=~".+"}) by(UUID) ) )', '{"key": "node_info", "node": "$.metric.Hostname", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "available_gpu": "$.value[1]"}'::jsonb, '노드의 사용 가능한 gpu 개수', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(20, '노드 가용량', 'GPU 개별 메모리 가용량', 'sum((topk(1, DCGM_FI_DEV_FB_FREE) by(UUID))) by(Hostname, exported_instance, ref_id, gpu, exported_pod)', '{"key": "node_info", "node": "$.metric.Hostname", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "available_gpu_memory": "$.metric.gpu:$.value[1]"}'::jsonb, '각 GPU별 사용량', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(21, '노드 가용량', 'GPU 개별 온도', 'sum((topk(1, DCGM_FI_DEV_GPU_TEMP) by (UUID)) ) by(Hostname, exported_instance, ref_id, gpu)', '{"key": "node_info", "node": "$.metric.Hostname", "cl_uid": "$.metric.ref_id", "gpu_temp": "$.metric.gpu:$.value[1]", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]"}'::jsonb, '각 GPU별 온도', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(22, '파드 기본정보', '파드 정보', 'kube_pod_info{namespace=~".+", namespace!~"#|namespace|"}', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "parent": "$.metric.created_by_name", "instance": "$.metric.host_ip", "namespace": "$.metric.namespace", "timestamp": "$.value[0]", "createByKind": "$.metric.created_by_kind", "createByName": "$.metric.created_by_name"}'::jsonb, '큰의미가 없고 join에 사용', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(23, '파드 기본정보', '파드 레이블', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node) kube_pod_labels{namespace=~".+", namespace!~"#|namespace|"}', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "label": "$.metric.startWith(label)", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]"}'::jsonb, 'XXXXXXXXXXXXXXXXXX', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(24, '파드 기본정보', '파드 생성시간', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node) sum by(uid, pod, namespace, ref_id) (kube_pod_created{namespace=~".+", namespace!~"#|namespace|"})', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "created_timestamp": "$.value[1]"}'::jsonb, '쿠버네티스가 파드를 생성된 시간
', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(25, '파드 기본정보', '파드 스케줄링 시간', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node)  sum by(pod, namespace, uid, ref_id) (kube_pod_status_scheduled_time{namespace=~".+", namespace!~"#|namespace|"})', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "scheduled_timestamp": "$.value[1]"}'::jsonb, '파드가 실제 노드에 배포된 시간(재시작되면 재시작시간, 딜레이시간
)', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(26, '파드 기본정보', '파드 완료 시간', 'kube_pod_info * on(uid) group_right(host_ip, node) sum by(pod, namespace, uid, ref_id) (kube_pod_completion_time{namespace=~".+",namespace!~"#|namespace|"})', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "completed_timestamp": "$.value[1]"}'::jsonb, '', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(27, '파드 컨테이너', '파드 CPU 1분 변화량', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) max by(pod, namespace, ref_id) (rate(container_cpu_usage_seconds_total{namespace=~".+", namespace!~"#|namespace|"}[1m])) * 1000', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_cpu_1m": "$.value[1]"}'::jsonb, '파드의 최근 1분간 사용율(초당으로 표현)', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(28, '파드 컨테이너', '파드 CPU 총 사용량', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) max by(pod, namespace, ref_id) (container_cpu_usage_seconds_total{namespace=~".+", namespace!~"#|namespace|"}) * 1000', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_cpu": "$.value[1]"}'::jsonb, '누적시간', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(29, '파드 컨테이너', '파드 MEMORY 현재 사용량', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) sum by(pod, namespace, ref_id)(container_memory_usage_bytes{container=~".+", namespace=~".+", namespace!~"#|namespace|"})', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_memory": "$.value[1]"}'::jsonb, '현재 메모리 사용량', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(30, '파드 컨테이너', '파드 MEMORY 1분 변화량', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) sum by(pod, namespace, ref_id)(rate(container_memory_usage_bytes{container=~".+", namespace=~".+", namespace!~"#|namespace|"}[1m]))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_memory_1m": "$.value[1]"}'::jsonb, '최근 1분간의 초당 사용량', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(31, '파드 컨테이너', '파드 디스크 1분 쓰기 바이트', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) max by(pod, namespace, ref_id) (sum by(device, pod, namespace, ref_id) (rate(container_fs_writes_bytes_total{container=~".+", namespace=~".+", namespace!~"#|namespace|"}[1m])))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_disk_writes_1m": "$.value[1]"}'::jsonb, '1분간 초당 쓰기 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(32, '파드 컨테이너', '파드 디스크 1분 읽기 바이트', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) max by(pod, namespace, ref_id)(sum by(device, pod, namespace, ref_id) (rate(container_fs_reads_bytes_total{container=~".+", namespace=~".+", namespace!~"#|namespace|"}[1m])))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_disk_reads_1m": "$.value[1]"}'::jsonb, '1분간 초당 읽기 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(33, '파드 컨테이너', '파드 디스크 IO 1분 사용율', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) max by(pod, namespace, ref_id)((sum by (device, pod, namespace, ref_id) (rate(container_fs_writes_bytes_total{container=~".+", namespace=~".+", namespace!~"#|namespace|"}[1m]))) + (sum by (device, pod, namespace, ref_id) (rate(container_fs_reads_bytes_total{container=~".+", namespace=~".+", namespace!~"#|namespace|"}[1m]))))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_disk_io_1m": "$.value[1]"}'::jsonb, '1분간 초당 읽기 + 쓰기 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(34, '파드 컨테이너', '파드 디스크 IO 총 사용율', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) max by(pod, namespace, ref_id)((sum by (device,pod, namespace, ref_id) (container_fs_writes_bytes_total{container=~".+", namespace=~".+", namespace!~"#|namespace|"})) + (sum by (device,pod, namespace, ref_id) (container_fs_reads_bytes_total{container=~".+", namespace=~".+", namespace!~"#|namespace|"})))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_disk_io": "$.value[1]"}'::jsonb, '총(읽기 + 쓰기) 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(35, '파드 컨테이너', '파드 네트워크 1분 송신율', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) sum by(pod, namespace, ref_id)(rate(container_network_transmit_bytes_total{namespace=~".+", namespace!~"#|namespace|"}[1m]))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_network_transmit_1m": "$.value[1]"}'::jsonb, '1분간의 초당 송신 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(36, '파드 컨테이너', '파드 네트워크 1분 수신율', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) sum by(pod, namespace, ref_id)(rate(container_network_receive_bytes_total{namespace=~".+", namespace!~"#|namespace|"}[1m]))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_network_receive_1m": "$.value[1]"}'::jsonb, '1분간의 초당 수신 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(37, '파드 컨테이너', '파드 네트워크 IO 1분 사용율', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) ((sum by (pod,namespace, ref_id) (rate(container_network_transmit_bytes_total{namespace=~".+", namespace=~".+", namespace!~"#|namespace|"}[1m]))) + (sum by (pod,namespace, ref_id) (rate(container_network_receive_bytes_total{namespace=~".+", namespace=~".+", namespace!~"#|namespace|"}[1m]))))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_network_io_1m": "$.value[1]"}'::jsonb, '1분간의 초당 송신 바이트 + 수신 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(38, '파드 컨테이너', '파드 네트워크 총 송신율', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) sum by(pod, namespace, ref_id)(container_network_transmit_bytes_total{namespace=~".+", namespace!~"#|namespace|"})', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_network_transmit": "$.value[1]"}'::jsonb, '총 송신 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(39, '파드 컨테이너', '파드 네트워크 총 수신율', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) sum by(pod, namespace, ref_id)(container_network_receive_bytes_total{namespace=~".+", namespace!~"#|namespace|"})', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_network_receive": "$.value[1]"}'::jsonb, '총 수신 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(40, '파드 컨테이너', '파드 네트워크 IO 총 사용율', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) ((sum by (pod, namespace, ref_id) (container_network_transmit_bytes_total{namespace=~".+", namespace!~"#|namespace|"})) + (sum by (pod, namespace, ref_id) (container_network_receive_bytes_total{namespace=~".+", namespace!~"#|namespace|"})))', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "usage_network_io": "$.value[1]"}'::jsonb, '총 (송신 + 수신) 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(41, '파드 컨테이너', 'GPU 개별 사용량', 'kube_pod_info * on (pod, ref_id) group_right(uid) sum((topk(1, (kube_pod_status_phase{phase="Running"} == 1) * on(pod, ref_id) group_right DCGM_FI_DEV_GPU_UTIL{pod=~".+"}) by (UUID)) ) by(Hostname, exported_instance, gpu, kubernetes_namespace, pod, ref_id)', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.Hostname", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "usage_gpu": "$.metric.gpu:$.value[1]"}'::jsonb, '사용중인 파드 관련 GPU,종료된 파드는 나타나지 않는다.', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(42, '파드 컨테이너', '파드 리소스 LIMITS', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node) (kube_pod_container_resource_limits{container!="wait", resource="cpu", unit="core", namespace!~"#|namespace|"} * 1000 or kube_pod_container_resource_limits{container!="wait", resource!="cpu", unit!="core", namespace!~"#|namespace|"})', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "limits": "$.metric.resource:$.value[1]", "instance": "$.metric.host_ip", "timestamp": "$.value[0]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(43, '파드 컨테이너', '파드 리소스 REQUESTS', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node) (kube_pod_container_resource_requests{container!="wait", resource="cpu", unit="core", namespace!~"#|namespace|"} * 1000 or kube_pod_container_resource_requests{container!="wait", resource!="cpu", unit!="core", namespace!~"#|namespace|"})', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "requests": "$.metric.resource:$.value[1]", "timestamp": "$.value[0]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(44, '파드 컨테이너', '파드 컨테이너 정보', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node) kube_pod_container_info{container!="wait", namespace=~".+", namespace!~"#|namespace|"}', '{"key": "container_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "container": "$.metric.container", "namespace": "$.metric.namespace", "timestamp": "$.value[0]"}'::jsonb, '큰 의미가 없으며 나중에 필요하면 추가하자.', NULL, 'Y', 'Y');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(45, '파드 컨테이너', '파드 컨테이너 시작시간', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node) sum by(pod, container, uid, ref_id) (kube_pod_container_state_started{container!="wait", namespace=~".+", namespace!~"#|namespace|"})', '{"key": "container_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "container": "$.metric.container", "timestamp": "$.value[0]", "created_timestamp": "$.value[1]"}'::jsonb, '실제 컨테이너의 시작시간(이미지 다운로드 시간제외한 이후에 실제 실행된 시간)', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(46, '파드 컨테이너', '파드 컨테이너 실행상태', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node) sum by(pod, container, uid, ref_id) (kube_pod_container_status_running{container!="wait",namespace=~".+", namespace!~"#|namespace|"} == 1)', '{"key": "container_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "running": "$.value[1]", "instance": "$.metric.host_ip", "container": "$.metric.container", "timestamp": "$.value[0]"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(47, '파드 컨테이너', '파드 컨테이너 대기상태', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node, namespace) sum by(pod, container, uid, reason, ref_id) (kube_pod_container_status_waiting_reason{container!="wait", namespace=~".+", namespace!~"#|namespace|"} == 1 or kube_pod_container_status_waiting{container!="wait", namespace=~".+", namespace!~"#|namespace|"} == 1 unless on(uid) kube_pod_container_status_waiting_reason{container!="wait", namespace=~".+", namespace!~"#|namespace|"} == 1)', '{"key": "container_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "waiting": "$.value[1]", "instance": "$.metric.host_ip", "container": "$.metric.container", "timestamp": "$.value[0]", "waiting_reason": "$.metric.reason"}'::jsonb, NULL, NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(48, '파드 컨테이너', '파드 컨테이너 종료상태', 'kube_pod_info * on(uid, ref_id) group_right(host_ip, node, namespace) sum by(pod, container, uid, reason, ref_id) (kube_pod_container_status_terminated_reason{container!="wait", namespace=~".+", namespace!~"#|namespace|"} == 1 
or kube_pod_container_status_terminated{container!="wait", namespace=~".+", namespace!~"#|namespace|"} == 1 unless on(uid) kube_pod_container_status_terminated_reason{container!="wait", namespace=~".+", namespace!~"#|namespace|"} == 1)', '{"key": "container_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "container": "$.metric.container", "timestamp": "$.value[0]", "terminated": "$.value[1]", "terminated_reason": "$.metric.reason"}'::jsonb, '종료상태와 이유를 한꺼번에 등록', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(49, '테스트', '여러가지', 'sum by(node, resource, instance, unit, ref_id) (kube_node_status_capacity{resource=~"cpu", unit="core"} * 1000 or kube_node_status_capacity{resource=~"cpu|pods|memory|ephemeral_storage"})', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.instance", "timestamp": "$.value[0]", "capacity_cpu": "$[?(@.metric.resource == ''cpu'')].value[1]", "capacity_disk": "$[?(@.metric.resource == ''ephemeral_storage'')].value[1]", "capacity_pods": "$[?(@.metric.resource == ''pods'')].value[1]", "capacity_memory": "$[?(@.metric.resource == ''memory'')].value[1]"}'::jsonb, '여러개를 한꺼번에 처리한 경우를 테스트함, 지우지 말고 나중에 보장', NULL, 'Y', 'Y');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(50, '노드전체용량', 'PODS 전체용량', 'sum by(node, resource, instance, unit, ref_id) ( kube_node_status_capacity{resource="pods"})', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "timestamp": "$.value[0]", "capacity_pods": "$.value[1]"}'::jsonb, 'pods의 총 용량', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(51, '노드가용량', 'PODS 가용량', 'sum by(node, ref_id) (kube_node_status_capacity{resource="pods"}) - count by(node, ref_id) (kube_pod_info)', '{"key": "node_info", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "timestamp": "$.value[0]", "available_pods": "$.value[1]"}'::jsonb, 'pods의 사용가능한 갯수', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(52, '노드가용량', 'Network 1분 수신율', 'node_uname_info * on(exported_instance, ref_id) group_right(nodename) sum by (ref_id, exported_instance) (((node_network_address_assign_type == 0) + on(device, exported_instance, ref_id) node_network_info{operstate="up"}) * on(device, exported_instance, ref_id) (rate(node_network_receive_bytes_total[1m])))', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "timestamp": "$.value[0]", "usage_network_receive_1m": "$.value[1]"}'::jsonb, '노드별 초당 네트워크 수신 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(53, '노드가용량', 'Network 1분 송신율', 'node_uname_info * on(exported_instance, ref_id) group_right(nodename) sum by (ref_id, exported_instance) (((node_network_address_assign_type == 0) + on(device, exported_instance, ref_id) node_network_info{operstate="up"}) * on(device, exported_instance, ref_id) (rate(node_network_transmit_bytes_total[1m])))', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "timestamp": "$.value[0]", "usage_network_transmit_1m": "$.value[1]"}'::jsonb, '노드별 초당 네트워크 송신 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(54, '노드가용량', '디스크 1분 쓰기 바이트', 'node_uname_info * on(exported_instance, ref_id) group_right(nodename) sum by (exported_instance, ref_id) (rate(node_disk_written_bytes_total{device!~"dm.*|sr.*"}[1m]))', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "timestamp": "$.value[0]", "usage_disk_write_1m": "$.value[1]"}'::jsonb, '노드별 초당 디스트 쓰기 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(55, '노드가용량', '디스크 1분 읽기 바이트', 'node_uname_info * on(exported_instance, ref_id) group_right(nodename) sum by (exported_instance, ref_id) (rate(node_disk_read_bytes_total{device!~"dm.*|sr.*"}[1m]))', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "timestamp": "$.value[0]", "usage_disk_read_1m": "$.value[1]"}'::jsonb, '노드별 초당 디스트 읽기 바이트', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(56, '파드 기본정보', '파드 상태 정보', 'kube_pod_info * on(namespace, pod, ref_id) group_right(host_ip, node, uid) kube_pod_status_phase{namespace!~"#|namespace|"} == 1', '{"key": "pod_info", "pod": "$.metric.pod", "uid": "$.metric.uid", "node": "$.metric.node", "cl_uid": "$.metric.ref_id", "instance": "$.metric.host_ip", "timestamp": "$.value[0]", "status_phase": "$.metric.phase"}'::jsonb, '파드 기본 상태정보 정보(Running, Sucessed, Pending, Failed, Unknow)', NULL, 'Y', 'N');
INSERT INTO k_hybrid.mo_promql_total
(uid, grp_nm, nm, cont, extract_path, memo, type_cd, default_at, delete_at)
VALUES(57, '노드전체용량', 'CPU 최대속도', 'topk(1,node_uname_info * on(exported_instance, ref_id) group_right(nodename) node_cpu_frequency_max_hertz) by(exported_instance, ref_id)', '{"key": "node_info", "node": "$.metric.nodename", "cl_uid": "$.metric.ref_id", "instance": "$.metric.exported_instance", "timestamp": "$.value[0]", "capacity_maxhz_cpu": "$.value[1]"}'::jsonb, '워크노드 최대 속도(tubor) 고급, 저급 판단에 필요하지 않을까???', NULL, 'Y', 'N');