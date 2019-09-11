[![](https://travis-ci.org/odinsbane/jfilament.svg?branch=master)](https://travis-ci.org/odinsbane/jfilament)

# jfilament
JFilament is an ImageJ plugin for segmentation and tracking of 2D and 3D filaments in fluorescenece microscopy images. The main algorithm used in Jfilament is "Stretching Open Active Contours".

This site has the source code of the project. For a compiled version and for user instructions go to http://athena.physics.lehigh.edu/jfilament.

This work is supported by NIH and by the Biosystems Dynamics Summer Institute at Lehigh University.

# release

Version 1.1.7
 - Clear current snake.
 - Create a mask image using snakes.
 - Look at function for 3D viewing.
 - BUG FIX: removing snake by deleting all of its points sometimes caused a crash.


Version 1.1.6

 - BUG FIX: A class file was omitted from mvn package.
 - Introduced a new 2D plugin for kimographs.

Version 1.1.5

 - Improved view rotation and controls.


Version 1.1.4

 - sculpting of snakes. By pressing 's' when a snake is selected, the snakes can be sculpted, points moved around.
 - contrast. The display range of the image can be changed.
 - indicator in the corner now indicates which snake is being modified, should correspond with snakes saved in file.
 - Building/Versioning update.

References:

M. B. Smith, H. Li, T. Shen, X. Huang, E. Yusuf, and D. Vavylonis, "Segmentation and Tracking of Cytoskeletal Filaments using Open Active Contours," Cytoskeleton 67:693-705 (2010)

H. Li, T. Shen, M. B. Smith, I. Fujiwara, D. Vavylonis, and X. Huang, "Automated Actin Filament Segmentation, Tracking, and Tip Elongation Measurements based on Open Active Contour Models," In Proc. of the IEEE International Symposium on Biomedical Imaging: From Nano to Macro, ISBI-09, Boston, 2009 
