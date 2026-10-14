package com.arenaserver.arenaserver.domain;

import java.util.Arrays;

/**
 * 棋盘类
 * 负责管理棋盘、实现把棋子放到棋盘上
 */
public class Board {
    public static final int ROWS = 6;   // 棋盘行数
    public static final int COLS = 7;   // 棋盘列数

    private final Disc[][] cells;   // 棋盘数组

    /**
     * 初始化棋盘，每一行设置为空
     */
    public Board() {
        cells = new Disc[ROWS][COLS];
        for (Disc[] row : cells) Arrays.fill(row, Disc.EMPTY);
    }

    /**
     * 获取棋盘某个位置的数据
     * @param row 行
     * @param col 列
     * @return 该位置的数据
     */
    public Disc getCell(int row, int col) {
        if (row < 0 || row >= ROWS ||
                col < 0 || col >= COLS) {
            throw new IllegalArgumentException(
                    "Invalid board position");
        }
        return cells[row][col];
    }

    /**
     * 棋子下落方法，该方法应当实现：
     * 1. 若列号不在 [0, 6] 范围内，则抛出异常
     * 2. 若当前下落的棋子是滚木，则抛出异常
     * 3. 若指定列还有空位，则将棋子放在最底部空位
     * 4. 若指定列已满，则抛出异常
     * 5. 返回棋子最终落入的行号
     * @param col 落棋的列
     * @param disc 当前下落的棋子
     * @return 棋子最终落入的行号
     */
    public int drop(int col, Disc disc) {
        // 1、2
        if (col < 0 || col >= COLS ||
        disc == null || disc == Disc.EMPTY)
            throw new IllegalArgumentException(
                    "Invalid column or disc"
            );

        // 3
        for (int i = 0; i < ROWS; i++) {
            /* 自下往上遍历棋盘数组指定列的每一行，若这一行是空着的就落棋 */
            if (cells[i][col] == Disc.EMPTY) {
                cells[i][col] = disc;
                return i; // 5
            }
        }

        // 4
        throw new IllegalStateException(
                "Column is full"
        );
    }

    /**
     * 某方向上的同色棋子计数方法
     * 方向区分正负
     * @param row 当前位置行
     * @param col 当前位置列
     * @param dr  搜索的列方向
     * @param dc  搜搜的行方向
     * @param disc  作为计数起点的棋子
     * @return  某方向上同色棋子的个数
     */
    private int countDirection(
            int row,
            int col,
            int dr,
            int dc,
            Disc disc
    ) {
        int count = 0;

        /* 记录更新后位置 */
        int r = row + dr, c = col + dc;

        /* 越界与同色检测 */
        while ( count < 3 &&
                r >= 0 && r < ROWS &&
                c >= 0 && c < COLS &&
                disc == cells[r][c]) {

            count++;

            r += dr;
            c += dc;
        }

        return  count;
    }

    /**
     * 判断一条线上是否有四个同色棋
     * 这个方法应当在落子后触发
     * @param row 行数
     * @param col 列数
     * @return 是否有四个同色棋
     */
    public boolean hasFourFrom(int row, int col) {
        /* 位置合法性检验 */
        if (row < 0 || row >= ROWS ||
        col < 0 || col >= COLS) throw new IllegalArgumentException(
                "Invalid board position"
        );

        /* 获取当前位置棋子 */
        Disc disc = getCell(row, col);
        if (disc == null || disc == Disc.EMPTY) return false;

        /* 定义四方向向量 */
        int[][] direction = {
                {0, 1},
                {1, 0},
                {1, 1},
                {1, -1}
        };

        /* 逐个遍历四组方向 */
        for (int[] dir : direction) {
            /* 正向 */
            int positiveCount = countDirection(
                    row,
                    col,
                    dir[0],
                    dir[1],
                    disc
                    );

            /* 负向 */
            int negativeCount = countDirection(
                    row,
                    col,
                    -dir[0],
                    -dir[1],
                    disc
            );

            if (positiveCount + negativeCount + 1 >= 4) return true;
        }

        return false;
    }
}
