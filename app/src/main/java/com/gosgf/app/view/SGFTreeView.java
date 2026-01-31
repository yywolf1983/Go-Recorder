package com.gosgf.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.gosgf.app.model.GoBoard;
import java.util.ArrayList;
import java.util.List;

public class SGFTreeView extends View {
    private Paint linePaint;
    private Paint nodePaint;
    private Paint textPaint;
    private Paint currentNodePaint;
    private Paint branchLabelPaint;
    private Paint inactiveNodePaint;
    private Paint inactiveLinePaint;
    
    private GoBoard board;
    private int nodeSize = 24;
    private int horizontalSpacing = 30;
    private int verticalSpacing = 18;
    private int branchVerticalSpacing = 16;
    
    private List<TreeNode> treeNodes;
    
    private static class TreeNode {
        int moveNumber;
        float x;
        float y;
        int branchIndex;
        int parentIndex;
        GoBoard.Move move;
        
        TreeNode(int moveNumber, float x, float y, int branchIndex, int parentIndex, GoBoard.Move move) {
            this.moveNumber = moveNumber;
            this.x = x;
            this.y = y;
            this.branchIndex = branchIndex;
            this.parentIndex = parentIndex;
            this.move = move;
        }
    }
    
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
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(3f);
        linePaint.setAntiAlias(true);
        
        nodePaint = new Paint();
        nodePaint.setColor(Color.LTGRAY);
        nodePaint.setStyle(Paint.Style.FILL);
        
        currentNodePaint = new Paint();
        currentNodePaint.setColor(Color.RED);
        currentNodePaint.setStyle(Paint.Style.FILL);
        
        inactiveNodePaint = new Paint();
        inactiveNodePaint.setColor(Color.rgb(200, 200, 200));
        inactiveNodePaint.setStyle(Paint.Style.FILL);
        
        inactiveLinePaint = new Paint();
        inactiveLinePaint.setColor(Color.rgb(200, 200, 200));
        inactiveLinePaint.setStrokeWidth(1.5f);
        inactiveLinePaint.setAntiAlias(true);
        
        branchLabelPaint = new Paint();
        branchLabelPaint.setColor(Color.BLUE);
        branchLabelPaint.setTextSize(10f);
        branchLabelPaint.setTextAlign(Paint.Align.LEFT);
        
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(14f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        treeNodes = new ArrayList<>();
    }
    
    public void setBoard(GoBoard board) {
        this.board = board;
        buildLinearTree();
        invalidate();
    }
    
    private void buildLinearTree() {
        treeNodes.clear();
        
        if (board == null) {
            return;
        }
        
        List<GoBoard.Move> history = board.getMoveHistory();
        if (history == null || history.isEmpty()) {
            return;
        }
        
        float startX = 40f;
        float startY = 40f;
        float branchSpacing = 60f;
        
        // 处理第一手分支（Sabaki风格）
        if (board.getCurrentMoveNumber() < 0 && !history.get(0).variations.isEmpty()) {
            List<List<GoBoard.Move>> startVariations = history.get(0).variations;
            float currentY = startY;
            
            // 为每个第一手分支构建树节点
            for (int i = 0; i < startVariations.size(); i++) {
                List<GoBoard.Move> variation = startVariations.get(i);
                if (!variation.isEmpty()) {
                    // 构建分支
                    buildBranchHorizontal(variation, i + 1, startX, currentY);
                    currentY += branchSpacing;
                }
            }
            requestLayout();
            return;
        }
        
        // Build main branch (horizontal)
        buildBranchHorizontal(history, 0, startX, startY);
        
        // Build variations - horizontal layout similar to Sabaki
        float currentY = startY + branchSpacing;
        for (int i = 0; i < history.size(); i++) {
            GoBoard.Move move = history.get(i);
            if (!move.variations.isEmpty()) {
                for (int j = 0; j < move.variations.size(); j++) {
                    List<GoBoard.Move> variation = move.variations.get(j);
                    if (!variation.isEmpty()) {
                        // Build variation branch
                        buildBranchHorizontal(variation, j + 1, startX, currentY);
                        currentY += branchSpacing;
                    }
                }
            }
        }
        
        requestLayout();
    }
    
    private TreeNode findNodeByMoveNumber(int moveNumber) {
        for (TreeNode node : treeNodes) {
            if (node.moveNumber == moveNumber && node.branchIndex == 0) {
                return node;
            }
        }
        return null;
    }
    
    private void buildBranch(List<GoBoard.Move> moves, int branchIndex, float startX, float startY) {
        for (int i = 0; i < moves.size(); i++) {
            GoBoard.Move move = moves.get(i);
            float x = startX + i * horizontalSpacing;
            float y = startY;
            TreeNode node = new TreeNode(i, x, y, branchIndex, i - 1, move);
            treeNodes.add(node);
        }
    }
    
