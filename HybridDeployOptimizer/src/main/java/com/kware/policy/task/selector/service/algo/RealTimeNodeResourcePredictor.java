package com.kware.policy.task.selector.service.algo;

/**
 * 아직확인하지 않은 모델
 * ND4J라이브러리를 활용해서 LSTM 모델로 자원 사용량을 예측한다.
 */

public class RealTimeNodeResourcePredictor {
/*
    private static final int FEATURES_COUNT = 1; // 사용할 특성 수 (시간)
    private static final int NUM_OUTPUTS = 1;   // 출력 수 (가용 용량)

    private INDArray weights;
    private INDArray bias;

    public RealTimeNodeResourcePredictor() {
        // 가중치와 편향 초기화
        weights = Nd4j.randn(FEATURES_COUNT, NUM_OUTPUTS); // 랜덤 가중치 초기화
        bias = Nd4j.zeros(NUM_OUTPUTS); // 초기 편향은 0으로 설정
    }

    // 실시간 데이터를 기반으로 모델 학습
    public void trainRealTime(List<PromMetricNode> nodeData) {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> {
            INDArray data = prepareRealTimeData(nodeData); // 실시간 데이터 준비
            INDArray labels = data.getColumns(1); // 레이블 설정 (여기서는 간단히 예제용으로 시간을 레이블로 사용)

            // 선형 회귀 모델 학습 (여기서는 ND4J의 단순히 랜덤 가중치와 편향을 초기화)
            // 특성 X와 레이블 Y를 사용하여 학습
            for (int i = 0; i < 100; i++) { // 예제로 100번 반복하여 학습
                INDArray predictions = data.mmul(weights).add(bias); // 예측값 계산
                INDArray loss = labels.sub(predictions); // 손실 계산
                INDArray gradient = data.transpose().mmul(loss).div(data.size(0)); // 그라디언트 계산
                weights.addi(gradient); // 가중치 업데이트
                bias.addi(loss.mean(0)); // 편향 업데이트
            }
        }, 0, 10, TimeUnit.MINUTES); // 10분마다 실행 (실제로는 데이터 업데이트에 맞게 조정)

        // executorService.shutdown(); // 원하는 시점에 종료
    }

    // 실시간 데이터 준비 및 전처리
    private INDArray prepareRealTimeData(List<PromMetricNode> nodeData) {
        int dataSize = nodeData.size();
        INDArray data = Nd4j.zeros(dataSize, FEATURES_COUNT + 1); // 시간 데이터를 포함한 입력 데이터

        for (int i = 0; i < dataSize; i++) {
            PromMetricNode node = nodeData.get(i);
            long timestamp = node.getCollectDt().getTime();
            double availableCpu = node.getAvailableCpu();
            double availableMemory = node.getAvailableMemory();
            double availableGpu = node.getAvailableGpu();
            double availableDisk = node.getAvailableDisk();

            // 입력 데이터 설정 (여기서는 간단히 시간만 사용)
            data.putScalar(i, 0, timestamp);
            // 추가적으로 필요한 경우 다른 리소스도 데이터에 추가 가능
        }

        return data;
    }

    // 예측 메서드
    public void predictNextTimestamp(List<PromMetricNode> nodeData, long nextTimestamp) {
        // 데이터 준비
        INDArray data = prepareRealTimeData(nodeData);

        // 예측 데이터 생성
        INDArray input = Nd4j.create(new double[]{nextTimestamp});
        INDArray prediction = input.mmul(weights).add(bias);

        // 예측 결과 출력 (여기서는 예시로 출력만 하지만, 실제 활용에 맞게 사용하면 됩니다)
        System.out.println("10분 후 예상 CPU 가용 용량: " + prediction.getDouble(0));
        // 다른 리소스에 대한 예측도 추가 가능
    }
*/
    /*
    public static void main(String[] args) {
        // 예제 데이터 생성 (실제 데이터는 데이터베이스나 외부에서 가져올 수 있음)
        List<PromMetricNode> nodeData = DataGenerator.generateNodeData();

        // 예측기 초기화 및 실시간 학습 시작
        RealTimeNodeResourcePredictor predictor = new RealTimeNodeResourcePredictor();
        predictor.trainRealTime(nodeData);

        // 예측
        long currentTimestamp = System.currentTimeMillis();
        long nextTimestamp = currentTimestamp + 10 * 60 * 1000; // 10분 후 시간
        predictor.predictNextTimestamp(nodeData, nextTimestamp);

        // 원하는 시점에 executorService.shutdown(); 호출하여 종료 가능
    }
    */
}
