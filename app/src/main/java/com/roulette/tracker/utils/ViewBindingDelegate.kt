package com.roulette.tracker.utils

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import timber.log.Timber

class FragmentViewBindingDelegate<T : ViewBinding>(
    private val fragment: Fragment,
    private val bindingFactory: (View) -> T
) : ReadOnlyProperty<Fragment, T> {
    
    private var binding: T? = null
    
    init {
        setupLifecycleObserver()
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        binding?.let { return it }

        check(fragment.viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            "Should not attempt to get bindings when Fragment views are destroyed."
        }

        return bindingFactory(thisRef.requireView()).also { 
            binding = it
            Timber.d("ViewBinding created for ${thisRef.javaClass.simpleName}")
        }
    }

    private fun setupLifecycleObserver() {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            binding = null
                            Timber.d("ViewBinding cleared for ${fragment.javaClass.simpleName}")
                        }
                    })
                }
            }
        })
    }
}

fun <T : ViewBinding> Fragment.viewBinding(
    bindingFactory: (View) -> T
): FragmentViewBindingDelegate<T> = FragmentViewBindingDelegate(this, bindingFactory) 