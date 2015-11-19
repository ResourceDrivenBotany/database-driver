# database-driver
was example database driver. Now is fledgeling server side code with networking

UPDATE NOV 19 3:38PM
Rudimentary gameplay added to both client and server. We can now print a calculated “growth” on server given by info sent by client. TODO: Scent/attacks. will be messy? probably.

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
	gameplay loop:
		int: resource ID
		int: resource amount
NO DATA IS CURRENTLY CHECKED EITHER ON CLIENT OR SERVERSIDE! crashes abound.

the Game is currently default "TheGame" PK value 1
