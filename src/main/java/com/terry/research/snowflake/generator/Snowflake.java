package com.terry.research.snowflake.generator;

import lombok.Builder;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.concurrent.locks.ReentrantLock;

public class Snowflake {

    /**
     * 55-bit UUID generator
     * <p>
     * [TIMESTAMP_BIT]
     * [SEQUENCE_BIT]
     */
    private static final int UNUSED_BIT = 1;
    private static final int TIMESTAMP_BIT = 42;
    private static final int SEQUENCE_BIT = 12;

    private static final long MAX_TIMESTAMP = (1L << TIMESTAMP_BIT) - 1;
    private static final int MAX_SEQUENCE = (1 << SEQUENCE_BIT) - 1;

    private volatile int sequence = 0;
    private volatile long lastTimestamp = 0L;
    private final long customEpoch;

    private static Snowflake instance;
    private static final Object lock = new Object();

    private Snowflake(long customEpoch) {
        this.customEpoch = customEpoch;
    }

    @Builder
    public static Snowflake instance(long customEpoch) {
        if (instance!=null) return instance;
        synchronized (lock) {
            if (instance==null) {
                instance = new Snowflake(customEpoch);
                return instance;
            }
            return instance;
        }
    }

    SecureRandom random = new SecureRandom();
    volatile int cnt = MAX_SEQUENCE;

    public synchronized long nextId() throws InterruptedException {
        long currentTimestamp = currentTimestamp();
        if (currentTimestamp==lastTimestamp) {
            if (cnt==0) {
                do {
                    currentTimestamp = currentTimestamp();
                } while (currentTimestamp==lastTimestamp);
                cnt = MAX_SEQUENCE;
                sequence = random.nextInt(1 << SEQUENCE_BIT);
            }

            ++sequence;
            sequence = sequence & MAX_SEQUENCE;
            cnt--;
        } else if (currentTimestamp > lastTimestamp) {
            sequence = random.nextInt(1 << SEQUENCE_BIT);
            cnt--;
        } else {
            throw new InterruptedException(
                String.format("current timestamp %d cannot be less than the last time %s stamp",
                    currentTimestamp, lastTimestamp)
            );
        }
        lastTimestamp = currentTimestamp;
        return (currentTimestamp << SEQUENCE_BIT) | sequence;
    }

    public String nextUid(String podName) throws InterruptedException {
        String uuid = encode(nextId());
        uuid = uuid + "A".repeat(11 - uuid.length());
        return podName.toUpperCase() + uuid;
    }

    private long currentTimestamp() {
        return MAX_TIMESTAMP & Clock.systemUTC().millis() - customEpoch;
    }

    public String encode(long result) {
        final int base = 32;
        final char[] reserved = "ABCDEFGHIJKLMNPQRSTUVWXYZ2456789".toCharArray();
        StringBuilder sb = new StringBuilder();

        while (result!=0) {
            int idx = (int) (result % base);
            sb.append(reserved[idx]);
            result /= base;
        }
        return sb.reverse().toString();
    }
}
