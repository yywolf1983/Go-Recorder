package com.gosgf.app.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import android.graphics.Point;
import java.util.Arrays;

// 添加导入
import java.util.regex.Pattern;
import java.util.regex.Matcher;
// 添加Log导入
import android.util.Log;

public class GoBoard {
    // 添加字段声明
    private String blackPlayer;
    private String whitePlayer;
    private String result;
    private String date;
    private static final int BOARD_SIZE = 19;
    private int[][] board;  // 0=空, 1=黑, 2=白
    private List<Move> moveHistory;
    private int currentPlayer; // 1=黑, 2=白
    private Point lastCapture = null; // 记录最后一次提子的位置
    
    public static class Move {
        public int x;
        public int y;
        public int color;  // 1=黑, 2=白
        public String comment;
        public List<List<Move>> variations = new ArrayList<>();
        
        public Move(int x, int y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    // 添加 getStoneAt 方法
    public int getStoneAt(int x, int y) {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            return -1; // 返回-1表示无效坐标
        }
        return board[x][y];
    }
    
    // 移除setComment方法
    // public void setComment(Move move, String comment) {...}
    
    public GoBoard() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        moveHistory = new ArrayList<>();
        currentPlayer = 1; // 黑子先行
    }
    
    // 补充带参数的构造方法
    public GoBoard(int size) {
        this(); // 调用无参构造
    }
    
    // 确保只有这一个placeStone方法
    // 修改placeStone方法，完善提子逻辑
    // 删除摆子模式标志字段
    private boolean editMode = false;
    
