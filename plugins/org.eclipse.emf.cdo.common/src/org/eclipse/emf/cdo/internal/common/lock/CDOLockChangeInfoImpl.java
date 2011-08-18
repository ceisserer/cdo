/**
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Caspar De Groot - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.common.lock;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.lock.CDOLockChangeInfo;
import org.eclipse.emf.cdo.common.lock.CDOLockOwner;
import org.eclipse.emf.cdo.common.lock.CDOLockState;

/**
 * @author Caspar De Groot
 */
public class CDOLockChangeInfoImpl implements CDOLockChangeInfo
{
  private final CDOBranchPoint branchPoint;

  private final CDOLockOwner lockOwner;

  private final CDOLockState[] lockStates;

  private final Operation operation;

  public CDOLockChangeInfoImpl(CDOBranchPoint branchPoint, CDOLockOwner lockOwner, CDOLockState[] lockStates,
      Operation operation)
  {
    this.branchPoint = branchPoint;
    this.lockOwner = lockOwner;
    this.lockStates = lockStates;
    this.operation = operation;
  }

  public CDOBranch getBranch()
  {
    return branchPoint.getBranch();
  }

  public long getTimeStamp()
  {
    return branchPoint.getTimeStamp();
  }

  public CDOLockOwner getLockOwner()
  {
    return lockOwner;
  }

  public CDOLockState[] getLockStates()
  {
    return lockStates;
  }

  public Operation getOperation()
  {
    return operation;
  }
}
