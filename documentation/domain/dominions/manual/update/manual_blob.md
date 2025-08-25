Dominions 6 Map Making Manual
Illwinter Game Design (ver 6.26)
Introduction
This document is a guide to making maps for Dominions 6. It
covers the basics of how to create a map and how to use the
functionality built into Dominions to customize and develop
them beyond the basics provided by the graphical interface of
the map editor. This manual should give the reader all the
tools necessary to create and modify maps, but it does not
provide an extensive tutorial on how to create a map from
scratch. Image processing guides are beyond the scope of this
document. The chapter about the random map generator
provides only the basics. The full potential of the RMG is
beyond the scope of this manual.
Requirements
With the help of a simple text editor and a paint program like
GIMP or Photoshop, it is possible to create new maps for
Dominions 6.
Data Directories
The data directory for storing saved games, mods and maps is
different depending on the operating system.
* Linux: ~/.dominions6
* Mac: ~/.dominions6
* Windows: %APPDATA%\dominions6
The easiest way to locate the data directories in Dominions 6
is by opening Game Tools from the Main Menu and using the
Open User Data Directory function. The operating system will
open the user data directory in a separate file manager
window.
Legal File Names
It is important that the file name doesn't contain any special
characters or spaces. Valid characters are a-z, A-Z,
underscore and of course a dot for the extension. Illegal file
names might work for single player, but will not work in
multiplayer.
The Map Image File
The image file of the map must be in Targa (.tga) format. The
file should be at least 256x256 pixels large and saved in 24 or
32 bit color. A suitable size for a map might be about
1600x1200 pixels. This section discusses issues and common
problems related to or caused by something in the map image
file or interactions between the map image file and map file.
Defining Provinces
Provinces are defined by white pixels (RGB value 255, 255,
255). Each single white pixel is considered a separate
province. If you need some white color on your map, use
something like (253, 253, 253) for white. It will look white to
the human eye, but will not cause extra provinces to appear.
When you add provinces to a map image, remember to use a
single 1x1 pixel brush. It is also a good idea to use an image
manipulation program that supports layers, so you can put the
province pixels in a separate layer and later merge them down
to form the final map file. That way mistakes will be easier to
correct.
Province Borders
Province borders are not strictly necessary in the map image,
but they are helpful for players in visualizing where to go and
which province borders which.
Drawing borders on a map image is best done on a separate
layer, which is then merged down to form the final image file.
This allows you to also export an image with borders only that
can be used by the map editor to automatically calculate the
correct province shapes (ctrl-b when in province area mode).
Transparency
An image file saved with improper alpha channel settings can
result in Dominions treating the image as being transparent
and showing only the map background instead of the map
graphic. This problem is fixed by removing the alpha channel
before saving.
The Map Editor
Once you have drawn your map you have to create a .map file
that contains certain information about the map. For
example, some of the information in the map file is the title of
the map and which provinces borders on which. The easiest
way to create a working map file is to use the map editor in
Dominions 6.
The Map Editor is located in the Game Tools menu. To edit an
existing map, select the Load Map option and then the desired
map. To create a new map, select New Map and enter the file
name of the map image file. Once you have created the map in
the editor and saved it, it can be used to play Dominions 6.
Interface
The map editor interface is very simple. There is box with a
list of province number, name, terrain types and other
province properties on the left and the rest of the screen is
taken up by the map itself. Provinces are marked by a silver
flag in their center. In a new map the provinces won't have any
names, but you can click on the province number to give it a
name. If a province is not named it will be given a suitable
1
random name when the game starts.
There are two modes in the map editor, the default is the
neighbor mode where you modify connections, terrains and
other province attributes. By pressing tab you will switch to
the Province Area mode that is used to setup which areas of
the map belongs to what province.
The province areas must be known for the game so that the
dominion overlay will appear over the correct areas. In this
mode you paint the area that is owned by the active province.
In both modes you can right click on the map to change the
active province.
Setting Connections
Provinces that have a connection between them are called
neighboring provinces, or neighbors.
To select a province, right-click on it.
To set a neighbor, click on an adjacent province.
To remove a neighbor, Ctrl-click on an adjacent province.
To change a standard connection to a mountain pass,
shift-click on the adjacent province once. Mountain passes are
shown as orange lines.
Impassable mountain borders are shown shown as a red lines.
These do not affect movement in any way, but they might
affect how the map looks.
To change a standard connection to a river border, shift-click
on the adjacent province thrice. River crossings are shown as
dark blue lines.
To change a standard connection to an impassable border,
shift-click on the adjacent province five times. Impassable
borders are shown as grey lines. Impassable borders can only
be utilized for spell targeting purposes.
Shift-click a neighbor to cycle through standard / border
mountains / mountain pass / river / bridge / impassable
connections without removing it.
Press r to change a border to a road. Roads borders are shown
as green lines.
Setting Terrain Type
To set a terrain type for the selected province, check all the
terrain types that apply from the command box.
As a general rule, a single province should only contain one
adverse terrain type (forest, waste, highland, swamp, cave), or
at the very most two. Adverse terrain slows down movement
and too many provinces with mixed terrain make a map
impassable and not very enjoyable to play on. Mountain only
indicates that a province is close to a mountain and does not
slow down movement.
You can freely mix small or large province markers, fresh
water, nostart (red castle), start (green castle), throne and
many sites with any terrain without affecting movement on
the map. Many sites means a province with a higher chance of
containing magic sites and should be used sparingly or where
map thematics require it.
All of the values set by these options are added into the
terrain mask of the province. (see section Terrain Type in the
Map File for more details). Note that some rare special terrain
types cannot be set from the map editor, these can be
manually added to the map file later if need be.
The keyboard shortcuts for the map editor can be seen by
pressing '?'.
Load the map you have drawn and click in all borders and
terrains. After the map file has been saved you can edit it
manually in a text editor if you want to add some extra map
commands. Many map commands cannot be entered via the
map editor.
Several advanced map commands will require knowing
monster numbers, fort numbers, poptype numbers, magic site
numbers or other identifying information. In addition to
terrain types and victory conditions, the tables in this manual
list nation numbers, fort numbers and poptype numbers.
Names and numbers for magic sites and monsters are
available through other fan-made documentation. You can
also check monster numbers and item numbers in the game by
selecting the monster or item and pressing ctrl+i.
Painting Province Areas
Press tab to switch between the Neighbor and Province Area
modes. In the Province Area mode you set the area that each
province should occupy. This must be set for the dominion
overlay to look correct and for mouse clicks to select the
province you actually click on.
The easiest way to get all areas correct is if you have a
separate image with only borders. It can be thin colored
borders on a black background or just a different solid colors
for each province (the solid colors can be reused for non
adjacent provinces). You can also use an image with thin
borders and an transparent background, but the borders
mustn't be black if you do it this way, as all transparency will
be replaced by black by the map editor.
Press ctrl-b to set all border areas from a border image. After
that you should press ctrl-x a few times to expand all areas if
there is gap between them. Then press ctrl-l to remove any
stray pixels that might be left and then you are are finished.
2
If you haven't got a border image you will have to paint the
province areas using the editor. Right click on a province to
select it, then paint the area. Pressing 0 will select no
province, which can be used to paint areas that are not used
by any province at all.
The Map File Commands
The .map file contains text commands and usually all of these
have been created by the map editor. But it can be useful to
edit some of these commands manually in a text editor for
some fine tuning that cannot be done in the editor.
Syntax
Two consecutive dashes -- are used to denote comments.
Anything after the dashes is ignored. This can be used to add
explanations of what is being added to a map file and what is
being intended with the commands.
All Dominions 6 map commands begin with a # sign. Map
commands may or may not have arguments. An argument is a
value after the command itself to denote something.
Arguments are indicated by <object> after the command
itself, e.g.
#setland <province nbr>
A map command that does not have an argument is used to
assign a fixed effect. The effect of these commands are
always the same.. Map commands that have arguments can
have one or more of them and the arguments may be of
several different types. Some commands require more than
one type of argument to work.
The following types of arguments are used for map commands
in Dominions 6:
* integer: a whole number
* percent: a percentage value (may be higher than 100 in some
cases). Expressed as integer in mod syntax, interpreted as
percentage.
* "string": text such as province names, map descriptions etc.
* bitmask: a bitmask is a special type of integer number. Any
integer
each power of 2 that it contains means a different thing. A
bitmask argument assigns ALL of these attributes to the
object being modded.
#terrain <prov nbr> <terrain mask>
is a command where the first argument of the command
selects the target province and the second argument bitmask
operation that assigns the specified terrain. Setting a bitmask
of 1601 (1+64+512+1024) would mean a small province (1)
that is a wasteland (64), which cannot be a starting location
(512) and which has a high probability of containing many
magic sites (1024).
Required Map Commands
These map commands must exist in every map file or the map
will not work.
#dom2title <text>
The title of the map. This must be the first command for every
map. The reason why this command is named #dom2title instead
of #dom6title is because the map command syntax used here
was first introduced in Dominions 2: The Ascension Wars and
much of it has been kept the same since then.
#imagefile <filename>
The image file of the map in TGA format. The file should be at
least 256x256 pixels large and saved in 24 or 32 bit color
formats. A suitable size for a map might be about 1600x1200
pixels. For more detailed information related to the image file,
see Chapter "The Map Image File".
#mapsize <width> <height>
Lets Dominions know the size of the map in pixels.
Basic Map Commands
These map commands are some of the most basic commands
of map making. These commands do not require an active
province (see Province Commands) even if they affect a
specific province.
#domversion <version>
Set the minimum version of Dominions required to use this map.
This number is usually 600 for Dominions 6 maps, meaning
Dominions version 6.00 or higher is required.
#description "text"
The description of the map that is shown after selecting a map to
play on.
#planename <text>
The name of the plane. The default is "Pantokrator's Realm".
#mapnohide
This command will prevent province fog of war on this plane.
This means that the entire map image will be show at once
without any obscured areas.
#nodeepcaves
If this command is used no extra random cave plane will be
added to this plane. If you have multiple planes, this command
must be added to all planes that should not have a cave
underneath, otherwise the caves will be added to the first plane
without this command.
#nodeepchoice
Disables the choice of underground plane. It will always be set to
off. If you have nodeepcaves on your only plane or all of your
planes if many, yoy should add this command as well. It will make
3
the map setup less confusing as no underground plane would be
added anyway.
#neighbour <province nbr> <province nbr>
Makes it possible to move between these two provinces (in both
directions). Use the map editor to set province neighbors. Doing
it from the map file with a text editor is VERY difficult.
#neighbourspec <land1> <land2> <spcnbr>
This command can be used to create a mountain pass or other
type of special border between two provinces. Spcnbr indicates
a special border types from these values: 0 = standard border, 1
= mountain pass, 2 = river border, 4 = impassable, 8 = road. You
really should use the map editor to enter this information.
#pb <x> <y> <len> <province nbr>
Sets what pixels belong to which province. This information
really cannot be entered without using the map editor.
#landname <province nbr> "name"
Sets the name of a specific province.
Terrain Type in the Map File
#terrain <province nbr> <terrain mask>
Sets the terrain of a province. The terrain is calculated by adding
certain numbers for different terrain types or other attributes.
Common Terrain Masks
2-pow Number Terrain
- 0 Plains
0 1 Small Province
1 2 Large Province
2 4 Sea
3 8 Freshwater
4 16 Highlands (or gorge)
5 32 Swamp
6 64 Waste
7 128 Forest (or kelp forest)
8 256 Farm
9 512 Nostart
10 1024 Many Sites
11 2048 Deep (combine with sea)
12 4096 Cave
23 8388608 Mountains
25 33554432 Good throne location
26 67108864 Good start location
27 134217728 Bad throne location
30 1073741824 Warmer
31 2147483648 Colder
36 68719476736 Cave Wall
Rare Terrain Masks
2-pow Number Terrain
13 8192 Fire sites
14 16384 Air sites
15 32768 Water sites
16 65536 Earth sites
17 131072 Astral sites
18 262144 Death sites
19 524288 Nature sites
20 1048576 Glamour sites
21 2097152 Blood sites
22 4194304 Holy sites
You should use the map editor to set the terrain values as it
would be very difficult to do it by hand.
Basic terrain masks are listed in tables Common Terrain
Masks and Rare Terrain Masks. Note that the terrain masks
used in editing maps are NOT the same as the terrain masks in
the Modding Manual that are used for modding magic sites.
All terrain masks listed in the Common Terrain Masks table
can be set from the map editor.
The terrain masks in the Rare Terrain Masks table cannot be
added from the map editor and you must add them to the base
terrain mask calculated by the map editor. The advanced
terrain masks make it more likely that when a magic site is
placed in the province, it will be of that specific type.
#gate <province nbr> <gate nbr>
This command creates a gateway in the current province.
Gateways connect to other gateways of the same number.
Advanced Map Commands
These map commands are not necessary to get a working
map, but they allow a great deal of customization and
enhancement. These commands do not require an active
province (see Province Commands) even if they affect a
specific province. Many of these commands are global and
affect all provinces on the map or map attributes that are not
directly tied to a specific province. It is recommended that
they be placed at the start of the map file after the
description.
#maptextcol <red> <green> <blue> <alpha>
Sets the color used to print province names. Each value should
be a decimal number between 0.0 and 1.0.
#mapdomcol <red> <green> <blue> <alpha>
Sets the color used for dominion overlay. Each value should be
an integer between 0 and 255.
#saildist <1-10>
Sets the maximum sail distance in sea provinces. A commander
with the sailing ability will be able to pass this many sea
provinces. It default to 2, but if seas are very large or
strategically important it might be good to reduce this to 1.
#features <0-100>
Sets the magic site frequency. This command will override the
site frequency specified in the game setup screen.
4
#nohomelandnames
When this switch is used, homelands will no longer be named
after their starting nations. For example, the home of Abysia
might be called The Summer Lands or whatever.
#nonamefilter
Map filter that displays province names is disabled when this
command is used.
#allowedplayer <nation nbr>
Makes this nation one of the allowed nations to play on this map.
Use this command multiple times or the map will only be able to
host one player. Nation numbers can be found in the tables
below. This command can be used to make era specific maps.
Early era nations
5 Arcoscephale, Golden Era
6 Mekone, Brazen Giants
7 Pangaea, Age of Revelry
8 Ermor, New Faith
9 Sauromatia, Amazon Queens
10 Fomoria, The Cursed Ones
11 Tir na n'Og, Land of the Ever Young
12 Marverni, Time of Druids
13 Ulm, Enigma of Steel
14 Pyrène, Kingdom of the Bekrydes
15 Agartha, Pale Ones
16 Abysia, Children of Flame
17 Hinnom, Sons of the Fallen
18 Ubar, Kingdom of the Unseen
19 Ur, The First City
20 Kailasa, Rise of the Ape Kings
21 Lanka, Land of Demons
22 T'ien Ch'i, Spring and Autumn
23 Yomi, Oni Kings
24 Caelum, Eagle Kings
25 Mictlan, Reign of Blood
26 Xibalba, Vigil of the Sun
27 C'tis, Lizard Kings
28 Machaka, Lion Kings
29 Berytos, The Phoenix Empire
30 Vanheim, Age of Vanir
31 Helheim, Dusk and Death
32 Rus, Sons of Heaven
33 Niefelheim, Sons of Winter
34 Muspelheim, Sons of Fire
40 Pelagia, Pearl Kings
41 Oceania, Coming of the Capricorns
42 Therodos, Telkhine Spectre
43 Atlantis, Emergence of the Deep Ones
44 R'lyeh, Time of Aboleths
Middle era nations
50 Arcoscephale, The Old Kingdom
51 Phlegra, Deformed Giants
52 Pangaea, Age of Bronze
53 Asphodel, Carrion Woods
54 Ermor, Ashen Empire
55 Sceleria, The Reformed Empire
56 Pythium, Emerald Empire
57 Man, Tower of Avalon
58 Eriu, Last of the Tuatha
59 Agartha, Golem Cult
60 Ulm, Forges of Ulm
61 Marignon, Fiery Justice
62 Pyrène, Time of the Akelarre
63 Abysia, Blood and Fire
64 Ashdod, Reign of the Anakim
65 Na'Ba, Queens of the Desert
66 Uruk, City States
67 Ind, Magnificent Kingdom of Exalted Virtue
68 Bandar Log, Land of the Apes
69 T'ien Ch'i, Imperial Bureaucracy
70 Shinuyama, Land of the Bakemono
71 Caelum, Reign of the Seraphim
72 Nazca, Kingdom of the Sun
73 Mictlan, Reign of the Lawgiver
74 Xibalba, Flooded Caves
75 C'tis, Miasma
76 Machaka, Reign of Sorcerors
77 Phaeacia, Isle of the Dark Ships
78 Vanheim, Arrival of Man
79 Vanarus, Land of the Chuds
80 Jotunheim, Iron Woods
81 Nidavangr, Bear, Wolf and Crow
85 Ys, Morgen Queens
86 Pelagia, Triton Kings
87 Oceania, Mermidons
88 Atlantis, Kings of the Deep
89 R'lyeh, Fallen Star
Late era nations
95 Arcoscephale, Sibylline Guidance
96 Phlegra, Sleeping Giants
97 Pangaea, New Era
98 Pythium, Serpent Cult
99 Lemuria, Soul Gates
100 Man, Towers of Chelms
101 Ulm, Black Forest
102 Agartha, Ktonian Dead
103 Marignon, Conquerors of the Sea
104 Abysia, Blood of Humans
105 Ragha, Dual Kingdom
106 Caelum, Return of the Raptors
107 Gath, Last of the Giants
108 Patala, Reign of the Nagas
5
109 T'ien Ch'i, Barbarian Kings
110 Jomon, Human Daimyos
111 Mictlan, Blood and Rain
112 Xibalba, Return of the Zotz
113 C'tis, Desert Tombs
115 Midgård, Age of Men
116 Bogarus, Age of Heroes
117 Utgård, Well of Urd
118 Vaettiheim, Wolf Kin Jarldom
119 Feminie, Sage-Queens
120 Piconye, Legacy of the Prester King
121 Andramania, Dog Republic
125 Erytheia, Kingdom of Two Worlds
126 Atlantis, Frozen Sea
127 R'lyeh, Dreamlands
Special Nations
Number Nation Note
0 Independents
2 Special Independents e.g. Horrors
4 Roaming Independents e.g. Barbarians
#computerplayer <nation nbr> <difficulty>
This nation will always be controlled by the computer. Difficulty
ranges from one to five. One is Easy AI. Two is Standard
difficulty, followed by Difficult (3), Mighty (4) and Impossible (5)
AI.
#victorycondition <condition> <attribute>
The game will end when one player fulfills a special condition, see
table Victory Conditions. Dominion score is 11-20 points per
converted province, depending on the strength of the dominion.
The value of 'condition'
Victory Conditions
Number Condition Attribute
0 Standard Nothing
6 Thrones Nbr of Ascension Points
#cannotwin <nation nbr>
This nation will not win even if they fulfill the victory condition.
Setting Start Locations
These commands allow you to set or deny specific provinces
as start locations and to control which nations starts where on
a map. The specstart locations will be used if you create a
game with the Use special starting locations option enabled.
#start <province nbr>
Sets a recommended start location. By creating at least one
start location for each player, every player will start at one of
these locations. If start provinces are set, nations will start at
these locations unless there are more nations than start
provinces. If there are more nations than start provinces, the
extra nations will start in eligible random locations.
If a province is set as a start province but its terrain mask
includes the value 512 (nostart), the nostart will override the
start command and no nation will start there. If no start
provinces are set, all provinces are available as random
starting locations unless set nonstartable with the nostart
command or in the map editor.
#nostart <province nbr>
Tags a province as nonstartable. No player will start here when
placed at random. This command can also be set from the map
editor, which adds 512 to the province's terrain mask.
#specstart <nation nbr> <land nbr>
Use this command to assign a specific nation to a specific start
location. Nation numbers can be found in the Nations tables. If
you use the #specstart command, please note that using the
#land command to select the starting province of the nation for
further modification results in the nation starting with no troops
and a dead god. This is because the #land command kills all units
initially placed in the province. In such situations the #setland
command should be used instead.
#teamstart <land nbr> <team nbr>
This command can be used in disciple games to force teams to
start at certain positions. E.g. to make one team start on one side
of the map and the other team on the other side. Team nbr is a
value between 0 and number of teams - 1. This value doesn't
correspond to the team number used when creating a game, it's
random which team will get which teamstart position. Use the
map editor and press ctrl 0-7 to set up the team positions in an
easy way.
Province Commands
These commands are used to manipulate specific provinces in
order to set different features manually instead of being
randomly assigned during game setup. Unless otherwise
specified, they only affect the active province.
#land <province nbr>
Sets the active province and kills everyone in it. All the following
commands will only affect the active province. Use this
command if you want to activate a province in order to replace
its random inhabitants with the monsters of your choice.
#setland <province nbr>
Sets the active province. All the following commands will only
affect the active province.
#poptype <poptype nbr>
Sets the population type of the active province. This determines
which troops may be recruited in the province. Poptype
numbers can be found in the "poptypes" table.
This command will override the poptype that was randomly
assigned to the province during game creation, but it will NOT
change the independent defenders, which will be of the
6
poptype this command overwrote.
Example: if the randomly determined poptype during game
creation was 42 (Jade Amazons) and the poptype has been set
to 25 (Barbarians) by this map command, the independent
defenders will still be Jade Amazons. You just won't be able to
recruit them.
Poptypes
Nbr Poptype
25 Barbarians
26 Horse Tribe
27 Militia, Archers, Hvy Inf
28 Militia, Archers, Hvy Inf
29 Militia, Archers, Hvy Inf
30 Militia, Longbow, Knight
31 Tritons
32 Lt Inf, Hvy Inf, X-Bow
33 Lt Inf, Hvy Inf, X-Bow
34 Raptors
35 Slingers
36 Lizards
37 Woodsmen
38 Hoburg
39 Militia, Archers, Lt Inf
40 Amazon, Crystal
41 Amazon, Garnet
42 Amazon, Jade
43 Amazon, Onyx
44 Troglodytes
45 Tritons, Shark Knights
46 Amber Clan Tritons
47 X-Bow, Hvy Cavalry
48 Militia, Lt Inf, Hvy Inf
49 Militia, Lt Inf, Hvy Inf
50 Militia, Lt Inf, Hvy Inf
51 Militia, Lt Cav, Hvy Cav
52 Militia, Lt Cav, Hvy Cav
53 Militia, Lt Cav, Hvy Cav
54 Hvy Inf, Hvy Cavalry
55 Hvy Inf, Hvy Cavalry
56 Hvy Inf, Hvy Cavalry
57 Shamblers
58 Lt Inf, Hvy Inf, X-Bow
59 Militia, Lt Inf, Archers
60 Militia, Lt Inf, Archers
61 Vaettir, Trolls
62 Tribals, Deer
63 Tritons
64 Tritons
65 Ichtyids
66 Vaettir
67 Vaettir, Dwarven Smith
68 Slingers, Hvy Inf, Elephants
69 Asmeg
70 Vaettir, Svartalf
71 Trolls
72 Mermen
73 Tritons, Triton Knights
74 Lt Inf, Lt Cav, Cataphracts
75 Hoburg, LA
76 Hoburg, EA
77 Atavi Apes
78 Tribals, Wolf
79 Tribals, Bear
80 Tribals, Lion
81 Pale Ones
82 Tribals, Jaguar
83 Tribals, Toad
84 Cavemen
85 Kappa
86 Bakemono
87 Bakemono
88 Ko-Oni
89 Fir Bolg
90 Turtle Tribe Tritons
91 Shark Tribe Tritons
92 Shark Tribe, Shark Riders
93 Zotz
94 Lava-born
95 Ichtyids with Shaman
96 Bone Tribe
97 Merrow
98 Kulullu
99 Bronze Hoplites
100 Bronze Hvy Inf
101 Bronze Hvy Cav, Hvy Inf
102 Bronze Hvy Spear
103 Cynocephalians
104 Bekrydes
105 Wet Ones
106 Nexus
#owner <nation nbr>
Changes the ownership of the active province. Nation nbr
indicates the new owner.
#killfeatures
Removes all magic sites from the active province.
#feature "<site name>" \| <site nbr>
Puts a specific magic site in the active province. Adding unique
sites to a map using this command will NOT prevent those sites
from appearing randomly, because the map file is only applied to
the game map after game setup has done random determination
of sites for each province.
7
#knownfeature "<site name>" \| <site nbr>
Puts a specific magic site in the active province. This site is
already found at the start of the game, regardless of its ordinary
path level. Using this command prevents special features of the
site that depend on its discovery from activating.
#fort <fort nbr>
Puts a specific fort in the active province. Fort numbers can be
found in the "Fortifications" table. Will replace a nation's default
fort if used on a capital location.
Fortifications
Number Fort
1 Palisades
2 Fortress
3 Castle
4 Citadel
5 Rock Walls
6 Fortress
7 Castle
8 Castle of Bronze and Crystal
9 Kelp Fort
10 Bramble Fort
11 City Palisades
12 Walled City
13 Fortified City
14 Great Walled City
15 Giant Palisades
16 Giant Fortress
17 Giant Castle
18 Giant Citadel
19 Grand Citadel
20 Ice Walls
21 Ice Fortress
22 Ice Castle
23 Ice Citadel
24 Wizard's Tower
25 Citadel of Power
27 Fortified village
28 Wooden Fort
29 Crystal Citadel
#temple
Puts a temple in the active province.
#lab
Puts a laboratory in the active province.
#unrest <0-500>
Sets the unrest level of the active province.
#population <0-50000>
Sets the population number of the active province.
#defence <0-125>
Sets the province defence of the active nation. This command
cannot be used for independent provinces.
#skybox "<pic.tga>
Sets the sky (battleground background) to a tga/rgb pic of your
choice for fights in the current province. The picture size should
be a power of two. 1024*1024 is a good size.
#batmap "<battlemap.d3m>
Sets the battleground that fights take place in for the current
province. You can use the special name ' empty ' for no
battleground, useful for battles in space perhaps. This will affect
fights both outside and inside castles.
#groundcol <red> <green> <blue>
Color the world with the specified colors for fights in the current
province. Color values range from 0 to 255.
#rockcol <red> <green> <blue>
Color the world with the specified colors for fights in the current
province. Color values range from 0 to 255.
#fogcol <red> <green> <blue>
Color the world with the specified colors for fights in the current
province. Color values range from 0 to 255.
Commander Commands
These commands are used to set specific monsters in the
active province and manipulate those monsters to modify
them from the base monster type to create thematic
provinces and special heroes. They must be used after the
#land or #setland commands, because they require an active
province. Whenever commanders and units are placed on a
map, the type can be set using either the monster number or
the monster name in quote marks. If the commander or unit
to be added is a new monster defined in a mod, then monster
number cannot be used and the name must be used instead.
#commander "<type>"
Puts one of these commanders in the active province. The
commander will have a random name according to its nametype.
This commander will be the active commander.
#comname "<name>"
Replaces the active commander’s random name with this one.
#bodyguards <nbr> "<type>"
Gives bodyguards to the active commander. This command only
affects independents. AI nations will ignore this command.
#units <nbr of units> "<type>"
Gives a squad of soldiers to the active commander.
#xp <0-900>
Gives experience points to the active commander.
8
#randomequip <rich>
Gives random magic items to the active commander. Rich must
be between 0 and 4. A value of 0 means small chance of getting a
magic item and 4 means large chance of getting many powerful
items.
#additem "<item name>"
Gives a magic item to active commander. Items cannot be
assigned by item number.
#clearmagic
Removes all magic skills from the active commander.
#mag_fire <level>
Gives active commander Fire magic.
#mag_air <level>
Gives active commander Air magic.
#mag_water <level>
Gives active commander Water magic.
#mag_earth <level>
Gives active commander Earth magic.
#mag_astral <level>
Gives active commander Astral magic.
#mag_death <level>
Gives active commander Death magic.
#mag_nature <level>
Gives active commander Nature magic.
#mag_glamour <level>
Gives active commander Glamour magic.
#mag_blood <level>
Gives active commander Blood magic.
#mag_priest <level>
Gives active commander Holy magic.
God Commands
These commands are used to set a specific pretender god for a
specific nation and will override the pretenders designed or
loaded during game setup. Each of these commands may be
used independently of the others. They do not require an
active province. If human controlled nations are assigned
gods or dominion scales, cheat detection will be triggered if
the player do not conform to the normal design point limits
for pretenders.
#god <nation nbr> "<type>"
Forces the god of one nation to be this monster. The god
becomes the active commander and can be manipulated with the
commander commands.
The same limitations on defining the commander type apply,
meaning that modded monsters must be defined by their
name instead of monster number. Using this command will
generate an error message and exit on game creation if the
nation is not in play on the map.
#dominionstr <nation nbr> <1-10>
Sets the dominion strength of a nation to a value between 1 and
10.
#scale chaos <nation nbr> <(-5)-5>
Forces the Order / Turmoil dominion scale of a nation to a value
between -5 and 5. A value of 5 means that the scale is fully
tipped to the right (Turmoil) and -5 means it is fully tipped to the
left (Order).
#scale lazy <nation nbr> <(-5)-5>
Forces the Productivity / Sloth dominion scale of a nation to a
value between -5 and 5. A value of 5 means that the scale is fully
tipped to the right (Sloth) and -5 means it is fully tipped to the
left (Productivity).
#scale cold <nation nbr> <(-5)-5>
Forces the Heat / Cold dominion scale of a nation to a value
between -5 and 5. A value of 5 means that the scale is fully
tipped to the right (Cold) and -5 means it is fully tipped to the left
(Heat).
#scale death <nation nbr> <(-5)-5>
Forces the Growth / Death dominion scale of a nation to a value
between -5 and 5. A value of 5 means that the scale is fully
tipped to the right (Death) and -5 means it is fully tipped to the
left (Growth).
#scale unluck <nation nbr> <(-5)-5>
Forces the Luck / Misfortune dominion scale of a nation to a
value between -5 and 5. A value of 5 means that the scale is fully
tipped to the right (Misfortune) and -5 means it is fully tipped to
the left (Luck).
#scale unmagic <nation nbr> <(-5)-5>
Forces the Magic / Drain dominion scale of a nation to a value
between -5 and 5. A value of 5 means that the scale is fully
tipped to the right (Drain) and -5 means it is fully tipped to the
left (Magic).
Multiple Planes
You can have a map with multiple planes if you want. Each
plane needs its own .map file and .tga file(s). The extra planes
must end with e.g. ..._plane2.map or ..._plane2.tga if it is plane
2 (the first extra plane).
When you start the map editor you should always load the
first plane, the rest if any will be loaded and saved
automatically.
There can be up to 8 planes in a map (7 extra ones).
9
Terrain Graphics
In order for Dominions to be able to change terrains of
provinces extra images are needed to show what those
terrains look like. If you have a map called mymap.map the
map images should be named like this:
mymap.tga Default look and water provinces
mymap_winter.tga Default winter look
mymap_forest.tga Look for forests
mymap_waste.tga Look for wastes
mymap_farm.tga Look for farms
mymap_swamp.tga Look for swamps
mymap_highland.tga Look for hioghlands
mymap_plain.tga Look for plains
mymap_kelp.tga Look for kelp forests
mymap_water.tga Look for drowned land provinces
All files (except the 2 first ones) can have a "w" added (e.g.
mymap_forestw.tga) to set the winter look for a specific
terrain. Note that if mymap.tga only contains plain provinces
there won't be any need for the mymap_plain.tga file.
It is not necessary to have all the terrain looks. The default
look will be used when no specific one is available.
If you don't provide specific terrain files you might want to set
the default terrain in the mpa editor. That way Dominions will
not randomize the start terrain of the provinces (except for
home provinces that will always be changed to a nations
preferred one).
If you have multiple planes the plane2 part should go first, e.g.
mymap_plane2_waste.tga.
Random Map Generator
Dominions 6 comes with a powerful random map generator
(RMG for short). The random map generator can be run from
the Game Tools menu and the preferences set as to size of
map, number of provinces, ratio of each terrain type and
various wrap options (east-west, north-south or full
wraparound).
The RMG creates beautifully rendered maps that are ready to
play right out of the box. However, it is worth using the map
editor to check the randomly generated map for province
connections that can be improved, because the random
generation procedure does not always see things like a human
player would.
Random Map Files
The RMG creates d6m files instead of the usual tga files that
are used to create hand drawn maps. The d6m files are unique
to Dominions 6 and contains the geography needed to render
the map with arbitrary terrains. The details of this file can be
found in the seperate "fileformat_d6m" document. It is usually
not necessary to know how this file is constructed, but
automated map generators might find it useful.
Steam Workshop
Maps that you have created can be uploaded to steam
workshop so that other Dominions players can easily use the
maps and comment on them.
When other users subscribe to a map that you have on the
steam workshop, they will also receive any updates you make
later on. However when a new game is started, the terrains,
locations and borders of all provinces are fixed and future
updates to the map will not change any of that. Updates to a
map during an ongoing game will only affect the graphics of
the map.
First you need to place your map in a single folder in your local
maps folder. To find where your local maps folder is start the
game, click 'Tools & Manuals', 'Open User Data Directory'
then the game will open a file browser where you can find the
'maps' folder. If you call your new map Antworld you should
have the following files (.tga can be be .rgb instead).
 ..../maps/Antworld/Antworld.map
 ..../maps/Antworld/Antworld.tga
 ..../maps/Antworld/banner.png
 ..../maps/Antworld/dom6ws.txt
