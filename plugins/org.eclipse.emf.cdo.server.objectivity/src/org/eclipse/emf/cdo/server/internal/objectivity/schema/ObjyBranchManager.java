/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Ibrahim Sallam - initial API and implementation
 */
package org.eclipse.emf.cdo.server.internal.objectivity.schema;

import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager.BranchLoader.BranchInfo;

import com.objy.db.app.ooId;
import com.objy.db.app.ooObj;
import com.objy.db.util.ooTreeSetX;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Ibrahim Sallam
 */
public class ObjyBranchManager extends ooObj
{
  protected int nextBranchId;

  protected ooTreeSetX branchSet;

  public ObjyBranchManager()
  {
    nextBranchId = 1; // 0 is the main branch.
  }

  protected void createTreeSet(ooObj clusterObject)
  {
    branchSet = new ooTreeSetX();
    clusterObject.cluster(branchSet);
  }

  public int getNextBranchId()
  {
    fetch();
    return nextBranchId;
  }

  public int createBranch(BranchInfo branchInfo)
  {
    markModified();
    int branchId = nextBranchId++;
    ObjyBranch newObjyBranch = new ObjyBranch(branchId, branchInfo);
    // if the baseBranchId is 0, then we just added to our branchSet, otherwise
    // we'll lookup the ObjyBranch with the id, and add the newly created
    // ObjyBranch to it's sub-branches set.
    int baseBranchId = branchInfo.getBaseBranchID();

    // Initially I thought we could make a tree of branches, but for the
    // first implementation we can just create a TreeSet of all branches,
    // then resolve sub-branches dynamically by checking baseBranchId.
    branchSet.add(newObjyBranch); // implicit clustering.

    // if (baseBranchId == 0) // main branch.
    // {
    // branchSet.add(newObjyBranch); // implicit clustering.
    // }
    // else
    // {
    // Iterator<ObjyBranch> treeItr = branchSet.iterator();
    // ObjyBranch objyBranch = null;
    // boolean found = false;
    // while (treeItr.hasNext() && !found)
    // {
    // objyBranch = treeItr.next();
    // if (baseBranchId == objyBranch.getBaseBranchId())
    // {
    // found = true;
    // continue;
    // }
    // objyBranch = null;
    // }
    //
    // objyBranch.addSubBranch(newObjyBranch);
    // }
    return branchId;
  }

  public ObjyBranch getBranch(int branchId)
  {
    fetch();
    Iterator<ObjyBranch> treeItr = branchSet.iterator();
    ObjyBranch objyBranch = null;
    while (treeItr.hasNext())
    {
      objyBranch = treeItr.next();
      if (branchId == objyBranch.getBranchId())
      {
        break;
      }
    }

    return objyBranch;
  }

  public static ObjyBranchManager create(ooId scopeContOid)
  {
    ObjyBranchManager branchManager = new ObjyBranchManager();
    ooObj clusterObject = ooObj.create_ooObj(scopeContOid);
    clusterObject.cluster(branchManager);
    branchManager.createTreeSet(branchManager);
    return branchManager;
  }

  public boolean deleteBranch(int branchId)
  {
    boolean done = false;

    markModified();
    Iterator<ObjyBranch> treeItr = branchSet.iterator();
    ObjyBranch objyBranch = null;
    while (treeItr.hasNext())
    {
      objyBranch = treeItr.next();
      if (branchId == objyBranch.getBranchId())
      {
        done = branchSet.remove(objyBranch);
        break;
      }
    }

    return done;
  }

  public List<ObjyBranch> getSubBranches(int branchId)
  {
    fetch();
    List<ObjyBranch> objyBranchList = new ArrayList<ObjyBranch>();

    Iterator<ObjyBranch> treeItr = branchSet.iterator();
    ObjyBranch objyBranch = null;
    while (treeItr.hasNext())
    {
      objyBranch = treeItr.next();
      if (branchId == objyBranch.getBaseBranchId())
      {
        objyBranchList.add(objyBranch);
      }
    }
    return objyBranchList;
  }

  public List<ObjyBranch> getBranches(int startId, int endId)
  {
    fetch();
    List<ObjyBranch> objyBranchList = new ArrayList<ObjyBranch>();

    int lastId = endId != 0 ? endId : Integer.MAX_VALUE;

    Iterator<ObjyBranch> treeItr = branchSet.iterator();
    ObjyBranch objyBranch = null;
    while (treeItr.hasNext())
    {
      objyBranch = treeItr.next();
      int id = objyBranch.getBranchId();
      if (id >= startId && id <= lastId)
      {
        objyBranchList.add(objyBranch);
      }
    }
    return objyBranchList;
  }

}
