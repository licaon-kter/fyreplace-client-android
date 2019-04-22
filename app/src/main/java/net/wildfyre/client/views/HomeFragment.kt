package net.wildfyre.client.views

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.wildfyre.client.R
import net.wildfyre.client.databinding.HomeActionsAreaReputationBinding
import net.wildfyre.client.databinding.HomeActionsAreaSpreadBinding
import net.wildfyre.client.viewmodels.HomeFragmentViewModel

class HomeFragment : FailureHandlingFragment(R.layout.fragment_home) {
    override lateinit var viewModel: HomeFragmentViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProviders.of(this).get(HomeFragmentViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.title = ""
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_home_actions, menu)

        HomeActionsAreaSpreadBinding.bind(menu.findItem(R.id.action_area_spread).actionView).apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
        }

        HomeActionsAreaReputationBinding.bind(menu.findItem(R.id.action_area_reputation).actionView).apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
        }

        val areaSelectorMenuItem = menu.findItem(R.id.action_area_selector)
        val areaSpinner = areaSelectorMenuItem?.actionView as Spinner

        areaSelectorMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            private val areaStuff = listOf(R.id.action_area_spread, R.id.action_area_reputation)
            private val nonAreaStuff = listOf(R.id.action_comments, R.id.action_subscribe)

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                expandAndHide(areaStuff, nonAreaStuff)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                expandAndHide(nonAreaStuff, areaStuff)
                return true
            }

            private fun expandAndHide(expand: List<Int>, hide: List<Int>) {
                expand.forEach {
                    menu.findItem(it).run {
                        if (actionView != null) {
                            expandActionView()
                        }

                        isVisible = true
                    }
                }

                hide.forEach {
                    menu.findItem(it).run {
                        if (actionView != null) {
                            collapseActionView()
                        }

                        isVisible = false
                    }
                }
            }
        })

        viewModel.preferredArea.observe(viewLifecycleOwner, Observer { area ->
            if (area == null) {
                return@Observer
            }

            areaSelectorMenuItem.title = getString(R.string.main_actions_area_selector, area.displayname)
            viewModel.areas.value
                ?.indexOfFirst { it.name == area.name }
                ?.run { areaSpinner.setSelection(this) }
        })

        val adapter = ArrayAdapter<String>(context!!, R.layout.home_actions_area_item)
        areaSpinner.adapter = adapter
        areaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val title = areaSpinner.selectedItem.toString()
                areaSelectorMenuItem.title = getString(R.string.main_actions_area_selector, title)
                viewModel.setPreferredAreaDisplayName(title)
                viewModel.updatePreferredArea()
            }
        }

        viewModel.areas.observe(viewLifecycleOwner, Observer { areas ->
            adapter.run { clear(); addAll(areas.map { it.displayname }) }

            if (viewModel.preferredAreaName.value == null) {
                viewModel.setPreferredAreaName(areas.first().name!!)
            }

            viewModel.updatePreferredArea()
        })

        viewModel.updateAreas()
    }
}