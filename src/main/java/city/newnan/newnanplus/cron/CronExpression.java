package city.newnan.newnanplus.cron;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 定时表达式类，比Linux的cron表达式多了一个"秒描述"
 */
public class CronExpression {
    private static final List<Integer> LEGAL_SECOND_INTERVAL_LIST = Arrays.asList(2, 3, 4, 5, 6, 10, 12, 15, 20, 30);
    private static final List<Integer> LEGAL_MINUTE_INTERVAL_LIST = LEGAL_SECOND_INTERVAL_LIST;
    private static final List<Integer> LEGAL_HOUR_INTERVAL_LIST = Arrays.asList(2, 3, 4, 6, 8, 12);
    private static final List<Integer> LEGAL_MONTH_INTERVAL_LIST = Arrays.asList(2, 3, 4, 6);

    private static final List<Integer> LAST_DAY_OF_MONTH = Arrays.asList(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);

    private static final HashMap<String, Integer> MONTH_NAME_MAP = new HashMap<String, Integer>() {{
        put("jan",  1); put("january",   1); put("1",   1);
        put("feb",  2); put("february",  2); put("2",   2);
        put("mar",  3); put("march",     3); put("3",   3);
        put("apr",  4); put("april",     4); put("4",   4);
        put("may",  5);                      put("5",   5);
        put("jun",  6); put("june",      6); put("6",   6);
        put("jul",  7); put("july",      7); put("7",   7);
        put("aug",  8); put("august",    8); put("8",   8);
        put("sept", 9); put("september", 9); put("9",   9);
        put("oct", 10); put("october",  10); put("10", 10);
        put("nov", 11); put("november", 11); put("11", 11);
        put("dec", 12); put("december", 12); put("12", 12);
    }};
    private static final HashMap<String, Integer> DAYOFWEEK_NAME_MAP = new HashMap<String, Integer>() {{
        put("mon", 1); put("monday",    1); put("1", 1);
        put("tue", 2); put("tuesday",   2); put("2", 2);
        put("wed", 3); put("wednesday", 3); put("3", 3);
        put("thu", 4); put("thursday",  4); put("4", 4);
        put("fri", 5); put("friday",    5); put("5", 5);
        put("sat", 6); put("saturday",  6); put("6", 6);
        put("sun", 7); put("sunday",    7); put("0", 7); put("7", 7);
    }};

    private static ZoneOffset localTimezoneOffset = ZoneOffset.of("+8");

    private final int[] secondList;
    private final int[] minuteList;
    private final int[] hourList;
    private final int[] monthList;
    private final List<Boolean> avaliableRegularDayOfWeek;

    // 虚拟转轮算法
    // 很简单，秒→分→时→天→月→年
    private int secondListPointer = -1;
    private int minuteListPointer = -1;
    private int hourListPointer = -1;
    private int dayPointer = -1;
    private int monthListPointer = -1;
    private int yearPointer = -1;

    /**
     * Cron表达式
     */
    public final String expressionString;

    /**
     * 毫秒时间戳，本地时间，也就是说，不是从标准格林尼治时间1970年开始，而是基于现在的时区
     * 0代表着已经没有下一次，该表达式已失效
      */
    private long nextTime = 1;

    /**
     * 构造函数
     * @param expressionString cron表达式，但是比Linux的cron表达式多了一个"秒描述"
     */
    public CronExpression(String expressionString) {
        // 表达式分割
        String[] expressionSplits = expressionString.split(" ");

        // 秒解析
        this.secondList = parseToArray(expressionSplits[0], 60, LEGAL_SECOND_INTERVAL_LIST, null);

        // 分解析
        this.minuteList = parseToArray(expressionSplits[1], 60, LEGAL_MINUTE_INTERVAL_LIST, null);

        // 时解析
        this.hourList = parseToArray(expressionSplits[2], 24, LEGAL_HOUR_INTERVAL_LIST, null);

        // 月解析
        this.monthList = parseToArray(expressionSplits[4], 12, LEGAL_MONTH_INTERVAL_LIST, MONTH_NAME_MAP);

        // 常规周解析
        this.avaliableRegularDayOfWeek = parseRegularDayOfWeekList(expressionSplits[5]);

        // 存储表达式
        this.expressionString = expressionString;

        // 初始化工作
        getNextTime();
    }

