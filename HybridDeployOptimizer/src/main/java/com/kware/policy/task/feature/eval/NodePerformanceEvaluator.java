package com.kware.policy.task.feature.eval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kware.policy.task.collector.service.vo.PromMetricNode;
import com.kware.policy.task.collector.service.vo.PromMetricNodeGPU;

import lombok.Data;

/**
 * Weighted Scoring and Classification for Machine Performance Evaluation
 */
public class NodePerformanceEvaluator {
 
	// QueueManager qm = QueueManager.getInstance();	
	// PromQueue    promQ         = qm.getPromQ();
    // RequestQueue requestQ      = qm.getRequestQ();
	// APIQueue     apiQ          = qm.getApiQ(); 
	
	
	public Map<String, Integer> getFormanceScore(List<PromMetricNode> lastNodes) {
		//Map<String, ClusterNode> clusterNodes = apiQ.getApiClusterNodeMap();
//		Map<String, ClusterNode> clusterNodes = apiQ.getReadOnlyApiMap(APIMapsName.NODE);
//		List<PromMetricNode> lastNodes = promQ.getLastPromMetricNodesReadOnly();
		
		double minCpuCount = Double.MAX_VALUE, maxCpuCount = Double.MIN_VALUE;
        double minCpuClock = Double.MAX_VALUE, maxCpuClock = Double.MIN_VALUE;
        double minMem      = Double.MAX_VALUE, maxMem      = Double.MIN_VALUE;
        double minDisk     = Double.MAX_VALUE, maxDisk     = Double.MIN_VALUE;
        double minGpuCount = Double.MAX_VALUE, maxGpuCount = Double.MIN_VALUE;
        double minGpuMem   = Double.MAX_VALUE, maxGpuMem   = Double.MIN_VALUE;
        
		for(PromMetricNode n: lastNodes) {
			// CPU 개수
			if (n.getCapacityCpu() < minCpuCount) minCpuCount = n.getCapacityCpu();
            if (n.getCapacityCpu() > maxCpuCount) maxCpuCount = n.getCapacityCpu();
            
            // CPU 클록
            if (n.getCapacityMaxHzCpu() < minCpuClock) minCpuClock = n.getCapacityMaxHzCpu();
            if (n.getCapacityMaxHzCpu() > maxCpuClock) maxCpuClock = n.getCapacityMaxHzCpu();

            // 메모리
            if (n.getCapacityMemory() < minMem) minMem = n.getCapacityMemory();
            if (n.getCapacityMemory() > maxMem) maxMem = n.getCapacityMemory();

            // 디스크
            if (n.getCapacityDisk() < minDisk) minDisk = n.getCapacityDisk();
            if (n.getCapacityDisk() > maxDisk) maxDisk = n.getCapacityDisk();

            // GPU 개수
            if (n.getCapacityGpu() < minGpuCount) minGpuCount = n.getCapacityGpu();
            if (n.getCapacityGpu() > maxGpuCount) maxGpuCount = n.getCapacityGpu();

            // GPU 메모리
            double memAvg = this.getGpuMemoryAverage(n.getMGpuList());
            if (memAvg < minGpuMem) minGpuMem = memAvg;
            if (memAvg > maxGpuMem) maxGpuMem = memAvg;
		}
		
		 // 3) Min-Max 정규화 & Setting
		Map<String, Machine> NodeNormals = new HashMap<String, Machine>();
		for(PromMetricNode n: lastNodes) {
			Machine m = new Machine();
            m.cpuCountNorm  = normalize(n.getCapacityCpu()     , minCpuCount, maxCpuCount);
            m.cpuClockNorm  = normalize(n.getCapacityMaxHzCpu(), minCpuClock, maxCpuClock);
            m.memoryNorm    = normalize(n.getCapacityMemory()  , minMem     , maxMem);
            m.diskNorm      = normalize(n.getCapacityDisk()    , minDisk    , maxDisk);
            m.gpuCountNorm  = normalize(n.getCapacityGpu()     , minGpuCount, maxGpuCount);
            m.gpuMemoryNorm = normalize(this.getGpuMemoryAverage(n.getMGpuList()), minGpuMem, maxGpuMem);
                        
            NodeNormals.put(n.getKey(), m);
        }

        // 4) 가중치 (예시)
        double wCpuCount  = 0.20;
        double wCpuClock  = 0.15;
        double wMem       = 0.20;
        double wDisk      = 0.10;
        double wGpuCount  = 0.20;
        double wGpuMem    = 0.15;

        // 5) 종합 점수 계산
        Map<String, Integer> resultScores = new HashMap<String, Integer>();
        
        for(Map.Entry<String, Machine> em : NodeNormals.entrySet()) {
        	Machine m = em.getValue();
            double score = 
                  wCpuCount  * m.getCpuCountNorm()
                + wCpuClock  * m.getCpuClockNorm()
                + wMem       * m.getMemoryNorm()
                + wDisk      * m.getDiskNorm()
                + wGpuCount  * m.getGpuCountNorm()
                + wGpuMem    * m.getGpuMemoryNorm();

            m.setScore(score);

            // 6) 1~10 등급 매핑
            //   score=0 → 1등급, score=1 → 10등급
            //   중간 값은 [ (score * 9) + 1 ] or 구간별 if문
            int grade = mapScoreToGrade(score);
            m.setGrade(grade);
            
            resultScores.put(em.getKey(), grade);
        }

        return resultScores;
	}
	
