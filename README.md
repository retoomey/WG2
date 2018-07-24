# WG2

WDSSII/MRMS Research GUI in [Java](https://www.oracle.com/java/)

WG2 is an experimental weather GUI designed and specialized for viewing and interacting with real-time data from
meteorology and hydrology algorithm sources, while focusing on time navigation, product synchronization and real-time
dynamic views of data.  It currently connects to our internal WDSSII formats at the [National Severe Storms Laboratory (NSSL)](http://www.nssl.noaa.gov)., in Norman, OK.

## History

Originally myself and several other coders at NSSL wrote a system called CODE or WDSSII in C++.  The purpose of this was to create a C++ algorithm framework for developing real-time weather processing algorithms using "Big Data". At the time, due to the massive real-time requirement for processing weather data, we developed many special data formats and tricks to squeeze as much processing speed out of the systems, as well as to reduce latency.  This lead to a basic need to quickly look at the results of new experimental weather algorithms.  So we made a weather display in C++ called WG, or the WDSSII GUI.  Being crazy scientists we kinda do what we want, and over time I pretty much became the OpenGL expert, squeezing out more frames per second and trying to look at data in new interesting ways.  I coded vertical slicing and isosurfaces and tracking tables and lots of fun stuff with OpenGL shaders and custom GUI design.

## NSSL Netcdf Data Format

We have internal netcdf formats we use and an internal database for a weather set that we call an 'index'. Data is grouped into radar coverage patterns by radar volume.  So basically if you want to play with this you need data from NSSL.
I can probably eventually stick up an archive case or two here.

## Current Status/Features

* Decided to make worldwind an optional data display.  This is just because for multi product synchronized views (C++ version), etc. the 'google earth' style tiling abilities are just too flaky and slow.  Also, offline ability is nice for our purposes.
* July/August 2018 -- Revisiting this with the purpose of synchronizing functionality to the C++ WDSS2 display we use at NSSL. I'm going to an alpha state and resigning the 3D projection world to match the original C++.  The goal is
to use and work with configuration files from original display to allow easy user migration.
* July 2018 -- So far the 3D camera math is ported and basic products redisplay.  Maps are showing up again.  I will need to rewrite the tracking projection code for point dragging to enable the vertical slicing and table tracking abilities.  This is because I used a lot of worldwind to do that originally and a new GL view will require its own math.

## Requirements

* Java
* OpenGL drivers/setup (You most likely have this)
* Linux (In theory should work windows/mac, however there are quirks with native or file access, etc. between operating system Java virtual machines and I haven't tested).

## Images

* First image of custom projection using camera math from C++ WG.
![WG2 example image](images/WG2-001.png?raw=true "WG2 example")
