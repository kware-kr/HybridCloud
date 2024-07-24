package com.kware.policy.task.selector;

import java.util.*;
/**
 * 모든 노드는 1코어(1000m) 여유, 메모리는 512M 여유, 디스크 2G여유, GPU: 관련없음
 * 이 정보는 설정을 통해서 DB에서 관리하고, 화면에서 설정할 수 있으면 좋겠네. 
 */
class Node {
    String name;
    int cpuCapacity;
    int memoryCapacity;
    int gpuCapacity;
    int networkCapacity;
    int diskIoCapacity;
    
    int cpuUsed;
    int memoryUsed;
    int gpuUsed;
    int networkUsed;
    int diskIoUsed;

    public Node(String name, int cpuCapacity, int memoryCapacity, int gpuCapacity, int networkCapacity, int diskIoCapacity) {
        this.name = name;
        this.cpuCapacity     = cpuCapacity;
        this.memoryCapacity  = memoryCapacity;
        this.gpuCapacity     = gpuCapacity;
        this.networkCapacity = networkCapacity;
        this.diskIoCapacity  = diskIoCapacity;
        
        this.cpuUsed     = 0;
        this.memoryUsed  = 0;
        this.gpuUsed     = 0;
        this.networkUsed = 0;
        this.diskIoUsed  = 0;
    }

    public boolean canAccommodate(int cpu, int memory, int gpu, int network, int diskIo) {
        return (cpuUsed     + cpu     <= cpuCapacity    ) &&
               (memoryUsed  + memory  <= memoryCapacity ) &&
               (gpuUsed     + gpu     <= gpuCapacity    ) &&
               (networkUsed + network <= networkCapacity) &&
               (diskIoUsed  + diskIo  <= diskIoCapacity );
    }

    public void allocateResources(int cpu, int memory, int gpu, int network, int diskIo) {
        if (canAccommodate(cpu, memory, gpu, network, diskIo)) {
            cpuUsed     += cpu;
            memoryUsed  += memory;
            gpuUsed     += gpu;
            networkUsed += network;
            diskIoUsed  += diskIo;
        } else {
            throw new IllegalArgumentException("Insufficient resources on node " + name);
        }
    }
}

class Cluster {
    String name;
    List<Node> nodes;

    public Cluster(String name) {
        this.name = name;
        this.nodes = new ArrayList<>();
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public Node findBestFitNode(int cpu, int memory, int gpu, int network, int diskIo) {
        Node bestNode = null;
        for (Node node : nodes) {
            if (node.canAccommodate(cpu, memory, gpu, network, diskIo)) {
                if (bestNode == null || compareNodes(node, bestNode, cpu, memory, gpu, network, diskIo) < 0) {
                    bestNode = node;
                }
            }
        }
        return bestNode;
    }

    private int compareNodes(Node a, Node b, int cpu, int memory, int gpu, int network, int diskIo) {
        int scoreA = (a.cpuCapacity     - a.cpuUsed)     - cpu     +
                     (a.memoryCapacity  - a.memoryUsed)  - memory  +
                     (a.gpuCapacity     - a.gpuUsed)     - gpu     +
                     (a.networkCapacity - a.networkUsed) - network +
                     (a.diskIoCapacity  - a.diskIoUsed)  - diskIo;
        
        int scoreB = (b.cpuCapacity     - b.cpuUsed)     - cpu     +
                     (b.memoryCapacity  - b.memoryUsed)  - memory  +
                     (b.gpuCapacity     - b.gpuUsed)     - gpu     +
                     (b.networkCapacity - b.networkUsed) - network +
                     (b.diskIoCapacity  - b.diskIoUsed)  - diskIo;
        return scoreA - scoreB;
    }
}

class Scheduler {
    List<Cluster> clusters;

    public Scheduler() {
        this.clusters = new ArrayList<>();
    }

    public void addCluster(Cluster cluster) {
        clusters.add(cluster);
    }

    public Node schedule(int cpu, int memory, int gpu, int network, int diskIo) {
        Node bestNode = null;
        for (Cluster cluster : clusters) {
            Node node = cluster.findBestFitNode(cpu, memory, gpu, network, diskIo);
            if (node != null) {
                if (bestNode == null || cluster.findBestFitNode(cpu, memory, gpu, network, diskIo) != null) {
                    bestNode = node;
                }
            }
        }
        if (bestNode != null) {
            bestNode.allocateResources(cpu, memory, gpu, network, diskIo);
        }
        return bestNode;
    }
}

/**
 * gpt 테스트 코드임
 */
public class NodeSelectorMain {
    public static void main(String[] args) {
        // Example setup
        Node node1 = new Node("node1", 100, 200, 2, 1000, 500);
        Node node2 = new Node("node2", 150, 300, 4, 1200, 600);
        Cluster cluster1 = new Cluster("cluster1");
        cluster1.addNode(node1);
        cluster1.addNode(node2);

        Scheduler scheduler = new Scheduler();
        scheduler.addCluster(cluster1);

        // Example workload
        int cpu = 50;
        int memory = 100;
        int gpu = 1;
        int network = 500;
        int diskIo = 300;

        Node allocatedNode = scheduler.schedule(cpu, memory, gpu, network, diskIo);

        if (allocatedNode != null) {
            System.out.println("Workload allocated to node: " + allocatedNode.name);
        } else {
            System.out.println("No suitable node found for the workload.");
        }
    }
}
