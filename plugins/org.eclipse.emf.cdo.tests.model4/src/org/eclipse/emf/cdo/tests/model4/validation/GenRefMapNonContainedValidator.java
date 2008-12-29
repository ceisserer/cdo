/**
 * <copyright>
 * </copyright>
 *
 * $Id: GenRefMapNonContainedValidator.java,v 1.1 2008-12-29 15:06:17 estepper Exp $
 */
package org.eclipse.emf.cdo.tests.model4.validation;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;

/**
 * A sample validator interface for {@link org.eclipse.emf.cdo.tests.model4.GenRefMapNonContained}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface GenRefMapNonContainedValidator
{
  boolean validate();

  boolean validateElements(EMap<String, EObject> value);
}
