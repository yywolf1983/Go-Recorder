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

    // 增强SGF解析方法，支持更多属性和变化图
    public static void parseSGF(String sgf, GoBoard board) throws InvalidSGFException {
        // 清空当前状态
        board.resetGame();
        
        // 解析头信息
        Pattern headerPattern = Pattern.compile(
            "(\\w+)\\[(.*?)\\]", 
            Pattern.DOTALL
        );
        Matcher headerMatcher = headerPattern.matcher(sgf);
        
        Map<String, String> properties = new HashMap<>();
        
        while (headerMatcher.find()) {
            String key = headerMatcher.group(1);
            String value = board.unescapeSGFText(headerMatcher.group(2));
            properties.put(key, value);
            
            switch (key) {
                case "PB": board.setBlackPlayer(value); break;
                case "PW": board.setWhitePlayer(value); break;
                case "RE": board.setResult(value); break;
                case "DT": board.setDate(value); break;
                case "SZ": 
                    // 处理棋盘大小属性
                    try {
                        int size = Integer.parseInt(value);
                        if (size != 19) {
                            Log.w("SGFParser", "不支持非19路棋盘，将使用默认19路棋盘");
                        }
                    } catch (NumberFormatException e) {
                        Log.e("SGFParser", "无效的棋盘大小: " + value);
                    }
                    break;
                // 可以添加更多属性解析
            }
        }
        
        // 解析棋步和变化图（增强版）
        parseMainLine(sgf, board, 0);
    }
    
    // 增强解析主线方法，支持递归解析变化图
    private static int parseMainLine(String sgf, GoBoard board, int startPos) {
        int pos = startPos;
        
        Pattern movePattern = Pattern.compile(
            ";(?<color>[BW])\\[(?<coord>[a-s]{0,2})\\](?:C\\[(?<comment>.*?)\\])?",
            Pattern.DOTALL
        );
        
        Matcher matcher = movePattern.matcher(sgf);
        while (matcher.find()) {
            try {
                int color = matcher.group("color").equals("B") ? 1 : 2;
                String coord = matcher.group("coord");
                String comment = matcher.group("comment");
                
                if (coord.isEmpty()) {
                    // 使用 GoBoard 的方法而不是直接访问字段
                    board.skipTurn();
                    if (comment != null) {
                        board.setComment(board.unescapeSGFText(comment));
                    }
                } else {
                    int x = coord.charAt(0) - 'a';
                    int y = coord.charAt(1) - 'a';
                    if (board.isValidCoordinate(x, y)) {
                        board.placeStone(x, y);
                        if (comment != null) {
                            board.setComment(board.unescapeSGFText(comment));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SGFParser", "解析棋步时出错: " + e.getMessage());
            }
        }
        return pos;
    }
    
    // 添加查找匹配括号的辅助方法
    private static int findMatchingClosingBracket(String text, int openPos) {
        int count = 1;
        for (int i = openPos + 1; i < text.length(); i++) {
            if (text.charAt(i) == '(') {
                count++;
            } else if (text.charAt(i) == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String escapeSGFText(String text) {
        // 增强空值处理
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
}