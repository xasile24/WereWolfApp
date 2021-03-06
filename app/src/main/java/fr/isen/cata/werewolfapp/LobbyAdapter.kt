package fr.isen.cata.werewolfapp

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class LobbyAdapter(private val lobbies: ArrayList<LobbyModel?>) : RecyclerView.Adapter<LobbyAdapter.ViewHolder>() {

    private lateinit var auth: FirebaseAuth
    private var currentPlayer: PlayerModel? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        auth = FirebaseAuth.getInstance()
        getCurrentPlayer()

        holder.name.text = lobbies[position]!!.name
        val nbPlayerMessage =
            lobbies[position]!!.listPlayer?.size.toString() + "/" + lobbies[position]!!.nbPlayer.toString()
        holder.nbPlayer.text = nbPlayerMessage

        holder.joinButton.setOnClickListener {
            if ((lobbies[position]!!.nbPlayer != lobbies[position]!!.listPlayer?.size)) {
                buttonEffect(holder.joinButton)
                val mDatabase = FirebaseDatabase.getInstance().reference

                mDatabase.child("Users").child(auth.currentUser!!.uid).child("currentGame")
                    .setValue(lobbies[position]!!.name)

                val playerList = lobbies[position]!!.listPlayer as MutableList<String>
                playerList.add(auth.currentUser!!.uid)

                mDatabase.child("Lobby").child(lobbies[position]!!.name).child("listPlayer").setValue(playerList)

                mDatabase.child("Users").child(currentPlayer!!.id).child("inLobby").setValue(true)


                val intent = Intent(holder.joinButton.context, LobbyActivity::class.java)
                holder.joinButton.context.startActivity(intent)
            } else {
                buttonEffectWrong(holder.joinButton)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.fragment_lobby_view_row, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return lobbies.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.pseudoText)
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
        val color = Color.parseColor("#228B22")
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

    private fun buttonEffectWrong(button: View) {
        val color = Color.parseColor("#DB1702")
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
