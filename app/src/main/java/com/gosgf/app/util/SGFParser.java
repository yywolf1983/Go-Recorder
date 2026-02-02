package com.gosgf.app.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SGF解析器 - 基于SGF标准（FF[4]）实现
 * 支持BNF语法解析、树状结构、分支处理、多值属性等
 */
public class SGFParser {
    
    /**
     * SGF节点类
     * 对应BNF中的<node>
     */
    public static class Node {
        private final Map<String, List<String>> properties = new HashMap<>();
        private final List<List<Node>> variations = new ArrayList<>();
        
        /**
         * 添加属性
         * @param ident 属性标识符
         * @param value 属性值
         */
        public void addProperty(String ident, String value) {
            properties.computeIfAbsent(ident, k -> new ArrayList<>()).add(value);
        }
        
        /**
         * 获取属性值列表
         * @param ident 属性标识符
         * @return 属性值列表
         */
        public List<String> getPropertyValues(String ident) {
            return properties.getOrDefault(ident, new ArrayList<>());
        }
        
        /**
         * 获取第一个属性值
         * @param ident 属性标识符
         * @return 第一个属性值，不存在返回null
         */
        public String getFirstPropertyValue(String ident) {
            List<String> values = properties.get(ident);
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }
        
        /**
         * 添加分支
         * @param variation 分支节点序列
         */
        public void addVariation(List<Node> variation) {
            variations.add(variation);
        }
        
        /**
         * 获取分支列表
         * @return 分支列表
         */
        public List<List<Node>> getVariations() {
            return variations;
        }
        
        /**
         * 检查是否有分支
         * @return 是否有分支
         */
        public boolean hasVariations() {
            return !variations.isEmpty();
        }
        
        /**
         * 检查是否有属性
         * @param ident 属性标识符
         * @return 是否有属性
         */
        public boolean hasProperty(String ident) {
            return properties.containsKey(ident);
        }
        
        /**
         * 获取所有属性标识符
         * @return 属性标识符列表
         */
        public List<String> getPropertyIdentifiers() {
            return new ArrayList<>(properties.keySet());
        }
    }
    
    /**
     * SGF树类
     * 对应BNF中的<sgf-tree>
     */
    public static class SGFTree {
        private final Node rootNode;
        private final List<Node> mainSequence;
        
        public SGFTree(Node rootNode, List<Node> mainSequence) {
            this.rootNode = rootNode;
            this.mainSequence = mainSequence;
        }
        
        /**
         * 获取根节点
         * @return 根节点
         */
        public Node getRootNode() {
            return rootNode;
        }
        
        /**
         * 获取主序列
         * @return 主序列节点列表
         */
        public List<Node> getMainSequence() {
            return mainSequence;
        }
        
        /**
         * 检查是否有效
         * @return 是否有效
         */
        public boolean isValid() {
            return rootNode != null && rootNode.hasProperty("FF") && 
                   rootNode.hasProperty("GM") && rootNode.hasProperty("SZ");
        }
    }
    
    /**
     * 解析SGF字符串
     * @param sgf SGF字符串
     * @return SGF树
     * @throws SGFParseException 解析异常
     */
    public static SGFTree parse(String sgf) throws SGFParseException {
        if (sgf == null || sgf.trim().isEmpty()) {
            throw new SGFParseException("Empty SGF string");
        }
        
        Parser parser = new Parser(sgf.trim());
        return parser.parseSGF();
    }
    
