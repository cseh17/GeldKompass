package com.atm_search.cseh_17.geld_kompass;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.MyViewHolder> {

    private LayoutInflater inflater;
    List<RVRowInformation> data;

    RVAdapter(Context context, List<RVRowInformation> data){

        inflater = LayoutInflater.from(context);
        this.data = data;
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.cutsom_rv_row, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        RVRowInformation current = data.get(position);

        holder.icon.setImageResource(current.iconId);
        String rowSubtitle = "umgefähre Entfernung: " + current.rowSubtitle + " m";
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
