package com.deniz.ESR;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import java.util.List;


public class FavoriteRoutesAdapter extends RecyclerView.Adapter<FavoriteRoutesAdapter.ViewHolder> {

    private List<String> favoriteRoutes;
    private OnRouteClickListener listener;

    public FavoriteRoutesAdapter(List<String> favoriteRoutes) {
        this.favoriteRoutes = favoriteRoutes;
    }
    public void setOnRouteClickListener(OnRouteClickListener listener) {
        this.listener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String routeJson = favoriteRoutes.get(position);
        RouteData routeData = new Gson().fromJson(routeJson, RouteData.class);

        String startAddress = routeData.getStartAddress();
        String endAddress = routeData.getEndAddress();

        String formattedText = "<br><b>Başlangıç:</b> " + startAddress + "<br><br><b>Varış:</b> <br><br>" + endAddress;
        holder.textView.setText(android.text.Html.fromHtml(formattedText));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRouteSelected(routeData);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onRouteLongClicked(routeJson, position);
            }
            return true;
        });

    }


    @Override
    public int getItemCount() {
        return favoriteRoutes.size();
    }


    public interface OnRouteClickListener {
        void onRouteSelected(RouteData routeData);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }



    public interface OnRouteLongClickListener {
        void onRouteLongClicked(String routeJson, int position);
    }

    private OnRouteLongClickListener longClickListener;

    public void setOnRouteLongClickListener(OnRouteLongClickListener listener) {
        this.longClickListener = listener;
    }

}

