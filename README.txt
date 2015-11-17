# database-driver
was example database driver. Now is fledgeling server side code with networking

download the Client and try it out!

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
	true multithreaded connections
	gameplay part 1: inc++ resources 1-3 specified by client
	gameplay part 2: database arithmetic
	gameplay part 3: send msg to client who won- save score to Scores table