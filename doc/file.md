# Java-2-Prim
the [Error Constants](#error-constants) section defines Constants for the `ERRNO` register.    
the [Method call](#method-call) section describes how natives code has to invoke java code, how native code is invoked by java code and how java code invokes other java methods.    
the [_JNI-Env_](#_jni-env_) section list all operations of the _JNI-Env_.    
the [Class Loading](#class-loading) section describes how classes are loaded and found    
the [Method excecution](#method-excecution) section describes how java methods are executed.    
the [Initialisation](#initialization) section describes how _pvm-java_ can be initilized.    

## Error Constants
+ `ERR_JAVA_THROW` : `17` : `HEX-11`
    + this constant indicates that an java exception is currently thrown
+ `ERR_JAVA_NO_SUCH_CLASS` : `18` : `HEX-12`
    + this constant indicates that a non-existing class was searched
+ `ERR_JAVA_NO_SUCH_METHOD` : `18` : `HEX-12`
    + this constant indicates that a non-existing method was searched
+ `ERR_JAVA_NO_SUCH_FIELD` : `19` : `HEX-13`
    + this constant indicates that a non-existing field was searched
+ `ERR_JAVA_PERM` : `20` : `HEX-14`
    + this constant indicates that it was tried to access a field or invoce a method, which is not visible
    + this constant can also indicate that a static/instance field/method was treated like an instance/static field/method
+ `ERR_JAVA_CAST` : `21` : `HEX-15`
    + this constant indicates that a cast failed

## Method call
### _JNI-Env_ Argument
#### Java code calls
the _JNI-Env_ pointer is passed in the `X1F` register    
+ if a `native` method is invoked _pvm-java_ sets the `X1F` register to the _JNI-Env_ pointer after the native code returns
    + the native code __must__ not modify the _JNI-Env_ pointers target/content
+ otherwise the _JNI-Env_ pointer also has to be stored in the `X1F` register when the method returns

#### native code calls java code
_pvm-java_ saves the `X1F` register on the stack and overwrites it with the _JNI-Env_ pointer.

### Arguments
if possible, the arguments are stored in the registers after the `X1F` register:
1. `X20` contains the first argument
    + this is either a reference to `this` or to the `class` of the method to be executed
2. `X21` contains the second argument
3. ...

if this is not possible (there are more arguments then registers after the `X1F` register exist)
1. `X20` contains the first argument
    + this is either a reference to `this` or to the `class` of the method to be executed
+ `X21` points to the arguments
+ `X21` was allocated with `INT_MEMORY_ALLOC` and is not needed anywhere else
    + this means that the method is can use `INT_MEMORY_REALLOC` to resize the array
    + this means also that the invoked method __must__ use the `INT_MEMORY_FREE` interrupt, once the block is no longer needed by the method

#### Native code calls
for native code to execute a method invocation it has to use the _JNI-Env_.    
additional to the description above, the native code has to store a _method-reference_ of the method to be invoced in the `X1E` register

### Return
#### Normal Return
if there is a return value it is stored in the `X00` register.

#### Return with an Exception being thrown
##### Native code
native code can use the `JNI-Env` to do the following exception handling related things:
+ throw an exception instance
    + _pvm-java_ then sets `ERRNO` to `ERR_JAVA_THROW` before it returns to the native code
+ ask/find out if there is an exception which is currently being thrown.
+ catch the exception instance which is currently being thrown
    + by doing so the native code also gets a reference to the exception instance
    + if `ERRNO` is still set to `ERR_JAVA_THROW` _pvm-java_ sets it to `0`

1. when native code returns,
2. no exception instance is currently being thrown and
3. `ERRNO` is set to a non-`0` and non-`ERR_JAVA_THROW` value then
4. _pvm-java_ automatically creates an exception instance and then imidiatly throws it.

native code can throw exceptions in two ways:
+ explicitly using the _JNI-Env_ to throw an exception instance
+ returning to java code with `ERRNO` set to a non-`0` and non-`ERR_JAVA_THROW` value
    + note that this only works, when no exception instance is already being thrown

##### Java Code
a reference to that exception is stored in the `X00` register and `ERRNO` is set to `ERR_JAVA_THROW`.    
when returning from native code _pvm-java_ __must__ set `X00` and `ERRNO`    
when returning from java code to native code _pvm-java_ __must__ save a reference to the exception instance from `X00` somewhere else, when `ERRNO` is set to `ERR_JAVA_THROW`

## _JNI-Env_
the _JNI-Env_ pointer is usually stored in the `X1F` register (see [Method Call](#method-call)).

note that no operation allows any of its reference parameters to be invalid or `null` (`-1`) unless otherwise noted.    
If any reference is invalid (or `null` (`-1`)) the behavior is undefined.    
a weak references become invalid when the instance which its target becomes garbage collected

note that a returned reference is usually a local-reference (unless otherwise noted)

Operations:
+ `offset=0=HEX-0`   : _throw_
    + `X00` stores a reference to the exception instance which should been thrown
    + `ERRNO` will be set to `ERR_JAVA_THROW`
    + if there is already an exception instance which is being thrown, it will be catched and ignored
+ `offset=8=HEX-8`   : _isThrowing_
    + sets `X00` to `1` if there is currently an exception instance being thrown
    + sets `X00` to `0` if currently there is no exception instance being thrown
+ `offset=16=HEX-10` : _getThrowingClass_
    + sets `X00` to a reference of the class from the exception instance, which is currently being thrown
    + sets `X00` to a `-1` if currently there is no exception instance being thrown
    + modifies `X01` and `X02`
+ `offset=24=HEX-18` : _catch_
    + sets `X00` to a reference of the exception instance, which is now handled
    + sets `X00` to a `-1` if currently there is no exception instance being thrown
    + modifies `X01` and `X02` if an excception is now handled
+ `offset=32=HEX-20` : _findModule_
    + `X00` is set to a negative value or zero or an _UTF-8_ `\0` terminated string, which contains the module name
        + if `X01` is negative or zero the current module will be used
          + if there is no current module (the native code was not called by java code) the behavior is undefined
    + `X00` will be set to a reference of the module
    + if the module could not be found `ERRNO` will be set to `ERR_JAVA_NO_SUCH_CLASS`
+ `offset=40=HEX-28` : _findClass_
    + `X00` is set to an _UTF-8_ `\0` terminated string, which contains the class binary name
    + `X01` is set to a negative value or zero or an _UTF-8_ `\0` terminated string, which contains the module name
        + if `X01` is negative or zero the current module will be used
          + if there is no current module (the native code was not called by java code) the behavior is undefined
    + `X00` will be set to a reference of the class
        + if the class was previusly not loaded, it will be loaded by this operation
    + if the class could not be found `ERRNO` will be set to `ERR_JAVA_NO_SUCH_CLASS`
+ `offset=48=HEX-30` : _findInstanceMethod_
    + `X00` is a reference to a class object
    + `X01` is set to an _UTF-8_ `\0` terminated string, which contains the method name
    + `X02` is set to an _UTF-8_ `\0` terminated string, which contains the method descriptor
    + `X03` will be set to a _method-reference_
    + if no such method could be found `ERRNO` will be set to `ERR_JAVA_NO_SUCH_METHOD`
+ `offset=56=HEX-38` : _findStaticMethod_
    + `X00` is a reference to a class object
    + `X01` is set to an _UTF-8_ `\0` terminated string, which contains the method name
    + `X02` is set to an _UTF-8_ `\0` terminated string, which contains the method descriptor
    + `X03` will be set to a _method-reference_
    + if no such method could be found `ERRNO` will be set to `ERR_JAVA_NO_SUCH_METHOD`
+ `offset=64=HEX-40` : _findInstanceField_
    + `X00` is a reference to a class object
    + `X01` is set to an _UTF-8_ `\0` terminated string, which contains the field name
    + `X02` is set to an _UTF-8_ `\0` terminated string, which contains the field descriptor
    + `X03` will be set to a _field-reference_
    + if no such method could be found `ERRNO` will be set to `ERR_JAVA_NO_SUCH_FIELD`
+ `offset=72=HEX-48` : _findStaticField_
    + `X00` is a reference to a class object
    + `X01` is set to an _UTF-8_ `\0` terminated string, which contains the field name
    + `X02` is set to an _UTF-8_ `\0` terminated string, which contains the field descriptor
    + `X03` will be set to a _field-reference_
    + if no such method could be found `ERRNO` will be set to `ERR_JAVA_NO_SUCH_FIELD`
+ `offset=80=HEX-50` : _invokeInstance_
    + `X1E` is a reference to a _method-reference_
    + `X20` is set to a reference of the object instance which should be invoked
    + `X00` will be set to the return value of the method (if any)
    + when the method returns by throwing an exception `ERRNO` will be set to `ERR_JAVA_THROW`
        + otherwise it will be set to the previus value of `ERRNO` (the value it had when the method was invoced)
    + when the permission is denied `ERRNO` will be set to `ERR_JAVA_PERM`
+ `offset=88=HEX-58` : _invokeStatic_
    + `X1E` is a reference to a _method-reference_
    + `X00` will be set to the return value of the method (if any)
    + when the method returns by throwing an exception `ERRNO` will be set to `ERR_JAVA_THROW`
        + otherwise it will be set to the previus value of `ERRNO` (the value it had when the method was invoced)
    + when the permission is denied `ERRNO` will be set to `ERR_JAVA_PERM`
+ `offset=96=HEX-60` : _invokeSuper_
    + `X1E` is a reference to a _method-reference_
    + `X20` is set to a reference of the object instance which should be invoked
    + `X00` will be set to the return value of the method (if any)
    + when the method returns by throwing an exception `ERRNO` will be set to `ERR_JAVA_THROW`
        + otherwise it will be set to the previus value of `ERRNO` (the value it had when the method was invoced)
    + when the permission is denied `ERRNO` will be set to `ERR_JAVA_PERM`
+ `offset=104=HEX-68` : _new_
    + `X1E` is a reference to a _method-reference_
    + `X00` will be set to a reference of the newly created instance
    + when the method returns by throwing an exception `ERRNO` will be set to `ERR_JAVA_THROW`
        + otherwise it will be set to the previus value of `ERRNO` (the value it had when the method was invoced)
    + when the permission is denied `ERRNO` will be set to `ERR_JAVA_PERM`
+ `offset=112=HEX-70` : _getStaticField_
    + `X00` is a reference to a _field-reference_
    + `X01` will be set to the value of the field
    + when the permission is denied `ERRNO` will be set to `ERR_JAVA_PERM`
+ `offset=120=HEX-78` : _getInstanceField_
    + `X00` is a reference to a _field-reference_
    + `X01` is a reference of the object instance
    + `X02` will be set to the value of the field
    + when the permission is denied `ERRNO` will be set to `ERR_JAVA_PERM`
    + when the field belongs to a class which is no superclass of the object `ERRNO` will be set to `ERR_JAVA_CAST`
+ `offset=128=HEX-80` : _putStaticField_
    + `X00` is a reference to a _field-reference_
    + `X01` is set to the new value of the field
    + when the permission is denied `ERRNO` will be set to `ERR_JAVA_PERM`
+ `offset=136=HEX-88` : _putInstanceField_
    + `X00` is a reference to a _field-reference_
    + `X01` is a reference of the object instance
    + `X02` is set to the new value of the field
    + when the permission is denied `ERRNO` will be set to `ERR_JAVA_PERM`
    + when the field belongs to a class which is no superclass of the object `ERRNO` will be set to `ERR_JAVA_CAST`
+ `offset=144=HEX-90` : _createLocalReference_
    + `X00` is set to a reference or an already garbage collected reference (but not removed)
    + `X01` is set to a new local reference to the same object instance
    + if the given reference references an already garbage collected reference, it will be removed automatically and `X01` will be set to `-1`
    + when the native code returns all its local references will be removed. this includes references which where passed as arguments
+ `offset=152=HEX-98` : _createGlobalReference_
    + `X00` is set to a reference or an already garbage collected reference (but not removed)
    + `X01` is set to a new globl reference to the same object instance
    + if the given reference references an already garbage collected reference, it will be removed automatically and `X01` will be set to `-1`
    + global references live until they are explicitly removed
+ `offset=160=HEX-A0` : _createWeakReference_
    + `X00` is set to a reference or an already garbage collected reference (but not removed)
    + `X01` is set to a new weak reference to the same object instance
    + if the given reference references an already garbage collected reference, it will be removed automatically and `X01` will be set to `-1`
    + weak references live until they are explicitly or implicitly removed
    + weak references do not prevent their target from being garbage collected
        + when the target instance is garbage collected, the reference is like the special `null` (`-1`) value
+ `offset=168=HEX-A8` : _removeReference_
    + `X00` is set to a reference or an already garbage collected reference (but not removed)
    + this operation removes the given reference and makes any further use of it invalid
+ `offset=176=HEX-B0` : _ensureFrameSize_
    + `X00` is set to the minimum amount of needed local references
    + native code starts with a frame large enugh to hold 16 local references
        + after _pvm-java:INIT_ was executed, the initial native frame won't be large enugh to hold any local references
+ `offset=184=HEX-B8` : _growFrame_
    + `X00` is set to the amount of additional needed local references
    + native code starts with a frame large enugh to hold 16 local references
        + after _pvm-java:INIT_ was executed, the initial native frame won't be large enugh to hold any local references
+ `offset=192=HEX-C0` : _shrinkFrame_
    + `X00` is set to the amount of no longer needed local references
    + native code starts with a frame large enugh to hold 16 local references
        + after _pvm-java:INIT_ was executed, the initial native frame won't be large enugh to hold any local references

### java code
java code can use none _JNI-Env_ operations except of the following:
+ there are none

## Class Loading
### java code
classes are loaded/found using the `INT_LOAD_LIB` interrupt.    
all classes have to store the offset (as a 64-bit value) of their `<cinit>` method at the start of the file. (the offset is relative to the file-start)    
if a class has no `<cinit>` method the offset is instead set to `-1`    

### native code
native code can load/find classes via the _JNI-Env_.

## Method excecution
the [Local Variables](#local-variables) section describes how the local variables are stored during the execution of an java method.    
the [Operant Stack](#operant-stack) section describes how the translator 

### Local Variables
the first local variables are used like they was passed on method invokation (see [Arguments](#arguments)).
then the translator should try to use the other registers for the variables.    
if there are not enugh registers for all variables:
+ if the [Arguments](#arguments) where passed using a pointer stored in `X20` the memory block of the arguments is resized
+ otherwise a new memory block will be allocated and stored in `X00`.
    + the newly allocated memory block will be large enugh to store all local variables, which are not stored in registers
    + the memory block will be freed before the method returns

### Operant Stack
the _PVM_ has no operant stack. thus the operant stack is removed at translation time.    
the translator has to find out for what the operant stack is used for and then translate the given operations without the operant stack.

the order and effective usage of the following must remain:
+ method invocations
+ field (and array element) accesses/assignments/modifications

for the outside it must look like the original bytecode whould be interpreted

## Initialization
to start the java main method first _pvm-java#INIT_ has to be executed.    
after _pvm-java#INIT_ returned the native code can use the _JNI-Env_ to execute java code.    
additionally _pvm-java#MAIN_ can be used to execute the `main` method of the java code

### _pvm-java#INIT_
to initialize _pvm-java_ load `"/java/pvm-java"` and execute the function _pvm-java#INIT_:
1. load a copy of the file `"/java/pvm-java"` to RAM
2. store the start address of the copy in the `X00` register
3. execute the function at address `X00 + [X00]`

this can be done by using the `INT_LOAD_LIB` interrupt
``` 
MOV X00, {ADDRESS_OF "/java/pvm-java\0"}
XOR X01, X01 |> or MOV X01, 0
MOV X02, 1
INT INT_LOAD_FILE
```
using the `INT_LOAD_LIB` interrupt also reduces the possibility of having two java enviroments.

after _pvm-java#INIT_ returned the register `X1F` will be overwritten with the `JNI-Env` pointer.

## _pvm-java#MAIN_

after [_pvm-java#INIT_](#_pvm-java-init_) was executed _pvm-java#MAIN_ can be used to start the `main` method of the java code.

### _pvm-java#MAIN_ arguments
_pvm-java#MAIN_ accept the following arguments:
+ `X03` `argv` (`char##`) the arguments of the program
    + ignored when set to `0` or a negative value.
    + ignored when `argc` is set to `1`, `0` or a negative value
    + `argv` is points to an array of `UTF-8` encoded `\0` terminated strings
    + the first entry (`argv[0]`/`[X03]`) is ignored
    + `argc` is used to now how many entries are in `argv`
+ `X04` `argc` (`num`) the number of arguments of the program
    + see `argv`
+ `X05` `unloadMe` (`ubyte#`) the pointer to the initial binary which was loaded at the start of the `PVM`
    + ignored if set to `0` or a negative value
    + otherwies the `INT_UNLOAD_LIB` interrupt is executed to unload `unloadMe`
+ `X06` `defaultMain` (`char#`) the binary name of the main class which should be started if no class is specified by `argv`
    + ignored if set to `0` or a negative value
    + `defaultMain` points to an `UTF-8` encoded `\0` terminated string
+ `X07` `alwaysDefaultMain` (`num`) `0` if `argv` is allowed to overwrite `defaultMain` and any other value if not
    + ignored if `defaultMain` is ignored
    + if `alwaysDefaultMain` is set to `0` `argv` can specify which class should be used as main class
    + if `alwaysDefaultMain` is set to a value other than `0` `argv` will be converted to a `String[]` and passed to `defaultMain`
        + note that the first entry of `argv` is still ignored
+ `X08` `defMainModule` (`char#`) the name of the default main classes module
    + ignored if `defaultMain` is ignored
    + ignored if set to `0` or a negative value
    + `defMainModule` points to an `UTF-8` encoded `\0` terminated string

if both `argv` and `defaultMain` are ignored _pvm-java#MAIN_ will exit with a non-zero value.

### executing _pvm-java#MAIN_
to only execute _pvm-java#MAIN_ jump to the address which is calculated by adding `8` to the memory address of the "/java/pvm-java" file:
```
JMPO {ADDRESS_OF "/java/pvm-java\0"}, 8
```
_pvm-java#MAIN_ will never return, so there is no reason to store the curren `IP` on the stack.    
note that this will fail, because at least on of the arguments `argv` or `defaultMain` has to be passed

a file which only initializes and then executes _pvm-java#MAIN_ can look like:
```
~READ_SYM "[THIS]" #ADD~FOR_ME 1 >
~IF #~FOR_ME
    #OFF_LEA_DEF_MAIN 0
    #OFF_LEA_DEF_MODULE 0
    #OFF_LEA_PVM_JAVA 0
~ENDIF
MOV X05, IP |> directly move IP to X05, because at the start IP has no offset
MOV X03, X01 |> set argv
MOV X04, X00 |> set argc
#POS_LEA_DEF_MAIN --POS--
LEA X06, OFF_LEA_DEF_MAIN |> set defaultMain
#POS_LEA_DEF_MODULE --POS--
LEA X08, OFF_LEA_DEF_MODULE |> set defMainModule
MOV X07, 1 |> note that alwaysDefaultMain/X07 is initilized with 0, so this is only needed when alwaysDefaultMain should have a non-zero value
#POS_LEA_PVM_JAVA --POS--
|> set the args for INT_LOAD_LIB
LEA X00, OFF_LEA_PVM_JAVA |> offset of /java/pvm-java
XOR X01, X01 |> or MOV X01, 0 |> set initOff
MOV X02, 1 |> set initIsPntr
INT INT_LOAD_LIB |> first call pvm-java#INIT
JMPO X00, 8 |> jump to pvm-java#MAIN
$not-align |> UTF-8 strings do not need to be 64-bit aligned
#EXP~OFF_LEA_PVM_JAVA (--POS-- - POS_LEA_PVM_JAVA)
: CHARS 'UTF-8' "/java/pvm-java\0" >
#EXP~OFF_LEA_DEF_MAIN (--POS-- - POS_LEA_DEF_MAIN)
: CHARS 'UTF-8' "my/def/ault/Main\0" > |> only needed if defaultMain should be set
#EXP~OFF_LEA_DEF_MODULE (--POS-- - POS_LEA_DEF_MODULE)
: CHARS 'UTF-8' "my.module\0" > |> only needed if defMainModule should be set
```