    /**
     * 获取该表达式在未来的最近一次执行时间
     * @return 未来最近一次执行的毫秒时间戳(本地时间，考虑时区)；如果表达式已失效，即不存在未来满足条件的执行时刻，则返回0
     */
    public long getNextTime() {
        // 失效的表达式，没有下一次了
        if (this.nextTime == 0)
            return 0;
        // 获得当前时间
        long curTime = System.currentTimeMillis();
        // 如果当前时间已晚于上次预计执行的时间，那么就计算下一次执行时间
        // 或者，表达式失效
        while (curTime >= this.nextTime || this.nextTime == 0) {
            tickNext(false);
        }
        return this.nextTime;
    }

    /**
     * 获取下一个可用时刻，或者初始化获得最近的下一个可用时刻
     * @param initFlag 初始化标志
     */
    private void tickNext(boolean initFlag) {
        // 如果是初始化模式
        if (initFlag) {
            // 重置转轮
            this.secondListPointer = -1;
            this.minuteListPointer = -1;
            this.hourListPointer = -1;
            this.dayPointer = -1;
            this.monthListPointer = -1;
            this.yearPointer = -1;

            LocalDateTime curTime = LocalDateTime.now();

            // 先定位下一个可用天(含今天)
            tickNextDay(curTime);

            // 如果是今天，那么时分秒也要定位到下一个可用时刻；
            // 如果不是今天，时分秒只要指向第一个可用时刻就可以
            if (curTime.getDayOfMonth() == this.dayPointer &&
                    curTime.getMonthValue() == this.monthList[this.monthListPointer] &&
                    curTime.getYear() == this.yearPointer) {
                this.secondListPointer = 0;
                this.minuteListPointer = 0;
                this.hourListPointer = 0;
            } else {
                int tmp, carrier;

                for (tmp = 0; tmp < this.secondList.length; tmp++) {
                    if (curTime.getSecond() <= this.secondList[tmp]) {
                        break;
                    }
                }
                // 如果都没找到，就说明需要到下一个可用分钟去找，故进位并指向第一个可用秒
                if (tmp == this.secondList.length) {
                    // 进位1
                    carrier = 1;
                    this.secondListPointer = 0;
                } else {
                    carrier = 0;
                    this.secondListPointer = tmp;
                }

                for (tmp = 0; tmp < this.minuteList.length; tmp++) {
                    if ((curTime.getMinute() + carrier) <= this.minuteList[tmp]) {
                        break;
                    }
                }
                // 如果都没找到，就说明需要到下一个可用小时去找，故进位并指向第一个可用时
                if (tmp == this.minuteList.length) {
                    // 进位1
                    carrier = 1;
                    this.minuteListPointer = 0;
                } else {
                    carrier = 0;
                    this.minuteListPointer = tmp;
                }

                for (tmp = 0; tmp < this.hourList.length; tmp++) {
                    if ((curTime.getHour() + carrier) <= this.hourList[tmp]) {
                        break;
                    }
                }
                // 如果没有找到，说明今天没有可用的时间，需要到下一个可用天去找
                if (tmp == this.hourList.length) {
                    this.secondListPointer = 0;
                    this.minuteListPointer = 0;
                    this.hourListPointer = 0;
                    tickNextDay(curTime);
                } else {
                    this.hourListPointer = tmp;
                }
            }
        } else {
            // 如果不是初始化模式
            this.secondListPointer = (this.secondListPointer + 1) % this.secondList.length;
            if (this.secondListPointer == 0) {
                this.minuteListPointer = (this.minuteListPointer + 1) % this.minuteList.length;
                if (this.minuteListPointer == 0) {
                    this.hourListPointer = (this.hourListPointer + 1) % this.hourList.length;
                    if (this.hourListPointer == 0) {
                        tickNextDay(LocalDateTime.now());
                    }
                }
            }
        }

        // 说明该表达式已失效，没有下一次可执行的时间了
        if (this.nextTime == 0)
            return;

        // 获取时间戳，考虑时区
        this.nextTime = LocalDateTime.of(
                this.yearPointer,
                this.monthListPointer,
                this.dayPointer,
                this.hourList[this.hourListPointer],
                this.minuteList[this.minuteListPointer],
                this.secondList[this.secondListPointer]
        ).toInstant(localTimezoneOffset).toEpochMilli();
    }

