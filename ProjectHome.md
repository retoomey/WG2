# WG2 (The WDSSII Java GUI Two) #

## Info ##
WG2 is an experimental weather GUI designed and specialized for viewing and interacting with real-time data from meteorology and hydrology algorithm sources, while focusing on time navigation, product synchronization and real-time views of data.
It currently connects to our internal WDSSII format in the [National Severe Storms Laboratory, in Norman, OK](http://www.nssl.noaa.gov=).
We have an internal netcdf format we use and an internal database for a weather set that we call an 'index'. Data is grouped into [radar volume coverage patterns](http://www.srh.noaa.gov/jetstream/doppler/vcp_max.htm)

## Purpose and Relationship to AWIPS II ##
Some of the WG2 code will most likely be shared into the [AWIPS II operational system](http://www.unidata.ucar.edu/software/awips2/) in the next year.  AWIPS II is a very large operational weather system that can take a large effort to install.  Students and researchers can have very different requirements than operational forecasters.  In this case, WG2 is the 40 MB car, AWIPS II is the two GB 777.

Some of the goals of WG2 vs. AWIPS II are:
  * Be easy and quick to install and use (WG2 is just extracted and started with a script)
  * Allow us to view new experimental data at resolutions, speeds and formats not available in other software, or not with a meteorological emphasis.
  * Avoid the 100% stability requirement of a large operational system.
  * Avoid the red-tape, etc. of developing within a large operational system.
  * Allow sharable code between experimental and operational displays.

## Binaries ##
Since google turned off binary downloads, I'm just sharing a simple google drive link to a recent build folder.  Download and extract the dated zip, it will make a folder called "WG2-datestamp".  Inside this folder, for each OS there is a launcher helper file, "windows.bat", for windows, "linux.sh" for linux, and one for mac.  This comes with the JOGL native libraries and links on the fly, so it should run without any additional setup.  I usually run WG2 in windows or in RedHat linux 64.  It 'should' work on the Mac, or be very close to working on a mac.  I usually run with an Nvidia setup, so ATI or other cards could have graphic quirks do to bugs in WG2 code that don't affect Nvidia.
A good way to tell if the native OpenGL libraries aren't working is that you get a black window instead of an earth ball.

## Downloads here ##
[Binary Distributions](https://drive.google.com/folderview?id=0B1SGqckpQa9ndkFobmNXRFQ1Mnc&usp=sharing)

## Features ##
  * Unique time and volume navigation.
  * Real-time 2D readout table scanning and linkage to 3D.  It's a pretty neat way to look at data.
  * Real-time dynamic Vertical slicing and CAPPI of volumes of data sources.

## Libraries ##
  * Uses [Nasa Worldwind](http://worldwind.arc.nasa.gov/java/) java API for main earth views.
  * Uses [InfoNode](http://www.infonode.net) docking windows for multiple display support and layout.
  * Uses [GeoTools](http://geotools.org) for GIS features (ESRI shapefiles).
  * Multithreaded.

## Roadmap ##
  * Current working on usability, isolating dependencies and experimental 3D stuff.
  * Would like to directly read level 2 data.

## Some Pictures (fairly old though) ##

Showing two data moments sharing a 3d vertical slice and a shpfile renderering of Oklahoma.
![http://wg2.googlecode.com/files/wg2-02242012.png](http://wg2.googlecode.com/files/wg2-02242012.png)

Showing vertical slice of RadialSet volume in earth view as well as 2D Chart
![http://wg2.googlecode.com/files/wg2-06092011.png](http://wg2.googlecode.com/files/wg2-06092011.png)

Showing the 2D tracking table view of a RadialSet looking at May 24, 2011 data
![http://wg2.googlecode.com/files/wg2netbeans5.png](http://wg2.googlecode.com/files/wg2netbeans5.png)

Showing basic RadialSet 'cone' shaped data with a data generated linear grey color map
![http://wg2.googlecode.com/files/wg2netbeans4.png](http://wg2.googlecode.com/files/wg2netbeans4.png)