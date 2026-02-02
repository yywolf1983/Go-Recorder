package com.gosgf.app.util;

import com.gosgf.app.model.GoBoard;

import java.util.ArrayList;
import java.util.List;

/**
 * SGF转换器 - 简化 SGFParser 和 GoBoard 之间的数据转换
 */
public class SGFConverter {
    
    /**
     * 将 SGF 节点转换为 GoBoard 移动
     * @param node SGF节点
     * @return GoBoard移动
     */
    public static GoBoard.Move nodeToMove(SGFParser.Node node) {
        // 解析黑棋移动
        String bMove = node.getFirstPropertyValue("B");
        if (bMove != null) {
            GoBoard.Move move = createMoveFromCoord(bMove, 1);
            parseNodeProperties(node, move);
            return move;
        }
        
        // 解析白棋移动
        String wMove = node.getFirstPropertyValue("W");
        if (wMove != null) {
            GoBoard.Move move = createMoveFromCoord(wMove, 2);
            parseNodeProperties(node, move);
            return move;
        }
        
        return null;
    }
    
    /**
     * 将 GoBoard 移动转换为 SGF 节点
     * @param move GoBoard移动
     * @return SGF节点
     */
    public static SGFParser.Node moveToNode(GoBoard.Move move) {
        SGFParser.Node node = new SGFParser.Node();
        
        // 添加移动坐标
        String coord = moveToCoord(move);
        if (move.color == 1) {
            node.addProperty("B", coord);
        } else {
            node.addProperty("W", coord);
        }
        
        // 添加注释
        if (move.comment != null && !move.comment.isEmpty()) {
            node.addProperty("C", move.comment);
        }
        
        // 添加标签
        if (move.label != null && !move.label.isEmpty()) {
            node.addProperty("LB", move.label);
        }
        
        // 添加标记
        switch (move.markType) {
            case 1:
                node.addProperty("TR", coord); // 三角形
                break;
            case 2:
                node.addProperty("SQ", coord); // 方形
                break;
            case 3:
                node.addProperty("CR", coord); // 圆形
                break;
            case 4:
                node.addProperty("BM", coord); // X标记
                break;
        }
        
        // 添加分支
        for (GoBoard.Variation variation : move.variations) {
            List<SGFParser.Node> varNodes = new ArrayList<>();
            for (GoBoard.Move varMove : variation.getMoves()) {
                varNodes.add(moveToNode(varMove));
            }
            node.addVariation(varNodes);
        }
        
        return node;
    }
    
