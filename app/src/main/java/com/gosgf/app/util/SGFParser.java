package com.gosgf.app.util;

import android.util.Log; // 添加这行导入语句
import com.gosgf.app.model.GoBoard;
import java.util.HashMap;
import java.util.Map;
import com.gosgf.app.model.InvalidSGFException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;
import java.util.List; // 添加这行导入
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.graphics.Point;

public class SGFParser {
    
    public static String saveToString(GoBoard board, String blackPlayer, String whitePlayer, String result) {
        StringBuilder sb = new StringBuilder();
        sb.append("(;FF[4]GM[1]SZ[19]\n");
        
        // 添加元数据
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sb.append("DT[").append(sdf.format(new Date())).append("]\n");
        
        // 添加玩家信息
        sb.append("PB[").append(escapeSGFText(blackPlayer)).append("]\n")
          .append("PW[").append(escapeSGFText(whitePlayer)).append("]\n")
          .append("RE[").append(escapeSGFText(result)).append("]\n")
          .append("KM[6.5]\n")  // 添加贴目信息
          .append("RU[Chinese]\n"); // 添加规则信息
        
        // 生成完整棋步（包含注释和变化图）
        List<GoBoard.Move> moves = board.getMoveHistory();
        for (int i = 0; i < moves.size(); i++) {
            GoBoard.Move move = moves.get(i);
            char color = (move.color == 1) ? 'B' : 'W';
            
            // 添加主线落子
            if (move.x == -1) {
                sb.append(";").append(color).append("[]");
            } else {
                char x = (char) ('a' + move.x);
                char y = (char) ('a' + move.y);
                sb.append(";").append(color).append("[").append(x).append(y).append("]");
            }
            
            // 添加注释
            if (move.comment != null && !move.comment.isEmpty()) {
                sb.append("C[").append(escapeSGFText(move.comment)).append("]");
            }
            
            // 添加变化图
            if (!move.variations.isEmpty()) {
                for (List<GoBoard.Move> variation : move.variations) {
                    sb.append("\n(");
                    for (GoBoard.Move varMove : variation) {
                        char varColor = (varMove.color == 1) ? 'B' : 'W';
                        if (varMove.x == -1) {
                            sb.append(";").append(varColor).append("[]");
                        } else {
                            char varX = (char) ('a' + varMove.x);
                            char varY = (char) ('a' + varMove.y);
                            sb.append(";").append(varColor).append("[").append(varX).append(varY).append("]");
                            
                            // 添加变化图中的注释
                            if (varMove.comment != null && !varMove.comment.isEmpty()) {
                                sb.append("C[").append(escapeSGFText(varMove.comment)).append("]");
                            }
                        }
                    }
                    sb.append(")");
                }
            }
            
            sb.append("\n");
        }
        
        sb.append(")");
        return sb.toString();
    }

    public static void parseSGF(String sgf, GoBoard board) throws InvalidSGFException {
        board.resetGame();
        Parser p = new Parser(sgf);
        List<List<Node>> trees = p.parseCollection();
        if (trees.isEmpty()) return;
        
        // 解析所有根分支为Move列表
        List<List<GoBoard.Move>> allRootMoves = new java.util.ArrayList<>();
        for (List<Node> tree : trees) {
            List<GoBoard.Move> moves = toMovesDeep(tree);
            allRootMoves.add(moves);
        }
        
        // 寻找默认路线：选择最长的根分支作为主分支
        int mainTreeIndex = 0;
        int maxLength = 0;
        for (int i = 0; i < allRootMoves.size(); i++) {
            int length = getTotalMoves(allRootMoves.get(i));
            if (length > maxLength) {
                maxLength = length;
                mainTreeIndex = i;
            }
        }
        
        // 从默认主分支应用头信息和初始设置
        List<Node> mainTree = trees.get(mainTreeIndex);
        applyHeader(mainTree, board);
        applyRootSetup(mainTree, board);
        board.snapshotInitialSetup();
        
        // 构建主分支
        List<GoBoard.Move> mainMoves = new java.util.ArrayList<>();
        
        // 使用默认主分支作为主分支
        if (!allRootMoves.get(mainTreeIndex).isEmpty()) {
            mainMoves.addAll(allRootMoves.get(mainTreeIndex));
        } else {
            // 如果默认主分支为空，创建一个虚手Move作为容器
            GoBoard.Move emptyMove = new GoBoard.Move(-1, -1, 1);
            mainMoves.add(emptyMove);
        }
        
        // 将其他根分支作为第一个Move的variations
        for (int i = 0; i < allRootMoves.size(); i++) {
            if (i == mainTreeIndex) continue; // 跳过主分支
            List<GoBoard.Move> rootMoves = allRootMoves.get(i);
            if (!rootMoves.isEmpty()) {
                mainMoves.get(0).variations.add(rootMoves);
            }
        }
        
        // 如果默认主分支本身有variations，也要添加到第一个Move
        if (!mainTree.isEmpty() && mainTree.get(0).variations != null && !mainTree.get(0).variations.isEmpty()) {
            for (List<Node> var : mainTree.get(0).variations) {
                List<GoBoard.Move> varMoves = toMovesDeep(var);
                if (!varMoves.isEmpty()) {
                    mainMoves.get(0).variations.add(varMoves);
                }
            }
        }
        
        board.setMoveHistory(mainMoves);
        // Set to start of game when loading (no moves should be displayed)
        board.setCurrentMoveNumber(-1);
    }
    
