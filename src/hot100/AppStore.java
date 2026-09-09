package hot100;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 用户数据存取：打卡/收藏进度 + 答题区代码，保存在 ~/.hot100/ 下。
 * 独立于安装目录，更新 / 重装应用不会丢失进度。
 */
public class AppStore {

    private static final File DIR = new File(System.getProperty("user.home"), ".hot100");
    private static final File PROGRESS_FILE = new File(DIR, "progress.properties");
    private static final File ANSWERS_FILE = new File(DIR, "answers.properties");
    private static final File NOTES_FILE = new File(DIR, "notes.properties");

    private static final Properties progress = new Properties();
    private static final Properties answers = new Properties();
    private static final Properties notes = new Properties();

    private AppStore() {
    }

    public static void load() {
        try {
            DIR.mkdirs();
        } catch (Exception ignored) {
            // 目录已存在等情况
        }
        loadInto(progress, PROGRESS_FILE);
        loadInto(answers, ANSWERS_FILE);
        loadInto(notes, NOTES_FILE);
    }

    private static void loadInto(Properties props, File file) {
        if (!file.isFile()) {
            return;
        }
        try (InputStreamReader r = new InputStreamReader(
                new java.io.FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException ex) {
            System.err.println("读取 " + file.getName() + " 失败: " + ex.getMessage());
        }
    }

    public static boolean isDone(int no) {
        return "true".equalsIgnoreCase(progress.getProperty("done." + no));
    }

    public static boolean isFav(int no) {
        return "true".equalsIgnoreCase(progress.getProperty("fav." + no));
    }

    public static void setDone(int no, boolean done) {
        setFlag(progress, "done." + no, done);
        save(progress, PROGRESS_FILE);
    }

    public static void setFav(int no, boolean fav) {
        setFlag(progress, "fav." + no, fav);
        save(progress, PROGRESS_FILE);
    }

    public static String getAnswer(int no, String lang) {
        return answers.getProperty(no + "." + lang, "");
    }

    public static void saveAnswer(int no, String lang, String code) {
        if (code == null || code.isEmpty()) {
            answers.remove(no + "." + lang);
        } else {
            answers.setProperty(no + "." + lang, code);
        }
        save(answers, ANSWERS_FILE);
    }

    public static String getNote(int no) {
        return notes.getProperty("note." + no, "");
    }

    public static void saveNote(int no, String text) {
        if (text == null || text.isEmpty()) {
            notes.remove("note." + no);
        } else {
            notes.setProperty("note." + no, text);
        }
        save(notes, NOTES_FILE);
    }

    private static void setFlag(Properties props, String key, boolean value) {
        if (value) {
            props.setProperty(key, "true");
        } else {
            props.remove(key);
        }
    }

    private static void save(Properties props, File file) {
        try (Writer w = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            props.store(w, "LeetCode Hot100 刷题助手 用户数据");
        } catch (IOException ex) {
            System.err.println("保存 " + file.getName() + " 失败: " + ex.getMessage());
        }
    }
}
