package com.example.gymguardian

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.gymguardian.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    val db = Firebase.firestore
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_clinet_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.loginBtn.setOnClickListener {
            auth.signInWithEmailAndPassword(
                binding.email.text.toString().trim(),
                binding.password.text.toString().trim()
            )
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        checkUserProfile(auth.currentUser!!.uid)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        binding.moveToSignup.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        binding.googleSignInButton.setOnClickListener {
            googleSignIn()
        }
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        currentUser?.let {
            checkUserProfile(it.uid)
        }
    }

    private fun checkUserProfile(uid: String) {
        db.collection("UsersInfo").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getString("preferredName") != null) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // User profile is not complete, redirect to MainActivity and show ProfileFragment
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("showProfileFragment", true)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("showProfileFragment", true)
                startActivity(intent)
                finish()
            }
    }

    private fun googleSignIn() {
        val signInClient = googleSignInClient.signInIntent
        launcher.launch(signInClient)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            manageResults(task)
        }
    }

    private fun manageResults(task: Task<GoogleSignInAccount>) {
        val account: GoogleSignInAccount? = task.result
        account?.let {
            val credentials = GoogleAuthProvider.getCredential(it.idToken, null)
            auth.signInWithCredential(credentials).addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    checkUserProfile(auth.currentUser!!.uid)
                } else {
                    Toast.makeText(this, signInTask.exception.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