    // 删除获取和设置摆子模式的方法
    public boolean isEditMode() {
        return editMode;
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
    
    // 修改placeStone方法，支持摆子模式
    // 修改hasLiberty方法，简化逻辑
    private boolean hasLiberty(int x, int y, Set<Point> group) {
        if (!isValidCoordinate(x, y)) {
            return false;
        }
        
        // 检查当前点是否是空点（气）
        if (board[x][y] == 0) {
            return true;
        }
        
        // 检查四个方向是否有空点
        int[][] directions = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (isValidCoordinate(nx, ny) && board[nx][ny] == 0) {
                return true;
            }
        }
        
        // 检查相连的同色棋子是否有气
        if (group.add(new Point(x, y))) {
            int color = board[x][y];
            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (isValidCoordinate(nx, ny) && board[nx][ny] == color && 
                    hasLiberty(nx, ny, group)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    // 修改placeStone方法中的自杀判断逻辑
    public boolean placeStone(int x, int y) {
        Log.d("GoBoard", "尝试落子: (" + x + "," + y + ")");
        
        // 添加坐标有效性检查
        if (!isValidCoordinate(x, y)) {
            Log.e("GoBoard", "无效坐标: (" + x + "," + y + ")");
            return false;
        }
        
        // 删除摆子模式相关代码块
        // 直接进入正常模式的落子逻辑
        
        // 检查位置是否已有棋子
        if (board[x][y] != 0) {
            Log.e("GoBoard", "位置已有棋子: (" + x + "," + y + ")");
            return false;
        }
        
        // 打劫规则检测
        if (lastCapture != null && lastCapture.x == x && lastCapture.y == y && 
            isSingleStoneCapture(x, y)) {
            Log.e("GoBoard", "打劫规则限制: (" + x + "," + y + ")");
            return false;
        }
        
        // 临时落子
        board[x][y] = currentPlayer;
        
        // 检查是否提掉对方的子
        boolean didCapture = checkCapture(x, y);
        
        // 检查自己的子是否有气（使用新的简化逻辑）
        Set<Point> selfGroup = new HashSet<>();
        boolean hasSelfLiberty = hasLiberty(x, y, selfGroup);
        
        // 如果自己没气且没有提子，则是自杀步
        if (!hasSelfLiberty && !didCapture) {
            board[x][y] = 0; // 恢复棋盘状态
            Log.e("GoBoard", "自杀落子: (" + x + "," + y + ")");
            return false;
        }
        
        // 正式落子
        // 正式落子
        Move newMove = new Move(x, y, currentPlayer);
        
        // 如果当前不是在最后一手，需要清除后续的变化
        if (currentMoveNumber < moveHistory.size() - 1) {
            moveHistory = new ArrayList<>(moveHistory.subList(0, currentMoveNumber + 1));
        }
        
        moveHistory.add(newMove);
        currentMoveNumber = moveHistory.size() - 1;
        
        // 切换玩家
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        
        return true;
    }
    
    // 修改checkCapture方法，完善提子逻辑
    private boolean checkCapture(int x, int y) {
        boolean didCapture = false;
        int opponentColor = (currentPlayer == 1) ? 2 : 1;
        
        // 检查四个方向
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            
            if (isValidCoordinate(nx, ny) && board[nx][ny] == opponentColor) {
                Set<Point> group = new HashSet<>();
                collectGroup(nx, ny, opponentColor, group);
                
                // 检查这个群组是否有气
                boolean hasLiberty = false;
                for (Point p : group) {
                    if (hasEmptyNeighbor(p.x, p.y)) {
                        hasLiberty = true;
                        break;
                    }
                }
                
                // 如果没有气，提掉这个群组
                if (!hasLiberty) {
                    // 记录提子前的状态（用于打劫判断）
                    if (group.size() == 1) {
                        Point p = group.iterator().next();
                        lastCapture = new Point(p.x, p.y);
                        Log.d("GoBoard", "记录打劫位置: (" + p.x + "," + p.y + ")");
                    } else {
                        lastCapture = null; // 不是单子提，清除打劫记录
                    }
                    
                    // 提掉这个群组
                    for (Point p : group) {
                        board[p.x][p.y] = 0;
                    }
                    
                    didCapture = true;
                }
            }
        }
        
        return didCapture;
    }
    
    // 在类的内部添加这些方法
    private boolean isSingleStoneCapture(int x, int y) {
        int opponentColor = (currentPlayer == 1) ? 2 : 1;
        int captureCount = 0;
        
        // 检查四个方向
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            
            if (isValidCoordinate(nx, ny) && board[nx][ny] == opponentColor) {
                Set<Point> group = new HashSet<>();
                collectGroup(nx, ny, opponentColor, group);
                
                if (group.size() == 1 && !hasLiberty(nx, ny, new HashSet<>(group))) {
                    captureCount++;
                }
            }
        }
        
        return captureCount == 1;
    }
    
    // 添加辅助方法收集棋子群
    private void collectGroup(int x, int y, int color, Set<Point> group) {
        if (!isValidCoordinate(x, y) || board[x][y] != color || 
            group.contains(new Point(x, y))) {
            return;
        }
        
        group.add(new Point(x, y));
        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] dir : directions) {
            collectGroup(x + dir[0], y + dir[1], color, group);
        }
    }

    // 添加缺失的hasEmptyNeighbor方法
    private boolean hasEmptyNeighbor(int x, int y) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (isValidCoordinate(nx, ny) && board[nx][ny] == 0) {
                return true;
            }
        }
        return false;
    }
    
    // 添加坐标有效性检查方法
    public boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    public boolean undo() {
        if (moveHistory.isEmpty()) {
            return false;
        }
        
        Move lastMove = moveHistory.remove(moveHistory.size() - 1);
        // 修正：悔棋时需要考虑坐标转换
        if (lastMove.x != -1) {  // 不是虚手
            board[lastMove.x][lastMove.y] = 0;
        }
        currentPlayer = lastMove.color;
        lastCapture = null;  // 清除打劫记录
        return true;
    }
    
    public List<Move> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }
    
    public int getCurrentPlayer() {
        return currentPlayer;
    }
    
    public int getCurrentPlayerColor() {
        return currentPlayer; // 假设已有currentPlayer字段存储当前玩家
    }
    
    public int getStone(int x, int y) {
        return board[x][y];
    }
    
    
    public Move getLastMove() {
        return moveHistory.isEmpty() ? null : moveHistory.get(moveHistory.size() - 1);
    }
    
    private int currentMoveNumber = -1; // 替换currentMoveNumber

    public void skipTurn() {
        // 创建虚手记录(x,y=-1)
        Move passMove = new Move(-1, -1, currentPlayer);
        moveHistory.add(passMove);
        currentMoveNumber = moveHistory.size() - 1;
        currentPlayer = 3 - currentPlayer; // 切换玩家
        
        // 添加虚手提示
        String passMessage = currentPlayer == 1 ? "白方虚手" : "黑方虚手";
        Log.d("GoBoard", passMessage);
    }

    public void resetBoardToCurrentMove() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i <= currentMoveNumber; i++) {
            Move move = moveHistory.get(i);
            // 添加虚手坐标检查
            if (move.x >= 0 && move.y >= 0) { // 只处理有效坐标
                board[move.x][move.y] = move.color;
            }
        }
    }

    public static String unescapeSGFText(String text) {
        return text.replace("\\\\", "\\")
                  .replace("\\]", "]")
                  .replace("\\[", "[")
                  .replace("\\n", "\n");
    }
    
    public int getCurrentMoveNumber() {
        return currentMoveNumber;
    }

    public void setCurrentMoveNumber(int number) {
        // 修正：设置当前手数时需要重置棋盘
        currentMoveNumber = Math.min(Math.max(number, -1), moveHistory.size()-1);
        resetBoardToCurrentMove();
    }
    
    public boolean previousMove() {
        if (currentMoveNumber >= 0) {  // 修改为currentMoveNumber
            currentMoveNumber--;       // 修改为currentMoveNumber
            resetBoardToCurrentMove();
            return true;
        }
        return false;
    }
    
    public boolean nextMove() {
        if (currentMoveNumber >= moveHistory.size() - 1) {
            return false;
        }
        currentMoveNumber++;
        resetBoardToCurrentMove();  // 确保调用这个方法
        return true;
    }
    
    public void loadFromSGF(String sgf) throws InvalidSGFException {
        // 清空当前状态
        resetGame();
        
        // 解析头信息
        Pattern headerPattern = Pattern.compile(
            "(PB|PW|RE|DT)\\[(.*?)\\]", 
            Pattern.DOTALL
        );
        Matcher headerMatcher = headerPattern.matcher(sgf);
        
        while (headerMatcher.find()) {
            String key = headerMatcher.group(1);
            String value = unescapeSGFText(headerMatcher.group(2));
            
            switch (key) {
                case "PB": setBlackPlayer(value); break;
                case "PW": setWhitePlayer(value); break;
                case "RE": setResult(value); break;
                case "DT": setDate(value); break;
            }
        }
        
        // 解析棋步
        // 修改棋步解析逻辑，同时处理注释
        Pattern movePattern = Pattern.compile(
            ";(?<color>[BW])\\[(?<coord>[a-s]{0,2})\\](?:C\\[(?<comment>.*?)\\])?", 
            Pattern.DOTALL
        );
        Matcher matcher = movePattern.matcher(sgf);
        
        while (matcher.find()) {
            int color = matcher.group("color").equals("B") ? 1 : 2;
            String coord = matcher.group("coord");
            String comment = matcher.group("comment");
            
            Move currentMove;
            if (coord.isEmpty()) {
                currentMove = new Move(-1, -1, color); // 虚手处理
            } else {
                int x = coord.charAt(0) - 'a';
                int y = coord.charAt(1) - 'a';
                currentMove = new Move(x, y, color);
            }
            
            // 设置注释
            if (comment != null) {
                currentMove.comment = unescapeSGFText(comment);
            }
            
            moveHistory.add(currentMove);
            currentPlayer = 3 - color;
            
            // 如果是实际落子则更新棋盘
            if (currentMove.x != -1) {
                board[currentMove.x][currentMove.y] = color;
            }
        }
        
        // 重置到当前手数
        currentMoveNumber = moveHistory.size() - 1;
        resetBoardToCurrentMove();
    }
    
    // 将以下方法移入类内部
    // 添加公共访问方法
    public String getBlackPlayer() {
        return blackPlayer;
    }

    public String getWhitePlayer() {
        return whitePlayer; 
    }

    public String getResult() {
        return result;
    }

    public void setBlackPlayer(String name) {  // 改为public
        this.blackPlayer = name; 
    }

    // 添加reset方法
    // 修改resetGame方法
    public void resetGame() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        moveHistory.clear();
        currentPlayer = 1;
        blackPlayer = "";
        whitePlayer = "";
        result = "";
        currentMoveNumber = -1;  // 使用currentMoveNumber替换currentMoveNumber
    }
    public void setWhitePlayer(String name) { 
        this.whitePlayer = name; 
    }

    public void setResult(String result) { 
        this.result = result; 
    }

    public void setDate(String date) { 
        this.date = date; 
    }
    
    public Move getMoveAt(int x, int y) {
        for (Move move : moveHistory) {
            if (move.x == x && move.y == y) {
                return move;
            }
        }
        return null;
    }
    
    public boolean hasBranch(int x, int y) {
        Move move = getMoveAt(x, y);
        return move != null && !move.variations.isEmpty();
    }
    
    // 添加以下方法
    // 修改所有currentMove为currentMoveNumber（共8处）
    public List<GoBoard.Move> getCurrentVariations() {
        if (currentMoveNumber < 0 || currentMoveNumber >= moveHistory.size()) {
            return Collections.emptyList();
        }
        return moveHistory.get(currentMoveNumber).variations.get(0);
    }
    
    public boolean selectVariation(int index) {
        if (currentMoveNumber >= 0 && currentMoveNumber < moveHistory.size()) {
            Move current = moveHistory.get(currentMoveNumber);
            if (index >= 0 && index < current.variations.size()) {
                moveHistory = new ArrayList<>(moveHistory.subList(0, currentMoveNumber + 1));
                moveHistory.addAll(current.variations.get(index));
                return true;
            }
        }
        return false;
    }
    
    public Move getCurrentMove() {
        if (currentMoveNumber >= 0 && currentMoveNumber < moveHistory.size()) {
            return moveHistory.get(currentMoveNumber);
        }
        return null;
    }
    
    // 将这些方法移到类内部
    public String getComment() {
        Move currentMove = getCurrentMove();
        if (currentMove != null) {
            return currentMove.comment.replace("\\\\", "\\")
                                   .replace("\\]", "]")
                                   .replace("\\[", "[")
                                   .replace("\\n", "\n");
        }
        return "";
    }

    public void setComment(String comment) {
        Move currentMove = getCurrentMove();
        if (currentMove != null) {
            currentMove.comment = comment.replace("\\", "\\\\")
                                   .replace("]", "\\]")
                                   .replace("[", "\\[")
                                   .replace("\n", "\\n");
        }
    }

    // 添加切换当前棋子颜色的方法（用于摆子模式）
    public void toggleCurrentPlayer() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }



