import hot100.Hot100App;
import hot100.Problem;
import hot100.ProblemLoader;
import java.io.File;

/** 骨架生成回归测试：方法签名保留、实现不泄漏、TODO 抛错、字段/嵌套类保留、100 题全覆盖 */
public class TestStub {
    static int failed = 0;

    public static void main(String[] args) throws Exception {
        var problems = ProblemLoader.load(new File("data"));
        check(problems.size() == 100, "100 题加载");

        // 场景1：两数之和（class Solution，单方法）
        Problem p1 = problems.get(0);
        String s1 = Hot100App.stubJavaFromSolution(p1.solutions.get(0).java);
        check(s1.contains("class Solution"), "类名保留");
        check(s1.contains("public int[] twoSum(int[] nums, int target)"), "方法签名保留");
        check(s1.contains("UnsupportedOperationException"), "方法体替换为 TODO 抛错");
        check(!s1.contains("seen.containsKey"), "参考实现不泄漏");
        check(!s1.contains("return new int[]{0, 1}"), "返回语句不泄漏");

        // 场景2：LRU 缓存（题单第 35 题，多方法 + 构造器 + 字段）
        Problem p35 = problems.get(34);
        String lru = Hot100App.stubJavaFromSolution(p35.solutions.get(0).java);
        check(lru.contains("class LRUCache"), "LRU 类名保留");
        check(lru.contains("public LRUCache(int capacity)"), "构造器签名保留");
        check(lru.contains("public int get(int key)"), "get 签名保留");
        check(lru.contains("public void put(int key, int value)"), "put 签名保留");
        check(!lru.contains("moveToFront"), "私有实现细节不泄漏");

        // 场景3：MedianFinder（字段 + 两方法）
        Problem p76 = problems.get(75);
        String mf = Hot100App.stubJavaFromSolution(p76.solutions.get(0).java);
        check(mf.contains("public void addNum(int num)"), "addNum 签名保留");
        check(mf.contains("public double findMedian()"), "findMedian 签名保留");
        check(mf.contains("PriorityQueue"), "字段保留");

        // 场景4：Python 两数之和
        String pys = Hot100App.stubPythonFromSolution(p1.solutions.get(0).py);
        check(pys.contains("class Solution:"), "Python 类保留");
        check(pys.contains("def twoSum"), "Python 方法签名保留");
        check(pys.contains("raise NotImplementedError"), "Python TODO 抛错");
        check(!pys.contains("seen = {}"), "Python 实现不泄漏");

        // 场景5：全题覆盖——100 题的 Java/Python 骨架均可生成
        int javaOk = 0, pyOk = 0;
        for (Problem p : problems) {
            String js = Hot100App.stubJavaFromSolution(p.solutions.get(0).java);
            if (js.contains("class ") && js.contains("TODO")) javaOk++;
            String ps = Hot100App.stubPythonFromSolution(p.solutions.get(0).py);
            if (ps.contains("class ") && ps.contains("TODO")) pyOk++;
        }
        check(javaOk == 100, "Java 骨架 100/100 生成");
        check(pyOk == 100, "Python 骨架 100/100 生成");

        System.out.println(failed == 0 ? "ALL STUB TESTS PASSED" : failed + " TESTS FAILED");
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
