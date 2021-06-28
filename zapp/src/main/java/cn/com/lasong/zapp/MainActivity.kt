package cn.com.lasong.zapp

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cn.com.lasong.utils.ILog
import cn.com.lasong.zapp.base.CoreActivity
import cn.com.lasong.zapp.databinding.ActivityMainBinding

class MainActivity : CoreActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_video), binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

//        val pickLauncher = registerForActivityResult(
//            ActivityResultContracts.GetContent(),
//            object : ActivityResultCallback<Uri?> {
//                override fun onActivityResult(uri: Uri?) {
//                    // Handle the returned Uri
//                    ILog.d("uri :" + uri)
//                }
//            })
//
//        pickLauncher.launch(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}