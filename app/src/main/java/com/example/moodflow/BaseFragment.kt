package com.example.moodflow

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.moodflow.data.PreferencesHelper

abstract class BaseFragment : Fragment() {
    protected lateinit var preferencesHelper: PreferencesHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)
        preferencesHelper = PreferencesHelper(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
    }

    protected abstract fun getLayoutId(): Int
    protected abstract fun setupViews(view: View)
}