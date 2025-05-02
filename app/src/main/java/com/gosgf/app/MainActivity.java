package com.gosgf.app;  // 确保包声明正确

import android.util.Log;
import com.gosgf.app.model.InvalidSGFException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.gosgf.app.view.BoardView;
import com.gosgf.app.model.GoBoard;
import com.gosgf.app.R;
import com.gosgf.app.util.SGFParser;
import java.util.Scanner;
import android.widget.TextView;
import java.util.List;
import java.util.stream.IntStream;
import android.view.View;
import android.view.MotionEvent;
import androidx.appcompat.app.AlertDialog;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_LOAD = 1;
    private static final int REQUEST_CODE_SAVE = 2;
    private static final int REQUEST_CODE_EDIT_INFO = 3;
    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final int BOARD_SIZE = 19;
    
    // 在类顶部统一定义变量
    private TextView tvComment;
    private TextView tvGameInfo;
    // 删除editMode变量声明
    // private boolean editMode = false;
    
    // 删除toggleEditMode方法
    // private void toggleEditMode() {
    //     GoBoard board = boardView.getBoard();
    //     if (board != null) {
    //         editMode = !editMode;
    //         board.setEditMode(editMode);
    //         updateGameInfo();
    //         String message = editMode ? "已进入摆子模式" : "已退出摆子模式";
    //         Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    //     }
    // }
    private BoardView boardView;
    private float startX, startY;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化视图组件
        boardView = findViewById(R.id.board_view);
        tvGameInfo = findViewById(R.id.tvGameInfo);
        tvComment = findViewById(R.id.tvComment);
        tvComment.setVisibility(View.GONE);
        
        // 初始化按钮
        Button btnPrev = findViewById(R.id.btnPrev);
        Button btnNext = findViewById(R.id.btnNext);
        Button btnNew = findViewById(R.id.btnNew);
        // 删除以下摆子模式相关代码
        // Button btnEditMode = findViewById(R.id.btnEditMode);
        // Button btnToggleColor = findViewById(R.id.btnToggleColor);
        // btnEditMode.setOnClickListener(v -> toggleEditMode());
        // btnToggleColor.setOnClickListener(v -> {
        //     if (boardView.getBoard() != null) {
        //         boardView.getBoard().toggleCurrentPlayer();
        //         updateGameInfo();
        //     }
        // });
        
        // 设置事件监听（必须放在方法体内）
        findViewById(R.id.btnLoad).setOnClickListener(v -> openFile(REQUEST_CODE_LOAD));
        findViewById(R.id.btnSave).setOnClickListener(v -> openFile(REQUEST_CODE_SAVE));
        findViewById(R.id.btnComment).setOnClickListener(v -> showCommentDialog());
        
        btnPrev.setOnClickListener(v -> {
            if (boardView.getBoard().previousMove()) {
                boardView.invalidateBoard();
                updateButtonStates();
                updateCommentDisplay(); // 添加这行
            }
        });
        
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            if (boardView.getBoard().nextMove()) {
                boardView.invalidateBoard();
                updateButtonStates();
                updateCommentDisplay(); // 添加这行
            }
        });
        
        findViewById(R.id.btnNew).setOnClickListener(v -> {
            boardView.getBoard().resetGame();
            boardView.invalidateBoard();
            updateButtonStates();
        });
        
        // 添加运行时权限检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CODE_PERMISSION);
            }
        }
        
        updateButtonStates();
        setupBoardGestures();
        
        // 添加虚手按钮初始化
        // 虚手按钮点击事件
        Button btnPass = findViewById(R.id.btnPass);
        btnPass.setOnClickListener(v -> {
            GoBoard board = boardView.getBoard();
            if (board != null) {
                board.skipTurn();
                boardView.invalidate();
                updateGameInfo();
                updateCommentDisplay();
                String player = board.getCurrentPlayer() == 1 ? "黑方" : "白方";
                Toast.makeText(this, player + "虚手成功", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // 设置棋盘手势
    private void setupBoardGestures() {
        // 实现棋盘手势逻辑
    }
    
    // 更新按钮状态
    private void updateButtonStates() {
        // 实现按钮状态更新逻辑
    }
    
    // 打开文件
    private void openFile(int requestCode) {
        Intent intent = new Intent(requestCode == REQUEST_CODE_LOAD ? 
            Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // 更通用的MIME类型
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                       Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                       Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        if (requestCode == REQUEST_CODE_SAVE) {
            intent.putExtra(Intent.EXTRA_TITLE, "game.sgf");
            intent.putExtra(Intent.EXTRA_ALLOW_REPLACE, true);
        }
        startActivityForResult(intent, requestCode);
    }
    
    // 显示注释对话框
    private void showCommentDialog() {
        GoBoard board = boardView.getBoard();
        if (board == null) return;
        
        // 获取当前手数和注释
        GoBoard.Move currentMove = board.getCurrentMove();
        if (currentMove == null) {
            Toast.makeText(this, "请先落子", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_comment, null);
        builder.setView(dialogView);
        
        // 设置对话框标题和内容
        TextView tvMoveInfo = dialogView.findViewById(R.id.tvMoveInfo);
        EditText etComment = dialogView.findViewById(R.id.etComment);
        
        int moveNumber = board.getCurrentMoveNumber() + 1; // 这里已经是从1开始
        String playerColor = currentMove.color == 1 ? "黑" : "白";
        tvMoveInfo.setText(String.format("第 %d 手 (%s)", moveNumber, playerColor));
        etComment.setText(currentMove.comment != null ? currentMove.comment : "");
        
        // 创建对话框
        AlertDialog dialog = builder.create();
        
        dialogView.findViewById(R.id.btnSaveComment).setOnClickListener(v -> {
            String comment = etComment.getText().toString().trim();
            currentMove.comment = comment;
            Toast.makeText(MainActivity.this, "注释已保存", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            updateCommentDisplay();
        });
        
        dialogView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    // 更新注释显示
    private void updateCommentDisplay() {
        GoBoard board = boardView.getBoard();
        if (board == null || tvComment == null) return;
        
        // 只显示当前手的注释
        GoBoard.Move currentMove = board.getCurrentMove();
        if (currentMove != null && currentMove.comment != null && !currentMove.comment.isEmpty()) {
            tvComment.setText(currentMove.comment);
            tvComment.setVisibility(View.VISIBLE);
        } else {
            tvComment.setVisibility(View.GONE);
        }
    }
    
    // 在所有需要更新注释的地方调用updateCommentDisplay()
    public void onBoardClick(int x, int y) {
        GoBoard board = boardView.getBoard();
        if (board != null && !board.isEditMode()) {
            if (board.placeStone(x, y)) {
                boardView.invalidate();
                updateGameInfo();
                updateCommentDisplay(); // 添加这行
            }
        }
    }
    
    public void previousMove() {
        if (boardView.getBoard().previousMove()) {
            boardView.invalidate();
            updateGameInfo();
            updateCommentDisplay(); // 添加这行
        }
    }
    
    public void nextMove() {
        if (boardView.getBoard().nextMove()) {
            boardView.invalidate();
            updateGameInfo();
            updateCommentDisplay(); // 添加这行
        }
    }
    
    // 更新游戏信息显示
    private void updateGameInfo() {
        GoBoard board = boardView.getBoard();
        if (board == null || tvGameInfo == null) return;
        
        String playerStatus = board.getCurrentPlayer() == 1 ? 
            "黑方回合" : "白方回合";
        
        int currentMove = Math.max(1, board.getCurrentMoveNumber() + 1); // 修改为从1开始
        int totalMoves = Math.max(1, board.getMoveHistory().size()); // 修改为从1开始
        
        String moveInfo = "第 " + currentMove + " 手 / 共 " + totalMoves + " 手";
        
        // 删除摆子模式判断
        tvGameInfo.setText(playerStatus + " | " + moveInfo);
    }
    
    // 重置游戏
    private void resetGame() {
        boardView.getBoard().resetGame();
        boardView.invalidateBoard();
        updateButtonStates();
        ((TextView)findViewById(R.id.tvCommentDisplay)).setText("新棋局已创建");
    }
    
    // 获取最新注释
    private String getLatestComment() {
        GoBoard board = boardView.getBoard();
        List<GoBoard.Move> moves = board.getMoveHistory();
        int currentMove = board.getCurrentMoveNumber();
        
        if (currentMove >= 0 && moves != null && currentMove < moves.size()) {
            GoBoard.Move move = moves.get(currentMove);
            if (move.comment != null && !move.comment.isEmpty()) {
                return move.comment;
            }
        }
        return "暂无注释";
    }
    
    // 分支选择
    public void onBranchSelect(View view) {
        GoBoard board = boardView.getBoard();
        List<List<GoBoard.Move>> currentVariations = board.getCurrentMove().variations;
        
        new AlertDialog.Builder(this)
            .setTitle("选择分支路径")
            .setItems(getBranchTitles(currentVariations), (dialog, which) -> {
                board.selectVariation(which);
                boardView.invalidate();
                updateButtonStates();
            })
            .show();
    }
    
    // 获取分支标题
    private String[] getBranchTitles(List<List<GoBoard.Move>> variations) {
        return IntStream.range(0, variations.size())
            .mapToObj(i -> "分支 " + (i + 1))
            .toArray(String[]::new);
    }
    
    // 确保onActivityResult方法在类内部
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == REQUEST_CODE_LOAD) {
                    try (InputStream is = getContentResolver().openInputStream(uri)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        try {
                            boardView.getBoard().resetBoardToCurrentMove(); // Add this line to ensure board is properly reset
                            boardView.getBoard().loadFromSGF(sb.toString());
                            boardView.invalidate();
                            updateCommentDisplay();
                            // 检查是否有虚手并提示
                            GoBoard board = boardView.getBoard();
                            if (board != null) {
                                for (GoBoard.Move move : board.getMoveHistory()) {
                                    if (move.x == -1) { // 虚手
                                        String player = move.color == 1 ? "黑方" : "白方";
                                        Toast.makeText(this, "加载棋局包含" + player + "虚手", Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                }
                            }
                            Toast.makeText(this, "棋局已加载(包含注释)", Toast.LENGTH_SHORT).show();
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Toast.makeText(this, "SGF文件包含无效坐标", Toast.LENGTH_SHORT).show();
                            boardView.getBoard().resetGame();
                            boardView.invalidate();
                        } catch (InvalidSGFException e) {
                            Toast.makeText(this, "无效的SGF格式: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (requestCode == REQUEST_CODE_SAVE) {
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        // 修改为无参方法调用
                        GoBoard board = boardView.getBoard();
                        String sgf = board.toSGFString(); // 移除布尔参数
                        os.write(sgf.getBytes(StandardCharsets.UTF_8));
                        Toast.makeText(this, "棋局已保存", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
} // 类结束的大括号