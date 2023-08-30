package de.hechler.patrick.j2p.understand;


@SuppressWarnings("javadoc")
public interface AbstractExpression {
	
	default CodeUnderstander.AgeExp age(int birth) {
		return new CodeUnderstander.AgeExp(this, birth);
	}
	
	interface Constant extends AbstractExpression {/**/}
	
	interface AccessableValue extends AbstractExpression {/**/}
	
	interface ParameterValue extends AbstractExpression {/**/}
	
	interface MethodResult extends AbstractExpression {/**/}
	
	interface CalculationResult extends AbstractExpression {/**/}
	
}
