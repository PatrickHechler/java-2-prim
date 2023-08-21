package de.hechler.patrick.j2p.translate;


@SuppressWarnings("javadoc")
public interface AbstractExpression {
	
	interface Constant extends AbstractExpression {
		
		interface Reference extends Constant {
			
			final Reference NULL = new StringConstant(null);
			
			record StringConstant(String value) implements Reference { }
			
		}
		
		record Int64(long value) implements Constant {}
		
		record Int32(int value) implements Constant {}
		
		record FP64(double value) implements Constant {}
		
		record FP32(float value) implements Constant {}
		
	}
	
	interface AccessableValue extends AbstractExpression {
		
	}
	
	interface MethodResult extends AbstractExpression {
		
	}
	
	interface CalculationResult extends AbstractExpression {
		
		enum UnaryOp {
			ARITMETIC_NEGATE, LOGICAL_NOT
		}
		
		record Unary(UnaryOp op, AbstractExpression value) implements CalculationResult {}
		
		enum BinaryOp {
			
			EQUAL, NOT_EQUAL, GREATHER, GREATHER_EQUAL, LOWER, LOWER_EQUAL,
			
			ADD, SUB, MUL, DIV, MOD,
			
			SHIFT_LEFT, SHIFT_ARITMETIC_RIGTH, SHIFT_LOGIC_RIGTH,
			
			ARRAY_INDEXING
			
		}
		
		record Biary(AbstractExpression a, BinaryOp op, AbstractExpression b) implements CalculationResult {}
		
	}
	
}