    /**
     * 保存SGF树为字符串
     * @param tree SGF树
     * @return SGF字符串
     * @throws SGFParseException 保存异常
     */
    public static String save(SGFTree tree) throws SGFParseException {
        if (tree == null) {
            throw new SGFParseException("Null SGF tree");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        
        // 保存根节点
        saveNode(tree.getRootNode(), sb);
        
        // 保存主序列
        for (Node node : tree.getMainSequence()) {
            saveNode(node, sb);
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * 保存节点
     */
    private static void saveNode(Node node, StringBuilder sb) {
        sb.append(";").append(" ");
        
        // 保存属性
        for (String ident : node.getPropertyIdentifiers()) {
            List<String> values = node.getPropertyValues(ident);
            for (String value : values) {
                sb.append(ident).append("[").append(escapeValue(value)).append("]").append(" ");
            }
        }
        
        // 保存分支
        for (List<Node> variation : node.getVariations()) {
            sb.append("(");
            for (Node varNode : variation) {
                saveNode(varNode, sb);
            }
            sb.append(")");
        }
    }
    
    /**
     * 转义属性值
     */
    private static String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        
        return value.replace("\\", "\\\\")
                   .replace("[", "\\[")
                   .replace("]", "\\]")
                   .replace(":", "\\:")
                   .replace("|", "\\|");
    }
    
    /**
     * 内部解析器类
     */
    private static class Parser {
        private final String input;
        private int position;
        
        public Parser(String input) {
            this.input = input;
            this.position = 0;
        }
        
        /**
         * 解析整个SGF
         * 对应BNF中的<sgf-tree>
         */
        public SGFTree parseSGF() throws SGFParseException {
            skipWhitespace();
            
            if (position >= input.length() || input.charAt(position) != '(') {
                throw new SGFParseException("Expected '(' at start of SGF tree");
            }
            
            List<Node> gameTree = parseGameTree();
            
            if (gameTree.isEmpty()) {
                throw new SGFParseException("Empty game tree");
            }
            
            Node rootNode = gameTree.get(0);
            List<Node> mainSequence = gameTree.subList(1, gameTree.size());
            
            return new SGFTree(rootNode, mainSequence);
        }
        
        /**
         * 解析游戏树
         * 对应BNF中的<sequence>
         */
        private List<Node> parseGameTree() throws SGFParseException {
            position++;
            List<Node> sequence = new ArrayList<>();
            
            while (position < input.length()) {
                char current = input.charAt(position);
                
                if (current == ';') {
                    // 解析节点
                    Node node = parseNode();
                    sequence.add(node);
                } else if (current == '(') {
                    // 解析分支
                    List<Node> variation = parseGameTree();
                    if (!sequence.isEmpty()) {
                        Node lastNode = sequence.get(sequence.size() - 1);
                        lastNode.addVariation(variation);
                    }
                } else if (current == ')') {
                    // 分支结束
                    position++;
                    break;
                } else if (Character.isWhitespace(current)) {
                    // 跳过空白字符
                    skipWhitespace();
                } else {
                    // 其他字符，跳过
                    position++;
                }
            }
            
            return sequence;
        }
        
        /**
         * 解析节点
         * 对应BNF中的<node>
         */
        private Node parseNode() throws SGFParseException {
            position++;
            Node node = new Node();
            
            skipWhitespace();
            
            while (position < input.length()) {
                char current = input.charAt(position);
                
                if (current == ';' || current == '(' || current == ')') {
                    // 节点结束
                    break;
                }
                
                // 解析属性
                String ident = parsePropertyIdent();
                List<String> values = parsePropertyValues();
                
                for (String value : values) {
                    node.addProperty(ident, value);
                }
                
                skipWhitespace();
            }
            
            return node;
        }
        
        /**
         * 解析属性标识符
         * 对应BNF中的<prop-ident>
         */
        private String parsePropertyIdent() throws SGFParseException {
            StringBuilder sb = new StringBuilder();
            
            while (position < input.length()) {
                char current = input.charAt(position);
                if (current >= 'A' && current <= 'Z') {
                    sb.append(current);
                    position++;
                } else {
                    break;
                }
            }
            
            if (sb.length() == 0) {
                throw new SGFParseException("Empty property identifier");
            }
            
            return sb.toString();
        }
        
        /**
         * 解析属性值列表
         * 对应BNF中的<property-list>
         */
        private List<String> parsePropertyValues() throws SGFParseException {
            List<String> values = new ArrayList<>();
            
            while (position < input.length() && input.charAt(position) == '[') {
                String value = parsePropertyValue();
                values.add(value);
            }
            
            return values;
        }
        
        /**
         * 解析单个属性值
         * 对应BNF中的<prop-value>
         */
        private String parsePropertyValue() throws SGFParseException {
            position++;
            StringBuilder sb = new StringBuilder();
            
            while (position < input.length()) {
                char current = input.charAt(position);
                
                if (current == '\\') {
                    // 处理转义字符
                    if (position + 1 < input.length()) {
                        char next = input.charAt(position + 1);
                        if (next == '[' || next == ']' || next == '\\' || next == ':' || next == '|') {
                            sb.append(next);
                            position += 2;
                        } else {
                            // 无效转义，当作普通字符
                            sb.append(current);
                            position++;
                        }
                    } else {
                        // 结尾的反斜杠
                        sb.append(current);
                        position++;
                    }
                } else if (current == ']') {
                    // 属性值结束
                    position++;
                    break;
                } else {
                    // 普通字符
                    sb.append(current);
                    position++;
                }
            }
            
            return sb.toString();
        }
        
        /**
         * 跳过空白字符
         */
        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }
    }
    
    /**
     * SGF解析异常
     */
    public static class SGFParseException extends Exception {
        public SGFParseException(String message) {
            super(message);
        }
    }
    
    /**
     * 解析让子
     * @param node SGF根节点
     * @param board GoBoard对象
     * @param handicap 让子数
     */
    private static void parseHandicapStones(Node node, com.gosgf.app.model.GoBoard board, int handicap) {
        if (node == null || board == null) {
            return;
        }
        
        try {
            // 解析AB（黑方让子）
            List<String> blackStones = node.getPropertyValues("AB");
            if (!blackStones.isEmpty()) {
                // 有明确指定的让子位置
                for (String coord : blackStones) {
                    if (coord != null && !coord.isEmpty()) {
                        com.gosgf.app.model.GoBoard.Move move = createMoveFromCoord(coord, 1);
                        if (move.x != -1 && move.y != -1 && move.x < 19 && move.y < 19) {
                            board.setupStone(move.x, move.y, 1);
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
                    com.gosgf.app.model.GoBoard.Move move = createMoveFromCoord(coord, 2);
                    if (move.x != -1 && move.y != -1 && move.x < 19 && move.y < 19) {
                        board.setupStone(move.x, move.y, 2);
                    }
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
    }
    
    /**
     * 自动放置让子棋子
     * @param board GoBoard对象
     * @param handicap 让子数
     */
    private static void placeAutomaticHandicap(com.gosgf.app.model.GoBoard board, int handicap) {
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
                board.setupStone(starPoints[0][1], starPoints[0][1], 1); // 左边中点
                board.setupStone(starPoints[3][1], starPoints[3][1], 1); // 右边中点
                break;
            case 7:
                // 七星（六星+中央）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[2][0], starPoints[2][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                board.setupStone(starPoints[0][1], starPoints[0][1], 1); // 左边中点
                board.setupStone(starPoints[3][1], starPoints[3][1], 1); // 右边中点
                break;
            case 8:
                // 八星（四个角+四个边）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                board.setupStone(starPoints[0][1], starPoints[0][1], 1); // 左边中点
                board.setupStone(starPoints[3][1], starPoints[3][1], 1); // 右边中点
                board.setupStone(starPoints[2][0], starPoints[0][0], 1); // 上边中点
                board.setupStone(starPoints[2][0], starPoints[4][1], 1); // 下边中点
                break;
            case 9:
                // 九星（八星+中央）
                board.setupStone(starPoints[0][0], starPoints[0][1], 1);
                board.setupStone(starPoints[1][0], starPoints[1][1], 1);
                board.setupStone(starPoints[2][0], starPoints[2][1], 1);
                board.setupStone(starPoints[3][0], starPoints[3][1], 1);
                board.setupStone(starPoints[4][0], starPoints[4][1], 1);
                board.setupStone(starPoints[0][1], starPoints[0][1], 1); // 左边中点
                board.setupStone(starPoints[3][1], starPoints[3][1], 1); // 右边中点
                board.setupStone(starPoints[2][0], starPoints[0][0], 1); // 上边中点
                board.setupStone(starPoints[2][0], starPoints[4][1], 1); // 下边中点
                break;
        }
    }
    
    /**
     * 向后兼容方法：解析SGF字符串并加载到棋盘
     * @param sgf SGF字符串
     * @param board GoBoard对象
     * @throws InvalidSGFException 解析异常
     */
    public static void parseSGF(String sgf, com.gosgf.app.model.GoBoard board) throws SGFParseException {
        try {
            SGFTree sgfTree = parse(sgf);
            
            // 解析根节点的头信息
            Node rootNode = sgfTree.getRootNode();
            if (rootNode != null) {
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
                if (handicap != null) {
                    try {
                        int hc = Integer.parseInt(handicap);
                        if (hc > 0 && hc <= 9) {
                            parseHandicapStones(rootNode, board, hc);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略无效的让子数
                    }
                } else {
                    // 检查是否有明确的让子坐标
                    List<String> blackStones = rootNode.getPropertyValues("AB");
                    List<String> whiteStones = rootNode.getPropertyValues("AW");
                    if (!blackStones.isEmpty() || !whiteStones.isEmpty()) {
                        parseHandicapStones(rootNode, board, 0);
                    }
                }
            }
            
            // 解析主序列
            List<Node> mainSequence = sgfTree.getMainSequence();
            
            // 如果主序列为空，检查根节点是否有分支
            if (mainSequence.isEmpty() && rootNode != null && rootNode.hasVariations()) {
                // 获取根节点的所有分支
                List<List<Node>> rootVariations = rootNode.getVariations();
                if (!rootVariations.isEmpty()) {
                    // 解析所有分支，添加到startVariations中
                    for (int i = 0; i < rootVariations.size(); i++) {
                        List<com.gosgf.app.model.GoBoard.Move> varMoves = new ArrayList<>();
                        parseSequenceToMoveList(rootVariations.get(i), varMoves);
                        if (!varMoves.isEmpty()) {
                            // 将分支添加到startVariations中
                            com.gosgf.app.model.GoBoard.Variation variation = new com.gosgf.app.model.GoBoard.Variation(varMoves, "分支 " + i);
                            // 使用反射获取startVariations字段并添加分支
                            try {
                                java.lang.reflect.Field field = com.gosgf.app.model.GoBoard.class.getDeclaredField("startVariations");
                                field.setAccessible(true);
                                java.util.List<com.gosgf.app.model.GoBoard.Variation> startVariations = (java.util.List<com.gosgf.app.model.GoBoard.Variation>) field.get(board);
                                startVariations.add(variation);
                            } catch (Exception e) {
                                // 忽略反射异常
                            }
                        }
                    }
                    
                    // 不设置moveHistory，让用户通过点击"下一步"选择分支
                    // 清空moveHistory，确保currentMoveNumber为-1
                    try {
                        java.lang.reflect.Field field = com.gosgf.app.model.GoBoard.class.getDeclaredField("moveHistory");
                        field.setAccessible(true);
                        java.util.List<com.gosgf.app.model.GoBoard.Move> moveHistory = (java.util.List<com.gosgf.app.model.GoBoard.Move>) field.get(board);
                        moveHistory.clear();
                    } catch (Exception e) {
                        // 忽略反射异常
                    }
                    
                    return; // 提前返回，因为已经处理了所有分支
                }
            }
            
            parseSequenceToBoard(mainSequence, board);
            
        } catch (SGFParseException e) {
            throw e;
        }
    }
    
    /**
     * 向后兼容方法：将节点序列解析到棋盘
     * @param nodes 节点列表
     * @param board GoBoard对象
     */
    private static void parseSequenceToBoard(List<Node> nodes, com.gosgf.app.model.GoBoard board) {
        List<com.gosgf.app.model.GoBoard.Move> moveHistory = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            // 解析当前节点的移动
            com.gosgf.app.model.GoBoard.Move move = parseNodeToMove(node);
            if (move != null) {
                moveHistory.add(move);
                
                // 解析分支
                List<List<Node>> variations = node.getVariations();
                for (int j = 0; j < variations.size(); j++) {
                    List<Node> variation = variations.get(j);
                    List<com.gosgf.app.model.GoBoard.Move> varMoves = new ArrayList<>();
                    parseSequenceToMoveList(variation, varMoves);
                    if (!varMoves.isEmpty()) {
                        move.addVariation(varMoves, "分支 " + move.variations.size());
                        
                        // 如果是第一个移动的分支，将其添加到startVariations中
                        if (i == 0) {
                            com.gosgf.app.model.GoBoard.Variation var = new com.gosgf.app.model.GoBoard.Variation(varMoves, "分支 " + j);
                            // 使用反射获取startVariations字段并添加分支
                            try {
                                java.lang.reflect.Field field = com.gosgf.app.model.GoBoard.class.getDeclaredField("startVariations");
                                field.setAccessible(true);
                                java.util.List<com.gosgf.app.model.GoBoard.Variation> startVariations = (java.util.List<com.gosgf.app.model.GoBoard.Variation>) field.get(board);
                                startVariations.add(var);
                            } catch (Exception e) {
                                // 忽略反射异常
                            }
                        }
                    }
                }
            }
        }
        
        // 将主序列作为一个分支添加到startVariations中
        if (!moveHistory.isEmpty()) {
            com.gosgf.app.model.GoBoard.Variation mainVariation = new com.gosgf.app.model.GoBoard.Variation(moveHistory, "主分支");
            // 使用反射获取startVariations字段并添加主分支
            try {
                java.lang.reflect.Field field = com.gosgf.app.model.GoBoard.class.getDeclaredField("startVariations");
                field.setAccessible(true);
                java.util.List<com.gosgf.app.model.GoBoard.Variation> startVariations = (java.util.List<com.gosgf.app.model.GoBoard.Variation>) field.get(board);
                startVariations.add(mainVariation);
            } catch (Exception e) {
                // 忽略反射异常
            }
        }
        
        board.setMoveHistory(moveHistory);
    }
    
    /**
     * 将节点序列解析为移动列表
     * @param nodes 节点列表
     * @param moves 移动列表
     */
    private static void parseSequenceToMoveList(List<Node> nodes, List<com.gosgf.app.model.GoBoard.Move> moves) {
        for (Node node : nodes) {
            com.gosgf.app.model.GoBoard.Move move = parseNodeToMove(node);
            if (move != null) {
                moves.add(move);
                
                // 解析分支
                List<List<Node>> variations = node.getVariations();
                for (List<Node> variation : variations) {
                    List<com.gosgf.app.model.GoBoard.Move> varMoves = new ArrayList<>();
                    parseSequenceToMoveList(variation, varMoves);
                    if (!varMoves.isEmpty()) {
                        move.addVariation(varMoves, "分支 " + move.variations.size());
                    }
                }
            }
        }
    }
    
    /**
     * 将节点解析为移动
     * @param node SGF节点
     * @return 移动对象
     */
    private static com.gosgf.app.model.GoBoard.Move parseNodeToMove(Node node) {
        // 解析黑棋移动
        String bMove = node.getFirstPropertyValue("B");
        if (bMove != null) {
            com.gosgf.app.model.GoBoard.Move move = createMoveFromCoord(bMove, 1);
            parseNodePropertiesToMove(node, move);
            return move;
        }
        
        // 解析白棋移动
        String wMove = node.getFirstPropertyValue("W");
        if (wMove != null) {
            com.gosgf.app.model.GoBoard.Move move = createMoveFromCoord(wMove, 2);
            parseNodePropertiesToMove(node, move);
            return move;
        }
        
        return null;
    }
    
    /**
     * 从坐标创建移动
     * @param coord SGF坐标
     * @param color 颜色
     * @return 移动对象
     */
    private static com.gosgf.app.model.GoBoard.Move createMoveFromCoord(String coord, int color) {
        // 使用 SGFConverter 中的方法，消除重复代码
        return SGFConverter.createMoveFromCoord(coord, color);
    }
    
    /**
     * 解析节点属性到移动
     * @param node SGF节点
     * @param move 移动对象
     */
    private static void parseNodePropertiesToMove(Node node, com.gosgf.app.model.GoBoard.Move move) {
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
        
        // 解析时间设置
        String timeLeft = node.getFirstPropertyValue("BL");
        if (timeLeft != null && move.color == 1) {
            // 黑方剩余时间
        }
        
        timeLeft = node.getFirstPropertyValue("WL");
        if (timeLeft != null && move.color == 2) {
            // 白方剩余时间
        }
    }
    
    /**
     * 向后兼容方法：将棋盘状态保存为SGF字符串
     * @param board GoBoard对象
     * @param blackPlayer 黑方玩家
     * @param whitePlayer 白方玩家
     * @param result 结果
     * @return SGF字符串
     */
    public static String saveToString(com.gosgf.app.model.GoBoard board, String blackPlayer, String whitePlayer, String result) {
        try {
            // 使用SGFConverter转换数据
            SGFTree sgfTree = SGFConverter.boardToSgfTree(board);
            return save(sgfTree);
        } catch (SGFParseException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * 将移动转换为坐标
     * @param move 移动对象
     * @return 坐标字符串
     */
    private static String moveToCoord(com.gosgf.app.model.GoBoard.Move move) {
        // 使用 SGFConverter 中的方法，消除重复代码
        return SGFConverter.moveToCoord(move);
    }
}