    /**
     * 进到下一天
     * 由于年月日+周都是相互关联的，不能拆开，所以要一起考虑
     * @param curTime LocalDateTime实例，当前时间
     */
    private void tickNextDay(LocalDateTime curTime) {
        // 表达式分割
        String[] expressionSplits = expressionString.split(" ");

        // 循环查找
        // 首先看看能不能直接找到本月的下一个可用日
        // 如果找不到(Day of Month、Day of Week和月天数三个条件)，就需要找下一个可用月
        // 找下一个可用月，如果找不到就需要找下一个可用年；
        // 此时如果存在下一个可用年就一定存在下一个可用月(可能不存在可用日)，否则所有年份都不存在可用月，这个可以用反证法证明
        // 所以寻找可用月不需要while循环
        while (!findNextDay(curTime, expressionSplits)) {
            // 如果有下一个可用年份，则至少存在一个可用月
            // 所以这个循环最多执行2次
            while (!findNextMonth(curTime)) {
                if (!findNextYear(curTime, expressionSplits)) {
                    // 没有下一个可用年份，该表达式已失效
                    this.nextTime = 0;
                    return;
                }
            }
        }
    }

    /**
     * 在当前monthListPointer内寻找下一个比当前dayPointer大的合法day
     * 如果dayPointer为-1，即未初始化，那么如果月份为-1则直接返回去初始化年份，否则指向当前年份月份大于等于当前日期的第一个日
     * @param expressionSplits Cron表达式分割
     * @return true为找到，false为未找到
     */
    private boolean findNextDay(LocalDateTime curTime, String[] expressionSplits) {
        // -1代表未初始化
        if (this.dayPointer == -1) {
            if (this.monthListPointer == -1) {
                // 如果月份也未初始化，那么就先返回false去找year
                return false;
            } else {
                // 如果月份初始化了，就找当前月份大于等于当前日的第一个日
                // 这里先将其置为今天的前一天或者当月0天(日从1开始)，这样自然就接上了
                if (this.yearPointer == curTime.getYear() && this.monthList[this.monthListPointer] == curTime.getMonthValue()) {
                    this.dayPointer = curTime.getDayOfMonth() - 1;
                } else {
                    this.dayPointer = 0;
                }
            }
        }

        // 如果已经是最后一天，就本月就没有下一个可用日了
        // 初始化并返回false，先找新的可用月
        if (this.dayPointer == LAST_DAY_OF_MONTH.get(this.monthList[this.monthListPointer])) {
            this.dayPointer = -1;
            return false;
        }

        // 接下来的任务就是去找比this.dayPointer大的符合条件的日子
        int nextDay = 32;
        // 要同时考虑 Day of Month 和 Day of Week
        // 如果二者都是*，则为每个月的每一天
        // 如果只有一个为*，就只考虑一个
        // 如果都不是*，就是二者之一
        if (expressionSplits[3].equals("*") && expressionSplits[5].equals("*")) {
            nextDay = this.dayPointer + 1;
        } else {
            LocalDateTime theDate = LocalDateTime.of(this.yearPointer, this.monthListPointer, 1, 0, 0);
            // Day of Month 部分
            if (!expressionSplits[3].equals("*")) {
                for (String splits : expressionSplits[3].split(",")) {
                    // 识别 'L' 每个月的最后一天
                    if (splits.equals("L")) {
                        int lastDayOfMonth = theDate.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
                        if (lastDayOfMonth < nextDay) {
                            nextDay = lastDayOfMonth;
                        }
                        continue;
                    }

                    // 识别 'W' 每个月第一个工作日
                    if (splits.equals("W")) {
                        int firstWeekday = 1;
                        DayOfWeek dayOfWeekInTheMonth = theDate.getDayOfWeek();
                        if (dayOfWeekInTheMonth == DayOfWeek.SATURDAY) {
                            firstWeekday += 2;
                        } else if (dayOfWeekInTheMonth == DayOfWeek.SUNDAY) {
                            firstWeekday += 1;
                        }
                        if (this.dayPointer < firstWeekday && firstWeekday < nextDay) {
                            nextDay = firstWeekday;
                        }
                        continue;
                    }

                    // 按 '-' 分割
                    String[] subSplits = splits.split("-");
                    if (subSplits.length == 2) {
                        int from = Integer.parseInt(subSplits[0]);
                        int to = Integer.parseInt(subSplits[1]);

                        // 如果这个区间在下一天已过去，就忽略
                        if (to <= this.dayPointer) {
                            continue;
                        }

                        // 如果下一天处于区间内，就取第二天
                        if (from <= (this.dayPointer + 1)) {
                            nextDay = this.yearPointer + 1;
                            break;
                        }

                        // 如果下一天的时候该区间还未开始，就看看这个区间的开始
                        if (from < nextDay)
                            nextDay = from;
                    } else {
                        // 日点
                        int number = Integer.parseInt(subSplits[0]);

                        // 如果这个年份在之前年份之后，就可以考虑
                        if (this.dayPointer < number && number < nextDay)
                            nextDay = number;
                    }
                }
            }
            // Day of Week 部分
            if (!expressionSplits[5].equals("*")) {
                for (String splits : expressionSplits[5].split(",")) {
                    // 先从表里面找最近的日期
                    if (this.avaliableRegularDayOfWeek != null) {
                        // 定义第二天而不是第一天，因为第一天可能是非法的(0)
                        LocalDateTime secondDate = LocalDateTime.of(this.yearPointer, this.monthListPointer,
                                this.dayPointer + 1, 0, 0);
                        int curDayOfWeek = secondDate.getDayOfWeek().getValue();

                        int tmpDay = this.dayPointer + 1;
                        int i = curDayOfWeek;
                        for (int j = 0; j < 7; j++) {
                            if (this.avaliableRegularDayOfWeek.get(i))
                                break;
                            tmpDay++;
                            i = (i == 7) ? 1 : (i + 1);
                        }
                        if (tmpDay < nextDay)
                            nextDay = tmpDay;
                    }

                    // \d+#\d+部分
                    if (!splits.contains("-") && splits.contains("#")) {
                        String[] subSplits = splits.split("#");
                        int dayOfWeek = DAYOFWEEK_NAME_MAP.get(subSplits[0].toLowerCase());
                        int numOfWeek = Integer.parseInt(subSplits[1]);

                        int day = theDate.with(TemporalAdjusters.dayOfWeekInMonth(numOfWeek,
                                DayOfWeek.of(dayOfWeek))).getDayOfMonth();
                        if (this.dayPointer < day && day < nextDay)
                            nextDay = day;
                    }
                }
            }
        }

        // 非法日期视为无可用
        if (nextDay > LAST_DAY_OF_MONTH.get(this.monthList[this.monthListPointer])) {
            this.dayPointer = -1;
            return false;
        }

        this.dayPointer = nextDay;
        return true;
    }

