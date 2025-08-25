8 Steam Workshop
================

Maps that you have created can be uploaded to steam workshop so that other Dominions players can easily use the maps and comment on them.

When other users subscribe to a map that you have on the steam workshop, they will also receive any updates you make later on. However when a new game is started, the terrains, locations and borders of all provinces are fixed and future updates to the map will not change any of that. Updates to a map during an ongoing game will only affect the graphics of the map.

First you need to place your map in a single folder in your local maps folder. To find where your local maps folder is start the game, click ‘Tools & Manuals’, ‘Open User Data Directory’ then the game will open a file browser where you can find the ‘maps’ folder. If you call your new map Antworld you should have the following files (.tga can be be .rgb instead).

    ..../maps/Antworld/Antworld.map
    ..../maps/Antworld/Antworld.tga
    ..../maps/Antworld/Antworld_winter.tga
    ..../maps/Antworld/banner.png
    ..../maps/Antworld/dom5ws.txt

The last two files (banner.png and dom5ws.txt) are only used for workshop purposes and you will have to create them now. The following two chapters describe what they are.

8.1 Workshop file: banner.png
-----------------------------

This is the icon for the workshop mod, it is only used in steam. It must be a 128\*128 pixels large PNG file.

8.2 Workshop file: dom5ws.txt
-----------------------------

This is a simple text file that sets the visibility status for your workshop map. It should contain one of the following lines.

    Visibility = "Public"
    Visibility = "Friends"
    Visibility = "Private"

Public means everyone will be able to see the workshop map, Private means only you will be able to and Friends is something in between. You can change this setting later by editing this file.

8.3 Workshop file: dom5ws\_pfid
-------------------------------

You should not create or edit this file. It will be created after your first upload to the workshop and it is necessary to keep this file to be able to edit the map later. If you delete this file a new workshop map will be created instead.

8.4 Uploading to Workshop
-------------------------

Start Dominions 5 (from Steam) click ‘Tools & Manuals’, ‘Map Editor’ and ‘Upload map to Steam Workshop’. Now select your .map file.

8.5 Important Note
------------------

Don’t use different maps with the same image file name or Dominions 5 might pick the wrong image to use even though they might be in different folders.

So if two maps e.g. Antworld.map and Antworld2.map both reference Antworld.tga, then an upload to the workshop is likely to fail. Hopefully this inconvenience can be fixed in the future.

