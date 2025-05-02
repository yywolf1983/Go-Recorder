package com.gosgf.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import com.gosgf.app.model.GoBoard;

public class GameInfoView extends View {
    private Paint textPaint;
    private GoBoard board;
    private String blackPlayer = "黑棋";
    private String whitePlayer = "白棋";
    private String gameResult = "未知";
    private String gameDate = "今天";
    
    public GameInfoView(Context context) {
        super(context);
        init();
    }
    
    public GameInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(36f);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
    }
    
    public void setBoard(GoBoard board) {
        this.board = board;
        invalidate();
    }
    
    public void setGameInfo(String blackPlayer, String whitePlayer, String result, String date) {
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        this.gameResult = result;
        this.gameDate = date;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float y = 50;
        float lineHeight = 50;
        
        canvas.drawText("对局信息", 20, y, textPaint);
        y += lineHeight;
        
        textPaint.setTextSize(30f);
        canvas.drawText("黑方: " + blackPlayer, 20, y, textPaint);
        y += lineHeight;
        
        canvas.drawText("白方: " + whitePlayer, 20, y, textPaint);
        y += lineHeight;
        
        canvas.drawText("结果: " + gameResult, 20, y, textPaint);
        y += lineHeight;
        
        canvas.drawText("日期: " + gameDate, 20, y, textPaint);
        y += lineHeight;
        
        if (board != null) {
            canvas.drawText("当前手数: " + (board.getCurrentMoveNumber() + 1), 20, y, textPaint);
            y += lineHeight;
            
            String currentPlayer = board.getCurrentPlayer() == 1 ? "黑方" : "白方";
            canvas.drawText("轮到: " + currentPlayer, 20, y, textPaint);
        }
    }
}