    /**
     * 在当前yearPointer内寻找下一个比当前monthListPointer大的合法month
     * 如果monthListPointer为-1，即未初始化，那么如果年份为-1则直接返回去初始化年份，否则指向当前年份大于等于当前月份的第一个月
     * @return true为找到，false为未找到
     */
    private boolean findNextMonth(LocalDateTime curTime) {
        // -1代表未初始化
        if (this.monthListPointer == -1) {
            if (this.yearPointer == -1) {
                // 如果年份也未初始化，那么就先返回false去找year
                return false;
            } else {
                // 如果年份初始化了，就找当前年份大于等于当前月份的第一个月
                if (this.yearPointer == curTime.getYear()) {
                    int i;
                    for (i = 0; i < this.monthList.length; i++) {
                        if (curTime.getMonthValue() <= this.monthList[i])
                            break;
                    }
                    if (i == this.monthList.length) {
                        return false;
                    }
                    this.monthListPointer = i;
                    return true;
                } else {
                    this.monthListPointer = 0;
                }
            }
        } else {
            // 如果已经初始化了，那么就自然进一
            this.monthListPointer = (this.monthListPointer + 1) % this.monthList.length;
            // 到达进位处，月复位，返回false以更新year，并再次初始化month
            if (this.monthListPointer == 0) {
                this.monthListPointer = -1;
                return false;
            }
        }
        // 否则，返回true
        return true;
    }

