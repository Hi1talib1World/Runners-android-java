package com.denzo.runners;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private List<String> searchResults;

    public SearchAdapter(List<String> searchResults) {
        this.searchResults = searchResults;
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SearchViewHolder holder, int position) {
        String result = searchResults.get(position);
        holder.textView.setText(result);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public SearchViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
