package com.denzo.runners.features.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.denzo.runners.R
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
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onQueryChanged(s?.toString() ?: "")
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
        
        val displayList = if (state.query.isEmpty()) state.featured else state.results
        
        binding.tvEmptyState.visibility = if (displayList.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
        
        val adapter = SearchResultAdapter(displayList) { result ->
            viewModel.onActionClicked(result)
        }
        binding.rvSearchResults.adapter = adapter
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
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val itemBinding: ItemSearchResultBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            
            fun bind(item: SearchResult) {
                itemBinding.tvName.text = item.name
                itemBinding.tvDescription.text = item.description
                
                if (item.type == SearchResultType.CLUB) {
                    itemBinding.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
                    itemBinding.btnAction.text = if (item.isActionTaken) "JOINED" else "JOIN"
                } else {
                    itemBinding.ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
                    itemBinding.btnAction.text = if (item.isActionTaken) "FOLLOWING" else "FOLLOW"
                }

                if (item.isActionTaken) {
                    itemBinding.btnAction.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_card_bg))
                    itemBinding.btnAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.runners_text_secondary))
                } else {
                    itemBinding.btnAction.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.runners_volt))
                    itemBinding.btnAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary))
                }

                itemBinding.btnAction.setOnClickListener {
                    onActionClick(item)
                }
            }
        }
    }
}