    /**
     * 在当前寻找下一个可用的年份
     * 如果yearPointer为-1，即未初始化，会定位到离现在最近的未来可用年份(包括今年)
     * @param curTime LocalDateTime实例，现在的时间日期
     * @param expressionSplits Cron表达式分割
     * @return true为找到，false为未找到
     */
    private boolean findNextYear(LocalDateTime curTime, String[] expressionSplits) {
        // -1代表未初始化，那么就定位到最近的可用年份
        // 先把yearPointer设为去年，然后后面一样处理就可以了
        if (this.yearPointer == -1) {
            this.yearPointer = curTime.getYear() - 1;
        }

        // 如果没有年份描述段，或者识别到 '*' 通配符
        if (expressionSplits.length == 6 || expressionSplits[6].equals("*")) {
            // 那么年份简单地增加一年就好
            this.yearPointer++;
        } else {
            // 否则就要找比其大的最小年份
            int nextYear = Integer.MAX_VALUE;
            for (String splits : expressionSplits[6].split(",")) {
                // 按 '-' 分割
                String[] subSplits = splits.split("-");
                if (subSplits.length == 2) {
                    // 年份区间
                    int from = Integer.parseInt(subSplits[0]);
                    int to = Integer.parseInt(subSplits[1]);

                    // 如果这个区间在下一年已过去，就忽略
                    if (to <= this.yearPointer) {
                        continue;
                    }

                    // 如果下一年处于区间内，就取明年
                    if (from <= (this.yearPointer + 1)) {
                        nextYear = this.yearPointer + 1;
                        break;
                    }

                    // 如果下一年的时候该区间还未开始，就看看这个区间的开始
                    if (from < nextYear)
                        nextYear = from;
                } else {
                    // 年份点
                    int number = Integer.parseInt(subSplits[0]);

                    // 如果这个年份在之前年份之后，就可以考虑
                    if (this.yearPointer < number && number < nextYear)
                        nextYear = number;
                }
            }
            // 如果没有找到
            if (nextYear == Integer.MAX_VALUE) {
                this.nextTime = 0;
                return false;
            }
            this.yearPointer = nextYear;
        }
        return true;
    }

