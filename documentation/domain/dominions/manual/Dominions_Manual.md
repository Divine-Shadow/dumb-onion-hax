    Dominions 5 Map Making Manual code{white-space: pre-wrap;} span.smallcaps{font-variant: small-caps;} span.underline{text-decoration: underline;} div.column{display: inline-block; vertical-align: top; width: 50%;} 

Dominions 5 Map Making Manual
=============================

Illwinter Game Design

*   [1 Introduction](#introduction)
*   [2 Requirements](#requirements)
*   [3 Data Directories](#data-directories)
*   [4 The Map Image File](#the-map-image-file)
    *   [4.1 Defining Provinces](#defining-provinces)
    *   [4.2 Province Borders](#province-borders)
    *   [4.3 Transparency](#transparency)
*   [5 The Map Editor](#the-map-editor)
    *   [5.1 Interface](#interface)
    *   [5.2 Setting Connections](#setting-connections)
    *   [5.3 Setting Terrain Type](#setting-terrain-type)
    *   [5.4 Painting Province Areas](#painting-province-areas)
*   [6 The Map File Commands](#the-map-file-commands)
    *   [6.1 Syntax](#syntax)
    *   [6.2 Required Map Commands](#required-map-commands)
    *   [6.3 Basic Map Commands](#basic-map-commands)
    *   [6.4 Terrain Type in the Map File](#terrain-type-in-the-map-file)
    *   [6.5 Advanced Map Commands](#advanced-map-commands)
    *   [6.6 Setting Start Locations](#setting-start-locations)
    *   [6.7 Province Commands](#province-commands)
    *   [6.8 Commander Commands](#commander-commands)
    *   [6.9 God Commands](#god-commands)
*   [7 Random Map Generator](#random-map-generator)
*   [8 Steam Workshop](#steam-workshop)
    *   [8.1 Workshop file: banner.png](#workshop-file-banner.png)
    *   [8.2 Workshop file: dom5ws.txt](#workshop-file-dom5ws.txt)
    *   [8.3 Workshop file: dom5ws\_pfid](#workshop-file-dom5ws_pfid)
    *   [8.4 Uploading to Workshop](#uploading-to-workshop)
    *   [8.5 Important Note](#important-note)
*   [9 Dominions 4 Maps](#dominions-4-maps)

1 Introduction
==============

This document is a guide to making maps for Dominions 5. It covers the basics of how to create a map and how to use the functionality built into Dominions to customize and develop them beyond the basics provided by the graphical interface of the map editor. This manual should give the reader all the tools necessary to create and modify maps, but it does not provide an extensive tutorial on how to create a map from scratch. Image processing guides are beyond the scope of this document. The chapter about the random map generator provides only the basics. The full potential of the RMG is beyond the scope of this manual.

2 Requirements
==============

With the help of a simple text editor and a paint program like GIMP or Photoshop, it is possible to create new maps for Dominions 5.

3 Data Directories
==================

The data directory for storing saved games, mods and maps is different depending on the operating system.

*   Linux: ~/.dominions5
*   Mac: ~/.dominions5
*   Windows: %APPDATA%5

The easiest way to locate the data directories in Dominions 5 is by opening Game Tools from the Main Menu and using the Open User Data Directory function. The operating system will open the user data directory in a separate file manager window.

4 The Map Image File
====================

The image file of the map should be in Targa (.tga), SGI (.rgb) or PNG (.png) format. The file should be at least 256x256 pixels large and saved in 24 or 32 bit color. PNG files must be non-interlaced and cannot use indexed colors. A suitable size for a map might be about 1600x1200 pixels. This section discusses issues and common problems related to or caused by something in the map image file or interactions between the map image file and map file.

4.1 Defining Provinces
----------------------

Provinces are defined by white pixels (RGB value 255, 255, 255). Each single white pixel is considered a separate province. If you need some white color on your map, use something like (253, 253, 253) for white. It will look white to the human eye, but will not cause extra provinces to appear. When you add provinces to a map image, remember to use a single 1x1 pixel brush. It is also a good idea to use an image manipulation program that supports layers, so you can put the province pixels in a separate layer and later merge them down to form the final map file. That way mistakes will be easier to correct.

4.2 Province Borders
--------------------

Province borders are not strictly necessary in the map image, but they are helpful for players in visualizing where to go and which province borders which.

Drawing borders on a map image is best done on a separate layer, which is then merged down to form the final image file. This allows you to also export an image with borders only that can be used by the map editor to automatically calculate the correct province shapes (ctrl-b when in province area mode).

4.3 Transparency
----------------

An image file saved with improper alpha channel settings can result in Dominions treating the image as being transparent and showing only the map background instead of the map graphic. This problem is fixed by removing the alpha channel before saving.

5 The Map Editor
================

Once you have drawn your map you have to create a .map file that contains certain information about the map. For example, some of the information in the map file is the title of the map and which provinces borders on which. The easiest way to create a working map file is to use the map editor in Dominions 5.

The Map Editor is located in the Game Tools menu. To edit an existing map, select the Load Map option and then the desired map. To create a new map, select New Map and enter the file name of the map image file. Once you have created the map in the editor and saved it, it can be used to play Dominions 5.

5.1 Interface
-------------

The map editor interface is very simple. There is box with a list of province number, name, terrain types and other province properties on the left and the rest of the screen is taken up by the map itself. Provinces are marked by a silver flag in their center. In a new map the provinces won’t have any names, but you can click on the province number to give it a name. If a province is not named it will be given a suitable random name when the game starts.

There are two modes in the map editor, the default is the neighbor mode where you modify connections, terrains and other province attributes. By pressing tab you will switch to the Province Area mode that is used to setup which areas of the map belongs to what province.

The province areas must be known for the game so that the dominion overlay will appear over the correct areas. In this mode you paint the area that is owned by the active province. In both modes you can right click on the map to change the active province.

5.2 Setting Connections
-----------------------

Provinces that have a connection between them are called neighboring provinces, or neighbors.

To select a province, right-click on it.

To set a neighbor, click on an adjacent province.

To remove a neighbor, Ctrl-click on an adjacent province.

To change a standard connection to a mountain pass, shift-click on the adjacent province once. Mountain passes are shown as red lines.

To change a standard connection to a river border, shift-click on the adjacent province twice. River crossings are shown as blue lines.

To change a standard connection to an impassable border, shift-click on the adjacent province thrice. Impassable borders are shown as grey lines. Impassable borders can only be utilized for spell targeting purposes.

Shift-click a neighbor to cycle through standard, mountain pass and river border, impassable connections without removing it.

Press _r_ to change a border to a road. Roads borders are shown as green lines.

5.3 Setting Terrain Type
------------------------

To set a terrain type for the selected province, check all the terrain types that apply from the command box.

As a general rule, a single province should only contain one adverse terrain type (forest, waste, highland, swamp, cave), or at the very most two. Adverse terrain slows down movement and too many provinces with mixed terrain make a map impassable and not very enjoyable to play on. Mountain only indicates that a province is close to a mountain and does not slow down movement.

You can freely mix small or large province markers, fresh water, nostart (red castle), start (green castle), throne and many sites with any terrain without affecting movement on the map. Many sites means a province with a higher chance of containing magic sites and should be used sparingly or where map thematics require it.

All of the values set by these options are added into the terrain mask of the province. (see section Terrain Type in the Map File for more details). Note that some rare special terrain types cannot be set from the map editor, these can be manually added to the map file later if need be.

The keyboard shortcuts for the map editor can be seen by pressing ‘?’.

Load the map you have drawn and click in all borders and terrains. After the map file has been saved you can edit it manually in a text editor if you want to add some extra map commands. Many map commands cannot be entered via the map editor.

Several advanced map commands will require knowing monster numbers, fort numbers, poptype numbers, magic site numbers or other identifying information. In addition to terrain types and victory conditions, the tables in this manual list nation numbers, fort numbers and poptype numbers. Names and numbers for magic sites and monsters are available through other fan-made documentation. You can also check monster numbers and item numbers in the game by selecting the monster or item and pressing ctrl+i.

5.4 Painting Province Areas
---------------------------

Press tab to switch between the Neighbor and Province Area modes. In the Province Area mode you set the area that each province should occupy. This must be set for the dominion overlay to look correct and for mouse clicks to select the province you actually click on.

The easiest way to get all areas correct is if you have a separate image with only borders. It can be thin colored borders on a black background or just a different solid colors for each province (the solid colors can be reused for non adjacent provinces). You can also use an image with thin borders and an transparent background, but the borders mustn’t be black if you do it this way, as all transparency will be replaced by black by the map editor.

Press ctrl-b to set all border areas from a border image. After that you should press ctrl-x a few times to expand all areas if there is gap between them. Then press ctrl-l to remove any stray pixels that might be left and then you are are finished.

If you haven’t got a border image you will have to paint the province areas using the editor. Right click on a province to select it, then paint the area. Pressing 0 will select no province, which can be used to paint areas that are not used by any province at all.

6 The Map File Commands
=======================

The .map file contains text commands and usually all of these have been created by the map editor. But it can be useful to edit some of these commands manually in a text editor for some fine tuning that cannot be done in the editor.

6.1 Syntax
----------

Two consecutive dashes – are used to denote comments. Anything after the dashes is ignored. This can be used to add explanations of what is being added to a map file and what is being intended with the commands.

All Dominions 5 map commands begin with a # sign. Map commands may or may not have arguments. An argument is a value after the command itself to denote something. Arguments are indicated by <object> after the command itself, e.g.

#setland <province nbr>

A map command that does not have an argument is used to assign a fixed effect. The effect of these commands are always the same.. Map commands that have arguments can have one or more of them and the arguments may be of several different types. Some commands require more than one type of argument to work.

The following types of arguments are used for map commands in Dominions 5:

*   integer: a whole number
*   percent: a percentage value (may be higher than 100 in some cases). Expressed as integer in mod syntax, interpreted as percentage.
*   “string”: text such as province names, map descriptions etc.
*   bitmask: a bitmask is a special type of integer number. Any integer can be expressed as a sum of the powers of 2. In a bitmask number each power of 2 that it contains means a different thing. A bitmask argument assigns ALL of these attributes to the object being modded.

Example: Setting the terrain type of a province

     #terrain \<prov nbr\> \<terrain mask\>

is a command where the first argument of the command selects the target province and the second argument bitmask operation that assigns the specified terrain. Setting a bitmask of 1601 (1+64+512+1024) would mean a small province (1) that is a wasteland (64), which cannot be a starting location (512) and which has a high probability of containing many magic sites (1024).

6.2 Required Map Commands
-------------------------

These map commands must exist in every map file or the map will not work.

**#dom2title <text>**  
The title of the map. This must be the first command for every map. The reason why this command is named #dom2title instead of #dom5title is because the map command syntax used here was first introduced in Dominions 2: The Ascension Wars and much of it has been kept the same since then.

**#imagefile <filename>**  
The image file of the map in TGA, RGB or PNG format. The file should be at least 256x256 pixels large and saved in 24 or 32 bit color formats. PNG files cannot be interlaced. A suitable size for a map might be about 1600x1200 pixels. For more detailed information related to the image file, see Chapter 5: The Map Image File.

**#mapsize <width> <height>**  
Lets Dominions 5 know the size of the map in pixels.

6.3 Basic Map Commands
----------------------

These map commands are some of the most basic commands of map making. These commands do not require an active province (see Province Commands) even if they affect a specific province.

**#domversion <version>**  
Set the minimum version of Dominions required to use this map. This number is usually 450 for Dominions 5 maps, meaning Dominions version 4.50 or higher is required.

**#winterimagefile <filename>**  
The image file for the winter version of the map. A winter map is not required, but you won’t get any winter look on cold provinces without it. A winter map must have exactly the same size as the normal map.

**#scenario**  
This command tags the map as a scenario and this will be indicated by a small burning star when selecting a map. It disables most game setup options, because those are supposed to be determined by map commands in the scenario map file.

**#description “text”**  
The description of the map that is shown after selecting a map to play on. Use two newlines to add a new paragraph. Alternatively, use ^ at the end of a line to indicate newline. If you use ^at the beginning of a line, it adds two newlines.

**#neighbour <province nbr> <province nbr>**  
Makes it possible to move between these two provinces (in both directions). Use the map editor to set province neighbors. Doing it from the map file with a text editor is VERY difficult.

**#neighbourspec <land1> <land2> <spcnbr>**  
This command can be used to create a mountain pass or other type of special border between two provinces. Spcnbr indicates a special border types from these values: 0 = standard border, 1 = mountain pass, 2 = river border, 4 = impassable, 8 = road. You really should use the map editor to enter this information.

**#pb <x> <y> <len> <province nbr>**  
Sets what pixels belong to which province. This information really cannot be entered without using the map editor.

**#landname <province nbr> “name”**  
Sets the name of a specific province.

6.4 Terrain Type in the Map File
--------------------------------

**#terrain <province nbr> <terrain mask>**  
Sets the terrain of a province. The terrain is calculated by adding certain numbers for different terrain types or other attributes.

Common Terrain Masks

2-pow

Number

Terrain

\-

0

Plains

0

1

Small Province

1

2

Large Province

2

4

Sea

3

8

Freshwater

4

16

Highlands (or gorge)

5

32

Swamp

6

64

Waste

7

128

Forest (or kelp forest)

8

256

Farm

9

512

Nostart

10

1024

Many Sites

11

2048

Deep Sea

12

4096

Cave

22

4194304

Mountains

24

16777216

Good throne location

25

33554432

Good start location

26

67108864

Bad throne location

29

536870912

Warmer

30

1073741824

Colder

Rare Terrain Masks

2-pow

Number

Terrain

13

8192

Fire sites

14

16384

Air sites

15

32768

Water sites

16

65536

Earth sites

17

131072

Astral sites

18

262144

Death sites

19

524288

Nature sites

20

1048576

Blood sites

21

2097152

Holy sites

You should use the map editor to set the terrain values as it would be very difficult to do it by hand.

Basic terrain masks are listed in tables Common Terrain Masks and Rare Terrain Masks. Note that the terrain masks used in editing maps are NOT the same as the terrain masks in the Modding Manual that are used for modding magic sites. All terrain masks listed in the Common Terrain Masks table can be set from the map editor.

The terrain masks in the Rare Terrain Masks table cannot be added from the map editor and you must add them to the base terrain mask calculated by the map editor. The advanced terrain masks make it more likely that when a magic site is placed in the province, it will be of that specific type.

6.5 Advanced Map Commands
-------------------------

These map commands are not necessary to get a working map, but they allow a great deal of customization and enhancement. These commands do not require an active province (see Province Commands) even if they affect a specific province. Many of these commands are global and affect all provinces on the map or map attributes that are not directly tied to a specific province. It is recommended that they be placed at the start of the map file after the description.

**#maptextcol <red> <green> <blue> <alpha>**  
Sets the color used to print province names. Each value should be between 0.0 and 1.0.

**#saildist <1-10>**  
Sets the maximum sail distance in sea provinces. A commander with the sailing ability will be able to pass this many sea provinces. It default to 2, but if seas are very large or strategically important it might be good to reduce this to 1.

**#features <0-100>**  
Sets the magic site frequency. This command will override the site frequency specified in the game setup screen.

**#nohomelandnames**  
When this switch is used, homelands will no longer be named after their starting nations. For example, the home of Abysia might be called The Summer Lands or whatever.

**#nonamefilter**  
Map filter that displays province names is disabled when this command is used. Does not work correctly.

**#allowedplayer <nation nbr>**  
Makes this nation one of the allowed nations to play on this map. Use this command multiple times or the map will only be able to host one player. Nation numbers can be found in the tables below. This command can be used to make era specific maps.

Early Era Nations

Number

Nation

Epithet

5

Arcoscephale

Golden Era

6

Ermor

New Faith

7

Ulm Enig

ma of Steel

8

Marverni

Time of Druids

9

Sauromatia

Amazon Queens

10

T’ien Ch’i

Spring and Autumn

11

Machaka

Lion Kings

12

Mictlan

Reign of Blood

13

Abysia

Children of Flame

14

Caelum

Eagle Kings

15

C’tis

Lizard Kings

16

Pangaea

Age of Revelry

17

Agartha

Pale Ones

18

Tir na n’Og

Land of the Ever Young

19

Fomoria

The Cursed Ones

20

Vanheim

Age of Vanir

21

Helheim

Dusk and Death

22

Niefelheim

Sons of Winter

25

Kailasa

Rise of the Ape Kings

26

Lanka

Land of Demons

27

Yomi

Oni Kings

28

Hinnom

Sons of the Fallen

29

Ur The

First City

30

Berytos

Phoenix Empire

31

Xibalba

Vigil of the Sun

36

Atlantis

Emergence of the Deep Ones

37

R’lyeh

Time of Aboleths

38

Pelagia

Pearl Kings

39

Oceania

Coming of the Capricorns

40

Therodos

Telkhine Spectre

Middle Era Nations

Number

Nation

Epithet

43

Arcoscephale

The Old Kingdom

44

Ermor

Ashen Empire

45

Sceleria

Reformed Empire

46

Pythium

Emerald Empire

47

Man Towe

r of Avalon

48

Eriu

Last of the Tuatha

49

Ulm Forg

es of Ulm

50

Marignon

Fiery Justice

51

Mictlan

Reign of the Lawgiver

52

T’ien Ch’i

Imperial Bureaucracy

53

Machaka

Reign of Sorcerors

54

Agartha

Golem Cult

55

Abysia

Blood and Fire

56

Caelum

Reign of the Seraphim

57

C’tis

Miasma

58

Pangaea

Age of Bronze

59

Asphodel

Carrion Woods

60

Vanheim

Arrival of Man

61

Jotunheim

Iron Woods

62

Vanarus

Land of the Chuds

63

Bandar Log

Land of the Apes

64

Shinuyama

Land of the Bakemono

65

Ashdod

Reign of the Anakim

66

Uruk

City States

67

Nazca

Kingdom of the Sun

68

Xibalba

Flooded Caves

73

Atlantis

Kings of the Deep

74

R’lyeh

Fallen Star

75

Pelagia

Triton Kings

76

Oceania

Mermidons

77

Ys Morg

en Queens

Late Era Nations

Number

Nation

Epithet

80

Arcoscephale

Sibylline Guidance

81

Pythium

Serpent Cult

82

Lemur

Soul Gate

83

Man

Towers of Chelms

84

Ulm

Black Forest

85

Marignon

Conquerors of the Sea

86

Mictlan

Blood and Rain

87

T’ien Ch’i

Barbarian Kings

89

Jomon

Human Daimyos

90

Agartha

Ktonian Dead

91

Abysia

Blood of Humans

92

Caelum

Return of the Raptors

93

C’tis

Desert Tombs

94

Pangaea

New Era

95

Midgård

Age of Men

96

Utgård

Well of Urd

97

Bogarus

Age of Heroes

98

Patala

Reign of the Nagas

99

Gath

Last of the Giants

100

Ragha

Dual Kingdom

101

Xibalba

Return of the Zotz

106

Atlantis

Frozen Sea

107

R’lyeh

Dreamlands

108

Erytheia

Kingdom of Two Worlds

Special Nations

Number

Nation

Note

0

Independents

2

Special Independents

e.g. Horrors

**#computerplayer <nation nbr> <difficulty>**  
This nation will always be controlled by the computer. Difficulty ranges from one to five. One is Easy AI. Two is Standard difficulty, followed by Difficult (3), Mighty (4) and Impossible (5) AI.

**#allies <nation nbr> <nation nbr>**  
These two players will not attack each other. This command will only affect computer players.

**#victorycondition <condition> <attribute>**  
The game will end when one player fulfills a special condition, see table Victory Conditions. Dominion score is 11-20 points per converted province, depending on the strength of the dominion. The value of ‘condition’ should be a number from 0 to 6.

Victory Conditions

Number

Condition

Attribute

0

Standard

Nothing

2

Dominion

Dominion score req.

3

Provinces

Provinces required

4

Research

Research points req.

6

Thrones

Nbr of Ascension Points

**#cannotwin <nation nbr>**  
This nation will not win when they fulfill a special victory condition.

**#victorypoints <land nbr> <1-7>**  
The player who has control over this province will control from one to seven victory points. If the province has a fort then the controller of the fort controls the victory points.

6.6 Setting Start Locations
---------------------------

These commands allow you to set or deny specific provinces as start locations and to control which nations starts where on a map. The specstart locations will be used if you create a game with the Use special starting locations option enabled.

**#start <province nbr>**  
Sets a recommended start location. By creating at least one start location for each player, every player will start at one of these locations. If start provinces are set, nations will start at these locations unless there are more nations than start provinces. If there are more nations than start provinces, the extra nations will start in eligible random locations. If a province is set as a start province but its terrain mask includes the value 512 (nostart), the nostart will override the start command and no nation will start there. If no start provinces are set, all provinces are available as random starting locations unless set nonstartable with the nostart command or in the map editor.

**#nostart <province nbr>**  
Tags a province as nonstartable. No player will start here when placed at random. This command can also be set from the map editor, which adds 512 to the province’s terrain mask.

**#specstart <nation nbr> <land nbr>**  
Use this command to assign a specific nation to a specific start location. Nation numbers can be found in the Early Era Nations table and the three following tables. If you use the #specstart command, please note that using the #land command to select the starting province of the nation for further modification results in the nation starting with no troops and a dead god. This is because the #land command kills all units initially placed in the province. In such situations the #setland command should be used instead.

**#teamstart <land nbr> <team nbr>**  
This command can be used in disciple games to force teams to start at certain positions. E.g. to make one team start on one side of the map and the other team on the other side. Team nbr is a value between 0 and number of teams - 1. This value doesn’t correspond to the team number used when creating a game, it’s random which team will get which teamstart position. Use the map editor and press ctrl 0-7 to set up the team positions in an easy way.

6.7 Province Commands
---------------------

These commands are used to manipulate specific provinces in order to set different features manually instead of being randomly assigned during game setup. Unless otherwise specified, they only affect the active province.

**#land <province nbr>**  
Sets the active province and kills everyone in it.  
All the following commands will only affect the active province. Use this command if you want to activate a province in order to replace its random inhabitants with the monsters of your choice.

**#setland <province nbr>**  
Sets the active province. All the following commands will only affect the active province.

**#poptype <poptype nbr>**  
Sets the population type of the active province. This determines which troops may be recruited in the province. Poptype numbers can be found in the large table on the following page. If poptype is set with a number higher than existing poptypes, there will be no units available for recruitment in the province. This command will override the poptype that was randomly assigned to the province during game creation, but it will NOT change the independent defenders, which will be of the poptype this command overwrote. Example: if the randomly determined poptype during game creation was 42 (Jade Amazons) and the poptype has been set to 25 (Barbarians) by this map command, the independent defenders will still be Jade Amazons. You just won’t be able to recruit them. If you want the independent defenders to match the specified poptype, you must set them manually in the map file using the Commander Commands . You should also use the #land command to select the province if you do not want the randomly assigned defenders in addition to the ones you set manually.

Poptypes

Number

Poptype

25

Barbarians

26

Horse Tribe

27

Militia, Archers, Hvy Inf

28

Militia, Archers, Hvy Inf

29

Militia, Archers, Hvy Inf

30

Militia, Longbow, Knight

31

Tritons

32

Lt Inf, Hvy Inf, X-Bow

33

Lt Inf, Hvy Inf, X-Bow

34

Raptors

35

Slingers

36

Lizards

37

Woodsmen

38

Hoburg

39

Militia, Archers, Lt Inf

40

Amazon, Crystal

41

Amazon, Garnet

42

Amazon, Jade

43

Amazon, Onyx

44

Troglodytes

45

Tritons, Shark Knights

46

Amber Clan Tritons

47

X-Bow, Hvy Cavalry

48

Militia, Lt Inf, Hvy Inf

49

Militia, Lt Inf, Hvy Inf

50

Militia, Lt Inf, Hvy Inf

51

Militia, Lt Cav, Hvy Cav

52

Militia, Lt Cav, Hvy Cav

53

Militia, Lt Cav, Hvy Cav

54

Hvy Inf, Hvy Cavalry

55

Hvy Inf, Hvy Cavalry

56

Hvy Inf, Hvy Cavalry

57

Shamblers

58

Lt Inf, Hvy Inf, X-Bow

59

Militia, Lt Inf, Archers

60

Militia, Lt Inf, Archers

61

Vaettir, Trolls

62

Tribals, Deer

63

Tritons

64

Tritons

65

Ichtyids

66

Vaettir

67

Vaettir, Dwarven Smith

68

Slingers, Hvy Inf, Elephants

69

Asmeg

70

Vaettir, Svartalf

71

Trolls

72

Mermen

73

Tritons, Triton Knights

74

Lt Inf, Lt Cav, Cataphracts

75

Hoburg, LA

76

Hoburg, EA

77

Atavi Apes

78

Tribals, Wolf

79

Tribals, Bear

80

Tribals, Lion

81

Pale Ones

82

Tribals, Jaguar

83

Tribals, Toad

84

Cavemen

85

Kappa

86

Bakemono

87

Bakemono

88

Ko-Oni

89

Fir Bolg

90

Turtle Tribe Tritons

91

Shark Tribe Tritons

92

Shark Tribe, Shark Riders

93

Zotz

94

Lava-born

95

Ichtyid Shaman

96

Bone Tribe

97

Merrow

98

Kulullu

**#owner <nation nbr>**  
Changes the ownership of the active province. Nation nbr indicates the new owner. Nation numbers can be found in the Early Era Nations table and the three following tables.

**#killfeatures**  
Removes all magic sites from the active province.

**#feature “<site name>” | <site nbr>**  
Puts a specific magic site in the active province. This command can be used a maximum of eight times per province, because that is the maximum number of sites a province can have. Adding unique sites to a map using this command will NOT prevent those sites from appearing randomly, because the map file is only applied to the game map after game setup has done random determination of sites for each province. If the #killfeatures command was not used and all site slots were already filled by randomly determined sites during game setup, this command will be ignored and the site won’t appear. These same limitations apply to the following command:

**#knownfeature “<site name>” | <site nbr>**  
Puts a specific magic site in the active province. This site is already found at the start of the game, regardless of its ordinary path level. Using this command prevents special features of the site that depend on its discovery from activating. For example, the magic site Academy of High Magics normally causes a laboratory to be built in the province upon discovery, but if the site is set by this command, the #lab command must be used to add a laboratory to the province. Otherwise the owner of the province must build the laboratory as normal and pay the gold cost.

**#fort <fort nbr>**  
Puts a specific fort in the active province. Fort nbr is a number between 1 and 29 and the list of fort numbers can be found in the Fortifications table. Will replace a nation’s default fort if used on a capital location.

Fortifications

Number

Fort

1

Palisades

2

Fortress

3

Castle

4

Citadel

5

Rock Walls

6

Fortress

7

Castle

8

Castle of Bronze and Crystal

9

Kelp Fort

10

Bramble Fort

11

City Palisades

12

Walled City

13

Fortified City

14

Great Walled City

15

Giant Palisades

16

Giant Fortress

17

Giant Castle

18

Giant Citadel

19

Grand Citadel

20

Ice Walls

21

Ice Fortress

22

Ice Castle

23

Ice Citadel

24

Wizard’s Tower

25

Citadel of Power

27

Fortified village

28

Wooden Fort

29

Crystal Citadel

**#temple**  
Puts a temple in the active province.

**#lab**  
Puts a laboratory in the active province.

**#unrest <0-500>**  
Sets the unrest level of the active province.

**#population <0-50000>**  
Sets the population number of the active province.

**#defence <0-125>**  
Sets the province defence of the active nation. This command cannot be used for independent provinces.

**#skybox “<pic.tga>”**  
Sets the sky (battleground background) to a tga/rgb pic of your choice for fights in the current province. The picture size should be a power of two. 512\*512 is a good size.

**#batmap “<battlemap.d3m>”**  
Sets the battleground that fights take place in for the current province. You can use the special name ’ empty ’ for no battleground, useful for battles in space perhaps. This will affect fights both outside and inside castles.

**#groundcol <red> <green> <blue>**  
Color the world with the specified colors for fights in the current province. Color values range from 0 to 255.

**#rockcol <red> <green> <blue>**  
Color the world with the specified colors for fights in the current province. Color values range from 0 to 255.

**#fogcol <red> <green> <blue>**  
Color the world with the specified colors for fights in the current province. Color values range from 0 to 255.

6.8 Commander Commands
----------------------

These commands are used to set specific monsters in the active province and manipulate those monsters to modify them from the base monster type to create thematic provinces and special heroes. They must be used after the #land or #setland commands, because they require an active province. Whenever commanders and units are placed on a map, the type can be set using either the monster number or the monster name in quote marks. If the commander or unit to be added is a new monster defined in a mod, then monster number cannot be used and the name must be used instead.

**#commander “<type>”**  
Puts one of these commanders in the active province. The commander will have a random name according to its nametype. This commander will be the active commander.

**#comname “<name>”**  
Replaces the active commander’s random name with this one.

**#bodyguards <nbr> “<type>”**  
Gives bodyguards to the active commander. This command only affects independents. AI nations will ignore this command.

**#units <nbr of units> “<type>”**  
Gives a squad of soldiers to the active commander.

**#xp <0-900>**  
Gives experience points to the active commander.

**#randomequip <rich>**  
Gives random magic items to the active commander. Rich must be between 0 and 4. A value of 0 means small chance of getting a magic item and 4 means large chance of getting many powerful items.

**#additem “<item name>”**  
Gives a magic item to active commander. Items cannot currently be assigned by item number.

**#clearmagic**  
Removes all magic skills from the active commander.

**#mag\_fire <level>**  
Gives active commander Fire magic.

**#mag\_air <level>**  
Gives active commander Air magic.

**#mag\_water <level>**  
Gives active commander Water magic.

**#mag\_earth <level>**  
Gives active commander Earth magic.

**#mag\_astral <level>**  
Gives active commander Astral magic.

**#mag\_death <level>**  
Gives active commander Death magic.

**#mag\_nature <level>**  
Gives active commander Nature magic.

**#mag\_blood <level>**  
Gives active commander Blood magic.

**#mag\_priest <level>**  
Gives active commander Holy magic.

6.9 God Commands
----------------

These commands are used to set a specific pretender god for a specific nation and will override the pretenders designed or loaded during game setup. Each of these commands may be used independently of the others. They do not require an active province. If human controlled nations are assigned gods or dominion scales, cheat detection will be triggered if the player do not conform to the normal design point limits for pretenders.

**#god <nation nbr> “<type>”**  
Forces the god of one nation to be this monster. The god becomes the active commander and can be manipulated with the commander commands.

The same limitations on defining the commander type apply, meaning that modded monsters must be defined by their name instead of monster number. Using this command will generate an error message and exit on game creation if the nation is not in play on the map.

**#dominionstr <nation nbr> <1-10>**  
Sets the dominion strength of a nation to a value between 1 and 10.

**#scale chaos <nation nbr> <(-3)-3>**  
Forces the Order / Turmoil dominion scale of a nation to a value between -3 and 3. A value of 3 means that the scale is fully tipped to the right (Turmoil) and -3 means it is fully tipped to the left (Order).

**#scale lazy <nation nbr> <(-3)-3>**  
Forces the Productivity / Sloth dominion scale of a nation to a value between -3 and 3. A value of 3 means that the scale is fully tipped to the right (Sloth) and -3 means it is fully tipped to the left (Productivity).

**#scale cold <nation nbr> <(-3)-3>**  
Forces the Heat / Cold dominion scale of a nation to a value between -3 and 3. A value of 3 means that the scale is fully tipped to the right (Cold) and -3 means it is fully tipped to the left (Heat).

**#scale death <nation nbr> <(-3)-3>**  
Forces the Growth / Death dominion scale of a nation to a value between -3 and 3. A value of 3 means that the scale is fully tipped to the right (Death) and -3 means it is fully tipped to the left (Growth).

**#scale unluck <nation nbr> <(-3)-3>**  
Forces the Luck / Misfortune dominion scale of a nation to a value between -3 and 3. A value of 3 means that the scale is fully tipped to the right (Misfortune) and -3 means it is fully tipped to the left (Luck).

**#scale unmagic <nation nbr> <(-3)-3>**  
Forces the Magic / Drain dominion scale of a nation to a value between -3 and 3. A value of 3 means that the scale is fully tipped to the right (Drain) and -3 means it is fully tipped to the left (Magic).

7 Random Map Generator
======================

Dominions 5 comes with a powerful random map generator (RMG for short). The random map generator can be run from the Game Tools menu and the preferences set as to size of map, number of provinces, ratio of each terrain type and various wrap options (east-west, north-south or full wraparound).

The RMG creates beautifully rendered maps that are ready to play right out of the box. However, it is worth using the map editor to check the randomly generated map for province connections that can be improved, because the random generation procedure does not always see things like a human player would.

Using command line switches, it is possible to specify some more options for the RMG than from the Game Tools menu. The RMG can be used to create maps with borders set to zero pixels in width. Some quick work with a paint program makes it possible to create a completely custom map with hand selected province locations and hand drawn borders without needing to do all the tedious map drawing completely from scratch if that is your preference.

Further, there is a command line switch called _–mapnospr_ which creates a completely blank map template without any graphics drawn on it. Only the landmass and seas are drawn and provinces are assigned as normal. Such a blank canvas can then be painted with the map graphics of the cartographer’s choice followed by province placement and drawing borders for a unique map.

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

9 Dominions 4 Maps
==================

Dominions 4 maps are compatible with Dominions 5 with certain reservations. Basic terrain masks and other map commands are the same, so those do not present a problem and unscripted Dominions 4 maps can be used without modification.

The main differences is that Dominions 4 maps do not contain information about the areas covered by each province. This information should be added by using the map editor. Maps can be used without this information, but the dominion overlay will often look bad.

Another major difference is that Dominions 4 maps do not have a winter variant. You can play without a winter map, but it will be difficult to know where movement is hindered and where rivers are frozen.

Scripted Dominions 4 maps will probably not work in Dominions 5 as numbers for nations, magic sites and forts have changed.