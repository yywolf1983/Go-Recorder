package com.gosgf.app;

import android.content.Intent; // 添加缺失的Intent导入
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class GameInfoActivity extends AppCompatActivity {
    private EditText etBlack, etWhite, etResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_info);
        
        etBlack = findViewById(R.id.etBlack);
        etWhite = findViewById(R.id.etWhite);
        etResult = findViewById(R.id.etResult);
        
        Button btnSave = findViewById(R.id.btnSaveInfo);
        btnSave.setOnClickListener(v -> saveAndReturn());
    }

    private void saveAndReturn() {
        Intent data = new Intent();
        data.putExtra("black", etBlack.getText().toString());
        data.putExtra("white", etWhite.getText().toString());
        data.putExtra("result", etResult.getText().toString());
        setResult(RESULT_OK, data);
        finish();
    }
}