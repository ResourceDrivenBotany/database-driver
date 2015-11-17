# database-driver
was example database driver. Now is fledgeling server side code with networking

download the Client and try it out!

I know it's a beautiful mess but things will get cleaned up in just a sec. Or some days. Sponsored by Rockstar Energy Drink tm.
don't judge my main class she's a BBW. will clean later.

Currently all tables are loaded and in use, loading values sent by client in following order

UTF: player name
UTF: plant name
int: plant type (leaving default as 1 for name)
Then the server does its magic and once numberofplayers == playersneededtostartgame gameplay loop begins

the Game is currently default "TheGame" PK value 1

You will see a small gameplay loop stub where gameplay will be added, probably after Server sends a flag to client indicating "go-time!"