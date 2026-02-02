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
// 添加SGFParser导入
import com.gosgf.app.util.SGFParser;
import com.gosgf.app.util.SGFConverter;
import java.util.Map;
import java.util.HashMap;

public class GoBoard {
    
    // Variation 类：封装分支信息
    public static class Variation {
        private List<Move> moves;
        private String name;
        
        public Variation(List<Move> moves, String name) {
            this.moves = moves;
            this.name = name;
        }
        
        public List<Move> getMoves() {
            return moves;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int size() {
            return moves.size();
        }
        
        // 检查是否与另一个分支相同
        public boolean isSameAs(Variation other) {
            if (other == null || other.moves.size() != this.moves.size()) {
                return false;
            }
            for (int i = 0; i < moves.size(); i++) {
                if (!moves.get(i).isSameAs(other.moves.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }
    // 添加字段声明
    private String blackPlayer;
    private String whitePlayer;
    private String result;
    private String date;
    private static final int BOARD_SIZE = 19;
    private int[][] board;  // 0=空, 1=黑, 2=白
    private int[][] initialBoard;
    private List<Move> moveHistory;
    private List<Move> mainBranchHistory;
    private int currentPlayer; // 1=黑, 2=白
    private Point lastCapture = null; // 记录最后一次提子的位置
    private boolean isBoardLocked = false; // 棋盘锁定标志，true表示棋盘已固定，不允许修改
    
    public static class Move {
        public int x;
        public int y;
        public int color;  // 1=黑, 2=白
        public String comment;
        public List<Variation> variations = new ArrayList<>();
        
        // 标记类型: 0=无, 1=三角形, 2=方形, 3=圆形, 4=X标记, 5=数字, 6=字母, 7=正方形, 8=三角形, 9=圆形, 10=叉号
        public int markType = 0;
        // 标签文本
        public String label = "";
        
        public Move(int x, int y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
        
        // 检查是否与另一个移动相同
        public boolean isSameAs(Move other) {
            if (other == null) return false;
            return this.x == other.x && this.y == other.y && this.color == other.color;
        }
        
        // 检查是否包含相同的分支
        public boolean hasSameVariation(List<Move> branch) {
            for (Variation existingVar : variations) {
                if (existingVar.getMoves().size() == branch.size()) {
                    boolean same = true;
                    for (int i = 0; i < existingVar.getMoves().size(); i++) {
                        if (!existingVar.getMoves().get(i).isSameAs(branch.get(i))) {
                            same = false;
                            break;
                        }
                    }
                    if (same) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        // 添加分支
        public void addVariation(List<Move> moves, String name) {
            variations.add(new Variation(moves, name));
        }
        
        // 设置分支名称
        public void setVariationName(int index, String name) {
            if (index >= 0 && index < variations.size()) {
                variations.get(index).setName(name);
            }
        }
        
        // 获取分支名称
        public String getVariationName(int index) {
            if (index >= 0 && index < variations.size()) {
                return variations.get(index).getName();
            }
            return "";
        }
        
        // 获取分支
        public List<Move> getVariation(int index) {
            if (index >= 0 && index < variations.size()) {
                return variations.get(index).getMoves();
            }
            return null;
        }
    }

    // 添加 getStoneAt 方法
    public int getStoneAt(int x, int y) {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            return -1; // 返回-1表示无效坐标
        }
        return board[x][y];
    }
    
    public void setupStone(int x, int y, int color) {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            return;
        }
        board[x][y] = color;
        initialBoard[x][y] = color; // 同时更新初始棋盘，确保重置时座子不丢失
    }
    
    public void removeStone(int x, int y) {
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            return;
        }
        board[x][y] = 0;
    }
    
    public void setCurrentPlayer(int color) {
        if (color == 1 || color == 2) {
            currentPlayer = color;
        }
    }
    
    // 移除setComment方法
    // public void setComment(Move move, String comment) {...}
    
    public GoBoard() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        initialBoard = new int[BOARD_SIZE][BOARD_SIZE];
        moveHistory = new ArrayList<>();
        mainBranchHistory = new ArrayList<>();
        currentPlayer = 1; // 黑子先行
        blackPlayer = "";
        whitePlayer = "";
        result = "";
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
    
    // 设置棋盘锁定状态
    public void setBoardLocked(boolean locked) {
        this.isBoardLocked = locked;
    }
    
    // 获取棋盘锁定状态
    public boolean isBoardLocked() {
        return isBoardLocked;
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
        Move newMove = new Move(x, y, currentPlayer);
        
        // 如果当前是棋局开始状态（currentMoveNumber = -1），处理第一手分支
        if (currentMoveNumber == -1) {
            // 如果已经有 moveHistory，将新的第一手作为一个新的分支添加到 startVariations 中
            if (!moveHistory.isEmpty()) {
                // 保存当前主线作为一个分支（如果不存在）
                boolean exists = false;
                for (Variation existingVar : startVariations) {
                    List<Move> existingMoves = existingVar.getMoves();
                    if (existingMoves.size() == moveHistory.size()) {
                        boolean same = true;
                        for (int i = 0; i < existingMoves.size(); i++) {
                            Move existingMove = existingMoves.get(i);
                            Move currentMove = moveHistory.get(i);
                            if (existingMove.x != currentMove.x || existingMove.y != currentMove.y || existingMove.color != currentMove.color) {
                                same = false;
                                break;
                            }
                        }
                        if (same) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    startVariations.add(new Variation(new ArrayList<>(moveHistory), "分支"));
                }
                
                // 创建一个新的分支，只包含新的第一手
                List<Move> newBranch = new ArrayList<>();
                newBranch.add(newMove);
                
                // 检查是否已存在相同的分支
                boolean newBranchExists = false;
                for (Variation existingVar : startVariations) {
                    List<Move> existingMoves = existingVar.getMoves();
                    if (existingMoves.size() == 1) {
                        Move existingMove = existingMoves.get(0);
                        if (existingMove.x == newMove.x && existingMove.y == newMove.y && existingMove.color == newMove.color) {
                            newBranchExists = true;
                            break;
                        }
                    }
                }
                
                if (!newBranchExists) {
                    String branchName = "分支 " + (startVariations.size() + 1);
                    startVariations.add(new Variation(newBranch, branchName));
                }
                
                // 将新的第一手设为主线，允许继续落子
                moveHistory = newBranch;
                currentMoveNumber = 0;
                mainBranchHistory = new ArrayList<>(newBranch);
                
                // 切换玩家
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
                
                return true;
            }
        }
        // 如果当前不是在最后一手，保留后续为分支并截断主线
        else if (currentMoveNumber >= 0 && currentMoveNumber < moveHistory.size() - 1) {
            Move base = moveHistory.get(currentMoveNumber);
            List<Move> remainder = new ArrayList<>(moveHistory.subList(currentMoveNumber + 1, moveHistory.size()));
            if (!remainder.isEmpty()) {
                base.variations.add(new Variation(remainder, "分支"));
            }
            moveHistory = new ArrayList<>(moveHistory.subList(0, currentMoveNumber + 1));
        }
        
        moveHistory.add(newMove);
        currentMoveNumber = moveHistory.size() - 1;
        
        // 更新主分支历史：如果我们在主分支末尾，则添加
        if (mainBranchHistory.isEmpty() || currentMoveNumber == mainBranchHistory.size()) {
            mainBranchHistory.add(newMove);
        }
        
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
    
    public void addMoveToHistory(Move move) {
        moveHistory.add(move);
    }
    
    public void setMoveHistory(List<Move> moves) {
        moveHistory = new ArrayList<>(moves);
        currentMoveNumber = -1; // 加载棋局时指针停留在第一手前
        resetBoardToCurrentMove();
    }
    
    public void snapshotInitialSetup() {
        initialBoard = copyBoard(board);
    }
    
    private int[][] copyBoard(int[][] src) {
        int[][] dst = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(src[i], 0, dst[i], 0, BOARD_SIZE);
        }
        return dst;
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
    private List<Variation> startVariations = new ArrayList<>(); // 存储起始分支信息

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
        board = copyBoard(initialBoard);
        for (int i = 0; i <= currentMoveNumber; i++) {
            Move move = moveHistory.get(i);
            replayMove(move);
        }
    }
    
    private void replayMove(Move move) {
        if (move == null) return;
        if (move.x < 0 || move.y < 0) {
            currentPlayer = move.color;
            currentPlayer = 3 - currentPlayer;
            return;
        }
        if (!isValidCoordinate(move.x, move.y)) return;
        if (board[move.x][move.y] != 0) return;
        currentPlayer = move.color;
        board[move.x][move.y] = currentPlayer;
        checkCapture(move.x, move.y);
        Set<Point> selfGroup = new HashSet<>();
        boolean hasSelfLiberty = hasLiberty(move.x, move.y, selfGroup);
        if (!hasSelfLiberty) {
            board[move.x][move.y] = 0;
        }
        currentPlayer = 3 - currentPlayer;
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
        // When at start of game (currentMoveNumber = -1), set player to black (1)
        // Unless there are setup stones, in which case keep the current player
        if (currentMoveNumber == -1) {
            // Check if there are setup stones by examining initialBoard
            boolean hasSetupStones = false;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (initialBoard[i][j] != 0) {
                        hasSetupStones = true;
                        break;
                    }
                }
                if (hasSetupStones) break;
            }
            // Only set to black if there are no setup stones
            if (!hasSetupStones) {
                currentPlayer = 1; // Black's turn at start
            }
        }
    }
    
    public boolean previousMove() {
        if (currentMoveNumber >= 0) {
            currentMoveNumber--;
            resetBoardToCurrentMove();
            return true;
        }
        return false;
    }
    
    public boolean nextMove() {
        // 起始态：如果有起始分支，选择第一个分支
        if (currentMoveNumber < 0) {
            if (!startVariations.isEmpty()) {
                return selectVariation(0);
            }
            // 如果没有起始分支，检查 moveHistory
            if (!moveHistory.isEmpty()) {
                currentMoveNumber = 0;
                resetBoardToCurrentMove();
                return true;
            }
            return false;
        }
        
        // 检查是否可以继续前进
        boolean canContinue = currentMoveNumber < moveHistory.size() - 1;
        
        // 检查当前手是否有分支
        boolean hasBranch = false;
        if (currentMoveNumber >= 0 && currentMoveNumber < moveHistory.size()) {
            Move currentMove = moveHistory.get(currentMoveNumber);
            hasBranch = !currentMove.variations.isEmpty();
        }
        
        // 如果不能继续前进且没有分支，返回false
        if (!canContinue && !hasBranch) {
            return false;
        }
        
        // 如果有分支，选择第一个分支
        if (hasBranch) {
            return selectVariation(0);
        }
        
        // 直接前进到下一步
        currentMoveNumber++;
        resetBoardToCurrentMove();
        return true;
    }
    
    public void loadFromSGF(String sgf) throws SGFParser.SGFParseException {
        // 使用 SGFParser 来解析 SGF 文件
        SGFParser.parseSGF(sgf, this);
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
        initialBoard = new int[BOARD_SIZE][BOARD_SIZE];
        moveHistory.clear();
        startVariations.clear(); // 重置起始分支信息
        mainBranchHistory = new ArrayList<>();
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
    
    public String getDate() {
        return date;
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
        if (moveHistory.get(currentMoveNumber).variations.isEmpty()) {
            return Collections.emptyList();
        }
        return moveHistory.get(currentMoveNumber).variations.get(0).getMoves();
    }
    
    public boolean hasCurrentVariations() {
        if (currentMoveNumber < 0 || currentMoveNumber >= moveHistory.size()) {
            return false;
        }
        return !moveHistory.get(currentMoveNumber).variations.isEmpty();
    }
    
    public int getCurrentVariationCount() {
        if (currentMoveNumber < 0 || currentMoveNumber >= moveHistory.size()) {
            return 0;
        }
        return moveHistory.get(currentMoveNumber).variations.size();
    }
    
    public boolean hasStartVariations() {
        return !startVariations.isEmpty();
    }
    
    public int getStartVariationsCount() {
        return startVariations.size();
    }
    
    public void addStartVariation(List<Move> moves, String name) {
        startVariations.add(new Variation(moves, name));
    }
    
    public boolean removeStartVariation(int index) {
        if (startVariations.isEmpty()) return false;
        if (index < 0 || index >= startVariations.size()) return false;
        startVariations.remove(index);
        return true;
    }
    
    public boolean removeCurrentVariation(int index) {
        if (currentMoveNumber < 0 || currentMoveNumber >= moveHistory.size()) {
            return false;
        }
        Move current = moveHistory.get(currentMoveNumber);
        if (current.variations.isEmpty()) return false;
        if (index < 0 || index >= current.variations.size()) return false;
        current.variations.remove(index);
        return true;
    }
    
    public boolean selectVariation(int index) {
        // 起始态选择分支：直接将主线替换为选择的分支，定位到分支第一步
        if (currentMoveNumber < 0) {
            if (startVariations.isEmpty()) return false;
            if (index < 0 || index >= startVariations.size()) return false;
                Variation variation = startVariations.get(index);
                List<Move> vMoves = variation.getMoves();
                if (!validateBranchFirstStep(vMoves)) {
                    Log.e("GoBoard", "分支第一步解析失败（起始态）");
                    return false;
                }
                
                // 保存当前主线作为一个分支（Sabaki风格）
                if (!moveHistory.isEmpty()) {
                    // 检查是否已存在相同的分支
                    boolean exists = false;
                    for (Variation existingVar : startVariations) {
                        List<Move> existingMoves = existingVar.getMoves();
                        if (existingMoves.size() == moveHistory.size()) {
                            boolean same = true;
                            for (int i = 0; i < existingMoves.size(); i++) {
                                Move existingMove = existingMoves.get(i);
                                Move currentMove = moveHistory.get(i);
                                if (existingMove.x != currentMove.x || existingMove.y != currentMove.y || existingMove.color != currentMove.color) {
                                    same = false;
                                    break;
                                }
                            }
                            if (same) {
                                exists = true;
                                break;
                            }
                        }
                    }
                    if (!exists) {
                        startVariations.add(new Variation(moveHistory, "分支"));
                    }
                }
                
                // 将选择的分支设为主线（Sabaki风格）
                moveHistory = new ArrayList<>();
                moveHistory.addAll(vMoves);
            currentMoveNumber = Math.min(0, moveHistory.size() - 1);
            resetBoardToCurrentMove();
            return true;
        }
        
        // 当前手态选择分支
        if (currentMoveNumber >= 0 && currentMoveNumber < moveHistory.size()) {
            Move current = moveHistory.get(currentMoveNumber);
            if (index >= 0 && index < current.variations.size()) {
                List<Move> prefix = new ArrayList<>(moveHistory.subList(0, currentMoveNumber + 1));
                Variation variation = current.variations.get(index);
                List<Move> vMoves = variation.getMoves();
                if (!validateBranchFirstStep(vMoves)) {
                    Log.e("GoBoard", "分支第一步解析失败（当前手态）");
                    return false;
                }
                
                // 保存当前后续步骤作为一个分支（Sabaki风格）
                if (currentMoveNumber < moveHistory.size() - 1) {
                    List<Move> currentRemainder = new ArrayList<>(moveHistory.subList(currentMoveNumber + 1, moveHistory.size()));
                    if (!currentRemainder.isEmpty()) {
                        // 检查是否已存在相同的分支
                        boolean exists = false;
                        for (Variation existingVar : current.variations) {
                            List<Move> existingMoves = existingVar.getMoves();
                            if (existingMoves.size() == currentRemainder.size()) {
                                boolean same = true;
                                for (int i = 0; i < existingMoves.size(); i++) {
                                    Move existingMove = existingMoves.get(i);
                                    Move currentMove = currentRemainder.get(i);
                                    if (existingMove.x != currentMove.x || existingMove.y != currentMove.y || existingMove.color != currentMove.color) {
                                        same = false;
                                        break;
                                    }
                                }
                                if (same) {
                                    exists = true;
                                    break;
                                }
                            }
                        }
                        if (!exists) {
                            current.variations.add(new Variation(currentRemainder, "分支"));
                        }
                    }
                }
                
                // 将选择的分支添加到主线
                moveHistory = prefix;
                moveHistory.addAll(vMoves);
                currentMoveNumber = Math.min(currentMoveNumber + 1, moveHistory.size() - 1);
                resetBoardToCurrentMove();
                return true;
            }
        }
        return false;
    }

    private boolean validateBranchFirstStep(List<Move> branch) {
        if (branch == null || branch.isEmpty()) return false;
        Move firstStep = branch.get(0);
        if (firstStep.x < 0 || firstStep.y < 0) {
            // 虚手作为分支第一步是允许的
            return true;
        }
        if (!isValidCoordinate(firstStep.x, firstStep.y)) {
            Log.e("GoBoard", "分支第一步坐标无效: (" + firstStep.x + "," + firstStep.y + ")");
            return false;
        }
        return true;
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
        if (currentMove != null && currentMove.comment != null) {
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
    
    // 添加标记到当前手
    public void setMark(int markType) {
        Move currentMove = getCurrentMove();
        if (currentMove != null) {
            currentMove.markType = markType;
        }
    }
    
    // 获取当前手的标记类型
    public int getMark() {
        Move currentMove = getCurrentMove();
        if (currentMove != null) {
            return currentMove.markType;
        }
        return 0;
    }
    
    // 设置标签到当前手
    public void setLabel(String label) {
        Move currentMove = getCurrentMove();
        if (currentMove != null) {
            currentMove.label = label;
        }
    }
    
    // 获取当前手的标签
    public String getLabel() {
        Move currentMove = getCurrentMove();
        if (currentMove != null) {
            return currentMove.label;
        }
        return "";
    }
    
    public void switchToMainBranch() {
        if (!mainBranchHistory.isEmpty()) {
            moveHistory = new ArrayList<>(mainBranchHistory);
            currentMoveNumber = moveHistory.size() - 1;
            resetBoardToCurrentMove();
        }
    }
    
    public int getMainBranchSize() {
        return mainBranchHistory.size();
    }

    // 添加切换当前棋子颜色的方法（用于摆子模式）
    public void toggleCurrentPlayer() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }
    
    // 添加分支相关方法
    public List<String> getVariationNames() {
        List<String> names = new ArrayList<>();
        if (currentMoveNumber >= 0 && currentMoveNumber < moveHistory.size()) {
            Move current = moveHistory.get(currentMoveNumber);
            for (int i = 0; i < current.variations.size(); i++) {
                names.add(current.getVariationName(i));
            }
        }
        return names;
    }
    
    public String getVariationStructure() {
        StringBuilder sb = new StringBuilder();
        
        // 主分支结构
        sb.append("主分支: " + moveHistory.size() + "手\n");
        
        // 分支结构
        for (int i = 0; i < moveHistory.size(); i++) {
            Move move = moveHistory.get(i);
            if (!move.variations.isEmpty()) {
                sb.append("第" + (i + 1) + "手: " + move.variations.size() + "个分支\n");
                for (int j = 0; j < move.variations.size(); j++) {
                    String name = move.getVariationName(j);
                    sb.append("  " + name + ": " + move.variations.get(j).size() + "手\n");
                }
            }
        }
        
        return sb.toString();
    }
    
    public List<List<Move>> getStartVariations() {
        List<List<Move>> result = new ArrayList<>();
        for (Variation variation : startVariations) {
            result.add(variation.getMoves());
        }
        return result;
    }
    
    public Variation getStartVariation(int index) {
        if (index < 0 || index >= startVariations.size()) {
            return null;
        }
        return startVariations.get(index);
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
            
            // 添加分支
            for (Variation variation : move.variations) {
                sb.append("(");
                for (Move varMove : variation.getMoves()) {
                    color = varMove.color == 1 ? "B" : "W";
                    if (varMove.x == -1) { // 虚手
                        sb.append(";").append(color).append("[]");
                    } else {
                        sb.append(";").append(color)
                          .append("[").append((char)('a'+varMove.x))
                          .append((char)('a'+varMove.y)).append("]");
                    }
                    // 添加注释
                    if (varMove.comment != null && !varMove.comment.isEmpty()) {
                        sb.append("C[").append(escapeSGFText(varMove.comment)).append("]");
                    }
                }
                sb.append(")");
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
