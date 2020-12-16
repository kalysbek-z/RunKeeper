package com.example.runkeeper.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.runkeeper.R
import com.example.runkeeper.ui.Adapter
import com.example.runkeeper.ui.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.history_fragment.*

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.history_fragment) {

    private val viewModel: HistoryViewModel by viewModels()

    private lateinit var runAdapter: Adapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()

        viewModel.getAllRuns.observe(viewLifecycleOwner, Observer {
            runAdapter.submitList(it)
        })

        back_button.setOnClickListener {
            findNavController().navigate(R.id.action_historyFragment_to_runFragment)
        }
    }

    private fun setRecyclerView() = recycler_view.apply {
        runAdapter = Adapter()
        adapter = runAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

}