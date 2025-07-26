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

