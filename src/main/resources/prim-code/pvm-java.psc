|> This file is part of the java-2-prim Project
|> DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
|> Copyright (C) 2023 Patrick Hechler
|>
|> This program is free software: you can redistribute it and/or modify
|> it under the terms of the GNU General Public License as published by
|> the Free Software Foundation, either version 3 of the License, or
|> (at your option) any later version.
|>
|> This program is distributed in the hope that it will be useful,
|> but WITHOUT ANY WARRANTY; without even the implied warranty of
|> MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
|> GNU General Public License for more details.
|>
|> You should have received a copy of the GNU General Public License
|> along with this program. If not, see <https://www.gnu.org/licenses/>.

~IF #~ME
	#pvm_java_INIT 0
	#pvm_java_MAIN 0
	#pvm_java_INIT_ADD_POS 0
	#JNI_Env_ADD_POS 0
~ELSE
	~READ_SYM "constants.psf" >
	~READ_SYM "internal-structures.psf" >
	~READ_SYM "[THIS]" --MY_CONSTS-- #ADD~ME 1 >
~ENDIF
:
	pvm_java_INIT
	pvm_java_MAIN
>

|>	hash set
|:	struct hs_list {
|		unum entries_size; // note that size contains the size (in bytes) of the entries array
|		num[] entries;
|	}
|:>
|:	struct hashset {
|		num# entries; // if negative the set is empty     // off = HEX-0
|		unum maxi;                                        // off = HEX-8
|		unum entry_count;                                 // off = HEX-10
|		func (num a, num b) --> <num equal> equalizer;    // off = HEX-18
|			X03: c --> c
|			X04: a --> equal (0 = not-equal)
|			X05: b --> b
|			X06..X0F: n --> n // both inclusive
|		func (num val) --> <unum hash> hashmaker;         // off = HEX-20
|			X03: c --> c
|			X04: val --> hash
|			X05..X0F: n --> n // both inclusive
|		// TODO check if X00..X02 are modifiable for equalizer/hashmaker
|	}
|:>
#EXP~hashset_maxi_OFF HEX-8
#EXP~hashset_entry_count_OFF HEX-10
#EXP~hashset_equalizer_OFF HEX-18
#EXP~hashset_hashmaker_OFF HEX-20
#EXP~hashset_SIZE HEX-28
|:	func hashset_get(struct hashset# set, unum hash, num val) --> <struct hashset# set_, num result>
|	X03: set --> set
|	X04: hash --> result (negative: not found; other: value)
|	X05: val --> ?
|	X06, X07: --> ?
|	other registers may also be modified if set#.equalizer modifies them
|:>
#EXP~hashset_get_POS --POS--
	MOV X07, [X03]
	SGN X07
	JMPLE returnX04_m1
	AND X04, [X03 + hashset_maxi_OFF]
	LSH X04, 3
	MOV X04, [X07 + X04]
	CMP X04, -1
	JMPGT hs_get_check_result
	JMPEQ return
	NOT X04
	MOV X06, X04
	MOV X07, [X06]
	@hs_get_list_loop
		MOV X04, [X06 + X07]
		CALNO [X03 + hashset_equalizer_OFF]
		JMPERR return
		SGN X04
		JMPNE hs_get_list_loop_found
		SUB X07, 8
		JMPZC hs_get_list_loop
		MOV X04, -1
		RET
		@hs_get_list_loop_found
			MOV X04, [X06 + X07]
			RET
	@hs_get_check_result
		CALNO [X03 + hashset_equalizer_OFF]
		JMPERR return
		SGN X04
		JMPEQ returnX04_m1
		MOV X04, X06
		RET
	@returnX04_m1
		MOV X04, -1
		RET

|:	func hashset_add(struct hashset# set, unum hash, num addval) --> <struct hashset# set_, num value>
|	X00 .. X02 --> ?
|	X03: set --> set_
|	X04: hash --> oldval
|	X05: newval --> ?
|	X06 .. X0F --> ?
|	other registers may also be modified if set#.equalizer or set#.hashmaker modifies them
|:>
#EXP~hashset_add_POS --POS--
	MOV X07, 1
	JMP hashset_add_put_impl
|:	func hashset_put(struct hashset# set, unum hash, num newval) --> <struct hashset# set_, num oldval>
|	X00 .. X02 --> ?
|	X03: set --> set_
|	X04: hash --> oldval
|	X05: putval --> ?
|	X06 .. X0F --> ?
|	other registers may also be modified if set#.equalizer or set#.hashmaker modifies them
|:>
#EXP~hashset_put_POS --POS--
	XOR X07, X07
@hashset_add_put_impl
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|:>
	MOV X06, [X03]
	SGN X06
	JMPLE hs_add_put__create_set
	MOV X08, [X03 + hashset_maxi_OFF]
	RLSH X08, 1
	MOV X09, [X03 + hashset_entry_count_OFF]
	CMP X08, X09
	JMPGT hs_add_put__no_grow
	LSH X08, 2
	OR X08, 3
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: new set#.maxi ((set#.maxi << 1) | 1)
	|	X09: set#.entry_count
	|:>
	MVAD X00, X08, 1
	LSH X00, 3
	MOV X01, HEX-FF
	MOV X02, X00
	INT INT_MEMORY_ALLOC
	JMPERR return
	INT INT_MEM_BSET
	MOV X0A, X00
	MVAD X09, X08, -8
	RLSH X09, 1
	MOV X0B, X04
	|:	X03: set
	|	X04: -
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: new set#.maxi ((set#.maxi << 1) | 1)
	|	X09: loop offset
	|	X0A: new set#.entries
	|	X0B: hash
	|:>
	@hs_add_put__grow_loop
		MOV X0C, [X06 + X09]
		CMP X0C, -1
		JMPEQ hs_add_put__grow_loop__fin_iter
		JMPLT hs_add_put__grow_loop__list
		MOV X04, X0C
		CALNO [X03 + hashset_hashmaker_OFF]
		JMPERR hs_resize__free_not_new
		AND X04, X08
		LSH X04, 3
		MOV X0D, [X0A + X04]
		CMP X0D, -1
		JMPGT hs_add_put__grow_loop__entry__create_list
		JMPLT hs_add_put__grow_loop__entry__add_list
		|:	X03: set
		|	X04: new offset of current loop entry
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: new set#.maxi ((set#.maxi << 1) | 1)
		|	X09: loop offset
		|	X0A: new set#.entries
		|	X0B: hash
		|	X0C: current loop entry ([X06 + X09])
		|	X0D: (new set#.entries)[current loop entries index] ([X0A + X04])
		|:>
		MOV [X0A + X04], X0C
		JMP hs_add_put__grow_loop__fin_iter
		|:	X08: (not new) set#.maxi
		|	X0A: (not new) set#.entries
		|	frees all lists in the not new set#.entries and the not new set#.entries; then returns to caller
		|	modifies:
		|		X00, X08
		|		(does not modify the set)
		|:>
		@hs_resize__free_not_new |> note that this function is also used by hs_rem_shrink
			LSH X08, 3
			@hs_resize__free_not_new_loop
				MOV X00, [X0A + X08]
				CMP X00, -1
				JMPGE hs_resize__free_not_new_loop__iter_end
				NOT X00
				INT INT_MEMORY_FREE
				@hs_resize__free_not_new_loop__iter_end
					USUB X08, 8
					JMPCC hs_resize__free_not_new_loop
			MOV X00, X0A
			INT INT_MEMORY_FREE
			RET
		|:	X03: set
		|	X04: new offset of current loop entry
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: new set#.maxi ((set#.maxi << 1) | 1)
		|	X09: loop offset
		|	X0A: new set#.entries
		|	X0B: hash
		|	X0C: current loop entry ([X06 + X09])
		|	X0D: (new set#.entries)[current loop entries index] ([X0A + X04])
		|:>
		@hs_add_put__grow_loop__entry__create_list
			MOV X00, 32
			INT INT_MEMORY_ALLOC
			JMPERR hs_resize__free_not_new
			MOV [X00], 16
			MOV [X00 + 8], X0D
			MOV [X00 + 16], X0C
			NOT X00
			MOV [X0A + X04], X00
			JMP hs_add_put__grow_loop__fin_iter
		|:	X03: set
		|	X04: new offset of current loop entry
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: new set#.maxi ((set#.maxi << 1) | 1)
		|	X09: loop offset
		|	X0A: new set#.entries
		|	X0B: hash
		|	X0C: current loop entry ([X06 + X09])
		|	X0D: (new set#.entries)[current loop entries index] ([X0A + X04]) (logical not of a hs_list)
		|:>
		@hs_add_put__grow_loop__entry__add_list
			NOT X0D
			ADD [X0D], 8
			MOV X0E, [X0D]
			MVAD X00, X0E, -1
			BCP X0E, X00
			JMPSB hs_add_put__grow_loop__entry__add_list__no_list_grow
				MOV X00, X0D
				MOV X01, X0E
				LSH X01, 1
				INT INT_MEMORY_REALLOC |> on error undo the increase list size is not needed, the list is freed
				JMPERR hs_resize__free_not_new
				MOV X0D, X00
				NOT X00
				MOV [X0A + X04], X00
			@hs_add_put__grow_loop__entry__add_list__no_list_grow
			MOV [X0D + X0E], X0C
			JMP hs_add_put__grow_loop__fin_iter
		|:	X03: set
		|	X04: -
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: new set#.maxi ((set#.maxi << 1) | 1)
		|	X09: loop offset
		|	X0A: new set#.entries
		|	X0B: hash
		|	X0C: current loop entry ([X06 + X09])
		|:>
		@hs_add_put__grow_loop__list
			NOT X0C
			MOV X0D, [X0C]
			|:	X03: set
			|	X04: -
			|	X05: newval
			|	X06: set#.entries
			|	X07: is_add
			|	X08: new set#.maxi ((set#.maxi << 1) | 1)
			|	X09: loop offset
			|	X0A: new set#.entries
			|	X0B: hash
			|	X0C: current loop entry ([X06 + X09])
			|	X0D: current loop entry list offset (inner loop offset)
			|:>
			@hs_add_put__grow_loop__list__loop
				MOV X0E, [X0C + X0D]
				MOV X04, X0E
				CALNO [X03 + hashset_hashmaker_OFF]
				JMPERR hs_resize__free_not_new
				AND X04, X08
				LSH X04, 3
				MOV X0F, [X0A + X04]
				CMP X0F, -1
				|:	X03: set
				|	X04: offset in new set#.entries
				|	X05: newval
				|	X06: set#.entries
				|	X07: is_add
				|	X08: new set#.maxi ((set#.maxi << 1) | 1)
				|	X09: loop offset
				|	X0A: new set#.entries
				|	X0B: hash
				|	X0C: current loop entry ([X06 + X09])
				|	X0D: current loop entry list offset (inner loop offset)
				|	X0E: current inner loop entry ([X0C + X0D]) (add this to the new entries)
				|	X0F: (new set#.entries)[index in new set#.entries] ([X0A + X04])
				|:>
				JMPGT hs_add_put__grow_loop__list__loop__create_list
				JMPLT hs_add_put__grow_loop__list__loop__add_list
				MOV [X0A + X04], X0E
				JMP hs_add_put__grow_loop__list__loop__iter_end
				@hs_add_put__grow_loop__list__loop__add_list
					NOT X0F
					ADD [X0F], 8
					MOV X03, [X0F]
					MVAD X02, X03, -1
					BCP X03, X02
					JMPSB hs_add_put__grow_loop__list__loop__add_list__no_grow
						MOV X01, X03
						LSH X01, 1
						MOV X00, X0F
						INT INT_MEMORY_REALLOC
						JMPERR hs_resize__free_not_new |> no need to shrink list, it is freed the same way
						MOV X0F, X00
						NOT X00
						MOV [X0A + X04], X00
					@hs_add_put__grow_loop__list__loop__add_list__no_grow
					MOV [X0F + X03], X0E
					JMP hs_add_put__grow_loop__list__loop__iter_end
				@hs_add_put__grow_loop__list__loop__create_list
					MOV X00, 32
					INT INT_MEMORY_ALLOC
					JMPERR hs_resize__free_not_new
					MOV [X00], 16
					MOV [X00 + 8], X0F
					MOV [X00 + 16], X0E
					NOT X00
					MOV [X0A + X04], X00
				@hs_add_put__grow_loop__list__loop__iter_end
					SUB X0D, 8
					JMPZC hs_add_put__grow_loop__list__loop
		@hs_add_put__grow_loop__fin_iter
			USUB X09, 8
			JMPCC hs_add_put__grow_loop
	|:	X03: set
	|	X04: -
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: new set#.maxi ((set#.maxi << 1) | 1)
	|	X09: loop offset
	|	X0A: new set#.entries
	|	X0B: hash
	|:>
	MOV X04, X0B
	MOV X0B, X08
	SWAP [X03 + hashset_maxi_OFF], X0B
	MOV [X03], X0A
	SWAP X0A, X06
	MOV X09, [X03 + hashset_entry_count_OFF]
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: set#.maxi
	|	X09: set#.entry_count
	|	X0A: old set#.entries
	|	X0B: old set#.maxi
	|:>
	LSH X0B, 3
	@hs_add_put__free_old_loop
		MOV X00, [X0A + X0B]
		CMP X00, -1
		JMPGE hs_add_put__free_old_loop__fin_iter
		NOT X00
		INT INT_MEMORY_FREE
		@hs_add_put__free_old_loop__fin_iter
			USUB X0B, 8
			JMPCC hs_add_put__free_old_loop
	MOV X00, X0A
	INT INT_MEMORY_FREE
	AND X04, X08
	JMP hs_add_put__after_grow
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|	X08: set#.maxi >>> 1
	|	X09: set#.entry_count
	|:>
	@hs_add_put__no_grow
		AND X04, [X03 + hashset_maxi_OFF]
	@hs_add_put__after_grow
		LSH X04, 3
		MOV X0A, [X06 + X04]
		CMP X0A, -1
		JMPGT hs_add_put__check_entry
		JMPLT hs_add_put__check_list
		MOV [X06 + X04], X05
		MOV X04, -1
		RET
		@hs_add_put__check_entry
			MOV X0B, X04
			MOV X04, X0A
			CALNO [X03 + hashset_equalizer_OFF]
			JMPERR return
			SGN X04
			JMPNE hs_add_put__check_entry__found
			MOV X00, 32
			INT INT_MEMORY_ALLOC
			JMPERR return
			MOV [X00], 8
			MOV [X00 + 8], X0A
			MOV [X00 + 16], X05
			NOT X00
			MOV [X06 + X04], X00
			INC [X03 + hashset_entry_count_OFF]
			MOV X04, -1
			RET
			@hs_add_put__check_entry__found
				SGN X07
				JMPEQ hs_add_put__check_entry__found__only_add
				MOV [X06 + X0B], X05
				@hs_add_put__check_entry__found__only_add
					MOV X04, X0A
					RET
		|:	X03: set
		|	X04: offset of the element ((newval.hash & set#.maxi) << 3)
		|	X05: newval
		|	X06: set#.entries
		|	X07: is_add
		|	X08: -
		|	X09: set#.entry_count
		|	X0A: set#.entries[newval.hash & set#.maxi] ([X06 + X04])
		|:>
		@hs_add_put__check_list
			NOT X0A
			MOV X08, [X0A]
			MOV X0B, X04
			@hs_add_put__check_list__loop
				MOV X04, [X0A + X08]
				CALNO [X03 + hashset_equalizer_OFF]
				JMPERR return
				SGN X04
				JMPNE hs_add_put__check_list__loop__found
				@hs_add_put__check_list__loop__iter_end
					SUB X08, 8
					JMPZC hs_add_put__check_list__loop
			ADD [X0A], 8
			MOV X01, [X0A]
			MVAD X02, X01, -1
			BCP X01, X02
			JMPSB hs_add_put__check_list__no_realloc
				MOV X00, X0A
				INT INT_MEMORY_REALLOC
				JMPERR return
				MOV X0A, X00
				NOT X00
				MOV [X06 + X0B], X00
			@hs_add_put__check_list__no_realloc
			MOV [X0A + X01], X05
			MOV X04, -1
			INC [X03 + hashset_entry_count_OFF]
			RET
			|:	X03: set
			|	X04: -
			|	X05: newval
			|	X06: set#.entries
			|	X07: is_add
			|	X08: current offset in list
			|	X09: set#.entry_count
			|	X0A: set#.entries[newval.hash & set#.maxi] ([X06 + X04])
			|	X0B: offset of the element ((newval.hash & set#.maxi) << 3)
			|:>
			@hs_add_put__check_list__loop__found
				SGN X07
				JMPEQ return
				SGN X07
				JMPNE hs_add_put__check_list__loop__found__only_add
				MOV X04, X05
				SWAP [X0A + X08], X04
				RET
				@hs_add_put__check_list__loop__found__only_add
					MOV X04, [X0A + X08]
					RET
	|:	X03: set
	|	X04: hash
	|	X05: newval
	|	X06: set#.entries
	|	X07: is_add
	|:>
	@hs_add_put__create_set
		MOV X00, HEX-40 |> 64 bytes : 8 entries
		MOV X01, HEX-FF
		MOV X02, X00
		INT INT_MEMORY_ALLOC
		JMPERR return
		INT INT_MEM_BSET
		AND X04, 7 |> calc offset ((hash & (8-1)) << 3)
		LSH X04, 3
		MOV [X00 + X04], X05 |> set value
		MOV [X03], X00
		MOV [X03 + hashset_maxi_OFF], 7
		MOV [X03 + hashset_entry_count_OFF], 1
		@return_X04_m1
			MOV X04, -1 |> there was no old value
			RET

|:	func hashset_remove(struct hashset# set, unum hash, num oldval) --> <struct hashset# set_, num oldval>
|	X00 .. X02 : --> ?
|	X03: set --> set_
|	X04: hash --> oldval
|	X05: remval --> ?
|	X06 .. X0E : --> ?
|:>
#EXP~hashset_remove_POS --POS--
	|:	X03: set
	|	X04: hash
	|	X05: remval
	|	X06: set#.entries
	|:>
	MOV X06, [X03]
	SGN X06
	JMPLE return_X04_m1
	AND X04, [X03 + hashset_maxi_OFF]
	LSH X04, 3
	MOV X07, [X03 + X04]
	CMP X07, -1
	JMPGT hs_rem__check_entry
	JMPEQ return_X04_m1
	NOT X07
	MOV X08, [X07]
	MOV X09, X04
	|:	X03: set
	|	X04: -
	|	X05: remval
	|	X06: set#.entries
	|	X07: list
	|	X08: list offset
	|	X09: offset
	|:>
	@hs_rem__check_list__loop
		MOV X04, [X07 + X08]
		CALNO [X03 + hashset_equalizer_OFF]
		JMPERR return
		SGN X04
		JMPNE hs_rem__check_list__loop__found
		SUB X08, 8
		JMPZC hs_rem__check_list__loop
	MOV X04, -1
	RET
		|:	X03: set
		|	X04: -
		|	X05: remval
		|	X06: set#.entries
		|	X07: list
		|	X08: list offset
		|	X09: offset
		|:>
		@hs_rem__check_list__loop__found
			MOV X04, [X07 + X08]
			MOV X02, [X07]
			CMP X02, 16
			JMPEQ hs_rem__check_list__loop__found__remove_list
			JMPLT hs_rem__check_list__loop__found__remove_entire_list
				SUB X02, X08
				SUB X02, 8
				JMPZS hs_rem__check_list__loop__found__shrink_list__no_mem_move
					MOV X01, X07 |> skip if the last entry is removed
					ADD X01, X08
					MVAD X00, X07, 8
					ADD X00, X08
					INT INT_MEM_MOV
				@hs_rem__check_list__loop__found__shrink_list__no_mem_move
				SUB [X07], 8
				MOV X01, [X07]
				MVAD X02, X01, -1
				BCP X02, X01
				JMPSB hs_rem_shrink
				ADD X01, 8
				MOV X00, X07
				INT INT_MEMORY_REALLOC
				NOT X00
				MOV [X06 + X09], X00
				JMP hs_rem_shrink
			@hs_rem__check_list__loop__found__remove_list
				CMP X08, 8
				JMPEQ hs_rem__check_list__loop__found__remove_list__off_is_8
				MOV X08, 8
				JMP hs_rem__check_list__loop__found__remove_list__new_off
				@hs_rem__check_list__loop__found__remove_list__off_is_8
					MOV X08, 16
				@hs_rem__check_list__loop__found__remove_list__new_off
				MOV [X06 + X09], [X07 + X08]
				MOV X00, X07
				INT INT_MEMORY_FREE
				JMP hs_rem_shrink
			@hs_rem__check_list__loop__found__remove_entire_list
				|> this should never happen
				MOV [X06 + X09], -1
				MOV X00, X07
				INT INT_MEMORY_FREE
				JMP hs_rem_shrink
	|:	X03: set
	|	X04: offset
	|	X05: remval
	|	X06: set#.entries
	|	X07: entry
	|:>
	@hs_rem__check_entry
		SWAP X04, X07
		CALNO [X03 + hashset_equalizer_OFF]
		JMPERR return
		SGN X04
		JMPEQ return_X04_m1
		MOV X04, -1
		SWAP [X03 + X07], X04
	|:	X03: set
	|	X04: oldvalue
	|	X05: -
	|	X06: set#.entries
	|	returns to caller, after possibly shriking the set
	|:>
	@hs_rem_shrink
		DEC [X03 + hashset_entry_count_OFF]
		MOV X05, [X03 + hashset_entry_count_OFF]
		MOV X07, [X03 + hashset_maxi_OFF]
		RLSH X07, 2
		CMP X07, X05
		JMPLT return
		SGN X05
		JMPEQ hs_rem_shrink__free_all
		MOV X08, [X03 + hashset_maxi_OFF]
		RLSH X08, 1
		MOV X00, X08
		LSH X00, 3
		MOV X01, HEX-FF
		MOV X02, X00
		INT INT_MEMORY_ALLOC
		JMPERR return
		INT INT_MEM_BSET
		MOV X0A, X00
		|:	X03: set
		|	X04: oldvalue
		|	X05: set#.entry_count
		|	X06: set#.entries
		|	X07: set#.maxi >>> 2
		|	X08: new set#.maxi
		|	X09: -
		|	X0A: new set#.entries
		|:>
		LSH X07, 5
		OR X07, BIN-10000 |> X07 <-- ( X07 | BIN-11000 ) - 8 // ( set#.maxi << 3 ) - 8
		MOV X0B, X04
		|:	X03: set
		|	X04: -
		|	X05: set#.entry_count
		|	X06: set#.entries
		|	X07: loop/set#.entries offset (start: (set#.maxi << 3) - 8)
		|	X08: new set#.maxi
		|	X09: -
		|	X0A: new set#.entries
		|	X0B: oldvalue
		|:>
		@hs_rem_shrink__loop
			MOV X09, [X06 + X07]
			CMP X09, -1
			JMPEQ hs_rem_shrink__loop_iter_end
			JMPLT hs_rem_shrink__loop__list
				MOV X04, X09
				CALNO [X03 + hashset_hashmaker_OFF]
				JMPERR hs_resize__free_not_new
				AND X04, X0A
				LSH X04, 3
				MOV X0C, [X0A + X04]
				CMP X0C, -1
				JMPLT hs_rem_shrink__loop__value__to_list
				JMPGT hs_rem_shrink__loop__value__to_value
				MOV [X0A + X04], X04
			@hs_rem_shrink__loop_iter_end
				USUB X07, 9
				JMPCC hs_rem_shrink__loop
		SWAP [X03], X0A
		SWAP [X03 + hashset_maxi_OFF], X08
		JMP hs_resize__free_not_new |> well, it frees the old
				@hs_rem_shrink__loop__value__to_value
					MOV X00, 32
					INT INT_MEMORY_ALLOC
					JMPERR hs_resize__free_not_new
					MOV [X00], 16
					MOV [X00 + 8], X0C
					MOV [X00 + 16], X09
					NOT X00
					MOV [X0A + X04], X00
					JMP hs_rem_shrink__loop_iter_end
				@hs_rem_shrink__loop__value__to_list
					NOT X0C
					ADD [X0C], 8
					MOV X01, [X0C]
					MVAD X00, X01, -1
					BCP X01, X00
					JMPSB hs_rem_shrink__loop__value__to_list__no_list_grow
						MOV X00, X0C
						LSH X01, 1
						INT INT_MEMORY_REALLOC |> on error undo the decrease list size is not needed
						JMPERR hs_resize__free_not_new
						MOV X0C, X00
						NOT X00
						MOV [X0A + X04], X00
						MOV X01, [X0C]
					@hs_rem_shrink__loop__value__to_list__no_list_grow
					MOV [X0C + X01], X09
					JMP hs_rem_shrink__loop_iter_end
			|:	X03: set
			|	X04: -
			|	X05: set#.entry_count
			|	X06: set#.entries
			|	X07: loop/set#.entries offset (start: (set#.maxi << 3) - 8)
			|	X08: new set#.maxi
			|	X09: current entry (a list) to be added to new set#.entries (X0A)
			|	X0A: new set#.entries
			|	X0B: oldvalue
			|:>
			@hs_rem_shrink__loop__list
				NOT X09
				MOV X0C, [X09]
				@hs_rem_shrink__loop__list__loop
					MOV X0D, [X09 + X0D]
					MOV X04, X0D
					CALNO [X03 + hashset_hashmaker_OFF]
					AND X04, X08
					LSH X04, 3
					MOV X0E, [X0A + X04]
					CMP X0E, -1
					JMPEQ hs_rem_shrink__loop__list__loop__to_empty
					JMPLT hs_rem_shrink__loop__list__loop__to_list
						MOV X00, 32
						INT INT_MEMORY_ALLOC
						JMPERR hs_resize__free_not_new
						MOV [X00], 16
						MOV [X00 + 8], X0E
						MOV [X00 + 16], X0D
						NOT X00
						MOV [X0A + X04], X00
						JMP hs_rem_shrink__loop__list__loop_iter_end
					|:	X03: set
					|	X04: offset in new set#.entries (X0A) of X0D/X0E
					|	X05: set#.entry_count
					|	X06: set#.entries
					|	X07: loop/set#.entries offset (start: (set#.maxi << 3) - 8)
					|	X08: new set#.maxi
					|	X09: current entry (a list) to be added to new set#.entries (X0A)
					|	X0A: new set#.entries
					|	X0B: oldvalue
					|	X0C: offset in the list
					|	X0D: current list entry to be added to new set#.entries (X0A)
					|	X0E: value in new set#.entries (X0A) at the place where X0C should be inserted
					|:>
					@hs_rem_shrink__loop__list__loop__to_list
						NOT X0E
						ADD [X0E], 8
						MOV X01, [X0E]
						MVAD X00, X01, -1
						BCP X01, X00
						JMPSB hs_rem_shrink__loop__list__loop__to_list__no_list_grow
							MOV X00, X0E
							LSH X01, 1
							INT INT_MEMORY_REALLOC |> on error undo the decrease list size is not needed
							JMPERR hs_resize__free_not_new
							MOV X0E, X00
							NOT X00
							MOV [X0A + X04], X00
							MOV X01, [X0E]
						@hs_rem_shrink__loop__list__loop__to_list__no_list_grow
						MOV [X0E + X01], X0D
						JMP hs_rem_shrink__loop__list__loop_iter_end
					@hs_rem_shrink__loop__list__loop__to_empty
						MOV [X0A + X04], X0D
					@hs_rem_shrink__loop__list__loop_iter_end
						SUB X0C, 8
						JMPZC hs_rem_shrink__loop__list__loop
				JMP hs_rem_shrink__loop__list__loop_iter_end
		@hs_rem_shrink__free_all
			MOV X00, X06
			INT INT_MEMORY_FREE
			MOV [X03], -1
			MOV [X03 + hashset_maxi_OFF], 0
			RET

|:	func hashset_for_each(struct hashset# set, addr consumer) --> <struct hashset# set_, addr consumer_>
|	X03: set --> set_
|	X04: addr (struct hashset# set, addr consumer, num val) --> <struct hashset# set_, addr consumer_> consumer
|		X03: set --> set_
|		X04: consumer --> consumer_
|		X05: val --> ?
|		X06 .. X09: n --> n
|	X05 .. X09 : --> ?
|	note that this function ignores the ERRNO register
|	this function does not need to do any cleanup and does not use the stack for anything except by using `RET`
|		to stop the iteration the consumer can execute `SUB SP, 8   RET` (or `PUSH XNN   RET`)
|:>
#EXP~hashset_for_each_POS --POS--
	MOV X06, [X03]
	SGN X06
	JMPLE return
	|:	X03: set
	|	X04: consumer
	|	X05: -
	|	X06: set#.entries
	|	X07: offset of set#.entries
	|:>
	MOV X07, [X03 + hashset_maxi_OFF]
	LSH X07, 3
	@hs_for_each__loop
		MOV X05, [X06 + X07]
		CMP X05, -1
		JMPEQ hs_for_each__loop_iter_end
		JMPLT hs_for_each__loop__list
		CALNO X04
		@hs_for_each__loop_iter_end
			USUB X07, 8
			JMPCC hs_for_each__loop
			RET
		@hs_for_each__loop__list
			MOV X08, X05
			NOT X08
			MOV X09, [X08]
			@hs_for_each__loop__list__loop
				MOV X05, [X08 + X09]
				CALNO X04
				@hs_for_each__loop__list__loop_iter_end
					SUB X09, 8
					JMPZC hs_for_each__loop__list__loop
			JMP hs_for_each__loop_iter_end


#native_throw_POS --POS--
: -1 >
#native_exec_class_POS --POS--
: -1 >
#local_reference_size_POS --POS--
: 0 >
#local_references_adr_POS --POS--
: -1 >

#modules_set_POS --POS--
:
	-1 |> entries
	0  |> maxi
	0  |> entry_count
	0  |> equalizer
	0  |> hashmaker
>

|:		func (num a, num b) --> <num equal> equalizer;    // off = HEX-18
|			X03: c --> c
|			X04: a --> equal (0 = not-equal)
|			X05: b --> b
|			X06..X0F: n --> n // both inclusive
|:>
#hs_modules_equal --POS--
	|> TODO implement

|:		func (num val) --> <unum hash> hashmaker;         // off = HEX-20
|			X03: c --> c
|			X04: val --> hash
|			X05..X0F: n --> n // both inclusive
|:>
#hs_modules_hash --POS--
	|> TODO implement

|:	creates a new local reference and let it refer to the X00 value
|	if needed the current frame will grow
|	parameter: X00: points to the instance
|	results:
|		X00: the local-reference on success otherwise set to -1
|		ERRNO: unmodified on success otherwise set to a non-zero value
|	modifies: X01, X02
|:>
@add_local_reference
	#REL_POS ( local_reference_size_POS - --POS-- )
	MOV X01, [IP + REL_POS]
	SGN X01
	JMPEQ create_local_frame
	#REL_POS ( local_references_adr_POS - --POS-- )
	MOV X02, [IP + REL_POS]
	@add_local_reference_loop
		SUB X01, 8
		SGN [X02 + X01]
		JMPLT found_free_reference
		SGN X01
		JMPGT add_local_reference_loop
	|> no unused ref found, grow by 4 references
	ADD X01, 32
	SWAP X00, X02
	INT INT_MEMORY_REALLOC
	SGN X00 |> do not use JMPERR, since ERRNO is allowed to be set to a non-zero value when this function is called
	JMPLE returnX00_m1
	#REL_POS ( local_references_adr_POS - --POS-- )
	MOV [IP + REL_POS], X00
	#REL_POS ( local_reference_size_POS - --POS-- )
	MOV [IP + REL_POS], X01
	ADD X00, X01
	SUB X00, 32
	MOV [X00], X02
	RET
	@found_free_reference
		MOV [X02 + X01], X00
		MOV X02, X00
		ADD X00, X01
		RET
	@create_local_frame
		MOV X01, X00
		MOV X00, 128
		INT INT_MEMORY_ALLOC
		SGN X00
		JMPLE returnX00_m1
		#REL_POS ( local_references_adr_POS - --POS-- )
		MOV [IP + REL_POS], X00
		#REL_POS ( local_reference_size_POS - --POS-- )
		MOV [IP + REL_POS], 128
		MOV [X00], X01
		RET


|:	X00 stores a reference to the exception instance which should been thrown
|	ERRNO will be set to ERR_JAVA_THROW
|	if there is already an exception instance which is being 
|:>
#JNI_Env_throw ( --POS-- - JNI_Env_ADD_POS )
	MOV ERRNO, ERR_JAVA_THROW
	#REL_POS ( native_throw_POS - --POS-- )
	MOV [IP + REL_POS], [X00]
	@return
	RET

#JNI_Env_isThrowing ( --POS-- - JNI_Env_ADD_POS )
	#REL_POS ( native_throw_POS - --POS-- )
	SGN [IP + REL_POS]
	JMPLT returnX00_0
	MOV X00, 1
	RET
	@returnX00_0
		XOR X00, X00
		RET

#JNI_Env_getThrowingClass ( --POS-- - JNI_Env_ADD_POS )
	#REL_POS ( native_throw_POS - --POS-- )
	MOV X00, [IP + REL_POS]
	SGN X00
	JMPLT returnX00_m1
	MOV X00, [X00]
	JMP add_local_reference
	@returnX00_m1
		MOV X00, -1
		RET

#JNI_Env_catch ( --POS-- - JNI_Env_ADD_POS )
	MOV X00, -1
	#REL_POS ( native_throw_POS - --POS-- )
	SWAP X00, [IP + REL_POS]
	SGN X00
	JMPLT return
	JMP add_local_reference

#JNI_Env_findModule ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_findClass ( --POS-- - JNI_Env_ADD_POS )
	SGN X02
	JMPGT JNI_Env_findClass_module_known
	#REL_POS ( native_exec_class_POS - --POS-- )
	MOV X02, [IP + REL_POS]
	MOV X02, [X02 + object_class_instance_module_OFF]
	@JNI_Env_findClass_module_known
		MOV X00, [X02 + module_instance_folder_OFF]
		INT INT_FOLDER_OPEN_DESC_FILE_OF_PATH
		JMPERR err_class_not_found
		INT INT_ELEMENT_GET_FLAGS
		JMPERR err_class_not_found_closeX00
		BCP X01, CLASS_FILE_FLAG
		JMPNB err_class_not_found_closeX00
		MOV X03, X00 |> save id for later
		MOV X02, 1
		XOR X01, X01
		INT INT_FILE_LOAD_LIB
		SWAP X03, X00
		JMPERR err_class_not_found_closeX00
		INT INT_ELEMENT_CLOSE |> can't fail here
		MOV X03, X00
		JMP add_local_reference
		@err_class_not_found_closeX00
			INT INT_ELEMENT_CLOSE
		@err_class_not_found
			MOV ERRNO, ERR_JAVA_NO_SUCH_CLASS
			RET

#JNI_Env_findInstanceMethod ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_findStaticMethod ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_findInstanceField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_findStaticField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_invokeInstance ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_invokeStatic ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_invokeSuper ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_new ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_getStaticField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_getInstanceField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_putStaticField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_putInstanceField ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_createLocalReference ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_createGlobalReference ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_createWeakReference ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_removeReference ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_ensureFrameSize ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_growFrame ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET

#JNI_Env_shrinkFrame ( --POS-- - JNI_Env_ADD_POS )
	|> TODO implement
	RET


: 1 >
#JNI_Env_POS --POS--
:
	|> GENERATED-CODE-START
	|> this code-block is automatic generated, do not modify
	JNI_Env_throw
	JNI_Env_isThrowing
	JNI_Env_getThrowingClass
	JNI_Env_catch
	JNI_Env_findModule
	JNI_Env_findClass
	JNI_Env_findInstanceMethod
	JNI_Env_findStaticMethod
	JNI_Env_findInstanceField
	JNI_Env_findStaticField
	JNI_Env_invokeInstance
	JNI_Env_invokeStatic
	JNI_Env_invokeSuper
	JNI_Env_new
	JNI_Env_getStaticField
	JNI_Env_getInstanceField
	JNI_Env_putStaticField
	JNI_Env_putInstanceField
	JNI_Env_createLocalReference
	JNI_Env_createGlobalReference
	JNI_Env_createWeakReference
	JNI_Env_removeReference
	JNI_Env_ensureFrameSize
	JNI_Env_growFrame
	JNI_Env_shrinkFrame
	
	|> here is the end of the automatic generated code-block
	|> GENERATED-CODE-END
>

#JNI_Env_LEN ( --POS-- - JNI_Env_POS )

|:	during native code execution do not overwrite those interrupts
|	illegal memory:
|		fast for: throw new NullPointerException();
|	aritmetic error:
|		fast for: throw new AritmeticException("/ by 0");
|:>

#OFF_ILL_MEM ( INT_ERROR_ILLEGAL_MEMORY * 8 )
#OFF_AR_ERR ( INT_ERROR_ARITHMETIC_ERROR * 8 )

$not-align

#AR_ERR_PATH_POS --POS--
: CHARS 'UTF-8' "/java/base/java/lang/ArithmeticException.class\0" >

#OOM_ERR_PATH_POS --POS--
: CHARS 'UTF-8' "/java/base/java/lang/OutOfMemoryError.class\0" >

#NP_ERR_PATH_POS --POS--
: CHARS 'UTF-8' "/java/base/java/lang/NullPointerException.class\0" >

#AR_ERR_MSG_POS --POS--
: CHARS 'ASCII' "/ by zero" >
#AR_ERR_MSG_LEN ( --POS-- - AR_ERR_MSG_POS )

$align
:>

#JAVA_INT_ILLEGAL_MEMORY --POS--
	#REL_POS ( JNI_Env_POS - 8 - --POS-- )
	SGN [IP + REL_POS]
	JMPNE NATIVE_INT_ILL_MEM
		#STATUS_ALL_EXCEPT_EQUAL ( -1 ^ STATUS_EQUAL )
		AND STATUS, STATUS_ALL_EXCEPT_EQUAL
		|:	X00:
		|		0: illegal mem
		|		1: aritmetic error
		|	other reigsters: unmodified since interrupt
		|:>
		@JAVA_INT_HANDLER
			|> X09 stores interrupt pointer
			|> IP register is at offset HEX-0
			|> ERRNO register is at offset HEX-28
			|> X00 register is at offset HEX-30
			|> X09 is the last saved register
			MOV [X09 + HEX-28], ERR_JAVA_THROW
			XOR ERRNO, ERRNO |> allow use of JMPERR
			JMPEQ JAVA_INT_HANDLER__ILL_MEM
				
				|> TODO implement
				JMP JAVA_INT_HANDLER__END
			@JAVA_INT_HANDLER__ILL_MEM
				XOR X01, X01
				MOV X02, [X09]
				MVW X01, [X02]
				CMP X01, UHEX-0004 |> MOV
				JMPEQ JAVA_INT_HANDLER__ILL_MEM__NPE
				CMP X01, UHEX-0320 |> PUSH
				JMPEQ @JAVA_INT_HANDLER__ILL_MEM__OOM
				CMP X01, UHEX-0322 |> PUSHBLK
				JMPEQ @JAVA_INT_HANDLER__ILL_MEM__OOM
				CMP X01, UHEX-0300 |> CALL
				JMPEQ @JAVA_INT_HANDLER__ILL_MEM__OOM
				CMP X01, UHEX-0230 |> INT
				JMPEQ @JAVA_INT_HANDLER__ILL_MEM__OOM
				CMP X01, UHEX-0301 |> CALO
				JMPEQ @JAVA_INT_HANDLER__ILL_MEM__OOM
				CMP X01, UHEX-0302 |> CALNO
				JMPEQ @JAVA_INT_HANDLER__ILL_MEM__OOM
				@JAVA_INT_HANDLER__ILL_MEM__NPE
					#REL_POS ( NP_ERR_PATH_POS - --POS-- )
					LEA X00, REL_POS
					XOR X01, X01
					MOV X02, 1
					INT INT_LOAD_LIB
					JMPERR JAVA_INT_HANDLER__ILL_MEM__LOAD_LIB_ERR
					
					
					|> TODO implement
					JMP JAVA_INT_HANDLER__END
				@JAVA_INT_HANDLER__ILL_MEM__OOM
					
					|> TODO implement
					
			@JAVA_INT_HANDLER__END
			|> overwrite IP and return
			SGN X1D
			JMPLE JAVA_INT_HANDLER__RETURN
				MOV [X09], X1D
				IRET
			@JAVA_INT_HANDLER__RETURN
				POP [X09]
				IRET
			@JAVA_INT_HANDLER__ILL_MEM__LOAD_LIB_ERR
				
				|> TODO implement
				
	@NATIVE_INT_ILL_MEM
		MOV [INTP + OFF_ILL_MEM], -1
		INT INT_ERROR_ILLEGAL_MEMORY

#JAVA_INT_ARITHMETIC_ERROR --POS--
	#REL_POS ( JNI_Env_POS - 8 - --POS-- )
	SGN [IP + REL_POS]
	JMPNE NATIVE_INT_AR_ERR
		OR STATUS, STATUS_EQUAL
		JMP JAVA_INT_HANDLER
	@NATIVE_INT_AR_ERR
		MOV [INTP + OFF_AR_ERR], -1
		INT INT_ERROR_ARITHMETIC_ERROR

#EXP~pvm_java_INIT --POS--
	|> save X00
		PUSH X00
	|> init JNI-Env
		|> init JNI-Env register
			#REL_POS ( JNI_Env_POS - --POS-- )
			LEA X1F, REL_POS
		|> make the JNI-Env.* relative addresses absolute
			#JNI_Env_LEN_m8 ( JNI_Env_LEN - 8 )
			MOV X00, JNI_Env_LEN_m8
			@pvm_java_INIT_JNI_env_loop
				~IF #~ME
					#EXP~JNI_Env_ADD_POS --POS--
				~ENDIF
				ADD [X1F + X00], IP
				USUB X00, 8
				JMPCC pvm_java_INIT_JNI_env_loop
	|> init module hash set
		#REL_POS ( hs_modules_hash - --POS-- )
		#REL_POS2 ( ( modules_set_POS + hashset_hashmaker_OFF ) - --POS-- )
		LEA [IP + REL_POS2], REL_POS
		#REL_POS ( hs_modules_equal - --POS-- )
		#REL_POS2 ( ( modules_set_POS + hashset_equalizer_OFF ) - --POS-- )
		LEA [IP + REL_POS2], REL_POS
	|> overwrite interrupts, which can occure during java code execution
		#REL_POS ( JAVA_INT_ILLEGAL_MEMORY - --POS-- )
		LEA [INTP + OFF_ILL_MEM], REL_POS
		#REL_POS ( JAVA_INT_ARITHMETIC_ERROR - --POS-- )
		LEA [INTP + OFF_AR_ERR], REL_POS
	|> restore X00 and return
		POP X00
		RET

#EXP~pvm_java_MAIN --POS--
		|> TODO implement
