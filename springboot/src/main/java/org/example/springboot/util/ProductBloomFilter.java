package org.example.springboot.util;

import java.util.BitSet;

/**
 * 轻量布隆过滤器（缓存穿透防护）。
 *
 * <p>容量公式（可向面试官推导）：
 * <pre>
 *   m = -n·ln(p) / (ln2)²   （位数组大小，n=期望元素数，p=误判率）
 *   k = m/n · ln2           （哈希函数个数）
 * </pre>
 * 相比 hutool 的 {@code BitMapBloomFilter(int)}（其内部按 m/5*8MB 分配位，容量过大会 OOM），
 * 本实现按公式精确分配，20000 元素规模仅约 24KB，且行为完全可控、可单测。
 */
public class ProductBloomFilter {

    /** 位数组 */
    private final BitSet bits;
    /** 位数组大小 */
    private final long size;
    /** 哈希函数个数 */
    private final int hashCount;

    public ProductBloomFilter(int expectedElements, double falsePositiveRate) {
        int n = Math.max(1, expectedElements);
        double p = Math.max(1e-9, Math.min(1 - 1e-9, falsePositiveRate));
        double ln2 = Math.log(2);
        // m = -n·ln(p) / (ln2)²
        long m = (long) Math.ceil(-n * Math.log(p) / (ln2 * ln2));
        // 兜底：限制到 500 万位（约 0.6MB），避免误配置撑爆内存
        this.size = Math.min(Math.max(m, 64L), 5_000_000L);
        int k = (int) Math.round(this.size / n * ln2);
        this.hashCount = Math.max(1, Math.min(k, 32));
        this.bits = new BitSet((int) this.size);
    }

    public synchronized void add(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        int h1 = value.hashCode();
        int h2 = h1 >>> 16;
        for (int i = 0; i < hashCount; i++) {
            int idx = Math.floorMod(h1 + i * h2, (int) size);
            bits.set(idx);
        }
    }

    public synchronized boolean contains(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        int h1 = value.hashCode();
        int h2 = h1 >>> 16;
        for (int i = 0; i < hashCount; i++) {
            int idx = Math.floorMod(h1 + i * h2, (int) size);
            if (!bits.get(idx)) {
                return false;
            }
        }
        return true;
    }

    public long bitSize() {
        return size;
    }

    public int hashCount() {
        return hashCount;
    }
}