public String toSGFString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(;GM[1]FF[4]");
    
    // 添加头信息
    if (blackPlayer != null && !blackPlayer.isEmpty()) {
        sb.append("PB[").append(escapeSGFText(blackPlayer)).append("]");
    }
    if (whitePlayer != null && !whitePlayer.isEmpty()) {
        sb.append("PW[").append(escapeSGFText(whitePlayer)).append("]");
    }
    if (result != null && !result.isEmpty()) {
        sb.append("RE[").append(escapeSGFText(result)).append("]");
    }
    
    // 添加棋步
    for (Move move : moveHistory) {
        String color = move.color == 1 ? "B" : "W";
        if (move.x == -1) { // 虚手
            sb.append(";").append(color).append("[]");
        } else {
            sb.append(";").append(color)
              .append("[").append((char)('a'+move.x))
              .append((char)('a'+move.y)).append("]");
        }
        // 添加注释
        if (move.comment != null && !move.comment.isEmpty()) {
            sb.append("C[").append(escapeSGFText(move.comment)).append("]");
        }
    }
    sb.append(")");
    return sb.toString();
}

private String escapeSGFText(String text) {
    return text.replace("\\", "\\\\")
              .replace("]", "\\]")
              .replace("[", "\\[")
              .replace("\n", "\\n");
}
} // 类结束的大括号