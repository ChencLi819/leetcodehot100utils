package hot100;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 从 data/ 目录加载全部题目数据（UTF-8 纯文本，每个分类一个 .txt）。
 *
 * 数据格式（@@ 开头的行为标记）：
 *   @@PROBLEM            新题目开始
 *   @@NO: 1 / @@ID: 1 / @@TITLE: 两数之和 / @@DIFF: 简单
 *   @@CATEGORY: 哈希 / @@TAGS: 数组;哈希表 / @@URL: https://...
 *   @@DESC               题面描述（多行，直到下一个 @@）
 *   @@SOLUTION: 方法一：哈希表（推荐）   新解法开始
 *   @@IDEA               该解法的思路（多行）
 *   @@JAVA               该解法 Java 代码（多行）
 *   @@PY                 该解法 Python 代码（多行）
 *   ……（每题可含多个 @@SOLUTION，要求至少 3 个）
 */
public class ProblemLoader {

    public static List<Problem> load(File dataDir) throws IOException {
        List<Problem> result = new ArrayList<>();
        File[] files = dataDir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) {
            return result;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File f : files) {
            parseFile(f, result);
        }
        result.sort(Comparator.comparingInt(p -> p.no));
        return result;
    }

    private static void parseFile(File f, List<Problem> out) throws IOException {
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        Problem cur = null;          // 当前题目
        Problem.Solution curSol = null; // 当前解法
        String section = null;       // 当前多行段：DESC / IDEA / JAVA / PY
        StringBuilder buf = null;
        for (String line : lines) {
            if (line.startsWith("@@")) {
                flush(cur, curSol, section, buf);
                section = null;
                buf = null;
                String body = line.substring(2).trim();
                int c = body.indexOf(':');
                String key = (c >= 0) ? body.substring(0, c).trim() : body;
                String val = (c >= 0) ? body.substring(c + 1).trim() : "";
                switch (key) {
                    case "PROBLEM":
                        cur = new Problem();
                        out.add(cur);
                        curSol = null;
                        break;
                    case "SOLUTION":
                        curSol = new Problem.Solution();
                        if (cur != null) {
                            cur.solutions.add(curSol);
                        }
                        curSol.title = val;
                        break;
                    case "NO":    if (cur != null) cur.no = Integer.parseInt(val); break;
                    case "ID":    if (cur != null) cur.id = Integer.parseInt(val); break;
                    case "TITLE": if (cur != null) cur.title = val; break;
                    case "DIFF":  if (cur != null) cur.diff = val; break;
                    case "CATEGORY": if (cur != null) cur.category = val; break;
                    case "TAGS":  if (cur != null) cur.tags = val; break;
                    case "URL":   if (cur != null) cur.url = val; break;
                    case "DESC":
                        section = "DESC";
                        buf = new StringBuilder();
                        break;
                    case "IDEA":
                        section = "IDEA";
                        buf = new StringBuilder();
                        break;
                    case "JAVA":
                        section = "JAVA";
                        buf = new StringBuilder();
                        break;
                    case "PY":
                        section = "PY";
                        buf = new StringBuilder();
                        break;
                    default:
                        /* 忽略未知标记 */
                }
            } else if (buf != null) {
                buf.append(line).append('\n');
            }
        }
        flush(cur, curSol, section, buf);
    }

    private static void flush(Problem p, Problem.Solution s, String section, StringBuilder buf) {
        if (p == null || section == null) {
            return;
        }
        String content = strip(buf.toString());
        switch (section) {
            case "DESC": p.desc = content; break;          // DESC 挂在题目上
            case "IDEA": if (s != null) s.idea = content; break; // 以下挂在解法上
            case "JAVA": if (s != null) s.java = content; break;
            case "PY":   if (s != null) s.py = content; break;
            default:     /* 忽略 */
        }
    }

    /** 去掉段首空行与段尾空白，保留内部缩进与空行 */
    private static String strip(String s) {
        int start = 0;
        while (start < s.length() && (s.charAt(start) == '\n' || s.charAt(start) == '\r')) {
            start++;
        }
        int end = s.length();
        while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(start, end);
    }
}
