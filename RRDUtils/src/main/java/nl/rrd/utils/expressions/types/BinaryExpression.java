package nl.rrd.utils.expressions.types;

import nl.rrd.utils.expressions.Expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BinaryExpression implements Expression {
	protected Expression operand1;
	protected Expression operand2;

	public BinaryExpression(Expression operand1, Expression operand2) {
		this.operand1 = operand1;
		this.operand2 = operand2;
	}

	public Expression getOperand1() {
		return operand1;
	}

	public Expression getOperand2() {
		return operand2;
	}

	@Override
	public List<Expression> getChildren() {
		List<Expression> result = new ArrayList<>();
		result.add(operand1);
		result.add(operand2);
		return result;
	}

	@Override
	public void substituteChild(int index, Expression expr) {
		if (index == 0)
			operand1 = expr;
		else if (index == 1)
			operand2 = expr;
	}

	@Override
	public List<Expression> getDescendants() {
		List<Expression> result = new ArrayList<>();
		for (Expression child : getChildren()) {
			result.add(child);
			result.addAll(child.getDescendants());
		}
		return result;
	}

	@Override
	public Set<String> getVariableNames() {
		Set<String> result = new HashSet<>();
		for (Expression child : getChildren()) {
			result.addAll(child.getVariableNames());
		}
		return result;
	}
}
