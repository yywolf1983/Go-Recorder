package com.gosgf.app.util;

import com.gosgf.app.model.GoBoard;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SGFConverterTest {
    private GoBoard board;

    @Before
    public void setUp() {
        board = new GoBoard();
    }

    @Test
    public void testCreateMoveFromCoord() {
        // 测试从坐标创建移动
        GoBoard.Move move1 = SGFConverter.createMoveFromCoord("pd", GoBoard.BLACK);
        assertEquals(9, move1.x);
        assertEquals(9, move1.y);
        assertEquals(GoBoard.BLACK, move1.color);

        // 测试虚手
        GoBoard.Move move2 = SGFConverter.createMoveFromCoord("tt", GoBoard.WHITE);
        assertEquals(-1, move2.x);
        assertEquals(-1, move2.y);
        assertEquals(GoBoard.WHITE, move2.color);

        // 测试无效坐标
        GoBoard.Move move3 = SGFConverter.createMoveFromCoord("invalid", GoBoard.BLACK);
        assertEquals(-1, move3.x);
        assertEquals(-1, move3.y);
        assertEquals(GoBoard.BLACK, move3.color);
    }

    @Test
    public void testMoveToCoord() {
        // 测试将移动转换为坐标
        GoBoard.Move move1 = new GoBoard.Move(9, 9, GoBoard.BLACK);
        String coord1 = SGFConverter.moveToCoord(move1);
        assertEquals("pd", coord1);

        // 测试虚手
        GoBoard.Move move2 = new GoBoard.Move(-1, -1, GoBoard.WHITE);
        String coord2 = SGFConverter.moveToCoord(move2);
        assertEquals("tt", coord2);
    }

    @Test
    public void testSgfTreeToBoard() {
        // 测试将 SGF 树转换为 GoBoard
        try {
            // 创建一个简单的 SGF 字符串
            String sgfContent = "(;FF[4]GM[1]SZ[19]PB[Black]PW[White]RE[B+R];B[pd];W[qe];B[rf])";
            
            // 解析 SGF
            SGFParser.SGFTree sgfTree = SGFParser.parse(sgfContent);
            assertNotNull(sgfTree);
            
            // 转换为 GoBoard
            SGFConverter.sgfTreeToBoard(sgfTree, board);
            
            // 验证转换结果
            assertEquals("Black", board.getBlackPlayer());
            assertEquals("White", board.getWhitePlayer());
            assertEquals("B+R", board.getResult());
            assertEquals(3, board.moveHistory.size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("测试失败: " + e.getMessage());
        }
    }

    @Test
    public void testBoardToSgfTree() {
        // 测试将 GoBoard 转换为 SGF 树
        try {
            // 设置 GoBoard 状态
            board.setBlackPlayer("Black");
            board.setWhitePlayer("White");
            board.setResult("B+R");
            board.placeStone(9, 9, GoBoard.BLACK);
            board.placeStone(10, 10, GoBoard.WHITE);
            
            // 转换为 SGF 树
            SGFParser.SGFTree sgfTree = SGFConverter.boardToSgfTree(board);
            assertNotNull(sgfTree);
            
            // 验证转换结果
            SGFParser.Node rootNode = sgfTree.getRootNode();
            assertEquals("4", rootNode.getFirstPropertyValue("FF"));
            assertEquals("1", rootNode.getFirstPropertyValue("GM"));
            assertEquals("19", rootNode.getFirstPropertyValue("SZ"));
            assertEquals("Black", rootNode.getFirstPropertyValue("PB"));
            assertEquals("White", rootNode.getFirstPropertyValue("PW"));
            assertEquals("B+R", rootNode.getFirstPropertyValue("RE"));
            
            // 验证主序列
            assertEquals(2, sgfTree.getMainSequence().size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("测试失败: " + e.getMessage());
        }
    }

    @Test
    public void testHandicapPlacement() {
        // 测试让子放置
        try {
            // 创建一个带有让子的 SGF 字符串
            String sgfContent = "(;FF[4]GM[1]SZ[19]PB[Black]PW[White]HA[4];B[pd];W[qe])";
            
            // 解析 SGF
            SGFParser.SGFTree sgfTree = SGFParser.parse(sgfContent);
            assertNotNull(sgfTree);
            
            // 转换为 GoBoard
            SGFConverter.sgfTreeToBoard(sgfTree, board);
            
            // 验证让子是否正确放置
            // 检查星位点是否有黑棋
            assertTrue(board.board[3][3] == 1 || board.board[3][15] == 1 || 
                       board.board[15][3] == 1 || board.board[15][15] == 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("测试失败: " + e.getMessage());
        }
    }
}
