package com.deniz.ESR;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;



public class LoginActivity extends DrawerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupDrawer();

        EditText etLoginUsername = findViewById(R.id.et_login_username);
        EditText etLoginPassword = findViewById(R.id.et_login_password);
        Button btnLogin = findViewById(R.id.btn_login);

        UsersDatabase usersDatabase = new UsersDatabase(this);

        btnLogin.setOnClickListener(v -> {
            String username = etLoginUsername.getText().toString();
            String password = etLoginPassword.getText().toString();
            int userId = usersDatabase.authenticateUser(username, password);

            if (userId != -1) {
                // Giriş başarılı
                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("currentUserId", userId);
                editor.apply();
                Toast.makeText(this, "Hoş geldiniz!", Toast.LENGTH_SHORT).show();
                finish(); // Aktiviteyi kapat ya da yönlendirme yap
            } else {
                // Giriş başarısız
                Toast.makeText(this, "Hatalı kullanıcı adı veya şifre", Toast.LENGTH_SHORT).show();
            }

        });
    }
}

