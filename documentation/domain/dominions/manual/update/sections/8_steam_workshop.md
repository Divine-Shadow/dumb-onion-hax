8 Steam Workshop
================

Maps that you have created can be uploaded to Steam Workshop so that other Dominions players can easily use the maps and comment on them. When other users subscribe to a map that you have on the Steam Workshop, they will also receive any updates you make later on. However when a new game is started, the terrains, locations and borders of all provinces are fixed and future updates to the map will not change any of that. Updates to a map during an ongoing game will only affect the graphics of the map.

First you need to place your map in a single folder in your local maps folder. To find where your local maps folder is start the game, click 'Tools & Manuals', 'Open User Data Directory' then the game will open a file browser where you can find the 'maps' folder. If you call your new map Antworld you should have the following files (.tga can be .rgb instead).

```
..../maps/Antworld/Antworld.map
..../maps/Antworld/Antworld.tga
..../maps/Antworld/banner.png
..../maps/Antworld/dom6ws.txt
```

The last two files (banner.png and dom6ws.txt) are only used for workshop purposes and you will have to create them now. The following two chapters describe what they are.

### Workshop file: banner.png

This is the icon for the workshop mod, it is only used in Steam. It must be a PNG file that is 256x256 or 512x512 pixels large.

### Workshop file: dom6ws.txt

This is a simple text file that sets the visibility status for your workshop map. It should contain one of the following lines.

```
Visibility = "Public"
Visibility = "Friends"
Visibility = "Private"
```

Public means everyone will be able to see the workshop map, Private means only you will be able to and Friends is something in between. You can change this setting later by editing this file.

### Workshop file: dom6ws_pfid

You should not create or edit this file. It will be created after your first upload to the workshop and it is necessary to keep this file to be able to edit the map later. If you delete this file a new workshop map will be created instead.

### Uploading to Workshop

Start Dominions 6 (from Steam) click 'Tools & Manuals', 'Map Editor' and 'Upload map to Steam Workshop'. Now select your .map file.

### Important Note

Always name the map the same as the folder it is located in. So Antworld.map should be inside a folder called Antworld. The main image for that map should be called Antworld.tga.
