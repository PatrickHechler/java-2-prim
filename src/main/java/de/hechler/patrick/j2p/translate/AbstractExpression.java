package de.hechler.patrick.j2p.translate;


@SuppressWarnings("javadoc")
public interface AbstractExpression {
	
	interface Constant extends AbstractExpression {/**/}
	
	interface AccessableValue extends AbstractExpression {/**/}
	
	interface ParameterValue extends AbstractExpression {/**/}
	
	interface MethodResult extends AbstractExpression {/**/}
	
	interface CalculationResult extends AbstractExpression {/**/}
	
}
