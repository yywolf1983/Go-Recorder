package com.gosgf.app;  // 确保包声明正确

import android.util.Log;
import android.os.Bundle;
import android.widget.TextView;
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
import java.util.List;
import java.util.Map;
import com.gosgf.app.view.BoardView;
import com.gosgf.app.model.GoBoard;
import com.gosgf.app.R;
import com.gosgf.app.util.SGFParser;
import android.view.View;
import android.view.MotionEvent;
import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_LOAD = 1;
    private static final int REQUEST_CODE_SAVE = 2;
    private static final int BOARD_SIZE = 19;
    
    // UI组件
    private View toolbar;
    private Button btnNew, btnLoad, btnSave;
    private Button btnToStart, btnPrev, btnNext, btnPass;
    private Button btnComment, btnMark;
    private TextView tvGameInfo, tvCommentDisplay;
    private BoardView boardView;
    
    // 其他变量
    private float startX, startY;
    private ActivityResultLauncher<Intent> openDocumentLauncher;
    private ActivityResultLauncher<Intent> createDocumentLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化UI组件
        initUI();
        
        // 初始化文件选择器
        initFileLaunchers();
        
        // 设置事件监听器
        setupListeners();
        
        // 初始化游戏信息
        updateGameInfo();
    }
    
    private void initUI() {
        // 工具栏按钮
        toolbar = findViewById(R.id.toolbar);
        btnNew = findViewById(R.id.btnNew);
        btnLoad = findViewById(R.id.btnLoad);
        btnSave = findViewById(R.id.btnSave);
        
        // 导航按钮
        btnToStart = findViewById(R.id.btnToStart);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnPass = findViewById(R.id.btnPass);
        
        // 功能按钮
        btnComment = findViewById(R.id.btnComment);
        btnMark = findViewById(R.id.btnMark);
        // 移除分支选择按钮，所有分支操作直接在棋盘上完成
        
        // 信息显示
        tvGameInfo = findViewById(R.id.tvGameInfo);
        tvCommentDisplay = findViewById(R.id.tvCommentDisplay);
        
        // 棋盘视图
        boardView = findViewById(R.id.board_view);
    }
    
    private void initFileLaunchers() {
        openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        loadSGF(uri);
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
                        saveSGF(uri);
                    }
                }
            }
        );
    }
    
    private void setupListeners() {
        // 工具栏按钮
        btnNew.setOnClickListener(v -> newGame());
        btnLoad.setOnClickListener(v -> openFile(REQUEST_CODE_LOAD));
        btnSave.setOnClickListener(v -> openFile(REQUEST_CODE_SAVE));
        
        // 导航按钮
        btnToStart.setOnClickListener(v -> goToStart());
        btnPrev.setOnClickListener(v -> previousMove());
        btnNext.setOnClickListener(v -> nextMove());
        btnPass.setOnClickListener(v -> passMove());
        
        // 功能按钮
        btnComment.setOnClickListener(v -> showCommentDialog());
        btnMark.setOnClickListener(v -> showMarkDialog());
        // 移除分支选择按钮的点击事件，所有分支操作直接在棋盘上完成
        
        // 棋盘手势操作由BoardView自己处理
    }
    
    private void newGame() {
        boardView.getBoard().resetGame();
        boardView.invalidateBoard();
        updateGameInfo();
        updateCommentDisplay();
        Toast.makeText(this, "新游戏已创建", Toast.LENGTH_SHORT).show();
    }
    
    private void loadSGF(Uri uri) {
        // 添加加载确认对话框
        new AlertDialog.Builder(this)
            .setTitle("确认加载")
            .setMessage("确定要加载这个SGF文件吗？当前棋局将被替换。")
            .setPositiveButton("确定", (dialog, which) -> {
                doLoadSGF(uri);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void doLoadSGF(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            try {
                // 不调用 resetGame()，直接解析 SGF，这样不会清空 startVariations
                SGFParser.parseSGF(sb.toString(), boardView.getBoard());
                boardView.invalidateBoard();
                updateGameInfo();
                updateCommentDisplay();
                
                // 检查是否包含虚手
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
                
                Toast.makeText(this, "棋局已加载", Toast.LENGTH_SHORT).show();
            } catch (ArrayIndexOutOfBoundsException e) {
                Toast.makeText(this, "SGF文件包含无效坐标", Toast.LENGTH_SHORT).show();
                boardView.getBoard().resetGame();
                boardView.invalidateBoard();
            } catch (SGFParser.SGFParseException e) {
                Toast.makeText(this, "无效的SGF格式: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveSGF(Uri uri) {
        doSaveSGF(uri);
    }
    
    private void doSaveSGF(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri, "w")) { // 添加"w"参数确保覆盖现有文件
            GoBoard board = boardView.getBoard();
            String sgf = SGFParser.saveToString(board, board.getBlackPlayer(), board.getWhitePlayer(), board.getResult());
            os.write(sgf.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "棋局已保存", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openFile(int requestCode) {
        if (requestCode == REQUEST_CODE_LOAD) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // 使用更通用的文件类型，确保能找到SGF文件
            intent.setType("*/*");
            // 添加SGF文件扩展名过滤器
            String[] mimeTypes = {"application/sgf", "text/plain", "*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            openDocumentLauncher.launch(intent);
        } else if (requestCode == REQUEST_CODE_SAVE) {
            // 使用ACTION_CREATE_DOCUMENT来创建或覆盖文件
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // 使用更通用的文件类型，确保能找到SGF文件
            intent.setType("*/*");
            // 添加SGF文件扩展名过滤器
            String[] mimeTypes = {"application/sgf", "text/plain", "*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            // 设置默认文件名
            GoBoard board = boardView.getBoard();
            String defaultFileName = "game.sgf";
            if (board != null && !board.getBlackPlayer().isEmpty()) {
                defaultFileName = board.getBlackPlayer() + "_vs_" + board.getWhitePlayer() + ".sgf";
            }
            intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);
            createDocumentLauncher.launch(intent);
        }
    }
    
    private void goToStart() {
        GoBoard board = boardView.getBoard();
        board.setCurrentMoveNumber(-1);
        boardView.invalidateBoard();
        updateGameInfo();
        updateCommentDisplay();
    }
    
    private void previousMove() {
        GoBoard board = boardView.getBoard();
        if (board.getCurrentMoveNumber() == 0) {
            // 如果在第一手，回到起始状态
            board.setCurrentMoveNumber(-1);
        } else {
            board.previousMove();
        }
        boardView.invalidateBoard();
        updateGameInfo();
        updateCommentDisplay();
    }
    
    private void nextMove() {
        GoBoard board = boardView.getBoard();
        if (board.getCurrentMoveNumber() < 0) {
            // 如果在起始状态，直接进入第一步
            if (board.getMoveHistory().size() > 0) {
                // 如果已经有moveHistory，直接前进到第一步
                // 这样可以保持当前的分支，而不是默认选择第一个分支
                board.setCurrentMoveNumber(0);
                boardView.invalidateBoard();
                updateGameInfo();
                updateCommentDisplay();
            } else if (board.hasStartVariations()) {
                // 如果没有moveHistory，但是有startVariations，选择第一个分支
                board.selectVariation(0);
                // 选择分支后，手动前进到第一步
                board.setCurrentMoveNumber(0);
                boardView.invalidateBoard();
                updateGameInfo();
                updateCommentDisplay();
            }
        } else {
            // 直接执行下一步移动
            board.nextMove();
            boardView.invalidateBoard();
            updateGameInfo();
            updateCommentDisplay();
        }
    }
    
    private void passMove() {
        GoBoard board = boardView.getBoard();
        if (board.placeStone(-1, -1)) {
            boardView.invalidateBoard();
            updateGameInfo();
            updateCommentDisplay();
            Toast.makeText(this, "虚手", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showCommentDialog() {
        GoBoard board = boardView.getBoard();
        if (board.getCurrentMoveNumber() < 0) {
            Toast.makeText(this, "请先落子或选择分支", Toast.LENGTH_SHORT).show();
            return;
        }
        
        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setLines(3);
        editText.setText(board.getComment());
        
        new AlertDialog.Builder(this)
            .setTitle("添加注释")
            .setView(editText)
            .setPositiveButton("确定", (dialog, which) -> {
                board.setComment(editText.getText().toString());
                updateCommentDisplay();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showMarkDialog() {
        GoBoard board = boardView.getBoard();
        if (board.getCurrentMoveNumber() < 0) {
            Toast.makeText(this, "请先落子或选择分支", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] markOptions = {"无标记", "三角形", "方形", "圆形", "X标记"};
        
        new AlertDialog.Builder(this)
            .setTitle("添加标记")
            .setItems(markOptions, (dialog, which) -> {
                board.setMark(which);
                boardView.invalidateBoard();
            })
            .show();
    }
    
    public void showDeleteVariationConfirmDialog(int index, boolean isStartVariation) {
        GoBoard board = boardView.getBoard();
        String variationName = "";
        
        if (isStartVariation) {
            List<List<GoBoard.Move>> startVariations = board.getStartVariations();
            if (index >= 0 && index < startVariations.size()) {
                GoBoard.Variation variation = board.getStartVariation(index);
                if (variation != null) {
                    variationName = variation.getName();
                }
            }
        } else {
            if (board.getCurrentMoveNumber() >= 0) {
                GoBoard.Move current = board.getMoveHistory().get(board.getCurrentMoveNumber());
                if (current != null && index >= 0 && index < current.variations.size()) {
                    GoBoard.Variation variation = current.variations.get(index);
                    if (variation != null) {
                        variationName = variation.getName();
                    }
                }
            }
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除分支\"" + variationName + "\"吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                boolean success = false;
                if (isStartVariation) {
                    success = board.removeStartVariation(index);
                } else {
                    success = board.removeCurrentVariation(index);
                }
                
                if (success) {
                    boardView.invalidateBoard();
                    updateGameInfo();
                    updateCommentDisplay();
                    Toast.makeText(this, "分支已删除", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    // 所有分支选择相关的弹出窗口已移除，分支操作直接在棋盘上完成
    

    
    // 添加一个方法让BoardView调用，用于处理落子
    public void handleBoardClick(int x, int y) {
        GoBoard board = boardView.getBoard();
        if (board != null) {
            if (board.placeStone(x, y)) {
                boardView.invalidateBoard();
                updateGameInfo();
                updateCommentDisplay();
            } else {
                // 落子失败，检查原因并给出提示
                if (board.getStone(x, y) != 0) {
                    Toast.makeText(this, "该位置已有棋子", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "落子失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    public void updateGameInfo() {
        GoBoard board = boardView.getBoard();
        int moveNumber = board.getCurrentMoveNumber();
        int totalMoves = board.getMoveHistory().size();
        
        StringBuilder info = new StringBuilder();
        if (moveNumber < 0) {
            info.append("起始状态");
        } else {
            info.append("第").append(moveNumber + 1).append("手 / 共").append(totalMoves).append("手");
        }
        
        info.append(" - ");
        info.append(board.getCurrentPlayer() == 1 ? "黑方" : "白方").append("回合");
        
        // 添加分支信息
        List<String> variationNames = board.getVariationNames();
        if (!variationNames.isEmpty()) {
            info.append("\n分支: ").append(variationNames.size()).append("个");
        }
        
        // 添加玩家信息
        String blackPlayer = board.getBlackPlayer();
        String whitePlayer = board.getWhitePlayer();
        if ((blackPlayer != null && !blackPlayer.isEmpty()) || (whitePlayer != null && !whitePlayer.isEmpty())) {
            info.append("\n").append(blackPlayer != null ? blackPlayer : "").append(" vs " ).append(whitePlayer != null ? whitePlayer : "");
        }
        
        // 添加结果信息
        String result = board.getResult();
        if (result != null && !result.isEmpty()) {
            info.append("\n结果: " ).append(result);
        }
        
        tvGameInfo.setText(info.toString());
        // 添加点击事件，显示更多游戏信息
        tvGameInfo.setOnClickListener(v -> showGameInfoDialog());
    }
    
    private void showGameInfoDialog() {
        GoBoard board = boardView.getBoard();
        StringBuilder info = new StringBuilder();
        
        // 基本信息
        info.append("游戏信息:\n\n");
        
        // 玩家信息
        String blackPlayer = board.getBlackPlayer();
        String whitePlayer = board.getWhitePlayer();
        if ((blackPlayer != null && !blackPlayer.isEmpty()) || (whitePlayer != null && !whitePlayer.isEmpty())) {
            info.append("对局: " ).append(blackPlayer != null ? blackPlayer : "").append(" vs " ).append(whitePlayer != null ? whitePlayer : "\n");
        }
        
        // 结果信息
        String result = board.getResult();
        if (result != null && !result.isEmpty()) {
            info.append("结果: " ).append(result).append("\n");
        }
        
        // 棋盘信息
        info.append("棋盘大小: 19x19\n");
        
        // 步数信息
        int totalMoves = board.getMoveHistory().size();
        info.append("总步数: " ).append(totalMoves).append("\n");
        
        // 分支信息
        List<String> variationNames = board.getVariationNames();
        if (!variationNames.isEmpty()) {
            info.append("\n当前分支: " ).append(variationNames.size()).append("个\n");
            for (int i = 0; i < variationNames.size(); i++) {
                info.append("  " ).append(i + 1).append(". " ).append(variationNames.get(i)).append("\n");
            }
        }
        
        // 分支结构信息
        String structure = board.getVariationStructure();
        if (structure != null && !structure.isEmpty()) {
            info.append("\n分支结构:\n" ).append(structure);
        }
        
        new AlertDialog.Builder(this)
            .setTitle("详细游戏信息")
            .setMessage(info.toString())
            .setPositiveButton("确定", null)
            .show();
    }
    
    public void updateCommentDisplay() {
        GoBoard board = boardView.getBoard();
        String comment = board.getComment();
        if (comment == null || comment.isEmpty()) {
            tvCommentDisplay.setText("暂无注释");
            tvCommentDisplay.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            tvCommentDisplay.setText(comment);
            tvCommentDisplay.setTextColor(getResources().getColor(android.R.color.black));
            // 增加注释的显示效果
            tvCommentDisplay.setLineSpacing(4f, 1.2f);
        }
    }
}