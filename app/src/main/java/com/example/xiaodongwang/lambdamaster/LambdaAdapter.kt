package com.example.xiaodongwang.lambdamaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView

class LambdaAdapter(val context: MainActivity, val lambdas: ArrayList<Lambda>) : BaseAdapter() {

    inner class EditLambda(val lambdaHook: Lambda) : View.OnLongClickListener {
        override fun onLongClick(v: View?): Boolean {
            context.onButtonClickEditLambda(lambdaHook)
            return true
        }
    }

    inner class DeleteLambda(val lambdaHook: Lambda) : View.OnClickListener {
        override fun onClick(v: View?) {
            context.onButtonClickDeleteLambda(lambdaHook)

        }
    }

    override fun getCount() : Int {
        return lambdas.size
    }

    override fun getItem(position: Int) : Any {
        return lambdas.get(position)
    }

    override fun getItemId(position: Int) : Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup) : View {
        val view: View = convertView ?: LayoutInflater.from(context).
                inflate(R.layout.lambdas_layout, parent, false)
        val currentItem: Lambda = getItem(position) as Lambda
        val itemName: TextView = view.findViewById(R.id.lambda)!!
        itemName.setText(currentItem.name)
        val deleteItem: Button = view.findViewById(R.id.delete_lambda)!!
        deleteItem.setOnClickListener(DeleteLambda(currentItem))
        return view
    }
}