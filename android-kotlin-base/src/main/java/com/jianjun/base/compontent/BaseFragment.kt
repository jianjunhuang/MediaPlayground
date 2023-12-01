package com.jianjun.base.compontent

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

abstract class BaseFragment : Fragment() {

    private inline fun <reified T : ViewModel> Fragment.viewModels() = lazy {
        ViewModelProvider(requireActivity()).get(T::class.java)
    }

    fun onBackPressed(): Boolean {
        return false
    }
}