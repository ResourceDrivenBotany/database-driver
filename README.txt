# database-driver
was example database driver. Now is fledgeling server side code with networking

explanation video uploaded Nov 18 5:20pm
https://www.youtube.com/watch?v=MbWyeSTw3fg&feature=youtu.be
In case you’re interested in following the current state of the server

I know it's a beautiful mess but things will get cleaned up in just a sec. Or some days. Sponsored by Rockstar Energy Drink tm.
don't judge my main method she's a BBW. will clean later.

Currently all tables are loaded and in use, loading values sent by client in following order

UTF: player name
UTF: plant name
int: plant type (leaving default as 1 for name)
Then the server does its magic and once numberofplayers == playersneededtostartgame gameplay loop begins

the Game is currently default "TheGame" PK value 1


gameplay loop stub in progress.

TODO(short term): 
	gameplay:	1. read resource values from database
			2. increment by resource + amount sent by client
			3. plug into formula, then add to current game-growth-total[player#]
			4. send resource totals back to database.

		Scent “attack” will suck. write this after everything else is working. Requires collaboration between server and client side.

	final: send msg to client who won- save score to Scores table

GAMEPLAY NOTES: Constantly reading and writing to the database is a PITA. Just store values within each round in an array on the server for now. database will be updated at the end of each round.