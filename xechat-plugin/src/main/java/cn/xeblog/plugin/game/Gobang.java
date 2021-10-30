package cn.xeblog.plugin.game;

import cn.xeblog.plugin.action.GameAction;
import cn.xeblog.plugin.action.MessageAction;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.commons.entity.GobangDTO;
import cn.xeblog.commons.entity.Response;
import cn.xeblog.commons.enums.Action;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * 五子棋
 *
 * @author anlingyi
 * @date 2020/6/5
 */
public class Gobang extends AbstractGame<GobangDTO> {

    private JPanel chessPanel;

    private JLabel tips;

    private JPanel startPanel;

    // 每个格子的边框大小
    public static final int BORDER = 10;
    // 行数
    public static final int ROWS = 15;
    // 列数
    public static final int COLS = 15;
    // 棋子大小，约为格子的3/4
    private static final int CHESS_SIZE = Math.round(BORDER * 0.75f);
    // 棋盘宽度
    private static final int WIDTH = ROWS * BORDER + BORDER;
    // 棋盘高度
    private static final int HEIGHT = ROWS * BORDER + BORDER;
    // 棋子总数
    private static final int CHESS_TOTAL = ROWS * COLS;

    // 已下棋子数据
    private int[][] chessData;
    // 当前已下棋子数
    private int currentChessTotal;
    // 棋子类型，1黑棋 2白棋
    private int type;
    // 游戏是否结束
    private boolean isGameOver;

    private int status;

    private boolean put;

    // 高亮棋子
    Map<String, Boolean> chessHighlight;

    private String selfName;

    private String opponentName;

    private GameMode gameMode;

    private void initValue() {
        chessData = new int[ROWS][COLS];
        currentChessTotal = 0;
        isGameOver = false;
        status = 0;
        put = false;
        initChessHighLight();
    }

    @Getter
    @AllArgsConstructor
    private enum GameMode {
        HUMAN_VS_PC("人类VS电脑"),
        HUMAN_VS_HUMAN("人类VS人类"),
        ONLINE("在线PK");

        private String name;

        public static GameMode getMode(String name) {
            for (GameMode mode : values()) {
                if (mode.name.equals(name)) {
                    return mode;
                }
            }

            return HUMAN_VS_PC;
        }
    }

    @Override
    public void handle(Response<GobangDTO> response) {
        GobangDTO gobangDTO = response.getBody();
        setChess(gobangDTO.getX(), gobangDTO.getY(), gobangDTO.getType());

        checkStatus(opponentName);
        if (isGameOver) {
            return;
        }

        put = false;
        showTips(selfName + "(你)：思考中...");
    }

    private void checkStatus(String username) {
        boolean flag = true;
        switch (status) {
            case 1:
                showTips("游戏结束：" + username + "这个菜鸡赢了！");
                break;
            case 2:
                showTips("游戏结束：平局~ 卧槽？？？");
                break;
            case 0:
                flag = false;
                break;
            default:
                break;
        }

        isGameOver = flag;

        if (isGameOver) {
            JButton restartButton = new JButton("重新开始");
            restartButton.addActionListener(e -> {
                mainPanel.removeAll();
                initStartPanel();
                mainPanel.updateUI();
            });
            mainPanel.add(restartButton, BorderLayout.SOUTH);
        }
    }

