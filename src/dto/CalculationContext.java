package dto;

import configuration.dto.TaskConfiguration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public record CalculationContext(
        TaskConfiguration configuration,

        AtomicInteger taskCount,
        AtomicInteger nextIndexToWrite,

        CountDownLatch taskComplete,

        BlockingQueue<InputItem> taskQueue,
        BlockingQueue<ResultItem> resultQueue,

        ConcurrentMap<Integer, ResultItem> orderedResults,

        ExecutorService pool) {
}