package com.kware.policy.task.feature.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 워크로드 특성이 없는 경우에는 요청에 있는 
 * 
 * 우선순위 스케줄링 알고리즘 (Weighted Priority Scheduling)
 
 우선순위 5단계(P1~P5)
	P1: 매우 높음 (Highest)
	P2: 높음
	P3: 보통 (Normal)
	P4: 낮음
	P5: 매우 낮음 (Lowest)

 환경(dev, test, production)
	dev: 개발 환경, 실험/테스트 코드를 마음껏 실행하는 용도
	test: 검증 환경, 운영에 들어가기 전 사전 검증 목적
	production: 실제 비즈니스에 영향을 주는 운영 환경
	
 작업 시간(긴 시간 / 짧은 시간)  내부적으로 1~2시간(또는 4시간)을 기준으로 구분
	짧은 시간(Short Job): 통상 2시간 이내
	긴 시간(Long Job): 2시간 이상 ~ 수십 시간
   
 Checkpoint 유무
	Checkpoint 있음: 작업을 중단 후 재개 가능 → 선점 허용 가능성 높음
	Checkpoint 없음: 중단 시 재작업 또는 데이터 손실 → 비선점 또는 높은 우선순위지만 중단 불가

 워크로드 유형
	전처리 작업(Preprocessing)
	학습 모델 생성(Training)
	추론 모델 생성(Model Optimization / Model Packaging)
 */
public class PriorityPreemptionEvaluator {

    // 1) 환경 열거형
    public enum Environment {
        DEV, TEST, PROD
    }

    // 2) 작업 유형 열거형
    public enum TaskType {
        PRE,  // 전처리
        TRA,  // 학습
        INF   // 모델 최적화
    }
    
    // 3) 우선 순위 열거형
    public enum Priority {
    	hyCriticalPriority,
    	hyHighPriority,
        hyMediumPriority,
        hyLowPriority,
        hyVeryLowPriority
    }

    // 3) 작업 클래스
    public static class MlWorkloadTask {
        private Environment environment;  // dev, test, production
        private TaskType taskType;        // 전처리, 학습, 모델 최적화
        private boolean hasCheckpoint;    // 체크포인트 여부
        private boolean isShortJob;       // 짧은 작업(true) / 긴 작업(false)

        private int priorityScore;        // 계산된 우선순위 점수
        private String priorityLevel;     // P1~P5
        private boolean preemptive;       // true면 선점, false면 비선점

        public MlWorkloadTask(Environment environment, TaskType taskType,
                              boolean hasCheckpoint, boolean isShortJob) {
            this.environment = environment;
            this.taskType = taskType;
            this.hasCheckpoint = hasCheckpoint;
            this.isShortJob = isShortJob;
        }

        // 우선순위 계산
        public void calculatePriority() {
            int score = 0;

            // (1) 환경 가중치
            switch (environment) {
                case PROD:
                    score += 3;  // 예시로 prod=+3
                    break;
                case TEST:
                    score += 2;  // test=+2
                    break;
                case DEV:
                    score += 1;  // dev=+1
                    break;
            }

            // (2) 작업 시간 가중치
            if (isShortJob) {
                score += 2;      // 짧은 작업이면 +2
            } else {
                score += 1;      // 긴 작업이면 +1
            }

            // (3) 체크포인트 여부 가중치
            // -> 체크포인트가 있다고 해서 무조건 우선순위 높게 할 필요는 없지만,
            //    여기서는 예시로 +1만 줍니다. (원하는 로직에 맞게 조정 가능)
            if (hasCheckpoint) {
                score += 1; 
            }

            // (4) 작업 유형 가중치
            switch (taskType) {
                case PRE:
                    score += 1;  // 전처리
                    break;
                case TRA:
                    score += 2;  // 학습
                    break;
                case INF:
                    score += 1;  // 모델 최적화
                    break;
            }

            this.priorityScore = score;
            this.priorityLevel = mapScoreToPriority(score);

            // 우선순위 계산이 끝난 후, 선점/비선점 결정
            decidePreemption();
        }

        // 점수 -> P1~P5 매핑
        private String mapScoreToPriority(int score) {
            // (예시 구간) 필요하면 범위를 조정하세요
            if (score >= 7) {
                return Priority.hyCriticalPriority.toString();
            } else if (score >= 5) {
                return Priority.hyHighPriority.toString();
            } else if (score >= 3) {
                return Priority.hyMediumPriority.toString();
            } else if (score >= 2) {
                return Priority.hyLowPriority.toString();
            } else {
                return Priority.hyVeryLowPriority.toString();
            }
        }

        // 선점 / 비선점 결정
        private void decidePreemption() {
            // 예시 정책:
            // 1) 체크포인트 있으면 -> 선점
            // 2) 체크포인트 없고 긴 작업(isShortJob=false) -> 비선점
            // 3) 나머지 -> 비선점(필요하면 여기서 다른 분기 추가 가능)
            if (hasCheckpoint) {
                this.preemptive = true;
            } else {
                // 체크포인트 없음
                if (!isShortJob) {
                    // 긴 작업
                    this.preemptive = false;
                } else {
                    // 체크포인트 없음 + 짧은 작업
                    this.preemptive = false; 
                    // 정책에 따라 선점 가능으로 할 수도 있지만, 
                    // 여기서는 예시로 비선점 처리
                }
            }
        }

        // 실행 모드 출력용
        public String getPreemptionMode() {
            return preemptive ? "PreemptLowerPriority" : "Never";
        }

        @Override
        public String toString() {
            return String.format(
                "Task [env=%s, type=%s, chkpt=%s, shortJob=%s, score=%d, priority=%s, %s]",
                environment, taskType, hasCheckpoint, isShortJob,
                priorityScore, priorityLevel, getPreemptionMode()
            );
        }
    }

    // 4) 메인 함수
    public static void main(String[] args) {
        List<MlWorkloadTask> tasks = new ArrayList<>();

        // (env, taskType, hasCheckpoint, isShortJob)
        tasks.add(new MlWorkloadTask(Environment.PROD, TaskType.TRA, false, false));
        tasks.add(new MlWorkloadTask(Environment.PROD, TaskType.TRA, true , true));
        tasks.add(new MlWorkloadTask(Environment.DEV , TaskType.PRE, false, true));
        tasks.add(new MlWorkloadTask(Environment.TEST, TaskType.INF, true , false));
        tasks.add(new MlWorkloadTask(Environment.TEST, TaskType.TRA, false, false));

        // 우선순위 계산
        for (MlWorkloadTask task : tasks) {
            task.calculatePriority();
        }

        // 우선순위 높은 순으로 정렬
        tasks.sort(Comparator.comparingInt((MlWorkloadTask t) -> t.priorityScore).reversed());

        // 결과 출력
        System.out.println("=== 우선순위 및 선점/비선점 결과 ===");
        for (MlWorkloadTask task : tasks) {
            System.out.println(task);
        }
    }
}
