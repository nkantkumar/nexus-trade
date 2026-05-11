package com.trading.matching;

import java.util.concurrent.TimeUnit;

public class MatchingEngineBenchmark {
    private static final int WARMUP_ITERATIONS = 10000;
    private static final int TEST_ITERATIONS = 100000;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void testMatchingThroughput(Blackhole blackhole) {
        MatchingEngine engine = new MatchingEngine();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Order buy = new Order("B" + i, Order.Side.BUY, 100.0 + i, 100);
            Order sell = new Order("S" + i, Order.Side.SELL, 100.0 + i, 100);

            engine.processOrder(buy);
            engine.processOrder(sell);
        }
    }
}
