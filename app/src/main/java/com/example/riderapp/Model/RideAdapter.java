package com.example.riderapp.Model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.riderapp.R;

import java.util.ArrayList;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private ArrayList<RideItem> mExampleList;

    public RideAdapter(ArrayList<RideItem> exampleList) {
        mExampleList = exampleList;
    }
    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        RideViewHolder evh = new RideViewHolder(v);
        return evh;
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        RideItem currentItem = mExampleList.get(position);


        holder.mTextView1.setText(currentItem.getText_pickup());
        holder.mTextView2.setText(currentItem.getText_drop_off());
        holder.mDateText.setText(currentItem.getText_date());
    }

    @Override
    public int getItemCount() {
        return mExampleList.size();
    }

    public static class RideViewHolder extends RecyclerView.ViewHolder {


        public TextView mTextView1;
        public TextView mTextView2;
        public TextView mDateText;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);


            mTextView1 = itemView.findViewById(R.id.text_pickup);
            mTextView2 = itemView.findViewById(R.id.text_dropoff);
            mDateText = itemView.findViewById(R.id.text_date_time);
        }
    }
}
