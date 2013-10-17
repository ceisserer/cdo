/**
 */
package org.eclipse.emf.cdo.releng.setup;

import org.eclipse.emf.cdo.releng.workingsets.WorkingSet;
import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Set Working Task</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.WorkingSetTask#getWorkingSets <em>Working Sets</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.eclipse.emf.cdo.releng.setup.SetupPackage#getWorkingSetTask()
 * @model
 * @generated
 */
public interface WorkingSetTask extends SetupTask
{
  /**
   * Returns the value of the '<em><b>Working Sets</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.emf.cdo.releng.workingsets.WorkingSet}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Working Sets</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Working Sets</em>' containment reference list.
   * @see org.eclipse.emf.cdo.releng.setup.SetupPackage#getWorkingSetTask_WorkingSets()
   * @model containment="true" resolveProxies="true"
   * @generated
   */
  EList<WorkingSet> getWorkingSets();

} // SetWorkingTask