package kware.app.clsel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulatedAnnealingBinPacking {

	public static void main(String[] args) {
		// 노드 정보: CPU, Memory, GPU, GPU Memory, Disk
		List<Node> nodes = new ArrayList<>();
		nodes.add(new Node(8, 16, 2, 8, 100));
		nodes.add(new Node(12, 32, 4, 16, 200));
		// 추가 노드 정보 입력

		// 워크로드 요청 정보
		Node workload = new Node(6, 12, 1, 4, 50);

		// 초기 솔루션 생성 (랜덤 아이템 배치)
		int numItems = nodes.size();
		int numBins = nodes.size();
		List<List<Integer>> currentSolution = new ArrayList<>(numBins);
		for (int i = 0; i < numBins; i++) {
			currentSolution.add(new ArrayList<>());
		}
		Random random = new Random();
		for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {
			int binIndex = random.nextInt(numBins);
			currentSolution.get(binIndex).add(itemIndex);
		}
		double currentCost = calculateUsage(nodes, currentSolution, workload);

		// 초기 온도 및 냉각률 설정
		double initialTemperature = 100.0;
		double coolingRate = 0.95;

		// SA 메인 루프
		while (initialTemperature > 1) {
			// 랜덤하게 선택된 아이템을 다른 bin으로 이동
			List<List<Integer>> newSolution = new ArrayList<>(currentSolution);
			int itemIndex = random.nextInt(numItems);
			int sourceBinIndex = random.nextInt(numBins);
			int destBinIndex = random.nextInt(numBins);
			newSolution.get(sourceBinIndex).remove(Integer.valueOf(itemIndex));
			newSolution.get(destBinIndex).add(itemIndex);

			double newCost = calculateUsage(nodes, newSolution, workload);

			// 비용 차이 계산
			double costDifference = newCost - currentCost;

			// 새로운 솔루션이 더 나은지 확인
			if (costDifference < 0 || random.nextDouble() < Math.exp(-costDifference / initialTemperature)) {
				currentSolution = newSolution;
				currentCost = newCost;
			}

			// 온도를 냉각
			initialTemperature *= coolingRate;
		}

		// 최적 배치 출력
		System.out.println("최적 배치:");
		for (int i = 0; i < numBins; i++) {
			System.out.println("Bin " + (i + 1) + ": " + currentSolution.get(i));
		}
	}

	private static double calculateUsage(List<Node> nodes, List<List<Integer>> bins, Node workload) {
		double totalUsage = 0;
		for (int i = 0; i < bins.size(); i++) {
			Node binUsage = new Node(0, 0, 0, 0, 0);
			for (int itemIndex : bins.get(i)) {
				Node item = nodes.get(itemIndex);
				binUsage.add(item);
			}
			totalUsage += binUsage.calculateUsage(workload);
		}
		return totalUsage;
	}
}

class Node {
	int cpu;
	int memory;
	int gpu;
	int gpuMemory;
	int disk;

	public Node(int cpu, int memory, int gpu, int gpuMemory, int disk) {
		this.cpu = cpu;
		this.memory = memory;
		this.gpu = gpu;
		this.gpuMemory = gpuMemory;
		this.disk = disk;
	}

	public void add(Node other) {
		this.cpu += other.cpu;
		this.memory += other.memory;
		this.gpu += other.gpu;
		this.gpuMemory += other.gpuMemory;
		this.disk += other.disk;
	}

	public double calculateUsage(Node workload) {
		double usage = 0;
		if (cpu > 0)			usage += (double) (cpu - workload.cpu) / cpu;
		if (memory > 0)			usage += (double) (memory - workload.memory) / memory;
		if (gpu > 0)			usage += (double) (gpu - workload.gpu) / gpu;
		if (gpuMemory > 0)		usage += (double) (gpuMemory - workload.gpuMemory) / gpuMemory;
		if (disk > 0)			usage += (double) (disk - workload.disk) / disk;

		return usage;
	}
}

