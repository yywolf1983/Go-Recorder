package com.gosgf.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.gosgf.app.model.GoBoard;
import java.util.List;

public class SGFTreeView extends View {
    private Paint linePaint;
    private Paint nodePaint;
    private Paint textPaint;
    private Paint currentNodePaint;
    
    private GoBoard board;
    private int nodeSize = 30;
    private int horizontalSpacing = 50;
    private int verticalSpacing = 60;
    
    public SGFTreeView(Context context) {
        super(context);
        init();
    }
    
    public SGFTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(2f);
        
        nodePaint = new Paint();
        nodePaint.setColor(Color.LTGRAY);
        nodePaint.setStyle(Paint.Style.FILL);
        
        currentNodePaint = new Paint();
        currentNodePaint.setColor(Color.RED);
        currentNodePaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(20f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    public void setBoard(GoBoard board) {
        this.board = board;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (board == null) return;
        
        List<GoBoard.Move> moves = board.getMoveHistory();
        int currentMove = board.getCurrentMoveNumber();
        
        // 绘制主线
        int startX = 50;
        int startY = getHeight() / 2;
        
        for (int i = 0; i < moves.size(); i++) {
            int x = startX + i * horizontalSpacing;
            int y = startY;
            
            // 绘制连接线
            if (i > 0) {
                canvas.drawLine(x - horizontalSpacing + nodeSize/2, y, 
                               x - nodeSize/2, y, linePaint);
            }
            
            // 绘制节点
            Paint paint = (i == currentMove) ? currentNodePaint : nodePaint;
            canvas.drawCircle(x, y, nodeSize/2, paint);
            
            // 绘制手数
            canvas.drawText(String.valueOf(i + 1), x, y + 7, textPaint);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && board != null) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            
            // 计算点击的节点
            int startX = 50;
            int startY = getHeight() / 2;
            
            List<GoBoard.Move> moves = board.getMoveHistory();
            for (int i = 0; i < moves.size(); i++) {
                int nodeX = startX + i * horizontalSpacing;
                int nodeY = startY;
                
                // 检查点击是否在节点范围内
                if (Math.sqrt(Math.pow(x - nodeX, 2) + Math.pow(y - nodeY, 2)) <= nodeSize/2) {
                    // 跳转到该手
                    board.setCurrentMoveNumber(i);
                    invalidate();
                    return true;
                }
            }
        }
        return true;
    }
}