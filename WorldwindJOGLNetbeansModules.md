# Netbeans 7.0 rc2 #
Note, if you do a pull, you already have these modules setup by me in the project so you don't need to do this.  This is just for anyone fighting with their own Netbeans Module Application project or if this project gets messed up or something and things need to be set up again.
You could pull this project and run it to see it work and gut it for your own work.

# Creating Worldwind wrapper module #
  1. Download the newest zip of WorldWind java at:http://worldwind.arc.nasa.gov/java/
  1. Unzip it somewhere.
  1. In netbeans, "File"-->"New Project" Pick Library Wrapper Module.
  1. Select library: browse to where you put Worldwind.jar
  1. Name and Location: Project Name: worldwind, Project Location: Make it something like "hg\netbeans\worldwind" and Add to module suite: "hg\netbeans"  "netbeans' is just the top name I picked for my Netbeans Module Suite folder.

# Creating Jogl wrapper module #
  1. Download the jogl versions for all os you want to support currently from http://download.java.net/media/jogl/builds/archive/
  1. Do the same as worldwind above, but when adding jar files, select both the gluegen-rt.jar and the jogl.jar files.  Don't add worldwind.jar to this one, just the two jogl ones.
  1. Now the real trick is the native libraries.  Inside the module there is a 'release/modules/ext' folder that contains the two jars you picked in the wizard.  You want to make a 'release/modules/lib/arch/os folder for each architecture and os you want to support.  The arch and os names can be gotten in runtime from System.getProperty("os.arch"); and System.getProperty("os.name");  It goes by arch first, so for windows 64 I've just got this: 'release/modules/lib/amd64/gluegen-rt.dll', '...jogl.dll', 'jogl\_awt.gll' and 'jogl\_cg.dll'  If I had different dll files for two 64 bit windows os, I'd have subfolders under amd64.

# Linking module dependencies #
  1. Ok, you have to tell the worldwind module to use jogl:  Right click the module in the Projects window and do 'Properties'  Pick the 'Libraries' tab and on the right choose 'Module dependencies'.  Click the 'Add dependency...' button.  Add 'jogl'
  1. Ok, you have to tell our code 'wdssii' module (where all the code for this project is) to use 'worldwind' and 'jogl' (same as you just did for worldwind module)  Or you would tell any module that you plan on making worldwind/jogl api calls from.
  1. For our Suite 'netbeans', if you do properties you should see 'jogl' and 'worldwind' under 'Sources', 'suite modules' if you did it right.

# To Do #
  1. Worldwind uses gdal, I haven't messed with wrapping it yet since worldwind will just warn about it.
  1. I've only put the Jogl libraries for windows 64 in there, I need to create subfolders and test on different architectures.