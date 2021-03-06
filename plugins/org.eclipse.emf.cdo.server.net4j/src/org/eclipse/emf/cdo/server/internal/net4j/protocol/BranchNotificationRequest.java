/*
 * Copyright (c) 2010-2013, 2015 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - bug 201266
 *    Simon McDuff - bug 233490
 */
package org.eclipse.emf.cdo.server.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchChangedEvent.ChangeKind;
import org.eclipse.emf.cdo.common.protocol.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class BranchNotificationRequest extends CDOServerRequest
{
  private CDOBranch branch;

  private ChangeKind changeKind;

  public BranchNotificationRequest(CDOServerProtocol serverProtocol, CDOBranch branch, ChangeKind changeKind)
  {
    super(serverProtocol, CDOProtocolConstants.SIGNAL_BRANCH_NOTIFICATION);
    this.branch = branch;
    this.changeKind = changeKind;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    out.writeCDOBranch(branch);
    out.writeEnum(changeKind);
    out.writeString(branch.getName());
  }
}
