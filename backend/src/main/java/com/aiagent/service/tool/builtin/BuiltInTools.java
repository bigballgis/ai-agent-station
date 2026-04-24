package com.aiagent.service.tool.builtin;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内置 Function Calling 工具集
 * 
 * 提供常用的内置工具，无需外部 MCP 服务器即可使用。
 * 所有方法使用 langchain4j @Tool 注解声明，自动被 FunctionToolRegistry 扫描注册。
 * 
 * 工具分类:
 * - 数学计算: calculate, convertUnit
 * - 文本处理: extractRegex, countWords, generateUuid, formatText
 * - 日期时间: getCurrentTime, formatDate, calculateDateDiff
 * - 数据转换: toJson, fromJson, base64Encode, base64Decode
 * - 系统信息: getSystemInfo
 */
@Slf4j
@Component
public class BuiltInTools {

    // ==================== 数学计算工具 ====================

    @Tool("执行数学表达式计算，支持加减乘除、幂运算、括号。示例: \"2 + 3 * 4\", \"(10 - 3) ^ 2\"")
    public String calculate(String expression) {
        try {
            // 安全的数学表达式解析（仅允许数字和运算符）
            String sanitized = expression.replaceAll("[^0-9+\\-*/().^% ]", "");
            if (sanitized.isEmpty()) {
                return "{\"error\": \"空表达式\"}";
            }
            // 替换 ^ 为 Math.pow
            sanitized = sanitized.replace("^", "**");

            // 使用 ScriptEngine 安全评估（简单场景）
            // 对于生产环境应使用专门的数学表达式解析库
            double result = evaluateExpression(sanitized);
            return String.format("{\"expression\": \"%s\", \"result\": %s}", 
                    expression, formatNumber(result));
        } catch (Exception e) {
            return "{\"error\": \"计算失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Tool("单位转换。支持: 长度(km/m/cm/mm/mile/yard/foot/inch), 重量(kg/g/mg/lb/oz), 温度(c/f/k)")
    public String convertUnit(String value, String fromUnit, String toUnit) {
        try {
            double val = Double.parseDouble(value);
            double result = UnitConverter.convert(val, fromUnit.toLowerCase(), toUnit.toLowerCase());
            return String.format("{\"value\": %s, \"from\": \"%s\", \"to\": \"%s\", \"result\": %s}",
                    value, fromUnit, toUnit, formatNumber(result));
        } catch (Exception e) {
            return "{\"error\": \"单位转换失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ==================== 文本处理工具 ====================

    @Tool("使用正则表达式从文本中提取匹配内容")
    public String extractRegex(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher matcher = p.matcher(text);
            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(matcher.group());
            }
            return String.format("{\"pattern\": \"%s\", \"matchCount\": %d, \"matches\": %s}",
                    pattern, matches.size(), matches);
        } catch (Exception e) {
            return "{\"error\": \"正则提取失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Tool("统计文本的字数、字符数、行数、段落数")
    public String countWords(String text) {
        if (text == null || text.isEmpty()) {
            return "{\"words\": 0, \"characters\": 0, \"lines\": 0, \"paragraphs\": 0}";
        }
        int chars = text.length();
        int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        int lines = text.split("\n").length;
        int paragraphs = text.split("\n\\s*\n").length;

        return String.format("{\"words\": %d, \"characters\": %d, \"lines\": %d, \"paragraphs\": %d}",
                words, chars, lines, paragraphs);
    }

    @Tool("生成一个随机的 UUID v4 标识符")
    public String generateUuid() {
        return "{\"uuid\": \"" + UUID.randomUUID().toString() + "\"}";
    }

    @Tool("格式化文本: 支持转大写(upper)、转小写(lower)、首字母大写(capitalize)、驼峰(camelCase)、下划线(snake_case)、短横线(kebab-case)")
    public String formatText(String text, String format) {
        if (text == null) return "{\"error\": \"文本为空\"}";
        String result;
        switch (format.toLowerCase()) {
            case "upper":
                result = text.toUpperCase();
                break;
            case "lower":
                result = text.toLowerCase();
                break;
            case "capitalize":
                result = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
                break;
            case "camelcase":
                result = toCamelCase(text);
                break;
            case "snake_case":
            case "snake":
                result = toSnakeCase(text);
                break;
            case "kebab-case":
            case "kebab":
                result = toKebabCase(text);
                break;
            default:
                return "{\"error\": \"不支持的格式: " + format + "\"}";
        }
        return String.format("{\"original\": \"%s\", \"format\": \"%s\", \"result\": \"%s\"}",
                text.replace("\"", "'"), format, result);
    }

    // ==================== 日期时间工具 ====================

    @Tool("获取当前日期和时间，可指定时区（默认 Asia/Shanghai）")
    public String getCurrentTime(String timezone) {
        try {
            TimeZone tz = TimeZone.getTimeZone(
                    (timezone != null && !timezone.isEmpty()) ? timezone : "Asia/Shanghai");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(tz);
            Date now = new Date();

            Calendar cal = Calendar.getInstance(tz);
            cal.setTime(now);

            return String.format("{\"datetime\": \"%s\", \"date\": \"%s\", \"time\": \"%s\", " +
                            "\"dayOfWeek\": %d, \"dayOfYear\": %d, \"weekOfYear\": %d, \"timestamp\": %d}",
                    sdf.format(now),
                    new SimpleDateFormat("yyyy-MM-dd").format(now),
                    new SimpleDateFormat("HH:mm:ss").format(now),
                    cal.get(Calendar.DAY_OF_WEEK),
                    cal.get(Calendar.DAY_OF_YEAR),
                    cal.get(Calendar.WEEK_OF_YEAR),
                    now.getTime() / 1000);
        } catch (Exception e) {
            return "{\"error\": \"获取时间失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Tool("将时间戳或日期字符串格式化为指定格式。pattern 示例: yyyy-MM-dd, HH:mm:ss, yyyy年MM月dd日")
    public String formatDate(String dateInput, String pattern) {
        try {
            Date date;
            if (dateInput.matches("\\d{10,13}")) {
                long ts = Long.parseLong(dateInput);
                date = ts < 1e12 ? new Date(ts * 1000) : new Date(ts);
            } else {
                date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateInput);
            }
            SimpleDateFormat sdf = new SimpleDateFormat(
                    (pattern != null && !pattern.isEmpty()) ? pattern : "yyyy-MM-dd HH:mm:ss");
            return String.format("{\"formatted\": \"%s\", \"timestamp\": %d}",
                    sdf.format(date), date.getTime() / 1000);
        } catch (Exception e) {
            return "{\"error\": \"日期格式化失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Tool("计算两个日期之间的差值，返回天数、小时数、分钟数")
    public String calculateDateDiff(String startDate, String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            long diffMs = end.getTime() - start.getTime();
            long diffDays = diffMs / (1000 * 60 * 60 * 24);
            long diffHours = diffMs / (1000 * 60 * 60);
            long diffMinutes = diffMs / (1000 * 60);

            return String.format("{\"startDate\": \"%s\", \"endDate\": \"%s\", " +
                            "\"days\": %d, \"hours\": %d, \"minutes\": %d}",
                    startDate, endDate, diffDays, diffHours, diffMinutes);
        } catch (Exception e) {
            return "{\"error\": \"日期差计算失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ==================== 数据转换工具 ====================

    @Tool("将 Java 对象或键值对列表转换为 JSON 字符串")
    public String toJson(String data) {
        try {
            // 如果已经是 JSON，直接返回
            if (data.trim().startsWith("{") || data.trim().startsWith("[")) {
                return "{\"json\": " + data + "}";
            }
            // 简单 key=value 格式转 JSON
            Map<String, String> map = new LinkedHashMap<>();
            for (String pair : data.split("[,\n]")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return "{\"json\": " + om.writeValueAsString(map) + "}";
        } catch (Exception e) {
            return "{\"error\": \"JSON 转换失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Tool("对文本进行 Base64 编码")
    public String base64Encode(String text) {
        try {
            String encoded = Base64.getEncoder().encodeToString(text.getBytes("UTF-8"));
            return "{\"encoded\": \"" + encoded + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"Base64 编码失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Tool("对 Base64 编码的文本进行解码")
    public String base64Decode(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return "{\"decoded\": \"" + new String(decoded, "UTF-8") + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"Base64 解码失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ==================== 系统信息工具 ====================

    @Tool("获取系统运行时信息：JVM 内存、CPU 核数、操作系统、Java 版本等")
    public String getSystemInfo() {
        Runtime rt = Runtime.getRuntime();
        long totalMem = rt.totalMemory() / (1024 * 1024);
        long freeMem = rt.freeMemory() / (1024 * 1024);
        long maxMem = rt.maxMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;

        return String.format("{\"os\": \"%s\", \"arch\": \"%s\", \"javaVersion\": \"%s\", " +
                        "\"processors\": %d, \"jvmMemory\": {\"used_mb\": %d, \"free_mb\": %d, " +
                        "\"total_mb\": %d, \"max_mb\": %d}}",
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("java.version"),
                rt.availableProcessors(),
                usedMem, freeMem, totalMem, maxMem);
    }

    // ==================== 辅助方法 ====================

    private double evaluateExpression(String expr) {
        // 简单递归下降解析器（安全，不使用 eval）
        return new ExpressionParser(expr).parse();
    }

    private String formatNumber(double val) {
        if (val == Math.floor(val) && !Double.isInfinite(val)) {
            return String.valueOf((long) val);
        }
        return new BigDecimal(val).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String toCamelCase(String text) {
        String[] words = text.split("[_\\-\\s]+");
        StringBuilder sb = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            sb.append(Character.toUpperCase(words[i].charAt(0)));
            sb.append(words[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private String toSnakeCase(String text) {
        return text.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase().replaceAll("[\\s-]+", "_");
    }

    private String toKebabCase(String text) {
        return text.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase().replaceAll("[\\s_]+", "-");
    }

    /**
     * 简单的数学表达式解析器（递归下降）
     */
    private static class ExpressionParser {
        private final String expr;
        private int pos = -1;
        private int ch;

        ExpressionParser(String expr) {
            this.expr = expr;
        }

        void nextChar() {
            ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char) ch);
            return x;
        }

        double parseExpression() {
            double x = parseTerm();
            for (; ; ) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        double parseTerm() {
            double x = parseFactor();
            for (; ; ) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else if (eat('%')) x %= parseFactor();
                else return x;
            }
        }

        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                eat(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(expr.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }

            if (eat('^')) x = Math.pow(x, parseFactor());
            return x;
        }
    }

    /**
     * 单位转换器
     */
    private static class UnitConverter {
        private static final Map<String, Map<String, Double>> CONVERSIONS = new HashMap<>();

        static {
            // 长度 (基准: meter)
            Map<String, Double> length = new HashMap<>();
            length.put("km", 1000.0);
            length.put("m", 1.0);
            length.put("cm", 0.01);
            length.put("mm", 0.001);
            length.put("mile", 1609.344);
            length.put("yard", 0.9144);
            length.put("foot", 0.3048);
            length.put("inch", 0.0254);
            CONVERSIONS.put("length", length);

            // 重量 (基准: kg)
            Map<String, Double> weight = new HashMap<>();
            weight.put("kg", 1.0);
            weight.put("g", 0.001);
            weight.put("mg", 0.000001);
            weight.put("lb", 0.453592);
            weight.put("oz", 0.0283495);
            CONVERSIONS.put("weight", weight);

            // 温度 (特殊处理)
            Map<String, Double> temp = new HashMap<>();
            temp.put("c", null);
            temp.put("f", null);
            temp.put("k", null);
            CONVERSIONS.put("temperature", temp);
        }

        static double convert(double value, String from, String to) {
            // 温度特殊处理
            if (CONVERSIONS.get("temperature").containsKey(from)) {
                return convertTemperature(value, from, to);
            }

            // 查找单位所属类别
            for (Map<String, Double> category : CONVERSIONS.values()) {
                if (category.containsKey(from) && category.containsKey(to)) {
                    Double fromFactor = category.get(from);
                    Double toFactor = category.get(to);
                    if (fromFactor != null && toFactor != null) {
                        return value * fromFactor / toFactor;
                    }
                }
            }
            throw new RuntimeException("不支持的单位转换: " + from + " -> " + to);
        }

        private static double convertTemperature(double value, String from, String to) {
            if (from.equals(to)) return value;
            // 先转为摄氏度
            double celsius;
            switch (from) {
                case "c": celsius = value; break;
                case "f": celsius = (value - 32) * 5 / 9; break;
                case "k": celsius = value - 273.15; break;
                default: throw new RuntimeException("未知温度单位: " + from);
            }
            // 从摄氏度转为目标
            switch (to) {
                case "c": return celsius;
                case "f": return celsius * 9 / 5 + 32;
                case "k": return celsius + 273.15;
                default: throw new RuntimeException("未知温度单位: " + to);
            }
        }
    }
}
