/*
 * Copyright (c) 2010-2013, 2015, 2016, 2019 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests;

import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchChangedEvent;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.branch.CDOBranchPointRange;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionHandler;
import org.eclipse.emf.cdo.common.revision.CDORevisionManager;
import org.eclipse.emf.cdo.common.revision.CDORevisionUtil;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.internal.common.revision.AbstractCDORevisionCache;
import org.eclipse.emf.cdo.internal.server.mem.MEMStore;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.spi.common.branch.CDOBranchUtil;
import org.eclipse.emf.cdo.tests.config.IRepositoryConfig;
import org.eclipse.emf.cdo.tests.config.impl.ConfigTest.Requires;
import org.eclipse.emf.cdo.tests.model1.Company;
import org.eclipse.emf.cdo.tests.model1.OrderDetail;
import org.eclipse.emf.cdo.tests.model1.Product1;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.DanglingReferenceException;
import org.eclipse.emf.cdo.view.CDOView;

import org.eclipse.net4j.util.ReflectUtil;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;

import org.eclipse.emf.spi.cdo.CDOSessionProtocol;
import org.eclipse.emf.spi.cdo.InternalCDOSession;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 */
@Requires(IRepositoryConfig.CAPABILITY_BRANCHING)
public class BranchingTest extends AbstractCDOTest
{
  protected CDOSession session1;

  @Override
  protected void doSetUp() throws Exception
  {
    super.doSetUp();

    Field disableGC = ReflectUtil.getField(AbstractCDORevisionCache.class, "disableGC");
    ReflectUtil.setValue(disableGC, null, true);
  }

  @Override
  protected void doTearDown() throws Exception
  {
    session1 = null;
    Field disableGC = ReflectUtil.getField(AbstractCDORevisionCache.class, "disableGC");
    ReflectUtil.setValue(disableGC, null, false);
    super.doTearDown();
  }

  protected CDOSession openSession1()
  {
    session1 = openSession();
    return session1;
  }

  protected void closeSession1()
  {
    session1.close();
    session1 = null;
  }

  protected CDOSession openSession2()
  {
    return openSession();
  }

  protected String getBranchName(String name)
  {
    // New scenarios get clean repositories, no need to disambiguate them.
    return getClass().getSimpleName() + "_" + getName() + "_" + name;
  }

  public void testMainBranch() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    assertEquals(CDOBranch.MAIN_BRANCH_ID, mainBranch.getID());
    assertEquals(CDOBranch.MAIN_BRANCH_NAME, mainBranch.getName());
    assertEquals(null, mainBranch.getBase().getBranch());
    int branches = mainBranch.getBranches().length;
    closeSession1();

