/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.python.types;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.plugins.python.api.symbols.ClassSymbol;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.types.InferredType;
import org.sonar.python.semantic.ClassSymbolImpl;

class RuntimeType implements InferredType {

  private ClassSymbol typeClass;
  private String builtinFullyQualifiedName;
  private Set<String> typeClassSuperClassesFQN = null;
  private Set<String> typeClassMembersFQN = null;

  RuntimeType(ClassSymbol typeClass) {
    this.typeClass = typeClass;
  }

  RuntimeType(String builtinFullyQualifiedName) {
    this.builtinFullyQualifiedName = builtinFullyQualifiedName;
  }

  @Override
  public boolean isIdentityComparableWith(InferredType other) {
    if (other == AnyType.ANY || other instanceof DeclaredType) {
      return true;
    }
    if (other instanceof UnionType) {
      return other.isIdentityComparableWith(this);
    }
    return this.equals(other);
  }

  @Override
  public boolean canHaveMember(String memberName) {
    return getTypeClass().canHaveMember(memberName);
  }

  @Override
  public boolean declaresMember(String memberName) {
    return canHaveMember(memberName);
  }

  @Override
  public Optional<Symbol> resolveMember(String memberName) {
    return getTypeClass().resolveMember(memberName);
  }

  @Override
  public Optional<Symbol> resolveDeclaredMember(String memberName) {
    return resolveMember(memberName);
  }

  @Override
  public boolean canOnlyBe(String typeName) {
    return typeName.equals(getTypeClass().fullyQualifiedName());
  }

  @Override
  public boolean canBeOrExtend(String typeName) {
    return getTypeClass().canBeOrExtend(typeName);
  }

  @Override
  public boolean isCompatibleWith(InferredType other) {
    return InferredTypes.isTypeClassCompatibleWith(getTypeClass(), other);
  }

  public boolean mustBeOrExtend(String fullyQualifiedName) {
    return getTypeClass().isOrExtends(fullyQualifiedName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuntimeType that = (RuntimeType) o;
    return Objects.equals(getTypeClass().name(), that.getTypeClass().name()) &&
      Objects.equals(getTypeClass().fullyQualifiedName(), that.getTypeClass().fullyQualifiedName())
      && Objects.equals(typeClassSuperClassesFQN(), that.typeClassSuperClassesFQN())
      && Objects.equals(typeClassMembersFQN(), that.typeClassMembersFQN());
  }

  private Set<String> typeClassSuperClassesFQN() {
    if (typeClassSuperClassesFQN == null) {
      typeClassSuperClassesFQN = getTypeClass().superClasses().stream().map(Symbol::fullyQualifiedName).collect(Collectors.toSet());
    }
    return typeClassSuperClassesFQN;
  }

  private Set<String> typeClassMembersFQN() {
    if (typeClassMembersFQN == null) {
      typeClassMembersFQN = getTypeClass().declaredMembers().stream().map(Symbol::fullyQualifiedName).collect(Collectors.toSet());
    }
    return typeClassMembersFQN;
  }

  boolean hasUnresolvedHierarchy() {
    return ((ClassSymbolImpl) getTypeClass()).hasUnresolvedTypeHierarchy(false);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTypeClass().name(), getTypeClass().fullyQualifiedName(), typeClassSuperClassesFQN(), typeClassMembersFQN());
  }

  @Override
  public String toString() {
    return "RuntimeType(" + getTypeClass().fullyQualifiedName() + ')';
  }

  public ClassSymbol getTypeClass() {
    if (typeClass == null) {
      return TypeShed.typeShedClass(builtinFullyQualifiedName);
    }
    return typeClass;
  }
}
