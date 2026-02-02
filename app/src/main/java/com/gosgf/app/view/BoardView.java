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
import android.view.GestureDetector;
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
    private float scale = 1.0f; // 缩放比例
    private float translationX = 0.0f; // 平移X
    private float translationY = 0.0f; // 平移Y
    private boolean isBoardUILocked = false; // 棋盘UI锁定标志，true表示棋盘UI已固定，不允许移动和缩放
    
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
    
    // 手势检测器
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;
    
    // 长按删除分支相关
    private int longPressVariationIndex = -1;
    private boolean isLongPressVariationStart = false;
    private boolean isVariationClick = false; // 记录是否点击了分支提示
    private boolean isLongPressTriggered = false; // 记录是否触发了长按
    private static final long LONG_PRESS_TIMEOUT = 500; // 长按时间阈值（毫秒）
    private Runnable longPressRunnable;
    
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
        
        // 初始化缩放手势检测器
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // 检查棋盘UI是否被锁定
                if (isBoardUILocked) {
                    return false;
                }
                
                scale *= detector.getScaleFactor();
                // 限制缩放范围
                scale = Math.max(0.5f, Math.min(2.0f, scale));
                invalidate();
                return true;
            }
        });
        
        // 初始化手势检测器
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // 检查棋盘UI是否被锁定
                if (isBoardUILocked) {
                    return false;
                }
                
                translationX -= distanceX;
                translationY -= distanceY;
                invalidate();
                return true;
            }
        });
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
        
        // 应用缩放和平移变换
        canvas.save();
        canvas.translate(width / 2, height / 2);
        canvas.scale(scale, scale);
        canvas.translate(-width / 2, -height / 2);
        canvas.translate(translationX, translationY);
        
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
                            
                            // 查找当前位置的移动
                            GoBoard.Move move = null;
                            for (GoBoard.Move m : board.getMoveHistory()) {
                                if (m.x == x && m.y == y) {
                                    move = m;
                                    break;
                                }
                            }
                            
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
                            drawMark(canvas, cx, cy, stoneRadius, move);
                        }
                    }
                }
            }
        }
        
        // Draw next move position if it exists
        if (board != null) {
            int currentMoveNumber = board.getCurrentMoveNumber();
            List<GoBoard.Move> moveHistory = board.getMoveHistory();
            if (moveHistory != null && currentMoveNumber + 1 < moveHistory.size()) {
                GoBoard.Move nextMove = moveHistory.get(currentMoveNumber + 1);
                if (nextMove != null && nextMove.x >= 0 && nextMove.y >= 0) {
                    float cx = startX + nextMove.x * gridSize;
                    float cy = startY + nextMove.y * gridSize;
                    // Draw next move marker
                    Paint nextMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    nextMovePaint.setStyle(Paint.Style.STROKE);
                    nextMovePaint.setColor(Color.GREEN);
                    nextMovePaint.setStrokeWidth(2f);
                    canvas.drawCircle(cx, cy, stoneRadius * 0.8f, nextMovePaint);
                    // Draw "下" label for next move
                    Paint nextMoveLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    nextMoveLabelPaint.setColor(Color.GREEN);
                    nextMoveLabelPaint.setTextSize(stoneRadius * 0.6f);
                    nextMoveLabelPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("下", cx, cy + (nextMoveLabelPaint.getTextSize() * 0.35f), nextMoveLabelPaint);
                }
            }
        }
        
        if (board != null) {
            GoBoard.Move cm = board.getCurrentMove();
            if (cm != null && cm.variations != null && !cm.variations.isEmpty()) {
                for (int i = 0; i < cm.variations.size(); i++) {
                    GoBoard.Variation variation = cm.variations.get(i);
                    List<GoBoard.Move> var = variation.getMoves();
                    if (var != null && !var.isEmpty()) {
                        GoBoard.Move vm = var.get(0);
                        if (vm.x >= 0 && vm.y >= 0) {
                            float cx = startX + vm.x * gridSize;
                            float cy = startY + vm.y * gridSize;
                            float r = stoneRadius * 0.6f;
                            
                            // 绘制分支标记背景
                            Paint branchBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            branchBgPaint.setColor(Color.argb(100, 30, 144, 255));
                            branchBgPaint.setStyle(Paint.Style.FILL);
                            canvas.drawCircle(cx, cy, r, branchBgPaint);
                            
                            // 绘制分支标记边框
                            canvas.drawCircle(cx, cy, r, branchPaint);
                            
                            // 绘制分支编号
                            branchLabelPaint.setTextSize(stoneRadius * 0.8f);
                            canvas.drawText(String.valueOf(i + 1), cx, cy + (branchLabelPaint.getTextSize() * 0.35f), branchLabelPaint);
                            
                            // 绘制分支坐标
                            branchLabelPaint.setTextSize(stoneRadius * 0.4f);
                            String coord = toCoord(vm.x, vm.y);
                            canvas.drawText(coord, cx, cy - stoneRadius * 0.8f, branchLabelPaint);
                        }
                    }
                }
            } else if (cm == null) {
                // 起始态，显示第一手分支
                if (board.hasStartVariations()) {
                    List<List<GoBoard.Move>> startVariations = board.getStartVariations();
                    for (int i = 0; i < startVariations.size(); i++) {
                        List<GoBoard.Move> var = startVariations.get(i);
                        if (var != null && !var.isEmpty()) {
                            GoBoard.Move vm = var.get(0);
                            if (vm.x >= 0 && vm.y >= 0) {
                                float cx = startX + vm.x * gridSize;
                                float cy = startY + vm.y * gridSize;
                                float r = stoneRadius * 0.6f;
                                
                                // 绘制分支标记背景
                                Paint branchBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                branchBgPaint.setColor(Color.argb(100, 30, 144, 255));
                                branchBgPaint.setStyle(Paint.Style.FILL);
                                canvas.drawCircle(cx, cy, r, branchBgPaint);
                                
                                // 绘制分支标记边框
                                canvas.drawCircle(cx, cy, r, branchPaint);
                                
                                // 绘制分支编号
                                branchLabelPaint.setTextSize(stoneRadius * 0.8f);
                                canvas.drawText(String.valueOf(i + 1), cx, cy + (branchLabelPaint.getTextSize() * 0.35f), branchLabelPaint);
                                
                                // 绘制分支坐标
                                branchLabelPaint.setTextSize(stoneRadius * 0.4f);
                                String coord = toCoord(vm.x, vm.y);
                                canvas.drawText(coord, cx, cy - stoneRadius * 0.8f, branchLabelPaint);
                            }
                        }
                    }
                }
            }
        }
        
        // 恢复画布状态
        canvas.restore();
    }
    
    public void setBoard(GoBoard board) {
        this.board = board;
        invalidate();
    }
    
    // 设置棋盘UI锁定状态
    public void setBoardUILocked(boolean locked) {
        this.isBoardUILocked = locked;
    }
    
    // 获取棋盘UI锁定状态
    public boolean isBoardUILocked() {
        return isBoardUILocked;
    }
    
    public GoBoard getBoard() {
        return board;
    }
    
    public void invalidateBoard() {
        invalidate();
        requestLayout();  // 添加这行确保完全刷新
    }
    
    // 获取单元格大小，用于计算落子位置
    public float getCellSize() {
        // 确保gridSize有效
        if (gridSize <= 0) {
            // 计算临时gridSize
            int width = getWidth();
            int height = getHeight();
            float minDim = Math.min(width, height);
            float margin = Math.min(width, height) * 0.02f;
            float boardSize = Math.min(width, height) - (2 * margin);
            return boardSize / (BOARD_SIZE - 1);
        }
        return gridSize;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 确保尺寸计算正确
        calculateBoardDimensions();
        
        // 处理缩放手势
        if (!isBoardUILocked) {
            scaleDetector.onTouchEvent(event);
        }
        
        // 处理平移和点击手势
        if (!isBoardUILocked) {
            gestureDetector.onTouchEvent(event);
        }
        
        // 处理触摸事件
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = false;
                isVariationClick = false;
                isLongPressTriggered = false;
                
                // 检查是否点击了分支提示，启动长按计时器
                longPressVariationIndex = -1;
                isLongPressVariationStart = false;
                if (board != null) {
                    float x = event.getX();
                    float y = event.getY();
                    
                    GoBoard.Move cm = board.getCurrentMove();
                    if (cm != null && cm.variations != null && !cm.variations.isEmpty()) {
                        for (int i = 0; i < cm.variations.size(); i++) {
                            GoBoard.Variation variation = cm.variations.get(i);
                            List<GoBoard.Move> var = variation.getMoves();
                            if (var != null && !var.isEmpty()) {
                                GoBoard.Move vm = var.get(0);
                                if (vm.x >= 0 && vm.y >= 0) {
                                    float cx = startX + vm.x * gridSize;
                                    float cy = startY + vm.y * gridSize;
                                    float r = Math.max(stoneRadius * 0.85f, gridSize * 0.4f);
                                    float dx = x - cx;
                                    float dy = y - cy;
                                    if (dx * dx + dy * dy <= r * r) {
                                        isVariationClick = true;
                                        longPressVariationIndex = i;
                                        isLongPressVariationStart = true;
                                        // 启动长按计时器
                                        longPressRunnable = new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isLongPressVariationStart && longPressVariationIndex >= 0) {
                                                    // 长按触发，显示确认对话框
                                                    isLongPressTriggered = true;
                                                    if (getContext() instanceof MainActivity) {
                                                        MainActivity activity = (MainActivity) getContext();
                                                        activity.showDeleteVariationConfirmDialog(longPressVariationIndex, false);
                                                    }
                                                    isLongPressVariationStart = false;
                                                    longPressVariationIndex = -1;
                                                }
                                            }
                                        };
                                        postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (cm == null) {
                        // 检查起始状态的分支
                        if (board.hasStartVariations()) {
                            List<List<GoBoard.Move>> startVariations = board.getStartVariations();
                            for (int i = 0; i < startVariations.size(); i++) {
                                List<GoBoard.Move> var = startVariations.get(i);
                                if (var != null && !var.isEmpty()) {
                                    GoBoard.Move vm = var.get(0);
                                    if (vm.x >= 0 && vm.y >= 0) {
                                        float cx = startX + vm.x * gridSize;
                                        float cy = startY + vm.y * gridSize;
                                        float r = Math.max(stoneRadius * 0.85f, gridSize * 0.4f);
                                        float dx = x - cx;
                                        float dy = y - cy;
                                        if (dx * dx + dy * dy <= r * r) {
                                            isVariationClick = true;
                                            longPressVariationIndex = i;
                                            isLongPressVariationStart = true;
                                            // 启动长按计时器
                                            longPressRunnable = new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isLongPressVariationStart && longPressVariationIndex >= 0) {
                                                        // 长按触发，显示确认对话框
                                                        isLongPressTriggered = true;
                                                        if (getContext() instanceof MainActivity) {
                                                            MainActivity activity = (MainActivity) getContext();
                                                            activity.showDeleteVariationConfirmDialog(longPressVariationIndex, true);
                                                        }
                                                        isLongPressVariationStart = false;
                                                        longPressVariationIndex = -1;
                                                    }
                                                }
                                            };
                                            postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 检查是否正在缩放
                if (!scaleDetector.isInProgress() && !isBoardUILocked) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    
                    // 检查是否是拖动
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true;
                        // 取消长按计时器
                        if (longPressRunnable != null) {
                            removeCallbacks(longPressRunnable);
                            longPressRunnable = null;
                        }
                        isLongPressVariationStart = false;
                        longPressVariationIndex = -1;
                        
                        // 平移棋盘
                        translationX += dx;
                        translationY += dy;
                        invalidate();
                    }
                }
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                // 取消长按计时器
                if (longPressRunnable != null) {
                    removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }
                isLongPressVariationStart = false;
                
                // 如果不是拖动，则认为是点击
                if (!isDragging) {
                    float x = event.getX();
                    float y = event.getY();
                    
                    // 如果触发了长按，不执行任何操作
                    if (isLongPressTriggered) {
                        isLongPressTriggered = false;
                        return true;
                    }
                    
                    // 优先检测分支提示点击
                    if (board != null) {
                        GoBoard.Move cm = board.getCurrentMove();
                        if (cm != null && cm.variations != null && !cm.variations.isEmpty()) {
                            for (int i = 0; i < cm.variations.size(); i++) {
                                GoBoard.Variation variation = cm.variations.get(i);
                                List<GoBoard.Move> var = variation.getMoves();
                                if (var != null && !var.isEmpty()) {
                                    GoBoard.Move vm = var.get(0);
                                    if (vm.x >= 0 && vm.y >= 0) {
                                        float cx = startX + vm.x * gridSize;
                                        float cy = startY + vm.y * gridSize;
                                        float r = Math.max(stoneRadius * 0.85f, gridSize * 0.4f);
                                        float dx = x - cx;
                                        float dy = y - cy;
                                        if (dx * dx + dy * dy <= r * r) {
                                            // 直接处理分支选择
                                            if (board != null) {
                                                board.selectVariation(i);
                                                invalidateBoard();
                                                // 通知MainActivity更新游戏信息和注释
                                                if (getContext() instanceof MainActivity) {
                                                    MainActivity activity = (MainActivity) getContext();
                                                    activity.updateGameInfo();
                                                    activity.updateCommentDisplay();
                                                }
                                            }
                                            return true;
                                        }
                                    }
                                }
                            }
                        } else if (cm == null) {
                            // 检查起始状态的分支
                            if (board.hasStartVariations()) {
                                List<List<GoBoard.Move>> startVariations = board.getStartVariations();
                                for (int i = 0; i < startVariations.size(); i++) {
                                    List<GoBoard.Move> var = startVariations.get(i);
                                    if (var != null && !var.isEmpty()) {
                                        GoBoard.Move vm = var.get(0);
                                        if (vm.x >= 0 && vm.y >= 0) {
                                            float cx = startX + vm.x * gridSize;
                                            float cy = startY + vm.y * gridSize;
                                            float r = Math.max(stoneRadius * 0.85f, gridSize * 0.4f);
                                            float dx = x - cx;
                                            float dy = y - cy;
                                            if (dx * dx + dy * dy <= r * r) {
                                                // 直接处理分支选择
                                                if (board != null) {
                                                    board.selectVariation(i);
                                                    invalidateBoard();
                                                    // 通知MainActivity更新游戏信息和注释
                                                    if (getContext() instanceof MainActivity) {
                                                        MainActivity activity = (MainActivity) getContext();
                                                        activity.updateGameInfo();
                                                        activity.updateCommentDisplay();
                                                    }
                                                }
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 检查点击是否在棋盘范围内
                    if (!isVariationClick && x >= startX && x <= startX + gridSize * (BOARD_SIZE-1) &&
                        y >= startY && y <= startY + gridSize * (BOARD_SIZE-1)) {
                        
                        // 转换为棋盘坐标
                        int boardX = Math.round((x - startX) / gridSize);
                        int boardY = Math.round((y - startY) / gridSize);
                        
                        // 确保坐标在有效范围内
                        boardX = Math.max(0, Math.min(BOARD_SIZE-1, boardX));
                        boardY = Math.max(0, Math.min(BOARD_SIZE-1, boardY));
                        
                        // 调用MainActivity的方法处理落子
                        if (getContext() instanceof MainActivity) {
                            MainActivity activity = (MainActivity) getContext();
                            activity.handleBoardClick(boardX, boardY);
                        }
                    }
                }
                break;
        }
        
        return true;
    }
    
    // 计算棋盘尺寸
    private void calculateBoardDimensions() {
        int width = getWidth();
        int height = getHeight();
        float minDim = Math.min(width, height);
        float margin = minDim * 0.02f;
        float boardSize = minDim - (2 * margin);
        gridSize = boardSize / (BOARD_SIZE - 1);
        stoneRadius = gridSize * 0.48f;
        branchLabelPaint.setTextSize(stoneRadius * 0.9f);
        
        // 棋盘居中显示
        startX = (width - boardSize) / 2;
        startY = (height - boardSize) / 2;
    }
    
    private String toCoord(int x, int y) {
        String[] letters = {"A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T"};
        String col = (x >= 0 && x < letters.length) ? letters[x] : String.valueOf((char)('A' + x));
        int row = 19 - y;
        return col + row;
    }
    
    private void drawMark(Canvas canvas, float cx, float cy, float radius, GoBoard.Move move) {
        if (board == null || move == null) {
            return;
        }
        
        // 绘制标记
        switch (move.markType) {
            case 1: // 三角形
                drawTriangle(canvas, cx, cy, radius * 0.5f);
                break;
            case 2: // 方形
                drawSquare(canvas, cx, cy, radius * 0.5f);
                break;
            case 3: // 圆形
                drawCircleMark(canvas, cx, cy, radius * 0.5f);
                break;
            case 4: // X标记
                drawXMark(canvas, cx, cy, radius * 0.5f);
                break;
            case 5: // 数字标记
                drawNumberMark(canvas, cx, cy, radius * 0.6f, move.label);
                break;
            case 6: // 字母标记
                drawLetterMark(canvas, cx, cy, radius * 0.6f, move.label);
                break;
            case 7: // 正方形标记
                drawSquareMark(canvas, cx, cy, radius * 0.5f);
                break;
            case 8: // 三角形标记
                drawTriangleMark(canvas, cx, cy, radius * 0.5f);
                break;
            case 9: // 圆形标记
                drawCircleMark(canvas, cx, cy, radius * 0.5f);
                break;
            case 10: // 叉号标记
                drawCrossMark(canvas, cx, cy, radius * 0.5f);
                break;
        }
        
        // 绘制标签
        if (move.label != null && !move.label.isEmpty()) {
            // 为标签添加背景，使其更加清晰
            Paint labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            labelBgPaint.setColor(Color.argb(150, 255, 255, 255));
            labelBgPaint.setStyle(Paint.Style.FILL);
            
            float labelSize = radius * 0.8f;
            labelPaint.setTextSize(labelSize);
            float labelWidth = labelPaint.measureText(move.label);
            
            // 绘制标签背景
            canvas.drawRoundRect(
                cx - labelWidth / 2 - 4,
                cy - labelSize / 2 - 2,
                cx + labelWidth / 2 + 4,
                cy + labelSize / 2 + 2,
                4,
                4,
                labelBgPaint
            );
            
            // 绘制标签
            labelPaint.setColor(Color.BLUE);
            canvas.drawText(move.label, cx, cy + labelSize * 0.3f, labelPaint);
        }
    }
    
    private void drawNumberMark(Canvas canvas, float cx, float cy, float size, String label) {
        labelPaint.setTextSize(size * 1.2f);
        canvas.drawText(label, cx, cy + size * 0.3f, labelPaint);
    }
    
    private void drawLetterMark(Canvas canvas, float cx, float cy, float size, String label) {
        labelPaint.setTextSize(size * 1.2f);
        canvas.drawText(label, cx, cy + size * 0.3f, labelPaint);
    }
    
    private void drawSquareMark(Canvas canvas, float cx, float cy, float size) {
        float halfSize = size * 0.7f;
        canvas.drawRect(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize, markPaint);
    }
    
    private void drawTriangleMark(Canvas canvas, float cx, float cy, float size) {
        drawTriangle(canvas, cx, cy, size);
    }
    
    private void drawCrossMark(Canvas canvas, float cx, float cy, float size) {
        float halfSize = size * 0.7f;
        canvas.drawLine(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize, markPaint);
        canvas.drawLine(cx + halfSize, cy - halfSize, cx - halfSize, cy + halfSize, markPaint);
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
