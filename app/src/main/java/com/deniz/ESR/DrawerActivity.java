package com.deniz.ESR;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class DrawerActivity extends AppCompatActivity {
    protected DrawerLayout drawerLayout;
    protected ActionBarDrawerToggle toggle;
    protected NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        updateMenuItems();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_login) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_register) {
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_logout) {
                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("currentUserId");
                editor.apply();

                navigationView.getMenu().clear();
                navigationView.inflateMenu(R.menu.drawer_menu);
                updateMenuItems();
                Toast.makeText(this, "Çıkış yapıldı.", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.menu_home) {
                Intent intent = new Intent(this, MapsActivity.class);
                startActivity(intent);
            } else if (id == R.id.menu_favorites) {
                Intent intent = new Intent(this, FavoriteRoutesActivity.class);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onResume() {
        super.onResume();
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.drawer_menu);
        updateMenuItems();
    }

    protected void updateMenuItems() {

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = sharedPreferences.getInt("currentUserId", -1);

        Menu menu = navigationView.getMenu();
        menu.clear();
        navigationView.inflateMenu(R.menu.drawer_menu);

        boolean isLoggedIn = userId != -1;
        menu.findItem(R.id.nav_login).setVisible(!isLoggedIn);
        menu.findItem(R.id.nav_register).setVisible(!isLoggedIn);
        menu.findItem(R.id.nav_logout).setVisible(isLoggedIn);
    }


}
