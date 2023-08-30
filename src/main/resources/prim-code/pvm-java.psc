~IF #~ME
	~READ_SYM "constants.psf" >
	#pvm_java_INIT 0
	#pvm_java_MAIN 0
	#pvm_java_INIT_ADD_POS 0
~ELSE
	~READ_SYM "[THIS]" --MY_CONSTS-- #ADD~ME 1 >
~ENDIF
:
	pvm_java_INIT
	pvm_java_MAIN
>

|:	native object references:
|	:
|		|> negative if weak-reference and already gc
|		num object_address
|	>
|:>

#native_throw_pos --POS--
: -1 >

|:	X00 stores a reference to the exception instance which should been thrown
|	ERRNO will be set to ERR_JAVA_THROW
|	if there is already an exception instance which is being 
|:>
#JNI_Env_throw ( --POS-- - JNI_Env_ADD_POS )
	MOV ERRNO, ERR_JAVA_THROW
	#REL_POS ( native_throw_pos - --POS-- )
	MOV [IP + REL_POS], X00
	@return
	RET
|> TODO other JNI-Env functions

#JNI_Env_POS --POS--
:
	JNI_Env_throw
|> TODO other JNI-Env functions
>
#JNI_Env_LEN ( --POS-- - JNI_Env_POS )

#EXP~pvm_java_INIT --POS--
	#REL_POS ( JNI_Env_POS - --POS-- )
	LEA X1F, REL_POS
	PUSH X00
	XOR X00, X00
	@pvm_java_INIT_JNI_env_loop
	~IF #~ME
		#EXP~JNI_Env_ADD_POS --POS--
	#ENDIF
	ADD [X1F + X00], IP
	ADD X00, 8
	CMP X00, JNI_Env_LEN
	JMPLT pvm_java_INIT_JNI_env_loop
	POP X00
	RET

#EXP~pvm_java_MAIN --POS--
	|> TODO implement