    private void buildBranchHorizontal(List<GoBoard.Move> moves, int branchIndex, float startX, float startY) {
        for (int i = 0; i < moves.size(); i++) {
            GoBoard.Move move = moves.get(i);
            float x = startX + i * horizontalSpacing;
            float y = startY;
            TreeNode node = new TreeNode(i, x, y, branchIndex, i - 1, move);
            treeNodes.add(node);
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (treeNodes.isEmpty()) {
            return;
        }
        
        canvas.drawColor(Color.rgb(250, 250, 250));
        
        int currentMove = board != null ? board.getCurrentMoveNumber() : -1;
        
        // Determine current branch index
        int currentBranchIndex = 0;
        if (currentMove >= 0 && !treeNodes.isEmpty()) {
            for (TreeNode node : treeNodes) {
                if (node.moveNumber == currentMove && node.branchIndex > 0) {
                    currentBranchIndex = node.branchIndex;
                    break;
                }
            }
        }
        
        // Group nodes by branch
        List<List<TreeNode>> branches = new ArrayList<>();
        for (TreeNode node : treeNodes) {
            while (branches.size() <= node.branchIndex) {
                branches.add(new ArrayList<>());
            }
            branches.get(node.branchIndex).add(node);
        }
        
        // Draw each branch
        for (int b = 0; b < branches.size(); b++) {
            List<TreeNode> branch = branches.get(b);
            boolean isCurrentBranch = (b == currentBranchIndex);
            
            // Draw branch label
            if (b > 0 && !branch.isEmpty()) {
                String label = "分支" + b;
                Paint labelPaint = isCurrentBranch ? branchLabelPaint : inactiveLinePaint;
                // Find the first non-label node in the branch
                TreeNode firstNode = null;
                for (TreeNode node : branch) {
                    if (node.moveNumber != -1) {
                        firstNode = node;
                        break;
                    }
                }
                if (firstNode != null) {
                    // Draw label near the start of the branch
                    canvas.drawText(label, firstNode.x - 50, firstNode.y - 10, labelPaint);
                }
            }
            
            // Draw main branch label
            if (b == 0 && !branch.isEmpty()) {
                String label = "主分支";
                Paint labelPaint = isCurrentBranch ? branchLabelPaint : inactiveLinePaint;
                // Find the first non-label node in the branch
                TreeNode firstNode = null;
                for (TreeNode node : branch) {
                    if (node.moveNumber != -1) {
                        firstNode = node;
                        break;
                    }
                }
                if (firstNode != null) {
                    // Draw label near the start of the branch
                    canvas.drawText(label, firstNode.x - 50, firstNode.y - 10, labelPaint);
                }
            }
            
            // Draw connection lines from main branch to variations
            if (b > 0 && !branch.isEmpty()) {
                // Find the first non-label node in the variation branch
                TreeNode firstVarNode = null;
                for (TreeNode node : branch) {
                    if (node.moveNumber != -1) {
                        firstVarNode = node;
                        break;
                    }
                }
                if (firstVarNode != null) {
                    // Find corresponding node in main branch
                    TreeNode mainNode = findNodeByMoveNumber(firstVarNode.moveNumber);
                    if (mainNode != null) {
                        // Draw connection line
                        Paint linePaintToUse = isCurrentBranch ? linePaint : inactiveLinePaint;
                        canvas.drawLine(mainNode.x, mainNode.y, firstVarNode.x, firstVarNode.y, linePaintToUse);
                    }
                }
            }
            
            // Draw branch lines and nodes
            for (int i = 0; i < branch.size(); i++) {
                TreeNode node = branch.get(i);
                
                // Skip label nodes (moveNumber == -1)
                if (node.moveNumber == -1) {
                    continue;
                }
                
                // Draw lines between nodes
                if (i > 0) {
                    TreeNode prevNode = null;
                    for (int j = i - 1; j >= 0; j--) {
                        if (branch.get(j).moveNumber != -1) {
                            prevNode = branch.get(j);
                            break;
                        }
                    }
                    if (prevNode != null) {
                        Paint linePaintToUse = isCurrentBranch ? linePaint : inactiveLinePaint;
                        canvas.drawLine(prevNode.x, prevNode.y, node.x, node.y, linePaintToUse);
                    }
                }
                
                // Draw nodes
                boolean isCurrent = (node.moveNumber == currentMove && b == 0) || 
                                   (currentBranchIndex == b && node.moveNumber == currentMove);
                Paint paint;
                if (isCurrent) {
                    paint = currentNodePaint;
                } else if (isCurrentBranch) {
                    paint = nodePaint;
                } else {
                    paint = inactiveNodePaint;
                }
                canvas.drawCircle(node.x, node.y, nodeSize / 2, paint);
                
                // Draw move number
                Paint textPaintToUse = isCurrentBranch ? textPaint : inactiveLinePaint;
                canvas.drawText(String.valueOf(node.moveNumber + 1), node.x, node.y + 6, textPaintToUse);
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            
            for (TreeNode node : treeNodes) {
                float dx = x - node.x;
                float dy = y - node.y;
                if (dx * dx + dy * dy <= (nodeSize / 2 + 5) * (nodeSize / 2 + 5)) {
                    if (board != null) {
                        // 处理第一手分支的点击
                        if (board.getCurrentMoveNumber() < 0 && node.branchIndex > 0) {
                            // 调用MainActivity的onBranchSelectIndex方法
                            if (getContext() instanceof android.app.Activity) {
                                android.app.Activity activity = (android.app.Activity) getContext();
                                try {
                                    java.lang.reflect.Method method = activity.getClass().getMethod("onBranchSelectIndex", int.class);
                                    method.invoke(activity, node.branchIndex - 1);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            board.setCurrentMoveNumber(node.moveNumber);
                        }
                        invalidate();
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;
        
        for (TreeNode node : treeNodes) {
            width = Math.max(width, (int)(node.x + nodeSize + 20));
            height = Math.max(height, (int)(node.y + nodeSize + 10));
        }
        
        width = Math.max(width, getSuggestedMinimumWidth());
        height = Math.max(height, getSuggestedMinimumHeight());
        
        setMeasuredDimension(width, height);
    }
}