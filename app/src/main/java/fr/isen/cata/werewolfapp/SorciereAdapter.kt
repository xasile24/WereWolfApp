package fr.isen.cata.werewolfapp

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class SorciereAdapter(private val players: ArrayList<PlayerModel?>, _sorciereMortFragment: SorciereMortFragment) :
    RecyclerView.Adapter<SorciereAdapter.ViewHolder>() {

    val mDatabase = FirebaseDatabase.getInstance().reference
    private lateinit var auth: FirebaseAuth
    private lateinit var gameName: String
    private var sorciereMortFragment = _sorciereMortFragment


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        auth = FirebaseAuth.getInstance()
        gameName = players[position]!!.currentGame!!
        getPlayerAvatar(holder, players[position]!!)
        holder.pseudo.text = players[position]!!.pseudo

        holder.card.setOnClickListener {
            val id = players[position]!!.id
            sorciereMortFragment.compteur!!.cancel()
            useDeathPotion(id)
        }

    }

    private fun useDeathPotion(id: String) {


        mDatabase.child("Users").child(id).child("state").setValue(false)
        mDatabase.child("Party").child(gameName).child("deathPotion").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("SorciereFlag").setValue(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.chasseur_vote_view_row, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return players.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var avatar: ImageView = itemView.findViewById(R.id.avatarPlayerChasseur)
        var pseudo: TextView = itemView.findViewById(R.id.pseudoPlayerChasseur)
        var card: CardView = itemView.findViewById(R.id.chasseurCard)
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