    session = openSession2();
    mainBranch = session.getBranchManager().getBranch(CDOBranch.MAIN_BRANCH_ID);
    assertEquals(CDOBranch.MAIN_BRANCH_ID, mainBranch.getID());
    assertEquals(CDOBranch.MAIN_BRANCH_NAME, mainBranch.getName());
    assertEquals(null, mainBranch.getBase().getBranch());
    assertEquals(branches, mainBranch.getBranches().length);
    session.close();
  }

  public void testCreateBranch() throws Exception
  {
    String name = getBranchName("testing");

    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    CDOBranch branch = mainBranch.createBranch(name);
    assertNotSame(CDOBranch.MAIN_BRANCH_ID, branch.getID());
    assertEquals(name, branch.getName());
    assertEquals(CDOBranch.MAIN_BRANCH_ID, branch.getBase().getBranch().getID());
    assertEquals(0, branch.getBranches().length);
    session.close();
  }

  public void testRenameBranch() throws Exception
  {
    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    CDOBranch branch = mainBranch.createBranch("testing");
    branch.setName("renamed");
    closeSession1();

    session = openSession2();
    CDOBranch renamedBranch = session.getBranchManager().getBranch("MAIN/renamed");
    assertNotNull(renamedBranch);

    CDOBranch testingBranch = session.getBranchManager().getBranch("MAIN/testing");
    assertNull(testingBranch);

    try
    {
      session.getBranchManager().getMainBranch().setName("test");
      fail("Main branch can't be renamed");
    }
    catch (Exception expected)
    {
      // SUCCESS
    }

    String name = session.getBranchManager().getMainBranch().getName();
    assertEquals("Main branch can't be renamed", CDOBranch.MAIN_BRANCH_NAME, name);
  }

  public void testGetBranch() throws Exception
  {
    String name = getBranchName("testing");

    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    CDOBranch branch = mainBranch.createBranch(name);
    int id = branch.getID();
    closeSession1();

    session = openSession2();
    branch = session.getBranchManager().getBranch(id);
    assertEquals(id, branch.getID());
    assertEquals(name, branch.getName());
    assertEquals(CDOBranch.MAIN_BRANCH_ID, branch.getBase().getBranch().getID());
    assertEquals(0, branch.getBranches().length);
    session.close();
  }

  public void testGetSubBranches() throws Exception
  {
    String name1 = getBranchName("testing1");
    String name2 = getBranchName("testing2");

    CDOSession session = openSession1();
    CDOBranch mainBranch = session.getBranchManager().getMainBranch();
    int count = mainBranch.getBranches().length;

    mainBranch.createBranch(name1);
    mainBranch.createBranch(name2);
    closeSession1();

    session = openSession2();
    mainBranch = session.getBranchManager().getMainBranch();
    CDOBranch[] branches = mainBranch.getBranches();
    assertEquals(count + 2, branches.length);
    assertEquals(name1, branches[count + 0].getName());
    assertEquals(CDOBranch.MAIN_BRANCH_ID, branches[count + 0].getBase().getBranch().getID());
    assertEquals(name2, branches[count + 1].getName());
    assertEquals(CDOBranch.MAIN_BRANCH_ID, branches[count + 1].getBase().getBranch().getID());
    session.close();
  }

  public void testEvent() throws Exception
  {
    CDOSession session1 = openSession1();
    CDOSession session2 = openSession2();

    final AsyncResult<CDOBranch> result = new AsyncResult<>();
    session2.getBranchManager().addListener(new IListener()
    {
      @Override
      public void notifyEvent(IEvent event)
      {
        if (event instanceof CDOBranchChangedEvent)
        {
          CDOBranchChangedEvent e = (CDOBranchChangedEvent)event;
          result.setValue(e.getBranch());
        }
      }
    });

    CDOBranch mainBranch = session1.getBranchManager().getMainBranch();
    CDOBranch branch = mainBranch.createBranch(getBranchName("testing"));
    CDOBranch resultBranch = result.getValue();
    assertEquals(branch, resultBranch);

    closeSession1();
    session2.close();
  }

  public void testGetPath() throws Exception
  {
    String name1 = getBranchName("testing1");
    String name2 = getBranchName("testing2");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOBranch testing1 = mainBranch.createBranch(name1);
    CDOBranch testing2 = mainBranch.createBranch(name2);
    CDOBranch subsub = testing1.createBranch("subsub");
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    assertEquals(mainBranch, branchManager.getBranch("MAIN"));
    assertEquals(testing1, branchManager.getBranch("MAIN/" + name1));
    assertEquals(testing2, branchManager.getBranch("MAIN/" + name2));
    assertEquals(subsub, branchManager.getBranch("MAIN/" + name1 + "/subsub"));
    assertEquals(testing1, mainBranch.getBranch(name1));
    assertEquals(testing2, mainBranch.getBranch(name2));
    assertEquals(subsub, mainBranch.getBranch(name1 + "/subsub"));
    assertEquals(subsub, testing1.getBranch("subsub"));
    session.close();
  }

  public void testGetPathName() throws Exception
  {
    String name1 = getBranchName("testing1");
    String name2 = getBranchName("testing2");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOBranch testing1 = mainBranch.createBranch(name1);
    mainBranch.createBranch(name2);
    testing1.createBranch("subsub");
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    assertEquals("MAIN", branchManager.getBranch("MAIN").getPathName());
    assertEquals("MAIN/" + name1, branchManager.getBranch("MAIN/" + name1).getPathName());
    assertEquals("MAIN/" + name2, branchManager.getBranch("MAIN/" + name2).getPathName());
    assertEquals("MAIN/" + name1 + "/subsub", branchManager.getBranch("MAIN/" + name1 + "/subsub").getPathName());
    assertEquals("MAIN/" + name1, mainBranch.getBranch(name1).getPathName());
    assertEquals("MAIN/" + name2, mainBranch.getBranch(name2).getPathName());
    assertEquals("MAIN/" + name1 + "/subsub", mainBranch.getBranch(name1 + "/subsub").getPathName());
    assertEquals("MAIN/" + name1 + "/subsub", testing1.getBranch("subsub").getPathName());
    session.close();
  }

  public void testBasePath() throws Exception
  {
    String name1 = getBranchName("testing1");
    String name2 = getBranchName("testing2");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOBranch testing1 = mainBranch.createBranch(name1);
    CDOBranch testing2 = mainBranch.createBranch(name2);
    CDOBranch subsub = testing1.createBranch("subsub");
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    assertEquals(mainBranch.getBasePath(), new CDOBranchPoint[] { mainBranch.getBase() });
    assertEquals(testing1.getBasePath(), new CDOBranchPoint[] { mainBranch.getBase(), testing1.getBase() });
    assertEquals(testing2.getBasePath(), new CDOBranchPoint[] { mainBranch.getBase(), testing2.getBase() });
    assertEquals(subsub.getBasePath(), new CDOBranchPoint[] { mainBranch.getBase(), testing1.getBase(), subsub.getBase() });
    session.close();
  }

  public void testAncestor() throws Exception
  {
    String name1 = getBranchName("testing1");
    String name2 = getBranchName("testing2");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();
    CDOBranch mainBranch = branchManager.getMainBranch();

    CDOBranch testing1 = mainBranch.createBranch(name1);
    CDOBranch subsub1 = testing1.createBranch("subsub1");

    CDOBranch testing2 = mainBranch.createBranch(name2);
    CDOBranch subsub2 = testing2.createBranch("subsub2");

    closeSession1();
    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();

    assertAncestor(mainBranch.getBase(), mainBranch.getBase(), mainBranch.getHead());
    assertAncestor(mainBranch.getBase(), mainBranch.getBase(), testing1.getHead());
    assertAncestor(mainBranch.getBase(), mainBranch.getBase(), subsub1.getHead());
    assertAncestor(mainBranch.getBase(), mainBranch.getBase(), testing2.getHead());
    assertAncestor(mainBranch.getBase(), mainBranch.getBase(), subsub2.getHead());

    assertAncestor(testing1.getBase(), testing1.getBase(), testing1.getHead());
    assertAncestor(subsub1.getBase(), subsub1.getBase(), subsub1.getHead());
    assertAncestor(testing2.getBase(), testing2.getBase(), testing2.getHead());
    assertAncestor(subsub2.getBase(), subsub2.getBase(), subsub2.getHead());

    assertAncestor(testing1.getBase(), subsub1.getHead(), subsub2.getHead());
    assertAncestor(subsub2.getBase(), testing2.getHead(), subsub2.getHead());
    assertAncestor(subsub1.getBase(), testing1.getHead(), subsub1.getHead());

    session.close();
  }

  private void assertAncestor(CDOBranchPoint expected, CDOBranchPoint point1, CDOBranchPoint point2)
  {
    CDOBranchPoint ancestor1 = CDOBranchUtil.getAncestor(point1, point2);
    assertEquals(expected, ancestor1);

    CDOBranchPoint ancestor2 = CDOBranchUtil.getAncestor(point2, point1);
    assertEquals(expected, ancestor2);
  }

  public void testContainment() throws Exception
  {
    String name1 = getBranchName("testing1");
    String name2 = getBranchName("testing2");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();
    CDOBranch mainBranch = branchManager.getMainBranch();

    CDOBranch testing1 = mainBranch.createBranch(name1);
    CDOBranch subsub1 = testing1.createBranch("subsub1");

    CDOBranch testing2 = mainBranch.createBranch(name2);
    CDOBranch subsub2 = testing2.createBranch("subsub2");

    closeSession1();
    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();

    assertEquals(true, CDOBranchUtil.isContainedBy(mainBranch.getBase(), mainBranch.getHead()));
    assertEquals(true, CDOBranchUtil.isContainedBy(mainBranch.getBase(), testing1.getHead()));
    assertEquals(true, CDOBranchUtil.isContainedBy(mainBranch.getBase(), subsub1.getHead()));
    assertEquals(true, CDOBranchUtil.isContainedBy(mainBranch.getBase(), testing2.getHead()));
    assertEquals(true, CDOBranchUtil.isContainedBy(mainBranch.getBase(), subsub2.getHead()));

    assertEquals(true, CDOBranchUtil.isContainedBy(testing1.getBase(), testing1.getHead()));
    assertEquals(true, CDOBranchUtil.isContainedBy(subsub1.getBase(), subsub1.getHead()));
    assertEquals(true, CDOBranchUtil.isContainedBy(testing2.getBase(), testing2.getHead()));
    assertEquals(true, CDOBranchUtil.isContainedBy(subsub2.getBase(), subsub2.getHead()));

    session.close();
  }

  public void testCommit() throws Exception
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    OrderDetail orderDetail = getModel1Factory().createOrderDetail();
    orderDetail.setProduct(product);
    orderDetail.setPrice(5);

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);
    resource.getContents().add(orderDetail);

    CDOCommitInfo commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource(getResourcePath("/res"));
    orderDetail = (OrderDetail)resource.getContents().get(1);
    assertEquals(5.0f, orderDetail.getPrice());
    product = orderDetail.getProduct();
    assertEquals("CDO", product.getName());

    // Modify
    orderDetail.setPrice(10);
    commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime2 = commitInfo.getTimeStamp();

    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch(name);

    check(session, mainBranch, commitTime1, 5, "CDO");
    check(session, mainBranch, commitTime2, 5, "CDO");
    check(session, mainBranch, CDOBranchPoint.UNSPECIFIED_DATE, 5, "CDO");

    check(session, subBranch, commitTime1, 5, "CDO");
    check(session, subBranch, commitTime2, 10, "CDO");
    check(session, subBranch, CDOBranchPoint.UNSPECIFIED_DATE, 10, "CDO");

    session.close();
  }

  public void testCommitAddOrderDetail() throws Exception
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    OrderDetail orderDetail = getModel1Factory().createOrderDetail();
    orderDetail.setProduct(product);
    orderDetail.setPrice(5);

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);
    resource.getContents().add(orderDetail);

    CDOCommitInfo commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource(getResourcePath("/res"));
    orderDetail = (OrderDetail)resource.getContents().get(1);
    assertEquals(5.0f, orderDetail.getPrice());
    product = orderDetail.getProduct();
    assertEquals("CDO", product.getName());

    // Modify
    OrderDetail orderDetail2 = getModel1Factory().createOrderDetail();
    orderDetail2.setProduct(product);
    orderDetail2.setPrice(10);
    resource.getContents().add(0, orderDetail2);

    commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime2 = commitInfo.getTimeStamp();

    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch(name);

    check(session, mainBranch, commitTime1, 5, "CDO");
    check(session, mainBranch, commitTime2, 5, "CDO");
    check(session, mainBranch, CDOBranchPoint.UNSPECIFIED_DATE, 5, "CDO");

    check(session, subBranch, commitTime1, 5, "CDO");
    check(session, subBranch, commitTime2, 5, 10, "CDO");
    check(session, subBranch, CDOBranchPoint.UNSPECIFIED_DATE, 5, 10, "CDO");

    session.close();
  }

  public void testCommitRemoveOrderDetail() throws Exception
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    OrderDetail orderDetail = getModel1Factory().createOrderDetail();
    orderDetail.setProduct(product);
    orderDetail.setPrice(5);

    OrderDetail orderDetail2 = getModel1Factory().createOrderDetail();
    orderDetail2.setProduct(product);
    orderDetail2.setPrice(10);

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(orderDetail2);
    resource.getContents().add(product);
    resource.getContents().add(orderDetail);

    CDOCommitInfo commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource(getResourcePath("/res"));
    orderDetail = (OrderDetail)resource.getContents().get(2);
    assertEquals(5.0f, orderDetail.getPrice());
    product = orderDetail.getProduct();
    assertEquals("CDO", product.getName());

    // Modify
    resource.getContents().remove(product.getOrderDetails().remove(1));

    commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime2 = commitInfo.getTimeStamp();

    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch(name);

    check(session, mainBranch, commitTime1, 5, 10, "CDO");
    check(session, mainBranch, commitTime2, 5, 10, "CDO");
    check(session, mainBranch, CDOBranchPoint.UNSPECIFIED_DATE, 5, 10, "CDO");

    check(session, subBranch, commitTime1, 5, 10, "CDO");
    check(session, subBranch, commitTime2, 5, "CDO");
    check(session, subBranch, CDOBranchPoint.UNSPECIFIED_DATE, 5, "CDO");

    session.close();
  }

  public void testDetachExisting() throws Exception
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    OrderDetail orderDetail = getModel1Factory().createOrderDetail();
    orderDetail.setProduct(product);
    orderDetail.setPrice(5);

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);
    resource.getContents().add(orderDetail);

    CDOCommitInfo commitInfo = transaction.commit();
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource(getResourcePath("/res"));
    orderDetail = (OrderDetail)resource.getContents().get(1);
    assertEquals(5.0f, orderDetail.getPrice());
    product = orderDetail.getProduct();
    assertEquals("CDO", product.getName());

    // Modify
    orderDetail.setPrice(10);
    commitInfo = transaction.commit();
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime2 = commitInfo.getTimeStamp();

    // Detach an object that already has revision in subBranch
    resource.getContents().remove(1);

    try
    {
      // product.getOrderDetails() contains pointer to detached orderDetail
      commitInfo = transaction.commit();
      fail("CommitException expected");
    }
    catch (CommitException expected)
    {
      assertInstanceOf(DanglingReferenceException.class, expected.getCause());
    }

    orderDetail.setProduct(null);

    commitInfo = transaction.commit();
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime3 = commitInfo.getTimeStamp();

    orderDetail = getModel1Factory().createOrderDetail();
    orderDetail.setPrice(777);

    try
    {
      product.getOrderDetails().set(0, orderDetail);
      fail("IndexOutOfBoundsException expected");
    }
    catch (IndexOutOfBoundsException expected)
    {
      // Success
    }

    product.getOrderDetails().add(orderDetail);

    try
    {
      // New orderDetail is not attached
      commitInfo = transaction.commit();
      fail("CommitException expected");
    }
    catch (CommitException expected)
    {
      assertInstanceOf(DanglingReferenceException.class, expected.getCause());
    }

    resource.getContents().add(orderDetail);

    commitInfo = transaction.commit();
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime4 = commitInfo.getTimeStamp();

    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch(name);

    check(session, mainBranch, commitTime1, 5, "CDO");
    check(session, mainBranch, commitTime2, 5, "CDO");
    check(session, mainBranch, commitTime3, 5, "CDO");
    check(session, mainBranch, commitTime4, 5, "CDO");
    check(session, mainBranch, CDOBranchPoint.UNSPECIFIED_DATE, 5, "CDO");

    check(session, subBranch, commitTime1, 5, "CDO");
    check(session, subBranch, commitTime2, 10, "CDO");

    try
    {
      check(session, subBranch, commitTime3, 0, "CDO", 1);
      fail("IndexOutOfBoundsException expected");
    }
    catch (IndexOutOfBoundsException expected)
    {
      // Success
    }

    check(session, subBranch, commitTime4, 777, "CDO");
    check(session, subBranch, CDOBranchPoint.UNSPECIFIED_DATE, 777, "CDO");

    session.close();
  }

  private void check(CDOSession session, CDOBranch branch, long timeStamp, float price, String name, int size)
  {
    CDOView view = session.openView(branch, timeStamp);
    CDOResource resource = view.getResource(getResourcePath("/res"));

    int actualSize = resource.getContents().size();
    assertEquals(size, actualSize);

    dumpAll(session);
    OrderDetail orderDetail = (OrderDetail)resource.getContents().get(1);
    dumpAll(session);

    float actualPrice = orderDetail.getPrice();
    assertEquals(price, actualPrice);

    Product1 product = orderDetail.getProduct();
    dumpAll(session);

    String actualName = product.getName();
    assertEquals(name, actualName);

    view.close();
  }

  private void check(CDOSession session, CDOBranch branch, long timeStamp, float price, String name)
  {
    check(session, branch, timeStamp, price, name, 2);
  }

  private void check(CDOSession session, CDOBranch branch, long timeStamp, float price, float price2, String name)
  {
    CDOView view = session.openView(branch, timeStamp);
    CDOResource resource = view.getResource(getResourcePath("/res"));
    assertEquals(3, resource.getContents().size());

    dumpAll(session);
    OrderDetail orderDetail2 = (OrderDetail)resource.getContents().get(0);
    OrderDetail orderDetail = (OrderDetail)resource.getContents().get(2);

    dumpAll(session);
    assertEquals(price, orderDetail.getPrice());
    assertEquals(price2, orderDetail2.getPrice());

    Product1 product = orderDetail.getProduct();
    Product1 product2 = orderDetail2.getProduct();
    dumpAll(session);
    assertEquals(name, product.getName());
    assertEquals(name, product2.getName());

    view.close();
  }

  public void testDetachWithoutRevision() throws Exception
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);

    CDOCommitInfo commitInfo = transaction.commit();
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource(getResourcePath("/res"));
    product = (Product1)resource.getContents().get(0);
    assertEquals("CDO", product.getName());

    // Detach an object that has no revision in subBranch
    resource.getContents().remove(0);

    commitInfo = transaction.commit();
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime2 = commitInfo.getTimeStamp();

    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch(name);

    check(session, subBranch, commitTime1, "CDO");

    try
    {
      check(session, subBranch, commitTime2, "CDO");
      fail("IndexOutOfBoundsException expected");
    }
    catch (IndexOutOfBoundsException expected)
    {
      // Success
    }

    try
    {
      check(session, subBranch, CDOBranchPoint.UNSPECIFIED_DATE, "CDO");
      fail("IndexOutOfBoundsException expected");
    }
    catch (IndexOutOfBoundsException expected)
    {
      // Success
    }

    session.close();
  }

  public void testDetachWithoutRevision_CheckMainBranch() throws Exception
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);

    CDOCommitInfo commitInfo = transaction.commit();
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource(getResourcePath("/res"));
    product = (Product1)resource.getContents().get(0);
    assertEquals("CDO", product.getName());

    // Detach an object that has no revision in subBranch
    resource.getContents().remove(0);

    commitInfo = transaction.commit();
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime2 = commitInfo.getTimeStamp();

    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch(name);

    check(session, mainBranch, commitTime1, "CDO");
    check(session, mainBranch, commitTime2, "CDO");
    check(session, mainBranch, CDOBranchPoint.UNSPECIFIED_DATE, "CDO");

    check(session, subBranch, commitTime1, "CDO");

    try
    {
      check(session, subBranch, commitTime2, "CDO");
      fail("IndexOutOfBoundsException expected");
    }
    catch (IndexOutOfBoundsException expected)
    {
      // Success
    }

    try
    {
      check(session, subBranch, CDOBranchPoint.UNSPECIFIED_DATE, "CDO");
      fail("IndexOutOfBoundsException expected");
    }
    catch (IndexOutOfBoundsException expected)
    {
      // Success
    }

    session.close();
  }

  @CleanRepositoriesBefore(reason = "Revision counting")
  public void testhandleRevisionsAfterDetachInSubBranch() throws Exception
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);

    CDOCommitInfo commitInfo = transaction.commit();
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource(getResourcePath("/res"));
    product = (Product1)resource.getContents().get(0);
    assertEquals("CDO", product.getName());

    product.setName("handleRevisions");
    commitInfo = transaction.commit();
    assertEquals(subBranch, commitInfo.getBranch());

    resource.getContents().remove(0);
    commitInfo = transaction.commit();
    assertEquals(subBranch, commitInfo.getBranch());

    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch(name);

    final List<CDORevision> revisions = new ArrayList<>();

    CDOSessionProtocol sessionProtocol = ((InternalCDOSession)session).getSessionProtocol();
    sessionProtocol.handleRevisions(null, subBranch, false, CDOBranchPoint.UNSPECIFIED_DATE, false, new CDORevisionHandler()
    {
      @Override
      public boolean handleRevision(CDORevision revision)
      {
        if (revision.getEClass() == getModel1Package().getProduct1())
        {
          fail("Product1 has been detached and should not be passed in here");
        }

        revisions.add(revision);
        return true;
      }
    });

    assertEquals(3, revisions.size());
  }

  public void testSwitchViewTarget() throws CommitException
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);

    CDOCommitInfo commitInfo = transaction.commit();
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();
    transaction.close();

    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);

    CDOID id = CDOUtil.getCDOObject(product).cdoID();
    CDOView view = session.openView();
    product = (Product1)CDOUtil.getEObject(view.getObject(id));

    view.setBranch(subBranch);

    assertEquals(false, CDOUtil.getCDOObject(product).cdoState().equals(CDOState.INVALID));
    assertNotNull(product.getName());
  }

  public void testSwitchTransactionTarget() throws CommitException
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);

    // Commit to main branch
    long commitTime1 = transaction.commit().getTimeStamp();

    // Create sub branch
    CDOBranch subBranch = mainBranch.createBranch(name, commitTime1);

    // Switch to sub branch
    transaction.setBranch(subBranch);
    assertEquals("CDO", product.getName());

    // Commit to sub branch
    product.setName("EMF");
    transaction.commit();

    // Switch to main branch
    transaction.setBranch(mainBranch);
    assertEquals("CDO", product.getName());

    // Commit to main branch
    product.setName("EMF");
    transaction.commit();
  }

  /**
   * Bug 383602: Branch with base after the last finished commit can be created
   */
  public void testFutureBaseTime() throws CommitException
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();
    CDOBranch mainBranch = branchManager.getMainBranch();

    long future = System.currentTimeMillis() + 1000000L;
    CDOBranch subBranch = mainBranch.createBranch(name, future);
    assertEquals(true, subBranch.getBase().getTimeStamp() < future);
  }

  public void testObjectLifetime() throws Exception
  {
    Company company = getModel1Factory().createCompany();

    CDOSession session = openSession1();
    CDOTransaction transaction = session.openTransaction();
    CDOResource resource = transaction.createResource(getResourcePath("test"));
    resource.getContents().add(company);
    CDOCommitInfo firstCommit = transaction.commit();

    modifyCompany(transaction, company, getBranchName("sub1"));
    modifyCompany(transaction, company, getBranchName("sub2"));
    CDOBranch sub3 = modifyCompany(transaction, company, getBranchName("sub3"));

    CDOID id = CDOUtil.getCDOObject(company).cdoID();
    CDOBranchPoint branchPoint = sub3.getHead();
    CDORevisionManager revisionManager = session.getRevisionManager();
    CDOBranchPointRange lifetime = revisionManager.getObjectLifetime(id, branchPoint);

    assertEquals(firstCommit, lifetime.getStartPoint());
    assertEquals(sub3.getHead(), lifetime.getEndPoint());
  }

  public void testAuditViewOnBranch() throws Exception
  {
    String name = getBranchName("subBranch");

    CDOSession session = openSession1();
    CDOBranchManager branchManager = session.getBranchManager();

    // Commit to main branch
    CDOBranch mainBranch = branchManager.getMainBranch();
    CDOTransaction transaction = session.openTransaction(mainBranch);
    assertEquals(mainBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    Product1 product = getModel1Factory().createProduct1();
    product.setName("CDO");

    OrderDetail orderDetail = getModel1Factory().createOrderDetail();
    orderDetail.setProduct(product);
    orderDetail.setPrice(5.0f);

    CDOResource resource = transaction.createResource(getResourcePath("/res"));
    resource.getContents().add(product);
    resource.getContents().add(orderDetail);

    CDOCommitInfo commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime1 = commitInfo.getTimeStamp();

    // Modify main branch (change existing OrderDetail)
    orderDetail.setPrice(10.0f);
    commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(mainBranch, commitInfo.getBranch());
    long commitTime2 = commitInfo.getTimeStamp();
    transaction.close();

    // Commit to sub branch
    CDOBranch subBranch = mainBranch.createBranch(name);
    transaction = session.openTransaction(subBranch);
    assertEquals(subBranch, transaction.getBranch());
    assertEquals(CDOBranchPoint.UNSPECIFIED_DATE, transaction.getTimeStamp());

    resource = transaction.getResource(getResourcePath("/res"));
    orderDetail = (OrderDetail)resource.getContents().get(1);
    assertEquals(10.0f, orderDetail.getPrice());
    product = orderDetail.getProduct();
    assertEquals("CDO", product.getName());

    // Modify sub branch (add new OrderDetail)
    OrderDetail orderDetail2 = getModel1Factory().createOrderDetail();
    orderDetail2.setProduct(product);
    orderDetail2.setPrice(20.0f);
    resource.getContents().add(0, orderDetail2);

    commitInfo = transaction.commit();
    dumpAll(session);
    assertEquals(subBranch, commitInfo.getBranch());
    long commitTime3 = commitInfo.getTimeStamp();

    transaction.close();
    closeSession1();

    session = openSession2();
    branchManager = session.getBranchManager();
    mainBranch = branchManager.getMainBranch();
    subBranch = mainBranch.getBranch(name);

    check(session, mainBranch, commitTime1, 5.0f, "CDO");
    check(session, mainBranch, commitTime2, 10.0f, "CDO");
    check(session, mainBranch, CDOBranchPoint.UNSPECIFIED_DATE, 10.0f, "CDO");

    dumpAll(session);
    check(session, subBranch, commitTime1, 5.0f, "CDO"); // !
    check(session, subBranch, commitTime2, 10.0f, "CDO");
    check(session, subBranch, commitTime3, 10.0f, 20.0f, "CDO");
    check(session, subBranch, CDOBranchPoint.UNSPECIFIED_DATE, 10.0f, 20.0f, "CDO");

    session.close();
  }

  private CDOBranch modifyCompany(CDOTransaction transaction, Company company, String branchName) throws Exception
  {
    for (int i = 0; i < 10; i++)
    {
      company.setName("Company" + i);
      transaction.commit();
    }

    CDOBranch branch = transaction.getBranch().createBranch(branchName);
    transaction.setBranch(branch);
    return branch;
  }

  private void check(CDOSession session, CDOBranch branch, long timeStamp, String name)
  {
    CDOView view = session.openView(branch, timeStamp);
    CDOResource resource = view.getResource(getResourcePath("/res"));

    dumpAll(session);
    Product1 product = (Product1)resource.getContents().get(0);
    assertEquals(name, product.getName());

    view.close();
  }

  private void dumpAll(CDOSession session)
  {
    IStore store = getRepository().getStore();
    if (store instanceof MEMStore)
    {
      MEMStore memStore = (MEMStore)store;
      dump("MEMStore", memStore.getAllRevisions());
    }

    dump("ServerCache", getRepository().getRevisionManager().getCache().getAllRevisions());
    dump("ClientCache", ((InternalCDOSession)session).getRevisionManager().getCache().getAllRevisions());
  }

  public static void dump(String label, Map<CDOBranch, List<CDORevision>> revisions)
  {
    System.out.println();
    System.out.println();
    System.out.println(label);
    System.out.println("===============================================================================================");
    CDORevisionUtil.dumpAllRevisions(revisions, System.out);
    System.out.println();
    System.out.println();
  }

  public static void assertEquals(Object expected, Object actual)
  {
    if (expected instanceof CDOBranch && actual instanceof CDOBranch)
    {
      if (((CDOBranch)expected).getID() != ((CDOBranch)actual).getID())
      {
        failNotEquals(null, expected, actual);
      }

      return;
    }

    AbstractCDOTest.assertEquals(expected, actual);
  }
}
