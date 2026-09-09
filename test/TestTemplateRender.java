import hot100.Hot100App;
import hot100.Problem;
import hot100.ProblemLoader;
import java.io.File;
import java.lang.reflect.Method;

/** 模板渲染检查：确认按题目定制的示例缩进正确（main 体 8 空格，类闭合 4/0 空格） */
public class TestTemplateRender {
    public static void main(String[] args) throws Exception {
        var problems = ProblemLoader.load(new File("data"));
        Method m = Hot100App.class.getDeclaredMethod("template", String.class, Problem.class);
        m.setAccessible(true);

        for (int no : new int[]{1, 22, 35, 48, 91}) {
            Problem p = problems.get(no - 1);
            String java = (String) m.invoke(null, "Java", p);
            String py = (String) m.invoke(null, "Python", p);
            System.out.println("===== #" + no + " " + p.title + " (Java) =====");
            System.out.println(java);
            System.out.println("----- (Python) -----");
            System.out.println(py);
        }
    }
}
