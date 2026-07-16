package com.denzo.runners.features.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.databinding.FragmentSearchBinding
import com.denzo.runners.databinding.ItemSearchResultBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeUiState()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: SearchUiState) {
        binding.searchLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        val adapter = SearchResultAdapter(state.results) { result ->
            viewModel.onActionClicked(result)
        }
        binding.rvSearchResults.adapter = adapter
        
        binding.tvNoResults.visibility = if (state.results.isEmpty() && !state.isLoading && state.query.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SearchResultAdapter(
        private val items: List<SearchResult>,
        private val onActionClick: (SearchResult) -> Unit
    ) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemSearchResultBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root) {
            
            fun bind(result: SearchResult) {
                b.tvResultName.text = result.name
                b.tvResultSub.text = result.description
                
                if (result.isActionTaken) {
                    b.btnAction.text = "FOLLOWING"
                    b.btnAction.alpha = 0.5f
                } else {
                    b.btnAction.text = "FOLLOW"
                    b.btnAction.alpha = 1.0f
                }

                b.btnAction.setOnClickListener { onActionClick(result) }
            }
        }
    }
}
