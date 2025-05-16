package com.deniz.ESR;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import java.util.List;


public class FavoriteRoutesActivity extends DrawerActivity {
    private RecyclerView recyclerView;
    private TextView tvNoFavorites;
    private FavoriteRoutesAdapter adapter;
    private List<String> favoriteRoutes;
    private FavoritesDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_favorite_routes);
        setTitle("Favori Rotalar");

        setupDrawer();

        tvNoFavorites = findViewById(R.id.tv_no_favorites);
        recyclerView = findViewById(R.id.rv_favorite_routes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        DividerItemDecoration divider = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.item_decoration));
        recyclerView.addItemDecoration(divider);

        db = new FavoritesDatabase(this);

        loadFavoriteRoutes();

    }



    private void loadFavoriteRoutes() {

        favoriteRoutes = db.getAllFavorites();

        if (favoriteRoutes.isEmpty()) {
            tvNoFavorites.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoFavorites.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new FavoriteRoutesAdapter(favoriteRoutes);
                recyclerView.setAdapter(adapter);

                adapter.setOnRouteClickListener(routeData -> {
                    Intent intent = new Intent(FavoriteRoutesActivity.this, MapsActivity.class);
                    intent.putExtra("selectedRoute", new Gson().toJson(routeData));
                    startActivity(intent);
                });

                adapter.setOnRouteLongClickListener((routeJson, position) -> showDeleteConfirmationDialog(routeJson, position));
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }
    private void showDeleteConfirmationDialog(String routeJson, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Sil")
                .setMessage("Bu rotayı silmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> deleteFavoriteRoute(routeJson, position))
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void deleteFavoriteRoute(String routeJson, int position) {

        db.deleteFavorite(routeJson);
        favoriteRoutes.remove(position);
        adapter.notifyItemRemoved(position);

        if (favoriteRoutes.isEmpty()) {
            tvNoFavorites.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        Toast.makeText(this, "Favori rota silindi.", Toast.LENGTH_SHORT).show();
    }
}