    /**
     * 解析子表达式为int[]类型的位表
     * @param subExpression 子表达式，应满足正则表达式 (\d+(-\d+)?)(,\d+(-\d+)?)*
     * @param valueLimit 数值上线，排除非法数值，如秒是60，时是24
     * @return int[]实例
     */
    private int[] parseToArray(String subExpression, int valueLimit, List<Integer> legalIntervalList, Map<String, Integer> optionalMap) {
        ArrayList<Integer> tmpList = new ArrayList<Integer>();
        // 按 ',' 分割
        for (String splits : subExpression.split(",")) {
            // 识别 '*' 通配符
            if (splits.equals("*")) {
                for (int i = 0; i < valueLimit; i++) {
                    atomicListAdd(tmpList, i);
                }
                break;
            }

            // 识别 '*/d+' 间隔通配
            if (splits.matches("\\*/\\d+")) {
                int interval = parseInt(splits.split("/")[1], optionalMap);
                if (legalIntervalList.contains(interval)) {
                    for (int i = 0; i < valueLimit; i+=interval) {
                        atomicListAdd(tmpList, i);
                    }
                }
                break;
            }

            // 按 '-' 分割
            String[] subSplits = splits.split("-");
            if (subSplits.length == 2) {
                int from = parseInt(subSplits[0], optionalMap);
                int to = parseInt(subSplits[1], optionalMap);
                if (to > valueLimit)
                    to = valueLimit;
                for (; from < to; from++) {
                    atomicListAdd(tmpList, from);
                }
            } else {
                int number = parseInt(subSplits[0], optionalMap);
                if (number <= valueLimit)
                    atomicListAdd(tmpList, number);
            }
        }

        // List -> int[]
        Collections.sort(tmpList);
        int[] ans = new int[tmpList.size()];
        for (int i = 0; i < tmpList.size(); i++) {
            ans[i] = tmpList.get(i);
        }

        return ans;
    }

    /**
     * 解析周表达式中的常规部分(不含'#'的部分)
     * @param weekExpression 周表达式
     * @return 周可用情况List
     */
    private List<Boolean> parseRegularDayOfWeekList(String weekExpression) {
        List<Boolean> list = new ArrayList<Boolean>(8);
        list.add(false);

        if (weekExpression.equals("*")) {
            for (int i = 1; i <=7; i++)
                list.add(true);
            return list;
        }

        boolean ifEmpty = true;
        for (int i = 1; i <=7; i++)
            list.add(false);
        // 按 ',' 分割
        for (String splits : weekExpression.split(",")) {
            // 按 '-' 分割
            String[] subSplits = splits.split("-");
            if (subSplits.length == 2) {
                int from = DAYOFWEEK_NAME_MAP.get(subSplits[0].toLowerCase());
                int to = DAYOFWEEK_NAME_MAP.get(subSplits[1].toLowerCase());

                for (int i = from; i <= to; i = (i==7)?1:(i+1)) {
                    list.set(i, true);
                    ifEmpty = false;
                }
            } else {
                // 忽略 \d+#\d+
                if (!splits.contains("#")) {
                    int num = DAYOFWEEK_NAME_MAP.get(subSplits[0].toLowerCase());
                    list.set(num, true);
                    ifEmpty = false;
                }
            }
        }
        return ifEmpty ? null : list;
    }

    /**
     * 使用指定的映射Map来解析获得整数
     * @param s 待解析字符串
     * @param optionalMap 映射表
     * @return 解析得到的整数
     */
    private int parseInt(String s, Map<String, Integer> optionalMap) {
        if (optionalMap == null) {
            return Integer.parseInt(s);
        } else {
            return optionalMap.get(s.toLowerCase());
        }
    }

    /**
     * 不重复地添加一个元素到List里
     * @param list List实例
     * @param object 元素实例
     * @param <T> List模板类型
     */
    private <T> void atomicListAdd(List<T> list, T object) {
        if (list.contains(object))
            return;
        list.add(object);
    }

    /**
     * 重新设置时区
     * @param offsetId 时区ID
     */
    public void setTimeZoneOffset(String offsetId) {
        localTimezoneOffset = ZoneOffset.of(offsetId);
    }

    @Deprecated
    public void printPointer() {
        System.out.printf("second: %d\n", this.secondList[this.secondListPointer]);
        System.out.printf("minute: %d\n", this.monthList[this.minuteListPointer]);
        System.out.printf("hour: %d\n", this.hourList[this.hourListPointer]);
        System.out.printf("day: %d\n", this.dayPointer);
        System.out.printf("month: %d\n", this.monthList[this.monthListPointer]);
        System.out.printf("year: %d\n", this.yearPointer);
    }
}