    private void initChessPanel() {
        initValue();
        selfName = DataCache.username;
        if (GameAction.getOpponent() == null) {
            switch (gameMode) {
                case HUMAN_VS_PC:
                    opponentName = "人工制杖";
                    break;
                case HUMAN_VS_HUMAN:
                    opponentName = "路人甲";
                    if (type == 2) {
                        type = 1;
                        selfName = opponentName;
                        opponentName = DataCache.username;
                    }
                    break;
            }
        } else {
            opponentName = GameAction.getOpponent();
            gameMode = GameMode.ONLINE;
            if (GameAction.isProactive()) {
                type = 1;
            } else {
                type = 2;
                put = true;
            }
        }

        chessPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                paintChessBoard(g);
            }
        };

        mainPanel.setPreferredSize(new Dimension(WIDTH + 100, HEIGHT + 100));

        tips = new JLabel("", JLabel.CENTER);
        tips.setFont(new Font("", Font.BOLD, 13));
        tips.setForeground(new Color(237, 81, 38));
        tips.setBounds(0, 30, WIDTH + 60, 30);

        // 设置棋盘背景颜色
        chessPanel.setBackground(Color.LIGHT_GRAY);
        chessPanel.setBounds(mainPanel.getWidth() / 2 - WIDTH / 2, tips.getHeight() + 5, WIDTH, HEIGHT);

        mainPanel.add(tips);
        mainPanel.add(chessPanel);

        chessPanel.setEnabled(true);
        chessPanel.setVisible(true);

        chessPanel.addMouseListener(new MouseAdapter() {
            // 监听鼠标点击事件
            @Override
            public void mouseClicked(MouseEvent e) {
                if (put || isGameOver) {
                    return;
                }

                if (putChess(e.getX(), e.getY(), type)) {
                    checkStatus(selfName);

                    if (!isGameOver) {
                        showTips(opponentName + "：思考中...");
                    }

                    switch (gameMode) {
                        case ONLINE:
                            put = true;
                            send(currentRow, currentCol);
                            break;
                        case HUMAN_VS_PC:
                            break;
                        case HUMAN_VS_HUMAN:
                            type = 3 - type;
                            String tempName = selfName;
                            selfName = opponentName;
                            opponentName = tempName;
                            break;
                    }
                }
            }
        });

        String name = type == 1 ? selfName : opponentName;
        showTips(name + "-先下手为强！");
    }

    /**
     * 绘制棋盘
     *
     * @param g
     */
    private void paintChessBoard(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 画横线
        for (int i = 0; i < ROWS; i++) {
            g2.drawLine(BORDER, i * BORDER + BORDER, WIDTH - BORDER, i * BORDER + BORDER);
        }

        // 画纵线
        for (int i = 0; i < COLS; i++) {
            g2.drawLine(i * BORDER + BORDER, BORDER, i * BORDER + BORDER, HEIGHT - BORDER);
        }

        int starSize = BORDER / 3;
        int halfStarSize = starSize / 2;
        g2.fillOval(4 * BORDER - halfStarSize, 4 * BORDER - halfStarSize, starSize, starSize);
        g2.fillOval(12 * BORDER - halfStarSize, 4 * BORDER - halfStarSize, starSize, starSize);
        g2.fillOval(4 * BORDER - halfStarSize, 12 * BORDER - halfStarSize, starSize, starSize);
        g2.fillOval(12 * BORDER - halfStarSize, 12 * BORDER - halfStarSize, starSize, starSize);
        g2.fillOval(8 * BORDER - halfStarSize, 8 * BORDER - halfStarSize, starSize, starSize);

        if (currentChessTotal == 0) {
            return;
        }

        // 画棋子
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                int k = chessData[i][j];
                if (k == 0) {
                    continue;
                }

                if (k == 1) {
                    g2.setColor(Color.BLACK);
                } else if (k == 2) {
                    g2.setColor(Color.WHITE);
                }

                // 计算棋子外矩形左上顶点坐标
                int halfBorder = CHESS_SIZE / 2;
                int chessX = i * BORDER + BORDER - halfBorder;
                int chessY = j * BORDER + BORDER - halfBorder;

                g2.fillOval(chessX, chessY, CHESS_SIZE, CHESS_SIZE);

                if (isHighlight(i, j) || i == currentRow && j == currentCol) {
                    // 当前棋子高亮
                    g2.setColor(Color.RED);
                    g2.drawOval(chessX, chessY, CHESS_SIZE, CHESS_SIZE);
                }
            }
        }
    }

    private void initStartPanel() {
        startPanel = new JPanel();
        startPanel.setBounds(10, 10, 100, 200);

        mainPanel.setPreferredSize(new Dimension(150, 220));
        mainPanel.add(startPanel);

        JLabel label1 = new JLabel("游戏模式：");
        label1.setFont(new Font("", 1, 13));
        startPanel.add(label1);

        JRadioButton humanVsPCRadio = new JRadioButton(GameMode.HUMAN_VS_PC.getName(), false);
        humanVsPCRadio.setEnabled(false);
        humanVsPCRadio.setActionCommand(humanVsPCRadio.getText());
        JRadioButton humanVsHumanRadio = new JRadioButton(GameMode.HUMAN_VS_HUMAN.getName(), true);
        humanVsHumanRadio.setActionCommand(humanVsHumanRadio.getText());

        ButtonGroup modeRadioGroup = new ButtonGroup();
        modeRadioGroup.add(humanVsPCRadio);
        modeRadioGroup.add(humanVsHumanRadio);

        startPanel.add(humanVsPCRadio);
        startPanel.add(humanVsHumanRadio);

        JLabel label2 = new JLabel("选择棋子：");
        label2.setFont(new Font("", 1, 13));
        startPanel.add(label2);

        JRadioButton blackChessRadio = new JRadioButton("黑棋", true);
        blackChessRadio.setActionCommand("1");
        JRadioButton whiteChessRadio = new JRadioButton("白棋");
        whiteChessRadio.setActionCommand("2");

        ButtonGroup chessRadioGroup = new ButtonGroup();
        chessRadioGroup.add(blackChessRadio);
        chessRadioGroup.add(whiteChessRadio);

        startPanel.add(blackChessRadio);
        startPanel.add(whiteChessRadio);

        JButton startGameButton = new JButton("开始游戏");
        startGameButton.addActionListener(e -> {
            mainPanel.remove(startPanel);
            gameMode = GameMode.getMode(modeRadioGroup.getSelection().getActionCommand());
            type = Integer.parseInt(chessRadioGroup.getSelection().getActionCommand());
            initChessPanel();
        });

        startPanel.add(startGameButton);
    }

    @Override
    protected void init() {
        mainPanel.setLayout(null);
        initStartPanel();
    }

    @Override
    public void start() {
        super.start();
    }

    private int currentRow;
    private int currentCol;

    public boolean putChess(int x, int y, int type) {
        if (isGameOver) {
            return false;
        }

        // 计算出对应的行列 四舍五入取整
        int row = Math.round((float) (x - BORDER) / BORDER);
        int col = Math.round((float) (y - BORDER) / BORDER);

        if (row < 0 || col < 0 || row > ROWS - 1 || col > COLS - 1) {
            return false;
        }

        // 棋子圆心坐标
        int circleX = row * BORDER + BORDER;
        int circleY = col * BORDER + BORDER;

        // 判断鼠标点击的坐标是否在棋子圆外
        boolean notInCircle = Math.pow(circleX - x, 2) + Math.pow(circleY - y, 2) > Math.pow(CHESS_SIZE / 2, 2);

        if (notInCircle) {
            // 不在棋子圆内
            return false;
        }

        setChess(row, col, type);

        return true;
    }

    private void setChess(int x, int y, int type) {
        if (chessData[x][y] != 0) {
            // 此处已有棋子
            return;
        }

        currentRow = x;
        currentCol = y;
        currentChessTotal++;
        chessData[x][y] = type;
        // 重绘
        chessPanel.repaint();
        // 检查是否5连
        checkWinner(x, y, type);
    }

    /**
     * 检查是否和棋
     */
    public void checkPeace() {
        if (currentChessTotal == CHESS_TOTAL) {
            peacemaker();
        }
    }

    /**
     * 检查是否5连
     *
     * @param x
     * @param y
     * @param type
     */
    public void checkWinner(int x, int y, int type) {
        // 横轴
        initChessHighLight();
        int k = 1;
        for (int i = 1; i < 5; i++) {
            int preX = x - i;
            if (preX < 0) {
                break;
            }

            if (chessData[preX][y] != type) {
                break;
            }

            setChessHighlight(preX, y);

            if (++k == 5) {
                winner();
                return;
            }
        }
        for (int i = 1; i < 5; i++) {
            int nextX = x + i;
            if (nextX > ROWS - 1) {
                break;
            }

            if (chessData[nextX][y] != type) {
                break;
            }

            setChessHighlight(nextX, y);

            if (++k == 5) {
                winner();
                return;
            }
        }

        // 纵轴
        initChessHighLight();
        k = 1;
        for (int i = 1; i < 5; i++) {
            int preY = y - i;
            if (preY < 0) {
                break;
            }

            if (chessData[x][preY] != type) {
                break;
            }

            setChessHighlight(x, preY);

            if (++k == 5) {
                winner();
                return;
            }
        }
        for (int i = 1; i < 5; i++) {
            int nextY = y + i;
            if (nextY > COLS - 1) {
                break;
            }

            if (chessData[x][nextY] != type) {
                break;
            }

            setChessHighlight(x, nextY);

            if (++k == 5) {
                winner();
                return;
            }
        }

        // 左对角线
        initChessHighLight();
        k = 1;
        for (int i = 1; i < 5; i++) {
            int preX = x - i;
            int preY = y - i;
            if (preX < 0 || preY < 0) {
                break;
            }

            if (chessData[preX][preY] != type) {
                break;
            }

            setChessHighlight(preX, preY);

            if (++k == 5) {
                winner();
                return;
            }
        }
        for (int i = 1; i < 5; i++) {
            int nextX = x + i;
            int nextY = y + i;
            if (nextX > ROWS - 1 || nextY > COLS - 1) {
                break;
            }

            if (chessData[nextX][nextY] != type) {
                break;
            }

            setChessHighlight(nextX, nextY);

            if (++k == 5) {
                winner();
                return;
            }
        }

        // 右对角线
        initChessHighLight();
        k = 1;
        for (int i = 1; i < 5; i++) {
            int nextX = x + i;
            int preY = y - i;
            if (nextX > ROWS - 1 || preY < 0) {
                break;
            }

            if (chessData[nextX][preY] != type) {
                break;
            }

            setChessHighlight(nextX, preY);

            if (++k == 5) {
                winner();
                return;
            }
        }
        for (int i = 1; i < 5; i++) {
            int preX = x - i;
            int nextY = y + i;
            if (preX < 0 || nextY > COLS - 1) {
                break;
            }

            if (chessData[preX][nextY] != type) {
                break;
            }

            setChessHighlight(preX, nextY);

            if (++k == 5) {
                winner();
                return;
            }
        }

        // 检查是否和棋
        checkPeace();

        initChessHighLight();
    }

    private void winner() {
        chessPanel.repaint();
        status = 1;
    }

    private void peacemaker() {
        status = 2;
    }

    private void send(int x, int y) {
        String opponent = GameAction.getOpponent();
        GobangDTO dto = new GobangDTO();
        dto.setX(x);
        dto.setY(y);
        dto.setType(type);
        dto.setOpponentId(DataCache.userMap.get(opponent));
        MessageAction.send(dto, Action.GAME);
    }

    private void showTips(String msg) {
        if (isGameOver) {
            return;
        }

        tips.setText(msg);
    }

    private void initChessHighLight() {
        chessHighlight = new HashMap<>();
    }

    private void setChessHighlight(int x, int y) {
        this.chessHighlight.put(x + "," + y, true);
    }

    private boolean isHighlight(int x, int y) {
        if (chessHighlight == null) {
            return false;
        }

        return chessHighlight.containsKey(x + "," + y);
    }
}