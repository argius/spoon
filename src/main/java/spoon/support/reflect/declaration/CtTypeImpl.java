/*
 * Spoon - http://spoon.gforge.inria.fr/
 * Copyright (C) 2006 INRIA Futurs <renaud.pawlak@inria.fr>
 *
 * This software is governed by the CeCILL-C License under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at http://www.cecill.info.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the CeCILL-C License for more details.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */

package spoon.support.reflect.declaration;

import java.lang.annotation.Annotation;
import java.util.*;

import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.ReferenceTypeFilter;
import spoon.support.compiler.SnippetCompilationHelper;

import static spoon.reflect.ModelElementContainerDefaultCapacities.FIELDS_CONTAINER_DEFAULT_CAPACITY;
import static spoon.reflect.ModelElementContainerDefaultCapacities.TYPE_TYPE_PARAMETERS_CONTAINER_DEFAULT_CAPACITY;

/**
 * The implementation for {@link spoon.reflect.declaration.CtType}.
 */
public abstract class CtTypeImpl<T> extends CtNamedElementImpl  implements
		CtType<T> {

	private static final long serialVersionUID = 1L;

	List<CtTypeReference<?>> formalTypeParameters = EMPTY_LIST();

	Set<CtTypeReference<?>> interfaces = EMPTY_SET();

	Set<CtMethod<?>> methods = EMPTY_SET();

	private List<CtField<?>> fields = new ArrayList<CtField<?>>(
			FIELDS_CONTAINER_DEFAULT_CAPACITY);

	Set<CtType<?>> nestedTypes = EMPTY_SET();

	
	Set<ModifierKind> modifiers = EMPTY_SET();
	
	public CtTypeImpl() {
		super();
	}


	public <F> boolean addField(CtField<F> field) {
		if (!this.fields.contains(field)) {
			field.setParent(this);
			return this.fields.add(field);
		}

		// field already exists
		return false;
	}

	public <F> boolean removeField(CtField<F> field) {
		return this.fields.remove(field);
	}

	public CtField<?> getField(String name) {
		for (CtField<?> f : fields) {
			if (f.getSimpleName().equals(name)) {
				return f;
			}
		}
		return null;
	}

	public List<CtField<?>> getFields() {
		return fields;
	}


	public <N> boolean addNestedType(CtType<N> nestedType) {
		if (nestedTypes == CtElementImpl.<CtType<?>>EMPTY_SET()) {
			nestedTypes = new TreeSet<CtType<?>>();
		}
		nestedType.setParent(this);
		return this.nestedTypes.add(nestedType);
	}

	public <N> boolean removeNestedType(CtType<N> nestedType) {
		if (nestedTypes.isEmpty()) {
			return false;
		} else if (nestedTypes.size() == 1) {
			if (nestedTypes.contains(nestedType)) {
				nestedTypes = CtElementImpl.<CtType<?>>EMPTY_SET();
				return true;
			} else {
				return false;
			}
		} else {
			return this.nestedTypes.remove(nestedType);
		}
	}


	public Set<CtTypeReference<?>> getUsedTypes(boolean includeSamePackage) {
		Set<CtTypeReference<?>> typeRefs = new HashSet<CtTypeReference<?>>();
		for (CtTypeReference<?> typeRef : Query.getReferences(this,
				new ReferenceTypeFilter<CtTypeReference<?>>(
						CtTypeReference.class))) {
			if (!( typeRef.isPrimitive() ||
				   (typeRef instanceof CtArrayTypeReference) ||
				   typeRef.toString().equals(CtTypeReference.NULL_TYPE_NAME) ||
				   ( (typeRef.getPackage() != null) &&
					 "java.lang".equals(typeRef.getPackage().toString()))) &&
			    !( !includeSamePackage &&
		    		getPackageReference(typeRef).equals(this.getPackage().getReference()))) {
				typeRefs.add(typeRef);
			}
		}
		return typeRefs;
	}
	
	/**
	 * Return the package reference for the corresponding type reference. For
	 * inner type, return the package reference of the top-most enclosing type.
	 * This helper method is meant to deal with package references that are
	 * <code>null</code> for inner types.
	 * 
	 * @param tref  the type reference
	 * @return      the corresponding package reference
	 * @since 4.0
	 * @see CtTypeReference#getPackage()
	 */
	private static CtPackageReference getPackageReference( CtTypeReference<?> tref ) {
    	CtPackageReference pref = tref.getPackage();
    	while( pref == null ) {
    		tref = tref.getDeclaringType();
    		pref = tref.getPackage();
    	}
    	return pref;
	}

	public Class<T> getActualClass() {
		return getFactory().Type().createReference(this).getActualClass();
	}

	public CtType<?> getDeclaringType() {
		if(parent == null) {
			setParent(CtPackageImpl.ROOT_PACKAGE);
		}
		return getParent(CtType.class);
	}

	@SuppressWarnings("unchecked")
	public <N extends CtType<?>> N getNestedType(final String name) {
		class NestedTypeScanner extends CtScanner {
			CtType<?> type;

			public void checkType(CtType<?> type) {
				if (type.getSimpleName().equals(name)
						&& CtTypeImpl.this
						.equals(type.getDeclaringType())) {
					this.type = type;
				}
			}

			@Override
			public <U> void visitCtClass(
					spoon.reflect.declaration.CtClass<U> ctClass) {
				scan(ctClass.getNestedTypes());
				scan(ctClass.getConstructors());
				scan(ctClass.getMethods());

				checkType(ctClass);
			}

			@Override
			public <U> void visitCtInterface(
					spoon.reflect.declaration.CtInterface<U> intrface) {
				scan(intrface.getNestedTypes());
				scan(intrface.getMethods());

				checkType(intrface);
			}

			@Override
			public <U extends java.lang.Enum<?>> void visitCtEnum(
					spoon.reflect.declaration.CtEnum<U> ctEnum) {
				scan(ctEnum.getNestedTypes());
				scan(ctEnum.getConstructors());
				scan(ctEnum.getMethods());

				checkType(ctEnum);
			}

			@Override
			public <A extends Annotation> void visitCtAnnotationType(
					CtAnnotationType<A> annotationType) {
				scan(annotationType.getNestedTypes());

				checkType(annotationType);
			};

			CtType<?> getType() {
				return type;
			}
		}
		NestedTypeScanner scanner = new NestedTypeScanner();
		scanner.scan(this);
		return (N) scanner.getType();
	}

	public Set<CtType<?>> getNestedTypes() {
		return nestedTypes;
	}

	public CtPackage getPackage() {
		if (parent instanceof CtPackage) {
			return (CtPackage) parent;
		} else if (parent instanceof CtType) {
			return ((CtType<?>) parent).getPackage();
		} else {
			return null;
		}
	}
 

	@Override
	public CtTypeReference<T> getReference() {
		return getFactory().Type().createReference(this);
	}

	public boolean isTopLevel() {
		return (getDeclaringType() == null) && (getPackage() != null);
	}

	public void compileAndReplaceSnippets() {
		SnippetCompilationHelper.compileAndReplaceSnippetsIn(this);
	}


	@Override
	public Set<ModifierKind> getModifiers() {
		return modifiers;
	}

	@Override
	public boolean hasModifier(ModifierKind modifier) {
		return getModifiers().contains(modifier);
	}

	@Override
	public void setModifiers(Set<ModifierKind> modifiers) {
		this.modifiers = modifiers;
	}

	@Override
	public boolean addModifier(ModifierKind modifier) {
		if (modifiers == CtElementImpl.<ModifierKind>EMPTY_SET()) {
			this.modifiers = EnumSet.of(modifier);
			return true;
		}
		return modifiers.add(modifier);
	}

	@Override
	public boolean removeModifier(ModifierKind modifier) {
		return modifiers != CtElementImpl.<ModifierKind>EMPTY_SET() &&
				modifiers.remove(modifier);
	}

	@Override
	public void setVisibility(ModifierKind visibility) {
		if (modifiers == CtElementImpl.<ModifierKind> EMPTY_SET()) {
			this.modifiers = EnumSet.noneOf(ModifierKind.class);
		}
		getModifiers().remove(ModifierKind.PUBLIC);
		getModifiers().remove(ModifierKind.PROTECTED);
		getModifiers().remove(ModifierKind.PRIVATE);
		getModifiers().add(visibility);
	}

	@Override
	public ModifierKind getVisibility() {
		if (getModifiers().contains(ModifierKind.PUBLIC))
			return ModifierKind.PUBLIC;
		if (getModifiers().contains(ModifierKind.PROTECTED))
			return ModifierKind.PROTECTED;
		if (getModifiers().contains(ModifierKind.PRIVATE))
			return ModifierKind.PRIVATE;
		return null;
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isAnonymous() {
		return false;
	}

	@Override
	public CtTypeReference<?> getSuperclass() {
		// overridden in CtClassImpl
		return null;
	}

	@Override
	public boolean isInterface() {
		return false;
	}

	@Override
	public List<CtFieldReference<?>> getAllFields() {
		List<CtFieldReference<?>> l = new ArrayList<CtFieldReference<?>>(
				getFields().size());
		for (CtField<?> f: getFields()) {
			l.add(f.getReference());
		}
		if (this instanceof CtClass) {
			CtTypeReference<?> st = ((CtClass<?>) this).getSuperclass();
			if (st != null) {
				l.addAll(st.getAllFields());
			}
		}
		return l;
	}


	@Override
	public Collection<CtFieldReference<?>> getDeclaredFields() {
		List<CtFieldReference<?>> l = new ArrayList<CtFieldReference<?>>(
				getFields().size());
		for (CtField<?> f : getFields()) {
			l.add(f.getReference());
		}
		return Collections.unmodifiableCollection(l);
	}

	/**
	 * Tells if this type is a subtype of the given type.
	 */
	@Override
	public boolean isSubtypeOf(CtTypeReference<?> type) {
		return type.isSubtypeOf(getReference());
	}

	@Override
	public boolean isAssignableFrom(CtTypeReference<?> type) {
		return isSubtypeOf(type);
	}



	public <M> boolean addMethod(CtMethod<M> method) {
		if (methods == CtElementImpl.<CtMethod<?>> EMPTY_SET()) {
			methods = new TreeSet<CtMethod<?>>();
		}
		method.setParent(this);
		return methods.add(method);
	}

	public <M> boolean removeMethod(CtMethod<M> method) {
		if (methods.isEmpty()) {
			return false;
		} else if (methods.size() == 1) {
			if (methods.contains(method)) {
				methods = CtElementImpl.<CtMethod<?>>EMPTY_SET();
				return true;
			} else {
				return false;
			}
		} else {
			// This contains() is not needed for dealing with empty and
			// singleton sets (as they are dealt above), but removing contains()
			// check here might broke someone's code like
			// type.setMethods(immutableSet(a, b, c));
			// ...
			// type.removeMethod(d)
			return methods.contains(method) && methods.remove(method);
		}
	}

	public <S> boolean addSuperInterface(CtTypeReference<S> interfac) {
		if (interfaces == CtElementImpl.<CtTypeReference<?>> EMPTY_SET()) {
			interfaces = new TreeSet<CtTypeReference<?>>();
		}
		return interfaces.add(interfac);
	}

	public <S> boolean removeSuperInterface(CtTypeReference<S> interfac) {
		if (interfaces.isEmpty()) {
			return false;
		} else if (interfaces.size() == 1) {
			if (interfaces.contains(interfac)) {
				interfaces = CtElementImpl.<CtTypeReference<?>>EMPTY_SET();
				return true;
			} else {
				return false;
			}
		} else {
			// contains() not needed. see comment in removeMethod()
			return interfaces.contains(interfac) && interfaces.remove(interfac);
		}
	}

	public boolean addFormalTypeParameter(CtTypeReference<?> formalTypeParameter) {
		if (formalTypeParameters == CtElementImpl
				.<CtTypeReference<?>> EMPTY_LIST()) {
			formalTypeParameters = new ArrayList<CtTypeReference<?>>(
					TYPE_TYPE_PARAMETERS_CONTAINER_DEFAULT_CAPACITY);
		}
		return formalTypeParameters.add(formalTypeParameter);
	}

	public boolean removeFormalTypeParameter(
			CtTypeReference<?> formalTypeParameter) {
		return formalTypeParameters.contains(formalTypeParameter) &&
				formalTypeParameters.remove(formalTypeParameter);
	}

	public List<CtTypeReference<?>> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	@SuppressWarnings("unchecked")
	public <R> CtMethod<R> getMethod(CtTypeReference<R> returnType,
			String name, CtTypeReference<?>... parameterTypes) {
		for (CtMethod<?> mm : methods) {
			CtMethod<R> m = (CtMethod<R>) mm;
			if (m.getSimpleName().equals(name)) {
				if (!m.getType().equals(returnType)) {
					continue;
				}
				boolean cont = m.getParameters().size() == parameterTypes.length;
				for (int i = 0; cont && (i < m.getParameters().size())
						&& (i < parameterTypes.length); i++) {
					if (!m.getParameters().get(i).getType().getQualifiedName()
							.equals(parameterTypes[i].getQualifiedName())) {
						cont = false;
					}
				}
				if (cont) {
					return m;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <R> CtMethod<R> getMethod(String name,
			CtTypeReference<?>... parameterTypes) {
		for (CtMethod<?> m : methods) {
			if (m.getSimpleName().equals(name)) {
				boolean cont = m.getParameters().size() == parameterTypes.length;
				for (int i = 0; cont && (i < m.getParameters().size())
						&& (i < parameterTypes.length); i++) {
					// String
					// s1=m.getParameters().get(i).getType().getQualifiedName();
					// String s2=parameterTypes[i].getQualifiedName();
					if (!m.getParameters().get(i).getType()
							.equals(parameterTypes[i])) {
						cont = false;
					}
				}
				if (cont) {
					return (CtMethod<R>) m;
				}
			}
		}
		return null;
	}

	public Set<CtMethod<?>> getMethods() {
		return methods;
	}

	@Override
	public Set<CtMethod<?>> getMethodsAnnotatedWith(
			CtTypeReference<?>... annotationTypes) {
		Set<CtMethod<?>> result = new HashSet<CtMethod<?>>();
		for (CtMethod<?> m : methods) {
			for (CtAnnotation<?> a : m.getAnnotations()) {
				if (Arrays.asList(annotationTypes).contains(
						a.getAnnotationType())) {
					result.add(m);
				}
			}
		}
		return result;
	}

	@Override
	public List<CtMethod<?>> getMethodsByName(String name) {
		List<CtMethod<?>> result = new ArrayList<CtMethod<?>>(1);
		for (CtMethod<?> m : methods) {
			if (name.equals(m.getSimpleName())) {
				result.add(m);
			}
		}
		return result;
	}

	@Override
	public String getQualifiedName() {
		if (isTopLevel()) {
			if ((getPackage() != null)
					&& !getPackage().getSimpleName().equals(
					CtPackage.TOP_LEVEL_PACKAGE_NAME)) {
				return getPackage().getQualifiedName() + "." + getSimpleName();
			}
			return getSimpleName();
		}
		if (getDeclaringType() != null) {
			return getDeclaringType().getQualifiedName() + INNERTTYPE_SEPARATOR
					+ getSimpleName();
		}
		return getSimpleName();
	}

	public Set<CtTypeReference<?>> getSuperInterfaces() {
		return interfaces;
	}

	public void setFormalTypeParameters(
			List<CtTypeReference<?>> formalTypeParameters) {
		this.formalTypeParameters = formalTypeParameters;
	}

	public void setMethods(Set<CtMethod<?>> methods) {
		this.methods.clear();
		for(CtMethod meth: methods) {
			addMethod(meth);
		}
	}

	public void setSuperInterfaces(Set<CtTypeReference<?>> interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * Gets the executables declared by this type if applicable.
	 */
	public Collection<CtExecutableReference<?>> getDeclaredExecutables() {
		List<CtExecutableReference<?>> l = new ArrayList<CtExecutableReference<?>>(
				getMethods().size());
		for (CtExecutable<?> m : getMethods()) {
			l.add(m.getReference());
		}
		return Collections.unmodifiableCollection(l);
	}

	/**
	 * Gets the executables declared by this type and by all its supertypes if
	 * applicable.
	 */
	public Collection<CtExecutableReference<?>> getAllExecutables() {
		HashSet<CtExecutableReference<?>> l = new HashSet<CtExecutableReference<?>>(getDeclaredExecutables());
		if (this instanceof CtClass) {
			CtTypeReference<?> st = ((CtClass<?>) this).getSuperclass();
			if (st != null) {
				l.addAll(st.getAllExecutables());
			}
		}
		return l;		
	}
	
	@Override
	public Set<CtMethod<?>> getAllMethods() {
		Set<CtMethod<?>> l = new HashSet<CtMethod<?>>(getMethods());
		
		if ((getSuperclass() != null)
				&& (getSuperclass().getDeclaration() != null)) {
			CtType<?> t = getSuperclass().getDeclaration();
			l.addAll(t.getAllMethods());
		}

		for (CtTypeReference<?> ref : getSuperInterfaces()) {
			if (ref.getDeclaration() != null) {
				CtType<?> t = ref.getDeclaration();
				l.addAll(t.getAllMethods());
			}
		}

		return Collections.unmodifiableSet(l);		
	}
}
