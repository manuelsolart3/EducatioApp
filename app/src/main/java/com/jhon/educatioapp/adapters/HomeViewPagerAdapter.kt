package com.jhon.educatioapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.jhon.educatioapp.databinding.ItemHomeBinding
import com.jhon.educatioapp.models.ArticuloHome

class HomeViewPagerAdapter(private val context: Context, private val model: ArrayList<ArticuloHome>) : PagerAdapter() {

    override fun getCount(): Int {
        return model.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(context)
        val binding = ItemHomeBinding.inflate(inflater, container, false)
        val currentItem = model[position]

        // Vincular datos al diseño utilizando View Binding
        binding.nombres.text = currentItem.nombres
        binding.descripcion.text = currentItem.descripcion
        binding.imagen.setImageResource(currentItem.imagenid)

        container.addView(binding.root)
        return binding.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
