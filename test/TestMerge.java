import hot100.Hot100App;

/** 合并逻辑回归测试：载入题解保留 Main/main，替换同名类，追加新类 */
public class TestMerge {
    static int failed = 0;

    public static void main(String[] args) {
        String editor = "public class Main {\n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(new Solution().twoSum(new int[]{1}, 1));\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "class Solution {\n"
                + "    public int[] twoSum(int[] nums, int target) {\n"
                + "        return null; // 旧版本\n"
                + "    }\n"
                + "}";
        String sol = "class Solution {\n"
                + "    public int[] twoSum(int[] nums, int target) {\n"
                + "        java.util.Map<Integer, Integer> seen = new java.util.HashMap<>();\n"
                + "        return new int[]{0, 1}; // 新版本\n"
                + "    }\n"
                + "}";

        // 场景1：编辑器已有同名 Solution → 替换，Main 保留
        String m1 = Hot100App.mergeJavaSolution(editor, sol);
        check(count(m1, "class Solution") == 1, "同名 Solution 只保留一份");
        check(m1.contains("public class Main"), "Main 类保留");
        check(m1.contains("System.out.println"), "main 函数体保留");
        check(m1.contains("新版本"), "题解更新为新版");
        check(!m1.contains("旧版本"), "旧实现被替换");

        // 场景2：编辑器只有 Main → 题解类追加
        String editor2 = "public class Main {\n    public static void main(String[] args) {}\n}";
        String m2 = Hot100App.mergeJavaSolution(editor2, sol);
        check(m2.contains("class Solution") && m2.contains("public class Main"), "新类正确追加");

        // 场景3：空编辑器 → 直接用题解
        check(Hot100App.mergeJavaSolution("", sol).equals(sol), "空编辑器直接载入");

        // 场景4：嵌套类（LRU 内含 Node）只按顶层替换
        String editor4 = "public class Main {\n    public static void main(String[] args) {}\n}\n"
                + "class LRUCache {\n    class Node { int k; }\n    public LRUCache(int c) {}\n}";
        String sol4 = "class LRUCache {\n    class Node { int k, v; }\n    public LRUCache(int c) {}\n}";
        String m4 = Hot100App.mergeJavaSolution(editor4, sol4);
        check(count(m4, "class LRUCache") == 1, "LRUCache 替换为一份");
        check(m4.contains("int k, v;"), "嵌套类随顶层一起更新");
        check(m4.contains("public class Main"), "Main 保留");

        // 场景5：Python 题解插入守卫之前
        String pyEditor = "from typing import List\n\ndef main():\n    pass\n\n"
                + "if __name__ == \"__main__\":\n    main()";
        String pySol = "class Solution:\n    def twoSum(self, nums, target):\n        return [0, 1]";
        String m5 = Hot100App.mergePythonSolution(pyEditor, pySol);
        int guard = m5.indexOf("if __name__");
        check(guard > m5.indexOf("class Solution"), "Python 题解插入守卫之前");
        check(m5.trim().endsWith("main()"), "__main__ 守卫保留在末尾");
        check(m5.contains("def main():"), "main 函数保留");

        System.out.println(failed == 0 ? "ALL MERGE TESTS PASSED" : failed + " TESTS FAILED");
        if (failed > 0) System.exit(1);
    }

    static int count(String s, String sub) {
        int c = 0, i = 0;
        while ((i = s.indexOf(sub, i)) >= 0) {
            c++;
            i += sub.length();
        }
        return c;
    }

    static void check(boolean ok, String msg) {
        if (!ok) {
            failed++;
            System.out.println("FAIL: " + msg);
        } else {
            System.out.println("ok - " + msg);
        }
    }
}
