package fr.isen.cata.werewolfapp

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_love.*

class LoveFragment : Fragment() {

    private lateinit var mDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var currentPlayer: PlayerModel? = null
    var gameName: String = ""
    var game: PartyModel? = null
    var listId: MutableList<String>? = arrayListOf()
    var lovePlayer: PlayerModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        mDatabase = FirebaseDatabase.getInstance().reference

        getLover()
    }

    fun beginCompteur(compteurMax: Long) {
        object : CountDownTimer(compteurMax * 1000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = "" + (millisUntilFinished / 1000 + 1)
                loveTimer.text = timeLeft
            }

            override fun onFinish() {
                loveTimer.text = "0"
                Handler().postDelayed({
                    if (game!!.masterId == currentPlayer!!.id) {
                        mDatabase.child("Party").child(gameName).child("FinishFlags").child("LoverFlag").setValue(true)
                    }
                }, 1500)
            }
        }.start()
    }

    private fun displayLover() {
        if (lovePlayer == null) {
            val pasCoupFoudre = "Le coup de foudre n'est pas pour aujourd'hui"
            messageLove.text = pasCoupFoudre
            pseudoLove.setBackgroundResource(R.color.transparentBackground)
        } else {
            val coupFoudre = "Votre coeur appartient maintenant à"
            messageLove.text = coupFoudre
            pseudoLove.text = lovePlayer!!.pseudo
            pseudoLove.setBackgroundResource(R.drawable.pseudoshape)
            getPlayerAvatar(lovePlayer!!)
        }
    }

    private fun getLover() {

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
                                if (currentPlayer!!.inLove && user.inLove && user.id != currentPlayer!!.id) {
                                    lovePlayer = user
                                }
                            }
                        }
                    }
                }
                displayLover()
                beginCompteur(5)
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
                .into(avatarPlayerLove)
        }.addOnFailureListener {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_love, container, false)
    }


    companion object {
        fun newInstance() = LoveFragment()
    }
}