	private double getGpuMemoryAverage( Map<String, PromMetricNodeGPU>  _pg) {
		long memSum = 0;
        double memAvg = 0.0;
        for(Map.Entry<String, PromMetricNodeGPU> k : _pg.entrySet()) {
        	PromMetricNodeGPU gpu = k.getValue();
        	memSum += gpu.getCapacityMemory();
        }
        if(_pg.size() != 0) {
        	memAvg = memSum / _pg.size();
        }
        return memAvg;
	}
	
	
	@Data
	private class Machine {
		/*private double cpuCount;
		private double cpuClock;
		private double memory;
		private double disk;
		private double gpuCount;
		private double gpuMemory;
		*/
	    // 정규화된 값
	    private double cpuCountNorm;
	    private double cpuClockNorm;
	    private double memoryNorm;
	    private double diskNorm;
	    private double gpuCountNorm;
	    private double gpuMemoryNorm;

	    private double score; // 종합 점수
	    private int grade;    // 등급 (1~10)

	}

    // 최소-최대 정규화
    private double normalize(double value, double minVal, double maxVal) {
        if (Math.abs(maxVal - minVal) < 1e-9) {
            // max와 min이 같으면 분모가 0 -> 0.0 처리 or 1.0 등 임의
            return 0.0;
        }
        return (value - minVal) / (maxVal - minVal);
    }

    // 점수를 1~10등급으로 매핑
    // 단순하게 "score=0.0 -> 1등급, 1.0 -> 10등급"이라 가정
    private int mapScoreToGrade(double score) {
        // 등급을 float 구간으로 변환
        // score in [0,1] -> grade in [1..10]
        // grade = 1 + floor( score * 9 )
        //   0 <= score < 1
        //   e.g., score=0.0 -> grade=1, score=1.0 -> grade=10
        //   score=0.73 -> grade=1+ floor(0.73*9)=1+ floor(6.57)=7 -> 8등급(조정가능)

        // 어떤 방식을 쓰든 상관없음. 여기서는 조금 더 직관적으로 계산:
        double scaled = score * 9;     // 0~9
        int g = (int)Math.floor(scaled);
        int grade = g + 1;            // 1~10
        if (grade < 1) grade = 1;
        if (grade > 10) grade = 10;
        return grade;
    }
}
