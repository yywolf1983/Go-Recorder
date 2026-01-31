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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.gosgf.app.view.BoardView;
import com.gosgf.app.view.SGFTreeView;
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
    private SGFTreeView treeView;
    private float startX, startY;
    private ActivityResultLauncher<Intent> openDocumentLauncher;
    private ActivityResultLauncher<Intent> createDocumentLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try (InputStream is = getContentResolver().openInputStream(uri)) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            try {
                                SGFParser.parseSGF(sb.toString(), boardView.getBoard());
                                boardView.invalidate();
                                updateGameTree();
                                updateCommentDisplay();
                                updateButtonStates();
                                GoBoard board = boardView.getBoard();
                                if (board != null) {
                                    for (GoBoard.Move move : board.getMoveHistory()) {
                                        if (move.x == -1) {
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
                                updateGameTree();
                            } catch (InvalidSGFException e) {
                                Toast.makeText(this, "无效的SGF格式: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );
        createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                            GoBoard board = boardView.getBoard();
                            String sgf = SGFParser.saveToString(board, board.getBlackPlayer(), board.getWhitePlayer(), board.getResult());
                            os.write(sgf.getBytes(StandardCharsets.UTF_8));
                            Toast.makeText(this, "棋局已保存", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );
        
        // 初始化视图组件
        boardView = findViewById(R.id.board_view);
        treeView = findViewById(R.id.tree_view);
        tvGameInfo = findViewById(R.id.tvGameInfo);
        
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
            GoBoard board = boardView.getBoard();
            if (board.nextMove()) {
                boardView.invalidateBoard();
                updateButtonStates();
                updateCommentDisplay();
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
                updateGameTree();
                updateGameInfo();
                updateCommentDisplay();
                String player = board.getCurrentPlayer() == 1 ? "黑方" : "白方";
                Toast.makeText(this, player + "虚手成功", Toast.LENGTH_SHORT).show();
                updateButtonStates();
            }
        });
        
        // 到棋局开始按钮
        Button btnToStart = findViewById(R.id.btnToStart);
        btnToStart.setOnClickListener(v -> {
            GoBoard board = boardView.getBoard();
            if (board != null) {
                board.setCurrentMoveNumber(-1);
                boardView.invalidate();
                updateGameTree();
                updateButtonStates();
                updateCommentDisplay();
                Toast.makeText(this, "已回到棋局开始", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 到棋局结尾按钮
        Button btnToEnd = findViewById(R.id.btnToEnd);
        btnToEnd.setOnClickListener(v -> {
            GoBoard board = boardView.getBoard();
            if (board != null) {
                int totalMoves = board.getMoveHistory().size();
                board.setCurrentMoveNumber(totalMoves - 1);
                boardView.invalidate();
                updateGameTree();
                updateButtonStates();
                updateCommentDisplay();
                Toast.makeText(this, "已到棋局结尾", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // 设置棋盘手势
    private void setupBoardGestures() {
        // 实现棋盘手势逻辑
    }
    
    // 更新按钮状态
    private void updateButtonStates() {
        Button btnPrev = findViewById(R.id.btnPrev);
        Button btnNext = findViewById(R.id.btnNext);
        Button btnToStart = findViewById(R.id.btnToStart);
        Button btnToEnd = findViewById(R.id.btnToEnd);
        GoBoard board = boardView.getBoard();
        if (board == null) {
            if (btnPrev != null) btnPrev.setEnabled(false);
            if (btnNext != null) btnNext.setEnabled(false);
            if (btnToStart != null) btnToStart.setEnabled(false);
            if (btnToEnd != null) btnToEnd.setEnabled(false);
            return;
        }

        int cur = board.getCurrentMoveNumber();
        int total = board.getMoveHistory().size();
        if (btnPrev != null) btnPrev.setEnabled(cur >= 0);
        boolean blockNext = (cur < 0) && board.hasStartVariations();
        if (btnNext != null) btnNext.setEnabled(!blockNext && cur < total - 1);
        if (btnToStart != null) btnToStart.setEnabled(total > 0);
        if (btnToEnd != null) btnToEnd.setEnabled(total > 0 && cur < total - 1);
    }
    
    // 打开文件
    private void openFile(int requestCode) {
        Intent intent = new Intent(requestCode == REQUEST_CODE_LOAD ? 
            Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                       Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                       Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        if (requestCode == REQUEST_CODE_SAVE) {
            intent.putExtra(Intent.EXTRA_TITLE, "game.sgf");
            intent.putExtra(Intent.EXTRA_ALLOW_REPLACE, true);
            createDocumentLauncher.launch(intent);
        } else {
            openDocumentLauncher.launch(intent);
        }
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
        if (board == null) return;
        
        // 只显示当前手的注释
        GoBoard.Move currentMove = board.getCurrentMove();
        TextView tvCommentDisplay = findViewById(R.id.tvCommentDisplay);
        if (currentMove != null && currentMove.comment != null && !currentMove.comment.isEmpty()) {
            tvCommentDisplay.setText(currentMove.comment);
        } else {
            tvCommentDisplay.setText("暂无注释");
        }
    }
    
    // 在所有需要更新注释的地方调用updateCommentDisplay()
    public void onBoardClick(int x, int y) {
        GoBoard board = boardView.getBoard();
        if (board != null && !board.isEditMode()) {
            boolean willCreateVariation = board.getCurrentMoveNumber() < board.getMoveHistory().size() - 1;
            if (board.placeStone(x, y)) {
                boardView.invalidate();
                updateGameInfo();
                updateCommentDisplay(); // 添加这行
                updateButtonStates();
                if (willCreateVariation) {
                    Toast.makeText(this, "已创建分支", Toast.LENGTH_SHORT).show();
                }
                updateButtonStates();
            }
        }
    }
    
    public void previousMove() {
        if (boardView.getBoard().previousMove()) {
            boardView.invalidate();
            updateGameInfo();
            updateCommentDisplay(); // 添加这行
            updateButtonStates();
        }
    }
    
    public void nextMove() {
        if (boardView.getBoard().nextMove()) {
            boardView.invalidate();
            updateGameInfo();
            updateCommentDisplay(); // 添加这行
            updateButtonStates();
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
        
        // 更新游戏树视图
        if (treeView != null) {
            treeView.setBoard(board);
        }
    }
    
    // 更新游戏树视图
    private void updateGameTree() {
        if (treeView != null && boardView != null) {
            treeView.setBoard(boardView.getBoard());
        }
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
        if (board == null) {
            Toast.makeText(this, "当前无可选分支", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 处理第一手分支的情况
        if (board.getCurrentMoveNumber() < 0) {
            if (!board.hasStartVariations()) {
                Toast.makeText(this, "当前无可选分支", Toast.LENGTH_SHORT).show();
                return;
            }
            List<List<GoBoard.Move>> startVariations = board.getMoveHistory().get(0).variations;
            
            new AlertDialog.Builder(this)
                .setTitle("选择分支路径")
                .setItems(getBranchTitles(startVariations), (dialog, which) -> {
                    board.selectVariation(which);
                    boardView.invalidate();
                    updateButtonStates();
                    updateGameInfo();
                    updateCommentDisplay();
                })
                .show();
            return;
        }
        
        // 处理其他手的分支情况
        if (board.getCurrentMove() == null) {
            Toast.makeText(this, "当前无可选分支", Toast.LENGTH_SHORT).show();
            return;
        }
        List<List<GoBoard.Move>> currentVariations = board.getCurrentMove().variations;
        if (currentVariations == null || currentVariations.isEmpty()) {
            Toast.makeText(this, "当前手没有分支", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("选择分支路径")
            .setItems(getBranchTitles(currentVariations), (dialog, which) -> {
                board.selectVariation(which);
                boardView.invalidate();
                updateButtonStates();
                updateGameInfo();
                updateCommentDisplay();
            })
            .show();
    }
    
    // 直接根据分支索引进行切换（用于棋盘点击分支提示）
    public void onBranchSelectIndex(int index) {
        GoBoard board = boardView.getBoard();
        if (board == null) {
            Toast.makeText(this, "当前无可选分支", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 处理第一手分支的情况
        if (board.getCurrentMoveNumber() < 0) {
            if (!board.hasStartVariations()) {
                Toast.makeText(this, "当前无可选分支", Toast.LENGTH_SHORT).show();
                return;
            }
            List<List<GoBoard.Move>> vars = board.getMoveHistory().get(0).variations;
            if (vars == null || index < 0 || index >= vars.size()) {
                Toast.makeText(this, "分支不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean ok = board.selectVariation(index);
            if (!ok) {
                Toast.makeText(this, "分支第一步解析失败", Toast.LENGTH_SHORT).show();
                return;
            }
            boardView.invalidate();
            updateButtonStates();
            updateGameInfo();
            updateCommentDisplay();
            return;
        }
        
        // 处理其他手的分支情况
        if (board.getCurrentMove() == null) {
            Toast.makeText(this, "当前无可选分支", Toast.LENGTH_SHORT).show();
            return;
        }
        List<List<GoBoard.Move>> vars = board.getCurrentMove().variations;
        if (vars == null || index < 0 || index >= vars.size()) {
            Toast.makeText(this, "分支不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean ok = board.selectVariation(index);
        if (!ok) {
            Toast.makeText(this, "分支第一步解析失败", Toast.LENGTH_SHORT).show();
            return;
        }
        boardView.invalidate();
        updateButtonStates();
        updateGameInfo();
        updateCommentDisplay();
    }
    
    // 获取分支标题
    private String[] getBranchTitles(List<List<GoBoard.Move>> variations) {
        String[] titles = new String[variations.size()];
        for (int i = 0; i < variations.size(); i++) {
            List<GoBoard.Move> v = variations.get(i);
            String label = "分支 " + (i + 1);
            if (v != null && !v.isEmpty()) {
                GoBoard.Move m = v.get(0);
                if (m.x >= 0 && m.y >= 0) {
                    String color = m.color == 1 ? "黑" : "白";
                    label += ": " + color + " " + toCoord(m.x, m.y);
                }
            }
            titles[i] = label;
        }
        return titles;
    }
    
    private String toCoord(int x, int y) {
        String[] letters = {"A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T"};
        String col = (x >= 0 && x < letters.length) ? letters[x] : String.valueOf((char)('A' + x));
        int row = 19 - y;
        return col + row;
    }
    
    // 移除已弃用的 onActivityResult，改用 Activity Result API
} // 类结束的大括号
