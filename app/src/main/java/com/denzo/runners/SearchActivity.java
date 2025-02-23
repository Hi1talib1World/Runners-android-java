package com.denzo.runners;

import android.os.Bundle;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private SearchAdapter searchAdapter;
    private List<String> searchResults;
    private List<String> allItems;  // All possible search items (for demo purposes)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize search components
        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);

        // Sample data
        allItems = new ArrayList<>();
        allItems.add("Running Shoes");
        allItems.add("Running Watch");
        allItems.add("Sports Gear");
        allItems.add("Fitness Tracker");
        allItems.add("Jogging Pants");

        // RecyclerView setup
        searchResults = new ArrayList<>();
        searchAdapter = new SearchAdapter(searchResults);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(searchAdapter);

        // Set up SearchView listener to filter results
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Handle query submission (optional)
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter search results based on user input
                filterResults(newText);
                return true;
            }
        });
    }

    private void filterResults(String query) {
        searchResults.clear();
        if (query.isEmpty()) {
            searchAdapter.notifyDataSetChanged();
            return;
        }

        // Filter results based on the query
        for (String item : allItems) {
            if (item.toLowerCase().contains(query.toLowerCase())) {
                searchResults.add(item);
            }
        }

        // Notify the adapter about data changes
        searchAdapter.notifyDataSetChanged();

        // If no results are found, show a Toast message
        if (searchResults.isEmpty()) {
            Toast.makeText(SearchActivity.this, "No results found", Toast.LENGTH_SHORT).show();
        }
    }
}
