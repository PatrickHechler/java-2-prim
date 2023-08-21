# Abstract Code

abstract code contains three things:
+ a list of parameters
+ a list of commands
+ an exception handling table

## Commands

the following command types can be used
+ assign
    + has a target expression
    + and a assignment value expression
+ useless access
    + this is only needed, if the thing which is accessed is invalid, otherwise this can be ignored
    + an invalid access leads to an error
+ method invocation
    + has a target method reference
    + has a list of parameter value expressions
+ if
    + has a condition
    + has a target command index (in the command table it is itself)
+ goto
    + has a target command index (in the command table it is itself)
+ return
    + has an optional return value expression

### Expression

an expression can be on of the following:
+ constant
+ accessable value
+ method result
+ calculation result
