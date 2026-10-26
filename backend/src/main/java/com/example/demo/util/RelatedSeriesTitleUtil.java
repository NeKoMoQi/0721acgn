package com.example.demo.util;

import java.util.regex.Pattern;

/**
 * 从作品名提取「系列主标题」，用于多季 / 分期 / 篇的弱关联。
 */
public final class RelatedSeriesTitleUtil {

    private RelatedSeriesTitleUtil() {
    }

    /**
     * 供 MyBatis LIKE：对 % _ \ 等转义；配合 ESCAPE '!'。
     */
    public static String escapeLike(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("\\", "!\\");
    }

    /**
     * 反复去掉末尾常见的季 / 期 / cour 等后缀，得到系列主名。
     */
    public static String baseSeriesTitle(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim().replaceAll("\\s+", " ");
        for (int i = 0; i < 10; i++) {
            String next = stripTrailingSeriesOnce(s);
            if (next.equals(s)) {
                break;
            }
            s = next.replaceAll("\\s+", " ").trim();
        }
        return s;
    }

    private static String stripTrailingSeriesOnce(String s) {
        for (Pattern p : TRAILING_MARKERS) {
            java.util.regex.Matcher m = p.matcher(s);
            if (m.find() && m.start() >= 0) {
                return s.substring(0, m.start()).trim();
            }
        }
        return s;
    }

    /**
     * 题名是否属于同一 IP：以 seriesKey 为前缀，且衔接处为「结束 / 空白 / 标点 / 第…」等，
     * 避免误把「…间谍…」其它作品算进来（依赖 SQL 已用 seriesKey% 收窄）。
     */
    public static boolean isSameIpFamily(String candidateName, String seriesKey) {
        if (candidateName == null || seriesKey == null || seriesKey.isBlank()) {
            return false;
        }
        String n = candidateName.trim();
        String k = seriesKey.trim();
        if (k.length() < 3) {
            return false;
        }
        if (!n.startsWith(k)) {
            return false;
        }
        if (n.length() == k.length()) {
            return true;
        }
        char next = n.charAt(k.length());
        return Character.isWhitespace(next)
                || next == '：'
                || next == ':'
                || next == '（'
                || next == '('
                || next == '／'
                || next == '/'
                || next == '·'
                || next == '・'
                || next == '第'
                || Character.isDigit(next);
    }

    /**
     * 从末尾反复剥掉：配音版、衍生题名、季/期、cour、剧场版等，得到系列主名（如「间谍过家家」）。
     */
    private static final Pattern[] TRAILING_MARKERS = new Pattern[]{
            Pattern.compile("\\s+中配版\\s*$"),
            Pattern.compile("\\s+粤配版\\s*$"),
            Pattern.compile("\\s+日语版\\s*$"),
            Pattern.compile("\\s+中文(?:版)?\\s*$"),
            Pattern.compile("\\s+代号：.*\\s*$"),
            Pattern.compile("\\s+代號：.*\\s*$"),
            Pattern.compile(
                    "(\\(|（)[^()（）]{0,32}?(第\\s*[0-9一二三四五六七八九十百千两]+\\s*[季期部篇]|[Ss]eason\\s*\\d+|[Pp]art\\s*\\d+|クール[Ａ-ＺA-Za-z]{0,3})[^()（）]{0,12}?(\\)|）)\\s*$"),
            Pattern.compile("\\s+[：:／/]\\s*第?\\s*[0-9一二三四五六七八九十百千两]+\\s*[季期]\\s*$"),
            Pattern.compile("\\s+第\\s*[0-9一二三四五六七八九十百千两]+\\s*[季期部篇]\\s*$"),
            Pattern.compile("第\\s*[0-9一二三四五六七八九十百千两]+\\s*[季期部篇]\\s*$"),
            Pattern.compile("\\s+第\\s*[一二三四五六七八九十两]+\\s*[季期]\\s*$"),
            Pattern.compile("第\\s*[一二三四五六七八九十两]+\\s*[季期]\\s*$"),
            Pattern.compile("\\s+[Ss][Ee][Aa][Ss][Oo][Nn]\\s*\\d+\\s*$"),
            Pattern.compile("\\s+[Pp]art\\s*\\d+\\s*$"),
            Pattern.compile("\\s+[Cc]our\\s*\\d+\\s*$"),
            Pattern.compile("\\s+第\\s*\\d+\\s*[Cc]our\\s*$"),
            Pattern.compile("\\s+\\d+\\s*[Nn][Dd]\\s*[Ss]eason\\s*$"),
            Pattern.compile("\\s+总集篇\\s*$"),
            Pattern.compile("\\s+前篇\\s*$"),
            Pattern.compile("\\s+后篇\\s*$"),
            Pattern.compile("\\s+剧场版(\\s*[（(].*?[)）])?\\s*$"),
    };
}
