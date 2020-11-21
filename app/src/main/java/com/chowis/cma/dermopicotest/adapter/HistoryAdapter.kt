package com.chowis.cma.dermopicotest.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chowis.cma.dermopicotest.R
import com.chowis.cma.dermopicotest.model.Calibrate
import com.chowis.cma.dermopicotest.util.ClickListener
import com.chowis.cma.dermopicotest.util.inflate
import kotlinx.android.synthetic.main.item_view_history.view.*
import timber.log.Timber

class HistoryAdapter(
    private val historyList: List<Calibrate>,
    private val checkBoxClick: ClickListener
) : RecyclerView.Adapter<HistoryAdapter.HistoryHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        val inflatedView = parent.inflate(R.layout.item_view_history, false)
        return HistoryHolder(inflatedView, checkBoxClick)
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        val itemPhoto = historyList[position]
        holder.bindHistory(itemPhoto)

    }

    override fun getItemCount() = historyList.size

    class HistoryHolder(v: View, private val checkBoxClick: ClickListener) :
        RecyclerView.ViewHolder(v), View.OnClickListener {
        private var view: View = v
        private var history: Calibrate? = null

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
        }

        fun bindHistory(history: Calibrate) {
            this.history = history
            view.tv_id_no.text = history.image_id + "" + history.id
            view.tv_date.text = history.date
            view.tv_time.text = history.time
            view.checkBox.setOnCheckedChangeListener { _, isChecked ->
                checkBoxClick.onClickListener(isChecked, history)
            }
        }
    }

}