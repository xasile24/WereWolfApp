package fr.isen.cata.werewolfapp

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_sorciere_vie.*


class SorciereVieFragment : Fragment() {

    private lateinit var mDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var currentPlayer: PlayerModel? = null
    var gameName: String = ""
    var game: PartyModel? = null
    var wolfKill = ""
    var deadPlayer: PlayerModel? = null
    var lifePotion = false
    private var compteur: CountDownTimer? = null
    val compteurMax: Long = 10


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        buttonEffect(ResurrectButton)
        buttonEffect(letHimDieButton)

        mDatabase = FirebaseDatabase.getInstance().reference

        getLifePotion()


    }

    private fun getLifePotion() {
        val mUserReference = FirebaseDatabase.getInstance().getReference("")
        auth = FirebaseAuth.getInstance()
        val id: String = auth.currentUser!!.uid

        mUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val users: MutableList<PlayerModel?> = arrayListOf()
                if (dataSnapshot.exists()) {
                    for (i in dataSnapshot.child("Users").children) {
                        users.add(i.getValue(PlayerModel::class.java))
                    }
                    for (i in users) {
                        if (i?.id == id) {
                            currentPlayer = i
                            gameName = currentPlayer!!.currentGame!!
                        }
                    }
                }
                if (dataSnapshot.exists()) {
                    game = dataSnapshot.child("Party").child(gameName).getValue(PartyModel::class.java)
                    if (game != null) {
                        lifePotion = game!!.lifePotion

                        if (currentPlayer!!.state && currentPlayer!!.role == "Sorciere") {

                            compteur = object : CountDownTimer(compteurMax * 1000, 1000) {

                                override fun onTick(millisUntilFinished: Long) {
                                    val timeLeft = "" + (millisUntilFinished / 1000 + 1)
                                    sorciereVieCompteur.text = timeLeft
                                }

                                override fun onFinish() {
                                    val goText = "0"
                                    sorciereVieCompteur.text = goText
                                    Handler().postDelayed({
                                        val manager = MyFragmentManager()
                                        manager.sorciereMortFragment(context!!)
                                    }, 1000)
                                }
                            }.start()

                            if (lifePotion) {
                                getWolfKill()
                            } else {
                                displayNoPotionLife()
                            }

                        } else {
                            whoIsDeadText.visibility = View.INVISIBLE
                            choixSorciereMort.visibility = View.INVISIBLE
                            ResurrectButton.visibility = View.INVISIBLE
                            letHimDieButton.visibility = View.INVISIBLE
                            deadFace.visibility = View.INVISIBLE
                            sorciereVieCompteur.visibility = View.INVISIBLE
                            val noSorciereText = "La sorcière est en train de jouer ..."
                            estMortText.text = noSorciereText


                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun displayNoPotionLife() {
        makeInvisible()

        val nobodyDiedText = "Vous n'avez plus de potion de vie, attendez"
        whoIsDeadText.text = nobodyDiedText
    }

    private fun makeInvisible() {
        ResurrectButton.visibility = View.INVISIBLE
        deadFace.visibility = View.INVISIBLE
        estMortText.visibility = View.INVISIBLE
        choixSorciereMort.visibility = View.INVISIBLE
        val textContinue = "Continuer"
        letHimDieButton.text = textContinue
        letHimDieButton.setOnClickListener {
            goToDeathPotion()
        }
    }

    private fun goToDeathPotion() {
        if (compteur != null) {
            compteur!!.cancel()

        }
        val manager = MyFragmentManager()
        manager.sorciereMortFragment(context!!)
    }

    private fun getWolfKill() {

        val mUserReference = FirebaseDatabase.getInstance().getReference("")
        auth = FirebaseAuth.getInstance()

        mUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    game = dataSnapshot.child("Party").child(gameName).getValue(PartyModel::class.java)
                    if (game != null) {
                        wolfKill = game!!.wolfKill
                        if (wolfKill != "") {
                            displayDeadPlayer(wolfKill)

                            ResurrectButton.setOnClickListener {
                                mDatabase.child("Users").child(wolfKill).child("state").setValue(true)
                                mDatabase.child("Party").child(gameName).child("lifePotion").setValue(false)

                                mDatabase.child("Party").child(gameName).child("wolfKill").setValue("")
                                goToDeathPotion()
                            }

                            letHimDieButton.setOnClickListener {
                                mDatabase.child("Party").child(gameName).child("wolfKill").setValue("")
                                goToDeathPotion()
                            }
                        } else {
                            displayNoDeath()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun displayNoDeath() {
        makeInvisible()

        val nobodyDiedText = "Personne n'est mort"
        whoIsDeadText.text = nobodyDiedText
    }

    private fun displayDeadPlayer(idPlayer: String) {

        val mUserReference = FirebaseDatabase.getInstance().getReference("Users")

        mUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user: MutableList<PlayerModel?> = arrayListOf()
                if (dataSnapshot.exists()) {
                    user.clear()
                    for (i in dataSnapshot.children) {
                        user.add(i.getValue(PlayerModel::class.java))
                    }
                    for (i in user) {
                        if (i?.id == idPlayer) {
                            deadPlayer = i
                            getPlayerAvatar(deadPlayer!!)
                            whoIsDeadText.text = deadPlayer!!.pseudo
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun getPlayerAvatar(player: PlayerModel) {
        val storageReference = FirebaseStorage.getInstance().reference.child(player.id + "/avatar")

        storageReference.downloadUrl.addOnSuccessListener {
            Picasso.get()
                .load(it)
                .into(deadFace)
        }.addOnFailureListener {
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sorciere_vie, container, false)
    }

    companion object {
        fun newInstance() = SorciereVieFragment()
    }
}


