package fr.isen.cata.werewolfapp

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_lobby.*

class LobbyActivity : AppCompatActivity() {

    private lateinit var mDatabase: DatabaseReference
    private lateinit var mLobbyReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var currentPlayer: PlayerModel? = null
    private var context = this
    var lobby: LobbyModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)
        buttonEffect(startGame)
        startGame.setOnClickListener {
            startGame()
        }

        auth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference
        mLobbyReference = FirebaseDatabase.getInstance().reference.child("Lobby")
        setGameLauncher()
        setKickAndDestroyListener()

        playerView.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)

        val players: ArrayList<String?> = ArrayList()

        val adapter = PlayerAdapter(players)
        playerView.adapter = adapter

        setPlayerList(players)


    }

    override fun onBackPressed() {
        super.onBackPressed()

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
                            val gameName = currentPlayer!!.currentGame

                            mLobbyReference.child(gameName!!)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                                        if (dataSnapshot.exists()) {

                                            val list =
                                                dataSnapshot.child("listPlayer")
                                                    .getValue(object : GenericTypeIndicator<ArrayList<String>>() {})

                                            removeIdFromLobby(currentPlayer!!, list!!, gameName)
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
                                    }
                                })


                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })


    }

    private fun setKickAndDestroyListener() {
        val id: String = auth.currentUser!!.uid

        val mUserReference = FirebaseDatabase.getInstance().getReference("Users")

        mUserReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user: MutableList<PlayerModel?> = arrayListOf()
                if (dataSnapshot.exists()) {
                    for (i in dataSnapshot.children) {
                        user.add(i.getValue(PlayerModel::class.java))
                    }
                    for (i in user) {
                        if (i?.id == id) {
                            currentPlayer = i

                            if (!(currentPlayer!!.inLobby)) {
                                finish()
                                Toast.makeText(context, "Vous n'etes plus dans le lobby", Toast.LENGTH_LONG).show()
                            }

                            checkMasterPresenceAndEmptiness(currentPlayer!!)


                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun checkMasterPresenceAndEmptiness(currentPlayer: PlayerModel) {
        val gameName = currentPlayer.currentGame
        mLobbyReference.child(gameName!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.exists()) {
                    val list =
                        dataSnapshot.child("listPlayer").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})

                    if (list != null) {
                        val masterId = dataSnapshot.child("masterId").value as String
                        if (!list.contains(masterId)) {
                            removeIdFromLobby(currentPlayer, list, gameName)
                        }
                    } else {
                        mDatabase.child("Lobby").child(gameName).removeValue()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun removeIdFromLobby(
        currentPlayer: PlayerModel,
        list: ArrayList<String>,
        gameName: String
    ) {
        val idToRemove = currentPlayer.id

        list.remove(idToRemove)


        mDatabase.child("Lobby").child(gameName).child("listPlayer").setValue(list)
        mDatabase.child("Users").child(idToRemove).child("inLobby").setValue(false)
    }

    private fun setPlayerList(players: ArrayList<String?>) {

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

                            getLobbyPlayerList(players)


                        }
                    }
                }
                partyNameLobby.text = currentPlayer!!.currentGame
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun getLobbyPlayerList(players: ArrayList<String?>) {

        val gameName: String? = currentPlayer!!.currentGame
        val playersRef = mDatabase.child("Lobby").child(gameName!!).child("listPlayer")
        playersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                players.clear()
                if (dataSnapshot.exists()) {

                    for (i in dataSnapshot.children) {
                        players.add(i.value as String)

                        (playerView.adapter as PlayerAdapter).notifyDataSetChanged()

                    }
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun setGameLauncher() {

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

                            setLauncherListener()


                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun setLauncherListener() {
        val lobbyRef = mDatabase.child("Lobby")
        lobbyRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.exists()) {
                    launchGame(dataSnapshot)

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        })
    }

    private fun launchGame(dataSnapshot: DataSnapshot) {

        val startGameVal = dataSnapshot.child(currentPlayer!!.currentGame!!).child("startGame").value
        if (startGameVal == true) {

            val lobbyRef: String? = currentPlayer!!.currentGame
            lobby = dataSnapshot.child(lobbyRef!!).getValue(LobbyModel::class.java)
            if (lobby!!.masterId == currentPlayer!!.id) {
                createParty()
            }

            val intent = Intent(context, GameActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createParty() {
        val gameName: String? = currentPlayer!!.currentGame
        mDatabase.child("Lobby").child(gameName!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.exists()) {
                    mDatabase.child("Party").child(gameName).setValue(dataSnapshot.value)
                    setDefaultPartyValue(gameName)
                    mDatabase.child("Lobby").child(gameName).removeValue()

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        })
    }

    private fun setDefaultPartyValue(gameName: String) {
        mDatabase.child("Party").child(gameName).child("Flags").child("LoupFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("VoyanteFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("CupidonFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("SorciereFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("PipoteurFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("VoteFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("DeadNightFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("DeadVoteFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("DeadChasseurFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("TourFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("ChasseurFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("LoverFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("LowerFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("PipotedFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("PrintNightFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("PrintChasseurFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("Flags").child("PrintVoteFlag").setValue(false)


        mDatabase.child("Party").child(gameName).child("endGame").setValue(false)
        mDatabase.child("Party").child(gameName).child("nightGame").setValue(false)
        mDatabase.child("Party").child(gameName).child("voteResult").setValue("")
        mDatabase.child("Party").child(gameName).child("winner").setValue(0)
        mDatabase.child("Party").child(gameName).child("goToHome").setValue(false)



        mDatabase.child("Party").child(gameName).child("lifePotion").setValue(true)
        mDatabase.child("Party").child(gameName).child("deathPotion").setValue(true)

        mDatabase.child("Party").child(gameName).child("FinishFlags").child("LoupFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("VoyanteFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("CupidonFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("SorciereFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("PipoteurFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("PipotedFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("VoteFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("ChasseurFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("LoverFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("LowerFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("DeadNightFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("DeadVoteFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("DeadChasseurFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("PrintNightFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("PrintChasseurFlag").setValue(false)
        mDatabase.child("Party").child(gameName).child("FinishFlags").child("PrintVoteFlag").setValue(false)


    }

    private fun startGame() {

        val lobbbyRef: String? = currentPlayer!!.currentGame

        mLobbyReference.child(lobbbyRef!!).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                if (dataSnapshot.exists()) {
                    lobby = dataSnapshot.getValue(LobbyModel::class.java)
                    val nbPlayerAsked = lobby!!.nbPlayer
                    val nbPlayerReady = lobby!!.listPlayer!!.size
                    if (lobby!!.masterId == currentPlayer!!.id) {
                        val playerList: MutableList<String?> = arrayListOf()

                        for (user in dataSnapshot.child("listPlayer").children) {

                            playerList.add(user.value as String)
                        }
                        if (nbPlayerAsked == nbPlayerReady) {

                            attributeRole(lobby, playerList)
                            mDatabase.child("Lobby").child(currentPlayer!!.currentGame!!).child("startGame")
                                .setValue(true)
                        } else {
                            Toast.makeText(context, "Pas assez de joueurs, veuillez attendre", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
                // ...
            }

        })
    }

    private fun attributeRole(lobby: LobbyModel?, listOfUser: MutableList<String?>) {
        lobby?.let {
            val nbPlayer = lobby.nbPlayer
            val listPlayer = lobby.listPlayer
            val listPlayerInGameId: List<String?> = listOfUser.filter { userId -> listPlayer!!.contains(userId!!) }
            val listPlayerInGame: ArrayList<PlayerModel?> = ArrayList()

            idIntoPlayerModel(listPlayerInGameId, listPlayerInGame, nbPlayer)

        }
    }

    private fun idIntoPlayerModel(
        listPlayerInGameId: List<String?>,
        listPlayerInGame: ArrayList<PlayerModel?>,
        nbPlayer: Int
    ) {

        val mUserReference = FirebaseDatabase.getInstance().getReference("Users")

        mUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user: MutableList<PlayerModel?> = arrayListOf()
                if (dataSnapshot.exists()) {
                    user.clear()
                    for (i in dataSnapshot.children) {
                        user.add(i.getValue(PlayerModel::class.java))
                    }
                    for (j in listPlayerInGameId) {
                        for (i in user) {
                            if (i?.id == j) {
                                listPlayerInGame.add(i)
                            }
                        }
                    }

                    val roleList = getRandomRoles(nbPlayer)

                    listPlayerInGame.forEachIndexed { key, player ->
                        player?.role = roleList[key].name
                    }
                    listPlayerInGame.forEach {
                        mDatabase = FirebaseDatabase.getInstance().reference.child("")
                        mDatabase.child("Users").child(it?.id.toString()).child("role").setValue(it?.role)
                        mDatabase.child("Users").child(it?.id.toString()).child("charmed").setValue(false)
                        mDatabase.child("Users").child(it?.id.toString()).child("state").setValue(true)
                        mDatabase.child("Users").child(it?.id.toString()).child("inLove").setValue(false)

                        if (it?.role == "Sorcière") {
                            mDatabase.child("Users").child(it.id).child("deathPotion").setValue(true)
                            mDatabase.child("Users").child(it.id).child("lifePotion").setValue(true)
                        }
                    }

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun getRandomRoles(nbPlayer: Int): ArrayList<RoleModel> {
        val list = when (nbPlayer) {
            4 -> arrayListOf(
                LoupGarou(),
                Villageois(), Villageois(),
                Voyante()
            )

            5 -> arrayListOf(
                LoupGarou(),
                Villageois(), Ange(),
                Voyante(),
                Chasseur()
            )

            6 -> arrayListOf(
                LoupGarou(), LoupGarou(),
                Villageois(), Ange(),
                Voyante(),
                Chasseur()
            )

            7 -> arrayListOf(
                LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Ange(),
                Voyante(),
                Chasseur()
            )

            8 -> arrayListOf(
                LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Villageois(), Ange(),
                Voyante(),
                Chasseur()
            )

            9 -> arrayListOf(
                LoupGarou(), LoupGarou(), LoupGarou(),
                Villageois(), Sorciere(), Ange(),
                Voyante(),
                Chasseur(),
                Cupidon()
            )


            10 -> arrayListOf(
                LoupGarou(), LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Sorciere(),
                Voyante(),
                Chasseur(),
                Cupidon(),
                Ange()
            )

            11 -> arrayListOf(
                LoupGarou(), LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Villageois(), Villageois(),
                Voyante(),
                Chasseur(),
                Cupidon(),
                Sorciere()
            )

            12 -> arrayListOf(
                LoupGarou(), LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Villageois(), Sorciere(),
                Voyante(),
                Chasseur(),
                Cupidon(),
                Ange(),
                Pipoteur()
            )

            13 -> arrayListOf(
                LoupGarou(), LoupGarou(), LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Villageois(), Villageois(),
                Voyante(),
                Chasseur(),
                Cupidon(),
                Sorciere(),
                Pipoteur()
            )

            14 -> arrayListOf(
                LoupGarou(), LoupGarou(), LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Villageois(), Villageois(), Sorciere(),
                Voyante(),
                Chasseur(),
                Cupidon(),
                Ange(),
                Pipoteur()
            )

            15 -> arrayListOf(
                LoupGarou(), LoupGarou(), LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Villageois(), Villageois(), Villageois(), Villageois(),
                Voyante(),
                Cupidon(),
                Chasseur(),
                Sorciere(),
                Pipoteur()
            )

            16 -> arrayListOf(
                LoupGarou(),
                LoupGarou(),
                LoupGarou(),
                LoupGarou(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Voyante(),
                Ange(),
                Cupidon(),
                Chasseur(),
                Sorciere(),
                Pipoteur()
            )

            17 -> arrayListOf(
                LoupGarou(), LoupGarou(), LoupGarou(), LoupGarou(),
                Villageois(), Villageois(), Villageois(), Villageois(), Villageois(), Villageois(), Villageois(),
                Voyante(),
                Ange(),
                Cupidon(),
                Chasseur(),
                Sorciere(),
                Pipoteur()
            )

            18 -> arrayListOf(
                LoupGarou(),
                LoupGarou(),
                LoupGarou(),
                LoupGarou(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Villageois(),
                Voyante(),
                Ange(),
                Cupidon(),
                Chasseur(),
                Sorciere(),
                Pipoteur()
            )
            else -> arrayListOf(LoupGarou(), LoupGarou(), Voyante())
        }
        list.shuffle()
        return list
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
}
