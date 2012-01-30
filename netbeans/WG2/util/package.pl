#!/usr/bin/perl
use File::Copy;
use File::Path;
use File::Copy::Recursive qw(fcopy rcopy dircopy fmove rmove dirmove);

# I should probably do this with ANT..gotta learn it better first though,
# probably should get the whole build working with a generic ant file so it
# can be built with various IDES or without one at all.
# This is quick and dirty creating a tar/gz deployable display timestamped
# file on my windows box, lol.

# The project root directory
$phome = "/cygdrive/c/Users/Dyolf/Documents/NetBeansProjects/hg/netbeans/WG2";

# The location of the dist built directory
$dist = "$phome/dist";

# The location the native libraries.  Just gonna snag them all for now
$native = "$phome/release";

# The location of the w2config default folder
$w2config = "$phome/w2config";

# The location of the util/run folder
$w2config = "$phome/util/run";

# Calculate directory based on current date/time
($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)= localtime(time);
$t = sprintf("%4d%02d%02d%02d%02d", $year+1900, $mon+1, $mday, $hour, $min);
$temp = "WG2-$t";

print "Packaging...$temp\n";
rmtree($temp);
mkdir($temp);
copy ("$dist/WG2.jar", "$temp/WG2.jar");
dircopy ("$dist/lib", "$temp/lib/");
rcopy ("$native", "$temp/release");
rcopy ("$w2config", "$temp/w2config");
$temptar = $temp.".tar.gz";
$tararg = "cvfz $temptar $temp";
print "Tar is $tararg\n";
`tar $tararg`;
print "Success\n";

