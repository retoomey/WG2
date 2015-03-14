# Checking out project into Netbeans 7.0 #

## Windows ##
Ok this will get more filled in later, but here's the quick FYI on a windows machine:

  1. Install Netbeans 7.0 rc2 from http://www.netbeans.com/downloads/index.html  The regular Java Se is fine, you can install the monster big ones if you want to play with other features.
  1. Install windows mercurial from http://mercurial.selenic.com/
  1. Run Netbeans
  1. In menu item 'Tools' --> 'Options' go to the tab 'Miscellaneous' and go to 'Versioning'.  In the list on the left side there should be 'Mercurial'.  Go to it and make your Mercurial User Name your google email without the 'at' sign part.  In windows it should have put the TortoiseHg path in there for you already. Close out and go back to the main netbeans IDE window.  You don't need this step just to pull the code, but it will ask you for user/password when you push then.
  1. In menu item 'Team' --> 'Mercurial' go to 'Clone other...'.  If mercurial was installed correctly, a dialog will pop up asking for the Repository URL.  Put in 'https://wg2.googlecode.com/hg/'  Put in your google username without the 'at' sign.  Put in your password which is randomly generated on your google profile page under 'settings'.
  1. Hit next, the Default pull/push path should be correct, hit 'next' again..
  1. Pick the root directory for the checkout, clone name can stay 'hg'.  Leave the 'Scan for NetBeans Projects after clone' checked, hit 'finish'.
  1. Ok, netbeans doesn't always find the proper project on scanning.  If it finds 'netbeans' go ahead and open it.  If it says 'worldwind' or something else it's confused.  Cancel the dialog and then do 'File' --> 'Open Project..'.  You want the 'netbeans' folder within the 'hg' mercurial root directory.  Once you have opened this, in the netbeans project window you'll see it and 'modules'.  The code for this project is in the 'wdssii' project.  Double click and it will open it.
  1. You should be able to run it now.  If you can't try hiliting the root 'netbeans' project before running.