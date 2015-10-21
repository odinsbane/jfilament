package snakeprogram3d.display3d;

import javax.media.j3d.BranchGroup;


/**
 * Wrapper for a branch group.  This is a stripped down version
 * from another 3d type application for showing data.
 * 
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
  *
  **/
public interface DataObject{
        
        /**
          *     This is required to add to a group
          **/
        public BranchGroup getBranchGroup();


}