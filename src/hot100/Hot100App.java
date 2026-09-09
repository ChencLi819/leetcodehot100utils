package hot100;

import com.formdev.flatlaf.FlatLightLaf;
import hot100.Problem.Solution;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * LeetCode 热题 100 · 刷题助手（纯 Java Swing + FlatLaf，无 HTML/CSS/JS）。
 *
 * 功能：题目列表 / 搜索 / 筛选 / 打卡收藏 ·
 *       每题 ≥3 种参考解法（思路 + Java/Python 代码）·
 *       内置编译器答题区（Java 编译运行 / Python 运行）·
 *       进度保存在 ~/.hot100/。
 */
public class Hot100App extends JFrame {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------ 主题配色
    // 直接取自本机 WorkBuddy 官方资源：主题色采样自官方 App 图标（assets/app_icon.png）
    // 渐变 #0CC8A4(青) → #65D275(绿)；品牌绿 token 印证：#00C29A / #4ECBA0
    private static final Color ACCENT       = new Color(0x00, 0xC2, 0x9A); // 品牌绿（按钮/激活）
    private static final Color ACCENT_HOVER = new Color(0x00, 0xAD, 0x8A); // hover 深一档
    private static final Color ACCENT_DEEP  = new Color(0x00, 0xA2, 0x82); // 小号文字用深绿
    private static final Color ACCENT_SOFT  = new Color(0xE7, 0xF8, 0xF3); // 绿淡底（思路面板）
    private static final Color GRAD_A       = new Color(0x0C, 0xC8, 0xA4); // 官方渐变青
    private static final Color GRAD_B       = new Color(0x65, 0xD2, 0x75); // 官方渐变绿
    private static final Color INK          = new Color(0x33, 0x33, 0x33); // --cb-text-primary
    private static final Color INK_SOFT     = new Color(0x66, 0x66, 0x66); // --cb-text-secondary
    private static final Color INK_FAINT    = new Color(0x80, 0x80, 0x80); // --cb-text-tertiary
    private static final Color LINE         = new Color(0xE1, 0xE1, 0xE1); // --cb-vscode-panel-border
    private static final Color LINE_SOFT    = new Color(0xEB, 0xEB, 0xEB); // --cb-border-secondary
    private static final Color BG_CARD      = new Color(0xFA, 0xFA, 0xFA); // --cb-bg-card
    private static final Color GREEN        = new Color(0x16, 0x82, 0x5D); // --cb-diff-added（成功绿）
    private static final Color ORANGE       = new Color(0xF5, 0x9E, 0x0B); // --cb-warning（警示橙）
    private static final Color RED          = new Color(0xCF, 0x22, 0x2E); // --cb-danger（危险红）

    private static final Font FONT_UI   = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Microsoft YaHei UI", Font.BOLD, 14);
    private static final Font FONT_HEAD = new Font("Microsoft YaHei UI", Font.BOLD, 20);
    private static final Font FONT_META = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
    // 逻辑等宽字体：物理字体（如 Consolas）缺少中文字形会显示方块，逻辑字体可自动回退
    private static final Font FONT_CODE = new Font(Font.MONOSPACED, Font.PLAIN, 14);

    // ------------------------------------------------------------ 组件
    private final List<Problem> allProblems = new ArrayList<>();
    private final DefaultListModel<Problem> listModel = new DefaultListModel<>();
    /** 强制列表宽度=视口宽度：长标题只截断自身，右侧难度永远完整可见、无横向滚动条 */
    private final JList<Problem> problemList = new JList<Problem>(listModel) {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
    };

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> diffBox = new JComboBox<>();
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JCheckBox undoneBox = new JCheckBox("只看未完成");
    private final JCheckBox favBox = new JCheckBox("只看收藏");

    private final JLabel titleLabel = new JLabel(" ");
    private final JLabel metaLabel = new JLabel(" ");
    private final PillLabel diffPill = new PillLabel();
    private final JButton doneBtn = new JButton("标记完成");
    private final JButton favBtn = new JButton("☆ 收藏");
    private final JButton linkBtn = new JButton("力扣原题 ↗");

    private final JTabbedPane mainTabs = new JTabbedPane();
    private final JTextArea descArea = codeArea(FONT_UI, true, Color.WHITE, INK);

    private final JComboBox<String> solutionBox = new JComboBox<>();
    private final JTextArea ideaArea = codeArea(FONT_UI, true, ACCENT_SOFT, INK);
    private final JTextArea solJavaArea = codeArea(FONT_CODE, false, Color.WHITE, INK);
    private final JTextArea solPyArea = codeArea(FONT_CODE, false, Color.WHITE, INK);
    private final JTabbedPane solLangTabs = new JTabbedPane();

    private final JComboBox<String> langBox = new JComboBox<>(new String[]{"Java", "Python"});
    private final JButton runBtn = new JButton("▶ 运行");
    private final JButton loadSolBtn = new JButton("载入题解代码");
    private final JButton resetTplBtn = new JButton("恢复模板");
    private final JTextArea editorArea = codeArea(FONT_CODE, false, Color.WHITE, INK);
    private final JTextArea outputArea = codeArea(FONT_CODE, false, Color.WHITE, INK);

    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel(" ");

    /** 可折叠笔记侧边栏（按题目保存于 ~/.hot100/notes.properties） */
    private final JPanel notesPanel = new JPanel(new BorderLayout());
    private final JTextArea notesArea = new JTextArea();
    private final JLabel notesTitle = new JLabel("  笔记");
    private final JToggleButton notesToggle = new JToggleButton("笔记");
    private javax.swing.Timer noteSaveTimer; // 防抖：停顿 1.2s 后自动保存
    private final GroupedUndoManager editorUndo = new GroupedUndoManager();
    private final GroupedUndoManager notesUndo = new GroupedUndoManager();

    /** 分组撤销管理器：连续键入自动合并为一步（Ctrl+Z 一次撤回整段输入，符合“撤销上一步”直觉） */
    private static final class GroupedUndoManager extends javax.swing.undo.UndoManager {
        private javax.swing.undo.CompoundEdit compound; // 打开中的连续输入组
        private int pos = -1;       // 上次编辑后的光标位置
        private boolean lastInsert; // 上次编辑是否为插入

        private void endCompound() {
            if (compound != null) {
                compound.end();
                super.addEdit(compound);
                compound = null;
            }
        }

        /** 断开当前分组：下一个编辑将作为独立的一步（用于粘贴/剪切/缩进等独立操作前调用） */
        public void breakGroup() {
            endCompound();
        }

        @Override
        public boolean addEdit(javax.swing.undo.UndoableEdit e) {
            if (e instanceof javax.swing.text.AbstractDocument.DefaultDocumentEvent de
                    && de.getType() != javax.swing.event.DocumentEvent.EventType.CHANGE) {
                boolean insert = de.getType() == javax.swing.event.DocumentEvent.EventType.INSERT;
                int off = de.getOffset(), len = de.getLength();
                // 连续判定：同为插入且新内容紧跟原光标（打字）；同为删除且删在原光标处/前一字符（退格）
                boolean contiguous = compound != null && insert == lastInsert
                        && (insert ? off == pos : (off == pos || off == pos - 1));
                if (!contiguous) {
                    endCompound();
                    compound = new javax.swing.undo.CompoundEdit();
                    lastInsert = insert;
                }
                compound.addEdit(e);
                pos = insert ? off + len : off;
                return true;
            }
            endCompound(); // 样式/替换类编辑独立成步
            return super.addEdit(e);
        }

        @Override
        public void undo() throws javax.swing.undo.CannotUndoException {
            endCompound(); // 撤销前先结组：一次撤回整段连续输入
            super.undo();
        }

        @Override
        public boolean canUndo() {
            endCompound();
            return super.canUndo();
        }

        @Override
        public void redo() throws javax.swing.undo.CannotRedoException {
            endCompound();
            super.redo();
        }

        @Override
        public boolean canRedo() {
            endCompound();
            return super.canRedo();
        }

        @Override
        public void discardAllEdits() {
            endCompound();
            super.discardAllEdits();
        }
    }

    private Problem current;              // 当前展示的题目
    private boolean updatingSolutionBox;  // 防止联动递归
    private boolean running;              // 是否正在运行用户代码

    public Hot100App(List<Problem> problems) {
        super("LeetCode 热题 100 · 刷题助手");
        allProblems.addAll(problems);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        setIconImage(createAppIcon());
        initUI();
        bindEvents();
        applyFilters();
        setSize(1360, 860);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
    }

