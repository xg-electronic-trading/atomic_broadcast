package jmh.orderstate;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

public class BenchmarkRunner {

    private static final Log log = LogFactory.getLog(BenchmarkRunner.class.getName());

    public static void main(String[] args) {
        try {
            System.setProperty("agrona.disable.bounds.checks", "true");
            Options opt = new OptionsBuilder()
                    .include(OrderStateCacheBenchmark.class.getSimpleName())
                    .include(MapOrderStateCacheBenchmark.class.getSimpleName())
                    .forks(1)
                    .verbosity(VerboseMode.EXTRA)
                    .build();

            new Runner(opt).run();

        } catch (Exception e) {
            log.error().append("benchmark exception: ").appendLast(e);
        }
    }
}
