package com.digiapt.digiaptvideoapp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.digiapt.digiaptvideoapp.R
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setSupportActionBar(toolbar)
        toolbar.title = "Movies"

        //toolbar.subtitle = "English"
        //toolbar.setNavigationIcon(resources.getDrawable(R.drawable.logo))
        toolbar.setSubtitleTextColor(getResources().getColor(R.color.grey_100))
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorWhite))

        val navController = Navigation.findNavController(this, R.id.fragment)
        NavigationUI.setupWithNavController(nav_view, navController)
        NavigationUI.setupActionBarWithNavController(this,navController, drawer_layout)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
            Navigation.findNavController(this, R.id.fragment),
            drawer_layout
        )
    }
}