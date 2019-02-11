package de.atm_search.cseh_17.geld_kompass;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.MyViewHolder> {

    private LayoutInflater inflater;
    List<RVRowInformation> data;

    RVAdapter(Context context, List<RVRowInformation> data){

        inflater = LayoutInflater.from(context);
        this.data = data;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.cutsom_rv_row, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        RVRowInformation current = data.get(position);

        holder.icon.setImageResource(current.iconId);
        String rowSubtitle = "ungefähre Entfernung: " + current.rowSubtitle + " m";
        holder.title.setText(current.rowTitle);
        holder.subtitle.setText(rowSubtitle);

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder{

        ImageView icon;
        TextView title;
        TextView subtitle;

        private MyViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.rv_row_icon);
            title = itemView.findViewById(R.id.rv_row_title);
            subtitle = itemView.findViewById(R.id.rv_row_subtitle);

        }
    }
}
