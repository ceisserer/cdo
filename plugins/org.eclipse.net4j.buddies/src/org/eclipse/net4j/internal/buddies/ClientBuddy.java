/***************************************************************************
 * Copyright (c) 2004 - 2007 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.net4j.internal.buddies;

import org.eclipse.net4j.buddies.internal.protocol.Buddy;
import org.eclipse.net4j.buddies.protocol.IAccount;
import org.eclipse.net4j.buddies.protocol.IBuddy;
import org.eclipse.net4j.buddies.protocol.IMembership;

import java.util.Collection;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class ClientBuddy extends Buddy
{
  private String userID;

  private IAccount account;

  public ClientBuddy(ClientSession session, String userID)
  {
    super(session, null);
    this.userID = userID;
  }

  @Override
  public ClientSession getSession()
  {
    return (ClientSession)super.getSession();
  }

  public String getUserID()
  {
    return userID;
  }

  public IAccount getAccount()
  {
    if (account == null)
    {
      account = loadAccount(userID);
    }

    return account;
  }

  public IMembership[] initiate(Collection<IBuddy> buddies)
  {
    throw new UnsupportedOperationException();
  }

  public IMembership join(long collaborationID)
  {
    throw new UnsupportedOperationException();
  }

  public IMembership join(Object invitationToken)
  {
    throw new UnsupportedOperationException();
  }

  protected IAccount loadAccount(String userID)
  {
    // TODO Implement method ClientBuddy.loadAccount()
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  protected Set<String> loadFacilityTypes()
  {
    // TODO Implement method ClientBuddy.loadFacilityTypes()
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
