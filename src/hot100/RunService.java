package hot100;

import javax.swing.SwingWorker;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 内置编译运行服务（答题区）：
 *  - Java：调用 JDK 内置 JavaCompiler 编译源码，再以独立进程运行（带超时保护）；
 *  - Python：以独立进程运行（带超时保护）。
 * 全部在子进程中执行，崩溃 / 死循环均不影响应用本体。
 */
public class RunService {

    /** 一次运行的结果 */
    public static class RunResult {
        public boolean ok;          // 编译(如有)且运行流程成功走完
        public String output;       // 合并的输出/错误信息
    }

    /** 异步运行回调（在 EDT 回调） */
    public interface Callback {
        void onFinished(RunResult result);
    }

    private static final long TIMEOUT_SECONDS = 8;

    /**
     * 异步运行用户代码。
     *
     * @param language "Java" 或 "Python"
     * @param source   用户代码全文
     * @param cb       完成回调
     */
    public static void runAsync(String language, String source, Callback cb) {
        new SwingWorker<RunResult, Void>() {
            @Override
            protected RunResult doInBackground() {
                return "Java".equals(language) ? runJava(source) : runPython(source);
            }
            @Override
            protected void done() {
                try {
                    cb.onFinished(get());
                } catch (Exception ex) {
                    RunResult r = new RunResult();
                    r.ok = false;
                    r.output = "运行异常: " + ex.getMessage();
                    cb.onFinished(r);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------ Java

    /** 常用 import（力扣环境模拟；用户编辑器中由 ensureJavaImports 显式补全，此处为运行时兜底） */
    private static final String JAVA_IMPORTS =
            "import java.util.*;\n"
          + "import java.util.function.*;\n"
          + "import java.util.stream.*;\n"
          + "import java.io.*;\n"
          + "import java.math.*;\n"
          + "import java.text.*;\n"
          + "import java.util.concurrent.*;\n"
          + "import java.util.regex.*;\n";

    /** 预置数据结构定义（追加到源码末尾——Java 顶层类顺序无关，且不影响用户行号） */
    private static final String JAVA_TREENODE =
            "class TreeNode { int val; TreeNode left, right; TreeNode() {} "
          + "TreeNode(int v) { val = v; } "
          + "TreeNode(int v, TreeNode l, TreeNode r) { val = v; left = l; right = r; } }";
    private static final String JAVA_LISTNODE =
            "class ListNode { int val; ListNode next; ListNode() {} "
          + "ListNode(int v) { val = v; } ListNode(int v, ListNode n) { val = v; next = n; } }";
    private static final String JAVA_NODE =
            "class Node { int val; Node next, random; Node() {} Node(int v) { val = v; } "
          + "Node(int v, Node n) { val = v; next = n; } "
          + "Node(int v, Node n, Node r) { val = v; next = n; random = r; } }";

    /** 组装待编译源码：import 注入在最前（合法位置），数据结构定义追加在末尾 */
    private static String buildJavaSource(String source) {
        StringBuilder head = new StringBuilder(JAVA_IMPORTS);
        StringBuilder tail = new StringBuilder();
        if (!source.matches("(?s).*\\bclass\\s+TreeNode\\b.*")) {
            tail.append(JAVA_TREENODE).append('\n');
        }
        if (!source.matches("(?s).*\\bclass\\s+ListNode\\b.*")) {
            tail.append(JAVA_LISTNODE).append('\n');
        }
        if (!source.matches("(?s).*\\bclass\\s+Node\\b.*")) {
            tail.append(JAVA_NODE).append('\n');
        }
        // 用户代码若带 package 声明，import 需在其后
        String rest = source;
        int pkgEnd = 0;
        if (source.startsWith("package ")) {
            int nl = source.indexOf('\n');
            if (nl > 0) {
                pkgEnd = nl + 1;
                rest = source.substring(pkgEnd);
            }
        }
        head.append('\n');
        return source.substring(0, pkgEnd) + head + rest + tail;
    }

    private static int countLines(String s) {
        int n = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') n++;
        }
        return n;
    }

    /** import 注入占用的行数（编译诊断行号换算；数据结构定义追加在末尾，不影响行号） */
    private static int javaInjectedLines() {
        return countLines(JAVA_IMPORTS) + 1; // import 段 + 分隔空行
    }

    private static RunResult runJava(String source) {
        RunResult result = new RunResult();
        // 要求类名为 Main（模板中已说明），便于定位入口
        if (!source.contains("class Main")) {
            result.ok = false;
            result.output = "[检查失败] 未找到 class Main。\n"
                    + "答题区 Java 代码必须包含 public class Main 和 main 方法（可先点击「恢复模板」）。\n"
                    + "如需测试 Solution，请在 Main.main 中调用它。";
            return result;
        }
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // 力扣式源码增强：import 注入在最前（合法位置）、数据结构定义追加在末尾
        String fullSource = buildJavaSource(source);
        int injectedLines = javaInjectedLines();
        Path work = null;
        try {
            work = Files.createTempDirectory("hot100_run_");
            boolean success;
            if (compiler != null) {
                // 首选：内置 JavaCompiler API（内存编译）
                DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
                SimpleJavaFileObject file = new SimpleJavaFileObject(
                        URI.create("string:///Main.java"), javax.tools.JavaFileObject.Kind.SOURCE) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return fullSource;
                    }
                };
                List<String> options = Arrays.asList("-d", work.toString(), "-encoding", "UTF-8");
                success = compiler.getTask(null, null, diags, options, null,
                        Arrays.asList(file)).call();
                if (!success) {
                    StringBuilder sb = new StringBuilder("[编译失败]\n");
                    for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
                        if (d.getKind() == Diagnostic.Kind.ERROR) {
                            long line = Math.max(1, d.getLineNumber() - injectedLines);
                            sb.append(String.format("  第 %d 行: %s%n",
                                    line, d.getMessage(Locale.SIMPLIFIED_CHINESE)));
                        }
                    }
                    result.ok = false;
                    result.output = sb.toString();
                    return result;
                }
            } else {
                // 回退：精简运行时无 jdk.compiler 模块时，调用外部 javac 编译
                String javacExe = findJavac();
                if (javacExe == null) {
                    result.ok = false;
                    result.output = "[环境错误] 当前 Java 运行环境不含编译器模块。\n"
                            + "请使用完整 JDK 启动本应用，或重新打包时加入 jdk.compiler 模块。";
                    return result;
                }
                Path srcFile = work.resolve("Main.java");
                Files.write(srcFile, fullSource.getBytes(StandardCharsets.UTF_8));
                List<String> cmd = new ArrayList<>(Arrays.asList(javacExe,
                        "-d", work.toString(), "-encoding", "UTF-8",
                        srcFile.toAbsolutePath().toString()));
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                byte[] out = p.getInputStream().readAllBytes();
                success = p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
                        && p.exitValue() == 0;
                if (!success) {
                    result.ok = false;
                    result.output = "[编译失败]\n" + new String(out, StandardCharsets.UTF_8);
                    return result;
                }
            }
            // 用同一个运行时里的 java 启动子进程（强制 UTF-8 输出，避免中文乱码）
            String javaExe = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            List<String> cmd = new ArrayList<>(Arrays.asList(javaExe,
                    "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
                    "-Dstderr.encoding=UTF-8", "-cp", work.toString(), "Main"));
            return execProcess(result, cmd);
        } catch (Exception ex) {
            result.ok = false;
            result.output = "[运行异常] " + ex.getMessage();
            return result;
        } finally {
            cleanup(work);
        }
    }

