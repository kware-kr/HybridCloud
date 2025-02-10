-- DROP SCHEMA k_hybrid;

CREATE SCHEMA k_hybrid AUTHORIZATION postgres;


CREATE TABLE k_hybrid.mo_cluster (
	uid int4 NOT NULL, -- UID
	nm varchar(128) NOT NULL, -- cluster 이름
	info jsonb NOT NULL, -- cluster 정보
	memo text NULL, -- cluster 설명
	prom_url varchar(256) NULL, -- 프로메테우스 url
	feature jsonb DEFAULT '{"cloudType": "PRI"}'::jsonb NULL, -- 특성저장 PUB|PRI|ONP
	delete_at bpchar(1) DEFAULT 'N'::bpchar NULL,
	hash_val varchar(32) NULL, -- 데이터무결성 해쉬값
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	PRIMARY KEY (uid)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_cluster.uid IS 'UID';
COMMENT ON COLUMN k_hybrid.mo_cluster.nm IS 'cluster 이름';
COMMENT ON COLUMN k_hybrid.mo_cluster.info IS 'cluster 정보';
COMMENT ON COLUMN k_hybrid.mo_cluster.memo IS 'cluster 설명';
COMMENT ON COLUMN k_hybrid.mo_cluster.prom_url IS '프로메테우스 url';
COMMENT ON COLUMN k_hybrid.mo_cluster.hash_val IS '데이터무결성 해쉬값';
COMMENT ON COLUMN k_hybrid.mo_cluster.feature IS '특성저장 PUB|PRI|ONP';


-- DROP TABLE k_hybrid.mo_cluster_history;

CREATE TABLE k_hybrid.mo_cluster_history (
	sn bigserial NOT NULL, -- 일련번호
	tbl_nm varchar(64) NOT NULL, -- 관련테이블 명
	contents jsonb NULL, -- 컬럼 전체 내용
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 등록일
	PRIMARY KEY (sn)
);
COMMENT ON TABLE k_hybrid.mo_cluster_history IS '클러스터 설정관련 레코드의 히스토리를 저장한다.';

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_cluster_history.sn IS '일련번호';
COMMENT ON COLUMN k_hybrid.mo_cluster_history.tbl_nm IS '관련테이블 명';
COMMENT ON COLUMN k_hybrid.mo_cluster_history.contents IS '컬럼 전체 내용';
COMMENT ON COLUMN k_hybrid.mo_cluster_history.reg_dt IS '등록일';


CREATE TABLE k_hybrid.mo_cluster_node (
	uid serial4 NOT NULL, -- uuid
	cl_uid int4 NOT NULL, -- Cluster UID
	nm varchar(128) NOT NULL, -- cluster 이름
	no_uuid varchar(128) NULL, -- uuid
	info jsonb NULL, -- cluster 정보
	gpuinfo jsonb NULL, -- GPU정보를 별도로 추가한다.
	memo text NULL, -- 설명
	feature jsonb NULL, -- 특성저장
	auto_feature jsonb NULL, -- 자동특성
	delete_at bpchar(1) DEFAULT 'N'::bpchar NULL,
	hash_val varchar(32) NULL, -- 무결성검증 해쉬값
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	PRIMARY KEY (uid)
);
CREATE UNIQUE INDEX mo_cluster_node_cl_uid_idx ON k_hybrid.mo_cluster_node USING btree (cl_uid, nm);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_cluster_node.uid IS 'uuid';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.cl_uid IS 'Cluster UID';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.nm IS 'cluster 이름';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.no_uuid IS 'uuid';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.info IS 'cluster 정보';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.gpuinfo IS 'GPU정보를 별도로 추가한다.';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.memo IS '설명';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.hash_val IS '무결성검증 해쉬값';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.feature IS '특성저장';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.auto_feature IS '자동특성';


CREATE TABLE k_hybrid.mo_cluster_workload (
	ml_id varchar(128) NOT NULL, -- ml UID
	cl_uid int4 NULL, -- Cluster UID
	nm varchar(128) NOT NULL, -- workload 이름
	info jsonb NULL, -- ML 상세 정보
	memo text NULL, -- 설명
	delete_at bpchar(1) DEFAULT 'N'::bpchar NULL,
	hash_val varchar(32) NULL, -- 무결성검증 해쉬값
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	PRIMARY KEY (ml_id)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_cluster_workload.ml_id IS 'ml UID';
COMMENT ON COLUMN k_hybrid.mo_cluster_workload.cl_uid IS 'Cluster UID';
COMMENT ON COLUMN k_hybrid.mo_cluster_workload.nm IS 'workload 이름';
COMMENT ON COLUMN k_hybrid.mo_cluster_workload.info IS 'ML 상세 정보';
COMMENT ON COLUMN k_hybrid.mo_cluster_workload.memo IS '설명';
COMMENT ON COLUMN k_hybrid.mo_cluster_workload.hash_val IS '무결성검증 해쉬값';



CREATE TABLE k_hybrid.mo_common_feature (
	fea_name varchar(64) NOT NULL, -- 설정 이름
	fea_sub_name varchar(64) DEFAULT 'none'::character varying NOT NULL, -- 서브 설정 이름
	fea_content jsonb NULL, -- 설정 내용
	fea_desc varchar(128) NULL, -- 설정 설명
	delete_at bpchar(1) DEFAULT 'N'::bpchar NOT NULL,
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	PRIMARY KEY (fea_name, fea_sub_name)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_common_feature.fea_name IS '설정 이름';
COMMENT ON COLUMN k_hybrid.mo_common_feature.fea_sub_name IS '서브 설정 이름';
COMMENT ON COLUMN k_hybrid.mo_common_feature.fea_content IS '설정 내용';
COMMENT ON COLUMN k_hybrid.mo_common_feature.fea_desc IS '설정 설명';

-- DROP TABLE k_hybrid.mo_common_gpu_spec;

CREATE TABLE k_hybrid.mo_common_gpu_spec (
	manufacture varchar(8) NULL,
	product varchar(64) NULL,
	chip varchar(8) NULL,
	released varchar(16) NULL,
	bus_type varchar(16) NULL,
	memory int4 NULL,
	memory_bit int4 NULL,
	memory_detail varchar(32) NULL,
	gpu_clock int4 NULL,
	memory_clock int4 NULL,
	cudas int4 NULL,
	tmus int4 NULL,
	rops int4 NULL,
	score numeric NULL
);

CREATE TABLE k_hybrid.mo_events (
	id bigserial NOT NULL, -- 아이디
	"name" varchar(255) NOT NULL, -- 이벤트명
	event_type varchar(100) NOT NULL, -- 이벤트유형
	description text NULL, -- 이벤트설명
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 등록일자
	PRIMARY KEY (id)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_events.id IS '아이디';
COMMENT ON COLUMN k_hybrid.mo_events."name" IS '이벤트명';
COMMENT ON COLUMN k_hybrid.mo_events.event_type IS '이벤트유형';
COMMENT ON COLUMN k_hybrid.mo_events.description IS '이벤트설명';
COMMENT ON COLUMN k_hybrid.mo_events.reg_dt IS '등록일자';


-- DROP TABLE k_hybrid.mo_promql_result;

CREATE TABLE k_hybrid.mo_promql_result (
	uid bigserial NOT NULL, -- UID
	prql_uid int4 NOT NULL, -- PromQl UID
	collect_dt timestamp NOT NULL, -- 수집주기의 시작시간
	results jsonb NOT NULL, -- jsonb result
	cl_uid int4 NULL, -- Cluster UID
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 등록시간
	PRIMARY KEY (uid, prql_uid, collect_dt)
);
CREATE INDEX mo_promql_result_collect_dt_idx ON k_hybrid.mo_promql_result(collect_dt DESC);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_promql_result.uid IS 'UID';
COMMENT ON COLUMN k_hybrid.mo_promql_result.prql_uid IS 'PromQl UID';
COMMENT ON COLUMN k_hybrid.mo_promql_result.collect_dt IS '수집주기의 시작시간';
COMMENT ON COLUMN k_hybrid.mo_promql_result.results IS 'jsonb result';
COMMENT ON COLUMN k_hybrid.mo_promql_result.cl_uid IS 'Cluster UID';
COMMENT ON COLUMN k_hybrid.mo_promql_result.reg_dt IS '등록시간';

-- DROP TABLE k_hybrid.mo_promql_total;

CREATE TABLE k_hybrid.mo_promql_total (
	uid serial4 NOT NULL, -- UID
	grp_nm varchar(64) NOT NULL, -- PromQL 그룹이름
	nm varchar(128) NOT NULL, -- PromQL 이름
	cont text NOT NULL, -- PromQL 내용
	extract_path jsonb NULL, -- 추출 jsonb path
	memo text NULL, -- PromQl 설명
	type_cd varchar(20) NULL, -- PromQl 유형 코드
	default_at bpchar(1) DEFAULT 'Y'::bpchar NULL,
	delete_at bpchar(1) DEFAULT 'N'::bpchar NULL,
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	PRIMARY KEY (uid)
);
COMMENT ON TABLE k_hybrid.mo_promql_total IS '통합된 프로메테우스 쿼리를 위한 테이블';

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_promql_total.uid IS 'UID';
COMMENT ON COLUMN k_hybrid.mo_promql_total.grp_nm IS 'PromQL 그룹이름';
COMMENT ON COLUMN k_hybrid.mo_promql_total.nm IS 'PromQL 이름';
COMMENT ON COLUMN k_hybrid.mo_promql_total.cont IS 'PromQL 내용';
COMMENT ON COLUMN k_hybrid.mo_promql_total.extract_path IS '추출 jsonb path';
COMMENT ON COLUMN k_hybrid.mo_promql_total.memo IS 'PromQl 설명';
COMMENT ON COLUMN k_hybrid.mo_promql_total.type_cd IS 'PromQl 유형 코드';

-- DROP TABLE k_hybrid.mo_promql_type;

CREATE TABLE k_hybrid.mo_promql_type (
	cd varchar(20) NOT NULL, -- CODE
	memo text NULL, -- 설명
	unit varchar(20) NOT NULL, -- 단위
	delete_at bpchar(1) DEFAULT 'N'::bpchar NULL,
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT mo_promql_type_pkey PRIMARY KEY (cd)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_promql_type.cd IS 'CODE';
COMMENT ON COLUMN k_hybrid.mo_promql_type.memo IS '설명';
COMMENT ON COLUMN k_hybrid.mo_promql_type.unit IS '단위';


-- DROP TABLE k_hybrid.mo_resource_usage_node;

CREATE TABLE k_hybrid.mo_resource_usage_node (
	collect_dt timestamp NOT NULL, -- 수집주기의 시작시간
	cl_uid int4 NOT NULL, -- Cluster UID
	node_nm varchar(128) NOT NULL, -- 노드 명
	results jsonb NOT NULL, -- jsonb result
	last_at varchar DEFAULT 'Y'::character varying NOT NULL, -- 최종데이터여부
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL -- 등록시간
);
CREATE INDEX mo_resource_usage_node_collect_dt_idx ON k_hybrid.mo_resource_usage_node USING btree (collect_dt DESC);
CREATE UNIQUE INDEX mo_resource_usage_node_pkey ON k_hybrid.mo_resource_usage_node USING btree (collect_dt DESC, cl_uid, node_nm);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_resource_usage_node.collect_dt IS '수집주기의 시작시간';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_node.cl_uid IS 'Cluster UID';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_node.node_nm IS '노드 명';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_node.results IS 'jsonb result';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_node.reg_dt IS '등록시간';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_node.last_at IS '최종데이터여부';


-- DROP TABLE k_hybrid.mo_resource_usage_pod;

CREATE TABLE k_hybrid.mo_resource_usage_pod (
	collect_dt timestamp NOT NULL, -- 수집주기의 시작시간
	cl_uid int4 NOT NULL, -- Cluster UID
	ml_id varchar(128) NOT NULL, -- mlworkload id
	pod_uid varchar(128) NOT NULL, -- pod_uid
	results jsonb NOT NULL, -- jsonb result
	last_at varchar DEFAULT 'Y'::character varying NOT NULL, -- 최종데이터여부
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL -- 등록시간
);
CREATE INDEX mo_resource_usage_pod_collect_dt_idx ON k_hybrid.mo_resource_usage_pod USING btree (collect_dt DESC);
CREATE UNIQUE INDEX mo_resource_usage_pod_pkey ON k_hybrid.mo_resource_usage_pod USING btree (collect_dt DESC, ml_id, cl_uid, pod_uid);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_resource_usage_pod.collect_dt IS '수집주기의 시작시간';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_pod.cl_uid IS 'Cluster UID';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_pod.ml_id IS 'mlworkload id';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_pod.pod_uid IS 'pod_uid';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_pod.results IS 'jsonb result';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_pod.reg_dt IS '등록시간';
COMMENT ON COLUMN k_hybrid.mo_resource_usage_pod.last_at IS '최종데이터여부';

---------------------------------------------------------------------------------------------------------------------------------------------------

-- DROP TABLE k_hybrid.mo_user_request;

CREATE TABLE k_hybrid.mo_user_request (
	uid bigserial NOT NULL, -- 일련번호
	ml_id varchar(128) NOT NULL, -- ml UID
	nm varchar(128) NOT NULL, -- workload 이름
	request_json jsonb NOT NULL, -- workload 상세 정보
	request_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 요청시간
	noti_json jsonb NULL, -- 배포통지
	noti_dt timestamp NULL, -- 통지완료시간
	complete_dt timestamp NULL, -- 완료시간
	CONSTRAINT mo_user_request_pkey PRIMARY KEY (uid)
);

CREATE INDEX idx_mo_user_request_ml_id_request_dt ON k_hybrid.mo_user_request (ml_id, request_dt DESC);
-- Column comments

COMMENT ON COLUMN k_hybrid.mo_user_request.uid IS '일련번호';
COMMENT ON COLUMN k_hybrid.mo_user_request.ml_id IS 'ml UID';
COMMENT ON COLUMN k_hybrid.mo_user_request.nm IS 'workload 이름';
COMMENT ON COLUMN k_hybrid.mo_user_request.request_json IS 'workload 상세 JSON';
COMMENT ON COLUMN k_hybrid.mo_user_request.request_dt IS '요청시간';
COMMENT ON COLUMN k_hybrid.mo_user_request.noti_json IS '배포통지 상세 JSON';
COMMENT ON COLUMN k_hybrid.mo_user_request.noti_dt IS '통지완료시간';
COMMENT ON COLUMN k_hybrid.mo_user_request.complete_dt IS '완료시간';


-- DROP TABLE k_hybrid.mo_user_response;

CREATE TABLE k_hybrid.mo_user_response (
	req_uid int8 NOT NULL, -- 일련번호
	ml_id varchar(128) NOT NULL, -- ml UID
	cl_uid int4 NULL, -- Cluster UID
	no_uid varchar(128) NULL, -- Node UID
	info jsonb NULL, -- 응답 상세 정보
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 등록일시
	PRIMARY KEY (req_uid, ml_id)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_user_response.req_uid IS '일련번호';
COMMENT ON COLUMN k_hybrid.mo_user_response.ml_id IS 'ml UID';
COMMENT ON COLUMN k_hybrid.mo_user_response.cl_uid IS 'Cluster UID';
COMMENT ON COLUMN k_hybrid.mo_user_response.no_uid IS 'Node UID';
COMMENT ON COLUMN k_hybrid.mo_user_response.info IS '응답 상세 정보';
COMMENT ON COLUMN k_hybrid.mo_user_response.reg_dt IS '등록일시';

---------------------------------------------------------------------------------------------------------------------------------------------------

-- Permissions
GRANT ALL ON SCHEMA k_hybrid TO postgres;


--hyber table생성

/* 적용 6개월 청크단위, 7일 이후 데이터 압축*/

/* k_hybrid.mo_promql_result */
SELECT public.create_hypertable('k_hybrid.mo_promql_result', 'collect_dt', migrate_data => TRUE, if_not_exists => TRUE, associated_table_prefix=>'_promql_result_hyper',  chunk_time_interval =>interval '1 week');
ALTER TABLE k_hybrid.mo_promql_result SET (timescaledb.compress,  timescaledb.compress_orderby = 'collect_dt DESC, uid', timescaledb.compress_segmentby = 'prql_uid'); -- cl_uid로 나누어 압축할려는데, pk에 포함되어야한다.
SELECT public.add_compression_policy('k_hybrid.mo_promql_result', compress_after => INTERVAL '1 week');


/* k_hybrid.mo_resource_usage_node */
SELECT public.create_hypertable('k_hybrid.mo_resource_usage_node','collect_dt', migrate_data => TRUE, if_not_exists => TRUE, associated_table_prefix=>'_usage_node_hyper', chunk_time_interval =>interval '1 month');
ALTER TABLE k_hybrid.mo_resource_usage_node SET (timescaledb.compress,  timescaledb.compress_orderby = 'collect_dt DESC', timescaledb.compress_segmentby = 'cl_uid'); 
SELECT public.add_compression_policy('k_hybrid.mo_resource_usage_node', compress_after => INTERVAL '1 month');


/* k_hybrid.mo_resource_usage_pod */
SELECT public.create_hypertable('k_hybrid.mo_resource_usage_pod','collect_dt', migrate_data => TRUE, if_not_exists => TRUE, associated_table_prefix=>'_usage_pod_hyper', chunk_time_interval =>interval '1 month');
ALTER TABLE k_hybrid.mo_resource_usage_pod SET (timescaledb.compress,  timescaledb.compress_orderby = 'collect_dt DESC', timescaledb.compress_segmentby = 'cl_uid'); 
SELECT public.add_compression_policy('k_hybrid.mo_resource_usage_pod', compress_after => INTERVAL '1 month');
