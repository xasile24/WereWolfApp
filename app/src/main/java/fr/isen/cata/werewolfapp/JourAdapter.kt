package fr.isen.cata.werewolfapp

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class JourAdapter(private val players: ArrayList<PlayerModel?>) : RecyclerView.Adapter<JourAdapter.ViewHolder>() {

    val mDatabase = FirebaseDatabase.getInstance().reference
    private lateinit var auth: FirebaseAuth
    private var currentVote = ""


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        auth = FirebaseAuth.getInstance()
        getPlayerAvatar(holder, players[position]!!)
        holder.pseudo.text = players[position]!!.pseudo
        val nbVotesString = players[position]!!.nbVotesJour.toString()
        val votesString = "$nbVotesString votes"
        holder.nbVotes.text = votesString

        holder.card.setOnClickListener {
            val id = players[position]!!.id
            changeVote(id)
        }

        setVoteListener(holder, position)

    }

    private fun setVoteListener(holder: ViewHolder, position: Int) {
        val mUserReference = mDatabase.child("Users")

        mUserReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (i in dataSnapshot.children) {
                        val tempPlayer = i.getValue(PlayerModel::class.java)
                        if (tempPlayer!!.id == players[position]!!.id) {
                            val nbVotesString = tempPlayer.nbVotesJour.toString()
                            val votesString = "$nbVotesString votes"
                            holder.nbVotes.text = votesString
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun changeVote(id: String) {

        val mUserReference = mDatabase.child("Users")


        mUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    for (i in dataSnapshot.children) {
                        val tempPlayer = i.getValue(PlayerModel::class.java)

                        if (tempPlayer!!.id == id) {
                            tempPlayer.nbVotesJour += 1
                            mUserReference.child(id).child("nbVotesJour").setValue(tempPlayer.nbVotesJour)
                        }

                        if (tempPlayer.id == currentVote) {
                            tempPlayer.nbVotesJour -= 1
                            mUserReference.child(currentVote).child("nbVotesJour").setValue(tempPlayer.nbVotesJour)
                        }


                    }
                    currentVote = id

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.loup_vote_view_row, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return players.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var avatar: ImageView = itemView.findViewById(R.id.avatarPlayerLoup)
        var pseudo: TextView = itemView.findViewById(R.id.pseudoPlayerLoup)
        var nbVotes: TextView = itemView.findViewById(R.id.nbVotesLoup)
        var card: CardView = itemView.findViewById(R.id.loupCardPlayer)
    }

    private fun getPlayerAvatar(holder: ViewHolder, player: PlayerModel) {
        val storageReference = FirebaseStorage.getInstance().reference.child(player.id + "/avatar")

        storageReference.downloadUrl.addOnSuccessListener {
            Picasso.get()
                .load(it)
                .into(holder.avatar)
        }.addOnFailureListener {
        }
    }

}



