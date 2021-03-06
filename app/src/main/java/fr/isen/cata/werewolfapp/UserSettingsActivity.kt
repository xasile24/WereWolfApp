package fr.isen.cata.werewolfapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_user_settings.*
import java.io.ByteArrayOutputStream


class UserSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var currentPlayer: PlayerModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_settings)
        buttonEffect(saveButton)
        auth = FirebaseAuth.getInstance()

        settingsView.setOnClickListener {
            chooseGalleryOrCamera()
        }

        getCurrentPlayer()

        getPlayerAvatar()

        editPseudoButton.setOnClickListener {
            editPseudo()
        }

        saveButton.setOnClickListener {

            saveAvatar()

            saveUserPseudo()
        }

    }

    private fun getPlayerAvatar() {
        val storageReference = FirebaseStorage.getInstance().reference.child(auth.currentUser!!.uid + "/avatar")

        storageReference.downloadUrl.addOnSuccessListener {
            Picasso.get()
                .load(it)
                .into(avatarView)
        }.addOnFailureListener {
        }
    }

    private fun saveUserPseudo() {
        val mDatabase = FirebaseDatabase.getInstance().reference

        mDatabase.child("Users").child(auth.currentUser!!.uid).child("pseudo").setValue(pseudoText.text)
    }

    private fun saveAvatar() {
        val storage = FirebaseStorage.getInstance()

        val storageRef = storage.reference

        val mountainsRef = storageRef.child(auth.currentUser!!.uid + "/avatar")
        val bitmap = (avatarView.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = mountainsRef.putBytes(data)
        uploadTask.addOnFailureListener {
        }.addOnSuccessListener {

        }
    }

    private fun editPseudo() {
        val context = this
        val builder = AlertDialog.Builder(context, R.style.AlertDialogTheme)
        builder.setTitle("Modifier le pseudo")

        val view = layoutInflater.inflate(R.layout.pseudo_edit_layout, null)

        val editPseudo = view.findViewById(R.id.editPseudoContainer) as EditText
        editPseudo.setText(pseudoText.text.toString())


        builder.setView(view)

        // set up the ok button
        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            val newPseudo = editPseudo.text.toString()
            var isValid = true
            if (newPseudo.isBlank()) {
                editPseudo.error = "ERREUR"
                isValid = false
            }

            if (isValid) {
                pseudoText.text = newPseudo
            }

            if (isValid) {
                dialog.dismiss()
            }
        }

        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun getCurrentPlayer() {

        val id: String = auth.currentUser!!.uid

        val mUserReference = FirebaseDatabase.getInstance().getReference("Users")

        mUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user: MutableList<PlayerModel?> = arrayListOf()
                if (dataSnapshot.exists()) {
                    for (i in dataSnapshot.children) {
                        user.add(i.getValue(PlayerModel::class.java))
                    }
                    for (i in user) {
                        if (i?.id == id) {
                            currentPlayer = i

                            pseudoText.text = currentPlayer!!.pseudo

                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun chooseGalleryOrCamera() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Quelle action souhaitez vous faire ?")
        val pictureDialogItems = arrayOf("Choisir une de vos photos", "Utiliser la caméra")
        pictureDialog.setItems(
            pictureDialogItems
        ) { _, which ->
            when (which) {
                0 -> choiceIsGallery()
                1 -> choiceIsCamera()
            }
        }
        pictureDialog.show()
    }

    private fun choiceIsGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        startActivityForResult(galleryIntent, 1)
    }

    private fun choiceIsCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, 2)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (data != null) {
                val contentURI = data.data
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                avatarView.setImageBitmap(bitmap)
            }

        } else if (requestCode == 2) {
            val thumbnail = data!!.extras!!.get("data") as Bitmap
            avatarView!!.setImageBitmap(thumbnail)
        }

    }

    fun buttonEffect(button: View) {
        val color = Color.parseColor("#514e4e")
        button.setOnTouchListener { v, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    v.background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                    v.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    v.background.clearColorFilter()
                    v.invalidate()
                }
            }
            false
        }
    }
}
