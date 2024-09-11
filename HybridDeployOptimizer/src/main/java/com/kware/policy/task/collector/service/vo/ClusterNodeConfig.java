package com.kware.policy.task.collector.service.vo;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 성능의 정보를 등록한다.
 */

@Getter
@Setter
@ToString
public class ClusterNodeConfig extends ClusterDefault {
	private Integer noUid; // 노드 UID
    /**
     * API와 Prometheus의 metric을 통해서 자동 등록
     * 
		단계	클럭 속도	코어 수	메모리 양	총 점수	성능 단계
		1단계	≤ 2.0	≤ 4	≤ 16	3 - 4	1단계
		2단계	2.1 - 2.5	5 - 8	17 - 32	5 - 6	2단계
		3단계	2.6 - 3.0	9 - 16	33 - 64	7 - 8	3단계
		4단계	3.1 - 3.5	17 - 32	65 - 96	9 - 10	4단계
		5단계	3.6 - 4.0	33 - 64	97 - 128	11 - 12	5단계
		6단계	4.1 - 4.5	65 - 128	129 - 256	13 - 14	6단계
		7단계	4.6 - 5.0	129 - 192	257 - 512	15 - 16	7단계
		8단계	> 5.0	193 - 256	513 - 768	17 - 18	8단계
		9단계	> 5.0	> 256	769 - 1024	19 - 20	9단계
		10단계	> 5.0	> 256	> 1024	21 - 27	10단계
     */
	private Short genLevel; // 일반 성능 레벨 10단계
    
	/** 관리자가 직접 선택 
	 * 
	 	1단계	엔트리 레벨, 저사양 GPU. CUDA Cores와 메모리 용량이 매우 낮음.
		2단계	약간 더 높은 저사양 GPU. 기본적인 딥러닝이나 그래픽 작업에 적합.
		3단계	보급형 GPU, 약간 더 나은 병렬 처리 성능.
		4단계	중급 GPU, 대부분의 일반적인 작업에 적합.
		5단계	상위 중급 GPU, 딥러닝 및 고사양 그래픽 작업 가능.
		6단계	고급 GPU, 상당한 CUDA Cores와 메모리 용량.
		7단계	준하이엔드 GPU, 복잡한 딥러닝 모델과 고사양 시뮬레이션 작업에 적합.
		8단계	하이엔드 GPU, 높은 메모리 대역폭과 속도로 빠른 처리 가능.
		9단계	최상위급 GPU, 대규모 병렬 처리 및 고사양 연산 작업에 최적.
		10단계	최고 성능의 GPU, 최첨단 기술을 사용하며, 연구 및 대규모 데이터 처리에 적합.
	 */
    private Short gpuLevel; // GPU 성능 레벨 10단계
    
    /**
     * 관리자가 직접 선택
     * 
     	레벨	    설명	                         특징	                                                              클라우드 유형
		레벨 1	기본 보안	 (Basic Security)	- 비밀번호 보호 (단순 비밀번호)- 기본 방화벽- 자동 업데이트- 최소한의 물리적 보안	  퍼블릭, 프라이빗, 온프레미스
		레벨 2	표준 보안	 (Standard Security)- 강력한 비밀번호 정책- 네트워크 모니터링- 안티바이러스 소프트웨어- 기본 접근 제어	  퍼블릭, 프라이빗, 온프레미스
		레벨 3	강화된 보안(Enhanced Security)	- 다단계 인증 (MFA)- 암호화- 고급 방화벽 및 IDS- 정기적인 보안 감사	          프라이빗, 온프레미스
		레벨 4	고급 보안	 (Advanced Security)- 위협 탐지 및 대응- 권한 분리- SIEM- 고급 암호화	                          프라이빗, 온프레미스
		레벨 5	최고 보안	 (Highest Security)	- 물리적 및 논리적 보안 통합- 가상화 및 분리- 강화된 보안 정책- 침투 테스트	      온프레미스
     */
    private Short secLevel; // 보안 레벨 5단계
    /**
     * 클라우드 구분
     * PRI, PUB ONP
     */
    private CloudType cloudType; // 클라우드 구분, PRI PUB ONP
    
    /**
     * 추가적인 설정을 json으로 추가한 
      {
      	"aaa":"bbb",
      	"ccc": 1
      }
     */
    private String etc; // 추가 설정 (JSON 문자열)

    public static enum CloudType{
    	PRI, PUB, ONP
    }
    
	@Override
	public Object getUniqueKey() {
		return noUid;
	}
}

/* 

*/