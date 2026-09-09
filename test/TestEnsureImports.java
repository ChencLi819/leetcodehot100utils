import hot100.Hot100App;

/** import 补全回归测试（IDEA 风格：显式、可见、去重、跳过已覆盖包） */
public class TestEnsureImports {
    static int failed = 0;

    public static void main(String[] args) {
        // 场景1：无 import 的力扣风格代码 → 补 java.util 相关
        String code1 = "class Solution {\n"
                + "    public int[] twoSum(int[] nums, int target) {\n"
                + "        Map<Integer, Integer> seen = new HashMap<>();\n"
                + "        List<Integer> list = new ArrayList<>();\n"
                + "        Arrays.sort(nums);\n"
                + "        int[] arr = list.stream().mapToInt(Integer::intValue).toArray();\n"
                + "        java.util.List<Integer> collected = list.stream()"
                + ".collect(Collectors.toList());\n"
                + "        return arr;\n"
                + "    }\n"
                + "}";
        String m1 = Hot100App.ensureJavaImports(code1);
        check(m1.startsWith("import java.util."), "import 插入到文件顶部");
        check(m1.contains("import java.util.Map;"), "补 Map");
        check(m1.contains("import java.util.HashMap;"), "补 HashMap");
        check(m1.contains("import java.util.stream.Collectors;"), "补 Collectors");

        // 场景2：已有通配符 import java.util.* → 不补单条 java.util
        String code2 = "import java.util.*;\n\nclass S {\n    Map<String, List<Integer>> m;\n}";
        check(Hot100App.ensureJavaImports(code2).equals(code2), "通配符已覆盖时不补单条 import");

        // 场景3：跨包混合 → 只补缺失包
        String code3 = "import java.util.*;\n\nclass S {\n    java.util.stream.Stream<Integer> s;\n"
                + "    java.math.BigInteger b;\n}";
        String m3 = Hot100App.ensureJavaImports(code3);
        check(!m3.contains("import java.util.Map;"), "通配符覆盖包不重复补");

        // 场景4：package 声明时 import 插在其后
        String code4 = "package a.b;\n\nclass S {\n    Map<String, String> m;\n}";
        check(Hot100App.ensureJavaImports(code4).contains("package a.b;\nimport "),
                "import 位于 package 之后");

        // 场景5：Python 补 typing/collections/heapq
        String py = "class Solution:\n    def twoSum(self, nums: List[int], target: int):\n        pass";
        String mp = Hot100App.ensurePythonImports(py);
        check(mp.startsWith("from typing import *"), "Python 补 typing");
        check(mp.contains("from collections import *"), "Python 补 collections");
        String py2 = "from typing import *\n\nclass S:\n    pass";
        check(Hot100App.ensurePythonImports(py2).contains("from collections import *"),
                "Python 只补缺失项");

        System.out.println(failed == 0 ? "ALL ENSURE-IMPORTS TESTS PASSED" : failed + " TESTS FAILED");
        if (failed > 0) System.exit(1);
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
