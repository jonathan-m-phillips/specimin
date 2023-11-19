package org.checkerframework.specimin;

import com.google.common.base.Splitter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * An UnsolvedClass instance is a representation of a class that can not be solved by SymbolSolver.
 * The reason is that the class file is not in the root directory.
 */
public class UnsolvedClass {
  /** Set of methods belongs to the class */
  private final Set<UnsolvedMethod> methods;

  /** The name of the class */
  private final @ClassGetSimpleName String className;

  /** The fields of this class */
  private final Set<String> classFields;

  /**
   * The name of the package of the class. We rely on the import statements from the source codes to
   * guess the package name.
   */
  private final String packageName;

  /** This field records the number of type variables for this class */
  private int numberOfTypeVariables = 0;

  /**
   * Create an instance of UnsolvedClass
   *
   * @param className the name of the class
   * @param packageName the name of the package
   */
  public UnsolvedClass(@ClassGetSimpleName String className, String packageName) {
    this.className = className;
    this.methods = new HashSet<>();
    this.packageName = packageName;
    this.classFields = new HashSet<>();
  }

  /**
   * Get the list of methods from this synthetic class
   *
   * @return the list of methods
   */
  public Set<UnsolvedMethod> getMethods() {
    return methods;
  }

  /**
   * Get the name of this class.
   *
   * @return the name of the class
   */
  public @ClassGetSimpleName String getClassName() {
    return className;
  }

  /**
   * Get the package where this class belongs to
   *
   * @return the value of packageName
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Get the fields of this current class
   *
   * @return classVariables
   */
  public Set<String> getClassFields() {
    return classFields;
  }

  /**
   * Add a method to the class
   *
   * @param method the method to be added
   */
  public void addMethod(UnsolvedMethod method) {
    this.methods.add(method);
  }

  /**
   * Add field declaration to the class. We expect something like "int i" or "String y" instead of
   * just "i" and "y"
   *
   * @param variableExpression the expression of the variables to be added
   */
  public void addFields(String variableExpression) {
    this.classFields.add(variableExpression);
  }

  /** This method sets the number of type variables for the current class */
  public void setNumberOfTypeVariables(int numberOfTypeVariables) {
    this.numberOfTypeVariables = numberOfTypeVariables;
  }

  /**
   * This method tells the number of type variables for this class
   *
   * @return the number of type variables
   */
  public int getNumberOfTypeVariables() {
    return this.numberOfTypeVariables;
  }

  /**
   * Update the return type of a method. Note: this method is supposed to be used to update
   * synthetic methods, where the return type of each method is distinct.
   *
   * @param currentReturnType the current return type of this method
   * @param desiredReturnType the new return type
   */
  public void updateMethodByReturnType(
      @ClassGetSimpleName String currentReturnType, @ClassGetSimpleName String desiredReturnType) {
    for (UnsolvedMethod method : methods) {
      if (method.getReturnType().equals(currentReturnType)) {
        method.setReturnType(desiredReturnType);
      }
    }
  }

  /**
   * This method updates the types of fields in this class
   *
   * @param currentType the current type
   * @param correctType the desired type
   */
  public void updateFieldByType(String currentType, String correctType) {
    Iterator<String> iterator = classFields.iterator();
    Set<String> newFields = new HashSet<>();
    while (iterator.hasNext()) {
      List<String> elements = Splitter.on(' ').splitToList(iterator.next());
      // fieldExpression is guaranteed to have the form "TYPE FIELD_NAME". Since this field
      // expression is from a synthetic class, there is no annotation involved, so TYPE has no
      // space.
      String fieldType = elements.get(0);
      String fieldName = elements.get(1);
      if (fieldType.equals(currentType)) {
        iterator.remove();
        newFields.add(
            UnsolvedSymbolVisitor.setInitialValueForVariableDeclaration(
                correctType, correctType + " " + fieldName));
      }
    }

    for (String field : newFields) {
      classFields.add(field);
    }
  }

  /**
   * Return the content of the class as a compilable Java file.
   *
   * @return the content of the class
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(packageName).append(";\n");
    sb.append("public class ").append(className).append(getTypeVariablesAsString()).append(" {\n");
    for (String variableDeclarations : classFields) {
      sb.append("    " + "public " + variableDeclarations + ";\n");
    }
    for (UnsolvedMethod method : methods) {
      sb.append(method.toString());
    }
    sb.append("}\n");
    return sb.toString();
  }

  /**
   * Return a synthetic representation for type variables of the current class.
   *
   * @return the synthetic representation for type variables
   */
  public String getTypeVariablesAsString() {
    if (numberOfTypeVariables == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    // if class A has three type variables, the expression will be A<T, TT, TTT>
    result.append("<");
    for (int i = 0; i < numberOfTypeVariables; i++) {
      String typeExpression = "T" + "T".repeat(i);
      result.append(typeExpression).append(", ");
    }
    result.delete(result.length() - 2, result.length());
    result.append(">");
    return result.toString();
  }
}
