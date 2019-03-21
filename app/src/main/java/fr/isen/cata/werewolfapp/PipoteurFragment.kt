package fr.isen.cata.werewolfapp


import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_pipoteur.*

class PipoteurFragment : Fragment() {

    private lateinit var mDatabase: DatabaseReference
    private lateinit var adapter: PipoteurAdapter
    private lateinit var auth: FirebaseAuth

    private var currentPlayer: PlayerModel? = null
    var gameName: String = ""
    var game: PartyModel? = null
    var listId: MutableList<String>? = arrayListOf()
    var isAlivePlayer: Boolean = false
    var isPipoteurPlayer: Boolean = false
    val players: ArrayList<PlayerModel?> = ArrayList()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        mDatabase = FirebaseDatabase.getInstance().reference

        getVillagers(players)
    }

    private fun getVillagers(players: ArrayList<PlayerModel?>) {

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
                            if (currentPlayer!!.role == "Pipoteur") {
                                isPipoteurPlayer = true
                            }
                            if (currentPlayer!!.state) {
                                isAlivePlayer = true
                            }
                        }
                    }
                }
                if (isPipoteurPlayer && isAlivePlayer) {
                    if (dataSnapshot.exists()) {
                        game = dataSnapshot.child("Party").child(gameName).getValue(PartyModel::class.java)
                        if (game != null) {
                            if (game!!.listPlayer != null) {
                                listId = game!!.listPlayer
                            }
                        }
                    }
                    if (listId != null) {
                        players.clear()
                        for (i in listId!!) {
                            for (u in dataSnapshot.child("Users").children) {
                                val user = u.getValue(PlayerModel::class.java)
                                if (i == user!!.id) {
                                    if (user.state && user.id != currentPlayer!!.id) {
                                        players.add(user)
                                        pipoteurRecyclerView.layoutManager = GridLayoutManager(context!!, 2)
                                        adapter = PipoteurAdapter(players)
                                        pipoteurRecyclerView.adapter = adapter
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    pipoteurTextView.text = ""
                    val isPipotingText = "Le Pipoteur est en train de pipoter..."
                    noPipoteurMessage.text = isPipotingText
                }
                beginCompteur(10)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    fun beginCompteur(compteurMax: Long) {
        object : CountDownTimer(compteurMax * 1000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = "" + (millisUntilFinished / 1000 + 1)
                pipoteurTimer.text = timeLeft
            }

            override fun onFinish() {
                pipoteurTimer.text = "0"
                Handler().postDelayed({
                    if (isAlivePlayer && isPipoteurPlayer) {
                        pipotedPlayers()
                    }
                }, 1500)
            }
        }.start()
    }

    private fun pipotedPlayers() {
        if (adapter.victimPlayer != null && adapter.victimPlayer2 != null) {
            val mDatabase = FirebaseDatabase.getInstance().reference
            mDatabase.child("Users").child(adapter.victimPlayer!!.id).child("selected").setValue(false)
            mDatabase.child("Users").child(adapter.victimPlayer2!!.id).child("selected").setValue(false)
            mDatabase.child("Users").child(adapter.victimPlayer!!.id).child("charmed").setValue(true)
            mDatabase.child("Users").child(adapter.victimPlayer2!!.id).child("charmed").setValue(true)

        } else if (adapter.victimPlayer != null) {
            mDatabase.child("Users").child(adapter.victimPlayer!!.id).child("charmed").setValue(true)
            mDatabase.child("Users").child(adapter.victimPlayer!!.id).child("selected").setValue(false)
        } else if (adapter.victimPlayer2 != null) {
            mDatabase.child("Users").child(adapter.victimPlayer2!!.id).child("charmed").setValue(true)
            mDatabase.child("Users").child(adapter.victimPlayer2!!.id).child("selected").setValue(false)
        } else {
        }

        mDatabase.child("Party").child(gameName).child("FinishFlags").child("PipoteurFlag").setValue(true)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pipoteur, container, false)
    }


    companion object {
        fun newInstance() = PipoteurFragment()
    }
}


