package com.nyzg.swiftsail.bean;

public class HealthIndicator {
    /**
     * 睡眠时长评分（单位：毫秒）
     * 推荐：7–9 小时为最佳，<4 或 >12 为极差
     */
    static public float sleepIndicator(int hours) {
        if (hours <= 0) return 0.0f; // 默认偏低分
        if (hours >= 7 && hours <= 9) return 1.0f;

        // 左侧：4~7 小时线性上升
        if (hours >= 4 && hours < 7) {
            return (float) ((hours - 4) / 3.0f);
        }
        // 右侧：9~12 小时线性下降
        if (hours > 9 && hours <= 12) {
            return (float) (1.0f - (hours - 9) / 3.0f);
        }
        // <4 或 >12
        if (hours < 4) return (float) Math.max(0.0, hours / 4.0 * 0.3);
        return (float) Math.max(0.0, (16 - hours) / 4.0 * 0.3);
    }

    /**
     * 日行步数评分
     * 推荐：8000–10000+ 步为佳，<2000 为久坐
     */
    static public float stepIndicator(long step) {
        if (step <= 0) return 0.0f;

        if (step >= 10000) return 1.0f;
        if (step <= 2000) return (float) Math.max(0.0, step / 2000.0 * 0.3);

        // 2000 ~ 10000 线性增长
        return (float) (0.3 + (step - 2000) / 8000.0 * 0.7);
    }

    /**
     * 静息心率评分（单位：bpm）
     * 健康范围：50–70 bpm（运动员可能更低）
     * >100 为心动过速，<40 需谨慎（除非是运动员）
     */
    static public float restHeartRateIndicator(int r) {
        if (r <= 0) return 0.0f; // 未知给中等分

        if (r < 30 || r > 120) return 0.0f;
        if (r >= 50 && r <= 70) return 1.0f;

        // 40–50：良好 → 满分
        if (r >= 40 && r < 50) {
            return (r - 40) / 10.0f;
        }
        // 70–90：逐渐下降
        if (r > 70 && r <= 90) {
            return 1.0f - (r - 70) / 20.0f;
        }
        // 30–40 或 90–120：低分
        if (r < 40) {
            return (r - 30) / 10.0f * 0.5f;
        }
        return (120 - r) / 30.0f * 0.5f;
    }

    /**
     * 热量平衡评分：消耗 ≈ 摄入 为佳（避免大幅盈余或赤字）
     * 假设基础代谢约 1800–2200 卡，推荐摄入 2000 卡
     * 这里简化：消耗在 1500–2500 且与摄入差值小则高分
     */
    static public float caloricBalanceIndicator(long intake, long burned) {
        // 如果任一缺失，只看消耗是否合理
        if (burned <= 0) return 0.0f;

        // 默认摄入按 2000 卡估算
        intake = intake <= 0 ? intake : 2000L;

        // 消耗合理性（1000–3000）
        double burnScore = 0.0;
        if (burned >= 1500 && burned <= 2500) {
            burnScore = 1.0;
        } else if (burned >= 1000 && burned < 1500) {
            burnScore = (burned - 1000) / 500.0;
        } else if (burned > 2500 && burned <= 3500) {
            burnScore = 1.0 - (burned - 2500) / 1000.0;
        } else {
            burnScore = 0.1;
        }

        // 平衡性：|burn - intake| 越小越好（理想差值 < 300）
        long diff = Math.abs(burned - intake);
        double balanceScore = Math.max(0.0, 1.0 - diff / 1000.0); // 差1000卡则0分

        return (float) (burnScore * 0.6 + balanceScore * 0.4);
    }

    /**
     * 运动距离评分（单位：公里，传入的是 Long，代表公里数 × 1000？需确认）
     * 注意：你的 distance 是 "公里" 字符串，但 getExposeValue 返回 Long
     * 假设：distance = 实际公里数 × 1000（例如 5.2 公里 → 5200）
     * 如果是直接公里整数（如 5 表示 5 公里），请调整 scale
     */
    static public float distanceIndicator(double kilometers) {
        if (kilometers <= 1e-5) {
            return 0.0f;
        }

        // 最佳区间：3 ~ 8 公里/天（覆盖快走、慢跑、骑行等）
        if (kilometers >= 3f && kilometers <= 8f) {
            return 1.0f;
        }

        // 1 ~ 3 公里：线性上升
        if (kilometers >= 1 && kilometers < 3) {
            return (float) ((kilometers - 1) / 2.0f);
        }

        // 8 ~ 15 公里：缓慢下降（鼓励但不过度）
        if (kilometers > 8 && kilometers <= 15) {
            return (float) (1.0 - (kilometers - 8) / 7.0 * 0.3); // 最低 0.7
        }

        // 0.5 ~ 1 公里：低分但非零
        if (kilometers >= 0.5 && kilometers < 1) {
            return (float) ((kilometers - 0.5) / 0.5 * 0.3); // 0.0 → 0.3
        }

        // <0.5 公里：接近久坐
        if (kilometers < 0.5) {
            return (float) (kilometers / 0.5 * 0.2); // 最多 0.2
        }

        // >15 公里：可能过度训练，分数缓降
        if (kilometers > 15 && kilometers <= 30) {
            return (float) (0.7 - (kilometers - 15) / 15.0 * 0.5); // 降至 0.2
        }

        // >30 公里：极高强度，除非专业运动员，否则扣分
        return (float) Math.max(0.0, 0.2 - (kilometers - 30) / 20.0);
    }
}
