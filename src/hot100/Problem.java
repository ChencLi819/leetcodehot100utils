package hot100;

import java.util.ArrayList;
import java.util.List;

/**
 * 一道题目的数据模型（对应 data/ 目录下解析后的文本数据）。
 */
public class Problem {
    /** 题单序号 1..100 */
    public int no;
    /** 力扣题号 */
    public int id;
    /** 标题 */
    public String title = "";
    /** 难度：简单 / 中等 / 困难 */
    public String diff = "";
    /** 所属分类，如：哈希、双指针 */
    public String category = "";
    /** 标签，如：数组;哈希表 */
    public String tags = "";
    /** 力扣原题链接 */
    public String url = "";
    /** 题面描述 */
    public String desc = "";
    /** 多种参考解法（每题至少 3 种） */
    public final List<Solution> solutions = new ArrayList<>();
    /** 是否已完成（打卡），保存于 ~/.hot100/progress.properties */
    public boolean done;
    /** 是否收藏 */
    public boolean fav;

    /** 一种参考解法：方法名 + 思路 + Java/Python 代码 */
    public static class Solution {
        /** 方法名，如：方法一：哈希表（推荐） */
        public String title = "";
        /** 解题思路（一句话 + 复杂度） */
        public String idea = "";
        /** Java 代码 */
        public String java = "";
        /** Python 代码 */
        public String py = "";
    }
}
