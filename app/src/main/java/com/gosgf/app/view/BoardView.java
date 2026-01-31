package com.gosgf.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.Log;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import com.gosgf.app.model.GoBoard;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import androidx.core.content.ContextCompat;
import com.gosgf.app.R;
import com.gosgf.app.MainActivity;

// 正确声明应保持：
public class BoardView extends View {
    private static final int BOARD_SIZE = 19;
    private static final float MARGIN_PERCENT = 0.08f; // 减小边距占比到8%
    private static final float STONE_RADIUS_RATIO = 0.42f; // 减小棋子半径比例
    
    private Paint boardPaint;
    private Paint stonePaint;
    private Paint stoneShadowPaint;
    private Paint stoneHighlightPaint;
    private Paint starPointPaint;
    private Paint branchPaint;
    private Paint branchLabelPaint;
    private Paint markPaint;
    private Paint labelPaint;
    private Paint coordPaint;
    private GoBoard board;
    
    // 棋盘相关尺寸
    private float gridSize;
    private float stoneRadius;
    private float startX;
    private float startY;
    
    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        setClickable(true); // 添加这行确保视图可点击
    }
    
    private void init() {
        boardPaint = new Paint();
        boardPaint.setColor(Color.BLACK);
        boardPaint.setStrokeWidth(2f);
        boardPaint.setStyle(Paint.Style.STROKE);
        
        stonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stonePaint.setStyle(Paint.Style.FILL);
        
        stoneShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stoneShadowPaint.setColor(Color.argb(100, 0, 0, 0));
        stoneShadowPaint.setStyle(Paint.Style.FILL);
        
        stoneHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stoneHighlightPaint.setColor(Color.argb(80, 255, 255, 255));
        stoneHighlightPaint.setStyle(Paint.Style.FILL);
        
        starPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPointPaint.setColor(Color.BLACK);
        starPointPaint.setStyle(Paint.Style.FILL);
        
        branchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        branchPaint.setStyle(Paint.Style.STROKE);
        branchPaint.setColor(Color.rgb(30, 144, 255));
        branchPaint.setStrokeWidth(2f);
        
        branchLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        branchLabelPaint.setColor(Color.rgb(30, 144, 255));
        branchLabelPaint.setTextAlign(Paint.Align.CENTER);
        
        markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markPaint.setStyle(Paint.Style.STROKE);
        markPaint.setColor(Color.RED);
        markPaint.setStrokeWidth(2f);
        
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.BLUE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(12f);
        
        coordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coordPaint.setColor(Color.BLACK);
        coordPaint.setTextAlign(Paint.Align.CENTER);
        coordPaint.setTextSize(10f);
        
        board = new GoBoard();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 计算棋盘尺寸 - 使用更小的边距，让棋盘更大
        int width = getWidth();
        int height = getHeight();
        float minDim = Math.min(width, height);
        float margin = Math.min(width, height) * 0.02f;
        float boardSize = Math.min(width, height) - (2 * margin);
        gridSize = boardSize / (BOARD_SIZE - 1);
        stoneRadius = gridSize * 0.48f; // 进一步增大棋子半径比例
        branchLabelPaint.setTextSize(stoneRadius * 0.9f);
        
        // 棋盘居中显示
        startX = (width - boardSize) / 2;
        startY = (height - boardSize) / 2;
        
        // 绘制棋盘背景
        // 优化棋盘背景色
        canvas.drawColor(Color.rgb(238, 198, 145)); // 更温和的木色
        
        // 增加棋盘边框
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.rgb(150, 100, 50));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        canvas.drawRect(startX - 10, startY - 10, 
                       startX + boardSize + 10, 
                       startY + boardSize + 10, 
                       borderPaint);
        
        // 优化棋子绘制
        stonePaint.setShadowLayer(2f, 2f, 2f, Color.GRAY); // 添加阴影效果
        
        // 绘制网格线
        boardPaint.setStrokeWidth(1.5f);
        for (int i = 0; i < BOARD_SIZE; i++) {
            // 横线
            canvas.drawLine(
                startX, startY + i * gridSize,
                startX + boardSize, startY + i * gridSize,
                boardPaint
            );
            // 竖线
            canvas.drawLine(
                startX + i * gridSize, startY,
                startX + i * gridSize, startY + boardSize,
                boardPaint
            );
        }
        
        // 绘制坐标
        drawCoordinates(canvas, boardSize);
        
        // 绘制星位
        float starRadius = gridSize * 0.12f; // 减小星位大小
        int[] starPoints = {3, 9, 15}; // 标准19路棋盘星位
        
        for (int x : starPoints) {
            for (int y : starPoints) {
                float cx = startX + x * gridSize;
                float cy = startY + y * gridSize;
                canvas.drawCircle(cx, cy, starRadius, starPointPaint);
            }
        }
        
        // 绘制棋子
        if (board != null) {
            GoBoard.Move currentMove = board.getCurrentMove();
            
            for (int x = 0; x < BOARD_SIZE; x++) {
                for (int y = 0; y < BOARD_SIZE; y++) {
                    int stone = board.getStoneAt(x, y);
                    if (stone > 0) {
                        float cx = startX + x * gridSize;
                        float cy = startY + y * gridSize;
                        
                        // 确保棋子完全在棋盘范围内
                        if (cx >= startX - stoneRadius && 
                            cx <= startX + boardSize + stoneRadius &&
                            cy >= startY - stoneRadius && 
                            cy <= startY + boardSize + stoneRadius) {
                            
                            // 绘制棋子阴影
                            canvas.drawCircle(cx + 2, cy + 2, stoneRadius, stoneShadowPaint);
                            
                            // 绘制棋子
                            stonePaint.setColor(stone == 1 ? Color.BLACK : Color.WHITE);
                            canvas.drawCircle(cx, cy, stoneRadius, stonePaint);
                            
                            // 绘制棋子高光
                            if (stone == 1) {
                                // 黑棋高光
                                Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                highlightPaint.setColor(Color.argb(50, 255, 255, 255));
                                highlightPaint.setStyle(Paint.Style.FILL);
                                canvas.drawCircle(cx - stoneRadius * 0.3f, cy - stoneRadius * 0.3f, stoneRadius * 0.3f, highlightPaint);
                            } else {
                                // 白棋高光
                                canvas.drawCircle(cx - stoneRadius * 0.3f, cy - stoneRadius * 0.3f, stoneRadius * 0.4f, stoneHighlightPaint);
                            }
                            
                            // 为白棋添加黑色边框
                            if (stone == 2) {
                                Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                strokePaint.setStyle(Paint.Style.STROKE);
                                strokePaint.setColor(Color.BLACK);
                                strokePaint.setStrokeWidth(1.5f);
                                canvas.drawCircle(cx, cy, stoneRadius, strokePaint);
                            }
                            
                            // 高亮当前手
                            if (currentMove != null && currentMove.x == x && currentMove.y == y) {
                                Paint currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                currentPaint.setStyle(Paint.Style.STROKE);
                                currentPaint.setColor(Color.rgb(255, 100, 0));
                                currentPaint.setStrokeWidth(2f);
                                canvas.drawCircle(cx, cy, stoneRadius * 0.7f, currentPaint);
                            }
                            
                            // 绘制标记
                            drawMark(canvas, cx, cy, stoneRadius);
                        }
                    }
                }
            }
        }
        
        if (board != null) {
            GoBoard.Move cm = board.getCurrentMove();
            if (cm != null && cm.variations != null && !cm.variations.isEmpty()) {
                for (int i = 0; i < cm.variations.size(); i++) {
                    List<GoBoard.Move> var = cm.variations.get(i);
                    if (var != null && !var.isEmpty()) {
                        GoBoard.Move vm = var.get(0);
                        if (vm.x >= 0 && vm.y >= 0) {
                            float cx = startX + vm.x * gridSize;
                            float cy = startY + vm.y * gridSize;
                            float r = stoneRadius * 0.5f;
                            canvas.drawCircle(cx, cy, r, branchPaint);
                            canvas.drawText(String.valueOf(i + 1), cx, cy + (branchLabelPaint.getTextSize() * 0.35f), branchLabelPaint);
                            String coord = toCoord(vm.x, vm.y);
                            canvas.drawText(coord, cx, cy - stoneRadius * 0.9f, branchLabelPaint);
                        }
                    }
                }
            } else if (cm == null) {
                List<GoBoard.Move> history = board.getMoveHistory();
                if (history != null && !history.isEmpty()) {
                    GoBoard.Move first = history.get(0);
                    if (first.variations != null && !first.variations.isEmpty()) {
                        for (int i = 0; i < first.variations.size(); i++) {
                            List<GoBoard.Move> var = first.variations.get(i);
                            if (var != null && !var.isEmpty()) {
                                GoBoard.Move vm = var.get(0);
                                if (vm.x >= 0 && vm.y >= 0) {
                                    float cx = startX + vm.x * gridSize;
                                    float cy = startY + vm.y * gridSize;
                                    float r = stoneRadius * 0.5f;
                                    canvas.drawCircle(cx, cy, r, branchPaint);
                                    canvas.drawText(String.valueOf(i + 1), cx, cy + (branchLabelPaint.getTextSize() * 0.35f), branchLabelPaint);
                                    String coord = toCoord(vm.x, vm.y);
                                    canvas.drawText(coord, cx, cy - stoneRadius * 0.9f, branchLabelPaint);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public void setBoard(GoBoard board) {
        this.board = board;
        invalidate();
    }
    
    public GoBoard getBoard() {
        return board;
    }
    
    public void invalidateBoard() {
        invalidate();
        requestLayout();  // 添加这行确保完全刷新
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            
            // 优先检测分支提示点击
            if (board != null) {
                GoBoard.Move cm = board.getCurrentMove();
                if (cm != null && cm.variations != null && !cm.variations.isEmpty()) {
                    for (int i = 0; i < cm.variations.size(); i++) {
                        List<GoBoard.Move> var = cm.variations.get(i);
                        if (var != null && !var.isEmpty()) {
                            GoBoard.Move vm = var.get(0);
                            if (vm.x >= 0 && vm.y >= 0) {
                                float cx = startX + vm.x * gridSize;
                                float cy = startY + vm.y * gridSize;
                                float r = Math.max(stoneRadius * 0.85f, gridSize * 0.4f);
                                float dx = x - cx;
                                float dy = y - cy;
                                if (dx * dx + dy * dy <= r * r) {
                                    if (getContext() instanceof MainActivity) {
                                        ((MainActivity) getContext()).onBranchSelectIndex(i);
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                } else if (cm == null) {
                    List<GoBoard.Move> history = board.getMoveHistory();
                    if (history != null && !history.isEmpty()) {
                        GoBoard.Move first = history.get(0);
                        if (first.variations != null && !first.variations.isEmpty()) {
                            for (int i = 0; i < first.variations.size(); i++) {
                                List<GoBoard.Move> var = first.variations.get(i);
                                if (var != null && !var.isEmpty()) {
                                    GoBoard.Move vm = var.get(0);
                                    if (vm.x >= 0 && vm.y >= 0) {
                                        float cx = startX + vm.x * gridSize;
                                        float cy = startY + vm.y * gridSize;
                                        float r = Math.max(stoneRadius * 0.85f, gridSize * 0.4f);
                                        float dx = x - cx;
                                        float dy = y - cy;
                                        if (dx * dx + dy * dy <= r * r) {
                                            if (getContext() instanceof MainActivity) {
                                                ((MainActivity) getContext()).onBranchSelectIndex(i);
                                            }
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 检查点击是否在棋盘范围内
            if (x < startX || x > startX + gridSize * (BOARD_SIZE-1) ||
                y < startY || y > startY + gridSize * (BOARD_SIZE-1)) {
                return false;
            }
            
            // 转换为棋盘坐标
            int boardX = Math.round((x - startX) / gridSize);
            int boardY = Math.round((y - startY) / gridSize);
            
            // 确保坐标在有效范围内
            boardX = Math.max(0, Math.min(BOARD_SIZE-1, boardX));
            boardY = Math.max(0, Math.min(BOARD_SIZE-1, boardY));
            
            // 通知Activity处理落子
            if (getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).onBoardClick(boardX, boardY);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
    
    private String toCoord(int x, int y) {
        String[] letters = {"A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T"};
        String col = (x >= 0 && x < letters.length) ? letters[x] : String.valueOf((char)('A' + x));
        int row = 19 - y;
        return col + row;
    }
    
    private void drawMark(Canvas canvas, float cx, float cy, float radius) {
        if (board == null) {
            return;
        }
        
        GoBoard.Move currentMove = board.getCurrentMove();
        if (currentMove == null || currentMove.x < 0 || currentMove.y < 0) {
            return;
        }
        
        // 计算当前坐标
        int boardX = Math.round((cx - startX) / gridSize);
        int boardY = Math.round((cy - startY) / gridSize);
        
        // 只绘制当前手的标记
        if (currentMove.x != boardX || currentMove.y != boardY) {
            return;
        }
        
        // 绘制标记
        switch (currentMove.markType) {
            case 1: // 三角形
                drawTriangle(canvas, cx, cy, radius * 0.6f);
                break;
            case 2: // 方形
                drawSquare(canvas, cx, cy, radius * 0.6f);
                break;
            case 3: // 圆形
                drawCircleMark(canvas, cx, cy, radius * 0.6f);
                break;
            case 4: // X标记
                drawXMark(canvas, cx, cy, radius * 0.6f);
                break;
        }
        
        // 绘制标签
        if (currentMove.label != null && !currentMove.label.isEmpty()) {
            labelPaint.setTextSize(radius * 0.8f);
            canvas.drawText(currentMove.label, cx, cy + radius * 0.3f, labelPaint);
        }
    }
    
    private void drawTriangle(Canvas canvas, float cx, float cy, float size) {
        Path path = new Path();
        float angle = (float) (Math.PI / 2);
        for (int i = 0; i < 3; i++) {
            float x = cx + size * (float) Math.cos(angle + i * 2 * Math.PI / 3);
            float y = cy - size * (float) Math.sin(angle + i * 2 * Math.PI / 3);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        canvas.drawPath(path, markPaint);
    }
    
    private void drawSquare(Canvas canvas, float cx, float cy, float size) {
        float halfSize = size * 0.7f;
        canvas.drawRect(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize, markPaint);
    }
    
    private void drawCircleMark(Canvas canvas, float cx, float cy, float size) {
        canvas.drawCircle(cx, cy, size, markPaint);
    }
    
    private void drawXMark(Canvas canvas, float cx, float cy, float size) {
        float halfSize = size * 0.7f;
        canvas.drawLine(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize, markPaint);
        canvas.drawLine(cx + halfSize, cy - halfSize, cx - halfSize, cy + halfSize, markPaint);
    }
    
    private void drawCoordinates(Canvas canvas, float boardSize) {
        String[] letters = {"A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T"};
        
        coordPaint.setTextSize(gridSize * 0.4f);
        
        // 绘制顶部坐标（字母）
        for (int i = 0; i < BOARD_SIZE; i++) {
            float x = startX + i * gridSize;
            String letter = letters[i];
            canvas.drawText(letter, x, startY - gridSize * 0.3f, coordPaint);
        }
        
        // 绘制底部坐标（字母）
        for (int i = 0; i < BOARD_SIZE; i++) {
            float x = startX + i * gridSize;
            String letter = letters[i];
            canvas.drawText(letter, x, startY + boardSize + gridSize * 0.5f, coordPaint);
        }
        
        // 绘制左侧坐标（数字）
        for (int i = 0; i < BOARD_SIZE; i++) {
            float y = startY + i * gridSize;
            int number = 19 - i;
            canvas.drawText(String.valueOf(number), startX - gridSize * 0.5f, y + gridSize * 0.15f, coordPaint);
        }
        
        // 绘制右侧坐标（数字）
        for (int i = 0; i < BOARD_SIZE; i++) {
            float y = startY + i * gridSize;
            int number = 19 - i;
            canvas.drawText(String.valueOf(number), startX + boardSize + gridSize * 0.5f, y + gridSize * 0.15f, coordPaint);
        }
    }
}