    /**
     * 将 SGF 树转换为 GoBoard 状态
     * @param sgfTree SGF树
     * @param board GoBoard对象
     */
    public static void sgfTreeToBoard(SGFParser.SGFTree sgfTree, GoBoard board) {
        if (sgfTree == null || board == null) {
            System.err.println("SGF转换错误: 空参数");
            return;
        }
        
        try {
            // 解析根节点
            SGFParser.Node rootNode = sgfTree.getRootNode();
            if (rootNode != null) {
                // 解析游戏信息
                String pb = rootNode.getFirstPropertyValue("PB");
                String pw = rootNode.getFirstPropertyValue("PW");
                String re = rootNode.getFirstPropertyValue("RE");
                String dt = rootNode.getFirstPropertyValue("DT");
                
                if (pb != null) board.setBlackPlayer(pb);
                if (pw != null) board.setWhitePlayer(pw);
                if (re != null) board.setResult(re);
                if (dt != null) board.setDate(dt);
                
                // 解析让子信息
                String handicap = rootNode.getFirstPropertyValue("HA");
                List<String> blackStones = rootNode.getPropertyValues("AB");
                List<String> whiteStones = rootNode.getPropertyValues("AW");
                
                boolean hasHandicap = (handicap != null) || !blackStones.isEmpty() || !whiteStones.isEmpty();
                
                if (hasHandicap) {
                    try {
                        int hc = handicap != null ? Integer.parseInt(handicap) : blackStones.size();
                        if (hc > 0 && hc <= 9) {
                            parseHandicapStones(rootNode, board, hc);
                        } else {
                            System.err.println("SGF转换错误: 让子数必须在1-9之间");
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("SGF转换错误: 无效的让子数格式");
                    }
                }
            }
            
            // 解析根节点下的分支（第一手分支）
            if (sgfTree.hasRootVariations()) {
                parseRootVariationsToBoard(sgfTree.getRootVariations(), board);
            }
            
            // 解析主序列
            List<SGFParser.Node> mainSequence = sgfTree.getMainSequence();
            if (mainSequence != null && !mainSequence.isEmpty()) {
                parseSequenceToBoard(mainSequence, board);
            }
        } catch (Exception e) {
            System.err.println("SGF转换错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 将 GoBoard 状态转换为 SGF 树
     * @param board GoBoard对象
     * @return SGF树
     */
    public static SGFParser.SGFTree boardToSgfTree(GoBoard board) {
        // 创建根节点
        SGFParser.Node rootNode = new SGFParser.Node();
        rootNode.addProperty("FF", "4");
        rootNode.addProperty("GM", "1");
        rootNode.addProperty("SZ", "19");
        
        // 添加游戏信息
        String blackPlayer = board.getBlackPlayer();
        String whitePlayer = board.getWhitePlayer();
        String result = board.getResult();
        
        if (!blackPlayer.isEmpty()) {
            rootNode.addProperty("PB", blackPlayer);
        }
        if (!whitePlayer.isEmpty()) {
            rootNode.addProperty("PW", whitePlayer);
        }
        if (!result.isEmpty()) {
            rootNode.addProperty("RE", result);
        }
        
        // 检查是否有起始分支
        List<List<SGFParser.Node>> rootVariations = null;
        List<SGFParser.Node> mainSequence = new ArrayList<>();
        List<GoBoard.Move> moveHistory = board.getMoveHistory();
        
        if (board.hasStartVariations()) {
            // 多分支棋局：将所有起始分支保存到 rootVariations
            rootVariations = new ArrayList<>();
            
            List<List<GoBoard.Move>> startVariations = board.getStartVariations();
            for (List<GoBoard.Move> variationMoves : startVariations) {
                List<SGFParser.Node> variationNodes = new ArrayList<>();
                for (GoBoard.Move move : variationMoves) {
                    variationNodes.add(moveToNode(move));
                }
                rootVariations.add(variationNodes);
            }
        } else {
            // 普通棋局：将 moveHistory 作为主序列保存
            for (GoBoard.Move move : moveHistory) {
                mainSequence.add(moveToNode(move));
            }
        }
        
        return new SGFParser.SGFTree(rootNode, mainSequence, rootVariations);
    }
    
    /**
     * 从坐标创建移动
     * @param coord SGF坐标
     * @param color 颜色
     * @return GoBoard移动
     */
    public static GoBoard.Move createMoveFromCoord(String coord, int color) {
        if (coord.equals("tt")) {
            return new GoBoard.Move(-1, -1, color); // 虚手
        } else if (coord.length() == 2) {
            int x = coord.charAt(0) - 'a';
            int y = coord.charAt(1) - 'a';
            return new GoBoard.Move(x, y, color);
        } else {
            return new GoBoard.Move(-1, -1, color); // 无效坐标，当作虚手
        }
    }
    
    /**
     * 将移动转换为坐标
     * @param move GoBoard移动
     * @return SGF坐标
     */
    public static String moveToCoord(GoBoard.Move move) {
        if (move.x == -1 || move.y == -1) {
            return "tt"; // 虚手
        }
        char xChar = (char) ('a' + move.x);
        char yChar = (char) ('a' + move.y);
        return "" + xChar + yChar;
    }
    
    /**
     * 解析节点属性
     * @param node SGF节点
     * @param move GoBoard移动
     */
    private static void parseNodeProperties(SGFParser.Node node, GoBoard.Move move) {
        // 直接使用 GoBoard 中的 parseNodeProperties 方法
        // 注意：这里需要将 GoBoard 类中的 parseNodeProperties 方法改为静态，或者通过其他方式调用
        // 由于目前 GoBoard 中的方法是实例方法，我们暂时保留此方法的实现
        // 后续可以考虑将这些通用方法提取到工具类中
        
        // 解析注释
        String comment = node.getFirstPropertyValue("C");
        if (comment != null) {
            move.comment = comment;
        }
        
        // 解析标记
        String mark = node.getFirstPropertyValue("MA");
        if (mark != null) {
            move.markType = 1; // 三角形
        }
        
        String circle = node.getFirstPropertyValue("CR");
        if (circle != null) {
            move.markType = 3; // 圆形
        }
        
        String square = node.getFirstPropertyValue("SQ");
        if (square != null) {
            move.markType = 2; // 方形
        }
        
        String triangle = node.getFirstPropertyValue("TR");
        if (triangle != null) {
            move.markType = 1; // 三角形
        }
        
        String xmark = node.getFirstPropertyValue("BM");
        if (xmark != null) {
            move.markType = 4; // X标记
        }
        
        // 解析标签
        String label = node.getFirstPropertyValue("LB");
        if (label != null) {
            move.label = label;
        }
    }
    
    /**
     * 解析让子
     * @param node SGF根节点
     * @param board GoBoard对象
     * @param handicap 让子数
     */
    private static void parseHandicapStones(SGFParser.Node node, GoBoard board, int handicap) {
        if (node == null || board == null) {
            System.err.println("解析让子错误: 空参数");
            return;
        }
        
        try {
            // 解析AB（黑方让子）
            List<String> blackStones = node.getPropertyValues("AB");
            if (!blackStones.isEmpty()) {
                // 有明确指定的让子位置
                for (String coord : blackStones) {
                    if (coord != null && !coord.isEmpty()) {
                        GoBoard.Move move = createMoveFromCoord(coord, 1);
                        if (move.x != -1 && move.y != -1 && move.x < 19 && move.y < 19) {
                            board.setupStone(move.x, move.y, 1);
                        } else {
                            System.err.println("解析让子错误: 无效的黑棋坐标: " + coord);
                        }
                    }
                }
            } else if (handicap > 0) {
                // 没有明确指定让子位置，自动放置
                placeAutomaticHandicap(board, handicap);
            }
            
            // 解析AW（白方让子）
            List<String> whiteStones = node.getPropertyValues("AW");
            for (String coord : whiteStones) {
                if (coord != null && !coord.isEmpty()) {
                    GoBoard.Move move = createMoveFromCoord(coord, 2);
                    if (move.x != -1 && move.y != -1 && move.x < 19 && move.y < 19) {
                        board.setupStone(move.x, move.y, 2);
                    } else {
                        System.err.println("解析让子错误: 无效的白棋坐标: " + coord);
                    }
                }
            }
            
            // 保存让子信息到initialBoard
            board.snapshotInitialSetup();
        } catch (Exception e) {
            System.err.println("解析让子错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 自动放置让子棋子
     * @param board GoBoard对象
     * @param handicap 让子数
     */
    private static void placeAutomaticHandicap(GoBoard board, int handicap) {
        if (handicap <= 0 || handicap > 9) {
            return;
        }
        
        int size = 19; // 默认棋盘大小
        int center = (size - 1) / 2;
        int starPoint = 3; // 19路棋盘的星位点距离
        
        // 标准星位点
        int[][] starPoints = {
            {center - starPoint, center - starPoint}, // 左上
            {center - starPoint, center + starPoint}, // 左下
            {center, center},                         // 中央
            {center + starPoint, center - starPoint}, // 右上
            {center + starPoint, center + starPoint}  // 右下
        };
        
        // 根据让子数放置棋子
        switch (handicap) {
            case 2:
                // 对角星
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                break;
            case 3:
                // 三星（对角+中央）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[2][0], starPoints[2][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                break;
            case 4:
                // 四星（四个角）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                break;
            case 5:
                // 五星（四星+中央）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[2][0], starPoints[2][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                break;
            case 6:
                // 六星（四个角+两个边）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                board.setupStone(center - starPoint, center, 1); // 左边
                board.setupStone(center + starPoint, center, 1); // 右边
                break;
            case 7:
                // 七星（六星+中央）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[2][0], starPoints[2][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                board.setupStone(center - starPoint, center, 1); // 左边
                board.setupStone(center + starPoint, center, 1); // 右边
                break;
            case 8:
                // 八星（四个角+四个边）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                board.setupStone(center - starPoint, center, 1); // 左边
                board.setupStone(center + starPoint, center, 1); // 右边
                board.setupStone(center, center - starPoint, 1); // 上边
                board.setupStone(center, center + starPoint, 1); // 下边
                break;
            case 9:
                // 九星（八星+中央）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[2][0], starPoints[2][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                board.setupStone(center - starPoint, center, 1); // 左边
                board.setupStone(center + starPoint, center, 1); // 右边
                board.setupStone(center, center - starPoint, 1); // 上边
                board.setupStone(center, center + starPoint, 1); // 下边
                break;
        }
    }
    
    /**
     * 解析根节点下的分支到棋盘（第一手分支）
     * @param rootVariations 根节点下的分支列表
     * @param board GoBoard对象
     */
    private static void parseRootVariationsToBoard(List<List<SGFParser.Node>> rootVariations, GoBoard board) {
        if (rootVariations == null || rootVariations.isEmpty()) {
            return;
        }
        
        for (List<SGFParser.Node> variation : rootVariations) {
            List<GoBoard.Move> moves = new ArrayList<>();
            parseVariationToMoves(variation, moves);
            if (!moves.isEmpty()) {
                String branchName = "分支 " + (board.getStartVariationsCount() + 1);
                board.addStartVariation(moves, branchName);
            }
        }
    }
    
    /**
     * 解析节点序列到棋盘
     * @param nodes 节点列表
     * @param board GoBoard对象
     */
    private static void parseSequenceToBoard(List<SGFParser.Node> nodes, GoBoard board) {
        for (SGFParser.Node node : nodes) {
            GoBoard.Move move = nodeToMove(node);
            if (move != null) {
                board.addMoveToHistory(move);
                
                // 解析分支
                List<List<SGFParser.Node>> variations = node.getVariations();
                for (List<SGFParser.Node> variation : variations) {
                    List<GoBoard.Move> varMoves = new ArrayList<>();
                    parseVariationToMoves(variation, varMoves);
                    if (!varMoves.isEmpty()) {
                        String branchName = "分支 " + (move.variations.size() + 1);
                        move.addVariation(varMoves, branchName);
                    }
                }
            }
        }
    }
    
    /**
     * 解析分支到移动列表
     * @param nodes 节点列表
     * @param moves 移动列表
     */
    private static void parseVariationToMoves(List<SGFParser.Node> nodes, List<GoBoard.Move> moves) {
        for (SGFParser.Node node : nodes) {
            GoBoard.Move move = nodeToMove(node);
            if (move != null) {
                moves.add(move);
                
                // 解析嵌套分支
                List<List<SGFParser.Node>> variations = node.getVariations();
                for (List<SGFParser.Node> variation : variations) {
                    List<GoBoard.Move> varMoves = new ArrayList<>();
                    parseVariationToMoves(variation, varMoves);
                    if (!varMoves.isEmpty()) {
                        String branchName = "分支 " + (move.variations.size() + 1);
                        move.addVariation(varMoves, branchName);
                    }
                }
            }
        }
    }
}