    // ================================================================ UI 构建

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.add(buildSidebar(), BorderLayout.WEST);
        content.add(buildMainArea(), BorderLayout.CENTER);
        content.add(buildNotesPanel(), BorderLayout.EAST);
        content.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(content);
    }

    /** 右侧笔记侧边栏：可展开 / 收起，按题目自动保存 */
    private JComponent buildNotesPanel() {
        notesPanel.setBackground(Color.WHITE);
        notesPanel.setPreferredSize(new Dimension(300, 0));
        notesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, LINE),
                new EmptyBorder(14, 12, 10, 12)));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        notesTitle.setFont(FONT_BOLD);
        notesTitle.setForeground(INK);
        JLabel tip = new JLabel("自动保存");
        tip.setFont(FONT_META);
        tip.setForeground(INK_FAINT);
        JPanel headRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headRight.setOpaque(false);
        headRight.add(tip);
        head.add(notesTitle, BorderLayout.WEST);
        head.add(headRight, BorderLayout.EAST);
        head.setBorder(new EmptyBorder(0, 0, 8, 0));

        notesArea.setFont(FONT_UI);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBackground(BG_CARD);
        notesArea.setForeground(INK);
        notesArea.setCaretColor(INK);
        notesArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(notesArea);
        scroll.setBorder(roundedBorder(LINE));
        scroll.getViewport().setBackground(BG_CARD);

        notesPanel.add(head, BorderLayout.NORTH);
        notesPanel.add(scroll, BorderLayout.CENTER);
        return notesPanel;
    }

    /** 左侧边栏：品牌头 + 搜索 + 筛选 + 题目列表 */
    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(424, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, LINE));

        JPanel top = new JPanel(new BorderLayout(0, 0));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(18, 16, 14, 16));
        top.add(buildBrandHeader(), BorderLayout.NORTH);

        // 搜索 + 筛选
        JPanel filters = new JPanel(new BorderLayout(0, 8));
        filters.setOpaque(false);
        filters.setBorder(new EmptyBorder(0, 0, 12, 0));
        searchField.setFont(FONT_UI);
        searchField.putClientProperty("JTextField.placeholderText", "搜索：序号 / 题号 / 标题 / 标签");
        searchField.putClientProperty("JTextField.leadingIcon", createSearchIcon());
        diffBox.setFont(FONT_UI);
        for (String d : new String[]{"全部难度", "简单", "中等", "困难"}) {
            diffBox.addItem(d);
        }
        catBox.setFont(FONT_UI);
        catBox.addItem("全部分类");
        for (String c : categories()) {
            catBox.addItem(c);
        }
        undoneBox.setFont(FONT_UI);
        favBox.setFont(FONT_UI);
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterRow.setOpaque(false);
        filterRow.add(diffBox);
        filterRow.add(catBox);
        JPanel checkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        checkRow.setOpaque(false);
        checkRow.add(undoneBox);
        checkRow.add(favBox);
        JPanel stack = new JPanel(new java.awt.GridLayout(3, 1, 0, 8));
        stack.setOpaque(false);
        stack.add(searchField);
        stack.add(filterRow);
        stack.add(checkRow);
        filters.add(stack, BorderLayout.NORTH);
        top.add(filters, BorderLayout.SOUTH);

        sidebar.add(top, BorderLayout.NORTH);
        JScrollPane listScroll = new JScrollPane(problemList);
        listScroll.setBorder(null);
        // 只保留纵向滚动条：列表宽度恒等于视口，右侧难度不再被裁切
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.getViewport().setBackground(Color.WHITE);
        sidebar.add(listScroll, BorderLayout.CENTER);
        return sidebar;
    }

    /** 品牌头：原创渐变 Logo（官方同款绿色渐变风格）+ 标题 */
    private JComponent buildBrandHeader() {
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        JLabel logo = new JLabel(new ImageIcon(drawBrandLogo(48)));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setVerticalAlignment(SwingConstants.CENTER);
        brand.add(logo, BorderLayout.WEST);
        JPanel texts = new JPanel(new java.awt.GridLayout(2, 1, 0, 1));
        texts.setOpaque(false);
        JLabel title = new JLabel("LeetCode 热题 100");
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 15));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("刷题助手");
        subtitle.setFont(FONT_META);
        subtitle.setForeground(INK_FAINT);
        texts.add(title);
        texts.add(subtitle);
        brand.add(texts, BorderLayout.CENTER);
        return brand;
    }

    /** 右侧主区：题目头 + 三个选项卡 */
    private JComponent buildMainArea() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(14, 18, 10, 18));
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(mainTabs, BorderLayout.CENTER);

        // 选项卡 1：题目描述
        mainTabs.addTab("题目描述", scrollOf(descArea, new EmptyBorder(4, 2, 4, 8)));
        // 选项卡 2：参考题解
        mainTabs.addTab("参考题解", buildSolutionsTab());
        // 选项卡 3：我的作答（内置编译器）
        mainTabs.addTab("我的作答", buildAnswerTab());
        mainTabs.setFont(FONT_BOLD);

        return main;
    }

    /** 题目头：标题 + 元信息 + 操作按钮 */
    private JComponent buildHeader() {
        titleLabel.setFont(FONT_HEAD);
        titleLabel.setForeground(INK);
        metaLabel.setFont(FONT_META);
        metaLabel.setForeground(INK_SOFT);

        doneBtn.setFont(FONT_BOLD);
        favBtn.setFont(FONT_BOLD);
        linkBtn.setFont(FONT_UI);
        linkBtn.setForeground(ACCENT_DEEP);
        linkBtn.setBorderPainted(false);
        linkBtn.setContentAreaFilled(false);
        linkBtn.setFocusPainted(false);
        linkBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        notesToggle.setFont(FONT_UI);
        notesToggle.setSelected(true); // 默认展开笔记栏

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(notesToggle);
        buttons.add(doneBtn);
        buttons.add(favBtn);
        buttons.add(linkBtn);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        titleRow.add(titleLabel);
        titleRow.add(diffPill);
        JPanel texts = new JPanel(new java.awt.GridLayout(2, 1, 0, 3));
        texts.setOpaque(false);
        texts.add(titleRow);
        texts.add(metaLabel);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(texts, BorderLayout.CENTER);
        header.add(buttons, BorderLayout.EAST);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                new EmptyBorder(0, 0, 12, 0)));
        return header;
    }

    /** 参考题解选项卡：方法选择 + 思路 + Java/Python 代码 */
    private JComponent buildSolutionsTab() {
        solutionBox.setFont(FONT_BOLD);

        JLabel ideaTitle = new JLabel("解题思路");
        ideaTitle.setFont(FONT_BOLD);
        ideaTitle.setForeground(ACCENT_DEEP);
        ideaArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JPanel ideaPanel = new JPanel(new BorderLayout(0, 4));
        ideaPanel.setOpaque(false);
        ideaPanel.add(ideaTitle, BorderLayout.NORTH);
        JScrollPane ideaScroll = new JScrollPane(ideaArea);
        ideaScroll.setBorder(roundedBorder(new Color(0xCB, 0xEF, 0xE2)));
        ideaScroll.getViewport().setBackground(ACCENT_SOFT);
        ideaScroll.setPreferredSize(new Dimension(0, 92));
        ideaPanel.add(ideaScroll, BorderLayout.CENTER);

        solLangTabs.addTab("Java", scrollOf(solJavaArea, new EmptyBorder(6, 8, 6, 8)));
        solLangTabs.addTab("Python", scrollOf(solPyArea, new EmptyBorder(6, 8, 6, 8)));
        solLangTabs.setFont(FONT_BOLD);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(solutionBox, BorderLayout.NORTH);
        panel.add(ideaPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ideaPanel, solLangTabs);
        split.setResizeWeight(0.0);
        split.setDividerLocation(130);
        split.setBorder(null);
        split.setContinuousLayout(true);

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(solutionBox, BorderLayout.NORTH);
        wrap.add(split, BorderLayout.CENTER);
        return wrap;
    }

    /** 我的作答选项卡：工具栏 + 编辑器 + 输出控制台 */
    private JComponent buildAnswerTab() {
        langBox.setFont(FONT_BOLD);
        runBtn.setFont(FONT_BOLD);
        runBtn.setForeground(Color.WHITE);
        runBtn.setBackground(ACCENT);          // 品牌绿主按钮
        runBtn.setOpaque(true);
        runBtn.setBorderPainted(false);
        runBtn.setFocusPainted(false);
        runBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        runBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (runBtn.isEnabled()) runBtn.setBackground(ACCENT_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                runBtn.setBackground(ACCENT);
            }
        });
        loadSolBtn.setFont(FONT_UI);
        resetTplBtn.setFont(FONT_UI);

        JLabel hint = new JLabel(
                "IDEA 风格：载入题解自动补 import 并保留 main · Tab 展开模板（sout/psvm/fori/iter/tryc…）"
                        + " · Ctrl+Enter 运行 · Ctrl+滚轮缩放字号");
        hint.setFont(FONT_META);
        hint.setForeground(INK_SOFT);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("语言"));
        toolbar.add(langBox);
        toolbar.add(runBtn);
        toolbar.add(loadSolBtn);
        toolbar.add(resetTplBtn);
        toolbar.add(hint);

        editorArea.setEditable(true); // 答题区必须可编辑
        editorArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel consoleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        consoleBar.setBackground(BG_CARD);
        consoleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE_SOFT));
        JLabel consoleDot = new JLabel(dot(ACCENT));
        consoleDot.setBorder(new EmptyBorder(0, 0, 0, 6));
        JLabel consoleTitle = new JLabel("运行输出");
        consoleTitle.setFont(FONT_BOLD);
        consoleTitle.setForeground(INK_SOFT);
        consoleBar.add(consoleDot);
        consoleBar.add(consoleTitle);

        JPanel console = new JPanel(new BorderLayout());
        console.setBackground(Color.WHITE);
        console.setBorder(roundedBorder(LINE));
        console.add(consoleBar, BorderLayout.NORTH);
        JScrollPane outScroll = new JScrollPane(outputArea);
        outScroll.setBorder(null);
        outScroll.getViewport().setBackground(Color.WHITE);
        outScroll.setPreferredSize(new Dimension(0, 220));
        console.add(outScroll, BorderLayout.CENTER);

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setOpaque(false);
        editorPanel.add(toolbar, BorderLayout.NORTH);
        JScrollPane editScroll = new JScrollPane(editorArea);
        editScroll.setBorder(roundedBorder(LINE));
        editScroll.getViewport().setBackground(Color.WHITE);
        editorPanel.add(editScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, console);
        split.setResizeWeight(1.0);
        split.setDividerLocation(0.62);
        split.setBorder(null);
        split.setContinuousLayout(true);
        return split;
    }

    /** 底部状态栏：进度统计 + 进度条 */
    private JComponent buildStatusBar() {
        statusLabel.setFont(FONT_UI);
        statusLabel.setForeground(INK_SOFT);
        statusLabel.setBorder(new EmptyBorder(8, 18, 8, 0));
        progressBar.setPreferredSize(new Dimension(280, 14));
        progressBar.setStringPainted(true);
        progressBar.setFont(FONT_META);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 0, 0, 18));
        right.add(progressBar);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE));
        bar.add(statusLabel, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ================================================================ 事件绑定

    private void bindEvents() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        diffBox.addActionListener(e -> applyFilters());
        catBox.addActionListener(e -> applyFilters());
        undoneBox.addActionListener(e -> applyFilters());
        favBox.addActionListener(e -> applyFilters());

        problemList.setCellRenderer(new ProblemRenderer());
        problemList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showProblem(problemList.getSelectedValue());
                }
            }
        });

        solutionBox.addActionListener(e -> {
            if (!updatingSolutionBox) {
                showSolution();
            }
        });
        doneBtn.addActionListener(e -> toggleDone());
        favBtn.addActionListener(e -> toggleFav());
        linkBtn.addActionListener(e -> openUrl());

        runBtn.addActionListener(e -> runAnswer());
        resetTplBtn.addActionListener(e -> resetTemplate());
        loadSolBtn.addActionListener(e -> loadSolutionCode());
        langBox.addActionListener(e -> switchLanguage());
        notesToggle.addActionListener(e -> toggleNotes());

        // 笔记自动保存（防抖 1.2s）
        notesArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { scheduleNoteSave(); }
            @Override public void removeUpdate(DocumentEvent e) { scheduleNoteSave(); }
            @Override public void changedUpdate(DocumentEvent e) { scheduleNoteSave(); }
        });
        noteSaveTimer = new javax.swing.Timer(1200, e -> flushNoteSave());
        noteSaveTimer.setRepeats(false);

        // 编辑器：Ctrl+Enter 运行；Enter 智能展开/后缀（无命中时换行+缩进）；Tab 缩进 / Shift+Tab 反缩进
        editorArea.getInputMap().put(
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK),
                "runCode");
        editorArea.getActionMap().put("runCode", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runAnswer();
            }
        });

        // 编辑器 + 笔记区：全选 / 缩进 / 反缩进 / 撤销 / 重做
        installEditorKeys(editorArea, editorUndo, true);
        installEditorKeys(notesArea, notesUndo, false);

        // Ctrl+滚轮 缩放各内容区字号（题目描述 / 思路 / 题解代码 / 作答 / 输出）
        for (JTextArea area : new JTextArea[]{descArea, ideaArea, solJavaArea,
                solPyArea, editorArea, outputArea}) {
            attachFontZoom(area);
        }
    }

    /** Ctrl+滚轮 调节文本区字号（10~30），未按 Ctrl 时滚轮仍正常滚动 */
    private void attachFontZoom(JTextArea area) {
        area.addMouseWheelListener(e -> {
            if (!e.isControlDown()) {
                return;
            }
            Font f = area.getFont();
            int size = f.getSize() + (e.getWheelRotation() < 0 ? 1 : -1);
            size = Math.max(10, Math.min(30, size));
            if (size != f.getSize()) {
                area.setFont(f.deriveFont((float) size));
            }
            e.consume();
        });
    }

    /** 统一安装编辑键：Ctrl+A 全选 · Ctrl+Z 撤销 · Ctrl+Y/Ctrl+Shift+Z 重做 ·
     *  Tab 缩进（选中多行整体右移）· Shift+Tab 反缩进 · Enter（编辑器=智能展开，笔记=换行+缩进） */
    private void installEditorKeys(JTextArea area, javax.swing.undo.UndoManager undo,
                                   boolean smart) {
        area.setFocusTraversalKeysEnabled(false); // Tab 用于缩进而非切换焦点
        area.getDocument().addUndoableEditListener(e -> undo.addEdit(e.getEdit()));

        javax.swing.InputMap im = area.getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap am = area.getActionMap();

        javax.swing.Action selectAll = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                area.selectAll();
                area.requestFocusInWindow();
            }
        };
        im.put(javax.swing.KeyStroke.getKeyStroke("control A"), "selectAll");
        am.put("selectAll", selectAll);

        javax.swing.Action undoAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undo.canUndo()) {
                    undo.undo();
                }
            }
        };
        im.put(javax.swing.KeyStroke.getKeyStroke("control Z"), "undo");
        am.put("undo", undoAction);

        javax.swing.Action redoAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undo.canRedo()) {
                    undo.redo();
                }
            }
        };
        im.put(javax.swing.KeyStroke.getKeyStroke("control Y"), "redo");
        im.put(javax.swing.KeyStroke.getKeyStroke("control shift Z"), "redo");
        am.put("redo", redoAction);

        javax.swing.Action indentAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                indentSelection(area);
            }
        };
        im.put(javax.swing.KeyStroke.getKeyStroke("TAB"), "indent");
        am.put("indent", indentAction);

        javax.swing.Action dedentAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dedentSelection(area);
            }
        };
        im.put(javax.swing.KeyStroke.getKeyStroke("shift TAB"), "dedent");
        am.put("dedent", dedentAction);

        // 剪贴板（Word 式）：粘贴/剪切各自独立成撤销步；兼容 Windows 传统键位
        java.util.LinkedHashMap<String, javax.swing.Action> clip = new java.util.LinkedHashMap<>();
        clip.put("control C", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { area.copy(); }
        });
        clip.put("control X", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { ((GroupedUndoManager) undo).breakGroup(); area.cut(); }
        });
        clip.put("control V", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { ((GroupedUndoManager) undo).breakGroup(); area.paste(); }
        });
        clip.put("control INSERT", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { area.copy(); }
        });
        clip.put("shift DELETE", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { ((GroupedUndoManager) undo).breakGroup(); area.cut(); }
        });
        clip.put("shift INSERT", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { ((GroupedUndoManager) undo).breakGroup(); area.paste(); }
        });
        clip.forEach((ks, action) -> {
            im.put(javax.swing.KeyStroke.getKeyStroke(ks), "clip" + ks);
            am.put("clip" + ks, action);
        });

        javax.swing.Action enterAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!(smart && smartEnter())) {
                    newlineAndIndent(area);
                }
            }
        };
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        am.put("enter", enterAction);
    }

    // ================================================================ 数据展示

    private LinkedHashSet<String> categories() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (Problem p : allProblems) {
            set.add(p.category);
        }
        return set;
    }

    /** 刷新左侧列表（保留选中项） */
    private void applyFilters() {
        Problem selected = problemList.getSelectedValue();
        listModel.removeAllElements();
        String q = searchField.getText().trim().toLowerCase();
        String diff = (String) diffBox.getSelectedItem();
        String cat = (String) catBox.getSelectedItem();
        for (Problem p : allProblems) {
            if (!"全部难度".equals(diff) && !p.diff.equals(diff)) continue;
            if (!"全部分类".equals(cat) && !p.category.equals(cat)) continue;
            if (undoneBox.isSelected() && p.done) continue;
            if (favBox.isSelected() && !p.fav) continue;
            if (!q.isEmpty()) {
                String hay = (p.no + " " + p.id + " " + p.title + " " + p.tags + " " + p.category)
                        .toLowerCase();
                if (!hay.contains(q)) continue;
            }
            listModel.addElement(p);
        }
        if (selected != null && listModel.contains(selected)) {
            problemList.setSelectedValue(selected, true);
        } else if (!listModel.isEmpty()) {
            problemList.setSelectedIndex(0);
        } else {
            showProblem(null);
        }
        updateStatus();
    }

    /** 展示某题目（自动保存上一题草稿） */
    private void showProblem(Problem p) {
        if (p == current) {
            return;
        }
        saveCurrentAnswer();
        flushNoteSave(); // 保存上一题的笔记
        current = p;
        if (p == null) {
            titleLabel.setText("（无题目）");
            metaLabel.setText(" ");
            diffPill.setText(null);
            descArea.setText("");
            solutionBox.removeAllItems();
            solJavaArea.setText("");
            solPyArea.setText("");
            ideaArea.setText("");
            editorArea.setText("");
            outputArea.setText("");
            notesArea.setText("");
            notesTitle.setText("笔记");
            doneBtn.setEnabled(false);
            favBtn.setEnabled(false);
            linkBtn.setEnabled(false);
            return;
        }
        doneBtn.setEnabled(true);
        favBtn.setEnabled(true);
        linkBtn.setEnabled(true);
        titleLabel.setText(String.format("%03d · %s", p.no, p.title));
        diffPill.setText(p.diff);
        diffPill.setColor(diffColor(p.diff));
        metaLabel.setText("力扣 #" + p.id + "   ·   " + p.category + "   ·   "
                + (p.solutions.isEmpty() ? "" : p.solutions.size() + " 种解法"));
        descArea.setText(p.desc + "\n\n标签：" + p.tags + "\n链接：" + p.url);
        descArea.setCaretPosition(0);

        // 重建解法下拉
        updatingSolutionBox = true;
        solutionBox.removeAllItems();
        for (Solution s : p.solutions) {
            solutionBox.addItem(s.title);
        }
        if (!p.solutions.isEmpty()) {
            solutionBox.setSelectedIndex(0);
        }
        updatingSolutionBox = false;
        showSolution();

        // 载入草稿或按题目定制的模板
        String lang = (String) langBox.getSelectedItem();
        String saved = AppStore.getAnswer(p.no, lang);
        editorArea.setText(saved.isEmpty() ? template(lang, p) : saved);
        editorArea.setCaretPosition(0);
        outputArea.setText("");
        notesArea.setText(AppStore.getNote(p.no)); // 该题笔记
        notesArea.setCaretPosition(0);
        notesTitle.setText("笔记 · 第 " + p.no + " 题");
        // 切换题目后清空撤销历史，避免 Ctrl+Z 撤回其他题目的内容
        editorUndo.discardAllEdits();
        notesUndo.discardAllEdits();

        refreshActionButtons();
    }

    /** 展示当前选中的解法 */
    private void showSolution() {
        if (current == null || current.solutions.isEmpty()) {
            ideaArea.setText("");
            solJavaArea.setText("");
            solPyArea.setText("");
            return;
        }
        int idx = solutionBox.getSelectedIndex();
        if (idx < 0 || idx >= current.solutions.size()) {
            return;
        }
        Solution s = current.solutions.get(idx);
        ideaArea.setText(s.idea);
        ideaArea.setCaretPosition(0);
        solJavaArea.setText(s.java);
        solJavaArea.setCaretPosition(0);
        solPyArea.setText(s.py);
        solPyArea.setCaretPosition(0);
    }

    private void refreshActionButtons() {
        if (current == null) {
            return;
        }
        doneBtn.setText(current.done ? "✓ 已完成" : "标记完成");
        doneBtn.setForeground(current.done ? GREEN : INK);
        favBtn.setText(current.fav ? "★ 已收藏" : "☆ 收藏");
        favBtn.setForeground(current.fav ? ORANGE : INK);
    }

    // ================================================================ 操作

    private void toggleDone() {
        if (current == null) return;
        current.done = !current.done;
        AppStore.setDone(current.no, current.done);
        refreshActionButtons();
        updateStatus();
        if (undoneBox.isSelected()) {
            Problem keep = current;
            applyFilters();
            if (problemList.getSelectedValue() != keep) {
                // 被过滤掉时保持展示内容不变
            }
        } else {
            problemList.repaint();
        }
    }

    private void toggleFav() {
        if (current == null) return;
        current.fav = !current.fav;
        AppStore.setFav(current.no, current.fav);
        refreshActionButtons();
        updateStatus();
        if (favBox.isSelected()) {
            applyFilters();
        } else {
            problemList.repaint();
        }
    }

    private void openUrl() {
        if (current == null || current.url == null || current.url.isEmpty()) return;
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(current.url));
            } else {
                JOptionPane.showMessageDialog(this, "请手动访问：\n" + current.url);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "打开链接失败：" + ex.getMessage());
        }
    }

    /** 语言切换：保存旧语言草稿，载入新语言草稿/模板 */
    private void switchLanguage() {
        if (current == null) {
            return;
        }
        String newLang = (String) langBox.getSelectedItem();
        String saved = AppStore.getAnswer(current.no, newLang);
        editorArea.setText(saved.isEmpty() ? template(newLang, current) : saved);
        editorArea.setCaretPosition(0);
        editorUndo.discardAllEdits(); // 换语言后清空撤销历史
    }

    /** 展开 / 收起笔记侧边栏（收起前先保存） */
    private void toggleNotes() {
        boolean show = notesToggle.isSelected();
        notesPanel.setVisible(show);
        if (!show) {
            flushNoteSave();
        } else if (current != null) {
            notesArea.requestFocusInWindow();
        }
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void scheduleNoteSave() {
        if (noteSaveTimer != null) {
            noteSaveTimer.start(); // 重启防抖计时
        }
    }

    private void flushNoteSave() {
        if (noteSaveTimer != null && noteSaveTimer.isRunning()) {
            noteSaveTimer.stop();
        }
        if (current != null) {
            AppStore.saveNote(current.no, notesArea.getText());
        }
    }

    private void saveCurrentAnswer() {
        if (current != null && editorArea.getText() != null) {
            AppStore.saveAnswer(current.no, (String) langBox.getSelectedItem(),
                    editorArea.getText());
        }
    }

    /** 运行答题区代码（内置编译器） */
    private void runAnswer() {
        if (current == null || running) {
            return;
        }
        saveCurrentAnswer();
        String lang = (String) langBox.getSelectedItem();
        String code = editorArea.getText();
        running = true;
        runBtn.setEnabled(false);
        runBtn.setText("运行中…");
        outputArea.setText("[" + lang + "] 正在"
                + ("Java".equals(lang) ? "编译并运行" : "运行") + "…\n");
        RunService.runAsync(lang, code, result -> {
            running = false;
            runBtn.setEnabled(true);
            runBtn.setText("▶ 运行");
            String text = result.output == null || result.output.isEmpty()
                    ? "（无输出）" : result.output;
            outputArea.setText(text);
            outputArea.setCaretPosition(0);
        });
    }

    /** 载入当前解法代码到答题区（智能合并：保留 Main/main 入口，替换/新增题解类） */
    private void loadSolutionCode() {
        if (current == null || current.solutions.isEmpty()) {
            return;
        }
        int idx = Math.max(0, solutionBox.getSelectedIndex());
        Solution s = current.solutions.get(idx);
        String lang = (String) langBox.getSelectedItem();
        String code = "Java".equals(lang) ? s.java : s.py;
        String merged = "Java".equals(lang)
                ? ensureJavaImports(mergeJavaSolution(editorArea.getText(), code))
                : ensurePythonImports(mergePythonSolution(editorArea.getText(), code));
        editorArea.setText(merged);
        editorArea.setCaretPosition(0);
        AppStore.saveAnswer(current.no, lang, merged);
    }

    /** 恢复代码模板（按题目定制：调用本题方法的示例代码） */
    private void resetTemplate() {
        String tpl = template((String) langBox.getSelectedItem(), current);
        if (editorArea.getText().equals(tpl)) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确定恢复为初始模板？当前内容将被清除。",
                "恢复模板", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        editorArea.setText(tpl);
        editorArea.setCaretPosition(0);
        editorUndo.discardAllEdits(); // 恢复模板后清空撤销历史
        AppStore.saveAnswer(current == null ? -1 : current.no,
                (String) langBox.getSelectedItem(), editorArea.getText());
    }

    /** 按题目定制的答题模板：Main 内为示例调用；附本题 Solution 骨架（方法签名 + TODO）供训练 */
    private static String template(String lang, Problem p) {
        loadSamplesIfNeeded();
        if ("Java".equals(lang)) {
            String body = sampleBody(p, "java",
                    "// TODO：编写你的测试代码，然后点击「运行」或 Ctrl+Enter\n"
                  + "// 题目方法签名请参考「参考题解」\n"
                  + "Solution s = new Solution();");
            String stub = solutionStubJava(p);
            return "import java.util.*;\n\npublic class Main {\n"
                 + "    public static void main(String[] args) {\n"
                 + indent(body, 8) + "\n    }\n}"
                 + (stub.isEmpty() ? "" : "\n\n" + stub);
        }
        String body = sampleBody(p, "py",
                "# TODO：编写你的测试代码，然后点击「运行」或 Ctrl+Enter\ns = Solution()");
        String stub = solutionStubPython(p);
        return "from typing import List, Optional\nfrom collections import deque\n\n"
             + (stub.isEmpty() ? "" : stub + "\n\n")
             + "def main():\n" + indent(body, 4)
             + "\n\nif __name__ == \"__main__\":\n    main()";
    }

    /** 由本题第一个参考题解生成 Java 类骨架（方法签名 + TODO 抛错；字段/嵌套类保留） */
    private static String solutionStubJava(Problem p) {
        if (p == null || p.solutions.isEmpty()) {
            return "";
        }
        try {
            return stubJavaFromSolution(p.solutions.get(0).java);
        } catch (Exception ex) {
            return "";
        }
    }

    /** 由参考题解生成 Python 类骨架 */
    private static String solutionStubPython(Problem p) {
        if (p == null || p.solutions.isEmpty()) {
            return "";
        }
        try {
            return stubPythonFromSolution(p.solutions.get(0).py);
        } catch (Exception ex) {
            return "";
        }
    }

    private static String sampleBody(Problem p, String lang, String fallback) {
        if (p != null) {
            String[] pair = SAMPLES.get(p.no);
            if (pair != null) {
                String s = "java".equals(lang) ? pair[0] : pair[1];
                if (s != null && !s.isBlank()) {
                    return s;
                }
            }
        }
        return fallback;
    }

    /** 由参考题解生成 Java 类骨架：方法签名保留、方法体替换为 TODO 抛错；字段/嵌套类原样保留 */
    public static String stubJavaFromSolution(String solutionCode) {
        List<String[]> classes = topLevelClasses(solutionCode);
        if (classes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String[] c : classes) {
            sb.append(stubJavaClass(c[1])).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /** 将类中的方法体替换为 TODO 抛错（字段/嵌套类/静态块原样保留） */
    private static String stubJavaClass(String block) {
        int brace = block.indexOf('{');
        if (brace < 0) {
            return block;
        }
        StringBuilder out = new StringBuilder(block.substring(0, brace + 1)).append('\n');
        String body = block.substring(brace + 1, block.length() - 1); // 去掉最外层 { }
        int i = 0;
        while (i < body.length()) {
            while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
                i++;
            }
            if (i >= body.length()) {
                break;
            }
            int start = i;
            int j = i, depth = 0;
            boolean inStr = false, inChar = false, lineC = false, blockC = false;
            while (j < body.length()) {
                char c = body.charAt(j);
                char next = j + 1 < body.length() ? body.charAt(j + 1) : '\0';
                if (lineC) {
                    if (c == '\n') lineC = false;
                    j++;
                    continue;
                }
                if (blockC) {
                    if (c == '*' && next == '/') { blockC = false; j += 2; continue; }
                    j++;
                    continue;
                }
                if (inStr) {
                    if (c == '\\') { j += 2; continue; }
                    if (c == '"') inStr = false;
                    j++;
                    continue;
                }
                if (inChar) {
                    if (c == '\\') { j += 2; continue; }
                    if (c == '\'') inChar = false;
                    j++;
                    continue;
                }
                if (c == '/' && next == '/') { lineC = true; j += 2; continue; }
                if (c == '/' && next == '*') { blockC = true; j += 2; continue; }
                if (c == '"') { inStr = true; j++; continue; }
                if (c == '\'') { inChar = true; j++; continue; }
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { j++; break; }
                }
                else if (c == ';' && depth == 0) { j++; break; } // 字段结束
                j++;
            }
            String member = body.substring(start, Math.min(j, body.length()));
            String trimmed = member.trim();
            boolean nested = java.util.regex.Pattern
                    .compile("^(@\\w+\\s+)*(public |private |protected |static |final |abstract )*(class|interface|enum)\\b")
                    .matcher(trimmed).find();
            boolean isMethod = !nested && trimmed.contains("(") && trimmed.endsWith("}");
            if (isMethod) {
                int p0 = trimmed.indexOf('(');
                int pEnd = matchParen(trimmed, p0);
                if (p0 >= 0 && pEnd > p0) {
                    out.append("    ").append(trimmed, 0, pEnd + 1)
                       .append(" {\n        // TODO: 在此实现你的解法\n")
                       .append("        throw new UnsupportedOperationException(\"TODO\");\n")
                       .append("    }\n");
                    i = j;
                    continue;
                }
            }
            // 字段 / 嵌套类 / 静态块：统一缩进 4 空格（数据源代码顶格书写）
            for (String line : member.split("\n", -1)) {
                out.append(line.isBlank() ? "" : "    " + line).append('\n');
            }
            i = j;
        }
        out.append('}');
        return out.toString();
    }

    /** 圆括号配对（忽略字符串）：返回与 start 处 '(' 匹配的 ')' 下标 */
    private static int matchParen(String s, int start) {
        int depth = 0;
        boolean inStr = false, inChar = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inStr = false;
                continue;
            }
            if (inChar) {
                if (c == '\\') { i++; continue; }
                if (c == '\'') inChar = false;
                continue;
            }
            if (c == '"') inStr = true;
            else if (c == '\'') inChar = true;
            else if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /** 由参考题解生成 Python 类骨架：保留方法签名，方法体替换为 raise NotImplementedError */
    public static String stubPythonFromSolution(String solutionCode) {
        List<String[]> classes = pythonClasses(solutionCode);
        if (classes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String[] c : classes) {
            sb.append(stubPythonClass(c[1])).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /** 提取 Python 顶层类块：class 行起，至下一个零缩进非空行前 */
    private static List<String[]> pythonClasses(String code) {
        List<String[]> res = new ArrayList<>();
        String[] lines = code.split("\n", -1);
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (line.startsWith("class ") && line.contains(":")) {
                int start = i;
                int colon = line.indexOf(':');
                String name = line.substring(6, colon).trim();
                i++;
                while (i < lines.length && (lines[i].isBlank()
                        || lines[i].startsWith(" ") || lines[i].startsWith("\t"))) {
                    i++;
                }
                res.add(new String[]{name,
                        String.join("\n", java.util.Arrays.copyOfRange(lines, start, i))});
            } else {
                i++;
            }
        }
        return res;
    }

    /** Python 类骨架：方法签名保留，方法体替换为 raise NotImplementedError */
    private static String stubPythonClass(String block) {
        String[] lines = block.split("\n", -1);
        StringBuilder out = new StringBuilder(lines[0]).append('\n');
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.strip().startsWith("def ")) {
                out.append("    ").append(line.strip()).append('\n');
                out.append("        raise NotImplementedError(\"TODO\")\n");
                while (i + 1 < lines.length) { // 跳过旧方法体（缩进大于方法层的行）
                    String nx = lines[i + 1];
                    if (nx.isBlank() || nx.startsWith(" ") || nx.startsWith("\t")) {
                        i++;
                        continue;
                    }
                    break;
                }
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString().stripTrailing();
    }

    private static int leadingSpaces(String s) {
        int n = 0;
        while (n < s.length() && s.charAt(n) == ' ') {
            n++;
        }
        return n;
    }

    private static String indent(String s, int spaces) {
        String pad = " ".repeat(spaces);
        StringBuilder sb = new StringBuilder();
        for (String line : s.split("\n")) {
            sb.append(line.isBlank() ? "" : pad + line).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private void updateStatus() {
        int total = allProblems.size();
        int done = 0, easy = 0, med = 0, hard = 0, fav = 0;
        for (Problem p : allProblems) {
            if (p.done) {
                done++;
                if ("简单".equals(p.diff)) easy++;
                else if ("中等".equals(p.diff)) med++;
                else hard++;
            }
            if (p.fav) fav++;
        }
        statusLabel.setText(String.format(
                "已完成 %d / %d （简单 %d · 中等 %d · 困难 %d）   收藏 %d   %d 种参考解法/题",
                done, total, easy, med, hard, fav,
                allProblems.isEmpty() ? 0 : allProblems.get(0).solutions.size()));
        progressBar.setValue(total == 0 ? 0 : (int) Math.round(done * 100.0 / total));
    }

    /** IDEA 风格快速输入模板（Tab 展开）：语言 → 缩写 → 代码模板（| 为展开后光标位置） */
    private static final Map<String, Map<String, String>> LIVE_TEMPLATES = Map.of(
            "Java", Map.ofEntries(
                    Map.entry("sout", "System.out.println(|);"),
                    Map.entry("souf", "System.out.printf(|);"),
                    Map.entry("serr", "System.err.println(|);"),
                    Map.entry("psvm", "public static void main(String[] args) {\n        |\n    }"),
                    Map.entry("main", "public static void main(String[] args) {\n        |\n    }"),
                    Map.entry("fori", "for (int i = 0; i < |; i++) {\n            \n        }"),
                    Map.entry("for", "for (var item : |collection) {\n            \n        }"),
                    Map.entry("iter", "for (var item : |collection) {\n            \n        }"),
                    Map.entry("foreach", "for (var item : |collection) {\n            \n        }"),
                    Map.entry("ifn", "if (| == null) {\n            \n        }"),
                    Map.entry("inn", "if (| != null) {\n            \n        }"),
                    Map.entry("tryc", "try {\n            |\n        } catch (Exception e) {\n"
                            + "            e.printStackTrace();\n        }"),
                    Map.entry("thr", "throw new |;"),
                    Map.entry("psf", "public static final |"),
                    Map.entry("psfi", "public static final int |"),
                    Map.entry("psfs", "public static final String |"),
                    Map.entry("alist", "List<|> list = new ArrayList<>();"),
                    Map.entry("hmap", "Map<|, |> map = new HashMap<>();")),
            "Python", Map.ofEntries(
                    Map.entry("sout", "print(|)"),
                    Map.entry("main", "def main():\n    |\n\nif __name__ == \"__main__\":\n    main()"),
                    Map.entry("fori", "for i in range(|):"),
                    Map.entry("iter", "for | in items:"),
                    Map.entry("tryc", "try:\n    |\nexcept Exception as e:\n    print(e)")));

    /** Enter 智能行为：后缀补全（100.sout）→ 词模板（sout/psvm/fori）→ 默认换行+自动缩进 */
    private boolean smartEnter() {
        try {
            editorUndo.breakGroup(); // 展开独立成撤销步
            int caret = editorArea.getCaretPosition();
            String text = editorArea.getDocument().getText(0, caret);
            // 1) 后缀式：expr.sout / expr.nn / expr.var ...
            for (String[] pf : POSTFIX_TEMPLATES) {
                if (text.endsWith(pf[0])) {
                    int dot = text.length() - pf[0].length();
                    String expr = extractExpr(text, dot);
                    if (expr.isEmpty()) {
                        break;
                    }
                    int marker = pf[1].indexOf('|');
                    String expansion = pf[1].replace("%E", expr).replace("|", "");
                    editorArea.getDocument().remove(dot, pf[0].length());
                    editorArea.getDocument().insertString(dot, expansion, null);
                    if (marker >= 0) {
                        editorArea.setCaretPosition(dot + marker);
                    }
                    return true;
                }
            }
            // 2) 词模板：sout / psvm / fori ...
            int start = caret;
            while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) {
                start--;
            }
            String word = text.substring(start, caret);
            String lang = (String) langBox.getSelectedItem();
            String tpl = LIVE_TEMPLATES.getOrDefault(lang,
                    java.util.Collections.emptyMap()).get(word);
            if (tpl != null) {
                int marker = tpl.indexOf('|');
                String expansion = tpl.replace("|", "");
                editorArea.getDocument().remove(start, caret - start);
                editorArea.getDocument().insertString(start, expansion, null);
                editorArea.setCaretPosition(start + Math.max(0, marker));
                return true;
            }
        } catch (Exception ignored) {
            // 退回默认换行行为
        }
        return false;
    }

    /** 换行 + 自动缩进：继承上一行行首空白；行尾是 { 或 : 时自动加一层缩进 */
    private void newlineAndIndent(JTextArea area) {
        try {
            editorUndo.breakGroup(); // 换行独立成撤销步
            int caret = area.getCaretPosition();
            String text = area.getDocument().getText(0, caret);
            int ls = text.lastIndexOf('\n', Math.max(0, caret - 1)) + 1;
            String line = text.substring(ls, caret);
            StringBuilder pad = new StringBuilder("\n");
            for (char c : line.toCharArray()) {
                if (c == ' ') pad.append(' ');
                else if (c == '\t') pad.append("    ");
                else break;
            }
            String trimmed = line.trim();
            if (trimmed.endsWith("{") || trimmed.endsWith(":")) {
                pad.append("    ");
            }
            area.getDocument().insertString(caret, pad.toString(), null);
        } catch (Exception ignored) {
        }
    }

    /** 选中多行 → 整体缩进一层；无选中 → 光标处插 4 空格（单次撤销步） */
    private void indentSelection(JTextArea area) {
        try {
            editorUndo.breakGroup(); // 缩进独立成撤销步
            javax.swing.text.Document doc = area.getDocument();
            int s0 = area.getSelectionStart(), s1 = area.getSelectionEnd();
            int l0 = area.getLineOfOffset(s0);
            int l1 = area.getLineOfOffset(s1);
            if (l1 > l0 && s1 == area.getLineStartOffset(l1)) {
                l1--; // 选区止于行首时不缩进该行
            }
            if (l1 == l0) { // 单行：直接插入
                area.replaceSelection("    ");
                return;
            }
            int start = area.getLineStartOffset(l0);
            int end = area.getLineEndOffset(l1);
            String block = doc.getText(start, end - start);
            StringBuilder nb = new StringBuilder();
            for (String line : block.split("\n", -1)) {
                nb.append(line.isBlank() ? line : "    " + line).append('\n');
            }
            ((javax.swing.text.AbstractDocument) doc).replace(start, end - start,
                    nb.toString(), null); // 原子替换 = 一次撤销步
        } catch (Exception ignored) {
        }
    }

    /** 选中多行 → 整体反缩进一层；无选中 → 当前行反缩进（单次撤销步） */
    private void dedentSelection(JTextArea area) {
        try {
            editorUndo.breakGroup(); // 反缩进独立成撤销步
            javax.swing.text.Document doc = area.getDocument();
            int s0 = area.getSelectionStart(), s1 = area.getSelectionEnd();
            int l0 = area.getLineOfOffset(s0);
            int l1 = area.getLineOfOffset(s1);
            if (l1 > l0 && s1 == area.getLineStartOffset(l1)) {
                l1--;
            }
            if (l1 == l0) {
                l1 = l0; // 当前行
            }
            int start = area.getLineStartOffset(l0);
            int end = area.getLineEndOffset(l1);
            String block = doc.getText(start, end - start);
            StringBuilder nb = new StringBuilder();
            boolean changed = false;
            for (String line : block.split("\n", -1)) {
                if (line.startsWith("    ")) {
                    nb.append(line.substring(4)).append('\n');
                    changed = true;
                } else if (line.startsWith("\t")) {
                    nb.append(line.substring(1)).append('\n');
                    changed = true;
                } else {
                    nb.append(line).append('\n');
                }
            }
            if (changed) {
                ((javax.swing.text.AbstractDocument) doc).replace(start, end - start,
                        nb.toString(), null);
            }
        } catch (Exception ignored) {
        }
    }

    /** 后缀补全规则（Enter 触发）：%E 为捕获的表达式，| 为展开后光标位置 */
    private static final String[][] POSTFIX_TEMPLATES = {
            {".sout", "System.out.println(%E);|"},
            {".serr", "System.err.println(%E);|"},
            {".souf", "System.out.printf(\"%s%n\", %E);|"},
            {".fori", "for (int i = 0; i < %E; i++) {\n    \n}|"},
            {".iter", "for (var item : %E) {\n    \n}|"},
            {".nn", "if (%E != null) {\n    \n}|"},
            {".var", "var | = %E;"},
            {".return", "return %E;"},
            {".sysout", "System.out.println(%E);|"}
    };

    /** 从 endExclusive 前回退提取表达式：支持引号字符串与下标/链式调用（遇语句分隔符停止） */
    private static String extractExpr(String s, int endExclusive) {
        int i = endExclusive;
        while (i > 0) {
            char c = s.charAt(i - 1);
            if (c == '"') { // 字符串字面量：回退到配对的开引号
                i--;
                while (i > 0 && s.charAt(i - 1) != '"') {
                    i--;
                }
                if (i > 0) {
                    i--;
                }
                continue;
            }
            if (c == ' ' || c == '\t' || c == '\n' || c == ';' || c == '{'
                    || c == '(' || c == ',' || c == '=') {
                break;
            }
            i--;
        }
        return s.substring(i, endExclusive);
    }


    /** 每题定制示例（data/samples*.txt）：no → {Java 主体, Python 主体} */
    private static final Map<Integer, String[]> SAMPLES = new LinkedHashMap<>();
    private static boolean samplesLoaded;

    private static void loadSamplesIfNeeded() {
        if (samplesLoaded) {
            return;
        }
        samplesLoaded = true;
        File dir = resolveDataDir();
        File[] files = dir.listFiles((d, n) -> n.startsWith("samples") && n.endsWith(".txt"));
        if (files == null) {
            return;
        }
        java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        for (File f : files) {
            try {
                List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                int no = -1;
                String type = null;
                StringBuilder buf = new StringBuilder();
                for (String line : lines) {
                    if (line.startsWith("@@")) {
                        flushSample(no, type, buf);
                        String body = line.substring(2).trim();
                        int c = body.indexOf(':');
                        String key = c > 0 ? body.substring(0, c).trim() : "";
                        String val = c > 0 ? body.substring(c + 1).trim() : "";
                        if (key.equals("SAMPLE-JAVA") || key.equals("SAMPLE-PY")) {
                            no = Integer.parseInt(val);
                            type = key.endsWith("JAVA") ? "java" : "py";
                            buf = new StringBuilder();
                        } else {
                            no = -1;
                            type = null;
                            buf = new StringBuilder();
                        }
                        continue;
                    }
                    if (no > 0) {
                        buf.append(line).append('\n');
                    }
                }
                flushSample(no, type, buf);
            } catch (Exception ex) {
                System.err.println("示例加载失败: " + f.getName() + " " + ex.getMessage());
            }
        }
    }

    private static void flushSample(int no, String type, StringBuilder buf) {
        if (no <= 0 || type == null) {
            return;
        }
        String content = buf.toString().strip();
        if (content.isEmpty()) {
            return;
        }
        String[] pair = SAMPLES.computeIfAbsent(no, k -> new String[2]);
        if (type.equals("java")) {
            pair[0] = content;
        } else {
            pair[1] = content;
        }
    }

    private void shutdown() {
        saveCurrentAnswer();
        if (noteSaveTimer != null) {
            noteSaveTimer.stop();
        }
        flushNoteSave();
        dispose();
        System.exit(0);
    }

    // ================================================================ 题解合并（载入不覆盖 main）

    /** Java 合并：保留编辑器里的 Main 等入口结构；同名题解类被替换，新类追加到末尾 */
    public static String mergeJavaSolution(String editor, String solution) {
        if (editor == null || editor.trim().isEmpty()) {
            return solution;
        }
        List<String[]> solClasses = topLevelClasses(solution);
        if (solClasses.isEmpty()) {
            return trimEnd(editor) + "\n\n" + solution;
        }
        String merged = editor;
        StringBuilder extra = new StringBuilder();
        for (String[] sc : solClasses) {
            int[] span = classSpan(merged, sc[0]);
            if (span != null) {
                merged = merged.substring(0, span[0]) + sc[1] + merged.substring(span[1]);
            } else {
                extra.append("\n\n").append(sc[1]);
            }
        }
        if (extra.length() > 0) {
            merged = trimEnd(merged) + extra;
        }
        return merged;
    }

    /** Python 合并：题解插入到 if __name__ 守卫之前，main 入口保留在末尾 */
    public static String mergePythonSolution(String editor, String solution) {
        if (editor == null || editor.trim().isEmpty()) {
            return solution;
        }
        int guard = editor.indexOf("if __name__");
        if (guard < 0) {
            return trimEnd(editor) + "\n\n" + solution;
        }
        return trimEnd(editor.substring(0, guard)) + "\n\n" + solution + "\n\n"
                + editor.substring(guard);
    }

    /** 提取顶层类/接口/枚举：{类名, 完整代码块}（花括号配对，自动跳过嵌套类） */
    private static List<String[]> topLevelClasses(String code) {
        List<String[]> res = new ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern
                .compile("\\b(class|interface|enum)\\s+(\\w+)");
        int i = 0;
        while (true) {
            java.util.regex.Matcher m = p.matcher(code);
            if (!m.find(i) || depthAt(code, m.start()) != 0) {
                break;
            }
            int brace = code.indexOf('{', m.end());
            int end = (brace < 0) ? -1 : matchBrace(code, brace);
            if (end < 0) {
                break;
            }
            res.add(new String[]{m.group(2), code.substring(m.start(), end + 1)});
            i = end + 1;
        }
        return res;
    }

    /** 在 text 中定位名为 name 的类块，返回 {起, 止} 下标；不存在返回 null */
    private static int[] classSpan(String text, String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b(class|interface|enum)\\s+" + name + "\\b").matcher(text);
        while (m.find()) {
            int brace = text.indexOf('{', m.end());
            if (brace < 0) {
                break;
            }
            int end = matchBrace(text, brace);
            if (end > 0) {
                return new int[]{m.start(), end + 1};
            }
        }
        return null;
    }

    /** 忽略字符串与注释的花括号配对：返回与 start 处 '{' 匹配的 '}' 下标 */
    private static int matchBrace(String s, int start) {
        int depth = 0;
        boolean inStr = false, inChar = false, lineComment = false, blockComment = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            char next = i + 1 < s.length() ? s.charAt(i + 1) : '\0';
            if (lineComment) {
                if (c == '\n') lineComment = false;
                continue;
            }
            if (blockComment) {
                if (c == '*' && next == '/') { blockComment = false; i++; }
                continue;
            }
            if (inStr) {
                if (c == '\\') i++;
                else if (c == '"') inStr = false;
                continue;
            }
            if (inChar) {
                if (c == '\\') i++;
                else if (c == '\'') inChar = false;
                continue;
            }
            if (c == '/' && next == '/') { lineComment = true; i++; continue; }
            if (c == '/' && next == '*') { blockComment = true; i++; continue; }
            if (c == '"') { inStr = true; continue; }
            if (c == '\'') { inChar = true; continue; }
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /** idx 位置处的花括号嵌套深度（忽略字符串/注释），顶层为 0 */
    private static int depthAt(String s, int idx) {
        int depth = 0;
        boolean inStr = false, inChar = false, lineComment = false, blockComment = false;
        for (int i = 0; i < idx; i++) {
            char c = s.charAt(i);
            char next = i + 1 < s.length() ? s.charAt(i + 1) : '\0';
            if (lineComment) {
                if (c == '\n') lineComment = false;
                continue;
            }
            if (blockComment) {
                if (c == '*' && next == '/') { blockComment = false; i++; }
                continue;
            }
            if (inStr) {
                if (c == '\\') i++;
                else if (c == '"') inStr = false;
                continue;
            }
            if (inChar) {
                if (c == '\\') i++;
                else if (c == '\'') inChar = false;
                continue;
            }
            if (c == '/' && next == '/') { lineComment = true; i++; continue; }
            if (c == '/' && next == '*') { blockComment = true; i++; continue; }
            if (c == '"') { inStr = true; continue; }
            if (c == '\'') { inChar = true; continue; }
            if (c == '{') depth++;
            else if (c == '}') depth--;
        }
        return depth;
    }

    private static String trimEnd(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    /** 常用简单名 → 全限定名（IDEA 自动 import 的知识库） */
    private static final Map<String, String> KNOWN_IMPORTS = Map.ofEntries(
            Map.entry("Map", "java.util.Map"),
            Map.entry("HashMap", "java.util.HashMap"),
            Map.entry("LinkedHashMap", "java.util.LinkedHashMap"),
            Map.entry("TreeMap", "java.util.TreeMap"),
            Map.entry("Hashtable", "java.util.Hashtable"),
            Map.entry("List", "java.util.List"),
            Map.entry("ArrayList", "java.util.ArrayList"),
            Map.entry("LinkedList", "java.util.LinkedList"),
            Map.entry("Deque", "java.util.Deque"),
            Map.entry("ArrayDeque", "java.util.ArrayDeque"),
            Map.entry("Queue", "java.util.Queue"),
            Map.entry("PriorityQueue", "java.util.PriorityQueue"),
            Map.entry("Set", "java.util.Set"),
            Map.entry("HashSet", "java.util.HashSet"),
            Map.entry("LinkedHashSet", "java.util.LinkedHashSet"),
            Map.entry("TreeSet", "java.util.TreeSet"),
            Map.entry("SortedMap", "java.util.SortedMap"),
            Map.entry("SortedSet", "java.util.SortedSet"),
            Map.entry("Iterator", "java.util.Iterator"),
            Map.entry("ListIterator", "java.util.ListIterator"),
            Map.entry("Collections", "java.util.Collections"),
            Map.entry("Arrays", "java.util.Arrays"),
            Map.entry("Objects", "java.util.Objects"),
            Map.entry("Comparator", "java.util.Comparator"),
            Map.entry("Optional", "java.util.Optional"),
            Map.entry("OptionalInt", "java.util.OptionalInt"),
            Map.entry("OptionalLong", "java.util.OptionalLong"),
            Map.entry("OptionalDouble", "java.util.OptionalDouble"),
            Map.entry("StringJoiner", "java.util.StringJoiner"),
            Map.entry("StringTokenizer", "java.util.StringTokenizer"),
            Map.entry("Random", "java.util.Random"),
            Map.entry("Scanner", "java.util.Scanner"),
            Map.entry("Stack", "java.util.Stack"),
            Map.entry("Vector", "java.util.Vector"),
            Map.entry("NoSuchElementException", "java.util.NoSuchElementException"),
            Map.entry("Function", "java.util.function.Function"),
            Map.entry("BiFunction", "java.util.function.BiFunction"),
            Map.entry("BiConsumer", "java.util.function.BiConsumer"),
            Map.entry("BiPredicate", "java.util.function.BiPredicate"),
            Map.entry("Consumer", "java.util.function.Consumer"),
            Map.entry("Supplier", "java.util.function.Supplier"),
            Map.entry("UnaryOperator", "java.util.function.UnaryOperator"),
            Map.entry("BinaryOperator", "java.util.function.BinaryOperator"),
            Map.entry("IntFunction", "java.util.function.IntFunction"),
            Map.entry("ToIntFunction", "java.util.function.ToIntFunction"),
            Map.entry("Stream", "java.util.stream.Stream"),
            Map.entry("IntStream", "java.util.stream.IntStream"),
            Map.entry("LongStream", "java.util.stream.LongStream"),
            Map.entry("DoubleStream", "java.util.stream.DoubleStream"),
            Map.entry("Collectors", "java.util.stream.Collectors"),
            Map.entry("File", "java.io.File"),
            Map.entry("IOException", "java.io.IOException"),
            Map.entry("BufferedReader", "java.io.BufferedReader"),
            Map.entry("BufferedWriter", "java.io.BufferedWriter"),
            Map.entry("InputStreamReader", "java.io.InputStreamReader"),
            Map.entry("OutputStreamWriter", "java.io.OutputStreamWriter"),
            Map.entry("PrintWriter", "java.io.PrintWriter"),
            Map.entry("BigInteger", "java.math.BigInteger"),
            Map.entry("BigDecimal", "java.math.BigDecimal"),
            Map.entry("SimpleDateFormat", "java.text.SimpleDateFormat"),
            Map.entry("DecimalFormat", "java.text.DecimalFormat"),
            Map.entry("Pattern", "java.util.regex.Pattern"),
            Map.entry("Matcher", "java.util.regex.Matcher"));

    /** IDEA 风格 import 补全：按代码中出现的简单名补缺失 import（已有通配符导入的包跳过） */
    public static String ensureJavaImports(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        Map<String, List<String>> pkgGroups = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : KNOWN_IMPORTS.entrySet()) {
            String simple = e.getKey();
            if (!code.matches("(?s).*\\b" + simple + "\\b.*")) {
                continue;
            }
            String fq = e.getValue();
            if (code.contains("import " + fq + ";")) {
                continue; // 已显式导入
            }
            String pkg = fq.substring(0, fq.lastIndexOf('.'));
            boolean coveredByStar = code.contains("import " + pkg + ".*;");
            if (!coveredByStar) {
                pkgGroups.computeIfAbsent(pkg, k -> new ArrayList<>())
                        .add("import " + fq + ";");
            }
        }
        if (pkgGroups.isEmpty()) {
            return code;
        }
        StringBuilder ins = new StringBuilder();
        pkgGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(g -> {
                    java.util.Collections.sort(g.getValue());
                    g.getValue().forEach(line -> ins.append(line).append('\n'));
                    ins.append('\n'); // 包之间空行分组
                });
        if (code.startsWith("package ")) {
            int nl = code.indexOf('\n');
            if (nl > 0) {
                return code.substring(0, nl + 1) + ins + code.substring(nl + 1);
            }
        }
        return ins + code;
    }

    /** Python：补全 typing / collections / 常用模块导入（IDEA 风格显式导入） */
    public static String ensurePythonImports(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        StringBuilder add = new StringBuilder();
        if (!code.contains("from typing import")) {
            add.append("from typing import *\n");
        }
        if (!code.contains("from collections import")) {
            add.append("from collections import *\n");
        }
        if (!code.contains("import heapq")) {
            add.append("import heapq, itertools, functools, math, re, bisect\n");
        }
        if (add.length() == 0) {
            return code;
        }
        return add + code;
    }

    // ================================================================ 工具方法

    private static JTextArea codeArea(Font font, boolean wrap, Color bg, Color fg) {
        JTextArea area = new JTextArea();
        area.setFont(font);
        area.setEditable(false);
        area.setLineWrap(wrap);
        area.setWrapStyleWord(wrap);
        area.setBackground(bg);
        area.setForeground(fg);
        area.setCaretColor(fg);
        area.setTabSize(4);
        area.setBorder(new EmptyBorder(4, 4, 4, 4));
        return area;
    }

    /** 圆角卡片边框（WorkBuddy 盒子风格：1px 描边 + 12px 圆角） */
    private static javax.swing.border.Border roundedBorder(Color color) {
        return new javax.swing.border.AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.drawRoundRect(x, y, w - 1, h - 1, 12, 12);
                g2.dispose();
            }
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(1, 1, 1, 1);
            }
            @Override
            public boolean isBorderOpaque() {
                return true;
            }
            private static final long serialVersionUID = 1L;
        };
    }

    private static JScrollPane scrollOf(Component c, EmptyBorder margin) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.getViewport().setBackground(c.getBackground());
        if (c instanceof JTextArea) {
            Insets ins = margin.getBorderInsets(c);
            ((JTextArea) c).setMargin(new Insets(ins.top + 8, ins.left + 8,
                    ins.bottom + 8, ins.right + 8));
        }
        return sp;
    }

    private static javax.swing.Icon createSearchIcon() {
        return new javax.swing.Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(INK_SOFT);
                g2.setStroke(new java.awt.BasicStroke(1.6f));
                g2.drawOval(x + 2, y + 2, 8, 8);
                g2.drawLine(x + 10, y + 10, x + 14, y + 14);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        };
    }

    /** 原创 Logo：绿色渐变圆角 + 玻璃高光 + 代码符「</>」（契合刷题主题与品牌配色） */
    private static Image drawBrandLogo(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int r = size / 4;
        // 对角绿色渐变底
        g.setPaint(new GradientPaint(0, 0, GRAD_A, size, size, GRAD_B));
        g.fillRoundRect(0, 0, size, size, r * 2, r * 2);
        // 顶部玻璃高光
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 85),
                0, size / 2, new Color(255, 255, 255, 0)));
        g.fillRoundRect(0, 0, size, size / 2, r * 2, r * 2);
        // 中心代码符「</>」（带柔和投影）
        g.setFont(new Font("Consolas", Font.BOLD, Math.round(size * 0.42f)));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String text = "</>";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(new Color(0, 0, 0, 55));
        g.drawString(text, x, y + Math.max(1, size / 64));
        g.setColor(Color.WHITE);
        g.drawString(text, x, y);
        // 细内描边（徽章感）
        g.setColor(new Color(255, 255, 255, 60));
        g.drawRoundRect(0, 0, size - 1, size - 1, r * 2 - 1, r * 2 - 1);
        g.dispose();
        return img;
    }

    private static Image createAppIcon() {
        return drawBrandLogo(256);
    }

    /** 定位数据目录：优先工作目录，其次 jar 所在目录（适配 exe 打包） */
    static File resolveDataDir() {
        File cwd = new File("data").getAbsoluteFile();
        if (cwd.isDirectory()) {
            return cwd;
        }
        try {
            File codeSource = new File(Hot100App.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File dir = codeSource.getParentFile();
            for (int i = 0; i < 3 && dir != null; i++) {
                File candidate = new File(dir, "data");
                if (candidate.isDirectory()) {
                    return candidate;
                }
                dir = dir.getParentFile();
            }
        } catch (Exception ignored) {
            // 使用默认路径给出报错提示
        }
        return cwd;
    }

    // ================================================================ 列表渲染器

    /** 列表行：分类彩点 + 状态(✓/★) + 标题 + 难度，WorkBuddy 分类徽标风格 */
    private static final class ProblemRenderer extends JPanel implements ListCellRenderer<Problem> {
        private static final long serialVersionUID = 1L;
        private final JLabel dotLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel diffLabel = new JLabel();

        ProblemRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(7, 12, 7, 18)); // 右侧留出与滚动条的安全距离
            setOpaque(true);
            dotLabel.setPreferredSize(new Dimension(8, 8));
            dotLabel.setOpaque(false);
            statusLabel.setFont(FONT_BOLD);
            statusLabel.setPreferredSize(new Dimension(26, 18));
            statusLabel.setOpaque(false);
            titleLabel.setFont(FONT_UI);
            diffLabel.setFont(FONT_BOLD);
            diffLabel.setOpaque(false);
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            left.setOpaque(false);
            left.add(dotLabel);
            left.add(statusLabel);
            add(left, BorderLayout.WEST);
            add(titleLabel, BorderLayout.CENTER);
            add(diffLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Problem> list, Problem p,
                                                      int index, boolean selected, boolean focus) {
            dotLabel.setIcon(dot(categoryColor(p.category)));         // 分类彩点
            statusLabel.setText(p.done ? "✓" : (p.fav ? "★" : ""));
            statusLabel.setForeground(p.done ? GREEN : (p.fav ? ORANGE : INK_FAINT));
            titleLabel.setText(ellipsis(p.no + ". " + p.title, FONT_UI,
                    Math.max(80, list.getWidth() - 150)));            // 超长标题省略号截断
            diffLabel.setText(p.diff);
            diffLabel.setForeground(diffColor(p.diff));

            Color bg = selected ? list.getSelectionBackground() : list.getBackground();
            Color fg = selected ? list.getSelectionForeground() : INK;
            setBackground(bg);
            titleLabel.setForeground(p.done && !selected ? INK_FAINT : fg);
            return this;
        }
    }

    /** 文本超宽时以省略号截断（配合列表行宽自适应，保证右侧难度完整可见） */
    private static String ellipsis(String text, Font font, int maxWidth) {
        java.awt.FontMetrics fm = new JLabel().getFontMetrics(font);
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    /** 圆角「药丸」徽标（难度 / 状态等），淡底 + 主题色文字 */
    private static final class PillLabel extends JLabel {
        private static final long serialVersionUID = 1L;
        private Color pillColor = ACCENT;

        PillLabel() {
            setOpaque(false);
            setFont(FONT_BOLD);
            setHorizontalAlignment(CENTER);
            setBorder(new EmptyBorder(2, 10, 2, 10));
        }

        void setColor(Color c) {
            this.pillColor = c;
            setForeground(c);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            String text = getText();
            if (text == null || text.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            java.awt.FontMetrics fm = g2.getFontMetrics(getFont());
            int w = fm.stringWidth(text) + 20;
            int h = fm.getHeight();
            g2.setColor(alpha(pillColor, 24));           // 淡底
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(pillColor);                      // 主题色文字
            g2.setFont(getFont());
            g2.drawString(text, 10, fm.getAscent() - 1);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            String text = getText();
            if (text == null || text.isEmpty()) {
                return new Dimension(0, 0);
            }
            java.awt.FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 20, fm.getHeight());
        }
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static Color diffColor(String diff) {
        if ("简单".equals(diff)) {
            return GREEN;
        }
        if ("中等".equals(diff)) {
            return ORANGE;
        }
        return RED;
    }

    /** 分类 → 徽标色（WorkBuddy 分类徽标配色系） */
    private static Color categoryColor(String category) {
        switch (category) {
            case "哈希":         return new Color(0x83, 0x5C, 0xFF);
            case "双指针":       return new Color(0x3F, 0xB8, 0xFF);
            case "滑动窗口":     return new Color(0x2F, 0xCF, 0xE6);
            case "子串":         return new Color(0x00, 0xC2, 0x9A);
            case "普通数组":     return new Color(0xF5, 0x9E, 0x0B);
            case "矩阵":         return new Color(0xE3, 0x79, 0x33);
            case "链表":         return new Color(0x3B, 0x82, 0xF6);
            case "二叉树":       return new Color(0x8D, 0xC1, 0x49);
            case "图论":         return new Color(0x4E, 0xCB, 0xA0);
            case "回溯":         return new Color(0xA0, 0x74, 0xC4);
            case "二分查找":     return new Color(0x51, 0x9A, 0xBA);
            case "栈":           return new Color(0xC9, 0x7B, 0x63);
            case "堆":           return new Color(0xE0, 0x6B, 0x8C);
            case "贪心算法":     return new Color(0xD9, 0x77, 0x06);
            case "动态规划":     return new Color(0x6C, 0x4D, 0xFF);
            case "多维动态规划": return new Color(0x38, 0x70, 0xCD);
            case "技巧":         return new Color(0x9C, 0xA3, 0xAF);
            default:             return new Color(0x6B, 0x72, 0x80);
        }
    }

    private static javax.swing.Icon dot(Color c) {
        return new javax.swing.Icon() {
            @Override public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(x, y, 8, 8);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 8; }
            @Override public int getIconHeight() { return 8; }
        };
    }

    // ================================================================ 启动入口

    public static void main(String[] args) {
        // FlatLaf 现代扁平主题 + WorkBuddy 组件规格（绿色主题）
        try {
            UIManager.put("defaultFont", FONT_UI);
            // 主题强调色（绿色）
            UIManager.put("Component.accentColor", ACCENT);
            UIManager.put("Component.focusColor", new Color(0x00, 0xC2, 0x9A, 46));
            // 文本框/组合框规格：白底、#E0E0E0 常态边框、聚焦绿色描边（--cb-input-active-border）
            UIManager.put("Component.borderColor", new Color(0xE0, 0xE0, 0xE0));
            UIManager.put("Component.disabledBorderColor", new Color(0xE8, 0xE8, 0xE8));
            UIManager.put("Component.arrowType", "chevron");
            // 圆角体系：sm4 / md6 / lg8 / xl16
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ProgressBar.arc", 999);
            UIManager.put("List.selectionArc", 8);
            UIManager.put("TabbedPane.tabHeight", 36);
            // 选项卡规格：绿色下划线选中态、浅绿 hover、白底无灰块（WorkBuddy 页签风格）
            UIManager.put("TabbedPane.underlineColor", ACCENT);
            UIManager.put("TabbedPane.inactiveUnderlineColor", new Color(0, 0, 0, 0));
            UIManager.put("TabbedPane.tabSelectionHeight", 3);
            UIManager.put("TabbedPane.hoverColor", new Color(0x00, 0xC2, 0x9A, 28));
            UIManager.put("TabbedPane.focusColor", new Color(0, 0, 0, 0));
            UIManager.put("TabbedPane.background", Color.WHITE);
            UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
            UIManager.put("TabbedPane.contentSeparatorHeight", 1);
            UIManager.put("TabbedPane.tabsOverlapBorder", true);
            // 滚动条规格（--cb-scrollbar-thumb rgba(121,121,121,.4)，hover rgba(100,100,100,.7)）
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbColor", new Color(121, 121, 121, 102));
            UIManager.put("ScrollBar.hoverThumbColor", new Color(100, 100, 100, 178));
            UIManager.put("ScrollBar.trackColor", Color.WHITE);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("FlatLaf 初始化失败，使用系统默认主题: " + ex.getMessage());
        }

        AppStore.load();
        File dataDir = resolveDataDir();
        List<Problem> problems;
        try {
            problems = ProblemLoader.load(dataDir);
        } catch (Exception ex) {
            error("题目数据加载失败：" + ex.getMessage() + "\n数据目录：" + dataDir);
            return;
        }
        if (problems.isEmpty()) {
            error("未找到题目数据（data 目录）：" + dataDir.getAbsolutePath());
            return;
        }
        final List<Problem> data = problems;
        SwingUtilities.invokeLater(() -> new Hot100App(data).setVisible(true));
    }

    private static void error(String msg) {
        try {
            javax.swing.UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {
            // 忽略主题错误
        }
        JOptionPane.showMessageDialog(null, msg, "启动失败", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}