    // 计算分支的总手数（包括所有子分支）
    private static int getTotalMoves(List<GoBoard.Move> moves) {
        if (moves == null || moves.isEmpty()) return 0;
        int count = moves.size();
        for (GoBoard.Move move : moves) {
            for (List<GoBoard.Move> variation : move.variations) {
                count += getTotalMoves(variation);
            }
        }
        return count;
    }

    private static String escapeSGFText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\\", "\\\\")
                  .replace("]", "\\]")
                  .replace("[", "\\[") 
                  .replace("\n", "\\n");
    }

    private static String generateMoveString(GoBoard.Move move) {
        if (move.x == -1 && move.y == -1) {
            return ";" + (move.color == 1 ? "B" : "W") + "[]";
        }
        return String.format(";%s[%s%s]", 
            (move.color == 1 ? "B" : "W"),
            (char) ('a' + move.x),
            (char) ('a' + move.y));
    }

    private static void generateMovesWithVariations(StringBuilder sb, List<GoBoard.Move> moves, int depth) {
        for (GoBoard.Move move : moves) {
            sb.append("\n").append(generateMoveString(move));
            
            // 处理分支
            if (!move.variations.isEmpty()) {
                for (List<GoBoard.Move> variation : move.variations) {
                    sb.append("\n("); // 分支开始
                    generateMainVariation(sb, variation);
                    sb.append("\n)"); // 分支结束
                }
            }
        }
    }

    private static void generateMainVariation(StringBuilder sb, List<GoBoard.Move> variation) {
        for (GoBoard.Move move : variation) {
            sb.append(generateMoveString(move));
            if (!move.variations.isEmpty()) {
                for (List<GoBoard.Move> subVariation : move.variations) {
                    sb.append("\n(");
                    generateMainVariation(sb, subVariation);
                    sb.append(")");
                }
            }
        }
    }

    private static void applyHeader(List<Node> seq, GoBoard board) {
        if (seq.isEmpty()) return;
        Node root = seq.get(0);
        String pb = first(root.props.get("PB"));
        String pw = first(root.props.get("PW"));
        String re = first(root.props.get("RE"));
        String dt = first(root.props.get("DT"));
        String sz = first(root.props.get("SZ"));
        String ha = first(root.props.get("HA"));
        String pl = first(root.props.get("PL"));
        if (pb != null) board.setBlackPlayer(board.unescapeSGFText(pb));
        if (pw != null) board.setWhitePlayer(board.unescapeSGFText(pw));
        if (re != null) board.setResult(board.unescapeSGFText(re));
        if (dt != null) board.setDate(board.unescapeSGFText(dt));
        if (sz != null) {
            try {
                int s = Integer.parseInt(sz);
                if (s != 19) Log.w("SGFParser", "不支持非19路棋盘");
            } catch (Exception ignored) {}
        }
        if (ha != null) {
            try {
                int n = Integer.parseInt(ha);
                List<String> ab = root.props.get("AB");
                if ((ab == null || ab.isEmpty()) && n > 0) {
                    placeDefaultHandicap19(n, board);
                }
                if (n > 0) {
                    board.setCurrentPlayer(2);
                }
            } catch (Exception ignored) {}
        }
        if (pl != null) {
            if (pl.equals("B")) board.setCurrentPlayer(1);
            else if (pl.equals("W")) board.setCurrentPlayer(2);
        }
    }

    private static void applyRootSetup(List<Node> seq, GoBoard board) {
        if (seq.isEmpty()) return;
        Node n = seq.get(0);
        List<String> ab = n.props.get("AB");
        if (ab != null) {
            for (String v : ab) {
                for (Point pt : expandPoints(v)) {
                    board.setupStone(pt.x, pt.y, 1);
                }
            }
        }
        List<String> aw = n.props.get("AW");
        if (aw != null) {
            for (String v : aw) {
                for (Point pt : expandPoints(v)) {
                    board.setupStone(pt.x, pt.y, 2);
                }
            }
        }
        List<String> ae = n.props.get("AE");
        if (ae != null) {
            for (String v : ae) {
                for (Point pt : expandPoints(v)) {
                    board.removeStone(pt.x, pt.y);
                }
            }
        }
    }

    private static List<GoBoard.Move> toMovesDeep(List<Node> seq) {
        List<GoBoard.Move> moves = new java.util.ArrayList<>();
        for (Node n : seq) {
            List<String> b = n.props.get("B");
            List<String> w = n.props.get("W");
            String cmt = first(n.props.get("C"));
            GoBoard.Move m = null;
            if (b != null) {
                String v = b.isEmpty() ? "" : b.get(0);
                m = coordToMove(v, 1);
            } else if (w != null) {
                String v = w.isEmpty() ? "" : w.get(0);
                m = coordToMove(v, 2);
            }
            if (m != null) {
                if (cmt != null) m.comment = cmt;
                if (n.variations != null && !n.variations.isEmpty()) {
                    for (List<Node> var : n.variations) {
                        List<GoBoard.Move> vMoves = toMovesDeep(var);
                        m.variations.add(vMoves);
                    }
                }
                moves.add(m);
            }
        }
        return moves;
    }

    private static GoBoard.Move coordToMove(String coord, int color) {
        if (coord == null || coord.isEmpty()) return new GoBoard.Move(-1, -1, color);
        // 确保坐标只包含前两个字符（去除可能的额外字符）
        String cleanCoord = coord.trim();
        if (cleanCoord.length() < 2) return new GoBoard.Move(-1, -1, color);
        int x = cleanCoord.charAt(0) - 'a';
        int y = cleanCoord.charAt(1) - 'a';
        return new GoBoard.Move(x, y, color);
    }

    private static String first(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private static List<Point> expandPoints(String v) {
        List<Point> pts = new java.util.ArrayList<>();
        if (v == null) return pts;
        if (v.length() == 2) {
            pts.add(new Point(v.charAt(0) - 'a', v.charAt(1) - 'a'));
        } else if (v.length() == 5 && v.charAt(2) == ':') {
            int x1 = v.charAt(0) - 'a';
            int y1 = v.charAt(1) - 'a';
            int x2 = v.charAt(3) - 'a';
            int y2 = v.charAt(4) - 'a';
            int xa = Math.min(x1, x2), xb = Math.max(x1, x2);
            int ya = Math.min(y1, y2), yb = Math.max(y1, y2);
            for (int x = xa; x <= xb; x++) {
                for (int y = ya; y <= yb; y++) {
                    pts.add(new Point(x, y));
                }
            }
        }
        return pts;
    }
    private static void placeDefaultHandicap19(int n, GoBoard board) {
        List<Point> order = new java.util.ArrayList<>();
        order.add(new Point(15, 3));
        order.add(new Point(3, 15));
        order.add(new Point(15, 15));
        order.add(new Point(3, 3));
        order.add(new Point(9, 9));
        order.add(new Point(9, 3));
        order.add(new Point(9, 15));
        order.add(new Point(3, 9));
        order.add(new Point(15, 9));
        int m = Math.min(n, order.size());
        for (int k = 0; k < m; k++) {
            Point p = order.get(k);
            board.setupStone(p.x, p.y, 1);
        }
    }

    private static class Node {
        Map<String, List<String>> props = new HashMap<>();
        List<List<Node>> variations = new java.util.ArrayList<>();
    }

    private static class Parser {
        private final String s;
        private int i = 0;
        Parser(String s) { this.s = s; }
        List<List<Node>> parseCollection() {
            List<List<Node>> trees = new java.util.ArrayList<>();
            skipWs();
            while (i < s.length()) {
                if (s.charAt(i) == '(') {
                    trees.add(parseGameTree());
                } else i++;
                skipWs();
            }
            return trees;
        }
        List<Node> parseGameTree() {
            i++;
            List<Node> seq = new java.util.ArrayList<>();
            while (i < s.length()) {
                char ch = s.charAt(i);
                if (ch == ';') {
                    seq.add(parseNode());
                } else if (ch == '(') {
                    List<Node> var = parseGameTree();
                    if (!seq.isEmpty()) {
                        seq.get(seq.size()-1).variations.add(var);
                    }
                } else if (ch == ')') {
                    i++;
                    break;
                } else {
                    i++;
                }
            }
            return seq;
        }
        Node parseNode() {
            i++;
            Node n = new Node();
            skipWs();
            while (i < s.length()) {
                char ch = s.charAt(i);
                if (ch == ';' || ch == '(' || ch == ')') break;
                String ident = parseIdent();
                List<String> values = new java.util.ArrayList<>();
                skipWs();
                while (i < s.length() && s.charAt(i) == '[') {
                    values.add(parseValue());
                    skipWs();
                }
                n.props.put(ident, values);
                skipWs();
            }
            return n;
        }
        String parseIdent() {
            int start = i;
            while (i < s.length()) {
                char ch = s.charAt(i);
                if (ch >= 'A' && ch <= 'Z') i++; else break;
            }
            return s.substring(start, i);
        }
        String parseValue() {
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char ch = s.charAt(i);
                if (ch == '\\') {
                    if (i + 1 < s.length()) {
                        sb.append(s.charAt(i + 1));
                        i += 2;
                    } else {
                        i++;
                    }
                } else if (ch == ']') {
                    i++;
                    break;
                } else {
                    sb.append(ch);
                    i++;
                }
            }
            return sb.toString().trim();
        }
        void skipWs() {
            while (i < s.length()) {
                char ch = s.charAt(i);
                if (Character.isWhitespace(ch)) i++; else break;
            }
        }
    }
}
