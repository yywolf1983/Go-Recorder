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
        assertEquals(0, board.currentMoveNumber);
        assertTrue(board.moveHistory.isEmpty());
        assertTrue(board.startVariations.isEmpty());
    }

    @Test
    public void testPlaceStone() {
        // 测试正常落子
        boolean result = board.placeStone(9, 9, GoBoard.BLACK);
        assertTrue(result);
        assertEquals(1, board.moveHistory.size());
        assertEquals(0, board.currentMoveNumber);

        // 测试非法落子（已存在棋子）
        result = board.placeStone(9, 9, GoBoard.WHITE);
        assertFalse(result);
        assertEquals(1, board.moveHistory.size());
    }

    @Test
    public void testCapture() {
        // 测试提子
        board.placeStone(8, 9, GoBoard.WHITE);
        board.placeStone(9, 8, GoBoard.BLACK);
        board.placeStone(9, 10, GoBoard.BLACK);
        board.placeStone(10, 9, GoBoard.BLACK);
        board.placeStone(9, 9, GoBoard.BLACK); // 提掉白棋
        
        assertEquals(5, board.moveHistory.size());
        assertEquals(0, board.board[8][9]); // 白棋被提掉
    }

    @Test
    public void testKoRule() {
        // 测试打劫规则
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(9, 8, GoBoard.WHITE);
        board.placeStone(8, 9, GoBoard.WHITE);
        board.placeStone(10, 9, GoBoard.WHITE);
        board.placeStone(9, 10, GoBoard.BLACK); // 提掉白棋
        
        // 测试打劫回提
        boolean result = board.placeStone(9, 9, GoBoard.WHITE);
        assertFalse(result); // 打劫不允许立即回提
    }

    @Test
    public void testVariationCreation() {
        // 测试创建分支
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(10, 10, GoBoard.WHITE);
        
        // 创建分支
        boolean result = board.createVariation("测试分支");
        assertTrue(result);
        
        // 检查分支是否创建成功
        GoBoard.Move currentMove = board.moveHistory.get(board.currentMoveNumber);
        assertEquals(1, currentMove.variations.size());
        assertEquals("测试分支", currentMove.variations.get(0).getName());
    }

    @Test
    public void testSelectVariation() {
        // 测试选择分支
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(10, 10, GoBoard.WHITE);
        
        // 创建分支
        board.createVariation("分支1");
        board.placeStone(11, 11, GoBoard.BLACK); // 在分支中落子
        
        // 回到主分支
        board.previousMove();
        board.previousMove();
        
        // 选择分支
        boolean result = board.selectVariation(0);
        assertTrue(result);
        assertEquals(2, board.currentMoveNumber);
        assertEquals(11, board.moveHistory.get(2).x);
        assertEquals(11, board.moveHistory.get(2).y);
    }

    @Test
    public void testCompareVariations() {
        // 测试比较分支
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(10, 10, GoBoard.WHITE);
        
        // 创建两个分支
        board.createVariation("分支1");
        board.placeStone(11, 11, GoBoard.BLACK);
        
        board.previousMove();
        board.previousMove();
        board.createVariation("分支2");
        board.placeStone(8, 8, GoBoard.BLACK);
        
        // 比较分支
        List<GoBoard.Move> diff = board.compareVariations(0, 1);
        assertFalse(diff.isEmpty());
    }

    @Test
    public void testMergeVariation() {
        // 测试合并分支
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(10, 10, GoBoard.WHITE);
        
        // 创建分支
        board.createVariation("分支1");
        board.placeStone(11, 11, GoBoard.BLACK);
        
        // 回到主分支
        board.previousMove();
        board.previousMove();
        
        // 合并分支
        boolean result = board.mergeVariation(0);
        assertTrue(result);
        assertEquals(3, board.moveHistory.size());
    }

    @Test
    public void testExportVariation() {
        // 测试导出分支
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(10, 10, GoBoard.WHITE);
        
        // 创建分支
        board.createVariation("分支1");
        board.placeStone(11, 11, GoBoard.BLACK);
        
        // 导出分支
        String sgf = board.exportVariation(0);
        assertNotNull(sgf);
        assertTrue(sgf.contains("B[pd]"));
        assertTrue(sgf.contains("W[qe]"));
        assertTrue(sgf.contains("B[rf]"));
    }

    @Test
    public void testReorderVariation() {
        // 测试重排序分支
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(10, 10, GoBoard.WHITE);
        
        // 创建两个分支
        board.createVariation("分支1");
        board.previousMove();
        board.previousMove();
        board.createVariation("分支2");
        
        // 重排序分支
        boolean result = board.reorderVariation(1, 0);
        assertTrue(result);
        
        GoBoard.Move currentMove = board.moveHistory.get(board.currentMoveNumber);
        assertEquals("分支2", currentMove.variations.get(0).getName());
        assertEquals("分支1", currentMove.variations.get(1).getName());
    }

    @Test
    public void testCopyVariation() {
        // 测试复制分支
        board.placeStone(9, 9, GoBoard.BLACK);
        board.placeStone(10, 10, GoBoard.WHITE);
        
        // 创建分支
        board.createVariation("分支1");
        board.placeStone(11, 11, GoBoard.BLACK);
        
        // 复制分支
        boolean result = board.copyVariation(0, "分支1副本");
        assertTrue(result);
        
        board.previousMove();
        board.previousMove();
        GoBoard.Move currentMove = board.moveHistory.get(board.currentMoveNumber);
        assertEquals(2, currentMove.variations.size());
        assertEquals("分支1", currentMove.variations.get(0).getName());
        assertEquals("分支1副本", currentMove.variations.get(1).getName());
    }
}
