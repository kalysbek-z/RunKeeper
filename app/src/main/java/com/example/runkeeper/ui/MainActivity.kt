package com.example.runkeeper.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.runkeeper.ACTION_SHOW_RUN_FRAGMENT
import com.example.runkeeper.R
import com.example.runkeeper.database.RunDao
import com.example.runkeeper.ui.fragments.HistoryFragment
import com.example.runkeeper.ui.fragments.RunFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigateToRunFragment(intent)

        bottom_nav.setupWithNavController(nav_host_fragment.findNavController())

//        val runFragment = RunFragment()
//        val historyFragment = HistoryFragment()
//        currentFragment(runFragment)

        nav_host_fragment.findNavController()
            .addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.runFragment, R.id.historyFragment -> {
                        bottom_nav.visibility = View.VISIBLE
                    }
                }
            }

//        bottom_nav.setOnNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.run -> currentFragment(runFragment)
//                R.id.history -> currentFragment(historyFragment)
//            }
//            true
//        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToRunFragment(intent)
    }

    private fun currentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            commit()
        }

    private fun navigateToRunFragment(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_RUN_FRAGMENT) {
            nav_host_fragment.findNavController().navigate(R.id.action_global_runFragment)
        }
    }
}