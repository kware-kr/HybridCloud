package com.kware.policy.task.feature.eval;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodeGPU;
import com.kware.policy.task.common.service.CommonService;

import lombok.extern.slf4j.Slf4j;

/**
코드 설명
1. Min-Max 정규화:
  모든 GPU의 메모리, 코어, FLOPS 값을 정규화.
  0~1 범위로 변환.
2. GPU 점수 계산:
  각 GPU의 성능 점수를 계산:  GPU 점수=(0.4×메모리 정규화)+(0.3×코어 정규화)+(0.3×FLOPS 정규화)
  머신의 GPU 성능 점수는 해당 머신에 장착된 모든 GPU의 평균 점수.
3. GPU 등급 매핑:
  머신의 GPU 점수를 1~10단계로 변환.
4.출력:
  각 머신의 GPU 성능 점수와 등급을 출력.

알고리즘 이름 요약
1. 가중치 기반 점수화 (Weighted Scoring Algorithm)
  각 GPU의 특성(메모리, 코어, FLOPS 등)을 가중치에 따라 점수화.
  정규화를 통해 0~1 범위로 데이터를 통일한 뒤, 가중치를 적용하여 종합 점수를 계산.
2. 다기준 의사결정 (Multi-Criteria Decision Making, MCDM)
  여러 특성을 고려해 단일 성능 지표를 계산하는 방식.
  본 알고리즘은 MCDM의 기본적인 형태 중 하나로, Simple Additive Weighting (SAW) 방식에 해당:
  특성을 가중치 기반으로 선형 결합하여 최종 점수를 계산.
3. GPU 성능 등급화
  계산된 점수를 기반으로 1~10단계로 매핑하는 단계는 **점수 기반 분류(Score-Based Classification)**로 볼 수 있습니다.

전체 이름
"Weighted Scoring and Classification for GPU Performance Evaluation"  ==> "가중치 기반 GPU 성능 등급화 알고리즘"
 */
/*GPU 개별 스코어 생성 방법
 WITH adjusted_values AS (
    SELECT 
        *,
        -- Log Scaled Normalization for CUDA Cores
        LOG(cudas + 1) / NULLIF(LOG(MAX(cudas) OVER () + 1), 0) AS cudas_normalized,
        -- Log Scaled Normalization for Memory Size
        LOG(memory + 1) / NULLIF(LOG(MAX(memory) OVER () + 1), 0) AS memory_normalized,        
        -- Log Scaled Memory Bandwidth and Normalize
        LOG((memory_clock * memory_bit) / (8 * 1000) + 1) / 
        NULLIF(LOG(MAX((memory_clock * memory_bit) / (8 * 1000)) OVER () + 1), 0) AS memory_bandwidth_normalized,
        -- Log Scaled Normalization for GPU Clock
        LOG(gpu_clock + 1) / NULLIF(LOG(MAX(gpu_clock) OVER () + 1), 0) AS gpu_clock_normalized,
        CASE WHEN chip LIKE '%100' THEN 1  ELSE 0 END AS is_datacenter_gpu
    FROM k_hybrid.mo_common_gpu_spec
),
performance_scores AS (
    SELECT 
        *,
        -- Apply Weights and Calculate Final Performance Score
          (cudas_normalized * 0.4) 
        + (memory_normalized * 0.3) 
        + (memory_bandwidth_normalized * 0.2) 
        + (gpu_clock_normalized * 0.1) 
        + (is_datacenter_gpu * 0.5) 
        AS performance_score
    FROM adjusted_values
)
UPDATE k_hybrid.mo_common_gpu_spec AS c
SET score = a.performance_score
FROM performance_scores a
WHERE c.product = a.product AND c.gpu_clock = a.gpu_clock; 
 */
@Slf4j
public class GpuPerformanceEvaluator {

	CommonService comService = null;
	public void setCommonService(CommonService comService) {
		this.comService = comService;
	}
	
    public Map<String, Integer> getFormance(List<PromMetricNode> lastNodes) {
    	HashMap<String, Double> totalScoreMap = new HashMap<String, Double>();
    	
    	Map minMaxMap = this.comService.getCommonGpuMinMaxScore(); 
    	
    	BigDecimal maxDbGpuScore = (BigDecimal)minMaxMap.get("max");
    	BigDecimal minDbGpuScore = (BigDecimal)minMaxMap.get("min");
    	
    	// 각 머신의 성능 계산
    	for(PromMetricNode n : lastNodes) {
    		Map<String, PromMetricNodeGPU>  gpus = n.getMGpuList();
    		if(gpus == null || gpus.isEmpty()) {
    			//totalScoreMap.put(n.getKey(), 0.0);
    			continue;
    		}
    		
    		Double sumScores = 0.0;
    		//1개의 머신에는 다중 gpu가 있음
    		for(Map.Entry<String, PromMetricNodeGPU> entry : gpus.entrySet()) {
//    			entry.getKey();
    			PromMetricNodeGPU gpu = entry.getValue();
//    			gpu.getModel();  			
    			sumScores += comService.getCommonGpuScore(gpu.getModel());
    		}
    		totalScoreMap.put(n.getKey(), sumScores);
    	}
    	
    	//성능 정규화
    	//double minPerformance = totalScoreMap.values().stream().min(Double::compareTo).orElse(0.0);
        //double maxPerformance = totalScoreMap.values().stream().max(Double::compareTo).orElse(1.0);
        
        double minPerformance = minDbGpuScore.doubleValue();
        double maxPerformance = maxDbGpuScore.doubleValue();
        
        Map<String, Double> normalizedPerformances = totalScoreMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry ->{
                        	//double maxValues = maxPerformance;
                        	//double maxValues = maxDbGpuScore;
//                        	if(maxPerformance < 10)
//                        		maxValues = 10.0;
                        	return (entry.getValue() - minPerformance) / (maxPerformance - minPerformance);
                        }
                ));
    	
        if(log.isDebugEnabled()) {
    		log.debug("GPU Total Gpu: {}",totalScoreMap);
    	}
    	
        totalScoreMap.clear();
        //정규화된 점수를 기반으로 1~10단계 매핑
        Map<String, Integer> performanceRanks = normalizedPerformances.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (int) Math.floor(1 + 9 * entry.getValue())
                ));

        normalizedPerformances.clear();
        
        return performanceRanks;
    }
}
