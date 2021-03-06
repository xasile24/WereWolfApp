package fr.isen.cata.werewolfapp

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_sorciere_mort.*


class SorciereMortFragment : Fragment() {

    private lateinit var mDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var sorciereAdapter: SorciereAdapter

    private var currentPlayer: PlayerModel? = null
    var gameName: String = ""
    var game: PartyModel? = null
    var deathPotion = true
    var listId: MutableList<String>? = arrayListOf()
    var compteur: CountDownTimer? = null
    val compteurMax: Long = 10


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        mDatabase = FirebaseDatabase.getInstance().reference


        getDeathPotion()

        compteur = object : CountDownTimer(compteurMax * 1000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = "" + (millisUntilFinished / 1000 + 1)
                sorciereMortCompteur.text = timeLeft
            }

            override fun onFinish() {
                val goText = "0"
                sorciereMortCompteur.text = goText
                Handler().postDelayed({
                    mDatabase.child("Party").child(gameName).child("FinishFlags").child("SorciereFlag").setValue(true)

                }, 1000)
            }
        }.start()


    }

    private fun getDeathPotion() {
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
                        deathPotion = game!!.deathPotion

                        if (deathPotion) {
                            val players: ArrayList<PlayerModel?> = ArrayList()

                            sorciereRecyclerView.layoutManager = GridLayoutManager(context!!, 2)

                            sorciereAdapter = SorciereAdapter(players, this@SorciereMortFragment)

                            sorciereRecyclerView.adapter = sorciereAdapter

                            getPeople(players)

                            nobodyText.setOnClickListener {
                                if (compteur != null) {
                                    compteur!!.cancel()

                                }

                                mDatabase.child("Party").child(gameName).child("FinishFlags").child("SorciereFlag")
                                    .setValue(true)


                            }

                        } else {
                            displayNoPotionDeath()
                        }

                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun displayNoPotionDeath() {
        sorciereRecyclerView.visibility = View.INVISIBLE
        deathPotionChoiceText.visibility = View.INVISIBLE

        nobodyText.visibility = View.INVISIBLE

        val textNoPotionLeft = "Vous n'avez plus de potion de mort"
        invisibleText.text = textNoPotionLeft
        invisibleText.visibility = View.VISIBLE
        val textContinue = "Continuer"
        nobodyText.text = textContinue
        nobodyText.setOnClickListener {
            if (compteur != null) {
                compteur!!.cancel()

            }

            mDatabase.child("Party").child(gameName).child("FinishFlags").child("SorciereFlag").setValue(true)

        }
    }


    private fun getPeople(players: ArrayList<PlayerModel?>) {

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
                        if (game!!.listPlayer != null) {
                            listId = game!!.listPlayer
                        }
                    }
                }
                if (listId != null) {
                    for (i in listId!!) {
                        for (u in dataSnapshot.child("Users").children) {
                            val user = u.getValue(PlayerModel::class.java)
                            if (i == user!!.id) {
                                if (user.id != currentPlayer!!.id && user.state) {
                                    players.add(user)
                                    sorciereAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
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
        return inflater.inflate(R.layout.fragment_sorciere_mort, container, false)
    }


    companion object {
        fun newInstance() = SorciereMortFragment()
    }
}


