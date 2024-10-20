package com.terry.research.snowflake;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidCreator;
import com.terry.research.snowflake.generator.Snowflake;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
class SnowflakeIdGeneratorApplicationTests {
    String randomAddress() {
        SecureRandom secureRandom = new SecureRandom();
        return new StringJoiner(".")
            .add(String.valueOf(secureRandom.nextInt(256)))
            .add(String.valueOf(secureRandom.nextInt(256)))
            .add(String.valueOf(secureRandom.nextInt(256)))
            .add(String.valueOf(secureRandom.nextInt(256))).toString();
    }

    public String encodeAddress(String address) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
                throw new UnknownHostException("Localhost is unaccepted");
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        String[] parts = address.split("\\.");
        int p1 = Integer.parseInt(parts[0]);
        int p2 = Integer.parseInt(parts[1]);
        int p3 = Integer.parseInt(parts[2]);
        int p4 = Integer.parseInt(parts[3]);
        int rs = ((1 << 25) - 1) & (p1 << 24 | p2 << 16 | p3 << 8 | p4);

        char[] reserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345".toCharArray();
        StringBuilder sb = new StringBuilder();
        while (rs!=0) {
            sb.append(reserved[rs % 32]);
            rs /= 32;
        }

        return sb.reverse().toString();
    }

    public String podName() {
        return "26GHI";
    }

    long customEpoch = 1420070400000L;
    Snowflake snowflake = Snowflake.instance(customEpoch);

    public long nextId() {
        try {
            return snowflake.nextId();
        } catch (InterruptedException e) {
            System.err.println(
                "Interrupted exception"
            );
            log.error("error", e);
        }
        return 0L;
    }

    @SneakyThrows
    @Test
    void testPerformance() {
        int iteration = 1_000_000;
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        Future<Long>[] futures = new Future[iteration];
        long start = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(iteration);

        try {
            for (int i = 0; i < iteration; i++) {
                futures[i] = executorService.submit(() -> {
//                    Tsid tsid = TsidCreator.getTsid();
//                    String uid = tsid.encode(36);
                    long id = nextId();
                    countDownLatch.countDown();
                    return id;
                });
            }
            countDownLatch.await();
            long end = System.currentTimeMillis();
            System.err.printf("generate %,3d in %3d ms\r%n", iteration, (end - start));
            System.err.printf("Performance %,3d ops/millisecond\r%n", iteration / (end - start));

            if (iteration >= 10_000_000) {
                executorService.shutdownNow();
                System.exit(1);
            }
            // if interations is over > 10_000_000, skip output file.
            File file = new File(System.getProperty("user.dir").concat("/").concat("snowflake.txt"));
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                Set<String> set = new HashSet<>(iteration);
                int batch = 0;
                for (Future<Long> future : futures) {
                    String s = podName() + snowflake.encode(future.get());
                    if (set.contains(s)) {
                        System.err.println("Failed!!! " + s);
                        break;
                    }
                    set.add(s);
                    outputStream.write(s.concat("\t").getBytes());
                    if (batch++ < 100) continue;
                    outputStream.write("\r\n".getBytes());
                    batch = 0;
                }
            }
        } catch (InterruptedException interruptedException) {
            System.out.println(interruptedException.toString());
            executorService.shutdown();
        } catch (IOException ioException) {
            System.err.println(ioException.toString());
        } finally {
            executorService.shutdown();
        }
    }
}