The last two files (banner.png and dom6ws.txt) are only used
for workshop purposes and you will have to create them now.
The following two chapters describe what they are.
Workshop file: banner.png
This is the icon for the workshop mod, it is only used in steam.
It must be a PNG file that is 256x256 or 512x512 pixels large.
Workshop file: dom6ws.txt
This is a simple text file that sets the visibility status for your
workshop map. It should contain one of the following lines.
 Visibility = "Public"
 Visibility = "Friends"
 Visibility = "Private"
Public means everyone will be able to see the workshop map,
Private means only you will be able to and Friends is
something in between. You can change this setting later by
editing this file.
Workshop file: dom6ws_pfid
You should not create or edit this file. It will be created after
your first upload to the workshop and it is necessary to keep
this file to be able to edit the map later. If you delete this file a
new workshop map will be created instead.
10
Uploading to Workshop
Start Dominions 6 (from Steam) click 'Tools & Manuals', 'Map
Editor' and 'Upload map to Steam Workshop'. Now select
your .map file.
Important Note
Always name the map the same as the folder it is located in. So
Antworld.map should be inside a folder called Antworld. The
main image for that map should be called Antworld.tga.
Old Dominions 5 Maps
Dominions 5 maps are compatible with Dominions 6 with
certain reservations.
Basic terrain masks and other map commands are mostly the
same, but the border mountain number has changed so maps
that have those will need some modification (most easily done
using the map editor). Start locations also had their number
changed and have to be updated (can be done using the map
editor).
The main differences is that Dominions 5 maps do not contain
images for different terrains. This makes it impossible for
Dominions 6 to show the correct graphics for a province that
has changed its terrain or maybe sunk into the water.
Scripted Dominions 5 maps will probably not work in
Dominions 6 as numbers for nations have changed as well as
some monster names.