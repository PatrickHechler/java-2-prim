# Abstract Code

an abstract block of code contains two things:
+ a list of parameters
+ a list of commands

## Commands

the following command types can be used
+ assign
    + has a target expression
    + and a assignment value expression
+ access
    + has a target expression, which should be accessed
    + an invalid access leads to an error
+ method invocation
    + has a target method reference
    + has a list of parameter value expressions
+ new
    + object creation
+ assert
    + has a expression which is assumed to evaluate to true
    + if the expression does not evaluate to true this command fails
+ switch_goto
    + has a expression
    + has a default target abstract code block
    + has a map of expression to abstract code blocks
    + has a list of parameter expression for the target
+ if_goto
    + has a condition
    + has a two target abstract code blocks
    + has a list of the parameter expressions for the target
    + the first is executed if the condition evaulates to true, the other is executed if not
        + note that both code blocks get the same parameter values
+ goto
    + has a target abstract code block
    + has a list of the parameter expressions for the target
+ return
    + has an optional return value expression

### Expression

an expression can be on of the following:
+ constant
+ accessable value
+ parameter value
+ method result
+ calculation result

## Exception handling

additional each [Command](#commands) and each [Expression](#expression) has an exception handling table.    
this table maps exception types with goto commands.

the table is used when an exception occurs while calculating the expression (when the expression is an calculation result the inner expressions are handeld with their own exception table) or while executing the command (when the exception occurs while loading on of the commands expression the exception table of the exception is used instead)
