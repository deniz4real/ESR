package com.deniz.ESR;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class RegisterActivity extends DrawerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setupDrawer();
        EditText etFirstName = findViewById(R.id.et_first_name);
        EditText etLastName = findViewById(R.id.et_last_name);
        EditText etUsername = findViewById(R.id.et_username);
        EditText etPassword = findViewById(R.id.et_password);
        EditText etBloodType = findViewById(R.id.et_blood_type);
        EditText etEmergencyContactName = findViewById(R.id.et_emergency_contact_name);
        EditText etEmergencyContactPhone = findViewById(R.id.et_emergency_contact_phone);
        Button btnRegister = findViewById(R.id.btn_register);

        UsersDatabase usersDatabase = new UsersDatabase(this);

        btnRegister.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString();
            String lastName = etLastName.getText().toString();
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            String bloodType = etBloodType.getText().toString();
            String emergencyContactName = etEmergencyContactName.getText().toString();
            String emergencyContactPhone = etEmergencyContactPhone.getText().toString();

            if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show();
                return;
            }

            long userId = usersDatabase.addUser(firstName, lastName, username, password, bloodType, emergencyContactName, emergencyContactPhone);

            if (userId != -1) {
                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("currentUserId", (int) userId);
                editor.apply();

                Toast.makeText(this, "Kayıt başarıyla tamamlandı", Toast.LENGTH_SHORT).show();


                Intent intent = new Intent(this, PlaceDetailActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Bu kullanıcı adı zaten kullanılıyor. Lütfen başka bir kullanıcı adı deneyin.", Toast.LENGTH_SHORT).show();
            }

        });
    }
}
