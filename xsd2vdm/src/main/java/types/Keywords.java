/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	xsd2vdm is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	xsd2vdm is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with xsd2vdm. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package types;

import java.util.Arrays;
import java.util.List;

public class Keywords
{
	private static String[] keywords = { "abs", "all", "always", "and", "atomic", "be", "bool", "by", "card", "cases",
			"char", "comp", "compose", "conc", "dcl", "def", "definitions", "dinter", "div", "do", "dom", "dunion",
			"elems", "else", "elseif", "end", "eq", "error", "errs", "exists", "exists1", "exit", "exports", "ext",
			"false", "floor", "for", "forall", "from", "functions", "hd", "if", "imports", "in", "inds", "init",
			"inmap", "in set", "int", "inter", "inv", "inverse", "iota", "is", "lambda", "len", "let", "map",
			"measure", "merge", "mod", "module", "munion", "nat", "nat1", "nil", "not", "not in set", "of",
			"operations", "or", "ord", "others", "post", "power", "pre", "psubset", "pure", "rat", "rd", "real", "rem",
			"renamed", "return", "reverse", "rng", "seq", "seq1", "set", "set1", "skip", "specified", "st", "state",
			"struct", "subset", "then", "tixe", "tl", "to", "token", "traces", "trap", "true", "types", "undefined",
			"union", "uselib", "using", "values", "while", "with", "wr", "yet" };
	
	private static List<String> list = (List<String>) Arrays.asList(keywords);
	
	public static boolean isKeyword(String word)
	{
		return list.contains(word);
	}
}
