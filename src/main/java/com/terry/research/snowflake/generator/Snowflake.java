package com.terry.research.snowflake.generator;

import lombok.Builder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
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
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BIT) - 1;

    private volatile long sequence = 1L;
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

    ReentrantLock reentrantLock = new ReentrantLock(true);

    public synchronized long nextId() throws InterruptedException {
        long currentTimestamp = currentTimestamp();
        while (!reentrantLock.tryLock(1L, TimeUnit.NANOSECONDS)) {

        }
        try {
            if (currentTimestamp==lastTimestamp) {
                sequence = (1 + sequence) & MAX_SEQUENCE;
                if (sequence==0) {
                    do {
                        currentTimestamp = currentTimestamp();
                    } while (currentTimestamp==lastTimestamp);
                    sequence = 0;
                }


            } else if (currentTimestamp > lastTimestamp) {
                sequence = 0;
            } else {
                throw new InterruptedException(String.format("current timestamp %d could not be less than the last timestamp %d", currentTimestamp, lastTimestamp));
            }
            lastTimestamp = currentTimestamp;
            return (currentTimestamp << SEQUENCE_BIT) | sequence;
        } finally {
            reentrantLock.unlock();
        }
    }

    public String nextUid() throws InterruptedException {
        String uuid = encode(nextId());
        uuid = uuid + "A".repeat(11 - uuid.length());
        return uuid;
    }

    private long currentTimestamp() {
        return MAX_TIMESTAMP & ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli() - customEpoch;
    }

    private String encode(long result) {
        final int base = 32;
        final char[] reserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345".toCharArray();
        StringBuilder sb = new StringBuilder();

        while (result!=0) {
            int idx = (int) (result % base);
            sb.append(reserved[idx]);
            result /= base;
        }
        return sb.reverse().toString();
    }
}
