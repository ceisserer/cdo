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
package org.eclipse.emf.cdo.internal.ui.dialogs;

import org.eclipse.emf.cdo.internal.ui.SharedIcons;
import org.eclipse.emf.cdo.internal.ui.bundle.OM;
import org.eclipse.emf.cdo.util.CDOPackageType;
import org.eclipse.emf.cdo.util.CDOUtil;

import org.eclipse.net4j.ui.widgets.BaseDialog;

import org.eclipse.emf.ecore.EcorePackage;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
public class SelectPackageDialog extends BaseDialog<CheckboxTableViewer>
{
  private static final Set<String> NO_URIS = Collections.emptySet();

  private Map<String, CDOPackageType> packageTypes = CDOUtil.getPackageTypes();

  private Set<String> excludedURIs = new HashSet();

  private Set<String> checkedURIs = new HashSet();

  public SelectPackageDialog(Shell shell, String title, String message, Set<String> excludedURIs)
  {
    super(shell, DEFAULT_SHELL_STYLE | SWT.APPLICATION_MODAL, title, message, OM.Activator.INSTANCE.getDialogSettings());
    this.excludedURIs = excludedURIs;
  }

  public SelectPackageDialog(Shell shell, String title, String message)
  {
    this(shell, title, message, NO_URIS);
  }

  public Set<String> getCheckedURIs()
  {
    return checkedURIs;
  }

  @Override
  protected void createUI(Composite parent)
  {
    CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE | SWT.BORDER);
    viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    viewer.setContentProvider(new PackageContentProvider());
    viewer.setLabelProvider(new PackageLabelProvider());
    viewer.setInput(packageTypes);

    setCurrentViewer(viewer);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent)
  {
    super.createButtonsForButtonBar(parent);
    // final PreferenceButton showIgnored = new PreferenceButton(parent,
    // SWT.CHECK, OM.PREF_SHOW_IGNORED_BUNDLES);
    // showIgnored.setText("Show ignored bundles");
    // showIgnored.addSelectionListener(new SelectionAdapter()
    // {
    // @Override
    // public void widgetSelected(SelectionEvent e)
    // {
    // OM.PREF_SHOW_IGNORED_BUNDLES.setValue(showIgnored.getSelection());
    // getCurrentViewer().refresh(true);
    // }
    // });
    //
    // PreferenceButton startup = new PreferenceButton(parent, SWT.CHECK,
    // OM.PREF_CHECK_DURING_STARTUP);
    // startup.setText("Check during startup");
  }

  @Override
  protected void okPressed()
  {
    Object[] checkedElements = getCurrentViewer().getCheckedElements();
    for (Object checkedElement : checkedElements)
    {
      checkedURIs.add((String)checkedElement);
    }

    super.okPressed();
  }

  /**
   * @author Eike Stepper
   */
  private class PackageContentProvider implements IStructuredContentProvider
  {
    public PackageContentProvider()
    {
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }

    public Object[] getElements(Object inputElement)
    {
      Set<String> uris = new HashSet(packageTypes.keySet());
      uris.removeAll(excludedURIs);

      List<String> elements = new ArrayList(uris);
      Collections.sort(elements);
      return elements.toArray();
    }
  }

  /**
   * @author Eike Stepper
   */
  private class PackageLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider
  {
    private final Color red = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

    public PackageLabelProvider()
    {
    }

    @Override
    public String getText(Object element)
    {
      return element.toString();
    }

    @Override
    public Image getImage(Object element)
    {
      if (element instanceof String)
      {
        CDOPackageType packageType = packageTypes.get(element);
        switch (packageType)
        {
        case CONVERTED:
          return SharedIcons.getImage(SharedIcons.OBJ_EPACKAGE_CONVERTED);

        case LEGACY:
          return SharedIcons.getImage(SharedIcons.OBJ_EPACKAGE_LEGACY);

        case NATIVE:
          return SharedIcons.getImage(SharedIcons.OBJ_EPACKAGE_NATIVE);
        }
      }

      return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      return getText(element);
    }

    public Image getColumnImage(Object element, int columnIndex)
    {
      return getImage(element);
    }

    public Color getBackground(Object element, int columnIndex)
    {
      return null;
    }

    public Color getForeground(Object element, int columnIndex)
    {
      if (EcorePackage.eINSTANCE.getNsURI().equals(element))
      {
        return red;
      }

      return null;
    }
  }
}
