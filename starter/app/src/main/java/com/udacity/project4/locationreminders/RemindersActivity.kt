package com.udacity.project4.locationreminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.work.WorkRequest
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.authentication.LoginViewModel
import com.udacity.project4.databinding.ActivityRemindersBinding

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity(), MenuProvider {

    private lateinit var binding: ActivityRemindersBinding
    private lateinit var workRequest : WorkRequest
    private val loginViewModel by viewModels<LoginViewModel>()
    private var isLoggedIn = false

    //temporary user code
    var user = FirebaseAuth.getInstance().currentUser

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        onSignInResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addMenuProvider(this, this)

        loginViewModel.authenticationState.observe(this, Observer{ authenticationState ->
            when(authenticationState){
                LoginViewModel.AuthenticationState.AUTHENTICATED ->{
                    //add logout button
                    isLoggedIn = true
                }else ->{
                    //remove logout button
                    isLoggedIn = false
                launchSignInFlow()
                }
            }
        })

        if(!isPermissionGranted()){
            Log.d("RemindersActivity", "Requesting permissions")
            requestPermissions()
        }


        Log.d("Reminders Activity", "Launched")
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                binding.navHostFragment.findNavController().popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.logout -> {
                AuthUI.getInstance().signOut(this)

            }
        }
        return true
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if(isLoggedIn)
            menuInflater.inflate(R.menu.main_menu, menu)
    }


    override fun onDestroy() {
        super.onDestroy()
        removeMenuProvider(this)
    }
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            user = FirebaseAuth.getInstance().currentUser
            Log.d("SignIn", user?.displayName.toString())
            // ...
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }
    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun isPermissionGranted() : Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(){
        val requestPermissionLauncher = this.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val fineLocationGranted = results.getValue("android.permission.ACCESS_FINE_LOCATION") ?: false
            if (fineLocationGranted) {
                // Permission granted, proceed with map functionality
            } else {
                // Permission denied, handle the scenario
            }
        }

        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)
        requestPermissionLauncher.launch(permissions)
    }
}
