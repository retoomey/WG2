# WG2
WDSS2/MRMS Research GUI in Java

WG2 is an experimental weather GUI designed and specialized for viewing and interacting with real-time data from
meteorology and hydrology algorithm sources, while focusing on time navigation, product synchronization and real-time
dynamic views of data.  It currently connects to our internal WDSSII formats at the National Severe Storms Laboratory, in Norman, OK.
We have internal netcdf formats we use and an internal database for a weather set that we call an 'index'. Data is grouped into
radar coverage patterns by radar volume.

## Current Status/Features

* Revisiting this with the purpose of synchronizing functionality to the C++ WDSS2 display we use at NSSL.
So as of July 2018 I'm going to an alpha state and resigning the 3D projection world to match the original C++.  The goal is
to use and work with configuration files from original display to allow easy user migration.
* So far the 3D camera math is ported and basic products redisplay.  Maps are showing up again.  I will need to rewrite the
tracking projection code for point dragging to enable the vertical slicing and table tracking abilities.  This is because
I used a lot of worldwind to do that originally and a new GL view will require its own math.

## Requirements

* Java
