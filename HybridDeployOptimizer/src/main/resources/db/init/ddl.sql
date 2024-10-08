-- DROP SCHEMA k_hybrid;

CREATE SCHEMA k_hybrid AUTHORIZATION postgres;


CREATE TABLE k_hybrid.mo_cluster (
	uid int4 NOT NULL, -- UID
	nm varchar(128) NOT NULL, -- cluster 이름
	info jsonb NOT NULL, -- cluster 정보
	memo text NULL, -- cluster 설명
	prom_url varchar(256) NULL, -- 프로메테우스 url
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

-- Permissions

ALTER TABLE k_hybrid.mo_cluster_history OWNER TO postgres;
GRANT ALL ON TABLE k_hybrid.mo_cluster_history TO postgres;

-- DROP TABLE k_hybrid.mo_cluster_node;

CREATE TABLE k_hybrid.mo_cluster_node (
	uid serial4 NOT NULL, -- uuid
	cl_uid int4 NOT NULL, -- Cluster UID
	nm varchar(128) NOT NULL, -- cluster 이름
	no_uuid varchar(128) NULL, -- uuid
	info jsonb NULL, -- cluster 정보
	gpuinfo jsonb NULL, -- GPU정보를 별도로 추가한다.
	memo text NULL, -- 설명
	delete_at bpchar(1) DEFAULT 'N'::bpchar NULL,
	hash_val varchar(32) NULL, -- 무결성검증 해쉬값
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	PRIMARY KEY (uid)
);
CREATE UNIQUE INDEX mo_cluster_node_cl_uid_idx ON k_hybrid.mo_cluster_node(cl_uid, nm);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_cluster_node.uid IS 'uuid';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.cl_uid IS 'Cluster UID';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.nm IS 'cluster 이름';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.no_uuid IS 'uuid';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.info IS 'cluster 정보';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.gpuinfo IS 'GPU정보를 별도로 추가한다.';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.memo IS '설명';
COMMENT ON COLUMN k_hybrid.mo_cluster_node.hash_val IS '무결성검증 해쉬값';


-- DROP TABLE k_hybrid.mo_cluster_node_config;

CREATE TABLE k_hybrid.mo_cluster_node_config (
	no_uid serial4 NOT NULL, -- 노드 uid
	gen_level int2 NULL, -- 일반 성능 레벨 10단계
	gpu_level int2 NULL, -- GPU 성늘 레벨 10단계
	sec_level int2 NULL, -- 보안 레벨 5단계
	cloud_type varchar(3) NULL, -- 클라우드 구분,PRI PUB ONP
	etc jsonb NULL, -- 추가 설정
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT mo_cluster_node_config_pkey PRIMARY KEY (no_uid)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_cluster_node_config.no_uid IS '노드 uid';
COMMENT ON COLUMN k_hybrid.mo_cluster_node_config.gen_level IS '일반 성능 레벨 10단계';
COMMENT ON COLUMN k_hybrid.mo_cluster_node_config.gpu_level IS 'GPU 성늘 레벨 10단계';
COMMENT ON COLUMN k_hybrid.mo_cluster_node_config.sec_level IS '보안 레벨 5단계';
COMMENT ON COLUMN k_hybrid.mo_cluster_node_config.cloud_type IS '클라우드 구분,PRI PUB ONP';
COMMENT ON COLUMN k_hybrid.mo_cluster_node_config.etc IS '추가 설정';

-- DROP TABLE k_hybrid.mo_cluster_promql;

CREATE TABLE k_hybrid.mo_cluster_promql (
	prql_uid int4 NOT NULL, -- PromQl UID
	cl_uid int4 NOT NULL, -- Cluster UID
	memo text NOT NULL, -- 설명
	delete_at bpchar(1) DEFAULT 'N'::bpchar NULL,
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	PRIMARY KEY (prql_uid, cl_uid)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_cluster_promql.prql_uid IS 'PromQl UID';
COMMENT ON COLUMN k_hybrid.mo_cluster_promql.cl_uid IS 'Cluster UID';
COMMENT ON COLUMN k_hybrid.mo_cluster_promql.memo IS '설명';

-- DROP TABLE k_hybrid.mo_cluster_workload;

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

-- DROP TABLE k_hybrid.mo_common_config_group;

CREATE TABLE k_hybrid.mo_common_config_group (
	cfg_name varchar(32) NOT NULL, -- 설정 이름
	cfg_content jsonb NULL, -- 설정 내용
	cfg_desc varchar(128) NULL, -- 설정 설명
	delete_at bpchar(1) DEFAULT 'N'::bpchar NULL,
	reg_uid int8 NULL,
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updt_uid int8 NULL,
	updt_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	PRIMARY KEY (cfg_name)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_common_config_group.cfg_name IS '설정 이름';
COMMENT ON COLUMN k_hybrid.mo_common_config_group.cfg_content IS '설정 내용';
COMMENT ON COLUMN k_hybrid.mo_common_config_group.cfg_desc IS '설정 설명';

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


-- DROP TABLE k_hybrid.mo_resource_usage_pod;

CREATE TABLE k_hybrid.mo_resource_usage_pod (
	collect_dt timestamp NOT NULL, -- 수집주기의 시작시간
	cl_uid int4 NOT NULL, -- Cluster UID
	ml_id varchar(128) NOT NULL, -- mlworkload id
	pod_uid varchar(128) NOT NULL, -- pod_uid
	results jsonb NOT NULL, -- jsonb result
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

-- DROP TABLE k_hybrid.mo_user_request;

CREATE TABLE k_hybrid.mo_user_request (
	uid bigserial NOT NULL, -- 일련번호
	ml_id varchar(128) NOT NULL, -- ml UID
	nm varchar(128) NOT NULL, -- workload 이름
	info jsonb NOT NULL, -- workload 상세 정보
	status varchar(10) DEFAULT 'request'::character varying NULL, -- 요청 또는 배포완료
	reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 등록일시
	PRIMARY KEY (uid, ml_id)
);

-- Column comments

COMMENT ON COLUMN k_hybrid.mo_user_request.uid IS '일련번호';
COMMENT ON COLUMN k_hybrid.mo_user_request.ml_id IS 'ml UID';
COMMENT ON COLUMN k_hybrid.mo_user_request.nm IS 'workload 이름';
COMMENT ON COLUMN k_hybrid.mo_user_request.info IS 'workload 상세 정보';
COMMENT ON COLUMN k_hybrid.mo_user_request.status IS '요청 또는 배포완료';
COMMENT ON COLUMN k_hybrid.mo_user_request.reg_dt IS '등록일시';

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


-- Permissions
GRANT ALL ON SCHEMA k_hybrid TO postgres;


--hyber table생성

/* k_hybrid.mo_promql_result */
SELECT public.create_hypertable('k_hybrid.mo_promql_result'::regclass, 'collect_dt'::name, migrate_data => true, if_not_exists => true, associated_table_prefix => '_promql_result_hyper',  chunk_time_interval => interval '6 months');
ALTER TABLE k_hybrid.mo_promql_result SET (timescaledb.compress,  timescaledb.compress_orderby = 'collect_dt DESC'); 
SELECT public.add_compression_policy('k_hybrid.mo_promql_result', compress_after => INTERVAL '7 days');

/* k_hybrid.mo_resource_usage_node */

SELECT public.create_hypertable('k_hybrid.mo_resource_usage_node'::regclass,'collect_dt'::name, migrate_data => true, if_not_exists => true, associated_table_prefix => '_usage_node_hyper', chunk_time_interval => interval '6 months');
ALTER TABLE k_hybrid.mo_resource_usage_node SET (timescaledb.compress,  timescaledb.compress_orderby = 'collect_dt DESC', timescaledb.compress_segmentby = 'cl_uid'); 
SELECT public.add_compression_policy('k_hybrid.mo_resource_usage_node', compress_after => INTERVAL '7 days');

/* k_hybrid.mo_resource_usage_pod */

SELECT public.create_hypertable('k_hybrid.mo_resource_usage_pod'::regclass,'collect_dt'::name, migrate_data => true, if_not_exists => true, associated_table_prefix => '_usage_pod_hyper', chunk_time_interval => interval '6 months');
ALTER TABLE k_hybrid.mo_resource_usage_pod SET (timescaledb.compress,  timescaledb.compress_orderby = 'collect_dt DESC', timescaledb.compress_segmentby = 'cl_uid'); 
SELECT public.add_compression_policy('k_hybrid.mo_resource_usage_pod', compress_after => INTERVAL '7 days');
