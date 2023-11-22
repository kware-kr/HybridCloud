package kware.app.clsel;

public class HardwarePerformanceModel {
    public static void main(String[] args) {
        // Hardware specifications (you can adjust these values based on your hardware).
        int cpuCores = 8;
        double cpuClock = 3.2; // GHz
        int memoryCapacity = 16; // GB
        double memoryClock = 2666; // MHz
        int gpuCount = 2;
        int gpuCores = 3072;
        int gpuMemory = 8; // GB

        // Define weights for each component (you can adjust these based on your priorities).
        double cpuWeight = 0.4;
        double memoryWeight = 0.3;
        double gpuWeight = 0.3;

        // Normalize values (adjust the range based on your hardware).
        double normalizedCpuCores = normalize(cpuCores, 1, 32); // Assuming a range of 1 to 32 cores.
        double normalizedCpuClock = normalize(cpuClock, 1.0, 4.0); // Assuming a range of 1.0 GHz to 4.0 GHz.
        double normalizedMemoryCapacity = normalize(memoryCapacity, 2, 64); // Assuming a range of 2 GB to 64 GB.
        double normalizedMemoryClock = normalize(memoryClock, 800, 3200); // Assuming a range of 800 MHz to 3200 MHz.
        double normalizedGpuCount = normalize(gpuCount, 0, 4); // Assuming a range of 0 to 4 GPUs.
        double normalizedGpuCores = normalize(gpuCores, 1, 8192); // Assuming a range of 1 to 8192 cores.
        double normalizedGpuMemory = normalize(gpuMemory, 0, 16); // Assuming a range of 0 GB to 16 GB.

        // Calculate the performance score.
        double performanceScore = (cpuWeight * normalizedCpuCores + cpuWeight * normalizedCpuClock +
                                   memoryWeight * normalizedMemoryCapacity + memoryWeight * normalizedMemoryClock +
                                   gpuWeight * normalizedGpuCount + gpuWeight * normalizedGpuCores +
                                   gpuWeight * normalizedGpuMemory) / (cpuWeight + memoryWeight + gpuWeight);

        System.out.println("Performance Score: " + performanceScore);
    }

    public static double normalize(double value, double min, double max) {
        // Normalize the value to the range [0, 1].
        return (value - min) / (max - min);
    }
}
