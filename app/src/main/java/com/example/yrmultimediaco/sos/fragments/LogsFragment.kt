package com.example.yrmultimediaco.sos.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yrmultimediaco.sos.R
import com.example.yrmultimediaco.sos.util.LogsBus
import com.example.yrmultimediaco.sos.viewModels.LogsAdapter
import com.example.yrmultimediaco.sos.viewModels.LogsViewModel

class LogsFragment : Fragment(R.layout.fragment_logs) {

    private val vm: LogsViewModel by activityViewModels()

    private lateinit var adapter: LogsAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.logsRecycler)
        emptyText = view.findViewById(R.id.txtEmpty)

        adapter = LogsAdapter()

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        LogsBus.attach(vm)

        view.findViewById<Button>(R.id.btnClearLogs)
            .setOnClickListener {
                vm.clear()
            }

        vm.logs.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            emptyText.visibility =
                if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
