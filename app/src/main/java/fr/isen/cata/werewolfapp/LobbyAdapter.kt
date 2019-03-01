package fr.isen.cata.werewolfapp

import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_user_settings.*


class LobbyAdapter(private val lobbies: ArrayList<LobbyModel?>): RecyclerView.Adapter<LobbyAdapter.ViewHolder>() {

    private lateinit var auth: FirebaseAuth
    private var currentPlayer: PlayerModel? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        auth = FirebaseAuth.getInstance()
        getCurrentPlayer()
        holder.name.text = lobbies[position]!!.name
        val nbPlayerMessage = lobbies[position]!!.listPlayer?.size.toString() + "/" + lobbies[position]!!.nbPlayer.toString()
        holder.nbPlayer.text = nbPlayerMessage
        holder.joinButton.setOnClickListener {
            val mDatabase = FirebaseDatabase.getInstance().reference

            mDatabase.child("Users").child(auth.currentUser!!.uid).child("currentGame").setValue(lobbies[position]!!.name)
            mDatabase.child("Lobby").child(lobbies[position]!!.name).child("currentGame").setValue(auth.currentUser!!.uid)

            val playerList = lobbies[position]!!.listPlayer as MutableList<String>
            playerList.add(auth.currentUser!!.uid)

            mDatabase.child("Lobby").child(lobbies[position]!!.name).child("listPlayer").setValue(playerList)



            val intent = Intent(holder.joinButton.context, LobbyActivity::class.java)
            holder.joinButton.context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.fragment_lobby_view_row, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return lobbies.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.nameText)
        var nbPlayer: TextView = itemView.findViewById(R.id.nbPlayerText)
        var joinButton: Button = itemView.findViewById((R.id.joinButton))
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

                            Log.d("USERID------", currentPlayer!!.id)

                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

}
