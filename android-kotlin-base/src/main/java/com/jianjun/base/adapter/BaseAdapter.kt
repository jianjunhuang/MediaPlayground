package com.jianjun.base.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<D : Any, V : BaseViewHolder<D>> : RecyclerView.Adapter<V>() {

    protected abstract fun onDiffCallback(): DiffUtil.ItemCallback<D>

    protected open val diffUtil = AsyncListDiffer<D>(this, onDiffCallback())

    protected var clickListener: OnItemClickListener<D>? = null
    protected var longClickListener: OnItemLongClickListener<D>? = null

    override fun onBindViewHolder(holder: V, position: Int) {
        onBindViewHolder(holder, position, getData(position))
    }

    override fun onBindViewHolder(holder: V, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            onBindViewHolder(holder, position, getData(position), payloads)
        }
    }

    protected abstract fun onBindViewHolder(holder: V, position: Int, data: D)
    open fun onBindViewHolder(holder: V, position: Int, data: D, payloads: MutableList<Any>) {

    }

    fun getData(position: Int): D {
        return diffUtil.currentList[position]
    }

    fun getDataList(): List<D> = diffUtil.currentList
    protected fun getMutableDataList(): MutableList<D> = diffUtil.currentList

    override fun getItemCount(): Int {
        return diffUtil.currentList.size
    }

    fun update(dataList: List<D>) {
        diffUtil.submitList(dataList)
    }

    fun update(dataList: List<D>, callback: Runnable) {
        diffUtil.submitList(dataList, callback)
    }

    fun setOnItemClickListener(clickListener: OnItemClickListener<D>) {
        this.clickListener = clickListener
    }

    fun setOnItemLongClickListener(clickListener: OnItemLongClickListener<D>) {
        this.longClickListener = clickListener
    }

    interface OnItemClickListener<D : Any> {
        fun onItemClicked(position: Int, data: D?, adapter: BaseAdapter<D, *>)
    }

    interface OnItemLongClickListener<D : Any> {
        fun onItemLongClicked(position: Int, data: D?, adapter: BaseAdapter<D, *>)
    }

    fun BaseViewHolder<D>.bindClick() {
        this.itemView.setOnClickListener {
            clickListener?.onItemClicked(
                this.adapterPosition,
                diffUtil.currentList.getOrNull(adapterPosition),
                this@BaseAdapter
            )
        }
    }

    fun BaseViewHolder<D>.bindLongClick() {
        this.itemView.setOnLongClickListener {
            longClickListener?.onItemLongClicked(
                this.adapterPosition,
                diffUtil.currentList.getOrNull(adapterPosition),
                this@BaseAdapter
            )
            return@setOnLongClickListener true
        }
    }
}