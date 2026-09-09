import hot100.RunService;

import java.lang.reflect.Method;

/** 编译器端到端测试：力扣风格无 import 代码（Map/HashMap/中文字符串）直接编译运行 */
public class RunCompilerTest {

    public static void main(String[] args) throws Exception {
        // ---- Java：力扣风格（无任何 import，直接用 Map/HashMap），由运行时自动注入
        String java = "class Solution {\n"
                + "    public int[] twoSum(int[] nums, int target) {\n"
                + "        Map<Integer, Integer> seen = new HashMap<>();\n"
                + "        for (int i = 0; i < nums.length; i++) {\n"
                + "            if (seen.containsKey(target - nums[i])) {\n"
                + "                return new int[]{seen.get(target - nums[i]), i};\n"
                + "            }\n"
                + "            seen.put(nums[i], i);\n"
                + "        }\n"
                + "        return new int[0];\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "public class Main {\n"
                + "    public static void main(String[] args) {\n"
                + "        int[] r = new Solution().twoSum(new int[]{2, 7, 11, 15}, 9);\n"
                + "        System.out.println(\"答案: [\" + r[0] + \", \" + r[1] + \"]\");\n"
                + "    }\n"
                + "}";

        RunService.RunResult r1 = invoke("runJava", java);
        System.out.println("java ok=" + r1.ok);
        System.out.println(r1.output);
        if (!r1.ok || !r1.output.contains("[0, 1]")) {
            System.out.println("JAVA E2E FAILED");
            System.exit(1);
        }

        // ---- 故意写错（缺分号）验证行号换算正确（注入行数不影响报告行号）
        String bad = "class Solution {\n"
                + "    public int bad() {\n"
                + "        List<Integer> x = new ArrayList<>()  // 故意缺分号\n"
                + "        return 0;\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "public class Main {\n"
                + "    public static void main(String[] args) {}\n"
                + "}";
        RunService.RunResult r2 = invoke("runJava", bad);
        System.out.println("bad ok=" + r2.ok + " (应为 false)");
        System.out.println(r2.output);
        if (r2.ok || !r2.output.contains("[编译失败]")) {
            System.out.println("BAD-CODE TEST FAILED");
            System.exit(1);
        }

        // ---- Python：力扣风格注解（List/Optional）+ 中文输出
        String py = "class Solution:\n"
                + "    def twoSum(self, nums: List[int], target: int) -> List[int]:\n"
                + "        seen = {}\n"
                + "        for i, x in enumerate(nums):\n"
                + "            if target - x in seen:\n"
                + "                return [seen[target - x], i]\n"
                + "            seen[x] = i\n"
                + "        return []\n"
                + "\n"
                + "def main():\n"
                + "    print('答案:', Solution().twoSum([2, 7, 11, 15], 9))\n"
                + "\n"
                + "if __name__ == '__main__':\n"
                + "    main()";

        RunService.RunResult r3 = invoke("runPython", py);
        System.out.println("python ok=" + r3.ok);
        System.out.println(r3.output);
        if (!r3.ok) {
            System.out.println("PYTHON E2E FAILED");
            System.exit(1);
        }
        System.out.println("COMPILER E2E PASSED");
    }

    static RunService.RunResult invoke(String name, String source) throws Exception {
        Method m = RunService.class.getDeclaredMethod(name, String.class);
        m.setAccessible(true);
        return (RunService.RunResult) m.invoke(null, source);
    }
}