    /** 查找外部 javac：先 java.home（完整 JDK），再 PATH；找不到返回 null */
    private static String findJavac() {
        String[] candidates = {
                Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                Path.of(System.getProperty("java.home"), "bin", "javac.exe").toString(),
                "javac"
        };
        for (String c : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(c, "-version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.getInputStream().readAllBytes();
                if (p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                        && p.exitValue() == 0) {
                    return c;
                }
            } catch (Exception ignored) {
                // 尝试下一个候选
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- Python

    /** Python 预置：typing/collections 等常用导入 + 数据结构桩（力扣环境模拟） */
    private static String buildPythonSource(String source) {
        StringBuilder prelude = new StringBuilder();
        prelude.append("from typing import *\n")
               .append("from collections import *\n")
               .append("import heapq, itertools, functools, math, re, bisect, string\n");
        if (!source.contains("class TreeNode")) {
            prelude.append("class TreeNode:\n    def __init__(self, val=0, left=None, right=None):\n"
                    + "        self.val = val; self.left = left; self.right = right\n");
        }
        if (!source.contains("class ListNode")) {
            prelude.append("class ListNode:\n    def __init__(self, val=0, next=None):\n"
                    + "        self.val = val; self.next = next\n");
        }
        if (!source.contains("class Node")) {
            prelude.append("class Node:\n    def __init__(self, val=0, next=None, random=None):\n"
                    + "        self.val = val; self.next = next; self.random = random\n");
        }
        prelude.append('\n');
        return prelude + source;
    }

    /** 已探测到的 Python 解释器（null = 尚未探测，"" = 探测过但不存在） */
    private static String pythonExe;

    /**
     * 探测可用的 Python 解释器并缓存结果。
     * 候选顺序：python / python3 / py（PATH）→ 用户目录下 WorkBuddy 托管 Python → 常见安装位置。
     */
    private static String findPython() {
        if (pythonExe != null) {
            return pythonExe.isEmpty() ? null : pythonExe;
        }
        List<String> candidates = new ArrayList<>(Arrays.asList(
                "python", "python3", "py"));
        // WorkBuddy 托管 Python（本机常见位置）
        File wbBin = Path.of(System.getProperty("user.home"),
                ".workbuddy", "binaries", "python", "versions").toFile();
        if (wbBin.isDirectory()) {
            File[] vers = wbBin.listFiles(File::isDirectory);
            if (vers != null) {
                Arrays.sort(vers, java.util.Comparator.comparing(File::getName).reversed());
                for (File v : vers) {
                    File exe = Path.of(v.getAbsolutePath(), "python.exe").toFile();
                    if (exe.isFile()) {
                        candidates.add(exe.getAbsolutePath());
                        break;
                    }
                    File cur = Path.of(v.getAbsolutePath(), "current", "python.exe").toFile();
                    if (cur.isFile()) {
                        candidates.add(cur.getAbsolutePath());
                        break;
                    }
                }
            }
        }
        for (String c : candidates) {
            try {
                List<String> probe = "py".equals(c)
                        ? Arrays.asList("py", "-3", "-c", "print(1)")
                        : Arrays.asList(c, "-c", "print(1)");
                ProcessBuilder pb = new ProcessBuilder(probe);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.getInputStream().readAllBytes();
                if (p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                        && p.exitValue() == 0) {
                    pythonExe = c;
                    return c;
                }
            } catch (Exception ignored) {
                // 尝试下一个候选
            }
        }
        pythonExe = "";
        return null;
    }

    private static RunResult runPython(String source) {
        RunResult result = new RunResult();
        String py = findPython();
        if (py == null) {
            result.ok = false;
            result.output = "[环境错误] 未找到 Python 解释器。\n"
                    + "请安装 Python 3 并将其加入 PATH 环境变量后重启本应用。";
            return result;
        }
        Path work = null;
        try {
            work = Files.createTempDirectory("hot100_run_py_");
            Path file = work.resolve("main.py");
            Files.write(file, buildPythonSource(source).getBytes(StandardCharsets.UTF_8));
            List<String> cmd = "py".equals(py)
                    ? new ArrayList<>(Arrays.asList("py", "-3", "-X", "utf8",
                            file.toAbsolutePath().toString()))
                    : new ArrayList<>(Arrays.asList(py, "-X", "utf8",
                            file.toAbsolutePath().toString()));
            return execProcess(result, cmd);
        } catch (Exception ex) {
            result.ok = false;
            result.output = "[运行异常] " + ex.getMessage();
            return result;
        } finally {
            cleanup(work);
        }
    }

    // ---------------------------------------------------------------- 公共

    private static RunResult execProcess(RunResult result, List<String> cmd)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PYTHONIOENCODING", "utf-8"); // Python 输出统一 UTF-8
        pb.redirectErrorStream(true);                      // 合并 stderr
        Process process = pb.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            byte[] buf = new byte[4096];
            int n;
            try {
                while ((n = process.getInputStream().read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } catch (IOException ignored) {
                // 进程结束时的正常中断
            }
        });
        reader.setDaemon(true);
        reader.start();
        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            result.ok = false;
            result.output = out.toString(StandardCharsets.UTF_8)
                    + "\n[超时] 运行超过 " + TIMEOUT_SECONDS + " 秒已被强制终止（请检查是否存在死循环）。";
            return result;
        }
        reader.join(1000);
        int code = process.exitValue();
        result.ok = code == 0;
        result.output = out.toString(StandardCharsets.UTF_8);
        if (code != 0) {
            result.output += "\n[进程退出码] " + code;
        }
        return result;
    }

    private static void cleanup(Path dir) {
        if (dir == null) {
            return;
        }
        File[] files = dir.toFile().listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        dir.toFile().delete();
    }
}
