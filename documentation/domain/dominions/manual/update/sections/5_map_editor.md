5 The Map Editor
================

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
## 5.1 Interface
The map editor interface is very simple. There is box with a
list of province number, name, terrain types and other
province properties on the left and the rest of the screen is
taken up by the map itself. Provinces are marked by a silver
flag in their center. In a new map the provinces won't have any
names, but you can click on the province number to give it a
name. If a province is not named it will be given a suitable
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
## 5.2 Setting Connections
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
## 5.3 Setting Terrain Type
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
## 5.4 Painting Province Areas
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
If you haven't got a border image you will have to paint the
province areas using the editor. Right click on a province to
select it, then paint the area. Pressing 0 will select no
province, which can be used to paint areas that are not used
by any province at all.
