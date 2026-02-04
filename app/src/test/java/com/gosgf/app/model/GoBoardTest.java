package com.gosgf.app.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class GoBoardTest {
    private GoBoard board;

    @Before
    public void setUp() {
        board = new GoBoard();
    }

    @Test
    public void testInitialState() {
        assertEquals(0, board.getCurrentMoveNumber());
        assertTrue(board.getMoveHistory().isEmpty());
    }

    @Test
    public void testPlaceStone() {
        // 测试正常落子
        boolean result = board.placeStone(9, 9, GoBoard.BLACK);
        assertTrue(result);
        assertEquals(1, board.getMoveHistory().size());
        assertEquals(0, board.getCurrentMoveNumber());

        // 测试非法落子（已存在棋子）
        result = board.placeStone(9, 9, GoBoard.WHITE);
        assertFalse(result);
        assertEquals(1, board.getMoveHistory().size());
    }

    @Test
    public void testSelectVariation() {
        // 测试选择分支
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(10, 10, GoBoard.WHITE);
        
        // 创建分支
        boolean result = board.createVariation("分支1");
        assertTrue(result);
        
        // 选择分支
        result = board.selectVariation(0);
        assertTrue(result);
    }
}
