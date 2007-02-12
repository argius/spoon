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

package spoon.reflect.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines a symbolic evaluation path as a list of abstract
 * evaluation steps (see {@link spoon.reflect.eval.SymbolicEvaluationStep}).
 */
public class SymbolicEvaluationPath {

	/**
	 * Creates a symbolic evaluation path.
	 */
	public SymbolicEvaluationPath() {
	}

	List<SymbolicEvaluationStep> steps = new ArrayList<SymbolicEvaluationStep>();

	/**
	 * Creates and adds a step to this path.
	 * 
	 * @param kind
	 *            the step kind
	 * @param evaluator
	 *            the current evaluator (its state is used to initialize the new
	 *            step).
	 */
	public void addStep(StepKind kind, SymbolicEvaluator evaluator) {
		steps.add(new SymbolicEvaluationStep(kind, new SymbolicStackFrame(
				evaluator.getStack().getFrameStack().peek()), new SymbolicHeap(
				evaluator.getHeap())));
	}

	/**
	 * Gets the ith step.
	 */
	public SymbolicEvaluationStep getStep(int i) {
		return steps.get(i);
	}

	/**
	 * Returns the evaluation steps list.
	 */
	public List<SymbolicEvaluationStep> getSteps() {
		return steps;
	}

	/**
	 * Gets the number of steps in this path.
	 */
	public int getStepCount() {
		return steps.size();
	}

	/**
	 * Dumps this path on the screen.
	 */
	public void dump() {
		for (int i = 0; i < steps.size(); i++) {
			System.out.println((i + 1) + "\t" + steps.get(i).getKind() + " "
					+ steps.get(i).getFrame());
			steps.get(i).getHeap().dump();
		}
	}

	/**
	 * A string representation.
	 */
	@Override
	public String toString() {
		String res = "";
		for (int i = 0; i < steps.size(); i++) {
			res += steps.get(i).getKind() + "("
					+ steps.get(i).getFrame().getExecutable().getSimpleName()
					+ ");";
		}
		return res;
	}

	public SymbolicEvaluationPath getEnterSteps() {
		SymbolicEvaluationPath res=new SymbolicEvaluationPath();
		for(SymbolicEvaluationStep s:getSteps()) {
			if(s.kind==StepKind.ENTER) {
				res.getSteps().add(s);
			}
		}
		return res;
	}

	public SymbolicEvaluationPath getExitSteps() {
		SymbolicEvaluationPath res=new SymbolicEvaluationPath();
		for(SymbolicEvaluationStep s:getSteps()) {
			if(s.kind==StepKind.EXIT) {
				res.getSteps().add(s);
			}
		}
		return res;
	}

}