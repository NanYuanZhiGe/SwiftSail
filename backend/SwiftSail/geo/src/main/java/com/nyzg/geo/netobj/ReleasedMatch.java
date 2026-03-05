package com.nyzg.geo.netobj;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
public class ReleasedMatch {
    final public static Map<Short, String> LevelMap = Map.of(
            (short) 0, "初学",
            (short) 1, "新手",
            (short) 2, "业余",
            (short) 3, "熟练",
            (short) 4, "专业",
            (short) 5, "顶尖"
    );

    final public static Map<Short, String> levelDescMap = Map.of(
            (short) 0, "刚刚接触该项目，正在学习基础规则与动作，处于摸索阶段。",
            (short) 1, "掌握基本技巧，能进行简单练习，但实战经验尚且不足。",
            (short) 2, "具备一定实战能力，动作较为连贯，常参与业余娱乐活动。",
            (short) 3, "技术动作娴熟，战术意识清晰，能稳定应对各类常规挑战。",
            (short) 4, "经过系统训练，技术全面精湛，具备参加正规比赛的实力。",
            (short) 5, "行业佼佼者，拥有卓越天赋与丰富经验，代表最高竞技水平。"
    );
    public static Map<Short, String> sportMap = new java.util.HashMap<>();

    static {
        sportMap.put((short) 0, "羽毛球");
        sportMap.put((short) 1, "篮球");
        sportMap.put((short) 2, "足球");
        sportMap.put((short) 3, "乒乓球");
        sportMap.put((short) 4, "网球");
        sportMap.put((short) 5, "排球");
        sportMap.put((short) 6, "游泳");
        sportMap.put((short) 7, "跑步");
        sportMap.put((short) 8, "自行车");
        sportMap.put((short) 9, "滑雪");
        sportMap.put((short) 10, "滑冰");
        sportMap.put((short) 11, "瑜伽");
        sportMap.put((short) 12, "拳击");
        sportMap.put((short) 13, "跆拳道");
        sportMap.put((short) 14, "举重");
        sportMap.put((short) 15, "体操");
        sportMap.put((short) 16, "攀岩");
        sportMap.put((short) 17, "射箭");
        sportMap.put((short) 18, "击剑");
        sportMap.put((short) 19, "高尔夫");

        sportMap = Collections.unmodifiableMap(sportMap);
    }

    final public static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    public long userId;
    public String spot;
    public String lat;
    public String lon;
    public String startTime;//format yyyy/MM/dd HH:mm:ss
    public int hour;
    public int minute;
    public int cutTime;
    public short sport;
    public short part;
    public short myLevel;
    public short matchLevel1;
    public short matchLevel2;

    public String matchDesc;
    public short feeType;
    public String fee;
    //0已完成 1 进行中 2在匹配
    public short status;
    public short userCount;//default 1

    public String matchId;//generate by server

    public String validateSucceedAtNull() {
        if (userId <=0) {
            return "未登录用户无法匹配";
        }
        if (spot == null || spot.isBlank()) {
            return "运动地点不能为空";
        }
        try {
            Double.parseDouble(lat);
            Double.parseDouble(lon);
        } catch (Exception ignore) {
            return "经纬度无法解析";
        }
        if (startTime == null || startTime.isBlank()) {
            return "开始时间为空";
        }
        try {
            LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
        } catch (Exception ignore) {
            return "开始时间格式不正确";
        }
        if (hour < 0 || hour > 24) {
            return "运动的时间限制在24小时以内";
        }
        if (minute < 0 || minute > 59) {
            return "输入了无效的分钟数";
        }
        if (cutTime < 0 || cutTime > 72) {
            return "匹配截止时间限制在72小时内";
        }
        if (!sportMap.containsKey(sport)) {
            return "不存在该运动";
        }
        if (!LevelMap.containsKey(myLevel)
                || !LevelMap.containsKey(matchLevel1)
                || !LevelMap.containsKey(matchLevel2)) {
            return "选择的等级不存在";
        }
        if (matchLevel1 > matchLevel2) {
            short tmp = matchLevel2;
            matchLevel2 = matchLevel1;
            matchLevel1 = tmp;
        }
        if (feeType < 0 || feeType > 2) {
            return "该分担费用的选项不存在";
        }
        if (matchDesc!=null&&matchDesc.length()>100){
            matchDesc=matchDesc.substring(0,100);
        }
        try {
            BigDecimal f = new BigDecimal(fee);
            if (f.doubleValue() < 0) {
                fee = "0";
            }
            if (feeType > 0) {//免费或发起者承担
                fee = "0";
            }
        } catch (Exception ignore) {
            return "输入了非法的金额";
        }
        if (part < 0 || part > 200) {
            return "不支持该数量的参与人数";
        }
        return null;
    }
}